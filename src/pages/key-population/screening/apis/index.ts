import type { ImportConfirmOptions, ImportResultData } from "@@/composables/useImportIdentityConfirm"
import { request } from "@/http/axios"

export interface ScreeningKeyPopulationImportPreview {
  duplicateCount: number
  newCount: number
  duplicates: Array<{ name: string, idNumber: string }>
}

export interface ScreeningKeyPopulationImportResult extends ImportResultData {}

/** 预览重点人群筛查 Excel 导入（检测与系统重复人员） */
export function previewScreeningKeyPopulationUploadApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<ScreeningKeyPopulationImportPreview>>({
    url: "screening/key-population/upload/preview",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 上传重点人群筛查 Excel */
export function uploadScreeningKeyPopulationApi(file: File, overwrite = true, options: ImportConfirmOptions = {}) {
  const confirmSkipInvalid = options.confirmSkipInvalid ?? false
  const confirmSkipDuplicateInFile = options.confirmSkipDuplicateInFile ?? false
  const formData = new FormData()
  formData.append("file", file)
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  formData.append("confirmSkipDuplicateInFile", String(confirmSkipDuplicateInFile))
  return request<ApiResponseData<ScreeningKeyPopulationImportResult>>({
    url: "screening/key-population/upload",
    method: "post",
    data: formData,
    params: { overwrite, confirmSkipInvalid, confirmSkipDuplicateInFile },
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 重点人群/疫情筛查列表与导出共用查询参数 */
export interface ScreeningKeyPopulationQueryParams {
  name?: string
  idNumber?: string
  phone?: string
  district?: string
  townshipCommunity?: string
  crowdCategory?: string
  screenMethod?: string
  isLatent?: number
  diagnosisFirst?: string
  entryUnit?: string
  creatorUsername?: string
  hasChestXray?: string
  chestXrayResult?: string
  columnFilters?: string
  dateFrom?: string
  dateTo?: string
  createTimeFrom?: string
  createTimeTo?: string
  sortField?: string
  sortOrder?: string
  sourceType?: string
  formatIssue?: string
}

/** 导出重点人群筛查数据（可按勾选 ID 或当前筛选条件导出；空参数=导出全部） */
export function exportScreeningKeyPopulationApi(params?: ScreeningKeyPopulationQueryParams & { ids?: number[] }) {
  const { ids, ...rest } = params ?? {}
  return request<Blob>({
    url: "screening/key-population/export",
    method: "get",
    params: {
      ...rest,
      ...(ids && ids.length > 0 ? { ids: ids.join(",") } : {})
    },
    responseType: "blob",
    timeout: 120000
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
    data: ids,
    timeout: 120000
  })
}

/** 按当前筛选条件删除重点人群筛查记录 */
export function deleteScreeningKeyPopulationByFilterApi(params?: ScreeningKeyPopulationQueryParams) {
  return request<ApiResponseData<number>>({
    url: "screening/key-population/delete-by-filter",
    method: "delete",
    params,
    timeout: 300000
  })
}

/** 删除权限范围内全部重点人群筛查记录 */
export function deleteAllScreeningKeyPopulationApi() {
  return request<ApiResponseData<number>>({
    url: "screening/key-population/delete-all",
    method: "delete",
    timeout: 300000
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
} & ScreeningKeyPopulationQueryParams) {
  return request<ApiResponseData<any>>({
    url: "screening/key-population/list",
    method: "get",
    params
  })
}
