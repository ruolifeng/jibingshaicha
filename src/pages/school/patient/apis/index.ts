import { request } from "@/http/axios"

/** 导出患者管理列表（学校/重点人群） */
export function exportPatientListApi(params: {
  populationType: string
  name?: string
  idNumber?: string
  phone?: string
  dateFrom?: string
  dateTo?: string
}) {
  return request<Blob>({
    url: "export/patient-list",
    method: "get",
    params,
    responseType: "blob"
  })
}

/** 患者列表 */
export function getPatientListApi(params: {
  page: number
  size: number
  populationType: string
  name?: string
  idNumber?: string
  phone?: string
  dateFrom?: string
  dateTo?: string
}) {
  return request<ApiResponseData<any>>({
    url: "patient/list",
    method: "get",
    params
  })
}

/** 历史患者列表 */
export function getPatientHistoryApi(params: {
  page: number
  size: number
  populationType: string
  name?: string
  idNumber?: string
  phone?: string
  startTime?: string
  endTime?: string
}) {
  return request<ApiResponseData<any>>({
    url: "patient/history",
    method: "get",
    params
  })
}

/** 历史患者统计汇总 */
export function getPatientHistoryStatsApi(populationType: string) {
  return request<ApiResponseData<Record<string, number>>>({
    url: "patient/history/stats",
    method: "get",
    params: { populationType }
  })
}

/** 导入大疫情表 */
export function importEpidemicApi(file: File, populationType: string) {
  const formData = new FormData()
  formData.append("file", file)
  formData.append("populationType", populationType)
  return request<ApiResponseData<number>>({
    url: "patient/import-epidemic",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 保存首次随访 */
export function saveFirstVisitApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "patient/first-visit/save",
    method: "post",
    data
  })
}

/** 保存首次随访草稿 */
export function saveFirstVisitDraftApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "patient/first-visit/draft",
    method: "post",
    data
  })
}

/** 查询首次随访 */
export function getFirstVisitApi(patientId: string) {
  return request<ApiResponseData<any>>({
    url: `patient/first-visit/${patientId}`,
    method: "get"
  })
}

/** 保存后续随访 */
export function saveFollowUpApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "patient/follow-up/save",
    method: "post",
    data
  })
}

/** 保存后续随访草稿 */
export function saveFollowUpDraftApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "patient/follow-up/draft",
    method: "post",
    data
  })
}

/** 查询后续随访草稿 */
export function getFollowUpDraftApi(patientId: string) {
  return request<ApiResponseData<any>>({
    url: `patient/follow-up/draft/${patientId}`,
    method: "get"
  })
}

/** 患者结案全程管理统计（实际访视/服药次数） */
export function getFollowUpCaseClosureStatsApi(patientId: string, includeCurrentFollowUp = true) {
  return request<ApiResponseData<{ actualVisitCount: number, actualDoseCount: number }>>({
    url: `patient/follow-up/case-closure-stats/${patientId}`,
    method: "get",
    params: { includeCurrentFollowUp }
  })
}

/** 后续随访列表 */
export function getFollowUpListApi(patientId: string) {
  return request<ApiResponseData<any[]>>({
    url: `patient/follow-up/list/${patientId}`,
    method: "get"
  })
}

/** 删除单条后续随访记录 */
export function deleteFollowUpVisitApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `patient/follow-up/${id}`,
    method: "delete"
  })
}

/** 保存服药管理 */
export function saveMedicationApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "patient/medication/save",
    method: "post",
    data
  })
}

/** 查询服药管理 */
export function getMedicationApi(patientId: string) {
  return request<ApiResponseData<any>>({
    url: `patient/medication/${patientId}`,
    method: "get"
  })
}

/** 完成服药管理（归档） */
export function completeMedicationApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "patient/medication/complete",
    method: "post",
    data
  })
}

/** 归档患者 */
export function archivePatientApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `patient/archive/${id}`,
    method: "post"
  })
}
