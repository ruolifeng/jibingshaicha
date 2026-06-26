<script lang="ts" setup>
import type { PatientHeatmapData } from "../apis"
import type { DashboardSummaryData } from "@/pages/dashboard/apis"
import { getCurrentStatYear } from "@@/utils/stat-year"
import PatientHeatmapChart from "./PatientHeatmapChart.vue"

const props = defineProps<{
  summary: DashboardSummaryData
  loading?: boolean
  showHeatmap?: boolean
  heatmap?: PatientHeatmapData
  heatmapLoading?: boolean
}>()

const emit = defineEmits<{
  heatmapDrill: [district: string | null]
}>()

const managementYear = computed(() => props.summary.managementYear ?? getCurrentStatYear())

const periodText = computed(() => {
  const { statPeriodFrom, statPeriodTo, trackingPeriodFrom, trackingPeriodTo } = props.summary
  const from = statPeriodFrom || trackingPeriodFrom
  const to = statPeriodTo || trackingPeriodTo
  if (!from || !to) return ""
  return `统计周期：${from} 至 ${to}（上年度 12 月 1 日 — 本年度 11 月 30 日）`
})

function rateText(rate?: number) {
  return rate == null ? "—" : `${rate.toFixed(1)}%`
}
</script>

<template>
  <div class="workbench-stats">
    <div v-loading="!!loading">
      <el-alert
        v-if="periodText"
        :title="periodText"
        type="info"
        :closable="false"
        show-icon
        class="period-alert"
      />

      <div class="stat-grid">
        <div class="stat-block primary">
          <div class="stat-value">
            {{ summary.pendingVisit ?? 0 }}
          </div>
          <div class="stat-label">
            {{ managementYear }}年度管理患者数
          </div>
        </div>
      </div>

      <div class="pathogen-panel">
        <div class="panel-title">
          {{ managementYear }}年度病原学阳性情况
        </div>
        <div class="panel-content">
          <span>{{ managementYear }}年度病原学阳性人数：<strong>{{ summary.pathogenPositiveCount ?? 0 }}</strong> 例</span>
          <span class="divider">|</span>
          <span>病原学阳性率：<strong>{{ rateText(summary.pathogenPositiveRate) }}</strong></span>
        </div>
      </div>

      <div class="treatment-panel">
        <div class="panel-title">
          {{ managementYear }}年度治疗情况
        </div>
        <div class="panel-content">
          <span>{{ managementYear }}年度治疗成功人数：<strong>{{ summary.treatmentSuccessCount ?? 0 }}</strong> 例</span>
          <span class="divider">|</span>
          <span>治疗成功率：<strong>{{ rateText(summary.treatmentSuccessRate) }}</strong></span>
        </div>
      </div>

      <div class="referral-panel">
        <div class="panel-title">
          {{ managementYear }}年度推介情况
        </div>
        <div class="panel-content">
          <span>推介人数：<strong>{{ summary.recommendCount ?? 0 }}</strong> 例</span>
          <span class="divider">|</span>
          <span>到位人数：<strong>{{ summary.recommendArrivedCount ?? 0 }}</strong> 例</span>
          <span class="divider">|</span>
          <span>推介到位率：<strong>{{ rateText(summary.recommendArrivalRate) }}</strong></span>
        </div>
      </div>

      <div class="tracking-panel">
        <div class="panel-title">
          {{ summary.trackingStatYear ?? managementYear }}年度追踪情况
        </div>
        <div class="panel-content">
          <span>追踪人数：<strong>{{ summary.trackingCount ?? 0 }}</strong> 例</span>
          <span class="divider">|</span>
          <span>到位人数：<strong>{{ summary.trackingArrivedCount ?? 0 }}</strong> 例</span>
          <span class="divider">|</span>
          <span>追踪到位率：<strong>{{ rateText(summary.trackingArrivalRate) }}</strong></span>
        </div>
      </div>
    </div>

    <PatientHeatmapChart
      v-if="showHeatmap"
      :heatmap="heatmap ?? {}"
      :loading="heatmapLoading"
      @drill="emit('heatmapDrill', $event)"
    />
  </div>
</template>

<style lang="scss" scoped>
.workbench-stats {
  .period-alert {
    margin-bottom: 16px;
  }

  .stat-grid {
    margin-bottom: 16px;
  }

  .stat-block {
    display: inline-block;
    padding: 20px 28px;
    border-radius: 12px;
    background: #fffbf0;
    border: 1px solid #f5dab1;
    border-left: 4px solid #e6a23c;

    .stat-value {
      font-size: 36px;
      font-weight: 700;
      color: #e6a23c;
      line-height: 1;
    }

    .stat-label {
      margin-top: 8px;
      font-size: 14px;
      color: var(--el-text-color-secondary);
    }
  }

  .pathogen-panel,
  .treatment-panel,
  .referral-panel,
  .tracking-panel {
    margin-bottom: 12px;
    padding: 18px 24px;
    border-radius: 12px;

    .panel-title {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 10px;
    }

    .panel-content {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px 16px;
      font-size: 14px;
      color: var(--el-text-color-regular);

      strong {
        font-size: 18px;
        font-weight: 700;
      }
    }

    .divider {
      color: var(--el-text-color-placeholder);
    }
  }

  .pathogen-panel {
    background: #fffbf0;
    border: 1px solid #f5dab1;
    border-left: 4px solid #e6a23c;

    .panel-title,
    .panel-content strong {
      color: #e6a23c;
    }
  }

  .treatment-panel {
    background: #fff5f5;
    border: 1px solid #fbc4c4;
    border-left: 4px solid #f56c6c;

    .panel-title,
    .panel-content strong {
      color: #f56c6c;
    }
  }

  .referral-panel {
    background: #f0f7ff;
    border: 1px solid #b3d8ff;
    border-left: 4px solid #409eff;

    .panel-title,
    .panel-content strong {
      color: #409eff;
    }
  }

  .tracking-panel {
    background: #f0fff4;
    border: 1px solid #b3e19d;
    border-left: 4px solid #67c23a;

    .panel-title,
    .panel-content strong {
      color: #67c23a;
    }
  }
}
</style>
