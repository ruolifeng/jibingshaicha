<script lang="ts" setup>
import ArchivedLatentRecordsActions from "@@/components/ArchivedLatentRecordsActions.vue"
import { usePagination } from "@@/composables/usePagination"
import { getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import {
  batchDeleteLatentApi,
  exportLatentHistoryApi,
  getLatentHistoryListApi
} from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

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
  endTime: ""
})

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...searchForm
    }
    if (!params.name) delete params.name
    if (!params.idNumber) delete params.idNumber
    if (!params.populationType) delete params.populationType
    if (!params.phone) delete params.phone
    if (!params.startTime) delete params.startTime
    if (!params.endTime) delete params.endTime
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
  Object.assign(searchForm, { name: "", idNumber: "", phone: "", populationType: "", startTime: "", endTime: "" })
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
      dateTo: searchForm.endTime || undefined
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
        <el-table-column prop="idNumber" label="证件号" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" show-overflow-tooltip />
        <el-table-column label="治疗阶段">
          <template #default="{ row }">
            {{ treatmentPhaseLabel(row.treatmentPhase) }}
          </template>
        </el-table-column>
        <el-table-column prop="archivedTime" label="归档时间" />
        <el-table-column label="操作" fixed="right" width="320">
          <template #default="{ row }">
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
