import { usePagination } from "@@/composables/usePagination"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { getLatentAggregateListApi } from "../apis"

/** 在管潜伏感染者总览列表（含手动/导入密接，排除密接筛查同步数据） */
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
        referralResult: "latent",
        ...rest,
        ...extractDateRangeParams(dateRange)
      }
      if (!params.populationType) delete params.populationType
      if (!params.phone) delete params.phone
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
