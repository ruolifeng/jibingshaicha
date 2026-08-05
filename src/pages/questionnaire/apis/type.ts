export interface QuestionnaireItem {
  id: string | number
  departmentId: string | number | null
  title: string
  description: string | null
  category: string
  status: number
  /** 模板类型：public=公用模板，private=专属模板，null=普通问卷 */
  templateType: "public" | "private" | null
  startTime: string | null
  endTime: string | null
  totalVisits: number
  totalResponses: number
  createdBy: string | number
  createdAt: string
  updatedAt: string
}

export interface QuestionItem {
  id?: string | number
  questionnaireId?: string | number
  sortOrder: number
  type: string
  title: string
  description?: string
  required: number
  options?: string
  validationRules?: string
  logicRules?: string
  pageNumber: number
  _tempKey?: number
}

export interface ResponseItem {
  id: string | number
  questionnaireId: string | number
  accessToken: string | null
  respondentIp: string | null
  status: number
  startTime: string | null
  submitTime: string | null
  durationSeconds: number | null
  createdAt: string
}

export interface AnswerItem {
  id?: string | number
  responseId?: string | number
  questionId: string | number
  answerValue: string
}

export interface QuestionnaireCategoryItem {
  id: string | number
  code: string
  name: string
  sort: number
  createTime?: string
  updateTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export type QuestionnairePageResponse = ApiResponseData<PageResult<QuestionnaireItem>>
export type QuestionnaireDetailResponse = ApiResponseData<QuestionnaireItem>
export type QuestionListResponse = ApiResponseData<QuestionItem[]>
export type ResponsePageResponse = ApiResponseData<PageResult<ResponseItem>>

/** 选项题统计（单选/多选各选项人数） */
export interface ChoiceOptionRow {
  label: string
  value: string
  count: number
  percent: number
}

export interface ChoiceQuestionStat {
  questionId: string | number
  sortOrder: number
  type: string
  title: string
  baseCount: number
  blankCount: number
  otherCount: number
  optionRows: ChoiceOptionRow[]
}

export interface QuestionnaireStatistics {
  totalVisits: number
  totalResponses: number
  submitted: number
  badSample: number
  completionRate: number
  choiceQuestionStats?: ChoiceQuestionStat[]
}
