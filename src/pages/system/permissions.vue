<script lang="ts" setup>
import { ROLE_MAP, ROLE_OPTIONS } from "@@/constants/disease"
import {
  assignRolePermissionsApi,
  assignUserPermissionsApi,
  getPermissionAssignUsersApi,
  getPermissionTreeApi,
  getRolePermissionIdsApi,
  getUserPermissionIdsApi
} from "./apis"

const permissionTree = ref<any[]>([])
const loading = ref(false)

async function loadPermissionTree() {
  loading.value = true
  try {
    const { data } = await getPermissionTreeApi()
    permissionTree.value = data || []
  } finally {
    loading.value = false
  }
}

const activeTab = ref<"role" | "user">("role")

// ---------- 角色权限 ----------
const selectedRole = ref<number>(2)
const treeRef = ref<any>(null)

async function loadRolePermissions() {
  try {
    const { data } = await getRolePermissionIdsApi(selectedRole.value)
    await nextTick()
    if (treeRef.value) {
      treeRef.value.setCheckedKeys(filterLeafIds(permissionTree.value, data || []))
    }
  } catch { /* handled */ }
}

async function handleSaveRole() {
  if (!treeRef.value) return
  const checked = treeRef.value.getCheckedKeys() as number[]
  const halfChecked = treeRef.value.getHalfCheckedKeys() as number[]
  const allIds = [...checked, ...halfChecked]
  try {
    await assignRolePermissionsApi(selectedRole.value, allIds)
    ElMessage.success("角色权限已保存")
  } catch { /* handled */ }
}

watch(selectedRole, () => {
  if (activeTab.value === "role" && permissionTree.value.length > 0) {
    loadRolePermissions()
  }
})

// ---------- 用户额外权限（与角色权限合并） ----------
const userList = ref<any[]>([])
const selectedUserId = ref<number | undefined>()
const userTreeRef = ref<any>(null)

async function loadUserOptions() {
  try {
    const { data } = await getPermissionAssignUsersApi()
    userList.value = data ?? []
  } catch {
    userList.value = []
  }
}

async function loadUserExtraPermissions() {
  if (!selectedUserId.value) return
  try {
    const { data } = await getUserPermissionIdsApi(selectedUserId.value)
    await nextTick()
    if (userTreeRef.value) {
      userTreeRef.value.setCheckedKeys(filterLeafIds(permissionTree.value, data || []))
    }
  } catch { /* handled */ }
}

async function handleSaveUserPerms() {
  if (!selectedUserId.value || !userTreeRef.value) {
    ElMessage.warning("请先选择用户")
    return
  }
  const checked = userTreeRef.value.getCheckedKeys() as number[]
  const halfChecked = userTreeRef.value.getHalfCheckedKeys() as number[]
  const allIds = [...checked, ...halfChecked]
  try {
    await assignUserPermissionsApi(selectedUserId.value, allIds)
    ElMessage.success("用户额外权限已保存（登录后生效）")
  } catch { /* handled */ }
}

watch(selectedUserId, () => {
  if (activeTab.value === "user" && selectedUserId.value && permissionTree.value.length > 0) {
    loadUserExtraPermissions()
  }
})

watch(activeTab, (t: "role" | "user") => {
  if (t === "user" && permissionTree.value.length > 0 && selectedUserId.value) {
    nextTick(() => loadUserExtraPermissions())
  }
})

function userLabel(row: any) {
  const rn = ROLE_MAP[row.role] || ""
  return `${row.username}${rn ? ` · ${rn}` : ""}`
}

/** 只勾选叶子节点 ID（el-tree 父子联动展示；半选父节点由 halfChecked 保存） */
function filterLeafIds(tree: any[], allIds: number[]): number[] {
  const leafIds: number[] = []
  function walk(nodes: any[]) {
    for (const node of nodes) {
      if (!node.children || node.children.length === 0) {
        if (allIds.includes(node.id)) leafIds.push(node.id)
      } else {
        walk(node.children)
      }
    }
  }
  walk(tree)
  return leafIds
}

onMounted(async () => {
  await loadPermissionTree()
  await loadUserOptions()
  loadRolePermissions()
})

function getTypeTag(type: number) {
  return type === 1 ? "菜单" : "操作"
}
function getTypeColor(type: number): "primary" | "warning" {
  return type === 1 ? "primary" : "warning"
}
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">权限管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="按角色分配" name="role">
          <el-alert type="info" :closable="false" class="mb-4">
            选择角色后在权限树中勾选菜单与操作，保存后为该角色默认权限。超级管理员不受此限制。
          </el-alert>
          <div class="flex flex-wrap items-center gap-4 mb-4">
            <span class="font-bold">角色：</span>
            <el-radio-group v-model="selectedRole">
              <el-radio-button v-for="item in ROLE_OPTIONS" :key="item.value" :value="item.value" :disabled="item.value === 1">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
            <el-button type="primary" @click="handleSaveRole" :disabled="selectedRole === 1">
              保存角色权限
            </el-button>
          </div>
          <div class="perm-tree-wrapper">
            <el-tree
              ref="treeRef"
              v-loading="loading"
              :data="permissionTree"
              show-checkbox
              node-key="id"
              default-expand-all
              :props="{ label: 'name', children: 'children' }"
            >
              <template #default="{ data }">
                <span class="perm-tree-node">
                  <span>{{ data.name }}</span>
                  <el-tag :type="getTypeColor(data.type)" size="small" class="ml-2">{{ getTypeTag(data.type) }}</el-tag>
                  <span class="perm-code">{{ data.code }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-tab-pane>

        <el-tab-pane label="按用户追加" name="user">
          <el-alert type="warning" :closable="false" class="mb-4">
            此处配置的是<strong>在用户所属角色权限基础上的额外权限</strong>（并集）。保存后请让用户重新登录或刷新页面以加载最新权限。超级管理员无需配置。
          </el-alert>
          <div class="flex flex-wrap items-center gap-4 mb-4">
            <span class="font-bold">用户：</span>
            <el-select
              v-model="selectedUserId"
              filterable
              clearable
              placeholder="选择用户"
              style="width: 280px"
            >
              <el-option
                v-for="u in userList"
                :key="u.id"
                :label="userLabel(u)"
                :value="u.id"
                :disabled="u.role === 1"
              />
            </el-select>
            <el-button type="primary" :disabled="!selectedUserId" @click="handleSaveUserPerms">
              保存用户额外权限
            </el-button>
          </div>
          <div class="perm-tree-wrapper">
            <el-tree
              ref="userTreeRef"
              v-loading="loading"
              :data="permissionTree"
              show-checkbox
              node-key="id"
              default-expand-all
              :props="{ label: 'name', children: 'children' }"
            >
              <template #default="{ data }">
                <span class="perm-tree-node">
                  <span>{{ data.name }}</span>
                  <el-tag :type="getTypeColor(data.type)" size="small" class="ml-2">{{ getTypeTag(data.type) }}</el-tag>
                  <span class="perm-code">{{ data.code }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.ml-2 {
  margin-left: 8px;
}
.gap-4 {
  gap: 16px;
}

.perm-tree-wrapper {
  max-height: 560px;
  overflow-y: auto;
}

.perm-tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.perm-code {
  color: #909399;
  font-size: 12px;
  font-family: monospace;
}
</style>
