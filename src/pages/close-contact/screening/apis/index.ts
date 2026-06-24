import { request } from "@/http/axios"

/** 上传密接人群筛查 Excel（71列官方模板） */
export function uploadScreeningCloseContactApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{ successCount: number, errors: string[] }>>({
    url: "screening/close-contact/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
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

/** 导出密接人群筛查 Excel（71列官方模板，可再导入） */
export function exportScreeningCloseContactApi(ids?: number[]) {
  return request<Blob>({
    url: "screening/close-contact/export",
    method: "get",
    params: ids && ids.length > 0 ? { ids: ids.join(",") } : undefined,
    responseType: "blob"
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
    data: ids
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
