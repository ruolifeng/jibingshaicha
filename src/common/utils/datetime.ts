import dayjs from "dayjs"

const INVALID_DATE = "N/A"

/** 格式化日期时间 */
export function formatDateTime(datetime: string | number | Date = "", template: string = "YYYY-MM-DD HH:mm:ss") {
  const day = dayjs(datetime)
  return day.isValid() ? day.format(template) : INVALID_DATE
}

/**
 * 将后端返回的日期时间字符串格式化为友好显示格式
 * 空值返回 "-"，有效值返回 "YYYY-MM-DD HH:mm"
 */
export function fmtDate(value: string | number | Date | null | undefined): string {
  if (!value) return "-"
  const day = dayjs(value)
  return day.isValid() ? day.format("YYYY-MM-DD HH:mm") : "-"
}
