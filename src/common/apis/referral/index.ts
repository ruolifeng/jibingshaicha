import { request } from "@/http/axios"

export interface SentReferralVO {
  id: string
  bizType: string
  populationType: string
  moduleType: string
  subjectName: string
  senderId: string
  senderName: string
  senderOrgName: string
  receiverOrgId: string
  receiverName: string
  receiverOrgName: string
  /** 1=待确认  2=已接收  3=已拒绝 */
  status: number
  sentTime: string
  confirmedTime: string | null
  actualReferralDate?: string | null
  rejectedTime: string | null
  rejectReason: string | null
  referralReason: string | null
}

export interface ReferralRecord {
  id: string
  bizId: string
  bizType: string
  populationType: string
  moduleType: string
  subjectName: string
  status: number
  sentTime: string
  confirmedTime: string | null
  actualReferralDate?: string | null
  rejectedTime: string | null
  rejectReason: string | null
  referralReason: string | null
}

export interface SendReferralParams {
  bizId: string
  bizType: string
  populationType: string
  moduleType: string
  subjectName: string
  /** 业务摘要 JSON 字符串 */
  summary?: string
  /** 转诊原因 */
  referralReason?: string
  receiverOrgId: string
}

/** 发起转诊推送 */
export function sendReferralApi(data: SendReferralParams) {
  return request<ApiResponseData<null>>({
    url: "referral/send",
    method: "post",
    data
  })
}

/** 接收方确认接收 */
export function confirmReferralApi(id: string, actualReferralDate?: string) {
  return request<ApiResponseData<null>>({
    url: `referral/confirm/${id}`,
    method: "post",
    data: actualReferralDate ? { actualReferralDate } : undefined
  })
}

/** 接收方拒绝 */
export function rejectReferralApi(id: string, rejectReason?: string) {
  return request<ApiResponseData<null>>({
    url: `referral/reject/${id}`,
    method: "post",
    data: { rejectReason }
  })
}

/** 发送方重新发起（仅拒绝后可用） */
export function resendReferralApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `referral/resend/${id}`,
    method: "post"
  })
}

/** 查询某条业务关联的转诊列表 */
export function getReferralListApi(bizId: string, bizType: string) {
  return request<ApiResponseData<ReferralRecord[]>>({
    url: "referral/list",
    method: "get",
    params: { bizId, bizType }
  })
}

/** 已发送的转诊分页列表（五级仅本人发送，上级按辖区） */
export function getSentReferralListApi(params: { pageNum: number, size: number }) {
  return request<ApiResponseData<{ records: SentReferralVO[], total: number }>>({
    url: "referral/sent",
    method: "get",
    params
  })
}
