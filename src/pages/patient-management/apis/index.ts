/**
 * 聚合患者管理 — 公共接口
 * populationType 为空时后端返回所有来源的患者数据（含密接确诊患者）。
 */
import { request } from "@/http/axios"

/** 分页查询患者列表（支持跨来源聚合，populationType 为空返回全部） */
export function getPatientListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({ url: "patient/list", method: "get", params })
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

/** 查询首次随访详情 */
export function getFirstVisitDetailApi(patientId: number) {
  return request<ApiResponseData<any>>({ url: `patient/first-visit/detail/${patientId}`, method: "get" })
}

/** 保存后续随访（每次新增一条） */
export function saveFollowUpVisitApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/follow-up/save", method: "post", data })
}

/** 查询后续随访列表 */
export function getFollowUpVisitListApi(patientId: number) {
  return request<ApiResponseData<any[]>>({ url: `patient/follow-up/list/${patientId}`, method: "get" })
}

/** 保存服药管理 */
export function saveMedicationApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({ url: "patient/medication/save", method: "post", data })
}

/** 查询服药管理 */
export function getMedicationDetailApi(patientId: number) {
  return request<ApiResponseData<any>>({ url: `patient/medication/detail/${patientId}`, method: "get" })
}

/** 转诊操作（患者） */
export function referralPatientApi(data: { id: number; result: string; remark?: string }) {
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
