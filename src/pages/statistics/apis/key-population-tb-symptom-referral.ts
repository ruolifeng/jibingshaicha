import { withDepartmentIds } from "@@/utils/departmentFilter"
import { request } from "@/http/axios"

/** 重点人群肺结核可疑症状筛查和推介情况报表行 */
export interface KeyPopulationTbSymptomReferralStatisticsVO {
  district?: string
  elderCount?: number
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

/** 重点人群肺结核可疑症状筛查和推介情况报表 */
export function getKeyPopulationTbSymptomReferralStatisticsApi(params: StatParams) {
  return request<ApiResponseData<KeyPopulationTbSymptomReferralStatisticsVO[]>>({
    url: "statistics/key-population-tb-symptom-referral",
    method: "get",
    params: withDepartmentIds(params, params.departmentIds)
  })
}

/** 导出重点人群肺结核可疑症状筛查和推介情况报表 */
export function exportKeyPopulationTbSymptomReferralStatisticsApi(params: StatParams) {
  return request<Blob>({
    url: "statistics/key-population-tb-symptom-referral/export",
    method: "get",
    params: withDepartmentIds(params, params.departmentIds),
    responseType: "blob"
  })
}
