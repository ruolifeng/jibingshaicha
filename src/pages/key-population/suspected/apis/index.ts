import { request } from "@/http/axios"

/** 分页查询疑似结核数据 */
export function getSuspectedListApi(params: {
  page: number
  size: number
  populationType: string
  name?: string
  idNumber?: string
  trackingStatus?: number
  archived?: number
}) {
  return request<ApiResponseData<any>>({
    url: "latent/list",
    method: "get",
    params
  })
}

/** 追踪操作 */
export function trackSuspectedApi(data: { id: number, status: number, remark?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/track",
    method: "post",
    data
  })
}

/** 转诊操作 */
export function referralSuspectedApi(data: { id: number, result: string, remark?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/referral",
    method: "post",
    data
  })
}

/** 手动录入胸片检查与首次诊断结果（追踪到位后） */
export function submitXrayApi(data: {
  id: number
  hasChestXray: string
  chestXrayDate?: string
  chestXrayResult?: string
  diagnosisFirst: string
}) {
  return request<ApiResponseData<null>>({
    url: "latent/xray",
    method: "post",
    data
  })
}

/** 批量导入胸片+诊断 Excel */
export function importXrayApi(file: File, populationType: string) {
  const formData = new FormData()
  formData.append("file", file)
  formData.append("populationType", populationType)
  return request<ApiResponseData<number>>({
    url: "latent/xray/import",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}
