import { request } from "@/http/axios"

export interface SmsConfig {
  id?: string
  enabled: boolean
  secretId: string
  secretKeyMasked: string
  secretKeyConfigured: boolean
  sdkAppId: string
  signName: string
  templateId: string
  region: string
}

export function getSmsConfigApi() {
  return request<ApiResponseData<SmsConfig>>({
    url: "sms-config",
    method: "get"
  })
}

export function saveSmsConfigApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "sms-config",
    method: "put",
    data
  })
}

export function testSmsConfigApi(data: { phone?: string, message?: string }) {
  return request<ApiResponseData<string>>({
    url: "sms-config/test",
    method: "post",
    data
  })
}
