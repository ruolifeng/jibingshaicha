import printFormsCss from "@@/assets/styles/print-forms.css?raw"
import { ElMessage } from "element-plus"

/**
 * 打印工具（iframe 方式）
 *
 * 将目标元素的 HTML 写入隐藏 iframe，并注入自包含的打印样式后触发打印，
 * 避免 window.open 被浏览器拦截，也避免 el-dialog 定位/overflow 干扰。
 *
 * 表单样式与预览共用 print-forms.css，避免双轨漂移。
 */

const IMAGE_LOAD_TIMEOUT_MS = 15_000

/**
 * 去除浏览器默认页眉页脚（打印时间、网址、文档标题、页码）。
 * Chrome 在 @page margin 为 0 时不渲染页眉页脚；内容边距改由 body padding 控制。
 */
const PRINT_PAGE_CSS = `
  @page {
    size: A4;
    margin: 0;
  }
  @media print {
    html,
    body {
      margin: 0;
      padding: 10mm 12mm;
    }
  }
`

function buildPrintDocument(bodyHtml: string, extraCss = ""): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title></title>
  <style>
    body { margin: 0; padding: 20px; background: #fff; color: #303133; }
    ${PRINT_PAGE_CSS}
    ${printFormsCss}
    ${extraCss}
  </style>
</head>
<body>${bodyHtml}</body>
</html>`
}

function createPrintFrame(title: string): HTMLIFrameElement {
  const iframe = document.createElement("iframe")
  iframe.setAttribute("title", title)
  iframe.setAttribute("aria-hidden", "true")
  Object.assign(iframe.style, {
    position: "fixed",
    top: "-10000px",
    left: "0",
    width: "794px",
    height: "1123px",
    border: "0",
    opacity: "0",
    pointerEvents: "none"
  })
  document.body.appendChild(iframe)
  return iframe
}

function imageToDataUrl(img: HTMLImageElement): string | null {
  if (!img.complete || img.naturalWidth === 0 || img.naturalHeight === 0) return null
  try {
    const canvas = document.createElement("canvas")
    canvas.width = img.naturalWidth
    canvas.height = img.naturalHeight
    const ctx = canvas.getContext("2d")
    if (!ctx) return null
    ctx.drawImage(img, 0, 0)
    const isPng = /\.png(\?|$)/i.test(img.currentSrc || img.src)
    return canvas.toDataURL(isPng ? "image/png" : "image/jpeg", 0.92)
  } catch (error) {
    console.warn("[print] 图片转 dataURL 失败", img.currentSrc || img.src, error)
    return null
  }
}

/** 将预览区已加载的图片嵌入克隆节点，避免 iframe 内重复拉取导致后几张未就绪 */
function embedLoadedImages(sourceRoot: HTMLElement, targetRoot: HTMLElement) {
  const sourceImgs = Array.from(sourceRoot.querySelectorAll("img"))
  const targetImgs = Array.from(targetRoot.querySelectorAll("img"))
  targetImgs.forEach((targetImg, index) => {
    const sourceImg = sourceImgs[index]
    if (!(sourceImg instanceof HTMLImageElement)) return
    const dataUrl = imageToDataUrl(sourceImg)
    if (dataUrl) targetImg.setAttribute("src", dataUrl)
  })
}

function waitOneImage(img: HTMLImageElement, timeoutMs: number): Promise<void> {
  if (img.complete && img.naturalWidth > 0) return Promise.resolve()
  return new Promise((resolve) => {
    const finish = () => resolve()
    img.addEventListener("load", finish, { once: true })
    img.addEventListener("error", finish, { once: true })
    setTimeout(finish, timeoutMs)
  })
}

async function embedImageViaFetch(img: HTMLImageElement): Promise<void> {
  const src = img.getAttribute("src")
  if (!src || src.startsWith("data:")) return
  try {
    const response = await fetch(src, { credentials: "include" })
    if (!response.ok) return
    const blob = await response.blob()
    const dataUrl = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as string)
      reader.onerror = reject
      reader.readAsDataURL(blob)
    })
    img.setAttribute("src", dataUrl)
    await waitOneImage(img, IMAGE_LOAD_TIMEOUT_MS)
  } catch (error) {
    console.warn("[print] 拉取图片失败", src, error)
  }
}

/** 确保 iframe 内所有图片已内嵌或加载完成后再打印 */
async function ensurePrintImagesReady(doc: Document) {
  const images = Array.from(doc.querySelectorAll("img"))
  await Promise.all(images.map(async (img) => {
    if (img.src.startsWith("data:") && img.complete && img.naturalWidth > 0) return
    const dataUrl = imageToDataUrl(img)
    if (dataUrl) {
      img.setAttribute("src", dataUrl)
      return
    }
    await embedImageViaFetch(img)
    await waitOneImage(img, IMAGE_LOAD_TIMEOUT_MS)
  }))
}

/**
 * 将 HTML 写入 iframe 并触发浏览器打印（可选择「另存为 PDF」）
 *
 * @param bodyHtml  打印内容 HTML
 * @param title     iframe 无障碍标签，不会写入文档标题（避免浏览器页眉重复显示）
 * @param extraCss  额外的 CSS 字符串（可选）
 */
export function printHtml(bodyHtml: string, title = "打印", extraCss = "") {
  const iframe = createPrintFrame(title)
  const win = iframe.contentWindow

  if (!win) {
    iframe.remove()
    ElMessage.error("无法创建打印窗口，请刷新页面后重试")
    return
  }

  let printed = false
  let cleaned = false
  let preparing = false

  const cleanup = () => {
    if (cleaned) return
    cleaned = true
    iframe.remove()
  }

  const doPrint = () => {
    if (printed) return
    printed = true
    try {
      win.focus()
      win.print()
    } catch (error) {
      console.error("[printHtml] 打印失败", error)
      ElMessage.error("打印失败，请重试")
      cleanup()
    }
  }

  win.onafterprint = () => {
    cleanup()
  }

  const schedulePrint = async () => {
    if (preparing || printed) return
    const doc = win.document
    if (!doc) {
      ElMessage.error("无法创建打印窗口，请刷新页面后重试")
      cleanup()
      return
    }
    preparing = true
    try {
      await ensurePrintImagesReady(doc)
      doPrint()
    } catch (error) {
      console.error("[printHtml] 图片准备失败", error)
      ElMessage.warning("部分图片可能未加载完成，正在尝试打印")
      doPrint()
    } finally {
      preparing = false
    }
  }

  iframe.onload = () => {
    void schedulePrint()
  }
  iframe.srcdoc = buildPrintDocument(bodyHtml, extraCss)

  setTimeout(() => {
    if (!printed && win.document?.readyState === "complete") {
      void schedulePrint()
    }
  }, 500)

  setTimeout(cleanup, 60_000)
}

/**
 * 打印指定 ID 的 DOM 元素
 *
 * @param elementId 要打印区域的元素 id
 * @param title     iframe 无障碍标签（可选，不会出现在打印页眉）
 * @param extraCss  额外的 CSS 字符串，例如 @page 或覆盖样式（可选）
 */
export function printElement(elementId: string, title = "打印", extraCss = "") {
  const el = document.getElementById(elementId)
  if (!el) {
    console.error(`[printElement] 未找到元素 #${elementId}`)
    ElMessage.error("打印内容未就绪，请关闭预览后重试")
    return
  }

  const clone = el.cloneNode(true) as HTMLElement
  clone.removeAttribute("id")
  embedLoadedImages(el, clone)
  printHtml(clone.outerHTML, title, extraCss)
}
