<script lang="ts" setup>
import ReferralDialog from "@@/components/ReferralDialog.vue"
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import {
  CHEST_XRAY_RESULT_OPTIONS,
  getSuspectedConfirmDiagnosisLabel,
  isConfirmedPatientDiagnosis,
  SCREENING_DIAGNOSIS_SEARCH_OPTIONS,
  SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS,
  TRACKING_STATUS_MAP
} from "@@/constants/disease"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { getScreeningKeyPopulationDetailApi } from "@/pages/key-population/screening/apis"
import {
  getSuspectedListApi,
  importXrayApi,
  submitDiagnosisApi,
  submitXrayOnlyApi,
  trackSuspectedApi
} from "./apis"

const POPULATION_TYPE = "keyPopulation"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  dateRange: [] as string[],
  trackingStatus: undefined as number | undefined,
  archived: undefined as number | undefined,
  diagnosisFirst: "" as string
})

async function fetchData() {
  loading.value = true
  try {
    const { dateRange, ...rest } = searchForm
    const params: Parameters<typeof getSuspectedListApi>[0] = {
      page: paginationData.currentPage ?? 1,
      size: paginationData.pageSize ?? 10,
      populationType: POPULATION_TYPE,
      name: rest.name || undefined,
      idNumber: rest.idNumber || undefined,
      phone: rest.phone || undefined,
      trackingStatus: rest.trackingStatus,
      archived: rest.archived,
      diagnosisFirst: rest.diagnosisFirst || undefined,
      ...extractDateRangeParams(dateRange)
    }
    if (!searchForm.diagnosisFirst && searchForm.archived === undefined) {
      params.referralResult = "pending"
    }
    const { data } = await getSuspectedListApi(params)
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
  searchForm.trackingStatus = undefined
  searchForm.archived = undefined
  searchForm.diagnosisFirst = ""
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
  if (isConfirmedPatientDiagnosis(row)) return "confirmed-row"
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

// ==================== 录入胸片结果弹窗（V13 拆分：仅胸片字段） ====================
const xrayDialogVisible = ref(false)
const xrayRow = ref<any>(null)
const xrayForm = reactive({
  hasChestXray: "是",
  chestXrayDate: "",
  chestXrayResult: ""
})
const xrayImportLoading = ref(false)

function openXrayDialog(row: any) {
  xrayRow.value = row
  xrayForm.hasChestXray = row.hasChestXray || "是"
  xrayForm.chestXrayDate = row.chestXrayDate || ""
  xrayForm.chestXrayResult = row.chestXrayResult || ""
  xrayDialogVisible.value = true
}

async function handleSubmitXray() {
  if (xrayForm.hasChestXray === "是" && !xrayForm.chestXrayResult) {
    ElMessage.warning("请选择胸片结果")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await submitXrayOnlyApi({
      id: xrayRow.value.id,
      hasChestXray: xrayForm.hasChestXray,
      chestXrayDate: xrayForm.chestXrayDate || undefined,
      chestXrayResult: xrayForm.chestXrayResult || undefined
    })
    ElMessage.success("胸片结果录入成功")
    xrayDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

// ==================== 确认诊断弹窗 ====================
const diagnosisDialogVisible = ref(false)
const diagnosisRow = ref<any>(null)
const diagnosisForm = reactive({ diagnosisFirst: "" })

function openDiagnosisDialog(row: any) {
  diagnosisRow.value = row
  diagnosisForm.diagnosisFirst = row.diagnosisFirst || row.screeningDiagnosisFirst || ""
  diagnosisDialogVisible.value = true
}

async function handleSubmitDiagnosis() {
  if (!diagnosisForm.diagnosisFirst) {
    ElMessage.warning("请选择确认诊断")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await submitDiagnosisApi({
      id: diagnosisRow.value.id,
      diagnosisFirst: diagnosisForm.diagnosisFirst
    })
    ElMessage.success("确认诊断成功")
    diagnosisDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

async function handleImportXray(uploadFile: any) {
  xrayImportLoading.value = true
  try {
    const { data } = await importXrayApi(uploadFile.raw, POPULATION_TYPE)
    ElMessage.success(`批量更新 ${data} 条胸片结果数据`)
    fetchData()
  } catch {
    ElMessage.error("批量导入失败")
  } finally {
    xrayImportLoading.value = false
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
    const { data } = await getScreeningKeyPopulationDetailApi(row.screeningId)
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
        <el-form-item label="诊断结果">
          <el-select v-model="searchForm.diagnosisFirst" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in SCREENING_DIAGNOSIS_SEARCH_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
          <span class="text-lg font-bold">重点人群 — 待诊断管理</span>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-change="handleImportXray"
          >
            <el-button v-permission="'latent:xray'" :loading="xrayImportLoading" size="small">
              批量导入胸片结果
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
        <el-table-column label="确认诊断">
          <template #default="{ row }">
            {{ getSuspectedConfirmDiagnosisLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="归档">
          <template #default="{ row }">
            <el-tag :type="row.archived ? (isConfirmedPatientDiagnosis(row) ? 'danger' : 'info') : 'success'" size="small">
              {{ row.archived ? (isConfirmedPatientDiagnosis(row) ? "结案" : "已归档") : "进行中" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
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
            <!-- 录入胸片结果（追踪到位后、胸片结果未录入时可操作） -->
            <el-button
              v-if="row.trackingStatus === 1 && !row.chestXrayResult && !row.referralResult"
              v-permission="'latent:xray'"
              type="warning"
              size="small"
              @click="openXrayDialog(row)"
            >
              录入胸片结果
            </el-button>
            <!-- 确认诊断（追踪到位后、尚未完成诊断分流时可操作） -->
            <el-button
              v-if="row.trackingStatus === 1 && !row.referralResult"
              v-permission="'latent:diagnosis'"
              type="warning"
              size="small"
              @click="openDiagnosisDialog(row)"
            >
              确认诊断
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
    <ScreeningDetailDialog v-model:visible="screeningDetailVisible" type="keyPopulation" :data="screeningDetailData" />

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

    <!-- 录入胸片结果弹窗（V13 拆分） -->
    <el-dialog v-model="xrayDialogVisible" title="录入胸片检查结果" width="520px">
      <el-alert type="info" :closable="false" class="mb-4" description="以下数据已自动从初始导入记录填充，请确认无误后点击确认。如有需要可修改后再提交。" />
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
          <el-form-item label="胸片结果" required>
            <el-select v-model="xrayForm.chestXrayResult" placeholder="请选择" style="width: 100%">
              <el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </template>
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

    <!-- 确认诊断弹窗 -->
    <el-dialog v-model="diagnosisDialogVisible" title="确认诊断" width="520px">
      <el-form :model="diagnosisForm" label-width="0">
        <el-form-item required>
          <el-select v-model="diagnosisForm.diagnosisFirst" placeholder="请选择诊断结果" style="width: 100%">
            <el-option v-for="item in SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="diagnosisDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitDiagnosis">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="suspected_key"
      population-type="key"
      module-type="suspected"
      :subject-name="tierCareRow.name || ''"
    />

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
.el-table .confirmed-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
