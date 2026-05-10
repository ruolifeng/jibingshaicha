<script lang="ts" setup>
import ReferralDialog from "@@/components/ReferralDialog.vue"
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import {
  ACTIVE_ROUND_MAP,
  CHEST_XRAY_RESULT_OPTIONS,
  DIAGNOSIS_RESULT_OPTIONS,
  REFERRAL_RESULT_OPTIONS,
  TRACKING_STATUS_MAP
} from "@@/constants/disease"
import {
  getScreeningCloseContactDetailApi,
  getScreeningCloseContactListApi,
  submitThreeMonthCheckApi,
  updateScreeningCloseContactApi
} from "@/pages/close-contact/screening/apis"
import {
  getSuspectedListApi,
  importXrayApi,
  referralSuspectedApi,
  submitXrayApi,
  trackSuspectedApi
} from "./apis"

const POPULATION_TYPE = "closeContact"

/** 主 Tab：待诊断 / 监测随访 */
const activeMainTab = ref<"suspected" | "monitoring">("suspected")

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
    const { data } = await getSuspectedListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: POPULATION_TYPE,
      referralResult: "pending",
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
  searchForm.trackingStatus = undefined
  searchForm.archived = undefined
  handleSearch()
}

const submitting = ref(false)

// 转诊
const tierCareVisible = ref(false)
const tierCareRow = ref<any>(null)
function openTierCare(row: any) {
  tierCareRow.value = row
  tierCareVisible.value = true
}

function getRowClass({ row }: { row: any }) {
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

function openTrackDialog(row: any) {
  trackingRow.value = row
  trackForm.status = 1
  trackForm.remark = ""
  trackDialogVisible.value = true
}

async function handleTrack() {
  if (submitting.value) return
  submitting.value = true
  try {
    await trackSuspectedApi({ id: trackingRow.value.id, status: trackForm.status, remark: trackForm.remark })
    ElMessage.success("操作成功")
    trackDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 录入胸片+诊断弹窗 ====================
const xrayDialogVisible = ref(false)
const xrayRow = ref<any>(null)
const xrayForm = reactive({
  hasChestXray: "是",
  chestXrayDate: "",
  chestXrayResult: "",
  diagnosisFirst: ""
})
const xrayImportLoading = ref(false)

function openXrayDialog(row: any) {
  xrayRow.value = row
  // 自动填充第一次导入的筛查数据，用户仅需确认
  xrayForm.hasChestXray = row.hasChestXray || "是"
  xrayForm.chestXrayDate = row.chestXrayDate || ""
  xrayForm.chestXrayResult = row.chestXrayResult || ""
  xrayForm.diagnosisFirst = row.diagnosisFirst || ""
  xrayDialogVisible.value = true
}

async function handleSubmitXray() {
  if (!xrayForm.diagnosisFirst) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await submitXrayApi({
      id: xrayRow.value.id,
      hasChestXray: xrayForm.hasChestXray,
      chestXrayDate: xrayForm.chestXrayDate || undefined,
      chestXrayResult: xrayForm.chestXrayResult || undefined,
      diagnosisFirst: xrayForm.diagnosisFirst
    })
    ElMessage.success("录入成功")
    xrayDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

async function handleImportXray(uploadFile: any) {
  xrayImportLoading.value = true
  try {
    const { data } = await importXrayApi(uploadFile.raw, POPULATION_TYPE)
    ElMessage.success(`批量更新 ${data} 条胸片诊断数据`)
    fetchData()
  } catch {
    ElMessage.error("批量导入失败")
  } finally {
    xrayImportLoading.value = false
  }
}

// ==================== 诊断弹窗 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
const referralForm = reactive({ result: "", remark: "" })

function openReferralDialog(row: any) {
  referralRow.value = row
  const diagMap: Record<string, string> = {
    排除: "excluded",
    疑似肺结核: "suspected",
    确诊患者: "confirmed",
    潜伏感染者: "latent",
    其他: "other"
  }
  referralForm.result = diagMap[row.diagnosisFirst] || ""
  referralForm.remark = ""
  referralDialogVisible.value = true
}

async function handleReferral() {
  if (!referralForm.result) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await referralSuspectedApi({ id: referralRow.value.id, result: referralForm.result, remark: referralForm.remark })
    ElMessage.success("操作成功")
    referralDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
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
    const { data } = await getScreeningCloseContactDetailApi(row.screeningId)
    if (data) {
      screeningDetailData.value = data
      screeningDetailVisible.value = true
    } else {
      ElMessage.info("暂无筛查原始数据")
    }
  } catch { /* handled by interceptor */ }
}

function getTrackingStatusType(status: number) {
  if (status === 1) return "success"
  if (status === 2 || status === 4) return "danger"
  if (status === 3) return "warning"
  return "info"
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)

// ==================== 监测随访模块 ====================
/** 监测随访子 Tab：未做（随访监测）/ 未发现异常 */
const activeMonitoringTab = ref<"notDone" | "normal">("notDone")

const { paginationData: mPagination, handleCurrentChange: mHandleCurrentChange, handleSizeChange: mHandleSizeChange } = usePagination()

const mLoading = ref(false)
const mTableData = ref<any[]>([])
const mTotal = ref(0)
const mSearchForm = reactive({ name: "", idNumber: "" })

const MONITORING_RESULT_MAP: Record<string, string> = {
  notDone: "未做",
  normal: "未发现异常"
}

const CC_STATUS_MAP: Record<number, { label: string, type: string }> = {
  6: { label: "待3月复查", type: "warning" },
  7: { label: "3月复查阴性-结束", type: "success" },
  8: { label: "3月复查阳性", type: "danger" }
}

function mTagType(t: string): "primary" | "success" | "info" | "warning" | "danger" {
  const allowed = ["primary", "success", "info", "warning", "danger"]
  return (allowed.includes(t) ? t : "info") as "primary" | "success" | "info" | "warning" | "danger"
}

function getFollowupTag(result: string | undefined): string {
  if (!result) return "info"
  if (result.includes("活动性肺结核")) return "danger"
  if (result.includes("潜伏感染者")) return "warning"
  return "success"
}

function hasFollowup(row: any, month: number) {
  return !!row[`followup${month}Result`]
}

function checkActiveInFollowup(row: any): number | null {
  for (const m of [6, 12, 24]) {
    const r = row[`followup${m}Result`]
    if (r && r.includes("活动性肺结核")) return m
  }
  return null
}

async function fetchMonitoringData() {
  mLoading.value = true
  try {
    const { data } = await getScreeningCloseContactListApi({
      page: mPagination.currentPage,
      size: mPagination.pageSize,
      name: mSearchForm.name || undefined,
      idNumber: mSearchForm.idNumber || undefined,
      finalScreeningResult: MONITORING_RESULT_MAP[activeMonitoringTab.value]
    })
    mTableData.value = data.records
    mTotal.value = data.total
  } finally {
    mLoading.value = false
  }
}

function handleMSearch() {
  mPagination.currentPage = 1
  fetchMonitoringData()
}
function handleMReset() {
  mSearchForm.name = ""
  mSearchForm.idNumber = ""
  handleMSearch()
}

watch([activeMonitoringTab, () => mPagination.currentPage, () => mPagination.pageSize], fetchMonitoringData)
watch(activeMainTab, (val: string) => {
  if (val === "monitoring") fetchMonitoringData()
})

// ==================== 3月复查 ====================
const threeMonthCheckVisible = ref(false)
const threeMonthCheckRow = ref<any>(null)
const threeMonthForm = reactive({
  checkDate: "",
  checkResult: "",
  finalResult: "阴性" as "阴性" | "阳性"
})

function openThreeMonthCheck(row: any) {
  threeMonthCheckRow.value = row
  Object.assign(threeMonthForm, {
    checkDate: row.threeMonthCheckDate || "",
    checkResult: row.threeMonthCheckResult || "",
    finalResult: row.threeMonthFinalResult || "阴性"
  })
  threeMonthCheckVisible.value = true
}

async function handleSubmitThreeMonthCheck() {
  if (!threeMonthForm.checkDate) {
    ElMessage.warning("请选择复查日期")
    return
  }
  submitting.value = true
  try {
    await submitThreeMonthCheckApi(threeMonthCheckRow.value.id, {
      checkDate: threeMonthForm.checkDate,
      checkResult: threeMonthForm.checkResult,
      finalResult: threeMonthForm.finalResult
    })
    ElMessage.success(
      threeMonthForm.finalResult === "阴性"
        ? "3月复查阴性，流程结束"
        : "3月复查阳性，已转入潜伏感染者管理流程"
    )
    threeMonthCheckVisible.value = false
    fetchMonitoringData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

// ==================== 随访结果录入 ====================
const followupInputVisible = ref(false)
const followupInputMonth = ref<6 | 12 | 24>(6)
const followupInputRow = ref<any>(null)
const followupDetailVisible = ref(false)
const followupDetailRow = ref<any>(null)

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
const FOLLOWUP_MONTHS = [6, 12, 24]

function viewFollowupDetail(row: any) {
  followupDetailRow.value = row
  followupDetailVisible.value = true
}

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
    const { data } = await getScreeningCloseContactDetailApi(followupInputRow.value.id)
    if (data) {
      followupDetailRow.value = data
      followupInputRow.value = data
    }
    fetchMonitoringData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <!-- 主 Tab 切换 -->
    <el-tabs v-model="activeMainTab" type="card" class="mb-4">
      <el-tab-pane label="待诊断" name="suspected" />
      <el-tab-pane label="监测随访" name="monitoring" />
    </el-tabs>

    <!-- ==================== 待诊断内容 ==================== -->
    <template v-if="activeMainTab === 'suspected'">
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="追踪状态">
          <el-select v-model="searchForm.trackingStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(label, key) in TRACKING_STATUS_MAP" :key="key" :label="label" :value="Number(key)" />
          </el-select>
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
          <span class="text-lg font-bold">密接人群 — 待诊断管理</span>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-change="handleImportXray"
          >
            <el-button v-permission="'latent:xray'" :loading="xrayImportLoading" size="small">
              批量导入胸片诊断
            </el-button>
          </el-upload>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600" :row-class-name="getRowClass">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" />
        <!-- 密接人群特有：阳性轮次 -->
        <el-table-column label="阳性轮次">
          <template #default="{ row }">
            {{ ACTIVE_ROUND_MAP[row.activeRound] || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="追踪状态">
          <template #default="{ row }">
            <el-tag :type="getTrackingStatusType(row.trackingStatus)" size="small">
              {{ TRACKING_STATUS_MAP[row.trackingStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notInPlaceCount" label="未到位次数" />
        <el-table-column prop="trackingRemark" label="追踪备注" />
        <el-table-column prop="chestXrayResult" label="胸片结果" />
        <el-table-column prop="diagnosisFirst" label="胸片诊断" />
        <el-table-column label="确认诊断">
          <template #default="{ row }">
            {{ REFERRAL_RESULT_OPTIONS.find(o => o.value === row.referralResult)?.label || row.referralResult || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="归档">
          <template #default="{ row }">
            <el-tag :type="row.archived ? 'info' : 'success'" size="small">
              {{ row.archived ? "已归档" : "进行中" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="300">
          <template #default="{ row }">
            <el-button type="info" link size="small" @click="viewScreeningDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="row.trackingStatus == null || row.trackingStatus === 0 || row.trackingStatus === 2"
              v-permission="'latent:track'"
              type="primary"
              size="small"
              @click="openTrackDialog(row)"
            >
              追踪
            </el-button>
            <!-- 录入胸片结果（追踪到位后、尚未录入诊断时可操作） -->
            <el-button
              v-if="row.trackingStatus === 1 && !row.diagnosisFirst && !row.referralResult"
              v-permission="'latent:xray'"
              type="warning"
              size="small"
              @click="openXrayDialog(row)"
            >
              录入胸片结果
            </el-button>
            <!-- 诊断（胸片已录入时可操作） -->
            <el-button
              v-if="row.trackingStatus === 1 && row.diagnosisFirst && !row.referralResult"
              v-permission="'latent:referral'"
              type="warning"
              size="small"
              @click="openReferralDialog(row)"
            >
              诊断
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
    <ScreeningDetailDialog v-model:visible="screeningDetailVisible" type="closeContact" :data="screeningDetailData" />

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="suspected_close"
      population-type="close"
      module-type="suspected"
      :subject-name="tierCareRow.name || ''"
    />

    </template>
    <!-- end 待诊断 -->

    <!-- ==================== 监测随访内容 ==================== -->
    <template v-if="activeMainTab === 'monitoring'">
      <!-- 监测随访子 Tab -->
      <el-tabs v-model="activeMonitoringTab" class="mb-4" @tab-change="() => { mPagination.currentPage = 1; fetchMonitoringData() }">
        <el-tab-pane label="未做（6/12/24月随访监测）" name="notDone" />
        <el-tab-pane label="未发现异常（3月复查）" name="normal" />
      </el-tabs>

      <!-- 搜索栏 -->
      <el-card shadow="never" class="mb-4">
        <el-form :model="mSearchForm" inline>
          <el-form-item label="姓名">
            <el-input v-model="mSearchForm.name" placeholder="请输入姓名" clearable />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="mSearchForm.idNumber" placeholder="请输入证件号" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleMSearch">
              搜索
            </el-button>
            <el-button @click="handleMReset">
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 未做（6/12/24月随访监测） -->
      <el-card v-if="activeMonitoringTab === 'notDone'" shadow="never">
        <template #header>
          <span class="text-lg font-bold">密接人群 — 未做（6/12/24月随访监测）</span>
        </template>
        <el-table v-loading="mLoading" :data="mTableData" border stripe max-height="600">
          <el-table-column prop="name" label="姓名" fixed />
          <el-table-column prop="idNumber" label="身份证号" min-width="150" />
          <el-table-column prop="registrationDate" label="登记日期" />
          <el-table-column prop="sourcePatientName" label="原患者" />
          <el-table-column label="6月随访">
            <template #default="{ row }">
              <div class="text-xs text-gray-400">
                到期：{{ row.followup6DueDate || '—' }}
              </div>
              <el-tag v-if="row.followup6Result" :type="mTagType(getFollowupTag(row.followup6Result))" size="small">
                {{ row.followup6Result }}
              </el-tag>
              <span v-else class="text-gray-400 text-xs">待完成</span>
            </template>
          </el-table-column>
          <el-table-column label="12月随访">
            <template #default="{ row }">
              <div class="text-xs text-gray-400">
                到期：{{ row.followup12DueDate || '—' }}
              </div>
              <el-tag v-if="row.followup12Result" :type="mTagType(getFollowupTag(row.followup12Result))" size="small">
                {{ row.followup12Result }}
              </el-tag>
              <span v-else class="text-gray-400 text-xs">待完成</span>
            </template>
          </el-table-column>
          <el-table-column label="24月随访">
            <template #default="{ row }">
              <div class="text-xs text-gray-400">
                到期：{{ row.followup24DueDate || '—' }}
              </div>
              <el-tag v-if="row.followup24Result" :type="mTagType(getFollowupTag(row.followup24Result))" size="small">
                {{ row.followup24Result }}
              </el-tag>
              <span v-else class="text-gray-400 text-xs">待完成</span>
            </template>
          </el-table-column>
          <el-table-column label="状态">
            <template #default="{ row }">
              <span v-if="checkActiveInFollowup(row)" class="text-red-500 font-bold">
                第{{ checkActiveInFollowup(row) }}月→患者管理
              </span>
              <span v-else-if="hasFollowup(row, 24)" class="text-green-600">全部完成</span>
              <span v-else class="text-gray-400">监测中</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="220">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewFollowupDetail(row)">
                查看/录入详情
              </el-button>
              <el-dropdown v-permission="'closeContact:latent:followup'" trigger="click" size="small">
                <el-button type="success" link size="small">
                  快速录入 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openFollowupInput(row, 6)">
                      录入 6月随访
                    </el-dropdown-item>
                    <el-dropdown-item @click="openFollowupInput(row, 12)">
                      录入 12月随访
                    </el-dropdown-item>
                    <el-dropdown-item @click="openFollowupInput(row, 24)">
                      录入 24月随访
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="mPagination.currentPage" v-model:page-size="mPagination.pageSize"
            :page-sizes="[10, 20, 50]" :total="mTotal" layout="total, sizes, prev, pager, next, jumper"
            @current-change="mHandleCurrentChange" @size-change="mHandleSizeChange"
          />
        </div>
      </el-card>

      <!-- 未发现异常（3月复查） -->
      <el-card v-else-if="activeMonitoringTab === 'normal'" shadow="never">
        <template #header>
          <span class="text-lg font-bold">密接人群 — 未发现异常（3月后复查）</span>
        </template>
        <el-table v-loading="mLoading" :data="mTableData" border stripe max-height="600">
          <el-table-column prop="name" label="姓名" fixed />
          <el-table-column prop="idNumber" label="身份证号" min-width="150" />
          <el-table-column prop="registrationDate" label="登记日期" />
          <el-table-column prop="sourcePatientName" label="原患者" />
          <el-table-column prop="infectionCheckMethod" label="初次感染检测方法" />
          <el-table-column prop="infectionCheckResult" label="初次感染检测结果" />
          <el-table-column label="3月复查">
            <template #default="{ row }">
              <div v-if="row.threeMonthCheckDate">
                <div class="text-xs text-gray-400">
                  复查日期：{{ row.threeMonthCheckDate }}
                </div>
                <el-tag :type="row.threeMonthFinalResult === '阴性' ? 'success' : 'danger'" size="small">
                  {{ row.threeMonthFinalResult }}
                </el-tag>
              </div>
              <el-tag v-if="CC_STATUS_MAP[row.ccStatus]" :type="mTagType(CC_STATUS_MAP[row.ccStatus].type)" size="small" class="ml-1">
                {{ CC_STATUS_MAP[row.ccStatus].label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="160">
            <template #default="{ row }">
              <el-button
                v-permission="'closeContact:latent:check'"
                :type="row.threeMonthCheckDate ? 'warning' : 'primary'"
                link
                size="small"
                @click="openThreeMonthCheck(row)"
              >
                {{ row.threeMonthCheckDate ? '修改3月复查' : '录入3月复查' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="mPagination.currentPage" v-model:page-size="mPagination.pageSize"
            :page-sizes="[10, 20, 50]" :total="mTotal" layout="total, sizes, prev, pager, next, jumper"
            @current-change="mHandleCurrentChange" @size-change="mHandleSizeChange"
          />
        </div>
      </el-card>

      <!-- 随访监测详情弹窗 -->
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
                  <el-tag :type="mTagType(getFollowupTag(followupDetailRow[`followup${month}Result`]))">
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

      <!-- 随访结果录入弹窗 -->
      <el-dialog
        v-model="followupInputVisible"
        :title="`录入 ${followupInputMonth} 月随访结果 — ${followupInputRow?.name}`"
        width="560px"
        :close-on-click-modal="false"
      >
        <el-form :model="followupInputForm" label-width="110px">
          <el-form-item label="实际筛查日期">
            <el-date-picker v-model="followupInputForm.screenDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择筛查日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结核症状">
            <el-input v-model="followupInputForm.symptom1" placeholder="如：咳嗽、无症状等" />
          </el-form-item>
          <el-form-item label="影像检查方法">
            <el-input v-model="followupInputForm.imagingMethod" placeholder="如：胸片、CT等" />
          </el-form-item>
          <el-form-item label="影像检查结果">
            <el-input v-model="followupInputForm.imagingResult" placeholder="如：未见异常等" />
          </el-form-item>
          <el-form-item label="病原学方法">
            <el-input v-model="followupInputForm.sputumMethod" placeholder="如：痰涂片等" />
          </el-form-item>
          <el-form-item label="病原学结果">
            <el-input v-model="followupInputForm.sputumResult" placeholder="如：阴性、阳性" />
          </el-form-item>
          <el-form-item label="筛查结果" required>
            <el-select v-model="followupInputForm.result" placeholder="请选择筛查结果" style="width: 100%">
              <el-option v-for="opt in FOLLOWUP_RESULT_OPTIONS" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </el-form-item>
          <el-alert
            v-if="followupInputForm.result === '活动性肺结核'"
            title="判定为活动性肺结核后，该记录将自动进入患者管理流程"
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

      <!-- 3月复查弹窗 -->
      <el-dialog v-model="threeMonthCheckVisible" title="录入3月复查感染检测结果" width="500px" :close-on-click-modal="false">
        <el-form :model="threeMonthForm" label-width="130px">
          <el-form-item label="姓名">
            <el-input :value="threeMonthCheckRow?.name" disabled />
          </el-form-item>
          <el-form-item label="3月复查日期">
            <el-date-picker v-model="threeMonthForm.checkDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="复查感染检测结果">
            <el-input v-model="threeMonthForm.checkResult" placeholder="如：PPD阴性、EC阴性、IGRA阴性等" />
          </el-form-item>
          <el-form-item label="最终判定">
            <el-radio-group v-model="threeMonthForm.finalResult">
              <el-radio value="阴性">
                阴性（非潜伏感染者，流程结束）
              </el-radio>
              <el-radio value="阳性">
                阳性（转入潜伏感染者管理流程）
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-alert
            v-if="threeMonthForm.finalResult === '阳性'"
            title="判定为阳性后，该记录将自动转入【潜伏感染者】流程"
            type="warning"
            :closable="false"
            show-icon
            class="mt-2"
          />
        </el-form>
        <template #footer>
          <el-button @click="threeMonthCheckVisible = false">
            取消
          </el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmitThreeMonthCheck">
            提交复查结果
          </el-button>
        </template>
      </el-dialog>
    </template>
    <!-- end 监测随访 -->

    <!-- 追踪弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="追踪状态">
          <el-radio-group v-model="trackForm.status">
            <el-radio :value="1">
              到位
            </el-radio>
            <el-radio :value="2">
              未到位
            </el-radio>
            <el-radio :value="3">
              其他
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="trackForm.status === 3 || (trackForm.status === 2 && trackingRow?.notInPlaceCount >= 2)" label="备注原因">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" placeholder="请填写原因" />
        </el-form-item>
        <el-alert v-if="trackForm.status === 2 && trackingRow" :closable="false" class="mb-4">
          <template #default>
            当前已未到位 {{ trackingRow.notInPlaceCount }} 次，最多 3 次后自动归档
          </template>
        </el-alert>
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

    <!-- 录入胸片+诊断弹窗 -->
    <el-dialog v-model="xrayDialogVisible" title="确认胸片检查结果" width="520px">
      <el-alert
        type="info"
        :closable="false"
        class="mb-4"
        :description="`${xrayRow ? `【${ACTIVE_ROUND_MAP[xrayRow.activeRound] || ''}轮次】` : ''}以下数据已自动从初始导入记录填充，请确认无误后点击确认。如有需要可修改后再提交。`"
      />
      <el-form :model="xrayForm" label-width="110px">
        <el-form-item label="是否进行胸片检查">
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
            <el-date-picker v-model="xrayForm.chestXrayDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
          </el-form-item>
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
          <div class="mt-1 text-xs text-gray-400">
            <span v-if="xrayForm.diagnosisFirst === '排除'">→ 归档</span>
            <span v-else-if="xrayForm.diagnosisFirst === '疑似肺结核' || xrayForm.diagnosisFirst === '确诊患者'">→ 进入患者管理</span>
            <span v-else-if="xrayForm.diagnosisFirst === '潜伏感染者'">→ 进入潜伏感染管理</span>
            <span v-else-if="xrayForm.diagnosisFirst === '其他'">→ 填写备注后归档</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="xrayDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitXray">
          确认录入
        </el-button>
      </template>
    </el-dialog>

    <!-- 诊断弹窗 -->
    <el-dialog v-model="referralDialogVisible" title="诊断操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="诊断结果">
          <el-radio-group v-model="referralForm.result">
            <el-radio v-for="item in REFERRAL_RESULT_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="referralForm.result === 'other'" label="备注原因">
          <el-input v-model="referralForm.remark" type="textarea" :rows="3" placeholder="请填写原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="referralDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleReferral">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
.mt-1 {
  margin-top: 4px;
}
</style>

<style lang="scss">
.el-table .overdue-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
