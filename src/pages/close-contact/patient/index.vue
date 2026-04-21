<script lang="ts" setup>
import { ArrowDown } from "@element-plus/icons-vue"
import PrintNotice from "@@/components/PrintNotice.vue"
import PrintFirstVisit from "@@/components/PrintFirstVisit.vue"
import { usePagination } from "@@/composables/usePagination"
import {
  CROWD_CATEGORY_OPTIONS, TREATMENT_PLAN_OPTIONS, MANAGEMENT_METHOD_OPTIONS, SUPERVISOR_OPTIONS,
  SPUTUM_RESULT_OPTIONS, NOTICE_STATUS_MAP, PATIENT_TYPE_OPTIONS, PATIENT_MANAGEMENT_METHOD_OPTIONS,
  PATHOGEN_RESULT_OPTIONS, CHEST_XRAY_RESULT_OPTIONS,
  VISIT_METHOD_OPTIONS, SPUTUM_STATUS_OPTIONS, DRUG_RESISTANCE_OPTIONS, SYMPTOM_OPTIONS,
  MEDICATION_USAGE_OPTIONS, DRUG_FORM_OPTIONS, FIRST_VISIT_SUPERVISOR_OPTIONS, VENTILATION_OPTIONS,
  EDUCATION_ITEMS
} from "@@/constants/disease"
import {
  getPatientListApi, importEpidemicApi, saveFirstVisitApi, getFirstVisitApi,
  saveFollowUpApi, getFollowUpListApi, saveMedicationApi, getMedicationApi, completeMedicationApi
} from "./apis"
import { sendNoticeApi, confirmNoticeApi, getNoticeListByBizApi } from "@/pages/school/latent/apis"
import { getLevel5UsersApi } from "@@/apis/users"
import { useUserStore } from "@/pinia/stores/user"

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

const searchForm = reactive({ name: "", idNumber: "" })

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getPatientListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: "closeContact",
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
  handleSearch()
}

async function handleImportEpidemic(uploadFile: any) {
  try {
    const { data } = await importEpidemicApi(uploadFile.raw, "closeContact")
    ElMessage.success(`成功导入 ${data} 条大疫情数据`)
    fetchData()
  } catch { /* handled */ }
}

// ==================== 患者通知单 ====================
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const submitting = ref(false)

function getPatientRowClass({ row }: { row: any }) {
  if (!row.archived && row.createTime) {
    const diffDays = (Date.now() - new Date(row.createTime).getTime()) / 86400000
    if (diffDays > 3) return "overdue-row"
  }
  return ""
}

const noticeForm = reactive({
  idNumber: "", gender: "", birthDate: "", age: null as number | null,
  ethnicity: "", phone: "", crowdCategory: "",
  currentAddress: "", householdAddress: "",
  chestXrayDate: "", chestXrayResult: "",
  treatmentInstitution: "", issuedTime: "",
  patientType: "", managementMethod: "",
  treatmentPlan: "", customPlanDetail: "",
  sputumSmear: "", sputumCulture: "", molecularTest: "", pathologyTest: "",
  otherNotes: "",
  receiverOrgId: undefined as number | undefined
})

function openNoticeDialog(row: any) {
  noticeRow.value = row
  Object.assign(noticeForm, {
    idNumber: row.idNumber || "", gender: row.gender || "",
    birthDate: "", age: row.age || null,
    ethnicity: row.ethnicity || "", phone: row.phone || "",
    crowdCategory: "", currentAddress: row.currentAddress || "", householdAddress: row.householdAddress || "",
    chestXrayDate: "", chestXrayResult: "",
    treatmentInstitution: "", issuedTime: "",
    patientType: "", managementMethod: "",
    treatmentPlan: "", customPlanDetail: "",
    sputumSmear: "", sputumCulture: "", molecularTest: "", pathologyTest: "",
    otherNotes: "", receiverOrgId: undefined
  })
  noticeDialogVisible.value = true
}

async function handleSendNotice() {
  if (submitting.value) return
  submitting.value = true
  try {
    await sendNoticeApi({
      noticeType: "patient",
      populationType: "closeContact",
      bizId: noticeRow.value.id,
      patientName: noticeRow.value.name,
      ...noticeForm,
      treatmentPlan: noticeForm.treatmentPlan === "个体化方案" ? noticeForm.customPlanDetail : noticeForm.treatmentPlan,
      senderId: userStore.userId
    })
    ElMessage.success("患者通知单发送成功")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled */ } finally { submitting.value = false }
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
const firstVisitForm = reactive({
  visitDate: "", visitMethod: "", patientType: "",
  sputumStatus: "", drugResistance: "", symptoms: [] as string[],
  otherSymptoms: "", chemotherapy: "", medicationUsage: "",
  drugForm: [] as string[], supervisor: "", separateRoom: "", ventilation: "",
  smokingAmount: "", drinkingAmount: "", medicationLocation: "",
  medicationPickTime: "", educationItems: {} as Record<string, string>,
  nextVisitDate: "", doctorSignature: ""
})

function openFirstVisitDialog(row: any) {
  firstVisitRow.value = row
  Object.assign(firstVisitForm, {
    visitDate: "", visitMethod: "", patientType: "",
    sputumStatus: "", drugResistance: "", symptoms: [],
    otherSymptoms: "", chemotherapy: "", medicationUsage: "",
    drugForm: [], supervisor: "", separateRoom: "", ventilation: "",
    smokingAmount: "", drinkingAmount: "", medicationLocation: "",
    medicationPickTime: "", educationItems: {}, nextVisitDate: "", doctorSignature: ""
  })
  firstVisitDialogVisible.value = true
}

async function handleSaveFirstVisit() {
  try {
    await saveFirstVisitApi({
      patientId: firstVisitRow.value.id,
      populationType: "closeContact",
      ...firstVisitForm,
      symptoms: firstVisitForm.symptoms.join(","),
      drugForm: firstVisitForm.drugForm.join(","),
      educationItems: JSON.stringify(firstVisitForm.educationItems)
    })
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

// ==================== 后续随访 ====================
const followUpDialogVisible = ref(false)
const followUpRow = ref<any>(null)
const followUpForm = reactive({ visitDate: "", visitMethod: "", visitSituation: "", remarks: "" })

function openFollowUpDialog(row: any) {
  followUpRow.value = row
  Object.assign(followUpForm, { visitDate: "", visitMethod: "", visitSituation: "", remarks: "" })
  followUpDialogVisible.value = true
}

async function handleSaveFollowUp() {
  try {
    await saveFollowUpApi({
      patientId: followUpRow.value.id,
      populationType: "closeContact",
      ...followUpForm
    })
    ElMessage.success("后续随访保存成功")
    followUpDialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
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
  checkedDates: [] as string[]
})

const calendarMonth = ref(new Date())
const calendarDays = computed(() => {
  const year = calendarMonth.value.getFullYear()
  const month = calendarMonth.value.getMonth()
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const days: { date: string, day: number, blank: boolean }[] = []
  for (let i = 0; i < firstDay; i++) days.push({ date: "", day: 0, blank: true })
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`
    days.push({ date: dateStr, day: d, blank: false })
  }
  return days
})
const calendarTitle = computed(() => {
  const y = calendarMonth.value.getFullYear()
  const m = calendarMonth.value.getMonth() + 1
  return `${y}年${m}月`
})
function prevMonth() {
  const d = new Date(calendarMonth.value)
  d.setMonth(d.getMonth() - 1)
  calendarMonth.value = d
}
function nextMonth() {
  const d = new Date(calendarMonth.value)
  d.setMonth(d.getMonth() + 1)
  calendarMonth.value = d
}
function toggleDate(dateStr: string) {
  const idx = medicationForm.checkedDates.indexOf(dateStr)
  if (idx >= 0) medicationForm.checkedDates.splice(idx, 1)
  else medicationForm.checkedDates.push(dateStr)
}
function isDateChecked(dateStr: string) {
  return medicationForm.checkedDates.includes(dateStr)
}

function openMedicationDialog(row: any) {
  medicationRow.value = row
  medicationForm.managementMethod = ""
  medicationForm.supervisor = ""
  medicationForm.sputumResult = ""
  medicationForm.stopDate = ""
  medicationForm.checkedDates = []
  calendarMonth.value = new Date()
  getMedicationApi(row.id).then(({ data }) => {
    if (data) {
      medicationForm.managementMethod = data.managementMethod || ""
      medicationForm.supervisor = data.supervisor || ""
      medicationForm.sputumResult = data.sputumResult || ""
      medicationForm.stopDate = data.stopDate || ""
      try {
        medicationForm.checkedDates = data.medicationRecords
          ? (typeof data.medicationRecords === "string"
            ? JSON.parse(data.medicationRecords)
            : data.medicationRecords)
          : []
      } catch { medicationForm.checkedDates = [] }
    }
  }).catch(() => { /* 首次填写 */ })
  medicationDialogVisible.value = true
}

function handlePrintMedication() {
  const printContent = document.querySelector(".med-calendar")
  if (!printContent) return
  const printWindow = window.open("", "_blank")
  if (!printWindow) return
  const patientName = medicationRow.value?.name || ""
  printWindow.document.write(`
    <html><head><title>治疗记录卡 - ${patientName}</title>
    <style>
      body { font-family: sans-serif; padding: 20px; }
      h2 { text-align: center; }
      .info { margin-bottom: 16px; }
      .info span { margin-right: 24px; }
      .med-calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
      .med-calendar-cell { border: 1px solid #ccc; padding: 4px; text-align: center; min-height: 40px; }
      .checked { background: #e6f7e6; }
      .med-calendar-weekdays { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-weight: bold; margin-bottom: 4px; }
    </style></head><body>
    <h2>肺结核患者治疗记录卡</h2>
    <div class="info">
      <span>姓名：${patientName}</span>
      <span>管理方式：${medicationForm.managementMethod || ""}</span>
      <span>督导人员：${medicationForm.supervisor || ""}</span>
      <span>痰菌检查：${medicationForm.sputumResult || ""}</span>
    </div>
    ${printContent.outerHTML}
    </body></html>
  `)
  printWindow.document.close()
  printWindow.focus()
  printWindow.print()
  printWindow.close()
}

async function handleSaveMedication() {
  try {
    const saveData: Record<string, any> = {
      patientId: medicationRow.value.id,
      populationType: "closeContact",
      managementMethod: medicationForm.managementMethod,
      supervisor: medicationForm.supervisor,
      sputumResult: medicationForm.sputumResult,
      stopDate: medicationForm.stopDate,
      medicationRecords: JSON.stringify([...medicationForm.checkedDates].sort())
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

function handleActionCommand(command: string, row: any) {
  switch (command) {
    case "followUp": openFollowUpDialog(row); break
    case "followUpList": viewFollowUpList(row); break
    case "medication": openMedicationDialog(row); break
    case "printNotice": openPrintNotice(row); break
    case "printVisit": openPrintVisit(row); break
  }
}

// ==================== 打印功能 ====================
const printNoticeVisible = ref(false)
const printNoticeData = ref<any>(null)
const printVisitVisible = ref(false)
const printVisitData = ref<any>(null)
const printPatientName = ref("")

async function openPrintNotice(row: any) {
  try {
    const { data } = await getNoticeListByBizApi(row.id, "patient")
    printNoticeData.value = data?.[0] || row
    printNoticeVisible.value = true
  } catch {
    printNoticeData.value = row
    printNoticeVisible.value = true
  }
}

async function openPrintVisit(row: any) {
  try {
    const { data } = await getFirstVisitApi(row.id)
    if (!data) { ElMessage.info("暂无首次随访记录"); return }
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">密接人群 — 患者管理</span>
          <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleImportEpidemic">
            <el-button type="warning" v-permission="'patient:importEpidemic'">导入大疫情表</el-button>
          </el-upload>
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
              {{ row.source === "confirmed" ? "转诊确诊" : "大疫情导入" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="患者通知单">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewNotice(row)">
              {{ row.name }}通知单
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="首次随访">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewFirstVisit(row)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-permission="'patient:sendNotice'" type="primary" link size="small" @click="openNoticeDialog(row)">发送通知单</el-button>
              <el-button v-permission="'patient:firstVisit'" type="success" link size="small" @click="openFirstVisitDialog(row)">填写首次随访</el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleActionCommand(cmd, row)">
                <el-button type="primary" link size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="followUp">填写后续随访表</el-dropdown-item>
                    <el-dropdown-item command="followUpList">随访记录</el-dropdown-item>
                    <el-dropdown-item command="medication" divided>填写服药管理</el-dropdown-item>
                    <el-dropdown-item command="printNotice" divided>打印通知单</el-dropdown-item>
                    <el-dropdown-item command="printVisit">打印随访表</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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

    <!-- 患者通知单弹窗 -->
    <el-dialog v-model="noticeDialogVisible" title="填写患者通知单" width="680px">
      <el-form :model="noticeForm" label-width="110px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="姓名"><el-input :value="noticeRow?.name" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证"><el-input v-model="noticeForm.idNumber" /></el-form-item></el-col>
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
          <el-col :span="24"><el-form-item label="人群分类"><el-select v-model="noticeForm.crowdCategory" style="width: 100%"><el-option v-for="item in CROWD_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="24"><el-form-item label="现居住地址"><el-input v-model="noticeForm.currentAddress" placeholder="请输入现居住地址" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="户籍地址"><el-input v-model="noticeForm.householdAddress" placeholder="请输入户籍地址" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="患者类型"><el-select v-model="noticeForm.patientType" style="width: 100%"><el-option v-for="item in PATIENT_TYPE_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="管理方式"><el-select v-model="noticeForm.managementMethod" style="width: 100%"><el-option v-for="item in PATIENT_MANAGEMENT_METHOD_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">胸片检查</el-divider>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="胸片检查时间"><el-date-picker v-model="noticeForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="胸片检查结果"><el-select v-model="noticeForm.chestXrayResult" style="width: 100%"><el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">治疗方案</el-divider>
        <el-form-item label="治疗方案"><el-select v-model="noticeForm.treatmentPlan" style="width: 100%"><el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item v-if="noticeForm.treatmentPlan === '个体化方案'" label="方案详情"><el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="2" /></el-form-item>
        <el-divider content-position="left">病原学检查</el-divider>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="痰涂片"><el-select v-model="noticeForm.sputumSmear" style="width: 100%"><el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="痰培养"><el-select v-model="noticeForm.sputumCulture" style="width: 100%"><el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="分子检查"><el-select v-model="noticeForm.molecularTest" style="width: 100%"><el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="病理学检查"><el-select v-model="noticeForm.pathologyTest" style="width: 100%"><el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-divider content-position="left">机构信息</el-divider>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="治疗机构"><el-input v-model="noticeForm.treatmentInstitution" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="下发时间"><el-date-picker v-model="noticeForm.issuedTime" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="其他注意事项"><el-input v-model="noticeForm.otherNotes" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="接收单位"><el-select v-model="noticeForm.receiverOrgId" placeholder="请选择五级机构" filterable style="width: 100%"><el-option v-for="u in level5Users" :key="u.id" :label="`${u.realName || u.username} - ${u.orgName || '未设置机构'}`" :value="u.id" /></el-select></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSendNotice">发送通知单</el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情 -->
    <el-dialog v-model="noticeDetailVisible" title="患者通知单详情" width="700px">
      <el-descriptions v-if="noticeDetailData" :column="2" border>
        <el-descriptions-item label="姓名">{{ noticeDetailData.patientName }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ noticeDetailData.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ noticeDetailData.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ noticeDetailData.age }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ noticeDetailData.phone || "-" }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ noticeDetailData.ethnicity || "-" }}</el-descriptions-item>
        <el-descriptions-item label="人群分类">{{ noticeDetailData.crowdCategory || "-" }}</el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">{{ noticeDetailData.currentAddress || "-" }}</el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">{{ noticeDetailData.householdAddress || "-" }}</el-descriptions-item>
        <el-descriptions-item label="患者类型">{{ noticeDetailData.patientType || "-" }}</el-descriptions-item>
        <el-descriptions-item label="管理方式">{{ noticeDetailData.managementMethod || "-" }}</el-descriptions-item>
        <el-descriptions-item label="胸片检查时间">{{ noticeDetailData.chestXrayDate || "-" }}</el-descriptions-item>
        <el-descriptions-item label="胸片检查结果">{{ noticeDetailData.chestXrayResult || "-" }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ noticeDetailData.treatmentPlan || "-" }}</el-descriptions-item>
        <el-descriptions-item label="痰涂片">{{ noticeDetailData.sputumSmear || "-" }}</el-descriptions-item>
        <el-descriptions-item label="痰培养">{{ noticeDetailData.sputumCulture || "-" }}</el-descriptions-item>
        <el-descriptions-item label="分子检查">{{ noticeDetailData.molecularTest || "-" }}</el-descriptions-item>
        <el-descriptions-item label="病理学检查">{{ noticeDetailData.pathologyTest || "-" }}</el-descriptions-item>
        <el-descriptions-item label="治疗机构">{{ noticeDetailData.treatmentInstitution || "-" }}</el-descriptions-item>
        <el-descriptions-item label="下发时间">{{ noticeDetailData.issuedTime || "-" }}</el-descriptions-item>
        <el-descriptions-item v-if="noticeDetailData.otherNotes" label="其他注意事项" :span="2">{{ noticeDetailData.otherNotes }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ noticeDetailData.sentTime }}</el-descriptions-item>
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
          v-permission="'patient:confirmNotice'"
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
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="随访时间"><el-date-picker v-model="firstVisitForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="随访方式"><el-radio-group v-model="firstVisitForm.visitMethod"><el-radio v-for="item in VISIT_METHOD_OPTIONS" :key="item" :value="item">{{ item }}</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="患者类型"><el-radio-group v-model="firstVisitForm.patientType"><el-radio value="初治">初治</el-radio><el-radio value="复治">复治</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="痰菌情况"><el-select v-model="firstVisitForm.sputumStatus" style="width: 100%"><el-option v-for="item in SPUTUM_STATUS_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="耐药情况"><el-select v-model="firstVisitForm.drugResistance" style="width: 100%"><el-option v-for="item in DRUG_RESISTANCE_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="其他症状"><el-input v-model="firstVisitForm.otherSymptoms" placeholder="如有其他症状请填写" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="症状及体征"><el-checkbox-group v-model="firstVisitForm.symptoms"><el-checkbox v-for="s in SYMPTOM_OPTIONS" :key="s.value" :value="s.value">{{ s.label }}</el-checkbox></el-checkbox-group></el-form-item>
        <el-divider content-position="left">用药情况</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="化疗方案"><el-input v-model="firstVisitForm.chemotherapy" placeholder="化疗方案" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="用法"><el-radio-group v-model="firstVisitForm.medicationUsage"><el-radio v-for="item in MEDICATION_USAGE_OPTIONS" :key="item" :value="item">{{ item }}</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="督导人员"><el-select v-model="firstVisitForm.supervisor" style="width: 100%"><el-option v-for="item in FIRST_VISIT_SUPERVISOR_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="药品剂型"><el-checkbox-group v-model="firstVisitForm.drugForm"><el-checkbox v-for="item in DRUG_FORM_OPTIONS" :key="item" :value="item">{{ item }}</el-checkbox></el-checkbox-group></el-form-item>
        <el-divider content-position="left">居住环境与生活方式</el-divider>
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="单独居室"><el-radio-group v-model="firstVisitForm.separateRoom"><el-radio value="有">有</el-radio><el-radio value="无">无</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="通风情况"><el-select v-model="firstVisitForm.ventilation" style="width: 100%"><el-option v-for="item in VENTILATION_OPTIONS" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="吸烟(支/天)"><el-input v-model="firstVisitForm.smokingAmount" placeholder="0" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="饮酒(两/天)"><el-input v-model="firstVisitForm.drinkingAmount" placeholder="0" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">健康教育及培训</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="取药地点"><el-input v-model="firstVisitForm.medicationLocation" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="取药时间"><el-date-picker v-model="firstVisitForm.medicationPickTime" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col v-for="item in EDUCATION_ITEMS" :key="item" :span="12">
            <el-form-item :label="item" label-width="170px"><el-radio-group v-model="firstVisitForm.educationItems[item]"><el-radio value="掌握">掌握</el-radio><el-radio value="未掌握">未掌握</el-radio></el-radio-group></el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">其他</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="下次随访时间"><el-date-picker v-model="firstVisitForm.nextVisitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="评估医生签名"><el-input v-model="firstVisitForm.doctorSignature" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="firstVisitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveFirstVisit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 首次随访详情 -->
    <el-dialog v-model="firstVisitDetailVisible" title="首次入户随访记录详情" width="860px">
      <el-descriptions v-if="firstVisitDetailData" :column="3" border size="small">
        <el-descriptions-item label="随访时间">{{ firstVisitDetailData.visitDate }}</el-descriptions-item>
        <el-descriptions-item label="随访方式">{{ firstVisitDetailData.visitMethod || "-" }}</el-descriptions-item>
        <el-descriptions-item label="患者类型">{{ firstVisitDetailData.patientType || "-" }}</el-descriptions-item>
        <el-descriptions-item label="痰菌情况">{{ firstVisitDetailData.sputumStatus || "-" }}</el-descriptions-item>
        <el-descriptions-item label="耐药情况">{{ firstVisitDetailData.drugResistance || "-" }}</el-descriptions-item>
        <el-descriptions-item label="督导人员">{{ firstVisitDetailData.supervisor || "-" }}</el-descriptions-item>
        <el-descriptions-item label="症状及体征" :span="3">{{ firstVisitDetailData.symptoms || "-" }}</el-descriptions-item>
        <el-descriptions-item label="化疗方案">{{ firstVisitDetailData.chemotherapy || "-" }}</el-descriptions-item>
        <el-descriptions-item label="用法">{{ firstVisitDetailData.medicationUsage || "-" }}</el-descriptions-item>
        <el-descriptions-item label="药品剂型">{{ firstVisitDetailData.drugForm || "-" }}</el-descriptions-item>
        <el-descriptions-item label="单独居室">{{ firstVisitDetailData.separateRoom || "-" }}</el-descriptions-item>
        <el-descriptions-item label="通风情况">{{ firstVisitDetailData.ventilation || "-" }}</el-descriptions-item>
        <el-descriptions-item label="吸烟量">{{ firstVisitDetailData.smokingAmount || "-" }} 支/天</el-descriptions-item>
        <el-descriptions-item label="饮酒量">{{ firstVisitDetailData.drinkingAmount || "-" }} 两/天</el-descriptions-item>
        <el-descriptions-item label="下次随访">{{ firstVisitDetailData.nextVisitDate || "-" }}</el-descriptions-item>
        <el-descriptions-item label="评估医生">{{ firstVisitDetailData.doctorSignature || "-" }}</el-descriptions-item>
        <el-descriptions-item label="填写时间" :span="3">{{ firstVisitDetailData.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 后续随访弹窗 -->
    <el-dialog v-model="followUpDialogVisible" title="填写后续随访记录" width="560px">
      <el-form :model="followUpForm" label-width="100px">
        <el-form-item label="随访时间"><el-date-picker v-model="followUpForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="随访方式"><el-radio-group v-model="followUpForm.visitMethod"><el-radio v-for="item in VISIT_METHOD_OPTIONS" :key="item" :value="item">{{ item }}</el-radio></el-radio-group></el-form-item>
        <el-form-item label="随访情况"><el-input v-model="followUpForm.visitSituation" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="followUpForm.remarks" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followUpDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveFollowUp">保存</el-button>
      </template>
    </el-dialog>

    <!-- 后续随访记录列表 -->
    <el-dialog v-model="followUpListVisible" title="患者随访汇总表" width="800px">
      <el-table :data="followUpListData" border stripe>
        <el-table-column prop="visitSeq" label="随访次数" width="80" />
        <el-table-column prop="visitDate" label="随访时间" width="110" />
        <el-table-column prop="visitMethod" label="随访方式" width="80" />
        <el-table-column prop="visitSituation" label="随访情况" show-overflow-tooltip />
        <el-table-column prop="remarks" label="备注" width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="填写时间" width="160" />
      </el-table>
    </el-dialog>

    <!-- 服药管理弹窗 -->
    <el-dialog v-model="medicationDialogVisible" title="服药管理" width="700px">
      <el-form :model="medicationForm" label-width="130px">
        <el-form-item label="每日服药记录">
          <div class="med-calendar">
            <div class="med-calendar-header">
              <el-button text @click="prevMonth">&lt;</el-button>
              <span class="med-calendar-title">{{ calendarTitle }}</span>
              <el-button text @click="nextMonth">&gt;</el-button>
            </div>
            <div class="med-calendar-weekdays">
              <span v-for="w in ['日', '一', '二', '三', '四', '五', '六']" :key="w">{{ w }}</span>
            </div>
            <div class="med-calendar-grid">
              <div
                v-for="(cell, idx) in calendarDays"
                :key="idx"
                class="med-calendar-cell"
                :class="{ blank: cell.blank, checked: !cell.blank && isDateChecked(cell.date) }"
                @click="!cell.blank && toggleDate(cell.date)"
              >
                <template v-if="!cell.blank">
                  <span class="day-num">{{ cell.day }}</span>
                  <span v-if="isDateChecked(cell.date)" class="check-mark">✓</span>
                </template>
              </div>
            </div>
            <div class="med-calendar-summary">
              已服药 <strong>{{ medicationForm.checkedDates.length }}</strong> 天
            </div>
          </div>
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
        <el-button @click="medicationDialogVisible = false">取消</el-button>
        <el-button @click="handlePrintMedication">打印治疗记录卡</el-button>
        <el-button type="primary" @click="handleSaveMedication">
          {{ medicationForm.stopDate ? "完成并归档" : "保存" }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 打印通知单 -->
    <PrintNotice v-model:visible="printNoticeVisible" :notice-data="printNoticeData" notice-type="patient" />

    <!-- 打印首次随访表 -->
    <PrintFirstVisit v-model:visible="printVisitVisible" :visit-data="printVisitData" :patient-name="printPatientName" />
  </div>
</template>

<style lang="scss" scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }

.action-btns {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.med-calendar {
  width: 100%;
  &-header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    margin-bottom: 8px;
  }
  &-title { font-size: 16px; font-weight: bold; }
  &-weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    text-align: center;
    font-size: 13px;
    color: #909399;
    margin-bottom: 4px;
  }
  &-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
  }
  &-cell {
    aspect-ratio: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;
    position: relative;
    &.blank { border-color: transparent; cursor: default; }
    &.checked {
      background: #67c23a;
      border-color: #67c23a;
      color: #fff;
    }
    &:not(.blank):hover { border-color: #409eff; }
    .check-mark { font-size: 16px; font-weight: bold; line-height: 1; }
    .day-num { line-height: 1.2; }
  }
  &-summary {
    margin-top: 8px;
    text-align: center;
    font-size: 14px;
    color: #606266;
  }
}
</style>

<style lang="scss">
.el-table .overdue-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
