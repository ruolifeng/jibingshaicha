/**
 * 自动计算题型 · 公式工具
 *
 * 公式语法：
 *  - 变量占位符：`{<key>}`，<key> 在设计端为题目稳定键（stableKey），
 *    保存到数据库或填写端使用时为题目排序号（sortOrder，1-based）
 *  - 运算符：+ - * / ( ) %
 *  - 数字：支持整数与小数
 *
 * 例：`{1}*100/30/{2}` 表示 第1题 × 100 ÷ 30 ÷ 第2题
 */

/** 从题目标题提取字段编码（如 "FF12a 每日..." → "FF12a"） */
export function extractTitleCode(title: string | undefined): string | null {
  if (!title) return null
  const m = /^([A-Z][A-Z0-9]*)\b/i.exec(title.trim())
  return m?.[1] ?? null
}

/** 提取公式内所有变量占位符（去重，保持出现顺序） */
export function extractFormulaRefs(formula: string): string[] {
  if (!formula) return []
  const set = new Set<string>()
  const re = /\{([^}]+)\}/g
  let m: RegExpExecArray | null
  // eslint-disable-next-line no-cond-assign
  while ((m = re.exec(formula)) != null) {
    const k = m[1].trim()
    if (k) set.add(k)
  }
  return Array.from(set)
}

/** 公式合法字符校验：替换变量后表达式只允许 数字 / 小数点 / + - * / ( ) % / 空白 */
const SAFE_EXPR_RE = /^[\d+\-*/().%\s]+$/

export interface FormulaEvalResult {
  /** 计算成功的数值；失败为 null */
  value: number | null
  /** 静默错误标志：true 表示因依赖项尚未填写而无法计算（不应展示给用户） */
  pending: boolean
  /** 用户可见的错误信息（语法/非法字符等） */
  error: string | null
}

/**
 * 安全求值公式
 * @param formula 原始公式（含 `{key}` 占位符）
 * @param resolve 占位符 key → 数值；若未填写或非数字返回 null
 */
export function evalFormula(
  formula: string,
  resolve: (key: string) => number | null
): FormulaEvalResult {
  if (!formula || !formula.trim()) {
    return { value: null, pending: false, error: "公式为空" }
  }

  let pending = false
  const expr = formula.replace(/\{([^}]+)\}/g, (_, k: string) => {
    const v = resolve(k.trim())
    if (v == null || Number.isNaN(v)) {
      pending = true
      return "0"
    }
    // 负数加括号，避免破坏表达式（如 `5*-3` 会被解析正常，但 `5--3` 含义易混淆）
    return v < 0 ? `(${v})` : String(v)
  })

  if (pending) return { value: null, pending: true, error: null }

  if (!SAFE_EXPR_RE.test(expr)) {
    return { value: null, pending: false, error: "公式包含非法字符" }
  }

  try {
    // 受 SAFE_EXPR_RE 校验保护，不会执行任意代码
    // eslint-disable-next-line no-new-func
    const fn = new Function(`"use strict"; return (${expr})`)
    const result = fn()
    if (typeof result !== "number" || !Number.isFinite(result)) {
      return { value: null, pending: false, error: "计算结果非数字" }
    }
    return { value: result, pending: false, error: null }
  } catch {
    return { value: null, pending: false, error: "公式语法错误" }
  }
}

/**
 * 用户输入字符串 → 数字（用于 resolve 回调）
 * 处理常见情况：空字符串、纯数字字符串、带空白
 */
export function answerToNumber(raw: unknown): number | null {
  if (raw == null) return null
  const s = String(raw).trim()
  if (!s) return null
  const n = Number(s)
  return Number.isNaN(n) ? null : n
}

/**
 * 替换公式中的变量占位符（如 `{a}` → `{b}`）
 * 用于 stableKey ↔ sortOrder 转换
 */
export function remapFormulaRefs(
  formula: string,
  mapper: (oldKey: string) => string | null
): string {
  if (!formula) return formula
  return formula.replace(/\{([^}]+)\}/g, (_, k: string) => {
    const next = mapper(k.trim())
    return next ? `{${next}}` : "{?}"
  })
}
