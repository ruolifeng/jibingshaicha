import { useColumnDistinct } from "@@/composables/useColumnDistinct"
import { PATHOGEN_RESULT_FILTER_OPTIONS } from "@@/constants/disease"
import { getPatientColumnDistinctApi } from "../apis"

/** 患者列表页共用的表头筛选选项 / distinct 加载 */
export function usePatientTableHeaderFilters(archived?: number) {
  const genderFilterOptions = [
    { text: "男", value: "男" },
    { text: "女", value: "女" },
    { text: "男性", value: "男性" },
    { text: "女性", value: "女性" }
  ]
  const pathogenFilterOptions = PATHOGEN_RESULT_FILTER_OPTIONS.map(item => ({ text: item, value: item }))
  const populationTypeFilterOptions = [
    { text: "学生筛查", value: "school" },
    { text: "重点人群", value: "keyPopulation" },
    { text: "疫情筛查", value: "regular" },
    { text: "大疫情", value: "epidemic" },
    { text: "推介", value: "referral" },
    { text: "密接", value: "closeContact" },
    { text: "专病网", value: "specialDisease" }
  ]

  const { load, sourceValues, clearCache } = useColumnDistinct(async (field) => {
    const { data } = await getPatientColumnDistinctApi(field, archived)
    return Array.isArray(data) ? data : []
  })

  return {
    genderFilterOptions,
    pathogenFilterOptions,
    populationTypeFilterOptions,
    loadGenderOptions: () => load("gender"),
    loadPathogenOptions: () => load("diagnosisResult"),
    loadPopulationTypeOptions: () => load("populationType"),
    genderSourceValues: sourceValues("gender"),
    pathogenSourceValues: sourceValues("diagnosisResult"),
    populationTypeSourceValues: sourceValues("populationType"),
    clearDistinctCache: clearCache
  }
}
