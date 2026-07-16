<script lang="ts" setup>
/** 肺结核患者随访服务记录表 — 打印 / 保存 PDF */
import PrintAttachmentImages from "@@/components/PrintAttachmentImages.vue"
import {
  followUpFormatters,
  formatFollowUpSupervisor,
  formatFollowUpSymptoms,
  formatYesNo
} from "@@/utils/followUpVisitFormat"
import { printElement } from "@@/utils/print"

const props = defineProps<{
  visible: boolean
  visitData: Record<string, any> | null
  patientName?: string
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const display = computed(() => {
  const d = props.visitData
  if (!d) return null
  return {
    visitSeq: d.visitSeq ?? "-",
    visitDate: d.visitDate ?? "-",
    treatmentMonth: d.treatmentMonth ?? "-",
    visitMethod: followUpFormatters.visitMethod(d.visitMethod, d.visitMethodOther),
    supervisor: formatFollowUpSupervisor(d.supervisor, d.supervisorOther),
    symptoms: formatFollowUpSymptoms(d.symptoms),
    symptomsOther: d.symptomsOther || "-",
    smokingAmount: d.smokingAmount ?? "-",
    drinkingAmount: d.drinkingAmount ?? "-",
    chemotherapyPlan: d.chemotherapyPlan || "-",
    medicationUsage: followUpFormatters.medicationUsage(d.medicationUsage),
    drugForm: followUpFormatters.drugForm(d.drugForm),
    missedDoses: d.missedDoses ?? "-",
    adverseReaction: formatYesNo(d.adverseReaction),
    adverseReactionDetail: d.adverseReactionDetail || "-",
    complication: formatYesNo(d.complication),
    complicationDetail: d.complicationDetail || "-",
    referralDepartment: d.referralDepartment || "-",
    referralReason: d.referralReason || "-",
    referralTwoWeekResult: d.referralTwoWeekResult || "-",
    handlingOpinion: d.handlingOpinion || "-",
    nextVisitDate: d.nextVisitDate || "-",
    doctorSignature: d.doctorSignature || "-",
    stopTreatmentDate: d.stopTreatmentDate || "-",
    stopTreatment: d.stopTreatment || (d.stopTreatmentDate || d.stopTreatmentReason ? "是" : "否"),
    stopTreatmentReason: followUpFormatters.stopTreatmentReason(d.stopTreatmentReason, d.stopTreatmentReasonOther),
    shouldVisitCount: d.shouldVisitCount ?? "-",
    actualVisitCount: d.actualVisitCount ?? "-",
    shouldDoseCount: d.shouldDoseCount ?? "-",
    actualDoseCount: d.actualDoseCount ?? "-",
    medicationRate: d.medicationRate || "-",
    evaluatorSignature: d.evaluatorSignature || "-",
    remarks: d.remarks || "-"
  }
})

function handlePrint() {
  printElement("print-followup-content", "肺结核患者随访服务记录表")
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="预览 — 肺结核患者随访服务记录表"
    width="900px"
    top="5vh"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-if="display" id="print-followup-content" class="print-area">
      <h2 class="print-title">
        肺结核患者随访服务记录表
      </h2>
      <p v-if="patientName" class="print-subtitle">
        患者姓名：{{ patientName }} · 第 {{ display.visitSeq }} 次随访
      </p>
      <table class="visit-table" border="1" cellspacing="0" cellpadding="0">
        <tbody>
          <tr class="section-header">
            <td colspan="6">
              基本信息
            </td>
          </tr>
          <tr>
            <th>随访时间</th>
            <td>{{ display.visitDate }}</td>
            <th>治疗月序</th>
            <td>{{ display.treatmentMonth }}</td>
            <th>随访方式</th>
            <td>{{ display.visitMethod }}</td>
          </tr>
          <tr>
            <th>督导人员</th>
            <td colspan="5">
              {{ display.supervisor }}
            </td>
          </tr>
          <tr>
            <th>症状及体征</th>
            <td colspan="3">
              {{ display.symptoms }}
            </td>
            <th>症状-其它</th>
            <td>{{ display.symptomsOther }}</td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              生活方式指导
            </td>
          </tr>
          <tr>
            <th>吸烟(支/天)</th>
            <td>{{ display.smokingAmount }}</td>
            <th>饮酒(两/天)</th>
            <td colspan="3">
              {{ display.drinkingAmount }}
            </td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              用药
            </td>
          </tr>
          <tr>
            <th>化疗方案</th>
            <td colspan="5">
              {{ display.chemotherapyPlan }}
            </td>
          </tr>
          <tr>
            <th>用法</th>
            <td>{{ display.medicationUsage }}</td>
            <th>药品剂型</th>
            <td>{{ display.drugForm }}</td>
            <th>漏服药次数</th>
            <td>{{ display.missedDoses }}</td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              不良反应 / 并发症
            </td>
          </tr>
          <tr>
            <th>药物不良反应</th>
            <td>{{ display.adverseReaction }}</td>
            <th>不良反应详情</th>
            <td colspan="3">
              {{ display.adverseReactionDetail }}
            </td>
          </tr>
          <tr>
            <th>并发症/合并症</th>
            <td>{{ display.complication }}</td>
            <th>并发症详情</th>
            <td colspan="3">
              {{ display.complicationDetail }}
            </td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              转诊
            </td>
          </tr>
          <tr>
            <th>转诊科别</th>
            <td>{{ display.referralDepartment }}</td>
            <th>转诊原因</th>
            <td colspan="3">
              {{ display.referralReason }}
            </td>
          </tr>
          <tr>
            <th>2周内随访结果</th>
            <td colspan="5">
              {{ display.referralTwoWeekResult }}
            </td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              处理意见
            </td>
          </tr>
          <tr>
            <th>处理意见</th>
            <td colspan="5">
              {{ display.handlingOpinion }}
            </td>
          </tr>
          <tr>
            <th>下次随访时间</th>
            <td>{{ display.nextVisitDate }}</td>
            <th>随访医生签名</th>
            <td colspan="3">
              {{ display.doctorSignature }}
            </td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              停止治疗
            </td>
          </tr>
          <tr>
            <th>是否停止治疗</th>
            <td :colspan="display.stopTreatment === '是' ? 1 : 5">
              {{ display.stopTreatment }}
            </td>
            <template v-if="display.stopTreatment === '是'">
              <th>停止治疗时间</th>
              <td colspan="3">
                {{ display.stopTreatmentDate }}
              </td>
            </template>
          </tr>
          <tr v-if="display.stopTreatment === '是'">
            <th>停止治疗原因</th>
            <td colspan="5">
              {{ display.stopTreatmentReason }}
            </td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              全程管理情况
            </td>
          </tr>
          <tr>
            <th>应访视次数</th>
            <td>{{ display.shouldVisitCount }}</td>
            <th>实际访视次数</th>
            <td>{{ display.actualVisitCount }}</td>
            <th>应服药次数</th>
            <td>{{ display.shouldDoseCount }}</td>
          </tr>
          <tr>
            <th>实际服药次数</th>
            <td>{{ display.actualDoseCount }}</td>
            <th>服药率(%)</th>
            <td>{{ display.medicationRate }}</td>
            <th>评估医生签名</th>
            <td>{{ display.evaluatorSignature }}</td>
          </tr>
          <tr class="section-header">
            <td colspan="6">
              备注
            </td>
          </tr>
          <tr>
            <th>备注</th>
            <td colspan="5">
              {{ display.remarks }}
            </td>
          </tr>
        </tbody>
      </table>
      <PrintAttachmentImages :urls="visitData?.attachmentUrls" title="附件照片" />
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button type="primary" @click="handlePrint">
        打印 / 保存PDF
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.print-area {
  padding: 8px;
}

.print-title {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
}

.print-subtitle {
  text-align: center;
  font-size: 14px;
  margin-bottom: 12px;
  color: #606266;
}

.visit-table {
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    border: 1px solid #ccc;
    padding: 7px 10px;
    font-size: 13px;
    vertical-align: middle;
  }

  th {
    background: #f5f7fa;
    white-space: nowrap;
    font-weight: 600;
    width: 110px;
  }

  .section-header td {
    background: #e8f0fe;
    font-weight: bold;
    font-size: 13px;
    padding: 5px 10px;
    color: #1a3a6b;
  }
}
</style>
