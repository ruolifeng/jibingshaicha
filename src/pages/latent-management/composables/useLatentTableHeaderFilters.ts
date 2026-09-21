import type { Ref } from "vue"
import { useColumnDistinct } from "@@/composables/useColumnDistinct"
import {
  KEY_INFECTION_JUDGE_RESULT_OPTIONS,
  LATENT_MANUAL_POPULATION_TYPE_OPTIONS,
  mergeLatentMedicationUnitOptions
} from "@@/constants/disease"
import { computed } from "vue"
import { getLatentColumnDistinctApi } from "../apis"

/** 潜伏感染者列表页共用的表头筛选选项 / distinct 加载 */
export function useLatentTableHeaderFilters(populationType?: Ref<string> | (() => string | undefined)) {
  const genderFilterOptions = [
    { text: "男", value: "男" },
    { text: "女", value: "女" },
    { text: "男性", value: "男性" },
    { text: "女性", value: "女性" }
  ]
  const populationTypeFilterOptions = LATENT_MANUAL_POPULATION_TYPE_OPTIONS.map(item => ({
    text: item.label,
    value: item.value
  }))
  const infectionResultFilterOptions = KEY_INFECTION_JUDGE_RESULT_OPTIONS.map(item => ({
    text: item,
    value: item
  }))

  const resolvePopulationType = () => {
    if (!populationType) return undefined
    return typeof populationType === "function" ? populationType() : populationType.value
  }

  const { load, sourceValues, clearCache } = useColumnDistinct(async (field) => {
    const { data } = await getLatentColumnDistinctApi(field, resolvePopulationType())
    return Array.isArray(data) ? data : []
  })

  const rawMedicationUnitSourceValues = sourceValues("medicationManagementUnit")
  /** 含预设机构，并对「富顺县*」做名称归一 */
  const medicationUnitSourceValues = computed(() =>
    mergeLatentMedicationUnitOptions(rawMedicationUnitSourceValues.value)
  )

  return {
    genderFilterOptions,
    populationTypeFilterOptions,
    infectionResultFilterOptions,
    loadGenderOptions: () => load("gender"),
    loadPopulationTypeOptions: () => load("populationType"),
    loadInfectionResultOptions: () => load("infectionResult"),
    loadMedicationUnitOptions: () => load("medicationManagementUnit"),
    genderSourceValues: sourceValues("gender"),
    populationTypeSourceValues: sourceValues("populationType"),
    infectionResultSourceValues: sourceValues("infectionResult"),
    medicationUnitSourceValues,
    clearDistinctCache: clearCache
  }
}
