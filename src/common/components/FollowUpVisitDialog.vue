<script lang="ts" setup>
/**
 * 后续随访记录弹窗（V15）
 *
 * 按《后续随访服务记录表（肺结核患者随访服务记录表）》Excel 模板字段重建。
 * 学校 / 重点人群 / 密接 三个患者管理页面共用。
 *
 * 用法：
 *   <FollowUpVisitDialog
 *     v-model:visible="visible"
 *     :patient-id="row.id"
 *     :patient-name="row.name"
 *     population-type="school"
 *     @saved="onSaved"
 *   />
 */
import {
  FOLLOW_UP_DRUG_FORM_OPTIONS,
  FOLLOW_UP_MEDICATION_USAGE_OPTIONS,
  FOLLOW_UP_SUPERVISOR_OPTIONS,
  FOLLOW_UP_SYMPTOM_OPTIONS,
  FOLLOW_UP_VISIT_METHOD_OPTIONS,
  STOP_TREATMENT_REASON_OPTIONS,
  YES_NO_OPTIONS
} from "@@/constants/disease"
import { saveFollowUpApi } from "@/pages/school/patient/apis"
import ImageUploader from "./ImageUploader.vue"

interface Props {
  visible: boolean
  patientId: number | null
  patientName?: string
  populationType: "school" | "keyPopulation" | "regular" | "epidemic" | "referral" | "specialDisease" | "closeContact" | string
}

const props = withDefaults(defineProps<Props>(), {
  patientName: ""
})

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "saved"): void
}>()

const localVisible = computed({
  get: () => props.visible,
  set: v => emit("update:visible", v)
})

const submitting = ref(false)

interface FollowUpForm {
  visitDate: string
  treatmentMonth: number | null
  supervisor: string
  supervisorOther: string
  visitMethod: string
  symptoms: string[]
  symptomsOther: string
  smokingAmount: string
  drinkingAmount: string
  chemotherapyPlan: string
  medicationUsage: string
  drugForm: string
  missedDoses: number | null
  adverseReaction: string
  adverseReactionDetail: string
  complication: string
  complicationDetail: string
  referralDepartment: string
  referralReason: string
  referralTwoWeekResult: string
  handlingOpinion: string
  nextVisitDate: string
  doctorSignature: string
  stopTreatmentDate: string
  stopTreatmentReason: string
  shouldVisitCount: number | null
  actualVisitCount: number | null
  shouldDoseCount: number | null
  actualDoseCount: number | null
  medicationRate: string
  evaluatorSignature: string
  remarks: string
  attachmentUrls: string
}

const form = reactive<FollowUpForm>(createEmptyForm())

function createEmptyForm(): FollowUpForm {
  return {
    visitDate: "",
    treatmentMonth: null,
    supervisor: "",
    supervisorOther: "",
    visitMethod: "",
    symptoms: [],
    symptomsOther: "",
    smokingAmount: "",
    drinkingAmount: "",
    chemotherapyPlan: "",
    medicationUsage: "",
    drugForm: "",
    missedDoses: null,
    adverseReaction: "",
    adverseReactionDetail: "",
    complication: "",
    complicationDetail: "",
    referralDepartment: "",
    referralReason: "",
    referralTwoWeekResult: "",
    handlingOpinion: "",
    nextVisitDate: "",
    doctorSignature: "",
    stopTreatmentDate: "",
    stopTreatmentReason: "",
    shouldVisitCount: null,
    actualVisitCount: null,
    shouldDoseCount: null,
    actualDoseCount: null,
    medicationRate: "",
    evaluatorSignature: "",
    remarks: "",
    attachmentUrls: ""
  }
}

watch(
  () => props.visible,
  (v) => {
    if (v) Object.assign(form, createEmptyForm())
  }
)

async function handleSave() {
  if (!props.patientId) return
  if (!form.visitDate) {
    ElMessage.warning("请填写随访时间")
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await saveFollowUpApi({
      patientId: props.patientId,
      populationType: props.populationType,
      ...form,
      // 多选转 ","
      symptoms: form.symptoms.join(",")
    })
    ElMessage.success("后续随访保存成功")
    emit("saved")
    localVisible.value = false
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="localVisible"
    :title="`填写后续随访记录${patientName ? '——' + patientName : ''}`"
    width="900px"
    top="5vh"
    destroy-on-close
  >
    <el-form :model="form" label-width="120px" size="default">
      <!-- 基本信息 -->
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="随访时间" required>
            <el-date-picker
              v-model="form.visitDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="治疗月序">
            <el-input-number v-model="form.treatmentMonth" :min="1" :max="36" placeholder="第几月" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="随访方式">
            <el-select v-model="form.visitMethod" placeholder="请选择" style="width: 100%">
              <el-option v-for="o in FOLLOW_UP_VISIT_METHOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="督导人员">
            <el-select v-model="form.supervisor" placeholder="请选择" style="width: 100%">
              <el-option v-for="o in FOLLOW_UP_SUPERVISOR_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-if="form.supervisor === '4'" :span="16">
          <el-form-item label="督导人员-其他">
            <el-input v-model="form.supervisorOther" placeholder="请填写" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 症状及体征 -->
      <el-divider content-position="left">
        症状及体征（多选）
      </el-divider>
      <el-form-item label="症状">
        <el-checkbox-group v-model="form.symptoms">
          <el-checkbox v-for="o in FOLLOW_UP_SYMPTOM_OPTIONS" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item v-if="form.symptoms.includes('11')" label="症状-其它">
        <el-input v-model="form.symptomsOther" placeholder="请填写" />
      </el-form-item>

      <!-- 生活方式 -->
      <el-divider content-position="left">
        生活方式指导
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="吸烟（支/天）">
            <el-input v-model="form.smokingAmount" placeholder="如：10" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="饮酒（两/天）">
            <el-input v-model="form.drinkingAmount" placeholder="如：2" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 用药 -->
      <el-divider content-position="left">
        用药
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="化疗方案">
            <el-input v-model="form.chemotherapyPlan" placeholder="请填写" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="用法">
            <el-select v-model="form.medicationUsage" placeholder="请选择" style="width: 100%">
              <el-option v-for="o in FOLLOW_UP_MEDICATION_USAGE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="药品剂型">
            <el-select v-model="form.drugForm" placeholder="请选择" style="width: 100%">
              <el-option v-for="o in FOLLOW_UP_DRUG_FORM_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="漏服药次数">
            <el-input-number v-model="form.missedDoses" :min="0" placeholder="次" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 不良反应 / 并发症 -->
      <el-divider content-position="left">
        不良反应 / 并发症
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="药物不良反应">
            <el-radio-group v-model="form.adverseReaction">
              <el-radio v-for="o in YES_NO_OPTIONS" :key="o.value" :value="o.value">
                {{ o.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col v-if="form.adverseReaction === '2'" :span="16">
          <el-form-item label="不良反应详情">
            <el-input v-model="form.adverseReactionDetail" placeholder="请填写" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="并发症/合并症">
            <el-radio-group v-model="form.complication">
              <el-radio v-for="o in YES_NO_OPTIONS" :key="o.value" :value="o.value">
                {{ o.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col v-if="form.complication === '2'" :span="16">
          <el-form-item label="并发症详情">
            <el-input v-model="form.complicationDetail" placeholder="请填写" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 转诊 -->
      <el-divider content-position="left">
        转诊
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="转诊科别">
            <el-input v-model="form.referralDepartment" placeholder="请填写" />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="转诊原因">
            <el-input v-model="form.referralReason" placeholder="请填写" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="2周内随访结果">
            <el-input v-model="form.referralTwoWeekResult" placeholder="请填写" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 处理意见 & 下次随访 -->
      <el-divider content-position="left">
        处理意见
      </el-divider>
      <el-form-item label="处理意见">
        <el-input v-model="form.handlingOpinion" type="textarea" :rows="2" placeholder="请填写" />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="下次随访时间">
            <el-date-picker
              v-model="form.nextVisitDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="随访医生签名">
            <el-input v-model="form.doctorSignature" placeholder="请填写" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 停止治疗 -->
      <el-divider content-position="left">
        停止治疗
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="停止治疗时间">
            <el-date-picker
              v-model="form.stopTreatmentDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="停止治疗原因">
            <el-select v-model="form.stopTreatmentReason" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="o in STOP_TREATMENT_REASON_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 全程管理 -->
      <el-divider content-position="left">
        全程管理情况
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="应访视次数">
            <el-input-number
              v-model="form.shouldVisitCount"
              :min="0"
              controls-position="right"
              placeholder="次"
              class="follow-up-input-number"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="实际访视次数">
            <el-input-number
              v-model="form.actualVisitCount"
              :min="0"
              controls-position="right"
              placeholder="次"
              class="follow-up-input-number"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="应服药次数">
            <el-input-number
              v-model="form.shouldDoseCount"
              :min="0"
              controls-position="right"
              placeholder="次"
              class="follow-up-input-number"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="实际服药次数">
            <el-input-number
              v-model="form.actualDoseCount"
              :min="0"
              controls-position="right"
              placeholder="次"
              class="follow-up-input-number"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="服药率（%）">
            <el-input v-model="form.medicationRate" placeholder="如：95" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="评估医生签名">
            <el-input v-model="form.evaluatorSignature" placeholder="请填写" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 备注 & 附件 -->
      <el-divider content-position="left">
        备注与附件
      </el-divider>
      <el-form-item label="备注">
        <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="请填写" />
      </el-form-item>
      <el-form-item label="附件（2~6张图片）">
        <ImageUploader v-model="form.attachmentUrls" :min="2" :max="6" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="localVisible = false">
        取消
      </el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.follow-up-input-number {
  width: 100%;

  :deep(.el-input__wrapper) {
    width: 100%;
  }
}
</style>
