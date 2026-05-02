import { request } from "@/http/axios"

export interface SentReferralVO {
  id: number
  bizType: string
  populationType: string
  moduleType: string
  subjectName: string
  senderId: number
  senderName: string
  senderOrgName: string
  receiverOrgId: number
  receiverName: string
  receiverOrgName: string
  /** 1=待确认  2=已接收  3=已拒绝 */
  status: number
  sentTime: string
  confirmedTime: string | null
  rejectedTime: string | null
  rejectReason: string | null
}

export interface ReferralRecord {
  id: number
  bizId: number
  bizType: string
  populationType: string
  moduleType: string
  subjectName: string
  status: number
  sentTime: string
  confirmedTime: string | null
  rejectedTime: string | null
  rejectReason: string | null
}

export interface SendReferralParams {
  bizId: number
  bizType: string
  populationType: string
  moduleType: string
  subjectName: string
  /** 业务摘要 JSON 字符串 */
  summary?: string
  receiverOrgId: number
}

/** 发起分级诊疗推送 */
export function sendReferralApi(data: SendReferralParams) {
  return request<ApiResponseData<null>>({
    url: "referral/send",
    method: "post",
    data
  })
}

/** 接收方确认接收 */
export function confirmReferralApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `referral/confirm/${id}`,
    method: "post"
  })
}

/** 接收方拒绝 */
export function rejectReferralApi(id: number, rejectReason?: string) {
  return request<ApiResponseData<null>>({
    url: `referral/reject/${id}`,
    method: "post",
    data: { rejectReason }
  })
}

/** 发送方重新发起（仅拒绝后可用） */
export function resendReferralApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `referral/resend/${id}`,
    method: "post"
  })
}

/** 查询某条业务关联的分级诊疗列表 */
export function getReferralListApi(bizId: number, bizType: string) {
  return request<ApiResponseData<ReferralRecord[]>>({
    url: "referral/list",
    method: "get",
    params: { bizId, bizType }
  })
}

/** 当前用户已发送的分级诊疗分页列表 */
export function getSentReferralListApi(params: { pageNum: number; size: number }) {
  return request<ApiResponseData<{ records: SentReferralVO[]; total: number }>>({
    url: "referral/sent",
    method: "get",
    params
  })
}
