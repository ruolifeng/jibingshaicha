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
  STOP_TREATMENT_YES_NO_OPTIONS,
  YES_NO_OPTIONS
} from "@@/constants/disease"
import { applyFollowUpChemotherapyDefault, canEditFollowUpVisit, FOLLOW_UP_EDIT_DAYS_LEVEL5, shouldArchiveOnStopTreatment, STOP_TREATMENT_REASON_MDR } from "@@/utils/followUpVisit"
import { getFollowUpDraftApi, saveFollowUpApi, saveFollowUpDraftApi } from "@/pages/school/patient/apis"
import { useUserStore } from "@/pinia/stores/user"
import ImageUploader from "./ImageUploader.vue"

interface Props {
  visible: boolean
  patientId: number | null
  patientName?: string
  /** 患者行数据，用于预填化疗方案（病案首次治疗方案） */
  patientRow?: Record<string, any> | null
  populationType: "school" | "keyPopulation" | "regular" | "epidemic" | "referral" | "specialDisease" | "closeContact" | string
  /** 传入已有记录时为修改模式 */
  initialData?: Record<string, any> | null
}

const props = withDefaults(defineProps<Props>(), {
  patientName: "",
  patientRow: null,
  initialData: null
})

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "saved"): void
}>()

const userStore = useUserStore()

const localVisible = computed({
  get: () => props.visible,
  set: v => emit("update:visible", v)
})

const isEditMode = computed(() => !!props.initialData?.id)
const visitCreateTime = ref<string | null>(null)

const formLocked = computed(() =>
  isEditMode.value
  && !canEditFollowUpVisit(userStore.userRole, {
    status: 1,
    createTime: visitCreateTime.value,
    editable: props.initialData?.editable
  })
)

const dialogTitle = computed(() => {
  const suffix = props.patientName ? `——${props.patientName}` : ""
  if (isEditMode.value) {
    return formLocked.value ? `查看后续随访记录${suffix}` : `修改后续随访记录${suffix}`
  }
  return `填写后续随访记录${suffix}`
})

const submitting = ref(false)
const draftSaving = ref(false)
const draftId = ref<number | null>(null)

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
  stopTreatment: string
  stopTreatmentDate: string
  stopTreatmentReason: string
  stopTreatmentReasonOther: string
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
    stopTreatment: "否",
    stopTreatmentDate: "",
    stopTreatmentReason: "",
    stopTreatmentReasonOther: "",
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

function parseDraftData(data: Record<string, any>) {
  const stopTreatment = data.stopTreatment
    || (data.stopTreatmentDate || data.stopTreatmentReason ? "是" : "否")
  Object.assign(form, createEmptyForm(), {
    ...data,
    stopTreatment,
    symptoms: data.symptoms
      ? String(data.symptoms).split(",").map((s: string) => s.trim()).filter(Boolean)
      : []
  })
  draftId.value = data.id ?? null
}

function clearStopTreatmentFields() {
  form.stopTreatmentDate = ""
  form.stopTreatmentReason = ""
  form.stopTreatmentReasonOther = ""
}

watch(
  () => form.stopTreatment,
  (val) => {
    if (val !== "是") {
      clearStopTreatmentFields()
    }
  }
)

watch(
  () => form.stopTreatmentReason,
  (val) => {
    if (val !== "其它") {
      form.stopTreatmentReasonOther = ""
    }
  }
)

async function loadDraft() {
  if (!props.patientId) return
  draftId.value = null
  visitCreateTime.value = null
  Object.assign(form, createEmptyForm())
  try {
    const { data } = await getFollowUpDraftApi(props.patientId)
    if (data) {
      parseDraftData(data)
    }
  } catch { /* 无草稿 */ }
  applyFollowUpChemotherapyDefault(form, props.patientRow)
}

function loadInitialData() {
  if (!props.initialData) return
  parseDraftData(props.initialData)
  visitCreateTime.value = props.initialData.createTime ?? null
}

async function initForm() {
  if (props.initialData) {
    loadInitialData()
  } else {
    await loadDraft()
  }
}

watch(
  () => props.visible,
  (v) => {
    if (v) initForm()
  }
)

function buildPayload() {
  const payload: Record<string, any> = {
    id: draftId.value ?? undefined,
    patientId: props.patientId,
    populationType: props.populationType,
    ...form,
    symptoms: form.symptoms.join(",")
  }
  if (form.stopTreatment !== "是") {
    payload.stopTreatmentDate = null
    payload.stopTreatmentReason = null
    payload.stopTreatmentReasonOther = null
  } else if (form.stopTreatmentReason !== "其它") {
    payload.stopTreatmentReasonOther = null
  }
  return payload
}

function validateStopTreatment(): boolean {
  if (form.stopTreatment !== "是") return true
  if (!form.stopTreatmentDate) {
    ElMessage.warning("请选择停止治疗时间")
    return false
  }
  if (!form.stopTreatmentReason) {
    ElMessage.warning("请选择停止治疗原因")
    return false
  }
  if (form.stopTreatmentReason === "其它" && !form.stopTreatmentReasonOther.trim()) {
    ElMessage.warning("请填写停止治疗原因")
    return false
  }
  return true
}

async function handleSaveDraft() {
  if (!props.patientId || draftSaving.value) return
  draftSaving.value = true
  try {
    await saveFollowUpDraftApi(buildPayload())
    ElMessage.success("后续随访草稿已保存")
    emit("saved")
    localVisible.value = false
  } catch { /* handled by interceptor */ } finally {
    draftSaving.value = false
  }
}

async function handleSave() {
  if (!props.patientId) return
  if (!form.visitDate) {
    ElMessage.warning("请填写随访时间")
    return
  }
  if (!validateStopTreatment()) return
  if (form.stopTreatment === "是" && shouldArchiveOnStopTreatment(form.stopTreatment, form.stopTreatmentReason)) {
    try {
      await ElMessageBox.confirm(
        "选择该停止治疗原因后，患者档案将被锁定归档，不再出现在后续随访列表。是否确认保存？",
        "提示",
        { type: "warning" }
      )
    } catch {
      return
    }
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await saveFollowUpApi(buildPayload())
    const archived = form.stopTreatment === "是"
      && shouldArchiveOnStopTreatment(form.stopTreatment, form.stopTreatmentReason)
    ElMessage.success(archived ? "后续随访保存成功，患者已归档" : "后续随访保存成功")
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
    :title="dialogTitle"
    width="900px"
    top="5vh"
    destroy-on-close
  >
    <el-alert
      v-if="formLocked"
      type="warning"
      :closable="false"
      show-icon
      class="mb-3"
      :title="`后续随访已超过 ${FOLLOW_UP_EDIT_DAYS_LEVEL5} 天修改期限，仅可查看。如需修改请联系上级管理员。`"
    />
    <el-form :model="form" label-width="120px" size="default" :disabled="formLocked">
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
            <el-input v-model="form.chemotherapyPlan" placeholder="来自病案首次治疗方案，可修改" />
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
      <el-alert
        v-if="form.stopTreatment === '是' && shouldArchiveOnStopTreatment(form.stopTreatment, form.stopTreatmentReason)"
        type="warning"
        :closable="false"
        show-icon
        class="mb-3"
        title="选择该原因保存后，患者档案将被锁定归档，不再出现在后续随访列表"
      />
      <el-alert
        v-else-if="form.stopTreatment === '是' && form.stopTreatmentReason === STOP_TREATMENT_REASON_MDR"
        type="info"
        :closable="false"
        show-icon
        class="mb-3"
        title="转入耐多药治疗可继续填写后续随访，不会归档"
      />
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="是否停止治疗" required>
            <el-radio-group v-model="form.stopTreatment">
              <el-radio v-for="o in STOP_TREATMENT_YES_NO_OPTIONS" :key="o.value" :value="o.value">
                {{ o.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <template v-if="form.stopTreatment === '是'">
          <el-col :span="8">
            <el-form-item label="停止治疗时间" required>
              <el-date-picker
                v-model="form.stopTreatmentDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="停止治疗原因" required>
              <el-select v-model="form.stopTreatmentReason" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in STOP_TREATMENT_REASON_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.stopTreatmentReason === '其它'" :span="24">
            <el-form-item label="原因说明" required>
              <el-input v-model="form.stopTreatmentReasonOther" placeholder="请手动填写停止治疗原因" />
            </el-form-item>
          </el-col>
        </template>
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
        {{ formLocked ? "关闭" : "取消" }}
      </el-button>
      <template v-if="!formLocked">
        <el-button
          v-if="!isEditMode"
          type="primary"
          plain
          :loading="draftSaving"
          :disabled="submitting"
          @click="handleSaveDraft"
        >
          保存草稿
        </el-button>
        <el-button type="primary" :loading="submitting" :disabled="draftSaving" @click="handleSave">
          保存
        </el-button>
      </template>
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
