import { computed, reactive } from "vue"

/**
 * 服务端表头筛选：与顶部 searchForm 合并为 columnFilters JSON Query 参数。
 */
export function useServerColumnFilters() {
  const columnFilters = reactive<Record<string, string>>({})

  const hasActiveFilters = computed(() =>
    Object.values(columnFilters).some(v => !!v && String(v).trim() !== "")
  )

  function setFilter(field: string, value: string | string[] | null | undefined) {
    const normalized = Array.isArray(value)
      ? value.map(v => String(v).trim()).filter(Boolean).join(",")
      : (value == null ? "" : String(value).trim())
    if (!normalized) {
      delete columnFilters[field]
    } else {
      columnFilters[field] = normalized
    }
  }

  function clearFilters() {
    Object.keys(columnFilters).forEach((key) => {
      delete columnFilters[key]
    })
  }

  /** 传给后端的 columnFilters JSON；无筛选时返回 undefined */
  function toQueryParam(): string | undefined {
    const entries = Object.entries(columnFilters).filter(([, v]) => !!v && String(v).trim() !== "")
    if (entries.length === 0) return undefined
    return JSON.stringify(Object.fromEntries(entries))
  }

  return {
    columnFilters,
    hasActiveFilters,
    setFilter,
    clearFilters,
    toQueryParam
  }
}
