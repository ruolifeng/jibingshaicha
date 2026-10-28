import { parsePatientTreatmentPlan, SPUTUM_STATUS_OPTIONS } from "@@/constants/disease"
import { isLevel3OrAboveRole, resolveFirstTreatmentPlan, resolveImportFields } from "@@/utils/patient"

/** 首次随访编号：8位数字 */
export const FIRST_VISIT_FORM_NO_PATTERN = /^\d{8}$/

export const FIRST_VISIT_FORM_NO_RULES = [
  { required: true, message: "请填写编号", trigger: "blur" },
  { pattern: FIRST_VISIT_FORM_NO_PATTERN, message: "编号须为8位数字", trigger: "blur" }
]

/** 限制编号输入为最多8位数字 */
export function sanitizeFirstVisitFormNo(value?: string | null): string {
  return String(value ?? "").replace(/\D/g, "").slice(0, 8)
}

/** 校验首次随访编号是否有效 */
export function isValidFirstVisitFormNo(value?: string | null): boolean {
  return FIRST_VISIT_FORM_NO_PATTERN.test(String(value ?? ""))
}

/** 首次随访化疗方案：关联病案「首次治疗方案」预填，已有值则不覆盖 */
export function applyFirstVisitChemotherapyDefault(
  form: { chemotherapy?: string, chemotherapyDetail?: string },
  patientRow: Record<string, any> | null | undefined
) {
  if (form.chemotherapy?.trim()) return
  const plan = resolveFirstTreatmentPlan(patientRow)
  if (!plan) return
  const parsed = parsePatientTreatmentPlan(plan)
  form.chemotherapy = parsed.treatmentPlan
  form.chemotherapyDetail = parsed.customPlanDetail
}

/** 将已存化疗方案字符串规范为下拉选项 + 详情（其它敏感方案） */
export function normalizeFirstVisitChemotherapyFields(
  form: { chemotherapy?: string, chemotherapyDetail?: string }
) {
  const parsed = parsePatientTreatmentPlan(form.chemotherapy, form.chemotherapyDetail)
  form.chemotherapy = parsed.treatmentPlan
  form.chemotherapyDetail = parsed.customPlanDetail
}

/**
 * 从患者病原学结果推导首次随访痰菌情况默认值：
 * - 病原学阳性 / 阳性 → 阳性
 * - 病原学阴性 / 阴性 → 阴性
 * - 结核性胸膜炎 → 取详情「0月序分子生物学结果」
 */
export function resolveFirstVisitSputumStatusDefault(
  patientRow: Record<string, any> | null | undefined
): string {
  if (!patientRow) return ""
  const fields = resolveImportFields(patientRow)
  const pathogen = String(
    patientRow.diagnosisResult
    || fields["病原学结果"]
    || fields["诊断结果"]
    || ""
  ).trim()
  if (!pathogen) return ""

  if (pathogen.includes("结核性胸膜炎")) {
    const molecular = fields["0月序分子生物学结果"]
      || fields["0月单分子生物学结果"]
      || ""
    return normalizeSputumStatusOption(molecular)
  }

  if (
    pathogen.includes("病原学阳性")
    || pathogen.includes("病原学结果阳性")
    || pathogen === "阳性"
  ) {
    return "阳性"
  }
  if (pathogen.includes("病原学阴性") || pathogen === "阴性") {
    return "阴性"
  }
  return ""
}

/** 首次随访痰菌情况：按病原学结果预填，已有值则不覆盖 */
export function applyFirstVisitSputumStatusDefault(
  form: { sputumStatus?: string },
  patientRow: Record<string, any> | null | undefined
) {
  if (form.sputumStatus?.trim()) return
  const value = resolveFirstVisitSputumStatusDefault(patientRow)
  if (value) form.sputumStatus = value
}

function normalizeSputumStatusOption(raw: string): string {
  const text = String(raw ?? "").trim()
  if (!text) return ""
  if ((SPUTUM_STATUS_OPTIONS as readonly string[]).includes(text)) return text
  if (text.includes("阳性")) return "阳性"
  if (text.includes("阴性")) return "阴性"
  if (text.includes("未查")) return "未查痰"
  return ""
}

/** 普通用户修改已完成首次随访的时限（月） */
export const FIRST_VISIT_EDIT_MONTHS_NORMAL = 1

function startOfLocalDay(date: Date): Date {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d
}

/** 解析日期/时间；纯日期按本地日历日解析，避免 UTC 日界偏移 */
function parseVisitAnchorDate(value?: string | null): Date | null {
  if (!value) return null
  const text = String(value).trim()
  const dateOnly = text.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (dateOnly) {
    return new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]))
  }
  const parsed = new Date(text.replace(" ", "T"))
  if (Number.isNaN(parsed.getTime())) return null
  return parsed
}

/**
 * 已完成首次随访是否可修改：
 * 三级及以上（role 1–4）不限；
 * 普通用户自随访时间（visitDate，缺省回退 createTime）起 1 个月内可改。
 * 后端 firstVisitEditable / editable 优先。
 */
export function canEditFirstVisit(
  userRole: number,
  visit: {
    status?: number | string
    visitDate?: string | null
    createTime?: string | null
    editable?: boolean | null
  } | null | undefined
): boolean {
  if (!visit) return false
  if (visit.editable != null) return visit.editable
  if (Number(visit.status) !== 1) return true
  if (isLevel3OrAboveRole(userRole)) return true

  const visitDate = parseVisitAnchorDate(visit.visitDate)
  if (visitDate) {
    const deadline = startOfLocalDay(visitDate)
    deadline.setMonth(deadline.getMonth() + FIRST_VISIT_EDIT_MONTHS_NORMAL)
    return startOfLocalDay(new Date()).getTime() <= deadline.getTime()
  }

  const created = parseVisitAnchorDate(visit.createTime)
  if (!created) return true
  const deadline = new Date(created)
  deadline.setMonth(deadline.getMonth() + FIRST_VISIT_EDIT_MONTHS_NORMAL)
  return Date.now() <= deadline.getTime()
}

/** 首次随访方式展示（含「其他」手工录入） */
export function formatFirstVisitMethod(method?: string | null, other?: string | null): string {
  if (!method) return "-"
  if (method === "其他" && other?.trim()) return `其他（${other.trim()}）`
  return method
}
