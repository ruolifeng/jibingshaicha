/**
 * 聚合潜伏感染者管理 — 公共接口
 * 复用现有 latent/* 接口；populationType 为空时返回全部来源（含在管总览手动/导入的密接，排除密接筛查同步数据）。
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

/** 潜伏感染详情 */
export function getLatentDetailApi(id: number) {
  return request<ApiResponseData<any>>({ url: `latent/${id}`, method: "get" })
}

/** 更新潜伏感染基本信息 */
export function updateLatentApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: `latent/${id}`, method: "put", data })
}

/** 手动新增潜伏感染记录 */
export function createLatentApi(data: Record<string, any>) {
  return request<ApiResponseData<number>>({ url: "latent", method: "post", data })
}

/** 删除潜伏感染记录（级联删除） */
export function deleteLatentApi(id: number) {
  return request<ApiResponseData<null>>({ url: `latent/${id}`, method: "delete" })
}

/** 批量删除潜伏感染记录（级联删除） */
export function batchDeleteLatentApi(ids: number[]) {
  return request<ApiResponseData<null>>({ url: "latent/batch-delete", method: "delete", data: { ids } })
}

/** 导出在管潜伏感染者总表 */
export function exportAllLatentApi(params: Record<string, any>) {
  return request<Blob>({
    url: "export/all-latent",
    method: "get",
    params: { archived: 0, ...params },
    responseType: "blob"
  })
}

/** 批量导入潜伏感染者（字段与新增一致） */
export function importLatentApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{ successCount: number, errors: string[] }>>({
    url: "latent/import",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 下载潜伏感染者导入模板 */
export function downloadLatentTemplateApi() {
  return request<Blob>({
    url: "template/download",
    method: "get",
    params: { type: "latent" },
    responseType: "blob"
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
  return request<ApiResponseData<null>>({ url: "notice/send", method: "post", data })
}

/** 保存通知单草稿 */
export function saveNoticeDraftApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "notice/draft", method: "post", data })
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

/** 保存督导表草稿 */
export function saveSupervisionDraftApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "supervision/draft", method: "post", data })
}

/** 保存/提交督导表 */
export function saveSupervisionApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "supervision/save", method: "post", data })
}

/** 查询督导表草稿 */
export function getSupervisionDraftApi(latentInfectionId: number) {
  return request<ApiResponseData<any>>({ url: `supervision/draft/${latentInfectionId}`, method: "get" })
}

/** 督导表记录列表 */
export function getSupervisionListApi(latentInfectionId: number) {
  return request<ApiResponseData<any[]>>({ url: `supervision/list/${latentInfectionId}`, method: "get" })
}

/** 按 ID 查询督导表详情 */
export function getSupervisionByIdApi(id: number) {
  return request<ApiResponseData<any>>({ url: `supervision/${id}`, method: "get" })
}

/** 查询最新督导表详情（兼容旧接口） */
export function getSupervisionDetailApi(latentInfectionId: number) {
  return request<ApiResponseData<any>>({ url: `supervision/detail/${latentInfectionId}`, method: "get" })
}

/** 结案归档 */
export function closeCaseApi(id: number) {
  return request<ApiResponseData<null>>({ url: `latent/close-case/${id}`, method: "post" })
}

/** 历史患者列表（已归档） */
export function getLatentHistoryListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({ url: "latent/history", method: "get", params })
}

/** 导出历史潜伏感染者总表 */
export function exportLatentHistoryApi(params: Record<string, any>) {
  return request<Blob>({
    url: "export/all-latent",
    method: "get",
    params: { archived: 1, ...params },
    responseType: "blob"
  })
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
export function submitDiagnosisApi(data: { id: number, diagnosisFirst: string }) {
  return request<ApiResponseData<null>>({ url: "latent/diagnosis", method: "post", data })
}
