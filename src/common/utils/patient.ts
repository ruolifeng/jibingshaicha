import { PATHOGEN_RESULT_OPTIONS, PATIENT_TYPE_OPTIONS } from "@@/constants/disease"
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

/** 解析民族（与患者详情一致：主表 + 病案导入） */
export function resolvePatientEthnicity(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  if (row.ethnicity?.trim()) return String(row.ethnicity).trim()
  const fields = resolveImportFields(row)
  return fields["民族"] || parseEpidemicDataField(row.epidemicData, "民族")
}

/** 解析通知单患者类型（来自详情「治疗分类」，映射为初治/复治） */
export function resolveNoticePatientType(row: Record<string, any> | null | undefined): string {
  const tc = resolveTreatmentClass(row)
  if (!tc) return ""
  if (tc.includes("复治")) return "复治"
  if (tc.includes("初治")) return "初治"
  return (PATIENT_TYPE_OPTIONS as readonly string[]).includes(tc) ? tc : ""
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
  return PRIORITY_DETAIL_FIELDS.map(label => ({
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
    return resolvePatientPathogenResult(row)
  }
  if (label === "诊断结果") {
    return resolvePatientDiagnosisResult(row)
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
  if (row.firstTreatmentPlan?.trim()) return String(row.firstTreatmentPlan).trim()
  const fields = resolveImportFields(row)
  return fields["首次治疗方案"] || parseEpidemicDataField(row.epidemicData, "首次治疗方案")
}

/** 格式化通知单发送/接收时间 */
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

/** 通知单管理列表：发送时间（已下发/已确认时显示） */
export function resolveNoticeSentDisplayTime(row: Record<string, any> | null | undefined): string {
  if (!row || (row.noticeStatus !== 1 && row.noticeStatus !== 2)) return ""
  return formatNoticeSentTime(row.noticeSentTime)
}

/** 通知单管理列表：接收时间（对方确认后显示） */
export function resolveNoticeConfirmedDisplayTime(row: Record<string, any> | null | undefined): string {
  if (!row || row.noticeStatus !== 2) return ""
  return formatNoticeSentTime(row.noticeConfirmedTime)
}

/** 部分重点字段在 patient 主表也有对应值（病原学/诊断结果由专用 resolve 处理，不在此回退） */
function resolveFieldFromPatient(_row: Record<string, any> | null | undefined, _label: string): string {
  return ""
}

function isSpecialDiseasePatient(row: Record<string, any> | null | undefined): boolean {
  if (!row) return false
  return row.populationType === "specialDisease" || row.source === "specialDisease"
}

/**
 * 解析患者病原学结果（在管总览「病原学结果」列）。
 * 优先导入表「病原学结果」；专病网主表 diagnosisResult 存的是诊断结果，不能回退。
 * 手动录入无导入「病原学结果」时，主表 diagnosisResult 即表单病原学结果。
 */
export function resolvePatientPathogenResult(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  const fields = resolveImportFields(row)
  const fromImport = String(fields["病原学结果"] || "").trim()
  if (fromImport) return fromImport
  // 导入已带独立「诊断结果」时，主表字段属于诊断，不当作病原学展示
  if (String(fields["诊断结果"] || "").trim() || isSpecialDiseasePatient(row)) {
    return ""
  }
  return String(row.diagnosisResult || "").trim()
}

/**
 * 解析患者诊断结果（专病网/导入表「诊断结果」）。
 * 优先导入字段；专病网主表 diagnosisResult 由导入「诊断结果」写入，可作回退。
 * 手动表单写入的 diagnosisResult 是病原学结果，不回退到本列，避免与病原学列重复。
 */
export function resolvePatientDiagnosisResult(row: Record<string, any> | null | undefined): string {
  if (!row) return ""
  const fields = resolveImportFields(row)
  const fromImport = String(fields["诊断结果"] || "").trim()
  if (fromImport) return fromImport
  if (isSpecialDiseasePatient(row)) {
    return String(row.diagnosisResult || "").trim()
  }
  return ""
}

/** 将病原学结果映射为通知单「痰涂片」选项 */
export function mapPathogenResultToNoticeSputumSmear(pathogenResult?: string | null): string {
  if (!pathogenResult) return ""
  const normalized = pathogenResult.trim()
  if (!normalized) return ""
  if ((PATHOGEN_RESULT_OPTIONS as readonly string[]).includes(normalized)) return normalized
  if (normalized.includes("阳性")) return "阳性"
  if (normalized.includes("阴性")) return "阴性"
  if (normalized.includes("未出")) return "未出结果"
  if (normalized.includes("未做") || normalized.includes("未查")) return "未做"
  if (normalized.includes("未知")) return "未知"
  return ""
}

/** 从患者记录解析通知单「痰涂片」默认值 */
export function resolveNoticeSputumSmearFromPatient(row: Record<string, any> | null | undefined): string {
  return mapPathogenResultToNoticeSputumSmear(resolvePatientPathogenResult(row))
}

/** 通知单是否已发送（含待接收、已确认；兼容潜伏感染者 noticeSent 字段） */
export function isNoticeSent(row: Record<string, any> | null | undefined): boolean {
  if (!row) return false
  if (row.noticeSent === true) return true
  const status = Number(row.noticeStatus)
  return status === 1 || status === 2
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
    drugSensitivityH: fields["药敏结果：异烟肼（H）"] || "",
    cultureResult: fields["培养结果"] || fields["0月序培养结果"] || ""
  }
}

/** 转出待确认 */
export const PATIENT_TRANSFER_PENDING = "转出待确认"

/** 已转出 */
export const PATIENT_TRANSFERRED_OUT = "已转出"

/** 患者是否处于转出锁定（待确认或已转出，不可编辑/转出/删除） */
export function isPatientTransferLocked(row: Record<string, any> | null | undefined): boolean {
  const remark = row?.archiveRemark
  return remark === PATIENT_TRANSFER_PENDING || remark === PATIENT_TRANSFERRED_OUT
}

/** 患者是否处于转出待确认 */
export function isPatientTransferPending(row: Record<string, any> | null | undefined): boolean {
  return row?.archiveRemark === PATIENT_TRANSFER_PENDING
}

/** 转出状态展示文案 */
export function getPatientTransferStatusLabel(archiveRemark?: string | null): string {
  if (archiveRemark === PATIENT_TRANSFERRED_OUT) return PATIENT_TRANSFERRED_OUT
  if (archiveRemark === PATIENT_TRANSFER_PENDING) return PATIENT_TRANSFER_PENDING
  return ""
}
