<script lang="ts" setup>
import type { FormInstance, FormRules } from "element-plus"
import {
  CROWD_CATEGORY_OPTIONS,
  PREVENTIVE_TREATMENT_YES_NO_OPTIONS,
  TREATMENT_PLAN_OPTIONS
} from "@@/constants/disease"
import { getSupervisionDetailApi, saveSupervisionApi } from "@/pages/latent-management/apis"

const props = defineProps<{
  latentRow: any | null
  readonly?: boolean
}>()

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ success: [] }>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const archiving = ref(false)
const formId = ref<number | undefined>(undefined)

const supervisionForm = reactive({
  currentAddress: "",
  householdAddress: "",
  idNumber: "",
  gender: "",
  birthDate: "",
  ethnicity: "",
  category: "",
  hasPreventiveTreatment: "",
  treatmentPlan: "",
  treatmentStartDate: "",
  treatmentEndDate: "",
  managingUnit: "",
  supervisingDoctor: "",
  remark: ""
})

const rules: FormRules = {
  hasPreventiveTreatment: [{ required: true, message: "请选择是否开始预防性治疗", trigger: "change" }],
  treatmentPlan: [{ required: true, message: "请选择治疗方案", trigger: "change" }],
  treatmentStartDate: [{ required: true, message: "请选择治疗开始时间", trigger: "change" }],
  managingUnit: [{ required: true, message: "请输入管理单位", trigger: "blur" }]
}

function formatDateValue(value: unknown): string {
  if (!value) return ""
  const str = String(value)
  return str.length >= 10 ? str.slice(0, 10) : str
}

function resetFormFromRow(row: any) {
  formId.value = undefined
  supervisionForm.currentAddress = row.currentAddress || ""
  supervisionForm.householdAddress = row.householdAddress || ""
  supervisionForm.idNumber = row.idNumber || ""
  supervisionForm.gender = row.gender || ""
  supervisionForm.birthDate = formatDateValue(row.birthDate)
  supervisionForm.ethnicity = row.ethnicity || ""
  supervisionForm.category = row.crowdCategory || row.category || ""
  supervisionForm.hasPreventiveTreatment = ""
  supervisionForm.treatmentPlan = ""
  supervisionForm.treatmentStartDate = ""
  supervisionForm.treatmentEndDate = ""
  supervisionForm.managingUnit = ""
  supervisionForm.supervisingDoctor = ""
  supervisionForm.remark = ""
}

function applyDetailToForm(data: Record<string, any>, row: any) {
  resetFormFromRow(row)
  formId.value = data.id
  supervisionForm.currentAddress = data.currentAddress ?? supervisionForm.currentAddress
  supervisionForm.householdAddress = data.householdAddress ?? supervisionForm.householdAddress
  supervisionForm.idNumber = data.idNumber ?? supervisionForm.idNumber
  supervisionForm.gender = data.gender ?? supervisionForm.gender
  supervisionForm.birthDate = formatDateValue(data.birthDate) || supervisionForm.birthDate
  supervisionForm.ethnicity = data.ethnicity ?? supervisionForm.ethnicity
  supervisionForm.category = data.category ?? supervisionForm.category
  supervisionForm.hasPreventiveTreatment = data.hasPreventiveTreatment ?? ""
  supervisionForm.treatmentPlan = data.treatmentPlan ?? ""
  supervisionForm.treatmentStartDate = formatDateValue(data.treatmentStartDate) || ""
  supervisionForm.treatmentEndDate = formatDateValue(data.treatmentEndDate) || ""
  supervisionForm.managingUnit = data.managingUnit ?? ""
  supervisionForm.supervisingDoctor = data.supervisingDoctor ?? ""
  supervisionForm.remark = data.remark ?? ""
}

async function loadFormData() {
  if (!props.latentRow?.id) return
  try {
    const { data } = await getSupervisionDetailApi(props.latentRow.id)
    if (data) {
      applyDetailToForm(data, props.latentRow)
    } else {
      resetFormFromRow(props.latentRow)
    }
  } catch {
    resetFormFromRow(props.latentRow)
  }
}

watch(
  () => [visible.value, props.latentRow?.id] as const,
  ([open]) => {
    if (open && props.latentRow) {
      loadFormData()
      nextTick(() => formRef.value?.clearValidate())
    }
  }
)

function buildPayload(status: number) {
  return {
    ...(formId.value ? { id: formId.value } : {}),
    latentInfectionId: props.latentRow!.id,
    populationType: props.latentRow!.populationType,
    patientName: props.latentRow!.name,
    currentAddress: supervisionForm.currentAddress || undefined,
    householdAddress: supervisionForm.householdAddress || undefined,
    idNumber: supervisionForm.idNumber || undefined,
    gender: supervisionForm.gender || undefined,
    birthDate: supervisionForm.birthDate || undefined,
    ethnicity: supervisionForm.ethnicity || undefined,
    category: supervisionForm.category || undefined,
    hasPreventiveTreatment: supervisionForm.hasPreventiveTreatment,
    treatmentPlan: supervisionForm.treatmentPlan,
    treatmentStartDate: supervisionForm.treatmentStartDate,
    treatmentEndDate: supervisionForm.treatmentEndDate || undefined,
    managingUnit: supervisionForm.managingUnit,
    supervisingDoctor: supervisionForm.supervisingDoctor || undefined,
    remark: supervisionForm.remark || undefined,
    status
  }
}

async function validateForm() {
  if (!formRef.value) return false
  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

/** 提交督导表（status=1） */
async function handleSubmit() {
  if (!props.latentRow?.id || submitting.value) return
  if (!await validateForm()) return

  submitting.value = true
  try {
    await saveSupervisionApi(buildPayload(1))
    ElMessage.success("督导表提交成功")
    visible.value = false
    emit("success")
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

/** 归档督导表（status=2） */
async function handleArchive() {
  if (!props.latentRow?.id || archiving.value) return
  if (!await validateForm()) return

  try {
    await ElMessageBox.confirm("确认归档该督导表？归档后将同步预防性治疗信息至筛查表。", "归档确认", { type: "warning" })
  } catch {
    return
  }

  archiving.value = true
  try {
    await saveSupervisionApi(buildPayload(2))
    ElMessage.success("督导表已归档")
    visible.value = false
    emit("success")
  } catch { /* handled by interceptor */ } finally {
    archiving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="`预防性治疗督导表${latentRow?.name ? ` - ${latentRow.name}` : ''}`"
    width="720px"
    append-to-body
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="supervisionForm"
      :rules="readonly ? undefined : rules"
      label-width="140px"
      :disabled="readonly"
    >
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="现居住地址">
            <el-input v-model="supervisionForm.currentAddress" placeholder="请输入现居住地址" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="户籍地址">
            <el-input v-model="supervisionForm.householdAddress" placeholder="请输入户籍地址" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="身份证">
            <el-input v-model="supervisionForm.idNumber" placeholder="请输入身份证号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别">
            <el-select v-model="supervisionForm.gender" placeholder="请选择" clearable style="width: 100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="出生日期">
            <el-date-picker
              v-model="supervisionForm.birthDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="民族">
            <el-input v-model="supervisionForm.ethnicity" placeholder="请输入民族" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="人群分类">
            <el-select v-model="supervisionForm.category" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in CROWD_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否开始预防性治疗" prop="hasPreventiveTreatment">
            <el-select v-model="supervisionForm.hasPreventiveTreatment" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in PREVENTIVE_TREATMENT_YES_NO_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="治疗方案" prop="treatmentPlan">
            <el-select v-model="supervisionForm.treatmentPlan" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="治疗开始时间" prop="treatmentStartDate">
            <el-date-picker
              v-model="supervisionForm.treatmentStartDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="治疗结束时间">
            <el-date-picker
              v-model="supervisionForm.treatmentEndDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="管理单位" prop="managingUnit">
            <el-input v-model="supervisionForm.managingUnit" placeholder="请输入管理单位" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="督导医生">
            <el-input v-model="supervisionForm.supervisingDoctor" placeholder="请输入督导医生" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="备注">
        <el-input v-model="supervisionForm.remark" type="textarea" :rows="3" placeholder="请填写备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">
        {{ readonly ? "关闭" : "取消" }}
      </el-button>
      <template v-if="!readonly">
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交
        </el-button>
        <el-button type="success" :loading="archiving" @click="handleArchive">
          归档
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>
