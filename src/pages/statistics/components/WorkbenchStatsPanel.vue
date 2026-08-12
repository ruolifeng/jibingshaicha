<script lang="ts" setup>
import type { PatientHeatmapData } from "../apis"
import type { DashboardSummaryData } from "@/pages/dashboard/apis"
import { buildStatYearOptions, getCurrentStatYear } from "@@/utils/stat-year"
import { getPatientHeatmapApi, getWorkbenchStatisticsApi } from "../apis"
import PatientHeatmapChart from "./PatientHeatmapChart.vue"

const props = defineProps<{
  showHeatmap?: boolean
  departmentIds?: string[]
}>()

const yearOptions = buildStatYearOptions()

type PanelKey = "managed" | "pathogen" | "treatment" | "referral" | "tracking" | "heatmap"

const panelYears = reactive<Record<Exclude<PanelKey, "heatmap">, string>>({
  managed: String(getCurrentStatYear()),
  pathogen: String(getCurrentStatYear()),
  treatment: String(getCurrentStatYear()),
  referral: String(getCurrentStatYear()),
  tracking: String(getCurrentStatYear())
})
const heatmapYear = ref(String(getCurrentStatYear()))

function emptySummary(): DashboardSummaryData {
  return {
    pendingTracking: 0,
    pendingVisit: 0,
    pendingNotice: 0,
    upcomingReview: 0
  }
}

const panelSummaries = reactive<Record<Exclude<PanelKey, "heatmap">, DashboardSummaryData>>({
  managed: emptySummary(),
  pathogen: emptySummary(),
  treatment: emptySummary(),
  referral: emptySummary(),
  tracking: emptySummary()
})
const panelLoading = reactive<Record<Exclude<PanelKey, "heatmap">, boolean>>({
  managed: false,
  pathogen: false,
  treatment: false,
  referral: false,
  tracking: false
})

const heatmapLoading = ref(false)
const heatmapData = ref<PatientHeatmapData>({})
const heatmapDistrict = ref<string>()

async function fetchPanel(key: Exclude<PanelKey, "heatmap">) {
  panelLoading[key] = true
  try {
    const { data } = await getWorkbenchStatisticsApi(panelYears[key], props.departmentIds)
    panelSummaries[key] = { ...emptySummary(), ...(data || {}) }
  } catch {
    panelSummaries[key] = emptySummary()
  } finally {
    panelLoading[key] = false
  }
}

async function fetchAllPanels() {
  await Promise.all([
    fetchPanel("managed"),
    fetchPanel("pathogen"),
    fetchPanel("treatment"),
    fetchPanel("referral"),
    fetchPanel("tracking")
  ])
}

async function fetchPatientHeatmap(district?: string) {
  if (!props.showHeatmap) return
  heatmapLoading.value = true
  try {
    const { data } = await getPatientHeatmapApi(heatmapYear.value, district, props.departmentIds)
    heatmapData.value = data || {}
    heatmapDistrict.value = district
  } catch {
    if (district) {
      ElMessage.warning("无法查看该区县，请确认您有相应辖区权限")
    }
    heatmapData.value = {}
  } finally {
    heatmapLoading.value = false
  }
}

function handleHeatmapDrill(district: string | null) {
  fetchPatientHeatmap(district || undefined)
}

watch(() => panelYears.managed, () => fetchPanel("managed"))
watch(() => panelYears.pathogen, () => fetchPanel("pathogen"))
watch(() => panelYears.treatment, () => fetchPanel("treatment"))
watch(() => panelYears.referral, () => fetchPanel("referral"))
watch(() => panelYears.tracking, () => fetchPanel("tracking"))
watch(heatmapYear, () => {
  heatmapDistrict.value = undefined
  fetchPatientHeatmap()
})

watch(() => props.departmentIds, () => {
  fetchAllPanels()
  heatmapDistrict.value = undefined
  fetchPatientHeatmap()
}, { deep: true })

function panelYear(summary: DashboardSummaryData, fallback: string) {
  return String(summary.managementYear ?? summary.trackingStatYear ?? fallback)
}

function trackingYear(summary: DashboardSummaryData) {
  return String(summary.trackingStatYear ?? panelYears.tracking)
}

function periodText(summary: DashboardSummaryData) {
  const { statPeriodFrom, statPeriodTo, trackingPeriodFrom, trackingPeriodTo } = summary
  const from = statPeriodFrom || trackingPeriodFrom
  const to = statPeriodTo || trackingPeriodTo
  if (!from || !to) return ""
  return `统计周期：${from} 至 ${to}`
}

function rateText(rate?: number) {
  return rate == null ? "—" : `${rate.toFixed(1)}%`
}

onMounted(() => {
  fetchAllPanels()
  fetchPatientHeatmap()
})

defineExpose({
  refresh: async () => {
    await fetchAllPanels()
    heatmapDistrict.value = undefined
    await fetchPatientHeatmap()
  }
})
</script>

<template>
  <div class="workbench-stats">
    <div class="stat-grid">
      <div v-loading="panelLoading.managed" class="stat-block primary">
        <div class="panel-header">
          <div class="stat-value">
            {{ panelSummaries.managed.pendingVisit ?? 0 }}
          </div>
          <el-select v-model="panelYears.managed" class="panel-year-select" size="small">
            <el-option v-for="y in yearOptions" :key="`managed-${y}`" :label="`${y}年度`" :value="y" />
          </el-select>
        </div>
        <div class="stat-label">
          {{ panelYear(panelSummaries.managed, panelYears.managed) }}年度管理患者数
        </div>
        <div v-if="periodText(panelSummaries.managed)" class="panel-period">
          {{ periodText(panelSummaries.managed) }}
        </div>
      </div>
    </div>

    <div v-loading="panelLoading.pathogen" class="pathogen-panel">
      <div class="panel-header">
        <div class="panel-title">
          {{ panelYear(panelSummaries.pathogen, panelYears.pathogen) }}年度发病率 / 耐药筛查率
        </div>
        <el-select v-model="panelYears.pathogen" class="panel-year-select" size="small">
          <el-option v-for="y in yearOptions" :key="`pathogen-${y}`" :label="`${y}年度`" :value="y" />
        </el-select>
      </div>
      <div class="panel-content">
        <span>发病率分子人数：<strong>{{ panelSummaries.pathogen.pathogenPositiveCount ?? 0 }}</strong> 例</span>
        <span class="divider">|</span>
        <span>发病率：<strong>{{ rateText(panelSummaries.pathogen.pathogenPositiveRate) }}</strong></span>
        <span class="divider">|</span>
        <span>耐药筛查人数：<strong>{{ panelSummaries.pathogen.drugResistanceScreenedCount ?? 0 }}</strong> 例</span>
        <span class="divider">|</span>
        <span>耐药筛查率：<strong>{{ rateText(panelSummaries.pathogen.drugResistanceScreeningRate) }}</strong></span>
      </div>
    </div>

    <div v-loading="panelLoading.treatment" class="treatment-panel">
      <div class="panel-header">
        <div class="panel-title">
          {{ panelYear(panelSummaries.treatment, panelYears.treatment) }}年度治疗情况
        </div>
        <el-select v-model="panelYears.treatment" class="panel-year-select" size="small">
          <el-option v-for="y in yearOptions" :key="`treatment-${y}`" :label="`${y}年度`" :value="y" />
        </el-select>
      </div>
      <div class="panel-content">
        <span>{{ panelYear(panelSummaries.treatment, panelYears.treatment) }}年度治疗成功人数：<strong>{{ panelSummaries.treatment.treatmentSuccessCount ?? 0 }}</strong> 例</span>
        <span class="divider">|</span>
        <span>治疗成功率：<strong>{{ rateText(panelSummaries.treatment.treatmentSuccessRate) }}</strong></span>
      </div>
    </div>

    <div v-loading="panelLoading.referral" class="referral-panel">
      <div class="panel-header">
        <div class="panel-title">
          {{ panelYear(panelSummaries.referral, panelYears.referral) }}年度推介情况
        </div>
        <el-select v-model="panelYears.referral" class="panel-year-select" size="small">
          <el-option v-for="y in yearOptions" :key="`referral-${y}`" :label="`${y}年度`" :value="y" />
        </el-select>
      </div>
      <div class="panel-content">
        <span>推介人数：<strong>{{ panelSummaries.referral.recommendCount ?? 0 }}</strong> 例</span>
        <span class="divider">|</span>
        <span>到位人数：<strong>{{ panelSummaries.referral.recommendArrivedCount ?? 0 }}</strong> 例</span>
        <span class="divider">|</span>
        <span>推介到位率：<strong>{{ rateText(panelSummaries.referral.recommendArrivalRate) }}</strong></span>
      </div>
    </div>

    <div v-loading="panelLoading.tracking" class="tracking-panel">
      <div class="panel-header">
        <div class="panel-title">
          {{ trackingYear(panelSummaries.tracking) }}年度追踪情况
        </div>
        <el-select v-model="panelYears.tracking" class="panel-year-select" size="small">
          <el-option v-for="y in yearOptions" :key="`tracking-${y}`" :label="`${y}年度`" :value="y" />
        </el-select>
      </div>
      <div v-if="periodText(panelSummaries.tracking)" class="panel-period">
        {{ periodText(panelSummaries.tracking) }}
      </div>
      <div class="panel-content">
        <span>追踪人数：<strong>{{ panelSummaries.tracking.trackingCount ?? 0 }}</strong> 例</span>
        <span class="divider">|</span>
        <span>到位人数：<strong>{{ panelSummaries.tracking.trackingArrivedCount ?? 0 }}</strong> 例</span>
        <span class="divider">|</span>
        <span>追踪到位率：<strong>{{ rateText(panelSummaries.tracking.trackingArrivalRate) }}</strong></span>
      </div>
    </div>

    <PatientHeatmapChart
      v-if="showHeatmap"
      :heatmap="heatmapData"
      :loading="heatmapLoading"
      :year="heatmapYear"
      :year-options="yearOptions"
      @drill="handleHeatmapDrill"
      @update:year="heatmapYear = $event"
    />
  </div>
</template>

<style lang="scss" scoped>
.workbench-stats {
  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 10px;
  }

  .panel-year-select {
    width: 108px;
    flex-shrink: 0;
  }

  .panel-period {
    margin-bottom: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
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
