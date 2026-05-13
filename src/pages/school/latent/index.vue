<script lang="ts" setup>
import { getLevel5UsersApi } from "@@/apis/users"
import PrintSupervision from "@@/components/PrintSupervision.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import {
  CHECK_PERIOD_OPTIONS,
  CHECK_RESULT_OPTIONS,
  CHEST_XRAY_RESULT_OPTIONS,
  CROWD_CATEGORY_OPTIONS,
  INFECTION_METHOD_OPTIONS,
  INTERRUPT_MEDICATION_OPTIONS,
  MEDICATION_STATUS_OPTIONS,
  NOTICE_STATUS_MAP,
  SUPERVISION_CATEGORY_OPTIONS,
  SUPERVISION_MANAGER_TYPE_OPTIONS,
  SUPERVISION_METHOD_OPTIONS,
  TREATMENT_PHASE_MAP,
  TREATMENT_PLAN_OPTIONS
} from "@@/constants/disease"
import { getToken } from "@@/utils/cache/cookies"
import { idCardRule, phoneRule } from "@@/utils/validate"
import { getScreeningSchoolDetailApi } from "@/pages/school/screening/apis"
import { useUserStore } from "@/pinia/stores/user"
import {
  closeCaseApi,
  confirmNoticeApi,
  getCheckListApi,
  getFollowUpListApi,
  getLatentListApi,
  getNoticeListByBizApi,
  getSupervisionDetailApi,
  referralLatentApi,
  saveCheckApi,
  saveFollowUpApi,
  exportLatentListApi,
  saveSupervisionApi,
  sendNoticeApi,
  setMedicationStatusApi,
  submitXrayApi,
  trackLatentApi
} from "./apis"

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

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  archived: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getLatentListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: "school",
      // 潜伏感染管理仅展示已诊断为"潜伏感染者"的记录；
      // 待追踪 / 待录入诊断 / 待诊断的数据由"待诊断管理"模块负责。
      referralResult: "latent",
      ...searchForm
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
  searchForm.archived = undefined
  handleSearch()
}

const exporting = ref(false)
async function handleExport() {
  try {
    exporting.value = true
    const res = await exportLatentListApi({
      populationType: "school",
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      archived: searchForm.archived
    })
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "学校人群_潜伏感染管理.xlsx"
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
const trackStatus = ref<1 | 2 | 3>(1)
const trackRemark = ref("")

function openTrackDialog(row: any) {
  trackRow.value = row
  trackStatus.value = 1
  trackRemark.value = ""
  trackDialogVisible.value = true
}

async function handleTrack() {
  if (submitting.value) return
  submitting.value = true
  try {
    await trackLatentApi({ id: trackRow.value.id, status: trackStatus.value, remark: trackRemark.value })
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

const REFERRAL_OPTIONS = [
  { label: "排除", value: "excluded" },
  { label: "其他", value: "other" },
  { label: "疑似肺结核", value: "suspected" },
  { label: "确诊患者", value: "confirmed" },
  { label: "潜伏感染者", value: "latent" }
]

function openReferralDialog(row: any) {
  referralRow.value = row
  referralResultValue.value = ""
  referralRemark.value = ""
  referralDialogVisible.value = true
}

async function handleReferral() {
  if (!referralResultValue.value) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await referralLatentApi({ id: referralRow.value.id, result: referralResultValue.value, remark: referralRemark.value })
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
  receiverOrgId: undefined as number | undefined
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
    treatmentPlan: "",
    customPlanDetail: "",
    treatmentInstitution: "",
    issuedTime: getNowDateStr(),
    receiverOrgId: undefined
  })
  noticeDialogVisible.value = true
}

async function handleSendNotice() {
  if (submitting.value) return
  const valid = await noticeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    noticeForm.issuedTime = getNowDateStr()
    await sendNoticeApi({
      noticeType: "latent",
      populationType: "school",
      bizId: noticeRow.value.id,
      patientName: noticeRow.value.name,
      ...noticeForm,
      treatmentPlan: noticeForm.treatmentPlan === "个体化方案" ? noticeForm.customPlanDetail : noticeForm.treatmentPlan,
      senderId: userStore.userId
    })
    ElMessage.success("通知单发送成功")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 确认接收通知单 ====================
async function handleConfirmNotice(noticeId: number) {
  try {
    await ElMessageBox.confirm("确认接收此通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收")
    noticeDetailVisible.value = false
    fetchData()
  } catch { /* cancelled or handled */ }
}

// ==================== 通知单详情查看 ====================
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

// ==================== 督导表弹窗 ====================
const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)

/** 附件上传列表 */
const attachmentFileList = ref<{ name: string, url: string }[]>([])
const uploadAction = `${import.meta.env.VITE_BASE_URL}/file/upload`
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))

function beforeAttachmentUpload(file: File) {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error("附件大小不能超过 20MB")
    return false
  }
  return true
}

function handleAttachmentSuccess(response: any, uploadFile: any) {
  if (response.code === 200) {
    attachmentFileList.value.push({ name: uploadFile.name, url: import.meta.env.VITE_BASE_URL + response.data })
  } else {
    ElMessage.error(response.msg || "附件上传失败")
  }
}

function handleAttachmentRemove(uploadFile: { name: string }) {
  attachmentFileList.value = attachmentFileList.value.filter((f: { name: string, url: string }) => f.name !== uploadFile.name)
}

function handleAttachmentError() {
  ElMessage.error("附件上传失败，请重试")
}

/** 从 URL 中提取原始文件名（取 name 查询参数），兜底返回序号标签 */
function getAttachmentLabel(url: string, index: number | string): string {
  try {
    const match = url.match(/[?&]name=([^&]+)/)
    if (match) return decodeURIComponent(match[1])
  } catch { /* ignore */ }
  return `附件${Number(index) + 1}`
}

const supervisionForm = reactive({
  category: "",
  gender: "",
  age: null as number | null,
  phone: "",
  currentAddress: "",
  treatmentStartDate: "",
  treatmentEndDate: "",
  treatmentPlan: "",
  customPlanDetail: "",
  supervisionRecords: [] as { time: string, content: string, method: string, remark: string }[],
  interruptMedication: "",
  interruptCount: null as number | null,
  totalDoses: null as number | null,
  actualDoses: null as number | null,
  medicationRate: "",
  managerType: "",
  managerName: "",
  remark: "",
  attachmentUrls: ""
})

function openSupervisionDialog(row: any) {
  supervisionRow.value = row
  supervisionForm.category = row.crowdCategory || ""
  supervisionForm.gender = row.gender || ""
  supervisionForm.age = row.age || null
  supervisionForm.phone = row.phone || ""
  supervisionForm.currentAddress = row.currentAddress || ""
  supervisionForm.treatmentStartDate = ""
  supervisionForm.treatmentEndDate = ""
  supervisionForm.treatmentPlan = ""
  supervisionForm.customPlanDetail = ""
  supervisionForm.supervisionRecords = [{ time: "", content: "", method: "", remark: "" }]
  supervisionForm.interruptMedication = ""
  supervisionForm.interruptCount = null
  supervisionForm.totalDoses = null
  supervisionForm.actualDoses = null
  supervisionForm.medicationRate = ""
  supervisionForm.managerType = ""
  supervisionForm.managerName = ""
  supervisionForm.remark = ""
  supervisionForm.attachmentUrls = ""
  attachmentFileList.value = []
  supervisionDialogVisible.value = true
}

async function handleSaveSupervision() {
  if (submitting.value) return
  submitting.value = true
  try {
    // 自动计算用药率
    let rate = supervisionForm.medicationRate
    if (!rate && supervisionForm.totalDoses && supervisionForm.actualDoses !== null && supervisionForm.totalDoses > 0) {
      rate = `${((supervisionForm.actualDoses / supervisionForm.totalDoses) * 100).toFixed(1)}%`
    }
    const attachmentUrls = attachmentFileList.value.map((f: { name: string, url: string }) => f.url).join(",")
    await saveSupervisionApi({
      latentInfectionId: supervisionRow.value.id,
      populationType: "school",
      patientName: supervisionRow.value.name,
      category: supervisionForm.category || undefined,
      gender: supervisionForm.gender || undefined,
      age: supervisionForm.age || undefined,
      phone: supervisionForm.phone || undefined,
      currentAddress: supervisionForm.currentAddress || undefined,
      treatmentStartDate: supervisionForm.treatmentStartDate,
      treatmentEndDate: supervisionForm.treatmentEndDate || undefined,
      treatmentPlan: supervisionForm.treatmentPlan === "个体化方案"
        ? supervisionForm.customPlanDetail
          ? `个体化方案：${supervisionForm.customPlanDetail}`
          : "个体化方案"
        : supervisionForm.treatmentPlan,
      supervisionRecords: supervisionForm.supervisionRecords.length > 0
        ? JSON.stringify(supervisionForm.supervisionRecords)
        : undefined,
      interruptMedication: supervisionForm.interruptMedication || undefined,
      interruptCount: supervisionForm.interruptMedication === "有" ? supervisionForm.interruptCount : undefined,
      totalDoses: supervisionForm.totalDoses || undefined,
      actualDoses: supervisionForm.actualDoses || undefined,
      medicationRate: rate || undefined,
      managerType: supervisionForm.managerType || undefined,
      managerName: supervisionForm.managerName || undefined,
      remark: supervisionForm.remark || undefined,
      attachmentUrls: attachmentUrls || undefined,
      status: 2
    })
    ElMessage.success("督导表保存成功")
    supervisionDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 督导表查看 ====================
const supervisionDetailVisible = ref(false)
const supervisionDetailData = ref<any>(null)
const supervisionPrintVisible = ref(false)

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

// ==================== 服药状态设置 ====================
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

// ==================== 治疗管理弹窗（电话随访 + 按期检查） ====================
const treatmentDialogVisible = ref(false)
const treatmentRow = ref<any>(null)
const followUpList = ref<any[]>([])
const checkList = ref<any[]>([])

async function openTreatmentDialog(row: any) {
  treatmentRow.value = row
  treatmentDialogVisible.value = true
  await Promise.all([loadFollowUps(row.id), loadChecks(row.id)])
}

async function loadFollowUps(latentId: number) {
  try {
    const { data } = await getFollowUpListApi(latentId)
    followUpList.value = data || []
  } catch { /* handled */ }
}

async function loadChecks(latentId: number) {
  try {
    const { data } = await getCheckListApi(latentId)
    checkList.value = data || []
  } catch { /* handled */ }
}

const followUpFormVisible = ref(false)
const followUpForm = reactive({ followUpDate: "", followUpType: "电话随访", content: "", result: "" })

function openFollowUpForm() {
  followUpForm.followUpDate = ""
  followUpForm.content = ""
  followUpForm.result = ""
  followUpFormVisible.value = true
}

async function handleSaveFollowUp() {
  try {
    await saveFollowUpApi({
      latentInfectionId: treatmentRow.value.id,
      followUpDate: followUpForm.followUpDate,
      followUpType: followUpForm.followUpType,
      content: followUpForm.content,
      result: followUpForm.result,
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
  checkForm.checkDate = ""
  checkForm.checkPeriod = ""
  checkForm.checkResult = ""
  checkForm.content = ""
  checkFormVisible.value = true
}

async function handleSaveCheck() {
  try {
    await saveCheckApi({
      latentInfectionId: treatmentRow.value.id,
      checkDate: checkForm.checkDate,
      checkPeriod: checkForm.checkPeriod,
      checkResult: checkForm.checkResult,
      content: checkForm.content,
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
    const { data } = await getScreeningSchoolDetailApi(row.screeningId)
    if (data) {
      screeningDetailData.value = data
      screeningDetailVisible.value = true
    } else {
      ElMessage.info("暂无筛查原始数据")
    }
  } catch { /* handled by interceptor */ }
}

// ==================== 结案归档 ====================
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
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
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

    <!-- 数据表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">学校人群 — 潜伏感染管理</span>
          <el-button type="success" :loading="exporting" @click="handleExport">
            导出 Excel
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" />
        <el-table-column prop="chestXrayResult" label="胸片结果" />
        <el-table-column prop="diagnosisFirst" label="诊断结果" />
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
        <el-table-column label="当前阶段" fixed="right" width="110">
          <template #default="{ row }">
            <el-tag :type="getStageInfo(row).type" size="small">
              {{ getStageInfo(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="400">
          <template #default="{ row }">
            <el-button type="info" link size="small" @click="viewScreeningDetail(row)">
              查看详情
            </el-button>

            <!-- 阶段1：待追踪 -->
            <template v-if="row.trackingStatus === 0 && !row.archived">
              <el-button
                v-permission="'latent:track'"
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
                v-permission="'latent:xray'"
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
                v-permission="'latent:referral'"
                type="danger"
                size="small"
                @click="openReferralDialog(row)"
              >
                诊断
              </el-button>
            </template>

            <!-- 阶段4：潜伏感染者（已诊断为 latent） -->
            <template v-if="row.referralResult === 'latent'">
              <!-- 通知单 -->
              <el-button
                v-if="!row.noticeSent"
                v-permission="'latent:sendNotice'"
                type="primary"
                size="small"
                @click="openNoticeDialog(row)"
              >
                填写通知单
              </el-button>
              <el-button
                v-if="row.noticeSent"
                v-permission="'latent:sendNotice'"
                type="success"
                size="small"
                disabled
              >
                已发送通知单
              </el-button>
              <!-- 督导表 -->
              <el-button
                v-if="!row.supervisionCompleted"
                v-permission="'latent:supervision'"
                size="small"
                :disabled="!row.noticeSent"
                @click="openSupervisionDialog(row)"
              >
                填写督导表
              </el-button>
              <el-button
                v-if="row.supervisionCompleted"
                v-permission="'latent:supervision'"
                type="success"
                size="small"
                disabled
              >
                督导表已完成
              </el-button>
              <el-button
                type="info"
                size="small"
                @click="viewSupervision(row)"
              >
                查看督导表
              </el-button>
              <!-- 服药状态 -->
              <el-button
                v-if="row.treatmentPhase === 1 && !row.medicationStatus"
                v-permission="'latent:supervision'"
                type="warning"
                size="small"
                @click="openMedicationDialog(row)"
              >
                设置服药状态
              </el-button>
              <!-- 治疗管理 -->
              <el-button
                v-if="row.treatmentPhase === 1 && row.medicationStatus"
                v-permission="'latent:followUp'"
                type="primary"
                size="small"
                @click="openTreatmentDialog(row)"
              >
                治疗管理
              </el-button>
            </template>

            <!-- 通知单查看（所有有通知单的记录） -->
            <el-button
              v-if="row.noticeSent"
              type="info"
              link
              size="small"
              @click="viewNotice(row)"
            >
              查看通知单
            </el-button>

            <!-- 信息归集 -->
            <el-button
              type="info"
              size="small"
              @click="openAggregateDialog(row)"
            >
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
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 筛查详情弹窗 -->
    <ScreeningDetailDialog v-model:visible="screeningDetailVisible" type="school" :data="screeningDetailData" />

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="latent_school"
      population-type="school"
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
              <el-input v-model="noticeForm.currentAddress" placeholder="请输入现居住地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户籍地址">
              <el-input v-model="noticeForm.householdAddress" placeholder="请输入户籍地址" />
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
            <el-form-item label="检查方法">
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
            <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="noticeForm.treatmentPlan === '个体化方案'" label="方案详情">
          <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="3" placeholder="请注明详细的抗结核治疗方案" />
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
        <el-button type="primary" :loading="submitting" @click="handleSendNotice">
          发送通知单
        </el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情弹窗 -->
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
        <el-descriptions-item label="联系方式">
          {{ noticeDetailData.phone || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="民族">
          {{ noticeDetailData.ethnicity || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="人群分类">
          {{ noticeDetailData.crowdCategory }}
        </el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">
          {{ noticeDetailData.currentAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">
          {{ noticeDetailData.householdAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染检测时间">
          {{ noticeDetailData.infectionDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="检查方法">
          {{ noticeDetailData.infectionMethod || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染检查结果" :span="2">
          {{ noticeDetailData.infectionResultValue || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查时间">
          {{ noticeDetailData.chestXrayDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查结果">
          {{ noticeDetailData.chestXrayResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">
          {{ noticeDetailData.treatmentPlan || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗机构">
          {{ noticeDetailData.treatmentInstitution || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="下发时间">
          {{ noticeDetailData.issuedTime || "-" }}
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
        <el-descriptions-item v-if="noticeDetailData.confirmedTime" label="确认时间">
          {{ noticeDetailData.confirmedTime }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button
          v-if="noticeDetailData && noticeDetailData.status === 1 && userStore.userRole === 6"
          v-permission="'latent:confirmNotice'"
          type="primary"
          @click="handleConfirmNotice(noticeDetailData.id)"
        >
          确认接收
        </el-button>
      </template>
    </el-dialog>

    <!-- 督导表填写弹窗 -->
    <el-dialog v-model="supervisionDialogVisible" title="填写预防性治疗督导表" width="960px">
      <el-form :model="supervisionForm" label-width="140px">
        <el-divider content-position="left">
          基本信息
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input :value="supervisionRow?.name" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别">
              <el-select v-model="supervisionForm.category" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="item in SUPERVISION_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="supervisionForm.gender" placeholder="请选择" clearable style="width: 100%">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="supervisionForm.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电话号码">
              <el-input v-model="supervisionForm.phone" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="现住址">
          <el-input v-model="supervisionForm.currentAddress" placeholder="请输入现住址" />
        </el-form-item>

        <el-divider content-position="left">
          治疗方案
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始治疗时间">
              <el-date-picker v-model="supervisionForm.treatmentStartDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="治疗方案">
              <el-select v-model="supervisionForm.treatmentPlan" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="supervisionForm.treatmentPlan === '个体化方案'">
          <el-col :span="24">
            <el-form-item label="方案详情">
              <el-input v-model="supervisionForm.customPlanDetail" type="textarea" :rows="3" placeholder="请注明详细的抗结核治疗方案" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          督导记录
        </el-divider>
        <div v-for="(record, index) in supervisionForm.supervisionRecords" :key="index" class="mb-3 border rounded p-3">
          <el-row :gutter="8">
            <el-col :span="8">
              <el-form-item :label="`督导时间${index + 1}`" label-width="90px">
                <el-date-picker v-model="record.time" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="督导方式" label-width="80px">
                <el-select v-model="record.method" placeholder="请选择" clearable style="width: 100%">
                  <el-option v-for="item in SUPERVISION_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="flex items-center justify-end">
              <el-button v-if="supervisionForm.supervisionRecords.length > 1" type="danger" link size="small" @click="supervisionForm.supervisionRecords.splice(index, 1)">
                删除
              </el-button>
            </el-col>
          </el-row>
          <el-form-item label="督导内容" label-width="90px">
            <el-input v-model="record.content" type="textarea" :rows="2" placeholder="请填写督导内容" />
          </el-form-item>
          <el-form-item label="备注" label-width="90px">
            <el-input v-model="record.remark" placeholder="请填写备注" />
          </el-form-item>
        </div>
        <div class="mb-4">
          <el-button type="primary" link @click="supervisionForm.supervisionRecords.push({ time: '', content: '', method: '', remark: '' })">
            + 添加督导记录
          </el-button>
        </div>

        <el-divider content-position="left">
          全疗程规律治疗评价
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="中断用药">
              <el-radio-group v-model="supervisionForm.interruptMedication">
                <el-radio v-for="item in INTERRUPT_MEDICATION_OPTIONS" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="中断次数">
              <el-input-number v-model="supervisionForm.interruptCount" :min="0" :disabled="supervisionForm.interruptMedication !== '有'" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="全程应用药次数">
              <el-input-number v-model="supervisionForm.totalDoses" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="实际用药次数">
              <el-input-number v-model="supervisionForm.actualDoses" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="用药率">
              <el-input v-model="supervisionForm.medicationRate" placeholder="自动计算或手动填写" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="结束疗程时间">
          <el-date-picker v-model="supervisionForm.treatmentEndDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>

        <el-divider content-position="left">
          督导管理人员
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="管理人员类型">
              <el-select v-model="supervisionForm.managerType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="item in SUPERVISION_MANAGER_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理人员姓名">
              <el-input v-model="supervisionForm.managerName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          其他
        </el-divider>
        <el-form-item label="备注">
          <el-input v-model="supervisionForm.remark" type="textarea" :rows="3" placeholder="请填写备注" />
        </el-form-item>
        <el-form-item label="附件上传">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :file-list="attachmentFileList"
            :before-upload="beforeAttachmentUpload"
            :on-success="handleAttachmentSuccess"
            :on-remove="handleAttachmentRemove"
            :on-error="handleAttachmentError"
            multiple
          >
            <el-button type="primary" size="small">
              <el-icon class="mr-1">
                <Upload />
              </el-icon>
              点击上传
            </el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持图片、PDF 等格式，单个文件不超过 20MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supervisionDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveSupervision">
          保存督导表
        </el-button>
      </template>
    </el-dialog>

    <!-- 督导表详情弹窗 -->
    <el-dialog v-model="supervisionDetailVisible" title="督导表详情" width="780px" destroy-on-close>
      <el-descriptions v-if="supervisionDetailData" :column="2" border>
        <el-descriptions-item label="姓名">
          {{ supervisionDetailData.patientName }}
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          {{ supervisionDetailData.category || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ supervisionDetailData.gender || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ supervisionDetailData.age ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="电话号码">
          {{ supervisionDetailData.phone || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="现住址">
          {{ supervisionDetailData.currentAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗方案">
          {{ supervisionDetailData.treatmentPlan || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="开始治疗时间">
          {{ supervisionDetailData.treatmentStartDate || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider v-if="supervisionDetailData?.supervisionRecords" content-position="left">
        督导记录
      </el-divider>
      <el-table v-if="supervisionDetailData?.supervisionRecords" :data="JSON.parse(supervisionDetailData.supervisionRecords)" border stripe size="small" class="mb-4">
        <el-table-column prop="time" label="督导时间" width="120" />
        <el-table-column prop="method" label="督导方式" width="100" />
        <el-table-column prop="content" label="督导内容" />
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <el-descriptions v-if="supervisionDetailData" :column="2" border class="mb-4">
        <el-descriptions-item label="中断用药">
          {{ supervisionDetailData.interruptMedication || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="中断次数">
          {{ supervisionDetailData.interruptCount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="全程应用药次数">
          {{ supervisionDetailData.totalDoses ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="实际用药次数">
          {{ supervisionDetailData.actualDoses ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="用药率">
          {{ supervisionDetailData.medicationRate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="结束疗程时间">
          {{ supervisionDetailData.treatmentEndDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="管理人员类型">
          {{ supervisionDetailData.managerType || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="管理人员姓名">
          {{ supervisionDetailData.managerName || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions v-if="supervisionDetailData" :column="1" border>
        <el-descriptions-item label="备注">
          {{ supervisionDetailData.remark || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="附件">
          <template v-if="supervisionDetailData.attachmentUrls">
            <div v-for="(url, i) in supervisionDetailData.attachmentUrls.split(',')" :key="url">
              <a :href="url" target="_blank" rel="noopener noreferrer" class="text-blue-500 underline break-all">
                {{ getAttachmentLabel(url, i) }}
              </a>
            </div>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="supervisionDetailData.status === 2 ? 'success' : 'info'" size="small">
            {{ supervisionDetailData.status === 2 ? "已归档" : "进行中" }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="supervisionDetailVisible = false">
          关闭
        </el-button>
        <el-button type="primary" @click="supervisionPrintVisible = true">
          打印 / 保存PDF
        </el-button>
      </template>
    </el-dialog>

    <!-- 督导表打印预览 -->
    <PrintSupervision v-model:visible="supervisionPrintVisible" :data="supervisionDetailData" />

    <!-- 服药状态设置弹窗 -->
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

    <!-- 治疗管理弹窗 -->
    <el-dialog v-model="treatmentDialogVisible" :title="`预防治疗管理 — ${treatmentRow?.name || ''}`" width="800px">
      <el-descriptions :column="3" border class="mb-4">
        <el-descriptions-item label="姓名">
          {{ treatmentRow?.name }}
        </el-descriptions-item>
        <el-descriptions-item label="服药状态">
          {{ MEDICATION_STATUS_OPTIONS.find(o => o.value === treatmentRow?.medicationStatus)?.label || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗阶段">
          {{ TREATMENT_PHASE_MAP[treatmentRow?.treatmentPhase] || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs>
        <el-tab-pane label="电话随访">
          <div class="mb-3 flex justify-end">
            <el-button type="primary" size="small" v-permission="'latent:followUp'" @click="openFollowUpForm">
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
            <el-button type="primary" size="small" v-permission="'latent:check'" @click="openCheckForm">
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
          v-permission="'latent:closeCase'"
          type="danger"
          @click="handleCloseCase(treatmentRow)"
        >
          结案归档
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增电话随访弹窗 -->
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

    <!-- 新增按期检查弹窗 -->
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

    <!-- 信息归集汇总弹窗 -->
    <el-dialog v-model="aggregateDialogVisible" title="潜伏感染者信息归集" width="750px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">
          {{ aggregateRow?.name }}
        </el-descriptions-item>
        <el-descriptions-item label="证件号">
          {{ aggregateRow?.idNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ aggregateRow?.gender }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ aggregateRow?.age }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          {{ aggregateRow?.phone }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查结果">
          {{ aggregateRow?.infectionResult }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查">
          {{ aggregateRow?.hasChestXray || "-" }}
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
        <el-table-column prop="bizType" label="类型" width="120" />
        <el-table-column prop="receiverName" label="接收人" width="120" />
        <el-table-column prop="status" label="状态" width="100">
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
          <el-descriptions-item label="现住址" :span="2">
            {{ aggregateSupervision.currentAddress || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗方案" :span="2">
            {{ aggregateSupervision.treatmentPlan || "-" }}
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
          <el-table-column prop="time" label="督导时间" width="120" />
          <el-table-column prop="method" label="督导方式" width="100" />
          <el-table-column prop="content" label="督导内容" />
          <el-table-column prop="remark" label="备注" width="130" />
        </el-table>

        <!-- 全疗程规律治疗评价 -->
        <div class="mt-3 mb-1 text-sm font-bold text-gray-600">
          全疗程规律治疗评价
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="中断用药">
            {{ aggregateSupervision.interruptMedication || "-" }}
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
        <el-table-column prop="followUpDate" label="日期" width="120" />
        <el-table-column prop="followUpType" label="方式" width="100" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="result" label="结果" width="120" />
      </el-table>

      <el-divider content-position="left">
        按期检查记录
      </el-divider>
      <el-table :data="aggregateChecks" border stripe size="small" max-height="200">
        <el-table-column prop="checkDate" label="日期" width="120" />
        <el-table-column prop="checkPeriod" label="周期" width="100" />
        <el-table-column prop="checkResult" label="结果" width="100" />
        <el-table-column prop="content" label="详情" />
      </el-table>

      <template #footer>
        <el-button @click="aggregateDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 追踪弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="460px">
      <el-form label-width="90px">
        <el-form-item label="追踪结果">
          <el-radio-group v-model="trackStatus">
            <el-radio :value="1">到位</el-radio>
            <el-radio :value="2">未到位</el-radio>
            <el-radio :value="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="trackStatus === 2 || trackStatus === 3" label="备注原因">
          <el-input v-model="trackRemark" type="textarea" :rows="3" placeholder="请填写备注原因" />
        </el-form-item>
        <el-alert
          v-if="trackRow && trackRow.notInPlaceCount >= 2 && trackStatus === 2"
          title="注意：已连续未到位 2 次，再次未到位将强制终止追踪流程"
          type="warning"
          :closable="false"
          show-icon
          class="mb-3"
        />
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleTrack">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 录入胸片诊断弹窗 -->
    <el-dialog v-model="xrayDialogVisible" title="录入胸片检查与诊断结果" width="500px">
      <el-form :model="xrayForm" label-width="110px">
        <el-form-item label="是否做胸片">
          <el-radio-group v-model="xrayForm.hasChestXray">
            <el-radio value="是">是</el-radio>
            <el-radio value="否">否</el-radio>
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
            <el-option label="排除" value="排除" />
            <el-option label="疑似肺结核" value="疑似肺结核" />
            <el-option label="潜伏感染者" value="潜伏感染者" />
            <el-option label="确诊患者" value="确诊患者" />
            <el-option label="其他" value="其他" />
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
        <el-alert
          v-if="referralResultValue === 'confirmed' || referralResultValue === 'suspected'"
          :title="`确认后该记录将进入患者管理模块（${referralResultValue === 'confirmed' ? '确诊患者' : '疑似肺结核'}）`"
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
