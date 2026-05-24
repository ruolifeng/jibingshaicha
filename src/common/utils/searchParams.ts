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
