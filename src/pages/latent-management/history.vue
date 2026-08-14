<script lang="ts" setup>
import ArchivedLatentRecordsActions from "@@/components/ArchivedLatentRecordsActions.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { displayInfectionJudgeResult, getPopulationTypeLabel, getPopulationTypeTagType, TREATMENT_COMPLETION_STATUS_OPTIONS } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { useUserStore } from "@/pinia/stores/user"
import {
  batchDeleteLatentApi,
  exportLatentHistoryApi,
  getLatentHistoryListApi,
  unarchiveLatentFromCloseCaseApi
} from "./apis"
import { useLatentTableHeaderFilters } from "./composables/useLatentTableHeaderFilters"

const userStore = useUserStore()

const { paginationData, handleCurrentChange, handleSizeChange, getTableIndex } = usePagination()
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const exporting = ref(false)
const selectedRows = ref<any[]>([])

const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  populationType: "",
  startTime: "",
  endTime: "",
  treatmentCompletionStatus: ""
})

const {
  genderFilterOptions,
  populationTypeFilterOptions,
  infectionResultFilterOptions,
  loadGenderOptions,
  loadPopulationTypeOptions,
  loadInfectionResultOptions,
  genderSourceValues,
  populationTypeSourceValues,
  infectionResultSourceValues
} = useLatentTableHeaderFilters(() => searchForm.populationType)

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function fetchData() {
  loading.value = true
  try {
    const columnFiltersParam = toQueryParam()
    const params: Record<string, any> = {
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...searchForm,
      ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
    }
    if (!params.name) delete params.name
    if (!params.idNumber) delete params.idNumber
    if (!params.populationType) delete params.populationType
    if (!params.phone) delete params.phone
    if (!params.startTime) delete params.startTime
    if (!params.endTime) delete params.endTime
    if (!params.treatmentCompletionStatus) delete params.treatmentCompletionStatus
    const { data } = await getLatentHistoryListApi(params)
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
  Object.assign(searchForm, { name: "", idNumber: "", phone: "", populationType: "", startTime: "", endTime: "", treatmentCompletionStatus: "" })
  clearFilters()
  handleSearch()
}

onMounted(fetchData)
watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

async function handleExport() {
  if (total.value === 0) {
    ElMessage.warning("当前没有历史患者数据，将导出仅含表头的空表")
  }
  exporting.value = true
  try {
    const blob = await exportLatentHistoryApi({
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      populationType: searchForm.populationType || undefined,
      dateFrom: searchForm.startTime || undefined,
      dateTo: searchForm.endTime || undefined,
      treatmentCompletionStatus: searchForm.treatmentCompletionStatus || undefined
    })
    downloadBlob(blob as unknown as Blob, "潜伏感染者历史患者信息总表.xlsx")
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) return
  const names = selectedRows.value.map(r => r.name).join("、")
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 条记录（${names}）吗？关联的通知单、督导表等数据将一并删除，且不可恢复！`,
      "警告",
      { type: "warning" }
    )
    await batchDeleteLatentApi(selectedRows.value.map(r => r.id))
    ElMessage.success("删除成功")
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除失败")
  }
}

async function handleUnarchive(row: Record<string, any>) {
  try {
    await ElMessageBox.confirm(
      `确认解锁潜伏感染者 ${row.name} 的档案？解锁后可重新填写督导表与服药管理。`,
      "解锁档案",
      { type: "warning" }
    )
    await unarchiveLatentFromCloseCaseApi(row.id)
    ElMessage.success("已解锁，已恢复为在管状态")
    fetchData()
  } catch { /* cancelled or handled */ }
}

function treatmentPhaseLabel(phase?: number) {
  if (phase === 2) return "已结案"
  if (phase === 1) return "预防治疗中"
  return "未开始"
}
</script>

<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
      title="此处展示已归档（停止治疗、治愈结案等）的潜伏感染者，可从在管总览归档后在此查询"
    />

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
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width:140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="疫情筛查" value="regular" />
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
            <el-option label="密接" value="closeContact" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗完成情况">
          <el-select v-model="searchForm.treatmentCompletionStatus" placeholder="全部" clearable style="width:140px">
            <el-option v-for="item in TREATMENT_COMPLETION_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
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
      <div class="toolbar flex items-center justify-end gap-2" style="margin-bottom: 12px">
        <el-button
          v-permission="'latentManagement:history'"
          type="danger"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          删除
        </el-button>
        <el-button
          v-permission="'latentManagement:history'"
          type="success"
          :loading="exporting"
          @click="handleExport"
        >
          导出
        </el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column type="index" label="#" :index="getTableIndex" />
        <el-table-column prop="populationType" min-width="110">
          <template #header>
            <TableHeaderFilter
              label="数据来源"
              type="select"
              :options="populationTypeFilterOptions"
              :source-values="populationTypeSourceValues"
              :load-options="loadPopulationTypeOptions"
              :model-value="columnFilters.populationType"
              @change="(v) => { setFilter('populationType', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" min-width="90">
          <template #header>
            <TableHeaderFilter
              label="姓名"
              :model-value="columnFilters.name"
              @change="(v) => { setFilter('name', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="registrationNo" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="登记号"
              hint="数据来源：通知单（填写/保存潜伏感染者通知单后同步）"
              :model-value="columnFilters.registrationNo"
              @change="(v) => { setFilter('registrationNo', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ row.registrationNo || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="gender" min-width="80">
          <template #header>
            <TableHeaderFilter
              label="性别"
              type="select"
              :options="genderFilterOptions"
              :source-values="genderSourceValues"
              :load-options="loadGenderOptions"
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
        <el-table-column prop="infectionResult" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="感染筛查结果"
              type="select"
              :options="infectionResultFilterOptions"
              :source-values="infectionResultSourceValues"
              :load-options="loadInfectionResultOptions"
              :model-value="columnFilters.infectionResult"
              @change="(v) => { setFilter('infectionResult', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ displayInfectionJudgeResult(row.infectionResult) }}
          </template>
        </el-table-column>
        <el-table-column label="治疗阶段">
          <template #default="{ row }">
            {{ treatmentPhaseLabel(row.treatmentPhase) }}
          </template>
        </el-table-column>
        <el-table-column prop="archivedTime" label="归档时间" />
        <el-table-column prop="treatmentCompletionStatus" label="治疗完成情况" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="380">
          <template #default="{ row }">
            <el-button
              v-if="userStore.userRole !== 6"
              type="warning"
              link
              size="small"
              @click="handleUnarchive(row)"
            >
              解锁
            </el-button>
            <ArchivedLatentRecordsActions :row="row" />
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
