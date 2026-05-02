<script setup lang="ts">
import { CROWD_CATEGORY_OPTIONS, TREATMENT_PLAN_OPTIONS } from "@@/constants/disease"

interface Props {
  visible: boolean
  /** 关联数据（自动填充） */
  prefillData?: {
    patientName?: string
    currentAddress?: string
    householdAddress?: string
    idNumber?: string
    gender?: string
    birthDate?: string
    age?: number | null
    ethnicity?: string
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  "update:visible": [val: boolean]
  "submit": [data: any]
}>()

const formRef = ref()
const form = reactive({
  currentAddress: "",
  householdAddress: "",
  idNumber: "",
  gender: "",
  birthDate: "",
  age: null as number | null,
  ethnicity: "",
  crowdCategory: "",
  treatmentPlan: "",
  customPlanDetail: "",
  receiverOrgId: null as number | null
})

const rules = {
  idNumber: [{ required: true, message: "请输入身份证号", trigger: "blur" }],
  crowdCategory: [{ required: true, message: "请选择人群分类", trigger: "change" }],
  treatmentPlan: [{ required: true, message: "请选择治疗方案", trigger: "change" }]
}

watch(() => props.visible, (val) => {
  if (val && props.prefillData) {
    const d = props.prefillData
    form.currentAddress = d.currentAddress || ""
    form.householdAddress = d.householdAddress || ""
    form.idNumber = d.idNumber || ""
    form.gender = d.gender || ""
    form.birthDate = d.birthDate || ""
    form.age = d.age ?? null
    form.ethnicity = d.ethnicity || ""
  }
})

function handleClose() {
  emit("update:visible", false)
}

async function handleSubmit() {
  await formRef.value?.validate()
  emit("submit", { ...form })
  handleClose()
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="填写通知单"
    width="640px"
    destroy-on-close
    @update:model-value="(val: boolean) => emit('update:visible', val)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="现居住地址">
        <el-input v-model="form.currentAddress" placeholder="请输入现居住地址" />
      </el-form-item>
      <el-form-item label="户籍地址">
        <el-input v-model="form.householdAddress" placeholder="请输入户籍地址" />
      </el-form-item>
      <el-form-item label="身份证" prop="idNumber">
        <el-input v-model="form.idNumber" placeholder="请输入身份证号" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio value="男">
            男
          </el-radio>
          <el-radio value="女">
            女
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="出生日期">
        <el-date-picker v-model="form.birthDate" type="date" placeholder="请选择出生日期" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="年龄">
        <el-input-number v-model="form.age" :min="0" :max="150" />
      </el-form-item>
      <el-form-item label="民族">
        <el-input v-model="form.ethnicity" placeholder="请输入民族" />
      </el-form-item>
      <el-form-item label="人群分类" prop="crowdCategory">
        <el-select v-model="form.crowdCategory" placeholder="请选择">
          <el-option
            v-for="opt in CROWD_CATEGORY_OPTIONS"
            :key="opt"
            :label="opt"
            :value="opt"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="治疗方案" prop="treatmentPlan">
        <el-select v-model="form.treatmentPlan" placeholder="请选择治疗方案">
          <el-option
            v-for="opt in TREATMENT_PLAN_OPTIONS"
            :key="opt"
            :label="opt"
            :value="opt"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.treatmentPlan === '个体化方案'" label="方案详情">
        <el-input v-model="form.customPlanDetail" type="textarea" :rows="3" placeholder="请注明详细的抗结核治疗方案" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">
        取消
      </el-button>
      <el-button type="primary" @click="handleSubmit">
        发送
      </el-button>
    </template>
  </el-dialog>
</template>
