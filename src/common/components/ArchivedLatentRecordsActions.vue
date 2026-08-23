<script lang="ts" setup>
/** 历史潜伏感染者 — 查看关联记录（基本信息、通知单、督导表、服药管理） */
import LatentMedicationDialog from "@@/components/LatentMedicationDialog.vue"
import LatentNoticeDetailDialog from "@@/components/LatentNoticeDetailDialog.vue"
import LatentRecordDetailDialog from "@@/components/LatentRecordDetailDialog.vue"
import SupervisionFormDetailDialog from "@@/components/SupervisionFormDetailDialog.vue"
import {
  getLatentMedicationApi,
  getSupervisionByIdApi,
  getSupervisionListApi
} from "@/pages/latent-management/apis"

const props = defineProps<{
  row: Record<string, any>
}>()

const emit = defineEmits<{
  (e: "success"): void
}>()

const detailVisible = ref(false)
const noticeVisible = ref(false)
const supervisionListVisible = ref(false)
const supervisionListData = ref<any[]>([])
const supervisionDetailVisible = ref(false)
const supervisionDetailData = ref<Record<string, any> | null>(null)
const medicationVisible = ref(false)

function openDetail() {
  detailVisible.value = true
}

function viewNotice() {
  noticeVisible.value = true
}

async function viewSupervisionList() {
  try {
    const { data } = await getSupervisionListApi(props.row.id)
    supervisionListData.value = data || []
    supervisionListVisible.value = true
  } catch { /* handled */ }
}

async function viewSupervisionDetail(record: Record<string, any>) {
  try {
    const { data } = await getSupervisionByIdApi(record.id)
    supervisionDetailData.value = data
    supervisionDetailVisible.value = true
  } catch { /* handled */ }
}

async function viewMedication() {
  try {
    const { data } = await getLatentMedicationApi(props.row.id)
    if (!data) {
      ElMessage.info("暂无服药管理记录")
      return
    }
    medicationVisible.value = true
  } catch { /* handled */ }
}
</script>

<template>
  <div class="archived-record-actions">
    <el-button type="primary" link size="small" @click="openDetail">
      基本信息
    </el-button>
    <el-button type="info" link size="small" @click="viewNotice">
      通知单
    </el-button>
    <el-button type="info" link size="small" @click="viewSupervisionList">
      督导表
    </el-button>
    <el-button type="info" link size="small" @click="viewMedication">
      服药管理记录
    </el-button>

    <LatentRecordDetailDialog
      v-model:visible="detailVisible"
      :latent-id="row.id"
    />

    <LatentNoticeDetailDialog
      v-model:visible="noticeVisible"
      :latent-row="row"
      @success="emit('success')"
    />

    <LatentMedicationDialog
      v-model:visible="medicationVisible"
      :latent-row="row"
      read-only
    />

    <el-dialog
      v-model="supervisionListVisible"
      :title="`${row.name} - 督导表记录`"
      width="720px"
      append-to-body
    >
      <el-table :data="supervisionListData" border stripe>
        <el-table-column prop="formSeq" label="次序" />
        <el-table-column prop="createTime" label="填写时间" />
        <el-table-column label="状态">
          <template #default="{ row: record }">
            {{ record.status === 2 ? "已归档" : (record.status === 1 ? "已提交" : "未填写") }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row: record }">
            <el-button type="primary" link size="small" @click="viewSupervisionDetail(record)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!supervisionListData.length" description="暂无督导表记录" />
    </el-dialog>

    <SupervisionFormDetailDialog
      v-model:visible="supervisionDetailVisible"
      :form-data="supervisionDetailData"
      :patient-name="row.name"
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
