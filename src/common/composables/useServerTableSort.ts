import { reactive } from "vue"

export type TableSortOrder = "asc" | "desc"

export interface TableSortState {
  sortField: string
  sortOrder: TableSortOrder
}

/**
 * 服务端表格排序：与 Element Plus sort-change 配合，默认按 Excel 原行号升序。
 */
export function useServerTableSort(defaultField = "importRowNo", defaultOrder: TableSortOrder = "asc") {
  const sortState = reactive<TableSortState>({
    sortField: defaultField,
    sortOrder: defaultOrder
  })

  function onSortChange(payload: { prop?: string, order?: "ascending" | "descending" | null }) {
    if (!payload.order) {
      sortState.sortField = defaultField
      sortState.sortOrder = defaultOrder
      return
    }
    sortState.sortField = payload.prop || defaultField
    sortState.sortOrder = payload.order === "descending" ? "desc" : "asc"
  }

  function resetSort() {
    sortState.sortField = defaultField
    sortState.sortOrder = defaultOrder
  }

  function toQueryParam(): Partial<Pick<TableSortState, "sortField" | "sortOrder">> {
    if (sortState.sortField === defaultField && sortState.sortOrder === defaultOrder) {
      return {}
    }
    return {
      sortField: sortState.sortField,
      sortOrder: sortState.sortOrder
    }
  }

  /** 表格初始不显示排序箭头（默认按 Excel 行号，无对应列） */
  const defaultSort = undefined

  return {
    sortState,
    defaultSort,
    onSortChange,
    resetSort,
    toQueryParam
  }
}
