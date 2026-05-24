import { request } from "@/http/axios"

/** 上传重点人群筛查 Excel */
export function uploadScreeningKeyPopulationApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{ successCount: number, errors: string[] }>>({
    url: "screening/key-population/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 导出重点人群筛查数据（可按勾选ID导出） */
export function exportScreeningKeyPopulationApi(ids?: number[]) {
  return request<Blob>({
    url: "screening/key-population/export",
    method: "get",
    params: ids && ids.length > 0 ? { ids: ids.join(",") } : undefined,
    responseType: "blob"
  })
}

/** 更新重点人群筛查记录 */
export function updateScreeningKeyPopulationApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `screening/key-population/update/${id}`,
    method: "put",
    data
  })
}

/** 新增重点人群筛查记录 */
export function createScreeningKeyPopulationApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "screening/key-population/create",
    method: "post",
    data
  })
}

/** 删除重点人群筛查记录（级联删除后续所有关联数据） */
export function deleteScreeningKeyPopulationApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `screening/key-population/delete/${id}`,
    method: "delete"
  })
}

/** 批量删除重点人群筛查记录（级联删除） */
export function batchDeleteScreeningKeyPopulationApi(ids: number[]) {
  return request<ApiResponseData<null>>({
    url: "screening/key-population/batch-delete",
    method: "delete",
    data: ids
  })
}

/** 按 ID 查询重点人群筛查记录详情 */
export function getScreeningKeyPopulationDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `screening/key-population/${id}`,
    method: "get"
  })
}

/** 分页查询重点人群筛查数据 */
export function getScreeningKeyPopulationListApi(params: {
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
  dateFrom?: string
  dateTo?: string
}) {
  return request<ApiResponseData<any>>({
    url: "screening/key-population/list",
    method: "get",
    params
  })
}
