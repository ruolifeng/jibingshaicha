<script lang="ts" setup>
import type { Department } from "@@/apis/department"
import { getDepartmentListApi } from "@@/apis/department"
import { createUserApi, deleteUserApi, getUserListApi, updateUserApi } from "@@/apis/users"
import { usePagination } from "@@/composables/usePagination"
import { ROLE_MAP, ROLE_OPTIONS } from "@@/constants/disease"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const departmentList = ref<Department[]>([])

const searchForm = reactive({ username: "", role: undefined as number | undefined })

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getUserListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...searchForm
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function fetchDepartments() {
  try {
    const { data } = await getDepartmentListApi()
    departmentList.value = data
  } catch { /* ignore */ }
}

function getDeptName(departmentId: number | null | undefined) {
  if (!departmentId) return "-"
  return departmentList.value.find(d => d.id === departmentId)?.name || "-"
}

function handleSearch() {
  paginationData.currentPage = 1
  fetchData()
}

function handleReset() {
  searchForm.username = ""
  searchForm.role = undefined
  handleSearch()
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

function openEditDialog(row: any) {
  isEdit.value = true
  dialogTitle.value = "编辑用户"
  formData.id = row.id
  formData.username = row.username
  formData.password = ""
  formData.realName = row.realName || ""
  formData.role = row.role
  formData.orgName = row.orgName || ""
  formData.departmentId = row.departmentId || undefined
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (isEdit.value) {
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
      await createUserApi({ ...formData })
      ElMessage.success("创建成功")
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

// ==================== 删除 ====================
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除用户 "${row.username}" 吗？`, "提示", { type: "warning" })
    await deleteUserApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch { /* cancelled or handled */ }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)

fetchDepartments()
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
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
          <span class="text-lg font-bold">用户管理</span>
          <el-button v-permission="'user:create'" type="primary" @click="openCreateDialog">
            新增用户
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : row.role <= 4 ? 'warning' : 'primary'" size="small">
              {{ ROLE_MAP[row.role] || "未知" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属机构" show-overflow-tooltip />
        <el-table-column label="所属部门">
          <template #default="{ row }">
            {{ getDeptName(row.departmentId) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'user:edit'" type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button v-permission="'user:delete'" type="danger" size="small" :disabled="row.role === 1" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item :label="isEdit ? '新密码' : '密码'">
          <el-input v-model="formData.password" type="password" show-password :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
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
            style="width: 100%"
            :disabled="formData.role === 1"
          >
            <el-option
              v-for="dept in departmentList"
              :key="dept.id"
              :label="dept.name"
              :value="(dept.id as number)"
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
.mt-4 {
  margin-top: 16px;
}
</style>
