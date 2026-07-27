import type { ImportConfirmOptions, ImportResultData } from "@@/composables/useImportIdentityConfirm"
import { request } from "@/http/axios"

/** 上传密接人群筛查 Excel（72列官方模板） */
export function uploadScreeningCloseContactApi(file: File, options: ImportConfirmOptions = {}) {
  const confirmSkipInvalid = options.confirmSkipInvalid ?? false
  const confirmSkipDuplicateInFile = options.confirmSkipDuplicateInFile ?? false
  const formData = new FormData()
  formData.append("file", file)
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  formData.append("confirmSkipDuplicateInFile", String(confirmSkipDuplicateInFile))
  return request<ApiResponseData<ImportResultData>>({
    url: "screening/close-contact/upload",
    method: "post",
    data: formData,
    params: { confirmSkipInvalid, confirmSkipDuplicateInFile },
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 表头 Excel 式筛选：某列实际出现过的去重值 */
export function getScreeningCloseContactColumnDistinctApi(field: string) {
  return request<ApiResponseData<string[]>>({
    url: "screening/close-contact/column-distinct",
    method: "get",
    params: { field }
  })
}

/** 分页查询密接人群筛查数据 */
export function getScreeningCloseContactListApi(params: {
  page: number
  size: number
  name?: string
  idNumber?: string
  district?: string
  phone?: string
  dateFrom?: string
  dateTo?: string
  createTimeFrom?: string
  createTimeTo?: string
  creatorUsername?: string
  columnFilters?: string
  ccStatus?: number
  finalScreeningResult?: string
}) {
  return request<ApiResponseData<any>>({
    url: "screening/close-contact/list",
    method: "get",
    params
  })
}

/** 各最终筛查结果分类统计 */
export function countByResultApi() {
  return request<ApiResponseData<Record<string, number>>>({
    url: "screening/close-contact/count-by-result",
    method: "get"
  })
}

/** 密接人群筛查列表/导出/按筛选删除共用查询参数 */
export interface ScreeningCloseContactQueryParams {
  name?: string
  idNumber?: string
  district?: string
  phone?: string
  dateFrom?: string
  dateTo?: string
  createTimeFrom?: string
  createTimeTo?: string
  creatorUsername?: string
  columnFilters?: string
  ccStatus?: number
  finalScreeningResult?: string
  formatIssue?: string
}

/** 导出密接人群筛查 Excel（支持勾选 ID / 筛选条件 / 全部） */
export function exportScreeningCloseContactApi(params?: ScreeningCloseContactQueryParams & { ids?: number[] }) {
  const { ids, ...rest } = params ?? {}
  return request<Blob>({
    url: "screening/close-contact/export",
    method: "get",
    params: {
      ...rest,
      ...(ids && ids.length > 0 ? { ids: ids.join(",") } : {})
    },
    responseType: "blob",
    timeout: 120000
  })
}

/** 新增密接人群筛查记录 */
export function createScreeningCloseContactApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "screening/close-contact/create",
    method: "post",
    data
  })
}

/** 更新密接人群筛查记录 */
export function updateScreeningCloseContactApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `screening/close-contact/update/${id}`,
    method: "put",
    data
  })
}

/** 删除密接人群筛查记录（级联删除） */
export function deleteScreeningCloseContactApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `screening/close-contact/delete/${id}`,
    method: "delete"
  })
}

/** 批量删除密接人群筛查记录（级联删除） */
export function batchDeleteScreeningCloseContactApi(ids: number[]) {
  return request<ApiResponseData<null>>({
    url: "screening/close-contact/batch-delete",
    method: "delete",
    data: ids,
    timeout: 120000
  })
}

/** 按当前筛选条件删除密接人群筛查记录（级联删除） */
export function deleteScreeningCloseContactByFilterApi(params?: ScreeningCloseContactQueryParams) {
  return request<ApiResponseData<number>>({
    url: "screening/close-contact/delete-by-filter",
    method: "delete",
    params,
    timeout: 300000
  })
}

/** 删除权限范围内全部密接人群筛查记录（级联删除） */
export function deleteAllScreeningCloseContactApi() {
  return request<ApiResponseData<number>>({
    url: "screening/close-contact/delete-all",
    method: "delete",
    timeout: 300000
  })
}

/** 按 ID 查询密接人群筛查记录详情 */
export function getScreeningCloseContactDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `screening/close-contact/${id}`,
    method: "get"
  })
}

/** 设置预计完成治疗时间 */
export function setExpectedEndDateApi(id: number, expectedDate: string) {
  return request<ApiResponseData<null>>({
    url: `screening/close-contact/${id}/expected-end-date`,
    method: "post",
    params: { expectedDate }
  })
}

/** 确认治疗是否完成 */
export function confirmTreatmentApi(id: number, done: boolean) {
  return request<ApiResponseData<null>>({
    url: `screening/close-contact/${id}/confirm-treatment`,
    method: "post",
    params: { done }
  })
}

/** 提交3月复查结果（未发现异常流程） */
export function submitThreeMonthCheckApi(id: number, data: {
  checkDate: string
  checkResult: string
  finalResult: string
}) {
  return request<ApiResponseData<null>>({
    url: `screening/close-contact/${id}/three-month-check`,
    method: "post",
    params: data
  })
}
