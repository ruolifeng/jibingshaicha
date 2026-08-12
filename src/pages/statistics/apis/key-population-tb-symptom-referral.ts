import { withDepartmentIds } from "@@/utils/departmentFilter"
import { request } from "@/http/axios"

/** 重点人群肺结核可疑症状筛查和推介情况报表行 */
export interface KeyPopulationTbSymptomReferralStatisticsVO {
  district?: string
  /** 老年人数：模板无系统口径，页面手工填写，可为空 */
  elderCount?: number | null
  elderAnnualExamCount?: number
  elderSymptomScreenCount?: number
  elderChestXrayCount?: number
  elderInfectionScreenCount?: number
  elderSuspiciousSymptomCount?: number
  elderChestXrayAbnormalCount?: number
  elderInfectionAbnormalCount?: number
  elderReferralFormCount?: number
  elderArrivedCount?: number
  elderConfirmedTbCount?: number
  diabetesManagedCount?: number
  diabetesQuarterFollowCount?: number
  diabetesSymptomScreenCount?: number
  diabetesChestXrayCount?: number
  diabetesInfectionScreenCount?: number
  diabetesSuspiciousSymptomCount?: number
  diabetesChestXrayAbnormalCount?: number
  diabetesInfectionAbnormalCount?: number
  diabetesReferralFormCount?: number
  diabetesArrivedCount?: number
  diabetesConfirmedTbCount?: number
}

interface StatParams {
  year?: string
  district?: string
  departmentIds?: string[]
}

/** 地区选项（区县 + 乡镇/社区） */
export function getKeyPopulationTbSymptomReferralRegionOptionsApi(departmentIds?: string[]) {
  return request<ApiResponseData<string[]>>({
    url: "statistics/key-population-tb-symptom-referral/region-options",
    method: "get",
    params: withDepartmentIds({}, departmentIds)
  })
}

/** 重点人群肺结核可疑症状筛查和推介情况报表 */
export function getKeyPopulationTbSymptomReferralStatisticsApi(params: StatParams) {
  return request<ApiResponseData<KeyPopulationTbSymptomReferralStatisticsVO[]>>({
    url: "statistics/key-population-tb-symptom-referral",
    method: "get",
    params: withDepartmentIds(params, params.departmentIds)
  })
}

/** 导出（提交当前表格数据，保留手工填写的老年人数） */
export function exportKeyPopulationTbSymptomReferralStatisticsApi(
  year: string | undefined,
  rows: KeyPopulationTbSymptomReferralStatisticsVO[]
) {
  return request<Blob>({
    url: "statistics/key-population-tb-symptom-referral/export",
    method: "post",
    params: { year },
    data: rows,
    responseType: "blob"
  })
}
