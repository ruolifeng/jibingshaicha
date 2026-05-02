import { request } from "@/http/axios"

export interface Department {
  id?: number
  name: string
  description?: string
  createTime?: string
}

/** 部门列表 */
export function getDepartmentListApi() {
  return request<ApiResponseData<Department[]>>({
    url: "department/list",
    method: "get"
  })
}

/** 创建部门 */
export function createDepartmentApi(data: { name: string; description?: string }) {
  return request<ApiResponseData<null>>({
    url: "department/create",
    method: "post",
    data
  })
}

/** 更新部门 */
export function updateDepartmentApi(data: { id: number; name: string; description?: string }) {
  return request<ApiResponseData<null>>({
    url: "department/update",
    method: "put",
    data
  })
}

/** 删除部门 */
export function deleteDepartmentApi(id: number) {
  return request<ApiResponseData<null>>({
    url: `department/delete/${id}`,
    method: "delete"
  })
}
