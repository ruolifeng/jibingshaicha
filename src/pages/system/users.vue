<script lang="ts" setup>
import type { Department } from "@@/apis/department"
import type { UserRecord, UserTreeRow } from "@@/utils/userDepartmentTree"
import { getDepartmentListApi } from "@@/apis/department"
import { createUserApi, deleteUserApi, getUserListApi, updateUserApi } from "@@/apis/users"
import { ROLE_MAP, ROLE_OPTIONS } from "@@/constants/disease"
import { flattenDepartmentOptions } from "@@/utils/departmentTree"
import { STRONG_PASSWORD_HINT, validateStrongPassword } from "@@/utils/password"
import { buildUserDepartmentTree, walkUserTreeRows } from "@@/utils/userDepartmentTree"

const DEPT_LEVEL_MAP: Record<number, string> = {
  1: "市级",
  2: "区县",
  3: "社区/街道/乡镇"
}

const DEPT_LEVEL_TAG: Record<number, "primary" | "success" | "warning"> = {
  1: "primary",
  2: "success",
  3: "warning"
}

/** 树形展示一次拉取的用户上限 */
const USER_FETCH_SIZE = 5000

const loading = ref(false)
const allUsers = ref<UserRecord[]>([])
const departmentList = ref<Department[]>([])
const tableRef = ref()
const isExpandAll = ref(false)

const searchForm = reactive({ username: "", role: undefined as number | undefined })

const treeData = computed(() =>
  buildUserDepartmentTree(departmentList.value, allUsers.value, {
    username: searchForm.username,
    role: searchForm.role
  })
)

const totalUserCount = computed(() => {
  let count = 0
  walkUserTreeRows(treeData.value, (row) => {
    if (row.nodeType === "user") count++
  })
  return count
})

const departmentSelectOptions = computed(() => flattenDepartmentOptions(departmentList.value))

async function fetchData() {
  loading.value = true
  try {
    const [{ data: userData }, { data: deptData }] = await Promise.all([
      getUserListApi({ page: 1, size: USER_FETCH_SIZE }),
      getDepartmentListApi()
    ])
    allUsers.value = userData.records ?? []
    departmentList.value = deptData
    const total = userData.total ?? allUsers.value.length
    if (total > USER_FETCH_SIZE) {
      ElMessage.warning(`用户共 ${total} 人，仅加载前 ${USER_FETCH_SIZE} 人，请用搜索缩小范围`)
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  handleSearchExpand()
}

function handleReset() {
  searchForm.username = ""
  searchForm.role = undefined
  isExpandAll.value = false
  nextTick(() => setTreeExpanded(false))
}

function setTreeExpanded(expanded: boolean) {
  const walk = (rows: UserTreeRow[]) => {
    rows.forEach((row) => {
      tableRef.value?.toggleRowExpansion(row, expanded)
      if (row.children?.length) {
        walk(row.children)
      }
    })
  }
  walk(treeData.value)
}

function toggleExpandAll() {
  isExpandAll.value = !isExpandAll.value
  setTreeExpanded(isExpandAll.value)
}

function handleSearchExpand() {
  isExpandAll.value = true
  nextTick(() => setTreeExpanded(true))
}

function roleTagType(role?: number) {
  if (role === 1) return "danger"
  if (role != null && role <= 4) return "warning"
  return "primary"
}

// ==================== 新增/编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref("新增用户")
const isEdit = ref(false)
const formData = reactive({
  id: null as number | null,
  username: "",
  password: "",
  realName: "",
  role: 6,
  orgName: "",
  departmentId: undefined as number | undefined
})

function openCreateDialog() {
  isEdit.value = false
  dialogTitle.value = "新增用户"
  formData.id = null
  formData.username = ""
  formData.password = ""
  formData.realName = ""
  formData.role = 6
  formData.orgName = ""
  formData.departmentId = undefined
  dialogVisible.value = true
}

function openEditDialog(row: UserTreeRow) {
  if (row.nodeType !== "user" || row.id == null) return
  isEdit.value = true
  dialogTitle.value = "编辑用户"
  formData.id = row.id
  formData.username = row.username ?? ""
  formData.password = ""
  formData.realName = row.realName || ""
  formData.role = row.role ?? 6
  formData.orgName = row.orgName || ""
  formData.departmentId = row.departmentId || undefined
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (isEdit.value) {
      if (formData.password) {
        const passwordError = validateStrongPassword(formData.password)
        if (passwordError) {
          ElMessage.warning(passwordError)
          return
        }
      }
      const payload: Record<string, any> = {
        id: formData.id,
        username: formData.username,
        realName: formData.realName,
        role: formData.role,
        orgName: formData.orgName,
        departmentId: formData.departmentId
      }
      if (formData.password) payload.password = formData.password
      await updateUserApi(payload)
      ElMessage.success("更新成功")
    } else {
      if (!formData.password) {
        ElMessage.warning("请输入密码")
        return
      }
      const passwordError = validateStrongPassword(formData.password)
      if (passwordError) {
        ElMessage.warning(passwordError)
        return
      }
      await createUserApi({ ...formData })
      ElMessage.success("创建成功")
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

// ==================== 删除 ====================
async function handleDelete(row: UserTreeRow) {
  if (row.nodeType !== "user" || row.id == null) return
  try {
    await ElMessageBox.confirm(`确认删除用户 "${row.username}" 吗？`, "提示", { type: "warning" })
    await deleteUserApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch { /* cancelled or handled */ }
}

fetchData()
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="用户名或真实姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">
            用户管理
            <span class="user-total">（共 {{ totalUserCount }} 人）</span>
          </span>
          <div class="toolbar-actions">
            <el-button @click="toggleExpandAll">
              {{ isExpandAll ? "折叠全部" : "展开全部" }}
            </el-button>
            <el-button v-permission="'user:create'" type="primary" @click="openCreateDialog">
              新增用户
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="mb-3"
        title="按部门树形展示：点击部门行可折叠/展开其下用户；五级等基层用户较多时，可先折叠上级再逐级展开查找。"
      />

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="treeData"
        row-key="rowKey"
        border
        stripe
        :tree-props="{ children: 'children' }"
      >
        <el-table-column label="部门 / 用户" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="row.nodeType === 'dept'">
              <span class="dept-name">{{ row.name }}</span>
              <el-tag v-if="row.userCount" size="small" type="info" class="ml-2">
                {{ row.userCount }} 人
              </el-tag>
            </template>
            <template v-else>
              <span class="user-name">{{ row.realName || row.username }}</span>
              <span v-if="row.realName" class="user-sub">（{{ row.username }}）</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="层级 / 角色" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.nodeType === 'dept'" :type="DEPT_LEVEL_TAG[row.level ?? 1]" size="small">
              {{ DEPT_LEVEL_MAP[row.level ?? 1] || "—" }}
            </el-tag>
            <el-tag v-else :type="roleTagType(row.role)" size="small">
              {{ ROLE_MAP[row.role] || "未知" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="ID" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.nodeType === 'user'">{{ row.id }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="真实姓名" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.nodeType === 'user'">{{ row.realName || "—" }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="所属机构" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.nodeType === 'user'">{{ row.orgName || "—" }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span v-if="row.nodeType === 'user'">{{ row.createTime || "—" }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.nodeType === 'user'">
              <el-button v-permission="'user:edit'" type="primary" size="small" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button
                v-permission="'user:delete'"
                type="danger"
                size="small"
                :disabled="row.role === 1"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item :label="isEdit ? '新密码' : '密码'">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改' : '请输入密码'"
          />
          <div class="password-hint">
            {{ STRONG_PASSWORD_HINT }}
          </div>
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select
            v-model="formData.departmentId"
            placeholder="请选择部门（超级管理员无需选择）"
            clearable
            filterable
            style="width: 100%"
            :disabled="formData.role === 1"
          >
            <el-option
              v-for="dept in departmentSelectOptions"
              :key="dept.value"
              :label="dept.label"
              :value="dept.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属机构">
          <el-input v-model="formData.orgName" placeholder="请输入所属机构名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSubmit">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}

.mb-3 {
  margin-bottom: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.user-total {
  font-size: 14px;
  font-weight: normal;
  color: var(--el-text-color-secondary);
}

.dept-name {
  font-weight: 600;
}

.user-name {
  font-weight: 500;
}

.user-sub {
  margin-left: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

.ml-2 {
  margin-left: 8px;
}

.password-hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}
</style>
