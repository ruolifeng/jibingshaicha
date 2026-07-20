<script lang="ts" setup>
import type { FollowUpHistoryDisplayRow } from "@@/utils/followUpVisit"
import FirstVisitDetailDialog from "@@/components/FirstVisitDetailDialog.vue"
import FollowUpVisitDetailDialog from "@@/components/FollowUpVisitDetailDialog.vue"
import FollowUpVisitDialog from "@@/components/FollowUpVisitDialog.vue"
import PrintFirstVisit from "@@/components/PrintFirstVisit.vue"
import PrintFollowUp from "@@/components/PrintFollowUp.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_FILTER_OPTIONS } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import {
  buildFollowUpHistoryDisplayList,
  canEditFollowUpVisit,
  toFollowUpHistoryViewData
} from "@@/utils/followUpVisit"
import { getPatientTransferStatusLabel, isPatientTransferLocked } from "@@/utils/patient"
import { useUserStore } from "@/pinia/stores/user"
import { deleteFollowUpVisitApi, exportPatientFollowUpVisitsApi, getFirstVisitDetailApi, getFollowUpVisitListApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"

const userStore = useUserStore()

const { paginationData, handleCurrentChange, handleSizeChange, getTableIndex, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0, { followUpSearch: true })

const selectedRows = ref<any[]>([])
const exporting = ref(false)

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function handleExportSelected() {
  const ids = selectedRows.value.map(r => r.id).filter(Boolean)
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的患者")
    return
  }
  try {
    await ElMessageBox.confirm(`确认导出选中的 ${ids.length} 位患者的后续随访信息吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportPatientFollowUpVisitsApi(ids)
    downloadBlob(blob as unknown as Blob, "后续随访.xlsx")
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}

const followUpDialogVisible = ref(false)
const followUpPatient = ref<any>(null)

function openFollowUp(row: any) {
  followUpPatient.value = row
  followUpDialogVisible.value = true
}

const historyVisible = ref(false)
const historyList = ref<FollowUpHistoryDisplayRow[]>([])
const historyPatientName = ref("")
const historyPatient = ref<any>(null)
const historyDialogTitle = computed(() => `${historyPatientName.value} - 随访记录`)

const editDialogVisible = ref(false)
const editVisit = ref<Record<string, any> | null>(null)

const detailVisible = ref(false)
const detailData = ref<Record<string, any> | null>(null)

const firstVisitDetailVisible = ref(false)
const firstVisitDetailData = ref<Record<string, any> | null>(null)

const printVisible = ref(false)
const printData = ref<Record<string, any> | null>(null)
const printPatientName = ref("")

const firstVisitPrintVisible = ref(false)
const firstVisitPrintData = ref<Record<string, any> | null>(null)

async function loadFollowUpHistory(patientId: number) {
  const [followUpRes, firstVisitRes] = await Promise.all([
    getFollowUpVisitListApi(patientId),
    getFirstVisitDetailApi(patientId).catch(() => ({ data: null }))
  ])
  historyList.value = buildFollowUpHistoryDisplayList(firstVisitRes.data, followUpRes.data || [])
}

async function viewHistory(row: any) {
  historyPatient.value = row
  historyPatientName.value = row.name
  await loadFollowUpHistory(row.id)
  historyVisible.value = true
}

async function refreshHistoryList() {
  if (!historyPatient.value) return
  await loadFollowUpHistory(historyPatient.value.id)
}

function openEdit(record: FollowUpHistoryDisplayRow) {
  if (record.recordType !== "followUp") return
  editVisit.value = record.raw
  editDialogVisible.value = true
}

async function onEditSaved() {
  await refreshHistoryList()
  fetchData()
}

function viewDetail(row: FollowUpHistoryDisplayRow) {
  if (row.recordType === "firstVisit") {
    firstVisitDetailData.value = row.raw
    firstVisitDetailVisible.value = true
    return
  }
  detailData.value = toFollowUpHistoryViewData(row)
  detailVisible.value = true
}

function openPrint(row: FollowUpHistoryDisplayRow) {
  if (row.recordType === "firstVisit") {
    firstVisitPrintData.value = row.raw
    firstVisitPrintVisible.value = true
    return
  }
  printData.value = toFollowUpHistoryViewData(row)
  printPatientName.value = historyPatientName.value
  printVisible.value = true
}

async function handleDelete(record: FollowUpHistoryDisplayRow) {
  if (!historyPatient.value || record.recordType !== "followUp" || !record.id) return
  try {
    await ElMessageBox.confirm(
      `确认删除第 ${record.visitSeq} 次随访记录？删除后不可恢复。`,
      "删除确认",
      { type: "warning" }
    )
    await deleteFollowUpVisitApi(record.id)
    ElMessage.success("随访记录已删除")
    await refreshHistoryList()
    fetchData()
  } catch { /* cancel or handled */ }
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
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable filterable style="width:140px">
            <el-option v-for="item in PATHOGEN_RESULT_FILTER_OPTIONS" :key="item" :label="item" :value="item" />
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
          <el-input
            v-model="searchForm.medicationManagementUnit"
            placeholder="请输入"
            clearable
            style="width: 160px"
          />
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
      <div style="margin-bottom: 10px">
        <el-button
          type="success"
          :loading="exporting"
          :disabled="!selectedRows.length"
          @click="handleExportSelected"
        >
          导出
        </el-button>
      </div>
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column type="index" label="#" :index="getTableIndex" />
        <el-table-column label="数据来源">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="病原学结果" />
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <template v-if="!isPatientTransferLocked(row)">
              <el-button
                v-permission="'patientManagement:followUp:fill'" type="primary" link size="small"
                :disabled="row.archived === 1"
                @click="openFollowUp(row)"
              >
                填写后续随访
              </el-button>
            </template>
            <el-button type="info" link size="small" @click="viewHistory(row)">
              查看记录
            </el-button>
            <el-tag
              v-if="isPatientTransferLocked(row)"
              :type="row.archiveRemark === '已转出' ? 'info' : 'warning'"
              size="small"
              class="ml-1"
            >
              {{ getPatientTransferStatusLabel(row.archiveRemark) }}
            </el-tag>
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

    <FollowUpVisitDialog
      v-if="followUpPatient"
      v-model:visible="followUpDialogVisible"
      :patient-id="followUpPatient.id"
      :patient-name="followUpPatient.name"
      :patient-row="followUpPatient"
      :population-type="followUpPatient.populationType"
      @saved="fetchData"
    />

    <FollowUpVisitDialog
      v-if="historyPatient && editVisit"
      v-model:visible="editDialogVisible"
      :patient-id="historyPatient.id"
      :patient-name="historyPatient.name"
      :patient-row="historyPatient"
      :population-type="historyPatient.populationType"
      :initial-data="editVisit"
      @saved="onEditSaved"
    />

    <el-dialog
      v-model="historyVisible"
      :title="historyDialogTitle"
      width="900px"
      append-to-body
    >
      <el-table :data="historyList" border stripe>
        <el-table-column prop="visitSeq" label="第几次" width="80" />
        <el-table-column prop="visitDate" label="随访日期" />
        <el-table-column prop="treatmentMonth" label="治疗月序" />
        <el-table-column prop="visitMethodLabel" label="随访方式" />
        <el-table-column prop="missedDoses" label="漏服次数" />
        <el-table-column prop="nextVisitDate" label="下次随访">
          <template #default="{ row }">
            {{ row.nextVisitDate || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="doctorSignature" label="医生签名" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="row.recordType === 'followUp' && canEditFollowUpVisit(userStore.userRole, row) && !isPatientTransferLocked(historyPatient)"
              v-permission="'patientManagement:followUp:edit'"
              type="warning"
              link
              size="small"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-if="row.recordType === 'followUp' && canEditFollowUpVisit(userStore.userRole, row) && !isPatientTransferLocked(historyPatient)"
              v-permission="'patientManagement:followUp:edit'"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button type="info" link size="small" @click="openPrint(row)">
              打印
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyList.length" description="暂无随访记录" />
    </el-dialog>

    <FollowUpVisitDetailDialog
      v-model:visible="detailVisible"
      :visit-data="detailData"
      :patient-name="historyPatientName"
    />

    <FirstVisitDetailDialog
      v-model:visible="firstVisitDetailVisible"
      :visit-data="firstVisitDetailData"
      :patient-name="historyPatientName"
    />

    <PrintFollowUp
      v-if="printData"
      :visible="printVisible"
      :visit-data="printData"
      :patient-name="printPatientName"
      @update:visible="printVisible = $event"
    />

    <PrintFirstVisit
      v-if="firstVisitPrintData"
      v-model:visible="firstVisitPrintVisible"
      :visit-data="firstVisitPrintData"
      :patient-name="historyPatientName"
    />
  </div>
</template>
