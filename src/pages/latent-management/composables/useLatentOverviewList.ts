import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { extractDateRangeParams, mergeColumnFilter } from "@@/utils/searchParams"
import { getLatentAggregateListApi } from "../apis"

export interface LatentOverviewListOptions {
  /** 固定筛选（如服药管理仅显示到位=1；督导表请用 noticeSent） */
  trackingStatus?: number
  /** 固定通知单发送状态（督导表仅显示已发送） */
  noticeSent?: boolean
}

/** 在管潜伏感染者总览列表（含手动/导入密接，排除密接筛查同步数据） */
export function useLatentOverviewList(options: LatentOverviewListOptions = {}) {
  const { paginationData, handleCurrentChange, handleSizeChange, getTableIndex } = usePagination()
  const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

  const loading = ref(false)
  const tableData = ref<any[]>([])
  const allRecords = ref<any[]>([])
  const total = ref(0)
  const FETCH_ALL_SIZE = 10000
  const sortState = reactive<{ prop: string, order: "ascending" | "descending" | null }>({
    prop: "",
    order: null
  })

  const searchForm = reactive({
    name: "",
    idNumber: "",
    phone: "",
    populationType: "",
    keyPopulationSubCategories: [] as string[],
    creatorName: "",
    dateRange: [] as string[],
    formatIssue: "",
    trackingStatus: undefined as number | undefined,
    medicationManagementUnit: ""
  })

  function applySortAndPage() {
    const list = [...allRecords.value]
    if (sortState.order && sortState.prop === "createTime") {
      const desc = sortState.order === "descending"
      list.sort((a, b) => {
        const ta = a.createTime ? new Date(a.createTime).getTime() : 0
        const tb = b.createTime ? new Date(b.createTime).getTime() : 0
        return desc ? tb - ta : ta - tb
      })
    }
    const start = (paginationData.currentPage - 1) * paginationData.pageSize
    const end = start + paginationData.pageSize
    tableData.value = list.slice(start, end)
    total.value = list.length
  }

  async function fetchData() {
    loading.value = true
    try {
      const { dateRange, keyPopulationSubCategories, formatIssue, ...rest } = searchForm
      const columnFiltersParam = mergeColumnFilter(
        toQueryParam(),
        "medicationManagementUnit",
        rest.medicationManagementUnit
      )
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
        ...(formatIssue ? { formatIssue } : {}),
        ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {}),
        ...(options.trackingStatus != null ? { trackingStatus: options.trackingStatus } : {}),
        ...(options.noticeSent != null ? { noticeSent: options.noticeSent } : {})
      }
      if (!params.populationType) delete params.populationType
      if (!params.phone) delete params.phone
      if (!params.creatorName) delete params.creatorName
      if (!params.medicationManagementUnit) delete params.medicationManagementUnit
      if (params.trackingStatus == null) delete params.trackingStatus
      if (params.noticeSent == null) delete params.noticeSent
      const { data } = await getLatentAggregateListApi(params)
      allRecords.value = data.records ?? []
      applySortAndPage()
    } finally {
      loading.value = false
    }
  }

  function handleSortChange(payload: { prop?: string, order?: "ascending" | "descending" | null }) {
    sortState.prop = payload.prop || ""
    sortState.order = payload.order ?? null
    applySortAndPage()
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
    searchForm.formatIssue = ""
    searchForm.trackingStatus = undefined
    searchForm.medicationManagementUnit = ""
    sortState.prop = ""
    sortState.order = null
    clearFilters()
    handleSearch()
  }

  onMounted(fetchData)
  watch([() => paginationData.currentPage, () => paginationData.pageSize], applySortAndPage)

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
    handleReset,
    handleSortChange
  }
}
