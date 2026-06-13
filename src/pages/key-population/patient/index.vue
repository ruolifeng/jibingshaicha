<script lang="ts" setup>
import { getLevel5UsersApi } from "@@/apis/users"
import FirstVisitDetailDialog from "@@/components/FirstVisitDetailDialog.vue"
import FollowUpVisitDialog from "@@/components/FollowUpVisitDialog.vue"
import ImageUploader from "@@/components/ImageUploader.vue"
import MedicationCalendar from "@@/components/MedicationCalendar.vue"
import PrintFirstVisit from "@@/components/PrintFirstVisit.vue"
import PrintMedication from "@@/components/PrintMedication.vue"
import PrintNotice from "@@/components/PrintNotice.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { parseMedicationRecords, serializeMedicationRecords, type MedicationRecordsMap } from "@@/utils/medicationRecords"
import {
  CHEST_XRAY_RESULT_OPTIONS,
  CROWD_CATEGORY_OPTIONS,
  DRUG_FORM_OPTIONS,
  DRUG_RESISTANCE_OPTIONS,
  EDUCATION_ITEMS,
  FIRST_VISIT_SUPERVISOR_OPTIONS,
  MANAGEMENT_METHOD_OPTIONS,
  MEDICATION_USAGE_OPTIONS,
  NOTICE_STATUS_MAP,
  PATHOGEN_RESULT_OPTIONS,
  PATIENT_MANAGEMENT_METHOD_OPTIONS,
  PATIENT_TYPE_OPTIONS,
  SPUTUM_RESULT_OPTIONS,
  SPUTUM_STATUS_OPTIONS,
  SUPERVISOR_OPTIONS,
  SYMPTOM_OPTIONS,
  TREATMENT_PLAN_OPTIONS,
  VENTILATION_OPTIONS,
  VISIT_METHOD_OPTIONS,
  VISIT_METHOD_OTHER
} from "@@/constants/disease"
import { followUpFormatters } from "@@/utils/followUpVisitFormat"
import { ArrowDown } from "@element-plus/icons-vue"
import { applyFirstVisitChemotherapyDefault, isValidFirstVisitFormNo, sanitizeFirstVisitFormNo } from "@@/utils/firstVisit"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { idCardRule } from "@@/utils/validate"
import { getScreeningKeyPopulationDetailApi } from "@/pages/key-population/screening/apis"
import { confirmNoticeApi, getNoticeListByBizApi, saveNoticeDraftApi, sendNoticeApi } from "@/pages/school/latent/apis"
import { useUserStore } from "@/pinia/stores/user"
import {
  completeMedicationApi,
  getFirstVisitApi,
  getFollowUpListApi,
  getMedicationApi,
  exportPatientListApi,
  getPatientListApi,
  importEpidemicApi,
  saveFirstVisitApi,
  saveFirstVisitDraftApi,
  saveMedicationApi
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

async function fetchData() {
  loading.value = true
  try {
    const { dateRange, ...rest } = searchForm
    const { data } = await getPatientListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: "keyPopulation",
      ...rest,
      ...extractDateRangeParams(dateRange)
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

const exporting = ref(false)
async function handleExport() {
  try {
    exporting.value = true
    const res = await exportPatientListApi({
      populationType: "keyPopulation",
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      ...extractDateRangeParams(searchForm.dateRange)
    })
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "重点人群_患者管理.xlsx"
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } catch (err: any) {
    ElMessage.error(err?.message || "导出失败")
  } finally {
    exporting.value = false
  }
}

async function handleImportEpidemic(uploadFile: any) {
  try {
    const { data } = await importEpidemicApi(uploadFile.raw, "keyPopulation")
    ElMessage.success(`成功导入 ${data} 条大疫情数据`)
    fetchData()
  } catch { /* handled */ }
}

// ==================== 患者通知单 ====================
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const submitting = ref(false)
const noticeFormRef = ref()
const noticeFormRules = {
  idNumber: [idCardRule()],
  patientType: [{ required: true, message: "请选择患者类型", trigger: "change" }],
  managementMethod: [{ required: true, message: "请选择管理方式", trigger: "change" }],
  receiverOrgId: [{ required: true, message: "请选择接收单位", trigger: "change" }]
}

function getPatientRowClass({ row }: { row: any }) {
  if (!row.archived && row.createTime) {
    const diffDays = (Date.now() - new Date(row.createTime).getTime()) / 86400000
    if (diffDays > 3) return "overdue-row"
  }
  return ""
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
  // 感染筛查
  infectionDate: "",
  infectionMethod: "",
  infectionResultValue: "",
  chestXrayDate: "",
  chestXrayResult: "",
  treatmentInstitution: "",
  issuedTime: "",
  patientType: "",
  managementMethod: "",
  treatmentPlan: "",
  customPlanDetail: "",
  sputumSmear: "",
  sputumCulture: "",
  molecularTest: "",
  pathologyTest: "",
  otherNotes: "",
  receiverOrgId: undefined as number | undefined
})

function openNoticeDialog(row: any) {
  noticeRow.value = row
  if ((row.noticeStatus === 0 || row.noticeStatus === 2) && row.noticeId) {
    getNoticeListByBizApi(row.id, "patient").then(({ data }) => {
      const notice = data?.[0]
      if (notice) {
        Object.assign(noticeForm, {
          idNumber: notice.idNumber || "",
          gender: notice.gender || "",
          birthDate: notice.birthDate || "",
          age: notice.age || null,
          ethnicity: notice.ethnicity || "",
          phone: notice.phone || "",
          crowdCategory: notice.crowdCategory || "",
          currentAddress: notice.currentAddress || "",
          householdAddress: notice.householdAddress || "",
          infectionDate: notice.infectionDate || noticeRow.value?.screenDate || "",
          infectionMethod: notice.infectionMethod || noticeRow.value?.screenMethod || "",
          infectionResultValue: notice.infectionResultValue || noticeRow.value?.infectionResult || "",
          chestXrayDate: notice.chestXrayDate || "",
          chestXrayResult: notice.chestXrayResult || "",
          treatmentInstitution: notice.treatmentInstitution || "",
          issuedTime: notice.issuedTime || new Date().toISOString().slice(0, 10),
          patientType: notice.patientType || "",
          managementMethod: notice.managementMethod || "",
          treatmentPlan: notice.treatmentPlan || "",
          customPlanDetail: notice.customPlanDetail || "",
          sputumSmear: notice.sputumSmear || "",
          sputumCulture: notice.sputumCulture || "",
          molecularTest: notice.molecularTest || "",
          pathologyTest: notice.pathologyTest || "",
          otherNotes: notice.otherNotes || "",
          receiverOrgId: notice.receiverOrgId || undefined
        })
      }
    }).catch(() => { /* 忽略 */ })
  } else {
    Object.assign(noticeForm, {
      idNumber: row.idNumber || "",
      gender: row.gender || "",
      birthDate: "",
      age: row.age || null,
      ethnicity: row.ethnicity || "",
      phone: row.phone || "",
      crowdCategory: "",
      currentAddress: row.currentAddress || "",
      householdAddress: row.householdAddress || "",
      infectionDate: row.screenDate || "",
      infectionMethod: row.screenMethod || "",
      infectionResultValue: row.infectionResult || "",
      chestXrayDate: row.chestXrayDate || "",
      chestXrayResult: row.chestXrayResult || "",
      treatmentInstitution: "",
      issuedTime: new Date().toISOString().slice(0, 10),
      patientType: "",
      managementMethod: "",
      treatmentPlan: "",
      customPlanDetail: "",
      sputumSmear: "",
      sputumCulture: "",
      molecularTest: "",
      pathologyTest: "",
      otherNotes: "",
      receiverOrgId: undefined
    })
  }
  noticeDialogVisible.value = true
}

async function handleSendNotice() {
  if (submitting.value) return
  try {
    await noticeFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await sendNoticeApi({
      noticeType: "patient",
      populationType: "keyPopulation",
      bizId: noticeRow.value.id,
      patientName: noticeRow.value.name,
      ...noticeForm,
      treatmentPlan: noticeForm.treatmentPlan === "个体化方案" ? noticeForm.customPlanDetail : noticeForm.treatmentPlan,
      senderId: userStore.userId
    })
    ElMessage.success("患者通知单发送成功")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

async function handleSaveDraft() {
  if (submitting.value) return
  submitting.value = true
  try {
    await saveNoticeDraftApi({
      noticeType: "patient",
      populationType: "keyPopulation",
      bizId: noticeRow.value.id,
      patientName: noticeRow.value.name,
      ...noticeForm,
      treatmentPlan: noticeForm.treatmentPlan === "个体化方案" ? noticeForm.customPlanDetail : noticeForm.treatmentPlan,
      senderId: userStore.userId
    })
    ElMessage.success("通知单草稿已保存")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

// ==================== 确认接收患者通知单 ====================
async function handleConfirmNotice(noticeId: number) {
  try {
    await ElMessageBox.confirm("确认接收此患者通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收")
    noticeDetailVisible.value = false
    fetchData()
  } catch { /* cancelled or handled */ }
}

// ==================== 通知单查看 ====================
const noticeDetailVisible = ref(false)
const noticeDetailData = ref<any>(null)

async function viewNotice(row: any) {
  try {
    const { data } = await getNoticeListByBizApi(row.id, "patient")
    if (data?.length > 0) {
      noticeDetailData.value = data[0]
      noticeDetailVisible.value = true
    } else {
      ElMessage.info("暂无患者通知单")
    }
  } catch { /* handled */ }
}

// ==================== 首次随访 ====================
const firstVisitDialogVisible = ref(false)
const firstVisitRow = ref<any>(null)
const firstVisitCompleted = ref(false)
const firstVisitForm = reactive({
  id: undefined as number | undefined,
  formNo: "",
  visitDate: "",
  visitMethod: "",
  visitMethodOther: "",
  patientType: "",
  sputumStatus: "",
  drugResistance: "",
  symptoms: [] as string[],
  otherSymptoms: "",
  chemotherapy: "",
  medicationUsage: "",
  drugForm: [] as string[],
  supervisor: "",
  separateRoom: "",
  ventilation: "",
  smokingAmount: "",
  drinkingAmount: "",
  medicationLocation: "",
  medicationPickTime: "",
  educationItems: {} as Record<string, string>,
  nextVisitDate: "",
  doctorSignature: "",
  // V15 新增
  remarks: "",
  attachmentUrls: ""
})

function openFirstVisitDialog(row: any) {
  firstVisitRow.value = row
  firstVisitCompleted.value = false
  Object.assign(firstVisitForm, {
    id: undefined,
    formNo: "",
    visitDate: "",
    visitMethod: "",
    visitMethodOther: "",
    patientType: "",
    sputumStatus: "",
    drugResistance: "",
    symptoms: [],
    otherSymptoms: "",
    chemotherapy: "",
    medicationUsage: "",
    drugForm: [],
    supervisor: "",
    separateRoom: "",
    ventilation: "",
    smokingAmount: "",
    drinkingAmount: "",
    medicationLocation: "",
    medicationPickTime: "",
    educationItems: {},
    nextVisitDate: "",
    doctorSignature: "",
    remarks: "",
    attachmentUrls: ""
  })
  firstVisitDialogVisible.value = true
  loadFirstVisitForm(row.id)
}

async function loadFirstVisitForm(patientId: number) {
  try {
    const { data } = await getFirstVisitApi(patientId)
    if (data) {
      firstVisitCompleted.value = data.status === 1
      Object.assign(firstVisitForm, {
        ...data,
        symptoms: data.symptoms ? String(data.symptoms).split(",").map((s: string) => s.trim()).filter(Boolean) : [],
        drugForm: data.drugForm ? String(data.drugForm).split(",").map((s: string) => s.trim()).filter(Boolean) : [],
        educationItems: data.educationItems
          ? (typeof data.educationItems === "string" ? JSON.parse(data.educationItems) : data.educationItems)
          : {},
        attachmentUrls: data.attachmentUrls ?? ""
      })
    }
  } catch { /* 首次填写 */ }
  applyFirstVisitChemotherapyDefault(firstVisitForm, firstVisitRow.value)
}

watch(
  () => firstVisitForm.visitMethod,
  (val) => {
    if (val !== VISIT_METHOD_OTHER) {
      firstVisitForm.visitMethodOther = ""
    }
  }
)

function buildFirstVisitPayload() {
  return {
    patientId: firstVisitRow.value.id,
    populationType: "keyPopulation",
    ...firstVisitForm,
    visitMethodOther: firstVisitForm.visitMethod === VISIT_METHOD_OTHER
      ? firstVisitForm.visitMethodOther.trim()
      : null,
    symptoms: firstVisitForm.symptoms.join(","),
    drugForm: firstVisitForm.drugForm.join(","),
    educationItems: JSON.stringify(firstVisitForm.educationItems)
  }
}

async function handleSaveFirstVisitDraft() {
  try {
    await saveFirstVisitDraftApi(buildFirstVisitPayload())
    ElMessage.success("首次随访草稿已保存")
    firstVisitDialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

async function handleSaveFirstVisit() {
  if (!isValidFirstVisitFormNo(firstVisitForm.formNo)) {
    ElMessage.warning("请填写8位编号")
    return
  }
  if (firstVisitForm.visitMethod === VISIT_METHOD_OTHER && !firstVisitForm.visitMethodOther.trim()) {
    ElMessage.warning("请填写随访方式")
    return
  }
  try {
    await saveFirstVisitApi(buildFirstVisitPayload())
    ElMessage.success("首次随访保存成功")
    firstVisitDialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

// ==================== 首次随访查看 ====================
const firstVisitDetailVisible = ref(false)
const firstVisitDetailData = ref<any>(null)

async function viewFirstVisit(row: any) {
  try {
    const { data } = await getFirstVisitApi(row.id)
    if (data) {
      firstVisitDetailData.value = data
      firstVisitDetailVisible.value = true
    } else {
      ElMessage.info("暂无首次随访记录")
    }
  } catch { /* handled */ }
}

// ==================== 后续随访（V15：通用弹窗组件） ====================
const followUpDialogVisible = ref(false)
const followUpRow = ref<any>(null)

function openFollowUpDialog(row: any) {
  followUpRow.value = row
  followUpDialogVisible.value = true
}

// ==================== 后续随访列表查看 ====================
const followUpListVisible = ref(false)
const followUpListData = ref<any[]>([])

async function viewFollowUpList(row: any) {
  try {
    const { data } = await getFollowUpListApi(row.id)
    followUpListData.value = data || []
    followUpListVisible.value = true
  } catch { /* handled */ }
}

// ==================== 服药管理 ====================
const medicationDialogVisible = ref(false)
const medicationRow = ref<any>(null)
const medicationForm = reactive({
  managementMethod: "",
  supervisor: "",
  sputumResult: "",
  stopDate: "",
  dayMarks: {} as MedicationRecordsMap
})

const printMedicationVisible = ref(false)

function openMedicationDialog(row: any) {
  medicationRow.value = row
  medicationForm.managementMethod = ""
  medicationForm.supervisor = ""
  medicationForm.sputumResult = ""
  medicationForm.stopDate = ""
  medicationForm.dayMarks = {}
  getMedicationApi(row.id).then(({ data }) => {
    if (data) {
      medicationForm.managementMethod = data.managementMethod || ""
      medicationForm.supervisor = data.supervisor || ""
      medicationForm.sputumResult = data.sputumResult || ""
      medicationForm.stopDate = data.stopDate || ""
      medicationForm.dayMarks = parseMedicationRecords(data.medicationRecords)
    }
  }).catch(() => { /* 首次填写 */ })
  medicationDialogVisible.value = true
}

function handlePrintMedication() {
  printMedicationVisible.value = true
}

async function handleSaveMedication() {
  try {
    const saveData: Record<string, any> = {
      patientId: medicationRow.value.id,
      populationType: "keyPopulation",
      managementMethod: medicationForm.managementMethod,
      supervisor: medicationForm.supervisor,
      sputumResult: medicationForm.sputumResult,
      stopDate: medicationForm.stopDate,
      medicationRecords: serializeMedicationRecords(medicationForm.dayMarks)
    }
    if (medicationForm.stopDate) {
      await completeMedicationApi(saveData)
      ElMessage.success("服药管理完成，患者已归档")
      medicationDialogVisible.value = false
      fetchData()
    } else {
      await saveMedicationApi(saveData)
      ElMessage.success("服药管理保存成功")
      medicationDialogVisible.value = false
      fetchData()
    }
  } catch { /* handled */ }
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

function handleActionCommand(command: string, row: any) {
  switch (command) {
    case "followUp": openFollowUpDialog(row)
      break
    case "followUpList": viewFollowUpList(row)
      break
    case "medication": openMedicationDialog(row)
      break
    case "printNotice": openPrintNotice(row)
      break
    case "printVisit": openPrintVisit(row)
      break
  }
}

// ==================== 打印功能 ====================
const printNoticeVisible = ref(false)
const printNoticeData = ref<any>(null)
const printVisitVisible = ref(false)
const printVisitData = ref<any>(null)
const printPatientName = ref("")

async function openPrintNotice(row: any) {
  const today = new Date().toISOString().slice(0, 10)
  try {
    const { data } = await getNoticeListByBizApi(row.id, "patient")
    const notice = data?.[0]
    if (notice) {
      printNoticeData.value = {
        ...notice,
        infectionDate: notice.infectionDate || row.screenDate || "",
        infectionMethod: notice.infectionMethod || row.screenMethod || "",
        infectionResultValue: notice.infectionResultValue || row.infectionResult || "",
        chestXrayDate: notice.chestXrayDate || row.chestXrayDate || "",
        chestXrayResult: notice.chestXrayResult || row.chestXrayResult || "",
        issuedTime: notice.issuedTime || today
      }
    } else {
      // 未发送通知单时，用筛查行数据填充，字段名映射到打印组件期望的字段
      printNoticeData.value = {
        ...row,
        infectionDate: row.screenDate || "",
        infectionMethod: row.screenMethod || "",
        infectionResultValue: row.infectionResult || "",
        issuedTime: today
      }
    }
    printNoticeVisible.value = true
  } catch {
    printNoticeData.value = {
      ...row,
      infectionDate: row.screenDate || "",
      infectionMethod: row.screenMethod || "",
      infectionResultValue: row.infectionResult || "",
      issuedTime: today
    }
    printNoticeVisible.value = true
  }
}

async function openPrintVisit(row: any) {
  try {
    const { data } = await getFirstVisitApi(row.id)
    if (!data) {
      ElMessage.info("暂无首次随访记录")
      return
    }
    printVisitData.value = data
    printPatientName.value = row.name
    printVisitVisible.value = true
  } catch {
    ElMessage.info("暂无首次随访记录")
  }
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
          <span class="text-lg font-bold">重点人群 — 患者管理</span>
          <div class="flex gap-2">
            <el-button type="success" :loading="exporting" @click="handleExport">
              导出 Excel
            </el-button>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleImportEpidemic">
              <el-button type="warning" v-permission="'keyPopulation:patient:importEpidemic'">
                导入大疫情表
              </el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600" :row-class-name="getPatientRowClass">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="诊断结果" />
        <el-table-column prop="source" label="来源">
          <template #default="{ row }">
            <el-tag :type="row.source === 'confirmed' ? 'danger' : 'warning'" size="small">
              {{ row.source === "confirmed" ? "诊断确诊" : "大疫情导入" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="患者通知单">
          <template #default="{ row }">
            <template v-if="row.noticeStatus === 1 || row.noticeStatus === 2">
              <el-button type="primary" link size="small" @click="viewNotice(row)">
                {{ row.name }}通知单
              </el-button>
              <el-tag v-if="row.noticeStatus === 2" type="success" size="small" class="ml-1">
                已确认
              </el-tag>
            </template>
            <el-tag v-else-if="row.noticeStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="首次随访">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewFirstVisit(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="info" link size="small" @click="viewScreeningDetail(row)">
                查看详情
              </el-button>
              <template v-if="row.noticeStatus === null || row.noticeStatus === undefined">
                <el-button v-permission="'keyPopulation:patient:sendNotice'" type="primary" link size="small" @click="openNoticeDialog(row)">
                  填写通知单
                </el-button>
              </template>
              <template v-else-if="row.noticeStatus === 0">
                <el-button v-permission="'keyPopulation:patient:sendNotice'" type="primary" link size="small" @click="openNoticeDialog(row)">
                  填写通知单
                </el-button>
                <el-button v-permission="'keyPopulation:patient:sendNotice'" type="success" link size="small" @click="openNoticeDialog(row)">
                  发送通知单
                </el-button>
              </template>
              <template v-else-if="row.noticeStatus === 2">
                <el-button v-permission="'keyPopulation:patient:sendNotice'" type="primary" link size="small" @click="openNoticeDialog(row)">
                  发送通知单
                </el-button>
              </template>
              <el-button v-permission="'keyPopulation:patient:firstVisit'" type="success" link size="small" :disabled="!!row.hasFirstVisit" @click="openFirstVisitDialog(row)">
                填写首次随访
              </el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleActionCommand(cmd, row)">
                <el-button type="primary" link size="small">
                  更多<el-icon class="el-icon--right">
                    <ArrowDown />
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-permission="'keyPopulation:patient:followUp'" command="followUp">
                      填写后续随访表
                    </el-dropdown-item>
                    <el-dropdown-item command="followUpList">
                      随访记录
                    </el-dropdown-item>
                    <el-dropdown-item v-permission="'keyPopulation:patient:medication'" command="medication" divided>
                      填写服药管理
                    </el-dropdown-item>
                    <el-dropdown-item command="printNotice" divided>
                      打印通知单
                    </el-dropdown-item>
                    <el-dropdown-item command="printVisit">
                      打印随访表
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button v-permission="'referral'" type="warning" link size="small" @click="openTierCare(row)">
                转诊
              </el-button>
            </div>
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

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="patient_key"
      population-type="key"
      module-type="patient"
      :subject-name="tierCareRow.name || ''"
    />

    <!-- 患者通知单弹窗 -->
    <el-dialog v-model="noticeDialogVisible" title="填写患者通知单" width="680px">
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
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="患者类型" prop="patientType" required>
              <el-select v-model="noticeForm.patientType" style="width: 100%">
                <el-option v-for="item in PATIENT_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理方式" prop="managementMethod" required>
              <el-select v-model="noticeForm.managementMethod" style="width: 100%">
                <el-option v-for="item in PATIENT_MANAGEMENT_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
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
            <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="noticeForm.treatmentPlan === '个体化方案'" label="方案详情">
          <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="2" />
        </el-form-item>
        <el-divider content-position="left">
          病原学检查
        </el-divider>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="痰涂片">
              <el-select v-model="noticeForm.sputumSmear" style="width: 100%">
                <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="痰培养">
              <el-select v-model="noticeForm.sputumCulture" style="width: 100%">
                <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分子检查">
              <el-select v-model="noticeForm.molecularTest" style="width: 100%">
                <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="病理学检查">
          <el-select v-model="noticeForm.pathologyTest" style="width: 100%">
            <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
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
              <el-date-picker v-model="noticeForm.issuedTime" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="其他注意事项">
          <el-input v-model="noticeForm.otherNotes" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="接收单位" prop="receiverOrgId" required>
          <el-select v-model="noticeForm.receiverOrgId" placeholder="请选择五级机构" filterable style="width: 100%">
            <el-option v-for="u in level5Users" :key="u.id" :label="`${u.realName || u.username} - ${u.orgName || '未设置机构'}`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">
          取消
        </el-button>
        <el-button :loading="submitting" @click="handleSaveDraft">
          保存草稿
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSendNotice">
          发送
        </el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情 -->
    <el-dialog v-model="noticeDetailVisible" title="患者通知单详情" width="700px">
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
          {{ noticeDetailData.crowdCategory || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">
          {{ noticeDetailData.currentAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">
          {{ noticeDetailData.householdAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="患者类型">
          {{ noticeDetailData.patientType || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="管理方式">
          {{ noticeDetailData.managementMethod || "-" }}
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
        <el-descriptions-item label="痰涂片">
          {{ noticeDetailData.sputumSmear || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="痰培养">
          {{ noticeDetailData.sputumCulture || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="分子检查">
          {{ noticeDetailData.molecularTest || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="病理学检查">
          {{ noticeDetailData.pathologyTest || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗机构">
          {{ noticeDetailData.treatmentInstitution || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="下发时间">
          {{ noticeDetailData.issuedTime || "-" }}
        </el-descriptions-item>
        <el-descriptions-item v-if="noticeDetailData.otherNotes" label="其他注意事项" :span="2">
          {{ noticeDetailData.otherNotes }}
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
          v-permission="'keyPopulation:patient:confirmNotice'"
          type="primary"
          @click="handleConfirmNotice(noticeDetailData.id)"
        >
          确认接收
        </el-button>
      </template>
    </el-dialog>

    <!-- 首次随访弹窗 -->
    <el-dialog v-model="firstVisitDialogVisible" title="肺结核患者第一次入户随访记录" width="920px" top="5vh">
      <el-form :model="firstVisitForm" label-width="110px" size="default">
        <el-row justify="end" class="form-no-row">
          <el-col :span="8">
            <el-form-item label="编号" label-width="60px">
              <el-input
                v-model="firstVisitForm.formNo"
                maxlength="8"
                placeholder="请输入8位编号"
                @input="firstVisitForm.formNo = sanitizeFirstVisitFormNo(firstVisitForm.formNo)"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">
          基本信息
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="随访时间">
              <el-date-picker v-model="firstVisitForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="随访方式">
              <el-radio-group v-model="firstVisitForm.visitMethod">
                <el-radio v-for="item in VISIT_METHOD_OPTIONS" :key="item" :value="item">
                  {{ item }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="firstVisitForm.visitMethod === VISIT_METHOD_OTHER" :span="8">
            <el-form-item label="随访方式-其他">
              <el-input v-model="firstVisitForm.visitMethodOther" placeholder="请填写" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="患者类型">
              <el-radio-group v-model="firstVisitForm.patientType">
                <el-radio value="初治">
                  初治
                </el-radio><el-radio value="复治">
                  复治
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="痰菌情况">
              <el-select v-model="firstVisitForm.sputumStatus" style="width: 100%">
                <el-option v-for="item in SPUTUM_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="耐药情况">
              <el-select v-model="firstVisitForm.drugResistance" style="width: 100%">
                <el-option v-for="item in DRUG_RESISTANCE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="症状及体征">
          <el-checkbox-group v-model="firstVisitForm.symptoms">
            <el-checkbox v-for="s in SYMPTOM_OPTIONS" :key="s.value" :value="s.value">
              {{ s.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="其他症状">
          <el-input v-model="firstVisitForm.otherSymptoms" placeholder="选填，如有其他症状请填写" />
        </el-form-item>
        <el-divider content-position="left">
          用药情况
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="化疗方案">
              <el-input v-model="firstVisitForm.chemotherapy" placeholder="来自病案首次治疗方案，可修改" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="用法">
              <el-radio-group v-model="firstVisitForm.medicationUsage">
                <el-radio v-for="item in MEDICATION_USAGE_OPTIONS" :key="item" :value="item">
                  {{ item }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="督导人员">
              <el-select v-model="firstVisitForm.supervisor" style="width: 100%">
                <el-option v-for="item in FIRST_VISIT_SUPERVISOR_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="药品剂型">
          <el-checkbox-group v-model="firstVisitForm.drugForm">
            <el-checkbox v-for="item in DRUG_FORM_OPTIONS" :key="item" :value="item">
              {{ item }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-divider content-position="left">
          居住环境与生活方式
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="单独居室">
              <el-radio-group v-model="firstVisitForm.separateRoom">
                <el-radio value="有">
                  有
                </el-radio><el-radio value="无">
                  无
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="通风情况">
              <el-select v-model="firstVisitForm.ventilation" style="width: 100%">
                <el-option v-for="item in VENTILATION_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="吸烟(支/天)">
              <el-input v-model="firstVisitForm.smokingAmount" placeholder="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="饮酒(两/天)">
              <el-input v-model="firstVisitForm.drinkingAmount" placeholder="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">
          健康教育及培训
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="取药地点">
              <el-input v-model="firstVisitForm.medicationLocation" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="取药时间">
              <el-date-picker v-model="firstVisitForm.medicationPickTime" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col v-for="item in EDUCATION_ITEMS" :key="item" :span="12">
            <el-form-item :label="item" label-width="170px">
              <el-radio-group v-model="firstVisitForm.educationItems[item]">
                <el-radio value="掌握">
                  掌握
                </el-radio><el-radio value="未掌握">
                  未掌握
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">
          其他
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="下次随访时间">
              <el-date-picker v-model="firstVisitForm.nextVisitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评估医生签名">
              <el-input v-model="firstVisitForm.doctorSignature" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          备注与附件
        </el-divider>
        <el-form-item label="备注">
          <el-input v-model="firstVisitForm.remarks" type="textarea" :rows="2" placeholder="请填写" />
        </el-form-item>
        <el-form-item label="上传10张">
          <ImageUploader v-model="firstVisitForm.attachmentUrls" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="firstVisitDialogVisible = false">
          取消
        </el-button>
        <el-button v-if="!firstVisitCompleted" @click="handleSaveFirstVisitDraft">
          保存草稿
        </el-button>
        <el-button type="primary" @click="handleSaveFirstVisit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 首次随访详情 -->
    <FirstVisitDetailDialog
      v-model:visible="firstVisitDetailVisible"
      :visit-data="firstVisitDetailData"
    />

    <!-- 后续随访弹窗（V15：通用组件，按《后续随访服务记录表》模板） -->
    <FollowUpVisitDialog
      v-model:visible="followUpDialogVisible"
      :patient-id="followUpRow?.id ?? null"
      :patient-name="followUpRow?.name"
      :patient-row="followUpRow"
      population-type="keyPopulation"
      @saved="fetchData"
    />

    <!-- 后续随访记录列表 -->
    <el-dialog v-model="followUpListVisible" title="患者随访汇总表" width="800px">
      <el-table :data="followUpListData" border stripe>
        <el-table-column prop="visitSeq" label="随访次数" />
        <el-table-column prop="visitDate" label="随访时间" />
        <el-table-column label="随访方式">
          <template #default="{ row }">
            {{ followUpFormatters.visitMethod(row.visitMethod, row.visitMethodOther) }}
          </template>
        </el-table-column>
        <el-table-column prop="visitSituation" label="随访情况" show-overflow-tooltip />
        <el-table-column prop="remarks" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="填写时间" />
      </el-table>
    </el-dialog>

    <!-- 服药管理弹窗 -->
    <el-dialog v-model="medicationDialogVisible" title="服药管理" width="700px">
      <el-form :model="medicationForm" label-width="130px">
        <el-form-item label="每日服药记录">
          <MedicationCalendar v-model="medicationForm.dayMarks" />
        </el-form-item>
        <el-form-item label="管理方式">
          <el-select v-model="medicationForm.managementMethod" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in MANAGEMENT_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="督导人员">
          <el-select v-model="medicationForm.supervisor" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in SUPERVISOR_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗前痰菌检查">
          <el-select v-model="medicationForm.sputumResult" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in SPUTUM_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="停止完成时间">
          <el-date-picker v-model="medicationForm.stopDate" type="date" placeholder="填写后患者将归档" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-alert v-if="medicationForm.stopDate" type="warning" :closable="false">
          填写停止完成时间后，该患者将从患者管理列表移除，放入历史患者。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="medicationDialogVisible = false">
          取消
        </el-button>
        <el-button @click="handlePrintMedication">
          打印治疗记录卡
        </el-button>
        <el-button type="primary" @click="handleSaveMedication">
          {{ medicationForm.stopDate ? "完成并归档" : "保存" }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 筛查详情弹窗 -->
    <ScreeningDetailDialog v-model:visible="screeningDetailVisible" type="keyPopulation" :data="screeningDetailData" />

    <!-- 打印通知单 -->
    <PrintNotice v-model:visible="printNoticeVisible" :notice-data="printNoticeData" notice-type="patient" />

    <!-- 打印治疗记录卡 -->
    <PrintMedication
      v-model:visible="printMedicationVisible"
      :patient-data="medicationRow"
      :medication-data="medicationForm"
    />

    <!-- 打印首次随访表 -->
    <PrintFirstVisit v-model:visible="printVisitVisible" :visit-data="printVisitData" :patient-name="printPatientName" />
  </div>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

</style>

<style lang="scss">
.el-table .overdue-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
