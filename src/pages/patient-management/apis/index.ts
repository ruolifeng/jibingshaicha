import type { ImportConfirmOptions, ImportResultData } from "@@/composables/useImportIdentityConfirm"
/**
 * 聚合患者管理 — 公共接口
 * populationType 为空时后端返回所有来源的患者数据（含密接确诊患者）。
 */
import { request } from "@/http/axios"

/** 分页查询患者列表（支持跨来源聚合，populationType 为空返回全部） */
export function getPatientListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({ url: "patient/list", method: "get", params })
}

/** 患者详情 */
export function getPatientDetailApi(id: number) {
  return request<ApiResponseData<any>>({ url: `patient/${id}`, method: "get" })
}

/** 更新患者基本信息 */
export function updatePatientApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: `patient/${id}`, method: "put", data })
}

/** 手动新增在管患者 */
export function createPatientApi(data: Record<string, any>) {
  return request<ApiResponseData<number>>({ url: "patient", method: "post", data })
}

/** 批量删除患者（级联删除） */
export function batchDeletePatientsApi(ids: number[]) {
  return request<ApiResponseData<null>>({ url: "patient/batch-delete", method: "delete", data: { ids } })
}

/** 导出在管患者总表 */
export function exportAllPatientsApi(params: Record<string, any>) {
  return request<Blob>({
    url: "export/all-patients",
    method: "get",
    params: { archived: 0, ...params },
    responseType: "blob"
  })
}

/** 导出勾选患者的首次入户随访信息 */
export function exportPatientFirstVisitsApi(ids: number[]) {
  return request<Blob>({
    url: "export/patient-first-visits",
    method: "get",
    params: { ids: ids.join(",") },
    responseType: "blob"
  })
}

/** 导出勾选患者的后续随访信息 */
export function exportPatientFollowUpVisitsApi(ids: number[]) {
  return request<Blob>({
    url: "export/patient-follow-up-visits",
    method: "get",
    params: { ids: ids.join(",") },
    responseType: "blob"
  })
}

/** 批量导入在管患者（字段与新增一致） */
export function importPatientApi(file: File, options: ImportConfirmOptions = {}) {
  const confirmSkipInvalid = options.confirmSkipInvalid ?? false
  const confirmSkipDuplicateInFile = options.confirmSkipDuplicateInFile ?? false
  const formData = new FormData()
  formData.append("file", file)
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  formData.append("confirmSkipDuplicateInFile", String(confirmSkipDuplicateInFile))
  return request<ApiResponseData<ImportResultData>>({
    url: "patient/import",
    method: "post",
    data: formData,
    params: { confirmSkipInvalid, confirmSkipDuplicateInFile },
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 下载在管患者导入模板 */
export function downloadPatientTemplateApi() {
  return request<Blob>({
    url: "template/download",
    method: "get",
    params: { type: "patient" },
    responseType: "blob"
  })
}

/** 导出历史患者总表 */
export function exportPatientHistoryApi(params: Record<string, any>) {
  return request<Blob>({
    url: "export/all-patients",
    method: "get",
    params: { archived: 1, ...params },
    responseType: "blob"
  })
}

/** 历史患者列表 */
export function getPatientHistoryListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({ url: "patient/history", method: "get", params })
}

/** 删除患者（级联删除） */
export function deletePatientApi(id: number) {
  return request<ApiResponseData<null>>({ url: `patient/${id}`, method: "delete" })
}

/** 归档患者 */
export function archivePatientApi(id: number) {
  return request<ApiResponseData<null>>({ url: `patient/archive/${id}`, method: "post" })
}

/** 解锁停止治疗归档的患者（管理员） */
export function unarchivePatientFromStopTreatmentApi(id: number) {
  return request<ApiResponseData<null>>({ url: `patient/unarchive/${id}`, method: "post" })
}

/** 发送患者通知单 */
export function sendPatientNoticeApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "notice/send", method: "post", data })
}

/** 确认接收通知单 */
export function confirmNoticeApi(id: number) {
  return request<ApiResponseData<null>>({ url: `notice/confirm/${id}`, method: "post" })
}

/** 查询业务关联通知单列表 */
export function getNoticeListByBizApi(bizId: number, noticeType: string) {
  return request<ApiResponseData<any[]>>({
    url: "notice/list",
    method: "get",
    params: { bizId, noticeType }
  })
}

/** 保存/更新首次随访 */
export function saveFirstVisitApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/first-visit/save", method: "post", data })
}

/** 录入/更新领药记录 */
export function saveMedicationPickupApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/medication-pickup/save", method: "post", data })
}

/** 查询领药记录列表 */
export function getMedicationPickupListApi(patientId: number) {
  return request<ApiResponseData<any[]>>({ url: `patient/medication-pickup/list/${patientId}`, method: "get" })
}

/** 保存首次随访草稿 */
export function saveFirstVisitDraftApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/first-visit/draft", method: "post", data })
}

/** 查询首次随访详情 */
export function getFirstVisitDetailApi(patientId: number) {
  return request<ApiResponseData<any>>({ url: `patient/first-visit/${patientId}`, method: "get" })
}

/** 保存后续随访（每次新增一条） */
export function saveFollowUpVisitApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/follow-up/save", method: "post", data })
}

/** 保存后续随访草稿 */
export function saveFollowUpDraftApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/follow-up/draft", method: "post", data })
}

/** 查询后续随访草稿 */
export function getFollowUpDraftApi(patientId: number) {
  return request<ApiResponseData<any>>({ url: `patient/follow-up/draft/${patientId}`, method: "get" })
}

/** 查询后续随访列表 */
export function getFollowUpVisitListApi(patientId: number) {
  return request<ApiResponseData<any[]>>({ url: `patient/follow-up/list/${patientId}`, method: "get" })
}

/** 删除单条后续随访记录 */
export function deleteFollowUpVisitApi(id: number) {
  return request<ApiResponseData<null>>({ url: `patient/follow-up/${id}`, method: "delete" })
}

/** 保存服药管理 */
export function saveMedicationApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/medication/save", method: "post", data })
}

/** 查询服药管理 */
export function getMedicationDetailApi(patientId: number) {
  return request<ApiResponseData<any>>({ url: `patient/medication/${patientId}`, method: "get" })
}

/** 转诊操作（患者） */
export function referralPatientApi(data: { id: number, result: string, remark?: string }) {
  return request<ApiResponseData<null>>({ url: "patient/referral", method: "post", data })
}

/** 导入专病网/病案信息表（populationType=specialDisease） */
export function importSpecialDiseaseApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<number>>({ url: "patient/import-special-disease", method: "post", data: formData })
}

/** 下载数据导入模板（type: school/keyPopulation/regular） */
export function downloadTemplateApi(type: string) {
  return request<Blob>({
    url: "template/download",
    method: "get",
    params: { type },
    responseType: "blob"
  })
}
