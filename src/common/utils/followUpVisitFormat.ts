import {
  FOLLOW_UP_DRUG_FORM_OPTIONS,
  FOLLOW_UP_MEDICATION_USAGE_OPTIONS,
  FOLLOW_UP_SUPERVISOR_OPTIONS,
  FOLLOW_UP_SYMPTOM_OPTIONS,
  FOLLOW_UP_VISIT_METHOD_OPTIONS,
  STOP_TREATMENT_REASON_OPTIONS,
  YES_NO_OPTIONS
} from "@@/constants/disease"

type Option = { value: string, label: string }

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
  return optionLabel(YES_NO_OPTIONS, value)
}

export const followUpFormatters = {
  visitMethod: (v?: string | null) => optionLabel(FOLLOW_UP_VISIT_METHOD_OPTIONS, v),
  medicationUsage: (v?: string | null) => optionLabel(FOLLOW_UP_MEDICATION_USAGE_OPTIONS, v),
  drugForm: (v?: string | null) => optionLabel(FOLLOW_UP_DRUG_FORM_OPTIONS, v),
  stopTreatmentReason: (v?: string | null) => optionLabel(STOP_TREATMENT_REASON_OPTIONS, v)
}
