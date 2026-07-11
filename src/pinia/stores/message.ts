import { getMessageListApi, getUnreadCountApi } from "@/pages/message/apis"
import { pinia } from "@/pinia"
import { router } from "@/router"

const POLL_INTERVAL_MS = 60_000
const ALERT_STORAGE_KEY = "message-alerted-ids"

/** 需居中弹窗提醒的消息类型（按优先级排序） */
const ALERT_TYPE_PRIORITY: string[] = [
  "notice_receive",
  "notice_timeout",
  "referral_receive",
  "referral_tracking_receive",
  "supervision_timeout",
  "visit_timeout",
  "sputum_culture_pending",
  "review_reminder"
]

const ALERT_TYPE_TITLE: Record<string, string> = {
  notice_receive: "待接收通知单",
  notice_timeout: "通知单超时提醒",
  referral_receive: "待确认转出",
  referral_tracking_receive: "待确认推介",
  supervision_timeout: "督导表超时",
  visit_timeout: "随访超时",
  sputum_culture_pending: "痰培养未补充",
  review_reminder: "复查提醒"
}

function loadAlertedIds(): Set<number> {
  try {
    const raw = sessionStorage.getItem(ALERT_STORAGE_KEY)
    if (!raw) return new Set()
    const ids = JSON.parse(raw) as number[]
    return new Set(Array.isArray(ids) ? ids : [])
  } catch {
    return new Set()
  }
}

function persistAlertedIds(ids: Set<number>) {
  sessionStorage.setItem(ALERT_STORAGE_KEY, JSON.stringify([...ids]))
}

function buildAlertContent(type: string, items: any[]): string {
  const count = items.length
  if (count === 1) {
    return items[0].content || items[0].title || "您有新的未读消息，请前往消息中心处理。"
  }
  const title = ALERT_TYPE_TITLE[type] || "未读消息"
  return `您有 <strong>${count}</strong> 条${title}，请前往消息中心逐一处理。`
}

export const useMessageStore = defineStore("message", () => {
  const unreadCount = ref(0)
  const alertedIds = ref<Set<number>>(loadAlertedIds())
  const alertVisible = ref(false)

  let timer: ReturnType<typeof setInterval> | null = null

  function markAlerted(ids: number[]) {
    ids.forEach(id => alertedIds.value.add(id))
    persistAlertedIds(alertedIds.value)
  }

  async function showCenterAlert(title: string, msgContent: string) {
    alertVisible.value = true
    try {
      await ElMessageBox.alert(msgContent, title, {
        type: "warning",
        dangerouslyUseHTMLString: true,
        confirmButtonText: "前往消息中心",
        center: true,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showClose: false
      })
      await router.push("/message")
    } catch {
      // 用户关闭弹窗（不应出现，因为 showClose: false）
    } finally {
      alertVisible.value = false
    }
  }

  /** 检查未读消息并以居中弹窗提醒（每种类型每次只弹一条批次） */
  async function checkNewMessageAlerts() {
    if (alertVisible.value) return
    try {
      const { data } = await getMessageListApi({ page: 1, size: 50, isRead: 0 })
      const records: any[] = data?.records || []
      if (records.length === 0) return

      for (const type of ALERT_TYPE_PRIORITY) {
        const items = records.filter(
          item => item.type === type && !alertedIds.value.has(item.id)
        )
        if (items.length > 0) {
          markAlerted(items.map(item => item.id))
          await showCenterAlert(
            ALERT_TYPE_TITLE[type] || "消息提醒",
            buildAlertContent(type, items)
          )
          return
        }
      }

      const others = records.filter(item => !alertedIds.value.has(item.id))
      if (others.length > 0) {
        markAlerted(others.map(item => item.id))
        const content = others.length === 1
          ? (others[0].content || others[0].title || "您有新的未读消息，请前往消息中心查看。")
          : `您有 <strong>${others.length}</strong> 条未读消息，请前往消息中心查看。`
        await showCenterAlert("新消息提醒", content)
      }
    } catch {
      /* 静默失败 */
    }
  }

  async function fetchUnreadCount() {
    try {
      const { data } = await getUnreadCountApi()
      unreadCount.value = data || 0
      if (unreadCount.value > 0) {
        await checkNewMessageAlerts()
      }
    } catch {
      /* 静默失败 */
    }
  }

  function startPolling() {
    if (timer) return
    fetchUnreadCount()
    timer = setInterval(fetchUnreadCount, POLL_INTERVAL_MS)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function resetUnread() {
    unreadCount.value = 0
  }

  return {
    unreadCount,
    fetchUnreadCount,
    checkNewMessageAlerts,
    startPolling,
    stopPolling,
    resetUnread
  }
})

/** 在 setup 外使用 */
export function useMessageStoreOutside() {
  return useMessageStore(pinia)
}
