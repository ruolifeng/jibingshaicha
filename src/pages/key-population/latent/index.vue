<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import {
  TRACKING_STATUS_MAP, REFERRAL_RESULT_OPTIONS, CROWD_CATEGORY_OPTIONS, TREATMENT_PLAN_OPTIONS,
  NOTICE_STATUS_MAP, MEDICATION_STATUS_OPTIONS, TREATMENT_PHASE_MAP, CHECK_PERIOD_OPTIONS,
  CHECK_RESULT_OPTIONS, DIAGNOSIS_RESULT_OPTIONS, CHEST_XRAY_RESULT_OPTIONS,
  PREVENTIVE_RESULT_OPTIONS, PREVENTIVE_MANAGER_OPTIONS,
  INFECTION_METHOD_OPTIONS
} from "@@/constants/disease"
import { idCardRule, phoneRule } from "@@/utils/validate"
import {
  getLatentListApi, trackLatentApi, referralLatentApi, sendNoticeApi, confirmNoticeApi,
  getNoticeListByBizApi, saveSupervisionApi, getSupervisionDetailApi, setMedicationStatusApi,
  closeCaseApi, getFollowUpListApi, saveFollowUpApi, getCheckListApi, saveCheckApi,
  submitXrayApi, importXrayApi
} from "./apis"
import { getLevel5UsersApi } from "@@/apis/users"
import { useUserStore } from "@/pinia/stores/user"

const POPULATION_TYPE = "keyPopulation"

const userStore = useUserStore()
const level5Users = ref<any[]>([])

async function loadLevel5Users() {
  try {
    const { data } = await getLevel5UsersApi()
    level5Users.value = data || []
  } catch { /* handled */ }
}
onMounted(() => { loadLevel5Users() })

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  trackingStatus: undefined as number | undefined,
  archived: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getLatentListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: POPULATION_TYPE,
      ...searchForm
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { paginationData.currentPage = 1; fetchData() }
function handleReset() {
  searchForm.name = ""; searchForm.idNumber = ""; searchForm.trackingStatus = undefined; searchForm.archived = undefined
  handleSearch()
}

const submitting = ref(false)

function getLatentRowClass({ row }: { row: any }) {
  if (row.trackingStatus === 1 && !row.chestXrayResult && row.updateTime) {
    const diffDays = (Date.now() - new Date(row.updateTime).getTime()) / 86400000
    if (diffDays > 7) return "overdue-row"
  }
  return ""
}

// ==================== 追踪弹窗 ====================
const trackDialogVisible = ref(false)
const trackingRow = ref<any>(null)
const trackForm = reactive({ status: 1, remark: "" })

function openTrackDialog(row: any) { trackingRow.value = row; trackForm.status = 1; trackForm.remark = ""; trackDialogVisible.value = true }
async function handleTrack() {
  if (submitting.value) return; submitting.value = true
  try {
    await trackLatentApi({ id: trackingRow.value.id, status: trackForm.status, remark: trackForm.remark })
    ElMessage.success("操作成功"); trackDialogVisible.value = false; fetchData()
  } catch { /* handled */ } finally { submitting.value = false }
}

// ==================== V4：录入胸片+诊断弹窗 ====================
const xrayDialogVisible = ref(false)
const xrayRow = ref<any>(null)
const xrayForm = reactive({ hasChestXray: "是", chestXrayDate: "", chestXrayResult: "", diagnosisFirst: "" })
const xrayImportLoading = ref(false)

function openXrayDialog(row: any) {
  xrayRow.value = row
  xrayForm.hasChestXray = "是"; xrayForm.chestXrayDate = ""; xrayForm.chestXrayResult = ""; xrayForm.diagnosisFirst = ""
  xrayDialogVisible.value = true
}
async function handleSubmitXray() {
  if (!xrayForm.diagnosisFirst) { ElMessage.warning("请选择诊断结果"); return }
  if (submitting.value) return; submitting.value = true
  try {
    await submitXrayApi({ id: xrayRow.value.id, hasChestXray: xrayForm.hasChestXray, chestXrayDate: xrayForm.chestXrayDate || undefined, chestXrayResult: xrayForm.chestXrayResult || undefined, diagnosisFirst: xrayForm.diagnosisFirst })
    ElMessage.success("录入成功"); xrayDialogVisible.value = false; fetchData()
  } catch { /* handled */ } finally { submitting.value = false }
}
async function handleImportXray(uploadFile: any) {
  xrayImportLoading.value = true
  try {
    const { data } = await importXrayApi(uploadFile.raw, POPULATION_TYPE)
    ElMessage.success(`批量更新 ${data} 条胸片诊断数据`); fetchData()
  } catch { ElMessage.error("批量导入失败") } finally { xrayImportLoading.value = false }
}

// ==================== 转诊弹窗 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
const referralForm = reactive({ result: "", remark: "" })

function openReferralDialog(row: any) {
  referralRow.value = row
  const diagMap: Record<string, string> = { "排除": "excluded", "疑似肺结核": "suspected", "确诊患者": "confirmed", "潜伏感染者": "latent", "其他": "other" }
  referralForm.result = diagMap[row.diagnosisFirst] || ""; referralForm.remark = ""; referralDialogVisible.value = true
}
async function handleReferral() {
  if (!referralForm.result) { ElMessage.warning("请选择转诊结果"); return }
  if (submitting.value) return; submitting.value = true
  try {
    await referralLatentApi({ id: referralRow.value.id, result: referralForm.result, remark: referralForm.remark })
    ElMessage.success("操作成功"); referralDialogVisible.value = false; fetchData()
  } catch { /* handled */ } finally { submitting.value = false }
}

// ==================== 通知单弹窗 ====================
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const noticeFormRef = ref()
const noticeFormRules = { idNumber: [idCardRule()], phone: [phoneRule()] }
const noticeForm = reactive({
  idNumber: "", gender: "", birthDate: "", age: null as number | null,
  ethnicity: "", phone: "", crowdCategory: "",
  currentAddress: "", householdAddress: "",
  infectionDate: "", infectionMethod: "", infectionResultValue: "",
  chestXrayDate: "", chestXrayResult: "", treatmentPlan: "",
  treatmentInstitution: "", issuedTime: "",
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
    idNumber: row.idNumber || "", gender: row.gender || "", birthDate: row.birthDate || "", age: row.age || null,
    ethnicity: row.ethnicity || "", phone: row.phone || "", crowdCategory: row.crowdCategory || "", currentAddress: row.currentAddress || "", householdAddress: row.householdAddress || "",
    infectionDate: row.screenDate || "", infectionMethod: row.screenMethod || "", infectionResultValue: row.screenResult || row.infectionResult || "",
    chestXrayDate: row.chestXrayDate || "", chestXrayResult: row.chestXrayResult || "", treatmentPlan: "",
    treatmentInstitution: "", issuedTime: getNowDateStr(), receiverOrgId: undefined
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
    await sendNoticeApi({ noticeType: "latent", populationType: POPULATION_TYPE, bizId: noticeRow.value.id, patientName: noticeRow.value.name, ...noticeForm, senderId: userStore.userId })
    ElMessage.success("通知单发送成功"); noticeDialogVisible.value = false; fetchData()
  } catch { /* handled */ } finally { submitting.value = false }
}

// ==================== 确认接收通知单 ====================
const noticeDetailVisible = ref(false)
const noticeDetailData = ref<any>(null)
async function handleConfirmNotice(noticeId: number) {
  try {
    await ElMessageBox.confirm("确认接收此通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收"); noticeDetailVisible.value = false; fetchData()
  } catch { /* cancelled */ }
}
async function viewNotice(row: any) {
  try {
    const { data } = await getNoticeListByBizApi(row.id, "latent")
    if (data && data.length > 0) { noticeDetailData.value = data[0]; noticeDetailVisible.value = true } else { ElMessage.info("暂无通知单") }
  } catch { /* handled */ }
}

// ==================== 督导表弹窗（V4 新增三个字段） ====================
const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)
const supervisionForm = reactive({ treatmentStartDate: "", treatmentEndDate: "", treatmentPlan: "", supervisionContent: "", preventiveResult: "", preventiveManager: "" })

function openSupervisionDialog(row: any) {
  supervisionRow.value = row
  supervisionForm.treatmentStartDate = ""; supervisionForm.treatmentEndDate = ""; supervisionForm.treatmentPlan = ""; supervisionForm.supervisionContent = ""; supervisionForm.preventiveResult = ""; supervisionForm.preventiveManager = ""
  supervisionDialogVisible.value = true
}
async function handleSaveSupervision() {
  if (submitting.value) return; submitting.value = true
  try {
    await saveSupervisionApi({ latentInfectionId: supervisionRow.value.id, populationType: POPULATION_TYPE, patientName: supervisionRow.value.name, treatmentStartDate: supervisionForm.treatmentStartDate, treatmentEndDate: supervisionForm.treatmentEndDate || undefined, treatmentPlan: supervisionForm.treatmentPlan, supervisionContent: supervisionForm.supervisionContent, preventiveResult: supervisionForm.preventiveResult || undefined, preventiveManager: supervisionForm.preventiveManager || undefined, status: 2 })
    ElMessage.success("督导表保存成功"); supervisionDialogVisible.value = false; fetchData()
  } catch { /* handled */ } finally { submitting.value = false }
}

const supervisionDetailVisible = ref(false)
const supervisionDetailData = ref<any>(null)
async function viewSupervision(row: any) {
  try {
    const { data } = await getSupervisionDetailApi(row.id)
    if (data) { supervisionDetailData.value = data; supervisionDetailVisible.value = true } else { ElMessage.info("暂无督导表") }
  } catch { /* handled */ }
}

// ==================== 服药状态 ====================
const medicationDialogVisible = ref(false)
const medicationRow = ref<any>(null)
const medicationStatusValue = ref(1)
function openMedicationDialog(row: any) { medicationRow.value = row; medicationStatusValue.value = row.medicationStatus || 1; medicationDialogVisible.value = true }
async function handleSetMedication() {
  try { await setMedicationStatusApi({ id: medicationRow.value.id, medicationStatus: medicationStatusValue.value }); ElMessage.success("服药状态设置成功"); medicationDialogVisible.value = false; fetchData() } catch { /* handled */ }
}

// ==================== 治疗管理 ====================
const treatmentDialogVisible = ref(false)
const treatmentRow = ref<any>(null)
const followUpList = ref<any[]>([])
const checkList = ref<any[]>([])

async function openTreatmentDialog(row: any) {
  treatmentRow.value = row; treatmentDialogVisible.value = true
  await Promise.all([loadFollowUps(row.id), loadChecks(row.id)])
}
async function loadFollowUps(id: number) { try { const { data } = await getFollowUpListApi(id); followUpList.value = data || [] } catch { /* handled */ } }
async function loadChecks(id: number) { try { const { data } = await getCheckListApi(id); checkList.value = data || [] } catch { /* handled */ } }

const followUpFormVisible = ref(false)
const followUpForm = reactive({ followUpDate: "", followUpType: "电话随访", content: "", result: "" })
function openFollowUpForm() { followUpForm.followUpDate = ""; followUpForm.content = ""; followUpForm.result = ""; followUpFormVisible.value = true }
async function handleSaveFollowUp() {
  try { await saveFollowUpApi({ latentInfectionId: treatmentRow.value.id, followUpDate: followUpForm.followUpDate, followUpType: followUpForm.followUpType, content: followUpForm.content, result: followUpForm.result, operator: userStore.realName || userStore.username }); ElMessage.success("随访记录保存成功"); followUpFormVisible.value = false; loadFollowUps(treatmentRow.value.id) } catch { /* handled */ }
}

const checkFormVisible = ref(false)
const checkForm = reactive({ checkDate: "", checkPeriod: "", checkResult: "", content: "" })
function openCheckForm() { checkForm.checkDate = ""; checkForm.checkPeriod = ""; checkForm.checkResult = ""; checkForm.content = ""; checkFormVisible.value = true }
async function handleSaveCheck() {
  try { await saveCheckApi({ latentInfectionId: treatmentRow.value.id, checkDate: checkForm.checkDate, checkPeriod: checkForm.checkPeriod, checkResult: checkForm.checkResult, content: checkForm.content, operator: userStore.realName || userStore.username }); ElMessage.success("检查记录保存成功"); checkFormVisible.value = false; loadChecks(treatmentRow.value.id) } catch { /* handled */ }
}

// ==================== 信息归集 ====================
const aggregateDialogVisible = ref(false)
const aggregateRow = ref<any>(null)
const aggregateNotices = ref<any[]>([])
const aggregateSupervision = ref<any>(null)
const aggregateFollowUps = ref<any[]>([])
const aggregateChecks = ref<any[]>([])
async function openAggregateDialog(row: any) {
  aggregateRow.value = row; aggregateDialogVisible.value = true
  aggregateNotices.value = []; aggregateSupervision.value = null; aggregateFollowUps.value = []; aggregateChecks.value = []
  try {
    const [noticeRes, supervisionRes, followUpRes, checkRes] = await Promise.allSettled([
      getNoticeListByBizApi(row.id, "latent"), getSupervisionDetailApi(row.id),
      getFollowUpListApi(row.id), getCheckListApi(row.id)
    ])
    if (noticeRes.status === "fulfilled") aggregateNotices.value = noticeRes.value?.data || []
    if (supervisionRes.status === "fulfilled") aggregateSupervision.value = supervisionRes.value?.data || null
    if (followUpRes.status === "fulfilled") aggregateFollowUps.value = followUpRes.value?.data || []
    if (checkRes.status === "fulfilled") aggregateChecks.value = checkRes.value?.data || []
  } catch { /* partial */ }
}

async function handleCloseCase(row: any) {
  try {
    await ElMessageBox.confirm("确认结案归档该潜伏感染者吗？", "结案确认", { type: "warning" })
    await closeCaseApi(row.id); ElMessage.success("结案归档成功"); fetchData()
    if (treatmentDialogVisible.value) treatmentDialogVisible.value = false
  } catch { /* cancelled */ }
}

function getTrackingStatusType(status: number) {
  if (status === 1) return "success"
  if (status === 2 || status === 4) return "danger"
  if (status === 3) return "warning"
  return "info"
}

watch(() => [paginationData.currentPage, paginationData.pageSize], fetchData, { immediate: true })
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名"><el-input v-model="searchForm.name" placeholder="请输入姓名" clearable /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable /></el-form-item>
        <el-form-item label="追踪状态">
          <el-select v-model="searchForm.trackingStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(label, key) in TRACKING_STATUS_MAP" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="归档状态">
          <el-select v-model="searchForm.archived" placeholder="全部" clearable style="width: 120px">
            <el-option label="未归档" :value="0" /><el-option label="已归档" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">重点人群 — 潜伏感染管理</span>
          <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleImportXray">
            <el-button v-permission="'latent:xray'" :loading="xrayImportLoading" size="small">批量导入胸片诊断</el-button>
          </el-upload>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600" :row-class-name="getLatentRowClass">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" />
        <el-table-column label="追踪状态">
          <template #default="{ row }">
            <el-tag :type="getTrackingStatusType(row.trackingStatus)" size="small">{{ TRACKING_STATUS_MAP[row.trackingStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notInPlaceCount" label="未到位次数" />
        <el-table-column prop="trackingRemark" label="追踪备注" />
        <el-table-column prop="chestXrayResult" label="胸片结果" />
        <el-table-column prop="diagnosisFirst" label="诊断结果" />
        <el-table-column prop="referralResult" label="转诊结果">
          <template #default="{ row }">{{ REFERRAL_RESULT_OPTIONS.find(o => o.value === row.referralResult)?.label || row.referralResult || "-" }}</template>
        </el-table-column>
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-button v-if="row.referralResult === 'latent'" type="primary" link size="small" @click="viewNotice(row)">{{ row.name }}通知单</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="治疗阶段">
          <template #default="{ row }">
            <el-tag v-if="row.treatmentPhase" :type="row.treatmentPhase === 2 ? 'info' : 'warning'" size="small">{{ TREATMENT_PHASE_MAP[row.treatmentPhase] || "-" }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="服药状态">
          <template #default="{ row }">{{ MEDICATION_STATUS_OPTIONS.find(o => o.value === row.medicationStatus)?.label || "-" }}</template>
        </el-table-column>
        <el-table-column label="归档">
          <template #default="{ row }">
            <el-tag :type="row.archived ? 'info' : 'success'" size="small">{{ row.archived ? "已归档" : "进行中" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="260">
          <template #default="{ row }">
            <el-button v-if="row.trackingStatus === 0 || row.trackingStatus === 2" v-permission="'latent:track'" type="primary" size="small" @click="openTrackDialog(row)">追踪</el-button>
            <el-button v-if="row.trackingStatus === 1 && !row.diagnosisFirst" v-permission="'latent:xray'" type="warning" size="small" @click="openXrayDialog(row)">录入胸片诊断</el-button>
            <el-button v-if="row.trackingStatus === 1 && row.diagnosisFirst && !row.referralResult" v-permission="'latent:referral'" type="warning" size="small" @click="openReferralDialog(row)">转诊</el-button>
            <el-button v-if="row.referralResult === 'latent'" v-permission="'latent:sendNotice'" type="primary" size="small" @click="openNoticeDialog(row)">填写通知单</el-button>
            <el-button
              v-if="row.referralResult === 'latent'"
              v-permission="'latent:sendNotice'"
              type="success"
              size="small"
              :disabled="!!row.noticeSent"
              @click="openNoticeDialog(row)"
            >
              {{ row.noticeSent ? "已发送通知单" : "发送通知单" }}
            </el-button>
            <el-button v-if="row.referralResult === 'latent'" v-permission="'latent:supervision'" size="small" :disabled="!row.noticeSent" @click="openSupervisionDialog(row)">填写督导表</el-button>
            <el-button v-if="row.referralResult === 'latent'" type="info" size="small" @click="viewSupervision(row)">查看督导表</el-button>
            <el-button v-if="row.treatmentPhase === 1 && !row.medicationStatus" v-permission="'latent:supervision'" type="warning" size="small" @click="openMedicationDialog(row)">设置服药状态</el-button>
            <el-button v-if="row.treatmentPhase === 1 && row.medicationStatus" v-permission="'latent:followUp'" type="primary" size="small" @click="openTreatmentDialog(row)">治疗管理</el-button>
            <el-button v-if="!row.archived || row.treatmentPhase >= 1" type="info" size="small" @click="openAggregateDialog(row)">信息归集</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="paginationData.currentPage" v-model:page-size="paginationData.pageSize" :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper" @current-change="handleCurrentChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 追踪弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="追踪状态">
          <el-radio-group v-model="trackForm.status"><el-radio :value="1">到位</el-radio><el-radio :value="2">未到位</el-radio><el-radio :value="3">其他</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item v-if="trackForm.status === 3 || (trackForm.status === 2 && trackingRow?.notInPlaceCount >= 2)" label="备注原因">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" placeholder="请填写原因" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="trackDialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleTrack">确认</el-button></template>
    </el-dialog>

    <!-- 录入胸片+诊断弹窗 -->
    <el-dialog v-model="xrayDialogVisible" title="录入胸片检查与诊断结果" width="520px">
      <el-alert type="info" :closable="false" class="mb-4" description="追踪到位后，请录入胸片检查情况及诊断结果。" />
      <el-form :model="xrayForm" label-width="110px">
        <el-form-item label="是否进行胸片检查">
          <el-radio-group v-model="xrayForm.hasChestXray"><el-radio value="是">是</el-radio><el-radio value="否">否</el-radio></el-radio-group>
        </el-form-item>
        <template v-if="xrayForm.hasChestXray === '是'">
          <el-form-item label="胸片检查日期"><el-date-picker v-model="xrayForm.chestXrayDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="胸片结果">
            <el-select v-model="xrayForm.chestXrayResult" placeholder="请选择" style="width: 100%">
              <el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="诊断结果" required>
          <el-select v-model="xrayForm.diagnosisFirst" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in DIAGNOSIS_RESULT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="xrayDialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmitXray">确认录入</el-button></template>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <el-dialog v-model="referralDialogVisible" title="转诊操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="转诊结果">
          <el-radio-group v-model="referralForm.result">
            <el-radio v-for="item in REFERRAL_RESULT_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="referralForm.result === 'other'" label="备注原因"><el-input v-model="referralForm.remark" type="textarea" :rows="3" placeholder="请填写原因" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="referralDialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleReferral">确认</el-button></template>
    </el-dialog>

    <!-- 通知单弹窗 -->
    <el-dialog v-model="noticeDialogVisible" title="填写潜伏感染者通知单" width="680px">
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="noticeFormRules" label-width="110px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="姓名"><el-input :value="noticeRow?.name" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证" prop="idNumber"><el-input v-model="noticeForm.idNumber" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="性别"><el-select v-model="noticeForm.gender" style="width: 100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="年龄"><el-input-number v-model="noticeForm.age" :min="0" :max="150" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="出生日期"><el-date-picker v-model="noticeForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="联系方式"><el-input v-model="noticeForm.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="民族"><el-input v-model="noticeForm.ethnicity" placeholder="如：汉族" /></el-form-item></el-col>
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
          <el-col :span="24"><el-form-item label="现居住地址"><el-input v-model="noticeForm.currentAddress" placeholder="请输入现居住地址" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="户籍地址"><el-input v-model="noticeForm.householdAddress" placeholder="请输入户籍地址" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">感染检查</el-divider>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="感染检测时间"><el-date-picker v-model="noticeForm.infectionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="检查方法"><el-select v-model="noticeForm.infectionMethod" style="width: 100%"><el-option v-for="item in INFECTION_METHOD_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="检查结果"><el-input v-model="noticeForm.infectionResultValue" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">胸片检查</el-divider>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="胸片检查时间"><el-date-picker v-model="noticeForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="胸片检查结果"><el-select v-model="noticeForm.chestXrayResult" style="width: 100%"><el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">治疗方案</el-divider>
        <el-form-item label="治疗方案"><el-select v-model="noticeForm.treatmentPlan" style="width: 100%" placeholder="请选择治疗方案"><el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-divider content-position="left">机构信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="治疗机构"><el-input v-model="noticeForm.treatmentInstitution" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="下发时间"><el-input :model-value="noticeForm.issuedTime" disabled /></el-form-item></el-col>
        </el-row>
        <el-form-item label="接收单位"><el-select v-model="noticeForm.receiverOrgId" placeholder="请选择五级机构" filterable style="width: 100%"><el-option v-for="u in level5Users" :key="u.id" :label="`${u.realName || u.username} - ${u.orgName || '未设置机构'}`" :value="u.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="noticeDialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSendNotice">发送通知单</el-button></template>
    </el-dialog>

    <!-- 通知单详情弹窗 -->
    <el-dialog v-model="noticeDetailVisible" title="潜伏感染者通知单详情" width="680px">
      <el-descriptions v-if="noticeDetailData" :column="2" border>
        <el-descriptions-item label="姓名">{{ noticeDetailData.patientName }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ noticeDetailData.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ noticeDetailData.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ noticeDetailData.age }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ noticeDetailData.phone || "-" }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ noticeDetailData.ethnicity || "-" }}</el-descriptions-item>
        <el-descriptions-item label="人群分类">{{ noticeDetailData.crowdCategory }}</el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">{{ noticeDetailData.currentAddress || "-" }}</el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">{{ noticeDetailData.householdAddress || "-" }}</el-descriptions-item>
        <el-descriptions-item label="感染检测时间">{{ noticeDetailData.infectionDate || "-" }}</el-descriptions-item>
        <el-descriptions-item label="检查方法">{{ noticeDetailData.infectionMethod || "-" }}</el-descriptions-item>
        <el-descriptions-item label="感染检查结果" :span="2">{{ noticeDetailData.infectionResultValue || "-" }}</el-descriptions-item>
        <el-descriptions-item label="胸片检查时间">{{ noticeDetailData.chestXrayDate || "-" }}</el-descriptions-item>
        <el-descriptions-item label="胸片检查结果">{{ noticeDetailData.chestXrayResult || "-" }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ noticeDetailData.treatmentPlan || "-" }}</el-descriptions-item>
        <el-descriptions-item label="治疗机构">{{ noticeDetailData.treatmentInstitution || "-" }}</el-descriptions-item>
        <el-descriptions-item label="下发时间">{{ noticeDetailData.issuedTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="noticeDetailData.status === 2 ? 'success' : 'warning'" size="small">{{ NOTICE_STATUS_MAP[noticeDetailData.status] }}</el-tag></el-descriptions-item>
        <el-descriptions-item v-if="noticeDetailData.confirmedTime" label="确认时间">{{ noticeDetailData.confirmedTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button v-if="noticeDetailData && noticeDetailData.status === 1 && userStore.userRole === 6" v-permission="'latent:confirmNotice'" type="primary" @click="handleConfirmNotice(noticeDetailData.id)">确认接收</el-button>
      </template>
    </el-dialog>

    <!-- 督导表填写弹窗 -->
    <el-dialog v-model="supervisionDialogVisible" title="填写预防性治疗督导表" width="620px">
      <el-form :model="supervisionForm" label-width="130px">
        <el-form-item label="治疗开始日期"><el-date-picker v-model="supervisionForm.treatmentStartDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="治疗方案">
          <el-select v-model="supervisionForm.treatmentPlan" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="督导内容"><el-input v-model="supervisionForm.supervisionContent" type="textarea" :rows="4" placeholder="请填写督导内容" /></el-form-item>
        <el-divider>预防性治疗完成情况</el-divider>
        <el-form-item label="治疗完成时间"><el-date-picker v-model="supervisionForm.treatmentEndDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="预防性治疗结果">
          <el-select v-model="supervisionForm.preventiveResult" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="item in PREVENTIVE_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="随访管理人员">
          <el-select v-model="supervisionForm.preventiveManager" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="item in PREVENTIVE_MANAGER_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="supervisionDialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSaveSupervision">保存督导表</el-button></template>
    </el-dialog>

    <!-- 督导表详情 -->
    <el-dialog v-model="supervisionDetailVisible" title="督导表详情" width="620px">
      <el-descriptions v-if="supervisionDetailData" :column="2" border>
        <el-descriptions-item label="患者姓名">{{ supervisionDetailData.patientName }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案">{{ supervisionDetailData.treatmentPlan }}</el-descriptions-item>
        <el-descriptions-item label="治疗开始日期">{{ supervisionDetailData.treatmentStartDate }}</el-descriptions-item>
        <el-descriptions-item label="治疗完成时间">{{ supervisionDetailData.treatmentEndDate || "-" }}</el-descriptions-item>
        <el-descriptions-item label="预防性治疗结果">{{ supervisionDetailData.preventiveResult || "-" }}</el-descriptions-item>
        <el-descriptions-item label="随访管理人员">{{ supervisionDetailData.preventiveManager || "-" }}</el-descriptions-item>
        <el-descriptions-item label="督导内容" :span="2">{{ supervisionDetailData.supervisionContent }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 服药状态弹窗 -->
    <el-dialog v-model="medicationDialogVisible" title="设置服药状态" width="450px">
      <el-form label-width="100px">
        <el-form-item label="服药状态">
          <el-radio-group v-model="medicationStatusValue">
            <el-radio v-for="item in MEDICATION_STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="medicationDialogVisible = false">取消</el-button><el-button type="primary" @click="handleSetMedication">确认</el-button></template>
    </el-dialog>

    <!-- 治疗管理弹窗 -->
    <el-dialog v-model="treatmentDialogVisible" :title="`预防治疗管理 — ${treatmentRow?.name || ''}`" width="800px">
      <el-tabs>
        <el-tab-pane label="电话随访">
          <div class="mb-3 flex justify-end">
            <el-button type="primary" size="small" v-permission="'latent:followUp'" @click="openFollowUpForm">新增电话随访</el-button>
          </div>
          <el-table :data="followUpList" border stripe max-height="300">
            <el-table-column prop="followUpDate" label="随访日期" /><el-table-column prop="content" label="随访内容" /><el-table-column prop="result" label="随访结果" /><el-table-column prop="operator" label="操作人" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按期检查">
          <div class="mb-3 flex justify-end">
            <el-button type="primary" size="small" v-permission="'latent:check'" @click="openCheckForm">新增按期检查</el-button>
          </div>
          <el-table :data="checkList" border stripe max-height="300">
            <el-table-column prop="checkDate" label="检查日期" /><el-table-column prop="checkPeriod" label="检查周期" /><el-table-column prop="checkResult" label="检查结果" /><el-table-column prop="operator" label="操作人" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="treatmentDialogVisible = false">关闭</el-button>
        <el-button v-if="treatmentRow?.treatmentPhase === 1" v-permission="'latent:closeCase'" type="danger" @click="handleCloseCase(treatmentRow)">结案归档</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followUpFormVisible" title="新增电话随访" width="500px" append-to-body>
      <el-form :model="followUpForm" label-width="80px">
        <el-form-item label="随访日期"><el-date-picker v-model="followUpForm.followUpDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="随访内容"><el-input v-model="followUpForm.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="随访结果"><el-input v-model="followUpForm.result" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="followUpFormVisible = false">取消</el-button><el-button type="primary" @click="handleSaveFollowUp">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="checkFormVisible" title="新增按期检查" width="500px" append-to-body>
      <el-form :model="checkForm" label-width="80px">
        <el-form-item label="检查日期"><el-date-picker v-model="checkForm.checkDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="检查周期"><el-select v-model="checkForm.checkPeriod" style="width: 100%"><el-option v-for="item in CHECK_PERIOD_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="检查结果"><el-select v-model="checkForm.checkResult" style="width: 100%"><el-option v-for="item in CHECK_RESULT_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="检查详情"><el-input v-model="checkForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="checkFormVisible = false">取消</el-button><el-button type="primary" @click="handleSaveCheck">保存</el-button></template>
    </el-dialog>

    <!-- 信息归集汇总弹窗 -->
    <el-dialog v-model="aggregateDialogVisible" title="潜伏感染者信息归集" width="750px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ aggregateRow?.name }}</el-descriptions-item>
        <el-descriptions-item label="证件号">{{ aggregateRow?.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ aggregateRow?.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ aggregateRow?.age }}</el-descriptions-item>
        <el-descriptions-item label="感染筛查结果">{{ aggregateRow?.infectionResult }}</el-descriptions-item>
        <el-descriptions-item label="追踪状态">{{ TRACKING_STATUS_MAP[aggregateRow?.trackingStatus] || "-" }}</el-descriptions-item>
        <el-descriptions-item label="胸片结果">{{ aggregateRow?.chestXrayResult || "-" }}</el-descriptions-item>
        <el-descriptions-item label="诊断结果">{{ aggregateRow?.diagnosisFirst || "-" }}</el-descriptions-item>
        <el-descriptions-item label="转诊结果">{{ aggregateRow?.diagnosisResult || "-" }}</el-descriptions-item>
        <el-descriptions-item label="治疗阶段">{{ TREATMENT_PHASE_MAP[aggregateRow?.treatmentPhase] || "-" }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">通知单</el-divider>
      <el-table :data="aggregateNotices" border stripe size="small" max-height="200">
        <el-table-column prop="bizType" label="类型" width="120" />
        <el-table-column prop="receiverName" label="接收人" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row: n }"><el-tag :type="n.status === 1 ? 'success' : 'warning'" size="small">{{ NOTICE_STATUS_MAP[n.status] || "未知" }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" />
      </el-table>
      <el-divider content-position="left">督导表</el-divider>
      <el-descriptions v-if="aggregateSupervision" :column="2" border size="small">
        <el-descriptions-item label="治疗方案">{{ aggregateSupervision.treatmentPlan || "-" }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ aggregateSupervision.treatmentStartDate || "-" }}</el-descriptions-item>
        <el-descriptions-item label="完成日期">{{ aggregateSupervision.treatmentEndDate || "-" }}</el-descriptions-item>
        <el-descriptions-item label="治疗结果">{{ aggregateSupervision.preventiveResult || "-" }}</el-descriptions-item>
        <el-descriptions-item label="管理人员">{{ aggregateSupervision.preventiveManager || "-" }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无督导表记录" :image-size="60" />
      <el-divider content-position="left">电话随访</el-divider>
      <el-table :data="aggregateFollowUps" border stripe size="small" max-height="200">
        <el-table-column prop="followUpDate" label="日期" width="120" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="result" label="结果" width="120" />
      </el-table>
      <el-divider content-position="left">按期检查</el-divider>
      <el-table :data="aggregateChecks" border stripe size="small" max-height="200">
        <el-table-column prop="checkDate" label="日期" width="120" />
        <el-table-column prop="checkPeriod" label="周期" width="100" />
        <el-table-column prop="checkResult" label="结果" width="100" />
        <el-table-column prop="content" label="详情" />
      </el-table>
      <template #footer><el-button @click="aggregateDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
</style>

<style lang="scss">
.el-table .overdue-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
