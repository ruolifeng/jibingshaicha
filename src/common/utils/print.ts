import { ElMessage } from "element-plus"

/**
 * 打印工具（iframe 方式）
 *
 * 将目标元素的 HTML 写入隐藏 iframe，并注入自包含的打印样式后触发打印，
 * 避免 window.open 被浏览器拦截，也避免 el-dialog 定位/overflow 干扰。
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

const PRINT_ATTACHMENT_CSS = `
  .print-attachments { margin-top: 12px; }
  .print-attachments__title {
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #1a3a6b;
  }
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
  .print-attachments__item figcaption {
    margin-top: 4px;
    font-size: 12px;
    color: #606266;
  }
`

/** 打印表单通用样式（iframe 内无法依赖 Vue scoped，需显式写入） */
const PRINT_FORM_CSS = `
  .print-area { padding: 8px; }
  .print-header { position: relative; margin-bottom: 16px; }
  .print-title {
    text-align: center;
    font-size: 18px;
    font-weight: bold;
    margin: 0 0 8px;
  }
  .print-subtitle {
    text-align: center;
    font-size: 14px;
    margin-bottom: 12px;
    color: #606266;
  }
  .print-form-no {
    position: absolute;
    top: 0;
    right: 0;
    font-size: 14px;
  }
  .visit-table,
  .notice-table,
  .info-table,
  .sup-table,
  .form-table {
    width: 100%;
    border-collapse: collapse;
    border: 1px solid #333;
  }
  .visit-table th,
  .visit-table td,
  .notice-table th,
  .notice-table td,
  .info-table th,
  .info-table td,
  .sup-table th,
  .sup-table td,
  .form-table th,
  .form-table td {
    border: 1px solid #333;
    padding: 7px 10px;
    font-size: 13px;
    vertical-align: middle;
  }
  .visit-table th,
  .notice-table th,
  .info-table th,
  .sup-table th,
  .form-table th {
    background: #f5f7fa;
    white-space: nowrap;
    font-weight: 600;
  }
  .info-table th {
    background: #f0f0f0;
  }
  .visit-table th { width: 100px; }
  .notice-table th { width: 120px; }
  .sup-table th { width: 130px; vertical-align: top; }
  .form-table th { width: 110px; vertical-align: top; }
  .visit-table .section-header td,
  .sup-table .section-header td {
    background: #e8f0fe;
    font-weight: bold;
    font-size: 13px;
    padding: 5px 10px;
    color: #1a3a6b;
  }
  .visit-table .empty-cell,
  .sup-table .empty-cell {
    text-align: center;
    color: #999;
  }
  .print-footer {
    display: flex;
    justify-content: space-between;
    margin-top: 20px;
    font-size: 13px;
    color: #303133;
  }
  .med-table {
    width: 100%;
    border-collapse: collapse;
    border: 1px solid #333;
    font-size: 12px;
    table-layout: fixed;
  }
  .med-table th,
  .med-table td {
    border: 1px solid #333;
    text-align: center;
    padding: 0;
    height: 24px;
    line-height: 24px;
    vertical-align: middle;
  }
  .med-table .th-month {
    width: 52px;
    background: #f0f0f0;
    font-weight: 600;
    font-size: 11px;
    line-height: 1.3;
    padding: 3px 2px;
  }
  .med-table .th-day {
    background: #f0f0f0;
    font-weight: 600;
    font-size: 11px;
  }
  .med-table .td-month {
    background: #f0f0f0;
    font-weight: 600;
  }
  .med-table .td-invalid {
    background: repeating-linear-gradient(45deg, #e8e8e8, #e8e8e8 2px, #f5f5f5 2px, #f5f5f5 8px);
  }
  .med-table .td-mark-x,
  .med-table .td-mark-circled {
    color: #000;
    font-weight: bold;
    font-size: 14px;
  }
  .med-table .td-mark-circled {
    font-size: 15px;
  }
  @media print {
    body {
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .visit-table,
    .notice-table,
    .info-table,
    .sup-table,
    .form-table,
    .med-table,
    .visit-table th,
    .visit-table td,
    .notice-table th,
    .notice-table td,
    .info-table th,
    .info-table td,
    .sup-table th,
    .sup-table td,
    .form-table th,
    .form-table td,
    .med-table th,
    .med-table td {
      border: 1px solid #000 !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .visit-table th,
    .notice-table th,
    .sup-table th,
    .form-table th {
      background: #f5f7fa !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .info-table th {
      background: #f0f0f0 !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .visit-table .section-header td,
    .sup-table .section-header td {
      background: #e8f0fe !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .med-table .th-month,
    .med-table .th-day,
    .med-table .td-month {
      background: #f0f0f0 !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .med-table .td-invalid {
      background: repeating-linear-gradient(45deg, #e8e8e8, #e8e8e8 2px, #f5f5f5 2px, #f5f5f5 8px) !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
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
    ${PRINT_ATTACHMENT_CSS}
    ${PRINT_FORM_CSS}
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
