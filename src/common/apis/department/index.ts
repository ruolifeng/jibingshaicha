import { request } from "@/http/axios"

/** 部门层级：1 市级 → 2 区县 → 3 社区（数据范围按上级可见全部下级，同级区县互不可见） */
export interface Department {
  id?: number
  name: string
  description?: string
  parentId?: number | null
  /** 1 市级 2 区县 3 社区 */
  level?: number
  createTime?: string
}

/** 统计分析部门筛选树节点 */
export interface DepartmentFilterOption {
  id: number
  name: string
  level?: number
  parentId?: number | null
  children?: DepartmentFilterOption[]
}

export interface DepartmentPayload {
  name: string
  description?: string
  parentId?: number | null
  level: number
}

/** 部门列表 */
export function getDepartmentListApi() {
  return request<ApiResponseData<Department[]>>({
    url: "department/list",
    method: "get"
  })
}

/** 统计分析部门筛选选项（按当前用户层级隔离） */
export function getDepartmentFilterOptionsApi() {
  return request<ApiResponseData<DepartmentFilterOption[]>>({
    url: "department/filter-options",
    method: "get"
  })
}

/** 批量导入部门 Excel */
export function importDepartmentApi(file: File) {
  const formData = new FormData()
  formData.append("file", file)
  return request<ApiResponseData<{ successCount: number, errors: string[] }>>({
    url: "department/import",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000
  })
}

/** 创建部门 */
export function createDepartmentApi(data: DepartmentPayload) {
  return request<ApiResponseData<null>>({
    url: "department/create",
    method: "post",
    data
  })
}

/** 更新部门 */
export function updateDepartmentApi(data: DepartmentPayload & { id: number }) {
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
