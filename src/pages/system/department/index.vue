<script lang="ts" setup>
import {
  getDepartmentListApi,
  createDepartmentApi,
  updateDepartmentApi,
  deleteDepartmentApi,
  type Department
} from "@@/apis/department"

const loading = ref(false)
const tableData = ref<Department[]>([])

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getDepartmentListApi()
    tableData.value = data
  } finally {
    loading.value = false
  }
}

// ==================== 新增/编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref("新增部门")
const isEdit = ref(false)
const formRef = ref()
const formData = reactive<{ id: number | null; name: string; description: string }>({
  id: null,
  name: "",
  description: ""
})

const rules = {
  name: [{ required: true, message: "部门名称不能为空", trigger: "blur" }]
}

function openCreateDialog() {
  isEdit.value = false
  dialogTitle.value = "新增部门"
  formData.id = null
  formData.name = ""
  formData.description = ""
  dialogVisible.value = true
}

function openEditDialog(row: Department) {
  isEdit.value = true
  dialogTitle.value = "编辑部门"
  formData.id = row.id!
  formData.name = row.name
  formData.description = row.description || ""
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  try {
    if (isEdit.value) {
      await updateDepartmentApi({ id: formData.id!, name: formData.name, description: formData.description })
      ElMessage.success("更新成功")
    } else {
      await createDepartmentApi({ name: formData.name, description: formData.description })
      ElMessage.success("创建成功")
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

// ==================== 删除 ====================
async function handleDelete(row: Department) {
  try {
    await ElMessageBox.confirm(`确认删除部门「${row.name}」吗？删除后该部门下用户将失去部门归属。`, "提示", { type: "warning" })
    await deleteDepartmentApi(row.id!)
    ElMessage.success("删除成功")
    fetchData()
  } catch { /* cancelled or handled */ }
}

fetchData()
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">部门管理</span>
          <el-button type="primary" @click="openCreateDialog">新增部门</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="部门名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>
