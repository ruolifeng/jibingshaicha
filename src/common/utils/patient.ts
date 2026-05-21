/** 从 patient.epidemicData JSON 中读取专病网导入的扩展字段 */
export function parseEpidemicDataField(
  epidemicData: string | Record<string, unknown> | null | undefined,
  field: string
): string {
  if (!epidemicData) return ""
  try {
    const obj = typeof epidemicData === "string" ? JSON.parse(epidemicData) : epidemicData
    const val = obj?.[field]
    return val != null ? String(val) : ""
  } catch {
    return ""
  }
}

/** 解析专病网导入的人群分类（优先 API 填充字段，回退 epidemicData；过滤非标准选项） */
export function resolvePatientCrowdCategory(
  row: Record<string, any> | null | undefined,
  options: readonly string[]
): string {
  if (!row) return ""
  const raw = row.crowdCategory || parseEpidemicDataField(row.epidemicData, "人群分类")
  return raw && options.includes(raw) ? raw : ""
}

/** 解析专病网导入的现管单位 */
export function resolvePatientCurrentUnit(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  return row.currentManagementUnit || parseEpidemicDataField(row.epidemicData, "现管单位")
}
