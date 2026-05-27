import { request } from "@/http/axios"

/** 获取可分配权限的同部门用户列表 */
export function getPermissionAssignUsersApi() {
  return request<ApiResponseData<any[]>>({
    url: "permission/users",
    method: "get"
  })
}

/** 获取权限树 */
export function getPermissionTreeApi() {
  return request<ApiResponseData<any[]>>({
    url: "permission/tree",
    method: "get"
  })
}

/** 获取角色已分配权限ID */
export function getRolePermissionIdsApi(role: number) {
  return request<ApiResponseData<number[]>>({
    url: `permission/role/${role}`,
    method: "get"
  })
}

/** 分配角色权限 */
export function assignRolePermissionsApi(role: number, permissionIds: number[]) {
  return request<ApiResponseData<null>>({
    url: "permission/assign",
    method: "post",
    data: { role, permissionIds }
  })
}

/** 获取用户额外分配的权限 ID（与角色权限合并生效） */
export function getUserPermissionIdsApi(userId: number) {
  return request<ApiResponseData<number[]>>({
    url: `permission/user/${userId}`,
    method: "get"
  })
}

/** 全量替换某用户的额外权限 */
export function assignUserPermissionsApi(userId: number, permissionIds: number[]) {
  return request<ApiResponseData<null>>({
    url: "permission/assign-user",
    method: "post",
    data: { userId, permissionIds }
  })
}
