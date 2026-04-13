import { request } from "@/http/axios"

/** 上传密接人群筛查 Excel */
export function uploadScreeningCloseContactApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<number>>({
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
  isLatent?: number
}) {
  return request<ApiResponseData<any>>({
    url: "screening/close-contact/list",
    method: "get",
    params
  })
}
