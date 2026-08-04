/** 列表「格式问题」筛选（与后端 formatIssue / 导入校验一致） */
export const FORMAT_ISSUE_OPTIONS = [
  { label: "未填写证件号", value: "missingId" },
  { label: "证件号格式异常", value: "idNumber" },
  { label: "手机号格式异常", value: "phone" },
  { label: "证件号或手机号异常", value: "any" }
] as const

export type FormatIssueValue = (typeof FORMAT_ISSUE_OPTIONS)[number]["value"]
