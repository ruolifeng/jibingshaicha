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

/** 大汇总表导出（三类人群合并） */
export function exportWideTableApi(year?: string) {
  return request<Blob>({
    url: "export/wide-table",
    method: "get",
    params: { year },
    responseType: "blob"
  })
}

/** 分类汇总表导出 */
export function exportCategoryTableApi(populationType: string, year?: string) {
  return request<Blob>({
    url: "export/category-table",
    method: "get",
    params: { populationType, year },
    responseType: "blob"
  })
}

/** 自定义字段导出 */
export function exportCustomApi(populationType: string, fields: string, year?: string) {
  return request<Blob>({
    url: "export/custom",
    method: "get",
    params: { populationType, fields, year },
    responseType: "blob"
  })
}
