import { request } from "@/http/axios"

/** 查询推介/追踪记录详情 */
export function getReferralTrackingDetailApi(id: number) {
  return request<ApiResponseData<any>>({
    url: `referral-tracking/${id}`,
    method: "get"
  })
}

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

/** 接收方开启共同追踪（发起方与接收方均可追踪，次数合并计算） */
export function enableJointTrackingApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/joint-tracking`,
    method: "post"
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
export function saveDiagnosisApi(id: number, diagnosisResult: string, diagnosisRemark?: string) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}/diagnosis`,
    method: "post",
    data: { diagnosisResult, diagnosisRemark }
  })
}

/** 删除推介/追踪记录 */
export function deleteReferralTrackingApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `referral-tracking/${id}`,
    method: "delete"
  })
}

/** 检查推介/追踪是否已有相同证件号+姓名记录 */
export function checkReferralDuplicateApi(params: { bizMode: string, idNumber: string, name: string }) {
  return request<ApiResponseData<{ exists: boolean }>>({
    url: "referral-tracking/check-duplicate",
    method: "get",
    params
  })
}

/** 大疫情表导入预览（检测重复患者） */
export function previewEpidemicTrackImportApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{
    duplicateCount: number
    newCount: number
    updateCount: number
    duplicates: { name: string, idNumber: string }[]
  }>>({
    url: "referral-tracking/import-epidemic/preview",
    method: "post",
    data: formData
  })
}

/** 大疫情表导入（追踪模块） */
export function importEpidemicTrackApi(file: File, addDuplicateRecords = false) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{ count: number, updated?: number, skipped?: number, batchNo: string }>>({
    url: "referral-tracking/import-epidemic",
    method: "post",
    params: { addDuplicateRecords },
    data: formData
  })
}

/** 导出追踪记录 */
export function exportReferralTrackApi(params: Record<string, any>) {
  return request<Blob>({
    url: "referral-tracking/export",
    method: "get",
    params,
    responseType: "blob"
  })
}

/** 获取一至五级用户列表（推介接收人选择） */
export function getLevel34UsersApi() {
  return request<ApiResponseData<any[]>>({
    url: "user/level34-users",
    method: "get"
  })
}
