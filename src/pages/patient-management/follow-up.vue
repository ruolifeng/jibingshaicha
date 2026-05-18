<script lang="ts" setup>
import FollowUpVisitDialog from "@@/components/FollowUpVisitDialog.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { usePatientList } from "./composables/usePatientList"
import { getFollowUpVisitListApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0)

// ==================== 后续随访弹窗 ====================
const followUpDialogVisible = ref(false)
const followUpPatient = ref<any>(null)

function openFollowUp(row: any) {
  followUpPatient.value = row
  followUpDialogVisible.value = true
}

// ==================== 查看历次随访记录 ====================
const historyVisible = ref(false)
const historyList = ref<any[]>([])
const historyPatientName = ref("")

async function viewHistory(row: any) {
  historyPatientName.value = row.name
  const { data } = await getFollowUpVisitListApi(row.id)
  historyList.value = data || []
  historyVisible.value = true
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'patientManagement:followUp'" type="primary" link size="small"
              :disabled="row.archived === 1"
              @click="openFollowUp(row)">填写后续随访</el-button>
            <el-button type="info" link size="small" @click="viewHistory(row)">查看记录</el-button>
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

    <!-- 后续随访弹窗（复用通用组件） -->
    <FollowUpVisitDialog
      v-if="followUpPatient"
      v-model:visible="followUpDialogVisible"
      :patient-id="followUpPatient.id"
      :patient-name="followUpPatient.name"
      :population-type="followUpPatient.populationType"
      @saved="fetchData"
    />

    <!-- 历次随访记录弹窗 -->
    <el-dialog v-model="historyVisible" :title="`${historyPatientName} - 随访记录`" width="720px" append-to-body>
      <el-table :data="historyList" border stripe>
        <el-table-column prop="visitSeq" label="第几次" width="70" />
        <el-table-column prop="visitDate" label="随访日期" width="110" />
        <el-table-column prop="treatmentMonth" label="治疗月序" width="80" />
        <el-table-column prop="visitMethod" label="随访方式" width="90" />
        <el-table-column prop="missedDoses" label="漏服次数" width="80" />
        <el-table-column prop="nextVisitDate" label="下次随访" width="110" />
        <el-table-column prop="doctorSignature" label="医生签名" width="100" />
        <el-table-column prop="remarks" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>
