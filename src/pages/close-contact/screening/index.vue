<script lang="ts" setup>
import type { FormInstance, FormRules } from "element-plus"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { runImportWithIdentityConfirm } from "@@/composables/useImportIdentityConfirm"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { HAS_PREVENTIVE_TREATMENT_OPTIONS } from "@@/constants/close-contact-case"
import { isSuspectedTbDiagnosis, SCREENING_DIAGNOSIS_SEARCH_OPTIONS, SUSPECTED_TB_DIAGNOSIS } from "@@/constants/disease"
import {
  CC_FINAL_RESULT_STAT_OPTIONS,
  CC_FINAL_SCREENING_RESULT_OPTIONS,
  CC_IMAGING_METHOD_OPTIONS,
  CC_IMAGING_RESULT_OPTIONS,
  CC_INFECTION_CHECK_METHOD_OPTIONS,
  CC_SPUTUM_METHOD_OPTIONS,
  CC_SPUTUM_RESULT_OPTIONS,
  CC_SYMPTOM1_OPTIONS,
  CONTACT_PLACE_OPTIONS,
  CONTACT_PLACE_OTHER,
  CONTACT_TYPE_OPTIONS,
  formatContactPlace,
  formatFieldWithOther,
  getFinalScreeningResultTagType,
  sanitizeScreeningOtherFields,
  SCREENING_FIELD_OTHER,
  selectOptionsWithLegacy
} from "@@/constants/screening-close-contact"
import { extractCreateTimeRangeParams, extractDateRangeParams } from "@@/utils/searchParams"
import { useRouter } from "vue-router"
import {
  batchDeleteScreeningCloseContactApi,
  countByResultApi,
  createScreeningCloseContactApi,
  deleteScreeningCloseContactApi,
  exportScreeningCloseContactApi,
  getScreeningCloseContactListApi,
  submitThreeMonthCheckApi,
  updateScreeningCloseContactApi,
  uploadScreeningCloseContactApi
} from "./apis"

const router = useRouter()
const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

const finalScreeningFilterOptions = CC_FINAL_SCREENING_RESULT_OPTIONS.map(item => ({ text: item, value: item }))

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

/** 分类统计 */
const resultStats = ref<Record<string, number>>({})

const searchForm = reactive({
  name: "",
  idNumber: "",
  district: "",
  phone: "",
  creatorUsername: "",
  dateRange: [] as string[],
  entryTimeRange: [] as string[],
  finalScreeningResult: "" as string
})

type TagType = "primary" | "success" | "info" | "warning" | "danger"

/** 类型安全的 el-tag type */
function tagType(t: string): TagType {
  const allowed = ["primary", "success", "info", "warning", "danger"]
  return (allowed.includes(t) ? t : "info") as TagType
}

/** 流程状态映射 */
const CC_STATUS_MAP: Record<number, { label: string, type: string }> = {
  0: { label: "待处理", type: "info" },
  1: { label: "活动性肺结核-结案", type: "danger" },
  9: { label: "疑似结核-结案", type: "warning" },
  2: { label: "潜伏感染-管理中", type: "warning" },
  3: { label: "潜伏感染-已归档", type: "info" },
  4: { label: "随访监测中", type: "warning" },
  5: { label: "随访监测-已归档", type: "info" },
  6: { label: "待3月复查", type: "warning" },
  7: { label: "3月复查阴性-结束", type: "success" },
  8: { label: "3月阳性-转潜伏流程", type: "danger" }
}

function getFinalResultTag(result?: string, _other?: string): TagType {
  if (!result || result === SCREENING_FIELD_OTHER) return "info"
  return getFinalScreeningResultTagType(result) as TagType
}

function formatFinalScreeningDisplay(result?: string, other?: string): string {
  return formatFieldWithOther(result, other) || ""
}

/** 下拉兼容历史数据 */
const imagingMethodSelectOptions = computed(() =>
  selectOptionsWithLegacy(CC_IMAGING_METHOD_OPTIONS, editForm.value.imagingMethod))
const imagingResultSelectOptions = computed(() =>
  selectOptionsWithLegacy(CC_IMAGING_RESULT_OPTIONS, editForm.value.imagingResult))
const sputumMethodSelectOptions = computed(() =>
  selectOptionsWithLegacy(CC_SPUTUM_METHOD_OPTIONS, editForm.value.sputumCheckMethod))
const sputumResultSelectOptions = computed(() =>
  selectOptionsWithLegacy(CC_SPUTUM_RESULT_OPTIONS, editForm.value.sputumCheckResult))
const finalResultSelectOptions = computed(() =>
  selectOptionsWithLegacy(CC_FINAL_SCREENING_RESULT_OPTIONS, editForm.value.finalScreeningResult))
const followupResultSelectOptions = computed(() => {
  const legacyValues = [editForm.value.followup6Result, editForm.value.followup12Result, editForm.value.followup24Result]
    .filter((value): value is string => !!value && !(CC_FINAL_SCREENING_RESULT_OPTIONS as readonly string[]).includes(value))
  return [...new Set(legacyValues), ...CC_FINAL_SCREENING_RESULT_OPTIONS]
})

const OTHER_FIELD_WATCH_PAIRS = [
  ["imagingMethod", "imagingMethodOther"],
  ["imagingResult", "imagingResultOther"],
  ["sputumCheckMethod", "sputumCheckMethodOther"],
  ["sputumCheckResult", "sputumCheckResultOther"],
  ["finalScreeningResult", "finalScreeningResultOther"]
] as const

async function fetchData() {
  loading.value = true
  try {
    const columnFiltersParam = toQueryParam()
    const listRes = await getScreeningCloseContactListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      district: searchForm.district || undefined,
      phone: searchForm.phone || undefined,
      creatorUsername: searchForm.creatorUsername || undefined,
      finalScreeningResult: searchForm.finalScreeningResult || undefined,
      ...extractDateRangeParams(searchForm.dateRange),
      ...extractCreateTimeRangeParams(searchForm.entryTimeRange),
      ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
    })
    tableData.value = listRes.data.records
    total.value = listRes.data.total
    try {
      const statsRes = await countByResultApi()
      resultStats.value = statsRes.data || {}
    } catch {
      resultStats.value = {}
    }
  } finally {
    loading.value = false
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
  searchForm.dateRange = []
  searchForm.entryTimeRange = []
  searchForm.finalScreeningResult = ""
  clearFilters()
  handleSearch()
}

const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

// 转诊
const tierCareVisible = ref(false)
const tierCareRow = ref<any>(null)
function openTierCare(row: any) {
  tierCareRow.value = row
  tierCareVisible.value = true
}

async function handleUpload(uploadFile: any) {
  try {
    const data = await runImportWithIdentityConfirm(uploadScreeningCloseContactApi, uploadFile.raw)
    if (!data) return
    importResult.value = data
    importResultVisible.value = true
    if (data.successCount > 0) fetchData()
  } catch (err: any) {
    ElMessage.error(err?.message || "上传失败")
  }
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function handleExport(ids?: number[]) {
  try {
    await ElMessageBox.confirm("确认导出当前选择的数据吗？", "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    const res = await exportScreeningCloseContactApi(ids)
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "密接人群筛查数据.xlsx"
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("导出失败")
  }
}

function handleExportSelected() {
  const ids = selectedRows.value.map((item: any) => item.id).filter(Boolean)
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport(ids)
}

/** 跳转到密接潜伏感染管理页 */
function goToLatent() {
  router.push("/close-contact/latent")
}

/** 编辑弹窗 */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<Record<string, any>>({})
const editFormRef = ref<FormInstance>()
const editMode = ref<"create" | "edit">("edit")

/** 编辑时兼容历史自由文本的接触场所 */
const contactPlaceSelectOptions = computed(() => {
  const current = editForm.value.contactPlace
  if (current && !(CONTACT_PLACE_OPTIONS as readonly string[]).includes(current)) {
    return [current, ...CONTACT_PLACE_OPTIONS]
  }
  return [...CONTACT_PLACE_OPTIONS]
})

const editContactRules: FormRules = {
  name: [{ required: true, message: "请输入接触者姓名", trigger: "blur" }],
  idNumber: [{ required: true, message: "请输入身份证号", trigger: "blur" }],
  phone: [{ required: true, message: "请输入联系电话", trigger: "blur" }],
  contactPlaceOther: [{
    validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
      if (editForm.value.contactPlace === CONTACT_PLACE_OTHER && !String(value || "").trim()) {
        callback(new Error("请填写接触场所具体内容"))
      } else {
        callback()
      }
    },
    trigger: "blur"
  }]
}

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
    phoneContactRelation: "",
    gender: "",
    ethnicity: "",
    contactType: "",
    contactPlace: "",
    contactPlaceOther: "",
    registrationDate: "",
    firstScreenDate: "",
    symptom1: "",
    symptom2: "",
    infectionCheckDate: "",
    infectionCheckMethod: "",
    infectionCheckResult: "",
    imagingDate: "",
    imagingMethod: "",
    imagingMethodOther: "",
    imagingResult: "",
    imagingResultOther: "",
    sputumCheckDate: "",
    sputumCheckMethod: "",
    sputumCheckMethodOther: "",
    sputumCheckResult: "",
    sputumCheckResultOther: "",
    finalScreeningResult: "",
    finalScreeningResultOther: "",
    followup6Result: "",
    followup12Result: "",
    followup24Result: "",
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
  nextTick(() => editFormRef.value?.clearValidate())
}

function handleEdit(row: any) {
  editMode.value = "edit"
  editForm.value = { ...row }
  editVisible.value = true
  nextTick(() => editFormRef.value?.clearValidate())
}

function validateContactBasicManual(): boolean {
  if (!editForm.value.name?.trim()) {
    ElMessage.warning("请输入接触者姓名")
    return false
  }
  if (!editForm.value.idNumber?.trim()) {
    ElMessage.warning("请输入身份证号")
    return false
  }
  if (!editForm.value.phone?.trim()) {
    ElMessage.warning("请输入联系电话")
    return false
  }
  if (editForm.value.contactPlace === CONTACT_PLACE_OTHER && !String(editForm.value.contactPlaceOther || "").trim()) {
    ElMessage.warning("接触场所选择「其他」时请填写具体内容")
    return false
  }
  return true
}

function validateFirstScreenManual(): boolean {
  const checks: { field: string, other: string, label: string }[] = [
    { field: "imagingMethod", other: "imagingMethodOther", label: "影像方法" },
    { field: "imagingResult", other: "imagingResultOther", label: "影像结果" },
    { field: "sputumCheckMethod", other: "sputumCheckMethodOther", label: "痰检方法" },
    { field: "sputumCheckResult", other: "sputumCheckResultOther", label: "痰检结果" },
    { field: "finalScreeningResult", other: "finalScreeningResultOther", label: "最终筛查结果" }
  ]
  for (const c of checks) {
    if (editForm.value[c.field] === SCREENING_FIELD_OTHER && !String(editForm.value[c.other] || "").trim()) {
      ElMessage.warning(`${c.label}选择「其他」时请填写具体内容`)
      return false
    }
  }
  return true
}

async function handleSave() {
  const form = editFormRef.value
  if (form) {
    try {
      await form.validate()
    } catch {
      ElMessage.warning("请完善接触者基本信息中的必填项")
      return
    }
  } else if (!validateContactBasicManual()) {
    return
  }
  if (!validateFirstScreenManual()) {
    return
  }
  const payload = sanitizeScreeningOtherFields(editForm.value)
  editSaving.value = true
  try {
    if (editMode.value === "create") {
      await createScreeningCloseContactApi(payload)
      ElMessage.success("新增成功")
    } else {
      await updateScreeningCloseContactApi(payload.id, payload)
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

watch(() => editForm.value.contactPlace, (place: string) => {
  if (place !== CONTACT_PLACE_OTHER) {
    editForm.value.contactPlaceOther = ""
  }
})

for (const [main, other] of OTHER_FIELD_WATCH_PAIRS) {
  watch(() => editForm.value[main], (val: string) => {
    if (val !== SCREENING_FIELD_OTHER) {
      editForm.value[other] = ""
    }
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」的筛查记录吗？删除后所有关联数据将一并删除，且不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    await deleteScreeningCloseContactApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除失败")
  }
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先勾选要删除的数据")
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 条筛查记录吗？删除后所有关联数据将一并删除，且不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    const ids = selectedRows.value.map((r: any) => r.id)
    await batchDeleteScreeningCloseContactApi(ids)
    ElMessage.success(`成功删除 ${ids.length} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("批量删除失败")
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailRow = ref<any>(null)
function viewDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

/** 判断随访月份是否有数据 */
function hasFollowupData(row: any, month: number): boolean {
  const key = `followup${month}Result`
  return !!row[key]
}

/** 获取随访结果的Tag类型 */
function getFollowupTag(result: string): string {
  if (!result) return "info"
  if (result.includes("活动性肺结核")) return "danger"
  if (isSuspectedTbDiagnosis(result)) return "warning"
  if (result.includes("潜伏感染者")) return "warning"
  if (result.includes("未发现异常")) return "success"
  return "info"
}

watch(() => [paginationData.currentPage, paginationData.pageSize], fetchData, { immediate: true })

// ==================== 3月复查 ====================
const threeMonthDialogVisible = ref(false)
const threeMonthRow = ref<any>(null)
const threeMonthSubmitting = ref(false)
const threeMonthForm = reactive({
  checkDate: "",
  checkResult: "",
  finalResult: "" as "阴性" | "阳性" | ""
})

function openThreeMonthDialog(row: any) {
  threeMonthRow.value = row
  threeMonthForm.checkDate = ""
  threeMonthForm.checkResult = ""
  threeMonthForm.finalResult = ""
  threeMonthDialogVisible.value = true
}

async function handleThreeMonthSubmit() {
  if (!threeMonthForm.checkDate || !threeMonthForm.checkResult || !threeMonthForm.finalResult) {
    ElMessage.warning("请填写完整的复查信息")
    return
  }
  if (threeMonthSubmitting.value) return
  threeMonthSubmitting.value = true
  try {
    await submitThreeMonthCheckApi(threeMonthRow.value.id, {
      checkDate: threeMonthForm.checkDate,
      checkResult: threeMonthForm.checkResult,
      finalResult: threeMonthForm.finalResult
    })
    ElMessage.success("3月复查结果已提交")
    threeMonthDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    threeMonthSubmitting.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="12" class="mb-4">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="opt in CC_FINAL_RESULT_STAT_OPTIONS" :key="opt.value">
        <el-card shadow="hover" class="stat-card" @click="searchForm.finalScreeningResult = opt.value; handleSearch()">
          <div class="flex items-center justify-between">
            <span class="text-sm text-gray-500">{{ opt.label }}</span>
            <el-tag :type="opt.type" size="small">
              {{ resultStats[opt.value] || 0 }} 人
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速导航 -->
    <el-card shadow="never" class="mb-4">
      <div class="flex items-center gap-3">
        <span class="text-sm text-gray-500 font-bold">快速跳转：</span>
        <el-button type="warning" size="small" @click="goToLatent">
          密接潜伏感染管理 →
        </el-button>
      </div>
    </el-card>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="接触者姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="接触者身份证号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model="searchForm.district" placeholder="请输入区县" clearable />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入联系电话" clearable />
        </el-form-item>
        <el-form-item label="录入用户">
          <el-input v-model="searchForm.creatorUsername" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="筛查时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="诊断结果">
          <el-select v-model="searchForm.finalScreeningResult" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in SCREENING_DIAGNOSIS_SEARCH_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">密接人群筛查数据</span>
          <div class="flex gap-2">
            <el-button v-permission="'closeContact:screening:create'" type="success" @click="handleCreate">
              新增数据
            </el-button>
            <el-button v-permission="'closeContact:screening:export'" @click="() => handleExport()">
              导出全部
            </el-button>
            <el-button v-permission="'closeContact:screening:export'" type="warning" :disabled="!selectedRows.length" @click="handleExportSelected">
              导出勾选
            </el-button>
            <el-button v-permission="'closeContact:screening:delete'" type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleUpload">
              <el-button type="primary" v-permission="'closeContact:screening:upload'">
                上传 Excel
              </el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <div class="table-scroll-wrap">
        <el-table v-loading="loading" :data="tableData" border stripe max-height="600" row-key="id" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="40" fixed />
          <el-table-column prop="name" min-width="110" show-overflow-tooltip>
            <template #header>
              <TableHeaderFilter
                label="接触者姓名"
                :model-value="columnFilters.name"
                @change="(v) => { setFilter('name', v); handleSearch() }"
              />
            </template>
          </el-table-column>
          <el-table-column prop="idNumber" min-width="180" show-overflow-tooltip>
            <template #header>
              <TableHeaderFilter
                label="接触者身份证号"
                :model-value="columnFilters.idNumber"
                @change="(v) => { setFilter('idNumber', v); handleSearch() }"
              />
            </template>
          </el-table-column>
          <el-table-column prop="age" label="年龄" width="70" />
          <el-table-column prop="phone" min-width="120" show-overflow-tooltip>
            <template #header>
              <TableHeaderFilter
                label="联系电话"
                :model-value="columnFilters.phone"
                @change="(v) => { setFilter('phone', v); handleSearch() }"
              />
            </template>
          </el-table-column>
          <el-table-column prop="city" label="市/州" min-width="100" show-overflow-tooltip />
          <el-table-column prop="district" min-width="100" show-overflow-tooltip>
            <template #header>
              <TableHeaderFilter
                label="区/县"
                :model-value="columnFilters.district"
                @change="(v) => { setFilter('district', v); handleSearch() }"
              />
            </template>
          </el-table-column>
          <el-table-column prop="contactType" label="接触类型" min-width="100" show-overflow-tooltip />
          <el-table-column label="接触场所" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{ formatContactPlace(row.contactPlace, row.contactPlaceOther) || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="sourcePatientName" label="原患者姓名" min-width="110" show-overflow-tooltip />
          <el-table-column prop="registrationDate" label="登记日期" min-width="110" />
          <el-table-column prop="infectionCheckMethod" label="感染检测方法" min-width="120" show-overflow-tooltip />
          <el-table-column prop="infectionCheckResult" label="感染检测结果" min-width="120" show-overflow-tooltip />
          <el-table-column prop="imagingDate" label="影像检查日期" min-width="120" />
          <el-table-column prop="imagingResult" label="影像结果" min-width="100" show-overflow-tooltip />
          <el-table-column prop="creatorUsername" min-width="100" show-overflow-tooltip>
            <template #header>
              <TableHeaderFilter
                label="录入用户"
                :model-value="columnFilters.creatorUsername"
                @change="(v) => { setFilter('creatorUsername', v); handleSearch() }"
              />
            </template>
          </el-table-column>
          <el-table-column label="6月随访" min-width="120">
            <template #default="{ row }">
              <el-tag v-if="hasFollowupData(row, 6)" :type="tagType(getFollowupTag(row.followup6Result))" size="small">
                {{ row.followup6Result }}
              </el-tag>
              <span v-else class="text-gray-400">—</span>
            </template>
          </el-table-column>
          <el-table-column label="12月随访" min-width="120">
            <template #default="{ row }">
              <el-tag v-if="hasFollowupData(row, 12)" :type="tagType(getFollowupTag(row.followup12Result))" size="small">
                {{ row.followup12Result }}
              </el-tag>
              <span v-else class="text-gray-400">—</span>
            </template>
          </el-table-column>
          <el-table-column label="24月随访" min-width="120">
            <template #default="{ row }">
              <el-tag v-if="hasFollowupData(row, 24)" :type="tagType(getFollowupTag(row.followup24Result))" size="small">
                {{ row.followup24Result }}
              </el-tag>
              <span v-else class="text-gray-400">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="finalScreeningResult" min-width="130">
            <template #header>
              <TableHeaderFilter
                label="最终筛查结果"
                type="select"
                :options="finalScreeningFilterOptions"
                :model-value="columnFilters.finalScreeningResult"
                @change="(v) => { setFilter('finalScreeningResult', v); handleSearch() }"
              />
            </template>
            <template #default="{ row }">
              <el-tag
                v-if="formatFinalScreeningDisplay(row.finalScreeningResult, row.finalScreeningResultOther)"
                :type="tagType(getFinalResultTag(row.finalScreeningResult, row.finalScreeningResultOther))"
                size="small"
              >
                {{ formatFinalScreeningDisplay(row.finalScreeningResult, row.finalScreeningResultOther) }}
              </el-tag>
              <span v-else class="text-gray-400">—</span>
            </template>
          </el-table-column>
          <el-table-column label="流程状态" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag v-if="CC_STATUS_MAP[row.ccStatus]" :type="tagType(CC_STATUS_MAP[row.ccStatus].type)" size="small">
                {{ CC_STATUS_MAP[row.ccStatus].label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">
                详情
              </el-button>
              <el-button v-permission="'closeContact:screening:edit'" type="warning" link size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button v-permission="'closeContact:screening:delete'" type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
              <el-button v-permission="'referral'" type="warning" link size="small" @click="openTierCare(row)">
                转诊
              </el-button>
              <el-button
                v-if="row.ccStatus === 6"
                type="success"
                link
                size="small"
                @click="openThreeMonthDialog(row)"
              >
                填写3月复查
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? '新增筛查记录' : '编辑筛查记录'" width="960px" :close-on-click-modal="false">
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
                <el-form-item label="原患者姓名">
                  <el-input v-model="editForm.sourcePatientName" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="原患者病案号">
                  <el-input v-model="editForm.sourcePatientCaseNo" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="原患者电话">
                  <el-input v-model="editForm.sourcePatientPhone" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="接触者基本信息">
          <el-form ref="editFormRef" :model="editForm" :rules="editContactRules" label-width="160px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="接触者姓名" prop="name" required>
                  <el-input v-model="editForm.name" placeholder="请输入" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="身份证号" prop="idNumber" required>
                  <el-input v-model="editForm.idNumber" placeholder="请输入" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="年龄">
                  <el-input-number v-model="editForm.age" :min="0" :max="150" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="联系电话" prop="phone" required>
                  <el-input v-model="editForm.phone" placeholder="请输入" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="联系电话与接触者关系">
                  <el-input v-model="editForm.phoneContactRelation" placeholder="如本人、父母等" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="性别">
                  <el-select v-model="editForm.gender" style="width:100%" clearable>
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
                  <el-select v-model="editForm.contactType" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in CONTACT_TYPE_OPTIONS" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="接触场所">
                  <el-select v-model="editForm.contactPlace" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in contactPlaceSelectOptions" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="editForm.contactPlace === CONTACT_PLACE_OTHER" :span="8">
                <el-form-item label="接触场所-其他" prop="contactPlaceOther" required>
                  <el-input v-model="editForm.contactPlaceOther" placeholder="请手工录入" />
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
        <el-tab-pane label="初次筛查">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="首次筛查日期">
                  <el-date-picker v-model="editForm.firstScreenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="结核症状1">
                  <el-select v-model="editForm.symptom1" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in CC_SYMPTOM1_OPTIONS" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="结核症状2">
                  <el-input v-model="editForm.symptom2" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测日期">
                  <el-date-picker v-model="editForm.infectionCheckDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测方法">
                  <el-select v-model="editForm.infectionCheckMethod" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in CC_INFECTION_CHECK_METHOD_OPTIONS" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="结果判定">
                  <el-input v-model="editForm.infectionCheckResult" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像检查日期">
                  <el-date-picker v-model="editForm.imagingDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像方法">
                  <el-select v-model="editForm.imagingMethod" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in imagingMethodSelectOptions" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="editForm.imagingMethod === SCREENING_FIELD_OTHER" :span="8">
                <el-form-item label="影像方法-其他" required>
                  <el-input v-model="editForm.imagingMethodOther" placeholder="请手工录入" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像结果">
                  <el-select v-model="editForm.imagingResult" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in imagingResultSelectOptions" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="editForm.imagingResult === SCREENING_FIELD_OTHER" :span="8">
                <el-form-item label="影像结果-其他" required>
                  <el-input v-model="editForm.imagingResultOther" placeholder="请手工录入" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="痰检日期">
                  <el-date-picker v-model="editForm.sputumCheckDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="痰检方法">
                  <el-select v-model="editForm.sputumCheckMethod" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in sputumMethodSelectOptions" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="editForm.sputumCheckMethod === SCREENING_FIELD_OTHER" :span="8">
                <el-form-item label="痰检方法-其他" required>
                  <el-input v-model="editForm.sputumCheckMethodOther" placeholder="请手工录入" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="痰检结果">
                  <el-select v-model="editForm.sputumCheckResult" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in sputumResultSelectOptions" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="editForm.sputumCheckResult === SCREENING_FIELD_OTHER" :span="8">
                <el-form-item label="痰检结果-其他" required>
                  <el-input v-model="editForm.sputumCheckResultOther" placeholder="请手工录入" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最终筛查结果">
                  <el-select v-model="editForm.finalScreeningResult" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in finalResultSelectOptions" :key="opt" :label="opt" :value="opt">
                      <span
                        :class="{
                          'text-red-600 font-medium': opt === '活动性肺结核',
                          'text-yellow-600 font-medium': opt === SUSPECTED_TB_DIAGNOSIS,
                        }"
                      >{{ opt }}</span>
                    </el-option>
                  </el-select>
                  <div v-if="editForm.finalScreeningResult === '活动性肺结核'" class="text-xs text-red-500 mt-1">
                    结案流程
                  </div>
                  <div v-else-if="isSuspectedTbDiagnosis(editForm.finalScreeningResult)" class="text-xs text-yellow-600 mt-1">
                    结案流程
                  </div>
                </el-form-item>
              </el-col>
              <el-col v-if="editForm.finalScreeningResult === SCREENING_FIELD_OTHER" :span="8">
                <el-form-item label="最终筛查结果-其他" required>
                  <el-input v-model="editForm.finalScreeningResultOther" placeholder="请手工录入" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="随访监测">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="6月随访结果">
                  <el-select v-model="editForm.followup6Result" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in followupResultSelectOptions" :key="opt" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="12月随访结果">
                  <el-select v-model="editForm.followup12Result" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in followupResultSelectOptions" :key="`12-${opt}`" :label="opt" :value="opt" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="24月随访结果">
                  <el-select v-model="editForm.followup24Result" placeholder="请选择" style="width:100%" clearable>
                    <el-option v-for="opt in followupResultSelectOptions" :key="`24-${opt}`" :label="opt" :value="opt" />
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name} — 密接筛查详情`" width="860px">
      <el-tabs v-if="detailRow">
        <el-tab-pane label="基本信息">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="接触者姓名">
              {{ detailRow.name }}
            </el-descriptions-item>
            <el-descriptions-item label="身份证号">
              {{ detailRow.idNumber }}
            </el-descriptions-item>
            <el-descriptions-item label="年龄">
              {{ detailRow.age }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ detailRow.phone }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话与接触者关系">
              {{ detailRow.phoneContactRelation || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="接触类型">
              {{ detailRow.contactType || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="接触场所">
              {{ formatContactPlace(detailRow.contactPlace, detailRow.contactPlaceOther) || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="原患者姓名">
              {{ detailRow.sourcePatientName }}
            </el-descriptions-item>
            <el-descriptions-item label="密接登记日期">
              {{ detailRow.registrationDate }}
            </el-descriptions-item>
            <el-descriptions-item label="最终筛查结果" :span="2">
              <el-tag :type="tagType(getFinalResultTag(detailRow.finalScreeningResult, detailRow.finalScreeningResultOther))">
                {{ formatFinalScreeningDisplay(detailRow.finalScreeningResult, detailRow.finalScreeningResultOther) || '—' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="流程状态">
              <el-tag v-if="CC_STATUS_MAP[detailRow.ccStatus]" :type="tagType(CC_STATUS_MAP[detailRow.ccStatus].type)">
                {{ CC_STATUS_MAP[detailRow.ccStatus].label }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="初次筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="首次筛查日期">
              {{ detailRow.firstScreenDate || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="结核症状1">
              {{ detailRow.symptom1 || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="感染检测方法">
              {{ detailRow.infectionCheckMethod || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="感染检测结果">
              {{ detailRow.infectionCheckResult || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="影像方法">
              {{ formatFieldWithOther(detailRow.imagingMethod, detailRow.imagingMethodOther) || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="影像结果">
              {{ formatFieldWithOther(detailRow.imagingResult, detailRow.imagingResultOther) || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="痰检方法">
              {{ formatFieldWithOther(detailRow.sputumCheckMethod, detailRow.sputumCheckMethodOther) || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="痰检结果">
              {{ formatFieldWithOther(detailRow.sputumCheckResult, detailRow.sputumCheckResultOther) || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="最终筛查结果" :span="2">
              <el-tag :type="tagType(getFinalResultTag(detailRow.finalScreeningResult, detailRow.finalScreeningResultOther))">
                {{ formatFinalScreeningDisplay(detailRow.finalScreeningResult, detailRow.finalScreeningResultOther) || '—' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="随访情况">
          <el-timeline>
            <!-- 3月复查（针对初次筛查阴性/未做的记录） -->
            <el-timeline-item
              :color="detailRow.threeMonthCheckDate ? '#67c23a' : '#909399'"
            >
              <template #dot>
                <el-icon v-if="detailRow.threeMonthCheckDate" color="#67c23a">
                  <CircleCheck />
                </el-icon>
                <el-icon v-else color="#909399">
                  <Clock />
                </el-icon>
              </template>
              <div class="mb-2">
                <span class="font-bold">3月复查</span>
              </div>
              <template v-if="detailRow.threeMonthCheckDate">
                <el-tag :type="detailRow.threeMonthFinalResult === '阴性' ? 'success' : 'danger'" size="small">
                  {{ detailRow.threeMonthFinalResult }}
                </el-tag>
                <span class="ml-2 text-sm text-gray-500">检测结果：{{ detailRow.threeMonthCheckResult }}</span>
                <span class="ml-2 text-sm text-gray-500">复查日期：{{ detailRow.threeMonthCheckDate }}</span>
              </template>
              <template v-else>
                <span class="text-gray-400 text-sm">尚未完成</span>
              </template>
            </el-timeline-item>
            <el-timeline-item
              v-for="month in [6, 12, 24]" :key="month"
              :color="hasFollowupData(detailRow, month) ? '#67c23a' : '#909399'"
            >
              <template #dot>
                <el-icon v-if="hasFollowupData(detailRow, month)" color="#67c23a">
                  <CircleCheck />
                </el-icon>
                <el-icon v-else color="#909399">
                  <Clock />
                </el-icon>
              </template>
              <div class="mb-2">
                <span class="font-bold">{{ month }}月随访</span>
                <span class="ml-3 text-gray-400">到期：{{ detailRow[`followup${month}DueDate`] || '—' }}</span>
              </div>
              <template v-if="hasFollowupData(detailRow, month)">
                <el-tag :type="tagType(getFollowupTag(detailRow[`followup${month}Result`]))" size="small">
                  {{ detailRow[`followup${month}Result`] }}
                </el-tag>
                <span class="ml-2 text-sm text-gray-500">实际筛查日期：{{ detailRow[`followup${month}ScreenDate`] }}</span>
              </template>
              <template v-else>
                <span class="text-gray-400 text-sm">尚未完成</span>
              </template>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
        <el-tab-pane label="预防治疗">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="是否开展预防治疗">
              {{ detailRow.hasPreventiveTreatment }}
            </el-descriptions-item>
            <el-descriptions-item label="预防性治疗方案">
              {{ detailRow.preventivePlan }}
            </el-descriptions-item>
            <el-descriptions-item label="是否完成治疗">
              {{ detailRow.treatmentCompleted }}
            </el-descriptions-item>
            <el-descriptions-item label="未完成原因">
              {{ detailRow.incompleteReason }}
            </el-descriptions-item>
            <el-descriptions-item label="预计完成时间">
              {{ detailRow.expectedTreatmentEndDate }}
            </el-descriptions-item>
            <el-descriptions-item label="备注">
              {{ detailRow.remark }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 3月复查弹窗 -->
    <el-dialog v-model="threeMonthDialogVisible" title="填写3月复查结果" width="480px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="复查日期" required>
          <el-date-picker
            v-model="threeMonthForm.checkDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择复查日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="感染检测结果" required>
          <el-input v-model="threeMonthForm.checkResult" placeholder="请输入感染检测结果（如 PPD阴性、IGRA阴性等）" />
        </el-form-item>
        <el-form-item label="最终判定结果" required>
          <el-radio-group v-model="threeMonthForm.finalResult">
            <el-radio value="阴性">
              阴性（结束流程）
            </el-radio>
            <el-radio value="阳性">
              阳性（转入潜伏感染流程）
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="threeMonthDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="threeMonthSubmitting" @click="handleThreeMonthSubmit">
          提交
        </el-button>
      </template>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="screening_close"
      population-type="close"
      module-type="screening"
      :subject-name="tierCareRow.name || ''"
    />

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert :title="`成功导入 ${importResult.successCount} 条数据`" type="success" :closable="false" class="mb-3" />
      <template v-if="importResult.errors.length > 0">
        <el-alert :title="`发现 ${importResult.errors.length} 条数据格式问题（已照常导入，请核查）`" type="warning" :closable="false" class="mb-3" />
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
.stat-card {
  cursor: pointer;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}

.table-scroll-wrap {
  width: 100%;
  overflow-x: auto;
}
</style>
