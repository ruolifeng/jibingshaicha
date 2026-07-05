/** 强密码提示文案 */
export const STRONG_PASSWORD_HINT = "至少8位，须包含字母、数字及特殊符号（如 !@#$% 等）"

const SPECIAL_CHAR_PATTERN = /[!@#$%^&*()_+\-=[\]{}|;:'",.<>?/~\\]/

/** 校验强密码，通过返回 null，失败返回错误信息 */
export function validateStrongPassword(password: string): string | null {
  if (!password) return null
  if (password.length < 8) return "密码至少8位"
  if (!/[A-Z]/i.test(password)) return "密码须包含字母"
  if (!/\d/.test(password)) return "密码须包含数字"
  if (!SPECIAL_CHAR_PATTERN.test(password)) return "密码须包含特殊符号（如 !@#$% 等）"
  return null
}

/** Element Plus 表单校验规则 — 强密码（非必填时为空则跳过） */
export function strongPasswordRule(required = false) {
  return {
    validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
      if (!value) {
        if (required) return callback(new Error("请输入密码"))
        return callback()
      }
      const message = validateStrongPassword(value)
      if (message) return callback(new Error(message))
      callback()
    },
    trigger: "blur"
  }
}
