import type { DashboardSummaryData } from "@/pages/dashboard/apis"
import { withDepartmentIds } from "@@/utils/departmentFilter"
import { request } from "@/http/axios"

interface StatParams {
  year?: string
  district?: string
  departmentIds?: string[]
}

/** 获取区县选项列表（从实际数据动态获取） */
export function getDistrictOptionsApi(departmentIds?: string[]) {
  return request<ApiResponseData<string[]>>({
    url: "statistics/district-options",
    method: "get",
    params: withDepartmentIds({}, departmentIds)
  })
}

/** 学校人群统计总表 */
export function getSchoolStatisticsApi(params: StatParams) {
  return request<ApiResponseData<any[]>>({
    url: "statistics/school",
    method: "get",
    params: withDepartmentIds(params, params.departmentIds)
  })
}

/** 区县统计表 */
export function getDistrictStatisticsApi(params: StatParams) {
  return request<ApiResponseData<any[]>>({
    url: "statistics/district",
    method: "get",
    params: withDepartmentIds(params, params.departmentIds)
  })
}

/** 我的工作台年度统计 */
export function getWorkbenchStatisticsApi(year?: number | string, departmentIds?: string[]) {
  return request<ApiResponseData<DashboardSummaryData>>({
    url: "statistics/workbench",
    method: "get",
    params: withDepartmentIds(
      year != null && year !== "" ? { year: Number(year) } : {},
      departmentIds
    )
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

export function getPatientHeatmapApi(year?: number | string, district?: string, departmentIds?: string[]) {
  return request<ApiResponseData<PatientHeatmapData>>({
    url: "statistics/patient-heatmap",
    method: "get",
    params: withDepartmentIds(
      {
        ...(year != null && year !== "" ? { year: Number(year) } : {}),
        ...(district ? { district } : {})
      },
      departmentIds
    )
  })
}

/** 导出学校人群统计 Excel */
export function exportSchoolStatisticsApi(params: StatParams) {
  return request<Blob>({
    url: "statistics/school/export",
    method: "get",
    params: withDepartmentIds(params, params.departmentIds),
    responseType: "blob"
  })
}

/** 导出区县统计 Excel */
export function exportDistrictStatisticsApi(params: StatParams) {
  return request<Blob>({
    url: "statistics/district/export",
    method: "get",
    params: withDepartmentIds(params, params.departmentIds),
    responseType: "blob"
  })
}

/** 大汇总表导出（三类人群合并） */
export function exportWideTableApi(year?: string, departmentIds?: string[]) {
  return request<Blob>({
    url: "export/wide-table",
    method: "get",
    params: withDepartmentIds({ year }, departmentIds),
    responseType: "blob"
  })
}

/** 分类汇总表导出 */
export function exportCategoryTableApi(populationType: string, year?: string, departmentIds?: string[]) {
  return request<Blob>({
    url: "export/category-table",
    method: "get",
    params: withDepartmentIds({ populationType, year }, departmentIds),
    responseType: "blob"
  })
}

/** 自定义字段导出 */
export function exportCustomApi(populationType: string, fields: string, year?: string, departmentIds?: string[]) {
  return request<Blob>({
    url: "export/custom",
    method: "get",
    params: withDepartmentIds({ populationType, fields, year }, departmentIds),
    responseType: "blob"
  })
}

/** P6 新增：患者信息总表导出（全部来源，含来源标签） */
export function exportAllPatientsApi(params?: {
  populationType?: string
  name?: string
  idNumber?: string
  archived?: number
  departmentIds?: string[]
}) {
  return request<Blob>({
    url: "export/all-patients",
    method: "get",
    params: params ? withDepartmentIds(params, params.departmentIds) : {},
    responseType: "blob"
  })
}

/** P6 新增：潜伏感染者信息总表导出（全部来源，含来源标签，默认排除密接） */
export function exportAllLatentApi(params?: {
  populationType?: string
  name?: string
  idNumber?: string
  archived?: number
  departmentIds?: string[]
}) {
  return request<Blob>({
    url: "export/all-latent",
    method: "get",
    params: params ? withDepartmentIds(params, params.departmentIds) : {},
    responseType: "blob"
  })
}
