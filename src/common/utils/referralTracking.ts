import { formatDateTime } from "@@/utils/datetime"

/** 推介/追踪单次记录 */
export interface TrackingHistoryItem {
  attempt: number
  status: number
  trackTime: string
  reason?: string
}

export const TRACK_STATUS_LABEL: Record<number, string> = {
  1: "到位",
  2: "未到位",
  3: "其他"
}

export const TRACKING_STATUS_MAP: Record<number, { label: string, type: string }> = {
  0: { label: "待追踪", type: "info" },
  1: { label: "到位", type: "success" },
  2: { label: "未到位", type: "warning" },
  3: { label: "其他", type: "" },
  4: { label: "强制结束", type: "danger" }
}

/** 解析追踪历史 JSON */
export function parseTrackingHistory(json?: string): TrackingHistoryItem[] {
  if (!json) return []
  try {
    return JSON.parse(json)
  } catch {
    return []
  }
}

/** 推介时间：已发送取发送时间，否则取创建时间 */
export function getRecommendTime(row: { recommendSentTime?: string, createTime?: string }) {
  return row.recommendSentTime || row.createTime
}

/** 展示到位时间：优先真实到位日期，否则回退系统记录时间 */
export function formatArrivalDisplay(row: { actualArrivalDate?: string, arrivalTime?: string }) {
  if (row.actualArrivalDate) {
    return formatDateTime(row.actualArrivalDate, "YYYY-MM-DD")
  }
  if (row.arrivalTime) {
    return formatDateTime(row.arrivalTime)
  }
  return "-"
}

/** 展示转诊/接收时间：优先真实转诊日期，否则回退系统记录时间 */
export function formatReferralDisplay(row: { actualReferralDate?: string | null, confirmedTime?: string | null }) {
  if (row.actualReferralDate) {
    return formatDateTime(row.actualReferralDate, "YYYY-MM-DD")
  }
  if (row.confirmedTime) {
    return formatDateTime(row.confirmedTime)
  }
  return "—"
}
