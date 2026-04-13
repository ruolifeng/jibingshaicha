import { request } from "@/http/axios"

/** 获取区县选项列表（从实际数据动态获取） */
export function getDistrictOptionsApi() {
  return request<ApiResponseData<string[]>>({
    url: "statistics/district-options",
    method: "get"
  })
}

/** 学校人群统计总表 */
export function getSchoolStatisticsApi(params: {
  year?: string
  district?: string
}) {
  return request<ApiResponseData<any[]>>({
    url: "statistics/school",
    method: "get",
    params
  })
}

/** 区县统计表 */
export function getDistrictStatisticsApi(params: {
  year?: string
  district?: string
}) {
  return request<ApiResponseData<any[]>>({
    url: "statistics/district",
    method: "get",
    params
  })
}

/** 导出学校人群统计 Excel */
export function exportSchoolStatisticsApi(params: {
  year?: string
  district?: string
}) {
  return request<Blob>({
    url: "statistics/school/export",
    method: "get",
    params,
    responseType: "blob"
  })
}

/** 导出区县统计 Excel */
export function exportDistrictStatisticsApi(params: {
  year?: string
  district?: string
}) {
  return request<Blob>({
    url: "statistics/district/export",
    method: "get",
    params,
    responseType: "blob"
  })
}
