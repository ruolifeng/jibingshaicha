<script lang="ts" setup>
import { getPopulationTypeLabel, NOTICE_STATUS_MAP } from "@@/constants/disease"
import {
  buildOrderedImportFields,
  buildPriorityImportFields,
  formatNoticeSentTime,
  isRetreatmentPatient,
  resolveRegistrationNo,
  resolveTreatmentClass
} from "@@/utils/patient"
import { getPatientDetailApi } from "@/pages/patient-management/apis"

const props = defineProps<{
  visible: boolean
  patientId: string | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const loading = ref(false)
const detail = ref<Record<string, any> | null>(null)

const priorityFields = computed(() => buildPriorityImportFields(detail.value))
const allImportFields = computed(() => buildOrderedImportFields(detail.value))
const hasImportFields = computed(() => allImportFields.value.length > 0)
const importSectionTitle = computed(() => {
  if (detail.value?.populationType === "specialDisease") return "专病网导入信息"
  if (detail.value?.populationType === "epidemic") return "大疫情报告卡信息"
  return "导入信息"
})

const visiblePriorityFields = computed(() => {
  if (!detail.value) return []
  return priorityFields.value.filter((item) => {
    if (item.label === "病原学结果" && detail.value?.diagnosisResult) return false
    if (item.label === "登记号" && resolveRegistrationNo(detail.value)) return false
    if (item.label === "治疗分类" && resolveTreatmentClass(detail.value)) return false
    return true
  })
})

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
    width="920px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="section-title">
          基本信息
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">
            <span :class="{ 'text-red-600 font-semibold': isRetreatmentPatient(detail) }">
              {{ detail.name }}
            </span>
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
          <el-descriptions-item label="登记号">
            {{ resolveRegistrationNo(detail) || "-" }}
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
          <el-descriptions-item label="病原学结果">
            {{ detail.diagnosisResult || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="人群分类">
            {{ detail.crowdCategory || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="现管单位">
            {{ detail.currentManagementUnit || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗分类">
            <span :class="{ 'text-red-600 font-semibold': isRetreatmentPatient(detail) }">
              {{ resolveTreatmentClass(detail) || "-" }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">
          管理状态
        </div>
        <el-descriptions :column="2" border>
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
          <el-descriptions-item label="通知单发送时间">
            {{ formatNoticeSentTime(detail.noticeSentTime) || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="服药管理单位">
            {{ detail.noticeMedicationUnit || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="通知单备注" :span="2">
            {{ detail.noticeRemark || "-" }}
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
          <el-descriptions-item label="感染检查方法">
            {{ detail.screenMethod || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查结果">
            {{ detail.infectionResult || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ detail.createTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.archived === 1" label="归档时间">
            {{ detail.archivedTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.archived === 1" label="归档备注">
            {{ detail.archiveRemark || "-" }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">
          重点信息
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item
            v-for="item in visiblePriorityFields"
            :key="item.label"
            :label="item.label"
            :span="item.label === '备注' ? 2 : 1"
          >
            <span
              v-if="item.label === '治疗分类'"
              :class="{ 'text-red-600 font-semibold': isRetreatmentPatient(detail) }"
            >
              {{ item.value || "-" }}
            </span>
            <span v-else>{{ item.value || "-" }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="hasImportFields" class="section-title">
          {{ importSectionTitle }}（全部）
        </div>
        <el-descriptions v-if="hasImportFields" :column="2" border class="import-fields">
          <el-descriptions-item
            v-for="item in allImportFields"
            :key="item.label"
            :label="item.label"
            :span="item.label === '备注' || item.label.length > 16 ? 2 : 1"
          >
            <span
              v-if="item.label === '治疗分类'"
              :class="{ 'text-red-600 font-semibold': isRetreatmentPatient(detail) }"
            >
              {{ item.value }}
            </span>
            <span v-else>{{ item.value }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.section-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);

  &:first-child {
    margin-top: 0;
  }
}

.import-fields {
  :deep(.el-descriptions__label) {
    min-width: 140px;
  }
}
</style>
