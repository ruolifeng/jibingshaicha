<script lang="ts" setup>
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { useColumnDistinct } from "@@/composables/useColumnDistinct"
import { runImportWithIdentityConfirm } from "@@/composables/useImportIdentityConfirm"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { CLOSE_CONTACT_CASE_COLUMNS, DIAGNOSIS_RESULT_OPTIONS, HAS_PREVENTIVE_TREATMENT_OPTIONS, REPORT_QUARTER_OPTIONS } from "@@/constants/close-contact-case"
import { PATHOGEN_RESULT_FILTER_OPTIONS } from "@@/constants/disease"
import { FORMAT_ISSUE_OPTIONS } from "@@/constants/format-issue"
import { downloadBlob } from "@@/utils/download"
import { confirmDangerDelete, confirmEditChange } from "@@/utils/listToolbar"
import { extractCreateTimeRangeParams } from "@@/utils/searchParams"
import {
  batchDeleteCloseContactCaseApi,
  createCloseContactCaseApi,
  deleteAllCloseContactCaseApi,
  deleteCloseContactCaseApi,
  deleteCloseContactCaseByFilterApi,
  downloadCloseContactCaseTemplateApi,
  exportCloseContactCaseApi,
  getCloseContactCaseColumnDistinctApi,
  getCloseContactCaseListApi,
  syncCloseContactCaseLatentApi,
  updateCloseContactCaseApi,
  uploadCloseContactCaseApi
} from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()
const batchDeleting = ref(false)

const diagnosisFilterOptions = DIAGNOSIS_RESULT_OPTIONS.map(item => ({ text: item.label, value: item.value }))
const pathogenFilterOptions = PATHOGEN_RESULT_FILTER_OPTIONS.map(item => ({ text: item, value: item }))
/** 支持表头筛选的列（与后端 columnFilters 白名单对齐） */
const HEADER_FILTER_META: Record<string, { label: string, type?: "text" | "select", options?: { text: string, value: string }[] }> = {
  creatorUsername: { label: "录入用户" },
  district: { label: "区/县", type: "select" },
  name: { label: "接触者姓名" },
  idNumber: { label: "身份证号" },
  phone: { label: "接触者电话" },
  sourcePatientName: { label: "患者姓名" },
  sourcePatientBacteriologyResult: { label: "病原学结果", type: "select", options: pathogenFilterOptions },
  finalScreeningResult: { label: "最终筛查结果", type: "select", options: diagnosisFilterOptions },
  infectionCheckResult: { label: "感染检测结果", type: "select" },
  imagingResult: { label: "影像结果", type: "select" },
  sputumCheckResult: { label: "痰检结果", type: "select" },
  hasPreventiveTreatment: { label: "是否预防性治疗", type: "select", options: HAS_PREVENTIVE_TREATMENT_OPTIONS.map(item => ({ text: item.label, value: item.value })) }
}

const DISTINCT_SELECT_FIELDS = new Set([
  "district",
  "city",
  "year",
  "gender",
  "sourcePatientBacteriologyResult",
  "finalScreeningResult",
  "infectionCheckResult",
  "imagingResult",
  "sputumCheckResult",
  "hasPreventiveTreatment"
])

const { load: loadDistinct, sourceValues: distinctValues } = useColumnDistinct(async (field) => {
  const { data } = await getCloseContactCaseColumnDistinctApi(field)
  return Array.isArray(data) ? data : []
})

function getDistinctSourceValues(field: string) {
  return DISTINCT_SELECT_FIELDS.has(field) ? distinctValues(field).value : []
}

function loadDistinctField(field: string) {
  if (DISTINCT_SELECT_FIELDS.has(field)) {
    return loadDistinct(field)
  }
}

const loadDistrictSearchOptions = () => loadDistinct("district")

const loading = ref(false)
const tableData = ref<any[]>([])
const syncingLatent = ref(false)
const total = ref(0)
const tableRef = ref<any>()

const searchForm = reactive({
  name: "",
  idNumber: "",
  district: "",
  phone: "",
  creatorUsername: "",
  diagnosisResult: "",
  sourcePatientBacteriologyResult: "",
  reportYear: "" as string,
  reportQuarterNo: "" as string,
  entryTimeRange: [] as string[],
  formatIssue: "" as string
})

/** 组合为后端可识别的「2026年Q2」 */
const reportQuarterParam = computed(() => {
  if (!searchForm.reportYear || !searchForm.reportQuarterNo) return undefined
  return `${searchForm.reportYear}年Q${searchForm.reportQuarterNo}`
})

const previewColumns = CLOSE_CONTACT_CASE_COLUMNS

function getHeaderFilter(field: string) {
  return HEADER_FILTER_META[field]
}

function onHeaderFilterChange(field: string, value: string) {
  setFilter(field, value)
  handleSearch()
}

async function fetchData() {
  loading.value = true
  try {
    const { entryTimeRange, reportYear: _y, reportQuarterNo: _q, formatIssue, ...rest } = searchForm
    const columnFiltersParam = toQueryParam()
    const res = await getCloseContactCaseListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...rest,
      name: rest.name || undefined,
      idNumber: rest.idNumber || undefined,
      district: rest.district || undefined,
      phone: rest.phone || undefined,
      creatorUsername: rest.creatorUsername || undefined,
      diagnosisResult: rest.diagnosisResult || undefined,
      sourcePatientBacteriologyResult: rest.sourcePatientBacteriologyResult || undefined,
      reportQuarter: reportQuarterParam.value,
      ...(formatIssue ? { formatIssue } : {}),
      ...extractCreateTimeRangeParams(entryTimeRange),
      ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
    selectedRows.value = []
    tableRef.value?.clearCheckboxRow?.()
  }
}

function handleSearch() {
  paginationData.currentPage = 1
  fetchData()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.district = ""
  searchForm.phone = ""
  searchForm.creatorUsername = ""
  searchForm.diagnosisResult = ""
  searchForm.sourcePatientBacteriologyResult = ""
  searchForm.reportYear = ""
  searchForm.reportQuarterNo = ""
  searchForm.entryTimeRange = []
  searchForm.formatIssue = ""
  clearFilters()
  handleSearch()
}

const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, missingIdCount?: number, errors: string[] }>({ successCount: 0, errors: [] })
const templateDownloading = ref(false)
const selectedRows = ref<any[]>([])

function syncSelectedRows() {
  selectedRows.value = tableRef.value?.getCheckboxRecords?.() ?? []
}

function handleCheckboxChange() {
  syncSelectedRows()
}

async function handleDownloadTemplate() {
  templateDownloading.value = true
  try {
    const blob = await downloadCloseContactCaseTemplateApi()
    downloadBlob(blob as unknown as Blob, "密接个案表模板.xlsx")
    ElMessage.success("模板下载成功")
  } catch {
    ElMessage.error("模板下载失败")
  } finally {
    templateDownloading.value = false
  }
}

async function handleUpload(uploadFile: any) {
  try {
    const data = await runImportWithIdentityConfirm(uploadCloseContactCaseApi, uploadFile.raw)
    if (!data) return
    importResult.value = data
    importResultVisible.value = true
    if (data.successCount > 0) fetchData()
  } catch (err: any) {
    ElMessage.error(err?.message || "上传失败")
  }
}

async function handleSyncLatent() {
  try {
    await ElMessageBox.confirm(
      "将把最终筛查结果为「潜伏感染者」的个案同步到潜伏感染者在管：已手工录入的按证件号补充空白字段，未录入的自动新建。是否继续？",
      "同步潜伏在管",
      { confirmButtonText: "确认同步", cancelButtonText: "取消", type: "info" }
    )
  } catch {
    return
  }
  syncingLatent.value = true
  try {
    const { data } = await syncCloseContactCaseLatentApi()
    ElMessage.success(`同步完成，共处理 ${data ?? 0} 条`)
  } catch (err: any) {
    ElMessage.error(err?.message || "同步失败")
  } finally {
    syncingLatent.value = false
  }
}

function getSelectedRows() {
  return selectedRows.value
}

function buildListQueryParams() {
  const { entryTimeRange, reportYear: _y, reportQuarterNo: _q, formatIssue, ...rest } = searchForm
  const columnFiltersParam = toQueryParam()
  return {
    name: rest.name || undefined,
    idNumber: rest.idNumber || undefined,
    district: rest.district || undefined,
    phone: rest.phone || undefined,
    creatorUsername: rest.creatorUsername || undefined,
    diagnosisResult: rest.diagnosisResult || undefined,
    sourcePatientBacteriologyResult: rest.sourcePatientBacteriologyResult || undefined,
    reportQuarter: reportQuarterParam.value,
    ...(formatIssue ? { formatIssue } : {}),
    ...extractCreateTimeRangeParams(entryTimeRange),
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
  }
}

function buildExportParams(exportType?: "latent" | "confirmed") {
  return { ...buildListQueryParams(), exportType }
}

async function handleExport(ids?: string[], exportType?: "latent" | "confirmed", mode: "all" | "filtered" | "selected" = "filtered") {
  const isSelectedExport = !!ids?.length
  const label = exportType === "latent"
    ? "潜伏感染者"
    : exportType === "confirmed"
      ? "确诊患者"
      : isSelectedExport
        ? `选中的 ${ids!.length} 条`
        : mode === "all"
          ? "全部"
          : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    const blob = await exportCloseContactCaseApi(
      isSelectedExport
        ? { ids }
        : mode === "all"
          ? { exportType }
          : { ...buildExportParams(exportType) }
    )
    const filename = exportType === "latent"
      ? "密接个案表_潜伏感染者.xlsx"
      : exportType === "confirmed"
        ? "密接个案表_确诊患者.xlsx"
        : "密接个案表.xlsx"
    downloadBlob(blob as unknown as Blob, filename)
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error(err?.message || "导出失败")
  }
}

function handleExportSelected() {
  const rows = getSelectedRows()
  const ids = rows.map((item: any) => item.id).filter(Boolean)
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport(ids, undefined, "selected")
}

/** 编辑弹窗 */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<Record<string, any>>({})
const editMode = ref<"create" | "edit">("edit")

function getEmptyEditForm() {
  return {
    city: "",
    district: "",
    sourcePatientName: "",
    sourcePatientCaseNo: "",
    sourcePatientPhone: "",
    name: "",
    idNumber: "",
    age: undefined,
    phone: "",
    gender: "",
    ethnicity: "",
    contactType: "",
    contactPlace: "",
    registrationDate: "",
    firstScreenDate: "",
    symptom1: "",
    symptom2: "",
    infectionCheckDate: "",
    infectionCheckMethod: "",
    infectionCheckResult: "",
    imagingDate: "",
    imagingMethod: "",
    imagingResult: "",
    sputumCheckDate: "",
    sputumCheckMethod: "",
    sputumCheckResult: "",
    finalScreeningResult: "",
    hasPreventiveTreatment: "",
    preventivePlan: "",
    treatmentCompleted: "",
    remark: ""
  }
}

function handleCreate() {
  editMode.value = "create"
  editForm.value = getEmptyEditForm()
  editVisible.value = true
}

function handleEdit(row: any) {
  editMode.value = "edit"
  editForm.value = { ...row }
  editVisible.value = true
}

async function handleSave() {
  if (editMode.value === "edit") {
    const name = editForm.value.name?.trim() || "该个案"
    const confirmed = await confirmEditChange(`「${name}」信息`)
    if (!confirmed) return
  }
  editSaving.value = true
  try {
    if (editMode.value === "create") {
      await createCloseContactCaseApi(editForm.value)
      ElMessage.success("新增成功")
    } else {
      await updateCloseContactCaseApi(editForm.value.id, editForm.value)
      ElMessage.success("保存成功")
    }
    editVisible.value = false
    fetchData()
  } catch {
    ElMessage.error(editMode.value === "create" ? "新增失败" : "保存失败")
  } finally {
    editSaving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」的个案记录吗？删除后不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    await deleteCloseContactCaseApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除失败")
  }
}

async function handleBatchDelete() {
  const rows = getSelectedRows()
  if (!rows.length) {
    ElMessage.warning("请先勾选要删除的数据")
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${rows.length} 条个案记录吗？删除后不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    const ids = rows.map((r: any) => r.id)
    batchDeleting.value = true
    await batchDeleteCloseContactCaseApi(ids)
    ElMessage.success(`成功删除 ${ids.length} 条记录`)
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
    message: "确定删除当前筛选条件下的全部个案记录吗？删除后不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteCloseContactCaseByFilterApi(buildListQueryParams())
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    fetchData()
  } catch {
    ElMessage.error("删除筛选结果失败")
  } finally {
    batchDeleting.value = false
  }
}

async function handleDeleteAll() {
  const ok = await confirmDangerDelete({
    title: "删除全部",
    message: "确定删除权限范围内的全部个案记录吗？此操作不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteAllCloseContactCaseApi()
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    handleReset()
  } catch {
    ElMessage.error("删除全部失败")
  } finally {
    batchDeleting.value = false
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailRow = ref<any>(null)

function viewDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

function formatDetailValue(val: unknown) {
  if (val === null || val === undefined || val === "") return "—"
  return String(val)
}

type TagType = "primary" | "success" | "info" | "warning" | "danger"

function getDiagnosisTag(result: string): TagType {
  if (result === "活动性肺结核") return "danger"
  if (result === "潜伏感染者") return "warning"
  if (result === "未发现异常") return "success"
  return "info"
}

watch(() => [paginationData.currentPage, paginationData.pageSize], fetchData, { immediate: true })
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="接触者姓名" clearable />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="searchForm.idNumber" placeholder="身份证号" clearable />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="searchForm.phone" placeholder="接触者电话" clearable />
        </el-form-item>
        <el-form-item label="区县">
          <el-select
            v-model="searchForm.district"
            filterable
            clearable
            placeholder="请选择区/县"
            style="width: 160px"
            @visible-change="(v: boolean) => v && loadDistrictSearchOptions()"
          >
            <el-option v-for="item in distinctValues('district').value" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="录入用户">
          <el-input v-model="searchForm.creatorUsername" placeholder="录入账号" clearable />
        </el-form-item>
        <el-form-item label="格式问题">
          <el-select v-model="searchForm.formatIssue" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="item in FORMAT_ISSUE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="病原学结果">
          <el-select
            v-model="searchForm.sourcePatientBacteriologyResult"
            placeholder="全部"
            clearable
            filterable
            style="width: 160px"
          >
            <el-option v-for="item in PATHOGEN_RESULT_FILTER_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="最终筛查结果">
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="opt in DIAGNOSIS_RESULT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="报表填报季度">
          <el-date-picker
            v-model="searchForm.reportYear"
            type="year"
            value-format="YYYY"
            placeholder="年份"
            clearable
            style="width: 110px"
          />
          <el-select
            v-model="searchForm.reportQuarterNo"
            placeholder="季度"
            clearable
            style="width: 90px; margin-left: 8px"
          >
            <el-option v-for="opt in REPORT_QUARTER_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="录入时间">
          <el-date-picker
            v-model="searchForm.entryTimeRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
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

    <!-- 电子表格预览 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between flex-wrap gap-2">
          <div>
            <span class="text-lg font-bold">密接个案表</span>
            <span class="text-sm text-gray-400 ml-2">线上预览模式 · 横向滚动查看全部字段</span>
          </div>
          <div class="flex gap-2 flex-wrap">
            <el-button v-permission="'closeContact:case:create'" type="success" @click="handleCreate">
              新增
            </el-button>
            <el-button v-permission="'closeContact:case:export'" type="primary" plain @click="() => handleExport(undefined, undefined, 'filtered')">
              导出筛选结果
            </el-button>
            <el-button v-permission="'closeContact:case:delete'" type="danger" plain :loading="batchDeleting" @click="handleDeleteFiltered">
              删除筛选结果
            </el-button>
            <el-button v-permission="'closeContact:case:export'" type="warning" :disabled="!selectedRows.length" @click="handleExportSelected">
              导出勾选
            </el-button>
            <el-button v-permission="'closeContact:case:delete'" type="danger" :loading="batchDeleting" :disabled="!selectedRows.length" @click="handleBatchDelete">
              删除勾选
            </el-button>
            <el-button
              v-permission="'closeContact:case:upload'"
              type="success"
              plain
              :loading="templateDownloading"
              @click="handleDownloadTemplate"
            >
              下载模板
            </el-button>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleUpload">
              <el-button v-permission="'closeContact:case:upload'" type="primary">
                导入 Excel
              </el-button>
            </el-upload>
            <el-button
              v-permission="'closeContact:case:upload'"
              type="warning"
              plain
              :loading="syncingLatent"
              @click="handleSyncLatent"
            >
              同步潜伏在管
            </el-button>
            <el-button v-permission="'closeContact:case:export'" @click="() => handleExport(undefined, undefined, 'all')">
              导出全部
            </el-button>
            <el-button v-permission="'closeContact:case:delete'" type="danger" plain :loading="batchDeleting" @click="handleDeleteAll">
              删除全部
            </el-button>
            <el-button v-permission="'closeContact:case:export'" type="warning" plain @click="() => handleExport(undefined, 'latent', 'filtered')">
              导出潜伏感染者
            </el-button>
            <el-button v-permission="'closeContact:case:export'" type="danger" plain @click="() => handleExport(undefined, 'confirmed', 'filtered')">
              导出确诊患者
            </el-button>
          </div>
        </div>
      </template>

      <div class="spreadsheet-wrap">
        <vxe-table
          ref="tableRef"
          :data="tableData"
          :loading="loading"
          border
          stripe
          max-height="620"
          :row-config="{ keyField: 'id' }"
          :column-config="{ resizable: true }"
          :scroll-x="{ enabled: true, gt: 0 }"
          :scroll-y="{ enabled: true, gt: 0 }"
          show-overflow
          show-header-overflow
          @checkbox-change="handleCheckboxChange"
          @checkbox-all="handleCheckboxChange"
        >
          <vxe-column type="checkbox" width="40" fixed="left" />
          <vxe-column
            v-for="col in previewColumns"
            :key="col.field"
            :field="col.field"
            :title="col.title"
            :min-width="col.width"
            :fixed="col.fixed"
          >
            <template v-if="getHeaderFilter(col.field)" #header>
              <TableHeaderFilter
                :label="getHeaderFilter(col.field)!.label"
                :type="getHeaderFilter(col.field)!.type || 'text'"
                :options="getHeaderFilter(col.field)!.options || []"
                :source-values="getDistinctSourceValues(col.field)"
                :load-options="() => loadDistinctField(col.field)"
                :model-value="columnFilters[col.field]"
                @change="(v) => onHeaderFilterChange(col.field, v)"
              />
            </template>
            <template v-if="col.field === 'finalScreeningResult'" #default="{ row }">
              <el-tag v-if="row.finalScreeningResult" :type="getDiagnosisTag(row.finalScreeningResult)" size="small">
                {{ row.finalScreeningResult }}
              </el-tag>
              <span v-else class="text-gray-400">—</span>
            </template>
          </vxe-column>
          <vxe-column title="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">
                详情
              </el-button>
              <el-button v-permission="'closeContact:case:edit'" type="warning" link size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button v-permission="'closeContact:case:delete'" type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </vxe-column>
        </vxe-table>
      </div>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? '新增个案' : '编辑个案'" width="960px" :close-on-click-modal="false">
      <el-tabs>
        <el-tab-pane label="原患者信息">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="市/州">
                  <el-input v-model="editForm.city" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="区/县">
                  <el-input v-model="editForm.district" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="患者姓名">
                  <el-input v-model="editForm.sourcePatientName" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="传报卡号">
                  <el-input v-model="editForm.sourcePatientCaseNo" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="患者电话">
                  <el-input v-model="editForm.sourcePatientPhone" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="接触者基本信息">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="接触者姓名">
                  <el-input v-model="editForm.name" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="身份证号">
                  <el-input v-model="editForm.idNumber" placeholder="可填无" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="年龄">
                  <el-input-number v-model="editForm.age" :min="0" :max="150" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="接触者电话">
                  <el-input v-model="editForm.phone" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="性别">
                  <el-select v-model="editForm.gender" style="width:100%">
                    <el-option label="男" value="男" /><el-option label="女" value="女" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="民族">
                  <el-input v-model="editForm.ethnicity" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="接触类型">
                  <el-input v-model="editForm.contactType" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="接触场所">
                  <el-input v-model="editForm.contactPlace" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="登记日期">
                  <el-date-picker v-model="editForm.registrationDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="筛查与诊断">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="首次筛查日期">
                  <el-date-picker v-model="editForm.firstScreenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测方法">
                  <el-input v-model="editForm.infectionCheckMethod" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测结果">
                  <el-input v-model="editForm.infectionCheckResult" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像方法">
                  <el-input v-model="editForm.imagingMethod" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像结果">
                  <el-input v-model="editForm.imagingResult" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="痰检方法">
                  <el-input v-model="editForm.sputumCheckMethod" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="痰检结果">
                  <el-input v-model="editForm.sputumCheckResult" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最终筛查结果">
                  <el-select v-model="editForm.finalScreeningResult" style="width:100%" clearable>
                    <el-option v-for="opt in DIAGNOSIS_RESULT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="预防治疗">
          <el-form :model="editForm" label-width="160px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="是否开展预防治疗">
                  <el-select v-model="editForm.hasPreventiveTreatment" style="width:100%" clearable placeholder="请选择">
                    <el-option v-for="opt in HAS_PREVENTIVE_TREATMENT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="预防性治疗方案">
                  <el-input v-model="editForm.preventivePlan" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否完成治疗">
                  <el-input v-model="editForm.treatmentCompleted" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注">
                  <el-input v-model="editForm.remark" type="textarea" :rows="2" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="editVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗：展示全部字段（与表格列一致），手动新增表单字段保持不变 -->
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name || ''} — 密接个案详情`" width="960px" top="5vh">
      <el-descriptions v-if="detailRow" :column="2" border class="case-detail-desc">
        <el-descriptions-item
          v-for="col in CLOSE_CONTACT_CASE_COLUMNS"
          :key="col.field"
          :label="col.title"
          :span="['remark', 'contraindicationRemark', 'preventivePlanRemark', 'createTime'].includes(col.field) ? 2 : 1"
        >
          <template v-if="col.field === 'finalScreeningResult'">
            <el-tag v-if="detailRow.finalScreeningResult" :type="getDiagnosisTag(detailRow.finalScreeningResult)">
              {{ detailRow.finalScreeningResult }}
            </el-tag>
            <span v-else>—</span>
          </template>
          <template v-else>
            {{ formatDetailValue(detailRow[col.field]) }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRow.gender" label="性别">
          {{ detailRow.gender }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRow.ethnicity" label="民族">
          {{ detailRow.ethnicity }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRow.householdAddress" label="户籍地址" :span="2">
          {{ detailRow.householdAddress }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRow.currentAddress" label="现住址" :span="2">
          {{ detailRow.currentAddress }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert :title="`成功导入 ${importResult.successCount} 条数据`" type="success" :closable="false" class="mb-3" />
      <template v-if="importResult.errors.length > 0">
        <el-alert :title="importResult.missingIdCount ? `其中 ${importResult.missingIdCount} 条未填写身份证号已导入，其余问题见下表` : `发现 ${importResult.errors.length} 条数据格式问题（已照常导入，请核查）`" type="warning" :closable="false" class="mb-3" />
        <el-table :data="importResult.errors.map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" />
          <el-table-column prop="msg" label="错误信息" />
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

<style lang="scss" scoped>
.spreadsheet-wrap {
  width: 100%;
  overflow: hidden;

  :deep(.vxe-table) {
    font-size: 13px;
  }

  :deep(.vxe-header--column) {
    background-color: var(--el-fill-color-light);
    font-weight: 600;
  }
}

.case-detail-desc {
  max-height: 70vh;
  overflow-y: auto;
}
</style>
