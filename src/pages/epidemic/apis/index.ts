import { request } from "@/http/axios"

interface EpidemicImportResult {
  count: number
  batchNo: string
}

/** 上传大疫情表 Excel */
export function importEpidemicDataApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<EpidemicImportResult>>({
    url: "epidemic/import",
    method: "post",
    data: formData
  })
}

/** 分页查询大疫情待诊断列表 */
export function getEpidemicListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({
    url: "epidemic/list",
    method: "get",
    params
  })
}

/** 追踪操作 */
export function trackEpidemicApi(data: { id: number, status: number, remark?: string }) {
  return request<ApiResponseData<null>>({
    url: "epidemic/track",
    method: "post",
    data
  })
}

/** 录入胸片结果 */
export function submitEpidemicXrayApi(data: {
  id: number
  hasChestXray: string
  chestXrayDate?: string
  chestXrayResult?: string
}) {
  return request<ApiResponseData<null>>({
    url: "epidemic/xray",
    method: "post",
    data
  })
}

/** 录入诊断并触发自动分流 */
export function submitEpidemicDiagnosisApi(data: { id: number, diagnosisResult: string }) {
  return request<ApiResponseData<null>>({
    url: "epidemic/diagnosis",
    method: "post",
    data
  })
}
