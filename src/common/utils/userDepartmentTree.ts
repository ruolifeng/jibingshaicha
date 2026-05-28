import type { Department } from "@@/apis/department"
import type { DepartmentTree } from "@@/utils/departmentTree"
import { buildDepartmentTree } from "@@/utils/departmentTree"

export type UserTreeRowType = "dept" | "user"

export interface UserTreeRow {
  rowKey: string
  nodeType: UserTreeRowType
  children?: UserTreeRow[]
  /** 部门节点 */
  id?: number
  name?: string
  level?: number
  /** 含下级部门在内的用户总数 */
  userCount?: number
  /** 用户节点 */
  username?: string
  realName?: string
  role?: number
  orgName?: string
  departmentId?: number | null
  createTime?: string
}

export interface UserRecord {
  id: number
  username: string
  realName?: string
  role: number
  orgName?: string
  departmentId?: number | null
  createTime?: string
}

export interface BuildUserTreeOptions {
  username?: string
  role?: number
}

const UNASSIGNED_KEY = "dept-unassigned"

function sortUsers(list: UserRecord[]) {
  list.sort(
    (a, b) => (a.role ?? 99) - (b.role ?? 99) || String(a.username).localeCompare(String(b.username), "zh-CN")
  )
}

function userToNode(user: UserRecord): UserTreeRow {
  return {
    rowKey: `user-${user.id}`,
    nodeType: "user",
    id: user.id,
    username: user.username,
    realName: user.realName,
    role: user.role,
    orgName: user.orgName,
    departmentId: user.departmentId,
    createTime: user.createTime
  }
}

function filterUsers(users: UserRecord[], options?: BuildUserTreeOptions): UserRecord[] {
  let result = users
  const keyword = options?.username?.trim()
  if (keyword) {
    const kw = keyword.toLowerCase()
    result = result.filter(
      u => u.username?.toLowerCase().includes(kw) || (u.realName?.toLowerCase()?.includes(kw) ?? false)
    )
  }
  if (options?.role != null) {
    result = result.filter(u => u.role === options.role)
  }
  return result
}

function enrichDeptNode(dept: DepartmentTree, usersByDept: Map<number, UserRecord[]>): UserTreeRow {
  const subDepts = dept.children?.map(child => enrichDeptNode(child, usersByDept)) ?? []
  const directUsers = [...(usersByDept.get(dept.id!) ?? [])]
  sortUsers(directUsers)
  const userNodes = directUsers.map(userToNode)

  let userCount = userNodes.length
  for (const sub of subDepts) {
    userCount += sub.userCount ?? 0
  }

  const children = [...subDepts, ...userNodes]
  const node: UserTreeRow = {
    rowKey: `dept-${dept.id}`,
    nodeType: "dept",
    id: dept.id,
    name: dept.name,
    level: dept.level,
    userCount
  }
  if (children.length) {
    node.children = children
  }
  return node
}

function pruneEmptyDeptNodes(nodes: UserTreeRow[]): UserTreeRow[] {
  return nodes
    .map((node) => {
      if (node.nodeType === "user") return node
      const children = node.children ? pruneEmptyDeptNodes(node.children) : []
      const userCount = children.reduce((sum, child) => {
        if (child.nodeType === "user") return sum + 1
        return sum + (child.userCount ?? 0)
      }, 0)
      if (userCount === 0) return null
      const next: UserTreeRow = { ...node, userCount, children: children.length ? children : undefined }
      if (!next.children?.length) delete next.children
      return next
    })
    .filter((node): node is UserTreeRow => node != null)
}

/** 按部门树挂载用户，支持搜索时裁剪无用户的部门分支 */
export function buildUserDepartmentTree(
  departments: Department[],
  users: UserRecord[],
  options?: BuildUserTreeOptions
): UserTreeRow[] {
  const filtered = filterUsers(users, options)

  const usersByDept = new Map<number, UserRecord[]>()
  const unassigned: UserRecord[] = []
  const deptIds = new Set(departments.map(d => d.id).filter((id): id is number => id != null))

  for (const user of filtered) {
    const deptId = user.departmentId
    if (deptId == null || !deptIds.has(deptId)) {
      unassigned.push(user)
    } else {
      if (!usersByDept.has(deptId)) usersByDept.set(deptId, [])
      usersByDept.get(deptId)!.push(user)
    }
  }

  let roots = buildDepartmentTree(departments).map(dept => enrichDeptNode(dept, usersByDept))
  // 隐藏无用户的部门分支，避免五级用户场景下满屏空节点
  roots = pruneEmptyDeptNodes(roots)

  if (unassigned.length) {
    sortUsers(unassigned)
    roots.push({
      rowKey: UNASSIGNED_KEY,
      nodeType: "dept",
      name: "未分配部门",
      userCount: unassigned.length,
      children: unassigned.map(userToNode)
    })
  }

  return roots
}

/** 遍历树节点（含用户叶子） */
export function walkUserTreeRows(nodes: UserTreeRow[], visit: (row: UserTreeRow) => void) {
  for (const node of nodes) {
    visit(node)
    if (node.children?.length) {
      walkUserTreeRows(node.children, visit)
    }
  }
}

/** el-tree-select 节点（部门不可选，仅用户可选） */
export interface DepartmentUserSelectNode {
  value: number | string
  label: string
  disabled?: boolean
  children?: DepartmentUserSelectNode[]
}

function userSelectLabel(user: UserRecord): string {
  const name = user.realName?.trim() || user.username
  const unit = user.orgName?.trim()
  return unit ? `${name}（${unit}）` : name
}

function toDepartmentUserSelectNodes(rows: UserTreeRow[]): DepartmentUserSelectNode[] {
  return rows.map((row) => {
    if (row.nodeType === "dept") {
      const children = row.children ? toDepartmentUserSelectNodes(row.children) : []
      return {
        value: row.rowKey,
        label: row.name ?? "未命名部门",
        disabled: true,
        children: children.length ? children : undefined
      }
    }
    return {
      value: row.id!,
      label: userSelectLabel(row as UserRecord),
      disabled: false
    }
  })
}

/** 构建部门-用户树下拉选项（转出/接收方选择） */
export function buildDepartmentUserSelectTree(
  departments: Department[],
  users: UserRecord[]
): DepartmentUserSelectNode[] {
  return toDepartmentUserSelectNodes(buildUserDepartmentTree(departments, users))
}
