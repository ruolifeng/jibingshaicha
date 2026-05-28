<script lang="ts" setup>
import AttachmentPreviewList from "@@/components/AttachmentPreviewList.vue"
/** 后续随访记录 — 完整详情查看 */
import PrintFollowUp from "@@/components/PrintFollowUp.vue"
import {
  followUpFormatters,
  formatFollowUpSupervisor,
  formatFollowUpSymptoms,
  formatYesNo
} from "@@/utils/followUpVisitFormat"

const props = defineProps<{
  visible: boolean
  visitData: Record<string, any> | null
  patientName?: string
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const printVisible = ref(false)

const d = computed(() => props.visitData)
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`后续随访记录详情${patientName ? ` — ${patientName}` : ''}${d?.visitSeq ? `（第${d.visitSeq}次）` : ''}`"
    width="920px"
    top="5vh"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <template v-if="d">
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="随访时间">
          {{ d.visitDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗月序">
          {{ d.treatmentMonth ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="随访方式">
          {{ followUpFormatters.visitMethod(d.visitMethod, d.visitMethodOther) }}
        </el-descriptions-item>
        <el-descriptions-item label="督导人员" :span="3">
          {{ formatFollowUpSupervisor(d.supervisor, d.supervisorOther) }}
        </el-descriptions-item>
        <el-descriptions-item label="症状及体征" :span="2">
          {{ formatFollowUpSymptoms(d.symptoms) }}
        </el-descriptions-item>
        <el-descriptions-item label="症状-其它">
          {{ d.symptomsOther || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        生活方式指导
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="吸烟(支/天)">
          {{ d.smokingAmount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="饮酒(两/天)">
          {{ d.drinkingAmount ?? "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        用药
      </el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="化疗方案" :span="3">
          {{ d.chemotherapyPlan || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="用法">
          {{ followUpFormatters.medicationUsage(d.medicationUsage) }}
        </el-descriptions-item>
        <el-descriptions-item label="药品剂型">
          {{ followUpFormatters.drugForm(d.drugForm) }}
        </el-descriptions-item>
        <el-descriptions-item label="漏服药次数">
          {{ d.missedDoses ?? "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        不良反应 / 并发症
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="药物不良反应">
          {{ formatYesNo(d.adverseReaction) }}
        </el-descriptions-item>
        <el-descriptions-item label="不良反应详情">
          {{ d.adverseReactionDetail || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="并发症/合并症">
          {{ formatYesNo(d.complication) }}
        </el-descriptions-item>
        <el-descriptions-item label="并发症详情">
          {{ d.complicationDetail || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        转诊
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="转诊科别">
          {{ d.referralDepartment || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="转诊原因">
          {{ d.referralReason || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="2周内随访结果" :span="2">
          {{ d.referralTwoWeekResult || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        处理意见
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="处理意见" :span="2">
          {{ d.handlingOpinion || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="下次随访时间">
          {{ d.nextVisitDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="随访医生签名">
          {{ d.doctorSignature || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        停止治疗
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="是否停止治疗">
          {{ d.stopTreatment || (d.stopTreatmentDate || d.stopTreatmentReason ? "是" : "否") }}
        </el-descriptions-item>
        <template v-if="(d.stopTreatment || (d.stopTreatmentDate || d.stopTreatmentReason ? '是' : '否')) === '是'">
          <el-descriptions-item label="停止治疗时间">
            {{ d.stopTreatmentDate || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="停止治疗原因" :span="2">
            {{ followUpFormatters.stopTreatmentReason(d.stopTreatmentReason, d.stopTreatmentReasonOther) }}
          </el-descriptions-item>
        </template>
      </el-descriptions>

      <el-divider content-position="left">
        全程管理情况
      </el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="应访视次数">
          {{ d.shouldVisitCount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="实际访视次数">
          {{ d.actualVisitCount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="应服药次数">
          {{ d.shouldDoseCount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="实际服药次数">
          {{ d.actualDoseCount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="服药率(%)">
          {{ d.medicationRate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="评估医生签名">
          {{ d.evaluatorSignature || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        其他
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="备注" :span="2">
          {{ d.remarks || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">
          <AttachmentPreviewList :urls="d.attachmentUrls" />
        </el-descriptions-item>
        <el-descriptions-item label="填写时间">
          {{ d.createTime || "-" }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button v-if="d" type="primary" @click="printVisible = true">
        打印 / 保存PDF
      </el-button>
    </template>
  </el-dialog>

  <PrintFollowUp
    v-if="d"
    :visible="printVisible"
    :visit-data="d"
    :patient-name="patientName"
    @update:visible="printVisible = $event"
  />
</template>
