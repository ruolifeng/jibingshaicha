import { request } from "@/http/axios"

/** 分页查询推介/追踪记录 */
export function getReferralTrackingListApi(params: Record<string, any>) {
  return request<ApiResponseData<any>>({
    url: "referral-tracking/list",
    method: "get",
    params
  })
}

/** 新增推介或追踪记录（bizMode: recommend/track） */
export function createReferralTrackingApi(data: Record<string, any>) {
  return request<ApiResponseData<any>>({
    url: "referral-tracking",
    method: "post",
    data
  })
}

/** 更新基本信息 */
export function updateReferralTrackingApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}`,
    method: "put",
    data
  })
}

/** 发送推介通知（bizMode=recommend） */
export function sendRecommendApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/send`,
    method: "post"
  })
}

/** 接收方确认接受推介通知单 */
export function confirmRecommendApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/confirm`,
    method: "post"
  })
}

/** 接收方拒绝推介通知单 */
export function rejectRecommendApi(id: number, reason?: string) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/reject`,
    method: "post",
    data: { reason }
  })
}

/** 追踪操作（status: 1到位 2未到位 3其他） */
export function trackReferralApi(id: number, status: number, remark?: string) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/track`,
    method: "post",
    data: { status, remark }
  })
}

/** 保存到位后的感染筛查+胸片信息 */
export function saveScreeningInfoApi(id: number, data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/screening`,
    method: "post",
    data
  })
}

/** 保存诊断结果并分流 */
export function saveDiagnosisApi(id: number, diagnosisResult: string) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/diagnosis`,
    method: "post",
    data: { diagnosisResult }
  })
}

/** 删除推介/追踪记录 */
export function deleteReferralTrackingApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}`,
    method: "delete"
  })
}

/** 获取三/四级用户列表（推介追踪接收人选择） */
export function getLevel34UsersApi() {
  return request<ApiResponseData<any[]>>({
    url: "user/level34-users",
    method: "get"
  })
}
