import {
  FOLLOW_UP_DRUG_FORM_OPTIONS,
  FOLLOW_UP_MEDICATION_USAGE_OPTIONS,
  FOLLOW_UP_SUPERVISOR_OPTIONS,
  FOLLOW_UP_SYMPTOM_OPTIONS,
  FOLLOW_UP_VISIT_METHOD_OPTIONS,
  FOLLOW_UP_VISIT_METHOD_OTHER,
  FOLLOW_UP_YES_NO_HAVE_OPTIONS,
  STOP_TREATMENT_REASON_OPTIONS
} from "@@/constants/disease"

interface Option { value: string, label: string }

export function optionLabel(options: Option[], value?: string | null) {
  if (!value) return "-"
  return options.find(o => o.value === value)?.label ?? value
}

export function formatFollowUpSymptoms(raw?: string | null) {
  if (!raw) return "-"
  const map = Object.fromEntries(FOLLOW_UP_SYMPTOM_OPTIONS.map(o => [o.value, o.label]))
  return raw
    .split(",")
    .map(s => map[s.trim()] ?? s.trim())
    .filter(Boolean)
    .join("、") || "-"
}

export function formatFollowUpSupervisor(value?: string | null, other?: string | null) {
  const label = optionLabel(FOLLOW_UP_SUPERVISOR_OPTIONS, value)
  if (value === "4" && other) return `${label}（${other}）`
  return label
}

export function formatYesNo(value?: string | null) {
  return optionLabel(FOLLOW_UP_YES_NO_HAVE_OPTIONS, value)
}

export function formatStopTreatmentReason(reason?: string | null, other?: string | null) {
  if (!reason) return "-"
  if (reason === "其它" && other) return `其它（${other}）`
  return optionLabel(STOP_TREATMENT_REASON_OPTIONS, reason)
}

export function formatFollowUpVisitMethod(value?: string | null, other?: string | null) {
  if (!value) return "-"
  if (value === FOLLOW_UP_VISIT_METHOD_OTHER && other?.trim()) {
    return `其他（${other.trim()}）`
  }
  return optionLabel(FOLLOW_UP_VISIT_METHOD_OPTIONS, value)
}

export const followUpFormatters = {
  visitMethod: (v?: string | null, other?: string | null) => formatFollowUpVisitMethod(v, other),
  medicationUsage: (v?: string | null) => optionLabel(FOLLOW_UP_MEDICATION_USAGE_OPTIONS, v),
  drugForm: (v?: string | null) => optionLabel(FOLLOW_UP_DRUG_FORM_OPTIONS, v),
  stopTreatmentReason: (v?: string | null, other?: string | null) => formatStopTreatmentReason(v, other)
}
