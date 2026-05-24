import { resolveFirstTreatmentPlan } from "@@/utils/patient"

/** 五级用户（role=6）已完成首次随访的可编辑天数 */
export const FIRST_VISIT_EDIT_DAYS_LEVEL5 = 10

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
  form: { chemotherapy?: string },
  patientRow: Record<string, any> | null | undefined
) {
  if (form.chemotherapy?.trim()) return
  const plan = resolveFirstTreatmentPlan(patientRow)
  if (plan) form.chemotherapy = plan
}

/** 五级用户：已完成首次随访记录创建后 10 天内可修改；管理员（role≠6）随时可改 */
export function canEditFirstVisit(
  userRole: number,
  visit: { status?: number, createTime?: string | null } | null | undefined
): boolean {
  if (!visit || visit.status !== 1) return true
  if (userRole !== 6) return true
  if (!visit.createTime) return true
  const created = new Date(String(visit.createTime).replace(" ", "T"))
  if (Number.isNaN(created.getTime())) return true
  const deadline = new Date(created)
  deadline.setDate(deadline.getDate() + FIRST_VISIT_EDIT_DAYS_LEVEL5)
  return Date.now() <= deadline.getTime()
}
