<script lang="ts" setup>
import type { NotifyItem } from "./type"
import { Bell } from "@element-plus/icons-vue"
import { getMessageListApi, getUnreadCountApi, markMessageReadApi } from "@/pages/message/apis"
import List from "./List.vue"

defineOptions({ name: "Notify" })

const router = useRouter()

/** 后端消息类型到展示配置的映射 */
const TYPE_CONFIG: Record<string, { label: string, status: NotifyItem["status"] }> = {
  notice_timeout: { label: "通知单超时", status: "danger" },
  supervision_timeout: { label: "督导表超时", status: "warning" },
  visit_timeout: { label: "随访超时", status: "warning" },
  review_reminder: { label: "复查提醒", status: "primary" }
}

/** 面板宽度 */
const popoverWidth = 380

/** 角标最大值 */
const badgeMax = 99

/** 未读数 */
const unreadCount = ref(0)

/** 未读消息（最近 10 条） */
const unreadList = ref<NotifyItem[]>([])

/** 原始未读消息（用于点击跳转/标记已读） */
const rawList = ref<any[]>([])

const loading = ref(false)

let timer: ReturnType<typeof setInterval> | null = null

/** 拉取未读数量 */
async function fetchUnreadCount() {
  try {
    const { data } = await getUnreadCountApi()
    unreadCount.value = data || 0
  } catch { /* 静默失败，避免打扰用户 */ }
}

/** 拉取未读消息列表 */
async function fetchUnreadList() {
  loading.value = true
  try {
    const { data } = await getMessageListApi({ page: 1, size: 10, isRead: 0 })
    const records = data?.records || []
    rawList.value = records
    unreadList.value = records.map((item: any) => {
      const cfg = TYPE_CONFIG[item.type as string]
      return {
        title: item.title,
        description: item.content,
        datetime: item.createTime,
        extra: cfg?.label || item.type,
        status: cfg?.status || "info"
      } as NotifyItem
    })
  } finally {
    loading.value = false
  }
}

/** 打开面板时刷新数据 */
function handleVisibleChange(visible: boolean) {
  if (visible) fetchUnreadList()
}

/** 全部标为已读 */
async function handleMarkAllRead() {
  if (rawList.value.length === 0) return
  try {
    await Promise.all(rawList.value.map((item: any) => markMessageReadApi(item.id)))
    ElMessage.success("全部标为已读")
    unreadCount.value = 0
    unreadList.value = []
    rawList.value = []
  } catch { /* handled by interceptor */ }
}

/** 跳转到消息中心 */
function handleViewAll() {
  router.push("/message")
}

onMounted(() => {
  fetchUnreadCount()
  // 每 60s 轮询一次未读数量
  timer = setInterval(fetchUnreadCount, 60000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="notify">
    <el-popover
      placement="bottom"
      :width="popoverWidth"
      trigger="click"
      @visible-change="handleVisibleChange"
    >
      <template #reference>
        <el-badge :value="unreadCount" :max="badgeMax" :hidden="unreadCount === 0">
          <el-tooltip effect="dark" content="消息通知" placement="bottom">
            <el-icon :size="20">
              <Bell />
            </el-icon>
          </el-tooltip>
        </el-badge>
      </template>
      <template #default>
        <div class="notify-header">
          <span class="notify-title">未读消息（{{ unreadCount }}）</span>
          <el-button v-if="unreadList.length > 0" link type="primary" size="small" @click="handleMarkAllRead">
            全部已读
          </el-button>
        </div>
        <el-scrollbar v-loading="loading" height="400px">
          <List :data="unreadList" />
        </el-scrollbar>
        <div class="notify-footer">
          <el-button link @click="handleViewAll">
            查看全部消息
          </el-button>
        </div>
      </template>
    </el-popover>
  </div>
</template>

<style lang="scss" scoped>
.notify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  .notify-title {
    font-weight: bold;
    font-size: 14px;
  }
}
.notify-footer {
  text-align: center;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color);
}
</style>
