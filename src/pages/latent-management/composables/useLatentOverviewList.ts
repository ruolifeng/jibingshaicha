import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { getLatentAggregateListApi } from "../apis"

/** 在管潜伏感染者总览列表（含手动/导入密接，排除密接筛查同步数据） */
export function useLatentOverviewList() {
  const { paginationData, handleCurrentChange, handleSizeChange, getTableIndex } = usePagination()
  const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

  const loading = ref(false)
  const tableData = ref<any[]>([])
  const total = ref(0)
  const FETCH_ALL_SIZE = 10000

  const searchForm = reactive({
    name: "",
    idNumber: "",
    phone: "",
    populationType: "",
    keyPopulationSubCategories: [] as string[],
    creatorName: "",
    dateRange: [] as string[]
  })

  async function fetchData() {
    loading.value = true
    try {
      const { dateRange, keyPopulationSubCategories, ...rest } = searchForm
      const columnFiltersParam = toQueryParam()
      const params: Record<string, any> = {
        page: 1,
        size: FETCH_ALL_SIZE,
        archived: 0,
        referralResult: "latent",
        ...rest,
        ...extractDateRangeParams(dateRange),
        ...(keyPopulationSubCategories.length > 0
          ? { crowdCategory: keyPopulationSubCategories.join(",") }
          : {}),
        ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
      }
      if (!params.populationType) delete params.populationType
      if (!params.phone) delete params.phone
      if (!params.creatorName) delete params.creatorName
      const { data } = await getLatentAggregateListApi(params)
      const records = data.records ?? []
      const start = (paginationData.currentPage - 1) * paginationData.pageSize
      const end = start + paginationData.pageSize
      tableData.value = records.slice(start, end)
      total.value = records.length
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    paginationData.currentPage = 1
    fetchData()
  }

  function handleReset() {
    searchForm.name = ""
    searchForm.idNumber = ""
    searchForm.phone = ""
    searchForm.populationType = ""
    searchForm.keyPopulationSubCategories = []
    searchForm.creatorName = ""
    searchForm.dateRange = []
    clearFilters()
    handleSearch()
  }

  onMounted(fetchData)
  watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

  return {
    paginationData,
    handleCurrentChange,
    handleSizeChange,
    getTableIndex,
    loading,
    tableData,
    total,
    searchForm,
    columnFilters,
    setFilter,
    clearFilters,
    toQueryParam,
    fetchData,
    handleSearch,
    handleReset
  }
}
