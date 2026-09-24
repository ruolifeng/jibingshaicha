<script lang="ts" setup>
import type { PendingNoticeItem } from "../apis"
import LatentNoticeDetailDialog from "@@/components/LatentNoticeDetailDialog.vue"
import PatientNoticeDetailDialog from "@@/components/PatientNoticeDetailDialog.vue"
import { getPopulationTypeLabel } from "@@/constants/disease"
import { getDashboardPendingNoticesApi } from "../apis"

const props = defineProps<{
  departmentIds?: string[]
  /** 是否展开明细表；由父级「待确认通知单」卡片控制 */
  expanded: boolean
}>()

const emit = defineEmits<{
  (e: "confirmed"): void
}>()

const loading = ref(false)
const tableData = ref<PendingNoticeItem[]>([])

const latentDetailVisible = ref(false)
const latentDetailRow = ref<Record<string, any> | null>(null)
const patientDetailVisible = ref(false)
const patientDetailRow = ref<Record<string, any> | null>(null)

async function fetchData() {
  if (!props.expanded) return
  loading.value = true
  try {
    const { data } = await getDashboardPendingNoticesApi(props.departmentIds)
    tableData.value = data ?? []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function noticeTypeLabel(type?: string) {
  if (type === "patient") return "患者通知单"
  if (type === "latent") return "潜伏者通知单"
  return type || "—"
}

function viewNotice(row: PendingNoticeItem) {
  if (!row.id) {
    ElMessage.warning("通知单编号缺失")
    return
  }
  if (row.noticeType === "patient") {
    patientDetailRow.value = {
      id: row.bizId,
      name: row.patientName,
      noticeId: row.id,
      noticeStatus: row.status
    }
    patientDetailVisible.value = true
    return
  }
  latentDetailRow.value = {
    id: row.bizId,
    name: row.patientName,
    noticeId: row.id,
    noticeStatus: row.status
  }
  latentDetailVisible.value = true
}

function onNoticeConfirmed() {
  fetchData()
  emit("confirmed")
}

watch(
  () => [props.expanded, props.departmentIds] as const,
  ([expanded]) => {
    if (expanded) fetchData()
  },
  { deep: true, immediate: true }
)

defineExpose({ refresh: fetchData })
</script>

<template>
  <div v-if="expanded" class="pending-notice-panel">
    <el-table v-loading="loading" :data="tableData" border stripe size="small">
      <el-table-column label="状态" width="100">
        <template #default>
          <el-tag type="warning" size="small">
            待确认
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="通知单类型" min-width="120">
        <template #default="{ row }">
          {{ noticeTypeLabel(row.noticeType) }}
        </template>
      </el-table-column>
      <el-table-column prop="patientName" label="姓名" min-width="110" show-overflow-tooltip />
      <el-table-column label="数据来源" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.populationType ? getPopulationTypeLabel(row.populationType) : "—" }}
        </template>
      </el-table-column>
      <el-table-column label="发送方" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.senderName || row.senderOrgName || "—" }}
        </template>
      </el-table-column>
      <el-table-column label="接收方" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.receiverName || row.receiverOrgName || "—" }}
        </template>
      </el-table-column>
      <el-table-column prop="sentTime" label="发送时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewNotice(row)">
            查看通知单
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无待确认通知单" :image-size="64" />
      </template>
    </el-table>

    <LatentNoticeDetailDialog
      v-model:visible="latentDetailVisible"
      :latent-row="latentDetailRow"
      @success="onNoticeConfirmed"
    />
    <PatientNoticeDetailDialog
      v-model:visible="patientDetailVisible"
      :patient-row="patientDetailRow"
      @success="onNoticeConfirmed"
    />
  </div>
</template>

<style scoped lang="scss">
.pending-notice-panel {
  margin: 0 0 20px;
}
</style>
