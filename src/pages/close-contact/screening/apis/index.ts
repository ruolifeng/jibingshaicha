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

/** 导出密接人群筛查数据 */
export function exportScreeningCloseContactApi() {
  return request<Blob>({
    url: "screening/close-contact/export",
    method: "get",
    responseType: "blob"
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

/** 删除密接人群筛查记录（级联删除后续所有关联数据） */
export function deleteScreeningCloseContactApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `screening/close-contact/delete/${id}`,
    method: "delete"
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
