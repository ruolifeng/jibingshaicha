import { SPUTUM_STATUS_OPTIONS } from "@@/constants/disease"
import { resolveFirstTreatmentPlan, resolveImportFields } from "@@/utils/patient"

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

/** 首次随访方式展示（含「其他」手工录入） */
export function formatFirstVisitMethod(method?: string | null, other?: string | null): string {
  if (!method) return "-"
  if (method === "其他" && other?.trim()) return `其他（${other.trim()}）`
  return method
}
