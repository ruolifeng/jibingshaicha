/** 筛查列表「人群分类」筛选项（支持多选，逗号分隔传参） */
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

/** 治疗方案选项（8种，V5新增"不服药"） */
export const TREATMENT_PLAN_OPTIONS = [
  "FDC-2HRZE/4HR",
  "2HRZE/4HR",
  "FDC-2HRZES/6HRE",
  "2HRZES/6HRE",
  "FDC-2HRZE/10HRE",
  "2HRZE/10HRE",
  "个体化方案",
  "不服药"
]

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
  { label: "确诊患者", value: "确诊患者" },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: "其他", value: "其他" }
] as const

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

/** 待诊断-确认诊断选项（学生/重点/疫情筛查） */
export const SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS = [
  { label: "确诊患者", value: "确诊患者" },
  { label: "排除", value: "排除" },
  { label: "潜伏感染者", value: "潜伏感染者" }
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
  确诊患者: "confirmed",
  潜伏感染者: "latent"
}

/** 筛查管理列表 — 诊断结果搜索选项 */
export const SCREENING_DIAGNOSIS_SEARCH_OPTIONS = [
  { label: "排除", value: "排除" },
  { label: "正常", value: "正常" },
  { label: SUSPECTED_TB_DIAGNOSIS, value: SUSPECTED_TB_DIAGNOSIS },
  { label: "确诊患者", value: "确诊患者" },
  { label: "潜伏感染者", value: "潜伏感染者" }
]

/** 筛查管理编辑 — 诊断结果选项（含「其他」兼容历史数据） */
export const SCREENING_DIAGNOSIS_EDIT_OPTIONS = [
  ...SCREENING_DIAGNOSIS_SEARCH_OPTIONS,
  { label: "其他", value: "其他" }
]
/** 终态正常类诊断值列表（供后端筛选等使用） */
export const NORMAL_TERMINAL_DIAGNOSIS_VALUES = ["排除", "正常", "其它", "其他"]

/** 终态正常类诊断（排除/正常/其它），展示为「正常」 */
export const NORMAL_TERMINAL_DIAGNOSIS = new Set(NORMAL_TERMINAL_DIAGNOSIS_VALUES)

/** 筛查列表「待确诊/判定结果」列展示文案 */
export function getScreeningLatentStatusLabel(row: {
  isLatent?: number
  diagnosisFirst?: string
}): string {
  if (row.isLatent !== 1) return "正常"
  const diagnosis = row.diagnosisFirst?.trim()
  if (diagnosis && NORMAL_TERMINAL_DIAGNOSIS.has(diagnosis)) return "正常"
  if (isSuspectedTbDiagnosis(diagnosis)) return "待诊断"
  if (diagnosis === "确诊患者") return "已确诊患者"
  if (diagnosis === "潜伏感染者") return "已确诊潜伏感染者"
  return "待诊断"
}

/** 筛查列表「待确诊/判定结果」列标签颜色 */
export function getScreeningLatentStatusTagType(row: {
  isLatent?: number
  diagnosisFirst?: string
}): "success" | "warning" | "danger" {
  if (row.isLatent !== 1) return "success"
  const diagnosis = row.diagnosisFirst?.trim()
  if (diagnosis && NORMAL_TERMINAL_DIAGNOSIS.has(diagnosis)) return "success"
  if (diagnosis === "潜伏感染者") return "warning"
  if (diagnosis === "确诊患者") return "danger"
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
  return row.diagnosisFirst === "确诊患者"
    || row.screeningDiagnosisFirst === "确诊患者"
    || row.referralResult === "confirmed"
    || row.diagnosisResult === "确诊患者"
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
    if (draft === "确诊患者") return "确诊患者"
    const matched = SUSPECTED_CONFIRM_DIAGNOSIS_OPTIONS.find(o => o.value === draft)
    if (matched) return matched.label
    return draft
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

/** 历史错误选项「3HP」→ 正确为「3HR」 */
const LATENT_TREATMENT_PLAN_LEGACY: Record<string, string> = {
  "3HP": "3HR"
}

/** 潜伏感染者治疗方案选项（与患者管理治疗方案不同） */
export const LATENT_TREATMENT_PLAN_OPTIONS = [
  "6H/9H",
  "3HR",
  "3HR/4R",
  "母牛分枝杆菌",
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

/** 患者通知单：将治疗方案字符串回填到表单（含个体化方案） */
export function applyPatientNoticeTreatmentPlan(
  form: { treatmentPlan: string, customPlanDetail: string },
  plan?: string
) {
  const tp = (plan || "").trim()
  if (!tp) {
    form.treatmentPlan = ""
    form.customPlanDetail = ""
    return
  }
  if (!TREATMENT_PLAN_OPTIONS.includes(tp)) {
    form.treatmentPlan = "个体化方案"
    form.customPlanDetail = tp
  } else {
    form.treatmentPlan = tp
    form.customPlanDetail = ""
  }
}

/** 患者通知单管理方式 */
export const PATIENT_MANAGEMENT_METHOD_OPTIONS = ["全程督导", "强化督导", "全程管理", "未管理"]

/** 病原学/病理学检查结果 */
export const PATHOGEN_RESULT_OPTIONS = ["未出结果", "阴性", "阳性", "病原学结果阳性", "未做", "未知"]

/** 感染检查方法 */
export const INFECTION_METHOD_OPTIONS = ["PPD", "EC", "IGRA"]

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

/** 是/否（1无/2有） */
export const YES_NO_OPTIONS = [
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
