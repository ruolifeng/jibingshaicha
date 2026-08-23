<script lang="ts" setup>
import type { TrackConfirmPayload } from "@@/components/TrackingOperationDialog.vue"
import LatentRecordDetailDialog from "@@/components/LatentRecordDetailDialog.vue"
import LatentRecordEditDialog from "@@/components/LatentRecordEditDialog.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import TrackingHistoryPanel from "@@/components/TrackingHistoryPanel.vue"
import TrackingOperationDialog from "@@/components/TrackingOperationDialog.vue"
import { useColumnDistinct } from "@@/composables/useColumnDistinct"
import { runImportWithIdentityConfirm } from "@@/composables/useImportIdentityConfirm"
import {
  displayInfectionJudgeResult,
  displayInfectionScreenMethod,
  getPopulationTypeLabel,
  getPopulationTypeTagType,
  KEY_INFECTION_JUDGE_RESULT_OPTIONS,
  KEY_INFECTION_SCREEN_METHOD_OPTIONS,
  LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS,
  LATENT_MANUAL_POPULATION_TYPE_OPTIONS,
  TRACKING_STATUS_MAP
} from "@@/constants/disease"
import { FORMAT_ISSUE_OPTIONS } from "@@/constants/format-issue"
import { LATENT_IMPORT_FIELDS } from "@@/constants/latent-import"
import { downloadBlob } from "@@/utils/download"
import { getLatentTransferStatusLabel, isLatentTransferLocked } from "@@/utils/latent"
import { confirmDangerDelete } from "@@/utils/listToolbar"
import { parseTrackingHistory } from "@@/utils/referralTracking"
import { extractDateRangeParams } from "@@/utils/searchParams"
import {
  batchDeleteLatentApi,
  closeCaseApi,
  deleteLatentByFilterApi,
  downloadLatentTemplateApi,
  exportAllLatentApi,
  getLatentColumnDistinctApi,
  importLatentApi,
  trackLatentApi
} from "./apis"
import { useLatentOverviewList } from "./composables/useLatentOverviewList"

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
  toQueryParam,
  fetchData,
  handleSearch,
  handleReset
} = useLatentOverviewList()

const genderFilterOptions = [
  { text: "男", value: "男" },
  { text: "女", value: "女" }
]
const populationTypeFilterOptions = LATENT_MANUAL_POPULATION_TYPE_OPTIONS.map(item => ({
  text: item.label,
  value: item.value
}))

const { load: loadDistinct, sourceValues: distinctValues, clearCache } = useColumnDistinct(async (field) => {
  const { data } = await getLatentColumnDistinctApi(field, searchForm.populationType || undefined)
  return Array.isArray(data) ? data : []
})
const loadGenderOptions = () => loadDistinct("gender")
const loadPopulationTypeOptions = () => loadDistinct("populationType")
const loadCrowdCategoryOptions = () => loadDistinct("crowdCategory")
const loadCreatorOptions = () => loadDistinct("creatorUsername")
const screenMethodFilterOptions = KEY_INFECTION_SCREEN_METHOD_OPTIONS.map(item => ({ text: item, value: item }))
const infectionResultFilterOptions = KEY_INFECTION_JUDGE_RESULT_OPTIONS.map(item => ({ text: item, value: item }))

watch(() => searchForm.populationType, (val) => {
  clearCache()
  if (val !== "keyPopulation") {
    searchForm.keyPopulationSubCategories = []
  }
})

const detailVisible = ref(false)
const editVisible = ref(false)
const currentId = ref<string | null>(null)
const exporting = ref(false)
const batchDeleting = ref(false)
const importing = ref(false)
const templateDownloading = ref(false)
const importDialogVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, missingIdCount?: number, errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function buildListQueryParams() {
  const columnFiltersParam = toQueryParam()
  return {
    name: searchForm.name || undefined,
    idNumber: searchForm.idNumber || undefined,
    phone: searchForm.phone || undefined,
    populationType: searchForm.populationType || undefined,
    creatorName: searchForm.creatorName || undefined,
    crowdCategory: searchForm.keyPopulationSubCategories.length
      ? searchForm.keyPopulationSubCategories.join(",")
      : undefined,
    formatIssue: searchForm.formatIssue || undefined,
    trackingStatus: searchForm.trackingStatus,
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {}),
    ...extractDateRangeParams(searchForm.dateRange)
  }
}

function openDetail(row: any) {
  currentId.value = row.id
  detailVisible.value = true
}

function openCreate() {
  currentId.value = null
  editVisible.value = true
}

function openEdit(row: any) {
  currentId.value = row.id
  editVisible.value = true
}

// ==================== 转出 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)

function openReferral(row: any) {
  referralRow.value = row
  referralDialogVisible.value = true
}

// ==================== 追踪 ====================
const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const historyViewVisible = ref(false)
const historyViewRow = ref<any>(null)
const trackSubmitting = ref(false)

function canTrack(row: any) {
  if (!row || row.archived === 1 || isLatentTransferLocked(row)) return false
  const status = row.trackingStatus
  return status == null || status === 0 || status === 2
}

function trackingStatusTagType(status: number | null | undefined) {
  if (status === 1) return "success"
  if (status === 2) return "danger"
  if (status === 3 || status === 4) return "info"
  return "warning"
}

function hasTrackingHistory(row: any) {
  return parseTrackingHistory(row?.trackingHistoryJson).length > 0 || !!row?.trackingRemark?.trim()
}

function openHistoryView(row: any) {
  historyViewRow.value = row
  historyViewVisible.value = true
}

function openTrackDialog(row: any) {
  trackRow.value = row
  trackDialogVisible.value = true
}

async function handleTrack(payload: TrackConfirmPayload) {
  if (trackSubmitting.value || !trackRow.value) return
  trackSubmitting.value = true
  try {
    await trackLatentApi({
      id: trackRow.value.id,
      status: payload.status,
      remark: payload.remark,
      actualArrivalDate: payload.actualArrivalDate
    })
    ElMessage.success("追踪操作已保存")
    trackDialogVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    trackSubmitting.value = false
  }
}

function noticeConfirmStatusLabel(status: number | null | undefined) {
  if (status === 2) return "已确认"
  if (status === 1) return "待确认"
  return "—"
}

function noticeConfirmStatusType(status: number | null | undefined) {
  if (status === 2) return "success"
  if (status === 1) return "warning"
  return "info"
}

async function handleArchive(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.name}」结案归档？归档后将移入「历史患者」。`,
      "归档确认",
      { type: "warning", confirmButtonText: "确认归档", cancelButtonText: "取消" }
    )
    await closeCaseApi(row.id)
    ElMessage.success("归档成功，已移入历史患者")
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error(err?.message || "归档失败")
  }
}

/** 导出：filtered=筛选结果 / selected=勾选 */
async function handleExport(mode: "filtered" | "selected" = "filtered", ids?: string[]) {
  const isSelected = mode === "selected"
  const label = isSelected ? `选中的 ${ids!.length} 条` : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportAllLatentApi(isSelected ? { ids } : buildListQueryParams())
    downloadBlob(blob as unknown as Blob, "在管潜伏感染者信息总表.xlsx")
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}

function handleExportSelected() {
  const ids = selectedRows.value.map(r => r.id).filter(Boolean)
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport("selected", ids)
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先勾选要删除的数据")
    return
  }
  if (selectedRows.value.some(r => isLatentTransferLocked(r))) {
    ElMessage.warning("选中记录包含已转出或转出待确认的数据，不可删除")
    return
  }
  const names = selectedRows.value.map(r => r.name).join("、")
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 条记录（${names}）吗？关联的通知单、督导表、患者等数据将一并删除，且不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    batchDeleting.value = true
    await batchDeleteLatentApi(selectedRows.value.map(r => r.id))
    ElMessage.success(`成功删除 ${selectedRows.value.length} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除勾选失败")
  } finally {
    batchDeleting.value = false
  }
}

async function handleDeleteFiltered() {
  const ok = await confirmDangerDelete({
    title: "删除筛选结果",
    message: "确定删除当前筛选条件下的全部在管潜伏感染者吗？关联的通知单、督导表、患者等数据将一并删除，且不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteLatentByFilterApi(buildListQueryParams())
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch {
    ElMessage.error("删除筛选结果失败")
  } finally {
    batchDeleting.value = false
  }
}

function openImportDialog() {
  importResult.value = { successCount: 0, errors: [] }
  importDialogVisible.value = true
}

async function handleDownloadTemplate() {
  templateDownloading.value = true
  try {
    const blob = await downloadLatentTemplateApi()
    downloadBlob(blob as unknown as Blob, "潜伏感染者导入模板.xlsx")
    ElMessage.success("模板下载成功")
  } catch {
    ElMessage.error("模板下载失败")
  } finally {
    templateDownloading.value = false
  }
}

async function handleImport(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) return
  if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
    ElMessage.error("请上传 .xlsx 或 .xls 文件")
    return
  }
  importing.value = true
  try {
    const data = await runImportWithIdentityConfirm(importLatentApi, file)
    if (!data) return
    importResult.value = data ?? { successCount: 0, errors: [] }
    importResultVisible.value = true
    importDialogVisible.value = false
    if (data.successCount > 0) {
      fetchData()
    }
  } catch {
    ElMessage.error("导入失败")
  } finally {
    importing.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
      :title="`当前在管潜伏感染者共 ${total} 人`"
    />

    <el-card class="search-wrapper" shadow="never">
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="时间段">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="item in LATENT_MANUAL_POPULATION_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="searchForm.populationType === 'keyPopulation'" label="重点人群分类">
          <el-select
            v-model="searchForm.keyPopulationSubCategories"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="item in LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="录入者">
          <el-input v-model="searchForm.creatorName" placeholder="姓名或账号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="格式问题">
          <el-select v-model="searchForm.formatIssue" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="item in FORMAT_ISSUE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="追踪状态">
          <el-select v-model="searchForm.trackingStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(label, val) in TRACKING_STATUS_MAP" :key="val" :label="label" :value="Number(val)" />
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

    <el-card shadow="never" style="margin-top: 10px">
      <div class="toolbar flex items-center justify-end gap-2 flex-wrap" style="margin-bottom: 12px">
        <el-button
          v-permission="'latentManagement:overview'"
          type="success"
          @click="openCreate"
        >
          新增
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
          type="primary"
          plain
          :loading="exporting"
          @click="handleExport('filtered')"
        >
          导出筛选结果
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
          type="danger"
          plain
          :loading="batchDeleting"
          @click="handleDeleteFiltered"
        >
          删除筛选结果
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
          type="warning"
          :disabled="selectedRows.length === 0"
          :loading="exporting"
          @click="handleExportSelected"
        >
          导出勾选
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
          type="danger"
          :disabled="selectedRows.length === 0"
          :loading="batchDeleting"
          @click="handleBatchDelete"
        >
          删除勾选
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
          type="primary"
          @click="openImportDialog"
        >
          导入
        </el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column type="index" label="#" :index="getTableIndex" />
        <el-table-column prop="name" min-width="90">
          <template #header>
            <TableHeaderFilter
              label="姓名"
              :model-value="columnFilters.name"
              @change="(v) => { setFilter('name', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="registrationNo" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="登记号"
              hint="数据来源：通知单（填写/保存潜伏感染者通知单后同步）"
              :model-value="columnFilters.registrationNo"
              @change="(v) => { setFilter('registrationNo', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ row.registrationNo || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="gender" min-width="80">
          <template #header>
            <TableHeaderFilter
              label="性别"
              type="select"
              :options="genderFilterOptions"
              :source-values="distinctValues('gender').value"
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
        <el-table-column prop="screenMethod" min-width="180" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="感染筛查方法"
              type="select"
              :options="screenMethodFilterOptions"
              :model-value="columnFilters.screenMethod"
              @change="(v) => { setFilter('screenMethod', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ displayInfectionScreenMethod(row.screenMethod, row.infectionResult) }}
          </template>
        </el-table-column>
        <el-table-column prop="infectionResult" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="感染筛查结果"
              type="select"
              :options="infectionResultFilterOptions"
              :model-value="columnFilters.infectionResult"
              @change="(v) => { setFilter('infectionResult', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ displayInfectionJudgeResult(row.infectionResult) }}
          </template>
        </el-table-column>
        <el-table-column prop="creatorUsername" min-width="100">
          <template #header>
            <TableHeaderFilter
              label="录入用户"
              type="select"
              :source-values="distinctValues('creatorUsername').value"
              :load-options="loadCreatorOptions"
              :model-value="columnFilters.creatorUsername"
              @change="(v) => { setFilter('creatorUsername', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column label="追踪状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="trackingStatusTagType(row.trackingStatus)" size="small">
              {{ TRACKING_STATUS_MAP[row.trackingStatus] ?? "待追踪" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通知单" min-width="90">
          <template #default="{ row }">
            <el-tag v-if="row.noticeStatus === 1 || row.noticeStatus === 2" type="success" size="small">
              已发送
            </el-tag>
            <el-tag v-else-if="row.noticeStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <span v-else class="text-gray-400">未发送</span>
          </template>
        </el-table-column>
        <el-table-column label="通知单确认状态" min-width="120">
          <template #default="{ row }">
            <el-tag
              v-if="row.noticeStatus === 1 || row.noticeStatus === 2"
              :type="noticeConfirmStatusType(row.noticeStatus)"
              size="small"
            >
              {{ noticeConfirmStatusLabel(row.noticeStatus) }}
            </el-tag>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="populationType" min-width="110">
          <template #header>
            <TableHeaderFilter
              label="数据来源"
              type="select"
              :options="populationTypeFilterOptions"
              :source-values="distinctValues('populationType').value"
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
        <el-table-column prop="crowdCategory" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="人群分类"
              type="select"
              :source-values="distinctValues('crowdCategory').value"
              :load-options="loadCrowdCategoryOptions"
              :model-value="columnFilters.crowdCategory"
              @change="(v) => { setFilter('crowdCategory', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ row.crowdCategory || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="转出状态" width="110">
          <template #default="{ row }">
            <el-tag
              v-if="getLatentTransferStatusLabel(row.archiveRemark)"
              :type="row.archiveRemark === '已转出' ? 'info' : 'warning'"
              size="small"
            >
              {{ getLatentTransferStatusLabel(row.archiveRemark) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="260">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="hasTrackingHistory(row)"
              type="info"
              link
              size="small"
              @click="openHistoryView(row)"
            >
              追踪记录
            </el-button>
            <template v-if="!isLatentTransferLocked(row)">
              <el-button
                v-if="canTrack(row)"
                v-permission="'latentManagement:track'"
                type="primary"
                link
                size="small"
                @click="openTrackDialog(row)"
              >
                追踪
              </el-button>
              <el-button
                v-permission="'latentManagement:edit'"
                type="warning"
                link
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'latentManagement:referral'"
                type="info"
                link
                size="small"
                @click="openReferral(row)"
              >
                转出
              </el-button>
              <el-button
                v-permission="'latentManagement:close'"
                type="danger"
                link
                size="small"
                @click="handleArchive(row)"
              >
                归档
              </el-button>
            </template>
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

    <LatentRecordDetailDialog
      v-model:visible="detailVisible"
      :latent-id="currentId"
    />
    <LatentRecordEditDialog
      v-model:visible="editVisible"
      :latent-id="currentId"
      @success="fetchData"
    />

    <!-- 转出弹窗 -->
    <ReferralDialog
      v-if="referralRow"
      v-model="referralDialogVisible"
      :biz-id="referralRow.id"
      biz-type="latent_aggregate"
      module-type="latent"
      :population-type="referralRow.populationType"
      :subject-name="referralRow.name || ''"
      @success="fetchData"
    />

    <TrackingOperationDialog
      v-model="trackDialogVisible"
      :history-json="trackRow?.trackingHistoryJson"
      :not-in-place-count="trackRow?.notInPlaceCount ?? 0"
      :loading="trackSubmitting"
      @confirm="handleTrack"
    />

    <el-dialog v-model="historyViewVisible" title="追踪记录" width="520px">
      <TrackingHistoryPanel
        v-if="parseTrackingHistory(historyViewRow?.trackingHistoryJson).length"
        :history-json="historyViewRow?.trackingHistoryJson"
      />
      <p v-else-if="historyViewRow?.trackingRemark">
        {{ historyViewRow.trackingRemark }}
      </p>
      <p v-else class="text-secondary">
        暂无追踪记录
      </p>
      <template #footer>
        <el-button @click="historyViewVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="批量导入潜伏感染者" width="560px">
      <el-alert
        type="info"
        :closable="false"
        class="mb-3"
        title="请先下载模板，按表头填写数据后上传。字段与「新增」表单一致。"
      />
      <div class="mb-3">
        <p class="text-sm text-gray-500 mb-2">
          模板包含字段：{{ LATENT_IMPORT_FIELDS.join("、") }}
        </p>
        <p class="text-sm text-gray-500">
          数据来源可填写：{{ LATENT_MANUAL_POPULATION_TYPE_OPTIONS.map(item => item.label).join("、") }}；亦可用「重点人群-老年人」「密接-家庭内」等格式
        </p>
        <p class="text-sm text-gray-500 mt-2">
          人群分类：重点人群填写老年人/糖尿病/双感（可多选，用顿号分隔）；数据来源已填「密接-家庭内」「密接-家庭外」时无需再填
        </p>
        <p class="text-sm text-gray-500 mt-2">
          证件号、联系电话列建议设为「文本」格式，避免 Excel 自动转换导致校验失败
        </p>
      </div>
      <div class="flex gap-2 mb-4">
        <el-button type="success" :loading="templateDownloading" @click="handleDownloadTemplate">
          下载模板
        </el-button>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx,.xls"
          :on-change="handleImport"
        >
          <el-button type="primary" :loading="importing">
            选择文件并导入
          </el-button>
        </el-upload>
      </div>
    </el-dialog>

    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert
        v-if="importResult.successCount > 0"
        :title="`成功导入 ${importResult.successCount} 条数据`"
        type="success"
        :closable="false"
        class="mb-3"
      />
      <el-alert
        v-else
        title="未成功导入任何数据，请检查模板与填写内容"
        type="warning"
        :closable="false"
        class="mb-3"
      />
      <template v-if="(importResult.errors?.length ?? 0) > 0">
        <el-alert
          :title="importResult.missingIdCount
            ? `其中 ${importResult.missingIdCount} 条未填写身份证号已导入，其余问题见下表`
            : `发现 ${importResult.errors?.length ?? 0} 条数据存在问题`"
          type="warning"
          :closable="false"
          class="mb-3"
        />
        <el-table :data="(importResult.errors ?? []).map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" width="60" />
          <el-table-column prop="msg" label="说明" />
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
