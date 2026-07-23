<script lang="ts" setup>
import type { FormInstance, FormRules } from "element-plus"
import ImageUploader from "@@/components/ImageUploader.vue"
import {
  DRUG_FORM_OPTIONS,
  DRUG_RESISTANCE_OPTIONS,
  EDUCATION_ITEMS,
  FIRST_VISIT_SUPERVISOR_OPTIONS,
  MEDICATION_USAGE_OPTIONS,
  SPUTUM_CULTURE_NOT_DONE,
  SPUTUM_CULTURE_OPTIONS,
  SPUTUM_STATUS_OPTIONS,
  SYMPTOM_OPTIONS,
  VENTILATION_OPTIONS,
  VISIT_METHOD_OPTIONS,
  VISIT_METHOD_OTHER
} from "@@/constants/disease"
import { applyFirstVisitChemotherapyDefault, applyFirstVisitSputumStatusDefault, canEditFirstVisit, FIRST_VISIT_EDIT_DAYS_LEVEL5, FIRST_VISIT_FORM_NO_RULES, sanitizeFirstVisitFormNo } from "@@/utils/firstVisit"
import { confirmEditChange } from "@@/utils/listToolbar"
import { getFirstVisitDetailApi, saveFirstVisitApi, saveFirstVisitDraftApi } from "@/pages/patient-management/apis"
import { useUserStore } from "@/pinia/stores/user"

const props = defineProps<{
  visible: boolean
  patientRow: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

function createEmptyForm() {
  const educationItems: Record<string, string> = {}
  EDUCATION_ITEMS.forEach((item) => {
    educationItems[item] = ""
  })
  return {
    id: undefined as number | undefined,
    formNo: "",
    visitDate: "",
    visitMethod: "",
    visitMethodOther: "",
    patientType: "",
    sputumStatus: "",
    sputumCulture: "",
    drugResistance: "",
    symptoms: [] as string[],
    otherSymptoms: "",
    chemotherapy: "",
    medicationUsage: "",
    drugForm: [] as string[],
    supervisor: "",
    separateRoom: "",
    ventilation: "",
    smokingAmount: "",
    drinkingAmount: "",
    medicationLocation: "",
    medicationPickTime: "",
    educationItems,
    nextVisitDate: "",
    doctorSignature: "",
    remarks: "",
    attachmentUrls: ""
  }
}

const firstVisitForm = reactive(createEmptyForm())
const formRef = ref<FormInstance>()
const saving = ref(false)
const draftSaving = ref(false)
const firstVisitCompleted = ref(false)
const visitCreateTime = ref<string | null>(null)
const userStore = useUserStore()

const formLocked = computed(() =>
  firstVisitCompleted.value
  && !canEditFirstVisit(userStore.userRole, { status: 1, createTime: visitCreateTime.value })
)

const rules: FormRules = {
  formNo: FIRST_VISIT_FORM_NO_RULES,
  visitDate: [{ required: true, message: "请选择随访时间", trigger: "change" }],
  visitMethod: [{ required: true, message: "请选择随访方式", trigger: "change" }],
  visitMethodOther: [{
    validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
      if (firstVisitForm.visitMethod === VISIT_METHOD_OTHER && !value?.trim()) {
        callback(new Error("请填写随访方式"))
      } else {
        callback()
      }
    },
    trigger: "blur"
  }],
  patientType: [{ required: true, message: "请选择患者类型", trigger: "change" }],
  sputumStatus: [{ required: true, message: "请选择痰菌情况", trigger: "change" }],
  sputumCulture: [{ required: true, message: "请选择或录入痰培养情况", trigger: "change" }],
  drugResistance: [{ required: true, message: "请选择耐药情况", trigger: "change" }],
  chemotherapy: [{ required: true, whitespace: true, message: "请填写化疗方案", trigger: "blur" }],
  medicationUsage: [{ required: true, message: "请选择用法", trigger: "change" }],
  supervisor: [{ required: true, message: "请选择督导人员", trigger: "change" }],
  drugForm: [{
    type: "array",
    required: true,
    min: 1,
    message: "请至少选择一种药品剂型",
    trigger: "change"
  }],
  medicationLocation: [{ required: true, whitespace: true, message: "请填写取药地点", trigger: "blur" }],
  medicationPickTime: [{ required: true, message: "请选择取药时间", trigger: "change" }],
  nextVisitDate: [{ required: true, message: "请选择下次随访时间", trigger: "change" }],
  doctorSignature: [{ required: true, whitespace: true, message: "请填写评估医生签名", trigger: "blur" }]
}

function educationItemRules(label: string) {
  return [{
    validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
      if (!value || (value !== "掌握" && value !== "未掌握")) {
        callback(new Error(`请选择「${label}」的掌握情况`))
      } else {
        callback()
      }
    },
    trigger: "change"
  }]
}

function parseLoadedData(data: Record<string, any>) {
  const base = createEmptyForm()
  const symptoms = data.symptoms
    ? String(data.symptoms).split(",").map((s: string) => s.trim()).filter(Boolean)
    : []
  const drugForm = data.drugForm
    ? String(data.drugForm).split(",").map((s: string) => s.trim()).filter(Boolean)
    : []
  let educationItems = { ...base.educationItems }
  if (data.educationItems) {
    try {
      const parsed = typeof data.educationItems === "string"
        ? JSON.parse(data.educationItems)
        : data.educationItems
      educationItems = { ...educationItems, ...parsed }
    } catch { /* ignore */ }
  }
  Object.assign(firstVisitForm, {
    ...base,
    ...data,
    symptoms,
    drugForm,
    educationItems,
    attachmentUrls: data.attachmentUrls ?? ""
  })
}

async function loadExisting() {
  if (!props.patientRow) return
  firstVisitCompleted.value = false
  visitCreateTime.value = null
  Object.assign(firstVisitForm, createEmptyForm())
  try {
    const { data } = await getFirstVisitDetailApi(props.patientRow.id)
    if (data) {
      parseLoadedData(data)
      firstVisitCompleted.value = data.status === 1
      visitCreateTime.value = data.createTime ?? null
    }
  } catch { /* 首次填写 */ }
  applyFirstVisitChemotherapyDefault(firstVisitForm, props.patientRow)
  applyFirstVisitSputumStatusDefault(firstVisitForm, props.patientRow)
}

watch(
  () => firstVisitForm.visitMethod,
  (val) => {
    if (val !== VISIT_METHOD_OTHER) {
      firstVisitForm.visitMethodOther = ""
    }
  }
)

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      await loadExisting()
      nextTick(() => formRef.value?.clearValidate())
    }
  }
)

function close() {
  emit("update:visible", false)
}

function buildPayload() {
  return {
    patientId: props.patientRow!.id,
    populationType: props.patientRow!.populationType,
    ...firstVisitForm,
    visitMethodOther: firstVisitForm.visitMethod === VISIT_METHOD_OTHER
      ? firstVisitForm.visitMethodOther.trim()
      : null,
    symptoms: firstVisitForm.symptoms.join(","),
    drugForm: firstVisitForm.drugForm.join(","),
    educationItems: JSON.stringify(firstVisitForm.educationItems)
  }
}

async function handleSaveDraft() {
  if (!props.patientRow || draftSaving.value) return
  draftSaving.value = true
  try {
    await saveFirstVisitDraftApi(buildPayload())
    ElMessage.success("首次随访草稿已保存")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    draftSaving.value = false
  }
}

async function handleSave() {
  if (!props.patientRow || saving.value) return
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning("请完善必填项后再保存")
    return
  }
  if (!firstVisitCompleted.value && firstVisitForm.sputumCulture === SPUTUM_CULTURE_NOT_DONE) {
    try {
      await ElMessageBox.confirm(
        "痰培养选择「未做」将生成系统提醒消息，状态显示为「未补充」，请后续及时补充痰培养结果。",
        "痰培养未做提醒",
        { type: "warning", confirmButtonText: "确认保存", cancelButtonText: "返回修改" }
      )
    } catch {
      return
    }
  }
  if (firstVisitCompleted.value) {
    const name = props.patientRow.name?.trim() || "该患者"
    const confirmed = await confirmEditChange(`「${name}」的首次随访`)
    if (!confirmed) return
  }
  saving.value = true
  try {
    await saveFirstVisitApi(buildPayload())
    ElMessage.success("首次随访保存成功")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="肺结核患者第一次入户随访记录"
    width="920px"
    top="5vh"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-alert
      v-if="formLocked"
      type="warning"
      :closable="false"
      show-icon
      class="mb-3"
      :title="`首次入户随访已超过 ${FIRST_VISIT_EDIT_DAYS_LEVEL5} 天修改期限，仅可查看。如需修改请联系上级管理员。`"
    />
    <el-form ref="formRef" :model="firstVisitForm" :rules="rules" label-width="110px" size="default" :disabled="formLocked">
      <el-row justify="end" class="form-no-row">
        <el-col :span="8">
          <el-form-item label="编号" prop="formNo" label-width="60px">
            <el-input
              v-model="firstVisitForm.formNo"
              maxlength="8"
              placeholder="请输入8位编号"
              @input="firstVisitForm.formNo = sanitizeFirstVisitFormNo(firstVisitForm.formNo)"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="随访时间" prop="visitDate">
            <el-date-picker v-model="firstVisitForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="随访方式" prop="visitMethod">
            <el-radio-group v-model="firstVisitForm.visitMethod">
              <el-radio v-for="item in VISIT_METHOD_OPTIONS" :key="item" :value="item">
                {{ item }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col v-if="firstVisitForm.visitMethod === VISIT_METHOD_OTHER" :span="8">
          <el-form-item label="随访方式-其他" prop="visitMethodOther">
            <el-input v-model="firstVisitForm.visitMethodOther" placeholder="请填写" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="患者类型" prop="patientType">
            <el-radio-group v-model="firstVisitForm.patientType">
              <el-radio value="初治">
                初治
              </el-radio>
              <el-radio value="复治">
                复治
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="痰菌情况" prop="sputumStatus">
            <el-select v-model="firstVisitForm.sputumStatus" style="width: 100%">
              <el-option v-for="item in SPUTUM_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="痰培养" prop="sputumCulture">
            <el-select
              v-model="firstVisitForm.sputumCulture"
              style="width: 100%"
              placeholder="请选择或输入"
              filterable
              allow-create
              default-first-option
            >
              <el-option v-for="item in SPUTUM_CULTURE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="耐药情况" prop="drugResistance">
            <el-select v-model="firstVisitForm.drugResistance" style="width: 100%">
              <el-option v-for="item in DRUG_RESISTANCE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="症状及体征">
        <el-checkbox-group v-model="firstVisitForm.symptoms">
          <el-checkbox v-for="s in SYMPTOM_OPTIONS" :key="s.value" :value="s.value">
            {{ s.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="其他症状">
        <el-input v-model="firstVisitForm.otherSymptoms" placeholder="选填，如有其他症状请填写" />
      </el-form-item>

      <el-divider content-position="left">
        用药情况
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="化疗方案" prop="chemotherapy">
            <el-input v-model="firstVisitForm.chemotherapy" placeholder="来自病案首次治疗方案，可修改" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="用法" prop="medicationUsage">
            <el-radio-group v-model="firstVisitForm.medicationUsage">
              <el-radio v-for="item in MEDICATION_USAGE_OPTIONS" :key="item" :value="item">
                {{ item }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="督导人员" prop="supervisor">
            <el-select v-model="firstVisitForm.supervisor" style="width: 100%">
              <el-option v-for="item in FIRST_VISIT_SUPERVISOR_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="药品剂型" prop="drugForm">
        <el-checkbox-group v-model="firstVisitForm.drugForm">
          <el-checkbox v-for="item in DRUG_FORM_OPTIONS" :key="item" :value="item">
            {{ item }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-divider content-position="left">
        居住环境与生活方式
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-form-item label="单独居室">
            <el-radio-group v-model="firstVisitForm.separateRoom">
              <el-radio value="有">
                有
              </el-radio>
              <el-radio value="无">
                无
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="通风情况">
            <el-select v-model="firstVisitForm.ventilation" style="width: 100%">
              <el-option v-for="item in VENTILATION_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="吸烟(支/天)">
            <el-input v-model="firstVisitForm.smokingAmount" placeholder="0" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="饮酒(两/天)">
            <el-input v-model="firstVisitForm.drinkingAmount" placeholder="0" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        健康教育及培训
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="取药地点" prop="medicationLocation">
            <el-input v-model="firstVisitForm.medicationLocation" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="取药时间" prop="medicationPickTime">
            <el-date-picker
              v-model="firstVisitForm.medicationPickTime"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col v-for="item in EDUCATION_ITEMS" :key="item" :span="12">
          <el-form-item
            :label="item"
            label-width="170px"
            :prop="`educationItems.${item}`"
            :rules="educationItemRules(item)"
          >
            <el-radio-group v-model="firstVisitForm.educationItems[item]">
              <el-radio value="掌握">
                掌握
              </el-radio>
              <el-radio value="未掌握">
                未掌握
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        其他
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="下次随访时间" prop="nextVisitDate">
            <el-date-picker v-model="firstVisitForm.nextVisitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="评估医生签名" prop="doctorSignature">
            <el-input v-model="firstVisitForm.doctorSignature" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        备注与附件
      </el-divider>
      <el-form-item label="备注">
        <el-input v-model="firstVisitForm.remarks" type="textarea" :rows="2" placeholder="请填写" />
      </el-form-item>
      <el-form-item label="上传10张">
        <ImageUploader v-model="firstVisitForm.attachmentUrls" :disabled="formLocked" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">
        关闭
      </el-button>
      <template v-if="!formLocked">
        <el-button v-if="!firstVisitCompleted" type="primary" plain :loading="draftSaving" :disabled="saving" @click="handleSaveDraft">
          保存草稿
        </el-button>
        <el-button type="primary" :loading="saving" :disabled="draftSaving" @click="handleSave">
          保存
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>
