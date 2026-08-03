<script lang="ts" setup>
import type { Department } from "@@/apis/department"
import {
  createDepartmentApi,
  deleteDepartmentApi,
  getDepartmentListApi,
  importDepartmentApi,
  updateDepartmentApi
} from "@@/apis/department"

type DepartmentTree = Department & { children?: DepartmentTree[] }

const LEVEL_MAP: Record<number, string> = {
  1: "市级",
  2: "区县",
  3: "社区/街道/乡镇"
}

const LEVEL_TAG: Record<number, "primary" | "success" | "warning"> = {
  1: "primary",
  2: "success",
  3: "warning"
}

const LEVEL_OPTIONS = [
  { value: 1, label: "市级（可挂下属区县/社区数据）" },
  { value: 2, label: "区县（仅本区及下属社区，与兄弟区县隔离）" },
  { value: 3, label: "社区/街道/乡镇（挂在区县下）" }
]

const loading = ref(false)
const tableData = ref<Department[]>([])
const tableRef = ref()
const isExpandAll = ref(true)
const importLoading = ref(false)

function buildDepartmentTree(list: Department[]): DepartmentTree[] {
  const map = new Map<string, DepartmentTree>()
  const roots: DepartmentTree[] = []

  for (const item of list) {
    if (item.id == null) continue
    map.set(String(item.id), { ...item, children: [] })
  }

  for (const node of map.values()) {
    const parentId = node.parentId != null ? String(node.parentId) : null
    if (parentId != null && map.has(parentId)) {
      map.get(parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  }

  const sortNodes = (nodes: DepartmentTree[]) => {
    nodes.sort((a, b) => (a.level ?? 0) - (b.level ?? 0) || String(a.id ?? "").localeCompare(String(b.id ?? "")))
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

const treeData = computed(() => buildDepartmentTree(tableData.value))

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getDepartmentListApi()
    tableData.value = data
  } finally {
    loading.value = false
  }
}

function toggleExpandAll() {
  isExpandAll.value = !isExpandAll.value
  const toggle = (rows: DepartmentTree[]) => {
    rows.forEach((row) => {
      tableRef.value?.toggleRowExpansion(row, isExpandAll.value)
      if (row.children?.length) {
        toggle(row.children)
      }
    })
  }
  toggle(treeData.value)
}

// ==================== 新增/编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref("新增部门")
const isEdit = ref(false)
const formRef = ref()
const formData = reactive<{
  id: string | null
  name: string
  description: string
  level: number
  parentId: string | undefined
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
  const rows = tableData.value.filter((d: Department): d is Department & { id: string } => d.id != null)
  if (formData.level === 2) {
    return rows.filter((d: Department & { id: string }) => d.id !== selfId && d.level === 1).map((d: Department & { id: string }) => ({ id: d.id, name: d.name }))
  }
  if (formData.level === 3) {
    return rows.filter((d: Department & { id: string }) => d.id !== selfId && d.level === 2).map((d: Department & { id: string }) => ({ id: d.id, name: d.name }))
  }
  return [] as { id: string, name: string }[]
})

function resetForm(level = 1, parentId?: string) {
  formData.id = null
  formData.name = ""
  formData.description = ""
  formData.level = level
  formData.parentId = parentId
}

function openCreateDialog() {
  isEdit.value = false
  dialogTitle.value = "新增部门"
  resetForm(1)
  dialogVisible.value = true
}

function openCreateChildDialog(row: Department) {
  if ((row.level ?? 1) >= 3) return
  isEdit.value = false
  dialogTitle.value = "新增下级部门"
  resetForm((row.level ?? 1) + 1, row.id)
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
    ElMessage.warning("区县或社区/街道/乡镇必须选择上级部门")
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

async function handleImport(uploadFile: any) {
  if (!uploadFile?.raw) return
  importLoading.value = true
  try {
    const { data } = await importDepartmentApi(uploadFile.raw)
    await fetchData()
    if (data.errors?.length) {
      const errorText = data.errors.slice(0, 20).join("<br>")
      const moreText = data.errors.length > 20 ? `<br>其余 ${data.errors.length - 20} 条错误请检查 Excel 后重新导入` : ""
      ElMessageBox.alert(
        `成功导入 ${data.successCount} 条。<br>${errorText}${moreText}`,
        "导入完成",
        { dangerouslyUseHTMLString: true, type: data.successCount > 0 ? "warning" : "error" }
      )
    } else {
      ElMessage.success(`成功导入 ${data.successCount} 条部门`)
    }
  } catch { /* handled */ } finally {
    importLoading.value = false
  }
}

fetchData()
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">部门管理</span>
          <div class="toolbar-actions">
            <el-button @click="toggleExpandAll">
              {{ isExpandAll ? "折叠全部" : "展开全部" }}
            </el-button>
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept=".xlsx,.xls"
              :on-change="handleImport"
            >
              <el-button type="success" :loading="importLoading">
                导入 Excel
              </el-button>
            </el-upload>
            <el-button type="primary" @click="openCreateDialog">
              新增部门
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="mb-3"
        title="三级结构：市级 → 区县 → 社区/街道/乡镇。导入 Excel 首行需包含：部门名称、层级、上级部门、描述。"
      />

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="treeData"
        row-key="id"
        border
        stripe
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="部门名称" min-width="260" show-overflow-tooltip />
        <el-table-column label="层级" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="LEVEL_TAG[row.level ?? 1]" size="small">
              {{ LEVEL_MAP[row.level ?? 1] || "—" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="(row.level ?? 1) < 3"
              type="success"
              link
              size="small"
              @click="openCreateChildDialog(row)"
            >
              新增下级
            </el-button>
            <el-button type="primary" link size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
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

.toolbar-actions {
  display: flex;
  gap: 8px;
}
</style>
