import { parsePatientTreatmentPlan, SPUTUM_STATUS_OPTIONS } from "@@/constants/disease"
import { resolveFirstTreatmentPlan, resolveImportFields } from "@@/utils/patient"

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

/** 首次随访已完成记录可随时修改（仍受权限控制） */
export function canEditFirstVisit(
  _userRole: number,
  _visit: { status?: number, createTime?: string | null } | null | undefined
): boolean {
  return true
}

/** 首次随访方式展示（含「其他」手工录入） */
export function formatFirstVisitMethod(method?: string | null, other?: string | null): string {
  if (!method) return "-"
  if (method === "其他" && other?.trim()) return `其他（${other.trim()}）`
  return method
}
