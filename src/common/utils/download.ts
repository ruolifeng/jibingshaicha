import { getToken } from "@@/utils/cache/cookies"
import axios, { isAxiosError } from "axios"

/** 下载 Blob 文件 */
export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement("a")
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function readBlobJsonMessage(blob: Blob): Promise<string | undefined> {
  try {
    const json = JSON.parse(await blob.text()) as { msg?: string }
    return json.msg
  } catch {
    return undefined
  }
}

export interface AuthDownloadOptions {
  /** 请求超时（毫秒），大文件导出建议 120000+ */
  timeout?: number
  /** 为 false 时不弹出全局错误提示，由调用方自行处理 */
  showError?: boolean
}

/** 带 JWT 认证的文件下载，成功返回 true */
export async function authDownload(
  url: string,
  fallbackName = "download.xlsx",
  options: AuthDownloadOptions = {}
): Promise<boolean> {
  const { timeout = 120_000, showError = true } = options
  try {
    const token = getToken()
    const resp = await axios.get(url, {
      baseURL: import.meta.env.VITE_BASE_URL,
      responseType: "blob",
      timeout,
      headers: { Authorization: token ? `Bearer ${token}` : "" }
    })
    if (resp.data.type?.includes("application/json")) {
      const msg = await readBlobJsonMessage(resp.data)
      if (showError) {
        ElMessage.error(msg || "下载失败")
      }
      return false
    }
    const disposition = resp.headers["content-disposition"] as string | undefined
    let fileName = fallbackName
    if (disposition && fallbackName === "download.xlsx") {
      const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;\s]+)/i)
      if (match) fileName = decodeURIComponent(match[1].replace(/"/g, ""))
    }
    downloadBlob(new Blob([resp.data]), fileName)
    return true
  } catch (err) {
    if (isAxiosError(err) && err.response?.data instanceof Blob) {
      const msg = await readBlobJsonMessage(err.response.data)
      if (showError) {
        ElMessage.error(msg || "下载失败")
      }
      return false
    }
    if (showError) {
      ElMessage.error("下载失败")
    }
    return false
  }
}
