<script lang="ts" setup>
import type { TableColumnCtx } from "element-plus"
import type { KeyPopulationTbSymptomReferralStatisticsVO } from "@/pages/statistics/apis/key-population-tb-symptom-referral"
import ScopedDepartmentMultiSelect from "@@/components/ScopedDepartmentMultiSelect.vue"
import { buildStatYearOptions, getCurrentStatYear } from "@@/utils/stat-year"
import { getDistrictOptionsApi } from "@/pages/statistics/apis"
import {
  exportKeyPopulationTbSymptomReferralStatisticsApi,
  getKeyPopulationTbSymptomReferralStatisticsApi
} from "@/pages/statistics/apis/key-population-tb-symptom-referral"

defineOptions({ name: "StatisticsKeyPopulationTbSymptomReferral" })

const filterForm = reactive({
  year: String(getCurrentStatYear()),
  district: "",
  departmentIds: [] as string[]
})

const yearOptions = buildStatYearOptions()
const districtOptions = ref<string[]>([])
const loading = ref(false)
const tableData = ref<KeyPopulationTbSymptomReferralStatisticsVO[]>([])
/** 有可选部门时才展示「部门」筛选项 */
const showDepartmentFilter = ref(false)

async function loadDistrictOptions() {
  try {
    const { data } = await getDistrictOptionsApi(filterForm.departmentIds)
    districtOptions.value = data || []
  } catch { /* ignore */ }
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getKeyPopulationTbSymptomReferralStatisticsApi({
      year: filterForm.year,
      district: filterForm.district,
      departmentIds: filterForm.departmentIds
    })
    tableData.value = data || []
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadDistrictOptions()
  fetchData()
}

function handleReset() {
  filterForm.year = String(getCurrentStatYear())
  filterForm.district = ""
  filterForm.departmentIds = []
  handleSearch()
}

function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

async function handleExport() {
  try {
    const data = await exportKeyPopulationTbSymptomReferralStatisticsApi({
      year: filterForm.year,
      district: filterForm.district,
      departmentIds: filterForm.departmentIds
    })
    downloadBlob(
      data as unknown as Blob,
      `重点人群肺结核可疑症状筛查和推介情况报表_${filterForm.year || "全部"}.xlsx`
    )
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

interface SummaryMethodProps {
  columns: TableColumnCtx<KeyPopulationTbSymptomReferralStatisticsVO>[]
  data: KeyPopulationTbSymptomReferralStatisticsVO[]
}

function getSummaries(param: SummaryMethodProps) {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = "合计"
      return
    }
    const prop = column.property as keyof KeyPopulationTbSymptomReferralStatisticsVO | undefined
    if (!prop) {
      sums[index] = ""
      return
    }
    const total = data.reduce((acc, row) => acc + Number(row[prop] ?? 0), 0)
    sums[index] = String(total)
  })
  return sums
}

onMounted(() => {
  loadDistrictOptions()
  fetchData()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form :model="filterForm" inline>
        <el-form-item v-show="showDepartmentFilter" label="部门">
          <ScopedDepartmentMultiSelect
            v-model="filterForm.departmentIds"
            @visibility-change="showDepartmentFilter = $event"
          />
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="filterForm.year" placeholder="选择年份" clearable style="width: 120px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="filterForm.district" placeholder="全部区县" clearable style="width: 160px">
            <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" v-permission="'statistics:export'" @click="handleExport">
            导出 Excel
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="report-title mb-3">
        {{ filterForm.year || "" }}年自贡市重点人群肺结核可疑症状筛查和推介情况报表
      </div>
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        max-height="640"
        show-summary
        :summary-method="getSummaries"
      >
        <el-table-column prop="district" label="地区" fixed min-width="110" />

        <el-table-column label="老年人" align="center">
          <el-table-column prop="elderCount" label="老年人数" min-width="100" />
          <el-table-column prop="elderAnnualExamCount" label="参加年度体检人数" min-width="130" />
          <el-table-column label="筛查方式" align="center">
            <el-table-column prop="elderSymptomScreenCount" label="进行症状筛查人数" min-width="130" />
            <el-table-column prop="elderChestXrayCount" label="开展胸部影像学筛查人数" min-width="160" />
            <el-table-column prop="elderInfectionScreenCount" label="开展感染筛查人数" min-width="130" />
          </el-table-column>
          <el-table-column label="筛查异常人数" align="center">
            <el-table-column prop="elderSuspiciousSymptomCount" label="肺结核可疑症状人数" min-width="140" />
            <el-table-column prop="elderChestXrayAbnormalCount" label="胸部影像学筛查异常人数" min-width="160" />
            <el-table-column prop="elderInfectionAbnormalCount" label="开展感染筛查异常人数" min-width="150" />
          </el-table-column>
          <el-table-column label="转诊、推荐及确诊人数" align="center">
            <el-table-column prop="elderReferralFormCount" label="开具推介转诊单人数" min-width="140" />
            <el-table-column prop="elderArrivedCount" label="到结核病定点医疗机构就诊人数" min-width="190" />
            <el-table-column prop="elderConfirmedTbCount" label="诊断为肺结核的人数" min-width="140" />
          </el-table-column>
        </el-table-column>

        <el-table-column label="糖尿病患者" align="center">
          <el-table-column prop="diabetesManagedCount" label="管理的糖尿病患者数" min-width="140" />
          <el-table-column prop="diabetesQuarterFollowCount" label="完成糖尿病管理季度随访的患者数" min-width="200" />
          <el-table-column label="筛查方式" align="center">
            <el-table-column prop="diabetesSymptomScreenCount" label="进行症状筛查人数" min-width="130" />
            <el-table-column prop="diabetesChestXrayCount" label="开展胸部影像学筛查人数" min-width="160" />
            <el-table-column prop="diabetesInfectionScreenCount" label="开展感染筛查人数" min-width="130" />
          </el-table-column>
          <el-table-column label="筛查异常人数" align="center">
            <el-table-column prop="diabetesSuspiciousSymptomCount" label="肺结核可疑症状人数" min-width="140" />
            <el-table-column prop="diabetesChestXrayAbnormalCount" label="胸部影像学筛查异常人数" min-width="160" />
            <el-table-column prop="diabetesInfectionAbnormalCount" label="开展感染筛查异常人数" min-width="150" />
          </el-table-column>
          <el-table-column label="转诊、推荐及确诊人数" align="center">
            <el-table-column prop="diabetesReferralFormCount" label="开具推介转诊单人数" min-width="140" />
            <el-table-column prop="diabetesArrivedCount" label="到结核病定点医疗机构就诊人数" min-width="190" />
            <el-table-column prop="diabetesConfirmedTbCount" label="诊断为肺结核的人数" min-width="140" />
          </el-table-column>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.mb-3 {
  margin-bottom: 12px;
}

.mb-4 {
  margin-bottom: 16px;
}

.report-title {
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  color: var(--el-text-color-primary);
}
</style>
