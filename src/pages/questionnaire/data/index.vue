<script lang="ts" setup>
import type {
  AnswerItem,
  ChoiceQuestionStat,
  QuestionItem,
  QuestionnaireItem,
  QuestionnaireStatistics,
  ResponseItem
} from "../apis/type"
import { fmtDate } from "@@/utils/datetime"
import { authDownload } from "@@/utils/download"
import { ArrowLeft, CircleCheck, DataAnalysis, Download, Edit, Histogram, View, WarnTriangleFilled } from "@element-plus/icons-vue"
import { getQuestionnaireDetailApi, getResponseDetailApi, getResponsePageApi, getStatisticsApi, updateResponseStatusApi } from "../apis"
import ChoiceOptionStatsCharts from "./components/ChoiceOptionStatsCharts.vue"

const route = useRoute()
const router = useRouter()
const qId = computed(() => route.params.id as string)

const questionnaire = ref<QuestionnaireItem | null>(null)
const responses = ref<ResponseItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref<number | undefined>(undefined)
const loading = ref(false)

const stats = ref<QuestionnaireStatistics | null>(null)

const choiceRadioStats = computed(() =>
  (stats.value?.choiceQuestionStats ?? []).filter((s: ChoiceQuestionStat) => s.type === "radio")
)
const choiceCheckboxStats = computed(() =>
  (stats.value?.choiceQuestionStats ?? []).filter((s: ChoiceQuestionStat) => s.type === "checkbox")
)
const choiceTab = ref<"radio" | "checkbox">("radio")

watch(
  stats,
  (s) => {
    if (!s?.choiceQuestionStats?.length) return
    const hasR = s.choiceQuestionStats.some(x => x.type === "radio")
    const hasC = s.choiceQuestionStats.some(x => x.type === "checkbox")
    if (!hasR && hasC) choiceTab.value = "checkbox"
    else if (hasR) choiceTab.value = "radio"
  },
  { immediate: true }
)

const statusMap: Record<number, { label: string, type: "info" | "success" | "warning" | "danger" | "primary" }> = {
  0: { label: "进行中", type: "primary" },
  1: { label: "有效提交", type: "success" },
  2: { label: "不良样本", type: "danger" }
}

/** 过滤器下拉选项（-1 为特殊值：全部已提交，含不良） */
const filterOptions = [
  { label: "已提交（含不良）", value: -1 },
  { label: "有效提交", value: 1 },
  { label: "不良样本", value: 2 },
  { label: "进行中", value: 0 }
]

async function fetchData() {
  loading.value = true
  try {
    const params: { pageNum: number, pageSize: number, status?: number, submitted?: boolean } = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (statusFilter.value === -1) {
      params.submitted = true
    } else if (statusFilter.value !== undefined) {
      params.status = statusFilter.value
    }
    const { data } = await getResponsePageApi(qId.value, params)
    responses.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function fetchAll() {
  const { data: q } = await getQuestionnaireDetailApi(qId.value)
  questionnaire.value = q
  const { data: s } = await getStatisticsApi(qId.value)
  stats.value = s
  fetchData()
}

function sanitizeFileName(name: string): string {
  return name.replace(/[\\/:*?"<>|]/g, "_").trim() || "问卷数据"
}

const exportLoading = ref(false)

/** scope: "valid" 仅有效提交 | "submitted" 含不良 | "all" 全部；format: label 可读文本 | spss 编码 */
async function handleExport(
  scope: "valid" | "submitted" | "all" = "submitted",
  format: "label" | "spss" = "label"
) {
  if (exportLoading.value) return
  exportLoading.value = true

  const loadingMsg = ElMessage({ message: "正在导出，请稍候…", type: "info", duration: 0 })

  try {
    const params = new URLSearchParams()
    if (scope === "valid") params.set("status", "1")
    else if (scope === "submitted") params.set("submitted", "true")
    params.set("format", format)

    const query = params.toString()
    const url = `/questionnaire/${qId.value}/export${query ? `?${query}` : ""}`

    const scopeLabel = scope === "valid" ? "有效提交" : scope === "submitted" ? "全部已提交" : "全部记录"
    const dateStr = new Date().toLocaleDateString("zh-CN").replace(/\//g, "-")
    const fileName = `${sanitizeFileName(questionnaire.value?.title || "问卷数据")}_${scopeLabel}_${dateStr}.xlsx`

    const ok = await authDownload(url, fileName, { showError: false, timeout: 120_000 })
    if (ok) {
      ElMessage.success("导出完成")
    } else {
      ElMessage.error("导出失败，请重试")
    }
  } catch (err) {
    console.error("[Export]", err)
    ElMessage.error("导出失败，请重试")
  } finally {
    exportLoading.value = false
    loadingMsg.close()
  }
}

function formatDuration(seconds: number | null): string {
  if (seconds == null) return "-"
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}分${s}秒`
}

// 查看详情
const detailDialog = ref(false)
const detailLoading = ref(false)
const detailAnswers = ref<AnswerItem[]>([])
const detailQuestions = ref<QuestionItem[]>([])
const detailResponse = ref<ResponseItem | null>(null)

async function handleViewDetail(row: ResponseItem) {
  detailDialog.value = true
  detailLoading.value = true
  try {
    const { data } = await getResponseDetailApi(row.id)
    detailResponse.value = data.response
    detailAnswers.value = data.answers
    detailQuestions.value = data.questions
  } catch {
    ElMessage.error("加载详情失败")
  } finally {
    detailLoading.value = false
  }
}

function getQuestionTitle(qId: string | number): string {
  const q = detailQuestions.value.find(q => q.id === qId)
  return q ? q.title || "(未设置标题)" : `题目#${qId}`
}

// 编辑状态
const editStatusDialog = ref(false)
const editingRow = ref<ResponseItem | null>(null)
const editStatus = ref(0)

function openEditStatus(row: ResponseItem) {
  editingRow.value = row
  editStatus.value = row.status
  editStatusDialog.value = true
}

async function handleSaveStatus() {
  if (!editingRow.value) return
  try {
    await updateResponseStatusApi(editingRow.value.id, editStatus.value)
    ElMessage.success("状态更新成功")
    editStatusDialog.value = false
    fetchAll()
  } catch {
    ElMessage.error("更新失败")
  }
}

fetchAll()
</script>

<template>
  <div class="app-container">
    <el-page-header @back="router.push('/questionnaire/list')">
      <template #icon>
        <el-icon><ArrowLeft /></el-icon>
      </template>
      <template #title>
        返回列表
      </template>
      <template #content>
        <span style="font-size: 16px; font-weight: 600">{{ questionnaire?.title || "问卷数据" }}</span>
      </template>
      <template #extra>
        <el-dropdown
          v-permission="'questionnaire:data:export'"
          split-button
          type="primary"
          :loading="exportLoading"
          @click="handleExport('submitted', 'label')"
        >
          <el-icon style="margin-right: 5px">
            <Download />
          </el-icon>
          导出 Excel
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleExport('valid', 'label')">
                导出有效提交（可读文本）
              </el-dropdown-item>
              <el-dropdown-item @click="handleExport('submitted', 'label')">
                导出全部已提交（可读文本）
              </el-dropdown-item>
              <el-dropdown-item @click="handleExport('all', 'label')">
                导出全部记录（可读文本）
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleExport('submitted', 'spss')">
                导出 SPSS 编码格式
              </el-dropdown-item>
              <el-dropdown-item @click="handleExport('valid', 'spss')">
                导出有效提交（SPSS 编码）
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </el-page-header>

    <!-- 统计卡片 -->
    <div v-if="stats" class="stats-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-icon-wrap" style="background: #ecf5ff">
          <el-icon color="#409eff" :size="22">
            <DataAnalysis />
          </el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value" style="color: var(--el-color-primary)">
            {{ stats.totalVisits }}
          </div>
          <div class="stat-label">
            访问量
          </div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-icon-wrap" style="background: #f0f9eb">
          <el-icon color="#67c23a" :size="22">
            <CircleCheck />
          </el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value" style="color: var(--el-color-success)">
            {{ stats.submitted }}
          </div>
          <div class="stat-label">
            有效提交
            <span v-if="stats.badSample > 0" style="font-size: 11px; color: #c0c4cc; margin-left: 4px">（含 {{ stats.badSample }} 不良）</span>
          </div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-icon-wrap" style="background: #fef0f0">
          <el-icon color="#f56c6c" :size="22">
            <WarnTriangleFilled />
          </el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value" style="color: var(--el-color-danger)">
            {{ stats.badSample }}
          </div>
          <div class="stat-label">
            不良样本
          </div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-icon-wrap" style="background: #fdf6ec">
          <el-icon color="#e6a23c" :size="22">
            <Histogram />
          </el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value" style="color: var(--el-color-warning)">
            {{ stats.completionRate }}%
          </div>
          <div class="stat-label">
            完成率
          </div>
        </div>
      </el-card>
    </div>

    <!-- 单选 / 多选选项统计 -->
    <el-card
      v-if="stats && (choiceRadioStats.length > 0 || choiceCheckboxStats.length > 0)"
      shadow="never"
      class="choice-stats-card"
    >
      <template #header>
        <div class="choice-stats-header">
          <span style="font-weight: 600">选项题答题统计</span>
          <el-text size="small" type="info" style="margin-left: 10px">
            基于已提交与不良样本的回收记录，展示各选项被选择人数及占比
          </el-text>
        </div>
      </template>

      <el-tabs v-model="choiceTab" class="choice-tabs">
        <el-tab-pane :label="`单选题（${choiceRadioStats.length}）`" name="radio">
          <el-empty v-if="choiceRadioStats.length === 0" description="本问卷暂无单选题" />
          <el-collapse v-else accordion class="choice-collapse">
            <el-collapse-item v-for="item in choiceRadioStats" :key="item.questionId" :name="String(item.questionId)">
              <template #title>
                <div class="collapse-title">
                  <span class="q-order">第 {{ item.sortOrder }} 题</span>
                  <span class="q-title-text">{{ item.title || "（未命名题目）" }}</span>
                  <el-tag size="small" type="info" effect="plain">
                    回收基数 {{ item.baseCount }}
                  </el-tag>
                </div>
              </template>
              <el-table :data="item.optionRows" stripe size="small" border class="choice-table">
                <el-table-column prop="label" label="选项" min-width="160" show-overflow-tooltip />
                <el-table-column prop="count" label="人数" width="100" align="center" />
                <el-table-column label="占比" width="110" align="center">
                  <template #default="{ row }">
                    {{ row.percent }}%
                  </template>
                </el-table-column>
              </el-table>
              <ChoiceOptionStatsCharts :rows="item.optionRows" />
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>

        <el-tab-pane :label="`多选题（${choiceCheckboxStats.length}）`" name="checkbox">
          <el-empty v-if="choiceCheckboxStats.length === 0" description="本问卷暂无多选题" />
          <el-alert
            v-else
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
            title="多选题中，各选项人数为「选择该选项的答卷份数」；占比分母为回收基数，各选项占比之和可能大于 100%。"
          />
          <el-collapse v-if="choiceCheckboxStats.length" accordion class="choice-collapse">
            <el-collapse-item v-for="item in choiceCheckboxStats" :key="item.questionId" :name="String(item.questionId)">
              <template #title>
                <div class="collapse-title">
                  <span class="q-order">第 {{ item.sortOrder }} 题</span>
                  <span class="q-title-text">{{ item.title || "（未命名题目）" }}</span>
                  <el-tag size="small" type="info" effect="plain">
                    回收基数 {{ item.baseCount }}
                  </el-tag>
                </div>
              </template>
              <el-table :data="item.optionRows" stripe size="small" border class="choice-table">
                <el-table-column prop="label" label="选项" min-width="160" show-overflow-tooltip />
                <el-table-column prop="count" label="人数" width="100" align="center" />
                <el-table-column label="占比" width="110" align="center">
                  <template #default="{ row }">
                    {{ row.percent }}%
                  </template>
                </el-table-column>
              </el-table>
              <ChoiceOptionStatsCharts :rows="item.optionRows" />
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 回收数据 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">回收数据 <span style="font-size: 13px; color: #909399; font-weight: 400">（共 {{ total }} 条）</span></span>
          <el-select v-model="statusFilter" placeholder="全部状态" clearable size="small" style="width: 150px" @change="fetchData">
            <el-option v-for="opt in filterOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
      </template>

      <el-table v-loading="loading" :data="responses" stripe>
        <el-table-column prop="id" label="编号" align="center" />
        <el-table-column label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间">
          <template #default="{ row }">
            {{ fmtDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间">
          <template #default="{ row }">
            {{ fmtDate(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column label="填写时长" align="center">
          <template #default="{ row }">
            {{ formatDuration(row.durationSeconds) }}
          </template>
        </el-table-column>
        <el-table-column label="IP">
          <template #default="{ row }">
            {{ row.respondentIp || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="handleViewDetail(row)">
              详情
            </el-button>
            <el-button link type="warning" :icon="Edit" @click="openEditStatus(row)">
              状态
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialog" title="回答详情" width="600px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailResponse" :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="编号">
            {{ detailResponse.id }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[detailResponse.status]?.type || 'info'" size="small">
              {{ statusMap[detailResponse.status]?.label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ fmtDate(detailResponse.startTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ fmtDate(detailResponse.submitTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="填写时长">
            {{ formatDuration(detailResponse.durationSeconds) }}
          </el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">
          <span style="font-size: 13px; color: #606266">答案内容</span>
        </el-divider>
        <div v-for="(ans, idx) in detailAnswers" :key="idx" class="answer-item">
          <div class="answer-question">
            <span class="answer-num">{{ idx + 1 }}</span>
            {{ getQuestionTitle(ans.questionId) }}
          </div>
          <div class="answer-value">
            {{ ans.answerValue || '(未作答)' }}
          </div>
        </div>
        <el-empty v-if="!detailLoading && !detailAnswers.length" description="暂无答案" :image-size="60" />
      </div>
    </el-dialog>

    <!-- 编辑状态弹窗 -->
    <el-dialog v-model="editStatusDialog" title="修改回收状态" width="400px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="编号">
          {{ editingRow?.id }}
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editStatus">
            <el-radio :value="0">
              进行中
            </el-radio>
            <el-radio :value="1">
              已提交
            </el-radio>
            <el-radio :value="2">
              不良样本
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editStatusDialog = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleSaveStatus">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 16px;
}
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}
.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-body {
  flex: 1;
  min-width: 0;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.answer-item {
  margin-bottom: 10px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  border-left: 3px solid var(--el-color-primary-light-5);
}
.answer-question {
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.answer-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 50%;
  font-size: 11px;
  flex-shrink: 0;
}
.answer-value {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  padding-left: 24px;
}

.choice-stats-card {
  margin-top: 16px;
}
.choice-stats-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.choice-tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}
.choice-collapse {
  border: none;
}
.choice-collapse :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 48px;
  line-height: 1.4;
  padding: 8px 12px;
}
.collapse-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
  padding-right: 8px;
}
.q-order {
  font-weight: 600;
  color: var(--el-color-primary);
  flex-shrink: 0;
}
.q-title-text {
  flex: 1;
  min-width: 120px;
  color: #303133;
  font-size: 14px;
}
.choice-table {
  margin-bottom: 4px;
}
</style>
