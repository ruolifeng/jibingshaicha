<script lang="ts" setup>
import { ROLE_OPTIONS } from "@@/constants/disease"
import { getPermissionTreeApi, getRolePermissionIdsApi, assignRolePermissionsApi } from "./apis"

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

// 当前选中的角色
const selectedRole = ref<number>(2)

// 角色拥有的权限 ID（已勾选）
const checkedIds = ref<number[]>([])

const treeRef = ref<any>(null)

async function loadRolePermissions() {
  try {
    const { data } = await getRolePermissionIdsApi(selectedRole.value)
    checkedIds.value = data || []
    // 只设置叶子节点为 checked（el-tree 会自动推导父节点）
    await nextTick()
    if (treeRef.value) {
      treeRef.value.setCheckedKeys(filterLeafIds(permissionTree.value, data || []))
    }
  } catch { /* handled */ }
}

/** 从全部权限树中筛出叶子节点 ID（el-tree 要求只设置叶子节点的 checked） */
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

async function handleSave() {
  if (!treeRef.value) return
  const checked = treeRef.value.getCheckedKeys() as number[]
  const halfChecked = treeRef.value.getHalfCheckedKeys() as number[]
  const allIds = [...checked, ...halfChecked]
  try {
    await assignRolePermissionsApi(selectedRole.value, allIds)
    ElMessage.success("权限分配成功")
    checkedIds.value = allIds
  } catch { /* handled */ }
}

watch(selectedRole, () => {
  if (permissionTree.value.length > 0) loadRolePermissions()
})

onMounted(async () => {
  await loadPermissionTree()
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

      <el-alert type="info" :closable="false" class="mb-4">
        选择角色后，在下方权限树中勾选该角色可访问的菜单和操作，点击「保存」生效。超级管理员默认拥有所有权限。
      </el-alert>

      <div class="flex items-center gap-4 mb-4">
        <span class="font-bold">选择角色：</span>
        <el-radio-group v-model="selectedRole">
          <el-radio-button v-for="item in ROLE_OPTIONS" :key="item.value" :value="item.value" :disabled="item.value === 1">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="handleSave" :disabled="selectedRole === 1">
          保存权限
        </el-button>
      </div>

      <el-divider />

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
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 { margin-bottom: 16px; }
.gap-4 { gap: 16px; }
.ml-2 { margin-left: 8px; }

.perm-tree-wrapper {
  max-height: 600px;
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
