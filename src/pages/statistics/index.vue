<script lang="ts" setup>
import { getSchoolStatisticsApi, getDistrictStatisticsApi, exportSchoolStatisticsApi, exportDistrictStatisticsApi } from "./apis"

defineOptions({ name: "Statistics" })

const activeTab = ref("school")

// ==================== 筛选条件 ====================
const filterForm = reactive({
  year: String(new Date().getFullYear()),
  district: ""
})

const districtOptions = ["自流井区", "高新区", "荣县", "富顺县", "贡井区", "大安区", "沿滩区"]
const yearOptions = Array.from({ length: 10 }, (_, i) => String(new Date().getFullYear() - i))

// ==================== 学校人群统计 ====================
const schoolLoading = ref(false)
const schoolData = ref<any[]>([])

async function fetchSchoolStatistics() {
  schoolLoading.value = true
  try {
    const { data } = await getSchoolStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    schoolData.value = data || []
  } catch { /* handled */ } finally {
    schoolLoading.value = false
  }
}

// ==================== 区县统计 ====================
const districtLoading = ref(false)
const districtData = ref<any[]>([])

async function fetchDistrictStatistics() {
  districtLoading.value = true
  try {
    const { data } = await getDistrictStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    districtData.value = data || []
  } catch { /* handled */ } finally {
    districtLoading.value = false
  }
}

// ==================== 搜索与重置 ====================
function handleSearch() {
  if (activeTab.value === "school") {
    fetchSchoolStatistics()
  } else {
    fetchDistrictStatistics()
  }
}

function handleReset() {
  filterForm.year = String(new Date().getFullYear())
  filterForm.district = ""
  handleSearch()
}

// ==================== 导出 ====================
function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

async function handleExportSchool() {
  try {
    const data = await exportSchoolStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    downloadBlob(data as unknown as Blob, `学校人群统计总表_${filterForm.year}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

async function handleExportDistrict() {
  try {
    const data = await exportDistrictStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    downloadBlob(data as unknown as Blob, `区县统计表_${filterForm.year}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

// ==================== Tab 切换时自动加载数据 ====================
function handleTabChange(tab: string) {
  if (tab === "school") {
    fetchSchoolStatistics()
  } else {
    fetchDistrictStatistics()
  }
}

onMounted(() => {
  fetchSchoolStatistics()
})
</script>

<template>
  <div class="app-container">
    <!-- 筛选条件 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="filterForm" inline>
        <el-form-item label="年份">
          <el-select v-model="filterForm.year" placeholder="选择年份" clearable>
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="filterForm.district" placeholder="全部区县" clearable>
            <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Tab 切换 -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 学校人群统计总表 -->
        <el-tab-pane label="辖区教育机构统计总表" name="school">
          <div class="mb-3 flex justify-end">
            <el-button type="success" v-permission="'statistics:export'" @click="handleExportSchool">导出 Excel</el-button>
          </div>
          <el-table v-loading="schoolLoading" :data="schoolData" border stripe max-height="600" show-summary>
            <el-table-column prop="district" label="区县" width="100" fixed />
            <el-table-column prop="schoolName" label="学校名称" width="180" />
            <el-table-column prop="shouldScreenCount" label="应筛查人数" width="110" />
            <el-table-column prop="actualScreenCount" label="实际筛查人数" width="120" />
            <el-table-column prop="closeContactCount" label="密切接触人数" width="120" />
            <el-table-column prop="suspiciousSymptomCount" label="可疑症状人数" width="120" />
            <el-table-column prop="chestXrayCount" label="胸片检查人数" width="120" />
            <el-table-column prop="chestXrayAbnormalCount" label="胸片异常人数" width="120" />
            <el-table-column prop="ppdTestCount" label="结核菌素检测人数" width="140" />
            <el-table-column prop="ppdPositive1" label="PPD+" width="80" />
            <el-table-column prop="ppdPositive2" label="PPD++" width="80" />
            <el-table-column prop="ppdPositive3" label="PPD+++" width="90" />
            <el-table-column prop="ppdPositiveTotal" label="PPD阳性合计" width="120" />
            <el-table-column prop="ecNegative" label="EC阴性" width="90" />
            <el-table-column prop="ecPositive" label="EC阳性" width="90" />
            <el-table-column prop="igraPositive" label="IGRA阳性" width="100" />
            <el-table-column prop="igraNegative" label="IGRA阴性" width="100" />
            <el-table-column prop="tbPatientCount" label="肺结核/疑似患者" width="130" />
            <el-table-column prop="remark" label="备注" width="150" />
          </el-table>
        </el-tab-pane>

        <!-- 区县统计表 -->
        <el-tab-pane label="区县统计表" name="district">
          <div class="mb-3 flex justify-end">
            <el-button type="success" v-permission="'statistics:export'" @click="handleExportDistrict">导出 Excel</el-button>
          </div>
          <el-table v-loading="districtLoading" :data="districtData" border stripe max-height="600" show-summary>
            <el-table-column prop="district" label="区/县" width="100" fixed />
            <el-table-column prop="actualScreenCount" label="实际筛查人数" width="120" />
            <el-table-column prop="closeContactCount" label="密切接触人数" width="120" />
            <el-table-column prop="suspiciousSymptomCount" label="可疑症状人数" width="120" />
            <el-table-column prop="chestXrayCount" label="胸片检查人数" width="120" />
            <el-table-column prop="chestXrayAbnormalCount" label="胸片异常人数" width="120" />
            <el-table-column prop="ppdTestCount" label="结核菌素检测人数" width="140" />
            <el-table-column prop="ppdPositive1" label="PPD+" width="80" />
            <el-table-column prop="ppdPositive2" label="PPD++" width="80" />
            <el-table-column prop="ppdPositive3" label="PPD+++" width="90" />
            <el-table-column prop="ppdPositiveTotal" label="PPD阳性合计" width="120" />
            <el-table-column prop="ecNegative" label="EC阴性" width="90" />
            <el-table-column prop="ecPositive" label="EC阳性" width="90" />
            <el-table-column prop="igraPositive" label="IGRA阳性" width="100" />
            <el-table-column prop="igraNegative" label="IGRA阴性" width="100" />
            <el-table-column prop="tbPatientCount" label="肺结核/疑似患者" width="130" />
            <el-table-column prop="remark" label="备注" width="150" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
</style>
