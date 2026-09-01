import type { ImportConfirmOptions, ImportResultData } from "@@/composables/useImportIdentityConfirm"
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

/** 表头 Excel 式筛选：某列实际出现过的去重值 */
export function getLatentColumnDistinctApi(
  field: string,
  populationType?: string,
  referralResult = "latent"
) {
  return request<ApiResponseData<string[]>>({
    url: "latent/column-distinct",
    method: "get",
    params: { field, populationType, archived: 0, referralResult }
  })
}

/** 潜伏感染详情 */
export function getLatentDetailApi(id: string) {
  return request<ApiResponseData<any>>({ url: `latent/${id}`, method: "get" })
}

/** 更新潜伏感染基本信息 */
export function updateLatentApi(id: string, data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: `latent/${id}`, method: "put", data })
}

/** 手动新增潜伏感染记录 */
export function createLatentApi(data: Record<string, any>) {
  return request<ApiResponseData<number>>({ url: "latent", method: "post", data })
}

/** 删除潜伏感染记录（级联删除） */
export function deleteLatentApi(id: string) {
  return request<ApiResponseData<null>>({ url: `latent/${id}`, method: "delete" })
}

/** 批量删除潜伏感染记录（级联删除） */
export function batchDeleteLatentApi(ids: string[]) {
  return request<ApiResponseData<null>>({ url: "latent/batch-delete", method: "delete", data: { ids } })
}

/** 按当前筛选条件删除在管潜伏感染者 */
export function deleteLatentByFilterApi(params: Record<string, any>) {
  return request<ApiResponseData<number>>({
    url: "latent/delete-by-filter",
    method: "delete",
    params: { archived: 0, referralResult: "latent", ...params },
    timeout: 300000
  })
}

/** 导出在管潜伏感染者总表（与列表一致：仅 referralResult=latent；支持 ids 勾选导出） */
export function exportAllLatentApi(params: Record<string, any>) {
  const { ids, ...rest } = params
  return request<Blob>({
    url: "export/all-latent",
    method: "get",
    params: {
      archived: 0,
      referralResult: "latent",
      ...rest,
      ...(Array.isArray(ids) && ids.length ? { ids: ids.join(",") } : {})
    },
    responseType: "blob"
  })
}

/** 批量导入潜伏感染者（字段与新增一致） */
export function importLatentApi(file: File, options: ImportConfirmOptions = {}) {
  const confirmSkipInvalid = options.confirmSkipInvalid ?? false
  const confirmSkipDuplicateInFile = options.confirmSkipDuplicateInFile ?? false
  const formData = new FormData()
  formData.append("file", file)
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  formData.append("confirmSkipDuplicateInFile", String(confirmSkipDuplicateInFile))
  return request<ApiResponseData<ImportResultData>>({
    url: "latent/import",
    method: "post",
    data: formData,
    params: { confirmSkipInvalid, confirmSkipDuplicateInFile },
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
export function trackLatentApi(data: { id: string, status: number, remark?: string, actualArrivalDate?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/track",
    method: "post",
    data
  })
}

/** 转诊操作 */
export function referralLatentApi(data: { id: string, result: string, remark?: string, actualReferralDate?: string }) {
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
export function confirmNoticeApi(id: string) {
  return request<ApiResponseData<null>>({ url: `notice/confirm/${id}`, method: "post" })
}

/** 通知单详情 */
export function getNoticeDetailApi(id: string) {
  return request<ApiResponseData<any>>({ url: `notice/detail/${id}`, method: "get" })
}

/** 查询业务关联通知单列表 */
export function getNoticeListByBizApi(bizId: string, noticeType: string) {
  return request<ApiResponseData<any[]>>({
    url: "notice/list",
    method: "get",
    params: { bizId, noticeType }
  })
}

/** 修改潜伏感染者通知单登记号（同步主表，总览/督导/服药/历史共用） */
export function updateNoticeRegistrationNoApi(noticeId: string, registrationNo: string) {
  return request<ApiResponseData<null>>({
    url: `notice/${noticeId}/registration-no`,
    method: "post",
    data: { registrationNo }
  })
}

/** 修改通知单联系电话、现居住地址、户籍地址（同步潜伏感染主表） */
export function updateNoticeContactApi(noticeId: string, data: {
  phone?: string
  currentAddress?: string
  householdAddress?: string
}) {
  return request<ApiResponseData<null>>({
    url: `notice/${noticeId}/contact`,
    method: "post",
    data
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
export function getSupervisionDraftApi(latentInfectionId: string) {
  return request<ApiResponseData<any>>({ url: `supervision/draft/${latentInfectionId}`, method: "get" })
}

/** 督导表记录列表 */
export function getSupervisionListApi(latentInfectionId: string) {
  return request<ApiResponseData<any[]>>({ url: `supervision/list/${latentInfectionId}`, method: "get" })
}

/** 导出督导表（ids 勾选；否则按当前筛选） */
export function exportLatentSupervisionFormsApi(params: Record<string, any>) {
  const { ids, ...rest } = params
  return request<Blob>({
    url: "export/latent-supervision-forms",
    method: "get",
    params: {
      archived: 0,
      referralResult: "latent",
      trackingStatus: 1,
      dateFilterBy: "supervisionFill",
      ...rest,
      ...(Array.isArray(ids) && ids.length ? { ids: ids.join(",") } : {})
    },
    responseType: "blob"
  })
}

/** 导出潜伏感染者服药管理（ids 勾选；否则按当前筛选） */
export function exportLatentMedicationsApi(params: Record<string, any>) {
  const { ids, ...rest } = params
  return request<Blob>({
    url: "export/latent-medications",
    method: "get",
    params: {
      archived: 0,
      referralResult: "latent",
      trackingStatus: 1,
      ...rest,
      ...(Array.isArray(ids) && ids.length ? { ids: ids.join(",") } : {})
    },
    responseType: "blob"
  })
}

/** 导出潜伏感染者通知单（ids 勾选；否则按当前筛选） */
export function exportLatentNoticesApi(params: Record<string, any>) {
  const { ids, ...rest } = params
  return request<Blob>({
    url: "export/latent-notices",
    method: "get",
    params: {
      archived: 0,
      referralResult: "latent",
      dateFilterBy: "noticeFill",
      ...rest,
      ...(Array.isArray(ids) && ids.length ? { ids: ids.join(",") } : {})
    },
    responseType: "blob"
  })
}

/** 按 ID 查询督导表详情 */
export function getSupervisionByIdApi(id: string) {
  return request<ApiResponseData<any>>({ url: `supervision/${id}`, method: "get" })
}

/** 删除督导表记录 */
export function deleteSupervisionApi(id: string) {
  return request<ApiResponseData<null>>({ url: `supervision/${id}`, method: "delete" })
}

/** 查询最新督导表详情（兼容旧接口） */
export function getSupervisionDetailApi(latentInfectionId: string) {
  return request<ApiResponseData<any>>({ url: `supervision/detail/${latentInfectionId}`, method: "get" })
}

/** 结案归档 */
export function closeCaseApi(id: string) {
  return request<ApiResponseData<null>>({ url: `latent/close-case/${id}`, method: "post" })
}

/** 解锁结案归档的潜伏感染者（管理员） */
export function unarchiveLatentFromCloseCaseApi(id: string) {
  return request<ApiResponseData<null>>({ url: `latent/unarchive/${id}`, method: "post" })
}

/** 历史患者列表（已归档） */
export function getLatentHistoryListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({ url: "latent/history", method: "get", params })
}

/** 导出历史潜伏感染者总表（仅已确认为潜伏感染者的归档数据） */
export function exportLatentHistoryApi(params: Record<string, any>) {
  return request<Blob>({
    url: "export/all-latent",
    method: "get",
    params: { archived: 1, referralResult: "latent", ...params },
    responseType: "blob"
  })
}

/** 录入胸片结果（仅胸片，不含诊断） */
export function submitXrayOnlyApi(data: {
  id: string
  hasChestXray: string
  chestXrayDate?: string
  chestXrayResult?: string
}) {
  return request<ApiResponseData<null>>({ url: "latent/xray-only", method: "post", data })
}

/** 录入诊断结果 */
export function submitDiagnosisApi(data: { id: string, diagnosisFirst: string }) {
  return request<ApiResponseData<null>>({ url: "latent/diagnosis", method: "post", data })
}

/** 保存潜伏感染者服药管理 */
export function saveLatentMedicationApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "latent/medication/save", method: "post", data })
}

/** 查询潜伏感染者服药管理 */
export function getLatentMedicationApi(latentInfectionId: string) {
  return request<ApiResponseData<any>>({ url: `latent/medication/${latentInfectionId}`, method: "get" })
}

/** 完成潜伏感染者服药管理（归档） */
export function completeLatentMedicationApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "latent/medication/complete", method: "post", data })
}

/** 保存潜伏感染者领药记录 */
export function saveLatentMedicationPickupApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "latent/medication-pickup/save", method: "post", data })
}

/** 潜伏感染者领药记录列表 */
export function getLatentMedicationPickupListApi(latentInfectionId: string) {
  return request<ApiResponseData<any[]>>({ url: `latent/medication-pickup/list/${latentInfectionId}`, method: "get" })
}

/** 删除潜伏感染者领药记录 */
export function deleteLatentMedicationPickupApi(id: string) {
  return request<ApiResponseData<null>>({ url: `latent/medication-pickup/${id}`, method: "delete" })
}
