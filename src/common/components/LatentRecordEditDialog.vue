<script lang="ts" setup>
import { idCardRule, phoneRule } from "@@/utils/validate"
import { createLatentApi, getLatentDetailApi, updateLatentApi } from "@/pages/latent-management/apis"

const props = defineProps<{
  visible: boolean
  latentId: number | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const isCreate = computed(() => props.latentId == null)

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  populationType: "",
  name: "",
  gender: "",
  age: null as number | null,
  idNumber: "",
  phone: "",
  infectionResult: "",
  diagnosisFirst: "",
  hasChestXray: "",
  chestXrayDate: "",
  chestXrayResult: "",
  trackingRemark: ""
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
    age: null,
    idNumber: "",
    phone: "",
    infectionResult: "",
    diagnosisFirst: "",
    hasChestXray: "",
    chestXrayDate: "",
    chestXrayResult: "",
    trackingRemark: ""
  })
}

async function loadDetail() {
  if (!props.latentId) return
  const { data } = await getLatentDetailApi(props.latentId)
  if (!data) return
  Object.assign(form, {
    populationType: data.populationType || "",
    name: data.name || "",
    gender: data.gender || "",
    age: data.age ?? null,
    idNumber: data.idNumber || "",
    phone: data.phone || "",
    infectionResult: data.infectionResult || "",
    diagnosisFirst: data.diagnosisFirst || "",
    hasChestXray: data.hasChestXray || "",
    chestXrayDate: data.chestXrayDate || "",
    chestXrayResult: data.chestXrayResult || "",
    trackingRemark: data.trackingRemark || ""
  })
}

watch(() => props.visible, async (val) => {
  if (val) {
    resetForm()
    if (props.latentId) {
      await loadDetail()
    }
    nextTick(() => formRef.value?.clearValidate())
  }
})

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
      await createLatentApi({ ...form })
      ElMessage.success("新增成功")
    } else {
      const { populationType, ...payload } = form
      await updateLatentApi(props.latentId!, payload)
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
    :title="isCreate ? '新增潜伏感染者' : '修改潜伏感染者信息'"
    width="660px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-row :gutter="16">
        <el-col v-if="isCreate" :span="12">
          <el-form-item label="数据来源" prop="populationType">
            <el-select v-model="form.populationType" placeholder="请选择" style="width: 100%">
              <el-option label="学生筛查" value="school" />
              <el-option label="重点人群" value="keyPopulation" />
              <el-option label="常规筛查" value="regular" />
              <el-option label="大疫情" value="epidemic" />
              <el-option label="推介" value="referral" />
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
          <el-form-item label="年龄">
            <el-input-number v-model="form.age" :min="0" :max="150" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证件号" prop="idNumber">
            <el-input v-model="form.idNumber" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="感染筛查结果">
            <el-input v-model="form.infectionResult" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否胸片检查">
            <el-select v-model="form.hasChestXray" style="width: 100%">
              <el-option label="是" value="是" />
              <el-option label="否" value="否" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="胸片检查日期">
            <el-date-picker v-model="form.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="胸片检查结果">
            <el-input v-model="form.chestXrayResult" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="首次诊断">
            <el-input v-model="form.diagnosisFirst" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="追踪备注">
            <el-input v-model="form.trackingRemark" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="close">
        取消
      </el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isCreate ? "新增" : "保存" }}
      </el-button>
    </template>
  </el-dialog>
</template>
