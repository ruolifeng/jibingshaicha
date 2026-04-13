<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { CROWD_CATEGORY_OPTIONS, TREATMENT_PLAN_OPTIONS, MANAGEMENT_METHOD_OPTIONS, SUPERVISOR_OPTIONS, SPUTUM_RESULT_OPTIONS, NOTICE_STATUS_MAP } from "@@/constants/disease"
import {
  getPatientListApi, importEpidemicApi, saveFirstVisitApi, getFirstVisitApi,
  saveFollowUpApi, getFollowUpListApi, saveMedicationApi, getMedicationApi, completeMedicationApi
} from "./apis"
import { sendNoticeApi, getNoticeListByBizApi } from "@/pages/school/latent/apis"

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
const noticeForm = reactive({
  currentAddress: "", householdAddress: "", idNumber: "", gender: "",
  birthDate: "", age: null as number | null, ethnicity: "",
  crowdCategory: "", treatmentPlan: "", customPlanDetail: ""
})

function openNoticeDialog(row: any) {
  noticeRow.value = row
  Object.assign(noticeForm, {
    currentAddress: row.currentAddress || "",
    householdAddress: row.householdAddress || "",
    idNumber: row.idNumber || "",
    gender: row.gender || "",
    birthDate: "", age: row.age || null, ethnicity: row.ethnicity || "",
    crowdCategory: "", treatmentPlan: "", customPlanDetail: ""
  })
  noticeDialogVisible.value = true
}

async function handleSendNotice() {
  try {
    await sendNoticeApi({
      noticeType: "patient",
      populationType: "closeContact",
      bizId: noticeRow.value.id,
      patientName: noticeRow.value.name,
      ...noticeForm,
      treatmentPlan: noticeForm.treatmentPlan === "个体化方案" ? noticeForm.customPlanDetail : noticeForm.treatmentPlan,
      senderId: 0
    })
    ElMessage.success("患者通知单发送成功")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
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
const firstVisitForm = reactive({ visitDate: "", visitContent: "" })

function openFirstVisitDialog(row: any) {
  firstVisitRow.value = row
  firstVisitForm.visitDate = ""
  firstVisitForm.visitContent = ""
  firstVisitDialogVisible.value = true
}

async function handleSaveFirstVisit() {
  try {
    await saveFirstVisitApi({
      patientId: firstVisitRow.value.id,
      populationType: "closeContact",
      visitDate: firstVisitForm.visitDate,
      visitContent: firstVisitForm.visitContent
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
const followUpForm = reactive({ visitDate: "", visitContent: "" })

function openFollowUpDialog(row: any) {
  followUpRow.value = row
  followUpForm.visitDate = ""
  followUpForm.visitContent = ""
  followUpDialogVisible.value = true
}

async function handleSaveFollowUp() {
  try {
    await saveFollowUpApi({
      patientId: followUpRow.value.id,
      populationType: "closeContact",
      visitDate: followUpForm.visitDate,
      visitContent: followUpForm.visitContent
    })
    ElMessage.success("后续随访保存成功")
    followUpDialogVisible.value = false
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
      medicationForm.checkedDates = data.medicationRecords ? data.medicationRecords.split(",") : []
    }
  }).catch(() => { /* 首次填写 */ })
  medicationDialogVisible.value = true
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
      medicationRecords: [...medicationForm.checkedDates].sort().join(",")
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
    }
  } catch { /* handled */ }
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

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" width="90" fixed />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="idNumber" label="证件号" width="180" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="diagnosisResult" label="诊断结果" width="100" />
        <el-table-column prop="source" label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="row.source === 'confirmed' ? 'danger' : 'warning'" size="small">
              {{ row.source === "confirmed" ? "转诊确诊" : "大疫情导入" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="患者通知单" width="130">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewNotice(row)">
              {{ row.name }}通知单
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="首次随访" width="110">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewFirstVisit(row)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" v-permission="'patient:sendNotice'" @click="openNoticeDialog(row)">发送通知单</el-button>
            <el-button type="success" size="small" v-permission="'patient:firstVisit'" @click="openFirstVisitDialog(row)">首次随访</el-button>
            <el-button type="warning" size="small" v-permission="'patient:followUp'" @click="openFollowUpDialog(row)">后续随访</el-button>
            <el-button size="small" @click="viewFollowUpList(row)">随访记录</el-button>
            <el-button type="danger" size="small" v-permission="'patient:medication'" @click="openMedicationDialog(row)">服药管理</el-button>
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
    <el-dialog v-model="noticeDialogVisible" title="填写患者通知单" width="600px">
      <el-form :model="noticeForm" label-width="100px">
        <el-form-item label="现居住地址"><el-input v-model="noticeForm.currentAddress" /></el-form-item>
        <el-form-item label="户籍地址"><el-input v-model="noticeForm.householdAddress" /></el-form-item>
        <el-form-item label="身份证"><el-input v-model="noticeForm.idNumber" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="noticeForm.gender"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="noticeForm.birthDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="年龄"><el-input-number v-model="noticeForm.age" :min="0" :max="150" /></el-form-item>
        <el-form-item label="民族"><el-input v-model="noticeForm.ethnicity" /></el-form-item>
        <el-form-item label="人群分类">
          <el-select v-model="noticeForm.crowdCategory">
            <el-option v-for="item in CROWD_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-select v-model="noticeForm.treatmentPlan">
            <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="noticeForm.treatmentPlan === '个体化方案'" label="方案详情">
          <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSendNotice">发送通知单</el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情 -->
    <el-dialog v-model="noticeDetailVisible" title="患者通知单详情" width="600px">
      <el-descriptions v-if="noticeDetailData" :column="2" border>
        <el-descriptions-item label="姓名">{{ noticeDetailData.patientName }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ noticeDetailData.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ noticeDetailData.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ noticeDetailData.age }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ noticeDetailData.treatmentPlan }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ noticeDetailData.sentTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="noticeDetailData.status === 2 ? 'success' : 'warning'" size="small">
            {{ NOTICE_STATUS_MAP[noticeDetailData.status] }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 首次随访弹窗 -->
    <el-dialog v-model="firstVisitDialogVisible" title="首次入户随访记录" width="600px">
      <el-form :model="firstVisitForm" label-width="100px">
        <el-form-item label="随访日期">
          <el-date-picker v-model="firstVisitForm.visitDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="随访内容">
          <el-input v-model="firstVisitForm.visitContent" type="textarea" :rows="6" placeholder="请填写首次入户随访记录内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="firstVisitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveFirstVisit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 首次随访详情 -->
    <el-dialog v-model="firstVisitDetailVisible" title="首次随访详情" width="600px">
      <el-descriptions v-if="firstVisitDetailData" :column="2" border>
        <el-descriptions-item label="随访日期">{{ firstVisitDetailData.visitDate }}</el-descriptions-item>
        <el-descriptions-item label="填写时间">{{ firstVisitDetailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="随访内容" :span="2">{{ firstVisitDetailData.visitContent }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 后续随访弹窗 -->
    <el-dialog v-model="followUpDialogVisible" title="后续随访记录" width="600px">
      <el-form :model="followUpForm" label-width="100px">
        <el-form-item label="随访日期">
          <el-date-picker v-model="followUpForm.visitDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="随访内容">
          <el-input v-model="followUpForm.visitContent" type="textarea" :rows="6" placeholder="请填写后续随访记录" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followUpDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveFollowUp">保存</el-button>
      </template>
    </el-dialog>

    <!-- 后续随访记录列表 -->
    <el-dialog v-model="followUpListVisible" title="后续随访记录列表" width="700px">
      <el-table :data="followUpListData" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="visitDate" label="随访日期" width="120" />
        <el-table-column prop="visitContent" label="随访内容" />
        <el-table-column prop="createTime" label="填写时间" width="180" />
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
          <el-select v-model="medicationForm.managementMethod" placeholder="请选择">
            <el-option v-for="item in MANAGEMENT_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="督导人员">
          <el-select v-model="medicationForm.supervisor" placeholder="请选择">
            <el-option v-for="item in SUPERVISOR_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗前痰菌检查">
          <el-select v-model="medicationForm.sputumResult" placeholder="请选择">
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
        <el-button type="primary" @click="handleSaveMedication">
          {{ medicationForm.stopDate ? "完成并归档" : "保存" }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }

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
