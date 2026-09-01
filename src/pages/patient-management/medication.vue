<script lang="ts" setup>
import PatientMedicationDialog from "@@/components/PatientMedicationDialog.vue"
import PatientMedicationPickupDetailDialog from "@@/components/PatientMedicationPickupDetailDialog.vue"
import PatientMedicationPickupDialog from "@@/components/PatientMedicationPickupDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_FILTER_OPTIONS } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { confirmDangerDelete } from "@@/utils/listToolbar"
import {
  canEditMedicationPickup,
  formatMedicationPickupDrugs,
  formatMedicationPickupQuantities,
  PATIENT_MEDICATION_PAGE_PERMISSIONS,
  PATIENT_MEDICATION_PICKUP_COLUMN_PERMISSIONS,
  PATIENT_MEDICATION_PICKUP_PERMISSIONS,
  PATIENT_MEDICATION_PICKUP_VIEW_PERMISSIONS
} from "@@/utils/medicationPickup"
import { getPatientTransferStatusLabel, isPatientTransferLocked, resolveMedicationManagementUnit, resolvePatientDiagnosisResult, resolvePatientPathogenResult, resolveRegistrationNo } from "@@/utils/patient"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { useUserStore } from "@/pinia/stores/user"
import { batchDeletePatientsApi, deletePatientsByFilterApi, exportPatientMedicationsApi, getMedicationPickupListApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"
import { usePatientTableHeaderFilters } from "./composables/usePatientTableHeaderFilters"

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.userRole === 1)

/** 填写/修改领药 */
const canManagePickup = computed(() =>
  PATIENT_MEDICATION_PICKUP_PERMISSIONS.some(code => userStore.hasPermission(code))
)

/** 查看领药记录按钮 */
const canViewPickupRecords = computed(() =>
  PATIENT_MEDICATION_PICKUP_VIEW_PERMISSIONS.some(code => userStore.hasPermission(code))
)

/** 展示领药情况列 */
const canViewPickup = computed(() =>
  PATIENT_MEDICATION_PICKUP_COLUMN_PERMISSIONS.some(code => userStore.hasPermission(code))
)

const {
  paginationData,
  handleCurrentChange,
  handleSizeChange,
  getTableIndex,
  loading,
  tableData,
  total,
  searchForm,
  columnFilters,
  setFilter,
  toQueryParam,
  fetchData,
  handleSearch,
  handleReset
} = usePatientList(0, { medicationSearch: true })

const {
  genderFilterOptions,
  pathogenFilterOptions,
  populationTypeFilterOptions,
  loadGenderOptions,
  loadPopulationTypeOptions,
  loadMedicationUnitOptions,
  genderSourceValues,
  populationTypeSourceValues,
  medicationUnitSourceValues
} = usePatientTableHeaderFilters(0)

const medicationDialogVisible = ref(false)
const medicationRow = ref<any>(null)

const selectedRows = ref<any[]>([])
const exporting = ref(false)
const batchDeleting = ref(false)

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function buildListQueryParams() {
  const columnFiltersParam = toQueryParam()
  return {
    name: searchForm.name || undefined,
    idNumber: searchForm.idNumber || undefined,
    phone: searchForm.phone || undefined,
    diagnosisResult: searchForm.diagnosisResult || undefined,
    populationType: searchForm.populationType || undefined,
    medicationManagementUnit: searchForm.medicationManagementUnit || undefined,
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {}),
    ...extractDateRangeParams(searchForm.dateRange)
  }
}

async function handleExport(mode: "filtered" | "selected" = "filtered", ids?: string[]) {
  const isSelected = mode === "selected"
  const label = isSelected ? `选中的 ${ids!.length} 位患者` : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}服药管理数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportPatientMedicationsApi(isSelected ? { ids } : buildListQueryParams())
    downloadBlob(blob as unknown as Blob, "患者服药管理.xlsx")
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
    ElMessage.warning("请先勾选要导出的患者")
    return
  }
  handleExport("selected", ids)
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先勾选要删除的数据")
    return
  }
  const hasLocked = selectedRows.value.some(r => isPatientTransferLocked(r))
  if (hasLocked && !isSuperAdmin.value) {
    ElMessage.warning("选中记录包含已转出或转出待确认的患者，不可删除")
    return
  }
  const names = selectedRows.value.map(r => r.name).join("、")
  const forceTip = hasLocked && isSuperAdmin.value
    ? "选中含转出待确认/已转出记录，超级管理员强制删除将一并清理关联转出数据。"
    : ""
  try {
    await ElMessageBox.confirm(
      `${forceTip}确定删除选中的 ${selectedRows.value.length} 条记录（${names}）吗？关联的通知单、随访、服药等数据将一并删除，且不可恢复！`,
      hasLocked ? "超级管理员强制删除" : "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    batchDeleting.value = true
    await batchDeletePatientsApi(selectedRows.value.map(r => r.id))
    ElMessage.success(`成功删除 ${selectedRows.value.length} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除勾选失败")
  } finally {
    batchDeleting.value = false
  }
}

async function handleDeleteFiltered() {
  const ok = await confirmDangerDelete({
    title: "删除筛选结果",
    message: "确定删除当前筛选条件下的全部在管患者吗？关联的通知单、随访、服药等数据将一并删除，且不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deletePatientsByFilterApi(buildListQueryParams())
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch {
    ElMessage.error("删除筛选结果失败")
  } finally {
    batchDeleting.value = false
  }
}

const pickupDialogVisible = ref(false)
const pickupRow = ref<any>(null)
const editPickup = ref<Record<string, any> | null>(null)

const historyVisible = ref(false)
const historyList = ref<any[]>([])
const historyPatientName = ref("")
const historyPatient = ref<any>(null)
const historyDialogTitle = computed(() => `${historyPatientName.value} - 领药记录`)

const detailVisible = ref(false)
const detailRecord = ref<Record<string, any> | null>(null)

function openMedication(row: any) {
  medicationRow.value = row
  medicationDialogVisible.value = true
}

function hasPickupData(row: Record<string, any>) {
  return (row.medicationPickupCount ?? 0) > 0
}

function canAddPickup(row: Record<string, any>) {
  return row.archived !== 1 && !isPatientTransferLocked(row)
}

function openPickup(row: any) {
  pickupRow.value = row
  editPickup.value = null
  pickupDialogVisible.value = true
}

async function viewHistory(row: any) {
  historyPatient.value = row
  historyPatientName.value = row.name
  const { data } = await getMedicationPickupListApi(row.id)
  historyList.value = data || []
  historyVisible.value = true
}

async function refreshHistoryList() {
  if (!historyPatient.value) return
  const { data } = await getMedicationPickupListApi(historyPatient.value.id)
  historyList.value = data || []
}

function openEdit(record: Record<string, any>) {
  editPickup.value = record
  pickupRow.value = historyPatient.value
  pickupDialogVisible.value = true
}

async function onPickupSaved() {
  await refreshHistoryList()
  fetchData()
}

function viewDetail(record: Record<string, any>) {
  detailRecord.value = record
  detailVisible.value = true
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
        <el-form-item label="时间段">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="服药管理单位">
          <el-select
            v-model="searchForm.medicationManagementUnit"
            placeholder="全部"
            clearable
            filterable
            allow-create
            default-first-option
            style="width: 200px"
            @visible-change="(visible) => visible && loadMedicationUnitOptions()"
            @change="() => { setFilter('medicationManagementUnit', ''); handleSearch() }"
          >
            <el-option
              v-for="item in medicationUnitSourceValues"
              :key="item"
              :label="item"
              :value="item"
            />
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
          v-permission="[...PATIENT_MEDICATION_PAGE_PERMISSIONS]"
          type="primary"
          plain
          :loading="exporting"
          @click="handleExport('filtered')"
        >
          导出筛选结果
        </el-button>
        <el-button
          v-permission="'patientManagement:delete'"
          type="danger"
          plain
          :loading="batchDeleting"
          @click="handleDeleteFiltered"
        >
          删除筛选结果
        </el-button>
        <el-button
          v-permission="[...PATIENT_MEDICATION_PAGE_PERMISSIONS]"
          type="warning"
          :disabled="!selectedRows.length"
          :loading="exporting"
          @click="handleExportSelected"
        >
          导出勾选
        </el-button>
        <el-button
          v-permission="'patientManagement:delete'"
          type="danger"
          :disabled="!selectedRows.length"
          :loading="batchDeleting"
          @click="handleBatchDelete"
        >
          删除勾选
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
        <el-table-column prop="registrationNo" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="登记号"
              :model-value="columnFilters.registrationNo"
              @change="(v) => { setFilter('registrationNo', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ resolveRegistrationNo(row) || "-" }}
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
        <el-table-column label="诊断结果" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolvePatientDiagnosisResult(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="服药管理单位" min-width="140" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="服药管理单位"
              type="select"
              :source-values="medicationUnitSourceValues"
              :load-options="loadMedicationUnitOptions"
              :model-value="columnFilters.medicationManagementUnit || searchForm.medicationManagementUnit"
              @change="(v) => {
                searchForm.medicationManagementUnit = ''
                setFilter('medicationManagementUnit', v)
                handleSearch()
              }"
            />
          </template>
          <template #default="{ row }">
            {{ resolveMedicationManagementUnit(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="canViewPickup"
          prop="medicationEntryUnit"
          label="录入单位"
          min-width="120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.medicationEntryUnit || "-" }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="canViewPickup"
          prop="medicationEntryPerson"
          label="录入人员"
          min-width="100"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.medicationEntryPerson || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" :width="canViewPickup ? 100 : 120">
          <template #default="{ row }">
            <template v-if="!isPatientTransferLocked(row)">
              <el-button
                v-permission="[...PATIENT_MEDICATION_PAGE_PERMISSIONS]"
                type="primary"
                link
                size="small"
                :disabled="row.archived === 1"
                @click="openMedication(row)"
              >
                服药管理
              </el-button>
            </template>
            <el-tag
              v-else
              :type="row.archiveRemark === '已转出' ? 'info' : 'warning'"
              size="small"
            >
              {{ getPatientTransferStatusLabel(row.archiveRemark) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canViewPickup" label="领药情况" min-width="260" fixed="right">
          <template #default="{ row }">
            <div v-if="hasPickupData(row)" class="medication-pickup-cell">
              <div>共 {{ row.medicationPickupCount }} 次</div>
              <div v-if="row.medicationPickTime">
                最近：{{ row.medicationPickTime }}
              </div>
              <div v-if="row.medicationChemotherapy">
                药品：{{ row.medicationChemotherapy }}
              </div>
              <div v-if="row.medicationDrugForm">
                数量：{{ row.medicationDrugForm }}
              </div>
            </div>
            <span v-else class="text-gray-400">未录入</span>
            <div class="pickup-actions">
              <el-button
                v-if="canManagePickup && canAddPickup(row)"
                v-permission="[...PATIENT_MEDICATION_PICKUP_PERMISSIONS]"
                type="primary"
                link
                size="small"
                @click="openPickup(row)"
              >
                填写领药
              </el-button>
              <el-button
                v-if="canViewPickupRecords && hasPickupData(row)"
                v-permission="[...PATIENT_MEDICATION_PICKUP_VIEW_PERMISSIONS]"
                type="info"
                link
                size="small"
                @click="viewHistory(row)"
              >
                查看记录
              </el-button>
            </div>
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

    <PatientMedicationDialog
      v-model:visible="medicationDialogVisible"
      :patient-row="medicationRow"
      @success="fetchData"
    />

    <PatientMedicationPickupDialog
      v-if="pickupRow"
      v-model:visible="pickupDialogVisible"
      :patient-row="pickupRow"
      :initial-data="editPickup"
      @success="onPickupSaved"
      @update:visible="(v) => { if (!v) editPickup = null }"
    />

    <el-dialog
      v-model="historyVisible"
      :title="historyDialogTitle"
      width="1100px"
      append-to-body
    >
      <el-table :data="historyList" border stripe>
        <el-table-column prop="pickupSeq" label="第几次" width="80" />
        <el-table-column prop="pickupTime" label="领取时间" width="120" />
        <el-table-column label="药品及用量" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatMedicationPickupDrugs(row.drugs) }}
          </template>
        </el-table-column>
        <el-table-column label="领取数量" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatMedicationPickupQuantities(row.drugs, row.quantity, row.quantityUnit) || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="dispensingUnit" label="发药单位" min-width="120" show-overflow-tooltip />
        <el-table-column prop="entryUnit" label="录入单位" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.entryUnit || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="entryPerson" label="录入人员" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.entryPerson || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="canEditMedicationPickup(userStore.userRole, row) && !isPatientTransferLocked(historyPatient)"
              v-permission="[...PATIENT_MEDICATION_PICKUP_PERMISSIONS]"
              type="warning"
              link
              size="small"
              @click="openEdit(row)"
            >
              修改
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyList.length" description="暂无领药记录" />
    </el-dialog>

    <PatientMedicationPickupDetailDialog
      v-model:visible="detailVisible"
      :record="detailRecord"
      :patient-name="historyPatientName"
    />
  </div>
</template>

<style lang="scss" scoped>
.medication-pickup-cell {
  line-height: 1.6;
  font-size: 13px;
  margin-bottom: 4px;
}

.pickup-actions {
  margin-top: 2px;
}
</style>
