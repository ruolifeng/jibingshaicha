export interface QuestionnaireShowWhen {
  field: string
  value: string
}

export interface QuestionnaireField {
  key: string
  label: string
  type: "input" | "select" | "date" | "number" | "textarea"
  required?: boolean
  options?: string[]
  showWhen?: QuestionnaireShowWhen
}

export interface QuestionnaireFieldGroup {
  group: string
  fields: QuestionnaireField[]
}

export interface QuestionnaireConfig {
  code: string
  title: string
  subtitle: string
  enabled: boolean
  populationType: string
  groups: QuestionnaireFieldGroup[]
}

export const QUESTIONNAIRE_CODE = "school"

export const FIELD_TYPE_LABELS: Record<string, string> = {
  input: "文本输入",
  select: "下拉选择",
  date: "日期选择",
  number: "数字输入",
  textarea: "多行文本"
}

export const FIELD_TYPE_OPTIONS = [
  { label: "文本输入", value: "input" },
  { label: "下拉选择", value: "select" },
  { label: "日期选择", value: "date" },
  { label: "数字输入", value: "number" },
  { label: "多行文本", value: "textarea" }
]

/** 与 screening_school 表字段对应，自定义题目时需使用这些 key 才能入库统计 */
export const SCREENING_FIELD_KEYS = [
  "year", "city", "district", "name", "gender", "birthDate", "age", "idType", "idNumber",
  "ethnicity", "phone", "householdAddress", "currentAddress", "schoolType", "schoolName",
  "className", "tbHistory", "closeContactHistory", "suspiciousSymptoms", "hasInfectionScreen",
  "screenDate", "screenMethod", "screenResult", "infectionResult", "hasChestXray",
  "chestXrayDate", "chestXrayResult", "remark"
]
