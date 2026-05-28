import { request } from "@/http/axios"

/** 下载密接个案表导入模板（73 列表头） */
export function downloadCloseContactCaseTemplateApi() {
  return request<Blob>({
    url: "template/download",
    method: "get",
    params: { type: "closeContactCase" },
    responseType: "blob"
  })
}

/** 上传密接个案表 Excel（73列模板） */
export function uploadCloseContactCaseApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{ successCount: number, errors: string[] }>>({
    url: "close-contact/case/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 分页查询密接个案表 */
export function getCloseContactCaseListApi(params: {
  page: number
  size: number
  name?: string
  idNumber?: string
  district?: string
  phone?: string
  creatorUsername?: string
  diagnosisResult?: string
}) {
  return request<ApiResponseData<any>>({
    url: "close-contact/case/list",
    method: "get",
    params
  })
}

/** 导出密接个案表 */
export function exportCloseContactCaseApi(params?: {
  ids?: number[]
  name?: string
  idNumber?: string
  district?: string
  phone?: string
  creatorUsername?: string
  diagnosisResult?: string
  exportType?: "latent" | "confirmed"
}) {
  const query: Record<string, string> = {}
  if (params?.ids?.length) query.ids = params.ids.join(",")
  if (params?.name) query.name = params.name
  if (params?.idNumber) query.idNumber = params.idNumber
  if (params?.district) query.district = params.district
  if (params?.phone) query.phone = params.phone
  if (params?.creatorUsername) query.creatorUsername = params.creatorUsername
  if (params?.diagnosisResult) query.diagnosisResult = params.diagnosisResult
  if (params?.exportType) query.exportType = params.exportType
  return request<Blob>({
    url: "close-contact/case/export",
    method: "get",
    params: query,
    responseType: "blob"
  })
}

/** 新增密接个案 */
export function createCloseContactCaseApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "close-contact/case/create",
    method: "post",
    data
  })
}

/** 更新密接个案 */
export function updateCloseContactCaseApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `close-contact/case/update/${id}`,
    method: "put",
    data
  })
}

/** 删除密接个案 */
export function deleteCloseContactCaseApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `close-contact/case/delete/${id}`,
    method: "delete"
  })
}

/** 批量删除密接个案 */
export function batchDeleteCloseContactCaseApi(ids: number[]) {
  return request<ApiResponseData<null>>({
    url: "close-contact/case/batch-delete",
    method: "delete",
    data: ids
  })
}

/** 按 ID 查询密接个案详情 */
export function getCloseContactCaseDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `close-contact/case/${id}`,
    method: "get"
  })
}
