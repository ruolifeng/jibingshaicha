import type { Department } from "@@/apis/department"

export type DepartmentTree = Department & { children?: DepartmentTree[] }

/** 将扁平部门列表转为树形结构 */
export function buildDepartmentTree(list: Department[]): DepartmentTree[] {
  const map = new Map<number, DepartmentTree>()
  const roots: DepartmentTree[] = []

  for (const item of list) {
    if (item.id == null) continue
    map.set(item.id, { ...item, children: [] })
  }

  for (const node of map.values()) {
    const parentId = node.parentId
    if (parentId != null && map.has(parentId)) {
      map.get(parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  }

  const sortNodes = (nodes: DepartmentTree[]) => {
    nodes.sort((a, b) => (a.level ?? 0) - (b.level ?? 0) || (a.id ?? 0) - (b.id ?? 0))
    nodes.forEach((node) => {
      if (node.children?.length) {
        sortNodes(node.children)
      } else {
        delete node.children
      }
    })
  }
  sortNodes(roots)
  return roots
}

/** 部门下拉选项（带层级缩进） */
export function flattenDepartmentOptions(
  list: Department[],
  indent = "　"
): { label: string, value: number }[] {
  const walk = (nodes: DepartmentTree[], depth = 0): { label: string, value: number }[] => {
    const result: { label: string, value: number }[] = []
    for (const node of nodes) {
      if (node.id != null) {
        result.push({
          label: `${indent.repeat(depth)}${node.name}`,
          value: node.id
        })
      }
      if (node.children?.length) {
        result.push(...walk(node.children, depth + 1))
      }
    }
    return result
  }
  return walk(buildDepartmentTree(list))
}
