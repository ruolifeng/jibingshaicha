import { request } from "@/http/axios"

/** 上传疫情筛查 Excel（复用重点人群接口，sourceType=regular） */
export function uploadScreeningRegularApi(file: File, confirmSkipInvalid = false) {
  const formData = new FormData()
  formData.append("file", file)
  formData.append("sourceType", "regular")
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  return request<ApiResponseData<{ successCount: number, invalidIdentityCount?: number, requireIdentityConfirm?: boolean, errors: string[] }>>({
    url: "screening/key-population/upload",
    method: "post",
    data: formData,
    params: { confirmSkipInvalid },
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 导出疫情筛查数据 */
export function exportScreeningRegularApi(ids?: number[]) {
  return request<Blob>({
    url: "screening/key-population/export",
    method: "get",
    params: ids && ids.length > 0 ? { ids: ids.join(","), sourceType: "regular" } : { sourceType: "regular" },
    responseType: "blob"
  })
}

/** 更新疫情筛查记录 */
export function updateScreeningRegularApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `screening/key-population/update/${id}`,
    method: "put",
    data: { ...data, sourceType: "regular" }
  })
}

/** 新增疫情筛查记录 */
export function createScreeningRegularApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "screening/key-population/create",
    method: "post",
    data: { ...data, sourceType: "regular" }
  })
}

/** 删除疫情筛查记录（级联删除后续所有关联数据） */
export function deleteScreeningRegularApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `screening/key-population/delete/${id}`,
    method: "delete"
  })
}

/** 批量删除疫情筛查记录 */
export function batchDeleteScreeningRegularApi(ids: number[]) {
  return request<ApiResponseData<null>>({
    url: "screening/key-population/batch-delete",
    method: "delete",
    data: ids
  })
}

/** 按 ID 查询疫情筛查记录详情 */
export function getScreeningRegularDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `screening/key-population/${id}`,
    method: "get"
  })
}

/** 分页查询疫情筛查数据 */
export function getScreeningRegularListApi(params: {
  page: number
  size: number
  name?: string
  idNumber?: string
  phone?: string
  district?: string
  townshipCommunity?: string
  crowdCategory?: string // 支持逗号分隔多选，如「老年人,糖尿病」
  screenMethod?: string
  isLatent?: number
  diagnosisFirst?: string
  entryUnit?: string
  creatorUsername?: string
  columnFilters?: string
  dateFrom?: string
  dateTo?: string
  createTimeFrom?: string
  createTimeTo?: string
}) {
  return request<ApiResponseData<any>>({
    url: "screening/key-population/list",
    method: "get",
    params: { ...params, sourceType: "regular" }
  })
}
