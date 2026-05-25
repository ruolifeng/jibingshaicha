import { resolveFirstTreatmentPlan } from "@@/utils/patient"

/** 五级用户（role=6）已完成后续随访的可编辑天数 */
export const FOLLOW_UP_EDIT_DAYS_LEVEL5 = 10

/** 后续随访化疗方案：关联病案「首次治疗方案」预填，已有值则不覆盖 */
export function applyFollowUpChemotherapyDefault(
  form: { chemotherapyPlan?: string },
  patientRow: Record<string, any> | null | undefined
) {
  if (form.chemotherapyPlan?.trim()) return
  const plan = resolveFirstTreatmentPlan(patientRow)
  if (plan) form.chemotherapyPlan = plan
}

/** 停止治疗原因：转入耐多药治疗（不归档，可继续随访） */
export const STOP_TREATMENT_REASON_MDR = "转入耐多药治疗"

/** 停止治疗归档备注前缀 */
export const ARCHIVE_REMARK_STOP_TREATMENT_PREFIX = "停止治疗："

/** 停止治疗是否应归档（完成疗程/死亡/丢失/其它，不含转入耐多药治疗） */
export function shouldArchiveOnStopTreatment(stopTreatment: string, reason: string): boolean {
  return stopTreatment === "是" && !!reason && reason !== STOP_TREATMENT_REASON_MDR
}

/** 是否为停止治疗导致的归档 */
export function isStopTreatmentArchive(archiveRemark?: string | null): boolean {
  return !!archiveRemark?.startsWith(ARCHIVE_REMARK_STOP_TREATMENT_PREFIX)
}

/** 五级用户：已完成后续随访记录创建后 10 天内可修改；管理员（role≠6）随时可改 */
export function canEditFollowUpVisit(
  userRole: number,
  visit: { status?: number, createTime?: string | null, editable?: boolean | null } | null | undefined
): boolean {
  if (!visit) return false
  if (visit.editable != null) return visit.editable
  if (visit.status !== 1) return true
  if (userRole !== 6) return true
  if (!visit.createTime) return true
  const created = new Date(String(visit.createTime).replace(" ", "T"))
  if (Number.isNaN(created.getTime())) return true
  const deadline = new Date(created)
  deadline.setDate(deadline.getDate() + FOLLOW_UP_EDIT_DAYS_LEVEL5)
  return Date.now() <= deadline.getTime()
}
