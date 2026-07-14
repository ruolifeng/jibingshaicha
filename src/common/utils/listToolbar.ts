/**
 * 列表工具栏危险删除确认（删除筛选 / 删除全部）。
 * @returns true 表示用户确认，false 表示取消
 */
export async function confirmDangerDelete(options: {
  title: string
  message: string
  confirmText?: string
}): Promise<boolean> {
  try {
    await ElMessageBox.confirm(options.message, options.title, {
      confirmButtonText: options.confirmText ?? "确认删除",
      cancelButtonText: "取消",
      type: "warning",
      confirmButtonClass: "el-button--danger"
    })
    return true
  } catch {
    return false
  }
}

/** 从 blob 响应触发下载 */
export function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement("a")
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
