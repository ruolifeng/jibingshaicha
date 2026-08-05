<script lang="ts" setup>
import type { QuestionnaireCategoryItem, QuestionnaireItem } from "../apis/type"
import { fmtDate } from "@@/utils/datetime"
import { CopyDocument, Delete, Document, InfoFilled, Lock, Plus, Search, Share } from "@element-plus/icons-vue"
import { createFromTemplateApi, deleteTemplateApi, getCategoryListApi, getTemplateListApi } from "../apis"

const router = useRouter()

// 当前激活的标签页：public=公用模板，private=专属模板
const activeTab = ref<"public" | "private">("public")

const loading = ref(false)
const tableData = ref<QuestionnaireItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const keyword = ref("")

const categories = ref<QuestionnaireCategoryItem[]>([])
const categoryMap = computed(() => {
  const map: Record<string, string> = {}
  for (const c of categories.value) map[c.code] = c.name
  return map
})

async function fetchCategories() {
  try {
    const { data } = await getCategoryListApi()
    categories.value = data
  } catch {
    categories.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getTemplateListApi({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      templateType: activeTab.value
    })
    // 客户端关键字筛选（接口若支持可移至后端）
    const kw = keyword.value.trim().toLowerCase()
    tableData.value = kw
      ? data.records.filter((r: QuestionnaireItem) => r.title.toLowerCase().includes(kw))
      : data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchData()
}

function handleTabChange() {
  pageNum.value = 1
  keyword.value = ""
  fetchData()
}

// 从模板创建问卷
const createDialog = ref(false)
const creatingTemplate = ref<QuestionnaireItem | null>(null)
const newTitle = ref("")

function openCreateFromTemplate(tpl: QuestionnaireItem) {
  creatingTemplate.value = tpl
  newTitle.value = ""
  createDialog.value = true
}

async function handleCreateFromTemplate() {
  if (!creatingTemplate.value) return
  try {
    await createFromTemplateApi(creatingTemplate.value.id, newTitle.value || undefined)
    ElMessage.success("从模板创建成功，请前往问卷列表查看")
    createDialog.value = false
    router.push("/questionnaire/list")
  } catch {
    ElMessage.error("创建失败")
  }
}

// 删除模板
async function handleDelete(row: QuestionnaireItem) {
  await ElMessageBox.confirm(`确认删除模板「${row.title}」？`, "删除确认", { type: "warning" })
  try {
    await deleteTemplateApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch {
    ElMessage.error("删除失败")
  }
}

fetchCategories().then(() => fetchData())
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <!-- 标题说明 -->
      <div class="page-header">
        <div>
          <h3 class="page-title">
            问卷模板
          </h3>
          <p class="page-desc">
            <el-icon class="desc-icon">
              <InfoFilled />
            </el-icon>
            <span><b>公用模板</b> / <b>专属模板</b>：均按部门树隔离，上级可见下级，同级互不可见</span>
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/questionnaire/list')">
          去问卷列表保存模板
        </el-button>
      </div>

      <!-- Tab 切换 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane name="public">
          <template #label>
            <span class="tab-label">
              <el-icon><Share /></el-icon>
              公用模板
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="private">
          <template #label>
            <span class="tab-label">
              <el-icon><Lock /></el-icon>
              专属模板
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索模板名称"
          clearable
          style="width: 280px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">
          搜索
        </el-button>
      </div>

      <!-- 卡片网格 -->
      <div v-loading="loading" class="template-grid">
        <el-empty v-if="!loading && tableData.length === 0" description="暂无模板" :image-size="100" />

        <div v-for="tpl in tableData" :key="tpl.id" class="tpl-card">
          <!-- 类型标签 -->
          <div class="tpl-badge" :class="tpl.templateType === 'public' ? 'badge-public' : 'badge-private'">
            {{ tpl.templateType === "public" ? "公用" : "专属" }}
          </div>

          <div class="tpl-body">
            <div class="tpl-icon">
              <el-icon size="28">
                <Document />
              </el-icon>
            </div>
            <div class="tpl-info">
              <div class="tpl-title" :title="tpl.title">
                {{ tpl.title }}
              </div>
              <div class="tpl-meta">
                <span>{{ categoryMap[tpl.category] || tpl.category }}</span>
                <span class="meta-sep">·</span>
                <span>{{ fmtDate(tpl.createdAt) }}</span>
              </div>
              <div v-if="tpl.description" class="tpl-desc">
                {{ tpl.description }}
              </div>
            </div>
          </div>

          <div class="tpl-footer">
            <el-button type="primary" size="small" :icon="CopyDocument" @click="openCreateFromTemplate(tpl)">
              使用此模板
            </el-button>
            <!-- 专属模板：有 questionnaire:template:delete 权限可删 -->
            <el-button
              v-if="tpl.templateType === 'private'"
              v-permission="'questionnaire:template:delete'"
              type="danger"
              size="small"
              plain
              :icon="Delete"
              @click="handleDelete(tpl)"
            >
              删除
            </el-button>
            <!-- 模板删除：需 questionnaire:template:delete 且在数据范围内 -->
            <el-button
              v-if="tpl.templateType === 'public'"
              v-permission="'questionnaire:template:delete'"
              type="danger"
              size="small"
              plain
              :icon="Delete"
              @click="handleDelete(tpl)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </el-card>

    <!-- 从模板创建弹窗 -->
    <el-dialog v-model="createDialog" title="从模板创建问卷" width="460px" :close-on-click-modal="false">
      <div v-if="creatingTemplate" class="create-preview">
        <div class="preview-label">
          模板
        </div>
        <div class="preview-title">
          {{ creatingTemplate.title }}
        </div>
        <div v-if="creatingTemplate.description" class="preview-desc">
          {{ creatingTemplate.description }}
        </div>
      </div>
      <el-form label-width="90px" style="margin-top: 16px">
        <el-form-item label="问卷标题">
          <el-input v-model="newTitle" placeholder="留空则自动以模板名命名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleCreateFromTemplate">
          创建问卷
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}

.page-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.page-desc {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.desc-icon {
  color: var(--el-color-primary);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  min-height: 140px;
}

.tpl-card {
  position: relative;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
  background: var(--el-bg-color);
  transition:
    box-shadow 0.2s,
    transform 0.2s;
  display: flex;
  flex-direction: column;

  &:hover {
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }
}

.tpl-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;

  &.badge-public {
    background: var(--el-color-primary-light-8);
    color: var(--el-color-primary);
  }

  &.badge-private {
    background: var(--el-color-success-light-8);
    color: var(--el-color-success);
  }
}

.tpl-body {
  display: flex;
  gap: 12px;
  padding: 16px 16px 10px;
  flex: 1;
}

.tpl-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tpl-info {
  flex: 1;
  min-width: 0;
}

.tpl-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 40px;
  line-height: 1.4;
  margin-bottom: 4px;
}

.tpl-meta {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}

.meta-sep {
  color: var(--el-border-color);
}

.tpl-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  /* stylelint-disable-next-line */
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.tpl-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-extra-light);
}

.create-preview {
  padding: 12px 14px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.preview-label {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 4px;
}

.preview-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.preview-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
