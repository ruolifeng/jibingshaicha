import { request } from "@/http/axios"

/** 上传学校人群筛查 Excel */
export function uploadScreeningSchoolApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{ successCount: number, errors: string[] }>>({
    url: "screening/school/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 导出学校人群筛查数据（可按勾选ID导出） */
export function exportScreeningSchoolApi(ids?: number[]) {
  return request<Blob>({
    url: "screening/school/export",
    method: "get",
    params: ids && ids.length > 0 ? { ids: ids.join(",") } : undefined,
    responseType: "blob"
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
    data: ids
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
  name?: string
  idNumber?: string
  schoolName?: string
  district?: string
  isLatent?: number
  diagnosisFirst?: string
  phone?: string
  entryUnit?: string
  dateFrom?: string
  dateTo?: string
}) {
  return request<ApiResponseData<any>>({
    url: "screening/school/list",
    method: "get",
    params
  })
}
