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

/** 获取首页待处理事项汇总数据 */
export function getDashboardSummaryApi() {
  return request<ApiResponseData<Record<string, number>>>({
    url: "dashboard/summary",
    method: "get"
  })
}

/** 获取所有上传批次（任务）列表 */
export function getDashboardBatchesApi() {
  return request<ApiResponseData<string[]>>({
    url: "dashboard/batches",
    method: "get"
  })
}

/** 按任务批次获取三类人群统计数据 */
export function getDashboardTaskStatsApi(batch?: string) {
  return request<ApiResponseData<TaskStatsData>>({
    url: "dashboard/task-stats",
    method: "get",
    params: batch ? { batch } : {}
  })
}

/** 获取消息通知统计（通知单 + 转诊） */
export function getDashboardMessageStatsApi() {
  return request<ApiResponseData<MessageStatsData>>({
    url: "dashboard/message-stats",
    method: "get"
  })
}
