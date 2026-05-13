/**
 * 新窗口打印工具
 *
 * 将目标元素的 HTML 连同当前页面的所有 CSS（含 scoped 样式）
 * 一起写入新窗口并触发打印，彻底避免 el-dialog 定位/overflow 干扰。
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
    return
  }

  const printWindow = window.open("", "_blank", "width=900,height=700")
  if (!printWindow) {
    console.error("[printElement] 无法打开新窗口，请检查浏览器弹窗拦截设置")
    return
  }

  printWindow.document.write(`
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
      <meta charset="utf-8" />
      <title>${title}</title>
      ${collectLinkTags()}
      <style>
        ${collectInlineStyles()}
        /* 打印时去除浏览器默认边距 */
        body { margin: 0; padding: 20px; background: #fff; }
        ${extraCss}
      </style>
    </head>
    <body>
      ${el.outerHTML}
    </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()

  // 等待样式/图片加载完成后再触发打印
  printWindow.onload = () => {
    printWindow.print()
    printWindow.close()
  }
  // 兜底：部分浏览器不触发 onload（纯 HTML 无外部资源时）
  setTimeout(() => {
    try {
      printWindow.print()
      printWindow.close()
    }
    catch {
      // 窗口可能已被用户关闭，忽略错误
    }
  }, 600)
}
