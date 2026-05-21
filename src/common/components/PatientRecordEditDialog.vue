<script lang="ts" setup>
import { idCardRule, phoneRule } from "@@/utils/validate"
import { createPatientApi, getPatientDetailApi, updatePatientApi } from "@/pages/patient-management/apis"

const props = defineProps<{
  visible: boolean
  patientId: number | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const isCreate = computed(() => props.patientId == null)

const formRef = ref()
const submitting = ref(false)
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
  diagnosisResult: ""
})

const rules = computed(() => ({
  ...(isCreate.value
    ? { populationType: [{ required: true, message: "请选择数据来源", trigger: "change" }] }
    : {}),
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(!isCreate.value)]
}))

function resetForm() {
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
    diagnosisResult: ""
  })
}

async function loadDetail() {
  if (!props.patientId) return
  const { data } = await getPatientDetailApi(props.patientId)
  if (!data) return
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
    diagnosisResult: data.diagnosisResult || ""
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

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (isCreate.value) {
      await createPatientApi({ ...form })
      ElMessage.success("新增成功")
    } else {
      await updatePatientApi(props.patientId!, { ...form })
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
    width="660px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col v-if="isCreate" :span="24">
          <el-form-item label="数据来源" prop="populationType">
            <el-select v-model="form.populationType" placeholder="请选择" style="width: 100%">
              <el-option label="学生筛查" value="school" />
              <el-option label="重点人群" value="keyPopulation" />
              <el-option label="常规筛查" value="regular" />
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
            <el-select v-model="form.gender" style="width: 100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
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
            <el-input v-model="form.idNumber" />
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
        <el-col :span="24">
          <el-form-item label="诊断结果">
            <el-input v-model="form.diagnosisResult" />
          </el-form-item>
        </el-col>
      </el-row>
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
