<script lang="ts" setup>
import { idCardRule, phoneRule } from "@@/utils/validate"
import { getLatentDetailApi, updateLatentApi } from "@/pages/latent-management/apis"

const props = defineProps<{
  visible: boolean
  latentId: number | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const formRef = ref()
const submitting = ref(false)
const form = reactive({
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

const rules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(true)]
}

async function loadDetail() {
  if (!props.latentId) return
  const { data } = await getLatentDetailApi(props.latentId)
  if (!data) return
  Object.assign(form, {
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
    await loadDetail()
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
    await updateLatentApi(props.latentId!, { ...form })
    ElMessage.success("保存成功")
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
    title="修改潜伏感染者信息"
    width="660px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-row :gutter="16">
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
        保存
      </el-button>
    </template>
  </el-dialog>
</template>
