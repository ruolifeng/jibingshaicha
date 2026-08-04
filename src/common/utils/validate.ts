/** 判断是否为数组 */
export function isArray<T>(arg: T) {
  return Array.isArray ? Array.isArray(arg) : Object.prototype.toString.call(arg) === "[object Array]"
}

/** 判断是否为字符串 */
export function isString(str: unknown) {
  return typeof str === "string" || str instanceof String
}

/** 判断是否为外链 */
export function isExternal(path: string) {
  const reg = /^(https?:|mailto:|tel:)/
  return reg.test(path)
}

/** 证件号空值或「无」等占位（与后端 ImportIdentitySupport 一致） */
export function isMissingIdNumber(id: string | null | undefined): boolean {
  if (!id || !String(id).trim()) return true
  const v = String(id).trim()
  return v === "无"
    || v === "无证件号"
    || v === "无身份证"
    || v === "无身份证号"
    || v === "-"
    || v === "/"
    || v === "——"
    || v === "—"
}

/** 规范化证件号：占位符转为空字符串 */
export function normalizeIdNumber(id: string | null | undefined): string {
  if (isMissingIdNumber(id)) return ""
  return String(id).trim()
}

/** 18位身份证校验（格式 + 校验位） */
export function validateIdCard(id: string): boolean {
  if (!id || id.length !== 18) return false
  const reg = /^\d{17}[\dX]$/i
  if (!reg.test(id)) return false
  // 加权因子
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checkCodes = ["1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"]
  let sum = 0
  for (let i = 0; i < 17; i++) {
    sum += Number.parseInt(id[i]) * weights[i]
  }
  return checkCodes[sum % 11] === id[17].toUpperCase()
}

/** 11位手机号校验 */
export function validatePhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}

/** Element Plus 表单校验规则 — 身份证（非必填时为空/「无」则跳过） */
export function idCardRule(required = false) {
  return {
    validator: (_rule: any, value: string, callback: (err?: Error) => void) => {
      if (isMissingIdNumber(value)) {
        if (required) return callback(new Error("请填写身份证号"))
        return callback()
      }
      if (!validateIdCard(value)) return callback(new Error("身份证号格式不正确（需18位）"))
      callback()
    },
    trigger: "blur"
  }
}

/** Element Plus 表单校验规则 — 手机号（非必填时为空则跳过） */
export function phoneRule(required = false) {
  return {
    validator: (_rule: any, value: string, callback: (err?: Error) => void) => {
      if (!value) {
        if (required) return callback(new Error("请填写联系电话"))
        return callback()
      }
      if (!validatePhone(value)) return callback(new Error("手机号格式不正确（需11位）"))
      callback()
    },
    trigger: "blur"
  }
}
