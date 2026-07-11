import type { ImportConfirmOptions, ImportResultData } from "@@/composables/useImportIdentityConfirm"
import { request } from "@/http/axios"

/** 上传学校人群筛查 Excel */
export function uploadScreeningSchoolApi(file: File, options: ImportConfirmOptions = {}) {
  const confirmSkipInvalid = options.confirmSkipInvalid ?? false
  const confirmSkipDuplicateInFile = options.confirmSkipDuplicateInFile ?? false
  const formData = new FormData()
  formData.append("file", file)
  formData.append("confirmSkipInvalid", String(confirmSkipInvalid))
  formData.append("confirmSkipDuplicateInFile", String(confirmSkipDuplicateInFile))
  return request<ApiResponseData<ImportResultData>>({
    url: "screening/school/upload",
    method: "post",
    data: formData,
    params: { confirmSkipInvalid, confirmSkipDuplicateInFile },
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 学校人群筛查列表与导出共用查询参数 */
export interface ScreeningSchoolQueryParams {
  name?: string
  idNumber?: string
  schoolName?: string
  district?: string
  isLatent?: number
  diagnosisFirst?: string
  phone?: string
  entryUnit?: string
  creatorUsername?: string
  columnFilters?: string
  year?: string
  createTimeFrom?: string
  createTimeTo?: string
  sortField?: string
  sortOrder?: string
}

/** 导出学校人群筛查数据（可按勾选 ID 或当前筛选条件导出） */
export function exportScreeningSchoolApi(params?: ScreeningSchoolQueryParams & { ids?: number[] }) {
  const { ids, ...rest } = params ?? {}
  return request<Blob>({
    url: "screening/school/export",
    method: "get",
    params: {
      ...rest,
      ...(ids && ids.length > 0 ? { ids: ids.join(",") } : {})
    },
    responseType: "blob",
    timeout: 120000
  })
}

/** 更新学校人群筛查记录 */
export function updateScreeningSchoolApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `screening/school/update/${id}`,
    method: "put",
    data
  })
}

/** 新增学校人群筛查记录 */
export function createScreeningSchoolApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "screening/school/create",
    method: "post",
    data
  })
}

/** 删除学校人群筛查记录（级联删除后续所有关联数据） */
export function deleteScreeningSchoolApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `screening/school/delete/${id}`,
    method: "delete"
  })
}

/** 批量删除学校人群筛查记录（级联删除） */
export function batchDeleteScreeningSchoolApi(ids: number[]) {
  return request<ApiResponseData<null>>({
    url: "screening/school/batch-delete",
    method: "delete",
    data: ids,
    timeout: 120000
  })
}

/** 按 ID 查询学校人群筛查记录详情 */
export function getScreeningSchoolDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `screening/school/${id}`,
    method: "get"
  })
}

/** 分页查询学校人群筛查数据 */
export function getScreeningSchoolListApi(params: {
  page: number
  size: number
} & ScreeningSchoolQueryParams) {
  return request<ApiResponseData<any>>({
    url: "screening/school/list",
    method: "get",
    params
  })
}
