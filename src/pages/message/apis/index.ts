import { request } from "@/http/axios"

/** 消息列表 */
export function getMessageListApi(params: { page: number, size: number, isRead?: number }) {
  return request<ApiResponseData<any>>({
    url: "message/list",
    method: "get",
    params
  })
}

/** 标记已读 */
export function markMessageReadApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `message/read/${id}`,
    method: "post"
  })
}

/** 在消息页确认接收通知单 */
export function confirmNoticeFromMessageApi(noticeId: string) {
  return request<ApiResponseData<null>>({
    url: `notice/confirm/${noticeId}`,
    method: "post"
  })
}

/** 未读消息数 */
export function getUnreadCountApi() {
  return request<ApiResponseData<number>>({
    url: "message/unread-count",
    method: "get"
  })
}

/** 删除消息 */
export function deleteMessageApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `message/${id}`,
    method: "delete"
  })
}

export interface SentNoticeVO {
  id: string
  noticeType: string
  populationType: string
  patientName: string
  senderId: string
  senderName: string
  senderOrgName: string
  receiverOrgId: string
  receiverName: string
  receiverOrgName: string
  /** 1=已发送 2=已确认 */
  status: number
  sentTime: string
  confirmedTime: string | null
}

/** 查询已发送的通知单列表（五级仅本人发送，上级按辖区） */
export function getSentNoticeListApi(params: { pageNum: number, size: number }) {
  return request<ApiResponseData<{ records: SentNoticeVO[], total: number }>>({
    url: "notice/sent",
    method: "get",
    params
  })
}

/** 催促接收方接收通知单 */
export function remindNoticeApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `notice/remind/${id}`,
    method: "post"
  })
}

// ====== 转诊相关 ======

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

export interface ReferralDetailVO {
  id: string
  bizType: string
  populationType: string
  moduleType: string
  subjectName: string
  /** 业务摘要 JSON 字符串 */
  summary: string | null
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

/** 查询转诊详情（含发送方信息） */
export function getReferralDetailApi(id: string) {
  return request<ApiResponseData<ReferralDetailVO>>({
    url: `referral/${id}`,
    method: "get"
  })
}

/** 在消息页确认接收转诊 */
export function confirmReferralFromMessageApi(referralId: string, actualReferralDate?: string) {
  return request<ApiResponseData<null>>({
    url: `referral/confirm/${referralId}`,
    method: "post",
    data: actualReferralDate ? { actualReferralDate } : undefined
  })
}

/** 在消息页拒绝转诊 */
export function rejectReferralFromMessageApi(referralId: string, rejectReason?: string) {
  return request<ApiResponseData<null>>({
    url: `referral/reject/${referralId}`,
    method: "post",
    data: { rejectReason }
  })
}

/** 查询已发送的转诊列表（五级仅本人发送，上级按辖区） */
export function getSentReferralListApi(params: { pageNum: number, size: number }) {
  return request<ApiResponseData<{ records: SentReferralVO[], total: number }>>({
    url: "referral/sent",
    method: "get",
    params
  })
}
