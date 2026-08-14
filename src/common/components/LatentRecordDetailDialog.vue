<script lang="ts" setup>
import TrackingHistoryPanel from "@@/components/TrackingHistoryPanel.vue"
import { displayInfectionJudgeResult, displayInfectionScreenMethod, getPopulationTypeLabel, TRACKING_STATUS_MAP } from "@@/constants/disease"
import { parseTrackingHistory } from "@@/utils/referralTracking"
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

const hasCloseContactCase = computed(() => !!detail.value?.closeContactCaseId || !!detail.value?.finalScreeningResult)

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
    width="860px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="section-title">
          基本信息
        </div>
        <el-descriptions :column="2" border>
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
            {{ detail.crowdCategory || detail.contactType || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查时间">
            {{ detail.infectionScreenDate || detail.screenDate || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查方法">
            {{ displayInfectionScreenMethod(detail.screenMethod, detail.infectionResult) }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查结果">
            {{ displayInfectionJudgeResult(detail.infectionResult || detail.screenResult) }}
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
          <el-descriptions-item label="影像检查方法">
            {{ detail.imagingMethod || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="首次诊断">
            {{ detail.diagnosisFirst || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="追踪情况" :span="2">
            <TrackingHistoryPanel
              v-if="parseTrackingHistory(detail.trackingHistoryJson).length"
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
          <el-descriptions-item v-if="detail.archived === 1 || detail.treatmentPhase != null" label="治疗阶段">
            {{ detail.treatmentPhase === 2 ? "已结案" : (detail.treatmentPhase === 1 ? "预防治疗中" : "未开始") }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ detail.createTime || "-" }}
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="hasCloseContactCase">
          <div class="section-title">
            密接个案信息
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="市/州">
              {{ detail.city || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="区/县">
              {{ detail.district || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="登记日期">
              {{ detail.registrationDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="填表日期">
              {{ detail.reportDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="原患者姓名">
              {{ detail.sourcePatientName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="原患者病案号">
              {{ detail.sourcePatientCaseNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="原患者病原学结果">
              {{ detail.sourcePatientBacteriologyResult || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="原患者电话">
              {{ detail.sourcePatientPhone || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="接触类型">
              {{ detail.contactType || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="接触场所">
              {{ detail.contactPlace || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="最终筛查结果">
              {{ detail.finalScreeningResult || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="痰检日期">
              {{ detail.sputumCheckDate || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="痰检方法">
              {{ detail.sputumCheckMethod || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="痰检结果">
              {{ detail.sputumCheckResult || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="有无禁忌症">
              {{ detail.hasContraindication || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="不宜管理预防性治疗原因">
              {{ detail.noTreatmentReason || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="禁忌症/其他原因备注" :span="2">
              {{ detail.contraindicationRemark || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="是否预防性治疗">
              {{ detail.hasPreventiveTreatment || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="预防性治疗方案">
              {{ detail.preventivePlan || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="其他方案备注" :span="2">
              {{ detail.preventivePlanRemark || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="是否完成治疗">
              {{ detail.treatmentCompleted || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="未完成治疗原因">
              {{ detail.incompleteReason || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="6月随访结果">
              {{ detail.followup6Result || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="12月随访结果">
              {{ detail.followup12Result || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="24月随访结果" :span="2">
              {{ detail.followup24Result || "-" }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </template>
    </div>
  </el-dialog>
</template>

<style lang="scss" scoped>
.section-title {
  margin: 4px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);

  &:not(:first-child) {
    margin-top: 18px;
  }
}
</style>
