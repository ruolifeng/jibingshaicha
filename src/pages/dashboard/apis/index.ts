import { request } from "@/http/axios"

/** 获取首页待处理事项汇总数据 */
export function getDashboardSummaryApi() {
  return request<ApiResponseData<Record<string, number>>>({
    url: "dashboard/summary",
    method: "get"
  })
}
