import { request } from "@/http/axios"

/** 导出潜伏感染管理列表（学校/重点人群） */
export function exportLatentListApi(params: {
  populationType: string
  name?: string
  idNumber?: string
  phone?: string
  dateFrom?: string
  dateTo?: string
  archived?: number
}) {
  return request<Blob>({
    url: "export/latent-list",
    method: "get",
    params,
    responseType: "blob"
  })
}

/** 分页查询潜伏感染数据 */
export function getLatentListApi(params: {
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
  columnFilters?: string
}) {
  return request<ApiResponseData<any>>({
    url: "latent/list",
    method: "get",
    params
  })
}

/** 追踪操作 */
export function trackLatentApi(data: { id: string, status: number, remark?: string, actualArrivalDate?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/track",
    method: "post",
    data
  })
}

/** 转诊操作 */
export function referralLatentApi(data: { id: string, result: string, remark?: string, actualReferralDate?: string }) {
  return request<ApiResponseData<null>>({
    url: "latent/referral",
    method: "post",
    data
  })
}

/** 发送通知单 */
export function sendNoticeApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "notice/send",
    method: "post",
    data
  })
}

/** 保存通知单草稿（填写但不发送） */
export function saveNoticeDraftApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "notice/draft",
    method: "post",
    data
  })
}

/** 确认接收通知单 */
export function confirmNoticeApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `notice/confirm/${id}`,
    method: "post"
  })
}

/** 通知单详情 */
export function getNoticeDetailApi(id: string) {
  return request<ApiResponseData<any>>({
    url: `notice/detail/${id}`,
    method: "get"
  })
}

/** 查询业务关联通知单 */
export function getNoticeListByBizApi(bizId: string, noticeType: string) {
  return request<ApiResponseData<any[]>>({
    url: "notice/list",
    method: "get",
    params: { bizId, noticeType }
  })
}

/** 保存督导表 */
export function saveSupervisionApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "supervision/save",
    method: "post",
    data
  })
}

/** 查询督导表详情 */
export function getSupervisionDetailApi(latentInfectionId: string) {
  return request<ApiResponseData<any>>({
    url: `supervision/detail/${latentInfectionId}`,
    method: "get"
  })
}

/** 设置服药状态 */
export function setMedicationStatusApi(data: { id: string, medicationStatus: number }) {
  return request<ApiResponseData<null>>({
    url: "latent/medication-status",
    method: "post",
    data
  })
}

/** 结案归档 */
export function closeCaseApi(id: string) {
  return request<ApiResponseData<null>>({
    url: `latent/close-case/${id}`,
    method: "post"
  })
}

/** 查询电话随访记录 */
export function getFollowUpListApi(latentId: string) {
  return request<ApiResponseData<any[]>>({
    url: `latent/follow-up/list/${latentId}`,
    method: "get"
  })
}

/** 新增电话随访记录 */
export function saveFollowUpApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "latent/follow-up/save",
    method: "post",
    data
  })
}

/** 查询按期检查记录 */
export function getCheckListApi(latentId: string) {
  return request<ApiResponseData<any[]>>({
    url: `latent/check/list/${latentId}`,
    method: "get"
  })
}

/** 新增按期检查记录 */
export function saveCheckApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "latent/check/save",
    method: "post",
    data
  })
}

// ==================== V4 新增：胸片与诊断 ====================

/**
 * 手动录入胸片检查与首次诊断结果（追踪到位后）
 * diagnosisFirst 取值：排除/疑似结核/潜伏感染者/确诊患者/其他
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

/**
 * 批量导入胸片+诊断 Excel（含转诊阶段 Z-AE 列，按证件号匹配更新）
 */
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
