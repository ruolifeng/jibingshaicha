import { request } from "@/http/axios"

export interface PopulationStat {
  screeningTotal: number
  latentCount: number
  latentRatio: number
  patientCount: number
  patientRatio: number
}

export interface TaskStatsData {
  school: PopulationStat
  keyPopulation: PopulationStat
  closeContact: PopulationStat
}

export interface MessageStatsData {
  latentNoticeSent: number
  latentNoticeConfirmed: number
  patientNoticeSent: number
  patientNoticeConfirmed: number
  referralSent: number
  referralConfirmed: number
  referralRejected: number
}

export interface DashboardSummaryData {
  pendingTracking: number
  pendingVisit: number
  pendingNotice: number
  upcomingReview: number
  /** 统计年度（用于「某某年度管理患者数」展示） */
  managementYear?: number
  /** 统计周期起止（上年度 12/1—本年度 11/30） */
  statPeriodFrom?: string
  statPeriodTo?: string
  /** 病原学阳性人数（在管总览+历史患者中「病原学结果阳性」口径） */
  pathogenPositiveCount?: number
  /** 病原学阳性率（病原学结果阳性人数 / 年度管理患者数，%，保留 1 位小数） */
  pathogenPositiveRate?: number
  /** 推介人数（已发送） */
  recommendCount?: number
  /** 推介到位人数 */
  recommendArrivedCount?: number
  /** 推介到位率（%，保留 1 位小数） */
  recommendArrivalRate?: number
  /** 追踪统计年度（周期结束日所在自然年） */
  trackingStatYear?: number
  /** 追踪统计周期起止（上年度12/1—本年度11/30） */
  trackingPeriodFrom?: string
  trackingPeriodTo?: string
  /** 追踪人数 */
  trackingCount?: number
  /** 追踪到位人数 */
  trackingArrivedCount?: number
  /** 追踪到位率（%，保留 1 位小数） */
  trackingArrivalRate?: number
  /** 治疗成功人数（后续随访完成疗程） */
  treatmentSuccessCount?: number
  /** 治疗成功率（%，保留 1 位小数） */
  treatmentSuccessRate?: number
}

/** 获取首页待处理事项汇总数据 */
export function getDashboardSummaryApi(year?: number | string) {
  return request<ApiResponseData<DashboardSummaryData>>({
    url: "dashboard/summary",
    method: "get",
    params: year != null && year !== "" ? { year: Number(year) } : {}
  })
}

export interface DashboardBatchOption {
  value: string
  label: string
}

/** 获取所有上传批次（任务）列表 */
export function getDashboardBatchesApi() {
  return request<ApiResponseData<DashboardBatchOption[]>>({
    url: "dashboard/batches",
    method: "get"
  })
}

/** 按年度获取三类人群统计数据 */
export function getDashboardTaskStatsApi(year?: number | string) {
  return request<ApiResponseData<TaskStatsData>>({
    url: "dashboard/task-stats",
    method: "get",
    params: year != null && year !== "" ? { year: Number(year) } : {}
  })
}

/** 获取消息通知统计（通知单 + 转诊） */
export function getDashboardMessageStatsApi() {
  return request<ApiResponseData<MessageStatsData>>({
    url: "dashboard/message-stats",
    method: "get"
  })
}
