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

/** 推介模块：未到位达到该次数后强制结束（与后端一致） */
export const RECOMMEND_FORCE_END_THRESHOLD = 4

/** 统一转为数字状态，避免接口偶发字符串导致 includes/=== 判断失败 */
export function toTrackingStatus(value: unknown): number | null {
  if (value === null || value === undefined || value === "") return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

/** 是否已真正结束追踪流程（强制结束，或诊断结案归档） */
export function isTrackingFlowClosed(
  row: { trackingStatus?: unknown, archived?: unknown, diagnosisResult?: unknown }
): boolean {
  const status = toTrackingStatus(row.trackingStatus)
  if (status === 4) return true
  const hasDiagnosis = row.diagnosisResult !== null && row.diagnosisResult !== undefined && row.diagnosisResult !== ""
  // 仅「有诊断且已归档」视为结案；待追踪/未到位/其他/到位未诊断即使误归档也应可继续
  return Number(row.archived) === 1 && hasDiagnosis
}

/** 原生追踪（大疫情等）未到位强制结束次数 */
export const TRACK_FORCE_END_THRESHOLD = 3

/**
 * 是否显示「追踪」按钮：
 * - 到位(1)、强制结束(4)：不显示
 * - 已诊断并归档（结案）：不显示
 * - 待追踪(0) / 未到位(2) / 其他(3)：未到位次数未达上限则继续显示
 * 「其他」仅表示本次备注结果，不结束追踪流程。
 */
export function canShowContinueTrackButton(
  row: { trackingStatus?: unknown, archived?: unknown, notInPlaceCount?: unknown, diagnosisResult?: unknown },
  forceEndThreshold = RECOMMEND_FORCE_END_THRESHOLD
): boolean {
  const status = toTrackingStatus(row.trackingStatus)
  if (status === 1 || status === 4) return false
  if (isTrackingFlowClosed(row)) return false
  if (status === null || status === 0 || status === 2 || status === 3) {
    return Number(row.notInPlaceCount ?? 0) < forceEndThreshold
  }
  return false
}

/** 到位后且尚未诊断：可录入感染检测/胸片与诊断 */
export function canShowArrivalFollowupButtons(
  row: { trackingStatus?: unknown, archived?: unknown, diagnosisResult?: unknown }
): boolean {
  if (toTrackingStatus(row.trackingStatus) !== 1) return false
  const diagnosis = row.diagnosisResult
  if (diagnosis !== null && diagnosis !== undefined && diagnosis !== "") return false
  // 到位未诊断时即使误归档也允许补录
  return true
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

/** 推介追踪诊断结果展示；选择「其他」时附带备注 */
export function formatReferralDiagnosisDisplay(row: {
  diagnosisResult?: string | null
  diagnosisRemark?: string | null
}) {
  if (!row.diagnosisResult) return ""
  if (row.diagnosisResult === "其他" && row.diagnosisRemark) {
    return `${row.diagnosisResult}：${row.diagnosisRemark}`
  }
  return row.diagnosisResult
}
