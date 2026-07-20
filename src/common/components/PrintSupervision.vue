<script lang="ts" setup>
import PrintAttachmentImages from "@@/components/PrintAttachmentImages.vue"
import { normalizeLatentTreatmentPlan } from "@@/constants/disease"
import { formatNoticeSentTime } from "@@/utils/patient"
import { printElement } from "@@/utils/print"

/** 结核病潜伏感染者预防性治疗督导表打印/PDF 预览组件 */
const props = defineProps<{
  visible: boolean
  data: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const supervisionRecords = computed(() => {
  const raw = props.data?.supervisionRecords
  if (!raw) return []
  try {
    const parsed = typeof raw === "string" ? JSON.parse(raw) : raw
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

function handlePrint() {
  printElement("print-supervision-content", "结核病潜伏感染者预防性治疗督导表")
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="预览 — 结核病潜伏感染者预防性治疗督导表"
    width="860px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div id="print-supervision-content" class="print-area">
      <h2 class="print-title">
        结核病潜伏感染者预防性治疗督导表
      </h2>

      <table class="sup-table" border="1" cellspacing="0" cellpadding="0">
        <tbody>
          <tr class="section-header">
            <td colspan="4">
              基本信息
            </td>
          </tr>
          <tr>
            <th>姓名</th>
            <td>{{ data?.patientName || "-" }}</td>
            <th>人群分类</th>
            <td>{{ data?.category || "-" }}</td>
          </tr>
          <tr>
            <th>现居住地址</th>
            <td>{{ data?.currentAddress || "-" }}</td>
            <th>户籍地址</th>
            <td>{{ data?.householdAddress || "-" }}</td>
          </tr>
          <tr>
            <th>身份证</th>
            <td>{{ data?.idNumber || "-" }}</td>
            <th>性别</th>
            <td>{{ data?.gender || "-" }}</td>
          </tr>
          <tr>
            <th>年龄</th>
            <td>{{ data?.age ?? "-" }}</td>
            <th>电话号码</th>
            <td>{{ data?.phone || "-" }}</td>
          </tr>
          <tr>
            <th>电话备注</th>
            <td colspan="3">
              {{ data?.phoneRemark || "-" }}
            </td>
          </tr>
          <tr>
            <th>出生日期</th>
            <td>{{ data?.birthDate || "-" }}</td>
            <th>民族</th>
            <td>{{ data?.ethnicity || "-" }}</td>
          </tr>
          <tr>
            <th>是否开始预防性治疗</th>
            <td>{{ data?.hasPreventiveTreatment || "-" }}</td>
            <th>治疗方案</th>
            <td>{{ normalizeLatentTreatmentPlan(data?.treatmentPlan) || "-" }}</td>
          </tr>
          <tr>
            <th>治疗开始时间</th>
            <td>{{ data?.treatmentStartDate || "-" }}</td>
            <th>治疗结束时间</th>
            <td>{{ data?.treatmentEndDate || "-" }}</td>
          </tr>
          <tr>
            <th>管理单位</th>
            <td>{{ data?.managingUnit || "-" }}</td>
            <th>督导医生</th>
            <td>{{ data?.supervisingDoctor || "-" }}</td>
          </tr>

          <tr class="section-header">
            <td colspan="4">
              督导记录
            </td>
          </tr>
          <template v-if="supervisionRecords.length">
            <tr v-for="(record, index) in supervisionRecords" :key="index">
              <th>第{{ index + 1 }}次</th>
              <td colspan="3">
                督导时间：{{ record.time || "-" }}；
                督导方式：{{ record.method || "-" }}；
                督导内容：{{ record.content || "-" }}；
                备注：{{ record.remark || "-" }}
              </td>
            </tr>
          </template>
          <tr v-else>
            <td colspan="4" class="empty-cell">
              —
            </td>
          </tr>

          <tr class="section-header">
            <td colspan="4">
              全疗程规律治疗评价
            </td>
          </tr>
          <tr>
            <th>中断用药</th>
            <td>{{ data?.interruptMedication || "-" }}</td>
            <th>治疗完成情况</th>
            <td>{{ data?.treatmentCompletionStatus || "-" }}</td>
          </tr>
          <tr>
            <th>中断次数</th>
            <td>{{ data?.interruptCount ?? "-" }}</td>
            <th>全程应用药次数</th>
            <td>{{ data?.totalDoses ?? "-" }}</td>
          </tr>
          <tr>
            <th>实际用药次数</th>
            <td>{{ data?.actualDoses ?? "-" }}</td>
            <th>用药率(%)</th>
            <td>{{ data?.medicationRate || "-" }}</td>
          </tr>
          <tr>
            <th>结束疗程时间</th>
            <td>{{ data?.treatmentEndDate || "-" }}</td>
            <th>管理人员类型</th>
            <td>{{ data?.managerType || "-" }}</td>
          </tr>
          <tr>
            <th>管理人员姓名</th>
            <td colspan="3">
              {{ data?.managerName || "-" }}
            </td>
          </tr>

          <tr class="section-header">
            <td colspan="4">
              其他
            </td>
          </tr>
          <tr>
            <th>备注</th>
            <td colspan="3">
              {{ data?.remark || "-" }}
            </td>
          </tr>
          <tr v-if="data?.createTime">
            <th>填写时间</th>
            <td colspan="3">
              {{ formatNoticeSentTime(data.createTime) }}
            </td>
          </tr>
        </tbody>
      </table>

      <PrintAttachmentImages :urls="data?.attachmentUrls" title="附件照片" />
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

<style lang="scss">
@import "@@/assets/styles/print-forms.css";
</style>
