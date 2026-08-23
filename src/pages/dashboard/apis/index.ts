import { withDepartmentIds } from "@@/utils/departmentFilter"
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
  /** 追踪模块待追踪人数（不含筛查管理版块） */
  pendingTracking: number
  pendingVisit: number
  pendingNotice: number
  upcomingReview: number
  /** 统计年度（用于「某某年度管理患者数」展示） */
  managementYear?: number
  /** 统计周期起止（自然年 1/1—12/31） */
  statPeriodFrom?: string
  statPeriodTo?: string
  /** 发病率分子人数（经典病原学阳性 + 结核性胸膜炎且0月序影像/分子阳） */
  pathogenPositiveCount?: number
  /** 发病率（分子 / 年度管理患者数，%，保留 1 位小数） */
  pathogenPositiveRate?: number
  /** 耐药筛查人数（首次随访耐药情况为耐药/非耐药） */
  drugResistanceScreenedCount?: number
  /** 耐药筛查率（耐药筛查人数 / 年度管理患者数，%，保留 1 位小数） */
  drugResistanceScreeningRate?: number
  /** 推介人数（已发送） */
  recommendCount?: number
  /** 推介到位人数 */
  recommendArrivedCount?: number
  /** 推介到位率（%，保留 1 位小数） */
  recommendArrivalRate?: number
  /** 追踪统计年度（周期结束日所在自然年） */
  trackingStatYear?: number
  /** 追踪统计周期起止（自然年 1/1—12/31） */
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
export function getDashboardSummaryApi(year?: number | string, departmentIds?: string[]) {
  return request<ApiResponseData<DashboardSummaryData>>({
    url: "dashboard/summary",
    method: "get",
    params: withDepartmentIds(
      year != null && year !== "" ? { year: Number(year) } : {},
      departmentIds
    )
  })
}

export interface DashboardBatchOption {
  value: string
  label: string
}

/** 获取所有上传批次（任务）列表 */
export function getDashboardBatchesApi(departmentIds?: string[]) {
  return request<ApiResponseData<DashboardBatchOption[]>>({
    url: "dashboard/batches",
    method: "get",
    params: withDepartmentIds({}, departmentIds)
  })
}

/** 按年度获取三类人群统计数据 */
export function getDashboardTaskStatsApi(year?: number | string, departmentIds?: string[]) {
  return request<ApiResponseData<TaskStatsData>>({
    url: "dashboard/task-stats",
    method: "get",
    params: withDepartmentIds(
      year != null && year !== "" ? { year: Number(year) } : {},
      departmentIds
    )
  })
}

/** 获取消息通知统计（通知单 + 转诊） */
export function getDashboardMessageStatsApi(departmentIds?: string[]) {
  return request<ApiResponseData<MessageStatsData>>({
    url: "dashboard/message-stats",
    method: "get",
    params: withDepartmentIds({}, departmentIds)
  })
}

export interface UpcomingVisitSupervisionItem {
  type: "follow_up" | "supervision"
  bizId: string
  name: string
  dueDate: string
  leadDays: number
  /** 管理人对应机构 */
  managerOrgName?: string
}

/** 首页：距下次随访/督导 7/3/1 天的提醒 */
export function getUpcomingVisitSupervisionApi(departmentIds?: string[]) {
  return request<ApiResponseData<UpcomingVisitSupervisionItem[]>>({
    url: "reminder/visit-supervision/upcoming",
    method: "get",
    params: withDepartmentIds({}, departmentIds)
  })
}
