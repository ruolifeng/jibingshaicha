import type { MedicationRecordsMap } from "@@/utils/medicationRecords"
import { SPUTUM_RESULT_OPTIONS } from "@@/constants/disease"
import { getEarliestMedicationMarkedDate } from "@@/utils/medicationRecords"
import { resolveImportFields, resolvePatientPathogenResult } from "@@/utils/patient"

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

/** 患者「病原学结果」→ 治疗记录卡「治疗前痰菌检查」 */
export function mapPathogenResultToMedicationSputum(pathogenResult?: string | null): string {
  if (!pathogenResult) return ""
  const text = String(pathogenResult).trim()
  if (!text) return ""
  if ((SPUTUM_RESULT_OPTIONS as readonly string[]).includes(text)) return text
  if (text.includes("阳性")) return "阳性"
  if (text.includes("阴性")) return "阴性"
  if (text.includes("未出") || text.includes("未知")) return "无结果"
  if (text.includes("未做") || text.includes("未查")) return "未检查"
  return ""
}

/** 从患者行解析治疗前痰菌检查默认值（与服药管理列表「病原学结果」口径一致） */
export function resolveMedicationSputumFromPatient(
  patientRow?: Record<string, any> | null
): string {
  if (!patientRow) return ""
  const pathogen = resolvePatientPathogenResult(patientRow)
  if (pathogen.includes("结核性胸膜炎")) {
    const fields = resolveImportFields(patientRow)
    const molecular = fields["0月序分子生物学结果"] || fields["0月单分子生物学结果"] || ""
    return mapPathogenResultToMedicationSputum(molecular)
  }
  return mapPathogenResultToMedicationSputum(pathogen)
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

/** 督导表「督导管理人员」→ 服药管理「督导人员」 */
export function mapSupervisionManagerToMedication(managerType?: string | null): string {
  if (!managerType) return ""
  const mapping: Record<string, string> = {
    医务人员: "医生",
    家庭成员: "家属",
    志愿者: "志愿者",
    患者本人: "患者本人",
    其他: "其他"
  }
  return mapping[managerType] ?? managerType
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
 * 督导人员来自已完成首次随访；治疗前痰菌检查优先取病原学结果；
 * 开始治疗日期来自已保存或日历。
 */
export function applyMedicationFormDefaults(
  form: MedicationFormFields,
  options: {
    saved?: Record<string, any> | null
    firstVisit?: Record<string, any> | null
    patientRow?: Record<string, any> | null
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

  if (!form.sputumResult) {
    form.sputumResult = resolveMedicationSputumFromPatient(options.patientRow)
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

/**
 * 潜伏感染者服药管理默认值：优先已保存记录，其次督导表
 * （开始治疗日期 / 停止完成时间 / 督导人员）。
 */
export function applyLatentMedicationFormDefaults(
  form: MedicationFormFields,
  options: {
    saved?: Record<string, any> | null
    supervision?: Record<string, any> | null
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

  const supervision = options.supervision
  if (supervision) {
    if (!form.supervisor && supervision.managerType) {
      form.supervisor = mapSupervisionManagerToMedication(supervision.managerType)
    }
    if (!form.startTreatmentDate && supervision.treatmentStartDate) {
      form.startTreatmentDate = supervision.treatmentStartDate
    }
    if (!form.stopDate && supervision.treatmentEndDate) {
      form.stopDate = supervision.treatmentEndDate
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
