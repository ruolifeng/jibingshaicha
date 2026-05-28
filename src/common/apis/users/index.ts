import type * as Users from "./type"
import { request } from "@/http/axios"

/** 获取当前登录用户详情 */
export function getCurrentUserApi() {
  return request<Users.CurrentUserResponseData>({
    url: "user/me",
    method: "get"
  })
}

/** 用户列表 */
export function getUserListApi(params: { page: number, size: number, username?: string, role?: number }) {
  return request<ApiResponseData<any>>({
    url: "user/list",
    method: "get",
    params
  })
}

/** 创建用户 */
export function createUserApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "user/create",
    method: "post",
    data
  })
}

/** 更新用户 */
export function updateUserApi(data: Record<string, any>) {
  return request<ApiResponseData<null>>({
    url: "user/update",
    method: "put",
    data
  })
}

/** 删除用户 */
export function deleteUserApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `user/delete/${id}`,
    method: "delete"
  })
}

/** 获取五级机构用户列表（通知单接收单位） */
export function getLevel5UsersApi() {
  return request<ApiResponseData<Users.UserInfo[]>>({
    url: "user/level5-users",
    method: "get"
  })
}

/** 获取转出接收方用户（四级/五级，部门-用户树） */
export function getReferralReceiverUsersApi() {
  return request<ApiResponseData<Users.UserInfo[]>>({
    url: "user/referral-receiver-users",
    method: "get"
  })
}
