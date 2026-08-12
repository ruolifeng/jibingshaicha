<script lang="ts" setup>
import ArchivedPatientRecordsActions from "@@/components/ArchivedPatientRecordsActions.vue"
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { DIAGNOSIS_RESULT_OPTIONS } from "@@/constants/disease"
import { getScreeningSchoolDetailApi } from "@/pages/school/screening/apis"
import { getPatientHistoryApi, getPatientHistoryStatsApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

const genderFilterOptions = [
  { text: "男", value: "男" },
  { text: "女", value: "女" }
]
const diagnosisResultFilterOptions = DIAGNOSIS_RESULT_OPTIONS.map(item => ({
  text: item.label,
  value: item.value
}))

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({ name: "", idNumber: "", phone: "", startTime: "", endTime: "" })

const statistics = ref({ totalCount: 0, confirmedCount: 0, epidemicCount: 0, maleCount: 0, femaleCount: 0 })

async function fetchStats() {
  try {
    const { data } = await getPatientHistoryStatsApi("school")
    statistics.value = {
      totalCount: data.totalCount || 0,
      confirmedCount: data.confirmedCount || 0,
      epidemicCount: data.epidemicCount || 0,
      maleCount: data.maleCount || 0,
      femaleCount: data.femaleCount || 0
    }
  } catch { /* handled */ }
}

async function fetchData() {
  loading.value = true
  try {
    const columnFiltersParam = toQueryParam()
    const params: Parameters<typeof getPatientHistoryApi>[0] = {
      page: paginationData.currentPage ?? 1,
      size: paginationData.pageSize ?? 10,
      populationType: "school",
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      startTime: searchForm.startTime || undefined,
      endTime: searchForm.endTime || undefined,
      ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
    }
    const { data } = await getPatientHistoryApi(params)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
})

function handleSearch() {
  paginationData.currentPage = 1
  fetchData()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.phone = ""
  searchForm.startTime = ""
  searchForm.endTime = ""
  clearFilters()
  handleSearch()
}

// ==================== 筛查详情查看 ====================
const screeningDetailVisible = ref(false)
const screeningDetailData = ref<any>(null)

async function viewScreeningDetail(row: any) {
  if (!row.screeningId) {
    ElMessage.info("暂无筛查原始数据")
    return
  }
  try {
    const { data } = await getScreeningSchoolDetailApi(row.screeningId)
    if (data) {
      screeningDetailData.value = data
      screeningDetailVisible.value = true
    } else {
      ElMessage.info("暂无筛查原始数据")
    }
  } catch { /* handled by interceptor */ }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入联系电话" clearable />
        </el-form-item>
        <el-form-item label="归档时间">
          <el-date-picker
            v-model="searchForm.startTime"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
          />
          <span class="mx-2 text-gray-400">至</span>
          <el-date-picker
            v-model="searchForm.endTime"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="text-lg font-bold">统计汇总</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="4">
          <el-statistic title="历史患者总数" :value="statistics.totalCount" />
        </el-col>
        <el-col :span="5">
          <el-statistic title="诊断确诊" :value="statistics.confirmedCount" />
        </el-col>
        <el-col :span="5">
          <el-statistic title="大疫情导入" :value="statistics.epidemicCount" />
        </el-col>
        <el-col :span="5">
          <el-statistic title="男性" :value="statistics.maleCount" />
        </el-col>
        <el-col :span="5">
          <el-statistic title="女性" :value="statistics.femaleCount" />
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span class="text-lg font-bold">学校人群 — 历史患者</span>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" min-width="90" fixed>
          <template #header>
            <TableHeaderFilter
              label="姓名"
              :model-value="columnFilters.name"
              @change="(v) => { setFilter('name', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="gender" min-width="80">
          <template #header>
            <TableHeaderFilter
              label="性别"
              type="select"
              :options="genderFilterOptions"
              :model-value="columnFilters.gender"
              @change="(v) => { setFilter('gender', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" min-width="160" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="证件号"
              :model-value="columnFilters.idNumber"
              @change="(v) => { setFilter('idNumber', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="phone" min-width="120">
          <template #header>
            <TableHeaderFilter
              label="联系电话"
              :model-value="columnFilters.phone"
              @change="(v) => { setFilter('phone', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisResult" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="诊断结果"
              type="select"
              :options="diagnosisResultFilterOptions"
              :model-value="columnFilters.diagnosisResult"
              @change="(v) => { setFilter('diagnosisResult', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源">
          <template #default="{ row }">
            {{ row.source === "confirmed" ? "诊断确诊" : "大疫情导入" }}
          </template>
        </el-table-column>
        <el-table-column prop="archivedTime" label="归档时间" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" fixed="right" width="420">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="info" link size="small" @click="viewScreeningDetail(row)">
                筛查详情
              </el-button>
              <ArchivedPatientRecordsActions :row="row" />
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
    <!-- 筛查详情弹窗 -->
    <ScreeningDetailDialog v-model:visible="screeningDetailVisible" type="school" :data="screeningDetailData" />
  </div>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
.action-btns {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
</style>
