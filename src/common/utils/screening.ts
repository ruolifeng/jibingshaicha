const SCREEN_RESULT_UNIT = "mm*mm"
const PPD_DIMENSION_PATTERN = /\d+\s*[*×xX]\s*\d+/

/** 是否已包含 mm*mm / mm×mm 等单位 */
function hasScreenResultUnit(text: string): boolean {
  return /mm\s*[*×xX]\s*mm/i.test(text)
}

/** PPD 斑痕测量值（如 3*6、PPD: 3*6）展示时补充 mm*mm 单位 */
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
