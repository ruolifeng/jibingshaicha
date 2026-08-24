<script setup lang="ts">
import type { TrackConfirmPayload } from "@@/components/TrackingOperationDialog.vue"
import type { EpidemicImportSkippedItem } from "../apis/index"
import ReferralDiagnosisDialog from "@@/components/ReferralDiagnosisDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import TrackingOperationDialog from "@@/components/TrackingOperationDialog.vue"
import { useColumnDistinct } from "@@/composables/useColumnDistinct"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { isConfirmedPatientDiagnosis, REFERRAL_CROWD_CATEGORY_OPTIONS, REFERRAL_TRACKING_DIAGNOSIS_OPTIONS } from "@@/constants/disease"
import { EPIDEMIC_TRACK_IMPORT_FIELDS } from "@@/constants/epidemic-track-import"
import { PAGE_SIZE_OPTIONS } from "@@/constants/pagination"
import {
  applyReferralChestXrayResult,
  isReferralChestXrayOther,
  normalizeReferralInfectionResult,
  normalizeReferralScreenMethod,
  REFERRAL_CHEST_XRAY_RESULT_OPTIONS,
  REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS,
  REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS,
  referralSelectOptionsWithLegacy,
  resolveReferralChestXrayResultForSave
} from "@@/constants/referral-tracking"
import { formatDateTime } from "@@/utils/datetime"
import { downloadBlob } from "@@/utils/download"
import { confirmDangerDelete } from "@@/utils/listToolbar"
import {
  formatArrivalDisplay,
  formatReferralDiagnosisDisplay,
  getRecommendTime,
  parseTrackingHistory,
  TRACK_STATUS_LABEL,
  TRACKING_STATUS_MAP
} from "@@/utils/referralTracking"
import { extractCreateTimeRangeParams, extractDateRangeParams } from "@@/utils/searchParams"
import { idCardRule, phoneRule } from "@@/utils/validate"
import { ElMessage, ElMessageBox } from "element-plus"
import { computed, nextTick, onMounted, reactive, ref } from "vue"
import { useUserStore } from "@/pinia/stores/user"
import {
  batchDeleteReferralTrackingApi,
  deleteAllReferralTrackingApi,
  deleteReferralTrackingApi,
  deleteReferralTrackingByFilterApi,
  enableJointTrackingApi,
  exportReferralTrackApi,
  getCountyLevel3UsersApi,
  getReferralTrackingColumnDistinctApi,
  getReferralTrackingDetailApi,
  getReferralTrackingListApi,
  importEpidemicTrackApi,
  previewEpidemicTrackImportApi,
  saveScreeningInfoApi,
  trackReferralApi,
  updateReferralTrackingApi
} from "../apis/index"
import { createReferralWithDuplicateConfirm, isReferralDuplicateCancel } from "../composables/useReferralDuplicateConfirm"

const userStore = useUserStore()

/** 推介已确认且开启共同追踪时，发起方与接收方均可操作 */
function isJointTrackingEnabled(row: any) {
  return Number(row?.jointTracking) === 1
}

function isPendingCrossTown(row: any) {
  return Number(row?.crossTownConfirmStatus) === 1
}

function isRejectedCrossTown(row: any) {
  return Number(row?.crossTownConfirmStatus) === 3
}

/** 有接收人时仅接收人可操作；共同追踪时发起方与接收方均可；无接收人时创建人或辖区一至五级用户可操作 */
function canOperateTrack(row: any) {
  if (isPendingCrossTown(row) || isRejectedCrossTown(row)) return false
  if (userStore.userRole === 1) return true
  const uid = String(userStore.userId)
  if (isFromRecommend(row) && row.receiverUserId) {
    if (isJointTrackingEnabled(row)) {
      return uid === String(row.receiverUserId) || uid === String(row.creatorId)
    }
    return uid === String(row.receiverUserId)
  }
  if (row.receiverUserId && Number(row?.crossTownConfirmStatus) === 2) {
    // 跨镇已确认：创建五级与接收三级均可
    return uid === String(row.receiverUserId) || uid === String(row.creatorId)
      || (userStore.userRole >= 2 && userStore.userRole <= 6)
  }
  if (row.receiverUserId && !isFromRecommend(row) && Number(row?.crossTownConfirmStatus) !== 2) {
    return uid === String(row.receiverUserId)
  }
  if (uid === String(row.creatorId)) return true
  // 追踪/大疫情：辖区一至五级用户对可见记录均可操作
  return userStore.userRole >= 2 && userStore.userRole <= 6
}

/** 接收方在推介确认后可开启共同追踪 */
function canEnableJointTracking(row: any) {
  if (row.archived || row.recommendStatus !== 2 || isJointTrackingEnabled(row)) return false
  return String(row.receiverUserId) === String(userStore.userId) || userStore.userRole === 1
}

// ===== 列表 =====
const loading = ref(false)
const exporting = ref(false)
const batchDeleting = ref(false)
const uploading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedRows = ref<any[]>([])
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()
const genderFilterOptions = [
  { text: "男", value: "男" },
  { text: "女", value: "女" }
]
const diagnosisFilterOptions = REFERRAL_TRACKING_DIAGNOSIS_OPTIONS.map(item => ({
  text: item.label,
  value: item.value
}))

const { load: loadDistinct, sourceValues: distinctValues } = useColumnDistinct(async (field) => {
  const { data } = await getReferralTrackingColumnDistinctApi(field, "track")
  return Array.isArray(data) ? data : []
})
const loadCreatorOptions = () => loadDistinct("creatorName")
const loadEntryUnitOptions = () => loadDistinct("entryUnit")

const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  township: "",
  creatorName: "",
  entryUnit: "",
  dateRange: [] as string[],
  createTimeRange: [] as string[],
  trackingStatus: undefined as number | undefined
})
const paginationData = reactive({ currentPage: 1, pageSize: 20 })

/** 列表/筛选导出/按筛删除共用查询参数（不含分页） */
function buildFilterParams() {
  const columnFiltersParam = toQueryParam()
  return {
    bizMode: "track",
    name: searchForm.name || undefined,
    idNumber: searchForm.idNumber || undefined,
    phone: searchForm.phone || undefined,
    township: searchForm.township || undefined,
    creatorName: searchForm.creatorName || undefined,
    entryUnit: searchForm.entryUnit || undefined,
    trackingStatus: searchForm.trackingStatus,
    ...extractDateRangeParams(searchForm.dateRange),
    ...extractCreateTimeRangeParams(searchForm.createTimeRange),
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
  }
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getReferralTrackingListApi({
      ...buildFilterParams(),
      page: paginationData.currentPage,
      size: paginationData.pageSize
    })
    tableData.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)

function handleSearch() {
  paginationData.currentPage = 1
  fetchList()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.phone = ""
  searchForm.township = ""
  searchForm.creatorName = ""
  searchForm.entryUnit = ""
  searchForm.dateRange = []
  searchForm.createTimeRange = []
  searchForm.trackingStatus = undefined
  clearFilters()
  handleSearch()
}

// ===== 大疫情导入 =====
const importDialogVisible = ref(false)
const importResult = ref<{ count: number, updated?: number, pendingConfirm?: number, batchNo: string } | null>(null)
const crossTownDialogVisible = ref(false)
const crossTownGroups = ref<{ township: string, items: EpidemicImportSkippedItem[], receiverUserId: string }[]>([])
const countyLevel3Users = ref<any[]>([])
let pendingImportFile: File | null = null
let pendingAddDuplicateRecords = false

function openImportDialog() {
  importResult.value = null
  importDialogVisible.value = true
}

function groupCrossTownItems(items: EpidemicImportSkippedItem[]) {
  const map = new Map<string, EpidemicImportSkippedItem[]>()
  for (const item of items) {
    if (item.reason && item.reason !== "cross_township") continue
    // 与后端 townshipReceivers 键一致：保留原始乡镇名（可为空串）
    const key = (item.township || "").trim()
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(item)
  }
  return Array.from(map.entries()).map(([township, groupItems]) => ({
    township,
    items: groupItems,
    receiverUserId: ""
  }))
}

function crossTownGroupLabel(township: string) {
  return township || "未知乡镇"
}

async function loadCountyLevel3Users() {
  try {
    const { data } = await getCountyLevel3UsersApi()
    countyLevel3Users.value = data || []
  } catch {
    countyLevel3Users.value = []
  }
}

async function handleEpidemicFileChange(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) return
  if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
    ElMessage.error("请上传 .xlsx 或 .xls 格式的大疫情表文件")
    return
  }
  uploading.value = true
  importResult.value = null
  try {
    const preview = await previewEpidemicTrackImportApi(file)
    const previewSkipped = preview.data?.skippedItems ?? []
    let townshipReceivers: Record<string, string> | undefined

    if (previewSkipped.length > 0) {
      await loadCountyLevel3Users()
      const groups = groupCrossTownItems(previewSkipped)
      if (groups.length > 0) {
        if (countyLevel3Users.value.length === 0) {
          ElMessage.warning("本区县暂无三级用户可选，跨镇人员无法发起确认导入")
        }
        // 先处理重复确认，再弹跨镇选人
        let addDuplicateRecords = false
        if ((preview.data?.duplicateCount ?? 0) > 0) {
          const ok = await confirmDuplicateChoice(preview.data!)
          if (ok === null) {
            ElMessage.info("已取消导入")
            return
          }
          addDuplicateRecords = ok
        }
        crossTownGroups.value = groups
        pendingImportFile = file
        pendingAddDuplicateRecords = addDuplicateRecords
        crossTownDialogVisible.value = true
        return
      }
    }

    let addDuplicateRecords = false
    if ((preview.data?.duplicateCount ?? 0) > 0) {
      const ok = await confirmDuplicateChoice(preview.data!)
      if (ok === null) {
        ElMessage.info("已取消导入")
        return
      }
      addDuplicateRecords = ok
    }
    await doEpidemicImport(file, addDuplicateRecords, townshipReceivers)
  } catch {
    ElMessage.error("导入失败，请确认文件格式是否符合大疫情表模板")
  } finally {
    uploading.value = false
  }
}

/** @returns true=新增重复，false=更新，null=取消 */
async function confirmDuplicateChoice(data: {
  duplicateCount: number
  duplicates: { name: string, idNumber: string, cardId?: string, township?: string }[]
}): Promise<boolean | null> {
  const duplicateNames = (data.duplicates ?? [])
    .slice(0, 5)
    .map((item) => {
      const loc = [item.township, item.cardId ? `原卡片${item.cardId}` : ""].filter(Boolean).join(" / ")
      return `${item.name}（${item.idNumber}${loc ? `，${loc}` : ""}）`
    })
    .join("、")
  const more = (data.duplicateCount ?? 0) > 5 ? ` 等共 ${data.duplicateCount} 人` : ""
  try {
    await ElMessageBox.confirm(
      `本单位已有同姓名+身份证追踪记录：${duplicateNames}${more}。\n默认将「更新已有记录」写入最新报告卡信息（满足 48 小时上报）；若需另建一条追踪请选「新增」。`,
      "重复患者确认",
      { confirmButtonText: "更新已有记录", cancelButtonText: "新增一条追踪", distinguishCancelAndClose: true, type: "warning" }
    )
    return false
  } catch (action) {
    if (action === "cancel") return true
    return null
  }
}

async function submitCrossTownImport() {
  const missing = crossTownGroups.value.filter(g => !g.receiverUserId)
  if (missing.length > 0) {
    ElMessage.warning(`请为乡镇「${missing.map(m => crossTownGroupLabel(m.township)).join("、")}」选择区县三级用户`)
    return
  }
  if (!pendingImportFile) return
  const townshipReceivers: Record<string, string> = {}
  for (const g of crossTownGroups.value) {
    townshipReceivers[g.township] = g.receiverUserId
  }
  uploading.value = true
  try {
    await doEpidemicImport(pendingImportFile, pendingAddDuplicateRecords, townshipReceivers)
    crossTownDialogVisible.value = false
    pendingImportFile = null
  } catch {
    ElMessage.error("导入失败，请确认文件格式是否符合大疫情表模板")
  } finally {
    uploading.value = false
  }
}

function onCrossTownDialogClosed() {
  pendingImportFile = null
  crossTownGroups.value = []
}

async function doEpidemicImport(
  file: File,
  addDuplicateRecords: boolean,
  townshipReceivers?: Record<string, string>
) {
  const res = await importEpidemicTrackApi(file, addDuplicateRecords, townshipReceivers)
  importResult.value = res.data
  const skippedItems = res.data.skippedItems ?? []
  const skipped = res.data.skipped ?? skippedItems.length
  const pending = res.data.pendingConfirm ?? 0
  const parts = [
    `新建 ${res.data.count} 条`,
    `更新 ${res.data.updated ?? 0} 条`
  ]
  if (pending > 0) parts.push(`跨镇待确认 ${pending} 条`)
  if (skipped > 0) parts.push(`未处理 ${skipped} 条`)
  ElMessage.success(`导入完成：${parts.join("，")}`)
  if (pending > 0) {
    ElMessage.info("跨镇记录已提交区县三级确认，确认前不可追踪操作")
  }
  if (skippedItems.length > 0) {
    const detail = skippedItems
      .slice(0, 5)
      .map(item => item.message || `${item.name}（${item.township || "未知乡镇"}）`)
      .join("\n")
    const more = skippedItems.length > 5 ? `\n... 等共 ${skippedItems.length} 条` : ""
    ElMessageBox.alert(`${detail}${more}`, "部分跨镇未导入说明", {
      confirmButtonText: "知道了",
      type: "warning"
    })
  }
  importDialogVisible.value = false
  fetchList()
}

/** 导出：filtered=筛选 / selected=勾选 / all=全部 */
async function handleExport(mode: "filtered" | "selected" | "all" = "filtered", ids?: string[]) {
  const isSelected = mode === "selected"
  const label = isSelected
    ? `选中的 ${ids!.length} 条`
    : mode === "all"
      ? "全部"
      : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}追踪数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportReferralTrackApi(
      isSelected
        ? { bizMode: "track", ids }
        : mode === "all"
          ? { bizMode: "track" }
          : buildFilterParams()
    )
    downloadBlob(blob as unknown as Blob, "追踪记录导出.xlsx")
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}

function handleExportSelected() {
  const ids = selectedRows.value.map((item: any) => item.id).filter(Boolean)
  if (ids.length === 0) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport("selected", ids)
}

async function handleDeleteFiltered() {
  const ok = await confirmDangerDelete({
    title: "删除筛选结果",
    message: "确定删除当前筛选条件下的全部追踪记录吗？此操作不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteReferralTrackingByFilterApi(buildFilterParams())
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    selectedRows.value = []
    fetchList()
  } catch {
    ElMessage.error("删除筛选结果失败")
  } finally {
    batchDeleting.value = false
  }
}

async function handleDeleteAll() {
  const ok = await confirmDangerDelete({
    title: "删除全部",
    message: "确定删除权限范围内的全部追踪记录吗？此操作不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteAllReferralTrackingApi("track")
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    selectedRows.value = []
    handleReset()
  } catch {
    ElMessage.error("删除全部失败")
  } finally {
    batchDeleting.value = false
  }
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先勾选要删除的数据")
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 条追踪记录吗？此操作不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    const ids = selectedRows.value.map((r: any) => r.id)
    batchDeleting.value = true
    const { data } = await batchDeleteReferralTrackingApi(ids)
    ElMessage.success(`成功删除 ${data ?? ids.length} 条记录`)
    selectedRows.value = []
    fetchList()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("批量删除失败")
  } finally {
    batchDeleting.value = false
  }
}

// ===== 编辑 =====
const editDialogVisible = ref(false)
const editRow = ref<any>(null)
const editTrackingHistory = ref<{ attempt: number, status: number, trackTime: string, reason: string }[]>([])
const editForm = reactive({
  name: "",
  gender: "",
  birthDate: "",
  age: undefined as number | undefined,
  idNumber: "",
  phone: "",
  currentAddress: "",
  crowdCategory: "",
  trackReason: "",
  cardId: "",
  parentName: "",
  workplace: "",
  township: "",
  caseCategory: "",
  diseaseName: "",
  reportUnit: "",
  reportCardTime: "",
  epidemicRemark: "",
  diagnosisResult: "",
  diagnosisRemark: ""
})

const canEditDiagnosis = computed(() => Boolean(editRow.value?.diagnosisResult))
const canEditTrackingHistory = computed(() => editTrackingHistory.value.length > 0)

/** 编辑诊断：兼容历史「确诊患者 / 其他」 */
const editDiagnosisOptions = computed(() => {
  const current = editForm.diagnosisResult
  const opts: Array<{ label: string, value: string }> = REFERRAL_TRACKING_DIAGNOSIS_OPTIONS.map(item => ({ ...item }))
  if (current && !opts.some(item => item.value === current)) {
    opts.push({ label: current, value: current })
  }
  return opts
})

async function openEditDialog(row: any) {
  editRow.value = row
  Object.assign(editForm, {
    name: row.name ?? "",
    gender: row.gender ?? "",
    birthDate: row.birthDate ?? "",
    age: row.age,
    idNumber: row.idNumber ?? "",
    phone: row.phone ?? "",
    currentAddress: row.currentAddress ?? "",
    crowdCategory: row.crowdCategory ?? "",
    trackReason: row.trackReason ?? "",
    cardId: row.cardId ?? "",
    parentName: row.parentName ?? "",
    workplace: row.workplace ?? "",
    township: row.township ?? "",
    caseCategory: row.caseCategory ?? "",
    diseaseName: row.diseaseName ?? "",
    reportUnit: row.reportUnit ?? "",
    reportCardTime: row.reportCardTime ?? "",
    epidemicRemark: row.epidemicRemark ?? "",
    diagnosisResult: row.diagnosisResult ?? "",
    diagnosisRemark: row.diagnosisRemark ?? ""
  })
  editTrackingHistory.value = parseTrackingHistory(row.trackingHistoryJson).map(item => ({
    attempt: item.attempt,
    status: item.status,
    trackTime: item.trackTime,
    reason: item.reason ?? ""
  }))
  // 列表可能缺诊断备注/完整追踪过程，打开时拉详情补齐
  editDialogVisible.value = true
  try {
    const res = await getReferralTrackingDetailApi(row.id)
    const detail = res.data
    if (detail) {
      editRow.value = { ...row, ...detail }
      editForm.diagnosisResult = detail.diagnosisResult ?? editForm.diagnosisResult
      editForm.diagnosisRemark = detail.diagnosisRemark ?? editForm.diagnosisRemark
      editTrackingHistory.value = parseTrackingHistory(detail.trackingHistoryJson).map(item => ({
        attempt: item.attempt,
        status: item.status,
        trackTime: item.trackTime,
        reason: item.reason ?? ""
      }))
    }
  } catch {
    /* 详情失败时仍可用列表数据编辑基本信息 */
  }
}

async function handleEditSave() {
  if (canEditDiagnosis.value) {
    if (!editForm.diagnosisResult) {
      ElMessage.warning("请选择诊断结果")
      return
    }
    if (editForm.diagnosisResult === "其他" && !editForm.diagnosisRemark.trim()) {
      ElMessage.warning("选择其他时请填写诊断备注")
      return
    }
  }
  if (canEditTrackingHistory.value) {
    const emptyRemark = editTrackingHistory.value.find(item => !item.reason.trim())
    if (emptyRemark) {
      ElMessage.warning(`请填写第${emptyRemark.attempt}次追踪备注`)
      return
    }
  }

  const payload: Record<string, any> = { ...editForm }
  if (canEditDiagnosis.value) {
    payload.diagnosisResult = editForm.diagnosisResult
    payload.diagnosisRemark = editForm.diagnosisResult === "其他"
      ? editForm.diagnosisRemark.trim()
      : ""
  } else {
    delete payload.diagnosisResult
    delete payload.diagnosisRemark
  }
  if (canEditTrackingHistory.value) {
    payload.trackingHistory = editTrackingHistory.value.map(item => ({
      attempt: item.attempt,
      status: item.status,
      reason: item.reason.trim()
    }))
  }

  await updateReferralTrackingApi(editRow.value.id, payload)
  ElMessage.success("保存成功")
  editDialogVisible.value = false
  fetchList()
}

function isEpidemicRow(row: any) {
  return row?.sourceType === "epidemic"
}

/** 由推介确认后转入的追踪记录 */
function isFromRecommend(row: any) {
  return Boolean(row?.recommendConfirmTime || row?.recommendSentTime || row?.recommendStatus === 2)
}

const RECOMMEND_STATUS_MAP: Record<number, { label: string, type: string }> = {
  0: { label: "未发送", type: "info" },
  1: { label: "已发送", type: "warning" },
  2: { label: "已接受", type: "success" },
  3: { label: "已拒绝", type: "danger" }
}

// ===== 查看详情 =====
const viewDialogVisible = ref(false)
const viewLoading = ref(false)
const viewDetail = ref<any>(null)
const viewTrackingHistory = computed(() =>
  parseTrackingHistory(viewDetail.value?.trackingHistoryJson)
)

async function openViewDialog(row: any) {
  viewDialogVisible.value = true
  viewLoading.value = true
  viewDetail.value = null
  try {
    const res = await getReferralTrackingDetailApi(row.id)
    viewDetail.value = res.data
  } catch {
    ElMessage.error("加载详情失败")
    viewDialogVisible.value = false
  } finally {
    viewLoading.value = false
  }
}

// ===== 新增追踪 =====
const createDialogVisible = ref(false)
const createForm = reactive({
  name: "",
  gender: "",
  birthDate: "",
  age: undefined as number | undefined,
  idType: "居民身份证",
  idNumber: "",
  ethnicity: "",
  phone: "",
  householdAddress: "",
  currentAddress: "",
  crowdCategory: "",
  screenMethod: "",
  infectionResult: "",
  chestXrayDate: "",
  chestXrayResult: "",
  chestXrayRemark: "",
  diagnosisResult: "",
  trackReason: ""
})
const createFormRef = ref()

const createFormRules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(false)],
  phone: [phoneRule(true)],
  currentAddress: [{ required: true, message: "请填写现住址", trigger: "blur" }],
  crowdCategory: [{ required: true, message: "请选择人群分类", trigger: "change" }],
  trackReason: [{ required: true, message: "请填写追踪原因", trigger: "blur" }]
}

function openCreateDialog() {
  Object.assign(createForm, {
    name: "",
    gender: "",
    birthDate: "",
    age: undefined,
    idType: "居民身份证",
    idNumber: "",
    ethnicity: "",
    phone: "",
    householdAddress: "",
    currentAddress: "",
    crowdCategory: "",
    screenMethod: "",
    infectionResult: "",
    chestXrayDate: "",
    chestXrayResult: "",
    chestXrayRemark: "",
    diagnosisResult: "",
    trackReason: ""
  })
  createDialogVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

async function handleCreate() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  if (isReferralChestXrayOther(createForm.chestXrayResult) && !createForm.chestXrayRemark.trim()) {
    ElMessage.warning("请填写胸片检查结果备注")
    return
  }
  try {
    const { chestXrayRemark, ...rest } = createForm
    await createReferralWithDuplicateConfirm({
      ...rest,
      chestXrayResult: resolveReferralChestXrayResultForSave(createForm.chestXrayResult, chestXrayRemark),
      bizMode: "track"
    })
    ElMessage.success("追踪记录创建成功")
    createDialogVisible.value = false
    fetchList()
  } catch (err) {
    if (!isReferralDuplicateCancel(err)) {
      ElMessage.error("创建失败")
    }
  }
}

// ===== 追踪操作 =====
const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const trackSubmitting = ref(false)

function openTrackDialog(row: any) {
  trackRow.value = row
  trackDialogVisible.value = true
}

async function handleTrack(payload: TrackConfirmPayload) {
  if (trackSubmitting.value) return
  trackSubmitting.value = true
  try {
    const willForceEnd = payload.status === 2 && (trackRow.value?.notInPlaceCount ?? 0) >= 2
    await trackReferralApi(trackRow.value.id, payload.status, payload.remark, payload.actualArrivalDate)
    if (willForceEnd) {
      ElMessage.warning("已记录第 3 次未到位，追踪已强制结束")
    } else if (payload.status === 1) {
      ElMessage.success("已确认到位")
    } else {
      ElMessage.success("追踪记录已保存")
    }
    trackDialogVisible.value = false
    fetchList()
  } finally {
    trackSubmitting.value = false
  }
}

async function handleEnableJointTracking(row: any) {
  await ElMessageBox.confirm(
    `确认对「${row.name}」开启共同追踪吗？开启后您与推介发起方均可进行追踪，双方操作次数合并计算。`,
    "共同追踪确认",
    { type: "warning", confirmButtonText: "确认开启", cancelButtonText: "取消" }
  )
  await enableJointTrackingApi(row.id)
  ElMessage.success("已开启共同追踪，推介发起方也可参与追踪")
  fetchList()
}

// ===== 筛查信息 =====
const screeningDialogVisible = ref(false)
const screeningRow = ref<any>(null)
const screeningForm = reactive({
  hasInfectionScreen: "",
  screenDate: "",
  screenMethod: "",
  infectionResult: "",
  hasChestXray: "",
  chestXrayDate: "",
  chestXrayResult: "",
  chestXrayRemark: ""
})

const chestXrayResultSelectOptions = computed(() =>
  referralSelectOptionsWithLegacy(REFERRAL_CHEST_XRAY_RESULT_OPTIONS, screeningForm.chestXrayResult))

function openScreeningDialog(row: any) {
  screeningRow.value = row
  Object.assign(screeningForm, {
    hasInfectionScreen: row.hasInfectionScreen ?? "",
    screenDate: row.screenDate ?? "",
    screenMethod: normalizeReferralScreenMethod(row.screenMethod),
    infectionResult: normalizeReferralInfectionResult(row.infectionResult),
    hasChestXray: row.hasChestXray ?? "",
    chestXrayDate: row.chestXrayDate ?? "",
    chestXrayResult: "",
    chestXrayRemark: ""
  })
  applyReferralChestXrayResult(screeningForm, row.chestXrayResult)
  screeningDialogVisible.value = true
}

async function handleSaveScreening() {
  if (isReferralChestXrayOther(screeningForm.chestXrayResult) && !screeningForm.chestXrayRemark.trim()) {
    ElMessage.warning("请填写胸片检查结果备注")
    return
  }
  const { chestXrayRemark, ...rest } = screeningForm
  await saveScreeningInfoApi(screeningRow.value.id, {
    ...rest,
    chestXrayResult: resolveReferralChestXrayResultForSave(screeningForm.chestXrayResult, chestXrayRemark)
  })
  ElMessage.success("筛查信息已保存")
  screeningDialogVisible.value = false
  fetchList()
}

// ===== 诊断 =====
const diagnosisDialogVisible = ref(false)
const diagnosisRow = ref<any>(null)

function openDiagnosisDialog(row: any) {
  diagnosisRow.value = row
  diagnosisDialogVisible.value = true
}

// ===== 删除 =====
async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.name}」的追踪记录？`, "删除确认", { type: "warning" })
  await deleteReferralTrackingApi(row.id)
  ElMessage.success("删除成功")
  fetchList()
}

// ===== 状态标签辅助 =====
function getRowClass({ row }: { row: any }) {
  if (row.archived && isConfirmedPatientDiagnosis(row)) return "confirmed-row"
  return ""
}
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card class="search-wrapper" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入联系电话" clearable />
        </el-form-item>
        <el-form-item label="乡镇">
          <el-input v-model="searchForm.township" placeholder="请输入乡镇" clearable />
        </el-form-item>
        <el-form-item label="录入者">
          <el-select
            v-model="searchForm.creatorName"
            filterable
            clearable
            placeholder="请选择录入者"
            style="width: 160px"
            @visible-change="(v: boolean) => v && loadCreatorOptions()"
          >
            <el-option v-for="item in distinctValues('creatorName').value" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="录入单位">
          <el-select
            v-model="searchForm.entryUnit"
            filterable
            clearable
            placeholder="请选择录入单位"
            style="width: 180px"
            @visible-change="(v: boolean) => v && loadEntryUnitOptions()"
          >
            <el-option v-for="item in distinctValues('entryUnit').value" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告卡录入时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="录入时间">
          <el-date-picker
            v-model="searchForm.createTimeRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="追踪状态">
          <el-select v-model="searchForm.trackingStatus" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="(item, val) in TRACKING_STATUS_MAP"
              :key="val"
              :label="item.label"
              :value="Number(val)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="toolbar-wrapper" style="margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap">
        <el-button v-permission="'referralManagement:create'" type="primary" @click="openCreateDialog">
          新增追踪
        </el-button>
        <el-button v-permission="'referralManagement:export'" :loading="exporting" @click="() => handleExport('filtered')">
          导出筛选结果
        </el-button>
        <el-button
          v-permission="'referralManagement:delete'"
          type="danger"
          plain
          :loading="batchDeleting"
          @click="handleDeleteFiltered"
        >
          删除筛选结果
        </el-button>
        <el-button
          v-permission="'referralManagement:export'"
          type="warning"
          :loading="exporting"
          :disabled="selectedRows.length === 0"
          @click="handleExportSelected"
        >
          导出勾选
        </el-button>
        <el-button
          v-permission="'referralManagement:delete'"
          type="danger"
          :loading="batchDeleting"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          删除勾选
        </el-button>
        <el-button v-permission="'referralManagement:epidemicImport'" type="success" @click="openImportDialog">
          大疫情导入
        </el-button>
        <el-button v-permission="'referralManagement:export'" :loading="exporting" @click="() => handleExport('all')">
          导出全部
        </el-button>
        <el-button
          v-permission="'referralManagement:delete'"
          type="danger"
          plain
          :loading="batchDeleting"
          @click="handleDeleteAll"
        >
          删除全部
        </el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        row-key="id"
        :row-class-name="getRowClass"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" fixed />
        <el-table-column label="来源" width="130">
          <template #default="{ row }">
            <el-tag :type="isEpidemicRow(row) ? 'danger' : 'info'" size="small">
              {{ isEpidemicRow(row) ? "大疫情" : "手动" }}
            </el-tag>
            <el-tag v-if="isPendingCrossTown(row)" type="warning" size="small" style="margin-left: 4px">
              待区县三级确认
            </el-tag>
            <el-tag v-else-if="isRejectedCrossTown(row)" type="info" size="small" style="margin-left: 4px">
              跨镇已拒绝
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cardId" label="卡片ID" width="120" show-overflow-tooltip />
        <el-table-column prop="name" min-width="100">
          <template #header>
            <TableHeaderFilter
              label="患者姓名"
              :model-value="columnFilters.name"
              @change="(v) => { setFilter('name', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="parentName" label="患儿家长姓名" show-overflow-tooltip />
        <el-table-column prop="gender" min-width="80">
          <template #header>
            <TableHeaderFilter
              label="性别"
              type="select"
              :options="genderFilterOptions"
              :model-value="columnFilters.gender"
              @change="(v) => { setFilter('gender', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column label="出生日期" width="110">
          <template #default="{ row }">
            {{ row.birthDate || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="idNumber" min-width="160" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="有效证件号"
              :model-value="columnFilters.idNumber"
              @change="(v) => { setFilter('idNumber', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="workplace" label="工作单位" show-overflow-tooltip />
        <el-table-column prop="phone" min-width="120">
          <template #header>
            <TableHeaderFilter
              label="联系电话"
              :model-value="columnFilters.phone"
              @change="(v) => { setFilter('phone', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="township" min-width="100" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="乡镇"
              :model-value="columnFilters.township"
              @change="(v) => { setFilter('township', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="currentAddress" min-width="160" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="现住详细地址"
              :model-value="columnFilters.currentAddress"
              @change="(v) => { setFilter('currentAddress', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="creatorUserName" min-width="100" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="录入用户"
              :model-value="columnFilters.creatorUserName"
              @change="(v) => { setFilter('creatorUserName', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="crowdCategory" label="人群分类" show-overflow-tooltip />
        <el-table-column prop="caseCategory" label="病例分类" show-overflow-tooltip />
        <el-table-column prop="diseaseName" label="疾病名称" show-overflow-tooltip />
        <el-table-column prop="reportUnit" label="报告单位" show-overflow-tooltip />
        <el-table-column label="报告卡录入时间" min-width="160">
          <template #default="{ row }">
            {{ row.reportCardTime ? formatDateTime(row.reportCardTime) : "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="epidemicRemark" label="备注" show-overflow-tooltip />
        <el-table-column prop="trackReason" label="追踪原因" show-overflow-tooltip />
        <el-table-column label="共同追踪" width="90">
          <template #default="{ row }">
            <el-tag v-if="isFromRecommend(row)" :type="row.jointTracking === 1 ? 'success' : 'info'" size="small">
              {{ row.jointTracking === 1 ? "已开启" : "未开启" }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="追踪状态">
          <template #default="{ row }">
            <el-tag
              :type="TRACKING_STATUS_MAP[row.trackingStatus]?.type as any"
              size="small"
            >
              {{ TRACKING_STATUS_MAP[row.trackingStatus]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="追踪次数">
          <template #default="{ row }">
            {{ row.notInPlaceCount > 0 ? `${row.notInPlaceCount}次未到位` : "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisResult" min-width="160" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="诊断结果"
              type="select"
              :options="diagnosisFilterOptions"
              :model-value="columnFilters.diagnosisResult"
              @change="(v) => { setFilter('diagnosisResult', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            <el-tag
              v-if="row.diagnosisResult"
              :type="row.archived && isConfirmedPatientDiagnosis(row) ? 'danger' : 'info'"
              size="small"
            >
              {{ formatReferralDiagnosisDisplay(row) }}{{ row.archived && isConfirmedPatientDiagnosis(row) ? "（结案）" : "" }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="录入时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="到位时间" min-width="120">
          <template #default="{ row }">
            {{ formatArrivalDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column label="追踪过程" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="parseTrackingHistory(row.trackingHistoryJson).length">
              <span
                v-for="(item, idx) in parseTrackingHistory(row.trackingHistoryJson)"
                :key="item.attempt"
              >
                <template v-if="idx">；</template>
                第{{ item.attempt }}次 {{ formatDateTime(item.trackTime) }}
                {{ TRACK_STATUS_LABEL[item.status] }}
                <template v-if="item.reason">（{{ item.reason }}）</template>
              </span>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openViewDialog(row)">
              查看
            </el-button>
            <el-tooltip
              v-if="isPendingCrossTown(row)"
              content="待区县三级确认，确认前不可追踪操作"
              placement="top"
            >
              <span style="color: #909399; font-size: 12px; margin-left: 4px">待确认</span>
            </el-tooltip>
            <el-button
              v-if="canOperateTrack(row)"
              v-permission="'referralManagement:edit'"
              type="primary" link size="small"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <!-- 接收方开启共同追踪 -->
            <el-button
              v-if="canEnableJointTracking(row) && isFromRecommend(row)"
              v-permission="'referralManagement:confirm'"
              type="success" link size="small"
              @click="handleEnableJointTracking(row)"
            >
              共同追踪
            </el-button>
            <!-- 追踪：待追踪或未到位 -->
            <el-button
              v-if="canOperateTrack(row) && [0, 2].includes(row.trackingStatus) && !row.archived"
              v-permission="'referralManagement:trackOperate'"
              type="warning" link size="small"
              @click="openTrackDialog(row)"
            >
              追踪
            </el-button>
            <!-- 筛查信息：已到位 -->
            <el-button
              v-if="canOperateTrack(row) && row.trackingStatus === 1 && !row.diagnosisResult"
              v-permission="'referralManagement:xray'"
              type="primary" link size="small"
              @click="openScreeningDialog(row)"
            >
              录入感染检测结果及胸片结果
            </el-button>
            <!-- 诊断：已到位 -->
            <el-button
              v-if="canOperateTrack(row) && row.trackingStatus === 1 && !row.diagnosisResult"
              v-permission="'referralManagement:diagnosis'"
              type="success" link size="small"
              @click="openDiagnosisDialog(row)"
            >
              录入诊断
            </el-button>
            <!-- 删除 -->
            <el-button
              v-if="canOperateTrack(row)"
              v-permission="'referralManagement:delete'"
              type="danger" link size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 14px; justify-content: flex-end"
        layout="total, sizes, prev, pager, next"
        :total="total || 0"
        :page-size="paginationData.pageSize || 20"
        :current-page="paginationData.currentPage || 1"
        :page-sizes="[...PAGE_SIZE_OPTIONS]"
        @size-change="(val: number) => { paginationData.pageSize = val; fetchList() }"
        @current-change="(val: number) => { paginationData.currentPage = val; fetchList() }"
      />
    </el-card>

    <!-- 查看追踪详情 -->
    <el-dialog v-model="viewDialogVisible" title="追踪详情" width="800px">
      <div v-loading="viewLoading">
        <template v-if="viewDetail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="数据来源">
              <el-tag :type="isEpidemicRow(viewDetail) ? 'danger' : 'info'" size="small">
                {{ isEpidemicRow(viewDetail) ? "大疫情导入" : "手动录入" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="录入者">
              {{ viewDetail.creatorUserName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="录入单位" :span="2">
              {{ viewDetail.entryUnitName || "-" }}
            </el-descriptions-item>
            <template v-if="isPendingCrossTown(viewDetail) || Number(viewDetail.crossTownConfirmStatus) === 2 || isRejectedCrossTown(viewDetail)">
              <el-descriptions-item label="跨镇确认">
                <el-tag v-if="isPendingCrossTown(viewDetail)" type="warning" size="small">
                  待区县三级确认
                </el-tag>
                <el-tag v-else-if="Number(viewDetail.crossTownConfirmStatus) === 2" type="success" size="small">
                  已确认
                </el-tag>
                <el-tag v-else type="info" size="small">
                  已拒绝
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="确认接收人">
                {{ viewDetail.receiverUserName || "-" }}
              </el-descriptions-item>
              <el-descriptions-item v-if="viewDetail.crossTownConfirmTime" label="确认时间">
                {{ formatDateTime(viewDetail.crossTownConfirmTime) }}
              </el-descriptions-item>
            </template>
            <template v-if="isFromRecommend(viewDetail)">
              <el-descriptions-item label="推介来源" :span="2">
                <el-tag type="success" size="small">
                  推介确认转入
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="推介接收人">
                {{ viewDetail.receiverUserName || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="推介状态">
                <el-tag
                  v-if="viewDetail.recommendStatus !== null && viewDetail.recommendStatus !== undefined"
                  :type="RECOMMEND_STATUS_MAP[viewDetail.recommendStatus]?.type as any"
                  size="small"
                >
                  {{ RECOMMEND_STATUS_MAP[viewDetail.recommendStatus]?.label }}
                </el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="推介时间">
                {{ getRecommendTime(viewDetail) ? formatDateTime(getRecommendTime(viewDetail)!) : "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="推介确认时间">
                {{ viewDetail.recommendConfirmTime ? formatDateTime(viewDetail.recommendConfirmTime) : "-" }}
              </el-descriptions-item>
              <el-descriptions-item v-if="viewDetail.recommendReason" label="原推介原因" :span="2">
                {{ viewDetail.recommendReason }}
              </el-descriptions-item>
              <el-descriptions-item label="共同追踪">
                <el-tag :type="viewDetail.jointTracking === 1 ? 'success' : 'info'" size="small">
                  {{ viewDetail.jointTracking === 1 ? "已开启" : "未开启" }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="viewDetail.jointTrackingTime" label="共同追踪时间">
                {{ formatDateTime(viewDetail.jointTrackingTime) }}
              </el-descriptions-item>
            </template>
            <template v-if="isEpidemicRow(viewDetail)">
              <el-descriptions-item label="卡片ID">
                {{ viewDetail.cardId || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="患儿家长姓名">
                {{ viewDetail.parentName || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="工作单位">
                {{ viewDetail.workplace || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="乡镇">
                {{ viewDetail.township || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="病例分类">
                {{ viewDetail.caseCategory || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="疾病名称">
                {{ viewDetail.diseaseName || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="报告单位">
                {{ viewDetail.reportUnit || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="报告卡录入时间">
                {{ viewDetail.reportCardTime ? formatDateTime(viewDetail.reportCardTime) : "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">
                {{ viewDetail.epidemicRemark || "-" }}
              </el-descriptions-item>
            </template>
            <el-descriptions-item label="患者姓名">
              {{ viewDetail.name || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="性别">
              {{ viewDetail.gender || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="出生日期">
              {{ viewDetail.birthDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="年龄">
              {{ viewDetail.age ?? "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="证件类型">
              {{ viewDetail.idType || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="有效证件号">
              {{ viewDetail.idNumber || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="民族">
              {{ viewDetail.ethnicity || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ viewDetail.phone || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="户籍地址" :span="2">
              {{ viewDetail.householdAddress || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="现住详细地址" :span="2">
              {{ viewDetail.currentAddress || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="人群分类">
              {{ viewDetail.crowdCategory || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="追踪原因">
              {{ viewDetail.trackReason || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="追踪状态">
              <el-tag :type="TRACKING_STATUS_MAP[viewDetail.trackingStatus]?.type as any" size="small">
                {{ TRACKING_STATUS_MAP[viewDetail.trackingStatus]?.label }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="未到位次数">
              {{ viewDetail.notInPlaceCount > 0 ? `${viewDetail.notInPlaceCount}次` : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="录入时间">
              {{ viewDetail.createTime ? formatDateTime(viewDetail.createTime) : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="到位时间">
              {{ formatArrivalDisplay(viewDetail) }}
            </el-descriptions-item>
            <el-descriptions-item label="诊断结果">
              <el-tag
                v-if="viewDetail.diagnosisResult"
                :type="viewDetail.archived && isConfirmedPatientDiagnosis(viewDetail) ? 'danger' : 'info'"
                size="small"
              >
                {{ formatReferralDiagnosisDisplay(viewDetail) }}{{ viewDetail.archived && isConfirmedPatientDiagnosis(viewDetail) ? "（结案）" : "" }}
              </el-tag>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="viewDetail.diagnosisRemark" label="诊断备注" :span="2">
              {{ viewDetail.diagnosisRemark }}
            </el-descriptions-item>
            <el-descriptions-item label="诊断时间">
              {{ viewDetail.diagnosisTime ? formatDateTime(viewDetail.diagnosisTime) : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="是否感染检测">
              {{ viewDetail.hasInfectionScreen || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="感染检测日期">
              {{ viewDetail.screenDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="感染检测方法">
              {{ normalizeReferralScreenMethod(viewDetail.screenMethod) || viewDetail.screenMethod || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="感染检测结果">
              {{ viewDetail.infectionResult || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="是否胸片检查">
              {{ viewDetail.hasChestXray || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="胸片检查日期">
              {{ viewDetail.chestXrayDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="胸片结果" :span="2">
              {{ viewDetail.chestXrayResult || "-" }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="view-tracking-section">
            <div class="view-tracking-title">
              追踪过程
            </div>
            <div v-if="viewTrackingHistory.length === 0" class="tracking-history-empty">
              暂无追踪记录
            </div>
            <div v-else class="tracking-history">
              <div v-for="item in viewTrackingHistory" :key="item.attempt" class="tracking-history-item">
                <span class="tracking-history-attempt">第{{ item.attempt }}次</span>
                <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
                  {{ TRACK_STATUS_LABEL[item.status] }}
                </el-tag>
                <span class="tracking-history-time">{{ formatDateTime(item.trackTime) }}</span>
                <span v-if="item.reason" class="tracking-history-reason">备注：{{ item.reason }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 跨镇导入：按乡镇选择区县三级确认人 -->
    <el-dialog
      v-model="crossTownDialogVisible"
      title="跨镇人员需区县三级确认"
      width="720px"
      :close-on-click-modal="false"
      @closed="onCrossTownDialogClosed"
    >
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px">
        以下人员现住址乡镇与当前账号单位不一致。请按乡镇选择本区县三级用户发起确认；确认前可在列表查看，但不可追踪操作。
      </el-alert>
      <div
        v-for="(group, gIdx) in crossTownGroups"
        :key="`${group.township || '_empty'}-${gIdx}`"
        style="margin-bottom: 20px; padding: 12px; background: #fafafa; border-radius: 6px"
      >
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 10px; flex-wrap: wrap">
          <strong>{{ crossTownGroupLabel(group.township) }}</strong>
          <el-tag size="small" type="info">
            {{ group.items.length }} 人
          </el-tag>
          <el-select
            v-model="group.receiverUserId"
            placeholder="选择区县三级用户（必选）"
            filterable
            style="width: 280px"
          >
            <el-option
              v-for="u in countyLevel3Users"
              :key="u.id"
              :label="`${u.realName || u.username}${u.departmentName ? `（${u.departmentName}）` : ''}`"
              :value="String(u.id)"
            />
          </el-select>
        </div>
        <div style="font-size: 13px; color: #606266; line-height: 1.7">
          <div v-for="(item, idx) in group.items.slice(0, 8)" :key="`${item.idNumber}-${idx}`">
            {{ item.name }}（{{ item.idNumber }}）
            <span v-if="item.message" style="color: #909399"> — {{ item.message }}</span>
          </div>
          <div v-if="group.items.length > 8" style="color: #909399">
            ... 等共 {{ group.items.length }} 人
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="crossTownDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="countyLevel3Users.length === 0"
          @click="submitCrossTownImport"
        >
          确认并导入
        </el-button>
      </template>
    </el-dialog>

    <!-- 大疫情导入弹窗 -->
    <el-dialog v-model="importDialogVisible" title="大疫情导入" width="680px">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          上传大疫情网（报告卡）导出文件（.xlsx / .xls），系统自动提取以下字段并创建追踪记录。
        </template>
        <template #default>
          <div style="margin-top: 8px; font-size: 13px; line-height: 1.8; color: #606266">
            {{ EPIDEMIC_TRACK_IMPORT_FIELDS.join("、") }}
          </div>
        </template>
      </el-alert>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept=".xlsx,.xls"
        :on-change="handleEpidemicFileChange"
        drag
        style="width: 100%"
      >
        <div style="padding: 24px 0">
          <div style="font-size: 16px; color: #606266">
            拖拽大疫情表文件到此处，或 <span style="color: #409eff">点击上传</span>
          </div>
          <div style="font-size: 12px; color: #909399; margin-top: 8px">
            支持 .xlsx / .xls 格式
          </div>
        </div>
      </el-upload>
      <div v-if="uploading" style="text-align: center; margin-top: 16px; color: #606266">
        正在解析并导入，请稍候...
      </div>
      <el-result
        v-if="importResult"
        icon="success"
        :title="`成功导入 ${importResult.count} 条追踪记录`"
        :sub-title="`批次号：${importResult.batchNo}`"
      />
      <template #footer>
        <el-button @click="importDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑追踪记录" width="720px">
      <el-form :model="editForm" label-width="120px">
        <el-row :gutter="16">
          <template v-if="isEpidemicRow(editRow)">
            <el-col :span="12">
              <el-form-item label="卡片ID">
                <el-input v-model="editForm.cardId" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="患儿家长姓名">
                <el-input v-model="editForm.parentName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="工作单位">
                <el-input v-model="editForm.workplace" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="乡镇">
                <el-input v-model="editForm.township" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="病例分类">
                <el-input v-model="editForm.caseCategory" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="疾病名称">
                <el-input v-model="editForm.diseaseName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="报告单位">
                <el-input v-model="editForm.reportUnit" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="报告卡录入时间">
                <el-date-picker v-model="editForm.reportCardTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="editForm.epidemicRemark" type="textarea" :rows="2" />
              </el-form-item>
            </el-col>
          </template>
          <el-col :span="12">
            <el-form-item label="患者姓名">
              <el-input v-model="editForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="editForm.gender" style="width: 100%">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="editForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="editForm.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效证件号">
              <el-input v-model="editForm.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="editForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现住详细地址">
              <el-input v-model="editForm.currentAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人群分类">
              <el-input v-model="editForm.crowdCategory" />
            </el-form-item>
          </el-col>
          <el-col v-if="!isEpidemicRow(editRow)" :span="24">
            <el-form-item label="追踪原因">
              <el-input v-model="editForm.trackReason" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <template v-if="canEditDiagnosis">
            <el-col :span="24">
              <el-divider content-position="left">
                诊断结果
              </el-divider>
            </el-col>
            <el-col :span="24">
              <el-form-item label="诊断结果" required>
                <el-radio-group v-model="editForm.diagnosisResult">
                  <el-radio
                    v-for="item in editDiagnosisOptions"
                    :key="item.value"
                    :value="item.value"
                  >
                    {{ item.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col v-if="editForm.diagnosisResult === '其他'" :span="24">
              <el-form-item label="诊断备注" required>
                <el-input
                  v-model="editForm.diagnosisRemark"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  placeholder="请输入其他诊断结果说明"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-alert
                title="修改诊断结果仅更新本页展示，不会重新触发分流（如创建潜伏感染者）"
                type="info"
                :closable="false"
                show-icon
                style="margin-bottom: 8px"
              />
            </el-col>
          </template>
          <template v-if="canEditTrackingHistory">
            <el-col :span="24">
              <el-divider content-position="left">
                追踪过程
              </el-divider>
            </el-col>
            <el-col
              v-for="item in editTrackingHistory"
              :key="item.attempt"
              :span="24"
            >
              <el-form-item :label="`第${item.attempt}次追踪`" required>
                <div class="edit-tracking-meta">
                  <el-select v-model="item.status" style="width: 120px" size="small">
                    <el-option label="到位" :value="1" />
                    <el-option label="未到位" :value="2" />
                    <el-option label="其他" :value="3" />
                  </el-select>
                  <span class="edit-tracking-time">{{ formatDateTime(item.trackTime) }}</span>
                  <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
                    {{ TRACK_STATUS_LABEL[item.status] || "-" }}
                  </el-tag>
                </div>
                <el-input
                  v-model="item.reason"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  placeholder="请填写追踪备注"
                />
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleEditSave">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增追踪弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增追踪记录" width="660px">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="createForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="createForm.gender" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="createForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="createForm.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件类型">
              <el-input v-model="createForm.idType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号" prop="idNumber">
              <el-input v-model="createForm.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="民族">
              <el-input v-model="createForm.ethnicity" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="createForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户籍地址">
              <el-input v-model="createForm.householdAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现住址" prop="currentAddress">
              <el-input v-model="createForm.currentAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="人群分类" prop="crowdCategory">
              <el-select v-model="createForm.crowdCategory" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in REFERRAL_CROWD_CATEGORY_OPTIONS"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-divider content-position="left">
              筛查与诊断（选填）
            </el-divider>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染检测方法">
              <el-select v-model="createForm.screenMethod" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染检测结果">
              <el-select v-model="createForm.infectionResult" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片检查日期">
              <el-date-picker
                v-model="createForm.chestXrayDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片结果">
              <el-select
                v-model="createForm.chestXrayResult"
                placeholder="请选择"
                clearable
                style="width: 100%"
                @change="() => { if (!isReferralChestXrayOther(createForm.chestXrayResult)) createForm.chestXrayRemark = '' }"
              >
                <el-option
                  v-for="opt in REFERRAL_CHEST_XRAY_RESULT_OPTIONS"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="isReferralChestXrayOther(createForm.chestXrayResult)" :span="24">
            <el-form-item label="胸片结果备注">
              <el-input v-model="createForm.chestXrayRemark" type="textarea" :rows="2" placeholder="请填写其他胸片检查结果" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="诊断结果">
              <el-radio-group v-model="createForm.diagnosisResult">
                <el-radio
                  v-for="item in REFERRAL_TRACKING_DIAGNOSIS_OPTIONS"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="追踪原因" prop="trackReason">
              <el-input
                v-model="createForm.trackReason"
                type="textarea"
                :rows="3"
                placeholder="请填写追踪原因"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleCreate">
          确认创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 追踪操作弹窗 -->
    <TrackingOperationDialog
      v-model="trackDialogVisible"
      :history-json="trackRow?.trackingHistoryJson"
      :not-in-place-count="trackRow?.notInPlaceCount ?? 0"
      :loading="trackSubmitting"
      @confirm="handleTrack"
    />

    <!-- 录入筛查信息弹窗 -->
    <el-dialog v-model="screeningDialogVisible" title="录入感染检测结果及胸片结果" width="600px">
      <el-form :model="screeningForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否感染检测">
              <el-select v-model="screeningForm.hasInfectionScreen" style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染检测日期">
              <el-date-picker v-model="screeningForm.screenDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染检测方法">
              <el-select v-model="screeningForm.screenMethod" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in referralSelectOptionsWithLegacy(REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS, screeningForm.screenMethod)"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染检测结果">
              <el-select v-model="screeningForm.infectionResult" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in referralSelectOptionsWithLegacy(REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS, screeningForm.infectionResult)"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否胸片检查">
              <el-select v-model="screeningForm.hasChestXray" style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片检查日期">
              <el-date-picker v-model="screeningForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片结果">
              <el-select
                v-model="screeningForm.chestXrayResult"
                placeholder="请选择"
                clearable
                style="width: 100%"
                @change="() => { if (!isReferralChestXrayOther(screeningForm.chestXrayResult)) screeningForm.chestXrayRemark = '' }"
              >
                <el-option
                  v-for="opt in chestXrayResultSelectOptions"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="isReferralChestXrayOther(screeningForm.chestXrayResult)" :span="24">
            <el-form-item label="胸片结果备注">
              <el-input v-model="screeningForm.chestXrayRemark" type="textarea" :rows="2" placeholder="请填写其他胸片检查结果" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="screeningDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSaveScreening">
          保存
        </el-button>
      </template>
    </el-dialog>

    <ReferralDiagnosisDialog
      v-model:visible="diagnosisDialogVisible"
      :record-id="diagnosisRow?.id"
      @success="fetchList"
    />
  </div>
</template>

<style scoped lang="scss">
.tracking-history {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tracking-history-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
}

.tracking-history-attempt {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tracking-history-time {
  color: var(--el-text-color-secondary);
}

.tracking-history-reason {
  width: 100%;
  color: var(--el-text-color-regular);
}

.tracking-history-empty {
  padding: 16px 0;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.view-tracking-section {
  margin-top: 16px;
}

.view-tracking-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.edit-tracking-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.edit-tracking-time {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>

<style lang="scss">
.el-table .confirmed-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
