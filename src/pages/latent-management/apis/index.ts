/**
 * 聚合潜伏感染者管理 — 公共接口
 * 复用现有 latent/* 接口，但 populationType 留空以查询全部来源（不含密接）。
 * 注：后端 getLatentListApi 当 populationType 为空时返回所有记录；
 *     若需排除密接，在后端通过 NOT IN ('closeContact') 过滤；
 *     当前先由前端在搜索参数中控制（不传 populationType 即全部）。
 */
import { request } from "@/http/axios"

/** 聚合分页查询潜伏感染数据（populationType 为空时返回全部来源） */
export function getLatentAggregateListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({
    url: "latent/list",
    method: "get",
    params
  })
}

/** 追踪操作 */
export function trackLatentApi(data: { id: number; status: number; remark?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/track",
    method: "post",
    data
  })
}

/** 转诊操作 */
export function referralLatentApi(data: { id: number; result: string; remark?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/referral",
    method: "post",
    data
  })
}

/** 发送通知单 */
export function sendNoticeApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "notice/send", method: "post", data })
}

/** 确认接收通知单 */
export function confirmNoticeApi(id: number) {
  return request<ApiResponseData<null>>({ url: `notice/confirm/${id}`, method: "post" })
}

/** 通知单详情 */
export function getNoticeDetailApi(id: number) {
  return request<ApiResponseData<any>>({ url: `notice/detail/${id}`, method: "get" })
}

/** 查询业务关联通知单列表 */
export function getNoticeListByBizApi(bizId: number, noticeType: string) {
  return request<ApiResponseData<any[]>>({
    url: "notice/list",
    method: "get",
    params: { bizId, noticeType }
  })
}

/** 保存督导表 */
export function saveSupervisionApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "supervision/save", method: "post", data })
}

/** 查询督导表详情 */
export function getSupervisionDetailApi(latentInfectionId: number) {
  return request<ApiResponseData<any>>({ url: `supervision/detail/${latentInfectionId}`, method: "get" })
}

/** 结案归档 */
export function closeCaseApi(id: number) {
  return request<ApiResponseData<null>>({ url: `latent/close-case/${id}`, method: "post" })
}

/** 录入胸片结果（仅胸片，不含诊断） */
export function submitXrayOnlyApi(data: {
  id: number
  hasChestXray: string
  chestXrayDate?: string
  chestXrayResult?: string
}) {
  return request<ApiResponseData<null>>({ url: "latent/xray-only", method: "post", data })
}

/** 录入诊断结果 */
export function submitDiagnosisApi(data: { id: number; diagnosisFirst: string }) {
  return request<ApiResponseData<null>>({ url: "latent/diagnosis", method: "post", data })
}
