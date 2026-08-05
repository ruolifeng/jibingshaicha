<script lang="ts" setup>
import type { ChoiceOptionRow } from "../../apis/type"
import * as echarts from "echarts"

const props = defineProps<{ rows: ChoiceOptionRow[] }>()

const barRef = ref<HTMLDivElement>()
const pieRef = ref<HTMLDivElement>()
let barInst: echarts.ECharts | null = null
let pieInst: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

interface ChartData {
  name: string
  value: number
  fullName: string
}

const visibleRows = computed<ChoiceOptionRow[]>(() => props.rows.filter((r: ChoiceOptionRow) => r.count > 0))
const barHeight = computed(() => Math.max(280, visibleRows.value.length * 42 + 72))

function buildSeriesData(): ChartData[] {
  return visibleRows.value
    .map((r: ChoiceOptionRow) => ({ name: r.label.length > 16 ? `${r.label.slice(0, 16)}…` : r.label, value: r.count, fullName: r.label }))
}

function render() {
  const seriesData = buildSeriesData()
  const labels = seriesData.map(d => d.name)
  const values = seriesData.map(d => d.value)
  const maxValue = Math.max(1, ...values)

  if (barRef.value) {
    if (!barInst) barInst = echarts.init(barRef.value)
    barInst.setOption(
      {
        tooltip: {
          trigger: "axis",
          axisPointer: { type: "shadow" },
          formatter: (params: unknown) => {
            const p = Array.isArray(params) ? params[0] : params
            if (!p || typeof p !== "object" || !("dataIndex" in p)) return ""
            const i = (p as { dataIndex: number }).dataIndex
            const row = seriesData[i]
            return row ? `${row.fullName}<br/>人数：${row.value}` : ""
          }
        },
        grid: { left: 8, right: 48, bottom: 26, top: 12, containLabel: true },
        xAxis: {
          type: "value",
          minInterval: 1,
          max: maxValue,
          splitNumber: Math.min(maxValue, 5),
          axisLabel: { show: false },
          axisTick: { show: false }
        },
        yAxis: {
          type: "category",
          data: labels,
          inverse: true,
          axisLabel: { width: 160, overflow: "truncate" }
        },
        series: [
          {
            type: "bar",
            data: values,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: "#409eff" },
                { offset: 1, color: "#a0cfff" }
              ])
            },
            label: {
              show: true,
              position: "right",
              formatter: "{c}"
            },
            barMaxWidth: 28
          }
        ]
      },
      true
    )
  }

  if (pieRef.value) {
    if (!pieInst) pieInst = echarts.init(pieRef.value)
    pieInst.setOption(
      {
        color: ["#409eff", "#67c23a", "#e6a23c", "#f56c6c", "#909399", "#b37feb", "#36cfc9", "#ffc53d"],
        tooltip: {
          trigger: "item",
          formatter: (p: { name: string, value: number, percent: number }) =>
            `${p.name}<br/>人数：${p.value}（${p.percent}%）`
        },
        legend: { type: "scroll", bottom: 0, left: "center", textStyle: { fontSize: 12 } },
        series: [
          {
            type: "pie",
            radius: ["38%", "68%"],
            center: ["50%", "44%"],
            data: seriesData.map(d => ({ name: d.name, value: d.value })),
            label: { formatter: "{b}\n{d}%", fontSize: 12, minMargin: 6 },
            emphasis: {
              itemStyle: { shadowBlur: 8, shadowOffsetX: 0, shadowColor: "rgba(0,0,0,0.15)" }
            }
          }
        ]
      },
      true
    )
  }
}

function resize() {
  nextTick(() => {
    barInst?.resize()
    pieInst?.resize()
  })
}

watch(
  () => props.rows,
  () => nextTick(() => {
    render()
    resize()
  }),
  { deep: true }
)

onMounted(() => {
  nextTick(() => {
    render()
    resize()
    if (barRef.value && pieRef.value) {
      resizeObserver = new ResizeObserver(resize)
      resizeObserver.observe(barRef.value)
      resizeObserver.observe(pieRef.value)
    }
  })
  window.addEventListener("resize", resize)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", resize)
  resizeObserver?.disconnect()
  barInst?.dispose()
  pieInst?.dispose()
  resizeObserver = null
  barInst = null
  pieInst = null
})
</script>

<template>
  <div class="choice-charts">
    <div class="chart-wrap">
      <div class="chart-caption">
        人数分布（条形图）
      </div>
      <div ref="barRef" class="chart-box bar-chart-box" :style="{ height: `${barHeight}px` }" />
    </div>
    <div class="chart-wrap">
      <div class="chart-caption">
        占比分布（环形图）
      </div>
      <div ref="pieRef" class="chart-box pie-chart-box" />
    </div>
  </div>
</template>

<style scoped>
.choice-charts {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 16px;
  margin-top: 4px;
}
.chart-wrap {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 8px 8px 4px;
  border: 1px solid var(--el-border-color-lighter);
}
.chart-caption {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
  padding-left: 4px;
}
.chart-box {
  width: 100%;
}
.pie-chart-box {
  height: 320px;
}
@media (max-width: 900px) {
  .choice-charts {
    grid-template-columns: 1fr;
  }
}
</style>
