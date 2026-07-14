import { DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE, PAGE_SIZE_OPTIONS } from "@@/constants/pagination"

export { MAX_PAGE_SIZE } from "@@/constants/pagination"

interface PaginationData {
  total?: number
  currentPage?: number
  pageSizes?: number[]
  pageSize?: number
  layout?: string
}

/** 默认的分页参数 */
const DEFAULT_PAGINATION_DATA = {
  total: 0,
  currentPage: 1,
  pageSizes: [...PAGE_SIZE_OPTIONS],
  pageSize: DEFAULT_PAGE_SIZE,
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

  /** 表格序号：跨页连续（第 2 页从 11 起，而非每页从 1 起） */
  const getTableIndex = (index: number) => {
    const page = paginationData.currentPage || 1
    const size = paginationData.pageSize || 10
    return (page - 1) * size + index + 1
  }

  return { paginationData, handleCurrentChange, handleSizeChange, getTableIndex }
}
