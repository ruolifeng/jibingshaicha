<script lang="ts" setup>
import LatentRecordDetailDialog from "@@/components/LatentRecordDetailDialog.vue"
import LatentRecordEditDialog from "@@/components/LatentRecordEditDialog.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import { LATENT_IMPORT_FIELDS } from "@@/constants/latent-import"
import { getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { extractDateRangeParams } from "@@/utils/searchParams"
import { batchDeleteLatentApi, downloadLatentTemplateApi, exportAllLatentApi, importLatentApi } from "./apis"
import { useLatentOverviewList } from "./composables/useLatentOverviewList"

const {
  paginationData, handleCurrentChange, handleSizeChange,
  loading, tableData, total, searchForm, fetchData, handleSearch, handleReset
} = useLatentOverviewList()

const detailVisible = ref(false)
const editVisible = ref(false)
const currentId = ref<number | null>(null)
const exporting = ref(false)
const importing = ref(false)
const templateDownloading = ref(false)
const importDialogVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref<{ successCount: number, errors: string[] }>({ successCount: 0, errors: [] })
const selectedRows = ref<any[]>([])

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
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

// ==================== 转出 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)

function openReferral(row: any) {
  referralRow.value = row
  referralDialogVisible.value = true
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportAllLatentApi({
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      phone: searchForm.phone || undefined,
      populationType: searchForm.populationType || undefined,
      ...extractDateRangeParams(searchForm.dateRange)
    })
    downloadBlob(blob as unknown as Blob, "在管潜伏感染者信息总表.xlsx")
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
      `确定删除选中的 ${selectedRows.value.length} 条记录（${names}）吗？关联的通知单、督导表、患者等数据将一并删除，且不可恢复！`,
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

function openImportDialog() {
  importResult.value = { successCount: 0, errors: [] }
  importDialogVisible.value = true
}

async function handleDownloadTemplate() {
  templateDownloading.value = true
  try {
    const blob = await downloadLatentTemplateApi()
    downloadBlob(blob as unknown as Blob, "潜伏感染者导入模板.xlsx")
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
    const { data } = await importLatentApi(file)
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
</script>

<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
      :title="`当前在管潜伏感染者共 ${total} 人`"
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
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width: 140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="疫情筛查" value="regular" />
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
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
      <div class="toolbar flex items-center justify-end gap-2" style="margin-bottom: 12px">
        <el-button
          v-permission="'latentManagement:overview'"
          type="primary"
          @click="openCreate"
        >
          新增
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
          type="primary"
          plain
          @click="openImportDialog"
        >
          导入
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
          type="danger"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          删除
        </el-button>
        <el-button
          v-permission="'latentManagement:overview'"
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
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" show-overflow-tooltip />
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-tag v-if="row.noticeStatus === 1 || row.noticeStatus === 2" type="success" size="small">
              已发送
            </el-tag>
            <el-tag v-else-if="row.noticeStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <el-tag v-else type="info" size="small">
              未发送
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据来源">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-permission="'latentManagement:edit'"
              type="warning"
              link
              size="small"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-permission="'latentManagement:referral'"
              type="info"
              link
              size="small"
              @click="openReferral(row)"
            >
              转出
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

    <LatentRecordDetailDialog
      v-model:visible="detailVisible"
      :latent-id="currentId"
    />
    <LatentRecordEditDialog
      v-model:visible="editVisible"
      :latent-id="currentId"
      @success="fetchData"
    />

    <!-- 转出弹窗 -->
    <ReferralDialog
      v-if="referralRow"
      v-model="referralDialogVisible"
      :biz-id="referralRow.id"
      biz-type="latent_aggregate"
      module-type="latent"
      :population-type="referralRow.populationType"
      :subject-name="referralRow.name || ''"
    />

    <el-dialog v-model="importDialogVisible" title="批量导入潜伏感染者" width="560px">
      <el-alert
        type="info"
        :closable="false"
        class="mb-3"
        title="请先下载模板，按表头填写数据后上传。字段与「新增」表单一致。"
      />
      <div class="mb-3">
        <p class="text-sm text-gray-500 mb-2">
          模板包含字段：{{ LATENT_IMPORT_FIELDS.join("、") }}
        </p>
        <p class="text-sm text-gray-500">
          数据来源可填写：学生筛查、重点人群、疫情筛查、大疫情、推介
        </p>
        <p class="text-sm text-gray-500 mt-2">
          证件号、联系电话列建议设为「文本」格式，避免 Excel 自动转换导致校验失败
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
          :title="`发现 ${importResult.errors?.length ?? 0} 条数据存在问题（已跳过）`"
          type="warning"
          :closable="false"
          class="mb-3"
        />
        <el-table :data="(importResult.errors ?? []).map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" width="60" />
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
