<script lang="ts" setup>
import type { ReferralDetailVO, SentNoticeVO, SentReferralVO } from "./apis"
import { getPopulationTypeLabel, NOTICE_STATUS_MAP } from "@@/constants/disease"
import { usePagination } from "@@/composables/usePagination"
import { getNoticeDetailApi } from "@/pages/school/latent/apis"
import {
  confirmRecommendApi,
  getReferralTrackingDetailApi,
  rejectRecommendApi
} from "@/pages/referral-management/apis"
import {
  confirmNoticeFromMessageApi,
  confirmReferralFromMessageApi,
  deleteMessageApi,
  getMessageListApi,
  getReferralDetailApi,
  getSentNoticeListApi,
  getSentReferralListApi,
  markMessageReadApi,
  rejectReferralFromMessageApi,
  remindNoticeApi
} from "./apis"
import { useRouter } from "vue-router"
import { useMessageStore } from "@/pinia/stores/message"

defineOptions({ name: "Message" })

const router = useRouter()
const messageStore = useMessageStore()

// ====== 收到的消息 ======
const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const isReadFilter = ref<number | undefined>(undefined)

const MESSAGE_TYPE_LABEL_MAP: Record<string, string> = {
  notice_receive: "待接收通知单",
  notice_confirmed: "通知单已接收",
  notice_timeout: "通知单超时",
  supervision_timeout: "督导表超时",
  visit_timeout: "随访超时",
  referral_receive: "待确认转出",
  referral_confirmed: "转出已接收",
  referral_rejected: "转出已被拒绝",
  referral_tracking_receive: "待确认推介",
  referral_tracking_confirmed: "推介已接收",
  referral_tracking_rejected: "推介已被拒绝"
}

function getMessageTypeTagType(type: string) {
  if (type === "notice_timeout") return "danger"
  if (type === "notice_receive") return "warning"
  if (type === "notice_confirmed") return "success"
  if (type === "referral_receive") return "warning"
  if (type === "referral_confirmed") return "success"
  if (type === "referral_rejected") return "danger"
  if (type === "referral_tracking_receive") return "warning"
  if (type === "referral_tracking_confirmed") return "success"
  if (type === "referral_tracking_rejected") return "danger"
  return "info"
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getMessageListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      isRead: isReadFilter.value
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(row: any) {
  try {
    await markMessageReadApi(row.id)
    row.isRead = 1
    ElMessage.success("已标记为已读")
    await messageStore.fetchUnreadCount()
  } catch { /* handled */ }
}

async function handleDeleteMessage(row: any) {
  try {
    await ElMessageBox.confirm("确认删除该消息？删除后不可恢复。", "删除消息", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning"
    })
    await deleteMessageApi(row.id)
    ElMessage.success("消息已删除")
    await fetchData()
  } catch { /* cancelled or handled */ }
}

async function handleReceiveNotice(row: any) {
  if (!row.bizId) {
    ElMessage.warning("通知单编号缺失，无法接收")
    return
  }
  try {
    await confirmNoticeFromMessageApi(row.bizId)
    await markMessageReadApi(row.id)
    row.isRead = 1
    row.type = "notice_confirmed"
    ElMessage.success("通知单接收成功")
    await messageStore.fetchUnreadCount()
  } catch { /* handled */ }
}

// ==================== 通知单详情查看 ====================
const noticeDetailVisible = ref(false)
const noticeDetailData = ref<any>(null)
const noticeDetailLoading = ref(false)

async function viewNoticeDetail(row: any) {
  if (!row.bizId) {
    ElMessage.warning("通知单编号缺失")
    return
  }
  noticeDetailLoading.value = true
  noticeDetailVisible.value = true
  noticeDetailData.value = null
  try {
    const { data } = await getNoticeDetailApi(row.bizId)
    noticeDetailData.value = data
  } catch { /* handled */ } finally {
    noticeDetailLoading.value = false
  }
}

// ====== 转出：消息中确认/拒绝 ======
const rejectDialogVisible = ref(false)
const rejectingRow = ref<any>(null)
const rejectReason = ref("")

function openRejectDialog(row: any) {
  rejectingRow.value = row
  rejectReason.value = ""
  rejectDialogVisible.value = true
}

async function handleConfirmReferral(row: any) {
  if (!row.bizId) {
    ElMessage.warning("转出记录编号缺失")
    return
  }
  try {
    await confirmReferralFromMessageApi(row.bizId)
    await markMessageReadApi(row.id)
    ElMessage.success("已确认接收转出信息")
    await fetchData()
    await messageStore.fetchUnreadCount()
  } catch { /* handled */ }
}

async function handleRejectReferral() {
  const row = rejectingRow.value
  if (!row?.bizId) return
  try {
    if (row.type === "referral_tracking_receive") {
      await rejectRecommendApi(row.bizId, rejectReason.value || undefined)
    } else {
      await rejectReferralFromMessageApi(row.bizId, rejectReason.value || undefined)
      row.type = "referral_rejected"
    }
    await markMessageReadApi(row.id)
    rejectDialogVisible.value = false
    ElMessage.success("已拒绝")
    await fetchData()
    await messageStore.fetchUnreadCount()
  } catch { /* handled */ }
}

async function handleConfirmReferralTracking(row: any) {
  if (!row.bizId) {
    ElMessage.warning("推介记录编号缺失")
    return
  }
  try {
    await confirmRecommendApi(row.bizId)
    ElMessage.success("已确认接收推介，请前往「追踪」页面开展追踪")
    await fetchData()
    await messageStore.fetchUnreadCount()
    router.push("/referral-management/track")
  } catch { /* handled */ }
}

const referralTrackingDetailVisible = ref(false)
const referralTrackingDetailData = ref<any>(null)
const referralTrackingDetailLoading = ref(false)

async function viewReferralTrackingDetail(row: any) {
  if (!row.bizId) {
    ElMessage.warning("推介记录编号缺失")
    return
  }
  referralTrackingDetailLoading.value = true
  referralTrackingDetailVisible.value = true
  referralTrackingDetailData.value = null
  try {
    const { data } = await getReferralTrackingDetailApi(row.bizId)
    referralTrackingDetailData.value = data
  } catch { /* handled */ } finally {
    referralTrackingDetailLoading.value = false
  }
}

// ==================== 转出详情查看 ====================
const referralDetailVisible = ref(false)
const referralDetailData = ref<ReferralDetailVO | null>(null)
const referralDetailLoading = ref(false)

/** 解析 summary JSON，提取可读字段 */
function parseSummary(summary: string | null | undefined): Record<string, string> {
  if (!summary) return {}
  try {
    return JSON.parse(summary) as Record<string, string>
  } catch {
    return {}
  }
}

async function viewReferralDetail(row: any) {
  if (!row.bizId) {
    ElMessage.warning("转出记录编号缺失")
    return
  }
  referralDetailLoading.value = true
  referralDetailVisible.value = true
  referralDetailData.value = null
  try {
    const { data } = await getReferralDetailApi(row.bizId)
    referralDetailData.value = data
    await fetchData()
  } catch { /* handled */ } finally {
    referralDetailLoading.value = false
  }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)

onMounted(() => {
  messageStore.fetchUnreadCount()
})

// ====== 已发送通知单 ======
const {
  paginationData: sentPagination,
  handleCurrentChange: sentHandleCurrentChange,
  handleSizeChange: sentHandleSizeChange
} = usePagination()

const sentLoading = ref(false)
const sentTableData = ref<SentNoticeVO[]>([])
const sentTotal = ref(0)

const LEGACY_POPULATION_LABEL: Record<string, string> = {
  school: "学校人群",
  key: "重点人群",
  close: "密接人群",
  keyPopulation: "重点人群",
  closeContact: "密接人群"
}

function resolvePopulationTypeLabel(type: string): string {
  return LEGACY_POPULATION_LABEL[type] ?? getPopulationTypeLabel(type)
}
const NOTICE_TYPE_MAP: Record<string, string> = {
  latent: "潜伏者通知单",
  patient: "患者通知单"
}

async function fetchSentData() {
  sentLoading.value = true
  try {
    const { data } = await getSentNoticeListApi({
      pageNum: sentPagination.currentPage,
      size: sentPagination.pageSize
    })
    sentTableData.value = data.records
    sentTotal.value = data.total
  } finally {
    sentLoading.value = false
  }
}

async function handleRemind(row: SentNoticeVO) {
  try {
    await remindNoticeApi(row.id)
    ElMessage.success("催促消息已发送")
  } catch { /* handled */ }
}

watch(
  () => [sentPagination.currentPage, sentPagination.pageSize],
  fetchSentData,
  { immediate: true }
)

// ====== 已发送转出 ======
const {
  paginationData: referralPagination,
  handleCurrentChange: referralHandleCurrentChange,
  handleSizeChange: referralHandleSizeChange
} = usePagination()

const referralLoading = ref(false)
const referralTableData = ref<SentReferralVO[]>([])
const referralTotal = ref(0)

const MODULE_TYPE_MAP: Record<string, string> = {
  screening: "筛查管理",
  suspected: "疑似结核管理",
  latent: "潜伏感染者管理",
  patient: "患者管理"
}

async function fetchReferralData() {
  referralLoading.value = true
  try {
    const { data } = await getSentReferralListApi({
      pageNum: referralPagination.currentPage,
      size: referralPagination.pageSize
    })
    referralTableData.value = data.records
    referralTotal.value = data.total
  } finally {
    referralLoading.value = false
  }
}

watch(
  () => [referralPagination.currentPage, referralPagination.pageSize],
  fetchReferralData,
  { immediate: true }
)

// ====== Tab 切换 ======
const activeTab = ref("received")
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- 收到的消息 -->
        <el-tab-pane label="收到的消息" name="received">
          <div class="mb-4 flex justify-end">
            <el-radio-group v-model="isReadFilter" @change="fetchData">
              <el-radio-button :value="undefined">
                全部
              </el-radio-button>
              <el-radio-button :value="0">
                未读
              </el-radio-button>
              <el-radio-button :value="1">
                已读
              </el-radio-button>
            </el-radio-group>
          </div>

          <el-table v-loading="loading" :data="tableData" border stripe>
            <el-table-column prop="title" label="标题">
              <template #default="{ row }">
                <span :class="{ 'font-bold': !row.isRead }">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" />
            <el-table-column prop="type" label="类型">
              <template #default="{ row }">
                <el-tag size="small" :type="getMessageTypeTagType(row.type)">
                  {{ MESSAGE_TYPE_LABEL_MAP[row.type] || row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="row.isRead ? 'info' : 'success'" size="small">
                  {{ row.isRead ? "已读" : "未读" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" />
            <el-table-column label="操作" fixed="right">
              <template #default="{ row }">
                <!-- 通知单查看 & 接收 -->
                <template v-if="row.type === 'notice_receive' || row.type === 'notice_confirmed'">
                  <el-button type="info" size="small" link @click="viewNoticeDetail(row)">
                    查看通知单
                  </el-button>
                  <el-button
                    v-if="row.type === 'notice_receive' && !row.isRead"
                    type="primary"
                    size="small"
                    @click="handleReceiveNotice(row)"
                  >
                    接收通知单
                  </el-button>
                </template>
                <!-- 推介追踪：查看详情 & 确认/拒绝 -->
                <template v-if="row.type === 'referral_tracking_receive' || row.type === 'referral_tracking_confirmed' || row.type === 'referral_tracking_rejected'">
                  <el-button type="info" size="small" link @click="viewReferralTrackingDetail(row)">
                    查看详情
                  </el-button>
                  <template v-if="row.type === 'referral_tracking_receive'">
                    <el-button type="success" size="small" @click="handleConfirmReferralTracking(row)">
                      确认接收
                    </el-button>
                    <el-button type="danger" size="small" @click="openRejectDialog(row)">
                      拒绝
                    </el-button>
                  </template>
                </template>
                <!-- 转出查看详情 & 确认/拒绝 -->
                <template v-if="row.type === 'referral_receive' || row.type === 'referral_confirmed' || row.type === 'referral_rejected'">
                  <el-button type="info" size="small" link @click="viewReferralDetail(row)">
                    查看详情
                  </el-button>
                  <template v-if="row.type === 'referral_receive'">
                    <el-button type="success" size="small" @click="handleConfirmReferral(row)">
                      确认接收
                    </el-button>
                    <el-button type="danger" size="small" @click="openRejectDialog(row)">
                      拒绝
                    </el-button>
                  </template>
                </template>
                <el-button v-if="!row.isRead" type="primary" size="small" link @click="handleMarkRead(row)">
                  标为已读
                </el-button>
                <el-button type="danger" size="small" link @click="handleDeleteMessage(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination
              v-model:current-page="paginationData.currentPage"
              v-model:page-size="paginationData.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="handleCurrentChange"
              @size-change="handleSizeChange"
            />
          </div>
        </el-tab-pane>

        <!-- 已发送通知单 -->
        <el-tab-pane label="已发送通知单" name="sent">
          <el-table v-loading="sentLoading" :data="sentTableData" border stripe>
            <el-table-column prop="senderName" label="发送者">
              <template #default="{ row }">
                <div>{{ row.senderName || "—" }}</div>
                <div class="text-xs text-gray-400">
                  {{ row.senderOrgName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sentTime" label="发送时间" />
            <el-table-column label="内容">
              <template #default="{ row }">
                {{ NOTICE_TYPE_MAP[row.noticeType] || row.noticeType }} —
                {{ row.patientName }}（{{ resolvePopulationTypeLabel(row.populationType) }}）
              </template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="row.status === 2 ? 'success' : 'warning'" size="small">
                  {{ row.status === 2 ? "已接收" : "待接收" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="receiverName" label="接收者">
              <template #default="{ row }">
                <div>{{ row.receiverName || "—" }}</div>
                <div class="text-xs text-gray-400">
                  {{ row.receiverOrgName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="confirmedTime" label="接收时间">
              <template #default="{ row }">
                {{ row.confirmedTime || "—" }}
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status !== 2" type="warning" size="small" @click="handleRemind(row)">
                  点击提醒
                </el-button>
                <span v-else class="text-sm text-gray-400">已接收</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination
              v-model:current-page="sentPagination.currentPage"
              v-model:page-size="sentPagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="sentTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="sentHandleCurrentChange"
              @size-change="sentHandleSizeChange"
            />
          </div>
        </el-tab-pane>

        <!-- 已发送转出 -->
        <el-tab-pane label="已发送转出" name="referral">
          <el-table v-loading="referralLoading" :data="referralTableData" border stripe>
            <el-table-column prop="subjectName" label="对象姓名" />
            <el-table-column label="类型">
              <template #default="{ row }">
                {{ resolvePopulationTypeLabel(row.populationType) }} —
                {{ MODULE_TYPE_MAP[row.moduleType] || row.moduleType }}
              </template>
            </el-table-column>
            <el-table-column prop="sentTime" label="发送时间" />
            <el-table-column prop="referralReason" label="转出原因" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.referralReason || "—" }}
              </template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 2 ? 'success' : row.status === 3 ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ row.status === 2 ? "已接收" : row.status === 3 ? "已拒绝" : "待确认" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="receiverName" label="接收者">
              <template #default="{ row }">
                <div>{{ row.receiverName || "—" }}</div>
                <div class="text-xs text-gray-400">
                  {{ row.receiverOrgName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="confirmedTime" label="接收时间">
              <template #default="{ row }">
                {{ row.confirmedTime || "—" }}
              </template>
            </el-table-column>
            <el-table-column prop="rejectReason" label="拒绝原因">
              <template #default="{ row }">
                {{ row.rejectReason || "—" }}
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination
              v-model:current-page="referralPagination.currentPage"
              v-model:page-size="referralPagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="referralTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="referralHandleCurrentChange"
              @size-change="referralHandleSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 拒绝转出弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝" width="400px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">
          取消
        </el-button>
        <el-button type="danger" @click="handleRejectReferral">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- 转出详情弹窗 -->
    <el-dialog v-model="referralDetailVisible" title="转出详情" width="680px" append-to-body>
      <div v-loading="referralDetailLoading" style="min-height: 80px">
        <el-descriptions v-if="referralDetailData" :column="2" border>
          <el-descriptions-item label="对象姓名">
            {{ referralDetailData.subjectName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="人群类型">
            {{ resolvePopulationTypeLabel(referralDetailData.populationType) }}
          </el-descriptions-item>
          <el-descriptions-item label="模块">
            {{ MODULE_TYPE_MAP[referralDetailData.moduleType] || referralDetailData.moduleType }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag
              :type="referralDetailData.status === 2 ? 'success' : referralDetailData.status === 3 ? 'danger' : 'warning'"
              size="small"
            >
              {{ referralDetailData.status === 2 ? "已接收" : referralDetailData.status === 3 ? "已拒绝" : "待确认" }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发送方" :span="2">
            {{ referralDetailData.senderName || "-" }}
            <span v-if="referralDetailData.senderOrgName" class="text-gray-400 ml-1">（{{ referralDetailData.senderOrgName }}）</span>
          </el-descriptions-item>
          <el-descriptions-item label="接收方" :span="2">
            {{ referralDetailData.receiverName || "-" }}
            <span v-if="referralDetailData.receiverOrgName" class="text-gray-400 ml-1">（{{ referralDetailData.receiverOrgName }}）</span>
          </el-descriptions-item>
          <el-descriptions-item label="发送时间">
            {{ referralDetailData.sentTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="接收时间">
            {{ referralDetailData.confirmedTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item v-if="referralDetailData.referralReason" label="转出原因" :span="2">
            {{ referralDetailData.referralReason }}
          </el-descriptions-item>
          <el-descriptions-item v-if="referralDetailData.rejectReason" label="拒绝原因" :span="2">
            {{ referralDetailData.rejectReason }}
          </el-descriptions-item>
          <!-- 业务摘要字段（动态渲染） -->
          <template v-if="referralDetailData.summary">
            <el-descriptions-item
              v-for="(val, key) in parseSummary(referralDetailData.summary)"
              :key="key"
              :label="String(key)"
            >
              {{ val || "-" }}
            </el-descriptions-item>
          </template>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="referralDetailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 推介追踪详情弹窗 -->
    <el-dialog v-model="referralTrackingDetailVisible" title="推介详情" width="680px" append-to-body>
      <div v-loading="referralTrackingDetailLoading" style="min-height: 80px">
        <el-descriptions v-if="referralTrackingDetailData" :column="2" border>
          <el-descriptions-item label="姓名">
            {{ referralTrackingDetailData.name || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ referralTrackingDetailData.gender || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="证件号">
            {{ referralTrackingDetailData.idNumber || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ referralTrackingDetailData.phone || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="人群分类">
            {{ referralTrackingDetailData.crowdCategory || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="推介接收人">
            {{ referralTrackingDetailData.receiverUserName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="现住址" :span="2">
            {{ referralTrackingDetailData.currentAddress || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="推介原因" :span="2">
            {{ referralTrackingDetailData.recommendReason || "-" }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="referralTrackingDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情弹窗 -->
    <el-dialog v-model="noticeDetailVisible" title="通知单详情" width="680px" append-to-body>
      <div v-loading="noticeDetailLoading" style="min-height: 80px">
        <el-descriptions v-if="noticeDetailData" :column="2" border>
          <el-descriptions-item label="姓名">
            {{ noticeDetailData.patientName }}
          </el-descriptions-item>
          <el-descriptions-item label="身份证">
            {{ noticeDetailData.idNumber || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ noticeDetailData.gender || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ noticeDetailData.age || "-" }}
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
          <el-descriptions-item label="感染检测时间">
            {{ noticeDetailData.infectionDate || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="检查方法">
            {{ noticeDetailData.infectionMethod || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="感染检查结果" :span="2">
            {{ noticeDetailData.infectionResultValue || "-" }}
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
          <el-descriptions-item v-if="noticeDetailData.customPlanDetail" label="方案详情" :span="2">
            {{ noticeDetailData.customPlanDetail }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗机构">
            {{ noticeDetailData.treatmentInstitution || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="下发时间">
            {{ noticeDetailData.issuedTime || "-" }}
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
            {{ noticeDetailData.sentTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="noticeDetailData.status === 2 ? 'success' : 'warning'" size="small">
              {{ NOTICE_STATUS_MAP[noticeDetailData.status] || "-" }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="noticeDetailData.confirmedTime" label="确认时间">
            {{ noticeDetailData.confirmedTime }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="noticeDetailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mt-4 {
  margin-top: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}
</style>
