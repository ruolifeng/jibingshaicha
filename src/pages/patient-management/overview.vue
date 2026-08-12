<script lang="ts" setup>
import { confirmReferralApi, getReferralListApi } from "@@/apis/referral"
import ConfirmReferralDialog from "@@/components/ConfirmReferralDialog.vue"
import PatientRecordDetailDialog from "@@/components/PatientRecordDetailDialog.vue"
import PatientRecordEditDialog from "@@/components/PatientRecordEditDialog.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import { runImportWithIdentityConfirm } from "@@/composables/useImportIdentityConfirm"
import { getLatentPopulationDisplayLabel, getPopulationTypeTagType, LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS, NOTICE_STATUS_MAP, PATHOGEN_RESULT_FILTER_OPTIONS } from "@@/constants/disease"
import { FORMAT_ISSUE_OPTIONS } from "@@/constants/format-issue"
import { PATIENT_MANUAL_IMPORT_FIELDS } from "@@/constants/patient-import"
import { downloadBlob } from "@@/utils/download"
import { confirmDangerDelete } from "@@/utils/listToolbar"
import { getPatientTransferStatusLabel, isPatientTransferLocked, isPatientTransferPending, isRetreatmentPatient, resolveMedicationManagementUnit, resolvePatientDiagnosisResult, resolvePatientPathogenResult, resolveRegistrationNo, resolveTreatmentClass } from "@@/utils/patient"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { useUserStore } from "@/pinia/stores/user"
import {
  archivePatientApi,
  batchDeletePatientsApi,
  deletePatientsByFilterApi,
  downloadPatientTemplateApi,
  exportAllPatientsApi,
  getPatientColumnDistinctApi,
  importPatientApi
} from "./apis"
import { usePatientList } from "./composables/usePatientList"

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.userRole === 1)

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
} = usePatientList(0, { overviewSearch: true })

const genderFilterOptions = [
  { text: "男", value: "男" },
  { text: "女", value: "女" }
]
const pathogenFilterOptions = PATHOGEN_RESULT_FILTER_OPTIONS.map(item => ({ text: item, value: item }))
const populationTypeFilterOptions = [
  { text: "学生筛查", value: "school" },
  { text: "重点人群", value: "keyPopulation" },
  { text: "疫情筛查", value: "regular" },
  { text: "大疫情", value: "epidemic" },
  { text: "推介", value: "referral" },
  { text: "密接", value: "closeContact" },
  { text: "专病网", value: "specialDisease" }
]

/** 表头 Excel 式：服务端实际去重值 */
const pathogenSourceValues = ref<string[]>([])
const genderSourceValues = ref<string[]>([])
const populationTypeSourceValues = ref<string[]>([])
const medicationUnitSourceValues = ref<string[]>([])

async function loadColumnDistinct(field: string, target: Ref<string[]>) {
  try {
    const { data } = await getPatientColumnDistinctApi(field, 0)
    target.value = Array.isArray(data) ? data : []
  } catch {
    // 接口失败时仍可用预设选项筛选
  }
}

const loadGenderOptions = () => loadColumnDistinct("gender", genderSourceValues)
const loadPathogenOptions = () => loadColumnDistinct("diagnosisResult", pathogenSourceValues)
const loadPopulationTypeOptions = () => loadColumnDistinct("populationType", populationTypeSourceValues)
const loadMedicationUnitOptions = () => loadColumnDistinct("medicationManagementUnit", medicationUnitSourceValues)

watch(() => searchForm.populationType, (val) => {
  if (val !== "keyPopulation") {
    searchForm.keyPopulationSubCategories = []
  }
})

const detailVisible = ref(false)
const editVisible = ref(false)
const currentId = ref<string | null>(null)
const exporting = ref(false)
const batchDeleting = ref(false)
const importing = ref(false)
const templateDownloading = ref(false)
const importDialogVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, missingIdCount?: number, errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function buildListQueryParams() {
  const columnFiltersParam = toQueryParam()
  return {
    name: searchForm.name || undefined,
    idNumber: searchForm.idNumber || undefined,
    phone: searchForm.phone || undefined,
    currentAddress: searchForm.currentAddress || undefined,
    diagnosisResult: searchForm.diagnosisResult || undefined,
    populationType: searchForm.populationType || undefined,
    medicationManagementUnit: searchForm.medicationManagementUnit || undefined,
    creatorUsername: searchForm.creatorUsername || undefined,
    crowdCategory: searchForm.keyPopulationSubCategories.length
      ? searchForm.keyPopulationSubCategories.join(",")
      : undefined,
    formatIssue: searchForm.formatIssue || undefined,
    dateFilterBy: "registrationDate",
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {}),
    ...extractDateRangeParams(searchForm.dateRange)
  }
}

function openDetail(row: any) {
  currentId.value = row.id
  detailVisible.value = true
}

function openCreate() {
  currentId.value = null
  editVisible.value = true
}

function openEdit(row: any) {
  currentId.value = row.id
  editVisible.value = true
}

const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)

function openReferral(row: any) {
  referralRow.value = row
  referralDialogVisible.value = true
}

function medicationStatusTagType(status?: string) {
  if (status === "已完成") return "success"
  if (status === "进行中") return "info"
  return "warning"
}

async function handleArchive(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认将患者「${row.name}」归档？归档后将移入「历史患者」。`,
      "归档确认",
      { type: "warning", confirmButtonText: "确认归档", cancelButtonText: "取消" }
    )
    await archivePatientApi(row.id)
    ElMessage.success("归档成功，已移入历史患者")
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error(err?.message || "归档失败")
  }
}

/** 导出：filtered=筛选结果 / selected=勾选 */
async function handleExport(mode: "filtered" | "selected" = "filtered", ids?: string[]) {
  const isSelected = mode === "selected"
  const label = isSelected ? `选中的 ${ids!.length} 条` : "当前筛选条件下的"
  if (!isSelected && total.value === 0) {
    ElMessage.warning("当前没有在管患者数据，将导出仅含表头的空表")
  }
  try {
    await ElMessageBox.confirm(`确认导出${label}数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportAllPatientsApi(isSelected ? { ids } : buildListQueryParams())
    downloadBlob(blob as unknown as Blob, "在管患者信息总表.xlsx")
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

function openImportDialog() {
  importResult.value = { successCount: 0, errors: [] }
  importDialogVisible.value = true
}

async function handleDownloadTemplate() {
  templateDownloading.value = true
  try {
    const blob = await downloadPatientTemplateApi()
    downloadBlob(blob as unknown as Blob, "在管患者导入模板.xlsx")
    ElMessage.success("模板下载成功")
  } catch {
    ElMessage.error("模板下载失败")
  } finally {
    templateDownloading.value = false
  }
}

async function handleImport(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) return
  if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
    ElMessage.error("请上传 .xlsx 或 .xls 文件")
    return
  }
  importing.value = true
  try {
    const data = await runImportWithIdentityConfirm(importPatientApi, file)
    if (!data) return
    importResult.value = data ?? { successCount: 0, errors: [] }
    importResultVisible.value = true
    importDialogVisible.value = false
    if (data.successCount > 0) {
      fetchData()
    }
  } catch {
    ElMessage.error("导入失败")
  } finally {
    importing.value = false
  }
}

const confirmTransferDialogVisible = ref(false)
const confirmTransferPending = ref<{ id: string, name: string } | null>(null)
const confirmTransferLoading = ref(false)

async function handleAdminConfirmTransfer(row: any) {
  if (!isPatientTransferPending(row)) return
  try {
    const { data } = await getReferralListApi(row.id, "patient_aggregate")
    const pending = (data ?? []).find((r: { status: number }) => r.status === 1)
    if (!pending) {
      ElMessage.warning("未找到待确认的转出记录")
      return
    }
    await ElMessageBox.confirm(
      `确定代接收方确认接收患者「${row.name}」的转出信息吗？确认后将复制患者数据至接收方并在本机构标记为已转出。`,
      "代确认接收转出",
      { type: "warning" }
    )
    confirmTransferPending.value = { id: pending.id, name: row.name }
    confirmTransferDialogVisible.value = true
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("代确认失败")
  }
}

async function submitAdminConfirmTransfer(actualReferralDate: string) {
  if (!confirmTransferPending.value) return
  confirmTransferLoading.value = true
  try {
    await confirmReferralApi(confirmTransferPending.value.id, actualReferralDate)
    ElMessage.success("已代接收方确认转出")
    confirmTransferDialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error("代确认失败")
  } finally {
    confirmTransferLoading.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
      :title="`当前在管患者共 ${total} 人`"
    />

    <el-card class="search-wrapper" shadow="never">
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="病原学结果">
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable filterable style="width: 140px">
            <el-option v-for="item in PATHOGEN_RESULT_FILTER_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="登记日期">
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
          >
            <el-option
              v-for="item in medicationUnitSourceValues"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="录入用户">
          <el-input v-model="searchForm.creatorUsername" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="格式问题">
          <el-select v-model="searchForm.formatIssue" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="item in FORMAT_ISSUE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="住址">
          <el-input v-model="searchForm.currentAddress" placeholder="请输入现住址" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width: 140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="疫情筛查" value="regular" />
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
            <el-option label="密接" value="closeContact" />
            <el-option label="专病网" value="specialDisease" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="searchForm.populationType === 'keyPopulation'" label="重点人群分类">
          <el-select
            v-model="searchForm.keyPopulationSubCategories"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="item in LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS"
              :key="item"
              :label="item"
              :value="item"
            />
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

    <el-card shadow="never" style="margin-top: 10px">
      <div class="toolbar flex items-center justify-end gap-2 flex-wrap" style="margin-bottom: 12px">
        <el-button
          v-permission="'patientManagement:overview'"
          type="success"
          @click="openCreate"
        >
          新增
        </el-button>
        <el-button
          v-permission="'patientManagement:overview'"
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
          v-permission="'patientManagement:overview'"
          type="warning"
          :disabled="selectedRows.length === 0"
          :loading="exporting"
          @click="handleExportSelected"
        >
          导出勾选
        </el-button>
        <el-button
          v-permission="'patientManagement:delete'"
          type="danger"
          :disabled="selectedRows.length === 0"
          :loading="batchDeleting"
          @click="handleBatchDelete"
        >
          删除勾选
        </el-button>
        <el-button
          v-permission="'patientManagement:overview'"
          type="primary"
          @click="openImportDialog"
        >
          导入
        </el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        row-key="id"
        :default-sort="defaultSort"
        :row-class-name="({ row }) => (isRetreatmentPatient(row) ? 'retreatment-row' : '')"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column type="index" label="#" :index="getTableIndex" />
        <el-table-column prop="registrationNo" label="登记号" min-width="120" show-overflow-tooltip sortable="custom">
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
          <template #default="{ row }">
            <span :class="{ 'text-red-600 font-semibold': isRetreatmentPatient(row) }">
              {{ row.name }}
            </span>
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
        <el-table-column prop="currentAddress" min-width="140" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="现住址"
              :model-value="columnFilters.currentAddress"
              @change="(v) => { setFilter('currentAddress', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisResult" min-width="120" show-overflow-tooltip>
          <template #header>
            <TableHeaderFilter
              label="病原学结果"
              type="select"
              :options="pathogenFilterOptions"
              :source-values="pathogenSourceValues"
              :load-options="loadPathogenOptions"
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
        <el-table-column prop="creatorUsername" min-width="100">
          <template #header>
            <TableHeaderFilter
              label="录入用户"
              :model-value="columnFilters.creatorUsername"
              @change="(v) => { setFilter('creatorUsername', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column label="治疗分类" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'text-red-600 font-semibold': isRetreatmentPatient(row) }">
              {{ resolveTreatmentClass(row) || "-" }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="服药管理单位" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveMedicationManagementUnit(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-tag v-if="row.noticeStatus === 2" type="success" size="small">
              已确认
            </el-tag>
            <el-tag v-else-if="row.noticeStatus === 1" type="warning" size="small">
              {{ NOTICE_STATUS_MAP[1] }}
            </el-tag>
            <el-tag v-else-if="row.noticeStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <span v-else class="text-gray-400">未发送</span>
          </template>
        </el-table-column>
        <el-table-column label="首次随访">
          <template #default="{ row }">
            <el-tag :type="row.hasFirstVisit ? 'success' : 'warning'" size="small">
              {{ row.hasFirstVisit ? "已完成" : "待填写" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="后续随访" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="(row.followUpCount ?? 0) > 0" type="success" size="small">
              已完成 {{ row.followUpCount }} 次
            </el-tag>
            <el-tag v-else type="warning" size="small">
              待填写
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服药管理完成情况" min-width="130">
          <template #default="{ row }">
            <el-tag
              :type="medicationStatusTagType(row.medicationManagementStatus)"
              size="small"
            >
              {{ row.medicationManagementStatus || "待填写" }}
            </el-tag>
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
              {{ getLatentPopulationDisplayLabel(row.populationType, row.crowdCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="转出状态" width="110">
          <template #default="{ row }">
            <el-tag
              v-if="getPatientTransferStatusLabel(row.archiveRemark)"
              :type="row.archiveRemark === '已转出' ? 'info' : 'warning'"
              size="small"
            >
              {{ getPatientTransferStatusLabel(row.archiveRemark) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="isSuperAdmin && isPatientTransferPending(row)"
              type="success"
              link
              size="small"
              @click="handleAdminConfirmTransfer(row)"
            >
              代确认接收
            </el-button>
            <el-button
              v-if="!isPatientTransferLocked(row)"
              v-permission="'patientManagement:edit'"
              type="warning"
              link
              size="small"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-if="!isPatientTransferLocked(row)"
              v-permission="'patientManagement:referral'"
              type="info"
              link
              size="small"
              @click="openReferral(row)"
            >
              转出
            </el-button>
            <el-button
              v-if="!isPatientTransferLocked(row)"
              v-permission="'patientManagement:edit'"
              type="danger"
              link
              size="small"
              @click="handleArchive(row)"
            >
              归档
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

    <PatientRecordDetailDialog
      v-model:visible="detailVisible"
      :patient-id="currentId"
    />
    <PatientRecordEditDialog
      v-model:visible="editVisible"
      :patient-id="currentId"
      @success="fetchData"
    />

    <ReferralDialog
      v-if="referralRow"
      v-model="referralDialogVisible"
      :biz-id="referralRow.id"
      biz-type="patient_aggregate"
      module-type="patient"
      :population-type="referralRow.populationType"
      :subject-name="referralRow.name || ''"
      @success="fetchData"
    />

    <ConfirmReferralDialog
      v-model="confirmTransferDialogVisible"
      :subject-name="confirmTransferPending?.name"
      :loading="confirmTransferLoading"
      @confirm="submitAdminConfirmTransfer"
    />

    <el-dialog v-model="importDialogVisible" title="批量导入在管患者" width="600px">
      <el-alert
        type="info"
        :closable="false"
        class="mb-3"
        title="请先下载模板，按表头填写数据后上传。字段与「新增」表单一致。"
      />
      <div class="mb-3">
        <p class="text-sm text-gray-500 mb-2">
          模板包含字段：{{ PATIENT_MANUAL_IMPORT_FIELDS.join("、") }}
        </p>
        <p class="text-sm text-gray-500">
          数据来源可填写：学生筛查、重点人群、疫情筛查、大疫情、推介、密接、专病网
        </p>
        <p class="text-sm text-gray-500 mt-2">
          治疗分类含「复治」的患者将在总览标红；证件号、联系电话列建议设为文本格式
        </p>
      </div>
      <div class="flex gap-2 mb-4">
        <el-button type="success" :loading="templateDownloading" @click="handleDownloadTemplate">
          下载模板
        </el-button>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx,.xls"
          :on-change="handleImport"
        >
          <el-button type="primary" :loading="importing">
            选择文件并导入
          </el-button>
        </el-upload>
      </div>
    </el-dialog>

    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert
        v-if="importResult.successCount > 0"
        :title="`成功导入 ${importResult.successCount} 条数据`"
        type="success"
        :closable="false"
        class="mb-3"
      />
      <el-alert
        v-else
        title="未成功导入任何数据，请检查模板与填写内容"
        type="warning"
        :closable="false"
        class="mb-3"
      />
      <template v-if="(importResult.errors?.length ?? 0) > 0">
        <el-alert
          :title="importResult.missingIdCount
            ? `其中 ${importResult.missingIdCount} 条未填写身份证号已导入，其余问题见下表`
            : `发现 ${importResult.errors?.length ?? 0} 条数据存在问题`"
          type="warning"
          :closable="false"
          class="mb-3"
        />
        <el-table :data="(importResult.errors ?? []).map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" width="60" />
          <el-table-column prop="msg" label="说明" />
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
:deep(.retreatment-row) {
  --el-table-tr-bg-color: #fef2f2;
}
</style>
