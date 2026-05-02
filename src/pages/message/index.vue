<script lang="ts" setup>
import type { SentNoticeVO, SentReferralVO } from "./apis"
import { usePagination } from "@@/composables/usePagination"
import {
  confirmNoticeFromMessageApi,
  confirmReferralFromMessageApi,
  getMessageListApi,
  getSentNoticeListApi,
  getSentReferralListApi,
  markMessageReadApi,
  rejectReferralFromMessageApi,
  remindNoticeApi

} from "./apis"

defineOptions({ name: "Message" })

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
  referral_receive: "待确认分级诊疗",
  referral_confirmed: "分级诊疗已接收",
  referral_rejected: "分级诊疗已被拒绝"
}

function getMessageTypeTagType(type: string) {
  if (type === "notice_timeout") return "danger"
  if (type === "notice_receive") return "warning"
  if (type === "notice_confirmed") return "success"
  if (type === "referral_receive") return "warning"
  if (type === "referral_confirmed") return "success"
  if (type === "referral_rejected") return "danger"
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
  } catch { /* handled */ }
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
    ElMessage.success("通知单接收成功")
  } catch { /* handled */ }
}

// ====== 分级诊疗：消息中确认/拒绝 ======
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
    ElMessage.warning("分级诊疗记录编号缺失")
    return
  }
  try {
    await confirmReferralFromMessageApi(row.bizId)
    await markMessageReadApi(row.id)
    row.isRead = 1
    ElMessage.success("已确认接收分级诊疗信息")
  } catch { /* handled */ }
}

async function handleRejectReferral() {
  const row = rejectingRow.value
  if (!row?.bizId) return
  try {
    await rejectReferralFromMessageApi(row.bizId, rejectReason.value || undefined)
    await markMessageReadApi(row.id)
    row.isRead = 1
    rejectDialogVisible.value = false
    ElMessage.success("已拒绝分级诊疗")
  } catch { /* handled */ }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)

// ====== 已发送通知单 ======
const {
  paginationData: sentPagination,
  handleCurrentChange: sentHandleCurrentChange,
  handleSizeChange: sentHandleSizeChange
} = usePagination()

const sentLoading = ref(false)
const sentTableData = ref<SentNoticeVO[]>([])
const sentTotal = ref(0)

const POPULATION_TYPE_MAP: Record<string, string> = {
  school: "学校人群",
  key: "重点人群",
  close: "密接人群"
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

// ====== 已发送分级诊疗 ======
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
            <el-table-column prop="title" label="标题" min-width="200">
              <template #default="{ row }">
                <span :class="{ 'font-bold': !row.isRead }">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="300" />
            <el-table-column prop="type" label="类型" width="150">
              <template #default="{ row }">
                <el-tag size="small" :type="getMessageTypeTagType(row.type)">
                  {{ MESSAGE_TYPE_LABEL_MAP[row.type] || row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isRead ? 'info' : 'success'" size="small">
                  {{ row.isRead ? "已读" : "未读" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column label="操作" fixed="right" width="220">
              <template #default="{ row }">
                <!-- 通知单接收 -->
                <el-button
                  v-if="row.type === 'notice_receive' && !row.isRead"
                  type="primary"
                  size="small"
                  @click="handleReceiveNotice(row)"
                >
                  接收通知单
                </el-button>
                <!-- 分级诊疗确认/拒绝 -->
                <template v-if="row.type === 'referral_receive' && !row.isRead">
                  <el-button type="success" size="small" @click="handleConfirmReferral(row)">
                    确认接收
                  </el-button>
                  <el-button type="danger" size="small" @click="openRejectDialog(row)">
                    拒绝
                  </el-button>
                </template>
                <el-button v-if="!row.isRead" type="primary" size="small" link @click="handleMarkRead(row)">
                  标为已读
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
            <el-table-column prop="senderName" label="发送者" width="110">
              <template #default="{ row }">
                <div>{{ row.senderName || "—" }}</div>
                <div class="text-xs text-gray-400">
                  {{ row.senderOrgName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sentTime" label="发送时间" width="170" />
            <el-table-column label="内容" min-width="180">
              <template #default="{ row }">
                {{ NOTICE_TYPE_MAP[row.noticeType] || row.noticeType }} —
                {{ row.patientName }}（{{ POPULATION_TYPE_MAP[row.populationType] || row.populationType }}）
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 2 ? 'success' : 'warning'" size="small">
                  {{ row.status === 2 ? "已接收" : "待接收" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="receiverName" label="接收者" width="110">
              <template #default="{ row }">
                <div>{{ row.receiverName || "—" }}</div>
                <div class="text-xs text-gray-400">
                  {{ row.receiverOrgName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="confirmedTime" label="接收时间" width="170">
              <template #default="{ row }">
                {{ row.confirmedTime || "—" }}
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="100">
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

        <!-- 已发送分级诊疗 -->
        <el-tab-pane label="已发送分级诊疗" name="referral">
          <el-table v-loading="referralLoading" :data="referralTableData" border stripe>
            <el-table-column prop="subjectName" label="对象姓名" width="110" />
            <el-table-column label="类型" width="160">
              <template #default="{ row }">
                {{ POPULATION_TYPE_MAP[row.populationType] || row.populationType }} —
                {{ MODULE_TYPE_MAP[row.moduleType] || row.moduleType }}
              </template>
            </el-table-column>
            <el-table-column prop="sentTime" label="发送时间" width="170" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 2 ? 'success' : row.status === 3 ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ row.status === 2 ? "已接收" : row.status === 3 ? "已拒绝" : "待确认" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="receiverName" label="接收者" width="110">
              <template #default="{ row }">
                <div>{{ row.receiverName || "—" }}</div>
                <div class="text-xs text-gray-400">
                  {{ row.receiverOrgName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="confirmedTime" label="接收时间" width="170">
              <template #default="{ row }">
                {{ row.confirmedTime || "—" }}
              </template>
            </el-table-column>
            <el-table-column prop="rejectReason" label="拒绝原因" min-width="120">
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

    <!-- 拒绝分级诊疗弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝分级诊疗" width="400px" append-to-body>
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
