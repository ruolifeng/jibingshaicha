<script lang="ts" setup>
import ReferralDialog from "@@/components/ReferralDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { getScreeningLatentStatusLabel, getScreeningLatentStatusTagType, isConfirmedPatientDiagnosis, SCREENING_DIAGNOSIS_EDIT_OPTIONS, SCREENING_DIAGNOSIS_SEARCH_OPTIONS } from "@@/constants/disease"
import { formatScreenResultDisplay } from "@@/utils/screening"
import { extractCreateTimeRangeParams } from "@@/utils/searchParams"
import { batchDeleteScreeningSchoolApi, createScreeningSchoolApi, deleteScreeningSchoolApi, exportScreeningSchoolApi, getScreeningSchoolListApi, updateScreeningSchoolApi, uploadScreeningSchoolApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  schoolName: "",
  district: "",
  phone: "",
  entryUnit: "",
  year: "" as string,
  isLatent: undefined as number | undefined,
  diagnosisFirst: "" as string,
  entryTimeRange: [] as string[]
})

async function fetchData() {
  loading.value = true
  try {
    const { entryUnit, year, entryTimeRange, ...rest } = searchForm
    const { data } = await getScreeningSchoolListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...rest,
      ...extractCreateTimeRangeParams(entryTimeRange),
      ...(year ? { year } : {}),
      ...(entryUnit ? { entryUnit } : {})
    })
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
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.schoolName = ""
  searchForm.district = ""
  searchForm.phone = ""
  searchForm.entryUnit = ""
  searchForm.year = ""
  searchForm.isLatent = undefined
  searchForm.diagnosisFirst = ""
  searchForm.entryTimeRange = []
  handleSearch()
}

function getRowClass({ row }: { row: any }) {
  return isConfirmedPatientDiagnosis(row) ? "confirmed-row" : ""
}

// 转出
const tierCareVisible = ref(false)
const tierCareRow = ref<any>(null)
function openTierCare(row: any) {
  tierCareRow.value = row
  tierCareVisible.value = true
}

/** Excel 上传 */
const uploadRef = ref()
const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

async function handleUpload(uploadFile: any) {
  try {
    const { data } = await uploadScreeningSchoolApi(uploadFile.raw)
    importResult.value = data
    importResultVisible.value = true
    fetchData()
  } catch {
    ElMessage.error("上传失败")
  }
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

/** 导出 Excel（支持导出全部或勾选项） */
async function handleExport(ids?: number[]) {
  try {
    await ElMessageBox.confirm("确认导出当前选择的数据吗？", "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    const res = await exportScreeningSchoolApi(ids)
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "学校人群筛查数据.xlsx"
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("导出失败")
  }
}

function handleExportSelected() {
  const ids = selectedRows.value.map((item: any) => item.id).filter(Boolean)
  if (ids.length === 0) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport(ids)
}

/** 编辑弹窗 */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<Record<string, any>>({})
const editMode = ref<"create" | "edit">("edit")
const detailVisible = ref(false)
const detailRow = ref<any>(null)

function getEmptyEditForm() {
  return {
    year: "",
    city: "",
    district: "",
    name: "",
    gender: "",
    birthDate: "",
    age: undefined,
    idType: "",
    idNumber: "",
    ethnicity: "",
    phone: "",
    householdAddress: "",
    currentAddress: "",
    schoolType: "",
    schoolName: "",
    className: "",
    tbHistory: "",
    closeContactHistory: "",
    suspiciousSymptoms: "",
    hasInfectionScreen: "",
    screenDate: "",
    screenMethod: "",
    screenResult: "",
    infectionResult: "",
    hasChestXray: "",
    chestXrayDate: "",
    chestXrayResult: "",
    sputumSmearResult: "",
    molecularBiologyResult: "",
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
  editForm.value = { ...row }
  editVisible.value = true
}

function viewDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

async function handleSave() {
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
    await batchDeleteScreeningSchoolApi(ids)
    ElMessage.success(`成功删除 ${ids.length} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("批量删除失败")
  }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
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
          <el-input v-model="searchForm.district" placeholder="请输入区县" clearable />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入联系电话" clearable />
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
        <el-form-item label="判定结果">
          <el-select v-model="searchForm.isLatent" placeholder="全部" clearable style="width: 120px">
            <el-option label="待确诊" :value="1" />
            <el-option label="正常" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="诊断结果">
          <el-select v-model="searchForm.diagnosisFirst" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in SCREENING_DIAGNOSIS_SEARCH_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
          <div class="flex gap-2">
            <el-button type="success" @click="handleCreate">
              新增数据
            </el-button>
            <el-button @click="() => handleExport()">
              导出全部
            </el-button>
            <el-button type="warning" :disabled="selectedRows.length === 0" @click="handleExportSelected">
              导出勾选
            </el-button>
            <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
              批量删除
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
          </div>
        </div>
      </template>

      <!-- V4：移除胸片/诊断/痰涂片/分子生物学列（已移至潜伏感染追踪阶段录入），新增预防性治疗完成情况列 -->
      <el-table v-loading="loading" class="screening-data-table" :data="tableData" border stripe max-height="600" row-key="id" :row-class-name="getRowClass" @selection-change="handleSelectionChange">
        <el-table-column type="selection" fixed />
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="district" label="区县" />
        <el-table-column prop="schoolName" label="学校名称" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="schoolType" label="学校类型" />
        <el-table-column prop="ethnicity" label="民族" />
        <el-table-column prop="tbHistory" label="既往结核病史" />
        <el-table-column prop="closeContactHistory" label="密切接触史" />
        <el-table-column prop="suspiciousSymptoms" label="可疑症状" />
        <el-table-column label="学校人群感染筛查情况">
          <el-table-column prop="hasInfectionScreen" label="是否进行感染筛" min-width="100" />
          <el-table-column prop="screenDate" label="感染筛查日期" min-width="110" />
          <el-table-column prop="screenMethod" label="方法" min-width="80" />
          <el-table-column label="结果（PPD：mmXmm；EC及IGRA：阳性/阴性）" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              {{ formatScreenResultDisplay(row.screenResult, row.screenMethod) || "-" }}
            </template>
          </el-table-column>
          <el-table-column prop="infectionResult" label="感染筛查结果" min-width="110" />
        </el-table-column>
        <el-table-column label="学校人群胸片检查">
          <el-table-column prop="hasChestXray" label="是否进行胸片检查" min-width="120" />
          <el-table-column prop="chestXrayDate" label="胸片检查日期" min-width="110" />
          <el-table-column prop="chestXrayResult" label="胸片结果" min-width="90" />
        </el-table-column>
        <el-table-column prop="sputumSmearResult" label="痰涂片结果" />
        <el-table-column prop="molecularBiologyResult" label="分子生物学结果" />
        <el-table-column prop="diagnosisFirst" label="诊断结果" />
        <!-- 预防性治疗情况（督导表归档后同步） -->
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
            <el-button v-permission="'referral'" type="warning" link size="small" @click="openTierCare(row)">
              转出
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
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? '新增筛查记录' : '编辑筛查记录'" width="900px" :close-on-click-modal="false">
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
            <el-form-item label="市（州）">
              <el-input v-model="editForm.city" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县">
              <el-input v-model="editForm.district" />
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
            <el-form-item label="出生日期">
              <el-date-picker v-model="editForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="editForm.age" :min="0" :max="150" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="证件类型">
              <el-input v-model="editForm.idType" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="证件号">
              <el-input v-model="editForm.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="民族">
              <el-input v-model="editForm.ethnicity" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话">
              <el-input v-model="editForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="户籍地址">
              <el-input v-model="editForm.householdAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现住址">
              <el-input v-model="editForm.currentAddress" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          学校信息
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="学校类型">
              <el-input v-model="editForm.schoolType" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学校名称">
              <el-input v-model="editForm.schoolName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班级（院系）">
              <el-input v-model="editForm.className" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="既往结核病史">
              <el-select v-model="editForm.tbHistory" style="width:100%" clearable>
                <el-option label="有" value="有" />
                <el-option label="无" value="无" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="密切接触史">
              <el-select v-model="editForm.closeContactHistory" style="width:100%" clearable>
                <el-option label="有" value="有" />
                <el-option label="无" value="无" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可疑症状">
              <el-select v-model="editForm.suspiciousSymptoms" style="width:100%" clearable>
                <el-option label="有" value="有" />
                <el-option label="无" value="无" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          感染筛查
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="是否进行感染筛查">
              <el-select v-model="editForm.hasInfectionScreen" style="width:100%" clearable>
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="感染筛查日期">
              <el-date-picker v-model="editForm.screenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="筛查方法">
              <el-input v-model="editForm.screenMethod" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="筛查结果（mm*mm）">
              <el-input v-model="editForm.screenResult" placeholder="PPD 斑痕如：3*6" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="感染筛查结果">
              <el-select v-model="editForm.infectionResult" style="width:100%" clearable>
                <el-option label="PPD阴性" value="PPD阴性" />
                <el-option label="PPD+" value="PPD+" />
                <el-option label="PPD++" value="PPD++" />
                <el-option label="PPD+++" value="PPD+++" />
                <el-option label="EC阴性" value="EC阴性" />
                <el-option label="EC阳性" value="EC阳性" />
                <el-option label="IGRA阴性" value="IGRA阴性" />
                <el-option label="IGRA阳性" value="IGRA阳性" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          胸片与诊断
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="是否进行胸片检查">
              <el-select v-model="editForm.hasChestXray" style="width:100%" clearable>
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="胸片检查日期">
              <el-date-picker v-model="editForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="胸片结果">
              <el-select v-model="editForm.chestXrayResult" style="width:100%" clearable>
                <el-option label="正常" value="正常" />
                <el-option label="异常" value="异常" />
                <el-option label="未查" value="未查" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="痰涂片结果">
              <el-input v-model="editForm.sputumSmearResult" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分子生物学结果">
              <el-input v-model="editForm.molecularBiologyResult" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="诊断结果">
              <el-select v-model="editForm.diagnosisFirst" style="width:100%" clearable>
                <el-option v-for="item in SCREENING_DIAGNOSIS_EDIT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name || ''} - 详情`" width="900px">
      <el-descriptions v-if="detailRow" :column="3" border>
        <el-descriptions-item label="年份">
          {{ detailRow.year }}
        </el-descriptions-item>
        <el-descriptions-item label="市（州）">
          {{ detailRow.city }}
        </el-descriptions-item>
        <el-descriptions-item label="区县">
          {{ detailRow.district }}
        </el-descriptions-item>
        <el-descriptions-item label="姓名">
          {{ detailRow.name }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ detailRow.gender }}
        </el-descriptions-item>
        <el-descriptions-item label="出生日期">
          {{ detailRow.birthDate }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ detailRow.age }}
        </el-descriptions-item>
        <el-descriptions-item label="证件类型">
          {{ detailRow.idType }}
        </el-descriptions-item>
        <el-descriptions-item label="证件号">
          {{ detailRow.idNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="民族">
          {{ detailRow.ethnicity }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          {{ detailRow.phone }}
        </el-descriptions-item>
        <el-descriptions-item label="学校类型">
          {{ detailRow.schoolType }}
        </el-descriptions-item>
        <el-descriptions-item label="学校名称">
          {{ detailRow.schoolName }}
        </el-descriptions-item>
        <el-descriptions-item label="班级（院系）">
          {{ detailRow.className }}
        </el-descriptions-item>
        <el-descriptions-item label="既往结核病史">
          {{ detailRow.tbHistory }}
        </el-descriptions-item>
        <el-descriptions-item label="密切接触史">
          {{ detailRow.closeContactHistory }}
        </el-descriptions-item>
        <el-descriptions-item label="可疑症状">
          {{ detailRow.suspiciousSymptoms }}
        </el-descriptions-item>
        <el-descriptions-item label="是否进行感染筛">
          {{ detailRow.hasInfectionScreen }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查日期">
          {{ detailRow.screenDate }}
        </el-descriptions-item>
        <el-descriptions-item label="筛查方法">
          {{ detailRow.screenMethod }}
        </el-descriptions-item>
        <el-descriptions-item label="筛查结果">
          {{ formatScreenResultDisplay(detailRow.screenResult, detailRow.screenMethod) || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查结果">
          {{ detailRow.infectionResult }}
        </el-descriptions-item>
        <el-descriptions-item label="判定结果">
          {{ getScreeningLatentStatusLabel(detailRow) }}
        </el-descriptions-item>
        <el-descriptions-item label="是否进行胸片检查">
          {{ detailRow.hasChestXray }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查日期">
          {{ detailRow.chestXrayDate }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片结果">
          {{ detailRow.chestXrayResult }}
        </el-descriptions-item>
        <el-descriptions-item label="痰涂片结果">
          {{ detailRow.sputumSmearResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="分子生物学结果">
          {{ detailRow.molecularBiologyResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="诊断结果">
          {{ detailRow.diagnosisFirst }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="3">
          {{ detailRow.householdAddress }}
        </el-descriptions-item>
        <el-descriptions-item label="现住址" :span="3">
          {{ detailRow.currentAddress }}
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

    <!-- 转出弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="screening_school"
      population-type="school"
      module-type="screening"
      :subject-name="tierCareRow.name || ''"
    />

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
          :title="`发现 ${importResult.errors.length} 条数据存在格式问题（已照常导入，请核查）`"
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
</style>

<style lang="scss">
.el-table .confirmed-row td.el-table__cell {
  background-color: #fff2f0 !important;
  color: #f56c6c;
}
</style>
