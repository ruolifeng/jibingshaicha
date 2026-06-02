/**
 * 聚合患者管理公共逻辑
 * 各子菜单页面通过 usePagination + 本 composable 获取统一的患者列表数据。
 */
import { usePagination } from "@@/composables/usePagination"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { getPatientListApi } from "../apis"

export interface PatientListOptions {
  /** 在管总览：时间段按病案登记日期；服药管理单位筛选 */
  overviewSearch?: boolean
  /** 通知单管理：时间段按填写通知单时间；服药管理单位筛选 */
  noticeSearch?: boolean
  /** 首次入户随访：时间段按填写时间；服药管理单位筛选 */
  firstVisitSearch?: boolean
  /** 后续随访：时间段按填写时间；服药管理单位筛选 */
  followUpSearch?: boolean
}

function hasMedicationUnitSearch(options?: PatientListOptions) {
  return !!(options?.overviewSearch || options?.noticeSearch
    || options?.firstVisitSearch || options?.followUpSearch)
}

export function usePatientList(defaultArchived?: number, options?: PatientListOptions) {
  const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

  const loading = ref(false)
  const tableData = ref<any[]>([])
  const total = ref(0)

  const searchForm = reactive({
    name: "",
    idNumber: "",
    phone: "",
    currentAddress: "",
    diagnosisResult: "",
    populationType: "",
    dateRange: [] as string[],
    archived: defaultArchived,
    ...(hasMedicationUnitSearch(options) ? { medicationManagementUnit: "" } : {})
  })

  async function fetchData() {
    loading.value = true
    try {
      const { dateRange, ...rest } = searchForm
      const params: Record<string, any> = {
        page: paginationData.currentPage,
        size: paginationData.pageSize,
        ...rest,
        ...extractDateRangeParams(dateRange)
      }
      if (options?.overviewSearch) {
        params.dateFilterBy = "registrationDate"
      } else if (options?.noticeSearch) {
        params.dateFilterBy = "noticeFill"
      } else if (options?.firstVisitSearch) {
        params.dateFilterBy = "firstVisitFill"
      } else if (options?.followUpSearch) {
        params.dateFilterBy = "followUpFill"
      }
      if (!params.populationType) delete params.populationType
      if (!params.phone) delete params.phone
      if (!params.currentAddress) delete params.currentAddress
      if (!params.diagnosisResult) delete params.diagnosisResult
      if (!params.medicationManagementUnit) delete params.medicationManagementUnit
      const { data } = await getPatientListApi(params)
      tableData.value = data.records
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  function handleSearch() { paginationData.currentPage = 1; fetchData() }
  function handleReset() {
    searchForm.name = ""
    searchForm.idNumber = ""
    searchForm.phone = ""
    searchForm.currentAddress = ""
    searchForm.diagnosisResult = ""
    searchForm.populationType = ""
    searchForm.dateRange = []
    if (hasMedicationUnitSearch(options) && "medicationManagementUnit" in searchForm) {
      searchForm.medicationManagementUnit = ""
    }
    handleSearch()
  }

  onMounted(fetchData)
  watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

  return {
    paginationData, handleCurrentChange, handleSizeChange,
    loading, tableData, total,
    searchForm, fetchData, handleSearch, handleReset,
    overviewSearch: options?.overviewSearch ?? false
  }
}
