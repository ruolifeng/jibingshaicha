/** 密接筛查 — 接触类型 */
export const CONTACT_TYPE_OPTIONS = ["家庭内", "家庭外"] as const

/** 密接筛查 — 接触场所（含其他需手工录入） */
export const CONTACT_PLACE_OTHER = "其他（需手工录入）"

export const CONTACT_PLACE_OPTIONS = [
  "学校",
  "住所",
  "监管场所",
  "福利机构",
  "精神病院",
  "工矿企业",
  "娱乐场所",
  CONTACT_PLACE_OTHER
] as const

/** 初次筛查 — 通用「其他」选项文案 */
export const SCREENING_FIELD_OTHER = "其他（需手工录入）"

/** 结核症状1 */
export const CC_SYMPTOM1_OPTIONS = [
  "无症状",
  "未问诊",
  "咯血或血痰",
  "咳嗽咳痰<2周",
  "咳嗽咳痰≥2周",
  "胸闷",
  "胸痛",
  "低热",
  "盗汗"
] as const

/** 感染检测方法 */
export const CC_INFECTION_CHECK_METHOD_OPTIONS = [
  "结核菌素皮肤试验_PPD",
  "结核抗原皮肤试验_EC",
  "γ干扰素释放试验_IGRA",
  "未做"
] as const

/** 影像方法 */
export const CC_IMAGING_METHOD_OPTIONS = [
  "胸部X光片",
  "胸部CT",
  "未查",
  SCREENING_FIELD_OTHER
] as const

/** 影像结果 */
export const CC_IMAGING_RESULT_OPTIONS = [
  "未见异常",
  "异常(疑似活动性结核病变)",
  "异常(非活动性结核病变)",
  "未查",
  SCREENING_FIELD_OTHER
] as const

/** 痰检方法 */
export const CC_SPUTUM_METHOD_OPTIONS = [
  "涂片",
  "培养",
  "分子生物学",
  "未查",
  SCREENING_FIELD_OTHER
] as const

/** 痰检结果 */
export const CC_SPUTUM_RESULT_OPTIONS = [
  "阴性",
  "阳性",
  "污染",
  "未查",
  SCREENING_FIELD_OTHER
] as const

/** 最终筛查结果 */
export const CC_FINAL_SCREENING_RESULT_OPTIONS = [
  "未发现异常",
  "活动性肺结核",
  "疑似肺结核",
  "潜伏感染者",
  "未做",
  SCREENING_FIELD_OTHER
] as const

export type FinalResultTagType = "primary" | "success" | "info" | "warning" | "danger"

/** 最终筛查结果 — 展示标签颜色（活动性肺结核标红、疑似肺结核标黄） */
export const CC_FINAL_RESULT_TAG_MAP: Record<string, FinalResultTagType> = {
  活动性肺结核: "danger",
  疑似肺结核: "warning",
  潜伏感染者: "warning",
  未发现异常: "success",
  未做: "info"
}

/** 首页统计卡片展示的最终筛查结果 */
export const CC_FINAL_RESULT_STAT_OPTIONS: { label: string, value: string, type: FinalResultTagType }[] = [
  { label: "活动性肺结核", value: "活动性肺结核", type: "danger" },
  { label: "疑似肺结核", value: "疑似肺结核", type: "warning" },
  { label: "潜伏感染者", value: "潜伏感染者", type: "warning" },
  { label: "未发现异常", value: "未发现异常", type: "success" },
  { label: "未做", value: "未做", type: "info" }
]

/** 展示接触场所（其他时拼接手工录入内容） */
export function formatContactPlace(place?: string, other?: string): string {
  return formatFieldWithOther(place, other)
}

/** 展示带「其他」手工录入的字段 */
export function formatFieldWithOther(value?: string, other?: string): string {
  if (!value) return ""
  if (value === SCREENING_FIELD_OTHER && other?.trim()) {
    return `${value}：${other.trim()}`
  }
  return value
}

/** 获取最终筛查结果标签类型 */
export function getFinalScreeningResultTagType(result?: string): FinalResultTagType {
  if (!result) return "info"
  if (result === SCREENING_FIELD_OTHER) return "info"
  return CC_FINAL_RESULT_TAG_MAP[result] || "info"
}

/** 下拉兼容历史自由文本 */
export function selectOptionsWithLegacy<T extends string>(
  options: readonly T[],
  current?: string
): string[] {
  if (current && !(options as readonly string[]).includes(current)) {
    return [current, ...options]
  }
  return [...options]
}

/** 保存前清理未选「其他」时的补充字段 */
export function sanitizeScreeningOtherFields(form: Record<string, any>): Record<string, any> {
  const pairs: [string, string][] = [
    ["contactPlace", "contactPlaceOther"],
    ["imagingMethod", "imagingMethodOther"],
    ["imagingResult", "imagingResultOther"],
    ["sputumCheckMethod", "sputumCheckMethodOther"],
    ["sputumCheckResult", "sputumCheckResultOther"],
    ["finalScreeningResult", "finalScreeningResultOther"]
  ]
  const out = { ...form }
  for (const [main, other] of pairs) {
    const otherKey = other
    const mainVal = out[main]
    const useContactOther = main === "contactPlace"
    const otherConst = useContactOther ? CONTACT_PLACE_OTHER : SCREENING_FIELD_OTHER
    out[otherKey] = mainVal === otherConst ? String(out[otherKey] || "").trim() : ""
  }
  return out
}
