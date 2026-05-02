<script lang="ts" setup>
import ScreeningDetailDialog from "@@/components/ScreeningDetailDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { getScreeningSchoolDetailApi } from "@/pages/school/screening/apis"
import { getPatientHistoryApi, getPatientHistoryStatsApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({ name: "", idNumber: "", startTime: "", endTime: "" })

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
    const { data } = await getPatientHistoryApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: "school",
      ...searchForm
    })
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
  searchForm.startTime = ""
  searchForm.endTime = ""
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
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="诊断结果" />
        <el-table-column prop="source" label="来源">
          <template #default="{ row }">
            {{ row.source === "confirmed" ? "诊断确诊" : "大疫情导入" }}
          </template>
        </el-table-column>
        <el-table-column prop="archivedTime" label="归档时间" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button type="info" link size="small" @click="viewScreeningDetail(row)">
              查看详情
            </el-button>
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
</style>
