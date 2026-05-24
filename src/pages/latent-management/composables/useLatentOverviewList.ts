import { usePagination } from "@@/composables/usePagination"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { getLatentAggregateListApi } from "../apis"

/** 在管潜伏感染者总览列表（排除密接、未归档） */
export function useLatentOverviewList() {
  const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

  const loading = ref(false)
  const tableData = ref<any[]>([])
  const total = ref(0)
  const FETCH_ALL_SIZE = 10000

  const searchForm = reactive({
    name: "",
    idNumber: "",
    phone: "",
    populationType: "",
    dateRange: [] as string[]
  })

  async function fetchData() {
    loading.value = true
    try {
      const { dateRange, ...rest } = searchForm
      const params: Record<string, any> = {
        page: 1,
        size: FETCH_ALL_SIZE,
        archived: 0,
        ...rest,
        ...extractDateRangeParams(dateRange)
      }
      if (!params.populationType) delete params.populationType
      if (!params.phone) delete params.phone
      const { data } = await getLatentAggregateListApi(params)
      const filtered = (data.records ?? []).filter((r: any) => r.populationType !== "closeContact")
      const start = (paginationData.currentPage - 1) * paginationData.pageSize
      const end = start + paginationData.pageSize
      tableData.value = filtered.slice(start, end)
      total.value = filtered.length
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
    searchForm.dateRange = []
    handleSearch()
  }

  onMounted(fetchData)
  watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

  return {
    paginationData,
    handleCurrentChange,
    handleSizeChange,
    loading,
    tableData,
    total,
    searchForm,
    fetchData,
    handleSearch,
    handleReset
  }
}
