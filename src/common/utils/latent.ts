/** 转出待确认 */
export const LATENT_TRANSFER_PENDING = "转出待确认"

/** 已转出 */
export const LATENT_TRANSFERRED_OUT = "已转出"

/** 潜伏感染记录是否处于转出锁定 */
export function isLatentTransferLocked(row: Record<string, any> | null | undefined): boolean {
  const remark = row?.archiveRemark
  return remark === LATENT_TRANSFER_PENDING || remark === LATENT_TRANSFERRED_OUT
}

/** 转出状态展示文案 */
export function getLatentTransferStatusLabel(archiveRemark?: string | null): string {
  if (archiveRemark === LATENT_TRANSFERRED_OUT) return LATENT_TRANSFERRED_OUT
  if (archiveRemark === LATENT_TRANSFER_PENDING) return LATENT_TRANSFER_PENDING
  return ""
}
