<script setup lang="ts">
import type { TrackConfirmPayload } from "@@/components/TrackingOperationDialog.vue"
import PrintRecommend from "@@/components/PrintRecommend.vue"
import ReferralDiagnosisDialog from "@@/components/ReferralDiagnosisDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import TrackingOperationDialog from "@@/components/TrackingOperationDialog.vue"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { isConfirmedPatientDiagnosis, REFERRAL_CROWD_CATEGORY_OPTIONS, REFERRAL_TRACKING_DIAGNOSIS_OPTIONS } from "@@/constants/disease"
import { PAGE_SIZE_OPTIONS } from "@@/constants/pagination"
import {
  REFERRAL_CHEST_XRAY_RESULT_OPTIONS,
  REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS,
  REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS,
  referralSelectOptionsWithLegacy
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
import { extractDateRangeParams } from "@@/utils/searchParams"
import { idCardRule, phoneRule } from "@@/utils/validate"
import { ElMessage, ElMessageBox } from "element-plus"
import { computed, nextTick, onMounted, reactive, ref } from "vue"
import { useMessageStore } from "@/pinia/stores/message"
import { useUserStore } from "@/pinia/stores/user"
import {
  batchDeleteReferralTrackingApi,
  confirmRecommendApi,
  deleteAllReferralTrackingApi,
  deleteReferralTrackingApi,
  deleteReferralTrackingByFilterApi,
  enableJointTrackingApi,
  exportReferralTrackApi,
  getLevel34UsersApi,
  getReferralTrackingDetailApi,
  getReferralTrackingListApi,
  rejectRecommendApi,
  saveScreeningInfoApi,
  sendRecommendApi,
  trackReferralApi,
  updateReferralTrackingApi
} from "../apis/index"
import { createReferralWithDuplicateConfirm, isReferralDuplicateCancel } from "../composables/useReferralDuplicateConfirm"

const userStore = useUserStore()
const messageStore = useMessageStore()

/** 推介模块共同追踪：未到位 4 次强制结束 */
const RECOMMEND_FORCE_END_THRESHOLD = 4

function isJointTrackingEnabled(row: any) {
  return Number(row?.jointTracking) === 1
}

/** 已确认推介：接收方可点击追踪（自动开启共同追踪）；共同追踪开启后发起方也可追踪 */
function canOperateRecommendTrack(row: any) {
  if (row.archived || row.recommendStatus !== 2) return false
  if (userStore.userRole === 1) return true
  const uid = Number(userStore.userId)
  if (uid === Number(row.receiverUserId)) return true
  if (isJointTrackingEnabled(row) && uid === Number(row.creatorId)) return true
  return false
}

/** 超级管理员或拥有新增权限的一至五级用户可发起推介 */
const canCreateRecommend = computed(() =>
  userStore.userRole === 1 || ([2, 3, 4, 5, 6].includes(userStore.userRole) && userStore.hasPermission("referralManagement:create"))
)
/** 超级管理员和一至五级用户展示推介操作指引 */
const showRecommendGuide = computed(() => userStore.userRole === 1 || [2, 3, 4, 5, 6].includes(userStore.userRole))

// ===== 列表 =====
const loading = ref(false)
const exporting = ref(false)
const batchDeleting = ref(false)
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
const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  township: "",
  creatorOrEntryUnit: "",
  dateRange: [] as string[]
})
const paginationData = reactive({ currentPage: 1, pageSize: 20 })

/** 列表/筛选导出/按筛删除共用查询参数（不含分页） */
function buildFilterParams() {
  const columnFiltersParam = toQueryParam()
  return {
    bizMode: "recommend",
    name: searchForm.name || undefined,
    idNumber: searchForm.idNumber || undefined,
    phone: searchForm.phone || undefined,
    township: searchForm.township || undefined,
    creatorOrEntryUnit: searchForm.creatorOrEntryUnit || undefined,
    ...extractDateRangeParams(searchForm.dateRange),
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
  searchForm.creatorOrEntryUnit = ""
  searchForm.dateRange = []
  clearFilters()
  handleSearch()
}

/** 导出：filtered=筛选 / selected=勾选 / all=全部 */
async function handleExport(mode: "filtered" | "selected" | "all" = "filtered", ids?: number[]) {
  const isSelected = mode === "selected"
  const label = isSelected
    ? `选中的 ${ids!.length} 条`
    : mode === "all"
      ? "全部"
      : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}推介数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportReferralTrackApi(
      isSelected
        ? { bizMode: "recommend", ids }
        : mode === "all"
          ? { bizMode: "recommend" }
          : buildFilterParams()
    )
    downloadBlob(blob as unknown as Blob, "推介记录导出.xlsx")
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
    message: "确定删除当前筛选条件下的全部推介记录吗？此操作不可恢复！"
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
    message: "确定删除权限范围内的全部推介记录吗？此操作不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteAllReferralTrackingApi("recommend")
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
      `确定删除选中的 ${selectedRows.value.length} 条推介记录吗？此操作不可恢复！`,
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

// ===== 新增推介 =====
const createDialogVisible = ref(false)
const level34Users = ref<any[]>([])
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
  screenDate: "",
  screenMethod: "",
  infectionResult: "",
  chestXrayDate: "",
  chestXrayResult: "",
  recommendUnitName: "",
  fillUserName: "",
  recommendReason: "",
  receiverUserId: undefined as number | undefined
})
const createFormRef = ref()
const sendingRecommend = ref(false)
const createPrintVisible = ref(false)
const printData = ref<Record<string, any> | null>(null)

const createFormRules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(true)],
  currentAddress: [{ required: true, message: "请填写现住址", trigger: "blur" }],
  crowdCategory: [{ required: true, message: "请选择人群分类", trigger: "change" }],
  recommendReason: [{ required: true, message: "请填写推介原因", trigger: "blur" }],
  receiverUserId: [{ required: true, message: "请选择推介接收人", trigger: "change" }]
}

async function openCreateDialog() {
  if (level34Users.value.length === 0) {
    const res = await getLevel34UsersApi()
    level34Users.value = res.data ?? []
  }
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
    screenDate: "",
    screenMethod: "",
    infectionResult: "",
    chestXrayDate: "",
    chestXrayResult: "",
    recommendUnitName: resolveRecommendUnitName(),
    fillUserName: resolveFillUserName(),
    recommendReason: "",
    receiverUserId: undefined
  })
  createDialogVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

function resolveFillUserName() {
  return userStore.realName || userStore.username || ""
}

function resolveRecommendUnitName() {
  return userStore.orgName || ""
}

function resolveReceiverUserName(receiverUserId?: number) {
  if (!receiverUserId) return ""
  const receiver = level34Users.value.find(u => Number(u.id) === Number(receiverUserId))
  return receiver ? formatLevel34UserLabel(receiver) : ""
}

function buildRecommendPrintData(source: Record<string, any>) {
  return {
    ...source,
    receiverUserName: source.receiverUserName || resolveReceiverUserName(source.receiverUserId),
    recommendUnitName: source.recommendUnitName || resolveRecommendUnitName(),
    fillUserName: source.fillUserName || resolveFillUserName()
  }
}

function openCreatePrint() {
  printData.value = buildRecommendPrintData(createForm)
  createPrintVisible.value = true
}

function openViewPrint() {
  if (!viewDetail.value) return
  printData.value = buildRecommendPrintData({
    ...viewDetail.value,
    receiverUserName: viewDetail.value.receiverUserName,
    recommendSentTime: formatRecommendTime(viewDetail.value)
  })
  createPrintVisible.value = true
}

async function handleSendRecommend() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  sendingRecommend.value = true
  try {
    await createReferralWithDuplicateConfirm({ ...createForm, bizMode: "recommend" })
    ElMessage.success("推介通知单已发送")
    createDialogVisible.value = false
    fetchList()
  } catch (err) {
    if (!isReferralDuplicateCancel(err)) {
      ElMessage.error("发送失败")
    }
  } finally {
    sendingRecommend.value = false
  }
}

function formatLevel34UserLabel(u: any) {
  const unit = u.orgName?.trim() || "未填写单位"
  return `${u.username}（${unit}）`
}

/** 当前用户是否为推介创建人 */
function isCreator(row: any) {
  return Number(row.creatorId) === Number(userStore.userId)
}

/** 当前用户是否为推介接收人 */
function isReceiver(row: any) {
  return Number(row.receiverUserId) === Number(userStore.userId)
}

function canEditRecommend(row: any) {
  if (row.recommendStatus === 2 || row.recommendStatus === 3) return false
  if (userStore.userRole === 1) return !row.archived
  return isCreator(row) && !row.archived
}

/** 推介列表可见即可删（未追踪、已追踪、已结案等推介后各状态均允许，具体权限由 v-permission 控制） */
function canDeleteRecommend(_row: any) {
  if (userStore.userRole === 1) return true
  return userStore.userRole >= 2 && userStore.userRole <= 6
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

// ===== 编辑推介 =====
const editDialogVisible = ref(false)
const editRow = ref<any>(null)
const editFormRef = ref()
const savingEdit = ref(false)
const editForm = reactive({
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
  screenDate: "",
  screenMethod: "",
  infectionResult: "",
  chestXrayDate: "",
  chestXrayResult: "",
  recommendReason: ""
})

const editFormRules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(true)],
  currentAddress: [{ required: true, message: "请填写现住址", trigger: "blur" }],
  crowdCategory: [{ required: true, message: "请选择人群分类", trigger: "change" }],
  recommendReason: [{ required: true, message: "请填写推介原因", trigger: "blur" }]
}

async function openEditDialog(row: any) {
  editRow.value = row
  Object.assign(editForm, {
    name: row.name ?? "",
    gender: row.gender ?? "",
    birthDate: row.birthDate ?? "",
    age: row.age,
    idType: row.idType ?? "居民身份证",
    idNumber: row.idNumber ?? "",
    ethnicity: row.ethnicity ?? "",
    phone: row.phone ?? "",
    householdAddress: row.householdAddress ?? "",
    currentAddress: row.currentAddress ?? "",
    crowdCategory: row.crowdCategory ?? "",
    screenDate: row.screenDate ?? "",
    screenMethod: row.screenMethod ?? "",
    infectionResult: row.infectionResult ?? "",
    chestXrayDate: row.chestXrayDate ?? "",
    chestXrayResult: row.chestXrayResult ?? "",
    recommendReason: row.recommendReason ?? ""
  })
  editDialogVisible.value = true
  nextTick(() => editFormRef.value?.clearValidate())
}

async function handleEditSave() {
  try {
    await editFormRef.value?.validate()
  } catch {
    return
  }
  savingEdit.value = true
  try {
    await updateReferralTrackingApi(editRow.value.id, { ...editForm })
    ElMessage.success("保存成功")
    editDialogVisible.value = false
    fetchList()
  } finally {
    savingEdit.value = false
  }
}

// ===== 发送推介通知 =====
async function handleSend(row: any) {
  await ElMessageBox.confirm(`确认向接收人发送「${row.name}」的推介通知单？`, "发送确认", { type: "warning" })
  await sendRecommendApi(row.id)
  ElMessage.success("推介通知单已发送")
  fetchList()
}

// ===== 确认/拒绝推介 =====
async function handleConfirm(row: any) {
  await ElMessageBox.confirm(`确认接受「${row.name}」的推介通知单？确认后请在本页开展追踪。`, "确认接收", { type: "info" })
  await confirmRecommendApi(row.id)
  ElMessage.success("已确认接受，请在本页点击「追踪」开展共同追踪")
  await messageStore.fetchUnreadCount()
  fetchList()
}

const rejectDialogVisible = ref(false)
const rejectRow = ref<any>(null)
const rejectReason = ref("")

function openRejectDialog(row: any) {
  rejectRow.value = row
  rejectReason.value = ""
  rejectDialogVisible.value = true
}

async function handleReject() {
  await rejectRecommendApi(rejectRow.value.id, rejectReason.value)
  ElMessage.success("已拒绝推介通知单")
  rejectDialogVisible.value = false
  await messageStore.fetchUnreadCount()
  fetchList()
}

// ===== 追踪操作 =====
const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const trackSubmitting = ref(false)

function openTrackDialog(row: any) {
  trackRow.value = row
  trackDialogVisible.value = true
}

/** 推介模块：点击「追踪」即开启共同追踪（接收方首次点击时自动开启） */
async function handleRecommendTrack(row: any) {
  if (isReceiver(row) && !isJointTrackingEnabled(row)) {
    await ElMessageBox.confirm(
      `确认对「${row.name}」开启共同追踪并开展追踪吗？开启后您与推介发起方均可追踪，双方操作次数合并计算（${RECOMMEND_FORCE_END_THRESHOLD} 次未到位自动结束）。`,
      "追踪确认",
      { type: "warning", confirmButtonText: "确认", cancelButtonText: "取消" }
    )
    await enableJointTrackingApi(row.id)
    ElMessage.success("已开启共同追踪")
    await fetchList()
    const updated = tableData.value.find((r: any) => r.id === row.id) ?? { ...row, jointTracking: 1 }
    openTrackDialog(updated)
    return
  }
  openTrackDialog(row)
}

async function handleTrack(payload: TrackConfirmPayload) {
  if (trackSubmitting.value) return
  trackSubmitting.value = true
  try {
    const willForceEnd = payload.status === 2
      && (trackRow.value?.notInPlaceCount ?? 0) >= RECOMMEND_FORCE_END_THRESHOLD - 1
    await trackReferralApi(trackRow.value.id, payload.status, payload.remark, payload.actualArrivalDate)
    if (willForceEnd) {
      ElMessage.warning(`已记录第 ${RECOMMEND_FORCE_END_THRESHOLD} 次未到位，追踪已强制结束`)
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

// ===== 查看追踪记录（只读） =====
const historyViewVisible = ref(false)
const historyViewRow = ref<any>(null)
const historyViewList = computed(() =>
  parseTrackingHistory(historyViewRow.value?.trackingHistoryJson)
)

function formatRecommendTime(row: any) {
  const time = getRecommendTime(row)
  return time ? formatDateTime(time) : "-"
}

// ===== 筛查信息 =====
const screeningDialogVisible = ref(false)
const screeningRow = ref<any>(null)
const chestXrayResultSelectOptions = computed(() =>
  referralSelectOptionsWithLegacy(REFERRAL_CHEST_XRAY_RESULT_OPTIONS, screeningForm.chestXrayResult))

const screeningForm = reactive({
  hasInfectionScreen: "",
  screenDate: "",
  screenMethod: "",
  screenResult: "",
  infectionResult: "",
  hasChestXray: "",
  chestXrayDate: "",
  chestXrayResult: ""
})

function openScreeningDialog(row: any) {
  screeningRow.value = row
  Object.assign(screeningForm, {
    hasInfectionScreen: row.hasInfectionScreen ?? "",
    screenDate: row.screenDate ?? "",
    screenMethod: row.screenMethod ?? "",
    screenResult: row.screenResult ?? "",
    infectionResult: row.infectionResult ?? "",
    hasChestXray: row.hasChestXray ?? "",
    chestXrayDate: row.chestXrayDate ?? "",
    chestXrayResult: row.chestXrayResult ?? ""
  })
  screeningDialogVisible.value = true
}

async function handleSaveScreening() {
  await saveScreeningInfoApi(screeningRow.value.id, { ...screeningForm })
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
  await ElMessageBox.confirm(`确认删除「${row.name}」的推介记录？`, "删除确认", { type: "warning" })
  await deleteReferralTrackingApi(row.id)
  ElMessage.success("删除成功")
  fetchList()
}

// ===== 状态标签辅助 =====
function getRowClass({ row }: { row: any }) {
  if (row.archived && isConfirmedPatientDiagnosis(row)) return "confirmed-row"
  if (isCreator(row) && (row.recommendStatus === 2 || row.recommendStatus === 3)) {
    if (row.recommendStatus === 2 && isJointTrackingEnabled(row) && !row.archived
      && row.trackingStatus !== 4 && !row.diagnosisResult) {
      return ""
    }
    return "recommend-settled-row"
  }
  return ""
}
const RECOMMEND_STATUS_MAP: Record<number, { label: string, type: string }> = {
  0: { label: "未发送", type: "info" },
  1: { label: "已发送", type: "warning" },
  2: { label: "已接受", type: "success" },
  3: { label: "已拒绝", type: "danger" }
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
        <el-form-item label="录入者/录入单位">
          <el-input v-model="searchForm.creatorOrEntryUnit" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="新建推介时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
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
      <el-alert
        v-if="showRecommendGuide"
        type="info"
        :closable="false"
        class="mb-3"
        title="待接收的推介通知单会显示在下方，也可在「系统消息」中确认。接收方确认后请在本页点击「追踪」开展共同追踪（双方次数合并计算，4 次未到位自动结束）。您发起的推介仍保留在本页，共同追踪开启后您也可参与追踪。"
      />
      <div class="toolbar-wrapper" style="margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap">
        <el-button v-if="canCreateRecommend" type="primary" @click="openCreateDialog">
          新增推介
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
              :model-value="columnFilters.gender"
              @change="(v) => { setFilter('gender', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" min-width="160">
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
        <el-table-column prop="township" min-width="100">
          <template #header>
            <TableHeaderFilter
              label="乡镇"
              :model-value="columnFilters.township"
              @change="(v) => { setFilter('township', v); handleSearch() }"
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
        <el-table-column prop="crowdCategory" label="人群分类" />
        <el-table-column prop="recommendReason" label="推介原因" show-overflow-tooltip />
        <el-table-column prop="receiverUserName" label="推介接收人" />
        <el-table-column label="推介状态">
          <template #default="{ row }">
            <el-tag
              v-if="row.recommendStatus !== null && row.recommendStatus !== undefined"
              :type="RECOMMEND_STATUS_MAP[row.recommendStatus]?.type as any"
              size="small"
            >
              {{ RECOMMEND_STATUS_MAP[row.recommendStatus]?.label }}
            </el-tag>
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
        <el-table-column label="推介时间" min-width="160">
          <template #default="{ row }">
            {{ formatRecommendTime(row) }}
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
        <el-table-column label="追踪次数" width="100">
          <template #default="{ row }">
            {{ row.notInPlaceCount > 0 ? `${row.notInPlaceCount}次未到位` : "-" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openViewDialog(row)">
              查看
            </el-button>
            <el-button
              v-if="canEditRecommend(row)"
              type="primary" link size="small"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="canDeleteRecommend(row)"
              v-permission="'referralManagement:delete'"
              type="danger" link size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <!-- 补发未发送的推介 -->
            <el-button
              v-if="isCreator(row) && row.recommendStatus === 0 && !row.archived"
              type="primary" link size="small"
              @click="handleSend(row)"
            >
              发送推介
            </el-button>
            <!-- 接收人：确认/拒绝待接收推介 -->
            <el-button
              v-if="row.recommendStatus === 1 && isReceiver(row)"
              type="success" link size="small"
              @click="handleConfirm(row)"
            >
              确认接受
            </el-button>
            <el-button
              v-if="row.recommendStatus === 1 && isReceiver(row)"
              type="danger" link size="small"
              @click="openRejectDialog(row)"
            >
              拒绝
            </el-button>
            <!-- 已确认推介：追踪（合并共同追踪，仅推介模块） -->
            <el-button
              v-if="canOperateRecommendTrack(row) && [0, 2].includes(row.trackingStatus)"
              v-permission="'referralManagement:trackOperate'"
              type="warning" link size="small"
              @click="handleRecommendTrack(row)"
            >
              追踪
            </el-button>
            <el-button
              v-if="canOperateRecommendTrack(row) && row.trackingStatus === 1 && !row.diagnosisResult"
              v-permission="'referralManagement:xray'"
              type="primary" link size="small"
              @click="openScreeningDialog(row)"
            >
              录入胸片
            </el-button>
            <el-button
              v-if="canOperateRecommendTrack(row) && row.trackingStatus === 1 && !row.diagnosisResult"
              v-permission="'referralManagement:diagnosis'"
              type="success" link size="small"
              @click="openDiagnosisDialog(row)"
            >
              录入诊断
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

    <!-- 新增推介弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增推介记录" width="720px">
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
          <el-col :span="12">
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
          <el-col :span="12">
            <el-form-item label="推介接收人" prop="receiverUserId">
              <el-select
                v-model="createForm.receiverUserId"
                filterable
                placeholder="选择一至五级用户"
                style="width: 100%"
              >
                <el-option
                  v-for="u in level34Users"
                  :key="u.id"
                  :label="formatLevel34UserLabel(u)"
                  :value="u.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-divider content-position="left">
              筛查信息（选填）
            </el-divider>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查时间">
              <el-date-picker
                v-model="createForm.screenDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查方法">
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
            <el-form-item label="感染筛查结果">
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
            <el-form-item label="胸片筛查时间">
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
            <el-form-item label="胸片筛查结果">
              <el-select v-model="createForm.chestXrayResult" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in REFERRAL_CHEST_XRAY_RESULT_OPTIONS"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推介单位名称">
              <el-input v-model="createForm.recommendUnitName" readonly placeholder="系统自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="填写用户名称">
              <el-input v-model="createForm.fillUserName" readonly placeholder="系统自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="推介原因" prop="recommendReason">
              <el-input
                v-model="createForm.recommendReason"
                type="textarea"
                :rows="3"
                placeholder="请填写推介原因"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="recommend-dialog-footer">
          <el-button @click="openCreatePrint">
            打印 / 保存PDF
          </el-button>
          <div class="recommend-dialog-footer__actions">
            <el-button @click="createDialogVisible = false">
              取消
            </el-button>
            <el-button type="primary" :loading="sendingRecommend" @click="handleSendRecommend">
              发送推介
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <PrintRecommend v-model:visible="createPrintVisible" :data="printData" />

    <!-- 查看推介详情 -->
    <el-dialog v-model="viewDialogVisible" title="推介详情" width="760px">
      <div v-loading="viewLoading">
        <template v-if="viewDetail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="姓名">
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
            <el-descriptions-item label="证件号">
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
            <el-descriptions-item label="现住址" :span="2">
              {{ viewDetail.currentAddress || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="人群分类">
              {{ viewDetail.crowdCategory || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="录入者">
              {{ viewDetail.creatorUserName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="录入单位" :span="2">
              {{ viewDetail.entryUnitName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="推介单位名称">
              {{ viewDetail.recommendUnitName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="填写用户名称">
              {{ viewDetail.fillUserName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="推介原因" :span="2">
              {{ viewDetail.recommendReason || "-" }}
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
              {{ formatRecommendTime(viewDetail) }}
            </el-descriptions-item>
            <el-descriptions-item label="确认/拒绝时间">
              {{ viewDetail.recommendConfirmTime ? formatDateTime(viewDetail.recommendConfirmTime) : "-" }}
            </el-descriptions-item>
            <el-descriptions-item v-if="viewDetail.rejectedReason" label="拒绝原因" :span="2">
              {{ viewDetail.rejectedReason }}
            </el-descriptions-item>
            <el-descriptions-item label="感染筛查时间">
              {{ viewDetail.screenDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="感染筛查方法">
              {{ viewDetail.screenMethod || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">
              {{ viewDetail.infectionResult || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="胸片筛查时间">
              {{ viewDetail.chestXrayDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="胸片筛查结果" :span="2">
              {{ viewDetail.chestXrayResult || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="追踪状态">
              <el-tag
                :type="TRACKING_STATUS_MAP[viewDetail.trackingStatus]?.type as any"
                size="small"
              >
                {{ TRACKING_STATUS_MAP[viewDetail.trackingStatus]?.label }}
              </el-tag>
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
            <el-descriptions-item label="未到位次数">
              {{ viewDetail.notInPlaceCount > 0 ? `${viewDetail.notInPlaceCount}次` : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="共同追踪">
              <el-tag :type="viewDetail.jointTracking === 1 ? 'success' : 'info'" size="small">
                {{ viewDetail.jointTracking === 1 ? "已开启" : "未开启" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="viewDetail.jointTrackingTime" label="共同追踪时间">
              {{ formatDateTime(viewDetail.jointTrackingTime) }}
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="viewTrackingHistory.length" class="view-tracking-section">
            <div class="view-tracking-title">
              {{ viewDetail.jointTracking === 1 ? "共同追踪过程" : "对方追踪过程" }}
            </div>
            <div class="tracking-history">
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
        <el-button @click="openViewPrint">
          打印 / 保存PDF
        </el-button>
        <el-button @click="viewDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑推介弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑推介记录" width="720px">
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="editForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="editForm.gender" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
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
            <el-form-item label="证件类型">
              <el-input v-model="editForm.idType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号" prop="idNumber">
              <el-input v-model="editForm.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="民族">
              <el-input v-model="editForm.ethnicity" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="editForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户籍地址">
              <el-input v-model="editForm.householdAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现住址" prop="currentAddress">
              <el-input v-model="editForm.currentAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人群分类" prop="crowdCategory">
              <el-select v-model="editForm.crowdCategory" placeholder="请选择" style="width: 100%">
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
              筛查信息（选填）
            </el-divider>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查时间">
              <el-date-picker v-model="editForm.screenDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查方法">
              <el-select v-model="editForm.screenMethod" placeholder="请选择" clearable style="width: 100%">
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
            <el-form-item label="感染筛查结果">
              <el-select v-model="editForm.infectionResult" placeholder="请选择" clearable style="width: 100%">
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
            <el-form-item label="胸片筛查时间">
              <el-date-picker v-model="editForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片筛查结果">
              <el-select v-model="editForm.chestXrayResult" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in REFERRAL_CHEST_XRAY_RESULT_OPTIONS"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推介单位名称">
              <el-input :model-value="editRow?.recommendUnitName || '-'" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="填写用户名称">
              <el-input :model-value="editRow?.fillUserName || '-'" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="推介原因" prop="recommendReason">
              <el-input v-model="editForm.recommendReason" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="savingEdit" @click="handleEditSave">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 拒绝推介弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝推介" width="420px">
      <el-form label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">
          取消
        </el-button>
        <el-button type="danger" @click="handleReject">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看追踪记录弹窗（只读） -->
    <el-dialog v-model="historyViewVisible" title="追踪过程" width="520px">
      <div v-if="historyViewList.length === 0" class="tracking-history-empty">
        暂无追踪记录
      </div>
      <div v-else class="tracking-history">
        <div v-for="item in historyViewList" :key="item.attempt" class="tracking-history-item">
          <span class="tracking-history-attempt">第{{ item.attempt }}次</span>
          <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
            {{ TRACK_STATUS_LABEL[item.status] }}
          </el-tag>
          <span class="tracking-history-time">{{ formatDateTime(item.trackTime) }}</span>
          <span v-if="item.reason" class="tracking-history-reason">备注：{{ item.reason }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="historyViewVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 追踪操作弹窗 -->
    <TrackingOperationDialog
      v-model="trackDialogVisible"
      :history-json="trackRow?.trackingHistoryJson"
      :not-in-place-count="trackRow?.notInPlaceCount ?? 0"
      :force-end-threshold="RECOMMEND_FORCE_END_THRESHOLD"
      :loading="trackSubmitting"
      @confirm="handleTrack"
    />

    <!-- 录入筛查信息弹窗 -->
    <el-dialog v-model="screeningDialogVisible" title="录入筛查信息" width="600px">
      <el-form :model="screeningForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否感染筛查">
              <el-select v-model="screeningForm.hasInfectionScreen" style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查日期">
              <el-date-picker v-model="screeningForm.screenDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查方法">
              <el-select v-model="screeningForm.screenMethod" placeholder="请选择" clearable style="width: 100%">
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
            <el-form-item label="筛查结果">
              <el-input v-model="screeningForm.screenResult" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查结果">
              <el-select v-model="screeningForm.infectionResult" placeholder="请选择" clearable style="width: 100%">
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
            <el-form-item label="胸片检查结果">
              <el-select v-model="screeningForm.chestXrayResult" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in chestXrayResultSelectOptions"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
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
  padding: 24px 0;
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

.recommend-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.recommend-dialog-footer__actions {
  display: flex;
  gap: 8px;
}
</style>

<style lang="scss">
.el-table .confirmed-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}

.el-table .recommend-settled-row td.el-table__cell {
  background-color: var(--el-fill-color-light) !important;
  color: var(--el-text-color-secondary);
}
</style>
