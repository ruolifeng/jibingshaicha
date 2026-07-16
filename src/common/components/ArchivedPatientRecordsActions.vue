<script lang="ts" setup>
import type { FollowUpHistoryDisplayRow } from "@@/utils/followUpVisit"
/** 历史患者 — 查看关联记录（首次随访、后续随访、服药管理、通知单等） */
import FirstVisitDetailDialog from "@@/components/FirstVisitDetailDialog.vue"
import FollowUpVisitDetailDialog from "@@/components/FollowUpVisitDetailDialog.vue"
import PatientMedicationDialog from "@@/components/PatientMedicationDialog.vue"
import PatientNoticeDetailDialog from "@@/components/PatientNoticeDetailDialog.vue"
import PatientRecordDetailDialog from "@@/components/PatientRecordDetailDialog.vue"
import PrintFirstVisit from "@@/components/PrintFirstVisit.vue"
import PrintFollowUp from "@@/components/PrintFollowUp.vue"
import {
  buildFollowUpHistoryDisplayList,
  canEditFollowUpVisit

} from "@@/utils/followUpVisit"
import {
  deleteFollowUpVisitApi,
  getFirstVisitDetailApi,
  getFollowUpVisitListApi,
  getMedicationDetailApi
} from "@/pages/patient-management/apis"
import { useUserStore } from "@/pinia/stores/user"

const props = defineProps<{
  row: Record<string, any>
}>()

const userStore = useUserStore()

const detailVisible = ref(false)
const firstVisitDetailVisible = ref(false)
const firstVisitDetailData = ref<Record<string, any> | null>(null)
const followUpListVisible = ref(false)
const followUpListData = ref<FollowUpHistoryDisplayRow[]>([])
const followUpDetailVisible = ref(false)
const followUpDetailData = ref<Record<string, any> | null>(null)
const followUpPrintVisible = ref(false)
const followUpPrintData = ref<Record<string, any> | null>(null)
const firstVisitPrintVisible = ref(false)
const firstVisitPrintData = ref<Record<string, any> | null>(null)
const medicationVisible = ref(false)
const noticeVisible = ref(false)

function openDetail() {
  detailVisible.value = true
}

async function viewFirstVisit() {
  try {
    const { data } = await getFirstVisitDetailApi(props.row.id)
    if (data) {
      firstVisitDetailData.value = data
      firstVisitDetailVisible.value = true
    } else {
      ElMessage.info("暂无首次随访记录")
    }
  } catch { /* handled */ }
}

async function viewFollowUpList() {
  try {
    const [followUpRes, firstVisitRes] = await Promise.all([
      getFollowUpVisitListApi(props.row.id),
      getFirstVisitDetailApi(props.row.id).catch(() => ({ data: null }))
    ])
    followUpListData.value = buildFollowUpHistoryDisplayList(firstVisitRes.data, followUpRes.data || [])
    followUpListVisible.value = true
  } catch { /* handled */ }
}

function viewFollowUpDetail(record: FollowUpHistoryDisplayRow) {
  if (record.recordType === "firstVisit") {
    firstVisitDetailData.value = record.raw
    firstVisitDetailVisible.value = true
    return
  }
  followUpDetailData.value = record.raw
  followUpDetailVisible.value = true
}

function printFollowUp(record: FollowUpHistoryDisplayRow) {
  if (record.recordType === "firstVisit") {
    firstVisitPrintData.value = record.raw
    firstVisitPrintVisible.value = true
    return
  }
  followUpPrintData.value = record.raw
  followUpPrintVisible.value = true
}

async function refreshFollowUpList() {
  const [followUpRes, firstVisitRes] = await Promise.all([
    getFollowUpVisitListApi(props.row.id),
    getFirstVisitDetailApi(props.row.id).catch(() => ({ data: null }))
  ])
  followUpListData.value = buildFollowUpHistoryDisplayList(firstVisitRes.data, followUpRes.data || [])
}

async function handleDeleteFollowUp(record: FollowUpHistoryDisplayRow) {
  if (record.recordType !== "followUp" || !record.id) return
  try {
    await ElMessageBox.confirm(
      `确认删除第 ${record.visitSeq} 次后续随访记录？删除后不可恢复。`,
      "删除确认",
      { type: "warning" }
    )
    await deleteFollowUpVisitApi(record.id)
    ElMessage.success("随访记录已删除")
    await refreshFollowUpList()
  } catch { /* cancel or handled */ }
}

async function viewMedication() {
  try {
    const { data } = await getMedicationDetailApi(props.row.id)
    if (!data) {
      ElMessage.info("暂无服药管理记录")
      return
    }
    medicationVisible.value = true
  } catch { /* handled */ }
}

function viewNotice() {
  noticeVisible.value = true
}
</script>

<template>
  <div class="archived-record-actions">
    <el-button type="primary" link size="small" @click="openDetail">
      基本信息
    </el-button>
    <el-button type="info" link size="small" @click="viewFirstVisit">
      首次随访
    </el-button>
    <el-button type="info" link size="small" @click="viewFollowUpList">
      后续随访
    </el-button>
    <el-button type="info" link size="small" @click="viewMedication">
      服药管理
    </el-button>
    <el-button type="info" link size="small" @click="viewNotice">
      通知单
    </el-button>

    <PatientRecordDetailDialog
      v-model:visible="detailVisible"
      :patient-id="row.id"
    />

    <FirstVisitDetailDialog
      v-model:visible="firstVisitDetailVisible"
      :visit-data="firstVisitDetailData"
      :patient-name="row.name"
    />

    <el-dialog
      v-model="followUpListVisible"
      :title="`${row.name} - 随访记录`"
      width="900px"
      append-to-body
    >
      <el-table :data="followUpListData" border stripe>
        <el-table-column prop="visitSeq" label="第几次" width="80" />
        <el-table-column prop="visitDate" label="随访日期" />
        <el-table-column prop="treatmentMonth" label="治疗月序" />
        <el-table-column prop="visitMethodLabel" label="随访方式" />
        <el-table-column prop="missedDoses" label="漏服次数" />
        <el-table-column prop="nextVisitDate" label="下次随访">
          <template #default="{ row: record }">
            {{ record.nextVisitDate || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="doctorSignature" label="医生签名" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row: record }">
            <el-button type="primary" link size="small" @click="viewFollowUpDetail(record)">
              查看详情
            </el-button>
            <el-button
              v-if="record.recordType === 'followUp' && canEditFollowUpVisit(userStore.userRole, record)"
              v-permission="'patientManagement:followUp:edit'"
              type="danger"
              link
              size="small"
              @click="handleDeleteFollowUp(record)"
            >
              删除
            </el-button>
            <el-button type="info" link size="small" @click="printFollowUp(record)">
              打印
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!followUpListData.length" description="暂无随访记录" />
    </el-dialog>

    <FollowUpVisitDetailDialog
      v-model:visible="followUpDetailVisible"
      :visit-data="followUpDetailData"
      :patient-name="row.name"
    />

    <PrintFollowUp
      v-if="followUpPrintData"
      :visible="followUpPrintVisible"
      :visit-data="followUpPrintData"
      :patient-name="row.name"
      @update:visible="followUpPrintVisible = $event"
    />

    <PrintFirstVisit
      v-if="firstVisitPrintData"
      v-model:visible="firstVisitPrintVisible"
      :visit-data="firstVisitPrintData"
      :patient-name="row.name"
    />

    <PatientMedicationDialog
      v-model:visible="medicationVisible"
      :patient-row="row"
      read-only
    />

    <PatientNoticeDetailDialog
      v-model:visible="noticeVisible"
      :patient-row="row"
    />
  </div>
</template>

<style lang="scss" scoped>
.archived-record-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
