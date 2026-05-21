import { ElMessage } from "element-plus"

/**
 * 打印工具（iframe 方式）
 *
 * 将目标元素的 HTML 连同当前页面的样式写入隐藏 iframe 并触发打印，
 * 避免 window.open 被浏览器拦截，也避免 el-dialog 定位/overflow 干扰。
 */

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
    height: "700px",
    border: "0",
    opacity: "0",
    pointerEvents: "none"
  })
  document.body.appendChild(iframe)
  return iframe
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

  // 等待 iframe 内样式渲染完成后再触发打印
  const schedulePrint = () => setTimeout(doPrint, 300)
  if (doc.readyState === "complete") {
    schedulePrint()
  } else {
    win.onload = schedulePrint
    setTimeout(doPrint, 1000)
  }

  // 部分浏览器不触发 onafterprint，延迟清理 iframe
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
  printHtml(clone.outerHTML, title, extraCss)
}
