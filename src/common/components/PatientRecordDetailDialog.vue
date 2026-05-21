<script lang="ts" setup>
import { NOTICE_STATUS_MAP, getPopulationTypeLabel } from "@@/constants/disease"
import { getPatientDetailApi } from "@/pages/patient-management/apis"

const props = defineProps<{
  visible: boolean
  patientId: number | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const loading = ref(false)
const detail = ref<Record<string, any> | null>(null)

async function loadDetail() {
  if (!props.patientId) return
  loading.value = true
  try {
    const { data } = await getPatientDetailApi(props.patientId)
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
    title="患者详情"
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
        <el-descriptions-item label="证件类型">
          {{ detail.idType || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="证件号" :span="2">
          {{ detail.idNumber || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="民族">
          {{ detail.ethnicity || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">
          {{ detail.phone || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">
          {{ detail.householdAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="现住址" :span="2">
          {{ detail.currentAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="诊断结果">
          {{ detail.diagnosisResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.populationType === 'specialDisease'" label="人群分类">
          {{ detail.crowdCategory || "-" }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.populationType === 'specialDisease'" label="现管单位">
          {{ detail.currentManagementUnit || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="通知单状态">
          <template v-if="detail.noticeStatus == null">
            未发送
          </template>
          <template v-else>
            {{ detail.noticeStatus === 0 ? "草稿" : (NOTICE_STATUS_MAP[detail.noticeStatus] || "-") }}
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="首次随访">
          {{
            detail.firstVisitStatus === 1
              ? "已完成"
              : (detail.firstVisitStatus === 0 ? "草稿" : "未完成")
          }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查时间">
          {{ detail.chestXrayDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="胸片检查结果">
          {{ detail.chestXrayResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查日期">
          {{ detail.screenDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="感染筛查结果">
          {{ detail.infectionResult || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ detail.createTime || "-" }}
        </el-descriptions-item>
      </template>
    </el-descriptions>
  </el-dialog>
</template>
