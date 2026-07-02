import { resolveFirstTreatmentPlan } from "@@/utils/patient"

/** 五级用户（role=6）已完成后续随访的可编辑天数 */
export const FOLLOW_UP_EDIT_DAYS_LEVEL5 = 10

export interface FollowUpCaseClosureStats {
  actualVisitCount: number
  actualDoseCount: number
}

/** 草稿转完成或新建时，当前随访尚未计入已完成列表 */
export function shouldIncludeCurrentFollowUpInStats(
  initialData?: { id?: number, status?: number } | null
): boolean {
  if (!initialData?.id) return true
  return initialData.status !== 1
}

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

/**
 * 后续随访「查看记录」列表中的「下次随访」展示值（不含首次随访本身）：
 * - 第 1 次后续随访：取首次随访填写的下次随访时间
 * - 第 N 次（N>1）：取上一次后续随访记录中填写的下次随访时间
 */
export function resolveFollowUpListNextVisitDate(
  list: Array<{ nextVisitDate?: string | null }>,
  index: number,
  firstVisitNextDate?: string | null
): string {
  if (index === 0) {
    const fromFirstVisit = firstVisitNextDate?.trim()
    if (fromFirstVisit) return fromFirstVisit
    return list[0]?.nextVisitDate?.trim() || ""
  }
  const fromPrevious = list[index - 1]?.nextVisitDate?.trim()
  if (fromPrevious) return fromPrevious
  return list[index]?.nextVisitDate?.trim() || ""
}

/** 单条后续随访记录的「下次随访」展示值（与列表列逻辑一致） */
export function resolveFollowUpRecordNextVisitDate(
  visit: { visitSeq?: number | null, nextVisitDate?: string | null },
  list: Array<{ nextVisitDate?: string | null }>,
  firstVisitNextDate?: string | null
): string {
  const index = visit.visitSeq != null ? Math.max(visit.visitSeq - 1, 0) : 0
  return resolveFollowUpListNextVisitDate(list, index, firstVisitNextDate)
}

/** 新建后续随访时，「下次随访时间」默认值（关联首次随访或上一次后续随访） */
export function resolveFollowUpFormDefaultNextVisitDate(
  completedList: Array<{ nextVisitDate?: string | null }>,
  firstVisitNextDate?: string | null
): string {
  return resolveFollowUpListNextVisitDate(completedList, completedList.length, firstVisitNextDate)
}
