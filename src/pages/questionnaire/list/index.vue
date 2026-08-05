<script lang="ts" setup>
import type { QuestionnaireCategoryItem, QuestionnaireItem } from "../apis/type"
import { fmtDate } from "@@/utils/datetime"
import { ArrowDown, CopyDocument, Delete, Edit, Grid, List, Plus, Promotion, Search, View } from "@element-plus/icons-vue"
import QRCode from "qrcode"
import {
  createFromTemplateApi,
  createQuestionnaireApi,
  deleteQuestionnaireApi,
  getCategoryListApi,
  getQuestionnairePageApi,
  getTemplateListApi,
  saveAsTemplateApi,
  updateQuestionnaireApi,
  updateQuestionnaireStatusApi
} from "../apis"

const router = useRouter()
const loading = ref(false)
const tableData = ref<QuestionnaireItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref("")
const statusFilter = ref<number | undefined>(undefined)

// 视图模式：list=列表，card=卡片
const viewMode = ref<"list" | "card">("list")

const dialogVisible = ref(false)
const dialogTitle = ref("创建问卷")
const editingId = ref<string | number | null>(null)

const categories = ref<QuestionnaireCategoryItem[]>([])
const categoryMap = computed(() => {
  const map: Record<string, string> = {}
  for (const c of categories.value) map[c.code] = c.name
  return map
})
const defaultCategory = computed(() => categories.value.find(c => c.code === "custom")?.code || categories.value[0]?.code || "")

const formData = reactive({
  title: "",
  description: "",
  category: ""
})

const statusMap: Record<number, { label: string, type: "info" | "success" | "warning" | "danger" }> = {
  0: { label: "草稿", type: "info" },
  1: { label: "已发布", type: "success" },
  2: { label: "已暂停", type: "warning" },
  3: { label: "已关闭", type: "danger" }
}

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
    const { data } = await getQuestionnairePageApi({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchData()
}

function openCreate() {
  dialogTitle.value = "创建问卷"
  editingId.value = null
  Object.assign(formData, { title: "", description: "", category: defaultCategory.value })
  dialogVisible.value = true
}

function openEdit(row: QuestionnaireItem) {
  dialogTitle.value = "编辑问卷"
  editingId.value = row.id
  Object.assign(formData, { title: row.title, description: row.description || "", category: row.category })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formData.title) {
    ElMessage.warning("请输入问卷标题")
    return
  }
  if (!formData.category) {
    ElMessage.warning("请选择问卷分类")
    return
  }
  try {
    if (editingId.value) {
      await updateQuestionnaireApi(editingId.value, { ...formData })
      ElMessage.success("更新成功")
    } else {
      await createQuestionnaireApi({ ...formData })
      ElMessage.success("创建成功")
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error("操作失败")
  }
}

async function handleDelete(row: QuestionnaireItem) {
  await ElMessageBox.confirm(`确认删除问卷「${row.title}」？`, "删除确认", { type: "warning" })
  await deleteQuestionnaireApi(row.id)
  ElMessage.success("删除成功")
  fetchData()
}

async function handlePublish(row: QuestionnaireItem) {
  const nextStatus = row.status === 0 ? 1 : row.status === 1 ? 2 : row.status === 2 ? 1 : 0
  const actionLabel = nextStatus === 1 ? "发布" : nextStatus === 2 ? "暂停" : "恢复"
  await ElMessageBox.confirm(`确认${actionLabel}问卷「${row.title}」？`, `${actionLabel}确认`, { type: "info" })
  await updateQuestionnaireStatusApi(row.id, nextStatus)
  ElMessage.success(`${actionLabel}成功`)
  fetchData()
}

async function handleClose(row: QuestionnaireItem) {
  await ElMessageBox.confirm(`确认关闭问卷「${row.title}」？关闭后无法再填写。`, "关闭确认", { type: "warning" })
  await updateQuestionnaireStatusApi(row.id, 3)
  ElMessage.success("已关闭")
  fetchData()
}

function goDesign(row: QuestionnaireItem) {
  router.push(`/questionnaire/design/${row.id}`)
}

function goData(row: QuestionnaireItem) {
  router.push(`/questionnaire/data/${row.id}`)
}

// 二维码弹窗
const qrDialog = ref(false)
const qrUrl = ref("")
const qrDataUrl = ref("")
const qrCanvasRef = ref<HTMLCanvasElement>()

async function showQrCode(row: QuestionnaireItem) {
  const { origin, pathname } = window.location
  qrUrl.value = `${origin}${pathname}#/fill/${row.id}`
  qrDialog.value = true
  await nextTick()
  if (qrCanvasRef.value) {
    await QRCode.toCanvas(qrCanvasRef.value, qrUrl.value, { width: 250, margin: 2 })
  }
  qrDataUrl.value = await QRCode.toDataURL(qrUrl.value, { width: 250, margin: 2 })
}

function downloadQrCode() {
  if (!qrDataUrl.value) return
  const link = document.createElement("a")
  link.href = qrDataUrl.value
  link.download = "问卷二维码.png"
  link.click()
}

function copyQrUrl() {
  navigator.clipboard.writeText(qrUrl.value).then(() => {
    ElMessage.success("链接已复制")
  })
}

function printQrCode() {
  if (!qrDataUrl.value) return
  const win = window.open("", "_blank")
  if (win) {
    win.document.write(`<html><body style="text-align:center;padding:40px"><img src="${qrDataUrl.value}" style="width:300px" /><p style="font-size:16px;margin-top:16px">扫描二维码填写问卷</p><p style="font-size:12px;color:#999;word-break:break-all">${qrUrl.value}</p></body></html>`)
    win.document.close()
    win.onload = () => {
      win.print()
      win.close()
    }
  }
}

// 模板功能
const templateDialog = ref(false)
const templates = ref<QuestionnaireItem[]>([])
const selectedTemplateId = ref<string | number | undefined>(undefined)
const templateTitle = ref("")

async function handleSaveAsTemplate(row: QuestionnaireItem, templateType: "public" | "private") {
  const typeLabel = templateType === "public" ? "公用模板" : "专属模板"
  const tip = templateType === "public" ? "（本部门树可见）" : "（本部门树可见）"
  await ElMessageBox.confirm(`将「${row.title}」保存为${typeLabel}${tip}？`, "保存为模板", { type: "info" })
  try {
    await saveAsTemplateApi(row.id, templateType)
    ElMessage.success(`已保存为${typeLabel}`)
  } catch {
    ElMessage.error("保存模板失败")
  }
}

async function openFromTemplate() {
  templateDialog.value = true
  selectedTemplateId.value = undefined
  templateTitle.value = ""
  try {
    const { data } = await getTemplateListApi({ pageNum: 1, pageSize: 100 })
    templates.value = data.records
  } catch {
    templates.value = []
  }
}

async function handleCreateFromTemplate() {
  if (!selectedTemplateId.value) {
    ElMessage.warning("请选择模板")
    return
  }
  try {
    await createFromTemplateApi(selectedTemplateId.value, templateTitle.value || undefined)
    ElMessage.success("从模板创建成功")
    templateDialog.value = false
    fetchData()
  } catch {
    ElMessage.error("创建失败")
  }
}

fetchCategories().then(() => fetchData())
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div style="display: flex; gap: 12px">
          <el-input v-model="keyword" placeholder="搜索问卷标题" clearable style="width: 240px" @keyup.enter="handleSearch" @clear="handleSearch" />
          <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
            <el-option v-for="(v, k) in statusMap" :key="k" :label="v.label" :value="Number(k)" />
          </el-select>
          <el-button :icon="Search" type="primary" @click="handleSearch">
            搜索
          </el-button>
        </div>
        <div style="display: flex; gap: 8px; align-items: center">
          <!-- 视图切换 -->
          <el-button-group>
            <el-button :type="viewMode === 'list' ? 'primary' : 'default'" :icon="List" @click="viewMode = 'list'" />
            <el-button :type="viewMode === 'card' ? 'primary' : 'default'" :icon="Grid" @click="viewMode = 'card'" />
          </el-button-group>
          <el-button v-permission="'questionnaire:create'" :icon="CopyDocument" @click="openFromTemplate">
            从模板创建
          </el-button>
          <el-button v-permission="'questionnaire:create'" type="primary" :icon="Plus" @click="openCreate">
            创建问卷
          </el-button>
        </div>
      </div>

      <!-- 列表视图 -->
      <template v-if="viewMode === 'list'">
        <el-table v-loading="loading" :data="tableData" stripe>
          <el-table-column prop="title" label="问卷标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="分类">
            <template #default="{ row }">
              {{ categoryMap[row.category] || row.category }}
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
                {{ statusMap[row.status]?.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="访问/填写" align="center">
            <template #default="{ row }">
              {{ row.totalVisits }} / {{ row.totalResponses }}
            </template>
          </el-table-column>
          <el-table-column label="创建时间">
            <template #default="{ row }">
              {{ fmtDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" fixed="right" min-width="260">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click="goDesign(row)">
                设计
              </el-button>
              <el-button link type="primary" :icon="View" @click="goData(row)">
                数据
              </el-button>
              <el-button v-if="row.status === 1" link type="success" @click="showQrCode(row)">
                二维码
              </el-button>
              <el-button v-if="row.status !== 3" v-permission="'questionnaire:publish'" link :type="row.status === 1 ? 'warning' : 'success'" :icon="Promotion" @click="handlePublish(row)">
                {{ row.status === 0 ? "发布" : row.status === 1 ? "暂停" : "恢复" }}
              </el-button>
              <el-button v-if="row.status !== 3 && row.status !== 0" v-permission="'questionnaire:publish'" link type="info" @click="handleClose(row)">
                关闭
              </el-button>
              <el-dropdown v-permission="'questionnaire:create'" @command="(type) => handleSaveAsTemplate(row, type)">
                <el-button link type="info" :icon="CopyDocument">
                  存模板<el-icon class="el-icon--right">
                    <ArrowDown />
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="public">
                      存入公用模板（本部门树可见）
                    </el-dropdown-item>
                    <el-dropdown-item command="private">
                      存入专属模板（本部门树可见）
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button v-permission="'questionnaire:update'" link type="primary" :icon="Edit" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-permission="'questionnaire:delete'" link type="danger" :icon="Delete" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 卡片视图 -->
      <template v-else>
        <div v-loading="loading" class="card-grid">
          <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" style="grid-column: 1/-1" />
          <div v-for="row in tableData" :key="row.id" class="q-card">
            <!-- 状态顶部色条 -->
            <div class="card-status-bar" :class="`status-${row.status}`" />
            <div class="card-content">
              <!-- 头部 -->
              <div class="card-header">
                <span class="card-title">{{ row.title }}</span>
                <el-tag :type="statusMap[row.status]?.type || 'info'" size="small" style="flex-shrink:0">
                  {{ statusMap[row.status]?.label }}
                </el-tag>
              </div>
              <!-- 描述 -->
              <div v-if="row.description" class="card-desc">
                {{ row.description }}
              </div>
              <!-- 元信息 -->
              <div class="card-meta">
                <span class="meta-item">
                  <span class="meta-label">分类</span>
                  {{ categoryMap[row.category] || row.category }}
                </span>
              </div>
              <!-- 数据统计 -->
              <div class="card-stats">
                <div class="stat-item">
                  <span class="stat-value">{{ row.totalVisits ?? 0 }}</span>
                  <span class="stat-label">访问量</span>
                </div>
                <div class="stat-divider" />
                <div class="stat-item">
                  <span class="stat-value">{{ row.totalResponses ?? 0 }}</span>
                  <span class="stat-label">填写量</span>
                </div>
                <div class="stat-divider" />
                <div class="stat-item">
                  <span class="stat-value">{{ row.totalVisits ? Math.round((row.totalResponses ?? 0) / row.totalVisits * 100) : 0 }}%</span>
                  <span class="stat-label">完成率</span>
                </div>
              </div>
              <!-- 操作按钮 -->
              <div class="card-actions">
                <el-button link type="primary" size="small" :icon="Edit" @click="goDesign(row)">
                  设计
                </el-button>
                <el-button link type="primary" size="small" :icon="View" @click="goData(row)">
                  数据
                </el-button>
                <el-button v-if="row.status === 1" link type="success" size="small" @click="showQrCode(row)">
                  二维码
                </el-button>
                <el-button v-if="row.status !== 3" v-permission="'questionnaire:publish'" link :type="row.status === 1 ? 'warning' : 'success'" size="small" :icon="Promotion" @click="handlePublish(row)">
                  {{ row.status === 0 ? "发布" : row.status === 1 ? "暂停" : "恢复" }}
                </el-button>
                <el-button v-if="row.status !== 3 && row.status !== 0" v-permission="'questionnaire:publish'" link type="info" size="small" @click="handleClose(row)">
                  关闭
                </el-button>
                <el-dropdown v-permission="'questionnaire:create'" @command="(type) => handleSaveAsTemplate(row, type)">
                  <el-button link type="info" size="small" :icon="CopyDocument">
                    存模板<el-icon class="el-icon--right">
                      <ArrowDown />
                    </el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="public">
                        存入公用模板（本部门树可见）
                      </el-dropdown-item>
                      <el-dropdown-item command="private">
                        存入专属模板（本部门树可见）
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button v-permission="'questionnaire:update'" link type="primary" size="small" @click="openEdit(row)">
                  编辑
                </el-button>
                <el-button v-permission="'questionnaire:delete'" link type="danger" size="small" :icon="Delete" @click="handleDelete(row)">
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="formData.title" placeholder="请输入问卷标题" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="formData.category" style="width: 100%" placeholder="请选择分类">
            <el-option v-for="c in categories" :key="c.code" :label="c.name" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 二维码弹窗 -->
    <el-dialog v-model="qrDialog" title="问卷二维码" width="360px" @closed="qrDataUrl = ''">
      <div style="text-align: center; padding: 8px 0">
        <canvas ref="qrCanvasRef" style="border-radius: 8px; display: block; margin: 0 auto" />
        <el-text type="info" size="small" style="display: block; word-break: break-all; margin-top: 12px; line-height: 1.5">
          {{ qrUrl }}
        </el-text>
        <div style="margin-top: 16px; display: flex; justify-content: center; gap: 10px">
          <el-button type="primary" @click="downloadQrCode">
            下载图片
          </el-button>
          <el-button @click="copyQrUrl">
            复制链接
          </el-button>
          <el-button @click="printQrCode">
            打印
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 从模板创建对话框 -->
    <el-dialog v-model="templateDialog" title="从模板创建问卷" width="520px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="选择模板" required>
          <el-select v-model="selectedTemplateId" placeholder="请选择问卷模板" style="width: 100%">
            <el-option-group v-if="templates.some(t => t.templateType === 'public')" label="公用模板（本部门树）">
              <el-option
                v-for="t in templates.filter(t => t.templateType === 'public')"
                :key="t.id"
                :label="t.title"
                :value="t.id"
              />
            </el-option-group>
            <el-option-group v-if="templates.some(t => t.templateType === 'private')" label="专属模板（本部门树）">
              <el-option
                v-for="t in templates.filter(t => t.templateType === 'private')"
                :key="t.id"
                :label="t.title"
                :value="t.id"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="问卷标题">
          <el-input v-model="templateTitle" placeholder="可选，留空则自动命名" />
        </el-form-item>
      </el-form>
      <template v-if="!templates.length">
        <el-empty description="暂无可用模板，请前往「问卷模板」页面查看，或在问卷列表中将问卷保存为模板" :image-size="80" />
      </template>
      <template #footer>
        <el-button @click="templateDialog = false">
          取消
        </el-button>
        <el-button type="primary" :disabled="!selectedTemplateId" @click="handleCreateFromTemplate">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.q-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  transition:
    box-shadow 0.2s,
    transform 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }
}

.card-status-bar {
  height: 4px;

  &.status-0 {
    background: var(--el-color-info);
  }
  &.status-1 {
    background: var(--el-color-success);
  }
  &.status-2 {
    background: var(--el-color-warning);
  }
  &.status-3 {
    background: var(--el-color-danger);
  }
}

.card-content {
  padding: 14px 16px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  /* stylelint-disable-next-line */
  -webkit-box-orient: vertical;
}

.card-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 10px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  /* stylelint-disable-next-line */
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.meta-label {
  color: var(--el-text-color-placeholder);
}

.card-stats {
  display: flex;
  align-items: center;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 8px 0;
  margin-bottom: 12px;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: var(--el-border-color-lighter);
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 10px;
}
</style>
