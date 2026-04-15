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

/** 治疗方案选项（7种，V4与V2一致） */
export const TREATMENT_PLAN_OPTIONS = [
  "FDC-2HRZE/4HR",
  "2HRZE/4HR",
  "FDC-2HRZES/6HRE",
  "2HRZES/6HRE",
  "FDC-2HRZE/10HRE",
  "2HRZE/10HRE",
  "个体化方案"
]

/** 追踪状态 */
export const TRACKING_STATUS_MAP: Record<number, string> = {
  0: "待追踪",
  1: "到位",
  2: "未到位",
  3: "其他",
  4: "强制结束"
}

/**
 * 诊断结果选项（V4新增，追踪到位后录入）
 * 排除/疑似肺结核 → 归档；确诊患者 → 患者管理；潜伏感染者 → 通知单；其他 → 备注归档
 */
export const DIAGNOSIS_RESULT_OPTIONS = [
  { label: "排除", value: "排除" },
  { label: "疑似肺结核", value: "疑似肺结核" },
  { label: "潜伏感染者", value: "潜伏感染者" },
  { label: "确诊患者", value: "确诊患者" },
  { label: "其他", value: "其他" }
]

/**
 * 转诊结果选项（前端驱动转诊弹窗，基于 diagnosisFirst 自动映射）
 * V4 新增 suspected（疑似肺结核）
 */
export const REFERRAL_RESULT_OPTIONS = [
  { label: "排除", value: "excluded" },
  { label: "其他", value: "other" },
  { label: "疑似肺结核", value: "suspected" },
  { label: "确诊患者", value: "confirmed" },
  { label: "潜伏感染者", value: "latent" }
]

/** 胸片检查结果选项 */
export const CHEST_XRAY_RESULT_OPTIONS = ["正常", "异常", "未查"]

/** 预防性治疗结果（V4新增，督导表字段） */
export const PREVENTIVE_RESULT_OPTIONS = [
  "规范完成",
  "失访",
  "自行中断治疗",
  "确诊肺结核"
]

/** 预防性治疗期间随访管理人员（V4新增，督导表字段） */
export const PREVENTIVE_MANAGER_OPTIONS = [
  "医务人员（基层/疾控/定点医院）",
  "家庭成员",
  "志愿者（社区人员、学生等）",
  "智能辅助工具（电子药盒、手机等）",
  "自我管理"
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
  1: "已发送",
  2: "已确认"
}

/** 潜伏感染者通知单治疗方案 */
export const LATENT_TREATMENT_OPTIONS = ["免费药品", "生物制剂", "未治疗"]

/** 患者类型（患者通知单） */
export const PATIENT_TYPE_OPTIONS = ["初治", "复治"]

/** 患者通知单管理方式 */
export const PATIENT_MANAGEMENT_METHOD_OPTIONS = ["全程督导", "强化督导", "全程管理", "未管理"]

/** 病原学/病理学检查结果 */
export const PATHOGEN_RESULT_OPTIONS = ["未出结果", "阴性", "阳性", "未做", "未知"]

/** 感染检查方法 */
export const INFECTION_METHOD_OPTIONS = ["PPD", "EC", "IGRA"]

/** 密接人群阳性轮次 */
export const ACTIVE_ROUND_MAP: Record<number, string> = {
  1: "首次",
  2: "半年后",
  3: "一年后"
}

// ==================== 患者随访相关 ====================

/** 随访方式 */
export const VISIT_METHOD_OPTIONS = ["门诊", "家庭"]

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
