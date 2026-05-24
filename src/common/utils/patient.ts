import {
  EPIDEMIC_REPORT_HEADERS,
  PRIORITY_DETAIL_FIELDS,
  SPECIAL_DISEASE_HEADERS
} from "@@/constants/patient-import"

/** 从 patient.epidemicData JSON 中读取专病网/大疫情导入的扩展字段 */
export function parseEpidemicDataField(
  epidemicData: string | Record<string, unknown> | null | undefined,
  field: string
): string {
  if (!epidemicData) return ""
  try {
    const obj = typeof epidemicData === "string" ? JSON.parse(epidemicData) : epidemicData
    const val = obj?.[field]
    return val != null ? String(val) : ""
  } catch {
    return ""
  }
}

/** 解析专病网导入的人群分类（优先 API 填充字段，回退 epidemicData；过滤非标准选项） */
export function resolvePatientCrowdCategory(
  row: Record<string, any> | null | undefined,
  options: readonly string[]
): string {
  if (!row) return ""
  const raw = row.crowdCategory || parseEpidemicDataField(row.epidemicData, "人群分类")
  return raw && options.includes(raw) ? raw : ""
}

/** 解析专病网导入的现管单位 */
export function resolvePatientCurrentUnit(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  return row.currentManagementUnit
    || parseEpidemicDataField(row.epidemicData, "现管单位")
    || parseEpidemicDataField(row.epidemicData, "现管理单位")
}

/** 解析治疗分类 */
export function resolveTreatmentClass(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  return row.treatmentClass
    || row.importFields?.["治疗分类"]
    || parseEpidemicDataField(row.epidemicData, "治疗分类")
}

/** 是否为复治患者（治疗分类含「复治」） */
export function isRetreatmentPatient(row: Record<string, any> | null | undefined): boolean {
  const tc = resolveTreatmentClass(row)
  return !!tc && tc.includes("复治")
}

/** 获取导入字段 Map（优先 API 返回的 importFields，兼容 legacy 列索引 JSON） */
export function resolveImportFields(row: Record<string, any> | null | undefined): Record<string, string> {
  if (!row) return {}
  if (row.importFields && typeof row.importFields === "object" && Object.keys(row.importFields).length) {
    return row.importFields as Record<string, string>
  }
  if (!row.epidemicData) return {}
  try {
    const raw = typeof row.epidemicData === "string" ? JSON.parse(row.epidemicData) : row.epidemicData
    if (!raw || typeof raw !== "object") return {}
    const keys = Object.keys(raw)
    const indexKeys = keys.length > 0 && keys.every(k => /^\d+$/.test(k))
    if (indexKeys) {
      const headers = row.populationType === "specialDisease"
        ? SPECIAL_DISEASE_HEADERS
        : row.populationType === "epidemic"
          ? EPIDEMIC_REPORT_HEADERS
          : []
      const result: Record<string, string> = {}
      keys.sort((a, b) => Number(a) - Number(b)).forEach((k) => {
        const idx = Number(k)
        const val = raw[k]
        if (headers[idx] && val != null && String(val).trim()) {
          result[headers[idx]] = String(val).trim()
        }
      })
      return result
    }
    const result: Record<string, string> = {}
    for (const [k, v] of Object.entries(raw)) {
      if (v != null && String(v).trim()) result[k] = String(v).trim()
    }
    return result
  } catch {
    return {}
  }
}

/** 按表头顺序整理详情展示字段 */
export function buildOrderedImportFields(
  row: Record<string, any> | null | undefined
): Array<{ label: string, value: string }> {
  const fields = resolveImportFields(row)
  if (!fields || !Object.keys(fields).length) return []

  const headerOrder = row?.populationType === "specialDisease"
    ? SPECIAL_DISEASE_HEADERS
    : row?.populationType === "epidemic"
      ? EPIDEMIC_REPORT_HEADERS
      : []

  const ordered: Array<{ label: string, value: string }> = []
  const used = new Set<string>()

  for (const label of headerOrder) {
    const value = fields[label]
    if (value) {
      ordered.push({ label, value })
      used.add(label)
    }
  }
  for (const [label, value] of Object.entries(fields)) {
    if (!used.has(label) && value) {
      ordered.push({ label, value })
    }
  }
  return ordered
}

/** 详情优先展示字段（专病网标红部分） */
export function buildPriorityImportFields(
  row: Record<string, any> | null | undefined
): Array<{ label: string, value: string }> {
  const fields = resolveImportFields(row)
  return PRIORITY_DETAIL_FIELDS.map((label) => ({
    label,
    value: resolvePriorityFieldValue(fields, row, label)
  }))
}

function resolvePriorityFieldValue(
  fields: Record<string, string>,
  row: Record<string, any> | null | undefined,
  label: string
): string {
  if (label === "病原学结果") {
    return fields["病原学结果"] || fields["诊断结果"] || row?.diagnosisResult || ""
  }
  return fields[label] || resolveFieldFromPatient(row, label) || ""
}

/** 解析登记号（来自病案信息/专病网导入） */
export function resolveRegistrationNo(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  return row.registrationNo
    || row.importFields?.["登记号"]
    || parseEpidemicDataField(row.epidemicData, "登记号")
}

/** 解析服药管理单位（来自病案信息/专病网导入） */
export function resolveMedicationManagementUnit(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  return row.noticeMedicationUnit
    || row.importFields?.["服药管理单位"]
    || parseEpidemicDataField(row.epidemicData, "服药管理单位")
}

/** 解析首次治疗方案（来自病案信息/专病网导入） */
export function resolveFirstTreatmentPlan(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  const fields = resolveImportFields(row)
  return fields["首次治疗方案"] || parseEpidemicDataField(row.epidemicData, "首次治疗方案")
}

/** 格式化通知管理时间（下发通知单时间） */
export function formatNoticeSentTime(value: unknown): string {
  if (value == null || value === "") return ""
  const text = typeof value === "string" ? value : String(value)
  return text.replace("T", " ").slice(0, 19)
}

const NOTICE_RECEIVE_TIMEOUT_MS = 72 * 60 * 60 * 1000

function parseNoticeDateTime(value: unknown): number | null {
  if (value == null || value === "") return null
  const text = String(value).replace(" ", "T")
  const time = new Date(text).getTime()
  return Number.isNaN(time) ? null : time
}

/** 通知单已下发超过 72 小时且未接收 */
export function isNoticeReceiveOverdue(row: Record<string, any> | null | undefined): boolean {
  if (!row || row.noticeStatus !== 1) return false
  const sentAt = parseNoticeDateTime(row.noticeSentTime)
  if (sentAt == null) return false
  return Date.now() - sentAt > NOTICE_RECEIVE_TIMEOUT_MS
}

/** 通知单管理列表：已接收显示接收时间，已下发未接收显示下发时间 */
export function resolveNoticeManageTime(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  if (row.noticeStatus === 2) {
    return formatNoticeSentTime(row.noticeConfirmedTime) || formatNoticeSentTime(row.noticeSentTime)
  }
  if (row.noticeStatus === 1) {
    return formatNoticeSentTime(row.noticeSentTime)
  }
  return ""
}

/** 部分重点字段在 patient 主表也有对应值 */
function resolveFieldFromPatient(row: Record<string, any> | null | undefined, label: string): string {
  if (!row) return ""
  if (label === "病原学结果" || label === "诊断结果") return row.diagnosisResult || ""
  return ""
}

/** 手动新增/编辑表单：从 patient 记录解析病案扩展字段 */
export function resolveManualEpidemicFormFields(row: Record<string, any> | null | undefined) {
  const fields = resolveImportFields(row)
  return {
    registrationNo: fields["登记号"] || "",
    contactName: fields["联系人姓名"] || "",
    contactRelation: fields["联系人监护人与本人关系"] || "",
    contactGuardianPhone: fields["联系人监护人电话号码"] || "",
    comorbidity: fields["合并症"] || "",
    treatmentClass: fields["治疗分类"] || row?.treatmentClass || "",
    medicationManagementUnit: fields["服药管理单位"] || "",
    patientRemark: fields["备注"] || "",
    firstTreatmentPlan: fields["首次治疗方案"] || "",
    drugSensitivityR: fields["药敏结果：利福平（R）"] || "",
    drugSensitivityH: fields["药敏结果：异烟肼（H）"] || ""
  }
}
