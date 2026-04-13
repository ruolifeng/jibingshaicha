import { request } from "@/http/axios"

/** 上传学校人群筛查 Excel */
export function uploadScreeningSchoolApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<number>>({
    url: "screening/school/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
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
}) {
  return request<ApiResponseData<any>>({
    url: "screening/school/list",
    method: "get",
    params
  })
}
