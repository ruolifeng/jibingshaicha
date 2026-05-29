import type { UploadRequestOptions } from "element-plus"
import { request } from "@/http/axios"

/** 解析附件 URL 字段（兼容 JSON 数组、逗号分隔、单 URL） */
export function parseAttachmentUrls(value?: string | string[] | null): string[] {
  if (!value) return []
  if (Array.isArray(value)) return value.map(v => resolveFileUrl(v)).filter(Boolean)
  const trimmed = value.trim()
  if (!trimmed) return []
  if (trimmed.startsWith("[")) {
    try {
      const parsed = JSON.parse(trimmed)
      if (Array.isArray(parsed)) {
        return parsed.map(v => resolveFileUrl(String(v))).filter(Boolean)
      }
    } catch { /* fall through */ }
  }
  return trimmed.split(",").map(v => resolveFileUrl(v.trim())).filter(Boolean)
}

/** 将后端返回的相对路径转为可访问的完整 URL */
export function resolveFileUrl(path?: string | null): string {
  if (!path) return ""
  const val = path.trim()
  if (!val) return ""
  if (val.startsWith("http://") || val.startsWith("https://") || val.startsWith("blob:")) {
    return val
  }
  const base = (import.meta.env.VITE_BASE_URL || "").trim()
  if (val.startsWith("/api/")) return val
  if (val.startsWith("/file/")) return `${base}${val}`
  return `${base}${val.startsWith("/") ? val : `/${val}`}`
}

/** el-upload 原始响应 → 业务结果（兼容字符串 JSON） */
export function parseUploadApiResponse(response: unknown): { ok: boolean, url?: string, msg?: string } {
  let body: Record<string, any> | null = null
  if (typeof response === "string") {
    try {
      body = JSON.parse(response)
    } catch {
      body = null
    }
  } else if (response && typeof response === "object") {
    body = response as Record<string, any>
  }
  if (body?.code === 200 && body.data) {
    return { ok: true, url: resolveFileUrl(String(body.data)) }
  }
  return { ok: false, msg: body?.msg || "上传失败" }
}

/** 获取文件上传接口地址 */
export function getFileUploadAction(): string {
  return `${(import.meta.env.VITE_BASE_URL || "").trim()}/file/upload`
}

/** 通过 axios 上传（与业务接口一致，统一鉴权与错误提示） */
export async function uploadAttachmentFile(options: UploadRequestOptions) {
  const formData = new FormData()
  formData.append(options.filename || "file", options.file)
  try {
    const res = await request<ApiResponseData<string>>({
      url: "/file/upload",
      method: "post",
      data: formData
    })
    options.onSuccess(res)
  } catch (error) {
    options.onError(error as Parameters<UploadRequestOptions["onError"]>[0])
  }
}

/** 从 URL 提取展示名称 */
export function getAttachmentLabel(url: string, index = 0): string {
  try {
    const u = new URL(url, window.location.origin)
    const name = u.searchParams.get("name")
    if (name) return decodeURIComponent(name)
  } catch { /* ignore */ }
  const fileName = url.split("/").pop()?.split("?")[0]
  return fileName || `附件${index + 1}`
}

/** 判断是否为图片附件（用于预览） */
export function isImageAttachment(url: string): boolean {
  const lower = url.toLowerCase()
  return /\.(png|jpe?g|gif|webp|bmp|svg)(\?|$)/i.test(lower)
    || lower.includes("image/")
}
