import type { ImportConfirmOptions, ImportResultData } from "@@/composables/useImportIdentityConfirm"
import type { ScreeningKeyPopulationQueryParams } from "@/pages/key-population/screening/apis"
import { request } from "@/http/axios"

/** 上传疫情筛查 Excel（复用重点人群接口，sourceType=regular） */
export function uploadScreeningRegularApi(file: File, options: ImportConfirmOptions = {}) {
  const confirmSkipInvalid = options.confirmSkipInvalid ?? false
  const confirmSkipDuplicateInFile = options.confirmSkipDuplicateInFile ?? false
  const formData = new FormData()
  formData.append("file", file)
  formData.append("sourceType", "regular")
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  formData.append("confirmSkipDuplicateInFile", String(confirmSkipDuplicateInFile))
  return request<ApiResponseData<ImportResultData>>({
    url: "screening/key-population/upload",
    method: "post",
    data: formData,
    params: { confirmSkipInvalid, confirmSkipDuplicateInFile },
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 导出疫情筛查数据（可按勾选 ID 或当前筛选条件导出；空参数=导出全部） */
export function exportScreeningRegularApi(params?: ScreeningKeyPopulationQueryParams & { ids?: number[] }) {
  const { ids, ...rest } = params ?? {}
  return request<Blob>({
    url: "screening/key-population/export",
    method: "get",
    params: {
      sourceType: "regular",
      ...rest,
      ...(ids && ids.length > 0 ? { ids: ids.join(",") } : {})
    },
    responseType: "blob",
    timeout: 120000
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
    data: ids,
    timeout: 120000
  })
}

/** 按当前筛选条件删除疫情筛查记录 */
export function deleteScreeningRegularByFilterApi(params?: ScreeningKeyPopulationQueryParams) {
  return request<ApiResponseData<number>>({
    url: "screening/key-population/delete-by-filter",
    method: "delete",
    params: { ...params, sourceType: "regular" },
    timeout: 300000
  })
}

/** 删除权限范围内全部疫情筛查记录 */
export function deleteAllScreeningRegularApi() {
  return request<ApiResponseData<number>>({
    url: "screening/key-population/delete-all",
    method: "delete",
    params: { sourceType: "regular" },
    timeout: 300000
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
} & ScreeningKeyPopulationQueryParams) {
  return request<ApiResponseData<any>>({
    url: "screening/key-population/list",
    method: "get",
    params: { ...params, sourceType: "regular" }
  })
}
