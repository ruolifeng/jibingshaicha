import type { MedicationRecordsMap } from "@@/utils/medicationRecords"
import { getEarliestMedicationMarkedDate } from "@@/utils/medicationRecords"

/** 治疗记录卡管理方式固定值 */
export const MEDICATION_LOCKED_MANAGEMENT_METHOD = "全程管理"

/** 首次随访「痰菌情况」→ 治疗记录卡「治疗前痰菌检查」 */
export function mapFirstVisitSputumToMedication(sputumStatus?: string | null): string {
  if (!sputumStatus) return ""
  const mapping: Record<string, string> = {
    阳性: "阳性",
    阴性: "阴性",
    未查痰: "未检查"
  }
  return mapping[sputumStatus] ?? sputumStatus
}

/** 首次随访「督导人员」→ 服药管理「督导人员」 */
export function mapFirstVisitSupervisorToMedication(supervisor?: string | null): string {
  if (!supervisor) return ""
  const mapping: Record<string, string> = {
    医生: "医生",
    家属: "家属",
    自服药: "患者本人",
    其他: "其他"
  }
  return mapping[supervisor] ?? supervisor
}

export interface MedicationFormFields {
  managementMethod: string
  supervisor: string
  sputumResult: string
  startTreatmentDate: string
  stopDate: string
  dayMarks: MedicationRecordsMap
}

/** 根据服药日历最早标记日解析开始治疗日期 */
export function resolveStartTreatmentDate(
  stored: string | null | undefined,
  dayMarks: MedicationRecordsMap
): string {
  if (stored) return stored
  return getEarliestMedicationMarkedDate(dayMarks)
}

/**
 * 打开服药管理时填充默认值：管理方式锁定全程管理；
 * 督导人员、痰菌情况来自已完成首次随访；开始治疗日期来自已保存或日历。
 */
export function applyMedicationFormDefaults(
  form: MedicationFormFields,
  options: {
    saved?: Record<string, any> | null
    firstVisit?: Record<string, any> | null
  }
) {
  form.managementMethod = MEDICATION_LOCKED_MANAGEMENT_METHOD

  const saved = options.saved
  if (saved) {
    form.supervisor = saved.supervisor || form.supervisor
    form.sputumResult = saved.sputumResult || form.sputumResult
    form.stopDate = saved.stopDate || ""
    form.startTreatmentDate = resolveStartTreatmentDate(saved.startTreatmentDate, form.dayMarks)
  }

  const firstVisit = options.firstVisit
  if (firstVisit?.status === 1) {
    if (!form.supervisor && firstVisit.supervisor) {
      form.supervisor = mapFirstVisitSupervisorToMedication(firstVisit.supervisor)
    }
    if (!form.sputumResult && firstVisit.sputumStatus) {
      form.sputumResult = mapFirstVisitSputumToMedication(firstVisit.sputumStatus)
    }
  }

  if (!form.startTreatmentDate) {
    form.startTreatmentDate = getEarliestMedicationMarkedDate(form.dayMarks)
  }
}

/** 日历标记变化时同步开始治疗日期（未手改时取最早标记日） */
export function syncStartTreatmentDateFromMarks(
  form: Pick<MedicationFormFields, "startTreatmentDate" | "dayMarks">,
  manualEdited: boolean
) {
  if (manualEdited) return
  form.startTreatmentDate = getEarliestMedicationMarkedDate(form.dayMarks)
}

/** 下拉选项：保留已保存但不在标准选项中的历史值 */
export function medicationSelectOptions(options: string[], current?: string): string[] {
  if (!current || options.includes(current)) return options
  return [...options, current]
}
