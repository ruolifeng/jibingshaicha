<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from "vue"
import { useRouter } from "vue-router"
import { ElMessage, ElMessageBox } from "element-plus"
import { REFERRAL_CROWD_CATEGORY_OPTIONS } from "@@/constants/disease"
import { idCardRule, phoneRule } from "@@/utils/validate"
import { formatDateTime } from "@@/utils/datetime"
import {
  parseTrackingHistory,
  TRACK_STATUS_LABEL,
  TRACKING_STATUS_MAP,
  getRecommendTime
} from "@@/utils/referralTracking"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { useUserStore } from "@/pinia/stores/user"
import {
  getReferralTrackingListApi,
  createReferralTrackingApi,
  sendRecommendApi,
  confirmRecommendApi,
  rejectRecommendApi,
  trackReferralApi,
  saveScreeningInfoApi,
  saveDiagnosisApi,
  deleteReferralTrackingApi,
  getLevel34UsersApi
} from "../apis/index"

const userStore = useUserStore()
const router = useRouter()

/** 五级用户发起推介；三/四级用户接收并确认 */
const isLevel5User = computed(() => userStore.userRole === 6)
const isLevel34User = computed(() => userStore.userRole === 4 || userStore.userRole === 5)

// ===== 列表 =====
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  township: "",
  dateRange: [] as string[]
})
const paginationData = reactive({ currentPage: 1, pageSize: 20 })

async function fetchList() {
  loading.value = true
  try {
    const res = await getReferralTrackingListApi({
      bizMode: "recommend",
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      township: searchForm.township || undefined,
      ...extractDateRangeParams(searchForm.dateRange)
    })
    tableData.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

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

// ===== 新增推介 =====
const createDialogVisible = ref(false)
const level34Users = ref<any[]>([])
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
  recommendReason: "",
  receiverUserId: undefined as number | undefined
})
const createFormRef = ref()
const sendingRecommend = ref(false)

const createFormRules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(true)],
  currentAddress: [{ required: true, message: "请填写现住址", trigger: "blur" }],
  crowdCategory: [{ required: true, message: "请选择人群分类", trigger: "change" }],
  recommendReason: [{ required: true, message: "请填写推介原因", trigger: "blur" }],
  receiverUserId: [{ required: true, message: "请选择推介接收人", trigger: "change" }]
}

async function openCreateDialog() {
  if (level34Users.value.length === 0) {
    const res = await getLevel34UsersApi()
    level34Users.value = res.data ?? []
  }
  Object.assign(createForm, {
    name: "", gender: "", birthDate: "", age: undefined, idType: "居民身份证",
    idNumber: "", ethnicity: "", phone: "",
    householdAddress: "", currentAddress: "", crowdCategory: "",
    recommendReason: "", receiverUserId: undefined
  })
  createDialogVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

async function handleSendRecommend() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  sendingRecommend.value = true
  try {
    await createReferralTrackingApi({ ...createForm, bizMode: "recommend" })
    ElMessage.success("推介通知单已发送")
    createDialogVisible.value = false
    fetchList()
  } finally {
    sendingRecommend.value = false
  }
}

function formatLevel34UserLabel(u: any) {
  const unit = u.orgName?.trim() || "未填写单位"
  return `${u.username}（${unit}）`
}

// ===== 发送推介通知 =====
async function handleSend(row: any) {
  await ElMessageBox.confirm(`确认向接收人发送「${row.name}」的推介通知单？`, "发送确认", { type: "warning" })
  await sendRecommendApi(row.id)
  ElMessage.success("推介通知单已发送")
  fetchList()
}

// ===== 确认/拒绝推介 =====
async function handleConfirm(row: any) {
  await ElMessageBox.confirm(`确认接受「${row.name}」的推介通知单？确认后将进入追踪流程。`, "确认接收", { type: "info" })
  await confirmRecommendApi(row.id)
  ElMessage.success("已确认接受，请前往「追踪」页面开展追踪")
  fetchList()
  router.push("/referral-management/track")
}

const rejectDialogVisible = ref(false)
const rejectRow = ref<any>(null)
const rejectReason = ref("")

function openRejectDialog(row: any) {
  rejectRow.value = row
  rejectReason.value = ""
  rejectDialogVisible.value = true
}

async function handleReject() {
  await rejectRecommendApi(rejectRow.value.id, rejectReason.value)
  ElMessage.success("已拒绝推介通知单")
  rejectDialogVisible.value = false
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

async function handleTrack() {
  if (!trackForm.status) {
    ElMessage.warning("请选择追踪状态")
    return
  }
  if (trackForm.status === 2 && !trackForm.remark.trim()) {
    ElMessage.warning("未到位时必须填写原因")
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

// ===== 查看追踪记录（只读） =====
const historyViewVisible = ref(false)
const historyViewRow = ref<any>(null)
const historyViewList = computed(() =>
  parseTrackingHistory(historyViewRow.value?.trackingHistoryJson)
)

function openHistoryView(row: any) {
  historyViewRow.value = row
  historyViewVisible.value = true
}

function hasTrackingHistory(row: any) {
  return parseTrackingHistory(row?.trackingHistoryJson).length > 0
}

function formatRecommendTime(row: any) {
  const time = getRecommendTime(row)
  return time ? formatDateTime(time) : "-"
}

/** 当前用户是否为推介接收人 */
function isReceiver(row: any) {
  return Number(row.receiverUserId) === Number(userStore.userId)
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

function openDiagnosisDialog(row: any) {
  diagnosisRow.value = row
  diagnosisResult.value = ""
  diagnosisDialogVisible.value = true
}

async function handleSaveDiagnosis() {
  if (!diagnosisResult.value) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  await saveDiagnosisApi(diagnosisRow.value.id, diagnosisResult.value)
  ElMessage.success("诊断结果已保存")
  diagnosisDialogVisible.value = false
  fetchList()
}

// ===== 删除 =====
async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.name}」的推介记录？`, "删除确认", { type: "warning" })
  await deleteReferralTrackingApi(row.id)
  ElMessage.success("删除成功")
  fetchList()
}

// ===== 状态标签辅助 =====
const RECOMMEND_STATUS_MAP: Record<number, { label: string; type: string }> = {
  0: { label: "未发送", type: "info" },
  1: { label: "已发送", type: "warning" },
  2: { label: "已接受", type: "success" },
  3: { label: "已拒绝", type: "danger" }
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
      <el-alert
        v-if="isLevel34User"
        type="info"
        :closable="false"
        class="mb-3"
        title="待接收的推介通知单会显示在下方，也可在「系统消息」中确认。确认后将自动进入「追踪」流程。"
      />
      <div v-if="isLevel5User" class="toolbar-wrapper" style="margin-bottom: 12px">
        <el-button type="primary" @click="openCreateDialog">新增推介</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="crowdCategory" label="人群分类" />
        <el-table-column prop="recommendReason" label="推介原因" show-overflow-tooltip />
        <el-table-column prop="receiverUserName" label="推介接收人" />
        <el-table-column label="推介状态">
          <template #default="{ row }">
            <el-tag
              v-if="row.recommendStatus !== null && row.recommendStatus !== undefined"
              :type="RECOMMEND_STATUS_MAP[row.recommendStatus]?.type as any"
              size="small"
            >
              {{ RECOMMEND_STATUS_MAP[row.recommendStatus]?.label }}
            </el-tag>
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
        <el-table-column prop="diagnosisResult" label="诊断结果" />
        <el-table-column label="推介时间" min-width="160">
          <template #default="{ row }">
            {{ formatRecommendTime(row) }}
          </template>
        </el-table-column>
        <el-table-column label="到位时间" min-width="160">
          <template #default="{ row }">
            {{ row.arrivalTime ? formatDateTime(row.arrivalTime) : "-" }}
          </template>
        </el-table-column>
        <el-table-column label="追踪次数" width="100">
          <template #default="{ row }">
            {{ row.notInPlaceCount > 0 ? `${row.notInPlaceCount}次未到位` : "-" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <!-- 五级：补发未发送的推介 -->
            <el-button
              v-if="isLevel5User && row.recommendStatus === 0 && !row.archived"
              type="primary" link size="small"
              @click="handleSend(row)"
            >发送推介</el-button>
            <!-- 三/四级接收人：确认/拒绝待接收推介 -->
            <el-button
              v-if="row.recommendStatus === 1 && isReceiver(row)"
              type="success" link size="small"
              @click="handleConfirm(row)"
            >确认接受</el-button>
            <el-button
              v-if="row.recommendStatus === 1 && isReceiver(row)"
              type="danger" link size="small"
              @click="openRejectDialog(row)"
            >拒绝</el-button>
            <!-- 删除：五级可删自己发起的；三/四级可删待接收的 -->
            <el-button
              v-if="isLevel5User || (isReceiver(row) && row.recommendStatus === 1)"
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

    <!-- 新增推介弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增推介记录" width="660px">
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
          <el-col :span="12">
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
          <el-col :span="12">
            <el-form-item label="推介接收人" prop="receiverUserId">
              <el-select
                v-model="createForm.receiverUserId"
                filterable
                placeholder="选择一个三级或四级用户"
                style="width: 100%"
              >
                <el-option
                  v-for="u in level34Users"
                  :key="u.id"
                  :label="formatLevel34UserLabel(u)"
                  :value="u.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="推介原因" prop="recommendReason">
              <el-input
                v-model="createForm.recommendReason"
                type="textarea"
                :rows="3"
                placeholder="请填写推介原因"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendingRecommend" @click="handleSendRecommend">发送推介</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝推介弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝推介" width="420px">
      <el-form label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 查看追踪记录弹窗（只读） -->
    <el-dialog v-model="historyViewVisible" title="追踪过程" width="520px">
      <div v-if="historyViewList.length === 0" class="tracking-history-empty">暂无追踪记录</div>
      <div v-else class="tracking-history">
        <div v-for="item in historyViewList" :key="item.attempt" class="tracking-history-item">
          <span class="tracking-history-attempt">第{{ item.attempt }}次</span>
          <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
            {{ TRACK_STATUS_LABEL[item.status] }}
          </el-tag>
          <span class="tracking-history-time">{{ formatDateTime(item.trackTime) }}</span>
          <span v-if="item.reason" class="tracking-history-reason">原因：{{ item.reason }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="historyViewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 追踪操作弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="520px">
      <el-form label-width="100px">
        <!-- 已有追踪记录 -->
        <el-form-item v-if="trackHistory.length > 0" label="追踪记录">
          <div class="tracking-history">
            <div v-for="item in trackHistory" :key="item.attempt" class="tracking-history-item">
              <span class="tracking-history-attempt">第{{ item.attempt }}次</span>
              <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
                {{ TRACK_STATUS_LABEL[item.status] }}
              </el-tag>
              <span class="tracking-history-time">{{ formatDateTime(item.trackTime) }}</span>
              <span v-if="item.reason" class="tracking-history-reason">原因：{{ item.reason }}</span>
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
        <el-form-item v-if="trackForm.status === 2" label="未到位原因" required>
          <el-input
            v-model="trackForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请填写未到位原因"
          />
        </el-form-item>
        <el-form-item v-else-if="trackForm.status === 3" label="备注">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" placeholder="请填写备注" />
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
              <el-input v-model="screeningForm.screenMethod" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查结果">
              <el-input v-model="screeningForm.screenResult" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查结果">
              <el-select v-model="screeningForm.infectionResult" style="width: 100%">
                <el-option label="阴性" value="阴性" />
                <el-option label="PPD+" value="PPD+" />
                <el-option label="PPD++" value="PPD++" />
                <el-option label="PPD+++" value="PPD+++" />
                <el-option label="EC阳性" value="EC阳性" />
                <el-option label="IGRA阳性" value="IGRA阳性" />
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
              <el-select v-model="screeningForm.chestXrayResult" style="width: 100%">
                <el-option label="正常" value="正常" />
                <el-option label="异常" value="异常" />
                <el-option label="未查" value="未查" />
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
        <el-alert
          v-if="diagnosisResult === '确诊患者'"
          title="确诊患者将自动进入【患者管理】模块（populationType=referral）"
          type="info" :closable="false" style="margin-top: 8px"
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
  padding: 24px 0;
  text-align: center;
  color: var(--el-text-color-secondary);
}
</style>
