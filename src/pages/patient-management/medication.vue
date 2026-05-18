<script lang="ts" setup>
import { getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { usePatientList } from "./composables/usePatientList"
import { saveMedicationApi, getMedicationDetailApi, archivePatientApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0)

// ==================== 服药管理弹窗 ====================
const medicationDialogVisible = ref(false)
const medicationRow = ref<any>(null)
const medicationForm = reactive({
  patientId: 0,
  managementMethod: "",
  supervisingPerson: "",
  sputumTestResult: "",
  stopDate: "",
  remark: ""
})

async function openMedication(row: any) {
  medicationRow.value = row
  medicationForm.patientId = row.id
  try {
    const { data } = await getMedicationDetailApi(row.id)
    if (data) Object.assign(medicationForm, data)
    else {
      Object.assign(medicationForm, {
        patientId: row.id, managementMethod: "", supervisingPerson: "",
        sputumTestResult: "", stopDate: "", remark: ""
      })
    }
  } catch {
    medicationForm.patientId = row.id
  }
  medicationDialogVisible.value = true
}

async function handleMedicationSave() {
  await saveMedicationApi({ ...medicationForm })
  // 若已填停止完成时间，则触发归档
  if (medicationForm.stopDate) {
    await archivePatientApi(medicationForm.patientId)
    ElMessage.success("服药管理已保存，患者已归入历史患者")
  } else {
    ElMessage.success("服药管理已保存")
  }
  medicationDialogVisible.value = false
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <el-card class="search-wrapper" shadow="never">
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width:140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="常规筛查" value="regular" />
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
            <el-option label="密接" value="closeContact" />
            <el-option label="专病网" value="specialDisease" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:10px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="数据来源" width="100">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="idNumber" label="证件号" width="170" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="diagnosisResult" label="诊断结果" width="110" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'patientManagement:medication'" type="primary" link size="small"
              :disabled="row.archived === 1"
              @click="openMedication(row)">服药管理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination-container"
        :current-page="paginationData.currentPage || 1"
        :page-sizes="paginationData.pageSizes"
        :page-size="paginationData.pageSize || 10"
        :total="total || 0"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 服药管理弹窗 -->
    <el-dialog
      v-model="medicationDialogVisible"
      :title="`服药管理 - ${medicationRow?.name ?? ''}`"
      width="520px"
      append-to-body
    >
      <el-form :model="medicationForm" label-width="130px">
        <el-form-item label="管理方式">
          <el-select v-model="medicationForm.managementMethod">
            <el-option label="全程督导" value="全程督导" />
            <el-option label="强化期督导" value="强化期督导" />
            <el-option label="全程管理" value="全程管理" />
            <el-option label="自服药" value="自服药" />
          </el-select>
        </el-form-item>
        <el-form-item label="督导人员">
          <el-select v-model="medicationForm.supervisingPerson">
            <el-option label="医生" value="医生" /><el-option label="家属" value="家属" />
            <el-option label="志愿者" value="志愿者" /><el-option label="患者本人" value="患者本人" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗前痰菌结果">
          <el-select v-model="medicationForm.sputumTestResult">
            <el-option label="阴性" value="阴性" /><el-option label="阳性" value="阳性" />
            <el-option label="无结果" value="无结果" /><el-option label="未检查" value="未检查" />
          </el-select>
        </el-form-item>
        <el-form-item label="停止完成时间">
          <el-date-picker v-model="medicationForm.stopDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="medicationForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medicationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMedicationSave">
          {{ medicationForm.stopDate ? "保存并归档" : "保存" }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
