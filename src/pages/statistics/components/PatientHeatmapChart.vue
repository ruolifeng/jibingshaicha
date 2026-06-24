<script lang="ts" setup>
import type { PatientHeatmapData } from "../apis"
import * as echarts from "echarts"
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = defineProps<{
  heatmap: PatientHeatmapData
  loading?: boolean
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const rowLabels = props.heatmap.rowLabels ?? []
  const colLabels = props.heatmap.colLabels ?? []
  const points = props.heatmap.data ?? []
  const maxCount = props.heatmap.maxCount ?? 0

  if (!points.length) {
    chart.clear()
    chart.setOption({
      title: {
        text: "暂无患者分布数据",
        left: "center",
        top: "middle",
        textStyle: { color: "#909399", fontSize: 14, fontWeight: "normal" }
      },
      xAxis: { show: false },
      yAxis: { show: false },
      series: []
    })
    return
  }

  chart.setOption({
    tooltip: {
      position: "top",
      formatter: (params: any) => {
        const p = params.data as Array<string | number>
        return `${p[3]} / ${p[4]}<br/>患者数：<strong>${p[2]}</strong> 例`
      }
    },
    grid: {
      left: 100,
      right: 40,
      top: 56,
      bottom: 80
    },
    title: {
      subtext: props.heatmap.statPeriodFrom && props.heatmap.statPeriodTo
        ? `统计周期：${props.heatmap.statPeriodFrom} 至 ${props.heatmap.statPeriodTo} · 横轴为各辖区内社区序号，悬停查看名称`
        : "横轴为各辖区内社区序号，悬停查看具体社区名称",
      left: "center",
      top: 4,
      subtextStyle: { fontSize: 12, color: "#909399" }
    },
    xAxis: {
      type: "category",
      data: colLabels,
      splitArea: { show: true },
      axisLabel: { rotate: colLabels.length > 8 ? 30 : 0 }
    },
    yAxis: {
      type: "category",
      data: rowLabels,
      splitArea: { show: true }
    },
    visualMap: {
      min: 0,
      max: Math.max(maxCount, 1),
      calculable: true,
      orient: "horizontal",
      left: "center",
      bottom: 10,
      inRange: {
        color: ["#e8f4ff", "#409eff", "#1a56a8"]
      }
    },
    series: [{
      name: "患者数",
      type: "heatmap",
      data: points.map((p: Array<string | number>) => [p[0], p[1], p[2], p[3], p[4]]),
      label: {
        show: true,
        formatter: (params: any) => {
          const val = params.data[2]
          return val > 0 ? String(val) : ""
        }
      },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: "rgba(0,0,0,0.3)" }
      }
    }]
  }, true)
}

function handleResize() {
  chart?.resize()
}

watch(() => props.heatmap, () => {
  nextTick(() => {
    renderChart()
    chart?.resize()
  })
}, { deep: true })

watch(() => props.loading, (val) => {
  if (!val) {
    nextTick(() => chart?.resize())
  }
})

onMounted(() => {
  nextTick(() => {
    renderChart()
    chart?.resize()
  })
  window.addEventListener("resize", handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div v-loading="!!loading" class="patient-heatmap">
    <div class="heatmap-header">
      <span class="title">{{ heatmap.managementYear ?? "—" }}年度患者分布热力图</span>
      <span v-if="heatmap.total != null" class="total">共 {{ heatmap.total }} 例</span>
    </div>
    <div ref="chartRef" class="heatmap-chart" />
  </div>
</template>

<style lang="scss" scoped>
.patient-heatmap {
  margin-top: 20px;
  padding: 18px 20px;
  border-radius: 12px;
  background: #fafcff;
  border: 1px solid #d9ecff;
  border-left: 4px solid #409eff;

  .heatmap-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .title {
      font-size: 15px;
      font-weight: 600;
      color: #409eff;
    }

    .total {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }

  .heatmap-chart {
    width: 100%;
    height: 420px;
  }
}
</style>
