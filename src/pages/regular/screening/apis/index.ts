import { request } from "@/http/axios"

/** 上传常规筛查 Excel（复用重点人群接口，sourceType=regular） */
export function uploadScreeningRegularApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  formData.append("sourceType", "regular")
  return request<ApiResponseData<{ successCount: number, errors: string[] }>>({
    url: "screening/key-population/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 导出常规筛查数据 */
export function exportScreeningRegularApi(ids?: number[]) {
  return request<Blob>({
    url: "screening/key-population/export",
    method: "get",
    params: ids && ids.length > 0 ? { ids: ids.join(","), sourceType: "regular" } : { sourceType: "regular" },
    responseType: "blob"
  })
}

/** 更新常规筛查记录 */
export function updateScreeningRegularApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `screening/key-population/update/${id}`,
    method: "put",
    data: { ...data, sourceType: "regular" }
  })
}

/** 新增常规筛查记录 */
export function createScreeningRegularApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "screening/key-population/create",
    method: "post",
    data: { ...data, sourceType: "regular" }
  })
}

/** 删除常规筛查记录（级联删除后续所有关联数据） */
export function deleteScreeningRegularApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `screening/key-population/delete/${id}`,
    method: "delete"
  })
}

/** 批量删除常规筛查记录 */
export function batchDeleteScreeningRegularApi(ids: number[]) {
  return request<ApiResponseData<null>>({
    url: "screening/key-population/batch-delete",
    method: "delete",
    data: ids
  })
}

/** 按 ID 查询常规筛查记录详情 */
export function getScreeningRegularDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `screening/key-population/${id}`,
    method: "get"
  })
}

/** 分页查询常规筛查数据 */
export function getScreeningRegularListApi(params: {
  page: number
  size: number
  name?: string
  idNumber?: string
  phone?: string
  district?: string
  townshipCommunity?: string
  crowdCategory?: string
  screenMethod?: string
  isLatent?: number
  diagnosisFirst?: string
}) {
  return request<ApiResponseData<any>>({
    url: "screening/key-population/list",
    method: "get",
    params: { ...params, sourceType: "regular" }
  })
}
