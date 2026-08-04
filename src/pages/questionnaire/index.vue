<script lang="ts" setup>
import type { FormInstance, FormItemRule, FormRules } from "element-plus"
import type { QuestionnaireConfig, QuestionnaireField, QuestionnaireFieldGroup } from "@/common/constants/questionnaire"
import { isMissingIdNumber, normalizeIdNumber, validateIdCard } from "@@/utils/validate"
import dayjs from "dayjs"
import { QUESTIONNAIRE_CODE } from "@/common/constants/questionnaire"
import { getPublicQuestionnaireConfigApi, submitPublicQuestionnaireApi } from "@/pages/statistics/apis/questionnaire"

defineOptions({ name: "QuestionnaireSchool" })

const route = useRoute()
const questionnaireCode = computed(() => String(route.params.code || QUESTIONNAIRE_CODE))

const loadingStatus = ref(true)
const submitting = ref(false)
const submitted = ref(false)
const config = ref<QuestionnaireConfig | null>(null)

const formRef = ref<FormInstance>()
const form = reactive<Record<string, string | number | undefined>>({})

const rules = computed<FormRules>(() => {
  const result: FormRules = {}
  config.value?.groups.forEach((group) => {
    group.fields.filter(isFieldVisible).forEach((field) => {
      const fieldRules: FormItemRule[] = []
      if (field.required) {
        fieldRules.push({
          required: true,
          message: `请填写${field.label}`,
          trigger: field.type === "select" ? "change" : "blur"
        })
      }
      if (field.key === "idNumber") {
        fieldRules.push({
          validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
            if (isMissingIdNumber(value)) {
              callback()
              return
            }
            if (form.idType === "居民身份证" && !validateIdCard(String(value))) {
              callback(new Error("身份证号格式不正确（需18位）"))
            } else {
              callback()
            }
          },
          trigger: "blur"
        })
      }
      if (field.key === "phone" && field.required) {
        fieldRules.push({
          pattern: /^1[3-9]\d{9}$/,
          message: "请输入正确的手机号码",
          trigger: "blur"
        })
      }
      if (field.key === "age") {
        fieldRules.push({
          validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
            if (value !== undefined && value !== null && (value < 0 || value > 150)) {
              callback(new Error("年龄应在 0~150 之间"))
            } else {
              callback()
            }
          },
          trigger: "blur"
        })
      }
      if (fieldRules.length > 0) {
        result[field.key] = fieldRules
      }
    })
  })
  return result
})

function initForm(groups: QuestionnaireFieldGroup[]) {
  Object.keys(form).forEach(key => delete form[key])
  groups.forEach((group) => {
    group.fields.forEach((field) => {
      if (field.key === "year") {
        form[field.key] = String(new Date().getFullYear())
      } else if (field.key === "idType") {
        form[field.key] = field.options?.[0] || "居民身份证"
      } else {
        form[field.key] = field.type === "number" ? undefined : ""
      }
    })
  })
}

function isFieldVisible(field: QuestionnaireField) {
  if (!field.showWhen) return true
  return form[field.showWhen.field] === field.showWhen.value
}

function visibleFields(group: QuestionnaireFieldGroup) {
  return group.fields.filter(isFieldVisible)
}

function onDateChange(fieldKey: string, val: string) {
  if (fieldKey === "birthDate") {
    onBirthDateChange(val)
  }
}

function onBirthDateChange(val: string) {
  if (!val) return
  const birth = dayjs(val)
  const age = dayjs().diff(birth, "year")
  if ("age" in form) {
    form.age = age >= 0 ? age : undefined
  }
}

async function loadConfig() {
  loadingStatus.value = true
  try {
    const { data } = await getPublicQuestionnaireConfigApi(questionnaireCode.value)
    config.value = data
    initForm(data.groups || [])
  } catch {
    config.value = null
  } finally {
    loadingStatus.value = false
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    ElMessage.warning("请完整填写必填项后再提交")
    return
  }

  submitting.value = true
  try {
    const payload = { ...form, idNumber: normalizeIdNumber(String(form.idNumber ?? "")) }
    await submitPublicQuestionnaireApi(questionnaireCode.value, payload)
    submitted.value = true
    ElMessage.success("问卷提交成功！")
  } catch {
    ElMessage.error("提交失败，请稍后重试")
  } finally {
    submitting.value = false
  }
}

watch(
  () => [form.hasInfectionScreen, form.hasChestXray],
  () => nextTick(() => formRef.value?.clearValidate())
)

function handleReset() {
  if (!config.value) return
  initForm(config.value.groups)
  nextTick(() => formRef.value?.clearValidate())
}

onMounted(loadConfig)
</script>

<template>
  <div class="questionnaire-page">
    <div v-if="loadingStatus" class="status-screen">
      <el-icon class="loading-icon">
        <Loading />
      </el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="!config?.enabled" class="status-screen">
      <el-icon size="64" color="#e6a23c">
        <Warning />
      </el-icon>
      <h2 class="status-title">
        问卷填写已关闭
      </h2>
      <p class="status-desc">
        当前问卷暂未开放填写，请联系相关工作人员了解情况。
      </p>
    </div>

    <div v-else-if="submitted" class="status-screen">
      <el-icon size="64" color="#67c23a">
        <CircleCheck />
      </el-icon>
      <h2 class="status-title">
        提交成功
      </h2>
      <p class="status-desc">
        您的筛查信息已提交，感谢配合！
      </p>
    </div>

    <div v-else-if="config" class="questionnaire-wrap">
      <div class="questionnaire-header">
        <h1 class="title">
          {{ config.title }}
        </h1>
        <p v-if="config.subtitle" class="subtitle">
          {{ config.subtitle }}
        </p>
        <el-alert
          title="标注 * 的为必填项，请确保填写完整后再提交。"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="questionnaire-form">
        <el-card
          v-for="group in config.groups"
          v-show="visibleFields(group).length"
          :key="group.group"
          shadow="never"
          class="form-section"
        >
          <template #header>
            <span class="section-title">{{ group.group }}</span>
          </template>
          <el-row :gutter="16">
            <el-col
              v-for="field in visibleFields(group)"
              :key="field.key"
              :xs="24"
              :sm="field.type === 'textarea' ? 24 : 12"
            >
              <el-form-item :label="`${field.label}${field.required ? ' *' : ''}`" :prop="field.key">
                <el-input
                  v-if="field.type === 'input'"
                  v-model="form[field.key]"
                  :placeholder="`请填写${field.label}`"
                />
                <el-input
                  v-else-if="field.type === 'textarea'"
                  v-model="form[field.key]"
                  type="textarea"
                  :rows="3"
                  :placeholder="`请填写${field.label}`"
                />
                <el-input-number
                  v-else-if="field.type === 'number'"
                  v-model="form[field.key]"
                  :min="0"
                  :max="150"
                  style="width: 100%"
                />
                <el-date-picker
                  v-else-if="field.type === 'date'"
                  v-model="form[field.key]"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :placeholder="`请选择${field.label}`"
                  style="width: 100%"
                  @change="onDateChange(field.key, $event)"
                />
                <el-radio-group
                  v-else-if="field.type === 'select' && field.options && field.options.length <= 3"
                  v-model="form[field.key]"
                >
                  <el-radio v-for="opt in field.options" :key="opt" :value="opt">
                    {{ opt }}
                  </el-radio>
                </el-radio-group>
                <el-select
                  v-else-if="field.type === 'select'"
                  v-model="form[field.key]"
                  :placeholder="`请选择${field.label}`"
                  style="width: 100%"
                  clearable
                >
                  <el-option v-for="opt in field.options" :key="opt" :label="opt" :value="opt" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <div class="submit-area">
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            提交问卷
          </el-button>
          <el-button size="large" @click="handleReset">
            重新填写
          </el-button>
          <p class="privacy-note">
            本问卷数据仅用于结核病防控统计分析，信息将严格保密，不会用于其他任何目的。
          </p>
        </div>
      </el-form>
    </div>

    <div v-else class="status-screen">
      <el-icon size="64" color="#f56c6c">
        <CircleClose />
      </el-icon>
      <h2 class="status-title">
        加载失败
      </h2>
      <p class="status-desc">
        无法获取问卷信息，请稍后重试。
      </p>
      <el-button type="primary" class="mt-3" @click="loadConfig">
        重新加载
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.questionnaire-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}

.status-screen {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.loading-icon {
  font-size: 40px;
  color: #409eff;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.status-title {
  margin-top: 16px;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.status-desc {
  margin-top: 8px;
  color: #909399;
}

.questionnaire-wrap {
  max-width: 860px;
  margin: 0 auto;
}

.questionnaire-header {
  margin-bottom: 16px;

  .title {
    font-size: 22px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 8px;
    text-align: center;
  }

  .subtitle {
    color: #606266;
    text-align: center;
    margin-bottom: 12px;
    line-height: 1.6;
  }
}

.form-section {
  margin-bottom: 16px;
}

.section-title {
  font-weight: bold;
  color: #303133;
}

.submit-area {
  text-align: center;
  padding: 24px 0 40px;
}

.privacy-note {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
}

.mt-3 {
  margin-top: 12px;
}
</style>
