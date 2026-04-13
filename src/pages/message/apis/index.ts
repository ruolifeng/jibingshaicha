import { request } from "@/http/axios"

/** 消息列表 */
export function getMessageListApi(params: { page: number, size: number, isRead?: number }) {
  return request<ApiResponseData<any>>({
    url: "message/list",
    method: "get",
    params
  })
}

/** 标记已读 */
export function markMessageReadApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `message/read/${id}`,
    method: "post"
  })
}

/** 未读消息数 */
export function getUnreadCountApi() {
  return request<ApiResponseData<number>>({
    url: "message/unread-count",
    method: "get"
  })
}
