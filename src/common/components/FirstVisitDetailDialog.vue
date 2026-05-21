<script lang="ts" setup>
/** 首次入户随访记录 — 查看详情弹窗（学校人群 & 重点人群共用） */
import PrintFirstVisit from "@@/components/PrintFirstVisit.vue"
import AttachmentPreviewList from "@@/components/AttachmentPreviewList.vue"
import { SYMPTOM_OPTIONS } from "@@/constants/disease"

const props = defineProps<{
  visible: boolean
  visitData: Record<string, any> | null
  patientName?: string
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

/** 将存储的症状 value 列表（逗号分隔）转换为可读标签 */
const symptomLabels = computed(() => {
  const raw = props.visitData?.symptoms
  if (!raw) return "-"
  const valueMap = Object.fromEntries(SYMPTOM_OPTIONS.map(o => [o.value, o.label]))
  return raw
    .split(",")
    .map((v: string) => valueMap[v.trim()] ?? v.trim())
    .filter(Boolean)
    .join("、") || "-"
})

/** 解析健康教育项目（存储为 JSON 字符串） */
const educationEntries = computed<[string, string][]>(() => {
  const raw = props.visitData?.educationItems
  if (!raw) return []
  let obj: Record<string, string> = {}
  if (typeof raw === "string") {
    try { obj = JSON.parse(raw) } catch { return [] }
  } else {
    obj = raw as Record<string, string>
  }
  return Object.entries(obj) as [string, string][]
})

const printVisible = ref(false)
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="首次入户随访记录详情"
    width="900px"
    @update:model-value="emit('update:visible', $event)"
  >
    <template v-if="visitData">
      <!-- 基本信息 -->
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="患者姓名">
          {{ patientName || visitData.patientName || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="随访时间">
          {{ visitData.visitDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="随访方式">
          {{ visitData.visitMethod || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="患者类型">
          {{ visitData.patientType || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="痰菌情况">
          {{ visitData.sputumStatus || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="耐药情况">
          {{ visitData.drugResistance || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="症状及体征" :span="2">
          {{ symptomLabels }}
        </el-descriptions-item>
        <el-descriptions-item label="其他症状">
          {{ visitData.otherSymptoms || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 用药情况 -->
      <el-divider content-position="left">
        用药情况
      </el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="化疗方案">
          {{ visitData.chemotherapy || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="用法">
          {{ visitData.medicationUsage || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="督导人员">
          {{ visitData.supervisor || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="药品剂型" :span="3">
          {{ visitData.drugForm || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 居住环境与生活方式 -->
      <el-divider content-position="left">
        居住环境与生活方式
      </el-divider>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="单独居室">
          {{ visitData.separateRoom || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="通风情况">
          {{ visitData.ventilation || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="吸烟(支/天)">
          {{ visitData.smokingAmount ?? "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="饮酒(两/天)">
          {{ visitData.drinkingAmount ?? "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 健康教育及培训 -->
      <el-divider content-position="left">
        健康教育及培训
      </el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="取药地点">
          {{ visitData.medicationLocation || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="取药时间">
          {{ visitData.medicationPickTime || "-" }}
        </el-descriptions-item>
        <template v-if="educationEntries.length > 0">
          <el-descriptions-item
            v-for="[key, val] in educationEntries"
            :key="key"
            :label="key"
          >
            <el-tag :type="val === '掌握' ? 'success' : 'warning'" size="small">
              {{ val }}
            </el-tag>
          </el-descriptions-item>
        </template>
        <el-descriptions-item v-else label="健康教育" :span="2">
          -
        </el-descriptions-item>
      </el-descriptions>

      <!-- 其他 -->
      <el-divider content-position="left">
        其他
      </el-divider>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="下次随访时间">
          {{ visitData.nextVisitDate || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="评估医生签名">
          {{ visitData.doctorSignature || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="填写时间">
          {{ visitData.createTime || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="附件" :span="3">
          <AttachmentPreviewList :urls="visitData.attachmentUrls" />
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button v-if="visitData" type="primary" @click="printVisible = true">
        打印 / 保存PDF
      </el-button>
    </template>
  </el-dialog>

  <PrintFirstVisit
    v-if="visitData"
    :visible="printVisible"
    :visit-data="visitData"
    :patient-name="patientName"
    @update:visible="printVisible = $event"
  />
</template>
