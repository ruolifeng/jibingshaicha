<script lang="ts" setup>
import {
  LATENT_CLOSE_CONTACT_TYPE_OPTIONS,
  LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS,
  LATENT_MANUAL_POPULATION_TYPE_OPTIONS
} from "@@/constants/disease"
import { CONTACT_TYPE_OPTIONS } from "@@/constants/screening-close-contact"
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
const screeningId = ref<number | null>(null)
const showCrowdCategoryFields = computed(() => isCreate.value || screeningId.value == null)
const showKeyPopulationSubCategories = computed(() =>
  showCrowdCategoryFields.value && form.populationType === "keyPopulation"
)
const showCloseContactType = computed(() =>
  showCrowdCategoryFields.value && form.populationType === "closeContact"
)

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  populationType: "",
  keyPopulationSubCategories: [] as string[],
  closeContactType: "",
  name: "",
  gender: "",
  age: null as number | null,
  idNumber: "",
  phone: "",
  phoneContactRelation: "",
  householdAddress: "",
  currentAddress: "",
  infectionScreenDate: "",
  infectionResult: "",
  diagnosisFirst: "",
  hasChestXray: "",
  chestXrayDate: "",
  chestXrayResult: "",
  trackingRemark: "",
  remark: ""
})

const rules = computed(() => ({
  ...(isCreate.value
    ? { populationType: [{ required: true, message: "请选择数据来源", trigger: "change" }] }
    : {}),
  ...(showKeyPopulationSubCategories.value
    ? {
        keyPopulationSubCategories: [{
          type: "array" as const,
          required: true,
          min: 1,
          message: "请选择重点人群分类",
          trigger: "change"
        }]
      }
    : {}),
  ...(showCloseContactType.value
    ? { closeContactType: [{ required: true, message: "请选择密接类型", trigger: "change" }] }
    : {}),
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(!isCreate.value)]
}))

function resetForm() {
  screeningId.value = null
  Object.assign(form, {
    populationType: "",
    keyPopulationSubCategories: [],
    closeContactType: "",
    name: "",
    gender: "",
    age: null,
    idNumber: "",
    phone: "",
    phoneContactRelation: "",
    householdAddress: "",
    currentAddress: "",
    infectionScreenDate: "",
    infectionResult: "",
    diagnosisFirst: "",
    hasChestXray: "",
    chestXrayDate: "",
    chestXrayResult: "",
    trackingRemark: "",
    remark: ""
  })
}

function parseCrowdCategory(data: { populationType?: string, crowdCategory?: string }) {
  form.keyPopulationSubCategories = []
  form.closeContactType = ""
  if (!data.crowdCategory) return
  if (data.populationType === "keyPopulation") {
    form.keyPopulationSubCategories = data.crowdCategory
      .split(/[、,，/]/)
      .map(item => item.trim())
      .filter(Boolean)
      .filter(item => LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS.includes(item as typeof LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS[number]))
  } else if (data.populationType === "closeContact") {
    const type = data.crowdCategory.trim()
    if (LATENT_CLOSE_CONTACT_TYPE_OPTIONS.includes(type as typeof LATENT_CLOSE_CONTACT_TYPE_OPTIONS[number])) {
      form.closeContactType = type
    }
  }
}

function buildCrowdCategory() {
  if (form.populationType === "keyPopulation") {
    return form.keyPopulationSubCategories.join("、")
  }
  if (form.populationType === "closeContact") {
    return form.closeContactType
  }
  return ""
}

async function loadDetail() {
  if (!props.latentId) return
  const { data } = await getLatentDetailApi(props.latentId)
  if (!data) return
  screeningId.value = data.screeningId ?? null
  Object.assign(form, {
    populationType: data.populationType || "",
    name: data.name || "",
    gender: data.gender || "",
    age: data.age ?? null,
    idNumber: data.idNumber || "",
    phone: data.phone || "",
    phoneContactRelation: data.phoneContactRelation || "",
    householdAddress: data.householdAddress || "",
    currentAddress: data.currentAddress || "",
    infectionScreenDate: data.infectionScreenDate || data.screenDate || "",
    infectionResult: data.infectionResult || "",
    diagnosisFirst: data.diagnosisFirst || "",
    hasChestXray: data.hasChestXray || "",
    chestXrayDate: data.chestXrayDate || "",
    chestXrayResult: data.chestXrayResult || "",
    trackingRemark: data.trackingRemark || "",
    remark: data.remark || ""
  })
  parseCrowdCategory(data)
}

watch(() => form.populationType, (val, oldVal) => {
  if (val === oldVal) return
  form.keyPopulationSubCategories = []
  form.closeContactType = ""
})

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
    const crowdCategory = buildCrowdCategory()
    if (isCreate.value) {
      await createLatentApi({ ...form, crowdCategory })
      ElMessage.success("新增成功")
    } else {
      const { populationType, keyPopulationSubCategories, closeContactType, ...payload } = form
      await updateLatentApi(props.latentId!, {
        ...payload,
        ...(showCrowdCategoryFields.value ? { crowdCategory } : {})
      })
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
    width="720px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
      <el-row :gutter="16">
        <el-col v-if="isCreate" :span="12">
          <el-form-item label="数据来源" prop="populationType">
            <el-select v-model="form.populationType" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="item in LATENT_MANUAL_POPULATION_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-if="showKeyPopulationSubCategories" :span="12">
          <el-form-item label="重点人群分类" prop="keyPopulationSubCategories">
            <el-select
              v-model="form.keyPopulationSubCategories"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择（可多选）"
              style="width: 100%"
            >
              <el-option
                v-for="item in LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-if="showCloseContactType" :span="12">
          <el-form-item label="密接类型" prop="closeContactType">
            <el-select v-model="form.closeContactType" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="item in CONTACT_TYPE_OPTIONS"
                :key="item"
                :label="item"
                :value="item"
              />
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
          <el-form-item label="与联系人关系">
            <el-input v-model="form.phoneContactRelation" placeholder="如：本人、母亲" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="户籍地址">
            <el-input v-model="form.householdAddress" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="居住地址">
            <el-input v-model="form.currentAddress" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="感染筛查时间">
            <el-date-picker
              v-model="form.infectionScreenDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
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
          <el-form-item label="追踪情况">
            <el-input v-model="form.trackingRemark" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
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
