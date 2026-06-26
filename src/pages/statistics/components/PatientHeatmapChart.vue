<script lang="ts" setup>
import type { GeoJsonFeatureCollection } from "@@/utils/zigong-map"
import type { PatientHeatmapData } from "../apis"
import {
  buildMapSeriesData,
  buildTownshipGeoJson,
  findDistrictFeature,
  loadZigongCityGeo
} from "@@/utils/zigong-map"
import { ArrowLeft } from "@element-plus/icons-vue"
import * as echarts from "echarts"
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = defineProps<{
  heatmap: PatientHeatmapData
  loading?: boolean
}>()

const emit = defineEmits<{
  drill: [district: string | null]
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let cityGeo: GeoJsonFeatureCollection | null = null
let clickBound = false
let renderToken = 0

const mapReady = ref(false)
const mapError = ref("")

async function ensureCityGeo() {
  if (!cityGeo) {
    cityGeo = await loadZigongCityGeo()
    echarts.registerMap("zigong-city", cityGeo as any)
  }
}

function getMapName() {
  return props.heatmap.mapLevel === "district"
    ? `zigong-district-${props.heatmap.districtAdcode || props.heatmap.districtName}`
    : "zigong-city"
}

function buildSeriesData() {
  if (!cityGeo) return []
  if (props.heatmap.mapLevel === "district") {
    const districtFeature = findDistrictFeature(cityGeo, props.heatmap.districtName || "")
    if (!districtFeature) return []
    const townshipGeo = buildTownshipGeoJson(districtFeature, props.heatmap.regions ?? [])
    echarts.registerMap(getMapName(), townshipGeo as any)
    return buildMapSeriesData(props.heatmap.regions, townshipGeo.features)
  }
  return buildMapSeriesData(props.heatmap.regions, cityGeo.features)
}

function bindMapClick() {
  if (!chart || clickBound) return
  chart.on("click", (params: any) => {
    if (params.componentType !== "series" || params.seriesType !== "map") return
    if (props.heatmap.mapLevel !== "city") return
    const name = params.name as string
    if (!name || name === "未分配") return
    emit("drill", name)
  })
  clickBound = true
}

async function renderChart() {
  if (!chartRef.value) return
  const token = ++renderToken
  mapError.value = ""
  mapReady.value = false
  try {
    await ensureCityGeo()
    if (token !== renderToken) return
    if (!chart) {
      chart = echarts.init(chartRef.value)
    }

    const seriesData = buildSeriesData()
    const maxCount = Math.max(props.heatmap.maxCount ?? 0, 1)
    const isDistrict = props.heatmap.mapLevel === "district"
    const hasData = seriesData.some(item => item.value > 0)

    if (!seriesData.length) {
      chart.clear()
      chart.setOption({
        title: {
          text: isDistrict ? "暂无该区县乡镇数据" : "暂无地图数据",
          left: "center",
          top: "middle",
          textStyle: { color: "#909399", fontSize: 14, fontWeight: "normal" }
        }
      })
      return
    }

    chart.setOption({
      tooltip: {
        trigger: "item",
        formatter: (params: any) => {
          const val = params.value ?? 0
          return `${params.name}<br/>患者数：<strong>${val}</strong> 例`
        }
      },
      visualMap: {
        min: 0,
        max: maxCount,
        calculable: true,
        left: 20,
        bottom: 20,
        text: ["高", "低"],
        inRange: {
          color: ["#e8f4ff", "#66b1ff", "#409eff", "#1a56a8"]
        }
      },
      series: [{
        name: "患者数",
        type: "map",
        map: getMapName(),
        roam: true,
        scaleLimit: { min: 0.8, max: 4 },
        label: {
          show: true,
          fontSize: isDistrict ? 10 : 12,
          color: "#303133"
        },
        emphasis: {
          label: { show: true, fontWeight: "bold" },
          itemStyle: { areaColor: "#ffd666", borderColor: "#333" }
        },
        itemStyle: {
          borderColor: "#fff",
          borderWidth: 1
        },
        data: seriesData
      }]
    }, true)

    bindMapClick()

    if (!hasData && !isDistrict) {
      chart.setOption({
        title: {
          subtext: "当前年度暂无患者分布数据，可点击区县查看下级",
          left: "center",
          top: 8,
          subtextStyle: { fontSize: 12, color: "#909399" }
        }
      })
    }
  } catch (err) {
    if (token !== renderToken) return
    mapError.value = err instanceof Error ? err.message : "地图加载失败"
    chart?.clear()
  } finally {
    if (token === renderToken) {
      mapReady.value = true
    }
  }
}

function handleBack() {
  emit("drill", null)
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
  renderChart().finally(() => chart?.resize())
  window.addEventListener("resize", handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  chart?.dispose()
  chart = null
  clickBound = false
})
</script>

<template>
  <div v-loading="!!loading || !mapReady" class="patient-heatmap">
    <div class="heatmap-header">
      <div class="heatmap-header-left">
        <el-button
          v-if="heatmap.mapLevel === 'district'"
          link
          type="primary"
          :icon="ArrowLeft"
          @click="handleBack"
        >
          返回自贡市
        </el-button>
        <span class="title">
          {{ heatmap.managementYear ?? "—" }}年度患者分布热力图
          <template v-if="heatmap.mapLevel === 'district' && heatmap.districtName">
            · {{ heatmap.districtName }}
          </template>
        </span>
      </div>
      <span v-if="heatmap.total != null" class="total">共 {{ heatmap.total }} 例</span>
    </div>
    <div v-if="heatmap.statPeriodFrom && heatmap.statPeriodTo" class="heatmap-tip">
      统计周期：{{ heatmap.statPeriodFrom }} 至 {{ heatmap.statPeriodTo }}
      · {{ heatmap.mapLevel === "city" ? "点击区县下钻查看乡镇分布" : "展示乡镇/社区患者分布" }}
    </div>
    <el-alert v-if="mapError" :title="mapError" type="error" :closable="false" show-icon class="heatmap-error" />
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
    margin-bottom: 8px;
    gap: 12px;

    .heatmap-header-left {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .title {
      font-size: 15px;
      font-weight: 600;
      color: #409eff;
    }

    .total {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      flex-shrink: 0;
    }
  }

  .heatmap-tip {
    margin-bottom: 12px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .heatmap-error {
    margin-bottom: 12px;
  }

  .heatmap-chart {
    width: 100%;
    height: 520px;
  }
}
</style>
