interface PaginationData {
  total?: number
  currentPage?: number
  pageSizes?: number[]
  pageSize?: number
  layout?: string
}

/** 单页最大条数，避免一次渲染过多行导致页面卡顿 */
export const MAX_PAGE_SIZE = 100

/** 默认的分页参数 */
const DEFAULT_PAGINATION_DATA = {
  total: 0,
  currentPage: 1,
  pageSizes: [10, 20, 50, 100],
  pageSize: 10,
  layout: "total, sizes, prev, pager, next, jumper"
}

function clampPageSize(size: number | undefined) {
  const value = size ?? DEFAULT_PAGINATION_DATA.pageSize
  return Math.min(Math.max(value, 1), MAX_PAGE_SIZE)
}

/** 分页 Composable */
export function usePagination(initPaginationData: PaginationData = {}) {
  const pageSizes = (initPaginationData.pageSizes ?? DEFAULT_PAGINATION_DATA.pageSizes)
    .filter(size => size <= MAX_PAGE_SIZE)

  // 合并分页参数
  const paginationData = reactive({
    ...DEFAULT_PAGINATION_DATA,
    ...initPaginationData,
    pageSizes: pageSizes.length > 0 ? pageSizes : DEFAULT_PAGINATION_DATA.pageSizes,
    pageSize: clampPageSize(initPaginationData.pageSize)
  })

  // 改变当前页码
  const handleCurrentChange = (value: number) => {
    paginationData.currentPage = value
  }

  // 改变每页显示条数
  const handleSizeChange = (value: number) => {
    paginationData.pageSize = clampPageSize(value)
    paginationData.currentPage = 1
  }

  return { paginationData, handleCurrentChange, handleSizeChange }
}
