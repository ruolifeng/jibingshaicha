/**
 * 统计年度：上年度 12/1 — 本年度 11/30，statYear 为周期结束日所在自然年。
 * 12 月 1 日起进入下一统计年度（与后端 StatYearPeriod.current 一致）。
 */
export function getCurrentStatYear(): number {
  const now = new Date()
  const year = now.getFullYear()
  return now.getMonth() === 11 ? year + 1 : year
}

/** 生成最近若干年的统计年度选项（降序） */
export function buildStatYearOptions(count = 10): string[] {
  const current = getCurrentStatYear()
  return Array.from({ length: count }, (_, i) => String(current - i))
}
