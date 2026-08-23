<script lang="ts" setup>
import ArchivedPatientRecordsActions from "@@/components/ArchivedPatientRecordsActions.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_FILTER_OPTIONS, STOP_TREATMENT_REASON_OPTIONS } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { isStopTreatmentArchive } from "@@/utils/followUpVisit"
import { formatStopTreatmentReason } from "@@/utils/followUpVisitFormat"
import { resolvePatientDiagnosisResult, resolvePatientPathogenResult } from "@@/utils/patient"
import { useUserStore } from "@/pinia/stores/user"
import {
  batchDeletePatientsApi,
  exportPatientHistoryApi,
  getPatientHistoryListApi,
  unarchivePatientFromStopTreatmentApi
} from "./apis"
import { usePatientTableHeaderFilters } from "./composables/usePatientTableHeaderFilters"

const userStore = useUserStore()

const { paginationData, handleCurrentChange, handleSizeChange, getTableIndex } = usePagination()
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

const {
  genderFilterOptions,
  pathogenFilterOptions,
  populationTypeFilterOptions,
  loadGenderOptions,
  loadPopulationTypeOptions,
  genderSourceValues,
  populationTypeSourceValues
} = usePatientTableHeaderFilters(1)

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const exporting = ref(false)
const selectedRows = ref<any[]>([])

const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  diagnosisResult: "",
  populationType: "",
  startTime: "",
  endTime: "",
  stopTreatmentReason: ""
})

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
    if (!params.populationType) delete params.populationType
    if (!params.phone) delete params.phone
    if (!params.diagnosisResult) delete params.diagnosisResult
    if (!params.startTime) delete params.startTime
    if (!params.endTime) delete params.endTime
    if (!params.stopTreatmentReason) delete params.stopTreatmentReason
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
  Object.assign(searchForm, { name: "", idNumber: "", phone: "", diagnosisResult: "", populationType: "", startTime: "", endTime: "", stopTreatmentReason: "" })
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
    const blob = await exportPatientHistoryApi({
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      diagnosisResult: searchForm.diagnosisResult || undefined,
      populationType: searchForm.populationType || undefined,
      startTime: searchForm.startTime || undefined,
      endTime: searchForm.endTime || undefined,
      stopTreatmentReason: searchForm.stopTreatmentReason || undefined
    })
    downloadBlob(blob as unknown as Blob, "历史患者信息总表.xlsx")
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
      `确定删除选中的 ${selectedRows.value.length} 条记录（${names}）吗？关联的通知单、随访、服药等数据将一并删除，且不可恢复！`,
      "警告",
      { type: "warning" }
    )
    await batchDeletePatientsApi(selectedRows.value.map(r => r.id))
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
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable filterable style="width:180px">
            <el-option v-for="item in PATHOGEN_RESULT_FILTER_OPTIONS" :key="item" :label="item" :value="item" />
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
        <el-form-item label="停止治疗原因">
          <el-select v-model="searchForm.stopTreatmentReason" placeholder="全部" clearable style="width:160px">
            <el-option
              v-for="item in STOP_TREATMENT_REASON_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
          v-permission="'patientManagement:history'"
          type="danger"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          删除
        </el-button>
        <el-button
          v-permission="'patientManagement:history'"
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
        <el-table-column prop="diagnosisResult" min-width="110" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="病原学结果"
              type="select"
              :options="pathogenFilterOptions"
              :model-value="columnFilters.diagnosisResult"
              @change="(v) => { setFilter('diagnosisResult', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ resolvePatientPathogenResult(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="诊断结果" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolvePatientDiagnosisResult(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="停止治疗原因" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatStopTreatmentReason(row.stopTreatmentReason, row.stopTreatmentReasonOther) }}
          </template>
        </el-table-column>
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
