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

/** 治疗方案选项 */
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

/** 转诊结果选项 */
export const REFERRAL_RESULT_OPTIONS = [
  { label: "排除", value: "excluded" },
  { label: "其他", value: "other" },
  { label: "确诊", value: "confirmed" },
  { label: "潜伏感染者", value: "latent" }
]

/** 管理方式选项 */
export const MANAGEMENT_METHOD_OPTIONS = ["全程督导", "强化期督导", "全程管理", "自服药"]

/** 督导人员选项 */
export const SUPERVISOR_OPTIONS = ["医生", "家属", "志愿者", "患者本人"]

/** 治疗前痰菌检查结果选项 */
export const SPUTUM_RESULT_OPTIONS = ["阴性", "阳性", "无结果", "未检查"]

/** 通知单状态 */
export const NOTICE_STATUS_MAP: Record<number, string> = {
  1: "已发送",
  2: "已确认"
}

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
