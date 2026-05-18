<script lang="ts" setup>
import ImageUploader from "@@/components/ImageUploader.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { usePatientList } from "./composables/usePatientList"
import { saveFirstVisitApi, getFirstVisitDetailApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0)

// ==================== 首次随访 ====================
const firstVisitDialogVisible = ref(false)
const firstVisitRow = ref<any>(null)
const firstVisitForm = reactive<Record<string, any>>({
  patientId: 0,
  visitDate: "",
  visitMethod: "",
  symptomCough: "", symptomHemoptysis: "", symptomFever: "", symptomChestPain: "",
  symptomNightSweats: "", symptomAppetiteLoss: "", symptomFatigue: "",
  sputumSmear: "", chestXrayResult: "", managementMethod: "",
  adverseReaction: "", adverseReactionDetail: "",
  complication: "", complicationDetail: "",
  nextVisitDate: "", doctorSignature: "",
  remarks: "", attachmentUrls: ""
})

async function openFirstVisit(row: any) {
  firstVisitRow.value = row
  firstVisitForm.patientId = row.id
  // 尝试加载已有记录
  try {
    const { data } = await getFirstVisitDetailApi(row.id)
    if (data) {
      Object.assign(firstVisitForm, data)
    } else {
      Object.keys(firstVisitForm).forEach(k => {
        if (k !== "patientId") firstVisitForm[k] = ""
      })
      firstVisitForm.patientId = row.id
    }
  } catch {
    firstVisitForm.patientId = row.id
  }
  firstVisitDialogVisible.value = true
}

async function handleFirstVisitSave() {
  await saveFirstVisitApi({ ...firstVisitForm })
  ElMessage.success("首次随访记录已保存")
  firstVisitDialogVisible.value = false
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
        <el-table-column label="首次随访状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.firstVisitDone ? 'success' : 'warning'" size="small">
              {{ row.firstVisitDone ? "已完成" : "待填写" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'patientManagement:firstVisit'" type="primary" link size="small"
              :disabled="row.archived === 1"
              @click="openFirstVisit(row)">
              {{ row.firstVisitDone ? "查看/编辑" : "填写首次随访" }}
            </el-button>
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

    <!-- 首次随访弹窗 -->
    <el-dialog
      v-model="firstVisitDialogVisible"
      :title="`首次入户随访记录 - ${firstVisitRow?.name ?? ''}`"
      width="700px"
      append-to-body
    >
      <el-form :model="firstVisitForm" label-width="120px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="随访日期">
              <el-date-picker v-model="firstVisitForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访方式">
              <el-select v-model="firstVisitForm.visitMethod">
                <el-option label="门诊" value="门诊" /><el-option label="家庭" value="家庭" /><el-option label="电话" value="电话" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理方式">
              <el-select v-model="firstVisitForm.managementMethod">
                <el-option label="全程督导" value="全程督导" />
                <el-option label="强化期督导" value="强化期督导" />
                <el-option label="全程管理" value="全程管理" />
                <el-option label="自服药" value="自服药" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="痰菌检查">
              <el-select v-model="firstVisitForm.sputumSmear">
                <el-option label="阴性" value="阴性" /><el-option label="阳性" value="阳性" /><el-option label="未检查" value="未检查" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="不良反应">
              <el-select v-model="firstVisitForm.adverseReaction">
                <el-option label="无" value="无" /><el-option label="有" value="有" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="firstVisitForm.adverseReaction === '有'" label="不良反应详情">
              <el-input v-model="firstVisitForm.adverseReactionDetail" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次随访日期">
              <el-date-picker v-model="firstVisitForm.nextVisitDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访医生签名">
              <el-input v-model="firstVisitForm.doctorSignature" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="firstVisitForm.remarks" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附件（可选，最多6张）">
              <ImageUploader v-model="firstVisitForm.attachmentUrls" :min="0" :max="6" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="firstVisitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFirstVisitSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
