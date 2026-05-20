<script lang="ts" setup>
import ImageUploader from "@@/components/ImageUploader.vue"
import {
  DRUG_FORM_OPTIONS,
  DRUG_RESISTANCE_OPTIONS,
  EDUCATION_ITEMS,
  FIRST_VISIT_SUPERVISOR_OPTIONS,
  MEDICATION_USAGE_OPTIONS,
  SPUTUM_STATUS_OPTIONS,
  SYMPTOM_OPTIONS,
  VENTILATION_OPTIONS,
  VISIT_METHOD_OPTIONS
} from "@@/constants/disease"
import { getFirstVisitDetailApi, saveFirstVisitApi } from "@/pages/patient-management/apis"

const props = defineProps<{
  visible: boolean
  patientRow: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

function createEmptyForm() {
  const educationItems: Record<string, string> = {}
  EDUCATION_ITEMS.forEach((item) => {
    educationItems[item] = ""
  })
  return {
    visitDate: "",
    visitMethod: "",
    patientType: "",
    sputumStatus: "",
    drugResistance: "",
    symptoms: [] as string[],
    otherSymptoms: "",
    chemotherapy: "",
    medicationUsage: "",
    drugForm: [] as string[],
    supervisor: "",
    separateRoom: "",
    ventilation: "",
    smokingAmount: "",
    drinkingAmount: "",
    medicationLocation: "",
    medicationPickTime: "",
    educationItems,
    nextVisitDate: "",
    doctorSignature: "",
    remarks: "",
    attachmentUrls: ""
  }
}

const firstVisitForm = reactive(createEmptyForm())
const saving = ref(false)

function parseLoadedData(data: Record<string, any>) {
  const base = createEmptyForm()
  const symptoms = data.symptoms
    ? String(data.symptoms).split(",").map((s: string) => s.trim()).filter(Boolean)
    : []
  const drugForm = data.drugForm
    ? String(data.drugForm).split(",").map((s: string) => s.trim()).filter(Boolean)
    : []
  let educationItems = { ...base.educationItems }
  if (data.educationItems) {
    try {
      const parsed = typeof data.educationItems === "string"
        ? JSON.parse(data.educationItems)
        : data.educationItems
      educationItems = { ...educationItems, ...parsed }
    } catch { /* ignore */ }
  }
  Object.assign(firstVisitForm, {
    ...base,
    ...data,
    symptoms,
    drugForm,
    educationItems,
    attachmentUrls: data.attachmentUrls ?? ""
  })
}

async function loadExisting() {
  if (!props.patientRow) return
  Object.assign(firstVisitForm, createEmptyForm())
  try {
    const { data } = await getFirstVisitDetailApi(props.patientRow.id)
    if (data) {
      parseLoadedData(data)
    }
  } catch { /* 首次填写 */ }
}

watch(
  () => props.visible,
  (val) => {
    if (val) loadExisting()
  }
)

function close() {
  emit("update:visible", false)
}

async function handleSave() {
  if (!props.patientRow || saving.value) return
  saving.value = true
  try {
    await saveFirstVisitApi({
      patientId: props.patientRow.id,
      populationType: props.patientRow.populationType,
      ...firstVisitForm,
      symptoms: firstVisitForm.symptoms.join(","),
      drugForm: firstVisitForm.drugForm.join(","),
      educationItems: JSON.stringify(firstVisitForm.educationItems)
    })
    ElMessage.success("首次随访保存成功")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="肺结核患者第一次入户随访记录"
    width="920px"
    top="5vh"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form :model="firstVisitForm" label-width="110px" size="default">
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="随访时间">
            <el-date-picker v-model="firstVisitForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="随访方式">
            <el-radio-group v-model="firstVisitForm.visitMethod">
              <el-radio v-for="item in VISIT_METHOD_OPTIONS" :key="item" :value="item">
                {{ item }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="患者类型">
            <el-radio-group v-model="firstVisitForm.patientType">
              <el-radio value="初治">
                初治
              </el-radio>
              <el-radio value="复治">
                复治
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="痰菌情况">
            <el-select v-model="firstVisitForm.sputumStatus" style="width: 100%">
              <el-option v-for="item in SPUTUM_STATUS_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="耐药情况">
            <el-select v-model="firstVisitForm.drugResistance" style="width: 100%">
              <el-option v-for="item in DRUG_RESISTANCE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="其他症状">
            <el-input v-model="firstVisitForm.otherSymptoms" placeholder="如有其他症状请填写" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="症状及体征">
        <el-checkbox-group v-model="firstVisitForm.symptoms">
          <el-checkbox v-for="s in SYMPTOM_OPTIONS" :key="s.value" :value="s.value">
            {{ s.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-divider content-position="left">
        用药情况
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="化疗方案">
            <el-input v-model="firstVisitForm.chemotherapy" placeholder="化疗方案" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="用法">
            <el-radio-group v-model="firstVisitForm.medicationUsage">
              <el-radio v-for="item in MEDICATION_USAGE_OPTIONS" :key="item" :value="item">
                {{ item }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="督导人员">
            <el-select v-model="firstVisitForm.supervisor" style="width: 100%">
              <el-option v-for="item in FIRST_VISIT_SUPERVISOR_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="药品剂型">
        <el-checkbox-group v-model="firstVisitForm.drugForm">
          <el-checkbox v-for="item in DRUG_FORM_OPTIONS" :key="item" :value="item">
            {{ item }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-divider content-position="left">
        居住环境与生活方式
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-form-item label="单独居室">
            <el-radio-group v-model="firstVisitForm.separateRoom">
              <el-radio value="有">
                有
              </el-radio>
              <el-radio value="无">
                无
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="通风情况">
            <el-select v-model="firstVisitForm.ventilation" style="width: 100%">
              <el-option v-for="item in VENTILATION_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="吸烟(支/天)">
            <el-input v-model="firstVisitForm.smokingAmount" placeholder="0" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="饮酒(两/天)">
            <el-input v-model="firstVisitForm.drinkingAmount" placeholder="0" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        健康教育及培训
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="取药地点">
            <el-input v-model="firstVisitForm.medicationLocation" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="取药时间">
            <el-date-picker
              v-model="firstVisitForm.medicationPickTime"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col v-for="item in EDUCATION_ITEMS" :key="item" :span="12">
          <el-form-item :label="item" label-width="170px">
            <el-radio-group v-model="firstVisitForm.educationItems[item]">
              <el-radio value="掌握">
                掌握
              </el-radio>
              <el-radio value="未掌握">
                未掌握
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        其他
      </el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="下次随访时间">
            <el-date-picker v-model="firstVisitForm.nextVisitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="评估医生签名">
            <el-input v-model="firstVisitForm.doctorSignature" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        备注与附件
      </el-divider>
      <el-form-item label="备注">
        <el-input v-model="firstVisitForm.remarks" type="textarea" :rows="2" placeholder="请填写" />
      </el-form-item>
      <el-form-item label="附件（2~6张图片）">
        <ImageUploader v-model="firstVisitForm.attachmentUrls" :min="2" :max="6" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">
        取消
      </el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>
