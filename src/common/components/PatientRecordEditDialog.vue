<script lang="ts" setup>
import { CROWD_CATEGORY_OPTIONS, SPUTUM_CULTURE_OPTIONS } from "@@/constants/disease"
import { confirmEditChange } from "@@/utils/listToolbar"
import { resolveManualEpidemicFormFields } from "@@/utils/patient"
import { idCardRule, normalizeIdNumber, phoneRule } from "@@/utils/validate"
import { createPatientApi, getPatientDetailApi, updatePatientApi } from "@/pages/patient-management/apis"

const props = defineProps<{
  visible: boolean
  patientId: string | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const isCreate = computed(() => props.patientId == null)

const formRef = ref()
const submitting = ref(false)
const screeningId = ref<string | null>(null)

const form = reactive({
  populationType: "",
  name: "",
  gender: "",
  birthDate: "",
  age: null as number | null,
  idType: "居民身份证",
  idNumber: "",
  ethnicity: "",
  phone: "",
  householdAddress: "",
  currentAddress: "",
  diagnosisResult: "",
  crowdCategory: "",
  currentManagementUnit: "",
  screenDate: "",
  screenMethod: "",
  infectionResult: "",
  chestXrayDate: "",
  chestXrayResult: "",
  registrationNo: "",
  contactName: "",
  contactRelation: "",
  contactGuardianPhone: "",
  comorbidity: "",
  treatmentClass: "",
  medicationManagementUnit: "",
  patientRemark: "",
  firstTreatmentPlan: "",
  drugSensitivityR: "",
  drugSensitivityH: "",
  cultureResult: ""
})

const showSpecialDiseaseFields = computed(() =>
  form.populationType === "specialDisease" || !!form.crowdCategory || !!form.currentManagementUnit
)

const showScreeningFields = computed(() => screeningId.value != null)

const showInfectionScreeningFields = computed(() =>
  showScreeningFields.value && form.populationType !== "closeContact"
)

const rules = computed(() => ({
  ...(isCreate.value
    ? { populationType: [{ required: true, message: "请选择数据来源", trigger: "change" }] }
    : {}),
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(false)],
  phone: [phoneRule(!isCreate.value)]
}))

function resetForm() {
  screeningId.value = null
  Object.assign(form, {
    populationType: "",
    name: "",
    gender: "",
    birthDate: "",
    age: null,
    idType: "居民身份证",
    idNumber: "",
    ethnicity: "",
    phone: "",
    householdAddress: "",
    currentAddress: "",
    diagnosisResult: "",
    crowdCategory: "",
    currentManagementUnit: "",
    screenDate: "",
    screenMethod: "",
    infectionResult: "",
    chestXrayDate: "",
    chestXrayResult: "",
    registrationNo: "",
    contactName: "",
    contactRelation: "",
    contactGuardianPhone: "",
    comorbidity: "",
    treatmentClass: "",
    medicationManagementUnit: "",
    patientRemark: "",
    firstTreatmentPlan: "",
    drugSensitivityR: "",
    drugSensitivityH: "",
    cultureResult: ""
  })
}

async function loadDetail() {
  if (!props.patientId) return
  const { data } = await getPatientDetailApi(props.patientId)
  if (!data) return
  screeningId.value = data.screeningId ?? null
  Object.assign(form, {
    populationType: data.populationType || "",
    name: data.name || "",
    gender: data.gender || "",
    birthDate: data.birthDate || "",
    age: data.age ?? null,
    idType: data.idType || "居民身份证",
    idNumber: data.idNumber || "",
    ethnicity: data.ethnicity || "",
    phone: data.phone || "",
    householdAddress: data.householdAddress || "",
    currentAddress: data.currentAddress || "",
    diagnosisResult: data.diagnosisResult || "",
    crowdCategory: data.crowdCategory || "",
    currentManagementUnit: data.currentManagementUnit || "",
    screenDate: data.screenDate || "",
    screenMethod: data.screenMethod || "",
    infectionResult: data.infectionResult || "",
    chestXrayDate: data.chestXrayDate || "",
    chestXrayResult: data.chestXrayResult || "",
    ...resolveManualEpidemicFormFields(data)
  })
}

watch(
  () => [props.visible, props.patientId] as const,
  async ([visible]) => {
    if (!visible) return
    if (isCreate.value) {
      resetForm()
    } else {
      await loadDetail()
    }
    nextTick(() => formRef.value?.clearValidate())
  }
)

function close() {
  emit("update:visible", false)
}

function buildPayload() {
  const payload: Record<string, unknown> = { ...form, idNumber: normalizeIdNumber(form.idNumber) }
  if (!showScreeningFields.value) {
    delete payload.screenDate
    delete payload.screenMethod
    delete payload.infectionResult
    delete payload.chestXrayDate
    delete payload.chestXrayResult
  } else if (form.populationType === "closeContact") {
    delete payload.screenDate
    delete payload.screenMethod
    delete payload.infectionResult
  }
  if (!showSpecialDiseaseFields.value) {
    delete payload.crowdCategory
    delete payload.currentManagementUnit
  }
  return payload
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!isCreate.value) {
    const name = form.name?.trim() || "该患者"
    const confirmed = await confirmEditChange(`患者「${name}」信息`)
    if (!confirmed) return
  }
  submitting.value = true
  try {
    if (isCreate.value) {
      await createPatientApi(buildPayload())
      ElMessage.success("新增成功")
    } else {
      await updatePatientApi(props.patientId!, buildPayload())
      ElMessage.success("保存成功")
    }
    close()
    emit("success")
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isCreate ? '新增患者' : '修改患者信息'"
    width="720px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="数据来源" :prop="isCreate ? 'populationType' : undefined">
            <el-select
              v-model="form.populationType"
              placeholder="请选择"
              style="width: 100%"
              :disabled="!isCreate && !!screeningId"
            >
              <el-option label="学生筛查" value="school" />
              <el-option label="重点人群" value="keyPopulation" />
              <el-option label="疫情筛查" value="regular" />
              <el-option label="大疫情" value="epidemic" />
              <el-option label="推介" value="referral" />
              <el-option label="密接" value="closeContact" />
              <el-option label="专病网" value="specialDisease" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别">
            <el-select v-model="form.gender" clearable style="width: 100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
              <el-option label="男性" value="男性" />
              <el-option label="女性" value="女性" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="出生日期">
            <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="年龄">
            <el-input-number v-model="form.age" :min="0" :max="150" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证件类型">
            <el-input v-model="form.idType" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证件号" prop="idNumber">
            <el-input v-model="form.idNumber" placeholder="可填无" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="民族">
            <el-input v-model="form.ethnicity" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        地址信息
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="户籍地址">
            <el-input v-model="form.householdAddress" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="现住址">
            <el-input v-model="form.currentAddress" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        诊断信息
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="病原学结果">
            <el-input v-model="form.diagnosisResult" />
          </el-form-item>
        </el-col>
        <template v-if="showSpecialDiseaseFields">
          <el-col :span="12">
            <el-form-item label="人群分类">
              <el-select v-model="form.crowdCategory" clearable filterable allow-create style="width: 100%">
                <el-option v-for="item in CROWD_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="现管单位">
              <el-input v-model="form.currentManagementUnit" />
            </el-form-item>
          </el-col>
        </template>
      </el-row>

      <el-divider content-position="left">
        病案信息
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="登记号">
            <el-input v-model="form.registrationNo" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人姓名">
            <el-input v-model="form.contactName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="与本人关系">
            <el-input v-model="form.contactRelation" placeholder="联系人监护人与本人关系" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="监护人电话">
            <el-input v-model="form.contactGuardianPhone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="合并症">
            <el-input v-model="form.comorbidity" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="治疗分类">
            <el-input v-model="form.treatmentClass" placeholder="复治患者将在总览标红" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="服药管理单位">
            <el-input v-model="form.medicationManagementUnit" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="首次治疗方案">
            <el-input v-model="form.firstTreatmentPlan" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="利福平（R）">
            <el-input v-model="form.drugSensitivityR" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="异烟肼（H）">
            <el-input v-model="form.drugSensitivityH" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="培养结果">
            <el-select
              v-model="form.cultureResult"
              clearable
              filterable
              allow-create
              default-first-option
              placeholder="请选择或录入"
              style="width: 100%"
            >
              <el-option v-for="item in SPUTUM_CULTURE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.patientRemark" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
      </el-row>

      <template v-if="showScreeningFields">
        <el-divider content-position="left">
          筛查信息
        </el-divider>
        <el-row :gutter="16">
          <template v-if="showInfectionScreeningFields">
            <el-col :span="12">
              <el-form-item label="感染筛查日期">
                <el-date-picker v-model="form.screenDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="感染检查方法">
                <el-input v-model="form.screenMethod" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="感染筛查结果">
                <el-input v-model="form.infectionResult" />
              </el-form-item>
            </el-col>
          </template>
          <el-col :span="12">
            <el-form-item label="胸片检查日期">
              <el-date-picker v-model="form.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="胸片检查结果">
              <el-input v-model="form.chestXrayResult" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>
    </el-form>
    <template #footer>
      <el-button @click="close">
        取消
      </el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>
