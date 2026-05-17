import { request } from "@/http/axios"

export interface DataCleaningResult {
  totalCount: number
  abnormalCount: number
  fileId: string
  fileName: string
  errors: string[]
}

/** 上传筛查数据执行清洗 */
export function cleanScreeningDataApi(populationType: "school" | "keyPopulation" | "closeContact", file: File) {
  const formData = new FormData()
  formData.append("populationType", populationType)
  formData.append("file", file)
  return request<ApiResponseData<DataCleaningResult>>({
    url: "data-cleaning/clean",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 120000
  })
}

/** 下载清洗后的结果文件 */
export function downloadCleaningResultApi(fileId: string) {
  return request<Blob>({
    url: `data-cleaning/download/${fileId}`,
    method: "get",
    responseType: "blob",
    timeout: 120000
  })
}
