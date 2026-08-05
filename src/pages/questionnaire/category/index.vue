<script lang="ts" setup>
import type { QuestionnaireCategoryItem } from "../apis/type"
import { fmtDate } from "@@/utils/datetime"
import { Delete, Edit, Plus } from "@element-plus/icons-vue"
import {
  createCategoryApi,
  deleteCategoryApi,
  getCategoryListApi,
  updateCategoryApi
} from "../apis"

const loading = ref(false)
const tableData = ref<QuestionnaireCategoryItem[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref("新增分类")
const editingId = ref<string | number | null>(null)
const submitting = ref(false)

const formData = reactive({
  code: "",
  name: "",
  sort: 0
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getCategoryListApi()
    tableData.value = data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogTitle.value = "新增分类"
  editingId.value = null
  Object.assign(formData, { code: "", name: "", sort: (tableData.value.length + 1) * 10 })
  dialogVisible.value = true
}

function openEdit(row: QuestionnaireCategoryItem) {
  dialogTitle.value = "编辑分类"
  editingId.value = row.id
  Object.assign(formData, { code: row.code, name: row.name, sort: row.sort ?? 0 })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formData.name.trim()) {
    ElMessage.warning("请输入分类名称")
    return
  }
  if (!editingId.value && !formData.code.trim()) {
    ElMessage.warning("请输入分类编码")
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateCategoryApi(editingId.value, { name: formData.name.trim(), sort: formData.sort })
      ElMessage.success("更新成功")
    } else {
      await createCategoryApi({
        code: formData.code.trim(),
        name: formData.name.trim(),
        sort: formData.sort
      })
      ElMessage.success("创建成功")
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 错误由 axios 拦截器提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: QuestionnaireCategoryItem) {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」？若仍有问卷引用将无法删除。`, "删除确认", { type: "warning" })
  try {
    await deleteCategoryApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch {
    // 错误由 axios 拦截器提示
  }
}

fetchData()
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-hint">
          分类编码写入问卷记录，创建后不可修改；名称与排序可随时调整。
        </div>
        <el-button v-permission="'questionnaire:category'" type="primary" :icon="Plus" @click="openCreate">
          新增分类
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="code" label="编码" min-width="140" />
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ fmtDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'questionnaire:category'" link type="primary" :icon="Edit" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'questionnaire:category'" link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="formData.name" placeholder="如：结核病筛查" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input
            v-model="formData.code"
            placeholder="如：tb_screening（字母开头）"
            :disabled="!!editingId"
            maxlength="50"
          />
          <div v-if="editingId" class="form-tip">
            编码创建后不可修改
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.form-tip {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
