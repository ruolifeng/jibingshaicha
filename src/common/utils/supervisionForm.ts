/** 督导表记录是否可修改（五级用户 10 天内） */
export const SUPERVISION_EDIT_DAYS_LEVEL5 = 10

export function canEditSupervisionForm(
  userRole: number | null | undefined,
  record: { status?: number, createTime?: string | null, editable?: boolean | null }
) {
  if (!record || record.status !== 1) return false
  if (record.editable != null) return record.editable
  if (userRole == null || userRole !== 6) return true
  if (!record.createTime) return true
  const created = new Date(record.createTime.replace(" ", "T"))
  if (Number.isNaN(created.getTime())) return true
  const deadline = new Date(created)
  deadline.setDate(deadline.getDate() + SUPERVISION_EDIT_DAYS_LEVEL5)
  return deadline.getTime() >= Date.now()
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
