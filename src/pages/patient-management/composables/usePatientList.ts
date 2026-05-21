/**
 * 聚合患者管理公共逻辑
 * 各子菜单页面通过 usePagination + 本 composable 获取统一的患者列表数据。
 */
import { usePagination } from "@@/composables/usePagination"
import { getPatientListApi } from "../apis"

export function usePatientList(defaultArchived?: number) {
  const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

  const loading = ref(false)
  const tableData = ref<any[]>([])
  const total = ref(0)

  const searchForm = reactive({
    name: "",
    idNumber: "",
    phone: "",
    currentAddress: "",
    populationType: "",
    archived: defaultArchived
  })

  async function fetchData() {
    loading.value = true
    try {
      const params: Record<string, any> = {
        page: paginationData.currentPage,
        size: paginationData.pageSize,
        ...searchForm
      }
      if (!params.populationType) delete params.populationType
      if (!params.phone) delete params.phone
      if (!params.currentAddress) delete params.currentAddress
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
    searchForm.populationType = ""
    handleSearch()
  }

  onMounted(fetchData)
  watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

  return {
    paginationData, handleCurrentChange, handleSizeChange,
    loading, tableData, total,
    searchForm, fetchData, handleSearch, handleReset
  }
}
