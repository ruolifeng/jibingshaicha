/** 筛查列表「人群分类」筛选项（支持多选，逗号分隔传参；单选仅匹配单纯分类，多选 AND 匹配） */
export const SCREENING_CROWD_CATEGORY_SEARCH_OPTIONS = [
  { label: "密接", value: "密接" },
  { label: "学生", value: "学生" },
  { label: "教职工", value: "教职工" },
  { label: "老年人", value: "老年人" },
  { label: "糖尿病", value: "糖尿病" },
  { label: "双感", value: "双感" },
  { label: "既往结核史", value: "既往结核史" },
  { label: "非重点人群", value: "非重点人群" }
]

/** 人群分类选项 */
export const CROWD_CATEGORY_OPTIONS = [
  "密接",
  "学生",
  "教职工",
  "老年人",
  "糖尿病",
  "双感",
  "既往结核",
  "非重点人群"
]

/** 推介/追踪 人群分类选项 */
export const REFERRAL_CROWD_CATEGORY_OPTIONS = [
  "学生",
  "密接",
  "教职工",
  "老年人",
  "糖尿病",
  "双感",
  "既往结核史",
  "非重点人群"
]

/** 患者管理：其它敏感方案（需手动录入详情） */
export const PATIENT_OTHER_SENSITIVE_PLAN = "其它敏感方案"

/** 患者管理治疗方案选项（通知单 / 首次随访 / 后续随访） */
export const TREATMENT_PLAN_OPTIONS = [
  "2HRZE/4HR",
  "2HRZE/7-10HRE",
  "2HRZE/10HRE",
  "6-9RZELfx",
  PATIENT_OTHER_SENSITIVE_PLAN
]

/** 是否为患者「其它敏感方案」（含历史「个体化方案」） */
export function isPatientOtherSensitivePlan(plan?: string | null): boolean {
  return plan === PATIENT_OTHER_SENSITIVE_PLAN || plan === "个体化方案"
}

/** 追踪状态 */
export const TRACKING_STATUS_MAP: Record<number, string> = {
  0: "待追踪",
  1: "到位",
  2: "未到位",
  3: "其他",
  4: "强制结束"
}

/** 疑似结核诊断结果（筛查/待诊断统一口径） */
export const SUSPECTED_TB_DIAGNOSIS = "疑似结核"

/** 是否为疑似结核诊断（兼容历史「疑似肺结核」） */
export function isSuspectedTbDiagnosis(diagnosis?: string | null): boolean {
  const value = diagnosis?.trim()
  return value === SUSPECTED_TB_DIAGNOSIS || value === "疑似肺结核"
}

/**
 * 诊断结果选项（V4新增，追踪到位后录入）
 * 排除/疑似结核 → 归档；确诊患者 → 结案（不进入患者管理）；潜伏感染者 → 潜伏感染管理；其他 → 备注归档
 */
export const DIAGNOSIS_RESULT_OPTIONS = [
  { label: "排除", value: "排除" },
  { label: SUSPECTED_TB_DIAGNOSIS, value: SUSPECTED_TB_DIAGNOSIS },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: "确诊患者", value: "确诊患者" },
  { label: "其他", value: "其他" }
]

/** 推介追踪 — 录入诊断结果选项（追踪到位后） */
export const REFERRAL_TRACKING_DIAGNOSIS_OPTIONS = [
  { label: "排除", value: "排除" },
  { label: "正常", value: "正常" },
  { label: SUSPECTED_TB_DIAGNOSIS, value: SUSPECTED_TB_DIAGNOSIS },
  { label: "确诊结核", value: "确诊结核" },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: "在治患者", value: "在治患者" }
] as const

/** 推介追踪诊断结果：是否为标红结案类（不进患者管理） */
export function isReferralConfirmedDiagnosis(result?: string | null): boolean {
  const text = (result || "").trim()
  return text === "确诊结核" || text === "确诊患者" || text === "在治患者"
}
/**
 * 转诊结果选项（前端驱动转诊弹窗，基于 diagnosisFirst 自动映射）
 * V4 新增 suspected（疑似结核）
 */
export const REFERRAL_RESULT_OPTIONS = [
  { label: "排除", value: "excluded" },
  { label: "其他", value: "other" },
  { label: SUSPECTED_TB_DIAGNOSIS, value: "suspected" },
  { label: "确诊患者", value: "confirmed" },
  { label: "潜伏感染者", value: "latent" }
]

/** 胸片检查结果选项 */
export const CHEST_XRAY_RESULT_OPTIONS = ["正常", "异常", "未查"]

/** 待诊断-确认诊断选项（学生筛查，保留「确诊患者」） */
export const SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS = [
  { label: "确诊患者", value: "确诊患者" },
  { label: "排除", value: "排除" },
  { label: "潜伏感染者", value: "潜伏感染者" }
]

/** 待诊断-确认诊断选项（重点人群/疫情筛查） */
export const KEY_SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS = [
  { label: "确诊结核", value: "确诊结核" },
  { label: "排除", value: "排除" },
  { label: "正常", value: "正常" },
  { label: SUSPECTED_TB_DIAGNOSIS, value: SUSPECTED_TB_DIAGNOSIS },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: "在治患者", value: "在治患者" }
]

/** 待诊断-确认诊断对应的转诊结果码 */
export const SUSPECTED_REFERRAL_RESULT_OPTIONS = [
  { label: "排除", value: "excluded" },
  { label: "确诊患者", value: "confirmed" },
  { label: "潜伏感染者", value: "latent" }
]

/** 待诊断-诊断中文 → 转诊码 */
export const SUSPECTED_DIAGNOSIS_TO_REFERRAL: Record<string, string> = {
  排除: "excluded",
  正常: "excluded",
  确诊患者: "confirmed",
  确诊结核: "confirmed",
  在治患者: "confirmed",
  [SUSPECTED_TB_DIAGNOSIS]: "suspected",
  潜伏感染者: "latent"
}

/** 筛查管理列表 — 重点人群/疫情筛查诊断结果（统一口径） */
export const SCREENING_DIAGNOSIS_SEARCH_OPTIONS = [
  { label: "排除", value: "排除" },
  { label: "正常", value: "正常" },
  { label: SUSPECTED_TB_DIAGNOSIS, value: SUSPECTED_TB_DIAGNOSIS },
  { label: "确诊结核", value: "确诊结核" },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: "在治患者", value: "在治患者" }
]

/** 筛查管理编辑 — 重点人群/疫情筛查诊断结果 */
export const SCREENING_DIAGNOSIS_EDIT_OPTIONS = [...SCREENING_DIAGNOSIS_SEARCH_OPTIONS]

/** 学生筛查 — 筛查结果/诊断（与秋季新生表口径一致，含历史「确诊患者」） */
export const SCHOOL_DIAGNOSIS_SEARCH_OPTIONS = [
  { label: "排除", value: "排除" },
  { label: "正常", value: "正常" },
  { label: SUSPECTED_TB_DIAGNOSIS, value: SUSPECTED_TB_DIAGNOSIS },
  { label: "确诊患者", value: "确诊患者" },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: "其他", value: "其他" }
]

export const SCHOOL_DIAGNOSIS_EDIT_OPTIONS = [...SCHOOL_DIAGNOSIS_SEARCH_OPTIONS]
/** 终态正常类诊断值列表（供后端筛选等使用） */
export const NORMAL_TERMINAL_DIAGNOSIS_VALUES = ["排除", "正常", "其它", "其他"]

/** 终态正常类诊断（排除/正常/其它），展示为「正常」 */
export const NORMAL_TERMINAL_DIAGNOSIS = new Set(NORMAL_TERMINAL_DIAGNOSIS_VALUES)

/** 筛查列表「待确诊/判定结果」列展示文案 */
export function getScreeningLatentStatusLabel(row: {
  isLatent?: number
  diagnosisFirst?: string
}): string {
  const diagnosis = row.diagnosisFirst?.trim()
  // 筛查结果码优先：确诊结核/确诊患者标红保留；3=已进潜伏感染管理
  if (diagnosis === "确诊结核" || diagnosis === "确诊患者" || diagnosis === "在治患者") return "已确诊患者"
  if (diagnosis === "潜伏感染者") return "已确诊潜伏感染者"
  if (row.isLatent !== 1) return "正常"
  if (diagnosis && NORMAL_TERMINAL_DIAGNOSIS.has(diagnosis)) return "正常"
  if (isSuspectedTbDiagnosis(diagnosis)) return "待诊断"
  return "待诊断"
}

/** 筛查列表「待确诊/判定结果」列标签颜色 */
export function getScreeningLatentStatusTagType(row: {
  isLatent?: number
  diagnosisFirst?: string
}): "success" | "warning" | "danger" {
  const diagnosis = row.diagnosisFirst?.trim()
  if (diagnosis === "确诊结核" || diagnosis === "确诊患者" || diagnosis === "在治患者") return "danger"
  if (diagnosis === "潜伏感染者") return "warning"
  if (row.isLatent !== 1) return "success"
  if (diagnosis && NORMAL_TERMINAL_DIAGNOSIS.has(diagnosis)) return "success"
  if (isSuspectedTbDiagnosis(diagnosis)) return "warning"
  return "warning"
}

/** 是否为筛查确诊患者（待诊断/筛查列表标红用） */
export function isConfirmedPatientDiagnosis(row: {
  diagnosisFirst?: string
  screeningDiagnosisFirst?: string
  referralResult?: string
  diagnosisResult?: string
}): boolean {
  return isConfirmedTbDiagnosisText(row.diagnosisFirst)
    || isConfirmedTbDiagnosisText(row.screeningDiagnosisFirst)
    || row.referralResult === "confirmed"
    || isConfirmedTbDiagnosisText(row.diagnosisResult)
}

function isConfirmedTbDiagnosisText(value?: string | null): boolean {
  const text = value?.trim()
  return text === "确诊结核" || text === "确诊患者" || text === "在治患者"
}

/** 确认诊断列若误存日期，统一为 yyyy-MM-dd，避免窄列换行裁切 */
function normalizeConfirmDiagnosisDisplay(value: string): string {
  const text = String(value).trim()
  const matched = text.match(/^(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})/)
  if (!matched) return text
  const [, year, month, day] = matched
  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`
}

/** 获取待诊断列表「确认诊断」列展示文本 */
export function getSuspectedConfirmDiagnosisLabel(row: {
  diagnosisFirst?: string
  screeningDiagnosisFirst?: string
  referralResult?: string
}): string {
  const draft = row.diagnosisFirst || row.screeningDiagnosisFirst
  if (draft) {
    if (NORMAL_TERMINAL_DIAGNOSIS.has(draft)) return "正常"
    if (isSuspectedTbDiagnosis(draft)) return SUSPECTED_TB_DIAGNOSIS
    if (draft === "确诊患者" || draft === "确诊结核") return draft
    if (draft === "在治患者") return "在治患者"
    const matched = SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS.find(o => o.value === draft)
      || KEY_SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS.find(o => o.value === draft)
    if (matched) return matched.label
    return normalizeConfirmDiagnosisDisplay(draft)
  }
  const referral = SUSPECTED_REFERRAL_RESULT_OPTIONS.find(o => o.value === row.referralResult)
  return referral?.label || "-"
}

/** 预防性治疗结果（V4新增，督导表字段） */
export const PREVENTIVE_RESULT_OPTIONS = [
  "规范完成",
  "失访",
  "自行中断治疗",
  "确诊肺结核"
]

/** 是否进行预防性治疗（督导表） */
export const PREVENTIVE_TREATMENT_YES_NO_OPTIONS = [
  { label: "是", value: "是" },
  { label: "否", value: "否" }
]

/** 预防性治疗期间随访管理人员（V4新增，督导表字段） */
export const PREVENTIVE_MANAGER_OPTIONS = [
  "医务人员（基层/疾控/定点医院）",
  "家庭成员",
  "志愿者（社区人员、学生等）",
  "智能辅助工具（电子药盒、手机等）",
  "自我管理"
]

/** 督导表类别选项（V5新增） */
export const SUPERVISION_CATEGORY_OPTIONS = [
  "密接",
  "新生筛查",
  "65岁以上老年人",
  "糖尿病人",
  "双感",
  "其他"
]

/** 督导方式选项（V5新增） */
export const SUPERVISION_METHOD_OPTIONS = [
  "门诊",
  "家庭访视",
  "电话",
  "视频",
  "其他"
]

/** 治疗完成情况选项（督导表） */
export const TREATMENT_COMPLETION_STATUS_OPTIONS = [
  "完成治疗",
  "失败",
  "死亡",
  "失访",
  "不良反应停药",
  "未评估"
]

/** 中断用药选项（V5新增） */
export const INTERRUPT_MEDICATION_OPTIONS = [
  { label: "有", value: "有" },
  { label: "无", value: "无" }
]

/** 督导管理人员类型选项（V5新增） */
export const SUPERVISION_MANAGER_TYPE_OPTIONS = [
  "医务人员",
  "家庭成员",
  "志愿者",
  "患者本人",
  "其他"
]

/** 管理方式选项 */
export const MANAGEMENT_METHOD_OPTIONS = ["全程督导", "强化期督导", "全程管理", "自服药"]

/** 督导人员选项 */
export const SUPERVISOR_OPTIONS = ["医生", "家属", "志愿者", "患者本人"]

/** 治疗前痰菌检查结果选项 */
export const SPUTUM_RESULT_OPTIONS = ["阴性", "阳性", "无结果", "未检查"]

/** 服药状态选项 */
export const MEDICATION_STATUS_OPTIONS = [
  { label: "按要求服药", value: 1 },
  { label: "不服药", value: 2 }
]

/** 治疗阶段 */
export const TREATMENT_PHASE_MAP: Record<number, string> = {
  0: "未开始",
  1: "预防治疗中",
  2: "已结案"
}

/** 按期检查周期选项 */
export const CHECK_PERIOD_OPTIONS = ["3个月", "6个月", "12个月"]

/** 按期检查结果选项 */
export const CHECK_RESULT_OPTIONS = ["未发病", "发病", "其他"]

/** 通知单状态 */
export const NOTICE_STATUS_MAP: Record<number, string> = {
  0: "草稿",
  1: "已发送",
  2: "已确认"
}

/** 潜伏感染者个体方案选项值 */
export const LATENT_INDIVIDUAL_PLAN = "个体方案（需手动录入）"

/** 历史治疗方案文案 → 现行标准选项 */
const LATENT_TREATMENT_PLAN_LEGACY: Record<string, string> = {
  "3HR/4R": "4R",
  "母牛分枝杆菌": "注射用母牛分枝杆菌(微卡）"
}

/** 潜伏感染者治疗方案选项（督导表 / 通知单 / 密接个案预防性治疗方案共用） */
export const LATENT_TREATMENT_PLAN_OPTIONS = [
  "6H/9H",
  "3HR",
  "4R",
  "3HP",
  "6Lfx",
  "注射用母牛分枝杆菌(微卡）",
  LATENT_INDIVIDUAL_PLAN,
  "不服药"
]

/** @deprecated 请使用 LATENT_TREATMENT_PLAN_OPTIONS */
export const LATENT_TREATMENT_OPTIONS = LATENT_TREATMENT_PLAN_OPTIONS

/** 判断是否为潜伏感染者个体方案（含历史「个体化方案」） */
export function isLatentIndividualPlan(plan?: string): boolean {
  return plan === LATENT_INDIVIDUAL_PLAN || plan === "个体化方案"
}

/** 兼容历史存储的治疗方案文案 */
export function normalizeLatentTreatmentPlan(plan?: string | null): string {
  if (!plan) return ""
  return LATENT_TREATMENT_PLAN_LEGACY[plan] ?? plan
}

/** 解析潜伏感染者通知单治疗方案（表单回填） */
export function parseLatentNoticeTreatmentPlan(
  treatmentPlan?: string,
  customPlanDetail?: string
): { treatmentPlan: string, customPlanDetail: string } {
  const tp = normalizeLatentTreatmentPlan(treatmentPlan)
  if (tp && !LATENT_TREATMENT_PLAN_OPTIONS.includes(tp)) {
    return {
      treatmentPlan: LATENT_INDIVIDUAL_PLAN,
      customPlanDetail: customPlanDetail || tp
    }
  }
  if (isLatentIndividualPlan(tp)) {
    return {
      treatmentPlan: LATENT_INDIVIDUAL_PLAN,
      customPlanDetail: customPlanDetail || ""
    }
  }
  return {
    treatmentPlan: tp,
    customPlanDetail: customPlanDetail || ""
  }
}

/** 潜伏感染者通知单提交时格式化治疗方案 */
export function formatLatentNoticeTreatmentPlan(treatmentPlan: string, customPlanDetail?: string): string {
  if (isLatentIndividualPlan(treatmentPlan)) {
    return customPlanDetail || treatmentPlan
  }
  return treatmentPlan
}

/** 解析潜伏感染者督导表治疗方案（表单回填） */
export function parseLatentSupervisionTreatmentPlan(plan?: string): { treatmentPlan: string, customPlanDetail: string } {
  if (!plan) return { treatmentPlan: "", customPlanDetail: "" }
  plan = normalizeLatentTreatmentPlan(plan)

  const legacyPrefix = "个体化方案："
  const newPrefix = `${LATENT_INDIVIDUAL_PLAN}：`
  if (plan.startsWith(legacyPrefix)) {
    return {
      treatmentPlan: LATENT_INDIVIDUAL_PLAN,
      customPlanDetail: plan.slice(legacyPrefix.length)
    }
  }
  if (plan.startsWith(newPrefix)) {
    return {
      treatmentPlan: LATENT_INDIVIDUAL_PLAN,
      customPlanDetail: plan.slice(newPrefix.length)
    }
  }
  if (isLatentIndividualPlan(plan)) {
    return { treatmentPlan: LATENT_INDIVIDUAL_PLAN, customPlanDetail: "" }
  }
  if (!LATENT_TREATMENT_PLAN_OPTIONS.includes(plan)) {
    return { treatmentPlan: LATENT_INDIVIDUAL_PLAN, customPlanDetail: plan }
  }
  return { treatmentPlan: plan, customPlanDetail: "" }
}

/** 潜伏感染者督导表提交时格式化治疗方案 */
export function formatLatentSupervisionTreatmentPlan(treatmentPlan: string, customPlanDetail?: string): string {
  if (isLatentIndividualPlan(treatmentPlan)) {
    return customPlanDetail
      ? `${LATENT_INDIVIDUAL_PLAN}：${customPlanDetail}`
      : LATENT_INDIVIDUAL_PLAN
  }
  return treatmentPlan
}

/** 患者类型（患者通知单） */
export const PATIENT_TYPE_OPTIONS = ["初治", "复治"]

/**
 * 解析患者治疗方案（表单回填）
 * - 标准选项原样回填
 * - 「其它敏感方案：xxx」/「个体化方案：xxx」前缀解析为选项 + 详情
 * - 旧选项（FDC-*、不服药、个体化方案等）及自由文本 → 其它敏感方案 + 详情
 */
export function parsePatientTreatmentPlan(
  plan?: string | null,
  customPlanDetail?: string | null
): { treatmentPlan: string, customPlanDetail: string } {
  const tp = (plan || "").trim()
  const detail = (customPlanDetail || "").trim()
  if (!tp) {
    return { treatmentPlan: "", customPlanDetail: detail }
  }

  const legacyPrefix = "个体化方案："
  const otherPrefix = `${PATIENT_OTHER_SENSITIVE_PLAN}：`
  if (tp.startsWith(legacyPrefix)) {
    return {
      treatmentPlan: PATIENT_OTHER_SENSITIVE_PLAN,
      customPlanDetail: detail || tp.slice(legacyPrefix.length)
    }
  }
  if (tp.startsWith(otherPrefix)) {
    return {
      treatmentPlan: PATIENT_OTHER_SENSITIVE_PLAN,
      customPlanDetail: detail || tp.slice(otherPrefix.length)
    }
  }
  if (isPatientOtherSensitivePlan(tp)) {
    return {
      treatmentPlan: PATIENT_OTHER_SENSITIVE_PLAN,
      customPlanDetail: detail
    }
  }
  if (!TREATMENT_PLAN_OPTIONS.includes(tp)) {
    return {
      treatmentPlan: PATIENT_OTHER_SENSITIVE_PLAN,
      customPlanDetail: detail || tp
    }
  }
  return {
    treatmentPlan: tp,
    customPlanDetail: detail
  }
}

/** 患者通知单 / 随访：将治疗方案字符串回填到表单（含其它敏感方案） */
export function applyPatientNoticeTreatmentPlan(
  form: { treatmentPlan: string, customPlanDetail: string },
  plan?: string | null,
  customPlanDetail?: string | null
) {
  const parsed = parsePatientTreatmentPlan(plan, customPlanDetail)
  form.treatmentPlan = parsed.treatmentPlan
  form.customPlanDetail = parsed.customPlanDetail
}

/**
 * 患者治疗方案保存：选「其它敏感方案」时提交详情文本（与历史个体化逻辑一致）
 */
export function resolvePatientTreatmentPlanForSave(
  plan?: string | null,
  detail?: string | null
): string {
  if (isPatientOtherSensitivePlan(plan)) {
    return (detail || "").trim() || PATIENT_OTHER_SENSITIVE_PLAN
  }
  return (plan || "").trim()
}

/** 患者通知单管理方式 */
export const PATIENT_MANAGEMENT_METHOD_OPTIONS = ["全程督导", "强化督导", "全程管理", "未管理"]

/** 病原学/病理学检查结果（通知单痰涂片、痰培养等表单选项） */
export const PATHOGEN_RESULT_OPTIONS = ["未出结果", "阴性", "阳性", "病原学结果阳性", "未做", "未知"]

/**
 * 患者列表「病原学结果」筛选项（上方搜索与表头筛选共用）。
 * 不含「病原学阳性/阴性」等别名——选「阳性/阴性」时后端会自动匹配别名。
 * 「-」表示列表展示为空（无病原学结果）。
 */
export const PATHOGEN_RESULT_FILTER_OPTIONS = [
  ...PATHOGEN_RESULT_OPTIONS,
  "结核性胸膜炎",
  "-"
] as const

/** 在管总览 — 通知单状态筛选 */
export const PATIENT_NOTICE_STATUS_FILTER_OPTIONS = [
  { label: "未发送", value: "none" },
  { label: "草稿", value: "0" },
  { label: "已发送", value: "1" },
  { label: "已确认", value: "2" }
] as const

/** 在管总览 — 首次随访 / 后续随访完成情况筛选 */
export const PATIENT_VISIT_STATUS_FILTER_OPTIONS = [
  { label: "待填写", value: "pending" },
  { label: "已完成", value: "done" }
] as const

/** 在管总览 — 服药管理完成情况筛选 */
export const PATIENT_MEDICATION_STATUS_FILTER_OPTIONS = ["待填写", "进行中", "已完成"] as const

/** 感染检查方法（通知单等短码） */
export const INFECTION_METHOD_OPTIONS = ["PPD", "EC", "IGRA"]

/**
 * 重点人群 / 疫情筛查 / 密接个案 — 感染筛查方法（官方下拉）
 */
export const KEY_INFECTION_SCREEN_METHOD_OPTIONS = [
  "结核菌素皮肤试验_PPD",
  "结核抗原皮肤试验_EC",
  "γ干扰素释放试验_IGRA",
  "未做"
] as const

/**
 * 重点人群 / 疫情筛查 / 密接个案 — 感染检测结果 / 结果判定（官方下拉）
 */
export const KEY_INFECTION_JUDGE_RESULT_OPTIONS = [
  "一般阳性",
  "中度阳性",
  "强阳性",
  "阳性",
  "阴性",
  "未判读"
] as const

/** 展示感染筛查方法：短码/别名 → 官方下拉文案 */
export function displayInfectionScreenMethod(screenMethod?: string | null, infectionResult?: string | null): string {
  const method = (screenMethod || "").trim()
  if (method) {
    const upper = method.toUpperCase()
    if (method === "未做" || method === "未查") return "未做"
    if (KEY_INFECTION_SCREEN_METHOD_OPTIONS.includes(method as typeof KEY_INFECTION_SCREEN_METHOD_OPTIONS[number])) {
      return method
    }
    if (upper.includes("IGRA") || method.includes("干扰素")) return "γ干扰素释放试验_IGRA"
    if (upper === "EC" || upper.includes("EC") || method.includes("结核抗原")) return "结核抗原皮肤试验_EC"
    if (upper === "PPD" || upper.includes("PPD") || method.includes("结核菌素")) return "结核菌素皮肤试验_PPD"
    return method
  }
  const result = (infectionResult || "").trim()
  if (/IGRA/i.test(result)) return "γ干扰素释放试验_IGRA"
  if (/^EC/i.test(result) || /EC阳|EC阴/.test(result)) return "结核抗原皮肤试验_EC"
  if (/^PPD/i.test(result) || /PPD/.test(result)) return "结核菌素皮肤试验_PPD"
  return "-"
}

/** 展示结果判定：历史文案 → 官方下拉 */
export function displayInfectionJudgeResult(infectionResult?: string | null): string {
  const raw = (infectionResult || "").trim()
  if (!raw) return "-"
  if (KEY_INFECTION_JUDGE_RESULT_OPTIONS.includes(raw as typeof KEY_INFECTION_JUDGE_RESULT_OPTIONS[number])) {
    return raw
  }
  if (raw === "无法判读" || raw === "未判读") return "未判读"
  if (raw === "未感染") return "阴性"
  if (raw === "感染") return "阳性"
  if (raw.includes("PPD+++") || raw.includes("强阳")) return "强阳性"
  if (raw.includes("PPD++") || raw.includes("中度阳")) return "中度阳性"
  if (raw.includes("PPD+") || raw.includes("一般阳")) return "一般阳性"
  if (/阳性/.test(raw)) return "阳性"
  if (/阴性/.test(raw)) return "阴性"
  return raw
}

/** 通知单/编辑下拉：从多个候选值里取出可展示的结果判定 */
export function resolveInfectionJudgeSelectValue(...candidates: unknown[]): string {
  for (const candidate of candidates) {
    const display = displayInfectionJudgeResult(typeof candidate === "string" ? candidate : "")
    if (display && display !== "-") return display
  }
  return ""
}

/** 通知单/编辑下拉：官方选项 + 当前历史值（若尚未归一） */
export function infectionJudgeSelectOptions(current?: string | null): string[] {
  const options = [...KEY_INFECTION_JUDGE_RESULT_OPTIONS]
  const value = (current || "").trim()
  if (value && !options.includes(value as typeof KEY_INFECTION_JUDGE_RESULT_OPTIONS[number])) {
    return [value, ...options]
  }
  return options
}

/** 学生筛查 — 学校类型（2026 秋季新生入学表） */
export const SCHOOL_TYPE_OPTIONS = [
  "托幼机构",
  "小学",
  "初中",
  "高中阶段教育学校",
  "高等教育学校",
  "教职工",
  "其他"
]

/** 学生筛查 — 是否寄宿制 */
export const SCHOOL_BOARDING_TYPE_OPTIONS = ["寄宿制", "非寄宿制", "大学", "其他"]

/** 学生筛查 — 感染筛查方法（含未查） */
export const SCHOOL_SCREEN_METHOD_OPTIONS = ["PPD", "EC", "IGRA", "未查"]

/** 学生筛查 — 判定结果 */
export const SCHOOL_INFECTION_JUDGE_OPTIONS = ["未感染", "感染", "无法判读", "未查"]

/** 学生筛查 — 胸部影像学方法 */
export const SCHOOL_CHEST_METHOD_OPTIONS = ["胸部X线", "胸部CT", "其他", "未查"]

/** 学生筛查 — 胸部影像学结果（细分类） */
export const SCHOOL_CHEST_RESULT_OPTIONS = [
  "未见异常",
  "异常（疑似活动性结核病变）",
  "异常（非活动性结核病变）",
  "其他",
  "未查",
  // 兼容历史
  "正常",
  "异常"
]

/** 学生筛查 — 分子生物学/痰培养 */
export const SCHOOL_LAB_RESULT_OPTIONS = ["阴性", "阳性", "无法判读", "未查"]

/**
 * 学生筛查列表 — 表头点击展示的数字码/填写说明
 * （对齐《2026年秋季新生入学结核病筛查记录表新》第 5 行说明）
 */
export const SCHOOL_SCREENING_FIELD_HINTS = {
  schoolType: "填写数字，1=托幼机构，2=小学，3=初中，4=高中阶段教育学校，5=高等教育学校，6=教职工，7=其他（培训学校、特殊教育学校和专门学校等）",
  boardingType: "填写数字，1=寄宿制，2=非寄宿制，3=大学，4=其他",
  participatedScreening: "填写是/否",
  tbHistory: "填写有/无",
  closeContactHistory: "填写有/无",
  symptomCough: "填写有/无",
  symptomHemoptysis: "填写有/无",
  symptomOther: "填写有/无",
  screenMethod: "填写数字，1=结核菌素纯蛋白衍生物（PPD），2=重组结核分枝杆菌融合蛋白（EC），3=γ-干扰素释放试验（IGRA），4=未查",
  screenResult: "PPD填写横径×纵径（mm）及有无双圈、水泡、坏死、淋巴管炎等；EC和IGRA填写阳性/阴性；未查填写「无」",
  infectionResult: "填写数字，0=未感染，1=感染，2=无法判读，3=未查",
  chestXrayMethod: "填写数字，1=胸部X线，2=胸部CT，3=其他（需注明），4=未查",
  chestXrayResult: "填写数字，0=未见异常，1=异常（疑似活动性结核病变），2=异常（非活动性结核病变），3=其他（需注明），4=未查",
  molecularBiologyResult: "填写数字，0=阴性，1=阳性，2=无法判读，3=未查",
  sputumCultureResult: "0=阴性，1=阳性，2=无法判读，3=未查",
  diagnosisFirst: "填写数字，0=未发现异常，1=活动性肺结核，2=疑似肺结核，3=潜伏感染者，4=其他（需注明）"
} as const

/** 填写说明弹窗：字段与《2026年秋季新生入学结核病筛查记录表新》说明行一致 */
export const SCHOOL_SCREENING_FILL_INSTRUCTIONS = [
  { field: "类型", hint: SCHOOL_SCREENING_FIELD_HINTS.schoolType },
  { field: "是否寄宿制", hint: SCHOOL_SCREENING_FIELD_HINTS.boardingType },
  { field: "是否参加筛查", hint: SCHOOL_SCREENING_FIELD_HINTS.participatedScreening },
  { field: "有无既往结核病史", hint: SCHOOL_SCREENING_FIELD_HINTS.tbHistory },
  { field: "有无肺结核接触史", hint: SCHOOL_SCREENING_FIELD_HINTS.closeContactHistory },
  { field: "咳嗽，咳痰≥两周", hint: SCHOOL_SCREENING_FIELD_HINTS.symptomCough },
  { field: "咯血或血痰", hint: SCHOOL_SCREENING_FIELD_HINTS.symptomHemoptysis },
  { field: "可疑症状-其他", hint: SCHOOL_SCREENING_FIELD_HINTS.symptomOther },
  { field: "感染筛查-方法", hint: SCHOOL_SCREENING_FIELD_HINTS.screenMethod },
  { field: "感染筛查-结果", hint: SCHOOL_SCREENING_FIELD_HINTS.screenResult },
  { field: "判定结果", hint: SCHOOL_SCREENING_FIELD_HINTS.infectionResult },
  { field: "胸部影像学-方法", hint: SCHOOL_SCREENING_FIELD_HINTS.chestXrayMethod },
  { field: "胸部影像学-结果", hint: SCHOOL_SCREENING_FIELD_HINTS.chestXrayResult },
  { field: "分子生物学结果", hint: SCHOOL_SCREENING_FIELD_HINTS.molecularBiologyResult },
  { field: "痰培养结果", hint: SCHOOL_SCREENING_FIELD_HINTS.sputumCultureResult },
  { field: "筛查结果", hint: SCHOOL_SCREENING_FIELD_HINTS.diagnosisFirst }
]

/** 有无类选项 */
export const YES_NO_HAVE_OPTIONS = ["有", "无"]
export const YES_NO_OPTIONS = ["是", "否"]

/** 密接人群阳性轮次 */
export const ACTIVE_ROUND_MAP: Record<number, string> = {
  1: "首次",
  2: "半年后",
  3: "一年后"
}

// ==================== 患者随访相关 ====================

/** 随访方式（首次入户随访） */
export const VISIT_METHOD_OTHER = "其他"

export const VISIT_METHOD_OPTIONS = ["门诊", "家庭", VISIT_METHOD_OTHER]

/** 后续随访方式（可电话随访） */
export const FOLLOW_UP_METHOD_OPTIONS = ["门诊", "家庭", "电话", VISIT_METHOD_OTHER]

/** 痰菌情况 */
export const SPUTUM_STATUS_OPTIONS = ["阳性", "阴性", "未查痰"]

/** 痰培养选项（与通知单病原学选项一致） */
export const SPUTUM_CULTURE_OPTIONS = PATHOGEN_RESULT_OPTIONS

/** 痰培养「未做」标记值 */
export const SPUTUM_CULTURE_NOT_DONE = "未做"

/** 耐药情况 */
export const DRUG_RESISTANCE_OPTIONS = ["耐药", "非耐药", "未检测"]

/** 症状及体征（第一次入户随访） */
export const SYMPTOM_OPTIONS = [
  { value: "0", label: "没有症状" },
  { value: "1", label: "咳嗽咳痰" },
  { value: "2", label: "低热盗汗" },
  { value: "3", label: "咯血或血痰" },
  { value: "4", label: "胸痛消瘦" },
  { value: "5", label: "恶心纳差" },
  { value: "6", label: "头痛失眠" },
  { value: "7", label: "视物模糊" },
  { value: "8", label: "皮肤瘙痒、皮疹" },
  { value: "9", label: "耳鸣、听力下降" }
]

/** 用法 */
export const MEDICATION_USAGE_OPTIONS = ["每日", "间歇"]

/** 药品剂型 */
export const DRUG_FORM_OPTIONS = ["固定剂量复合制剂", "散装药", "板式组合药", "注射剂"]

/** 领药记录 — 抗结核药品名称 */
export const MEDICATION_PICKUP_DRUG_OPTIONS = [
  "HRZE（固定剂量复合制剂）",
  "HR（固定剂量复合制剂）",
  "异烟肼",
  "利福平",
  "乙胺丁醇",
  "吡嗪酰胺",
  "链霉素",
  "利福喷汀",
  "左氧氟沙星",
  "莫西沙星",
  "利奈唑胺",
  "环丝氨酸",
  "对氨基水杨酸"
]

/** 领药记录 — 领取数量单位 */
export const MEDICATION_PICKUP_UNIT_OPTIONS = ["盒", "瓶", "支", "板", "袋", "包", "颗", "粒", "片"]

/** 首次随访督导人员 */
export const FIRST_VISIT_SUPERVISOR_OPTIONS = ["医生", "家属", "自服药", "其他"]

/** 居室通风情况 */
export const VENTILATION_OPTIONS = ["良好", "一般", "差"]

/** 健康教育培训项目 */
export const EDUCATION_ITEMS = [
  "服药记录卡的填写",
  "服药方法及药品存放",
  "肺结核治疗疗程",
  "不规律服药危害",
  "服药后不良反应及处理",
  "治疗期间复诊查痰",
  "外出期间如何坚持服药",
  "生活习惯及注意事项",
  "密切接触者检查"
]

/** 角色映射 */
export const ROLE_MAP: Record<number, string> = {
  1: "超级管理员",
  2: "一级",
  3: "二级",
  4: "三级",
  5: "四级",
  6: "五级"
}

export const ROLE_OPTIONS = [
  { label: "超级管理员", value: 1 },
  { label: "一级", value: 2 },
  { label: "二级", value: 3 },
  { label: "三级", value: 4 },
  { label: "四级", value: 5 },
  { label: "五级", value: 6 }
]

// ==================== 操作日志（V13） ====================

/** 操作日志类型 — 严格按用户原文 5 类 */
export const OP_LOG_TYPE_OPTIONS = [
  { label: "登录", value: "login", tagType: "info" as const },
  { label: "导入", value: "import", tagType: "primary" as const },
  { label: "删除", value: "delete", tagType: "danger" as const },
  { label: "修改", value: "update", tagType: "warning" as const },
  { label: "导出", value: "export", tagType: "success" as const }
]

/** 操作日志类型 → 中文标签 */
export const OP_LOG_TYPE_LABEL: Record<string, string> = {
  login: "登录",
  import: "导入",
  delete: "删除",
  update: "修改",
  export: "导出",
  // 扩展位（按文档保留，默认未启用）
  create: "新增",
  logout: "登出"
}

/** 业务模块下拉（与后端 op_module 一致） */
export const OP_LOG_MODULE_OPTIONS = [
  { label: "筛查", value: "screening" },
  { label: "潜伏感染", value: "latent" },
  { label: "患者管理", value: "patient" },
  { label: "推介追踪", value: "referral" },
  { label: "系统", value: "system" },
  { label: "统计", value: "statistics" }
]

// ==================== 后续随访（V15，按《后续随访服务记录表》Excel 模板） ====================

/** 督导人员（1医生/2家属/3自服药/4其他） */
export const FOLLOW_UP_SUPERVISOR_OPTIONS = [
  { value: "1", label: "医生" },
  { value: "2", label: "家属" },
  { value: "3", label: "自服药" },
  { value: "4", label: "其他" }
]

/** 随访方式（V15，对齐模板的 1门诊/2家庭/3电话/4其他） */
export const FOLLOW_UP_VISIT_METHOD_OTHER = "4"

export const FOLLOW_UP_VISIT_METHOD_OPTIONS = [
  { value: "1", label: "门诊" },
  { value: "2", label: "家庭" },
  { value: "3", label: "电话" },
  { value: FOLLOW_UP_VISIT_METHOD_OTHER, label: "其他" }
]

/** 后续随访 — 症状及体征（多选，0-11） */
export const FOLLOW_UP_SYMPTOM_OPTIONS = [
  { value: "0", label: "没有症状" },
  { value: "1", label: "咳嗽咳痰" },
  { value: "2", label: "低热盗汗" },
  { value: "3", label: "咯血或血痰" },
  { value: "4", label: "胸痛消瘦" },
  { value: "5", label: "恶心纳差" },
  { value: "6", label: "关节疼痛" },
  { value: "7", label: "头痛失眠" },
  { value: "8", label: "视物模糊" },
  { value: "9", label: "皮肤瘙痒、皮疹" },
  { value: "10", label: "耳鸣、听力下降" },
  { value: "11", label: "其它" }
]

/** 用法 */
export const FOLLOW_UP_MEDICATION_USAGE_OPTIONS = [
  { value: "1", label: "每日" },
  { value: "2", label: "间歇" }
]

/** 药品剂型 */
export const FOLLOW_UP_DRUG_FORM_OPTIONS = [
  { value: "1", label: "固定剂量复合制剂" },
  { value: "2", label: "散装药" },
  { value: "3", label: "板式组合药" },
  { value: "4", label: "注射剂" }
]

/** 后续随访：有无（1无/2有） */
export const FOLLOW_UP_YES_NO_HAVE_OPTIONS = [
  { value: "1", label: "无" },
  { value: "2", label: "有" }
]

/** 是否停止治疗 */
export const STOP_TREATMENT_YES_NO_OPTIONS = [
  { value: "是", label: "是" },
  { value: "否", label: "否" }
]

/** 停止治疗原因 */
export const STOP_TREATMENT_REASON_OPTIONS = [
  { value: "完成疗程", label: "完成疗程" },
  { value: "死亡", label: "死亡" },
  { value: "丢失", label: "丢失" },
  { value: "转入耐多药治疗", label: "转入耐多药治疗" },
  { value: "其它", label: "其它" }
]

/** V16/V17 数据来源（populationType）标签映射，用于聚合列表的"数据来源"列 */
export const POPULATION_TYPE_LABEL_MAP: Record<string, { label: string, type: "primary" | "success" | "warning" | "danger" | "info" }> = {
  school: { label: "学生筛查", type: "primary" },
  keyPopulation: { label: "重点人群", type: "success" },
  regular: { label: "疫情筛查", type: "warning" },
  epidemic: { label: "大疫情", type: "danger" },
  referral: { label: "推介", type: "info" },
  closeContact: { label: "密接", type: "info" },
  specialDisease: { label: "专病网", type: "warning" },
  other: { label: "其它", type: "info" }
}

/** 潜伏感染者手动新增/导入可选数据来源 */
export const LATENT_MANUAL_POPULATION_TYPE_OPTIONS = [
  { label: "学生筛查", value: "school" },
  { label: "重点人群", value: "keyPopulation" },
  { label: "疫情筛查", value: "regular" },
  { label: "大疫情", value: "epidemic" },
  { label: "推介", value: "referral" },
  { label: "密接", value: "closeContact" },
  { label: "其它", value: "other" }
]

/** 潜伏感染者手动新增 — 重点人群子分类（可多选） */
export const LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS = ["老年人", "糖尿病", "双感"] as const

/** 潜伏感染者手动新增 — 密接类型（单选） */
export const LATENT_CLOSE_CONTACT_TYPE_OPTIONS = ["家庭内", "家庭外"] as const

/** 获取 populationType 对应的 label */
export function getPopulationTypeLabel(type: string): string {
  return POPULATION_TYPE_LABEL_MAP[type]?.label ?? type
}

/** 获取数据来源展示标签（含重点人群/密接细分） */
export function getLatentPopulationDisplayLabel(populationType: string, crowdCategory?: string | null): string {
  const base = getPopulationTypeLabel(populationType)
  if (!crowdCategory) return base
  if (populationType === "keyPopulation") {
    return crowdCategory
      .split(/[、,，/]/)
      .map(part => part.trim())
      .filter(Boolean)
      .map(part => `${base}-${part}`)
      .join("、")
  }
  if (populationType === "closeContact" && LATENT_CLOSE_CONTACT_TYPE_OPTIONS.includes(crowdCategory as typeof LATENT_CLOSE_CONTACT_TYPE_OPTIONS[number])) {
    return `${base}-${crowdCategory}`
  }
  return base
}

/** 获取 populationType 对应的 tag type */
export function getPopulationTypeTagType(type: string) {
  return POPULATION_TYPE_LABEL_MAP[type]?.type ?? "info"
}
