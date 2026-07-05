<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { CLOSE_CONTACT_CASE_COLUMNS, DIAGNOSIS_RESULT_OPTIONS, HAS_PREVENTIVE_TREATMENT_OPTIONS } from "@@/constants/close-contact-case"
import { downloadBlob } from "@@/utils/download"
import { extractCreateTimeRangeParams } from "@@/utils/searchParams"
import {
  batchDeleteCloseContactCaseApi,
  createCloseContactCaseApi,
  deleteCloseContactCaseApi,
  downloadCloseContactCaseTemplateApi,
  exportCloseContactCaseApi,
  getCloseContactCaseListApi,
  updateCloseContactCaseApi,
  uploadCloseContactCaseApi
} from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const tableRef = ref<any>()

const searchForm = reactive({
  name: "",
  idNumber: "",
  district: "",
  phone: "",
  creatorUsername: "",
  diagnosisResult: "",
  entryTimeRange: [] as string[]
})

const previewColumns = CLOSE_CONTACT_CASE_COLUMNS

async function fetchData() {
  loading.value = true
  try {
    const { entryTimeRange, ...rest } = searchForm
    const res = await getCloseContactCaseListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...rest,
      name: rest.name || undefined,
      idNumber: rest.idNumber || undefined,
      district: rest.district || undefined,
      phone: rest.phone || undefined,
      creatorUsername: rest.creatorUsername || undefined,
      diagnosisResult: rest.diagnosisResult || undefined,
      ...extractCreateTimeRangeParams(entryTimeRange)
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
    selectedRows.value = []
    tableRef.value?.clearCheckboxRow?.()
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
  searchForm.phone = ""
  searchForm.creatorUsername = ""
  searchForm.diagnosisResult = ""
  searchForm.entryTimeRange = []
  handleSearch()
}

const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, errors: string[] }>({ successCount: 0, errors: [] })
const templateDownloading = ref(false)
const selectedRows = ref<any[]>([])

function syncSelectedRows() {
  selectedRows.value = tableRef.value?.getCheckboxRecords?.() ?? []
}

function handleCheckboxChange() {
  syncSelectedRows()
}

async function handleDownloadTemplate() {
  templateDownloading.value = true
  try {
    const blob = await downloadCloseContactCaseTemplateApi()
    downloadBlob(blob as unknown as Blob, "密接个案表模板.xlsx")
    ElMessage.success("模板下载成功")
  } catch {
    ElMessage.error("模板下载失败")
  } finally {
    templateDownloading.value = false
  }
}

async function handleUpload(uploadFile: any) {
  try {
    const { data } = await uploadCloseContactCaseApi(uploadFile.raw)
    importResult.value = data
    importResultVisible.value = true
  } catch (err: any) {
    ElMessage.error(err?.message || "上传失败")
    return
  }
  fetchData()
}

function getSelectedRows() {
  return selectedRows.value
}

function buildExportParams(exportType?: "latent" | "confirmed") {
  const { entryTimeRange, ...rest } = searchForm
  return {
    name: rest.name || undefined,
    idNumber: rest.idNumber || undefined,
    district: rest.district || undefined,
    phone: rest.phone || undefined,
    creatorUsername: rest.creatorUsername || undefined,
    diagnosisResult: rest.diagnosisResult || undefined,
    ...extractCreateTimeRangeParams(entryTimeRange),
    exportType
  }
}

async function handleExport(ids?: number[], exportType?: "latent" | "confirmed") {
  const isSelectedExport = !!ids?.length
  const label = exportType === "latent"
    ? "潜伏感染者"
    : exportType === "confirmed"
      ? "确诊患者"
      : isSelectedExport
        ? `选中的 ${ids!.length} 条`
        : "全部"
  try {
    await ElMessageBox.confirm(`确认导出${label}数据吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    const blob = await exportCloseContactCaseApi(
      isSelectedExport
        ? { ids }
        : { ...buildExportParams(exportType) }
    )
    const filename = exportType === "latent"
      ? "密接个案表_潜伏感染者.xlsx"
      : exportType === "confirmed"
        ? "密接个案表_确诊患者.xlsx"
        : "密接个案表.xlsx"
    downloadBlob(blob as unknown as Blob, filename)
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error(err?.message || "导出失败")
  }
}

function handleExportSelected() {
  const rows = getSelectedRows()
  const ids = rows.map((item: any) => item.id).filter(Boolean)
  if (!ids.length) {
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

function getEmptyEditForm() {
  return {
    city: "",
    district: "",
    sourcePatientName: "",
    sourcePatientCaseNo: "",
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
      await createCloseContactCaseApi(editForm.value)
      ElMessage.success("新增成功")
    } else {
      await updateCloseContactCaseApi(editForm.value.id, editForm.value)
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
      `确定删除「${row.name}」的个案记录吗？删除后不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    await deleteCloseContactCaseApi(row.id)
    ElMessage.success("删除成功")
    fetchData()
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("删除失败")
  }
}

async function handleBatchDelete() {
  const rows = getSelectedRows()
  if (!rows.length) {
    ElMessage.warning("请先勾选要删除的数据")
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${rows.length} 条个案记录吗？删除后不可恢复！`,
      "危险操作确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning", confirmButtonClass: "el-button--danger" }
    )
    const ids = rows.map((r: any) => r.id)
    await batchDeleteCloseContactCaseApi(ids)
    ElMessage.success(`成功删除 ${ids.length} 条记录`)
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

type TagType = "primary" | "success" | "info" | "warning" | "danger"

function getDiagnosisTag(result: string): TagType {
  if (result === "活动性肺结核") return "danger"
  if (result === "潜伏感染者") return "warning"
  if (result === "未发现异常") return "success"
  return "info"
}

watch(() => [paginationData.currentPage, paginationData.pageSize], fetchData, { immediate: true })
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="接触者姓名" clearable />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="searchForm.idNumber" placeholder="身份证号" clearable />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="searchForm.phone" placeholder="接触者电话" clearable />
        </el-form-item>
        <el-form-item label="区县">
          <el-input v-model="searchForm.district" placeholder="区/县" clearable />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.creatorUsername" placeholder="录入账号" clearable />
        </el-form-item>
        <el-form-item label="最终筛查结果">
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="opt in DIAGNOSIS_RESULT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
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

    <!-- 电子表格预览 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between flex-wrap gap-2">
          <div>
            <span class="text-lg font-bold">密接个案表</span>
            <span class="text-sm text-gray-400 ml-2">线上预览模式 · 横向滚动查看全部字段</span>
          </div>
          <div class="flex gap-2 flex-wrap">
            <el-button v-permission="'closeContact:case:create'" type="success" @click="handleCreate">
              新增
            </el-button>
            <el-button
              v-permission="'closeContact:case:upload'"
              type="success"
              plain
              :loading="templateDownloading"
              @click="handleDownloadTemplate"
            >
              下载模板
            </el-button>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleUpload">
              <el-button v-permission="'closeContact:case:upload'" type="primary">
                导入 Excel
              </el-button>
            </el-upload>
            <el-button v-permission="'closeContact:case:export'" @click="() => handleExport()">
              导出全部
            </el-button>
            <el-button v-permission="'closeContact:case:export'" type="warning" :disabled="!selectedRows.length" @click="handleExportSelected">
              导出勾选
            </el-button>
            <el-button v-permission="'closeContact:case:export'" type="warning" plain @click="() => handleExport(undefined, 'latent')">
              导出潜伏感染者
            </el-button>
            <el-button v-permission="'closeContact:case:export'" type="danger" plain @click="() => handleExport(undefined, 'confirmed')">
              导出确诊患者
            </el-button>
            <el-button v-permission="'closeContact:case:delete'" type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">
              批量删除
            </el-button>
          </div>
        </div>
      </template>

      <div class="spreadsheet-wrap">
        <vxe-table
          ref="tableRef"
          :data="tableData"
          :loading="loading"
          border
          stripe
          height="620"
          :row-config="{ keyField: 'id' }"
          :column-config="{ resizable: true }"
          :scroll-x="{ enabled: true, gt: 0 }"
          :scroll-y="{ enabled: true, gt: 0 }"
          show-overflow
          show-header-overflow
          @checkbox-change="handleCheckboxChange"
          @checkbox-all="handleCheckboxChange"
        >
          <vxe-column type="checkbox" width="40" fixed="left" />
          <vxe-column
            v-for="col in previewColumns"
            :key="col.field"
            :field="col.field"
            :title="col.title"
            :min-width="col.width"
            :fixed="col.fixed"
          >
            <template v-if="col.field === 'finalScreeningResult'" #default="{ row }">
              <el-tag v-if="row.finalScreeningResult" :type="getDiagnosisTag(row.finalScreeningResult)" size="small">
                {{ row.finalScreeningResult }}
              </el-tag>
              <span v-else class="text-gray-400">—</span>
            </template>
          </vxe-column>
          <vxe-column title="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">
                详情
              </el-button>
              <el-button v-permission="'closeContact:case:edit'" type="warning" link size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button v-permission="'closeContact:case:delete'" type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </vxe-column>
        </vxe-table>
      </div>

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
    <el-dialog v-model="editVisible" :title="editMode === 'create' ? '新增个案' : '编辑个案'" width="960px" :close-on-click-modal="false">
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
                <el-form-item label="患者姓名">
                  <el-input v-model="editForm.sourcePatientName" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="传报卡号">
                  <el-input v-model="editForm.sourcePatientCaseNo" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="患者电话">
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
                <el-form-item label="接触者电话">
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
        <el-tab-pane label="筛查与诊断">
          <el-form :model="editForm" label-width="130px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="首次筛查日期">
                  <el-date-picker v-model="editForm.firstScreenDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测方法">
                  <el-input v-model="editForm.infectionCheckMethod" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="感染检测结果">
                  <el-input v-model="editForm.infectionCheckResult" />
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
                    <el-option v-for="opt in DIAGNOSIS_RESULT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
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
                  <el-select v-model="editForm.hasPreventiveTreatment" style="width:100%" clearable placeholder="请选择">
                    <el-option v-for="opt in HAS_PREVENTIVE_TREATMENT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
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
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name} — 密接个案详情`" width="900px">
      <el-descriptions v-if="detailRow" :column="3" border>
        <el-descriptions-item label="录入用户">
          {{ detailRow.creatorUsername || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="接触者姓名">
          {{ detailRow.name }}
        </el-descriptions-item>
        <el-descriptions-item label="身份证号">
          {{ detailRow.idNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          {{ detailRow.phone }}
        </el-descriptions-item>
        <el-descriptions-item label="市/州">
          {{ detailRow.city }}
        </el-descriptions-item>
        <el-descriptions-item label="区/县">
          {{ detailRow.district }}
        </el-descriptions-item>
        <el-descriptions-item label="患者姓名">
          {{ detailRow.sourcePatientName }}
        </el-descriptions-item>
        <el-descriptions-item label="密切接触者登记日期">
          {{ detailRow.registrationDate }}
        </el-descriptions-item>
        <el-descriptions-item label="最终筛查结果" :span="3">
          <el-tag v-if="detailRow.finalScreeningResult" :type="getDiagnosisTag(detailRow.finalScreeningResult)">
            {{ detailRow.finalScreeningResult }}
          </el-tag>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="感染检测方法">
          {{ detailRow.infectionCheckMethod }}
        </el-descriptions-item>
        <el-descriptions-item label="结果判定">
          {{ detailRow.infectionCheckResult }}
        </el-descriptions-item>
        <el-descriptions-item label="影像结果">
          {{ detailRow.imagingResult }}
        </el-descriptions-item>
        <el-descriptions-item label="6月随访结果">
          {{ detailRow.followup6Result || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="12月随访结果">
          {{ detailRow.followup12Result || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="24月随访结果">
          {{ detailRow.followup24Result || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">
          {{ detailRow.remark || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="录入时间" :span="3">
          {{ detailRow.createTime }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert :title="`成功导入 ${importResult.successCount} 条数据`" type="success" :closable="false" class="mb-3" />
      <template v-if="importResult.errors.length > 0">
        <el-alert :title="`发现 ${importResult.errors.length} 条数据格式问题（已照常导入，请核查）`" type="warning" :closable="false" class="mb-3" />
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
.spreadsheet-wrap {
  width: 100%;
  overflow: hidden;

  :deep(.vxe-table) {
    font-size: 13px;
  }

  :deep(.vxe-header--column) {
    background-color: var(--el-fill-color-light);
    font-weight: 600;
  }
}
</style>
