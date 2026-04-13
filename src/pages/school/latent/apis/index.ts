import { request } from "@/http/axios"

/** 分页查询潜伏感染数据 */
export function getLatentListApi(params: {
  page: number
  size: number
  populationType: string
  name?: string
  idNumber?: string
  trackingStatus?: number
  archived?: number
}) {
  return request<ApiResponseData<any>>({
    url: "latent/list",
    method: "get",
    params
  })
}

/** 追踪操作 */
export function trackLatentApi(data: { id: number, status: number, remark?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/track",
    method: "post",
    data
  })
}

/** 转诊操作 */
export function referralLatentApi(data: { id: number, result: string, remark?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/referral",
    method: "post",
    data
  })
}

/** 发送通知单 */
export function sendNoticeApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "notice/send",
    method: "post",
    data
  })
}

/** 确认接收通知单 */
export function confirmNoticeApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `notice/confirm/${id}`,
    method: "post"
  })
}

/** 通知单详情 */
export function getNoticeDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `notice/detail/${id}`,
    method: "get"
  })
}

/** 查询业务关联通知单 */
export function getNoticeListByBizApi(bizId: number, noticeType: string) {
  return request<ApiResponseData<any[]>>({
    url: "notice/list",
    method: "get",
    params: { bizId, noticeType }
  })
}

/** 保存督导表 */
export function saveSupervisionApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "supervision/save",
    method: "post",
    data
  })
}

/** 查询督导表详情 */
export function getSupervisionDetailApi(latentInfectionId: number) {
  return request<ApiResponseData<any>>({
    url: `supervision/detail/${latentInfectionId}`,
    method: "get"
  })
}
