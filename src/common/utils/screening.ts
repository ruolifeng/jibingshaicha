const SCREEN_RESULT_UNIT = "mm*mm"
const PPD_DIMENSION_PATTERN = /\d+\s*[*×x]\s*\d+/i
/** 人群分类「是」及历史 Excel 勾选符号 */
const CROWD_CATEGORY_YES_MARKS = new Set(["是", "√", "✓", "✔"])

/** 人群分类是否视为「是」（含历史勾选 √） */
export function isCrowdCategoryYes(value: unknown): boolean {
  if (value == null) return false
  const text = String(value).trim()
  return CROWD_CATEGORY_YES_MARKS.has(text)
}

/** 老年人/糖尿病等勾选符号展示为「是」 */
export function formatCrowdCategoryYesDisplay(value: unknown): string {
  if (value == null || value === "") return ""
  const text = String(value).trim()
  if (!text) return ""
  return isCrowdCategoryYes(text) ? "是" : text
}

/** 编辑回填：老年人/糖尿病勾选符号归一为「是」，避免下拉无匹配项 */
export function normalizeCrowdCategoryYesFields(row: Record<string, any>): Record<string, any> {
  const next = { ...row }
  if (isCrowdCategoryYes(next.crowdCategoryElder)) {
    next.crowdCategoryElder = "是"
  }
  if (isCrowdCategoryYes(next.crowdCategoryDiabetes)) {
    next.crowdCategoryDiabetes = "是"
  }
  return next
}

/**
 * 是否已包含单位：
 * - mm*mm / mm×mm（重点人群等）
 * - (mm) / （mm）（学生筛查官方表常见写法）
 */
function hasScreenResultUnit(text: string): boolean {
  return /mm\s*[*×x]\s*mm/i.test(text) || /[(（]\s*mm\s*[)）]/i.test(text)
}

/** PPD 斑痕测量值（如 3*6、PPD: 3*6）展示时补充 mm*mm 单位；已带 (mm) 等则不再追加 */
export function formatScreenResultDisplay(
  value: unknown,
  screenMethod?: unknown
): string {
  if (value == null || value === "") return ""
  const text = String(value).trim()
  if (!text || hasScreenResultUnit(text)) return text

  const method = screenMethod != null ? String(screenMethod) : ""
  const isPpd = /PPD/i.test(text) || /PPD/i.test(method)
  if (!isPpd || !PPD_DIMENSION_PATTERN.test(text)) return text

  return `${text} (${SCREEN_RESULT_UNIT})`
}
