<script lang="ts" setup>
import ScopedDepartmentMultiSelect from "@@/components/ScopedDepartmentMultiSelect.vue"
import { downloadBlob } from "@@/utils/download"
import { buildStatYearOptions, getCurrentStatYear } from "@@/utils/stat-year"
import {
  exportStudentReportStatisticsApi,
  getDistrictOptionsApi,
  getStudentReportStatisticsApi,
  STUDENT_REPORT_SCHOOL_CATEGORIES
} from "@/pages/statistics/apis"

defineOptions({ name: "SchoolSuspected" })

const filterForm = reactive({
  year: String(getCurrentStatYear()),
  district: "",
  departmentIds: [] as string[]
})

const yearOptions = buildStatYearOptions()
const districtOptions = ref<string[]>([])
const showDepartmentFilter = ref(false)
const loading = ref(false)
const tableData = ref<any[]>([])
const schoolCategories = ref<string[]>([...STUDENT_REPORT_SCHOOL_CATEGORIES])

async function loadDistrictOptions() {
  try {
    const { data } = await getDistrictOptionsApi(filterForm.departmentIds)
    districtOptions.value = data || []
  } catch { /* ignore */ }
}

async function fetchData() {
  if (!schoolCategories.value.length) {
    ElMessage.warning("请至少选择一个学校分类")
    return
  }
  loading.value = true
  try {
    const { data } = await getStudentReportStatisticsApi({
      year: filterForm.year,
      district: filterForm.district,
      departmentIds: filterForm.departmentIds,
      schoolCategories: schoolCategories.value
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
  schoolCategories.value = [...STUDENT_REPORT_SCHOOL_CATEGORIES]
  handleSearch()
}

function handleDepartmentChange(departmentIds?: string[]) {
  if (Array.isArray(departmentIds)) {
    filterForm.departmentIds = departmentIds
  }
  filterForm.district = ""
  handleSearch()
}

async function handleExport() {
  if (!schoolCategories.value.length) {
    ElMessage.warning("请至少选择一个学校分类")
    return
  }
  try {
    const data = await exportStudentReportStatisticsApi({
      year: filterForm.year,
      district: filterForm.district,
      departmentIds: filterForm.departmentIds,
      schoolCategories: schoolCategories.value
    })
    downloadBlob(data as unknown as Blob, `学生统计报表_${filterForm.year || "全部"}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
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
            @change="handleDepartmentChange"
          />
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="filterForm.year" placeholder="选择年份" clearable style="width: 120px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="filterForm.district" placeholder="全部区县" clearable filterable style="width: 160px">
            <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="学校分类">
          <el-select
            v-model="schoolCategories"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部学校分类"
            style="min-width: 280px"
          >
            <el-option
              v-for="c in STUDENT_REPORT_SCHOOL_CATEGORIES"
              :key="c"
              :label="c"
              :value="c"
            />
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
          <el-button
            type="success"
            v-permission="['statistics:export', 'school:suspected']"
            @click="handleExport"
          >
            导出 Excel
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span class="text-lg font-bold">学校人群 — 学生报表统计</span>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe max-height="600" show-summary>
        <el-table-column prop="schoolCategory" label="学校分类" min-width="160" fixed />
        <el-table-column prop="enrollmentCount" label="入学新生人数" min-width="120" />
        <el-table-column label="结核病检查情况" align="center">
          <el-table-column prop="acceptedExamCount" label="接受检查人数" min-width="120" />
          <el-table-column prop="standardizedExamCount" label="接受规范检查人数" min-width="140" />
        </el-table-column>
        <el-table-column prop="tbPatientCount" label="发现肺结核患者例数" min-width="150" />
      </el-table>
    </el-card>
  </div>
</template>
