<script lang="ts" setup>
import AttachmentPreviewList from "@@/components/AttachmentPreviewList.vue"
import PrintSupervision from "@@/components/PrintSupervision.vue"
import { normalizeLatentTreatmentPlan } from "@@/constants/disease"
import { getSupervisionRecordStatusLabel } from "@@/utils/supervisionForm"

const props = defineProps<{
  visible: boolean
  formData: Record<string, any> | null
  patientName?: string
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const printVisible = ref(false)
const d = computed(() => props.formData)

const supervisionRecords = computed(() => {
  const raw = d.value?.supervisionRecords
  if (!raw) return []
  try {
    const parsed = typeof raw === "string" ? JSON.parse(raw) : raw
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`督导表详情${patientName ? ` — ${patientName}` : ''}${d?.formSeq ? `（第${d.formSeq}次）` : ''}`"
    width="920px"
    top="5vh"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <template v-if="d">
      <el-descriptions :column="3" border size="small" class="mb-4">
        <el-descriptions-item label="状态">
          {{ getSupervisionRecordStatusLabel(d.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ d.createTime || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          {{ d.category || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="姓名">
          {{ d.patientName || patientName || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="人群分类">
          {{ d.category || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">
          {{ d.currentAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">
          {{ d.householdAddress || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="身份证">
          {{ d.idNumber || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ d.gender || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="年龄">
          {{ d.age ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="电话号码">
          {{ d.phone || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="电话备注" :span="2">
          {{ d.phoneRemark || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="出生日期">
          {{ d.birthDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="民族">
          {{ d.ethnicity || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="是否开始预防性治疗">
          {{ d.hasPreventiveTreatment || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗方案">
          {{ normalizeLatentTreatmentPlan(d.treatmentPlan) || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗开始时间">
          {{ d.treatmentStartDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗结束时间">
          {{ d.treatmentEndDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="下次督导时间">
          {{ d.nextSupervisionDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="管理单位">
          {{ d.managingUnit || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="督导医生">
          {{ d.supervisingDoctor || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        督导记录
      </el-divider>
      <el-table :data="supervisionRecords" border stripe size="small" class="mb-4">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="time" label="督导时间" />
        <el-table-column prop="method" label="督导方式" />
        <el-table-column prop="content" label="督导内容" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!supervisionRecords.length" description="暂无督导记录" />

      <el-divider content-position="left">
        全疗程规律治疗评价
      </el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="中断用药">
          {{ d.interruptMedication || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="治疗完成情况">
          {{ d.treatmentCompletionStatus || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="中断次数">
          {{ d.interruptCount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="用药率">
          {{ d.medicationRate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="全程应用药次数">
          {{ d.totalDoses ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="实际用药次数">
          {{ d.actualDoses ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="结束疗程时间">
          {{ d.treatmentEndDate || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        督导管理人员
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="管理人员类型">
          {{ d.managerType || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="管理人员姓名">
          {{ d.managerName || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ d.remark || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">
        附件
      </el-divider>
      <AttachmentPreviewList :urls="d.attachmentUrls" />
    </template>

    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button type="primary" @click="printVisible = true">
        打印
      </el-button>
    </template>

    <PrintSupervision
      v-if="d"
      :visible="printVisible"
      :data="d"
      @update:visible="printVisible = $event"
    />
  </el-dialog>
</template>
