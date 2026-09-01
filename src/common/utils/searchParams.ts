/** 将日期区间选择器的值转为 API 查询参数 */
export function appendDateRangeParams(
  params: Record<string, unknown>,
  dateRange?: string[] | null,
  fromKey = "dateFrom",
  toKey = "dateTo"
) {
  const [from, to] = dateRange ?? []
  if (from) params[fromKey] = from
  if (to) params[toKey] = to
}

export function extractDateRangeParams(dateRange?: string[] | null) {
  const [dateFrom, dateTo] = dateRange ?? []
  return {
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined
  }
}

/** 用户录入时间（create_time）区间 */
export function extractCreateTimeRangeParams(dateRange?: string[] | null) {
  const [createTimeFrom, createTimeTo] = dateRange ?? []
  return {
    createTimeFrom: createTimeFrom || undefined,
    createTimeTo: createTimeTo || undefined
  }
}

/** 表头筛选 JSON 参数（无筛选时不带该字段） */
export function appendColumnFiltersParam(
  params: Record<string, unknown>,
  columnFiltersJson?: string | null
) {
  if (columnFiltersJson) {
    params.columnFilters = columnFiltersJson
  }
}

/** 将搜索栏字段并入 columnFilters JSON（表头未筛该字段时） */
export function mergeColumnFilter(
  columnFiltersJson: string | undefined,
  field: string,
  value?: string | null
): string | undefined {
  const filters: Record<string, string> = columnFiltersJson
    ? JSON.parse(columnFiltersJson) as Record<string, string>
    : {}
  const trimmed = value?.trim() || ""
  if (trimmed && !filters[field]) {
    filters[field] = trimmed
  }
  const entries = Object.entries(filters).filter(([, v]) => !!v && String(v).trim() !== "")
  return entries.length ? JSON.stringify(Object.fromEntries(entries)) : undefined
}
