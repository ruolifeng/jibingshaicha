<script lang="ts" setup>
import { getLevel5UsersApi } from "@@/apis/users"
import AttachmentPreviewList from "@@/components/AttachmentPreviewList.vue"
import ImageUploader from "@@/components/ImageUploader.vue"
import PrintSupervision from "@@/components/PrintSupervision.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
/**
 * 密接人群 — 潜伏感染者管理
 *
 * 业务流程（基于 finalScreeningResult / ccStatus）：
 * ① 活动性肺结核  → 标红结案（不进入患者管理，此页不展示）
 * ② 潜伏感染者    → 是否进行预防治疗？
 *    - 是 → 填写督导表 + 设置预计完成治疗时间 → 到期确认（完成→归档，未完成→随访监测）
 *    - 否 → 进入监测随访流程（见待诊断-监测随访页）
 *
 * 注：未做/未发现异常两类人群的随访监测已移至【待诊断-监测随访】页面管理
 */
import { usePagination } from "@@/composables/usePagination"
import {
  CHEST_XRAY_RESULT_OPTIONS,
  formatLatentNoticeTreatmentPlan,
  formatLatentSupervisionTreatmentPlan,
  INFECTION_METHOD_OPTIONS,
  INTERRUPT_MEDICATION_OPTIONS,
  isLatentIndividualPlan,
  LATENT_TREATMENT_PLAN_OPTIONS,
  normalizeLatentTreatmentPlan,
  NOTICE_STATUS_MAP,
  parseLatentNoticeTreatmentPlan,
  parseLatentSupervisionTreatmentPlan,
  SUPERVISION_CATEGORY_OPTIONS,
  SUPERVISION_MANAGER_TYPE_OPTIONS,
  SUPERVISION_METHOD_OPTIONS,
  TREATMENT_COMPLETION_STATUS_OPTIONS
} from "@@/constants/disease"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { idCardRule } from "@@/utils/validate"
import {
  confirmTreatmentApi,
  getScreeningCloseContactDetailApi,
  getScreeningCloseContactListApi,
  setExpectedEndDateApi,
  updateScreeningCloseContactApi
} from "@/pages/close-contact/screening/apis"
import {
  confirmNoticeApi,
  getNoticeListByBizApi,
  getSupervisionDetailApi,
  saveSupervisionApi,
  sendNoticeApi
} from "@/pages/school/latent/apis"
import { useUserStore } from "@/pinia/stores/user"

const userStore = useUserStore()
const level5Users = ref<any[]>([])

onMounted(async () => {
  try {
    const { data } = await getLevel5UsersApi()
    level5Users.value = data || []
  } catch { /* ignore */ }
})

// ==================== 转诊 ====================
const tierCareVisible = ref(false)
const tierCareRow = ref<any>(null)
function openTierCare(row: any) {
  tierCareRow.value = row
  tierCareVisible.value = true
}

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({ name: "", idNumber: "", phone: "", dateRange: [] as string[] })

/** ccStatus 描述 */
const CC_STATUS_MAP: Record<number, { label: string, type: string }> = {
  2: { label: "管理中", type: "warning" },
  3: { label: "已归档", type: "success" },
  4: { label: "随访监测中", type: "warning" },
  5: { label: "随访监测归档", type: "info" },
  6: { label: "待3月复查", type: "warning" },
  7: { label: "3月复查阴性-结束", type: "success" },
  8: { label: "3月复查阳性", type: "danger" }
}

function getFollowupTag(result: string | undefined): string {
  if (!result) return "info"
  if (result.includes("活动性肺结核")) return "danger"
  if (result.includes("潜伏感染者")) return "warning"
  return "success"
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getScreeningCloseContactListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      finalScreeningResult: "潜伏感染者",
      ...extractDateRangeParams(searchForm.dateRange)
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
  handleSearch()
}

watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData, { immediate: true })

const submitting = ref(false)

// ==================== 预防治疗决策 ====================
const treatmentDecisionVisible = ref(false)
const treatmentDecisionRow = ref<any>(null)
const treatmentDecision = ref<"开展" | "未开展">("开展")

function openTreatmentDecision(row: any) {
  treatmentDecisionRow.value = row
  treatmentDecision.value = row.hasPreventiveTreatment === "开展" ? "开展" : "未开展"
  treatmentDecisionVisible.value = true
}

async function handleTreatmentDecision() {
  if (submitting.value) return
  submitting.value = true
  try {
    await updateScreeningCloseContactApi(treatmentDecisionRow.value.id, {
      ...treatmentDecisionRow.value,
      hasPreventiveTreatment: treatmentDecision.value,
      // 若未开展则 ccStatus=4（进入随访监测）
      ccStatus: treatmentDecision.value === "未开展" ? 4 : treatmentDecisionRow.value.ccStatus
    })
    ElMessage.success("已保存预防治疗决策")
    treatmentDecisionVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

// ==================== 设置预计完成时间 ====================
const expectedDateVisible = ref(false)
const expectedDateRow = ref<any>(null)
const expectedDateValue = ref("")

function openExpectedDateDialog(row: any) {
  expectedDateRow.value = row
  expectedDateValue.value = row.expectedTreatmentEndDate || ""
  expectedDateVisible.value = true
}

async function handleSaveExpectedDate() {
  if (!expectedDateValue.value) {
    ElMessage.warning("请选择预计完成时间")
    return
  }
  submitting.value = true
  try {
    await setExpectedEndDateApi(expectedDateRow.value.id, expectedDateValue.value)
    ElMessage.success("预计完成时间已设置")
    expectedDateVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

// ==================== 确认治疗完成/未完成 ====================
const confirmTreatmentVisible = ref(false)
const confirmTreatmentRow = ref<any>(null)

function openConfirmTreatment(row: any) {
  confirmTreatmentRow.value = row
  confirmTreatmentVisible.value = true
}

async function handleConfirmTreatment(done: boolean) {
  submitting.value = true
  try {
    await confirmTreatmentApi(confirmTreatmentRow.value.id, done)
    ElMessage.success(done ? "已标记完成，数据已归档" : "已标记未完成，进入随访监测")
    confirmTreatmentVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

// ==================== 督导表 ====================
const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)

const supervisionForm = reactive({
  category: "",
  gender: "",
  age: null as number | null,
  phone: "",
  phoneRemark: "",
  currentAddress: "",
  treatmentStartDate: "",
  treatmentEndDate: "",
  treatmentPlan: "",
  customPlanDetail: "",
  supervisionRecords: [] as { time: string, content: string, method: string, remark: string }[],
  treatmentCompletionStatus: "",
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
  supervisionForm.category = "密接"
  supervisionForm.gender = row.gender || ""
  supervisionForm.age = row.age || null
  supervisionForm.phone = row.phone || ""
  supervisionForm.phoneRemark = ""
  supervisionForm.currentAddress = row.currentAddress || ""
  supervisionForm.treatmentStartDate = ""
  supervisionForm.treatmentEndDate = ""
  const parsedPlan = parseLatentSupervisionTreatmentPlan(row.preventivePlan || "")
  supervisionForm.treatmentPlan = parsedPlan.treatmentPlan
  supervisionForm.customPlanDetail = parsedPlan.customPlanDetail
  supervisionForm.supervisionRecords = [{ time: "", content: "", method: "", remark: "" }]
  supervisionForm.treatmentCompletionStatus = ""
  supervisionForm.interruptMedication = ""
  supervisionForm.interruptCount = null
  supervisionForm.totalDoses = null
  supervisionForm.actualDoses = null
  supervisionForm.medicationRate = ""
  supervisionForm.managerType = ""
  supervisionForm.managerName = ""
  supervisionForm.remark = ""
  supervisionForm.attachmentUrls = ""
  supervisionDialogVisible.value = true
  getSupervisionDetailApi(row.id).then(({ data }) => {
    if (data?.phoneRemark) supervisionForm.phoneRemark = data.phoneRemark
  }).catch(() => {})
}

async function handleSaveSupervision() {
  if (submitting.value) return
  submitting.value = true
  try {
    let rate = supervisionForm.medicationRate
    if (!rate && supervisionForm.totalDoses && supervisionForm.actualDoses !== null) {
      rate = `${((supervisionForm.actualDoses / supervisionForm.totalDoses) * 100).toFixed(1)}%`
    }
    // 密接潜伏感染者的督导表关联到 screening_close_contact.id（作为 latentInfectionId 存入）
    await saveSupervisionApi({
      latentInfectionId: supervisionRow.value.id,
      populationType: "closeContact",
      patientName: supervisionRow.value.name,
      category: supervisionForm.category || undefined,
      gender: supervisionForm.gender || undefined,
      age: supervisionForm.age || undefined,
      phone: supervisionForm.phone || undefined,
      phoneRemark: supervisionForm.phoneRemark || undefined,
      currentAddress: supervisionForm.currentAddress || undefined,
      treatmentStartDate: supervisionForm.treatmentStartDate,
      treatmentEndDate: supervisionForm.treatmentEndDate || undefined,
      treatmentPlan: formatLatentSupervisionTreatmentPlan(supervisionForm.treatmentPlan, supervisionForm.customPlanDetail),
      supervisionRecords: JSON.stringify(supervisionForm.supervisionRecords),
      treatmentCompletionStatus: supervisionForm.treatmentCompletionStatus || undefined,
      interruptMedication: supervisionForm.interruptMedication || undefined,
      interruptCount: supervisionForm.interruptMedication === "有" ? supervisionForm.interruptCount : undefined,
      totalDoses: supervisionForm.totalDoses || undefined,
      actualDoses: supervisionForm.actualDoses || undefined,
      medicationRate: rate || undefined,
      managerType: supervisionForm.managerType || undefined,
      managerName: supervisionForm.managerName || undefined,
      remark: supervisionForm.remark || undefined,
      attachmentUrls: supervisionForm.attachmentUrls || undefined,
      status: 2
    })
    ElMessage.success("督导表保存成功")
    supervisionDialogVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

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
  } catch { /* handled */ }
}

// ==================== 通知单 ====================
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const noticeFormRef = ref()
const noticeFormRules = {
  receiverOrgId: [{ required: true, message: "请选择接收单位", trigger: "change" }],
  idNumber: [idCardRule()]
}
const noticeForm = reactive({
  idNumber: "",
  gender: "",
  birthDate: "",
  age: null as number | null,
  ethnicity: "",
  phone: "",
  crowdCategory: "密接",
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
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`
}

function openNoticeDialog(row: any) {
  noticeRow.value = row
  const parsedPlan = parseLatentNoticeTreatmentPlan(row.preventivePlan || "")
  Object.assign(noticeForm, {
    idNumber: row.idNumber || "",
    gender: row.gender || "",
    birthDate: row.birthDate || "",
    age: row.age || null,
    phone: row.phone || "",
    ethnicity: row.ethnicity || "",
    crowdCategory: "密接",
    currentAddress: row.currentAddress || "",
    householdAddress: row.householdAddress || "",
    infectionDate: row.infectionCheckDate || "",
    infectionMethod: row.infectionCheckMethod || "",
    infectionResultValue: row.infectionCheckResult || "",
    chestXrayDate: row.imagingDate || "",
    chestXrayResult: row.imagingResult || "",
    treatmentPlan: parsedPlan.treatmentPlan,
    customPlanDetail: parsedPlan.customPlanDetail,
    treatmentInstitution: "",
    issuedTime: getNowDateStr(),
    receiverOrgId: undefined
  })
  noticeDialogVisible.value = true
}

async function handleSendNotice() {
  if (submitting.value) return
  submitting.value = true
  try {
    noticeForm.issuedTime = getNowDateStr()
    await sendNoticeApi({
      noticeType: "latent",
      populationType: "closeContact",
      bizId: noticeRow.value.id,
      patientName: noticeRow.value.name,
      ...noticeForm,
      treatmentPlan: formatLatentNoticeTreatmentPlan(noticeForm.treatmentPlan, noticeForm.customPlanDetail),
      senderId: userStore.userId
    })
    ElMessage.success("通知单发送成功")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

const noticeDetailVisible = ref(false)
const noticeDetailData = ref<any>(null)

async function viewNotice(row: any) {
  try {
    const { data } = await getNoticeListByBizApi(row.id, "latent")
    if (data?.length) {
      noticeDetailData.value = data[0]
      noticeDetailVisible.value = true
    } else {
      ElMessage.info("暂无通知单")
    }
  } catch { /* handled */ }
}

async function handleConfirmNotice(noticeId: number) {
  try {
    await ElMessageBox.confirm("确认接收此通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收")
    noticeDetailVisible.value = false
    fetchData()
  } catch { /* cancelled */ }
}

// ==================== 随访监测详情 ====================
const followupDetailVisible = ref(false)
const followupDetailRow = ref<any>(null)

function viewFollowupDetail(row: any) {
  getScreeningCloseContactDetailApi(row.id).then(({ data }) => {
    followupDetailRow.value = data || row
    followupDetailVisible.value = true
  }).catch(() => {
    followupDetailRow.value = row
    followupDetailVisible.value = true
  })
}

/** 判断日期是否已过期 */
function isOverdue(dateStr: string | undefined): boolean {
  if (!dateStr) return false
  return new Date(dateStr) <= new Date()
}

/** 随访月份列表（用于 v-for 避免 as const） */
const FOLLOWUP_MONTHS = [6, 12, 24]

/** 类型安全的 el-tag type 返回（避免在模板中用 as any） */
function tagType(t: string): "primary" | "success" | "info" | "warning" | "danger" {
  const allowed = ["primary", "success", "info", "warning", "danger"]
  return (allowed.includes(t) ? t : "info") as "primary" | "success" | "info" | "warning" | "danger"
}

// ==================== 随访结果手动录入 ====================
const followupInputVisible = ref(false)
const followupInputMonth = ref<6 | 12 | 24>(6)
const followupInputRow = ref<any>(null)
const followupInputForm = reactive({
  screenDate: "",
  symptom1: "",
  imagingMethod: "",
  imagingResult: "",
  sputumMethod: "",
  sputumResult: "",
  result: ""
})

const FOLLOWUP_RESULT_OPTIONS = ["活动性肺结核", "潜伏感染者", "未发现异常", "其他"]

function openFollowupInput(row: any, month: number) {
  followupInputRow.value = row
  followupInputMonth.value = month as 6 | 12 | 24
  Object.assign(followupInputForm, {
    screenDate: row[`followup${month}ScreenDate`] || "",
    symptom1: row[`followup${month}Symptom1`] || "",
    imagingMethod: row[`followup${month}ImagingMethod`] || "",
    imagingResult: row[`followup${month}ImagingResult`] || "",
    sputumMethod: row[`followup${month}SputumMethod`] || "",
    sputumResult: row[`followup${month}SputumResult`] || "",
    result: row[`followup${month}Result`] || ""
  })
  followupInputVisible.value = true
}

async function handleSaveFollowupInput() {
  if (!followupInputForm.result) {
    ElMessage.warning("请选择筛查结果")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const month = followupInputMonth.value
    await updateScreeningCloseContactApi(followupInputRow.value.id, {
      ...followupInputRow.value,
      [`followup${month}ScreenDate`]: followupInputForm.screenDate || undefined,
      [`followup${month}Symptom1`]: followupInputForm.symptom1 || undefined,
      [`followup${month}ImagingMethod`]: followupInputForm.imagingMethod || undefined,
      [`followup${month}ImagingResult`]: followupInputForm.imagingResult || undefined,
      [`followup${month}SputumMethod`]: followupInputForm.sputumMethod || undefined,
      [`followup${month}SputumResult`]: followupInputForm.sputumResult || undefined,
      [`followup${month}Result`]: followupInputForm.result
    })
    ElMessage.success(`${month}月随访结果已保存`)
    followupInputVisible.value = false
    // 刷新详情弹窗中的数据
    const { data } = await getScreeningCloseContactDetailApi(followupInputRow.value.id)
    if (data) {
      followupDetailRow.value = data
      followupInputRow.value = data
    }
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="接触者姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入联系电话" clearable />
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

    <!-- 潜伏感染者管理 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">密接潜伏感染者管理</span>
          <el-alert title="流程：确认预防治疗 → 填写督导表 → 设置预计完成时间 → 到期确认完成/未完成" type="info" :closable="false" show-icon style="padding: 4px 12px;" />
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="idNumber" label="身份证号" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="sourcePatientName" label="原患者" />
        <el-table-column prop="registrationDate" label="登记日期" />
        <el-table-column prop="infectionCheckResult" label="感染检测结果" />
        <el-table-column label="是否开展预防治疗">
          <template #default="{ row }">
            <el-tag v-if="row.hasPreventiveTreatment" :type="row.hasPreventiveTreatment === '开展' ? 'success' : 'info'" size="small">
              {{ row.hasPreventiveTreatment }}
            </el-tag>
            <span v-else class="text-gray-400">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="预计完成时间">
          <template #default="{ row }">
            <span v-if="row.expectedTreatmentEndDate" :class="{ 'text-danger': isOverdue(row.expectedTreatmentEndDate) }">
              {{ row.expectedTreatmentEndDate }}
            </span>
            <span v-else class="text-gray-400">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="流程状态">
          <template #default="{ row }">
            <el-tag v-if="CC_STATUS_MAP[row.ccStatus]" :type="tagType(CC_STATUS_MAP[row.ccStatus].type)" size="small">
              {{ CC_STATUS_MAP[row.ccStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <!-- 步骤1: 确认预防治疗 -->
            <el-button v-permission="'closeContact:latent:treatmentDecision'" type="primary" size="small" @click="openTreatmentDecision(row)">
              {{ row.hasPreventiveTreatment ? '修改预防治疗' : '确认预防治疗' }}
            </el-button>

            <!-- 步骤2: 发送通知单（已发送后隐藏，改为显示已发送状态） -->
            <el-button
              v-if="!row.noticeSent"
              v-permission="'closeContact:latent:sendNotice'"
              type="success"
              size="small"
              :disabled="!row.hasPreventiveTreatment || row.hasPreventiveTreatment !== '开展'"
              @click="openNoticeDialog(row)"
            >
              发送通知单
            </el-button>
            <el-button
              v-else
              type="success"
              size="small"
              disabled
            >
              已发送通知单
            </el-button>

            <!-- 查看通知单 -->
            <el-button type="info" size="small" @click="viewNotice(row)">
              查看通知单
            </el-button>

            <!-- 步骤3: 填写督导表（已开展预防治疗） -->
            <el-button
              v-permission="'closeContact:latent:supervision'"
              type="warning"
              size="small"
              :disabled="!row.hasPreventiveTreatment || row.hasPreventiveTreatment !== '开展'"
              @click="openSupervisionDialog(row)"
            >
              填写督导表
            </el-button>

            <!-- 查看督导表 -->
            <el-button type="info" size="small" @click="viewSupervision(row)">
              查看督导表
            </el-button>

            <!-- 步骤4: 设置预计完成时间 -->
            <el-button
              v-permission="'closeContact:latent:setExpectedDate'"
              type="primary"
              size="small"
              :disabled="row.hasPreventiveTreatment !== '开展'"
              @click="openExpectedDateDialog(row)"
            >
              设置预计完成时间
            </el-button>

            <!-- 步骤5: 到期确认（仅在设置了预计时间且已到期后显示） -->
            <el-button
              v-if="row.expectedTreatmentEndDate && !row.treatmentCompleted && isOverdue(row.expectedTreatmentEndDate)"
              v-permission="'closeContact:latent:confirmTreatment'"
              type="danger"
              size="small"
              @click="openConfirmTreatment(row)"
            >
              是否完成治疗？
            </el-button>

            <!-- 未完成后查看随访监测 -->
            <el-button
              v-if="row.ccStatus === 4 || row.ccStatus === 5"
              type="info"
              size="small"
              @click="viewFollowupDetail(row)"
            >
              查看随访详情
            </el-button>
            <el-button v-permission="'referral'" type="warning" link size="small" @click="openTierCare(row)">
              转诊
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage" v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange" @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="latent_close"
      population-type="close"
      module-type="latent"
      :subject-name="tierCareRow.name || ''"
    />

    <!-- ==================== 弹窗：预防治疗决策 ==================== -->
    <el-dialog v-model="treatmentDecisionVisible" title="预防性治疗决策" width="440px">
      <el-form label-width="120px">
        <el-form-item label="是否开展预防治疗">
          <el-radio-group v-model="treatmentDecision">
            <el-radio value="开展">
              是（开展预防性治疗）
            </el-radio>
            <el-radio value="未开展">
              否（不开展）
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="treatmentDecision === '未开展'"
          title="选择【否】将进入6/12/24月随访监测流程"
          type="warning"
          :closable="false"
          show-icon
          class="mt-2"
        />
      </el-form>
      <template #footer>
        <el-button @click="treatmentDecisionVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleTreatmentDecision">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 弹窗：设置预计完成时间 ==================== -->
    <el-dialog v-model="expectedDateVisible" title="设置预计完成治疗时间" width="400px">
      <el-form label-width="120px">
        <el-form-item label="预计完成时间">
          <el-date-picker v-model="expectedDateValue" type="date" value-format="YYYY-MM-DD" style="width: 100%" placeholder="请选择预计完成日期" />
        </el-form-item>
        <el-alert title="到达该日期时，系统将提醒操作人员确认治疗是否完成" type="info" :closable="false" show-icon class="mt-2" />
      </el-form>
      <template #footer>
        <el-button @click="expectedDateVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveExpectedDate">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 弹窗：确认治疗完成/未完成 ==================== -->
    <el-dialog v-model="confirmTreatmentVisible" title="治疗完成情况确认" width="440px">
      <el-alert
        :title="`「${confirmTreatmentRow?.name}」的预计完成治疗时间（${confirmTreatmentRow?.expectedTreatmentEndDate}）已到期`"
        type="warning"
        :closable="false"
        show-icon
        class="mb-4"
      />
      <p class="text-center text-base mb-4">
        请确认该患者是否已完成预防性治疗？
      </p>
      <div class="flex gap-4 justify-center">
        <el-button type="success" :loading="submitting" @click="handleConfirmTreatment(true)">
          ✓ 已完成（归档）
        </el-button>
        <el-button type="warning" :loading="submitting" @click="handleConfirmTreatment(false)">
          ✗ 未完成（进入随访监测）
        </el-button>
      </div>
      <template #footer>
        <el-button @click="confirmTreatmentVisible = false">
          稍后处理
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 弹窗：填写督导表 ==================== -->
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
              <el-select v-model="supervisionForm.gender" clearable style="width: 100%">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="supervisionForm.age" :min="0" :max="150" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电话号码">
              <el-input v-model="supervisionForm.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="电话备注">
          <el-input
            v-model="supervisionForm.phoneRemark"
            placeholder="非本人电话时请填写说明（如与本人关系）"
          />
        </el-form-item>
        <el-form-item label="现住址">
          <el-input v-model="supervisionForm.currentAddress" />
        </el-form-item>
        <el-divider content-position="left">
          治疗方案
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始治疗时间">
              <el-date-picker v-model="supervisionForm.treatmentStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="治疗方案">
              <el-select v-model="supervisionForm.treatmentPlan" clearable style="width: 100%">
                <el-option v-for="item in LATENT_TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="isLatentIndividualPlan(supervisionForm.treatmentPlan)">
          <el-col :span="24">
            <el-form-item label="方案详情">
              <el-input v-model="supervisionForm.customPlanDetail" type="textarea" :rows="3" placeholder="请手动录入个体治疗方案详情" />
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
                <el-date-picker v-model="record.time" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="督导方式" label-width="80px">
                <el-select v-model="record.method" clearable style="width: 100%">
                  <el-option v-for="item in SUPERVISION_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="flex items-center justify-end">
              <el-button
                v-if="supervisionForm.supervisionRecords.length > 1" type="danger" link size="small"
                @click="supervisionForm.supervisionRecords.splice(index, 1)"
              >
                删除
              </el-button>
            </el-col>
          </el-row>
          <el-form-item label="督导内容" label-width="90px">
            <el-input v-model="record.content" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="备注" label-width="90px">
            <el-input v-model="record.remark" />
          </el-form-item>
        </div>
        <div class="mb-4">
          <el-button
            type="primary" link
            @click="supervisionForm.supervisionRecords.push({ time: '', content: '', method: '', remark: '' })"
          >
            + 添加督导记录
          </el-button>
        </div>
        <el-divider content-position="left">
          全疗程评价
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="中断用药">
              <el-radio-group v-model="supervisionForm.interruptMedication">
                <el-radio v-for="item in INTERRUPT_MEDICATION_OPTIONS" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="治疗完成情况">
              <el-select v-model="supervisionForm.treatmentCompletionStatus" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="item in TREATMENT_COMPLETION_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="中断次数">
              <el-input-number
                v-model="supervisionForm.interruptCount" :min="0"
                :disabled="supervisionForm.interruptMedication !== '有'" style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="全程应用药次数">
              <el-input-number v-model="supervisionForm.totalDoses" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="实际用药次数">
              <el-input-number v-model="supervisionForm.actualDoses" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="用药率">
              <el-input v-model="supervisionForm.medicationRate" placeholder="自动计算或手动填写" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="结束疗程时间">
          <el-date-picker v-model="supervisionForm.treatmentEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-divider content-position="left">
          督导管理人员
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="管理人员类型">
              <el-select v-model="supervisionForm.managerType" clearable style="width: 100%">
                <el-option v-for="item in SUPERVISION_MANAGER_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理人员姓名">
              <el-input v-model="supervisionForm.managerName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="supervisionForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="上传10张">
          <ImageUploader v-model="supervisionForm.attachmentUrls" />
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

    <!-- ==================== 弹窗：督导表详情 ==================== -->
    <el-dialog v-model="supervisionDetailVisible" title="督导表详情" width="780px">
      <el-descriptions v-if="supervisionDetailData" :column="2" border>
        <el-descriptions-item label="姓名">
          {{ supervisionDetailData.patientName }}
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          {{ supervisionDetailData.category || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ supervisionDetailData.gender || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ supervisionDetailData.age ?? "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="电话号码">
          {{ supervisionDetailData.phone || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="电话备注">
          {{ supervisionDetailData.phoneRemark || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗方案">
          {{ normalizeLatentTreatmentPlan(supervisionDetailData.treatmentPlan) || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="开始治疗时间">
          {{ supervisionDetailData.treatmentStartDate || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="结束疗程时间">
          {{ supervisionDetailData.treatmentEndDate || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗完成情况">
          {{ supervisionDetailData.treatmentCompletionStatus || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="用药率">
          {{ supervisionDetailData.medicationRate || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">
          <AttachmentPreviewList :urls="supervisionDetailData.attachmentUrls" />
        </el-descriptions-item>
      </el-descriptions>
      <el-table v-if="supervisionDetailData?.supervisionRecords" :data="JSON.parse(supervisionDetailData.supervisionRecords)" border stripe size="small" class="my-3">
        <el-table-column prop="time" label="督导时间" />
        <el-table-column prop="method" label="督导方式" />
        <el-table-column prop="content" label="督导内容" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
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

    <!-- ==================== 弹窗：发送通知单 ==================== -->
    <el-dialog v-model="noticeDialogVisible" title="填写潜伏感染者通知单" width="680px">
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="noticeFormRules" label-width="110px">
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
              <el-select v-model="noticeForm.gender" style="width:100%">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="noticeForm.age" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="人群分类">
              <el-input v-model="noticeForm.crowdCategory" />
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
          <el-select v-model="noticeForm.treatmentPlan" style="width: 100%">
            <el-option v-for="item in LATENT_TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isLatentIndividualPlan(noticeForm.treatmentPlan)" label="方案详情">
          <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="3" placeholder="请手动录入个体治疗方案详情" />
        </el-form-item>
        <el-form-item label="接收单位" prop="receiverOrgId">
          <el-select v-model="noticeForm.receiverOrgId" placeholder="请选择接收单位（必填）" filterable style="width:100%">
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
          {{ noticeDetailData.gender || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ noticeDetailData.age ?? "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="民族">
          {{ noticeDetailData.ethnicity || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="人群分类">
          {{ noticeDetailData.crowdCategory }}
        </el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">
          {{ noticeDetailData.currentAddress || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">
          {{ noticeDetailData.householdAddress || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染检测时间">
          {{ noticeDetailData.infectionDate || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="检查方法">
          {{ noticeDetailData.infectionMethod || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染检查结果" :span="2">
          {{ noticeDetailData.infectionResultValue || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查时间">
          {{ noticeDetailData.chestXrayDate || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查结果">
          {{ noticeDetailData.chestXrayResult || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">
          {{ normalizeLatentTreatmentPlan(noticeDetailData.treatmentPlan) || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗机构">
          {{ noticeDetailData.treatmentInstitution || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="下发时间">
          {{ noticeDetailData.issuedTime || "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">
          {{ noticeDetailData.sentTime }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="noticeDetailData.status === 2 ? 'success' : 'warning'" size="small">
            {{ NOTICE_STATUS_MAP[noticeDetailData.status] || (noticeDetailData.status === 2 ? "已确认接收" : "待接收") }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button
          v-if="noticeDetailData?.status === 1"
          v-permission="'closeContact:latent:confirmNotice'"
          type="primary"
          @click="handleConfirmNotice(noticeDetailData.id)"
        >
          确认接收
        </el-button>
        <el-button @click="noticeDetailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 弹窗：随访监测详情 ==================== -->
    <el-dialog v-model="followupDetailVisible" :title="`${followupDetailRow?.name} — 随访监测详情`" width="680px">
      <el-timeline v-if="followupDetailRow">
        <el-timeline-item
          v-for="month in FOLLOWUP_MONTHS"
          :key="month"
          :color="followupDetailRow[`followup${month}Result`] === '活动性肺结核' ? '#f56c6c' : followupDetailRow[`followup${month}Result`] ? '#67c23a' : '#909399'"
        >
          <div class="mb-2 flex items-center justify-between">
            <div>
              <span class="font-bold text-base">{{ month }}月随访</span>
              <span class="ml-3 text-sm text-gray-400">到期日期：{{ followupDetailRow[`followup${month}DueDate`] || '—' }}</span>
            </div>
            <el-button
              v-permission="'closeContact:latent:followup'"
              :type="followupDetailRow[`followup${month}Result`] ? 'warning' : 'primary'"
              size="small"
              link
              @click="openFollowupInput(followupDetailRow, month)"
            >
              {{ followupDetailRow[`followup${month}Result`] ? '修改随访结果' : '录入随访结果' }}
            </el-button>
          </div>
          <template v-if="followupDetailRow[`followup${month}Result`]">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="实际筛查日期">
                {{ followupDetailRow[`followup${month}ScreenDate`] || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="症状">
                {{ followupDetailRow[`followup${month}Symptom1`] || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="影像方法">
                {{ followupDetailRow[`followup${month}ImagingMethod`] || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="影像结果">
                {{ followupDetailRow[`followup${month}ImagingResult`] || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="病原学方法">
                {{ followupDetailRow[`followup${month}SputumMethod`] || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="病原学结果">
                {{ followupDetailRow[`followup${month}SputumResult`] || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="筛查结果" :span="2">
                <el-tag :type="tagType(getFollowupTag(followupDetailRow[`followup${month}Result`]))">
                  {{ followupDetailRow[`followup${month}Result`] }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </template>
          <template v-else>
            <span class="text-gray-400 text-sm">尚未完成，可手动录入或通过导入Excel补充</span>
          </template>
        </el-timeline-item>
      </el-timeline>
      <template #footer>
        <el-button @click="followupDetailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 弹窗：随访结果录入 ==================== -->
    <el-dialog
      v-model="followupInputVisible"
      :title="`录入 ${followupInputMonth} 月随访结果 — ${followupInputRow?.name}`"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="followupInputForm" label-width="110px">
        <el-form-item label="实际筛查日期">
          <el-date-picker
            v-model="followupInputForm.screenDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择筛查日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结核症状">
          <el-input v-model="followupInputForm.symptom1" placeholder="如：咳嗽、无症状等" />
        </el-form-item>
        <el-form-item label="影像检查方法">
          <el-input v-model="followupInputForm.imagingMethod" placeholder="如：胸片、CT等" />
        </el-form-item>
        <el-form-item label="影像检查结果">
          <el-input v-model="followupInputForm.imagingResult" placeholder="如：未见异常、右上肺阴影等" />
        </el-form-item>
        <el-form-item label="病原学方法">
          <el-input v-model="followupInputForm.sputumMethod" placeholder="如：痰涂片、分子生物学等" />
        </el-form-item>
        <el-form-item label="病原学结果">
          <el-input v-model="followupInputForm.sputumResult" placeholder="如：阴性、阳性等" />
        </el-form-item>
        <el-form-item label="筛查结果" required>
          <el-select v-model="followupInputForm.result" placeholder="请选择筛查结果" style="width: 100%">
            <el-option
              v-for="opt in FOLLOWUP_RESULT_OPTIONS"
              :key="opt"
              :label="opt"
              :value="opt"
            />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="followupInputForm.result === '活动性肺结核'"
          title="判定为活动性肺结核后，该记录将标红结案，不进入患者管理"
          type="warning"
          :closable="false"
          show-icon
          class="mt-1"
        />
      </el-form>
      <template #footer>
        <el-button @click="followupInputVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveFollowupInput">
          保存随访结果
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.text-danger {
  color: #f56c6c;
}
.text-success {
  color: #67c23a;
}
</style>
