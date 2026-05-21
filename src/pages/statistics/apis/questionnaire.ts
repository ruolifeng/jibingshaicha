import type { QuestionnaireConfig } from "@/common/constants/questionnaire"
import { request } from "@/http/axios"

/** 公开获取问卷配置 */
export function getPublicQuestionnaireConfigApi(code: string) {
  return request<ApiResponseData<QuestionnaireConfig>>({
    url: `questionnaire/public/${code}`,
    method: "get"
  })
}

/** 公开提交问卷 */
export function submitPublicQuestionnaireApi(code: string, data: Record<string, unknown>) {
  return request<ApiResponseData<null>>({
    url: `questionnaire/public/${code}/submit`,
    method: "post",
    data
  })
}

/** 管理端：获取问卷配置 */
export function getQuestionnaireConfigApi(code: string) {
  return request<ApiResponseData<QuestionnaireConfig>>({
    url: `questionnaire/${code}/config`,
    method: "get"
  })
}

/** 管理端：更新问卷配置 */
export function updateQuestionnaireConfigApi(code: string, data: QuestionnaireConfig) {
  return request<ApiResponseData<null>>({
    url: `questionnaire/${code}/config`,
    method: "put",
    data
  })
}

/** 管理端：切换问卷开关 */
export function updateQuestionnaireEnabledApi(code: string, enabled: boolean) {
  return request<ApiResponseData<null>>({
    url: `questionnaire/${code}/enabled`,
    method: "put",
    data: { enabled }
  })
}

/** 管理端：分页查询提交记录 */
export function getQuestionnaireSubmissionsApi(code: string, params: {
  page?: number
  size?: number
  name?: string
  idNumber?: string
}) {
  return request<ApiResponseData<{
    records: Record<string, unknown>[]
    total: number
  }>>({
    url: `questionnaire/${code}/submissions`,
    method: "get",
    params
  })
}

/** 管理端：导出提交记录 */
export function exportQuestionnaireSubmissionsApi(code: string, params?: {
  name?: string
  idNumber?: string
}) {
  return request<Blob>({
    url: `questionnaire/${code}/submissions/export`,
    method: "get",
    params,
    responseType: "blob"
  })
}
