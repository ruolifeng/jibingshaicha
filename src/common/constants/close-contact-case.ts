import { LATENT_TREATMENT_PLAN_OPTIONS } from "./disease"

/** 密接个案表 — 线上预览列定义（与官方 72 列 Excel 模板字段、顺序一致） */
export interface CloseContactCaseColumn {
  field: string
  title: string
  width: number
  fixed?: "left" | "right"
}

export const FINAL_SCREENING_OTHER = "其他（需注明）"

export const DIAGNOSIS_RESULT_OPTIONS = [
  { label: "未发现异常", value: "未发现异常" },
  { label: "活动性肺结核", value: "活动性肺结核" },
  { label: "疑似肺结核", value: "疑似肺结核" },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: FINAL_SCREENING_OTHER, value: FINAL_SCREENING_OTHER }
] as const

const FINAL_SCREENING_OTHER_PREFIX = `${FINAL_SCREENING_OTHER}：`

/** 回填最终筛查结果（含「其他」备注） */
export function applyFinalScreeningResult(
  form: { finalScreeningResult: string, finalScreeningRemark: string },
  value?: string | null
) {
  const raw = (value || "").trim()
  if (!raw) {
    form.finalScreeningResult = ""
    form.finalScreeningRemark = ""
    return
  }
  if (raw.startsWith(FINAL_SCREENING_OTHER_PREFIX)) {
    form.finalScreeningResult = FINAL_SCREENING_OTHER
    form.finalScreeningRemark = raw.slice(FINAL_SCREENING_OTHER_PREFIX.length).trim()
    return
  }
  if (raw === FINAL_SCREENING_OTHER || raw === "其他" || raw === "其它") {
    form.finalScreeningResult = FINAL_SCREENING_OTHER
    form.finalScreeningRemark = ""
    return
  }
  if (raw.startsWith("其他：") || raw.startsWith("其它：")) {
    form.finalScreeningResult = FINAL_SCREENING_OTHER
    form.finalScreeningRemark = raw.slice(3).trim()
    return
  }
  form.finalScreeningResult = raw
  form.finalScreeningRemark = ""
}

/** 保存时合并「其他」备注 */
export function resolveFinalScreeningResultForSave(result: string, remark?: string): string {
  const selected = (result || "").trim()
  if (selected !== FINAL_SCREENING_OTHER) return selected
  const detail = (remark || "").trim()
  return detail ? `${FINAL_SCREENING_OTHER_PREFIX}${detail}` : FINAL_SCREENING_OTHER
}

export function isFinalScreeningOther(result?: string | null): boolean {
  return (result || "").trim() === FINAL_SCREENING_OTHER
}
/** 报表填报季度（Q1-Q4） */
export const REPORT_QUARTER_OPTIONS = [
  { label: "Q1", value: "1" },
  { label: "Q2", value: "2" },
  { label: "Q3", value: "3" },
  { label: "Q4", value: "4" }
] as const

/** 是否开展预防治疗（与密接潜伏感染者流程一致） */
export const HAS_PREVENTIVE_TREATMENT_OPTIONS = [
  { label: "开展", value: "开展" },
  { label: "未开展", value: "未开展" }
] as const

/**
 * 预防性治疗方案（与潜伏感染者通知单一致，另含「其他」对应「其他方案，请备注」）
 */
export const PREVENTIVE_PLAN_OPTIONS = [
  ...LATENT_TREATMENT_PLAN_OPTIONS,
  "其他"
] as const

/** 编辑/新增表单不展示的系统列或 Excel 自动计算列 */
export const CLOSE_CONTACT_CASE_EDIT_EXCLUDE = new Set([
  "creatorUsername",
  "createTime",
  "reportQuarter",
  "registrationIntervalHint",
  "ageGroup"
])

/** 日期字段（编辑用 date-picker） */
export const CLOSE_CONTACT_CASE_DATE_FIELDS = new Set([
  "reportDate",
  "registrationDate",
  "firstScreenDate",
  "infectionCheckDate",
  "imagingDate",
  "sputumCheckDate",
  "followup6DueDate",
  "followup6ScreenDate",
  "followup6ImagingDate",
  "followup6SputumDate",
  "followup12DueDate",
  "followup12ScreenDate",
  "followup12ImagingDate",
  "followup12SputumDate",
  "followup24DueDate",
  "followup24ScreenDate",
  "followup24ImagingDate",
  "followup24SputumDate"
])

/** 编辑表单分组（覆盖表格全部业务列） */
export const CLOSE_CONTACT_CASE_EDIT_GROUPS: { key: string, label: string, fields: string[] }[] = [
  {
    key: "source",
    label: "原患者信息",
    fields: [
      "city",
      "district",
      "sourcePatientName",
      "sourcePatientCaseNo",
      "sourcePatientBacteriologyResult",
      "sourcePatientPhone",
      "reportDate",
      "registrationDate"
    ]
  },
  {
    key: "contact",
    label: "接触者基本信息",
    fields: [
      "name",
      "idNumber",
      "age",
      "phone",
      "gender",
      "ethnicity",
      "contactType",
      "contactPlace",
      "householdAddress",
      "currentAddress"
    ]
  },
  {
    key: "screen",
    label: "首次筛查与诊断",
    fields: [
      "firstScreenDate",
      "symptom1",
      "symptom2",
      "infectionCheckDate",
      "infectionCheckMethod",
      "infectionCheckResult",
      "imagingDate",
      "imagingMethod",
      "imagingResult",
      "sputumCheckDate",
      "sputumCheckMethod",
      "sputumCheckResult",
      "finalScreeningResult"
    ]
  },
  {
    key: "preventive",
    label: "预防治疗",
    fields: [
      "hasContraindication",
      "noTreatmentReason",
      "contraindicationRemark",
      "hasPreventiveTreatment",
      "preventivePlan",
      "preventivePlanRemark",
      "treatmentCompleted",
      "incompleteReason"
    ]
  },
  {
    key: "followup6",
    label: "6月随访",
    fields: [
      "followup6DueDate",
      "followup6ScreenDate",
      "followup6Symptom1",
      "followup6Symptom2",
      "followup6ImagingDate",
      "followup6ImagingMethod",
      "followup6ImagingResult",
      "followup6SputumDate",
      "followup6SputumMethod",
      "followup6SputumResult",
      "followup6Result"
    ]
  },
  {
    key: "followup12",
    label: "12月随访",
    fields: [
      "followup12DueDate",
      "followup12ScreenDate",
      "followup12Symptom1",
      "followup12Symptom2",
      "followup12ImagingDate",
      "followup12ImagingMethod",
      "followup12ImagingResult",
      "followup12SputumDate",
      "followup12SputumMethod",
      "followup12SputumResult",
      "followup12Result"
    ]
  },
  {
    key: "followup24",
    label: "24月随访",
    fields: [
      "followup24DueDate",
      "followup24ScreenDate",
      "followup24Symptom1",
      "followup24Symptom2",
      "followup24ImagingDate",
      "followup24ImagingMethod",
      "followup24ImagingResult",
      "followup24SputumDate",
      "followup24SputumMethod",
      "followup24SputumResult",
      "followup24Result"
    ]
  },
  {
    key: "remark",
    label: "备注",
    fields: ["remark"]
  }
]

/**
 * 电子表格预览列（顺序与 CloseContactCaseExcelHeaders / 官方模板一致）
 * 首尾追加系统字段：录入用户、录入时间
 */
export const CLOSE_CONTACT_CASE_COLUMNS: CloseContactCaseColumn[] = [
  { field: "creatorUsername", title: "录入用户", width: 90, fixed: "left" },
  { field: "city", title: "市/州（**市或**州）", width: 140 },
  { field: "district", title: "区/县（**区/县/市）", width: 140 },
  { field: "sourcePatientName", title: "患者姓名", width: 100 },
  { field: "sourcePatientCaseNo", title: "病案号", width: 180 },
  { field: "sourcePatientBacteriologyResult", title: "病原学结果", width: 160 },
  { field: "sourcePatientPhone", title: "患者电话", width: 120 },
  { field: "reportDate", title: "填表日期", width: 110 },
  { field: "registrationDate", title: "密切接触者登记日期（填写yyyy/mm/dd格式）", width: 260 },
  { field: "reportQuarter", title: "报表填报季度", width: 180 },
  { field: "registrationIntervalHint", title: "计算登记日期到当前日期的时间间隔，提示随访期限", width: 360 },
  { field: "name", title: "接触者姓名", width: 100 },
  { field: "idNumber", title: "身份证号", width: 170 },
  { field: "age", title: "年龄（岁）", width: 90 },
  { field: "ageGroup", title: "年龄组", width: 130 },
  { field: "phone", title: "接触者电话", width: 120 },
  { field: "contactType", title: "接触类型", width: 150 },
  { field: "contactPlace", title: "接触场所", width: 150 },
  { field: "firstScreenDate", title: "首次筛查日期", width: 120 },
  { field: "symptom1", title: "结核症状1", width: 160 },
  { field: "symptom2", title: "结核症状2（自行补充）", width: 160 },
  { field: "infectionCheckDate", title: "感染检测日期", width: 120 },
  { field: "infectionCheckMethod", title: "感染筛查方法", width: 180 },
  { field: "infectionCheckResult", title: "结果判定", width: 150 },
  { field: "imagingDate", title: "影像检查日期（填写yyyy/mm/dd格式）", width: 220 },
  { field: "imagingMethod", title: "影像方法", width: 150 },
  { field: "imagingResult", title: "影像结果", width: 150 },
  { field: "sputumCheckDate", title: "痰检留标日期（填写yyyy/mm/dd格式）", width: 220 },
  { field: "sputumCheckMethod", title: "痰检方法", width: 150 },
  { field: "sputumCheckResult", title: "痰检结果", width: 150 },
  { field: "finalScreeningResult", title: "最终筛查结果", width: 180 },
  { field: "hasContraindication", title: "有无禁忌症", width: 160 },
  { field: "noTreatmentReason", title: "不接受预防性治疗的原因", width: 240 },
  { field: "contraindicationRemark", title: "备注：其他原因和具体的禁忌症（自行填写）", width: 280 },
  { field: "hasPreventiveTreatment", title: "是否开展预防治疗", width: 200 },
  { field: "preventivePlan", title: "预防性治疗方案", width: 180 },
  { field: "preventivePlanRemark", title: "其他方案，请备注", width: 140 },
  { field: "treatmentCompleted", title: "是否完成治疗", width: 170 },
  { field: "incompleteReason", title: "若未完成预防性治疗请选择原因", width: 300 },
  { field: "followup6DueDate", title: "6月随访日期", width: 280 },
  { field: "followup6ScreenDate", title: "6月随访-症状筛查日期", width: 180 },
  { field: "followup6Symptom1", title: "6月随访-症状1", width: 200 },
  { field: "followup6Symptom2", title: "6月随访-症状2（自行填写）", width: 200 },
  { field: "followup6ImagingDate", title: "6月随访-影像检查日期", width: 180 },
  { field: "followup6ImagingMethod", title: "6月随访-影像检查方法", width: 240 },
  { field: "followup6ImagingResult", title: "6月随访-影像结果", width: 200 },
  { field: "followup6SputumDate", title: "6月随访-留标本时间", width: 160 },
  { field: "followup6SputumMethod", title: "6月随访-病原学检查方法", width: 260 },
  { field: "followup6SputumResult", title: "6月随访-病原学检查结果", width: 260 },
  { field: "followup6Result", title: "6月随访-筛查结果", width: 220 },
  { field: "followup12DueDate", title: "12月随访日期", width: 290 },
  { field: "followup12ScreenDate", title: "12月随访-症状筛查日期", width: 190 },
  { field: "followup12Symptom1", title: "12月随访-症状", width: 200 },
  { field: "followup12Symptom2", title: "12月随访-症状2（自行填写）", width: 200 },
  { field: "followup12ImagingDate", title: "12月随访-影像检查日期", width: 190 },
  { field: "followup12ImagingMethod", title: "12月随访-影像检查方法", width: 250 },
  { field: "followup12ImagingResult", title: "12月随访-影像结果", width: 210 },
  { field: "followup12SputumDate", title: "12月随访-留标本时间", width: 170 },
  { field: "followup12SputumMethod", title: "12月随访-病原学检查方法", width: 270 },
  { field: "followup12SputumResult", title: "12月随访-病原学检查结果", width: 270 },
  { field: "followup12Result", title: "12月随访-筛查结果", width: 230 },
  { field: "followup24DueDate", title: "24月随访日期", width: 290 },
  { field: "followup24ScreenDate", title: "24月随访-症状筛查日期", width: 190 },
  { field: "followup24Symptom1", title: "24月随访-症状", width: 200 },
  { field: "followup24Symptom2", title: "24月随访-症状2（自行填写）", width: 200 },
  { field: "followup24ImagingDate", title: "24月随访-影像检查日期", width: 190 },
  { field: "followup24ImagingMethod", title: "24月随访-影像检查方法", width: 250 },
  { field: "followup24ImagingResult", title: "24月随访-影像结果", width: 210 },
  { field: "followup24SputumDate", title: "24月随访-留标本时间", width: 170 },
  { field: "followup24SputumMethod", title: "24月随访-病原学检查方法", width: 270 },
  { field: "followup24SputumResult", title: "24月随访-病原学检查结果", width: 270 },
  { field: "followup24Result", title: "24月随访-筛查结果", width: 230 },
  { field: "remark", title: "备注", width: 150 },
  { field: "createTime", title: "录入时间", width: 160, fixed: "right" }
]

/** 字段名 → 列标题（编辑标签；含表格外扩展字段） */
export const CLOSE_CONTACT_CASE_FIELD_TITLE: Record<string, string> = {
  ...Object.fromEntries(CLOSE_CONTACT_CASE_COLUMNS.map(col => [col.field, col.title])),
  gender: "性别",
  ethnicity: "民族",
  householdAddress: "户籍地址",
  currentAddress: "现住址"
}

/** 编辑表单用较短标签（去掉括号说明） */
export function closeContactCaseFieldLabel(field: string): string {
  const title = CLOSE_CONTACT_CASE_FIELD_TITLE[field] || field
  return title.replace(/（[^）]*）/g, "").replace(/\([^)]*\)/g, "").trim() || title
}
