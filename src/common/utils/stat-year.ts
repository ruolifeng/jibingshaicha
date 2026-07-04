/**
 * 统计年度：自然年 1/1 — 12/31，statYear 即自然年（与后端 StatYearPeriod 一致）。
 */
export function getCurrentStatYear(): number {
  return new Date().getFullYear()
}

/** 生成最近若干年的统计年度选项（降序） */
export function buildStatYearOptions(count = 10): string[] {
  const current = getCurrentStatYear()
  return Array.from({ length: count }, (_, i) => String(current - i))
}
