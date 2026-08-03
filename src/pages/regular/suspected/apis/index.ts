import { request } from "@/http/axios"

/** 分页查询疑似结核数据 */
export function getSuspectedListApi(params: {
  page: number
  size: number
  populationType: string
  name?: string
  idNumber?: string
  phone?: string
  dateFrom?: string
  dateTo?: string
  trackingStatus?: number
  archived?: number
  referralResult?: string
  diagnosisFirst?: string
}) {
  return request<ApiResponseData<any>>({
    url: "latent/list",
    method: "get",
    params
  })
}

/** 追踪操作 */
export function trackSuspectedApi(data: { id: string, status: number, remark?: string, actualArrivalDate?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/track",
    method: "post",
    data
  })
}

/** 转诊操作 */
export function referralSuspectedApi(data: { id: string, result: string, remark?: string, actualReferralDate?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/referral",
    method: "post",
    data
  })
}

/**
 * 录入胸片检查结果（V13 拆分：仅胸片字段）
 */
export function submitXrayOnlyApi(data: {
  id: string
  hasChestXray: string
  chestXrayDate?: string
  chestXrayResult?: string
}) {
  return request<ApiResponseData<null>>({
    url: "latent/xray-only",
    method: "post",
    data
  })
}

/**
 * 录入首次诊断结果（V13 拆分：仅诊断字段；提交后自动驱动转诊）
 */
export function submitDiagnosisApi(data: {
  id: string
  diagnosisFirst: string
}) {
  return request<ApiResponseData<null>>({
    url: "latent/diagnosis",
    method: "post",
    data
  })
}

/**
 * @deprecated V13 起请改用 {@link submitXrayOnlyApi} + {@link submitDiagnosisApi}
 */
export function submitXrayApi(data: {
  id: string
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
