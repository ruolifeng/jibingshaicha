/** 将部门 ID 数组序列化为 API 查询参数 */
export function joinDepartmentIds(ids?: number[] | null): string | undefined {
  if (!ids?.length) return undefined
  return ids.join(",")
}

/** 合并 API 查询参数中的 departmentIds */
export function withDepartmentIds<T extends object>(
  params: T,
  departmentIds?: number[] | null
): T & { departmentIds?: string } {
  const joined = joinDepartmentIds(departmentIds)
  if (!joined) return params
  return { ...params, departmentIds: joined }
}
