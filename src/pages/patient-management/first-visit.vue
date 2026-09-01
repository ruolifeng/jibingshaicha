<script lang="ts" setup>
import FirstVisitDetailDialog from "@@/components/FirstVisitDetailDialog.vue"
import PatientFirstVisitFormDialog from "@@/components/PatientFirstVisitFormDialog.vue"
import PrintFirstVisit from "@@/components/PrintFirstVisit.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { DRUG_RESISTANCE_OPTIONS, getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_FILTER_OPTIONS, SPUTUM_CULTURE_OPTIONS } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { getPatientTransferStatusLabel, isPatientTransferLocked, resolveMedicationManagementUnit, resolvePatientDiagnosisResult, resolvePatientPathogenResult, resolveRegistrationNo } from "@@/utils/patient"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { WarningFilled } from "@element-plus/icons-vue"
import { useUserStore } from "@/pinia/stores/user"
import { exportPatientFirstVisitsApi, getFirstVisitDetailApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"
import { usePatientTableHeaderFilters } from "./composables/usePatientTableHeaderFilters"

const userStore = useUserStore()
const canEditFirstVisitPerm = computed(() => userStore.hasPermission("patientManagement:firstVisit:edit"))

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
  defaultSort,
  handleSortChange,
  fetchData,
  handleSearch,
  handleReset
} = usePatientList(0, { firstVisitSearch: true })

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
    diagnosisResult: searchForm.diagnosisResult || undefined,
    populationType: searchForm.populationType || undefined,
    medicationManagementUnit: searchForm.medicationManagementUnit || undefined,
    sputumCulture: searchForm.sputumCulture || undefined,
    drugResistance: searchForm.drugResistance || undefined,
    dateFilterBy: "firstVisitFill",
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {}),
    ...extractDateRangeParams(searchForm.dateRange)
  }
}

async function handleExport(mode: "filtered" | "selected" = "filtered", ids?: string[]) {
  const isSelected = mode === "selected"
  const label = isSelected ? `选中的 ${ids!.length} 位患者` : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}首次入户随访信息吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportPatientFirstVisitsApi(isSelected ? { ids } : buildListQueryParams())
    downloadBlob(blob as unknown as Blob, "首次入户随访.xlsx")
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

const firstVisitDialogVisible = ref(false)
const firstVisitDetailVisible = ref(false)
const firstVisitRow = ref<any>(null)
const firstVisitDetailData = ref<Record<string, any> | null>(null)
const printVisitVisible = ref(false)
const printVisitData = ref<Record<string, any> | null>(null)

function openFirstVisit(row: any) {
  firstVisitRow.value = row
  firstVisitDialogVisible.value = true
}

async function viewFirstVisit(row: any) {
  try {
    const { data } = await getFirstVisitDetailApi(row.id)
    if (data) {
      firstVisitRow.value = row
      firstVisitDetailData.value = data
      firstVisitDetailVisible.value = true
    } else {
      ElMessage.info("暂无首次随访记录")
    }
  } catch { /* handled */ }
}

async function openPrintFirstVisit(row: any) {
  try {
    const { data } = await getFirstVisitDetailApi(row.id)
    if (!data) {
      ElMessage.info("暂无首次随访记录")
      return
    }
    firstVisitRow.value = row
    printVisitData.value = data
    printVisitVisible.value = true
  } catch { /* handled */ }
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
        <el-form-item label="痰培养">
          <el-select v-model="searchForm.sputumCulture" placeholder="全部" clearable filterable style="width:140px">
            <el-option v-for="item in SPUTUM_CULTURE_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="耐药情况">
          <el-select v-model="searchForm.drugResistance" placeholder="全部" clearable style="width:120px">
            <el-option v-for="item in DRUG_RESISTANCE_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="填写时间">
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
          v-permission="'patientManagement:firstVisit'"
          type="primary"
          plain
          :loading="exporting"
          @click="handleExport('filtered')"
        >
          导出筛选结果
        </el-button>
        <el-button
          v-permission="'patientManagement:firstVisit'"
          type="warning"
          :disabled="!selectedRows.length"
          :loading="exporting"
          @click="handleExportSelected"
        >
          导出勾选
        </el-button>
      </div>
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        row-key="id"
        :default-sort="defaultSort"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column type="index" label="#" :index="getTableIndex" />
        <el-table-column prop="registrationNo" min-width="120" show-overflow-tooltip sortable="custom">
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
        <el-table-column label="首次随访">
          <template #default="{ row }">
            <el-tag v-if="row.firstVisitStatus === 1" type="success" size="small">
              已完成
            </el-tag>
            <el-tag v-else-if="row.firstVisitStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <el-tag v-else type="warning" size="small">
              待填写
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="firstVisitSputumCulture" label="痰培养">
          <template #default="{ row }">
            {{ row.firstVisitSputumCulture || "—" }}
          </template>
        </el-table-column>
        <el-table-column label="耐药情况" min-width="110">
          <template #default="{ row }">
            <span
              v-if="row.firstVisitDrugResistance === '耐药'"
              class="drug-resistance-warn"
            >
              <span>耐药</span>
              <el-icon class="drug-resistance-warn__icon" :size="14">
                <WarningFilled />
              </el-icon>
            </span>
            <template v-else>
              {{ row.firstVisitDrugResistance || "—" }}
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <template v-if="!isPatientTransferLocked(row)">
              <el-button
                v-if="!row.hasFirstVisit"
                v-permission="'patientManagement:firstVisit:fill'"
                type="primary"
                link
                size="small"
                :disabled="row.archived === 1"
                @click="openFirstVisit(row)"
              >
                填写首次随访
              </el-button>
              <el-button
                v-else-if="row.firstVisitEditable !== false && canEditFirstVisitPerm"
                type="primary"
                link
                size="small"
                :disabled="row.archived === 1"
                @click="openFirstVisit(row)"
              >
                修改首次随访
              </el-button>
              <el-button
                v-else
                type="primary"
                link
                size="small"
                @click="viewFirstVisit(row)"
              >
                查看首次随访
              </el-button>
              <template v-if="row.hasFirstVisit">
                <el-button type="info" link size="small" @click="viewFirstVisit(row)">
                  查看
                </el-button>
                <el-button type="warning" link size="small" @click="openPrintFirstVisit(row)">
                  打印
                </el-button>
              </template>
            </template>
            <template v-else>
              <el-button v-if="row.hasFirstVisit" type="info" link size="small" @click="viewFirstVisit(row)">
                查看
              </el-button>
              <el-tag :type="row.archiveRemark === '已转出' ? 'info' : 'warning'" size="small">
                {{ getPatientTransferStatusLabel(row.archiveRemark) }}
              </el-tag>
            </template>
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

    <PatientFirstVisitFormDialog
      v-model:visible="firstVisitDialogVisible"
      :patient-row="firstVisitRow"
      @success="fetchData"
    />

    <FirstVisitDetailDialog
      v-model:visible="firstVisitDetailVisible"
      :visit-data="firstVisitDetailData"
      :patient-name="firstVisitRow?.name"
    />

    <PrintFirstVisit
      v-if="printVisitData"
      :visible="printVisitVisible"
      :visit-data="printVisitData"
      :patient-name="firstVisitRow?.name"
      @update:visible="printVisitVisible = $event"
    />
  </div>
</template>

<style scoped lang="scss">
.drug-resistance-warn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #f56c6c;
  font-weight: 600;
}

.drug-resistance-warn__icon {
  color: #f56c6c;
  flex-shrink: 0;
}
</style>
