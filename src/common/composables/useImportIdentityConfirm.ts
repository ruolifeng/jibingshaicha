export interface ImportResultData {
  successCount: number
  insertCount?: number
  updateCount?: number
  skippedCount?: number
  duplicateCount?: number
  invalidIdentityCount?: number
  requireIdentityConfirm?: boolean
  errors: string[]
}

/**
 * 导入 Excel：若存在缺少姓名/身份证的行，先提示用户是否跳过无效行并继续导入有效数据。
 */
export async function runImportWithIdentityConfirm<T extends ImportResultData>(
  uploadFn: (file: File, confirmSkipInvalid?: boolean) => Promise<{ data: T }>,
  file: File
): Promise<T | null> {
  const { data } = await uploadFn(file, false)
  if (data.requireIdentityConfirm && (data.invalidIdentityCount ?? 0) > 0) {
    try {
      await ElMessageBox.confirm(
        `发现 ${data.invalidIdentityCount} 条数据缺少姓名或身份证，无法作为有效人员记录。\n是否继续导入其余有效数据？`,
        "无效导入确认",
        {
          confirmButtonText: "继续导入有效数据",
          cancelButtonText: "取消",
          type: "warning"
        }
      )
      const { data: confirmed } = await uploadFn(file, true)
      return confirmed
    } catch {
      return null
    }
  }
  return data
}

/** 展示导入结果弹窗前的通用提示 */
export function getImportResultAlertTitle(result: ImportResultData): string {
  if (result.successCount > 0) {
    return `成功导入 ${result.successCount} 条数据`
  }
  if (result.requireIdentityConfirm) {
    return "导入已取消"
  }
  return "未导入任何数据"
}
