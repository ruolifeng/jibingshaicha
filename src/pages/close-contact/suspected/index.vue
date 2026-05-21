<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import {
  getScreeningCloseContactDetailApi,
  getScreeningCloseContactListApi,
  submitThreeMonthCheckApi,
  updateScreeningCloseContactApi
} from "@/pages/close-contact/screening/apis"

defineOptions({ name: "CloseContactMonitoring" })

/** 监测随访子 Tab：6/12/24月随访监测 / 未发现异常3月复查 */
const activeMonitoringTab = ref<"followup" | "normal">("followup")

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const searchForm = reactive({ name: "", idNumber: "" })

const MONITORING_QUERY_MAP: Record<"followup" | "normal", { ccStatus?: number, finalScreeningResult?: string }> = {
  /** 含「未做」及潜伏感染者未完成治疗转入的 6/12/24 月随访监测 */
  followup: { ccStatus: 4 },
  normal: { finalScreeningResult: "未发现异常" }
}

const CC_STATUS_MAP: Record<number, { label: string, type: string }> = {
  6: { label: "待3月复查", type: "warning" },
  7: { label: "3月复查阴性-结束", type: "success" },
  8: { label: "3月复查阳性", type: "danger" }
}

function tagType(t: string): "primary" | "success" | "info" | "warning" | "danger" {
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

async function fetchData() {
  loading.value = true
  try {
    const query = MONITORING_QUERY_MAP[activeMonitoringTab.value]
    const { data } = await getScreeningCloseContactListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      ...query
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

watch([activeMonitoringTab, () => paginationData.currentPage, () => paginationData.pageSize], fetchData, { immediate: true })

const submitting = ref(false)

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
    fetchData()
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
  getScreeningCloseContactDetailApi(row.id).then(({ data }) => {
    followupDetailRow.value = data || row
    followupDetailVisible.value = true
  }).catch(() => {
    followupDetailRow.value = row
    followupDetailVisible.value = true
  })
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
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <!-- 子 Tab：未做 / 未发现异常 -->
    <el-tabs v-model="activeMonitoringTab" class="mb-4" @tab-change="() => { paginationData.currentPage = 1; fetchData() }">
      <el-tab-pane label="6/12/24月随访监测" name="followup" />
      <el-tab-pane label="未发现异常（3月复查）" name="normal" />
    </el-tabs>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
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

    <!-- 6/12/24月随访监测（含「未做」及潜伏感染者未完成治疗转入） -->
    <el-card v-if="activeMonitoringTab === 'followup'" shadow="never">
      <template #header>
        <span class="text-lg font-bold">密接人群 — 6/12/24月随访监测</span>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="idNumber" label="身份证号" />
        <el-table-column prop="registrationDate" label="登记日期" />
        <el-table-column prop="sourcePatientName" label="原患者" />
        <el-table-column prop="finalScreeningResult" label="筛查分类" min-width="110" show-overflow-tooltip />
        <el-table-column label="6月随访">
          <template #default="{ row }">
            <div class="text-xs text-gray-400">
              到期：{{ row.followup6DueDate || '—' }}
            </div>
            <el-tag v-if="row.followup6Result" :type="tagType(getFollowupTag(row.followup6Result))" size="small">
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
            <el-tag v-if="row.followup12Result" :type="tagType(getFollowupTag(row.followup12Result))" size="small">
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
            <el-tag v-if="row.followup24Result" :type="tagType(getFollowupTag(row.followup24Result))" size="small">
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
        <el-table-column label="操作" fixed="right">
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
          v-model:current-page="paginationData.currentPage" v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange" @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 未发现异常（3月复查） -->
    <el-card v-else-if="activeMonitoringTab === 'normal'" shadow="never">
      <template #header>
        <span class="text-lg font-bold">密接人群 — 未发现异常（3月后复查）</span>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="idNumber" label="身份证号" />
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
            <el-tag v-if="CC_STATUS_MAP[row.ccStatus]" :type="tagType(CC_STATUS_MAP[row.ccStatus].type)" size="small" class="ml-1">
              {{ CC_STATUS_MAP[row.ccStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
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
          v-model:current-page="paginationData.currentPage" v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange" @size-change="handleSizeChange"
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
