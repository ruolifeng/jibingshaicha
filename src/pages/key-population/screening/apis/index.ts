import { request } from "@/http/axios"

/** 上传重点人群筛查 Excel */
export function uploadScreeningKeyPopulationApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<number>>({
    url: "screening/key-population/upload",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 导出重点人群筛查数据 */
export function exportScreeningKeyPopulationApi() {
  return request<Blob>({
    url: "screening/key-population/export",
    method: "get",
    responseType: "blob"
  })
}

/** 分页查询重点人群筛查数据 */
export function getScreeningKeyPopulationListApi(params: {
  page: number
  size: number
  name?: string
  idNumber?: string
  district?: string
  isLatent?: number
}) {
  return request<ApiResponseData<any>>({
    url: "screening/key-population/list",
    method: "get",
    params
  })
}
