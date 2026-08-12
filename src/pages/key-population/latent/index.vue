<script lang="ts" setup>
import type { TrackConfirmPayload } from "@@/components/TrackingOperationDialog.vue"
import { getLevel5UsersApi } from "@@/apis/users"
import NoticeSentStatusButton from "@@/components/NoticeSentStatusButton.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"
import SupervisionFormDetailDialog from "@@/components/SupervisionFormDetailDialog.vue"
import SupervisionFormDialog from "@@/components/SupervisionFormDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import TrackingHistoryPanel from "@@/components/TrackingHistoryPanel.vue"
import TrackingOperationDialog from "@@/components/TrackingOperationDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import {
  CHECK_PERIOD_OPTIONS,
  CHECK_RESULT_OPTIONS,
  CHEST_XRAY_RESULT_OPTIONS,
  CROWD_CATEGORY_OPTIONS,
  formatLatentNoticeTreatmentPlan,
  INFECTION_METHOD_OPTIONS,
  isLatentIndividualPlan,
  KEY_INFECTION_JUDGE_RESULT_OPTIONS,
  LATENT_TREATMENT_PLAN_OPTIONS,
  MEDICATION_STATUS_OPTIONS,
  normalizeLatentTreatmentPlan,
  NOTICE_STATUS_MAP,
  parseLatentNoticeTreatmentPlan,
  REFERRAL_RESULT_OPTIONS,
  SCREENING_DIAGNOSIS_EDIT_OPTIONS,
  TREATMENT_PHASE_MAP
} from "@@/constants/disease"
import { parseTrackingHistory } from "@@/utils/referralTracking"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { idCardRule, phoneRule } from "@@/utils/validate"
import dayjs from "dayjs"
import { getScreeningKeyPopulationDetailApi } from "@/pages/key-population/screening/apis"
import { useUserStore } from "@/pinia/stores/user"
import {
  closeCaseApi,
  confirmNoticeApi,
  exportLatentListApi,
  getCheckListApi,
  getFollowUpListApi,
  getLatentListApi,
  getNoticeListByBizApi,
  getSupervisionDetailApi,
  referralLatentApi,
  saveCheckApi,
  saveFollowUpApi,
  saveNoticeDraftApi,
  sendNoticeApi,
  setMedicationStatusApi,
  submitXrayApi,
  trackLatentApi
} from "./apis"

const POPULATION_TYPE = "keyPopulation"

const userStore = useUserStore()
const level5Users = ref<any[]>([])

async function loadLevel5Users() {
  try {
    const { data } = await getLevel5UsersApi()
    level5Users.value = data || []
  } catch { /* handled */ }
}

onMounted(() => {
  loadLevel5Users()
})

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

const genderFilterOptions = [
  { text: "男", value: "男" },
  { text: "女", value: "女" }
]
const infectionResultFilterOptions = KEY_INFECTION_JUDGE_RESULT_OPTIONS.map(item => ({
  text: item,
  value: item
}))
const chestXrayFilterOptions = CHEST_XRAY_RESULT_OPTIONS.map(item => ({ text: item, value: item }))
const diagnosisFilterOptions = SCREENING_DIAGNOSIS_EDIT_OPTIONS.map(item => ({
  text: item.label,
  value: item.value
}))

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  dateRange: [] as string[],
  archived: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const { dateRange, ...rest } = searchForm
    const columnFiltersParam = toQueryParam()
    const { data } = await getLatentListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: POPULATION_TYPE,
      referralResult: "latent",
      ...rest,
      ...extractDateRangeParams(dateRange),
      ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
    })
    tableData.value = data.records
    total.value = data.total
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
  searchForm.phone = ""
  searchForm.dateRange = []
  searchForm.archived = undefined
  clearFilters()
  handleSearch()
}

const exporting = ref(false)
async function handleExport() {
  try {
    exporting.value = true
    const res = await exportLatentListApi({
      populationType: POPULATION_TYPE,
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      archived: searchForm.archived,
      ...extractDateRangeParams(searchForm.dateRange)
    })
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "重点人群_潜伏感染管理.xlsx"
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } catch (err: any) {
    ElMessage.error(err?.message || "导出失败")
  } finally {
    exporting.value = false
  }
}

const submitting = ref(false)

// ==================== 转诊 ====================
const tierCareVisible = ref(false)
const tierCareRow = ref<any>(null)
function openTierCare(row: any) {
  tierCareRow.value = row
  tierCareVisible.value = true
}

// ==================== 阶段判断 ====================
function getStageInfo(row: any): { label: string, type: "danger" | "warning" | "success" | "info" } {
  if (row.archived === 1) return { label: "已归档", type: "info" }
  if (row.referralResult === "latent") return { label: "潜伏感染者", type: "warning" }
  if (row.trackingStatus === 0) return { label: "待追踪", type: "danger" }
  if (row.trackingStatus === 1 && !row.diagnosisFirst) return { label: "待录入诊断", type: "warning" }
  if (row.trackingStatus === 1 && row.diagnosisFirst && !row.referralResult) return { label: "待诊断", type: "warning" }
  if (row.trackingStatus === 2) return { label: "未到位", type: "danger" }
  if (row.trackingStatus === 3 || row.trackingStatus === 4) return { label: "已终止", type: "info" }
  return { label: "-", type: "info" }
}

// ==================== 追踪弹窗 ====================
const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const historyViewVisible = ref(false)
const historyViewRow = ref<any>(null)

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
  if (submitting.value) return
  submitting.value = true
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
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 录入胸片诊断弹窗 ====================
const xrayDialogVisible = ref(false)
const xrayRow = ref<any>(null)
const xrayForm = reactive({
  hasChestXray: "否",
  chestXrayDate: "",
  chestXrayResult: "",
  diagnosisFirst: ""
})

function openXrayDialog(row: any) {
  xrayRow.value = row
  xrayForm.hasChestXray = "否"
  xrayForm.chestXrayDate = ""
  xrayForm.chestXrayResult = ""
  xrayForm.diagnosisFirst = ""
  xrayDialogVisible.value = true
}

async function handleSubmitXray() {
  if (!xrayForm.diagnosisFirst) {
    ElMessage.warning("请选择首次诊断结果")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await submitXrayApi({ id: xrayRow.value.id, ...xrayForm })
    ElMessage.success("胸片诊断录入成功")
    xrayDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 转诊弹窗 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
const referralResultValue = ref("")
const referralRemark = ref("")
const actualReferralDate = ref("")

const REFERRAL_OPTIONS = REFERRAL_RESULT_OPTIONS

function openReferralDialog(row: any) {
  referralRow.value = row
  referralResultValue.value = ""
  referralRemark.value = ""
  actualReferralDate.value = dayjs().format("YYYY-MM-DD")
  referralDialogVisible.value = true
}

async function handleReferral() {
  if (!referralResultValue.value) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  if (!actualReferralDate.value) {
    ElMessage.warning("请选择转诊时间")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await referralLatentApi({
      id: referralRow.value.id,
      result: referralResultValue.value,
      remark: referralRemark.value,
      actualReferralDate: actualReferralDate.value
    })
    ElMessage.success("诊断操作成功")
    referralDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 通知单弹窗 ====================
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const noticeFormRef = ref()
const noticeFormRules = {
  receiverOrgId: [{ required: true, message: "请选择接收单位", trigger: "change" }],
  idNumber: [idCardRule()],
  phone: [phoneRule()]
}
const noticeForm = reactive({
  idNumber: "",
  gender: "",
  birthDate: "",
  age: null as number | null,
  ethnicity: "",
  phone: "",
  crowdCategory: "",
  currentAddress: "",
  householdAddress: "",
  infectionDate: "",
  infectionMethod: "",
  infectionResultValue: "",
  chestXrayDate: "",
  chestXrayResult: "",
  treatmentPlan: "",
  customPlanDetail: "",
  treatmentInstitution: "",
  issuedTime: "",
  receiverOrgId: undefined as string | undefined
})

function getNowDateStr() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function openNoticeDialog(row: any) {
  noticeRow.value = row
  resetNoticeFormFromRow(row)
  noticeDialogVisible.value = true
  loadNoticeDraft(row)
}

function resetNoticeFormFromRow(row: any) {
  const parsedPlan = parseLatentNoticeTreatmentPlan(row.preventivePlan || "")
  Object.assign(noticeForm, {
    idNumber: row.idNumber || "",
    gender: row.gender || "",
    birthDate: row.birthDate || "",
    age: row.age || null,
    phone: row.phone || "",
    ethnicity: row.ethnicity || "",
    crowdCategory: row.crowdCategory || "",
    currentAddress: row.currentAddress || "",
    householdAddress: row.householdAddress || "",
    infectionDate: row.screenDate || "",
    infectionMethod: row.screenMethod || "",
    infectionResultValue: row.screenResult || row.infectionResult || "",
    chestXrayDate: row.chestXrayDate || "",
    chestXrayResult: row.chestXrayResult || "",
    treatmentPlan: parsedPlan.treatmentPlan,
    customPlanDetail: parsedPlan.customPlanDetail,
    treatmentInstitution: "",
    issuedTime: getNowDateStr(),
    receiverOrgId: undefined
  })
}

async function loadNoticeDraft(row: any) {
  if (row.noticeSent) return
  try {
    const { data } = await getNoticeListByBizApi(row.id, "latent")
    const notice = data?.[0]
    if (!notice) return
    if (notice.status !== 0 && notice.status !== 2) return
    Object.assign(noticeForm, {
      idNumber: notice.idNumber || row.idNumber || "",
      gender: notice.gender || row.gender || "",
      birthDate: notice.birthDate || row.birthDate || "",
      age: notice.age ?? row.age ?? null,
      phone: notice.phone || row.phone || "",
      ethnicity: notice.ethnicity || row.ethnicity || "",
      crowdCategory: notice.crowdCategory || row.crowdCategory || "",
      currentAddress: notice.currentAddress || row.currentAddress || "",
      householdAddress: notice.householdAddress || row.householdAddress || "",
      infectionDate: notice.infectionDate || row.screenDate || "",
      infectionMethod: notice.infectionMethod || row.screenMethod || "",
      infectionResultValue: notice.infectionResultValue || row.infectionResult || "",
      chestXrayDate: notice.chestXrayDate || row.chestXrayDate || "",
      chestXrayResult: notice.chestXrayResult || row.chestXrayResult || "",
      treatmentInstitution: notice.treatmentInstitution || "",
      issuedTime: notice.issuedTime || getNowDateStr(),
      receiverOrgId: notice.receiverOrgId || undefined
    })
    const parsed = parseLatentNoticeTreatmentPlan(
      notice.treatmentPlan || row.preventivePlan,
      notice.customPlanDetail
    )
    noticeForm.treatmentPlan = parsed.treatmentPlan
    noticeForm.customPlanDetail = parsed.customPlanDetail
  } catch { /* ignore */ }
}

function buildNoticePayload() {
  return {
    noticeType: "latent",
    populationType: POPULATION_TYPE,
    bizId: noticeRow.value.id,
    patientName: noticeRow.value.name,
    ...noticeForm,
    treatmentPlan: formatLatentNoticeTreatmentPlan(noticeForm.treatmentPlan, noticeForm.customPlanDetail),
    senderId: userStore.userId
  }
}

async function handleSaveNoticeDraft() {
  if (submitting.value) return
  submitting.value = true
  try {
    noticeForm.issuedTime = getNowDateStr()
    await saveNoticeDraftApi(buildNoticePayload())
    ElMessage.success("通知单草稿已保存")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

async function handleSendNotice() {
  if (submitting.value) return
  const valid = await noticeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    noticeForm.issuedTime = getNowDateStr()
    await sendNoticeApi(buildNoticePayload())
    ElMessage.success("通知单发送成功")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 确认接收通知单 ====================
async function handleConfirmNotice(noticeId: string) {
  try {
    await ElMessageBox.confirm("确认接收此通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收")
    noticeDetailVisible.value = false
    fetchData()
  } catch { /* cancelled or handled */ }
}

// ==================== 通知单详情 ====================
const noticeDetailVisible = ref(false)
const noticeDetailData = ref<any>(null)

async function viewNotice(row: any) {
  try {
    const { data } = await getNoticeListByBizApi(row.id, "latent")
    if (data && data.length > 0) {
      noticeDetailData.value = data[0]
      noticeDetailVisible.value = true
    } else {
      ElMessage.info("暂无通知单")
    }
  } catch { /* handled by interceptor */ }
}

// ==================== 督导表 ====================
const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)
const supervisionDetailVisible = ref(false)
const supervisionDetailData = ref<any>(null)

function openSupervisionDialog(row: any) {
  supervisionRow.value = {
    ...row,
    populationType: POPULATION_TYPE
  }
  supervisionDialogVisible.value = true
}

async function viewSupervision(row: any) {
  try {
    const { data } = await getSupervisionDetailApi(row.id)
    if (data) {
      supervisionDetailData.value = data
      supervisionDetailVisible.value = true
    } else {
      ElMessage.info("暂无督导表")
    }
  } catch { /* handled by interceptor */ }
}

// ==================== 服药状态 ====================
const medicationDialogVisible = ref(false)
const medicationRow = ref<any>(null)
const medicationStatusValue = ref(1)

function openMedicationDialog(row: any) {
  medicationRow.value = row
  medicationStatusValue.value = row.medicationStatus || 1
  medicationDialogVisible.value = true
}

async function handleSetMedication() {
  try {
    await setMedicationStatusApi({ id: medicationRow.value.id, medicationStatus: medicationStatusValue.value })
    ElMessage.success("服药状态设置成功")
    medicationDialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

// ==================== 治疗管理 ====================
const treatmentDialogVisible = ref(false)
const treatmentRow = ref<any>(null)
const followUpList = ref<any[]>([])
const checkList = ref<any[]>([])

async function openTreatmentDialog(row: any) {
  treatmentRow.value = row
  treatmentDialogVisible.value = true
  await Promise.all([loadFollowUps(row.id), loadChecks(row.id)])
}

async function loadFollowUps(latentId: string) {
  try {
    const { data } = await getFollowUpListApi(latentId)
    followUpList.value = data || []
  } catch { /* handled */ }
}

async function loadChecks(latentId: string) {
  try {
    const { data } = await getCheckListApi(latentId)
    checkList.value = data || []
  } catch { /* handled */ }
}

const followUpFormVisible = ref(false)
const followUpForm = reactive({ followUpDate: "", followUpType: "电话随访", content: "", result: "" })

function openFollowUpForm() {
  Object.assign(followUpForm, { followUpDate: "", content: "", result: "" })
  followUpFormVisible.value = true
}

async function handleSaveFollowUp() {
  try {
    await saveFollowUpApi({
      latentInfectionId: treatmentRow.value.id,
      ...followUpForm,
      operator: userStore.realName || userStore.username
    })
    ElMessage.success("随访记录保存成功")
    followUpFormVisible.value = false
    loadFollowUps(treatmentRow.value.id)
  } catch { /* handled */ }
}

const checkFormVisible = ref(false)
const checkForm = reactive({ checkDate: "", checkPeriod: "", checkResult: "", content: "" })

function openCheckForm() {
  Object.assign(checkForm, { checkDate: "", checkPeriod: "", checkResult: "", content: "" })
  checkFormVisible.value = true
}

async function handleSaveCheck() {
  try {
    await saveCheckApi({
      latentInfectionId: treatmentRow.value.id,
      ...checkForm,
      operator: userStore.realName || userStore.username
    })
    ElMessage.success("检查记录保存成功")
    checkFormVisible.value = false
    loadChecks(treatmentRow.value.id)
  } catch { /* handled */ }
}

// ==================== 信息归集 ====================
const aggregateDialogVisible = ref(false)
const aggregateRow = ref<any>(null)
const aggregateNotices = ref<any[]>([])
const aggregateSupervision = ref<any>(null)
const aggregateFollowUps = ref<any[]>([])
const aggregateChecks = ref<any[]>([])

async function openAggregateDialog(row: any) {
  aggregateRow.value = row
  aggregateDialogVisible.value = true
  aggregateNotices.value = []
  aggregateSupervision.value = null
  aggregateFollowUps.value = []
  aggregateChecks.value = []
  try {
    const [noticeRes, supervisionRes, followUpRes, checkRes] = await Promise.allSettled([
      getNoticeListByBizApi(row.id, "latent"),
      getSupervisionDetailApi(row.id),
      getFollowUpListApi(row.id),
      getCheckListApi(row.id)
    ])
    if (noticeRes.status === "fulfilled") aggregateNotices.value = noticeRes.value?.data || []
    if (supervisionRes.status === "fulfilled") aggregateSupervision.value = supervisionRes.value?.data || null
    if (followUpRes.status === "fulfilled") aggregateFollowUps.value = followUpRes.value?.data || []
    if (checkRes.status === "fulfilled") aggregateChecks.value = checkRes.value?.data || []
  } catch { /* partial load is fine */ }
}

// ==================== 筛查详情查看 ====================
const screeningDetailVisible = ref(false)
const screeningDetailData = ref<any>(null)

async function viewScreeningDetail(row: any) {
  if (!row.screeningId) {
    ElMessage.info("暂无筛查原始数据")
    return
  }
  try {
    const { data } = await getScreeningKeyPopulationDetailApi(row.screeningId)
    if (data) {
      screeningDetailData.value = data
      screeningDetailVisible.value = true
    } else {
      ElMessage.info("暂无筛查原始数据")
    }
  } catch { /* handled by interceptor */ }
}

async function handleCloseCase(row: any) {
  try {
    await ElMessageBox.confirm("确认结案归档该潜伏感染者吗？", "结案确认", { type: "warning" })
    await closeCaseApi(row.id)
    ElMessage.success("结案归档成功")
    fetchData()
    if (treatmentDialogVisible.value) treatmentDialogVisible.value = false
  } catch { /* cancelled */ }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
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
        <el-form-item label="归档状态">
          <el-select v-model="searchForm.archived" placeholder="全部" clearable style="width: 120px">
            <el-option label="未归档" :value="0" />
            <el-option label="已归档" :value="1" />
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

    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">重点人群 — 潜伏感染管理</span>
          <el-button type="success" :loading="exporting" @click="handleExport">
            导出 Excel
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" min-width="90" fixed>
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
        </el-table-column>
        <el-table-column prop="chestXrayResult" min-width="100">
          <template #header>
            <TableHeaderFilter
              label="胸片结果"
              type="select"
              :options="chestXrayFilterOptions"
              :model-value="columnFilters.chestXrayResult"
              @change="(v) => { setFilter('chestXrayResult', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisFirst" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="诊断结果"
              type="select"
              :options="diagnosisFilterOptions"
              :model-value="columnFilters.diagnosisFirst"
              @change="(v) => { setFilter('diagnosisFirst', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewNotice(row)">
              {{ row.name }}通知单
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="治疗阶段">
          <template #default="{ row }">
            <el-tag v-if="row.treatmentPhase" :type="row.treatmentPhase === 2 ? 'info' : 'warning'" size="small">
              {{ TREATMENT_PHASE_MAP[row.treatmentPhase] || "-" }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="服药状态">
          <template #default="{ row }">
            {{ MEDICATION_STATUS_OPTIONS.find(o => o.value === row.medicationStatus)?.label || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="当前阶段" fixed="right">
          <template #default="{ row }">
            <el-tag :type="getStageInfo(row).type" size="small">
              {{ getStageInfo(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button type="info" link size="small" @click="viewScreeningDetail(row)">
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

            <!-- 阶段1：待追踪 -->
            <template v-if="row.trackingStatus === 0 && !row.archived">
              <el-button
                v-permission="'keyPopulation:latent:track'"
                type="primary"
                size="small"
                @click="openTrackDialog(row)"
              >
                追踪
              </el-button>
            </template>

            <!-- 阶段2：追踪到位，待录入胸片诊断 -->
            <template v-if="row.trackingStatus === 1 && !row.diagnosisFirst && !row.referralResult">
              <el-button
                v-permission="'keyPopulation:latent:xray'"
                type="warning"
                size="small"
                @click="openXrayDialog(row)"
              >
                录入胸片诊断
              </el-button>
            </template>

            <!-- 阶段3：已录入诊断，待诊断 -->
            <template v-if="row.trackingStatus === 1 && row.diagnosisFirst && !row.referralResult">
              <el-button
                v-permission="'keyPopulation:latent:referral'"
                type="danger"
                size="small"
                @click="openReferralDialog(row)"
              >
                诊断
              </el-button>
            </template>

            <!-- 阶段4：潜伏感染者（已诊断为 latent） -->
            <template v-if="row.referralResult === 'latent'">
              <el-button v-if="!row.noticeSent" v-permission="'keyPopulation:latent:sendNotice'" type="primary" size="small" @click="openNoticeDialog(row)">
                填写通知单
              </el-button>
              <NoticeSentStatusButton
                v-if="row.noticeSent"
                v-permission="'keyPopulation:latent:sendNotice'"
                :link="false"
              />
              <el-button
                v-if="!row.supervisionCompleted"
                v-permission="'keyPopulation:latent:supervision'"
                size="small"
                :disabled="!row.noticeSent"
                @click="openSupervisionDialog(row)"
              >
                填写督导表
              </el-button>
              <el-button
                v-if="row.supervisionCompleted"
                v-permission="'keyPopulation:latent:supervision'"
                type="success"
                size="small"
                disabled
              >
                督导表已完成
              </el-button>
              <el-button type="info" size="small" @click="viewSupervision(row)">
                查看督导表
              </el-button>
              <el-button
                v-if="row.treatmentPhase === 1 && !row.medicationStatus"
                v-permission="'keyPopulation:latent:supervision'"
                type="warning"
                size="small"
                @click="openMedicationDialog(row)"
              >
                设置服药状态
              </el-button>
              <el-button
                v-if="row.treatmentPhase === 1 && row.medicationStatus"
                v-permission="'keyPopulation:latent:followUp'"
                type="primary"
                size="small"
                @click="openTreatmentDialog(row)"
              >
                治疗管理
              </el-button>
            </template>

            <!-- 通知单查看 -->
            <el-button
              v-if="row.noticeSent"
              type="info"
              link
              size="small"
              @click="viewNotice(row)"
            >
              查看通知单
            </el-button>

            <el-button type="info" size="small" @click="openAggregateDialog(row)">
              信息归集
            </el-button>
            <el-button v-permission="'referral'" type="warning" link size="small" @click="openTierCare(row)">
              转诊
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 筛查详情弹窗 -->
    <ScreeningDetailDialog v-model:visible="screeningDetailVisible" type="keyPopulation" :data="screeningDetailData" />

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="latent_key"
      population-type="key"
      module-type="latent"
      :subject-name="tierCareRow.name || ''"
    />

    <!-- 通知单弹窗 -->
    <el-dialog v-model="noticeDialogVisible" title="填写潜伏感染者通知单" width="680px">
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="noticeFormRules" label-width="110px">
        <el-divider content-position="left">
          基本信息
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input :value="noticeRow?.name" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证" prop="idNumber">
              <el-input v-model="noticeForm.idNumber" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="noticeForm.gender" style="width: 100%">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="noticeForm.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生日期">
              <el-date-picker v-model="noticeForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="联系方式">
              <el-input v-model="noticeForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="民族">
              <el-input v-model="noticeForm.ethnicity" placeholder="如：汉族" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="24">
            <el-form-item label="人群分类">
              <el-select v-model="noticeForm.crowdCategory" style="width: 100%">
                <el-option v-for="item in CROWD_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="24">
            <el-form-item label="现居住地址">
              <el-input v-model="noticeForm.currentAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户籍地址">
              <el-input v-model="noticeForm.householdAddress" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">
          感染检查
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="感染检测时间">
              <el-date-picker v-model="noticeForm.infectionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="感染检查方法">
              <el-select v-model="noticeForm.infectionMethod" style="width: 100%">
                <el-option v-for="item in INFECTION_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检查结果">
              <el-input v-model="noticeForm.infectionResultValue" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">
          胸片检查
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="胸片检查时间">
              <el-date-picker v-model="noticeForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片检查结果">
              <el-select v-model="noticeForm.chestXrayResult" style="width: 100%">
                <el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">
          治疗方案
        </el-divider>
        <el-form-item label="治疗方案">
          <el-select v-model="noticeForm.treatmentPlan" style="width: 100%" placeholder="请选择治疗方案">
            <el-option v-for="item in LATENT_TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isLatentIndividualPlan(noticeForm.treatmentPlan)" label="方案详情">
          <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="3" placeholder="请手动录入个体治疗方案详情" />
        </el-form-item>
        <el-divider content-position="left">
          机构信息
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="治疗机构">
              <el-input v-model="noticeForm.treatmentInstitution" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下发时间">
              <el-input :model-value="noticeForm.issuedTime" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="接收单位" prop="receiverOrgId">
          <el-select v-model="noticeForm.receiverOrgId" placeholder="请选择接收单位（必填）" filterable style="width: 100%">
            <el-option v-for="u in level5Users" :key="u.id" :label="`${u.realName || u.username} - ${u.orgName || '未设置机构'}`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">
          取消
        </el-button>
        <el-button :loading="submitting" @click="handleSaveNoticeDraft">
          保存草稿
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSendNotice">
          发送通知单
        </el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情 -->
    <el-dialog v-model="noticeDetailVisible" title="潜伏感染者通知单详情" width="680px">
      <el-descriptions v-if="noticeDetailData" :column="2" border>
        <el-descriptions-item label="姓名">
          {{ noticeDetailData.patientName }}
        </el-descriptions-item>
        <el-descriptions-item label="身份证">
          {{ noticeDetailData.idNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ noticeDetailData.gender }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ noticeDetailData.age }}
        </el-descriptions-item>
        <el-descriptions-item label="人群分类">
          {{ noticeDetailData.crowdCategory }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">
          {{ normalizeLatentTreatmentPlan(noticeDetailData.treatmentPlan) || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">
          {{ noticeDetailData.currentAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="下发人">
          {{ noticeDetailData.senderName || "-" }}
          <span v-if="noticeDetailData.senderOrgName" class="text-gray-400 ml-1">（{{ noticeDetailData.senderOrgName }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="接收人">
          {{ noticeDetailData.receiverName || "-" }}
          <span v-if="noticeDetailData.receiverOrgName" class="text-gray-400 ml-1">（{{ noticeDetailData.receiverOrgName }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">
          {{ noticeDetailData.sentTime }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="noticeDetailData.status === 2 ? 'success' : 'warning'" size="small">
            {{ NOTICE_STATUS_MAP[noticeDetailData.status] }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button
          v-if="noticeDetailData && noticeDetailData.status === 1 && userStore.userRole === 6"
          v-permission="'keyPopulation:latent:confirmNotice'"
          type="primary"
          @click="handleConfirmNotice(noticeDetailData.id)"
        >
          确认接收
        </el-button>
      </template>
    </el-dialog>

    <SupervisionFormDialog
      v-if="supervisionRow"
      v-model="supervisionDialogVisible"
      :latent-row="supervisionRow"
      @success="fetchData"
    />

    <SupervisionFormDetailDialog
      v-model:visible="supervisionDetailVisible"
      :form-data="supervisionDetailData"
      :patient-name="supervisionRow?.name"
    />

    <!-- 服药状态 -->
    <el-dialog v-model="medicationDialogVisible" title="设置服药状态" width="450px">
      <el-form label-width="100px">
        <el-form-item label="服药状态">
          <el-radio-group v-model="medicationStatusValue">
            <el-radio v-for="item in MEDICATION_STATUS_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medicationDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSetMedication">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 治疗管理 -->
    <el-dialog v-model="treatmentDialogVisible" :title="`预防治疗管理 — ${treatmentRow?.name || ''}`" width="800px">
      <el-tabs>
        <el-tab-pane label="电话随访">
          <div class="mb-3 flex justify-end">
            <el-button type="primary" size="small" v-permission="'keyPopulation:latent:followUp'" @click="openFollowUpForm">
              新增电话随访
            </el-button>
          </div>
          <el-table :data="followUpList" border stripe max-height="300">
            <el-table-column prop="followUpDate" label="随访日期" />
            <el-table-column prop="followUpType" label="随访方式" />
            <el-table-column prop="content" label="随访内容" />
            <el-table-column prop="result" label="随访结果" />
            <el-table-column prop="operator" label="操作人" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按期检查">
          <div class="mb-3 flex justify-end">
            <el-button type="primary" size="small" v-permission="'keyPopulation:latent:check'" @click="openCheckForm">
              新增按期检查
            </el-button>
          </div>
          <el-table :data="checkList" border stripe max-height="300">
            <el-table-column prop="checkDate" label="检查日期" />
            <el-table-column prop="checkPeriod" label="检查周期" />
            <el-table-column prop="checkResult" label="检查结果">
              <template #default="{ row }">
                <el-tag :type="row.checkResult === '未发病' ? 'success' : row.checkResult === '发病' ? 'danger' : 'warning'" size="small">
                  {{ row.checkResult }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="检查详情" />
            <el-table-column prop="operator" label="操作人" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="treatmentDialogVisible = false">
          关闭
        </el-button>
        <el-button
          v-if="treatmentRow?.treatmentPhase === 1"
          v-permission="'keyPopulation:latent:closeCase'"
          type="danger"
          @click="handleCloseCase(treatmentRow)"
        >
          结案归档
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增电话随访 -->
    <el-dialog v-model="followUpFormVisible" title="新增电话随访" width="500px" append-to-body>
      <el-form :model="followUpForm" label-width="80px">
        <el-form-item label="随访日期">
          <el-date-picker v-model="followUpForm.followUpDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="随访内容">
          <el-input v-model="followUpForm.content" type="textarea" :rows="4" placeholder="请填写随访内容" />
        </el-form-item>
        <el-form-item label="随访结果">
          <el-input v-model="followUpForm.result" placeholder="请填写随访结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followUpFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSaveFollowUp">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增按期检查 -->
    <el-dialog v-model="checkFormVisible" title="新增按期检查" width="500px" append-to-body>
      <el-form :model="checkForm" label-width="80px">
        <el-form-item label="检查日期">
          <el-date-picker v-model="checkForm.checkDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="检查周期">
          <el-select v-model="checkForm.checkPeriod" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in CHECK_PERIOD_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查结果">
          <el-select v-model="checkForm.checkResult" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in CHECK_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查详情">
          <el-input v-model="checkForm.content" type="textarea" :rows="3" placeholder="请填写检查详情" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSaveCheck">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 信息归集 -->
    <el-dialog v-model="aggregateDialogVisible" title="潜伏感染者信息归集" width="750px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">
          {{ aggregateRow?.name }}
        </el-descriptions-item>
        <el-descriptions-item label="证件号">
          {{ aggregateRow?.idNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片结果">
          {{ aggregateRow?.chestXrayResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="首次诊断">
          {{ aggregateRow?.diagnosisFirst || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗阶段">
          {{ TREATMENT_PHASE_MAP[aggregateRow?.treatmentPhase] || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        通知单
      </el-divider>
      <el-table :data="aggregateNotices" border stripe size="small" max-height="200">
        <el-table-column prop="receiverName" label="接收人" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row: n }">
            <el-tag :type="n.status === 1 ? 'success' : 'warning'" size="small">
              {{ NOTICE_STATUS_MAP[n.status] || "未知" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" />
      </el-table>

      <el-divider content-position="left">
        督导表
      </el-divider>
      <template v-if="aggregateSupervision">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="类别">
            {{ aggregateSupervision.category || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ aggregateSupervision.gender || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ aggregateSupervision.age ?? "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="电话号码">
            {{ aggregateSupervision.phone || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="电话备注">
            {{ aggregateSupervision.phoneRemark || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="现住址" :span="2">
            {{ aggregateSupervision.currentAddress || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗方案" :span="2">
            {{ normalizeLatentTreatmentPlan(aggregateSupervision.treatmentPlan) || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="开始日期">
            {{ aggregateSupervision.treatmentStartDate || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="完成日期">
            {{ aggregateSupervision.treatmentEndDate || "-" }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 督导记录 -->
        <div class="mt-3 mb-1 text-sm font-bold text-gray-600">
          督导记录
        </div>
        <el-table
          :data="aggregateSupervision.supervisionRecords ? JSON.parse(aggregateSupervision.supervisionRecords) : []"
          border stripe size="small"
        >
          <el-table-column prop="time" label="督导时间" />
          <el-table-column prop="method" label="督导方式" />
          <el-table-column prop="content" label="督导内容" />
          <el-table-column prop="remark" label="备注" />
        </el-table>

        <!-- 全疗程规律治疗评价 -->
        <div class="mt-3 mb-1 text-sm font-bold text-gray-600">
          全疗程规律治疗评价
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="中断用药">
            {{ aggregateSupervision.interruptMedication || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗完成情况">
            {{ aggregateSupervision.treatmentCompletionStatus || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="中断次数">
            {{ aggregateSupervision.interruptCount ?? "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="全程应用药次数">
            {{ aggregateSupervision.totalDoses ?? "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="实际用药次数">
            {{ aggregateSupervision.actualDoses ?? "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="用药率" :span="2">
            {{ aggregateSupervision.medicationRate || "-" }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 督导管理人员 -->
        <div class="mt-3 mb-1 text-sm font-bold text-gray-600">
          督导管理人员
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="管理人员类型">
            {{ aggregateSupervision.managerType || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="管理人员姓名">
            {{ aggregateSupervision.managerName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="归档时间">
            {{ aggregateSupervision.archivedTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ aggregateSupervision.remark || "-" }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="暂无督导表记录" :image-size="60" />

      <el-divider content-position="left">
        电话随访记录
      </el-divider>
      <el-table :data="aggregateFollowUps" border stripe size="small" max-height="200">
        <el-table-column prop="followUpDate" label="日期" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="result" label="结果" />
      </el-table>

      <el-divider content-position="left">
        按期检查记录
      </el-divider>
      <el-table :data="aggregateChecks" border stripe size="small" max-height="200">
        <el-table-column prop="checkDate" label="日期" />
        <el-table-column prop="checkPeriod" label="周期" />
        <el-table-column prop="checkResult" label="结果" />
        <el-table-column prop="content" label="详情" />
      </el-table>

      <template #footer>
        <el-button @click="aggregateDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 追踪弹窗 -->
    <TrackingOperationDialog
      v-model="trackDialogVisible"
      :history-json="trackRow?.trackingHistoryJson"
      :not-in-place-count="trackRow?.notInPlaceCount ?? 0"
      :loading="submitting"
      @confirm="handleTrack"
    />

    <!-- 查看追踪记录 -->
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

    <!-- 录入胸片诊断弹窗 -->
    <el-dialog v-model="xrayDialogVisible" title="录入胸片检查与诊断结果" width="500px">
      <el-form :model="xrayForm" label-width="110px">
        <el-form-item label="是否做胸片">
          <el-radio-group v-model="xrayForm.hasChestXray">
            <el-radio value="是">
              是
            </el-radio>
            <el-radio value="否">
              否
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="xrayForm.hasChestXray === '是'">
          <el-form-item label="胸片检查日期">
            <el-date-picker v-model="xrayForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="胸片检查结果">
            <el-select v-model="xrayForm.chestXrayResult" style="width: 100%" placeholder="请选择">
              <el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="首次诊断结果">
          <el-select v-model="xrayForm.diagnosisFirst" style="width: 100%" placeholder="请选择诊断结果">
            <el-option
              v-for="item in SCREENING_DIAGNOSIS_EDIT_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="xrayDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitXray">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 诊断弹窗 -->
    <el-dialog v-model="referralDialogVisible" title="诊断操作" width="460px">
      <el-form label-width="90px">
        <el-form-item label="诊断结果">
          <el-select v-model="referralResultValue" style="width: 100%" placeholder="请选择诊断结果">
            <el-option v-for="item in REFERRAL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="referralResultValue === 'other' || referralResultValue === 'excluded'" label="备注原因">
          <el-input v-model="referralRemark" type="textarea" :rows="3" placeholder="请填写备注原因" />
        </el-form-item>
        <el-form-item label="转诊时间" required>
          <el-date-picker
            v-model="actualReferralDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择患者真实转诊日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-alert
          v-if="referralResultValue === 'confirmed'"
          title="确认后该记录将标红结案，不进入患者管理"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-alert
          v-if="referralResultValue === 'suspected'"
          title="确认后该记录将保留在待诊断，不进入患者管理"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="referralDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleReferral">
          确认诊断
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-3 {
  margin-bottom: 12px;
}
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
</style>
