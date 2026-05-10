<script lang="ts" setup>
import ReferralDialog from "@@/components/ReferralDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { useRouter } from "vue-router"
import {
  batchDeleteScreeningCloseContactApi,
  countByResultApi,
  createScreeningCloseContactApi,
  deleteScreeningCloseContactApi,
  exportScreeningCloseContactApi,
  getScreeningCloseContactListApi,
  submitThreeMonthCheckApi,
  updateScreeningCloseContactApi,
  uploadScreeningCloseContactApi
} from "./apis"

const router = useRouter()
const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

/** 分类统计 */
const resultStats = ref<Record<string, number>>({})

const searchForm = reactive({
  name: "",
  idNumber: "",
  district: "",
  finalScreeningResult: "" as string
})

type TagType = "primary" | "success" | "info" | "warning" | "danger"

/** 类型安全的 el-tag type */
function tagType(t: string): TagType {
  const allowed = ["primary", "success", "info", "warning", "danger"]
  return (allowed.includes(t) ? t : "info") as TagType
}

/** 最终筛查结果选项 */
const FINAL_RESULT_OPTIONS: { label: string, value: string, type: TagType }[] = [
  { label: "活动性肺结核", value: "活动性肺结核", type: "danger" },
  { label: "潜伏感染者", value: "潜伏感染者", type: "warning" },
  { label: "未做", value: "未做", type: "info" },
  { label: "未发现异常", value: "未发现异常", type: "success" }
]

/** 流程状态映射 */
const CC_STATUS_MAP: Record<number, { label: string, type: string }> = {
  0: { label: "待处理", type: "info" },
  1: { label: "活动性肺结核", type: "danger" },
  2: { label: "潜伏感染-管理中", type: "warning" },
  3: { label: "潜伏感染-已归档", type: "info" },
  4: { label: "随访监测中", type: "warning" },
  5: { label: "随访监测-已归档", type: "info" },
  6: { label: "待3月复查", type: "warning" },
  7: { label: "3月复查阴性-结束", type: "success" },
  8: { label: "3月阳性-转潜伏流程", type: "danger" }
}

function getFinalResultTag(result: string): string {
  const opt = FINAL_RESULT_OPTIONS.find(o => o.value === result)
  return opt?.type || "info"
}

async function fetchData() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      getScreeningCloseContactListApi({
        page: paginationData.currentPage,
        size: paginationData.pageSize,
        name: searchForm.name || undefined,
        idNumber: searchForm.idNumber || undefined,
        district: searchForm.district || undefined,
        finalScreeningResult: searchForm.finalScreeningResult || undefined
      }),
      countByResultApi()
    ])
    tableData.value = listRes.data.records
    total.value = listRes.data.total
    resultStats.value = statsRes.data || {}
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
  searchForm.district = ""
  searchForm.finalScreeningResult = ""
  handleSearch()
}

const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

// 转诊
const tierCareVisible = ref(false)
const tierCareRow = ref<any>(null)
function openTierCare(row: any) {
  tierCareRow.value = row
  tierCareVisible.value = true
}

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
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的数据")
    return
  }
  handleExport(ids)
}

/** 跳转到密接潜伏感染管理页 */
function goToLatent() {
  router.push("/close-contact/latent")
}

/** 跳转到密接患者管理页 */
function goToPatient() {
  router.push("/close-contact/patient")
}

/** 编辑弹窗 */
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = ref<Record<string, any>>({})
const editMode = ref<"create" | "edit">("edit")

function getEmptyEditForm() {
  return {
    city: "",
    district: "",
    sourcePatientName: "",
    sourcePatientCaseNo: "",
    sourcePatientIdNumber: "",
    sourcePatientPhone: "",
    name: "",
    idNumber: "",
    age: undefined,
    phone: "",
    gender: "",
    ethnicity: "",
    contactType: "",
    contactPlace: "",
    registrationDate: "",
    firstScreenDate: "",
    symptom1: "",
    symptom2: "",
    infectionCheckDate: "",
    infectionCheckMethod: "",
    infectionCheckResult: "",
    imagingDate: "",
    imagingMethod: "",
    imagingResult: "",
    sputumCheckDate: "",
    sputumCheckMethod: "",
    sputumCheckResult: "",
    finalScreeningResult: "",
    hasPreventiveTreatment: "",
    preventivePlan: "",
    treatmentCompleted: "",
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

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」的筛查记录吗？删除后所有关联数据将一并删除，且不可恢复！`,
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
    await batchDeleteScreeningCloseContactApi(ids)
    ElMessage.success(`成功删除 ${ids.length} 条记录`)
    selectedRows.value = []
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("批量删除失败")
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailRow = ref<any>(null)
function viewDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
}

/** 判断随访月份是否有数据 */
function hasFollowupData(row: any, month: number): boolean {
  const key = `followup${month}Result`
  return !!row[key]
}

/** 获取随访结果的Tag类型 */
function getFollowupTag(result: string): string {
  if (!result) return "info"
  if (result.includes("活动性肺结核")) return "danger"
  if (result.includes("潜伏感染者")) return "warning"
  if (result.includes("未发现异常")) return "success"
  return "info"
}

watch(() => [paginationData.currentPage, paginationData.pageSize], fetchData, { immediate: true })

// ==================== 3月复查 ====================
const threeMonthDialogVisible = ref(false)
const threeMonthRow = ref<any>(null)
const threeMonthSubmitting = ref(false)
const threeMonthForm = reactive({
  checkDate: "",
  checkResult: "",
  finalResult: "" as "阴性" | "阳性" | ""
})

function openThreeMonthDialog(row: any) {
  threeMonthRow.value = row
  threeMonthForm.checkDate = ""
  threeMonthForm.checkResult = ""
  threeMonthForm.finalResult = ""
  threeMonthDialogVisible.value = true
}

async function handleThreeMonthSubmit() {
  if (!threeMonthForm.checkDate || !threeMonthForm.checkResult || !threeMonthForm.finalResult) {
    ElMessage.warning("请填写完整的复查信息")
    return
  }
  if (threeMonthSubmitting.value) return
  threeMonthSubmitting.value = true
  try {
    await submitThreeMonthCheckApi(threeMonthRow.value.id, {
      checkDate: threeMonthForm.checkDate,
      checkResult: threeMonthForm.checkResult,
      finalResult: threeMonthForm.finalResult
    })
    ElMessage.success("3月复查结果已提交")
    threeMonthDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ } finally {
    threeMonthSubmitting.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="12" class="mb-4">
      <el-col :span="6" v-for="opt in FINAL_RESULT_OPTIONS" :key="opt.value">
        <el-card shadow="hover" class="stat-card" @click="searchForm.finalScreeningResult = opt.value; handleSearch()">
          <div class="flex items-center justify-between">
            <span class="text-sm text-gray-500">{{ opt.label }}</span>
            <el-tag :type="opt.type" size="small">
              {{ resultStats[opt.value] || 0 }} 人
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速导航 -->
    <el-card shadow="never" class="mb-4">
      <div class="flex items-center gap-3">
        <span class="text-sm text-gray-500 font-bold">快速跳转：</span>
        <el-button type="warning" size="small" @click="goToLatent">
          密接潜伏感染管理 →
        </el-button>
        <el-button type="danger" size="small" @click="goToPatient">
          密接患者管理 →
        </el-button>
      </div>
    </el-card>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="接触者姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="接触者身份证号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model="searchForm.district" placeholder="请输入区县" clearable />
        </el-form-item>
        <el-form-item label="最终筛查结果">
          <el-select v-model="searchForm.finalScreeningResult" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="opt in FINAL_RESULT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
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

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">密接人群筛查数据</span>
          <div class="flex gap-2">
            <el-button v-permission="'closeContact:screening:create'" type="success" @click="handleCreate">
              新增数据
            </el-button>
            <el-button v-permission="'closeContact:screening:export'" @click="() => handleExport()">
              导出全部
            </el-button>
            <el-button v-permission="'closeContact:screening:export'" type="warning" :disabled="!selectedRows.length" @click="handleExportSelected">
              导出勾选
            </el-button>
            <el-button v-permission="'closeContact:screening:delete'" type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleUpload">
              <el-button type="primary" v-permission="'closeContact:screening:upload'">
                上传 Excel
              </el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600" row-key="id" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" fixed />
        <el-table-column prop="name" label="接触者姓名" fixed min-width="80" />
        <el-table-column prop="idNumber" label="接触者身份证号" min-width="160" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="phone" label="联系电话" min-width="120" />
        <el-table-column prop="city" label="市/州" />
        <el-table-column prop="district" label="区/县" />
        <el-table-column prop="contactType" label="接触类型" />
        <el-table-column prop="sourcePatientName" label="原患者姓名" />
        <el-table-column prop="registrationDate" label="登记日期" min-width="100" />
        <el-table-column prop="infectionCheckMethod" label="感染检测方法" min-width="120" />
        <el-table-column prop="infectionCheckResult" label="感染检测结果" min-width="80" />
        <el-table-column prop="imagingDate" label="影像检查日期" min-width="110" />
        <el-table-column prop="imagingResult" label="影像结果" min-width="100" />
        <el-table-column label="最终筛查结果" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-tag v-if="row.finalScreeningResult" :type="tagType(getFinalResultTag(row.finalScreeningResult))" size="small">
              {{ row.finalScreeningResult }}
            </el-tag>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="流程状态" min-width="130" fixed="right">
          <template #default="{ row }">
            <el-tag v-if="CC_STATUS_MAP[row.ccStatus]" :type="tagType(CC_STATUS_MAP[row.ccStatus].type)" size="small">
              {{ CC_STATUS_MAP[row.ccStatus].label }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 随访完成情况 -->
        <el-table-column label="6月随访" width="90">
          <template #default="{ row }">
            <el-tag v-if="hasFollowupData(row, 6)" :type="tagType(getFollowupTag(row.followup6Result))" size="small">
              {{ row.followup6Result }}
            </el-tag>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="12月随访" width="90">
          <template #default="{ row }">
            <el-tag v-if="hasFollowupData(row, 12)" :type="tagType(getFollowupTag(row.followup12Result))" size="small">
              {{ row.followup12Result }}
            </el-tag>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="24月随访" width="90">
          <template #default="{ row }">
            <el-tag v-if="hasFollowupData(row, 24)" :type="tagType(getFollowupTag(row.followup24Result))" size="small">
              {{ row.followup24Result }}
            </el-tag>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              详情
            </el-button>
            <el-button v-permission="'closeContact:screening:edit'" type="warning" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'closeContact:screening:delete'" type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
            <el-button v-permission="'referral'" type="warning" link size="small" @click="openTierCare(row)">
              转诊
            </el-button>
            <el-button
              v-if="row.ccStatus === 6"
              type="success"
              link
              size="small"
              @click="openThreeMonthDialog(row)"
            >
              填写3月复查
            </el-button>
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
        <el-tab-pane label="原患者信息">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="市/州">
                  <el-input v-model="editForm.city" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="区/县">
                  <el-input v-model="editForm.district" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="原患者姓名">
                  <el-input v-model="editForm.sourcePatientName" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="原患者身份证号">
                  <el-input v-model="editForm.sourcePatientIdNumber" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="原患者病案号">
                  <el-input v-model="editForm.sourcePatientCaseNo" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="原患者电话">
                  <el-input v-model="editForm.sourcePatientPhone" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="接触者基本信息">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="接触者姓名">
                  <el-input v-model="editForm.name" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="身份证号">
                  <el-input v-model="editForm.idNumber" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="年龄">
                  <el-input-number v-model="editForm.age" :min="0" :max="150" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="联系电话">
                  <el-input v-model="editForm.phone" />
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
                <el-form-item label="民族">
                  <el-input v-model="editForm.ethnicity" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="接触类型">
                  <el-input v-model="editForm.contactType" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="接触场所">
                  <el-input v-model="editForm.contactPlace" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="登记日期">
                  <el-date-picker v-model="editForm.registrationDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="初次筛查">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="首次筛查日期">
                  <el-date-picker v-model="editForm.firstScreenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="结核症状1">
                  <el-input v-model="editForm.symptom1" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="结核症状2">
                  <el-input v-model="editForm.symptom2" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测日期">
                  <el-date-picker v-model="editForm.infectionCheckDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测方法">
                  <el-input v-model="editForm.infectionCheckMethod" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="结果判定">
                  <el-input v-model="editForm.infectionCheckResult" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像检查日期">
                  <el-date-picker v-model="editForm.imagingDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像方法">
                  <el-input v-model="editForm.imagingMethod" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="影像结果">
                  <el-input v-model="editForm.imagingResult" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="痰检方法">
                  <el-input v-model="editForm.sputumCheckMethod" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="痰检结果">
                  <el-input v-model="editForm.sputumCheckResult" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最终筛查结果">
                  <el-select v-model="editForm.finalScreeningResult" style="width:100%" clearable>
                    <el-option v-for="opt in FINAL_RESULT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="预防治疗">
          <el-form :model="editForm" label-width="160px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="是否开展预防治疗">
                  <el-input v-model="editForm.hasPreventiveTreatment" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="预防性治疗方案">
                  <el-input v-model="editForm.preventivePlan" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否完成治疗">
                  <el-input v-model="editForm.treatmentCompleted" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注">
                  <el-input v-model="editForm.remark" type="textarea" :rows="2" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>
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
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name} — 密接筛查详情`" width="860px">
      <el-tabs v-if="detailRow">
        <el-tab-pane label="基本信息">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="接触者姓名">
              {{ detailRow.name }}
            </el-descriptions-item>
            <el-descriptions-item label="身份证号">
              {{ detailRow.idNumber }}
            </el-descriptions-item>
            <el-descriptions-item label="年龄">
              {{ detailRow.age }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ detailRow.phone }}
            </el-descriptions-item>
            <el-descriptions-item label="接触类型">
              {{ detailRow.contactType }}
            </el-descriptions-item>
            <el-descriptions-item label="接触场所">
              {{ detailRow.contactPlace }}
            </el-descriptions-item>
            <el-descriptions-item label="原患者姓名">
              {{ detailRow.sourcePatientName }}
            </el-descriptions-item>
            <el-descriptions-item label="原患者身份证号">
              {{ detailRow.sourcePatientIdNumber }}
            </el-descriptions-item>
            <el-descriptions-item label="密接登记日期">
              {{ detailRow.registrationDate }}
            </el-descriptions-item>
            <el-descriptions-item label="最终筛查结果" :span="2">
              <el-tag :type="tagType(getFinalResultTag(detailRow.finalScreeningResult))">
                {{ detailRow.finalScreeningResult || '—' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="流程状态">
              <el-tag v-if="CC_STATUS_MAP[detailRow.ccStatus]" :type="tagType(CC_STATUS_MAP[detailRow.ccStatus].type)">
                {{ CC_STATUS_MAP[detailRow.ccStatus].label }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="初次筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="首次筛查日期">
              {{ detailRow.firstScreenDate }}
            </el-descriptions-item>
            <el-descriptions-item label="感染检测方法">
              {{ detailRow.infectionCheckMethod }}
            </el-descriptions-item>
            <el-descriptions-item label="感染检测结果">
              {{ detailRow.infectionCheckResult }}
            </el-descriptions-item>
            <el-descriptions-item label="影像方法">
              {{ detailRow.imagingMethod }}
            </el-descriptions-item>
            <el-descriptions-item label="影像结果">
              {{ detailRow.imagingResult }}
            </el-descriptions-item>
            <el-descriptions-item label="痰检方法">
              {{ detailRow.sputumCheckMethod }}
            </el-descriptions-item>
            <el-descriptions-item label="痰检结果">
              {{ detailRow.sputumCheckResult }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="随访情况">
          <el-timeline>
            <!-- 3月复查（针对初次筛查阴性/未做的记录） -->
            <el-timeline-item
              :color="detailRow.threeMonthCheckDate ? '#67c23a' : '#909399'"
            >
              <template #dot>
                <el-icon v-if="detailRow.threeMonthCheckDate" color="#67c23a">
                  <CircleCheck />
                </el-icon>
                <el-icon v-else color="#909399">
                  <Clock />
                </el-icon>
              </template>
              <div class="mb-2">
                <span class="font-bold">3月复查</span>
              </div>
              <template v-if="detailRow.threeMonthCheckDate">
                <el-tag :type="detailRow.threeMonthFinalResult === '阴性' ? 'success' : 'danger'" size="small">
                  {{ detailRow.threeMonthFinalResult }}
                </el-tag>
                <span class="ml-2 text-sm text-gray-500">检测结果：{{ detailRow.threeMonthCheckResult }}</span>
                <span class="ml-2 text-sm text-gray-500">复查日期：{{ detailRow.threeMonthCheckDate }}</span>
              </template>
              <template v-else>
                <span class="text-gray-400 text-sm">尚未完成</span>
              </template>
            </el-timeline-item>
            <el-timeline-item
              v-for="month in [6, 12, 24]" :key="month"
              :color="hasFollowupData(detailRow, month) ? '#67c23a' : '#909399'"
            >
              <template #dot>
                <el-icon v-if="hasFollowupData(detailRow, month)" color="#67c23a">
                  <CircleCheck />
                </el-icon>
                <el-icon v-else color="#909399">
                  <Clock />
                </el-icon>
              </template>
              <div class="mb-2">
                <span class="font-bold">{{ month }}月随访</span>
                <span class="ml-3 text-gray-400">到期：{{ detailRow[`followup${month}DueDate`] || '—' }}</span>
              </div>
              <template v-if="hasFollowupData(detailRow, month)">
                <el-tag :type="tagType(getFollowupTag(detailRow[`followup${month}Result`]))" size="small">
                  {{ detailRow[`followup${month}Result`] }}
                </el-tag>
                <span class="ml-2 text-sm text-gray-500">实际筛查日期：{{ detailRow[`followup${month}ScreenDate`] }}</span>
              </template>
              <template v-else>
                <span class="text-gray-400 text-sm">尚未完成</span>
              </template>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
        <el-tab-pane label="预防治疗">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="是否开展预防治疗">
              {{ detailRow.hasPreventiveTreatment }}
            </el-descriptions-item>
            <el-descriptions-item label="预防性治疗方案">
              {{ detailRow.preventivePlan }}
            </el-descriptions-item>
            <el-descriptions-item label="是否完成治疗">
              {{ detailRow.treatmentCompleted }}
            </el-descriptions-item>
            <el-descriptions-item label="未完成原因">
              {{ detailRow.incompleteReason }}
            </el-descriptions-item>
            <el-descriptions-item label="预计完成时间">
              {{ detailRow.expectedTreatmentEndDate }}
            </el-descriptions-item>
            <el-descriptions-item label="备注">
              {{ detailRow.remark }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 3月复查弹窗 -->
    <el-dialog v-model="threeMonthDialogVisible" title="填写3月复查结果" width="480px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="复查日期" required>
          <el-date-picker
            v-model="threeMonthForm.checkDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择复查日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="感染检测结果" required>
          <el-input v-model="threeMonthForm.checkResult" placeholder="请输入感染检测结果（如 PPD阴性、IGRA阴性等）" />
        </el-form-item>
        <el-form-item label="最终判定结果" required>
          <el-radio-group v-model="threeMonthForm.finalResult">
            <el-radio value="阴性">
              阴性（结束流程）
            </el-radio>
            <el-radio value="阳性">
              阳性（转入潜伏感染流程）
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="threeMonthDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="threeMonthSubmitting" @click="handleThreeMonthSubmit">
          提交
        </el-button>
      </template>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="tierCareRow"
      v-model="tierCareVisible"
      :biz-id="tierCareRow.id"
      biz-type="screening_close"
      population-type="close"
      module-type="screening"
      :subject-name="tierCareRow.name || ''"
    />

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert :title="`成功导入 ${importResult.successCount} 条数据`" type="success" :closable="false" class="mb-3" />
      <template v-if="importResult.errors.length > 0">
        <el-alert :title="`发现 ${importResult.errors.length} 条数据格式问题（已照常导入，请核查）`" type="warning" :closable="false" class="mb-3" />
        <el-table :data="importResult.errors.map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" width="50" />
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
.stat-card {
  cursor: pointer;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}
</style>
