/** 推介/追踪 — 感染检测方法 */
export const REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS = [
  "结核菌素皮肤试验_PPD",
  "结核抗原皮肤试验_EC",
  "Y干扰素释放试验_IGRA",
  "未做"
] as const

/** 历史短码 / 别名 → 当前感染检测方法 */
const REFERRAL_SCREEN_METHOD_LEGACY_MAP: Record<string, string> = {
  "PPD": "结核菌素皮肤试验_PPD",
  "EC": "结核抗原皮肤试验_EC",
  "IGRA": "Y干扰素释放试验_IGRA",
  "未查": "未做",
  "未做": "未做",
  "结核菌素皮肤试验_PPD": "结核菌素皮肤试验_PPD",
  "结核抗原皮肤试验_EC": "结核抗原皮肤试验_EC",
  "Y干扰素释放试验_IGRA": "Y干扰素释放试验_IGRA",
  "γ干扰素释放试验_IGRA": "Y干扰素释放试验_IGRA",
  "γ-干扰素释放试验（IGRA）": "Y干扰素释放试验_IGRA"
}

/** 将历史筛查方法值规范为当前下拉选项（无法识别则原样返回） */
export function normalizeReferralScreenMethod(method?: string | null): string {
  const raw = (method || "").trim()
  if (!raw) return ""
  if (REFERRAL_SCREEN_METHOD_LEGACY_MAP[raw]) {
    return REFERRAL_SCREEN_METHOD_LEGACY_MAP[raw]
  }
  const upper = raw.toUpperCase()
  if (upper === "PPD" || raw.includes("PPD") || raw.includes("结核菌素")) {
    return "结核菌素皮肤试验_PPD"
  }
  if (upper === "EC" || raw.includes("EC") || raw.includes("结核抗原")) {
    return "结核抗原皮肤试验_EC"
  }
  if (upper.includes("IGRA") || raw.includes("干扰素")) {
    return "Y干扰素释放试验_IGRA"
  }
  if (raw === "未查" || raw === "未做") return "未做"
  return raw
}

/** 推介/追踪 — 感染检测结果 */
export const REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS = [
  "一般阳性",
  "中度阳性",
  "强阳性",
  "阳性",
  "阴性",
  "未判读"
] as const

/** 无结果 → 未判读；其余历史值原样保留，由下拉 legacy 兼容展示 */
export function normalizeReferralInfectionResult(result?: string | null): string {
  const raw = (result || "").trim()
  if (!raw) return ""
  if (raw === "无结果") return "未判读"
  return raw
}

/** 推介/追踪 — 胸片检查结果「其他」 */
export const REFERRAL_CHEST_XRAY_OTHER = "其他（手动写备注）"

/** 推介/追踪 — 胸片检查结果 */
export const REFERRAL_CHEST_XRAY_RESULT_OPTIONS = [
  "未见异常",
  "疑似活动性结核病变",
  "非活动性结核病变",
  REFERRAL_CHEST_XRAY_OTHER
] as const

const REFERRAL_CHEST_XRAY_OTHER_PREFIX = `${REFERRAL_CHEST_XRAY_OTHER}：`

/** 历史胸片结果 → 当前选项（无法映射则原样保留） */
export function normalizeReferralChestXrayResult(result?: string | null): string {
  const raw = (result || "").trim()
  if (!raw) return ""
  if (raw === "正常") return "未见异常"
  if (raw.startsWith(REFERRAL_CHEST_XRAY_OTHER_PREFIX) || raw === REFERRAL_CHEST_XRAY_OTHER) {
    return REFERRAL_CHEST_XRAY_OTHER
  }
  if (raw.startsWith("其他：") || raw.startsWith("其他（需注明）")) {
    return REFERRAL_CHEST_XRAY_OTHER
  }
  return raw
}

/** 回填胸片结果到表单（含「其他」备注） */
export function applyReferralChestXrayResult(
  form: { chestXrayResult: string, chestXrayRemark: string },
  value?: string | null
) {
  const raw = (value || "").trim()
  if (!raw) {
    form.chestXrayResult = ""
    form.chestXrayRemark = ""
    return
  }
  if (raw === "正常") {
    form.chestXrayResult = "未见异常"
    form.chestXrayRemark = ""
    return
  }
  if (raw.startsWith(REFERRAL_CHEST_XRAY_OTHER_PREFIX)) {
    form.chestXrayResult = REFERRAL_CHEST_XRAY_OTHER
    form.chestXrayRemark = raw.slice(REFERRAL_CHEST_XRAY_OTHER_PREFIX.length).trim()
    return
  }
  if (raw === REFERRAL_CHEST_XRAY_OTHER) {
    form.chestXrayResult = REFERRAL_CHEST_XRAY_OTHER
    form.chestXrayRemark = ""
    return
  }
  if (raw.startsWith("其他：")) {
    form.chestXrayResult = REFERRAL_CHEST_XRAY_OTHER
    form.chestXrayRemark = raw.slice(3).trim()
    return
  }
  form.chestXrayResult = raw
  form.chestXrayRemark = ""
}

/** 保存时合并「其他」备注到 chestXrayResult */
export function resolveReferralChestXrayResultForSave(result: string, remark?: string): string {
  const selected = (result || "").trim()
  if (selected !== REFERRAL_CHEST_XRAY_OTHER) return selected
  const detail = (remark || "").trim()
  return detail ? `${REFERRAL_CHEST_XRAY_OTHER_PREFIX}${detail}` : REFERRAL_CHEST_XRAY_OTHER
}

export function isReferralChestXrayOther(result?: string | null): boolean {
  return (result || "").trim() === REFERRAL_CHEST_XRAY_OTHER
}

/** 下拉兼容历史值（如旧数据短码） */
export function referralSelectOptionsWithLegacy(
  options: readonly string[],
  current?: string
): string[] {
  if (current && !options.includes(current)) {
    return [current, ...options]
  }
  return [...options]
}
