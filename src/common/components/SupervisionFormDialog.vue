<script lang="ts" setup>
import type { FormInstance, FormRules } from "element-plus"
import {
  INTERRUPT_MEDICATION_OPTIONS,
  SUPERVISION_CATEGORY_OPTIONS,
  SUPERVISION_MANAGER_TYPE_OPTIONS,
  SUPERVISION_METHOD_OPTIONS,
  TREATMENT_PLAN_OPTIONS
} from "@@/constants/disease"
import { getToken } from "@@/utils/cache/cookies"
import { getAttachmentLabel, parseAttachmentUrls, resolveFileUrl } from "@@/utils/attachment"
import { Upload } from "@element-plus/icons-vue"
import { getSupervisionDetailApi, saveSupervisionApi } from "@/pages/latent-management/apis"

interface SupervisionRecord {
  time: string
  content: string
  method: string
  remark: string
}

const props = defineProps<{
  latentRow: any | null
  readonly?: boolean
}>()

const emit = defineEmits<{ success: [] }>()
const visible = defineModel<boolean>({ default: false })
const formRef = ref<FormInstance>()
const submitting = ref(false)
const archiving = ref(false)
const formId = ref<number | undefined>(undefined)

const supervisionForm = reactive({
  category: "",
  gender: "",
  age: null as number | null,
  phone: "",
  currentAddress: "",
  treatmentStartDate: "",
  treatmentEndDate: "",
  treatmentPlan: "",
  customPlanDetail: "",
  supervisionRecords: [] as SupervisionRecord[],
  interruptMedication: "",
  interruptCount: null as number | null,
  totalDoses: null as number | null,
  actualDoses: null as number | null,
  medicationRate: "",
  managerType: "",
  managerName: "",
  remark: ""
})

const rules: FormRules = {
  treatmentStartDate: [{ required: true, message: "请选择开始治疗时间", trigger: "change" }],
  treatmentPlan: [{ required: true, message: "请选择治疗方案", trigger: "change" }]
}

const attachmentFileList = ref<{ name: string, url: string }[]>([])
const uploadAction = `${import.meta.env.VITE_BASE_URL}/file/upload`
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${getToken()}` }))

function formatDateValue(value: unknown): string {
  if (!value) return ""
  const str = String(value)
  return str.length >= 10 ? str.slice(0, 10) : str
}

function getAttachmentDisplayLabel(url: string, index: number | string): string {
  return getAttachmentLabel(url, Number(index))
}

function parseAttachmentUrlsField(urls?: string) {
  attachmentFileList.value = parseAttachmentUrls(urls).map((url, index) => ({
    name: getAttachmentDisplayLabel(url, index),
    url
  }))
}

function syncAttachmentFileList(uploadFiles: { name: string, url?: string, status?: string, uid?: number }[]) {
  attachmentFileList.value = uploadFiles
    .filter(file => file.status === "success" && file.url)
    .map((file, index) => ({
      name: file.name || getAttachmentDisplayLabel(file.url!, index),
      url: file.url!
    }))
}

function createEmptyRecord(): SupervisionRecord {
  return { time: "", content: "", method: "", remark: "" }
}

function parseTreatmentPlan(plan?: string) {
  if (plan?.startsWith("个体化方案：")) {
    supervisionForm.treatmentPlan = "个体化方案"
    supervisionForm.customPlanDetail = plan.replace("个体化方案：", "")
  } else if (plan === "个体化方案") {
    supervisionForm.treatmentPlan = "个体化方案"
    supervisionForm.customPlanDetail = ""
  } else {
    supervisionForm.treatmentPlan = plan ?? ""
    supervisionForm.customPlanDetail = ""
  }
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
  formId.value = undefined
  supervisionForm.category = row.crowdCategory || row.category || ""
  supervisionForm.gender = row.gender || ""
  supervisionForm.age = row.age ?? null
  supervisionForm.phone = row.phone || ""
  supervisionForm.currentAddress = row.currentAddress || ""
  supervisionForm.treatmentStartDate = ""
  supervisionForm.treatmentEndDate = ""
  supervisionForm.treatmentPlan = ""
  supervisionForm.customPlanDetail = ""
  supervisionForm.supervisionRecords = [createEmptyRecord()]
  supervisionForm.interruptMedication = ""
  supervisionForm.interruptCount = null
  supervisionForm.totalDoses = null
  supervisionForm.actualDoses = null
  supervisionForm.medicationRate = ""
  supervisionForm.managerType = ""
  supervisionForm.managerName = ""
  supervisionForm.remark = ""
  attachmentFileList.value = []
}

function applyDetailToForm(data: Record<string, any>, row: any) {
  resetFormFromRow(row)
  formId.value = data.id
  supervisionForm.category = data.category ?? supervisionForm.category
  supervisionForm.gender = data.gender ?? supervisionForm.gender
  supervisionForm.age = data.age ?? supervisionForm.age
  supervisionForm.phone = data.phone ?? supervisionForm.phone
  supervisionForm.currentAddress = data.currentAddress ?? supervisionForm.currentAddress
  supervisionForm.treatmentStartDate = formatDateValue(data.treatmentStartDate)
  supervisionForm.treatmentEndDate = formatDateValue(data.treatmentEndDate)
  parseTreatmentPlan(data.treatmentPlan)
  parseSupervisionRecords(data.supervisionRecords)
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

function beforeAttachmentUpload(file: File) {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error("附件大小不能超过 20MB")
    return false
  }
  return true
}

function handleAttachmentSuccess(response: any, uploadFile: any, uploadFiles: any[]) {
  if (response?.code === 200 && response?.data) {
    uploadFile.url = resolveFileUrl(response.data)
    uploadFile.status = "success"
    syncAttachmentFileList(uploadFiles)
  } else {
    uploadFile.status = "fail"
    ElMessage.error(response?.msg || "附件上传失败")
  }
}

function handleAttachmentChange(_uploadFile: any, uploadFiles: any[]) {
  syncAttachmentFileList(uploadFiles)
}

function handleAttachmentRemove(uploadFile: { name: string, url?: string, uid?: number }) {
  attachmentFileList.value = attachmentFileList.value.filter(
    f => f.url !== uploadFile.url && f.name !== uploadFile.name
  )
}

function handleAttachmentError() {
  ElMessage.error("附件上传失败，请重试")
}

function resolveMedicationRate() {
  if (supervisionForm.medicationRate) return supervisionForm.medicationRate
  if (supervisionForm.totalDoses && supervisionForm.actualDoses !== null && supervisionForm.totalDoses > 0) {
    return `${((supervisionForm.actualDoses / supervisionForm.totalDoses) * 100).toFixed(1)}%`
  }
  return undefined
}

function resolveTreatmentPlan() {
  if (supervisionForm.treatmentPlan === "个体化方案") {
    return supervisionForm.customPlanDetail
      ? `个体化方案：${supervisionForm.customPlanDetail}`
      : "个体化方案"
  }
  return supervisionForm.treatmentPlan
}

function buildPayload(status: number) {
  const attachmentUrls = attachmentFileList.value.map(f => f.url).join(",")
  return {
    ...(formId.value ? { id: formId.value } : {}),
    latentInfectionId: props.latentRow!.id,
    populationType: props.latentRow!.populationType,
    patientName: props.latentRow!.name,
    category: supervisionForm.category || undefined,
    gender: supervisionForm.gender || undefined,
    age: supervisionForm.age ?? undefined,
    phone: supervisionForm.phone || undefined,
    currentAddress: supervisionForm.currentAddress || undefined,
    treatmentStartDate: supervisionForm.treatmentStartDate || undefined,
    treatmentEndDate: supervisionForm.treatmentEndDate || undefined,
    treatmentPlan: resolveTreatmentPlan() || undefined,
    supervisionRecords: supervisionForm.supervisionRecords.length > 0
      ? JSON.stringify(supervisionForm.supervisionRecords)
      : undefined,
    interruptMedication: supervisionForm.interruptMedication || undefined,
    interruptCount: supervisionForm.interruptMedication === "有" ? supervisionForm.interruptCount ?? undefined : undefined,
    totalDoses: supervisionForm.totalDoses ?? undefined,
    actualDoses: supervisionForm.actualDoses ?? undefined,
    medicationRate: resolveMedicationRate(),
    managerType: supervisionForm.managerType || undefined,
    managerName: supervisionForm.managerName || undefined,
    remark: supervisionForm.remark || undefined,
    attachmentUrls: attachmentUrls || undefined,
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

/** 保存草稿（status=1） */
async function handleSaveDraft() {
  if (!props.latentRow?.id || submitting.value) return

  submitting.value = true
  try {
    await saveSupervisionApi(buildPayload(1))
    ElMessage.success("督导表草稿已保存")
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
    title="填写预防性治疗督导表"
    width="960px"
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
          <el-form-item label="类别">
            <el-select v-model="supervisionForm.category" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in SUPERVISION_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="性别">
            <el-select v-model="supervisionForm.gender" placeholder="请选择" clearable style="width: 100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
        </el-col>
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
      </el-row>
      <el-form-item label="现住址">
        <el-input v-model="supervisionForm.currentAddress" placeholder="请输入现住址" />
      </el-form-item>

      <el-divider content-position="left">
        治疗方案
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="开始治疗时间" prop="treatmentStartDate">
            <el-date-picker
              v-model="supervisionForm.treatmentStartDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="治疗方案" prop="treatmentPlan">
            <el-select v-model="supervisionForm.treatmentPlan" placeholder="请选择" clearable style="width: 100%">
              <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="supervisionForm.treatmentPlan === '个体化方案'">
        <el-col :span="24">
          <el-form-item label="方案详情">
            <el-input
              v-model="supervisionForm.customPlanDetail"
              type="textarea"
              :rows="3"
              placeholder="请注明详细的抗结核治疗方案"
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
              v-if="!readonly && supervisionForm.supervisionRecords.length > 1"
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
      <div v-if="!readonly" class="mb-4">
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
        <el-col :span="12">
          <el-form-item label="中断用药">
            <el-radio-group v-model="supervisionForm.interruptMedication">
              <el-radio v-for="item in INTERRUPT_MEDICATION_OPTIONS" :key="item.value" :value="item.value">
                {{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
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
      <el-form-item label="结束疗程时间">
        <el-date-picker
          v-model="supervisionForm.treatmentEndDate"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>

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
      <el-form-item label="附件上传">
        <el-upload
          :action="uploadAction"
          :headers="uploadHeaders"
          :file-list="attachmentFileList"
          :before-upload="beforeAttachmentUpload"
          :on-success="handleAttachmentSuccess"
          :on-change="handleAttachmentChange"
          :on-remove="handleAttachmentRemove"
          :on-error="handleAttachmentError"
          :disabled="readonly"
          multiple
        >
          <el-button type="primary" size="small" :disabled="readonly">
            <el-icon class="mr-1">
              <Upload />
            </el-icon>
            点击上传
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持图片、PDF 等格式，单个文件不超过 20MB
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">
        {{ readonly ? "关闭" : "取消" }}
      </el-button>
      <template v-if="!readonly">
        <el-button type="primary" :loading="submitting" @click="handleSaveDraft">
          保存草稿
        </el-button>
        <el-button type="success" :loading="archiving" @click="handleArchive">
          归档
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>
