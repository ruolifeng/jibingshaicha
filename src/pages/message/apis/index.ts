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
export function markMessageReadApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `message/read/${id}`,
    method: "post"
  })
}

/** 在消息页确认接收通知单 */
export function confirmNoticeFromMessageApi(noticeId: number) {
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

export interface SentNoticeVO {
  id: number
  noticeType: string
  populationType: string
  patientName: string
  senderId: number
  senderName: string
  senderOrgName: string
  receiverOrgId: number
  receiverName: string
  receiverOrgName: string
  /** 1=已发送 2=已确认 */
  status: number
  sentTime: string
  confirmedTime: string | null
}

/** 查询当前用户已发送的通知单列表 */
export function getSentNoticeListApi(params: { pageNum: number, size: number }) {
  return request<ApiResponseData<{ records: SentNoticeVO[], total: number }>>({
    url: "notice/sent",
    method: "get",
    params
  })
}

/** 催促接收方接收通知单 */
export function remindNoticeApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `notice/remind/${id}`,
    method: "post"
  })
}

// ====== 分级诊疗相关 ======

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

/** 在消息页确认接收分级诊疗 */
export function confirmReferralFromMessageApi(referralId: number) {
  return request<ApiResponseData<null>>({
    url: `referral/confirm/${referralId}`,
    method: "post"
  })
}

/** 在消息页拒绝分级诊疗 */
export function rejectReferralFromMessageApi(referralId: number, rejectReason?: string) {
  return request<ApiResponseData<null>>({
    url: `referral/reject/${referralId}`,
    method: "post",
    data: { rejectReason }
  })
}

/** 查询当前用户已发送的分级诊疗列表 */
export function getSentReferralListApi(params: { pageNum: number; size: number }) {
  return request<ApiResponseData<{ records: SentReferralVO[]; total: number }>>({
    url: "referral/sent",
    method: "get",
    params
  })
}
