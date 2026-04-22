<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import {
  TRACKING_STATUS_MAP, REFERRAL_RESULT_OPTIONS,
  DIAGNOSIS_RESULT_OPTIONS, CHEST_XRAY_RESULT_OPTIONS
} from "@@/constants/disease"
import {
  getSuspectedListApi, trackSuspectedApi, referralSuspectedApi,
  submitXrayApi, importXrayApi
} from "./apis"
import { getScreeningSchoolDetailApi } from "@/pages/school/screening/apis"
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"

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
      populationType: "school",
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

/** 超期预警：追踪到位超过7天仍未录入胸片 */
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
  } catch { /* handled by interceptor */ } finally { submitting.value = false }
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
  xrayForm.hasChestXray = "是"
  xrayForm.chestXrayDate = ""
  xrayForm.chestXrayResult = ""
  xrayForm.diagnosisFirst = ""
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
  } catch { /* handled by interceptor */ } finally { submitting.value = false }
}

async function handleImportXray(uploadFile: any) {
  xrayImportLoading.value = true
  try {
    const { data } = await importXrayApi(uploadFile.raw, "school")
    ElMessage.success(`批量更新 ${data} 条胸片诊断数据`)
    fetchData()
  } catch {
    ElMessage.error("批量导入失败")
  } finally {
    xrayImportLoading.value = false
  }
}

// ==================== 转诊弹窗 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
const referralForm = reactive({ result: "", remark: "" })

function openReferralDialog(row: any) {
  referralRow.value = row
  const diagMap: Record<string, string> = {
    "排除": "excluded",
    "疑似肺结核": "suspected",
    "确诊患者": "confirmed",
    "潜伏感染者": "latent",
    "其他": "other"
  }
  referralForm.result = diagMap[row.diagnosisFirst] || ""
  referralForm.remark = ""
  referralDialogVisible.value = true
}

async function handleReferral() {
  if (!referralForm.result) {
    ElMessage.warning("请选择转诊结果")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await referralSuspectedApi({ id: referralRow.value.id, result: referralForm.result, remark: referralForm.remark })
    ElMessage.success("操作成功")
    referralDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally { submitting.value = false }
}

// ==================== 筛查详情查看 ====================
const screeningDetailVisible = ref(false)
const screeningDetailData = ref<any>(null)

async function viewScreeningDetail(row: any) {
  if (!row.screeningId) { ElMessage.info("暂无筛查原始数据"); return }
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
    <!-- 搜索栏 -->
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
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">学校人群 — 疑似结核管理</span>
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
        <el-table-column prop="diagnosisFirst" label="诊断结果" />
        <el-table-column label="转诊结果">
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
            <el-button type="info" link size="small" @click="viewScreeningDetail(row)">查看详情</el-button>
            <!-- 追踪 -->
            <el-button
              v-if="row.trackingStatus === 0 || row.trackingStatus === 2"
              v-permission="'latent:track'"
              type="primary"
              size="small"
              @click="openTrackDialog(row)"
            >
              追踪
            </el-button>
            <!-- 录入胸片+诊断（追踪到位后、转诊前） -->
            <el-button
              v-if="row.trackingStatus === 1 && !row.diagnosisFirst"
              v-permission="'latent:xray'"
              type="warning"
              size="small"
              @click="openXrayDialog(row)"
            >
              录入胸片诊断
            </el-button>
            <!-- 转诊（胸片已录入） -->
            <el-button
              v-if="row.trackingStatus === 1 && row.diagnosisFirst && !row.referralResult"
              v-permission="'latent:referral'"
              type="warning"
              size="small"
              @click="openReferralDialog(row)"
            >
              转诊
            </el-button>
            <!-- 已转诊提示 -->
            <el-tag v-if="row.referralResult" type="info" size="small">
              已转诊：{{ REFERRAL_RESULT_OPTIONS.find(o => o.value === row.referralResult)?.label }}
            </el-tag>
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

    <!-- 追踪弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="追踪状态">
          <el-radio-group v-model="trackForm.status">
            <el-radio :value="1">到位</el-radio>
            <el-radio :value="2">未到位</el-radio>
            <el-radio :value="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="trackForm.status === 3 || (trackForm.status === 2 && trackingRow?.notInPlaceCount >= 2)" label="备注原因">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" placeholder="请填写原因" />
        </el-form-item>
        <el-alert v-if="trackForm.status === 2 && trackingRow" :closable="false" class="mb-4">
          <template #default>当前已未到位 {{ trackingRow.notInPlaceCount }} 次，最多 3 次后自动归档</template>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleTrack">确认</el-button>
      </template>
    </el-dialog>

    <!-- 录入胸片+诊断弹窗 -->
    <el-dialog v-model="xrayDialogVisible" title="录入胸片检查与诊断结果" width="520px">
      <el-alert type="info" :closable="false" class="mb-4" description="追踪到位后，请录入胸片检查情况及诊断结果。系统将根据诊断结果自动引导后续流程。" />
      <el-form :model="xrayForm" label-width="110px">
        <el-form-item label="是否进行胸片检查">
          <el-radio-group v-model="xrayForm.hasChestXray">
            <el-radio value="是">是</el-radio>
            <el-radio value="否">否</el-radio>
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
        <el-button @click="xrayDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitXray">确认录入</el-button>
      </template>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <el-dialog v-model="referralDialogVisible" title="转诊操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="转诊结果">
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
        <el-button @click="referralDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReferral">确认</el-button>
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
