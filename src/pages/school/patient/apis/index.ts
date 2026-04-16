import { request } from "@/http/axios"

/** 患者列表 */
export function getPatientListApi(params: {
  page: number
  size: number
  populationType: string
  name?: string
  idNumber?: string
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

/** 查询首次随访 */
export function getFirstVisitApi(patientId: number) {
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

/** 后续随访列表 */
export function getFollowUpListApi(patientId: number) {
  return request<ApiResponseData<any[]>>({
    url: `patient/follow-up/list/${patientId}`,
    method: "get"
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
export function getMedicationApi(patientId: number) {
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
export function archivePatientApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `patient/archive/${id}`,
    method: "post"
  })
}
