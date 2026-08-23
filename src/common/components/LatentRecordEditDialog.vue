<script lang="ts" setup>
import {
  CHEST_XRAY_RESULT_OPTIONS,
  displayInfectionJudgeResult,
  displayInfectionScreenMethod,
  infectionJudgeSelectOptions,
  KEY_INFECTION_SCREEN_METHOD_OPTIONS,
  LATENT_CLOSE_CONTACT_TYPE_OPTIONS,
  LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS,
  LATENT_MANUAL_POPULATION_TYPE_OPTIONS,
  SCHOOL_SCREEN_METHOD_OPTIONS
} from "@@/constants/disease"
import { formatDateTime } from "@@/utils/datetime"
import { confirmEditChange } from "@@/utils/listToolbar"
import { parseTrackingHistory, TRACK_STATUS_LABEL } from "@@/utils/referralTracking"
import { idCardRule, normalizeIdNumber, phoneRule } from "@@/utils/validate"
import { createLatentApi, getLatentDetailApi, updateLatentApi } from "@/pages/latent-management/apis"

const props = defineProps<{
  visible: boolean
  latentId: string | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const isCreate = computed(() => props.latentId == null)
const screeningId = ref<string | null>(null)
const showCrowdCategoryFields = computed(() => isCreate.value || screeningId.value == null)
const showKeyPopulationSubCategories = computed(() =>
  showCrowdCategoryFields.value && form.populationType === "keyPopulation"
)
const showCloseContactType = computed(() =>
  showCrowdCategoryFields.value && form.populationType === "closeContact"
)
const isSchoolSource = computed(() => form.populationType === "school")
const screenMethodOptions = computed(() =>
  isSchoolSource.value ? SCHOOL_SCREEN_METHOD_OPTIONS : [...KEY_INFECTION_SCREEN_METHOD_OPTIONS]
)
const infectionResultOptions = computed(() => infectionJudgeSelectOptions(form.infectionResult))
const FIXED_DIAGNOSIS_FIRST = "潜伏感染者"

const formRef = ref()
const submitting = ref(false)
/** 详情回填中：跳过 populationType 变更时的清空逻辑 */
const loadingDetail = ref(false)
const editTrackingHistory = ref<{ attempt: number, status: number, trackTime: string, reason: string }[]>([])
const canEditTrackingHistory = computed(() => !isCreate.value && editTrackingHistory.value.length > 0)
const trackStatusOptions = [
  { label: "到位", value: 1 },
  { label: "未到位", value: 2 },
  { label: "其他", value: 3 }
]

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
  screenMethod: "",
  infectionResult: "",
  diagnosisFirst: FIXED_DIAGNOSIS_FIRST,
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
          message: "请选择人群分类",
          trigger: "change"
        }]
      }
    : {}),
  ...(showCloseContactType.value
    ? { closeContactType: [{ required: true, message: "请选择人群分类", trigger: "change" }] }
    : {}),
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(false)],
  phone: [phoneRule(!isCreate.value)]
}))

function resetForm() {
  screeningId.value = null
  editTrackingHistory.value = []
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
    screenMethod: "",
    infectionResult: "",
    diagnosisFirst: FIXED_DIAGNOSIS_FIRST,
    hasChestXray: "",
    chestXrayDate: "",
    chestXrayResult: "",
    trackingRemark: "",
    remark: ""
  })
}

/** 兼容「重点人群-老年人」「密接-家庭内」等展示格式 */
function normalizeCrowdCategoryRaw(raw?: string | null) {
  const text = (raw || "").trim()
  if (!text) return ""
  if (text.startsWith("重点人群-")) return text.slice("重点人群-".length).trim()
  if (text.startsWith("密接-")) return text.slice("密接-".length).trim()
  return text
}

function parseCrowdCategory(data: { populationType?: string, crowdCategory?: string, contactType?: string }) {
  form.keyPopulationSubCategories = []
  form.closeContactType = ""
  const crowdCategory = normalizeCrowdCategoryRaw(data.crowdCategory || data.contactType)
  if (!crowdCategory) return
  if (data.populationType === "keyPopulation") {
    form.keyPopulationSubCategories = crowdCategory
      .split(/[、,，/]/)
      .map(item => item.trim())
      .filter(Boolean)
      .filter(item => LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS.includes(item as typeof LATENT_KEY_POPULATION_SUB_CATEGORY_OPTIONS[number]))
  } else if (data.populationType === "closeContact") {
    const type = crowdCategory.trim()
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
  // 加载期间跳过 populationType watch，避免清空已回填的密接类型/重点人群分类
  loadingDetail.value = true
  try {
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
      screenMethod: normalizeMethodForForm(data.screenMethod, data.infectionResult, data.populationType),
      infectionResult: normalizeResultForForm(data.infectionResult),
      diagnosisFirst: FIXED_DIAGNOSIS_FIRST,
      hasChestXray: data.hasChestXray || "",
      chestXrayDate: data.chestXrayDate || "",
      chestXrayResult: data.chestXrayResult || "",
      trackingRemark: data.trackingRemark || "",
      remark: data.remark || ""
    })
    editTrackingHistory.value = parseTrackingHistory(data.trackingHistoryJson).map(item => ({
      attempt: item.attempt,
      status: item.status,
      trackTime: item.trackTime,
      reason: item.reason ?? ""
    }))
    parseCrowdCategory(data)
    // 等 populationType 的异步 watch 排空后再结束 loading，防止回填被清空
    await nextTick()
  } finally {
    loadingDetail.value = false
  }
}

function normalizeMethodForForm(screenMethod?: string, infectionResult?: string, populationType?: string) {
  const raw = (screenMethod || "").trim()
  if (populationType === "school") {
    if (!raw) return ""
    if (SCHOOL_SCREEN_METHOD_OPTIONS.includes(raw as typeof SCHOOL_SCREEN_METHOD_OPTIONS[number])) return raw
    const upper = raw.toUpperCase()
    if (upper.includes("IGRA") || raw.includes("干扰素")) return "IGRA"
    if (upper.includes("EC") || raw.includes("结核抗原")) return "EC"
    if (upper.includes("PPD") || raw.includes("结核菌素")) return "PPD"
    if (raw === "未做" || raw === "未查") return "未查"
    return raw
  }
  const display = displayInfectionScreenMethod(screenMethod, infectionResult)
  return display === "-" ? "" : display
}

function normalizeResultForForm(infectionResult?: string) {
  const display = displayInfectionJudgeResult(infectionResult)
  return display === "-" ? "" : display
}

watch(() => form.populationType, (val, oldVal) => {
  if (loadingDetail.value || val === oldVal) return
  form.keyPopulationSubCategories = []
  form.closeContactType = ""
  // 切换数据来源时清空口径不同的感染字段，避免提交非法值
  if (oldVal) {
    form.screenMethod = ""
    form.infectionResult = ""
  }
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
  if (canEditTrackingHistory.value) {
    const emptyRemark = editTrackingHistory.value.find(item => !String(item.reason || "").trim())
    if (emptyRemark) {
      ElMessage.warning(`请填写第${emptyRemark.attempt}次追踪备注`)
      return
    }
  }
  if (!isCreate.value) {
    const name = form.name?.trim() || "该潜伏感染者"
    const confirmed = await confirmEditChange(`「${name}」信息`)
    if (!confirmed) return
  }
  submitting.value = true
  try {
    form.diagnosisFirst = FIXED_DIAGNOSIS_FIRST
    const crowdCategory = buildCrowdCategory()
    const idNumber = normalizeIdNumber(form.idNumber)
    if (isCreate.value) {
      await createLatentApi({ ...form, idNumber, crowdCategory, diagnosisFirst: FIXED_DIAGNOSIS_FIRST })
      ElMessage.success("新增成功")
    } else {
      const { populationType, keyPopulationSubCategories, closeContactType, trackingRemark, ...payload } = form
      const updateBody: Record<string, any> = {
        ...payload,
        idNumber,
        diagnosisFirst: FIXED_DIAGNOSIS_FIRST,
        ...(showCrowdCategoryFields.value ? { crowdCategory } : {})
      }
      if (canEditTrackingHistory.value) {
        updateBody.trackingHistory = editTrackingHistory.value.map(item => ({
          attempt: item.attempt,
          status: item.status,
          reason: String(item.reason || "").trim()
        }))
      } else {
        updateBody.trackingRemark = trackingRemark
      }
      await updateLatentApi(props.latentId!, updateBody)
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
    :title="isCreate ? '新增潜伏感染者' : '编辑潜伏感染者信息'"
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
          <el-form-item label="人群分类" prop="keyPopulationSubCategories">
            <el-select
              v-model="form.keyPopulationSubCategories"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择（可多选：老年人/糖尿病/双感）"
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
          <el-form-item label="人群分类" prop="closeContactType">
            <el-select v-model="form.closeContactType" placeholder="请选择（家庭内/家庭外）" style="width: 100%">
              <el-option
                v-for="item in LATENT_CLOSE_CONTACT_TYPE_OPTIONS"
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
            <el-input v-model="form.idNumber" placeholder="可填无" />
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
          <el-form-item label="现住地址">
            <el-input v-model="form.currentAddress" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="感染筛查日期">
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
          <el-form-item label="感染筛查方法">
            <el-select
              v-model="form.screenMethod"
              placeholder="请选择"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option v-for="item in screenMethodOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="感染筛查结果">
            <el-select
              v-model="form.infectionResult"
              placeholder="请选择"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option v-for="item in infectionResultOptions" :key="item" :label="item" :value="item" />
            </el-select>
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
            <el-select v-model="form.chestXrayResult" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="首次诊断">
            <el-input :model-value="FIXED_DIAGNOSIS_FIRST" readonly />
          </el-form-item>
        </el-col>
        <template v-if="canEditTrackingHistory">
          <el-col :span="24">
            <el-divider content-position="left">
              追踪情况
            </el-divider>
          </el-col>
          <el-col
            v-for="item in editTrackingHistory"
            :key="item.attempt"
            :span="24"
          >
            <el-form-item :label="`第${item.attempt}次追踪`" required>
              <div class="edit-tracking-meta">
                <el-select v-model="item.status" style="width: 120px">
                  <el-option
                    v-for="opt in trackStatusOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <span class="edit-tracking-time">{{ formatDateTime(item.trackTime) }}</span>
                <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
                  {{ TRACK_STATUS_LABEL[item.status] || "-" }}
                </el-tag>
              </div>
              <el-input
                v-model="item.reason"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="请填写追踪备注"
              />
            </el-form-item>
          </el-col>
        </template>
        <el-col v-else :span="24">
          <el-form-item label="追踪情况">
            <el-input v-model="form.trackingRemark" type="textarea" :rows="2" placeholder="暂无正式追踪记录时可填写说明" />
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

<style scoped lang="scss">
.edit-tracking-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.edit-tracking-time {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
