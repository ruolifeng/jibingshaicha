<script lang="ts" setup>
import TrackingHistoryPanel from "@@/components/TrackingHistoryPanel.vue"
import { getPopulationTypeLabel, TRACKING_STATUS_MAP } from "@@/constants/disease"
import { getLatentDetailApi } from "@/pages/latent-management/apis"

const props = defineProps<{
  visible: boolean
  latentId: string | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const loading = ref(false)
const detail = ref<Record<string, any> | null>(null)

async function loadDetail() {
  if (!props.latentId) return
  loading.value = true
  try {
    const { data } = await getLatentDetailApi(props.latentId)
    detail.value = data
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) loadDetail()
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="潜伏感染者详情"
    width="720px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-descriptions v-loading="loading" :column="2" border>
      <template v-if="detail">
        <el-descriptions-item label="姓名">
          {{ detail.name }}
        </el-descriptions-item>
        <el-descriptions-item label="数据来源">
          {{ getPopulationTypeLabel(detail.populationType) }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ detail.gender || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ detail.age ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="出生日期">
          {{ detail.birthDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="民族">
          {{ detail.ethnicity || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="证件号" :span="2">
          {{ detail.idNumber || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          {{ detail.phone || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="与联系人关系">
          {{ detail.phoneContactRelation || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">
          {{ detail.householdAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="现住址" :span="2">
          {{ detail.currentAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="人群分类">
          {{ detail.crowdCategory || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查时间">
          {{ detail.infectionScreenDate || detail.screenDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查结果">
          {{ detail.infectionResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="追踪状态">
          {{ TRACKING_STATUS_MAP[detail.trackingStatus] ?? "待追踪" }}
        </el-descriptions-item>
        <el-descriptions-item label="是否胸片检查">
          {{ detail.hasChestXray || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查日期">
          {{ detail.chestXrayDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查结果">
          {{ detail.chestXrayResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="首次诊断">
          {{ detail.diagnosisFirst || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="追踪情况" :span="2">
          <TrackingHistoryPanel
            v-if="detail.trackingHistoryJson"
            :history-json="detail.trackingHistoryJson"
          />
          <span v-else>{{ detail.trackingRemark || "-" }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ detail.remark || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="通知单">
          {{
            detail.noticeStatus === 0
              ? "草稿"
              : (detail.noticeSent ? "已发送" : "未发送")
          }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.archived === 1" label="归档时间">
          {{ detail.archivedTime || "-" }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.archived === 1" label="治疗阶段">
          {{ detail.treatmentPhase === 2 ? "已结案" : (detail.treatmentPhase === 1 ? "预防治疗中" : "未开始") }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ detail.createTime || "-" }}
        </el-descriptions-item>
      </template>
    </el-descriptions>
  </el-dialog>
</template>
