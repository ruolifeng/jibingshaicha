<script lang="ts" setup>
import type { DashboardSummaryData } from "../apis"
import ScopedDepartmentMultiSelect from "@@/components/ScopedDepartmentMultiSelect.vue"
import { DASHBOARD_ADMIN_TITLE, DASHBOARD_EDITOR_WELCOME } from "@@/constants/app"
import { buildStatYearOptions, getCurrentStatYear } from "@@/utils/stat-year"
import { Bell, Calendar, FirstAidKit, Refresh, Search } from "@element-plus/icons-vue"
import { getDashboardSummaryApi } from "../apis"

const selectedStatYear = ref(String(getCurrentStatYear()))
const selectedDepartmentIds = ref<string[]>([])
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
    const { data } = await getDashboardSummaryApi(selectedStatYear.value, selectedDepartmentIds.value)
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

watch(selectedDepartmentIds, () => {
  fetchSummary()
}, { deep: true })

const statCards = [
  { label: "待追踪人数", key: "pendingTracking" as const, color: "#f56c6c", icon: Search, bg: "#fff5f5" },
  { label: "年度管理患者数", key: "pendingVisit" as const, color: "#e6a23c", icon: FirstAidKit, bg: "#fffbf0" },
  { label: "待确认通知单", key: "pendingNotice" as const, color: "#409eff", icon: Bell, bg: "#f0f7ff" },
  { label: "近期复查(15天)", key: "upcomingReview" as const, color: "#67c23a", icon: Calendar, bg: "#f0fff4" }
]

const managementYear = computed(() => summary.value.managementYear ?? getCurrentStatYear())

function getStatCardLabel(key: string, label: string) {
  if (key === "pendingVisit") {
    return `${managementYear.value}年度管理患者数`
  }
  return label
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
</script>

<template>
  <div class="dashboard-wrap">
    <div class="db-header">
      <div class="db-header-left">
        <div class="db-title">
          {{ DASHBOARD_ADMIN_TITLE }}
        </div>
        <div class="db-subtitle">
          {{ DASHBOARD_EDITOR_WELCOME }}
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
        <el-button :icon="Refresh" circle :loading="summaryLoading" @click="fetchSummary" />
      </div>
    </div>

    <div class="section-label">
      <span class="label-bar" />待处理事项
    </div>
    <el-row :gutter="20" v-loading="summaryLoading" class="stat-row">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="12" :md="6">
        <div class="stat-card" :style="{ '--card-color': card.color, 'backgroundColor': card.bg }">
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

    <div v-loading="summaryLoading" class="year-stats-section">
      <div v-if="trackingPeriodText" class="year-stats-period">
        {{ trackingPeriodText }}
      </div>

      <div class="pathogen-panel">
        <div class="pathogen-title">
          {{ managementYear }}年度发病率 / 耐药筛查率
        </div>
        <div class="pathogen-content">
          <span>发病率分子人数：<strong>{{ summary.pathogenPositiveCount ?? 0 }}</strong> 例</span>
          <span class="pathogen-divider">|</span>
          <span>发病率：<strong>{{ pathogenPositiveRateText }}</strong></span>
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
  </div>
</template>

<style lang="scss" scoped>
.dashboard-wrap {
  padding: 28px 32px;
  min-height: 100%;
  background: var(--el-bg-color-page);
}

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

  .year-select {
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
}

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
}

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
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition:
    transform 0.2s,
    box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
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
