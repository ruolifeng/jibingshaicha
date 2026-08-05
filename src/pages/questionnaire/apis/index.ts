import type { AxiosRequestConfig } from "axios"
import type * as T from "./type"
import { request } from "@/http/axios"

// ====== 问卷CRUD ======

export function getQuestionnairePageApi(params: { pageNum: number, pageSize: number, keyword?: string, status?: number }) {
  return request<T.QuestionnairePageResponse>({ url: "questionnaire/page", method: "get", params })
}

export function getQuestionnaireDetailApi(id: string | number) {
  return request<T.QuestionnaireDetailResponse>({ url: `questionnaire/${id}`, method: "get" })
}

export function createQuestionnaireApi(data: Partial<T.QuestionnaireItem>) {
  return request<T.QuestionnaireDetailResponse>({ url: "questionnaire", method: "post", data })
}

export function updateQuestionnaireApi(id: string | number, data: Partial<T.QuestionnaireItem>) {
  return request<T.QuestionnaireDetailResponse>({ url: `questionnaire/${id}`, method: "put", data })
}

export function deleteQuestionnaireApi(id: string | number) {
  return request<ApiResponseData<null>>({ url: `questionnaire/${id}`, method: "delete" })
}

export function updateQuestionnaireStatusApi(id: string | number, status: number) {
  return request<ApiResponseData<null>>({ url: `questionnaire/${id}/status`, method: "put", params: { status } })
}

// ====== 题目 ======

export function getQuestionsApi(id: string | number, config?: AxiosRequestConfig) {
  return request<T.QuestionListResponse>({ ...config, url: `questionnaire/${id}/questions`, method: "get" })
}

export function saveQuestionsApi(id: string | number, data: T.QuestionItem[]) {
  return request<ApiResponseData<null>>({ url: `questionnaire/${id}/questions`, method: "post", data })
}

// ====== 二维码 ======

export function getQrUrlApi(id: string | number) {
  return request<ApiResponseData<string>>({ url: `questionnaire/${id}/qr-url`, method: "get" })
}

// ====== 模板 ======

/** 获取模板列表，templateType: public=公用模板, private=专属模板, 不传=所有 */
export function getTemplateListApi(params?: { pageNum?: number, pageSize?: number, templateType?: "public" | "private" }) {
  return request<T.QuestionnairePageResponse>({ url: "questionnaire/template/list", method: "get", params })
}

/** 存为模板，templateType: public=公用模板, private=专属模板（均按部门树隔离） */
export function saveAsTemplateApi(id: string | number, templateType: "public" | "private", title?: string) {
  return request<T.QuestionnaireDetailResponse>({
    url: `questionnaire/${id}/save-as-template`,
    method: "post",
    data: { templateType, ...(title ? { title } : {}) }
  })
}

export function deleteTemplateApi(id: string | number) {
  return request<ApiResponseData<null>>({ url: `questionnaire/template/${id}`, method: "delete" })
}

export function createFromTemplateApi(templateId: string | number, title?: string) {
  return request<T.QuestionnaireDetailResponse>({ url: "questionnaire/create-from-template", method: "post", data: { templateId, title } })
}

// ====== 分类 ======

export function getCategoryListApi() {
  return request<ApiResponseData<T.QuestionnaireCategoryItem[]>>({ url: "questionnaire/category/list", method: "get" })
}

export function createCategoryApi(data: { code: string, name: string, sort?: number }) {
  return request<ApiResponseData<T.QuestionnaireCategoryItem>>({ url: "questionnaire/category", method: "post", data })
}

export function updateCategoryApi(id: string | number, data: { name: string, sort?: number }) {
  return request<ApiResponseData<T.QuestionnaireCategoryItem>>({ url: `questionnaire/category/${id}`, method: "put", data })
}

export function deleteCategoryApi(id: string | number) {
  return request<ApiResponseData<null>>({ url: `questionnaire/category/${id}`, method: "delete" })
}

// ====== 回收数据 ======

export function getResponsePageApi(
  id: string | number,
  params: { pageNum: number, pageSize: number, status?: number, submitted?: boolean },
  config?: AxiosRequestConfig
) {
  return request<T.ResponsePageResponse>({ ...config, url: `questionnaire/${id}/responses`, method: "get", params })
}

export function getResponseDetailApi(responseId: string | number, config?: AxiosRequestConfig) {
  return request<ApiResponseData<{ response: T.ResponseItem, answers: T.AnswerItem[], questions: T.QuestionItem[] }>>({
    ...config,
    url: `questionnaire/response/${responseId}`,
    method: "get"
  })
}

export function getStatisticsApi(id: string | number) {
  return request<ApiResponseData<T.QuestionnaireStatistics>>({
    url: `questionnaire/${id}/statistics`,
    method: "get"
  })
}

export function updateResponseStatusApi(responseId: string | number, status: number) {
  return request<ApiResponseData<null>>({ url: `questionnaire/response/${responseId}/status`, method: "put", params: { status } })
}

/**
 * 获取问卷全量回答数据（用于前端 Excel 导出）
 * 后端接口：GET /questionnaire/{id}/responses/export-data
 * 返回所有回答 + 每条回答的答案列表，不分页
 */
export function getExportDataApi(id: string | number, params?: { status?: number, submitted?: boolean }) {
  return request<ApiResponseData<Array<{ response: T.ResponseItem, answers: T.AnswerItem[] }>>>({
    url: `questionnaire/${id}/responses/export-data`,
    method: "get",
    params,
    timeout: 120_000
  })
}
