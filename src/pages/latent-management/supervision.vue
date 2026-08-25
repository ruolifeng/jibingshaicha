<script lang="ts" setup>
import PrintSupervision from "@@/components/PrintSupervision.vue"
import SupervisionFormDetailDialog from "@@/components/SupervisionFormDetailDialog.vue"
import SupervisionFormDialog from "@@/components/SupervisionFormDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { getPopulationTypeLabel, getPopulationTypeTagType, getSuspectedConfirmDiagnosisLabel, normalizeLatentTreatmentPlan } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { isLatentTransferLocked } from "@@/utils/latent"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { canEditSupervisionForm, getSupervisionStatusLabel, mergeSupervisionProfileFields } from "@@/utils/supervisionForm"
import { useUserStore } from "@/pinia/stores/user"
import { deleteSupervisionApi, exportLatentSupervisionFormsApi, getLatentAggregateListApi, getLatentDetailApi, getSupervisionListApi } from "./apis"
import { useLatentTableHeaderFilters } from "./composables/useLatentTableHeaderFilters"

const userStore = useUserStore()
const route = useRoute()
const { paginationData, handleCurrentChange, handleSizeChange, getTableIndex } = usePagination()
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  phone: "",
  dateRange: [] as string[],
  creatorName: "",
  populationType: ""
})

const {
  genderFilterOptions,
  populationTypeFilterOptions,
  loadGenderOptions,
  loadPopulationTypeOptions,
  genderSourceValues,
  populationTypeSourceValues
} = useLatentTableHeaderFilters(() => searchForm.populationType)

async function fetchData() {
  loading.value = true
  try {
    const { dateRange, ...rest } = searchForm
    const columnFiltersParam = toQueryParam()
    const params: Record<string, any> = {
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      archived: 0,
      referralResult: "latent",
      trackingStatus: 1,
      dateFilterBy: "supervisionFill",
      ...rest,
      ...extractDateRangeParams(dateRange),
      ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {})
    }
    if (!params.populationType) delete params.populationType
    if (!params.phone) delete params.phone
    if (!params.creatorName) delete params.creatorName
    const { data } = await getLatentAggregateListApi(params)
    tableData.value = data.records ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  paginationData.currentPage = 1
  fetchData()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.phone = ""
  searchForm.dateRange = []
  searchForm.creatorName = ""
  searchForm.populationType = ""
  clearFilters()
  handleSearch()
}

/** 首页到期提醒「前往填写」带入姓名，返回是否发生变更 */
function syncNameFromRoute() {
  const name = typeof route.query.name === "string" ? route.query.name.trim() : ""
  if (!name || searchForm.name === name) return false
  searchForm.name = name
  paginationData.currentPage = 1
  return true
}

onMounted(() => {
  syncNameFromRoute()
  fetchData()
})
onActivated(() => {
  if (syncNameFromRoute()) fetchData()
})
watch(() => route.query.name, () => {
  if (syncNameFromRoute()) fetchData()
})

const selectedRows = ref<any[]>([])
const exporting = ref(false)

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function buildListQueryParams() {
  const columnFiltersParam = toQueryParam()
  return {
    name: searchForm.name || undefined,
    idNumber: searchForm.idNumber || undefined,
    phone: searchForm.phone || undefined,
    creatorName: searchForm.creatorName || undefined,
    populationType: searchForm.populationType || undefined,
    archived: 0,
    referralResult: "latent",
    trackingStatus: 1,
    dateFilterBy: "supervisionFill",
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {}),
    ...extractDateRangeParams(searchForm.dateRange)
  }
}

async function handleExport(mode: "filtered" | "selected" = "filtered", ids?: string[]) {
  const isSelected = mode === "selected"
  const label = isSelected ? `选中的 ${ids!.length} 条` : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}督导表数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportLatentSupervisionFormsApi(isSelected ? { ids } : buildListQueryParams())
    downloadBlob(blob as unknown as Blob, "督导表.xlsx")
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}

function handleExportSelected() {
  const ids = selectedRows.value.map(r => r.id).filter(Boolean)
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport("selected", ids)
}

watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

function getSupervisionStatusType(status?: number): "success" | "warning" | "info" {
  if (status === 2) return "success"
  if (status === 1) return "info"
  return "warning"
}

const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)

function openSupervision(row: any) {
  supervisionRow.value = row
  editRecord.value = null
  supervisionDialogVisible.value = true
}

const historyVisible = ref(false)
const historyList = ref<any[]>([])
const historyPatientName = ref("")
const historyRow = ref<any>(null)
const historyDialogTitle = computed(() => `${historyPatientName.value} - 督导表记录`)

const editDialogVisible = ref(false)
const editRecord = ref<Record<string, any> | null>(null)

const detailVisible = ref(false)
const detailData = ref<Record<string, any> | null>(null)

const printVisible = ref(false)
const printData = ref<Record<string, any> | null>(null)
const printPatientName = ref("")

async function viewHistory(row: any) {
  historyRow.value = row
  historyPatientName.value = row.name
  const { data } = await getSupervisionListApi(row.id)
  historyList.value = data || []
  historyVisible.value = true
}

async function refreshHistoryList() {
  if (!historyRow.value) return
  const { data } = await getSupervisionListApi(historyRow.value.id)
  historyList.value = data || []
}

function openEdit(record: Record<string, any>) {
  supervisionRow.value = historyRow.value
  editRecord.value = record
  editDialogVisible.value = true
}

async function onEditSaved() {
  editRecord.value = null
  await refreshHistoryList()
  fetchData()
}

async function resolveHistoryLatentProfile() {
  const row = historyRow.value
  if (!row?.id) return row
  try {
    const { data } = await getLatentDetailApi(row.id)
    if (data) return mergeSupervisionProfileFields(row, data)
  } catch { /* 回退列表行 */ }
  return row
}

async function viewDetail(row: Record<string, any>) {
  const profile = await resolveHistoryLatentProfile()
  detailData.value = mergeSupervisionProfileFields(row, profile)
  detailVisible.value = true
}

async function openPrint(row: Record<string, any>) {
  const profile = await resolveHistoryLatentProfile()
  printData.value = mergeSupervisionProfileFields(row, profile)
  printPatientName.value = historyPatientName.value
  printVisible.value = true
}

async function handleDelete(row: Record<string, any>) {
  if (isLatentTransferLocked(historyRow.value)) {
    ElMessage.warning("该记录已转出或转出待确认，不可删除")
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除第 ${row.formSeq ?? ""} 次督导表记录吗？删除后不可恢复。`,
      "删除确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    await deleteSupervisionApi(row.id)
    ElMessage.success("删除成功")
    await refreshHistoryList()
    if (!historyList.value.length) {
      historyVisible.value = false
    }
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error(err?.message || "删除失败")
  }
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
        <el-form-item label="填写督导表时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="录入者">
          <el-input v-model="searchForm.creatorName" placeholder="姓名或账号" clearable style="width:140px" />
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
      <div class="toolbar flex items-center justify-end gap-2 flex-wrap" style="margin-bottom: 12px">
        <el-button
          v-permission="'latentManagement:supervision'"
          type="primary"
          plain
          :loading="exporting"
          @click="handleExport('filtered')"
        >
          导出筛选结果
        </el-button>
        <el-button
          v-permission="'latentManagement:supervision'"
          type="warning"
          :disabled="!selectedRows.length"
          :loading="exporting"
          @click="handleExportSelected"
        >
          导出勾选
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="tableData"
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
        <el-table-column label="确认诊断" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ getSuspectedConfirmDiagnosisLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="督导表状态">
          <template #default="{ row }">
            <el-tag :type="getSupervisionStatusType(row.supervisionStatus)" size="small">
              {{ getSupervisionStatusLabel(row.supervisionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button
              v-permission="'latentManagement:supervision:fill'"
              type="primary"
              link
              size="small"
              :disabled="row.archived === 1"
              @click="openSupervision(row)"
            >
              填写督导表
            </el-button>
            <el-button type="info" link size="small" @click="viewHistory(row)">
              查看督导表记录
            </el-button>
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

    <SupervisionFormDialog
      v-if="supervisionRow && !editRecord"
      v-model="supervisionDialogVisible"
      :latent-row="supervisionRow"
      @success="fetchData"
    />

    <SupervisionFormDialog
      v-if="supervisionRow && editRecord"
      v-model="editDialogVisible"
      :latent-row="supervisionRow"
      :initial-data="editRecord"
      @success="onEditSaved"
    />

    <el-dialog
      v-model="historyVisible"
      :title="historyDialogTitle"
      width="900px"
      append-to-body
    >
      <el-table :data="historyList" border stripe>
        <el-table-column prop="formSeq" label="第几次" width="80" />
        <el-table-column prop="treatmentStartDate" label="开始治疗时间" />
        <el-table-column label="治疗方案" show-overflow-tooltip>
          <template #default="{ row }">
            {{ normalizeLatentTreatmentPlan(row.treatmentPlan) || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="managerName" label="管理人员" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            {{ getSupervisionStatusLabel(row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="canEditSupervisionForm(userStore.userRole, row)"
              v-permission="'latentManagement:supervision:edit'"
              type="warning"
              link
              size="small"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button type="info" link size="small" @click="openPrint(row)">
              打印
            </el-button>
            <el-button
              v-if="!isLatentTransferLocked(historyRow)"
              v-permission="'latentManagement:supervision:edit'"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyList.length" description="暂无督导表记录" />
    </el-dialog>

    <SupervisionFormDetailDialog
      v-model:visible="detailVisible"
      :form-data="detailData"
      :patient-name="historyPatientName"
    />

    <PrintSupervision
      v-if="printData"
      :visible="printVisible"
      :data="printData"
      @update:visible="printVisible = $event"
    />
  </div>
</template>
