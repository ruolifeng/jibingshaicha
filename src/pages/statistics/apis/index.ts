import type { DashboardSummaryData } from "@/pages/dashboard/apis"

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

/** 我的工作台年度统计 */
export function getWorkbenchStatisticsApi(year?: number | string) {
  return request<ApiResponseData<DashboardSummaryData>>({
    url: "statistics/workbench",
    method: "get",
    params: year != null && year !== "" ? { year: Number(year) } : {}
  })
}

/** 患者分布热力图（三级及以上用户） */
export interface PatientHeatmapRegion {
  name: string
  value?: number
  adcode?: string
}

export interface PatientHeatmapData {
  managementYear?: number
  statPeriodFrom?: string
  statPeriodTo?: string
  total?: number
  maxCount?: number
  /** city=自贡各区县 district=区县下乡镇 */
  mapLevel?: "city" | "district"
  districtName?: string
  districtAdcode?: string
  regions?: PatientHeatmapRegion[]
}

export function getPatientHeatmapApi(year?: number | string, district?: string) {
  return request<ApiResponseData<PatientHeatmapData>>({
    url: "statistics/patient-heatmap",
    method: "get",
    params: {
      ...(year != null && year !== "" ? { year: Number(year) } : {}),
      ...(district ? { district } : {})
    }
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

/** P6 新增：患者信息总表导出（全部来源，含来源标签） */
export function exportAllPatientsApi(params?: {
  populationType?: string
  name?: string
  idNumber?: string
  archived?: number
}) {
  return request<Blob>({
    url: "export/all-patients",
    method: "get",
    params,
    responseType: "blob"
  })
}

/** P6 新增：潜伏感染者信息总表导出（全部来源，含来源标签，默认排除密接） */
export function exportAllLatentApi(params?: {
  populationType?: string
  name?: string
  idNumber?: string
  archived?: number
}) {
  return request<Blob>({
    url: "export/all-latent",
    method: "get",
    params,
    responseType: "blob"
  })
}
