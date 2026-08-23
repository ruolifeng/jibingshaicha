<script lang="ts" setup>
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { useColumnDistinct } from "@@/composables/useColumnDistinct"
import { runImportWithIdentityConfirm } from "@@/composables/useImportIdentityConfirm"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import {
  applyFinalScreeningResult,
  CLOSE_CONTACT_CASE_COLUMNS,
  CLOSE_CONTACT_CASE_DATE_FIELDS,
  CLOSE_CONTACT_CASE_EDIT_GROUPS,
  closeContactCaseFieldLabel,
  DIAGNOSIS_RESULT_OPTIONS,
  HAS_PREVENTIVE_TREATMENT_OPTIONS,
  isFinalScreeningOther,
  PREVENTIVE_PLAN_OPTIONS,
  REPORT_QUARTER_OPTIONS,
  resolveFinalScreeningResultForSave
} from "@@/constants/close-contact-case"
import { FORMAT_ISSUE_OPTIONS } from "@@/constants/format-issue"
import {
  CC_INFECTION_CHECK_METHOD_OPTIONS,
  CC_INFECTION_CHECK_RESULT_OPTIONS
} from "@@/constants/screening-close-contact"
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

/** 支持表头筛选的列（与后端 columnFilters 白名单对齐） */
const HEADER_FILTER_META: Record<string, { label: string, type?: "text" | "select", options?: { text: string, value: string }[] }> = {
  creatorUsername: { label: "录入用户" },
  district: { label: "区/县", type: "select" },
  name: { label: "接触者姓名" },
  idNumber: { label: "身份证号" },
  phone: { label: "接触者电话" },
  sourcePatientName: { label: "患者姓名" },
  sourcePatientBacteriologyResult: { label: "病原学结果", type: "select" },
  finalScreeningResult: { label: "最终筛查结果", type: "select" },
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
const loadPathogenSearchOptions = () => loadDistinct("sourcePatientBacteriologyResult")
const loadFinalScreeningSearchOptions = () => loadDistinct("finalScreeningResult")

const loading = ref(false)
const tableData = ref<any[]>([])
const syncingLatent = ref(false)
const total = ref(0)
const tableRef = ref<any>()

const searchForm = reactive({
  name: "",
  idNumber: "",
  district: [] as string[],
  phone: "",
  creatorUsername: "",
  diagnosisResult: [] as string[],
  sourcePatientBacteriologyResult: [] as string[],
  reportYear: "" as string,
  reportQuarterNo: [] as string[],
  entryTimeRange: [] as string[],
  formatIssue: [] as string[]
})

/** 组合为后端可识别的「2026年Q1,2026年Q2」（支持多季度） */
const reportQuarterParam = computed(() => {
  if (!searchForm.reportYear || !searchForm.reportQuarterNo.length) return undefined
  return searchForm.reportQuarterNo.map(q => `${searchForm.reportYear}年Q${q}`).join(",")
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
    const res = await getCloseContactCaseListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...buildListQueryParams()
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
  searchForm.district = []
  searchForm.phone = ""
  searchForm.creatorUsername = ""
  searchForm.diagnosisResult = []
  searchForm.sourcePatientBacteriologyResult = []
  searchForm.reportYear = ""
  searchForm.reportQuarterNo = []
  searchForm.entryTimeRange = []
  searchForm.formatIssue = []
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
    district: rest.district.length ? rest.district.join(",") : undefined,
    phone: rest.phone || undefined,
    creatorUsername: rest.creatorUsername || undefined,
    diagnosisResult: rest.diagnosisResult.length ? rest.diagnosisResult.join(",") : undefined,
    sourcePatientBacteriologyResult: rest.sourcePatientBacteriologyResult.length
      ? rest.sourcePatientBacteriologyResult.join(",")
      : undefined,
    reportQuarter: reportQuarterParam.value,
    ...(formatIssue.length ? { formatIssue: formatIssue.join(",") } : {}),
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

/** 编辑弹窗（覆盖表格全部业务字段，清空即覆盖入库） */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<Record<string, any>>({})
const editMode = ref<"create" | "edit">("edit")
const editActiveTab = ref(CLOSE_CONTACT_CASE_EDIT_GROUPS[0]?.key || "source")

const EDIT_TEXTAREA_FIELDS = new Set([
  "remark",
  "contraindicationRemark",
  "preventivePlanRemark",
  "noTreatmentReason",
  "incompleteReason",
  "householdAddress",
  "currentAddress",
  "symptom2",
  "followup6Symptom2",
  "followup12Symptom2",
  "followup24Symptom2"
])

const EDIT_WIDE_FIELDS = new Set([
  ...EDIT_TEXTAREA_FIELDS,
  "householdAddress",
  "currentAddress"
])

function getEmptyEditForm() {
  const form: Record<string, any> = {}
  for (const group of CLOSE_CONTACT_CASE_EDIT_GROUPS) {
    for (const field of group.fields) {
      if (field === "age") {
        form[field] = undefined
      } else if (CLOSE_CONTACT_CASE_DATE_FIELDS.has(field)) {
        form[field] = null
      } else {
        form[field] = ""
      }
    }
  }
  form.finalScreeningRemark = ""
  return form
}

/** 提交前：空日期/空年龄转 null，保证 ALWAYS 策略能清空入库 */
function buildEditPayload() {
  const payload: Record<string, any> = { ...editForm.value }
  for (const field of CLOSE_CONTACT_CASE_DATE_FIELDS) {
    if (payload[field] === "" || payload[field] === undefined) payload[field] = null
  }
  if (payload.age === "" || payload.age === undefined) payload.age = null
  payload.finalScreeningResult = resolveFinalScreeningResultForSave(
    payload.finalScreeningResult,
    payload.finalScreeningRemark
  )
  delete payload.finalScreeningRemark
  // 系统/自动计算列不由表单改写（列表展示值可能被 DerivedSupport 临时填入）
  delete payload.reportQuarter
  delete payload.registrationIntervalHint
  delete payload.ageGroup
  delete payload.createTime
  delete payload.updateTime
  delete payload.creatorUsername
  delete payload.departmentId
  delete payload.uploadBatch
  delete payload.importRowNo
  delete payload.sourcePatientIdNumber
  return payload
}

function handleCreate() {
  editMode.value = "create"
  editActiveTab.value = CLOSE_CONTACT_CASE_EDIT_GROUPS[0]?.key || "source"
  editForm.value = getEmptyEditForm()
  editVisible.value = true
}

function handleEdit(row: any) {
  editMode.value = "edit"
  editActiveTab.value = CLOSE_CONTACT_CASE_EDIT_GROUPS[0]?.key || "source"
  // 先铺满空字段再合并行数据，保证未展示过的列清空后能覆盖入库
  editForm.value = { ...getEmptyEditForm(), ...row, finalScreeningRemark: "" }
  applyFinalScreeningResult(
    editForm.value as { finalScreeningResult: string, finalScreeningRemark: string },
    row.finalScreeningResult
  )
  editVisible.value = true
}

async function handleSave() {
  if (isFinalScreeningOther(editForm.value.finalScreeningResult)
    && !String(editForm.value.finalScreeningRemark || "").trim()) {
    ElMessage.warning("选择「其他（需注明）」时请填写说明")
    return
  }
  if (editMode.value === "edit") {
    const name = editForm.value.name?.trim() || "该个案"
    const confirmed = await confirmEditChange(`「${name}」信息`)
    if (!confirmed) return
  }
  editSaving.value = true
  try {
    const payload = buildEditPayload()
    if (editMode.value === "create") {
      await createCloseContactCaseApi(payload)
      ElMessage.success("新增成功")
    } else {
      await updateCloseContactCaseApi(payload.id, payload)
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
  if (result === "疑似肺结核" || result.startsWith("疑似")) return "warning"
  if (result === "潜伏感染者") return "warning"
  if (result === "未发现异常") return "success"
  return "info"
}

/** 编辑最终筛查结果：兼容历史「未做」等 */
const finalScreeningSelectOptions = computed(() => {
  const opts: Array<{ label: string, value: string }> = DIAGNOSIS_RESULT_OPTIONS.map(item => ({ ...item }))
  const current = String(editForm.value.finalScreeningResult || "").trim()
  if (current && !opts.some(item => item.value === current)) {
    opts.push({ label: current, value: current })
  }
  return opts
})

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
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            clearable
            placeholder="请选择区/县"
            style="width: 200px"
            @visible-change="(v: boolean) => v && loadDistrictSearchOptions()"
          >
            <el-option v-for="item in distinctValues('district').value" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="录入用户">
          <el-input v-model="searchForm.creatorUsername" placeholder="录入账号" clearable />
        </el-form-item>
        <el-form-item label="格式问题">
          <el-select
            v-model="searchForm.formatIssue"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部"
            clearable
            style="width: 220px"
          >
            <el-option v-for="item in FORMAT_ISSUE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="病原学结果">
          <el-select
            v-model="searchForm.sourcePatientBacteriologyResult"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部"
            clearable
            filterable
            style="width: 200px"
            @visible-change="(v: boolean) => v && loadPathogenSearchOptions()"
          >
            <el-option
              v-for="item in distinctValues('sourcePatientBacteriologyResult').value"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="最终筛查结果">
          <el-select
            v-model="searchForm.diagnosisResult"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部"
            clearable
            filterable
            style="width: 200px"
            @visible-change="(v: boolean) => v && loadFinalScreeningSearchOptions()"
          >
            <el-option
              v-for="item in distinctValues('finalScreeningResult').value"
              :key="item"
              :label="item"
              :value="item"
            />
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
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="季度"
            clearable
            style="width: 140px; margin-left: 8px"
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
                查看
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
          :page-sizes="paginationData.pageSizes"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗：覆盖表格全部业务字段（系统列/自动计算列除外） -->
    <el-dialog
      v-model="editVisible"
      :title="editMode === 'create' ? '新增个案' : '编辑个案'"
      width="1100px"
      top="4vh"
      :close-on-click-modal="false"
      class="case-edit-dialog"
    >
      <el-tabs v-model="editActiveTab">
        <el-tab-pane
          v-for="group in CLOSE_CONTACT_CASE_EDIT_GROUPS"
          :key="group.key"
          :label="group.label"
          :name="group.key"
        >
          <el-form :model="editForm" label-width="150px" class="case-edit-form">
            <el-row :gutter="16">
              <el-col
                v-for="field in group.fields"
                :key="field"
                :span="EDIT_WIDE_FIELDS.has(field) ? 24 : 8"
              >
                <el-form-item :label="closeContactCaseFieldLabel(field)">
                  <el-date-picker
                    v-if="CLOSE_CONTACT_CASE_DATE_FIELDS.has(field)"
                    v-model="editForm[field]"
                    type="date"
                    value-format="YYYY-MM-DD"
                    clearable
                    style="width:100%"
                  />
                  <el-input-number
                    v-else-if="field === 'age'"
                    v-model="editForm.age"
                    :min="0"
                    :max="150"
                    controls-position="right"
                    style="width:100%"
                  />
                  <el-select
                    v-else-if="field === 'gender'"
                    v-model="editForm.gender"
                    clearable
                    style="width:100%"
                  >
                    <el-option label="男" value="男" />
                    <el-option label="女" value="女" />
                  </el-select>
                  <template v-else-if="field === 'finalScreeningResult'">
                    <el-select
                      v-model="editForm.finalScreeningResult"
                      clearable
                      style="width:100%"
                      @change="() => { if (!isFinalScreeningOther(editForm.finalScreeningResult)) editForm.finalScreeningRemark = '' }"
                    >
                      <el-option
                        v-for="opt in finalScreeningSelectOptions"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                    <el-input
                      v-if="isFinalScreeningOther(editForm.finalScreeningResult)"
                      v-model="editForm.finalScreeningRemark"
                      type="textarea"
                      :rows="2"
                      placeholder="请注明其他最终筛查结果"
                      style="margin-top: 8px"
                    />
                  </template>
                  <el-select
                    v-else-if="field === 'hasPreventiveTreatment'"
                    v-model="editForm.hasPreventiveTreatment"
                    clearable
                    placeholder="请选择"
                    style="width:100%"
                  >
                    <el-option
                      v-for="opt in HAS_PREVENTIVE_TREATMENT_OPTIONS"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                  <el-select
                    v-else-if="field === 'preventivePlan'"
                    v-model="editForm.preventivePlan"
                    clearable
                    filterable
                    placeholder="请选择"
                    style="width:100%"
                  >
                    <el-option
                      v-for="opt in PREVENTIVE_PLAN_OPTIONS"
                      :key="opt"
                      :label="opt"
                      :value="opt"
                    />
                  </el-select>
                  <el-select
                    v-else-if="field === 'infectionCheckMethod'"
                    v-model="editForm.infectionCheckMethod"
                    clearable
                    filterable
                    style="width:100%"
                  >
                    <el-option
                      v-for="opt in CC_INFECTION_CHECK_METHOD_OPTIONS"
                      :key="opt"
                      :label="opt"
                      :value="opt"
                    />
                  </el-select>
                  <el-select
                    v-else-if="field === 'infectionCheckResult'"
                    v-model="editForm.infectionCheckResult"
                    clearable
                    filterable
                    style="width:100%"
                  >
                    <el-option
                      v-for="opt in CC_INFECTION_CHECK_RESULT_OPTIONS"
                      :key="opt"
                      :label="opt"
                      :value="opt"
                    />
                  </el-select>
                  <el-input
                    v-else-if="EDIT_TEXTAREA_FIELDS.has(field)"
                    v-model="editForm[field]"
                    type="textarea"
                    :rows="2"
                    clearable
                  />
                  <el-input
                    v-else
                    v-model="editForm[field]"
                    :placeholder="field === 'idNumber' ? '可填无' : undefined"
                    clearable
                  />
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

    <!-- 详情弹窗：展示全部字段（与表格列一致） -->
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

.case-edit-form {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 8px;
}
</style>
