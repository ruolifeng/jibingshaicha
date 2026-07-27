import type { ImportConfirmOptions, ImportResultData } from "@@/composables/useImportIdentityConfirm"
import { request } from "@/http/axios"

/** 下载密接个案表导入模板（72 列官方表头） */
export function downloadCloseContactCaseTemplateApi() {
  return request<Blob>({
    url: "template/download",
    method: "get",
    params: { type: "closeContactCase" },
    responseType: "blob"
  })
}

/** 上传密接个案表 Excel（72列官方模板） */
export function uploadCloseContactCaseApi(file: File, options: ImportConfirmOptions = {}) {
  const confirmSkipInvalid = options.confirmSkipInvalid ?? false
  const confirmSkipDuplicateInFile = options.confirmSkipDuplicateInFile ?? false
  const formData = new FormData()
  formData.append("file", file)
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  formData.append("confirmSkipDuplicateInFile", String(confirmSkipDuplicateInFile))
  return request<ApiResponseData<ImportResultData>>({
    url: "close-contact/case/upload",
    method: "post",
    data: formData,
    params: { confirmSkipInvalid, confirmSkipDuplicateInFile },
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 表头 Excel 式筛选：某列实际出现过的去重值 */
export function getCloseContactCaseColumnDistinctApi(field: string) {
  return request<ApiResponseData<string[]>>({
    url: "close-contact/case/column-distinct",
    method: "get",
    params: { field }
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
  columnFilters?: string
  diagnosisResult?: string
  sourcePatientBacteriologyResult?: string
  reportQuarter?: string
  createTimeFrom?: string
  createTimeTo?: string
  formatIssue?: string
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
  sourcePatientBacteriologyResult?: string
  reportQuarter?: string
  createTimeFrom?: string
  createTimeTo?: string
  columnFilters?: string
  exportType?: "latent" | "confirmed"
  formatIssue?: string
}) {
  const query: Record<string, string> = {}
  if (params?.ids?.length) query.ids = params.ids.join(",")
  if (params?.name) query.name = params.name
  if (params?.idNumber) query.idNumber = params.idNumber
  if (params?.district) query.district = params.district
  if (params?.phone) query.phone = params.phone
  if (params?.creatorUsername) query.creatorUsername = params.creatorUsername
  if (params?.diagnosisResult) query.diagnosisResult = params.diagnosisResult
  if (params?.sourcePatientBacteriologyResult) {
    query.sourcePatientBacteriologyResult = params.sourcePatientBacteriologyResult
  }
  if (params?.reportQuarter) query.reportQuarter = params.reportQuarter
  if (params?.createTimeFrom) query.createTimeFrom = params.createTimeFrom
  if (params?.createTimeTo) query.createTimeTo = params.createTimeTo
  if (params?.columnFilters) query.columnFilters = params.columnFilters
  if (params?.exportType) query.exportType = params.exportType
  if (params?.formatIssue) query.formatIssue = params.formatIssue
  return request<Blob>({
    url: "close-contact/case/export",
    method: "get",
    params: query,
    responseType: "blob",
    timeout: 120000
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
    data: ids,
    timeout: 120000
  })
}

/** 按当前筛选条件删除密接个案 */
export function deleteCloseContactCaseByFilterApi(params?: {
  name?: string
  idNumber?: string
  district?: string
  phone?: string
  creatorUsername?: string
  diagnosisResult?: string
  sourcePatientBacteriologyResult?: string
  reportQuarter?: string
  createTimeFrom?: string
  createTimeTo?: string
  columnFilters?: string
  formatIssue?: string
}) {
  return request<ApiResponseData<number>>({
    url: "close-contact/case/delete-by-filter",
    method: "delete",
    params,
    timeout: 300000
  })
}

/** 删除权限范围内全部密接个案 */
export function deleteAllCloseContactCaseApi() {
  return request<ApiResponseData<number>>({
    url: "close-contact/case/delete-all",
    method: "delete",
    timeout: 300000
  })
}

/** 按 ID 查询密接个案详情 */
export function getCloseContactCaseDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `close-contact/case/${id}`,
    method: "get"
  })
}
