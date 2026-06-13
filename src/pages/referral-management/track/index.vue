<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { REFERRAL_CROWD_CATEGORY_OPTIONS, isConfirmedPatientDiagnosis } from "@@/constants/disease"
import {
  REFERRAL_CHEST_XRAY_RESULT_OPTIONS,
  REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS,
  REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS,
  referralSelectOptionsWithLegacy
} from "@@/constants/referral-tracking"
import { idCardRule, phoneRule } from "@@/utils/validate"
import { formatDateTime } from "@@/utils/datetime"
import { downloadBlob } from "@@/utils/download"
import {
  parseTrackingHistory,
  TRACK_STATUS_LABEL,
  TRACKING_STATUS_MAP,
  getRecommendTime
} from "@@/utils/referralTracking"
import { EPIDEMIC_TRACK_IMPORT_FIELDS } from "@@/constants/epidemic-track-import"
import { useUserStore } from "@/pinia/stores/user"
import {
  getReferralTrackingListApi,
  getReferralTrackingDetailApi,
  createReferralTrackingApi,
  updateReferralTrackingApi,
  trackReferralApi,
  enableJointTrackingApi,
  saveScreeningInfoApi,
  saveDiagnosisApi,
  deleteReferralTrackingApi,
  importEpidemicTrackApi,
  exportReferralTrackApi
} from "../apis/index"

const userStore = useUserStore()

/** 推介已确认且开启共同追踪时，发起方与接收方均可操作 */
function isJointTrackingEnabled(row: any) {
  return Number(row?.jointTracking) === 1
}

/** 有接收人时仅接收人可操作；共同追踪时发起方与接收方均可；无接收人时创建人或辖区一至五级用户可操作 */
function canOperateTrack(row: any) {
  if (userStore.userRole === 1) return true
  const uid = Number(userStore.userId)
  if (isFromRecommend(row) && row.receiverUserId) {
    if (isJointTrackingEnabled(row)) {
      return uid === Number(row.receiverUserId) || uid === Number(row.creatorId)
    }
    return uid === Number(row.receiverUserId)
  }
  if (row.receiverUserId) {
    return uid === Number(row.receiverUserId)
  }
  if (uid === Number(row.creatorId)) return true
  // 追踪/大疫情：辖区一至五级用户对可见记录均可操作
  return userStore.userRole >= 2 && userStore.userRole <= 6
}

/** 接收方在推介确认后可开启共同追踪 */
function canEnableJointTracking(row: any) {
  if (row.archived || row.recommendStatus !== 2 || isJointTrackingEnabled(row)) return false
  return Number(row.receiverUserId) === Number(userStore.userId) || userStore.userRole === 1
}

// ===== 列表 =====
const loading = ref(false)
const exporting = ref(false)
const uploading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  township: "",
  dateRange: [] as string[]
})

async function fetchList() {
  loading.value = true
  try {
    const [dateFrom, dateTo] = searchForm.dateRange ?? []
    const res = await getReferralTrackingListApi({
      bizMode: "track",
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      township: searchForm.township || undefined,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined
    })
    tableData.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

const paginationData = reactive({ currentPage: 1, pageSize: 20 })

onMounted(fetchList)

function handleSearch() {
  paginationData.currentPage = 1
  fetchList()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.phone = ""
  searchForm.township = ""
  searchForm.dateRange = []
  handleSearch()
}

// ===== 大疫情导入 =====
const importDialogVisible = ref(false)
const importResult = ref<{ count: number, updated?: number, batchNo: string } | null>(null)

function openImportDialog() {
  importResult.value = null
  importDialogVisible.value = true
}

async function handleEpidemicFileChange(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) return
  if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
    ElMessage.error("请上传 .xlsx 或 .xls 格式的大疫情表文件")
    return
  }
  uploading.value = true
  importResult.value = null
  try {
    const res = await importEpidemicTrackApi(file)
    importResult.value = res.data
    ElMessage.success(`导入成功，新建 ${res.data.count} 条，更新 ${res.data.updated ?? 0} 条追踪记录`)
    importDialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error("导入失败，请确认文件格式是否符合大疫情表模板")
  } finally {
    uploading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const [dateFrom, dateTo] = searchForm.dateRange ?? []
    const blob = await exportReferralTrackApi({
      bizMode: "track",
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      township: searchForm.township || undefined,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined
    })
    downloadBlob(blob as unknown as Blob, "追踪记录导出.xlsx")
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}

// ===== 编辑 =====
const editDialogVisible = ref(false)
const editRow = ref<any>(null)
const editForm = reactive({
  name: "",
  gender: "",
  birthDate: "",
  age: undefined as number | undefined,
  idNumber: "",
  phone: "",
  currentAddress: "",
  crowdCategory: "",
  trackReason: "",
  cardId: "",
  parentName: "",
  workplace: "",
  township: "",
  caseCategory: "",
  diseaseName: "",
  reportUnit: "",
  reportCardTime: "",
  epidemicRemark: ""
})

function openEditDialog(row: any) {
  editRow.value = row
  Object.assign(editForm, {
    name: row.name ?? "",
    gender: row.gender ?? "",
    birthDate: row.birthDate ?? "",
    age: row.age,
    idNumber: row.idNumber ?? "",
    phone: row.phone ?? "",
    currentAddress: row.currentAddress ?? "",
    crowdCategory: row.crowdCategory ?? "",
    trackReason: row.trackReason ?? "",
    cardId: row.cardId ?? "",
    parentName: row.parentName ?? "",
    workplace: row.workplace ?? "",
    township: row.township ?? "",
    caseCategory: row.caseCategory ?? "",
    diseaseName: row.diseaseName ?? "",
    reportUnit: row.reportUnit ?? "",
    reportCardTime: row.reportCardTime ?? "",
    epidemicRemark: row.epidemicRemark ?? ""
  })
  editDialogVisible.value = true
}

async function handleEditSave() {
  await updateReferralTrackingApi(editRow.value.id, { ...editForm })
  ElMessage.success("保存成功")
  editDialogVisible.value = false
  fetchList()
}

function isEpidemicRow(row: any) {
  return row?.sourceType === "epidemic"
}

/** 由推介确认后转入的追踪记录 */
function isFromRecommend(row: any) {
  return Boolean(row?.recommendConfirmTime || row?.recommendSentTime || row?.recommendStatus === 2)
}

const RECOMMEND_STATUS_MAP: Record<number, { label: string; type: string }> = {
  0: { label: "未发送", type: "info" },
  1: { label: "已发送", type: "warning" },
  2: { label: "已接受", type: "success" },
  3: { label: "已拒绝", type: "danger" }
}

// ===== 查看详情 =====
const viewDialogVisible = ref(false)
const viewLoading = ref(false)
const viewDetail = ref<any>(null)
const viewTrackingHistory = computed(() =>
  parseTrackingHistory(viewDetail.value?.trackingHistoryJson)
)

async function openViewDialog(row: any) {
  viewDialogVisible.value = true
  viewLoading.value = true
  viewDetail.value = null
  try {
    const res = await getReferralTrackingDetailApi(row.id)
    viewDetail.value = res.data
  } catch {
    ElMessage.error("加载详情失败")
    viewDialogVisible.value = false
  } finally {
    viewLoading.value = false
  }
}

// ===== 新增追踪 =====
const createDialogVisible = ref(false)
const createForm = reactive({
  name: "",
  gender: "",
  birthDate: "",
  age: undefined as number | undefined,
  idType: "居民身份证",
  idNumber: "",
  ethnicity: "",
  phone: "",
  householdAddress: "",
  currentAddress: "",
  crowdCategory: "",
  trackReason: ""
})
const createFormRef = ref()

const createFormRules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(true)],
  currentAddress: [{ required: true, message: "请填写现住址", trigger: "blur" }],
  crowdCategory: [{ required: true, message: "请选择人群分类", trigger: "change" }],
  trackReason: [{ required: true, message: "请填写追踪原因", trigger: "blur" }]
}

function openCreateDialog() {
  Object.assign(createForm, {
    name: "", gender: "", birthDate: "", age: undefined, idType: "居民身份证",
    idNumber: "", ethnicity: "", phone: "",
    householdAddress: "", currentAddress: "", crowdCategory: "",
    trackReason: ""
  })
  createDialogVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

async function handleCreate() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  await createReferralTrackingApi({ ...createForm, bizMode: "track" })
  ElMessage.success("追踪记录创建成功")
  createDialogVisible.value = false
  fetchList()
}

// ===== 追踪操作 =====
const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const trackForm = reactive({ status: undefined as number | undefined, remark: "" })

const trackHistory = computed(() =>
  parseTrackingHistory(trackRow.value?.trackingHistoryJson)
)

const nextAttemptNo = computed(() => trackHistory.value.length + 1)

function openTrackDialog(row: any) {
  trackRow.value = row
  Object.assign(trackForm, { status: undefined, remark: "" })
  trackDialogVisible.value = true
}

async function handleEnableJointTracking(row: any) {
  await ElMessageBox.confirm(
    `确认对「${row.name}」开启共同追踪吗？开启后您与推介发起方均可进行追踪，双方操作次数合并计算。`,
    "共同追踪确认",
    { type: "warning", confirmButtonText: "确认开启", cancelButtonText: "取消" }
  )
  await enableJointTrackingApi(row.id)
  ElMessage.success("已开启共同追踪，推介发起方也可参与追踪")
  fetchList()
}

async function handleTrack() {
  if (!trackForm.status) {
    ElMessage.warning("请选择追踪状态")
    return
  }
  if (!trackForm.remark.trim()) {
    ElMessage.warning("请填写追踪备注")
    return
  }
  const willForceEnd = trackForm.status === 2 && (trackRow.value?.notInPlaceCount ?? 0) >= 2
  await trackReferralApi(trackRow.value.id, trackForm.status, trackForm.remark)
  if (willForceEnd) {
    ElMessage.warning("已记录第 3 次未到位，追踪已强制结束")
  } else if (trackForm.status === 1) {
    ElMessage.success("已确认到位")
  } else {
    ElMessage.success("追踪记录已保存")
  }
  trackDialogVisible.value = false
  fetchList()
}

// ===== 筛查信息 =====
const screeningDialogVisible = ref(false)
const screeningRow = ref<any>(null)
const screeningForm = reactive({
  hasInfectionScreen: "",
  screenDate: "",
  screenMethod: "",
  screenResult: "",
  infectionResult: "",
  hasChestXray: "",
  chestXrayDate: "",
  chestXrayResult: ""
})

const chestXrayResultSelectOptions = computed(() =>
  referralSelectOptionsWithLegacy(REFERRAL_CHEST_XRAY_RESULT_OPTIONS, screeningForm.chestXrayResult))

function openScreeningDialog(row: any) {
  screeningRow.value = row
  Object.assign(screeningForm, {
    hasInfectionScreen: row.hasInfectionScreen ?? "",
    screenDate: row.screenDate ?? "",
    screenMethod: row.screenMethod ?? "",
    screenResult: row.screenResult ?? "",
    infectionResult: row.infectionResult ?? "",
    hasChestXray: row.hasChestXray ?? "",
    chestXrayDate: row.chestXrayDate ?? "",
    chestXrayResult: row.chestXrayResult ?? ""
  })
  screeningDialogVisible.value = true
}

async function handleSaveScreening() {
  await saveScreeningInfoApi(screeningRow.value.id, { ...screeningForm })
  ElMessage.success("筛查信息已保存")
  screeningDialogVisible.value = false
  fetchList()
}

// ===== 诊断 =====
const diagnosisDialogVisible = ref(false)
const diagnosisRow = ref<any>(null)
const diagnosisResult = ref("")
const diagnosisRemark = ref("")

function openDiagnosisDialog(row: any) {
  diagnosisRow.value = row
  diagnosisResult.value = ""
  diagnosisRemark.value = ""
  diagnosisDialogVisible.value = true
}

async function handleSaveDiagnosis() {
  if (!diagnosisResult.value) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  if (diagnosisResult.value === "其他" && !diagnosisRemark.value.trim()) {
    ElMessage.warning("选择其他时请填写备注")
    return
  }
  await saveDiagnosisApi(diagnosisRow.value.id, diagnosisResult.value, diagnosisRemark.value.trim() || undefined)
  ElMessage.success(
    diagnosisResult.value === "确诊患者" ? "诊断结果已保存，该记录已标红结案" : "诊断结果已保存"
  )
  diagnosisDialogVisible.value = false
  fetchList()
}

// ===== 删除 =====
async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.name}」的追踪记录？`, "删除确认", { type: "warning" })
  await deleteReferralTrackingApi(row.id)
  ElMessage.success("删除成功")
  fetchList()
}

// ===== 状态标签辅助 =====
function getRowClass({ row }: { row: any }) {
  if (row.archived && isConfirmedPatientDiagnosis(row)) return "confirmed-row"
  return ""
}
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card class="search-wrapper" shadow="never">
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
        <el-form-item label="乡镇">
          <el-input v-model="searchForm.township" placeholder="请输入乡镇" clearable />
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
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="toolbar-wrapper" style="margin-bottom: 12px; display: flex; gap: 8px">
        <el-button v-permission="'referralManagement:create'" type="primary" @click="openCreateDialog">新增追踪</el-button>
        <el-button v-permission="'referralManagement:epidemicImport'" type="success" @click="openImportDialog">大疫情导入</el-button>
        <el-button v-permission="'referralManagement:export'" type="warning" :loading="exporting" @click="handleExport">导出</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe :row-class-name="getRowClass">
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="isEpidemicRow(row) ? 'danger' : 'info'" size="small">
              {{ isEpidemicRow(row) ? "大疫情" : "手动" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cardId" label="卡片ID" width="120" show-overflow-tooltip />
        <el-table-column prop="name" label="患者姓名" />
        <el-table-column prop="parentName" label="患儿家长姓名" show-overflow-tooltip />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column label="出生日期" width="110">
          <template #default="{ row }">
            {{ row.birthDate || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="idNumber" label="有效证件号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="workplace" label="工作单位" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="township" label="乡镇" width="100" show-overflow-tooltip />
        <el-table-column prop="currentAddress" label="现住详细地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="crowdCategory" label="人群分类" show-overflow-tooltip />
        <el-table-column prop="caseCategory" label="病例分类" show-overflow-tooltip />
        <el-table-column prop="diseaseName" label="疾病名称" show-overflow-tooltip />
        <el-table-column prop="reportUnit" label="报告单位" show-overflow-tooltip />
        <el-table-column label="报告卡录入时间" min-width="160">
          <template #default="{ row }">
            {{ row.reportCardTime ? formatDateTime(row.reportCardTime) : "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="epidemicRemark" label="备注" show-overflow-tooltip />
        <el-table-column prop="trackReason" label="追踪原因" show-overflow-tooltip />
        <el-table-column label="共同追踪" width="90">
          <template #default="{ row }">
            <el-tag v-if="isFromRecommend(row)" :type="row.jointTracking === 1 ? 'success' : 'info'" size="small">
              {{ row.jointTracking === 1 ? "已开启" : "未开启" }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="追踪状态">
          <template #default="{ row }">
            <el-tag
              :type="TRACKING_STATUS_MAP[row.trackingStatus]?.type as any"
              size="small"
            >
              {{ TRACKING_STATUS_MAP[row.trackingStatus]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="追踪次数">
          <template #default="{ row }">
            {{ row.notInPlaceCount > 0 ? `${row.notInPlaceCount}次未到位` : "-" }}
          </template>
        </el-table-column>
        <el-table-column label="诊断结果" width="120">
          <template #default="{ row }">
            <el-tag
              v-if="row.diagnosisResult"
              :type="row.archived && isConfirmedPatientDiagnosis(row) ? 'danger' : 'info'"
              size="small"
            >
              {{ row.diagnosisResult }}{{ row.archived && isConfirmedPatientDiagnosis(row) ? "（结案）" : "" }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="到位时间" min-width="160">
          <template #default="{ row }">
            {{ row.arrivalTime ? formatDateTime(row.arrivalTime) : "-" }}
          </template>
        </el-table-column>
        <el-table-column label="追踪过程" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="parseTrackingHistory(row.trackingHistoryJson).length">
              <span
                v-for="(item, idx) in parseTrackingHistory(row.trackingHistoryJson)"
                :key="item.attempt"
              >
                <template v-if="idx">；</template>
                第{{ item.attempt }}次 {{ formatDateTime(item.trackTime) }}
                {{ TRACK_STATUS_LABEL[item.status] }}
                <template v-if="item.reason">（{{ item.reason }}）</template>
              </span>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openViewDialog(row)">查看</el-button>
            <el-button
              v-if="canOperateTrack(row)"
              v-permission="'referralManagement:edit'"
              type="primary" link size="small"
              @click="openEditDialog(row)"
            >编辑</el-button>
            <!-- 接收方开启共同追踪 -->
            <el-button
              v-if="canEnableJointTracking(row) && isFromRecommend(row)"
              v-permission="'referralManagement:confirm'"
              type="success" link size="small"
              @click="handleEnableJointTracking(row)"
            >共同追踪</el-button>
            <!-- 追踪：待追踪或未到位 -->
            <el-button
              v-if="canOperateTrack(row) && [0, 2].includes(row.trackingStatus) && !row.archived"
              v-permission="'referralManagement:trackOperate'"
              type="warning" link size="small"
              @click="openTrackDialog(row)"
            >追踪</el-button>
            <!-- 筛查信息：已到位 -->
            <el-button
              v-if="canOperateTrack(row) && row.trackingStatus === 1 && !row.diagnosisResult"
              v-permission="'referralManagement:xray'"
              type="primary" link size="small"
              @click="openScreeningDialog(row)"
            >录入胸片</el-button>
            <!-- 诊断：已到位 -->
            <el-button
              v-if="canOperateTrack(row) && row.trackingStatus === 1 && !row.diagnosisResult"
              v-permission="'referralManagement:diagnosis'"
              type="success" link size="small"
              @click="openDiagnosisDialog(row)"
            >录入诊断</el-button>
            <!-- 删除 -->
            <el-button
              v-if="canOperateTrack(row)"
              v-permission="'referralManagement:delete'"
              type="danger" link size="small"
              @click="handleDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 14px; justify-content: flex-end"
        layout="total, sizes, prev, pager, next"
        :total="total || 0"
        :page-size="paginationData.pageSize || 20"
        :current-page="paginationData.currentPage || 1"
        @size-change="(val: number) => { paginationData.pageSize = val; fetchList() }"
        @current-change="(val: number) => { paginationData.currentPage = val; fetchList() }"
      />
    </el-card>

    <!-- 查看追踪详情 -->
    <el-dialog v-model="viewDialogVisible" title="追踪详情" width="800px">
      <div v-loading="viewLoading">
        <template v-if="viewDetail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="数据来源">
              <el-tag :type="isEpidemicRow(viewDetail) ? 'danger' : 'info'" size="small">
                {{ isEpidemicRow(viewDetail) ? "大疫情导入" : "手动录入" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="录入者">{{ viewDetail.creatorUserName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="录入单位" :span="2">{{ viewDetail.entryUnitName || "-" }}</el-descriptions-item>
            <template v-if="isFromRecommend(viewDetail)">
              <el-descriptions-item label="推介来源" :span="2">
                <el-tag type="success" size="small">推介确认转入</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="推介接收人">{{ viewDetail.receiverUserName || "-" }}</el-descriptions-item>
              <el-descriptions-item label="推介状态">
                <el-tag
                  v-if="viewDetail.recommendStatus !== null && viewDetail.recommendStatus !== undefined"
                  :type="RECOMMEND_STATUS_MAP[viewDetail.recommendStatus]?.type as any"
                  size="small"
                >
                  {{ RECOMMEND_STATUS_MAP[viewDetail.recommendStatus]?.label }}
                </el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="推介时间">
                {{ getRecommendTime(viewDetail) ? formatDateTime(getRecommendTime(viewDetail)!) : "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="推介确认时间">
                {{ viewDetail.recommendConfirmTime ? formatDateTime(viewDetail.recommendConfirmTime) : "-" }}
              </el-descriptions-item>
              <el-descriptions-item v-if="viewDetail.recommendReason" label="原推介原因" :span="2">
                {{ viewDetail.recommendReason }}
              </el-descriptions-item>
              <el-descriptions-item label="共同追踪">
                <el-tag :type="viewDetail.jointTracking === 1 ? 'success' : 'info'" size="small">
                  {{ viewDetail.jointTracking === 1 ? "已开启" : "未开启" }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="viewDetail.jointTrackingTime" label="共同追踪时间">
                {{ formatDateTime(viewDetail.jointTrackingTime) }}
              </el-descriptions-item>
            </template>
            <template v-if="isEpidemicRow(viewDetail)">
              <el-descriptions-item label="卡片ID">{{ viewDetail.cardId || "-" }}</el-descriptions-item>
              <el-descriptions-item label="患儿家长姓名">{{ viewDetail.parentName || "-" }}</el-descriptions-item>
              <el-descriptions-item label="工作单位">{{ viewDetail.workplace || "-" }}</el-descriptions-item>
              <el-descriptions-item label="乡镇">{{ viewDetail.township || "-" }}</el-descriptions-item>
              <el-descriptions-item label="病例分类">{{ viewDetail.caseCategory || "-" }}</el-descriptions-item>
              <el-descriptions-item label="疾病名称">{{ viewDetail.diseaseName || "-" }}</el-descriptions-item>
              <el-descriptions-item label="报告单位">{{ viewDetail.reportUnit || "-" }}</el-descriptions-item>
              <el-descriptions-item label="报告卡录入时间">
                {{ viewDetail.reportCardTime ? formatDateTime(viewDetail.reportCardTime) : "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ viewDetail.epidemicRemark || "-" }}</el-descriptions-item>
            </template>
            <el-descriptions-item label="患者姓名">{{ viewDetail.name || "-" }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ viewDetail.gender || "-" }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ viewDetail.birthDate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ viewDetail.age ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="证件类型">{{ viewDetail.idType || "-" }}</el-descriptions-item>
            <el-descriptions-item label="有效证件号">{{ viewDetail.idNumber || "-" }}</el-descriptions-item>
            <el-descriptions-item label="民族">{{ viewDetail.ethnicity || "-" }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ viewDetail.phone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="户籍地址" :span="2">{{ viewDetail.householdAddress || "-" }}</el-descriptions-item>
            <el-descriptions-item label="现住详细地址" :span="2">{{ viewDetail.currentAddress || "-" }}</el-descriptions-item>
            <el-descriptions-item label="人群分类">{{ viewDetail.crowdCategory || "-" }}</el-descriptions-item>
            <el-descriptions-item label="追踪原因">{{ viewDetail.trackReason || "-" }}</el-descriptions-item>
            <el-descriptions-item label="追踪状态">
              <el-tag :type="TRACKING_STATUS_MAP[viewDetail.trackingStatus]?.type as any" size="small">
                {{ TRACKING_STATUS_MAP[viewDetail.trackingStatus]?.label }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="未到位次数">
              {{ viewDetail.notInPlaceCount > 0 ? `${viewDetail.notInPlaceCount}次` : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ viewDetail.createTime ? formatDateTime(viewDetail.createTime) : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="到位时间">
              {{ viewDetail.arrivalTime ? formatDateTime(viewDetail.arrivalTime) : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="诊断结果">
              <el-tag
                v-if="viewDetail.diagnosisResult"
                :type="viewDetail.archived && isConfirmedPatientDiagnosis(viewDetail) ? 'danger' : 'info'"
                size="small"
              >
                {{ viewDetail.diagnosisResult }}{{ viewDetail.archived && isConfirmedPatientDiagnosis(viewDetail) ? "（结案）" : "" }}
              </el-tag>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="viewDetail.diagnosisRemark" label="诊断备注" :span="2">
              {{ viewDetail.diagnosisRemark }}
            </el-descriptions-item>
            <el-descriptions-item label="诊断时间">
              {{ viewDetail.diagnosisTime ? formatDateTime(viewDetail.diagnosisTime) : "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="是否感染筛查">{{ viewDetail.hasInfectionScreen || "-" }}</el-descriptions-item>
            <el-descriptions-item label="筛查日期">{{ viewDetail.screenDate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="筛查方法">{{ viewDetail.screenMethod || "-" }}</el-descriptions-item>
            <el-descriptions-item label="筛查结果">{{ viewDetail.screenResult || "-" }}</el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">{{ viewDetail.infectionResult || "-" }}</el-descriptions-item>
            <el-descriptions-item label="是否胸片检查">{{ viewDetail.hasChestXray || "-" }}</el-descriptions-item>
            <el-descriptions-item label="胸片检查日期">{{ viewDetail.chestXrayDate || "-" }}</el-descriptions-item>
            <el-descriptions-item label="胸片检查结果" :span="2">{{ viewDetail.chestXrayResult || "-" }}</el-descriptions-item>
          </el-descriptions>
          <div class="view-tracking-section">
            <div class="view-tracking-title">追踪过程</div>
            <div v-if="viewTrackingHistory.length === 0" class="tracking-history-empty">暂无追踪记录</div>
            <div v-else class="tracking-history">
              <div v-for="item in viewTrackingHistory" :key="item.attempt" class="tracking-history-item">
                <span class="tracking-history-attempt">第{{ item.attempt }}次</span>
                <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
                  {{ TRACK_STATUS_LABEL[item.status] }}
                </el-tag>
                <span class="tracking-history-time">{{ formatDateTime(item.trackTime) }}</span>
                <span v-if="item.reason" class="tracking-history-reason">备注：{{ item.reason }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 大疫情导入弹窗 -->
    <el-dialog v-model="importDialogVisible" title="大疫情导入" width="680px">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          上传大疫情网（报告卡）导出文件（.xlsx / .xls），系统自动提取以下字段并创建追踪记录。
        </template>
        <template #default>
          <div style="margin-top: 8px; font-size: 13px; line-height: 1.8; color: #606266">
            {{ EPIDEMIC_TRACK_IMPORT_FIELDS.join("、") }}
          </div>
        </template>
      </el-alert>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept=".xlsx,.xls"
        :on-change="handleEpidemicFileChange"
        drag
        style="width: 100%"
      >
        <div style="padding: 24px 0">
          <div style="font-size: 16px; color: #606266">
            拖拽大疫情表文件到此处，或 <span style="color: #409eff">点击上传</span>
          </div>
          <div style="font-size: 12px; color: #909399; margin-top: 8px">支持 .xlsx / .xls 格式</div>
        </div>
      </el-upload>
      <div v-if="uploading" style="text-align: center; margin-top: 16px; color: #606266">
        正在解析并导入，请稍候...
      </div>
      <el-result
        v-if="importResult"
        icon="success"
        :title="`成功导入 ${importResult.count} 条追踪记录`"
        :sub-title="`批次号：${importResult.batchNo}`"
      />
      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑追踪记录" width="720px">
      <el-form :model="editForm" label-width="120px">
        <el-row :gutter="16">
          <template v-if="isEpidemicRow(editRow)">
            <el-col :span="12"><el-form-item label="卡片ID"><el-input v-model="editForm.cardId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="患儿家长姓名"><el-input v-model="editForm.parentName" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="工作单位"><el-input v-model="editForm.workplace" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="乡镇"><el-input v-model="editForm.township" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="病例分类"><el-input v-model="editForm.caseCategory" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="疾病名称"><el-input v-model="editForm.diseaseName" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="报告单位"><el-input v-model="editForm.reportUnit" /></el-form-item></el-col>
            <el-col :span="12">
              <el-form-item label="报告卡录入时间">
                <el-date-picker v-model="editForm.reportCardTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="24"><el-form-item label="备注"><el-input v-model="editForm.epidemicRemark" type="textarea" :rows="2" /></el-form-item></el-col>
          </template>
          <el-col :span="12"><el-form-item label="患者姓名"><el-input v-model="editForm.name" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="editForm.gender" style="width: 100%">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="editForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="年龄"><el-input-number v-model="editForm.age" :min="0" :max="150" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="有效证件号"><el-input v-model="editForm.idNumber" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="editForm.phone" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="现住详细地址"><el-input v-model="editForm.currentAddress" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="人群分类"><el-input v-model="editForm.crowdCategory" /></el-form-item></el-col>
          <el-col v-if="!isEpidemicRow(editRow)" :span="24">
            <el-form-item label="追踪原因"><el-input v-model="editForm.trackReason" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增追踪弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增追踪记录" width="660px">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="createForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="createForm.gender" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="createForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="createForm.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件类型">
              <el-input v-model="createForm.idType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号" prop="idNumber">
              <el-input v-model="createForm.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="民族">
              <el-input v-model="createForm.ethnicity" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="createForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户籍地址">
              <el-input v-model="createForm.householdAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现住址" prop="currentAddress">
              <el-input v-model="createForm.currentAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="人群分类" prop="crowdCategory">
              <el-select v-model="createForm.crowdCategory" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in REFERRAL_CROWD_CATEGORY_OPTIONS"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="追踪原因" prop="trackReason">
              <el-input
                v-model="createForm.trackReason"
                type="textarea"
                :rows="3"
                placeholder="请填写追踪原因"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 追踪操作弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="520px">
      <el-form label-width="100px">
        <el-form-item v-if="trackHistory.length > 0" label="追踪记录">
          <div class="tracking-history">
            <div v-for="item in trackHistory" :key="item.attempt" class="tracking-history-item">
              <span class="tracking-history-attempt">第{{ item.attempt }}次</span>
              <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
                {{ TRACK_STATUS_LABEL[item.status] }}
              </el-tag>
              <span class="tracking-history-time">{{ formatDateTime(item.trackTime) }}</span>
              <span v-if="item.reason" class="tracking-history-reason">备注：{{ item.reason }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="追踪状态">
          <el-radio-group v-model="trackForm.status">
            <el-radio :value="1">到位</el-radio>
            <el-radio :value="2">未到位</el-radio>
            <el-radio :value="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" required>
          <el-input
            v-model="trackForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请填写本次追踪备注"
          />
        </el-form-item>
        <el-alert
          v-if="trackForm.status === 2 && trackRow"
          :title="`第 ${nextAttemptNo} 次追踪，当前已未到位 ${trackRow.notInPlaceCount ?? 0} 次，3 次未到位将自动结束追踪`"
          type="warning"
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTrack">确认</el-button>
      </template>
    </el-dialog>

    <!-- 录入筛查信息弹窗 -->
    <el-dialog v-model="screeningDialogVisible" title="录入筛查信息" width="600px">
      <el-form :model="screeningForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否感染筛查">
              <el-select v-model="screeningForm.hasInfectionScreen" style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查日期">
              <el-date-picker v-model="screeningForm.screenDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查方法">
              <el-select v-model="screeningForm.screenMethod" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查结果">
              <el-input v-model="screeningForm.screenResult" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查结果">
              <el-select v-model="screeningForm.infectionResult" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否胸片检查">
              <el-select v-model="screeningForm.hasChestXray" style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片检查日期">
              <el-date-picker v-model="screeningForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片检查结果">
              <el-select v-model="screeningForm.chestXrayResult" placeholder="请选择" clearable style="width: 100%">
                <el-option
                  v-for="opt in chestXrayResultSelectOptions"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="screeningDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveScreening">保存</el-button>
      </template>
    </el-dialog>

    <!-- 录入诊断弹窗 -->
    <el-dialog v-model="diagnosisDialogVisible" title="录入诊断结果" width="420px">
      <el-form label-width="100px">
        <el-form-item label="诊断结果">
          <el-radio-group v-model="diagnosisResult">
            <el-radio value="排除">排除</el-radio>
            <el-radio value="确诊患者">确诊患者</el-radio>
            <el-radio value="潜伏感染者">潜伏感染者</el-radio>
            <el-radio value="其他">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="diagnosisResult === '其他'" label="备注" required>
          <el-input
            v-model="diagnosisRemark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入其他诊断结果说明"
          />
        </el-form-item>
        <el-alert
          v-if="diagnosisResult === '确诊患者'"
          title="确诊患者将标红结案，不进入【患者管理】模块"
          type="warning" :closable="false" style="margin-top: 8px"
        />
        <el-alert
          v-if="diagnosisResult === '潜伏感染者'"
          title="潜伏感染者将自动进入【潜伏感染者管理】模块（populationType=referral）"
          type="info" :closable="false" style="margin-top: 8px"
        />
      </el-form>
      <template #footer>
        <el-button @click="diagnosisDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDiagnosis">确认诊断</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.tracking-history {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tracking-history-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
}

.tracking-history-attempt {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tracking-history-time {
  color: var(--el-text-color-secondary);
}

.tracking-history-reason {
  width: 100%;
  color: var(--el-text-color-regular);
}

.tracking-history-empty {
  padding: 16px 0;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.view-tracking-section {
  margin-top: 16px;
}

.view-tracking-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>

<style lang="scss">
.el-table .confirmed-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
