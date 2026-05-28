/** 推介/追踪 — 感染筛查方法 */
export const REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS = ["PPD", "EC", "IGRA", "未查"] as const

/** 推介/追踪 — 感染筛查结果 */
export const REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS = [
  "PPD阴性",
  "PPD+",
  "PPD++",
  "PPD+++",
  "EC阴性",
  "EC阳性",
  "IGRA阴性",
  "IGRA阳性",
  "无结果"
] as const

/** 推介/追踪 — 胸片筛查结果 */
export const REFERRAL_CHEST_XRAY_RESULT_OPTIONS = ["正常", "异常", "无结果"] as const

/** 下拉兼容历史值（如旧数据「未查」） */
export function referralSelectOptionsWithLegacy(
  options: readonly string[],
  current?: string
): string[] {
  if (current && !options.includes(current)) {
    return [current, ...options]
  }
  return [...options]
}
