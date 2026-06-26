<script lang="ts" setup>
import type { DashboardSummaryData } from "../apis"
import { DASHBOARD_EDITOR_WELCOME } from "@@/constants/app"
import { buildStatYearOptions, getCurrentStatYear } from "@@/utils/stat-year"
import { Bell, Calendar, FirstAidKit, Search } from "@element-plus/icons-vue"
import { getDashboardSummaryApi } from "../apis"

const selectedStatYear = ref(String(getCurrentStatYear()))
const yearOptions = buildStatYearOptions()
const summaryLoading = ref(false)
const summary = ref<DashboardSummaryData>({
  pendingTracking: 0,
  pendingVisit: 0,
  pendingNotice: 0,
  upcomingReview: 0
})

async function fetchSummary() {
  summaryLoading.value = true
  try {
    const { data } = await getDashboardSummaryApi(selectedStatYear.value)
    summary.value = { ...summary.value, ...(data || {}) }
  } catch { /* handled */ } finally {
    summaryLoading.value = false
  }
}

onMounted(() => {
  fetchSummary()
})

watch(selectedStatYear, () => {
  fetchSummary()
})

const cards = [
  { label: "待追踪人数", key: "pendingTracking" as const, color: "#f56c6c", icon: Search },
  { label: "年度管理患者数", key: "pendingVisit" as const, color: "#e6a23c", icon: FirstAidKit },
  { label: "待确认通知单", key: "pendingNotice" as const, color: "#409eff", icon: Bell },
  { label: "复查（15天内）", key: "upcomingReview" as const, color: "#67c23a", icon: Calendar }
]

const managementYear = computed(() => summary.value.managementYear ?? getCurrentStatYear())

function getCardLabel(key: string, label: string) {
  if (key === "pendingVisit") {
    return `${managementYear.value}年度管理患者数`
  }
  return label
}

const pathogenPositiveRateText = computed(() => {
  const rate = summary.value.pathogenPositiveRate
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

function alphaColor(hex: string, alpha = "20") {
  return hex + alpha
}
</script>

<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div class="dashboard-header-left">
        <div class="dashboard-title">
          我的工作台
        </div>
        <div class="dashboard-subtitle">
          {{ DASHBOARD_EDITOR_WELCOME }}
        </div>
      </div>
      <el-select
        v-model="selectedStatYear"
        placeholder="统计年度"
        class="year-select"
      >
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年度`" :value="y" />
      </el-select>
    </div>

    <div v-loading="summaryLoading" class="year-stats-section">
      <div v-if="trackingPeriodText" class="year-stats-period">
        {{ trackingPeriodText }}
      </div>

      <el-row :gutter="24">
        <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="12" :md="6">
          <div class="stat-card" :style="{ borderTopColor: card.color }">
            <div class="stat-icon-wrap" :style="{ backgroundColor: alphaColor(card.color) }">
              <el-icon :size="22" :style="{ color: card.color }">
                <component :is="card.icon" />
              </el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-num" :style="{ color: card.color }">
                {{ summary[card.key] ?? "—" }}
              </div>
              <div class="stat-label">
                {{ getCardLabel(card.key, card.label) }}
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="pathogen-panel">
        <div class="pathogen-title">
          {{ managementYear }}年度病原学阳性情况
        </div>
        <div class="pathogen-content">
          <span>{{ managementYear }}年度病原学阳性人数：<strong>{{ summary.pathogenPositiveCount ?? 0 }}</strong> 例</span>
          <span class="pathogen-divider">|</span>
          <span>病原学阳性率：<strong>{{ pathogenPositiveRateText }}</strong></span>
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
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  padding: 40px 48px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 36px;
}

.dashboard-header-left {
  flex: 1;
}

.year-select {
  width: 140px;
  flex-shrink: 0;
}

.year-stats-section {
  margin-bottom: 24px;
}

.year-stats-period {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--el-fill-color-extra-light);
  border-radius: 8px;
}

.dashboard-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.dashboard-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}

.stat-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  border-top: 3px solid transparent;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: var(--el-box-shadow-light);
  margin-bottom: 24px;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: var(--el-box-shadow);
  }

  .stat-icon-wrap {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-body {
    .stat-num {
      font-size: 34px;
      font-weight: 700;
      line-height: 1;
    }
    .stat-label {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      margin-top: 6px;
      white-space: nowrap;
    }
  }
}

.pathogen-panel {
  margin: 0 0 8px;
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

  .stat-period {
    font-size: 12px;
    color: var(--el-text-color-secondary);
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
  margin: 0 0 8px;
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
  margin: 0 0 8px;
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
  margin: 0 0 8px;
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
</style>
