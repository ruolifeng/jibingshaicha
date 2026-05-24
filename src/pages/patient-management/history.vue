<script lang="ts" setup>
import ArchivedPatientRecordsActions from "@@/components/ArchivedPatientRecordsActions.vue"
import { usePagination } from "@@/composables/usePagination"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_OPTIONS } from "@@/constants/disease"
import { isStopTreatmentArchive } from "@@/utils/followUpVisit"
import { useUserStore } from "@/pinia/stores/user"
import { getPatientHistoryListApi, unarchivePatientFromStopTreatmentApi } from "./apis"

const userStore = useUserStore()

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  diagnosisResult: "",
  populationType: "",
  startTime: "",
  endTime: ""
})

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...searchForm
    }
    if (!params.populationType) delete params.populationType
    if (!params.phone) delete params.phone
    if (!params.diagnosisResult) delete params.diagnosisResult
    const { data } = await getPatientHistoryListApi(params)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  paginationData.currentPage = 1
  fetchData()
}
function handleReset() {
  Object.assign(searchForm, { name: "", idNumber: "", phone: "", diagnosisResult: "", populationType: "", startTime: "", endTime: "" })
  handleSearch()
}

onMounted(fetchData)
watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

async function handleUnarchive(row: Record<string, any>) {
  try {
    await ElMessageBox.confirm(
      `确认解锁患者 ${row.name} 的档案？解锁后可重新填写后续随访。`,
      "解锁档案",
      { type: "warning" }
    )
    await unarchivePatientFromStopTreatmentApi(row.id)
    ElMessage.success("已解锁，患者已恢复为在管状态")
    fetchData()
  } catch { /* cancelled or handled */ }
}
</script>

<template>
  <div class="app-container">
    <el-card class="search-wrapper" shadow="never">
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="病原学结果">
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable filterable style="width:140px">
            <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width:140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="疫情筛查" value="regular" />
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
            <el-option label="密接" value="closeContact" />
            <el-option label="专病网" value="specialDisease" />
          </el-select>
        </el-form-item>
        <el-form-item label="归档时间">
          <el-date-picker v-model="searchForm.startTime" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width:140px" />
          <span style="margin:0 8px">~</span>
          <el-date-picker v-model="searchForm.endTime" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width:140px" />
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

    <el-card shadow="never" style="margin-top:10px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" />
        <el-table-column label="数据来源">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="病原学结果" />
        <el-table-column prop="archiveRemark" label="备注" min-width="100">
          <template #default="{ row }">
            {{ row.archiveRemark || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="archivedTime" label="归档时间" />
        <el-table-column label="操作" fixed="right" width="420">
          <template #default="{ row }">
            <el-button
              v-if="userStore.userRole !== 6 && isStopTreatmentArchive(row.archiveRemark)"
              type="warning"
              link
              size="small"
              @click="handleUnarchive(row)"
            >
              解锁
            </el-button>
            <ArchivedPatientRecordsActions :row="row" />
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination-container"
        :current-page="paginationData.currentPage || 1"
        :page-sizes="paginationData.pageSizes"
        :page-size="paginationData.pageSize || 10"
        :total="total || 0"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>
