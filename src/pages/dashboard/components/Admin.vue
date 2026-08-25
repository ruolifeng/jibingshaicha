<script lang="ts" setup>
import type { DashboardBatchOption, DashboardSummaryData, MessageStatsData, PopulationStat, TaskStatsData } from "../apis"
import ScopedDepartmentMultiSelect from "@@/components/ScopedDepartmentMultiSelect.vue"
import { buildStatYearOptions, getCurrentStatYear } from "@@/utils/stat-year"
import {
  Bell,
  Calendar,
  Connection,
  DataAnalysis,
  FirstAidKit,
  List,
  Refresh,
  School,
  Search,
  User,
  WarningFilled
} from "@element-plus/icons-vue"
import {
  getDashboardBatchesApi,
  getDashboardMessageStatsApi,
  getDashboardSummaryApi,
  getDashboardTaskStatsApi
} from "../apis"
import PendingTrackingPanel from "./PendingTrackingPanel.vue"
import UpcomingVisitSupervisionPanel from "./UpcomingVisitSupervisionPanel.vue"

const selectedStatYear = ref(String(getCurrentStatYear()))
const selectedDepartmentIds = ref<string[]>([])
const yearOptions = buildStatYearOptions()

const summaryLoading = ref(false)
const yearStatsLoading = ref(false)
const taskLoading = ref(false)
const summary = ref<DashboardSummaryData>({
  pendingTracking: 0,
  pendingVisit: 0,
  pendingNotice: 0,
  upcomingReview: 0
})
const batches = ref<DashboardBatchOption[]>([])
const selectedBatch = ref<string>("")
const taskStats = ref<TaskStatsData>({
  school: { screeningTotal: 0, latentCount: 0, latentRatio: 0, patientCount: 0, patientRatio: 0 },
  keyPopulation: { screeningTotal: 0, latentCount: 0, latentRatio: 0, patientCount: 0, patientRatio: 0 },
  closeContact: { screeningTotal: 0, latentCount: 0, latentRatio: 0, patientCount: 0, patientRatio: 0 }
})
const messageStats = ref<MessageStatsData>({
  latentNoticeSent: 0,
  latentNoticeConfirmed: 0,
  patientNoticeSent: 0,
  patientNoticeConfirmed: 0,
  referralSent: 0,
  referralConfirmed: 0,
  referralRejected: 0
})

/** 待追踪卡片是否展开下方明细 */
const pendingTrackingExpanded = ref(false)

async function fetchYearSummary() {
  yearStatsLoading.value = true
  try {
    const { data } = await getDashboardSummaryApi(selectedStatYear.value, selectedDepartmentIds.value)
    summary.value = { ...summary.value, ...(data || {}) }
  } catch { /* handled globally */ } finally {
    yearStatsLoading.value = false
  }
}

const reminderPanelRef = ref<{ refresh: () => Promise<void> } | null>(null)
const pendingTrackingPanelRef = ref<{ refresh: () => Promise<void> } | null>(null)

async function fetchAll() {
  summaryLoading.value = true
  taskLoading.value = true
  try {
    const [summaryRes, batchRes, msgRes] = await Promise.all([
      getDashboardSummaryApi(selectedStatYear.value, selectedDepartmentIds.value),
      getDashboardBatchesApi(selectedDepartmentIds.value),
      getDashboardMessageStatsApi(selectedDepartmentIds.value)
    ])
    summary.value = { ...summary.value, ...(summaryRes.data || {}) }
    batches.value = batchRes.data || []
    messageStats.value = msgRes.data || messageStats.value
  } catch { /* handled globally */ } finally {
    summaryLoading.value = false
  }
  await Promise.all([
    fetchTaskStats(),
    reminderPanelRef.value?.refresh(),
    pendingTrackingPanelRef.value?.refresh()
  ])
}

async function fetchTaskStats() {
  taskLoading.value = true
  try {
    const { data } = await getDashboardTaskStatsApi(
      selectedBatch.value || undefined,
      selectedDepartmentIds.value
    )
    if (data) taskStats.value = data
  } catch { /* handled globally */ } finally {
    taskLoading.value = false
  }
}

onMounted(() => {
  fetchAll()
})

watch(selectedBatch, () => {
  fetchTaskStats()
})

watch(selectedStatYear, () => {
  fetchYearSummary()
})

watch(selectedDepartmentIds, () => {
  fetchAll()
}, { deep: true })

const managementYear = computed(() => summary.value.managementYear ?? getCurrentStatYear())

const selectedBatchLabel = computed(() => {
  if (!selectedBatch.value) return ""
  return batches.value.find(b => b.value === selectedBatch.value)?.label || selectedBatch.value
})

// ===== 统计卡片配置 =====
const statCards = [
  { label: "待追踪人数", key: "pendingTracking" as const, color: "#f56c6c", icon: Search, bg: "#fff5f5" },
  { label: "年度管理患者数", key: "pendingVisit" as const, color: "#e6a23c", icon: FirstAidKit, bg: "#fffbf0" },
  { label: "待确认通知单", key: "pendingNotice" as const, color: "#409eff", icon: Bell, bg: "#f0f7ff" },
  { label: "近期复查(15天)", key: "upcomingReview" as const, color: "#67c23a", icon: Calendar, bg: "#f0fff4" }
]

function getStatCardLabel(key: string, label: string) {
  if (key === "pendingVisit") {
    return `${managementYear.value}年度管理患者数`
  }
  return label
}

function onStatCardClick(key: string) {
  if (key !== "pendingTracking") return
  pendingTrackingExpanded.value = !pendingTrackingExpanded.value
}

const pathogenPositiveRateText = computed(() => {
  const rate = summary.value.pathogenPositiveRate
  return rate == null ? "—" : `${rate.toFixed(1)}%`
})

const drugResistanceScreeningRateText = computed(() => {
  const rate = summary.value.drugResistanceScreeningRate
  return rate == null ? "—" : `${rate.toFixed(1)}%`
})

const treatmentSuccessRateText = computed(() => {
  const rate = summary.value.treatmentSuccessRate
  return rate == null ? "—" : `${rate.toFixed(1)}%`
})

const recommendArrivalRateText = computed(() => {
  const rate = summary.value.recommendArrivalRate
  return rate == null ? "—" : `${rate.toFixed(1)}%`
})

const trackingStatYear = computed(() => summary.value.trackingStatYear ?? getCurrentStatYear())

const trackingArrivalRateText = computed(() => {
  const rate = summary.value.trackingArrivalRate
  return rate == null ? "—" : `${rate.toFixed(1)}%`
})

const trackingPeriodText = computed(() => {
  const { statPeriodFrom, statPeriodTo, trackingPeriodFrom, trackingPeriodTo } = summary.value
  const from = statPeriodFrom || trackingPeriodFrom
  const to = statPeriodTo || trackingPeriodTo
  if (!from || !to) return ""
  return `统计周期：${from} 至 ${to}`
})

// ===== 人群卡片配置 =====
const popCards = [
  { label: "学校人群", key: "school", color: "#409eff", darkColor: "#1a6fc4", icon: School, latentColor: "#f56c6c", patientColor: "#e6a23c" },
  { label: "重点人群", key: "keyPopulation", color: "#67c23a", darkColor: "#2e8b2e", icon: User, latentColor: "#f56c6c", patientColor: "#e6a23c" },
  { label: "密接人群", key: "closeContact", color: "#f56c6c", darkColor: "#c0392b", icon: Connection, latentColor: "#9b59b6", patientColor: "#e6a23c" }
] as const

function getStat(key: keyof TaskStatsData): PopulationStat {
  return taskStats.value[key]
}

/** 格式化比例显示（1位小数） */
function fmt(n: number) {
  return n.toFixed(1)
}

/** 计算通知单/转诊进度条宽度 */
function barWidth(val: number, total: number) {
  if (!total) return "0%"
  return `${Math.min((val / total) * 100, 100).toFixed(1)}%`
}

const noticeMaxSent = computed(() =>
  Math.max(
    messageStats.value.latentNoticeSent,
    messageStats.value.patientNoticeSent,
    1
  )
)
</script>

<template>
  <div class="dashboard-wrap">
    <!-- ===== 顶部 Header ===== -->
    <div class="db-header">
      <div class="db-header-left">
        <div class="db-title">
          疾病监控工作台
        </div>
        <div class="db-subtitle">
          欢迎使用结核病筛查追踪管理系统
        </div>
      </div>
      <div class="db-header-right">
        <ScopedDepartmentMultiSelect v-model="selectedDepartmentIds" width="240px" />
        <el-select
          v-model="selectedStatYear"
          placeholder="统计年度"
          class="year-select"
        >
          <el-option v-for="y in yearOptions" :key="y" :label="`${y}年度`" :value="y" />
        </el-select>
        <el-select
          v-model="selectedBatch"
          placeholder="全部任务（不限批次）"
          clearable
          class="batch-select"
          :prefix-icon="List"
        >
          <el-option v-for="b in batches" :key="b.value" :label="b.label" :value="b.value" />
        </el-select>
        <el-button :icon="Refresh" circle :loading="taskLoading" @click="fetchAll" />
      </div>
    </div>

    <!-- ===== 待处理事项卡片 ===== -->
    <div class="section-label">
      <span class="label-bar" />待处理事项
    </div>
    <el-row :gutter="20" v-loading="summaryLoading" class="stat-row">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="12" :md="6">
        <div
          class="stat-card"
          :class="{
            clickable: card.key === 'pendingTracking',
            active: card.key === 'pendingTracking' && pendingTrackingExpanded,
          }"
          :style="{ '--card-color': card.color, 'backgroundColor': card.bg }"
          @click="onStatCardClick(card.key)"
        >
          <div class="stat-icon-wrap">
            <el-icon :size="24" :style="{ color: card.color }">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-num">
              {{ summary[card.key] ?? "—" }}
            </div>
            <div class="stat-label">
              {{ getStatCardLabel(card.key, card.label) }}
            </div>
          </div>
          <div class="stat-deco" :style="{ borderColor: card.color }" />
        </div>
      </el-col>
    </el-row>

    <PendingTrackingPanel
      ref="pendingTrackingPanelRef"
      :expanded="pendingTrackingExpanded"
      :department-ids="selectedDepartmentIds"
    />

    <UpcomingVisitSupervisionPanel ref="reminderPanelRef" :department-ids="selectedDepartmentIds" />

    <div v-loading="summaryLoading || yearStatsLoading" class="year-stats-section">
      <div v-if="trackingPeriodText" class="year-stats-period">
        {{ trackingPeriodText }}
      </div>

      <div class="pathogen-panel">
        <div class="pathogen-title">
          {{ managementYear }}年度病原学阳性率 / 耐药筛查率
        </div>
        <div class="pathogen-content">
          <span>病原学阳性人数：<strong>{{ summary.pathogenPositiveCount ?? 0 }}</strong> 例</span>
          <span class="pathogen-divider">|</span>
          <span>病原学阳性率：<strong>{{ pathogenPositiveRateText }}</strong></span>
          <span class="pathogen-divider">|</span>
          <span>耐药筛查人数：<strong>{{ summary.drugResistanceScreenedCount ?? 0 }}</strong> 例</span>
          <span class="pathogen-divider">|</span>
          <span>耐药筛查率：<strong>{{ drugResistanceScreeningRateText }}</strong></span>
        </div>
      </div>

      <div class="treatment-panel">
        <div class="treatment-title">
          {{ managementYear }}年度治疗情况
        </div>
        <div class="treatment-content">
          <span>{{ managementYear }}年度治疗成功人数：<strong>{{ summary.treatmentSuccessCount ?? 0 }}</strong> 例</span>
          <span class="treatment-divider">|</span>
          <span>治疗成功率：<strong>{{ treatmentSuccessRateText }}</strong></span>
        </div>
      </div>

      <div class="referral-panel">
        <div class="referral-title">
          {{ managementYear }}年度推介情况
        </div>
        <div class="referral-content">
          <span>推介人数：<strong>{{ summary.recommendCount ?? 0 }}</strong> 例</span>
          <span class="referral-divider">|</span>
          <span>到位人数：<strong>{{ summary.recommendArrivedCount ?? 0 }}</strong> 例</span>
          <span class="referral-divider">|</span>
          <span>推介到位率：<strong>{{ recommendArrivalRateText }}</strong></span>
        </div>
      </div>

      <div class="tracking-panel">
        <div class="tracking-title">
          {{ trackingStatYear }}年度追踪情况
        </div>
        <div class="tracking-content">
          <span>追踪人数：<strong>{{ summary.trackingCount ?? 0 }}</strong> 例</span>
          <span class="tracking-divider">|</span>
          <span>到位人数：<strong>{{ summary.trackingArrivedCount ?? 0 }}</strong> 例</span>
          <span class="tracking-divider">|</span>
          <span>追踪到位率：<strong>{{ trackingArrivalRateText }}</strong></span>
        </div>
      </div>
    </div>

    <!-- ===== 三类人群数据 ===== -->
    <div class="section-label" style="margin-top: 32px">
      <span class="label-bar" />人群筛查数据统计
      <span v-if="selectedBatch" class="batch-tag">{{ selectedBatchLabel }}</span>
      <span v-else class="batch-hint">全部任务汇总</span>
    </div>
    <el-row :gutter="20" v-loading="taskLoading" class="pop-row">
      <el-col v-for="pc in popCards" :key="pc.key" :xs="24" :sm="8">
        <div class="pop-card">
          <!-- 卡片顶部渐变 -->
          <div class="pop-card-top" :style="{ background: `linear-gradient(135deg, ${pc.color}, ${pc.darkColor})` }">
            <div class="pop-top-left">
              <el-icon :size="28" style="color: rgba(255,255,255,0.9)">
                <component :is="pc.icon" />
              </el-icon>
              <span class="pop-name">{{ pc.label }}</span>
            </div>
            <div class="pop-total-wrap">
              <div class="pop-total-num">
                {{ getStat(pc.key).screeningTotal }}
              </div>
              <div class="pop-total-label">
                筛查总数
              </div>
            </div>
          </div>

          <!-- 卡片内容区 -->
          <div class="pop-card-body">
            <!-- 潜伏感染者 -->
            <div class="pop-item">
              <div class="pop-item-header">
                <span class="pop-item-dot" :style="{ backgroundColor: pc.latentColor }" />
                <span class="pop-item-name">潜伏感染者</span>
                <span class="pop-item-count" :style="{ color: pc.latentColor }">
                  {{ getStat(pc.key).latentCount }}人
                </span>
                <span class="pop-item-ratio">{{ fmt(getStat(pc.key).latentRatio) }}%</span>
              </div>
              <div class="pop-bar-bg">
                <div
                  class="pop-bar-fill"
                  :style="{
                    width: `${Math.min(getStat(pc.key).latentRatio, 100)}%`,
                    backgroundColor: pc.latentColor,
                  }"
                />
              </div>
            </div>

            <!-- 确诊患者 -->
            <div class="pop-item" style="margin-top: 18px">
              <div class="pop-item-header">
                <span class="pop-item-dot" :style="{ backgroundColor: pc.patientColor }" />
                <span class="pop-item-name">确诊患者</span>
                <span class="pop-item-count" :style="{ color: pc.patientColor }">
                  {{ getStat(pc.key).patientCount }}人
                </span>
                <span class="pop-item-ratio">{{ fmt(getStat(pc.key).patientRatio) }}%</span>
              </div>
              <div class="pop-bar-bg">
                <div
                  class="pop-bar-fill"
                  :style="{
                    width: `${Math.min(getStat(pc.key).patientRatio, 100)}%`,
                    backgroundColor: pc.patientColor,
                  }"
                />
              </div>
            </div>

            <!-- 进度环图 -->
            <div class="pop-progress-row">
              <el-progress
                type="circle"
                :percentage="Number(fmt(getStat(pc.key).latentRatio))"
                :width="72"
                :stroke-width="6"
                :color="pc.latentColor"
              >
                <template #default="{ percentage }">
                  <span class="ring-inner-text">{{ percentage }}%</span>
                  <span class="ring-inner-label">潜伏率</span>
                </template>
              </el-progress>
              <el-progress
                type="circle"
                :percentage="Number(fmt(getStat(pc.key).patientRatio))"
                :width="72"
                :stroke-width="6"
                :color="pc.patientColor"
              >
                <template #default="{ percentage }">
                  <span class="ring-inner-text">{{ percentage }}%</span>
                  <span class="ring-inner-label">患病率</span>
                </template>
              </el-progress>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ===== 消息通知统计 ===== -->
    <div class="section-label" style="margin-top: 32px">
      <span class="label-bar" />消息通知统计
    </div>
    <el-row :gutter="20" class="msg-row">
      <!-- 通知单统计 -->
      <el-col :xs="24" :sm="12">
        <div class="msg-card">
          <div class="msg-card-title">
            <el-icon><Bell /></el-icon>通知单发送与接收
          </div>
          <div class="msg-table">
            <!-- 表头 -->
            <div class="msg-table-head">
              <span class="msg-col-name" />
              <span class="msg-col-bar" />
              <span class="msg-col-num">发送</span>
              <span class="msg-col-num">已确认</span>
              <span class="msg-col-num">确认率</span>
            </div>

            <!-- 潜伏通知单 -->
            <div class="msg-table-row">
              <span class="msg-col-name msg-label">
                <el-icon style="color: #9b59b6"><DataAnalysis /></el-icon>
                潜伏通知单
              </span>
              <span class="msg-col-bar">
                <div class="msg-bar-bg">
                  <div
                    class="msg-bar-fill"
                    :style="{ width: barWidth(messageStats.latentNoticeSent, noticeMaxSent), background: '#9b59b6' }"
                  />
                </div>
              </span>
              <span class="msg-col-num msg-num">{{ messageStats.latentNoticeSent }}</span>
              <span class="msg-col-num msg-confirmed">{{ messageStats.latentNoticeConfirmed }}</span>
              <span class="msg-col-num msg-ratio">
                {{ messageStats.latentNoticeSent > 0
                  ? fmt(messageStats.latentNoticeConfirmed / messageStats.latentNoticeSent * 100)
                  : "0.0" }}%
              </span>
            </div>

            <!-- 患者通知单 -->
            <div class="msg-table-row">
              <span class="msg-col-name msg-label">
                <el-icon style="color: #e6a23c"><FirstAidKit /></el-icon>
                患者通知单
              </span>
              <span class="msg-col-bar">
                <div class="msg-bar-bg">
                  <div
                    class="msg-bar-fill"
                    :style="{ width: barWidth(messageStats.patientNoticeSent, noticeMaxSent), background: '#e6a23c' }"
                  />
                </div>
              </span>
              <span class="msg-col-num msg-num">{{ messageStats.patientNoticeSent }}</span>
              <span class="msg-col-num msg-confirmed">{{ messageStats.patientNoticeConfirmed }}</span>
              <span class="msg-col-num msg-ratio">
                {{ messageStats.patientNoticeSent > 0
                  ? fmt(messageStats.patientNoticeConfirmed / messageStats.patientNoticeSent * 100)
                  : "0.0" }}%
              </span>
            </div>
          </div>

          <!-- 汇总 -->
          <div class="msg-summary">
            <div class="msg-sum-item">
              <span class="msg-sum-num">{{ messageStats.latentNoticeSent + messageStats.patientNoticeSent }}</span>
              <span class="msg-sum-label">总发送</span>
            </div>
            <div class="msg-sum-divider" />
            <div class="msg-sum-item">
              <span class="msg-sum-num success">{{ messageStats.latentNoticeConfirmed + messageStats.patientNoticeConfirmed }}</span>
              <span class="msg-sum-label">已确认</span>
            </div>
            <div class="msg-sum-divider" />
            <div class="msg-sum-item">
              <span class="msg-sum-num warn">
                {{ (messageStats.latentNoticeSent + messageStats.patientNoticeSent)
                  - (messageStats.latentNoticeConfirmed + messageStats.patientNoticeConfirmed) }}
              </span>
              <span class="msg-sum-label">待确认</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 转诊统计 -->
      <el-col :xs="24" :sm="12">
        <div class="msg-card">
          <div class="msg-card-title">
            <el-icon><Connection /></el-icon>转诊发送情况
          </div>

          <div class="referral-stats">
            <div class="ref-stat-item">
              <div
                class="ref-circle"
                style="border-color: #409eff; color: #409eff; background: #f0f7ff"
              >
                {{ messageStats.referralSent }}
              </div>
              <div class="ref-label">
                总发送
              </div>
            </div>
            <div class="ref-arrow">
              →
            </div>
            <div class="ref-stat-item">
              <div
                class="ref-circle"
                style="border-color: #67c23a; color: #67c23a; background: #f0fff4"
              >
                {{ messageStats.referralConfirmed }}
              </div>
              <div class="ref-label">
                已接收
              </div>
            </div>
            <div class="ref-arrow">
              ·
            </div>
            <div class="ref-stat-item">
              <div
                class="ref-circle"
                style="border-color: #f56c6c; color: #f56c6c; background: #fff5f5"
              >
                {{ messageStats.referralRejected }}
              </div>
              <div class="ref-label">
                已拒绝
              </div>
            </div>
          </div>

          <!-- 接收率进度条 -->
          <div class="ref-progress-section">
            <div class="ref-progress-label">
              <span>接收率</span>
              <span class="ref-progress-val">
                {{ messageStats.referralSent > 0
                  ? fmt(messageStats.referralConfirmed / messageStats.referralSent * 100)
                  : "0.0" }}%
              </span>
            </div>
            <el-progress
              :percentage="messageStats.referralSent > 0
                ? Number(fmt(messageStats.referralConfirmed / messageStats.referralSent * 100))
                : 0"
              :color="[{ color: '#67c23a', percentage: 60 }, { color: '#e6a23c', percentage: 80 }, { color: '#409eff', percentage: 100 }]"
              :stroke-width="10"
              :show-text="false"
            />
            <div class="ref-progress-label" style="margin-top: 12px">
              <span>拒绝率</span>
              <span class="ref-progress-val warn">
                {{ messageStats.referralSent > 0
                  ? fmt(messageStats.referralRejected / messageStats.referralSent * 100)
                  : "0.0" }}%
              </span>
            </div>
            <el-progress
              :percentage="messageStats.referralSent > 0
                ? Number(fmt(messageStats.referralRejected / messageStats.referralSent * 100))
                : 0"
              color="#f56c6c"
              :stroke-width="10"
              :show-text="false"
            />
          </div>

          <!-- 待确认提示 -->
          <div v-if="messageStats.referralSent - messageStats.referralConfirmed - messageStats.referralRejected > 0" class="ref-pending">
            <el-icon><WarningFilled /></el-icon>
            <span>
              {{ messageStats.referralSent - messageStats.referralConfirmed - messageStats.referralRejected }}
              条转诊待对方确认
            </span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.dashboard-wrap {
  padding: 28px 32px;
  min-height: 100%;
  background: var(--el-bg-color-page);
}

// ===== Header =====
.db-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 28px;
  color: #fff;
  box-shadow: 0 4px 20px rgba(26, 115, 232, 0.3);
}

.db-header-left {
  .db-title {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 1px;
  }
  .db-subtitle {
    font-size: 13px;
    margin-top: 6px;
    opacity: 0.85;
  }
}

.db-header-right {
  display: flex;
  align-items: center;
  gap: 12px;

  .year-select,
  .batch-select {
    width: 140px;

    :deep(.el-input__wrapper) {
      background: rgba(255, 255, 255, 0.15);
      box-shadow: none;
      border: 1px solid rgba(255, 255, 255, 0.35);
    }
    :deep(.el-input__inner) {
      color: #fff;
    }
    :deep(.el-input__inner::placeholder) {
      color: rgba(255, 255, 255, 0.7);
    }
    :deep(.el-select__caret),
    :deep(.el-input__prefix-inner) {
      color: rgba(255, 255, 255, 0.8);
    }
  }

  .batch-select {
    width: 280px;
  }
}

// ===== Section Label =====
.section-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;

  .label-bar {
    display: inline-block;
    width: 4px;
    height: 16px;
    background: var(--el-color-primary);
    border-radius: 2px;
  }

  .batch-tag {
    font-size: 12px;
    font-weight: 400;
    background: var(--el-color-primary-light-8);
    color: var(--el-color-primary);
    padding: 2px 10px;
    border-radius: 10px;
  }

  .batch-hint {
    font-size: 12px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
  }
}

// ===== Stat Cards =====
.stat-row {
  margin-bottom: 0;
}

.year-stats-section {
  margin: 4px 0 28px;
}

.year-stats-period {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--el-fill-color-extra-light);
  border-radius: 8px;
}

.stat-card {
  border-radius: 14px;
  padding: 22px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  border: 2px solid transparent;
  transition:
    transform 0.2s,
    box-shadow 0.2s,
    border-color 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  }

  &.clickable {
    cursor: pointer;
    user-select: none;
  }

  &.active {
    border-color: var(--card-color);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  }

  .stat-icon-wrap {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .stat-body {
    .stat-num {
      font-size: 32px;
      font-weight: 700;
      color: var(--card-color);
      line-height: 1;
    }
    .stat-label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      margin-top: 6px;
      white-space: nowrap;
    }
  }

  .stat-deco {
    position: absolute;
    right: -10px;
    bottom: -10px;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    border: 10px solid;
    opacity: 0.12;
  }
}

.pathogen-panel {
  margin: 4px 0 28px;
  padding: 18px 24px;
  border-radius: 12px;
  background: #fffbf0;
  border: 1px solid #f5dab1;
  border-left: 4px solid #e6a23c;

  .pathogen-title {
    font-size: 15px;
    font-weight: 600;
    color: #e6a23c;
    margin-bottom: 10px;
  }

  .pathogen-content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 16px;
    font-size: 14px;
    color: var(--el-text-color-regular);

    strong {
      color: #e6a23c;
      font-size: 18px;
      font-weight: 700;
    }
  }

  .pathogen-divider {
    color: var(--el-text-color-placeholder);
  }
}

.treatment-panel {
  margin: 4px 0 16px;
  padding: 18px 24px;
  border-radius: 12px;
  background: #fff5f5;
  border: 1px solid #fbc4c4;
  border-left: 4px solid #f56c6c;

  .treatment-title {
    font-size: 15px;
    font-weight: 600;
    color: #f56c6c;
    margin-bottom: 10px;
  }

  .treatment-content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 16px;
    font-size: 14px;
    color: var(--el-text-color-regular);

    strong {
      color: #f56c6c;
      font-size: 18px;
      font-weight: 700;
    }
  }

  .treatment-divider {
    color: var(--el-text-color-placeholder);
  }
}

.referral-panel {
  margin: 4px 0 28px;
  padding: 18px 24px;
  border-radius: 12px;
  background: #f0f7ff;
  border: 1px solid #b3d8ff;
  border-left: 4px solid #409eff;

  .referral-title {
    font-size: 15px;
    font-weight: 600;
    color: #409eff;
    margin-bottom: 10px;
  }

  .referral-content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 16px;
    font-size: 14px;
    color: var(--el-text-color-regular);

    strong {
      color: #409eff;
      font-size: 18px;
      font-weight: 700;
    }
  }

  .referral-divider {
    color: var(--el-text-color-placeholder);
  }
}

.tracking-panel {
  margin: 4px 0 28px;
  padding: 18px 24px;
  border-radius: 12px;
  background: #f0fff4;
  border: 1px solid #b3e19d;
  border-left: 4px solid #67c23a;

  .tracking-title {
    font-size: 15px;
    font-weight: 600;
    color: #67c23a;
    margin-bottom: 6px;
  }

  .tracking-period {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-bottom: 10px;
  }

  .tracking-content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 16px;
    font-size: 14px;
    color: var(--el-text-color-regular);

    strong {
      color: #67c23a;
      font-size: 18px;
      font-weight: 700;
    }
  }

  .tracking-divider {
    color: var(--el-text-color-placeholder);
  }
}

// ===== Population Cards =====
.pop-row {
  margin-bottom: 0;
}

.pop-card {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.07);
  margin-bottom: 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  transition:
    transform 0.2s,
    box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }
}

.pop-card-top {
  padding: 20px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pop-top-left {
  display: flex;
  align-items: center;
  gap: 10px;

  .pop-name {
    font-size: 16px;
    font-weight: 700;
    color: #fff;
    letter-spacing: 0.5px;
  }
}

.pop-total-wrap {
  text-align: right;

  .pop-total-num {
    font-size: 34px;
    font-weight: 800;
    color: rgba(255, 255, 255, 0.95);
    line-height: 1;
  }
  .pop-total-label {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.75);
    margin-top: 4px;
    text-align: center;
  }
}

.pop-card-body {
  padding: 20px 22px 16px;
}

.pop-item {
  .pop-item-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .pop-item-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .pop-item-name {
      font-size: 13px;
      color: var(--el-text-color-regular);
      flex: 1;
    }
    .pop-item-count {
      font-size: 14px;
      font-weight: 600;
    }
    .pop-item-ratio {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      min-width: 42px;
      text-align: right;
    }
  }
}

.pop-bar-bg {
  height: 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  overflow: hidden;
}

.pop-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.pop-progress-row {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.ring-inner-text {
  display: block;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}
.ring-inner-label {
  display: block;
  font-size: 10px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

// ===== Message Cards =====
.msg-row {
  margin-bottom: 0;
}

.msg-card {
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 22px 24px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.07);
  margin-bottom: 20px;
  border: 1px solid var(--el-border-color-lighter);
  min-height: 280px;
}

.msg-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

// 通知单表格
.msg-table {
  .msg-table-head {
    display: flex;
    align-items: center;
    padding: 0 0 8px;
    border-bottom: 1px dashed var(--el-border-color-lighter);
    margin-bottom: 14px;
  }

  .msg-table-row {
    display: flex;
    align-items: center;
    margin-bottom: 14px;
  }

  .msg-col-name {
    width: 110px;
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    font-weight: 600;
  }

  .msg-col-bar {
    flex: 1;
    padding: 0 12px;
  }

  .msg-col-num {
    width: 54px;
    text-align: center;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    font-weight: 600;
    flex-shrink: 0;
  }

  .msg-label {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--el-text-color-regular);
    font-size: 13px;
  }

  .msg-num {
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 700;
  }
  .msg-confirmed {
    color: #67c23a;
    font-size: 14px;
    font-weight: 700;
  }
  .msg-ratio {
    color: var(--el-color-primary);
    font-size: 13px;
  }
}

.msg-bar-bg {
  height: 10px;
  background: var(--el-fill-color-light);
  border-radius: 5px;
  overflow: hidden;
}
.msg-bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.85;
}

.msg-summary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
  padding: 16px;
  background: var(--el-fill-color-extra-light);
  border-radius: 10px;
}

.msg-sum-item {
  text-align: center;
  .msg-sum-num {
    display: block;
    font-size: 24px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    &.success {
      color: #67c23a;
    }
    &.warn {
      color: #e6a23c;
    }
  }
  .msg-sum-label {
    display: block;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }
}

.msg-sum-divider {
  width: 1px;
  height: 36px;
  background: var(--el-border-color-lighter);
}

// 转诊
.referral-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 0 20px;
}

.ref-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.ref-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 2px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}

.ref-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.ref-arrow {
  font-size: 20px;
  color: var(--el-text-color-placeholder);
  margin-bottom: 20px;
}

.ref-progress-section {
  border-top: 1px dashed var(--el-border-color-lighter);
  padding-top: 16px;
}

.ref-progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 8px;

  .ref-progress-val {
    font-weight: 600;
    color: var(--el-color-primary);
    &.warn {
      color: #f56c6c;
    }
  }
}

.ref-pending {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  padding: 8px 12px;
  background: #fff8e1;
  border-radius: 8px;
  font-size: 13px;
  color: #e6a23c;

  .el-icon {
    font-size: 14px;
  }
}
</style>
