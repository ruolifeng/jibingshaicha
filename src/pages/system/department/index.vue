<script lang="ts" setup>
import type { Department } from "@@/apis/department"
import {
  createDepartmentApi,
  deleteDepartmentApi,
  getDepartmentListApi,
  updateDepartmentApi
} from "@@/apis/department"

const LEVEL_MAP: Record<number, string> = {
  1: "市级",
  2: "区县",
  3: "社区"
}

const LEVEL_OPTIONS = [
  { value: 1, label: "市级（可挂下属区县/社区数据）" },
  { value: 2, label: "区县（仅本区及下属社区，与兄弟区县隔离）" },
  { value: 3, label: "社区（仅本机构及下属，挂在区县下）" }
]

const loading = ref(false)
const tableData = ref<Department[]>([])

function getParentName(parentId: number | null | undefined) {
  if (parentId == null) return "—"
  return tableData.value.find((d: Department) => d.id === parentId)?.name || "—"
}

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
const formData = reactive<{
  id: number | null
  name: string
  description: string
  level: number
  parentId: number | undefined
}>({
  id: null,
  name: "",
  description: "",
  level: 1,
  parentId: undefined
})

const rules = {
  name: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
  level: [{ required: true, message: "请选择层级", trigger: "change" }]
}

/** 当前可选的上级：区县只能选市级；社区只能选区县 */
const parentOptions = computed(() => {
  const selfId = formData.id
  const rows = tableData.value.filter((d: Department): d is Department & { id: number } => d.id != null)
  if (formData.level === 2) {
    return rows.filter((d: Department & { id: number }) => d.id !== selfId && d.level === 1).map(d => ({ id: d.id, name: d.name }))
  }
  if (formData.level === 3) {
    return rows.filter((d: Department & { id: number }) => d.id !== selfId && d.level === 2).map(d => ({ id: d.id, name: d.name }))
  }
  return [] as { id: number, name: string }[]
})

function openCreateDialog() {
  isEdit.value = false
  dialogTitle.value = "新增部门"
  formData.id = null
  formData.name = ""
  formData.description = ""
  formData.level = 1
  formData.parentId = undefined
  dialogVisible.value = true
}

function openEditDialog(row: Department) {
  isEdit.value = true
  dialogTitle.value = "编辑部门"
  formData.id = row.id!
  formData.name = row.name
  formData.description = row.description || ""
  formData.level = row.level ?? 1
  formData.parentId = row.parentId ?? undefined
  dialogVisible.value = true
}

watch(
  () => formData.level,
  (lv: number) => {
    if (lv === 1) {
      formData.parentId = undefined
    }
  }
)

async function handleSubmit() {
  await formRef.value?.validate()
  if (formData.level !== 1 && (formData.parentId == null || formData.parentId === undefined)) {
    ElMessage.warning("区县或社区部门必须选择上级部门")
    return
  }
  try {
    const parentId = formData.level === 1 ? null : formData.parentId
    if (isEdit.value) {
      await updateDepartmentApi({
        id: formData.id!,
        name: formData.name,
        description: formData.description,
        level: formData.level,
        parentId
      })
      ElMessage.success("更新成功")
    } else {
      await createDepartmentApi({
        name: formData.name,
        description: formData.description,
        level: formData.level,
        parentId
      })
      ElMessage.success("创建成功")
    }
    dialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

// ==================== 删除 ====================
async function handleDelete(row: Department) {
  try {
    await ElMessageBox.confirm(
      `确认删除部门「${row.name}」吗？若存在下级部门或关联用户，请先处理。`,
      "提示",
      { type: "warning" }
    )
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
          <el-button type="primary" @click="openCreateDialog">
            新增部门
          </el-button>
        </div>
      </template>

      <el-alert type="info" :closable="false" class="mb-3" title="三级结构：市级（1）可查看全部下属区县与社区数据；同级区县互不可见；社区挂在区县下。" />

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" />
        <el-table-column label="层级">
          <template #default="{ row }">
            {{ LEVEL_MAP[row.level ?? 1] || "—" }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="部门名称" />
        <el-table-column label="上级部门">
          <template #default="{ row }">
            {{ getParentName(row.parentId) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="96px">
        <el-form-item label="部门层级" prop="level">
          <el-select v-model="formData.level" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="item in LEVEL_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.level !== 1" label="上级部门" prop="parentId">
          <el-select
            v-model="formData.parentId"
            placeholder="请选择上级部门"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="p in parentOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" />
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
.mb-3 {
  margin-bottom: 12px;
}
</style>
