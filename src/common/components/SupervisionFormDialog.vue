<script lang="ts" setup>
import type { FormInstance, FormRules } from "element-plus"
import { getDepartmentListApi } from "@@/apis/department"
import ImageUploader from "@@/components/ImageUploader.vue"
import {
  formatLatentSupervisionTreatmentPlan,
  INTERRUPT_MEDICATION_OPTIONS,
  isLatentIndividualPlan,
  LATENT_TREATMENT_PLAN_OPTIONS,
  parseLatentSupervisionTreatmentPlan,
  PREVENTIVE_TREATMENT_YES_NO_OPTIONS,
  SUPERVISION_CATEGORY_OPTIONS,
  SUPERVISION_MANAGER_TYPE_OPTIONS,
  SUPERVISION_METHOD_OPTIONS,
  TREATMENT_COMPLETION_STATUS_OPTIONS
} from "@@/constants/disease"
import { parseAttachmentUrls } from "@@/utils/attachment"
import { canEditSupervisionForm, mergeSupervisionProfileFields } from "@@/utils/supervisionForm"
import {
  getLatentDetailApi,
  getSupervisionDraftApi,
  saveSupervisionApi,
  saveSupervisionDraftApi
} from "@/pages/latent-management/apis"
import { useUserStore } from "@/pinia/stores/user"

interface SupervisionRecord {
  time: string
  content: string
  method: string
  remark: string
}

const props = defineProps<{
  latentRow: any | null
  /** 传入已有记录时为修改模式 */
  initialData?: Record<string, any> | null
  readonly?: boolean
}>()

const emit = defineEmits<{ success: [] }>()
const visible = defineModel<boolean>({ default: false })
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const draftSaving = ref(false)
const archiving = ref(false)
const draftId = ref<string | undefined>(undefined)
const recordCreateTime = ref<string | null>(null)
/** 系统部门/机构名称（管理单位下拉） */
const orgNameOptions = ref<string[]>([])
const orgOptionsLoading = ref(false)

const isEditMode = computed(() => !!props.initialData?.id)
const formLocked = computed(() =>
  isEditMode.value
  && !canEditSupervisionForm(userStore.userRole, {
    status: props.initialData?.status,
    createTime: recordCreateTime.value,
    editable: props.initialData?.editable
  })
)

const dialogTitle = computed(() => {
  const suffix = props.latentRow?.name ? ` — ${props.latentRow.name}` : ""
  if (props.readonly || formLocked.value) return `查看督导表${suffix}`
  if (isEditMode.value) return `修改督导表${suffix}`
  return `填写督导表${suffix}`
})

const supervisionForm = reactive({
  category: "",
  gender: "",
  age: null as number | null,
  phone: "",
  phoneRemark: "",
  currentAddress: "",
  householdAddress: "",
  idNumber: "",
  birthDate: "",
  ethnicity: "",
  hasPreventiveTreatment: "",
  treatmentStartDate: "",
  treatmentEndDate: "",
  nextSupervisionDate: "",
  treatmentPlan: "",
  customPlanDetail: "",
  managingUnit: "",
  supervisingDoctor: "",
  supervisionRecords: [] as SupervisionRecord[],
  treatmentCompletionStatus: "",
  interruptMedication: "",
  interruptCount: null as number | null,
  totalDoses: null as number | null,
  actualDoses: null as number | null,
  medicationRate: "",
  managerType: "",
  managerName: "",
  remark: "",
  attachmentUrls: ""
})

const rules: FormRules = {
  treatmentStartDate: [{
    validator: (_rule, value, callback) => {
      if (supervisionForm.treatmentPlan !== "不服药" && !value) {
        callback(new Error("请选择开始治疗时间"))
        return
      }
      callback()
    },
    trigger: "change"
  }],
  treatmentPlan: [{ required: true, message: "请选择治疗方案", trigger: "change" }]
}

const formDisabled = computed(() => props.readonly || formLocked.value)
const isTreatmentStartDateRequired = computed(() => supervisionForm.treatmentPlan !== "不服药")

function formatDateValue(value: unknown): string {
  if (!value) return ""
  const str = String(value)
  return str.length >= 10 ? str.slice(0, 10) : str
}

function parseAttachmentUrlsField(urls?: string) {
  const parsed = parseAttachmentUrls(urls)
  supervisionForm.attachmentUrls = parsed.length ? JSON.stringify(parsed) : ""
}

function createEmptyRecord(): SupervisionRecord {
  return { time: "", content: "", method: "", remark: "" }
}

function parseTreatmentPlan(plan?: string) {
  const parsed = parseLatentSupervisionTreatmentPlan(plan)
  supervisionForm.treatmentPlan = parsed.treatmentPlan
  supervisionForm.customPlanDetail = parsed.customPlanDetail
}

function parseSupervisionRecords(records?: string) {
  if (!records) {
    supervisionForm.supervisionRecords = [createEmptyRecord()]
    return
  }
  try {
    const parsed = JSON.parse(records)
    supervisionForm.supervisionRecords = Array.isArray(parsed) && parsed.length > 0
      ? parsed
      : [createEmptyRecord()]
  } catch {
    supervisionForm.supervisionRecords = [createEmptyRecord()]
  }
}

function resetFormFromRow(row: any) {
  draftId.value = undefined
  recordCreateTime.value = null
  supervisionForm.category = row.crowdCategory || row.category || ""
  supervisionForm.gender = row.gender || ""
  supervisionForm.age = row.age ?? null
  supervisionForm.phone = row.phone || ""
  supervisionForm.phoneRemark = ""
  supervisionForm.currentAddress = row.currentAddress || ""
  supervisionForm.householdAddress = row.householdAddress || ""
  supervisionForm.idNumber = row.idNumber || ""
  supervisionForm.birthDate = formatDateValue(row.birthDate)
  supervisionForm.ethnicity = row.ethnicity || ""
  supervisionForm.hasPreventiveTreatment = ""
  supervisionForm.treatmentStartDate = ""
  supervisionForm.treatmentEndDate = ""
  supervisionForm.nextSupervisionDate = ""
  supervisionForm.treatmentPlan = ""
  supervisionForm.customPlanDetail = ""
  supervisionForm.managingUnit = row.managingUnit || row.preventiveManager || ""
  if (!supervisionForm.managingUnit.trim()) {
    supervisionForm.managingUnit = (userStore.orgName || userStore.departmentName || "").trim()
  }
  supervisionForm.supervisingDoctor = row.supervisingDoctor || ""
  supervisionForm.supervisionRecords = [createEmptyRecord()]
  supervisionForm.treatmentCompletionStatus = ""
  supervisionForm.interruptMedication = ""
  supervisionForm.interruptCount = null
  supervisionForm.totalDoses = null
  supervisionForm.actualDoses = null
  supervisionForm.medicationRate = ""
  supervisionForm.managerType = ""
  supervisionForm.managerName = ""
  supervisionForm.remark = ""
  supervisionForm.attachmentUrls = ""
}

function applyFormData(data: Record<string, any>, row: any) {
  resetFormFromRow(row)
  draftId.value = data.id
  recordCreateTime.value = data.createTime ?? null
  supervisionForm.category = data.category || supervisionForm.category
  supervisionForm.gender = data.gender || supervisionForm.gender
  supervisionForm.age = data.age ?? supervisionForm.age
  supervisionForm.phone = data.phone || supervisionForm.phone
  supervisionForm.phoneRemark = data.phoneRemark || supervisionForm.phoneRemark
  supervisionForm.currentAddress = data.currentAddress || supervisionForm.currentAddress
  // 空字符串不覆盖档案预填（身份证/民族等可能来自筛查回填）
  supervisionForm.householdAddress = data.householdAddress || supervisionForm.householdAddress
  supervisionForm.idNumber = data.idNumber || supervisionForm.idNumber
  supervisionForm.birthDate = formatDateValue(data.birthDate) || supervisionForm.birthDate
  supervisionForm.ethnicity = data.ethnicity || supervisionForm.ethnicity
  supervisionForm.hasPreventiveTreatment = data.hasPreventiveTreatment || supervisionForm.hasPreventiveTreatment
  supervisionForm.treatmentStartDate = formatDateValue(data.treatmentStartDate)
  supervisionForm.treatmentEndDate = formatDateValue(data.treatmentEndDate)
  supervisionForm.nextSupervisionDate = formatDateValue(data.nextSupervisionDate)
  parseTreatmentPlan(data.treatmentPlan)
  supervisionForm.managingUnit = data.managingUnit || supervisionForm.managingUnit
  supervisionForm.supervisingDoctor = data.supervisingDoctor || supervisionForm.supervisingDoctor
  parseSupervisionRecords(data.supervisionRecords)
  supervisionForm.treatmentCompletionStatus = data.treatmentCompletionStatus ?? ""
  supervisionForm.interruptMedication = data.interruptMedication ?? ""
  supervisionForm.interruptCount = data.interruptCount ?? null
  supervisionForm.totalDoses = data.totalDoses ?? null
  supervisionForm.actualDoses = data.actualDoses ?? null
  supervisionForm.medicationRate = data.medicationRate ?? ""
  supervisionForm.managerType = data.managerType ?? ""
  supervisionForm.managerName = data.managerName ?? ""
  supervisionForm.remark = data.remark ?? ""
  parseAttachmentUrlsField(data.attachmentUrls)
}

/** 拉取潜伏详情（含筛查回填的民族/出生日期），再与列表行合并 */
async function resolveLatentProfile(row: any) {
  try {
    const { data } = await getLatentDetailApi(row.id)
    if (data) return mergeSupervisionProfileFields(row, data)
  } catch { /* 回退列表行 */ }
  return row
}

async function loadDraft() {
  if (!props.latentRow?.id) return
  const profile = await resolveLatentProfile(props.latentRow)
  resetFormFromRow(profile)
  try {
    const { data } = await getSupervisionDraftApi(props.latentRow.id)
    if (data) {
      applyFormData(data, profile)
    }
  } catch { /* 无草稿 */ }
}

async function loadInitialData() {
  if (!props.initialData || !props.latentRow) return
  const profile = await resolveLatentProfile(props.latentRow)
  applyFormData(props.initialData, profile)
}

async function initForm() {
  if (props.initialData) {
    await loadInitialData()
  } else {
    await loadDraft()
  }
}

/** 加载系统机构名称（部门名称）作为管理单位选项 */
async function loadOrgNameOptions() {
  orgOptionsLoading.value = true
  try {
    const { data } = await getDepartmentListApi()
    const names = (Array.isArray(data) ? data : [])
      .map(item => (item.name || "").trim())
      .filter(Boolean)
    const extras = [
      userStore.orgName,
      userStore.departmentName,
      supervisionForm.managingUnit
    ]
      .map(item => (item || "").trim())
      .filter(Boolean)
    orgNameOptions.value = Array.from(new Set([...names, ...extras]))
  } catch {
    orgNameOptions.value = []
  } finally {
    orgOptionsLoading.value = false
  }
}

watch(
  () => [visible.value, props.latentRow?.id, props.initialData?.id] as const,
  async ([open]) => {
    if (open && props.latentRow) {
      await Promise.all([initForm(), loadOrgNameOptions()])
      // 表单加载后再补一次，确保已有管理单位出现在下拉中
      const current = supervisionForm.managingUnit?.trim()
      if (current && !orgNameOptions.value.includes(current)) {
        orgNameOptions.value = [...orgNameOptions.value, current]
      }
      nextTick(() => formRef.value?.clearValidate())
    }
  },
  { immediate: true }
)

watch(
  () => supervisionForm.treatmentPlan,
  () => {
    formRef.value?.clearValidate("treatmentStartDate")
  }
)

function resolveMedicationRate() {
  if (supervisionForm.medicationRate) return supervisionForm.medicationRate
  if (supervisionForm.totalDoses && supervisionForm.actualDoses !== null && supervisionForm.totalDoses > 0) {
    return `${((supervisionForm.actualDoses / supervisionForm.totalDoses) * 100).toFixed(1)}%`
  }
  return undefined
}

function resolveTreatmentPlan() {
  return formatLatentSupervisionTreatmentPlan(supervisionForm.treatmentPlan, supervisionForm.customPlanDetail)
}

function buildPayload(status: number) {
  const attachmentUrls = supervisionForm.attachmentUrls || undefined
  return {
    id: draftId.value ?? props.initialData?.id ?? undefined,
    latentInfectionId: props.latentRow!.id,
    populationType: props.latentRow!.populationType,
    patientName: props.latentRow!.name,
    category: supervisionForm.category || undefined,
    gender: supervisionForm.gender || undefined,
    age: supervisionForm.age ?? undefined,
    phone: supervisionForm.phone || undefined,
    phoneRemark: supervisionForm.phoneRemark || undefined,
    currentAddress: supervisionForm.currentAddress || undefined,
    householdAddress: supervisionForm.householdAddress || undefined,
    idNumber: supervisionForm.idNumber || undefined,
    birthDate: supervisionForm.birthDate || undefined,
    ethnicity: supervisionForm.ethnicity || undefined,
    hasPreventiveTreatment: supervisionForm.hasPreventiveTreatment || undefined,
    treatmentStartDate: supervisionForm.treatmentStartDate || undefined,
    treatmentEndDate: supervisionForm.treatmentEndDate || undefined,
    nextSupervisionDate: supervisionForm.nextSupervisionDate || undefined,
    treatmentPlan: resolveTreatmentPlan() || undefined,
    managingUnit: supervisionForm.managingUnit || undefined,
    supervisingDoctor: supervisionForm.supervisingDoctor || undefined,
    supervisionRecords: supervisionForm.supervisionRecords.length > 0
      ? JSON.stringify(supervisionForm.supervisionRecords)
      : undefined,
    treatmentCompletionStatus: supervisionForm.treatmentCompletionStatus || undefined,
    interruptMedication: supervisionForm.interruptMedication || undefined,
    interruptCount: supervisionForm.interruptMedication === "有" ? supervisionForm.interruptCount ?? undefined : undefined,
    totalDoses: supervisionForm.totalDoses ?? undefined,
    actualDoses: supervisionForm.actualDoses ?? undefined,
    medicationRate: resolveMedicationRate(),
    managerType: supervisionForm.managerType || undefined,
    managerName: supervisionForm.managerName || undefined,
    remark: supervisionForm.remark || undefined,
    attachmentUrls,
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

async function handleSaveDraft() {
  if (!props.latentRow?.id || draftSaving.value || formDisabled.value) return

  draftSaving.value = true
  try {
    await saveSupervisionDraftApi(buildPayload(0))
    ElMessage.success("督导表草稿已保存")
    visible.value = false
    emit("success")
  } catch { /* handled by interceptor */ } finally {
    draftSaving.value = false
  }
}

async function handleSubmit() {
  if (!props.latentRow?.id || submitting.value || formDisabled.value) return
  if (!await validateForm()) return

  submitting.value = true
  try {
    await saveSupervisionApi(buildPayload(1))
    ElMessage.success(isEditMode.value ? "督导表修改成功" : "督导表提交成功")
    visible.value = false
    emit("success")
  } catch { /* handled by interceptor */ } finally {
    submitting.value = false
  }
}

async function handleArchive() {
  if (!props.latentRow?.id || archiving.value || formDisabled.value) return
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
    :title="dialogTitle"
    width="960px"
    append-to-body
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="supervisionForm"
      :rules="formDisabled ? undefined : rules"
      label-width="140px"
      :disabled="formDisabled"
    >
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="姓名">
            <el-input :value="latentRow?.name" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="人群分类">
            <el-select v-model="supervisionForm.category" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in SUPERVISION_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="现居住地址">
        <el-input v-model="supervisionForm.currentAddress" placeholder="请输入现居住地址" />
      </el-form-item>
      <el-form-item label="户籍地址">
        <el-input v-model="supervisionForm.householdAddress" placeholder="请输入户籍地址" />
      </el-form-item>
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
        <el-col :span="8">
          <el-form-item label="年龄">
            <el-input-number v-model="supervisionForm.age" :min="0" :max="150" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="电话号码">
            <el-input v-model="supervisionForm.phone" placeholder="请输入" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="民族">
            <el-input v-model="supervisionForm.ethnicity" placeholder="请输入民族" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="电话备注">
        <el-input
          v-model="supervisionForm.phoneRemark"
          placeholder="非本人电话时请填写说明（如与本人关系）"
        />
      </el-form-item>
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
          <el-form-item label="是否开始预防性治疗">
            <el-select
              v-model="supervisionForm.hasPreventiveTreatment"
              placeholder="请选择"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in PREVENTIVE_TREATMENT_YES_NO_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        治疗方案
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="治疗方案" prop="treatmentPlan">
            <el-select v-model="supervisionForm.treatmentPlan" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in LATENT_TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="治疗开始时间" prop="treatmentStartDate" :required="isTreatmentStartDateRequired">
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
          <el-form-item label="下次督导时间">
            <el-date-picker
              v-model="supervisionForm.nextSupervisionDate"
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
          <el-form-item label="管理单位">
            <el-select
              v-model="supervisionForm.managingUnit"
              placeholder="请选择管理单位"
              clearable
              filterable
              :loading="orgOptionsLoading"
              style="width: 100%"
            >
              <el-option
                v-for="name in orgNameOptions"
                :key="name"
                :label="name"
                :value="name"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="督导医生">
        <el-input v-model="supervisionForm.supervisingDoctor" placeholder="请输入督导医生" />
      </el-form-item>
      <el-row v-if="isLatentIndividualPlan(supervisionForm.treatmentPlan)">
        <el-col :span="24">
          <el-form-item label="方案详情">
            <el-input
              v-model="supervisionForm.customPlanDetail"
              type="textarea"
              :rows="3"
              placeholder="请手动录入个体治疗方案详情"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        督导记录
      </el-divider>
      <div
        v-for="(record, index) in supervisionForm.supervisionRecords"
        :key="index"
        class="mb-3 border rounded p-3"
      >
        <el-row :gutter="8">
          <el-col :span="8">
            <el-form-item :label="`督导时间${index + 1}`" label-width="90px">
              <el-date-picker
                v-model="record.time"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="督导方式" label-width="80px">
              <el-select v-model="record.method" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="item in SUPERVISION_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8" class="flex items-center justify-end">
            <el-button
              v-if="!formDisabled && supervisionForm.supervisionRecords.length > 1"
              type="danger"
              link
              size="small"
              @click="supervisionForm.supervisionRecords.splice(index, 1)"
            >
              删除
            </el-button>
          </el-col>
        </el-row>
        <el-form-item label="督导内容" label-width="90px">
          <el-input v-model="record.content" type="textarea" :rows="2" placeholder="请填写督导内容" />
        </el-form-item>
        <el-form-item label="备注" label-width="90px">
          <el-input v-model="record.remark" placeholder="请填写备注" />
        </el-form-item>
      </div>
      <div v-if="!formDisabled" class="mb-4">
        <el-button
          type="primary"
          link
          @click="supervisionForm.supervisionRecords.push(createEmptyRecord())"
        >
          + 添加督导记录
        </el-button>
      </div>

      <el-divider content-position="left">
        全疗程规律治疗评价
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="中断用药">
            <el-radio-group v-model="supervisionForm.interruptMedication">
              <el-radio v-for="item in INTERRUPT_MEDICATION_OPTIONS" :key="item.value" :value="item.value">
                {{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="治疗完成情况">
            <el-select v-model="supervisionForm.treatmentCompletionStatus" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in TREATMENT_COMPLETION_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="中断次数">
            <el-input-number
              v-model="supervisionForm.interruptCount"
              :min="0"
              :disabled="supervisionForm.interruptMedication !== '有'"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="全程应用药次数">
            <el-input-number v-model="supervisionForm.totalDoses" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="实际用药次数">
            <el-input-number v-model="supervisionForm.actualDoses" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="用药率">
            <el-input v-model="supervisionForm.medicationRate" placeholder="自动计算或手动填写" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">
        督导管理人员
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="管理人员类型">
            <el-select v-model="supervisionForm.managerType" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in SUPERVISION_MANAGER_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="管理人员姓名">
            <el-input v-model="supervisionForm.managerName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        其他
      </el-divider>
      <el-form-item label="备注">
        <el-input v-model="supervisionForm.remark" type="textarea" :rows="3" placeholder="请填写备注" />
      </el-form-item>
      <el-form-item label="上传10张">
        <ImageUploader
          v-model="supervisionForm.attachmentUrls"
          :disabled="formDisabled"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">
        {{ formDisabled ? "关闭" : "取消" }}
      </el-button>
      <template v-if="!formDisabled">
        <el-button type="info" :loading="draftSaving" :disabled="submitting || archiving" @click="handleSaveDraft">
          保存草稿
        </el-button>
        <el-button type="primary" :loading="submitting" :disabled="draftSaving || archiving" @click="handleSubmit">
          {{ isEditMode ? "保存修改" : "提交" }}
        </el-button>
        <el-button type="success" :loading="archiving" :disabled="draftSaving || submitting" @click="handleArchive">
          归档
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>
