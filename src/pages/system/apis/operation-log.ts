import { request } from "@/http/axios"

export interface OperationLogItem {
  id: string
  userId: string | null
  userName: string | null
  realName: string | null
  departmentId: string | null
  role: number | null
  opType: string
  opModule: string | null
  opAction: string | null
  bizId: string | null
  bizType: string | null
  requestMethod: string | null
  requestUrl: string | null
  requestParams: string | null
  ip: string | null
  userAgent: string | null
  resultStatus: number
  errorMessage: string | null
  costMs: number | null
  createTime: string
}

export interface OperationLogQuery {
  page: number
  size: number
  opType?: string
  opModule?: string
  userName?: string
  keyword?: string
  startTime?: string
  endTime?: string
}

/** 分页查询操作日志 */
export function getOperationLogListApi(params: OperationLogQuery) {
  return request<ApiResponseData<{ records: OperationLogItem[], total: number }>>({
    url: "operation-log/list",
    method: "get",
    params
  })
}

/** 导出操作日志 */
export function exportOperationLogApi(params: Omit<OperationLogQuery, "page" | "size">) {
  return request<Blob>({
    url: "operation-log/export",
    method: "get",
    params,
    responseType: "blob"
  })
}
