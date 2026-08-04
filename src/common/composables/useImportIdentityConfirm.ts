export interface ImportConfirmOptions {
  confirmSkipInvalid?: boolean
  confirmSkipDuplicateInFile?: boolean
}

export interface ImportResultData {
  successCount: number
  insertCount?: number
  updateCount?: number
  skippedCount?: number
  duplicateCount?: number
  invalidIdentityCount?: number
  missingIdCount?: number
  duplicateInFileCount?: number
  duplicateInFileSummaries?: string[]
  requireIdentityConfirm?: boolean
  requireDuplicateInFileConfirm?: boolean
  errors: string[]
}

function buildDuplicateConfirmMessage(result: ImportResultData): string {
  const summaries = result.duplicateInFileSummaries ?? []
  const preview = summaries.slice(0, 5).join("\n")
  const more = summaries.length > 5 ? `\n... 等共 ${summaries.length} 个重复身份证` : ""
  return `Excel 中发现 ${summaries.length} 个身份证号在本表重复出现：\n${preview}${more}\n\n继续导入将保留每个身份证最后一行数据，其余重复行将跳过。是否继续导入？`
}

/**
 * 导入 Excel：依次处理「缺少姓名」与「文件内重复身份证」两类确认。
 * 有姓名但未填证件号的行会照常导入，并在结果中提醒。
 */
export async function runImportWithIdentityConfirm<T extends ImportResultData>(
  uploadFn: (file: File, options?: ImportConfirmOptions) => Promise<{ data: T }>,
  file: File
): Promise<T | null> {
  const options: ImportConfirmOptions = {}

  while (true) {
    const { data } = await uploadFn(file, options)

    if (data.requireIdentityConfirm && !options.confirmSkipInvalid && (data.invalidIdentityCount ?? 0) > 0) {
      try {
        await ElMessageBox.confirm(
          `发现 ${data.invalidIdentityCount} 条数据缺少姓名，无法作为有效人员记录。\n是否继续导入其余有效数据？`,
          "无效导入确认",
          {
            confirmButtonText: "继续导入有效数据",
            cancelButtonText: "取消",
            type: "warning"
          }
        )
        options.confirmSkipInvalid = true
        continue
      } catch {
        return null
      }
    }

    if (data.requireDuplicateInFileConfirm && !options.confirmSkipDuplicateInFile && (data.duplicateInFileCount ?? 0) > 0) {
      try {
        await ElMessageBox.confirm(
          buildDuplicateConfirmMessage(data),
          "文件内重复身份证提醒",
          {
            confirmButtonText: "继续导入",
            cancelButtonText: "取消",
            type: "warning"
          }
        )
        options.confirmSkipDuplicateInFile = true
        continue
      } catch {
        return null
      }
    }

    return data
  }
}

/** 展示导入结果弹窗前的通用提示 */
export function getImportResultAlertTitle(result: ImportResultData): string {
  if (result.successCount > 0) {
    return `成功导入 ${result.successCount} 条数据`
  }
  if (result.requireIdentityConfirm || result.requireDuplicateInFileConfirm) {
    return "导入已取消"
  }
  return "未导入任何数据"
}
