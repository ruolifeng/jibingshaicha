/** 每日服药标记：x=标 X，circled=圈 X，空白=未标记 */
export type MedicationDayMark = "x" | "circled"

export type MedicationRecordsMap = Record<string, MedicationDayMark>

export const MEDICATION_MARK_X_CHAR = "×"
export const MEDICATION_MARK_CIRCLED_CHAR = "Ⓧ"

/** 解析服药记录（兼容旧版日期数组与新版对象） */
export function parseMedicationRecords(raw: unknown): MedicationRecordsMap {
  if (!raw) return {}

  let parsed: unknown = raw
  if (typeof raw === "string") {
    const trimmed = raw.trim()
    if (!trimmed) return {}
    try {
      parsed = JSON.parse(trimmed)
    } catch {
      return {}
    }
  }

  if (Array.isArray(parsed)) {
    const map: MedicationRecordsMap = {}
    for (const item of parsed) {
      if (typeof item === "string" && /^\d{4}-\d{2}-\d{2}$/.test(item)) {
        map[item] = "x"
      }
    }
    return map
  }

  if (parsed && typeof parsed === "object") {
    const map: MedicationRecordsMap = {}
    for (const [date, mark] of Object.entries(parsed as Record<string, unknown>)) {
      if (mark === "x" || mark === "circled") {
        map[date] = mark
      }
    }
    return map
  }

  return {}
}

/** 序列化为 JSON 字符串（无标记时返回空字符串） */
export function serializeMedicationRecords(marks: MedicationRecordsMap): string {
  const entries = Object.entries(marks)
    .filter(([, mark]) => mark === "x" || mark === "circled")
    .sort(([a], [b]) => a.localeCompare(b))
  if (!entries.length) return ""
  return JSON.stringify(Object.fromEntries(entries))
}

/** 三态循环：空白 → X → Ⓧ → 空白 */
export function toggleMedicationDayMark(marks: MedicationRecordsMap, date: string): MedicationRecordsMap {
  const next = { ...marks }
  const current = next[date]
  if (!current) {
    next[date] = "x"
  } else if (current === "x") {
    next[date] = "circled"
  } else {
    delete next[date]
  }
  return next
}

export function getMedicationDayMark(marks: MedicationRecordsMap, date: string): MedicationDayMark | "" {
  return marks[date] || ""
}

export function formatMedicationDayMark(mark: MedicationDayMark | ""): string {
  if (mark === "x") return MEDICATION_MARK_X_CHAR
  if (mark === "circled") return MEDICATION_MARK_CIRCLED_CHAR
  return ""
}

/** 统计有标记的天数（x 与 Ⓧ 均计入） */
export function countMedicationMarkedDays(marks: MedicationRecordsMap, year?: number): number {
  return Object.entries(marks).filter(([date, mark]) => {
    if (mark !== "x" && mark !== "circled") return false
    if (year && !date.startsWith(String(year))) return false
    return true
  }).length
}

/** 获取最早有标记的服药日期（YYYY-MM-DD） */
export function getEarliestMedicationMarkedDate(marks: MedicationRecordsMap): string {
  return Object.entries(marks)
    .filter(([, mark]) => mark === "x" || mark === "circled")
    .map(([date]) => date)
    .sort()[0] || ""
}

/** 从标记记录中提取年份列表 */
export function getMedicationRecordYears(marks: MedicationRecordsMap): number[] {
  const years = new Set<number>()
  for (const date of Object.keys(marks)) {
    const y = Number(date.split("-")[0])
    if (!Number.isNaN(y) && y > 2000) years.add(y)
  }
  if (!years.size) years.add(new Date().getFullYear())
  return Array.from(years).sort((a, b) => a - b)
}
