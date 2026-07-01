import { ElMessage } from "element-plus"

/**
 * 打印工具（iframe 方式）
 *
 * 将目标元素的 HTML 连同当前页面的样式写入隐藏 iframe 并触发打印，
 * 避免 window.open 被浏览器拦截，也避免 el-dialog 定位/overflow 干扰。
 */

const IMAGE_LOAD_TIMEOUT_MS = 15_000
const PRINT_ATTACHMENT_CSS = `
  .print-attachments { margin-top: 12px; }
  .print-attachments__grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }
  .print-attachments__item {
    margin: 0;
    text-align: center;
    page-break-inside: avoid;
    break-inside: avoid;
  }
  .print-attachments__item img {
    width: 100%;
    max-width: 180px;
    max-height: 140px;
    object-fit: contain;
    border: 1px solid #ddd;
    display: block;
    margin: 0 auto;
  }
`

/** 收集当前文档中所有 <style> 标签的文本内容 */
function collectInlineStyles(): string {
  return Array.from(document.querySelectorAll("style"))
    .map(s => s.textContent ?? "")
    .join("\n")
}

/** 收集当前文档中所有外联样式表的 <link> 标签 HTML */
function collectLinkTags(): string {
  return Array.from(document.querySelectorAll<HTMLLinkElement>("link[rel='stylesheet']"))
    .map(l => `<link rel="stylesheet" href="${l.href}">`)
    .join("\n")
}

function buildPrintDocument(title: string, bodyHtml: string, extraCss = ""): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>${title}</title>
  ${collectLinkTags()}
  <style>
    ${collectInlineStyles()}
    body { margin: 0; padding: 20px; background: #fff; color: #303133; }
    @media print {
      body { padding: 0; }
    }
    ${PRINT_ATTACHMENT_CSS}
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
    width: "900px",
    height: "1200px",
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
 */
export function printHtml(bodyHtml: string, title = "打印", extraCss = "") {
  const iframe = createPrintFrame(title)
  const win = iframe.contentWindow
  const doc = win?.document

  if (!win || !doc) {
    iframe.remove()
    ElMessage.error("无法创建打印窗口，请刷新页面后重试")
    return
  }

  doc.open()
  doc.write(buildPrintDocument(title, bodyHtml, extraCss))
  doc.close()

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

  if (doc.readyState === "complete") {
    void schedulePrint()
  } else {
    win.onload = () => {
      void schedulePrint()
    }
  }

  setTimeout(cleanup, 60_000)
}

/**
 * 打印指定 ID 的 DOM 元素
 *
 * @param elementId 要打印区域的元素 id
 * @param title     打印窗口标题（可选）
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
