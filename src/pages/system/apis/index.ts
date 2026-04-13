import { request } from "@/http/axios"

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
