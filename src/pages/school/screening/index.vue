<script lang="ts" setup>
import RecommendCreateDialog from "@@/components/RecommendCreateDialog.vue"
import TableHeaderFilter from "@@/components/TableHeaderFilter.vue"
import TableHeaderHint from "@@/components/TableHeaderHint.vue"
import { useColumnDistinct } from "@@/composables/useColumnDistinct"
import { runImportWithIdentityConfirm } from "@@/composables/useImportIdentityConfirm"
import { MAX_PAGE_SIZE, usePagination } from "@@/composables/usePagination"
import { useServerColumnFilters } from "@@/composables/useServerColumnFilters"
import { useServerTableSort } from "@@/composables/useServerTableSort"
import { displaySchoolDiagnosis, getScreeningLatentStatusLabel, getScreeningLatentStatusTagType, isConfirmedPatientDiagnosis, SCHOOL_BOARDING_TYPE_OPTIONS, SCHOOL_CHEST_METHOD_OPTIONS, SCHOOL_CHEST_RESULT_OPTIONS, SCHOOL_DIAGNOSIS_EDIT_OPTIONS, SCHOOL_DIAGNOSIS_SEARCH_OPTIONS, SCHOOL_INFECTION_JUDGE_OPTIONS, SCHOOL_LAB_RESULT_OPTIONS, SCHOOL_SCREEN_METHOD_OPTIONS, SCHOOL_SCREENING_FIELD_HINTS, SCHOOL_SCREENING_FILL_INSTRUCTIONS, SCHOOL_TYPE_OPTIONS, toSchoolDiagnosisOfficial, YES_NO_HAVE_OPTIONS, YES_NO_OPTIONS } from "@@/constants/disease"
import { FORMAT_ISSUE_OPTIONS } from "@@/constants/format-issue"
import { PAGE_SIZE_OPTIONS } from "@@/constants/pagination"
import { confirmDangerDelete, confirmEditChange, triggerBlobDownload } from "@@/utils/listToolbar"
import { formatScreenResultDisplay } from "@@/utils/screening"
import { extractCreateTimeRangeParams } from "@@/utils/searchParams"
import { batchDeleteScreeningSchoolApi, createScreeningSchoolApi, deleteAllScreeningSchoolApi, deleteScreeningSchoolApi, deleteScreeningSchoolByFilterApi, exportScreeningSchoolApi, getScreeningSchoolColumnDistinctApi, getScreeningSchoolListApi, updateScreeningSchoolApi, uploadScreeningSchoolApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination({
  pageSizes: [...PAGE_SIZE_OPTIONS]
})
const { columnFilters, setFilter, clearFilters, toQueryParam } = useServerColumnFilters()
const { defaultSort, onSortChange, resetSort, toQueryParam: toSortQueryParam } = useServerTableSort()

const genderFilterOptions = [
  { text: "男", value: "男" },
  { text: "女", value: "女" }
]
const diagnosisFilterOptions = SCHOOL_DIAGNOSIS_SEARCH_OPTIONS.map(item => ({ text: item.label, value: item.value }))

const { load: loadDistinct, sourceValues: distinctValues } = useColumnDistinct(async (field) => {
  const { data } = await getScreeningSchoolColumnDistinctApi(field)
  return Array.isArray(data) ? data : []
})
const loadDistrictOptions = () => loadDistinct("district")
const loadGenderOptions = () => loadDistinct("gender")
const loadInfectionResultOptions = () => loadDistinct("infectionResult")

const loading = ref(false)
const batchDeleting = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  schoolName: "",
  district: "",
  entryUnit: "",
  creatorUsername: "",
  year: "" as string,
  isLatent: undefined as number | undefined,
  diagnosisFirst: "" as string,
  hasChestXray: "" as string,
  chestXrayResult: "" as string,
  molecularBiologyResult: "" as string,
  sputumCultureResult: "" as string,
  formatIssue: "" as string,
  entryTimeRange: [] as string[]
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getScreeningSchoolListApi({
      page: paginationData.currentPage,
      size: Math.min(paginationData.pageSize, MAX_PAGE_SIZE),
      ...buildListQueryParams()
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function buildListQueryParams() {
  const {
    entryUnit,
    creatorUsername,
    year,
    entryTimeRange,
    hasChestXray,
    chestXrayResult,
    molecularBiologyResult,
    sputumCultureResult,
    formatIssue,
    ...rest
  } = searchForm
  const columnFiltersParam = toQueryParam()
  return {
    ...rest,
    ...extractCreateTimeRangeParams(entryTimeRange),
    ...(year ? { year } : {}),
    ...(entryUnit ? { entryUnit } : {}),
    ...(creatorUsername ? { creatorUsername } : {}),
    ...(hasChestXray ? { hasChestXray } : {}),
    ...(chestXrayResult ? { chestXrayResult } : {}),
    ...(molecularBiologyResult ? { molecularBiologyResult } : {}),
    ...(sputumCultureResult ? { sputumCultureResult } : {}),
    ...(formatIssue ? { formatIssue } : {}),
    ...(columnFiltersParam ? { columnFilters: columnFiltersParam } : {}),
    ...toSortQueryParam()
  }
}

function handleSortChange(payload: { prop?: string, order?: "ascending" | "descending" | null }) {
  onSortChange(payload)
  handleSearch()
}

function handleSearch() {
  paginationData.currentPage = 1
  fetchData()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.schoolName = ""
  searchForm.district = ""
  searchForm.entryUnit = ""
  searchForm.creatorUsername = ""
  searchForm.year = ""
  searchForm.isLatent = undefined
  searchForm.diagnosisFirst = ""
  searchForm.hasChestXray = ""
  searchForm.chestXrayResult = ""
  searchForm.molecularBiologyResult = ""
  searchForm.sputumCultureResult = ""
  searchForm.formatIssue = ""
  searchForm.entryTimeRange = []
  clearFilters()
  resetSort()
  handleSearch()
}

function getRowClass({ row }: { row: any }) {
  return isConfirmedPatientDiagnosis(row) ? "confirmed-row" : ""
}

// 推介（预填筛查行 → 推介追踪）
const tierCareVisible = ref(false)
const tierCareRow = ref<any>(null)
function openTierCare(row: any) {
  tierCareRow.value = row
  tierCareVisible.value = true
}

/** Excel 上传 */
const uploadRef = ref()
const importResultVisible = ref(false)
const fillGuideVisible = ref(false)
const importResult = ref<{ successCount: number, missingIdCount?: number, errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

async function handleUpload(uploadFile: any) {
  try {
    const data = await runImportWithIdentityConfirm(uploadScreeningSchoolApi, uploadFile.raw)
    if (!data) return
    importResult.value = data
    importResultVisible.value = true
    if (data.successCount > 0) fetchData()
  } catch {
    ElMessage.error("上传失败")
  }
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function isRequestTimeout(err: any) {
  return err?.code === "ECONNABORTED" || String(err?.message ?? "").includes("超时")
}

/** 导出 Excel：filtered=筛选结果 / selected=勾选 / all=全部 */
async function handleExport(mode: "filtered" | "selected" | "all" = "filtered", ids?: string[]) {
  const isSelected = mode === "selected"
  const label = isSelected
    ? `选中的 ${ids!.length} 条`
    : mode === "all"
      ? "全部"
      : "当前筛选条件下的"
  try {
    await ElMessageBox.confirm(`确认导出${label}数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const res = await exportScreeningSchoolApi(
      isSelected ? { ids } : mode === "all" ? {} : buildListQueryParams()
    )
    triggerBlobDownload(
      new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }),
      "学校人群筛查数据.xlsx"
    )
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") {
      ElMessage.error(isRequestTimeout(err) ? "导出超时，请稍后重试或缩小导出范围" : "导出失败")
    }
  } finally {
    exporting.value = false
  }
}

function handleExportSelected() {
  const ids = selectedRows.value.map((item: any) => item.id).filter(Boolean)
  if (ids.length === 0) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport("selected", ids)
}

async function handleDeleteFiltered() {
  const ok = await confirmDangerDelete({
    title: "删除筛选结果",
    message: "确定删除当前筛选条件下的全部筛查记录吗？删除后关联数据将一并删除，且不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteScreeningSchoolByFilterApi(buildListQueryParams())
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    ElMessage.error(isRequestTimeout(err) ? "删除超时，请刷新后确认" : "删除筛选结果失败")
  } finally {
    batchDeleting.value = false
  }
}

async function handleDeleteAll() {
  const ok = await confirmDangerDelete({
    title: "删除全部",
    message: "确定删除权限范围内的全部筛查记录吗？此操作不可恢复！"
  })
  if (!ok) return
  batchDeleting.value = true
  try {
    const { data } = await deleteAllScreeningSchoolApi()
    ElMessage.success(`成功删除 ${data ?? 0} 条记录`)
    selectedRows.value = []
    handleReset()
  } catch (err: any) {
    ElMessage.error(isRequestTimeout(err) ? "删除超时，请刷新后确认" : "删除全部失败")
  } finally {
    batchDeleting.value = false
  }
}

/** 编辑弹窗 */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<Record<string, any>>({})
const editMode = ref<"create" | "edit">("edit")
const detailVisible = ref(false)
const detailRow = ref<any>(null)

function onScreenMethodChange() {
  if (editForm.value.screenMethod === "未查") {
    editForm.value.hasInfectionScreen = "否"
  } else if (editForm.value.screenMethod) {
    editForm.value.hasInfectionScreen = "是"
  }
}

function onChestMethodChange() {
  if (editForm.value.chestXrayMethod === "未查") {
    editForm.value.hasChestXray = "否"
  } else if (editForm.value.chestXrayMethod) {
    editForm.value.hasChestXray = "是"
  }
}

function syncSuspiciousSymptoms() {
  const yes = (v: string) => v === "有" || v === "是" || v === "1"
  const { symptomCough, symptomHemoptysis, symptomOther } = editForm.value
  if (yes(symptomCough) || yes(symptomHemoptysis) || yes(symptomOther)) {
    editForm.value.suspiciousSymptoms = "有"
  } else if (symptomCough || symptomHemoptysis || symptomOther) {
    editForm.value.suspiciousSymptoms = "无"
  }
}

function getEmptyEditForm() {
  return {
    year: "",
    reportingOrg: "",
    city: "",
    district: "",
    township: "",
    name: "",
    gender: "",
    age: undefined,
    idNumber: "",
    ethnicity: "",
    participatedScreening: "",
    householdAddress: "",
    schoolType: "",
    boardingType: "",
    schoolName: "",
    gradeName: "",
    className: "",
    tbHistory: "",
    closeContactHistory: "",
    suspiciousSymptoms: "",
    symptomCough: "",
    symptomHemoptysis: "",
    symptomOther: "",
    hasInfectionScreen: "",
    screenDate: "",
    screenMethod: "",
    screenResult: "",
    infectionResult: "",
    hasChestXray: "",
    chestXrayMethod: "",
    chestXrayDate: "",
    chestXrayResult: "",
    molecularBiologyResult: "",
    sputumCultureResult: "",
    diagnosisFirst: "",
    remark: ""
  }
}

function handleCreate() {
  editMode.value = "create"
  editForm.value = getEmptyEditForm()
  editVisible.value = true
}

function handleEdit(row: any) {
  editMode.value = "edit"
  editForm.value = { ...row, diagnosisFirst: toSchoolDiagnosisOfficial(row.diagnosisFirst) }
  editVisible.value = true
}

function viewDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

async function handleSave() {
  if (editMode.value === "edit") {
    const name = editForm.value.name?.trim() || "该筛查记录"
    const confirmed = await confirmEditChange(`「${name}」信息`)
    if (!confirmed) return
  }
  syncSuspiciousSymptoms()
  onScreenMethodChange()
  onChestMethodChange()
  editSaving.value = true
  try {
    if (editMode.value === "create") {
      await createScreeningSchoolApi(editForm.value)
      ElMessage.success("新增成功")
    } else {
      await updateScreeningSchoolApi(editForm.value.id, editForm.value)
      ElMessage.success("保存成功")
    }
    editVisible.value = false
    fetchData()
  } catch {
    ElMessage.error(editMode.value === "create" ? "新增失败" : "保存失败")
  } finally {
    editSaving.value = false
  }
}

/** 删除筛查记录（级联删除后续所有数据） */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」的筛查记录吗？删除后其对应的潜伏感染、患者管理等所有关联数据将一并删除，且不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    await deleteScreeningSchoolApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除失败")
  }
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    ElMessage.warning("请先勾选要删除的数据")
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 条筛查记录吗？删除后所有关联数据将一并删除，且不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    const ids = selectedRows.value.map((r: any) => r.id)
    batchDeleting.value = true
    await batchDeleteScreeningSchoolApi(ids)
    ElMessage.success(`成功删除 ${ids.length} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") {
      if (isRequestTimeout(err)) {
        ElMessage.warning("删除请求超时，数据可能已删除，正在刷新列表…")
        selectedRows.value = []
        fetchData()
      } else {
        ElMessage.error("批量删除失败")
      }
    }
  } finally {
    batchDeleting.value = false
  }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  () => {
    selectedRows.value = []
    fetchData()
  },
  { immediate: true }
)
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="学校名称">
          <el-input v-model="searchForm.schoolName" placeholder="请输入学校名称" clearable />
        </el-form-item>
        <el-form-item label="区县">
          <el-select
            v-model="searchForm.district"
            filterable
            clearable
            placeholder="请选择区县"
            style="width: 160px"
            @visible-change="(v: boolean) => v && loadDistrictOptions()"
          >
            <el-option v-for="item in distinctValues('district').value" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-date-picker
            v-model="searchForm.year"
            type="year"
            value-format="YYYY"
            placeholder="请选择年度"
            clearable
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item label="录入单位">
          <el-input v-model="searchForm.entryUnit" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="录入用户">
          <el-input v-model="searchForm.creatorUsername" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="格式问题">
          <el-select v-model="searchForm.formatIssue" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="item in FORMAT_ISSUE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否进行胸片检查">
          <el-select v-model="searchForm.hasChestXray" placeholder="全部" clearable style="width: 120px">
            <el-option label="是" value="是" />
            <el-option label="否" value="否" />
          </el-select>
        </el-form-item>
        <el-form-item label="胸片结果">
          <el-select v-model="searchForm.chestXrayResult" placeholder="全部" clearable style="width: 200px">
            <el-option v-for="item in SCHOOL_CHEST_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="分子生物学结果">
          <el-select v-model="searchForm.molecularBiologyResult" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in SCHOOL_LAB_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="痰培养结果">
          <el-select v-model="searchForm.sputumCultureResult" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in SCHOOL_LAB_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="判定结果">
          <el-select v-model="searchForm.isLatent" placeholder="全部" clearable style="width: 120px">
            <el-option label="待确诊" :value="1" />
            <el-option label="正常" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="诊断结果">
          <el-select v-model="searchForm.diagnosisFirst" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in SCHOOL_DIAGNOSIS_SEARCH_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="录入时间">
          <el-date-picker
            v-model="searchForm.entryTimeRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
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

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">学校人群筛查数据</span>
          <div class="flex gap-2 flex-wrap">
            <el-button type="success" @click="handleCreate">
              新增数据
            </el-button>
            <el-button :loading="exporting" @click="() => handleExport('filtered')">
              导出筛选结果
            </el-button>
            <el-button type="danger" plain :loading="batchDeleting" @click="handleDeleteFiltered">
              删除筛选结果
            </el-button>
            <el-button type="warning" :loading="exporting" :disabled="selectedRows.length === 0" @click="handleExportSelected">
              导出勾选
            </el-button>
            <el-button type="danger" :loading="batchDeleting" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
              删除勾选
            </el-button>
            <el-button @click="fillGuideVisible = true">
              填写说明
            </el-button>
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept=".xlsx,.xls"
              :on-change="handleUpload"
            >
              <el-button v-permission="'screening:upload'" type="primary">
                上传 Excel
              </el-button>
            </el-upload>
            <el-button :loading="exporting" @click="() => handleExport('all')">
              导出全部
            </el-button>
            <el-button type="danger" plain :loading="batchDeleting" @click="handleDeleteAll">
              删除全部
            </el-button>
          </div>
        </div>
      </template>

      <!-- 对齐 2026 秋季新生入学筛查表字段 -->
      <el-table
        v-loading="loading"
        class="screening-data-table"
        :data="tableData"
        border
        stripe
        max-height="600"
        row-key="id"
        :row-class-name="getRowClass"
        :default-sort="defaultSort"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" fixed />
        <el-table-column prop="creatorUsername" min-width="100" fixed sortable="custom">
          <template #header>
            <TableHeaderFilter
              label="录入用户"
              :model-value="columnFilters.creatorUsername"
              @change="(v) => { setFilter('creatorUsername', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" min-width="90" fixed sortable="custom">
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
              :source-values="distinctValues('gender').value"
              :load-options="loadGenderOptions"
              :model-value="columnFilters.gender"
              @change="(v) => { setFilter('gender', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" sortable="custom" />
        <el-table-column prop="idNumber" min-width="160">
          <template #header>
            <TableHeaderFilter
              label="身份证号"
              :model-value="columnFilters.idNumber"
              @change="(v) => { setFilter('idNumber', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年份" min-width="80" sortable="custom" />
        <el-table-column prop="reportingOrg" label="填报机构" min-width="110" show-overflow-tooltip />
        <el-table-column prop="district" min-width="90" sortable="custom">
          <template #header>
            <TableHeaderFilter
              label="县区"
              type="select"
              :source-values="distinctValues('district').value"
              :load-options="loadDistrictOptions"
              :model-value="columnFilters.district"
              @change="(v) => { setFilter('district', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="township" label="乡镇/街道" min-width="100" show-overflow-tooltip />
        <el-table-column prop="schoolType" min-width="100">
          <template #header>
            <TableHeaderHint label="类型" :hint="SCHOOL_SCREENING_FIELD_HINTS.schoolType" />
          </template>
        </el-table-column>
        <el-table-column prop="boardingType" min-width="100">
          <template #header>
            <TableHeaderHint label="是否寄宿制" :hint="SCHOOL_SCREENING_FIELD_HINTS.boardingType" />
          </template>
        </el-table-column>
        <el-table-column prop="schoolName" min-width="120" sortable="custom">
          <template #header>
            <TableHeaderFilter
              label="学校名称"
              :model-value="columnFilters.schoolName"
              @change="(v) => { setFilter('schoolName', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="gradeName" min-width="80">
          <template #header>
            <TableHeaderFilter
              label="年级"
              :model-value="columnFilters.gradeName"
              @change="(v) => { setFilter('gradeName', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="className" min-width="90">
          <template #header>
            <TableHeaderFilter
              label="班级"
              :model-value="columnFilters.className"
              @change="(v) => { setFilter('className', v); handleSearch() }"
            />
          </template>
        </el-table-column>
        <el-table-column prop="ethnicity" label="民族" />
        <el-table-column min-width="110">
          <template #header>
            <TableHeaderHint label="是否参加筛查" :hint="SCHOOL_SCREENING_FIELD_HINTS.participatedScreening" />
          </template>
          <template #default="{ row }">
            {{ row.participatedScreening || "-" }}
          </template>
        </el-table-column>
        <el-table-column min-width="110">
          <template #header>
            <TableHeaderHint label="既往结核病史" :hint="SCHOOL_SCREENING_FIELD_HINTS.tbHistory" />
          </template>
          <template #default="{ row }">
            {{ row.tbHistory || "-" }}
          </template>
        </el-table-column>
        <el-table-column min-width="110">
          <template #header>
            <TableHeaderHint label="肺结核接触史" :hint="SCHOOL_SCREENING_FIELD_HINTS.closeContactHistory" />
          </template>
          <template #default="{ row }">
            {{ row.closeContactHistory || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="结核病可疑症状">
          <el-table-column min-width="120">
            <template #header>
              <TableHeaderHint label="咳嗽咳痰≥两周" :hint="SCHOOL_SCREENING_FIELD_HINTS.symptomCough" />
            </template>
            <template #default="{ row }">
              {{ row.symptomCough || "-" }}
            </template>
          </el-table-column>
          <el-table-column min-width="100">
            <template #header>
              <TableHeaderHint label="咯血或血痰" :hint="SCHOOL_SCREENING_FIELD_HINTS.symptomHemoptysis" />
            </template>
            <template #default="{ row }">
              {{ row.symptomHemoptysis || "-" }}
            </template>
          </el-table-column>
          <el-table-column min-width="80">
            <template #header>
              <TableHeaderHint label="其他" :hint="SCHOOL_SCREENING_FIELD_HINTS.symptomOther" />
            </template>
            <template #default="{ row }">
              {{ row.symptomOther || "-" }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="感染筛查">
          <el-table-column prop="screenDate" label="感染筛查时间" min-width="110" sortable="custom" />
          <el-table-column prop="screenMethod" min-width="80">
            <template #header>
              <TableHeaderHint label="方法" :hint="SCHOOL_SCREENING_FIELD_HINTS.screenMethod" />
            </template>
          </el-table-column>
          <el-table-column min-width="120" show-overflow-tooltip>
            <template #header>
              <TableHeaderHint label="结果" :hint="SCHOOL_SCREENING_FIELD_HINTS.screenResult" />
            </template>
            <template #default="{ row }">
              {{ formatScreenResultDisplay(row.screenResult, row.screenMethod) || "-" }}
            </template>
          </el-table-column>
          <el-table-column prop="infectionResult" min-width="100">
            <template #header>
              <TableHeaderFilter
                label="判定结果"
                type="select"
                :hint="SCHOOL_SCREENING_FIELD_HINTS.infectionResult"
                :source-values="distinctValues('infectionResult').value"
                :load-options="loadInfectionResultOptions"
                :model-value="columnFilters.infectionResult"
                @change="(v) => { setFilter('infectionResult', v); handleSearch() }"
              />
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="胸部影像学">
          <el-table-column prop="chestXrayDate" label="胸片检查时间" min-width="110" />
          <el-table-column prop="chestXrayMethod" min-width="90">
            <template #header>
              <TableHeaderHint label="方法" :hint="SCHOOL_SCREENING_FIELD_HINTS.chestXrayMethod" />
            </template>
          </el-table-column>
          <el-table-column prop="chestXrayResult" min-width="140" show-overflow-tooltip>
            <template #header>
              <TableHeaderHint label="结果" :hint="SCHOOL_SCREENING_FIELD_HINTS.chestXrayResult" />
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column prop="molecularBiologyResult" min-width="120">
          <template #header>
            <TableHeaderHint label="分子生物学结果" :hint="SCHOOL_SCREENING_FIELD_HINTS.molecularBiologyResult" />
          </template>
        </el-table-column>
        <el-table-column prop="sputumCultureResult" min-width="100">
          <template #header>
            <TableHeaderHint label="痰培养结果" :hint="SCHOOL_SCREENING_FIELD_HINTS.sputumCultureResult" />
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisFirst" min-width="110">
          <template #header>
            <TableHeaderFilter
              label="筛查结果"
              type="select"
              :hint="SCHOOL_SCREENING_FIELD_HINTS.diagnosisFirst"
              :options="diagnosisFilterOptions"
              :model-value="columnFilters.diagnosisFirst"
              @change="(v) => { setFilter('diagnosisFirst', v); handleSearch() }"
            />
          </template>
          <template #default="{ row }">
            {{ displaySchoolDiagnosis(row.diagnosisFirst) || "—" }}
          </template>
        </el-table-column>
        <!-- 预防性治疗情况（结案进入历史患者后同步） -->
        <el-table-column label="潜伏感染者管理情况">
          <el-table-column prop="preventivePlan" label="预防性治疗方案" min-width="120" show-overflow-tooltip />
          <el-table-column prop="preventiveStartDate" label="预防性治疗开始时间（年月日）" min-width="150" />
          <el-table-column prop="preventiveEndDate" label="预防性治疗完成时间（年月日）" min-width="150" />
          <el-table-column prop="preventiveResult" label="预防性治疗结果" min-width="110" />
          <el-table-column prop="preventiveManager" label="预防性治疗期间随访管理人员" min-width="150" show-overflow-tooltip />
        </el-table-column>
        <el-table-column label="待确诊" fixed="right" min-width="90">
          <template #default="{ row }">
            <el-tag :type="getScreeningLatentStatusTagType(row)" size="small">
              {{ getScreeningLatentStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" fixed="right" min-width="200">
          <template #default="{ row }">
            <el-button type="info" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
            <el-button v-permission="['referral', 'referralManagement:create']" type="warning" link size="small" @click="openTierCare(row)">
              推介
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :page-sizes="paginationData.pageSizes"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? '新增筛查记录' : '编辑筛查记录'" width="960px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="150px" class="edit-form">
        <el-divider content-position="left">
          基本信息
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="年份">
              <el-input v-model="editForm.year" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="填报机构">
              <el-input v-model="editForm.reportingOrg" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市州">
              <el-input v-model="editForm.city" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="县区">
              <el-input v-model="editForm.district" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="乡镇/街道">
              <el-input v-model="editForm.township" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="editForm.schoolType" style="width:100%" clearable>
                <el-option v-for="item in SCHOOL_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否寄宿制">
              <el-select v-model="editForm.boardingType" style="width:100%" clearable>
                <el-option v-for="item in SCHOOL_BOARDING_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学校名称">
              <el-input v-model="editForm.schoolName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="姓名">
              <el-input v-model="editForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="editForm.gender" style="width:100%">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="身份证号">
              <el-input v-model="editForm.idNumber" placeholder="可填无" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="editForm.age" :min="0" :max="150" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="民族">
              <el-input v-model="editForm.ethnicity" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年级">
              <el-input v-model="editForm.gradeName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班级">
              <el-input v-model="editForm.className" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="户籍所在地">
              <el-input v-model="editForm.householdAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否参加筛查">
              <el-select v-model="editForm.participatedScreening" style="width:100%" clearable>
                <el-option v-for="item in YES_NO_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="既往结核病史">
              <el-select v-model="editForm.tbHistory" style="width:100%" clearable>
                <el-option v-for="item in YES_NO_HAVE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="肺结核接触史">
              <el-select v-model="editForm.closeContactHistory" style="width:100%" clearable>
                <el-option v-for="item in YES_NO_HAVE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          结核病可疑症状
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="咳嗽咳痰≥两周">
              <el-select v-model="editForm.symptomCough" style="width:100%" clearable>
                <el-option v-for="item in YES_NO_HAVE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="咯血或血痰">
              <el-select v-model="editForm.symptomHemoptysis" style="width:100%" clearable>
                <el-option v-for="item in YES_NO_HAVE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="其他">
              <el-select v-model="editForm.symptomOther" style="width:100%" clearable>
                <el-option v-for="item in YES_NO_HAVE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          感染筛查
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="感染筛查时间">
              <el-date-picker v-model="editForm.screenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="方法">
              <el-select v-model="editForm.screenMethod" style="width:100%" clearable @change="onScreenMethodChange">
                <el-option v-for="item in SCHOOL_SCREEN_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结果">
              <el-input v-model="editForm.screenResult" placeholder="PPD：横径×纵径；EC/IGRA：阳性/阴性" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="判定结果">
              <el-select v-model="editForm.infectionResult" style="width:100%" clearable>
                <el-option v-for="item in SCHOOL_INFECTION_JUDGE_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          胸部影像学与筛查结果
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="胸片检查时间">
              <el-date-picker v-model="editForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="影像方法">
              <el-select v-model="editForm.chestXrayMethod" style="width:100%" clearable @change="onChestMethodChange">
                <el-option v-for="item in SCHOOL_CHEST_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="影像结果">
              <el-select v-model="editForm.chestXrayResult" style="width:100%" clearable>
                <el-option v-for="item in SCHOOL_CHEST_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分子生物学结果">
              <el-select v-model="editForm.molecularBiologyResult" style="width:100%" clearable>
                <el-option v-for="item in SCHOOL_LAB_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="痰培养结果">
              <el-select v-model="editForm.sputumCultureResult" style="width:100%" clearable>
                <el-option v-for="item in SCHOOL_LAB_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="筛查结果">
              <el-select v-model="editForm.diagnosisFirst" style="width:100%" clearable>
                <el-option v-for="item in SCHOOL_DIAGNOSIS_EDIT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          备注
        </el-divider>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name || ''} - 详情`" width="960px">
      <el-descriptions v-if="detailRow" :column="3" border>
        <el-descriptions-item label="年份">
          {{ detailRow.year }}
        </el-descriptions-item>
        <el-descriptions-item label="填报机构">
          {{ detailRow.reportingOrg || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="市州">
          {{ detailRow.city }}
        </el-descriptions-item>
        <el-descriptions-item label="县区">
          {{ detailRow.district }}
        </el-descriptions-item>
        <el-descriptions-item label="乡镇/街道">
          {{ detailRow.township || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ detailRow.schoolType }}
        </el-descriptions-item>
        <el-descriptions-item label="是否寄宿制">
          {{ detailRow.boardingType || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="学校名称">
          {{ detailRow.schoolName }}
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          {{ detailRow.name }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ detailRow.gender }}
        </el-descriptions-item>
        <el-descriptions-item label="身份证号">
          {{ detailRow.idNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ detailRow.age }}
        </el-descriptions-item>
        <el-descriptions-item label="民族">
          {{ detailRow.ethnicity }}
        </el-descriptions-item>
        <el-descriptions-item label="年级">
          {{ detailRow.gradeName || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="班级">
          {{ detailRow.className }}
        </el-descriptions-item>
        <el-descriptions-item label="是否参加筛查">
          {{ detailRow.participatedScreening || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="既往结核病史">
          {{ detailRow.tbHistory }}
        </el-descriptions-item>
        <el-descriptions-item label="肺结核接触史">
          {{ detailRow.closeContactHistory }}
        </el-descriptions-item>
        <el-descriptions-item label="咳嗽咳痰≥两周">
          {{ detailRow.symptomCough || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="咯血或血痰">
          {{ detailRow.symptomHemoptysis || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="可疑症状其他">
          {{ detailRow.symptomOther || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查时间">
          {{ detailRow.screenDate }}
        </el-descriptions-item>
        <el-descriptions-item label="筛查方法">
          {{ detailRow.screenMethod }}
        </el-descriptions-item>
        <el-descriptions-item label="筛查结果">
          {{ formatScreenResultDisplay(detailRow.screenResult, detailRow.screenMethod) || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="判定结果">
          {{ detailRow.infectionResult }}
        </el-descriptions-item>
        <el-descriptions-item label="待确诊状态">
          {{ getScreeningLatentStatusLabel(detailRow) }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查时间">
          {{ detailRow.chestXrayDate }}
        </el-descriptions-item>
        <el-descriptions-item label="影像方法">
          {{ detailRow.chestXrayMethod || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="影像结果">
          {{ detailRow.chestXrayResult }}
        </el-descriptions-item>
        <el-descriptions-item label="分子生物学结果">
          {{ detailRow.molecularBiologyResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="痰培养结果">
          {{ detailRow.sputumCultureResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="筛查结果（诊断）">
          {{ displaySchoolDiagnosis(detailRow.diagnosisFirst) || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍所在地" :span="3">
          {{ detailRow.householdAddress }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">
          {{ detailRow.remark || "-" }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 推介弹窗 -->
    <RecommendCreateDialog
      v-model="tierCareVisible"
      :source="tierCareRow"
      default-crowd-category="学生"
    />

    <!-- 填写说明 -->
    <el-dialog v-model="fillGuideVisible" title="填写说明 — 2026年秋季新生入学结核病筛查记录表" width="720px">
      <p class="fill-guide-tip">
        说明与《2026年秋季新生入学结核病筛查记录表新》Excel 第 5 行一致。导入/新增时请按数字码或对应中文填写。点击表头带虚线下划线的列名也可查看该列说明。
      </p>
      <el-table :data="SCHOOL_SCREENING_FILL_INSTRUCTIONS" border stripe max-height="480">
        <el-table-column prop="field" label="字段" width="160" />
        <el-table-column prop="hint" label="填写说明" min-width="400" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="fillGuideVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert
        :title="`成功导入 ${importResult.successCount} 条数据`"
        type="success"
        :closable="false"
        class="mb-3"
      />
      <template v-if="importResult.errors.length > 0">
        <el-alert
          :title="importResult.missingIdCount
            ? `其中 ${importResult.missingIdCount} 条未填写身份证号已导入，其余问题见下表`
            : `发现 ${importResult.errors.length} 条数据存在格式问题（已照常导入，请核查）`"
          type="warning"
          :closable="false"
          class="mb-3"
        />
        <el-table :data="importResult.errors.map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" />
          <el-table-column prop="msg" label="错误信息" />
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

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
.edit-form {
  padding: 0 8px;

  :deep(.el-form-item__label) {
    white-space: nowrap;
  }
}

.fill-guide-tip {
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  font-size: 13px;
}
</style>

<style lang="scss">
.el-table .confirmed-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
