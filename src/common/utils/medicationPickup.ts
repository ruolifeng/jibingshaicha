/** 五级用户（role=6）领药记录的可编辑天数 */
export const MEDICATION_PICKUP_EDIT_DAYS_LEVEL5 = 10

/** 填写/修改领药记录（含 V2 聚合权限与旧模块权限码，任一即可） */
export const PATIENT_MEDICATION_PICKUP_PERMISSIONS = [
  "patientManagement:pickup",
  "patientManagement:medication",
  "patient:medication",
  "keyPopulation:patient:medication",
  "closeContact:patient:medication"
] as const

/** 进入服药管理页 / 服药日历（聚合模块） */
export const PATIENT_MEDICATION_PAGE_PERMISSIONS = [
  "patientManagement:medication",
  ...PATIENT_MEDICATION_PICKUP_PERMISSIONS
] as const

/** 五级用户：领药记录创建后 10 天内可修改；管理员（role≠6）随时可改 */
export function canEditMedicationPickup(
  userRole: number,
  record: { createTime?: string | null, editable?: boolean | null } | null | undefined
): boolean {
  if (!record) return false
  if (record.editable != null) return record.editable
  if (userRole !== 6) return true
  if (!record.createTime) return true
  const created = new Date(record.createTime.replace(" ", "T"))
  const deadline = new Date(created)
  deadline.setDate(deadline.getDate() + MEDICATION_PICKUP_EDIT_DAYS_LEVEL5)
  return Date.now() <= deadline.getTime()
}

export interface MedicationPickupDrugItem {
  name: string
  dosage: string
}

export function parseMedicationPickupDrugs(drugsJson?: string | unknown[] | null): MedicationPickupDrugItem[] {
  if (!drugsJson) return []
  try {
    const parsed = typeof drugsJson === "string" ? JSON.parse(drugsJson) : drugsJson
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((item: any) => ({
        name: String(item?.name ?? "").trim(),
        dosage: String(item?.dosage ?? "").trim()
      }))
      .filter(item => item.name || item.dosage)
  } catch {
    return []
  }
}

export function formatMedicationPickupDrugs(drugsJson?: string | null): string {
  return parseMedicationPickupDrugs(drugsJson)
    .map(item => `${item.name}${item.dosage ? `（${item.dosage}）` : ""}`)
    .join("；")
}
