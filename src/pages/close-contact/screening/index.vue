<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { uploadScreeningCloseContactApi, getScreeningCloseContactListApi, exportScreeningCloseContactApi, deleteScreeningCloseContactApi, updateScreeningCloseContactApi, createScreeningCloseContactApi } from "./apis"
import { ACTIVE_ROUND_MAP } from "@@/constants/disease"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  district: "",
  isLatent: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getScreeningCloseContactListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...searchForm
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { paginationData.currentPage = 1; fetchData() }
function handleReset() {
  searchForm.name = ""; searchForm.idNumber = ""; searchForm.district = ""; searchForm.isLatent = undefined
  handleSearch()
}

const importResultVisible = ref(false)
const importResult = ref<{ successCount: number; errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

async function handleUpload(uploadFile: any) {
  try {
    const { data } = await uploadScreeningCloseContactApi(uploadFile.raw)
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
    const res = await exportScreeningCloseContactApi(ids)
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "密接人群筛查数据.xlsx"
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

function getLatentTag(isLatent: number) { return isLatent === 1 ? "danger" : "success" }
function getActiveRoundTag(round: number) {
  if (round === 1) return "success"
  if (round === 2) return "warning"
  return "danger"
}

/**
 * 推算密接人群下次复查时间
 * - 首次筛查后6个月做半年复查
 * - 半年筛查后6个月做一年复查
 * - 完成三轮或已确认为潜伏管理者则无需复查
 */
function getNextReviewDate(row: any): string {
  if (row.isLatent === 1) return "已判定疑似结核"
  if (row.activeRound === 3 || row.oneYearInfectionResult) return "已完成三轮"
  if (row.halfYearScreenDate) {
    const d = new Date(row.halfYearScreenDate)
    d.setMonth(d.getMonth() + 6)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}（一年复查）`
  }
  if (row.firstScreenDate) {
    const d = new Date(row.firstScreenDate)
    d.setMonth(d.getMonth() + 6)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}（半年复查）`
  }
  return "—"
}

function isReviewNearDue(row: any): boolean {
  const dateStr = getNextReviewDate(row)
  if (dateStr.includes("—") || dateStr.includes("已")) return false
  const reviewDate = new Date(dateStr.split("（")[0])
  const diffDays = (reviewDate.getTime() - Date.now()) / 86400000
  return diffDays >= 0 && diffDays <= 15
}

/** 编辑弹窗 */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<Record<string, any>>({})
const editMode = ref<"create" | "edit">("edit")

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
    occupation: "",
    phone: "",
    householdAddress: "",
    currentAddress: "",
    contactType: "",
    sourcePatientName: "",
    sourcePatientConfirmDate: "",
    sourcePatientIdNumber: "",
    firstScreenDate: "",
    firstSymptomResult: "",
    firstInfectionMethod: "",
    firstScreenResult: "",
    firstInfectionResult: "",
    firstHasChestXray: "",
    firstChestXrayDate: "",
    firstChestXrayResult: "",
    firstDiagnosis: "",
    halfYearScreenDate: "",
    halfYearSymptomResult: "",
    halfYearInfectionMethod: "",
    halfYearScreenResult: "",
    halfYearInfectionResult: "",
    halfYearHasChestXray: "",
    halfYearChestXrayDate: "",
    halfYearChestXrayResult: "",
    halfYearDiagnosis: "",
    oneYearScreenDate: "",
    oneYearSymptomResult: "",
    oneYearInfectionMethod: "",
    oneYearScreenResult: "",
    oneYearInfectionResult: "",
    oneYearHasChestXray: "",
    oneYearChestXrayDate: "",
    oneYearChestXrayResult: "",
    oneYearDiagnosis: "",
    hasPreventiveTreatment: "",
    preventivePlan: "",
    preventiveStartDate: "",
    preventiveEndDate: "",
    preventiveResult: "",
    preventiveManager: "",
    benefitMethod: "",
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

async function handleSave() {
  editSaving.value = true
  try {
    if (editMode.value === "create") {
      await createScreeningCloseContactApi(editForm.value)
      ElMessage.success("新增成功")
    } else {
      await updateScreeningCloseContactApi(editForm.value.id, editForm.value)
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
    await deleteScreeningCloseContactApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除失败")
  }
}

/** 三轮筛查详情弹窗 */
const detailVisible = ref(false)
const detailRow = ref<any>(null)
function viewDetail(row: any) { detailRow.value = row; detailVisible.value = true }

watch(() => [paginationData.currentPage, paginationData.pageSize], fetchData, { immediate: true })
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名"><el-input v-model="searchForm.name" placeholder="请输入姓名" clearable /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable /></el-form-item>
        <el-form-item label="区县"><el-input v-model="searchForm.district" placeholder="请输入区县" clearable /></el-form-item>
        <el-form-item label="判定结果">
          <el-select v-model="searchForm.isLatent" placeholder="全部" clearable style="width: 120px">
            <el-option label="疑似结核" :value="1" /><el-option label="正常" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">密接人群筛查数据（V4 三轮）</span>
          <div class="flex gap-2">
            <el-button v-permission="'screening:create'" type="success" @click="handleCreate">新增数据</el-button>
            <el-button v-permission="'screening:export'" @click="() => handleExport()">导出全部</el-button>
            <el-button v-permission="'screening:export'" type="warning" :disabled="selectedRows.length === 0" @click="handleExportSelected">导出勾选</el-button>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleUpload">
              <el-button type="primary" v-permission="'screening:upload'">上传 Excel</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <!-- V4：按三轮折叠展示，点击"查看详情"弹窗展示三轮完整字段 -->
      <el-table v-loading="loading" :data="tableData" border stripe max-height="600" row-key="id" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" fixed />
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="district" label="区县" />
        <el-table-column prop="ethnicity" label="民族" />
        <el-table-column prop="occupation" label="职业" />
        <el-table-column prop="contactType" label="接触类型" />
        <el-table-column prop="sourcePatientName" label="原患者姓名" />
        <el-table-column prop="sourcePatientConfirmDate" label="原患者确诊日期" />
        <el-table-column prop="sourcePatientIdNumber" label="原患者身份证号" />

        <!-- 三轮感染筛查摘要 -->
        <el-table-column label="首次感染结果">
          <template #default="{ row }">
            <el-tag v-if="row.firstInfectionResult" :type="row.firstInfectionResult.includes('阳') || row.firstInfectionResult.includes('PPD+') ? 'danger' : 'success'" size="small">
              {{ row.firstInfectionResult || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="半年后感染结果">
          <template #default="{ row }">
            <el-tag v-if="row.halfYearInfectionResult" :type="row.halfYearInfectionResult.includes('阳') || row.halfYearInfectionResult.includes('PPD+') ? 'danger' : 'success'" size="small">
              {{ row.halfYearInfectionResult || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="一年后感染结果">
          <template #default="{ row }">
            <el-tag v-if="row.oneYearInfectionResult" :type="row.oneYearInfectionResult.includes('阳') || row.oneYearInfectionResult.includes('PPD+') ? 'danger' : 'success'" size="small">
              {{ row.oneYearInfectionResult || "-" }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 阳性轮次 -->
        <el-table-column label="阳性轮次">
          <template #default="{ row }">
            <el-tag v-if="row.activeRound" :type="getActiveRoundTag(row.activeRound)" size="small">
              {{ ACTIVE_ROUND_MAP[row.activeRound] }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <!-- 预防性治疗 -->
        <el-table-column prop="preventivePlan" label="预防性治疗方案" />
        <el-table-column prop="preventiveResult" label="治疗结果" />
        <el-table-column prop="benefitMethod" label="惠民方式" />

        <el-table-column label="疑似结核">
          <template #default="{ row }">
            <el-tag :type="getLatentTag(row.isLatent)" size="small">
              {{ row.isLatent === 1 ? "疑似结核" : "正常" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下次复查时间" min-width="160" fixed="right">
          <template #default="{ row }">
            <span :class="{ 'review-near-due': isReviewNearDue(row) }">{{ getNextReviewDate(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">查看详情</el-button>
            <el-button v-permission="'screening:edit'" type="warning" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'screening:delete'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? '新增筛查记录' : '编辑筛查记录'" width="960px" :close-on-click-modal="false">
      <el-tabs>
        <!-- 基本信息 Tab -->
        <el-tab-pane label="基本信息">
          <el-form :model="editForm" label-width="130px" class="edit-form">
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="年份"><el-input v-model="editForm.year" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="市（州）"><el-input v-model="editForm.city" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="区县"><el-input v-model="editForm.district" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="姓名"><el-input v-model="editForm.name" /></el-form-item></el-col>
              <el-col :span="8">
                <el-form-item label="性别">
                  <el-select v-model="editForm.gender" style="width:100%">
                    <el-option label="男" value="男" /><el-option label="女" value="女" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8"><el-form-item label="出生日期"><el-date-picker v-model="editForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="年龄"><el-input-number v-model="editForm.age" :min="0" :max="150" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="证件类型"><el-input v-model="editForm.idType" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="证件号"><el-input v-model="editForm.idNumber" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="民族"><el-input v-model="editForm.ethnicity" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="职业"><el-input v-model="editForm.occupation" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="联系电话"><el-input v-model="editForm.phone" /></el-form-item></el-col>
              <el-col :span="16"><el-form-item label="户籍地址"><el-input v-model="editForm.householdAddress" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="现住址"><el-input v-model="editForm.currentAddress" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="接触类型"><el-input v-model="editForm.contactType" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="原患者姓名"><el-input v-model="editForm.sourcePatientName" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="原患者确诊日期"><el-date-picker v-model="editForm.sourcePatientConfirmDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="原患者身份证号"><el-input v-model="editForm.sourcePatientIdNumber" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 首次筛查 Tab -->
        <el-tab-pane label="首次筛查">
          <el-form :model="editForm" label-width="130px" class="edit-form">
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="筛查日期"><el-date-picker v-model="editForm.firstScreenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="症状筛查结果"><el-input v-model="editForm.firstSymptomResult" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="感染检查方法"><el-input v-model="editForm.firstInfectionMethod" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="感染检查结果"><el-input v-model="editForm.firstScreenResult" /></el-form-item></el-col>
              <el-col :span="16">
                <el-form-item label="感染筛查结果">
                  <el-select v-model="editForm.firstInfectionResult" style="width:100%" clearable>
                    <el-option label="PPD阴性" value="PPD阴性" /><el-option label="PPD+" value="PPD+" />
                    <el-option label="PPD++" value="PPD++" /><el-option label="PPD+++" value="PPD+++" />
                    <el-option label="EC阴性" value="EC阴性" /><el-option label="EC阳性" value="EC阳性" />
                    <el-option label="IGRA阴性" value="IGRA阴性" /><el-option label="IGRA阳性" value="IGRA阳性" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8"><el-form-item label="是否进行胸片"><el-input v-model="editForm.firstHasChestXray" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="胸片日期"><el-date-picker v-model="editForm.firstChestXrayDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="胸片结果"><el-input v-model="editForm.firstChestXrayResult" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="诊断结果"><el-input v-model="editForm.firstDiagnosis" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 半年后筛查 Tab -->
        <el-tab-pane label="半年后筛查">
          <el-form :model="editForm" label-width="130px" class="edit-form">
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="筛查日期"><el-date-picker v-model="editForm.halfYearScreenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="症状筛查结果"><el-input v-model="editForm.halfYearSymptomResult" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="感染检查方法"><el-input v-model="editForm.halfYearInfectionMethod" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="感染检查结果"><el-input v-model="editForm.halfYearScreenResult" /></el-form-item></el-col>
              <el-col :span="16">
                <el-form-item label="感染筛查结果">
                  <el-select v-model="editForm.halfYearInfectionResult" style="width:100%" clearable>
                    <el-option label="PPD阴性" value="PPD阴性" /><el-option label="PPD+" value="PPD+" />
                    <el-option label="PPD++" value="PPD++" /><el-option label="PPD+++" value="PPD+++" />
                    <el-option label="EC阴性" value="EC阴性" /><el-option label="EC阳性" value="EC阳性" />
                    <el-option label="IGRA阴性" value="IGRA阴性" /><el-option label="IGRA阳性" value="IGRA阳性" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8"><el-form-item label="是否进行胸片"><el-input v-model="editForm.halfYearHasChestXray" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="胸片日期"><el-date-picker v-model="editForm.halfYearChestXrayDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="胸片结果"><el-input v-model="editForm.halfYearChestXrayResult" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="诊断结果"><el-input v-model="editForm.halfYearDiagnosis" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 一年后筛查 Tab -->
        <el-tab-pane label="一年后筛查">
          <el-form :model="editForm" label-width="130px" class="edit-form">
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="筛查日期"><el-date-picker v-model="editForm.oneYearScreenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="症状筛查结果"><el-input v-model="editForm.oneYearSymptomResult" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="感染检查方法"><el-input v-model="editForm.oneYearInfectionMethod" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="感染检查结果"><el-input v-model="editForm.oneYearScreenResult" /></el-form-item></el-col>
              <el-col :span="16">
                <el-form-item label="感染筛查结果">
                  <el-select v-model="editForm.oneYearInfectionResult" style="width:100%" clearable>
                    <el-option label="PPD阴性" value="PPD阴性" /><el-option label="PPD+" value="PPD+" />
                    <el-option label="PPD++" value="PPD++" /><el-option label="PPD+++" value="PPD+++" />
                    <el-option label="EC阴性" value="EC阴性" /><el-option label="EC阳性" value="EC阳性" />
                    <el-option label="IGRA阴性" value="IGRA阴性" /><el-option label="IGRA阳性" value="IGRA阳性" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8"><el-form-item label="是否进行胸片"><el-input v-model="editForm.oneYearHasChestXray" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="胸片日期"><el-date-picker v-model="editForm.oneYearChestXrayDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="胸片结果"><el-input v-model="editForm.oneYearChestXrayResult" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="诊断结果"><el-input v-model="editForm.oneYearDiagnosis" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 预防性治疗 Tab -->
        <el-tab-pane label="预防性治疗">
          <el-form :model="editForm" label-width="160px" class="edit-form">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="是否进行预防性治疗"><el-input v-model="editForm.hasPreventiveTreatment" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="预防性治疗方案"><el-input v-model="editForm.preventivePlan" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="开始时间"><el-date-picker v-model="editForm.preventiveStartDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="完成时间"><el-date-picker v-model="editForm.preventiveEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="治疗结果"><el-input v-model="editForm.preventiveResult" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="随访管理人员"><el-input v-model="editForm.preventiveManager" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="惠民方式"><el-input v-model="editForm.benefitMethod" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 三轮筛查详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name} — 筛查详情`" width="800px">
      <el-descriptions v-if="detailRow" :column="2" border class="mb-4">
        <el-descriptions-item label="姓名">{{ detailRow.name }}</el-descriptions-item>
        <el-descriptions-item label="证件号">{{ detailRow.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ detailRow.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detailRow.age }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ detailRow.ethnicity }}</el-descriptions-item>
        <el-descriptions-item label="职业">{{ detailRow.occupation }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailRow.phone }}</el-descriptions-item>
        <el-descriptions-item label="接触类型">{{ detailRow.contactType }}</el-descriptions-item>
        <el-descriptions-item label="原患者姓名">{{ detailRow.sourcePatientName }}</el-descriptions-item>
        <el-descriptions-item label="原患者确诊日期">{{ detailRow.sourcePatientConfirmDate }}</el-descriptions-item>
        <el-descriptions-item label="原患者身份证号">{{ detailRow.sourcePatientIdNumber }}</el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">{{ detailRow.householdAddress }}</el-descriptions-item>
        <el-descriptions-item label="现住址" :span="2">{{ detailRow.currentAddress }}</el-descriptions-item>
      </el-descriptions>

      <!-- 三轮筛查分栏 -->
      <el-tabs v-if="detailRow">
        <el-tab-pane label="首次筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="筛查日期">{{ detailRow.firstScreenDate }}</el-descriptions-item>
            <el-descriptions-item label="症状筛查结果">{{ detailRow.firstSymptomResult }}</el-descriptions-item>
            <el-descriptions-item label="感染检查方法">{{ detailRow.firstInfectionMethod }}</el-descriptions-item>
            <el-descriptions-item label="感染检查结果">{{ detailRow.firstScreenResult }}</el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">
              <el-tag :type="detailRow.firstInfectionResult?.includes('阳') || detailRow.firstInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                {{ detailRow.firstInfectionResult || "-" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否进行胸片">{{ detailRow.firstHasChestXray }}</el-descriptions-item>
            <el-descriptions-item label="胸片日期">{{ detailRow.firstChestXrayDate }}</el-descriptions-item>
            <el-descriptions-item label="胸片结果">{{ detailRow.firstChestXrayResult }}</el-descriptions-item>
            <el-descriptions-item label="诊断结果" :span="2">
              <el-tag v-if="detailRow.firstDiagnosis" size="small">{{ detailRow.firstDiagnosis }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="半年后筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="筛查日期">{{ detailRow.halfYearScreenDate }}</el-descriptions-item>
            <el-descriptions-item label="症状筛查结果">{{ detailRow.halfYearSymptomResult }}</el-descriptions-item>
            <el-descriptions-item label="感染检查方法">{{ detailRow.halfYearInfectionMethod }}</el-descriptions-item>
            <el-descriptions-item label="感染检查结果">{{ detailRow.halfYearScreenResult }}</el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">
              <el-tag :type="detailRow.halfYearInfectionResult?.includes('阳') || detailRow.halfYearInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                {{ detailRow.halfYearInfectionResult || "-" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否进行胸片">{{ detailRow.halfYearHasChestXray }}</el-descriptions-item>
            <el-descriptions-item label="胸片日期">{{ detailRow.halfYearChestXrayDate }}</el-descriptions-item>
            <el-descriptions-item label="胸片结果">{{ detailRow.halfYearChestXrayResult }}</el-descriptions-item>
            <el-descriptions-item label="诊断结果" :span="2">
              <el-tag v-if="detailRow.halfYearDiagnosis" size="small">{{ detailRow.halfYearDiagnosis }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="一年后筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="筛查日期">{{ detailRow.oneYearScreenDate }}</el-descriptions-item>
            <el-descriptions-item label="症状筛查结果">{{ detailRow.oneYearSymptomResult }}</el-descriptions-item>
            <el-descriptions-item label="感染检查方法">{{ detailRow.oneYearInfectionMethod }}</el-descriptions-item>
            <el-descriptions-item label="感染检查结果">{{ detailRow.oneYearScreenResult }}</el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">
              <el-tag :type="detailRow.oneYearInfectionResult?.includes('阳') || detailRow.oneYearInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                {{ detailRow.oneYearInfectionResult || "-" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否进行胸片">{{ detailRow.oneYearHasChestXray }}</el-descriptions-item>
            <el-descriptions-item label="胸片日期">{{ detailRow.oneYearChestXrayDate }}</el-descriptions-item>
            <el-descriptions-item label="胸片结果">{{ detailRow.oneYearChestXrayResult }}</el-descriptions-item>
            <el-descriptions-item label="诊断结果" :span="2">
              <el-tag v-if="detailRow.oneYearDiagnosis" size="small">{{ detailRow.oneYearDiagnosis }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="预防性治疗情况">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="是否进行预防性治疗">{{ detailRow.hasPreventiveTreatment }}</el-descriptions-item>
            <el-descriptions-item label="预防性治疗方案">{{ detailRow.preventivePlan }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ detailRow.preventiveStartDate }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ detailRow.preventiveEndDate }}</el-descriptions-item>
            <el-descriptions-item label="治疗结果">{{ detailRow.preventiveResult }}</el-descriptions-item>
            <el-descriptions-item label="随访管理人员">{{ detailRow.preventiveManager }}</el-descriptions-item>
            <el-descriptions-item label="惠民方式">{{ detailRow.benefitMethod }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detailRow.remark }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="flex items-center justify-between">
          <div>
            <span v-if="detailRow?.activeRound" class="text-sm text-gray-500">
              阳性轮次：
              <el-tag :type="getActiveRoundTag(detailRow.activeRound)" size="small">{{ ACTIVE_ROUND_MAP[detailRow.activeRound] }}</el-tag>
            </span>
            <span v-else-if="detailRow?.isLatent === 0 && detailRow?.oneYearInfectionResult" class="text-sm text-gray-400">三轮均阴性，已归档</span>
          </div>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert :title="`成功导入 ${importResult.successCount} 条数据`" type="success" :closable="false" class="mb-3" />
      <template v-if="importResult.errors.length > 0">
        <el-alert :title="`发现 ${importResult.errors.length} 条数据存在格式问题（已照常导入，请核查）`" type="warning" :closable="false" class="mb-3" />
        <el-table :data="importResult.errors.map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" width="50" />
          <el-table-column prop="msg" label="错误信息" />
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.edit-form { padding: 0 8px; }

.review-near-due {
  color: #e6a23c;
  font-weight: bold;
}
</style>
