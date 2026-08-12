/** 督导表需从潜伏档案 / 筛查回填的身份字段 */
const SUPERVISION_PROFILE_KEYS = [
  "idNumber",
  "ethnicity",
  "birthDate",
  "householdAddress",
  "currentAddress",
  "gender",
  "phone",
  "phoneRemark"
] as const

function firstNonEmpty(...values: unknown[]): string {
  for (const value of values) {
    if (value == null) continue
    const text = String(value).trim()
    if (text) return text
  }
  return ""
}

/** 用潜伏档案补齐督导表空缺的身份信息（民族/出生日期等可能来自筛查回填） */
export function mergeSupervisionProfileFields(
  formData: Record<string, any> | null | undefined,
  latentProfile: Record<string, any> | null | undefined
): Record<string, any> {
  const form = { ...(formData || {}) }
  if (!latentProfile) return form
  for (const key of SUPERVISION_PROFILE_KEYS) {
    form[key] = firstNonEmpty(form[key], latentProfile[key])
  }
  if (form.age == null || form.age === "") {
    form.age = latentProfile.age ?? form.age
  }
  if (!form.patientName) {
    form.patientName = firstNonEmpty(form.patientName, latentProfile.name, latentProfile.patientName)
  }
  if (!form.category) {
    form.category = firstNonEmpty(form.category, latentProfile.crowdCategory, latentProfile.category)
  }
  return form
}

/** 已提交督导表可随时修改（已归档除外；后端 editable 优先） */
export function canEditSupervisionForm(
  _userRole: number | null | undefined,
  record: { status?: number, createTime?: string | null, editable?: boolean | null }
) {
  if (!record || record.status !== 1) return false
  if (record.editable != null) return record.editable
  return true
}

export function getSupervisionStatusLabel(status?: number) {
  if (status === 2) return "已归档"
  if (status === 1) return "已提交"
  return "待填写"
}

export function getSupervisionRecordStatusLabel(status?: number) {
  if (status === 2) return "已归档"
  if (status === 1) return "已提交"
  return "草稿"
}
