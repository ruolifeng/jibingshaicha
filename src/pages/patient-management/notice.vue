<script lang="ts" setup>
import PatientNoticeDetailDialog from "@@/components/PatientNoticeDetailDialog.vue"
import PatientNoticeFormDialog from "@@/components/PatientNoticeFormDialog.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_OPTIONS } from "@@/constants/disease"
import {
  isNoticeReceiveOverdue,
  isPatientTransferLocked,
  getPatientTransferStatusLabel,
  resolveMedicationManagementUnit,
  resolveNoticeConfirmedDisplayTime,
  resolveNoticeSentDisplayTime
} from "@@/utils/patient"
import { deletePatientApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0, { noticeSearch: true })

const noticeDialogVisible = ref(false)
const noticeDetailVisible = ref(false)
const noticeRow = ref<any>(null)

function openNotice(row: any) {
  noticeRow.value = row
  noticeDialogVisible.value = true
}

function viewNotice(row: any) {
  noticeRow.value = row
  noticeDetailVisible.value = true
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除患者 ${row.name} 及其所有关联数据？此操作不可恢复。`, "警告", { type: "error" })
  await deletePatientApi(row.id)
  ElMessage.success("已删除")
  fetchData()
}

const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
function openReferral(row: any) {
  referralRow.value = row
  referralDialogVisible.value = true
}

function getNoticeRowClass({ row }: { row: any }) {
  return isNoticeReceiveOverdue(row) ? "notice-overdue-row" : ""
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
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="病原学结果">
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable filterable style="width:140px">
            <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="填写通知单时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="服药管理单位">
          <el-input
            v-model="searchForm.medicationManagementUnit"
            placeholder="请输入"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width:140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="疫情筛查" value="regular" />
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
            <el-option label="密接" value="closeContact" />
            <el-option label="专病网" value="specialDisease" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:10px">
      <el-table :data="tableData" v-loading="loading" border stripe :row-class-name="getNoticeRowClass">
        <el-table-column type="index" label="#" />
        <el-table-column label="数据来源">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="病原学结果" />
        <el-table-column label="发送时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'notice-overdue-text': isNoticeReceiveOverdue(row) }">
              {{ resolveNoticeSentDisplayTime(row) || "-" }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="接收时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveNoticeConfirmedDisplayTime(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="服药管理单位" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveMedicationManagementUnit(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.noticeRemark || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="患者通知单">
          <template #default="{ row }">
            <template v-if="row.noticeStatus === 1 || row.noticeStatus === 2">
              <el-button type="primary" link size="small" @click="viewNotice(row)">
                {{ row.name }}通知单
              </el-button>
              <el-tag v-if="row.noticeStatus === 2" type="success" size="small" class="ml-1">
                已确认
              </el-tag>
            </template>
            <el-tag v-else-if="row.noticeStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <template v-if="!isPatientTransferLocked(row)">
              <template v-if="row.noticeStatus == null || row.noticeStatus === 0">
                <el-button
                  v-permission="'patientManagement:notice'"
                  type="primary"
                  link
                  size="small"
                  :disabled="row.archived === 1"
                  @click="openNotice(row)"
                >
                  填写通知单
                </el-button>
              </template>
              <template v-else-if="row.noticeStatus === 1 || row.noticeStatus === 2">
                <el-button
                  v-permission="'patientManagement:notice'"
                  type="primary"
                  link
                  size="small"
                  :disabled="row.archived === 1"
                  @click="openNotice(row)"
                >
                  发送通知单
                </el-button>
              </template>
              <el-button v-permission="'patientManagement:referral'" type="info" link size="small" @click="openReferral(row)">
                转出
              </el-button>
              <el-button v-permission="'patientManagement:delete'" type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
            <el-tag v-else :type="row.archiveRemark === '已转出' ? 'info' : 'warning'" size="small">
              {{ getPatientTransferStatusLabel(row.archiveRemark) }}
            </el-tag>
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

    <PatientNoticeFormDialog
      v-model:visible="noticeDialogVisible"
      :patient-row="noticeRow"
      @success="fetchData"
    />

    <PatientNoticeDetailDialog
      v-model:visible="noticeDetailVisible"
      :patient-row="noticeRow"
      @success="fetchData"
    />

    <ReferralDialog
      v-if="referralRow"
      v-model="referralDialogVisible"
      :biz-id="referralRow.id"
      biz-type="patient_aggregate"
      module-type="patient"
      :population-type="referralRow.populationType"
      :subject-name="referralRow.name || ''"
      @success="fetchData"
    />
  </div>
</template>

<style scoped lang="scss">
:deep(.notice-overdue-row) {
  --el-table-tr-bg-color: #fef0f0;
}

.notice-overdue-text {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
