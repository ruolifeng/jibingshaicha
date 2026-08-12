<script lang="ts" setup>
import NoticeSentStatusButton from "@@/components/NoticeSentStatusButton.vue"
import PatientNoticeDetailDialog from "@@/components/PatientNoticeDetailDialog.vue"
import PatientNoticeFormDialog from "@@/components/PatientNoticeFormDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_FILTER_OPTIONS } from "@@/constants/disease"
import {
  getPatientTransferStatusLabel,
  isNoticeReceiveOverdue,
  isNoticeSent,
  isPatientTransferLocked,
  resolveMedicationManagementUnit,
  resolveNoticeConfirmedDisplayTime,
  resolveNoticeSentDisplayTime
} from "@@/utils/patient"
import { deletePatientApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"
import { usePatientTableHeaderFilters } from "./composables/usePatientTableHeaderFilters"

const {
  paginationData,
  handleCurrentChange,
  handleSizeChange,
  getTableIndex,
  loading,
  tableData,
  total,
  searchForm,
  columnFilters,
  setFilter,
  fetchData,
  handleSearch,
  handleReset
} = usePatientList(0, { noticeSearch: true })

const {
  genderFilterOptions,
  pathogenFilterOptions,
  populationTypeFilterOptions,
  loadGenderOptions,
  loadPathogenOptions,
  loadPopulationTypeOptions,
  genderSourceValues,
  pathogenSourceValues,
  populationTypeSourceValues
} = usePatientTableHeaderFilters(0)

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
            <el-option v-for="item in PATHOGEN_RESULT_FILTER_OPTIONS" :key="item" :label="item" :value="item" />
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
        <el-table-column type="index" label="#" :index="getTableIndex" />
        <el-table-column prop="populationType" min-width="110">
          <template #header>
            <TableHeaderFilter
              label="数据来源"
              type="select"
              :options="populationTypeFilterOptions"
              :source-values="populationTypeSourceValues"
              :load-options="loadPopulationTypeOptions"
              :model-value="columnFilters.populationType"
              @change="(v) => { setFilter('populationType', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" min-width="90">
          <template #header>
            <TableHeaderFilter
              label="姓名"
              :model-value="columnFilters.name"
              @change="(v) => { setFilter('name', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="gender" min-width="80">
          <template #header>
            <TableHeaderFilter
              label="性别"
              type="select"
              :options="genderFilterOptions"
              :source-values="genderSourceValues"
              :load-options="loadGenderOptions"
              :model-value="columnFilters.gender"
              @change="(v) => { setFilter('gender', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" min-width="160" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="证件号"
              :model-value="columnFilters.idNumber"
              @change="(v) => { setFilter('idNumber', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="phone" min-width="120">
          <template #header>
            <TableHeaderFilter
              label="联系电话"
              :model-value="columnFilters.phone"
              @change="(v) => { setFilter('phone', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisResult" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="病原学结果"
              type="select"
              :options="pathogenFilterOptions"
              :source-values="pathogenSourceValues"
              :load-options="loadPathogenOptions"
              :model-value="columnFilters.diagnosisResult"
              @change="(v) => { setFilter('diagnosisResult', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column label="发送时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'notice-overdue-text': isNoticeReceiveOverdue(row) }">
              {{ resolveNoticeSentDisplayTime(row) || "-" }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="发送人" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.noticeSenderName || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="接收时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveNoticeConfirmedDisplayTime(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="接收人" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.noticeReceiverName || "-" }}
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
                  v-permission="'patientManagement:notice:fill'"
                  type="primary"
                  link
                  size="small"
                  :disabled="row.archived === 1"
                  @click="openNotice(row)"
                >
                  填写通知单
                </el-button>
              </template>
              <NoticeSentStatusButton v-else-if="isNoticeSent(row)" />
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
