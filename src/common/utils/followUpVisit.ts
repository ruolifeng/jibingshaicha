import { formatFirstVisitMethod } from "@@/utils/firstVisit"
import { followUpFormatters } from "@@/utils/followUpVisitFormat"
import { resolveFirstTreatmentPlan } from "@@/utils/patient"

export interface FollowUpCaseClosureStats {
  actualVisitCount: number
  actualDoseCount: number
}

/** 草稿转完成或新建时，当前随访尚未计入已完成列表 */
export function shouldIncludeCurrentFollowUpInStats(
  initialData?: { id?: string, status?: number } | null
): boolean {
  if (!initialData?.id) return true
  return initialData.status !== 1
}

/**
 * 后续随访化疗方案预填（已有值则不覆盖）：
 * 1. 优先同步上一次后续随访的化疗方案
 * 2. 其次同步首次随访的化疗方案
 * 3. 再次回退病案「首次治疗方案」
 */
export function applyFollowUpChemotherapyDefault(
  form: { chemotherapyPlan?: string },
  options?: {
    previousFollowUpChemotherapy?: string | null
    firstVisitChemotherapy?: string | null
    patientRow?: Record<string, any> | null
  }
) {
  if (form.chemotherapyPlan?.trim()) return
  const fromPrevious = options?.previousFollowUpChemotherapy?.trim()
  if (fromPrevious) {
    form.chemotherapyPlan = fromPrevious
    return
  }
  const fromFirstVisit = options?.firstVisitChemotherapy?.trim()
  if (fromFirstVisit) {
    form.chemotherapyPlan = fromFirstVisit
    return
  }
  const plan = resolveFirstTreatmentPlan(options?.patientRow)
  if (plan) form.chemotherapyPlan = plan
}

/** 取最近一次已完成后续随访的化疗方案；编辑时可限定为当前记录之前的随访 */
export function resolvePreviousFollowUpChemotherapy(
  completedList: Array<{ id?: string | number, chemotherapyPlan?: string | null }>,
  options?: { beforeId?: string | number | null }
): string {
  let list = completedList
  const beforeId = options?.beforeId
  if (beforeId != null && String(beforeId) !== "") {
    const idx = completedList.findIndex(item => String(item.id) === String(beforeId))
    if (idx >= 0) {
      list = completedList.slice(0, idx)
    } else {
      list = completedList.filter(item => String(item.id) !== String(beforeId))
    }
  }
  for (let i = list.length - 1; i >= 0; i--) {
    const plan = list[i]?.chemotherapyPlan?.trim()
    if (plan) return plan
  }
  return ""
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

/** 后续随访已完成记录可随时修改（仍受权限控制；后端 editable 优先） */
export function canEditFollowUpVisit(
  _userRole: number,
  visit: { status?: number, createTime?: string | null, editable?: boolean | null } | null | undefined
): boolean {
  if (!visit) return false
  if (visit.editable != null) return visit.editable
  return true
}

/** 随访记录列表行：首次随访 + 后续随访统一展示 */
export interface FollowUpHistoryDisplayRow {
  recordType: "firstVisit" | "followUp"
  /** 列表「第几次」：以首次为 1，后续依次为 2、3… */
  visitSeq: number
  /** 随访日期：首次取 visitDate（非取药时间），后续取本条 visitDate */
  visitDate: string
  /** 下次随访：取本条填写的 nextVisitDate */
  nextVisitDate: string
  treatmentMonth: string | number
  visitMethodLabel: string
  missedDoses: string | number
  doctorSignature: string
  /** 后续随访原始字段（修改/删除/打印用） */
  id?: string
  status?: number
  createTime?: string | null
  editable?: boolean | null
  /** 详情弹窗用完整原始数据 */
  raw: Record<string, any>
}

/**
 * 合并「已完成首次随访 + 后续随访」为查看记录列表。
 * 首次随访的随访日期必须用 visitDate，禁止误用 medicationPickTime（取药时间）。
 * 「第几次」以首次为 1 起计，后续随访依次为 2、3…
 */
export function buildFollowUpHistoryDisplayList(
  firstVisit: Record<string, any> | null | undefined,
  followUps: Array<Record<string, any>> | null | undefined
): FollowUpHistoryDisplayRow[] {
  const rows: FollowUpHistoryDisplayRow[] = []
  if (firstVisit && Number(firstVisit.status) === 1) {
    rows.push({
      recordType: "firstVisit",
      visitSeq: 1,
      visitDate: String(firstVisit.visitDate || "").trim(),
      nextVisitDate: String(firstVisit.nextVisitDate || "").trim(),
      treatmentMonth: "-",
      visitMethodLabel: formatFirstVisitMethod(firstVisit.visitMethod, firstVisit.visitMethodOther),
      missedDoses: "-",
      doctorSignature: firstVisit.doctorSignature || "",
      raw: firstVisit
    })
  }
  for (const item of followUps || []) {
    if (Number(item.status) === 0) continue
    rows.push({
      recordType: "followUp",
      // 临时占位，合并后按列表顺序重编号
      visitSeq: 0,
      visitDate: String(item.visitDate || "").trim(),
      nextVisitDate: String(item.nextVisitDate || "").trim(),
      treatmentMonth: item.treatmentMonth ?? "-",
      visitMethodLabel: followUpFormatters.visitMethod(item.visitMethod, item.visitMethodOther),
      missedDoses: item.missedDoses ?? "-",
      doctorSignature: item.doctorSignature || "",
      id: item.id,
      status: item.status,
      createTime: item.createTime,
      editable: item.editable,
      raw: item
    })
  }
  return rows.map((row, index) => ({ ...row, visitSeq: index + 1 }))
}

/** 查看详情 / 打印时带上列表展示序号（与「第几次」列一致） */
export function toFollowUpHistoryViewData(record: FollowUpHistoryDisplayRow): Record<string, any> {
  return { ...record.raw, visitSeq: record.visitSeq }
}

/**
 * 新建后续随访时，「下次随访时间」默认值：
 * - 已有后续随访：取最近一次后续随访填写的下次随访时间
 * - 否则：取首次随访填写的下次随访时间
 */
export function resolveFollowUpFormDefaultNextVisitDate(
  completedList: Array<{ nextVisitDate?: string | null }>,
  firstVisitNextDate?: string | null
): string {
  if (completedList.length > 0) {
    const last = completedList[completedList.length - 1]?.nextVisitDate?.trim()
    if (last) return last
  }
  return firstVisitNextDate?.trim() || ""
}
