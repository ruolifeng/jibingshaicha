<script lang="ts" setup>
import type { MedicationRecordsMap } from "@@/utils/medicationRecords"
/** 服药管理弹窗（含每日服药日历 + 治疗记录卡打印） */
import MedicationCalendar from "@@/components/MedicationCalendar.vue"
import PrintMedication from "@@/components/PrintMedication.vue"
import {
  MANAGEMENT_METHOD_OPTIONS,
  SPUTUM_RESULT_OPTIONS,
  SUPERVISOR_OPTIONS
} from "@@/constants/disease"
import { confirmEditChange } from "@@/utils/listToolbar"
import {
  applyMedicationFormDefaults,
  MEDICATION_LOCKED_MANAGEMENT_METHOD,
  medicationSelectOptions,
  syncStartTreatmentDateFromMarks
} from "@@/utils/medicationManagement"
import {
  getEarliestMedicationMarkedDate,

  parseMedicationRecords,
  serializeMedicationRecords
} from "@@/utils/medicationRecords"
import {
  completeMedicationApi,
  getFirstVisitApi,
  getMedicationApi,
  saveMedicationApi
} from "@/pages/school/patient/apis"

const props = defineProps<{
  visible: boolean
  patientRow: Record<string, any> | null
  /** 只读模式：归档后仅查看，不可编辑 */
  readOnly?: boolean
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const medicationForm = reactive({
  managementMethod: MEDICATION_LOCKED_MANAGEMENT_METHOD,
  supervisor: "",
  sputumResult: "",
  startTreatmentDate: "",
  stopDate: "",
  dayMarks: {} as MedicationRecordsMap
})

const saving = ref(false)
const draftSaving = ref(false)
const printVisible = ref(false)
const formId = ref<number | undefined>(undefined)
const startTreatmentDateManual = ref(false)

const supervisorOptions = computed(() =>
  medicationSelectOptions(SUPERVISOR_OPTIONS, medicationForm.supervisor)
)
const sputumResultOptions = computed(() =>
  medicationSelectOptions(SPUTUM_RESULT_OPTIONS, medicationForm.sputumResult)
)

function resetForm() {
  formId.value = undefined
  startTreatmentDateManual.value = false
  medicationForm.managementMethod = MEDICATION_LOCKED_MANAGEMENT_METHOD
  medicationForm.supervisor = ""
  medicationForm.sputumResult = ""
  medicationForm.startTreatmentDate = ""
  medicationForm.stopDate = ""
  medicationForm.dayMarks = {}
}

async function loadMedication() {
  if (!props.patientRow) return
  resetForm()
  let saved: Record<string, any> | null = null
  try {
    const { data } = await getMedicationApi(props.patientRow.id)
    if (data) {
      saved = data
      formId.value = data.id
      medicationForm.dayMarks = parseMedicationRecords(data.medicationRecords)
      if (data.startTreatmentDate) {
        medicationForm.startTreatmentDate = data.startTreatmentDate
        const earliest = getEarliestMedicationMarkedDate(medicationForm.dayMarks)
        startTreatmentDateManual.value = !earliest || data.startTreatmentDate !== earliest
      }
    }
  } catch { /* 首次填写 */ }

  let firstVisit: Record<string, any> | null = null
  try {
    const { data } = await getFirstVisitApi(props.patientRow.id)
    firstVisit = data
  } catch { /* 无首次随访 */ }

  applyMedicationFormDefaults(medicationForm, { saved, firstVisit })
}

watch(
  () => medicationForm.dayMarks,
  () => {
    syncStartTreatmentDateFromMarks(medicationForm, startTreatmentDateManual.value)
  },
  { deep: true }
)

watch(
  () => props.visible,
  (val) => {
    if (val) loadMedication()
  }
)

function close() {
  emit("update:visible", false)
}

function buildSaveData() {
  return {
    ...(formId.value ? { id: formId.value } : {}),
    patientId: props.patientRow!.id,
    populationType: props.patientRow!.populationType,
    managementMethod: MEDICATION_LOCKED_MANAGEMENT_METHOD,
    supervisor: medicationForm.supervisor,
    sputumResult: medicationForm.sputumResult,
    startTreatmentDate: medicationForm.startTreatmentDate || null,
    stopDate: medicationForm.stopDate,
    medicationRecords: serializeMedicationRecords(medicationForm.dayMarks)
  }
}

/** 保存草稿：仅保存数据，不归档患者 */
async function handleSaveDraft() {
  if (!props.patientRow || draftSaving.value) return
  draftSaving.value = true
  try {
    await saveMedicationApi(buildSaveData())
    ElMessage.success("服药管理草稿已保存")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    draftSaving.value = false
  }
}

async function handleSave() {
  if (!props.patientRow || saving.value) return
  if (formId.value) {
    const name = props.patientRow.name?.trim() || "该患者"
    const confirmed = await confirmEditChange(`「${name}」的服药管理`)
    if (!confirmed) return
  }
  saving.value = true
  try {
    const saveData = buildSaveData()
    if (medicationForm.stopDate) {
      await completeMedicationApi(saveData)
      ElMessage.success("服药管理完成，患者已归档")
    } else {
      await saveMedicationApi(saveData)
      ElMessage.success("服药管理保存成功")
    }
    close()
    emit("success")
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

function handleStartTreatmentDateChange() {
  startTreatmentDateManual.value = true
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`${readOnly ? '查看服药管理' : '服药管理'}${patientRow?.name ? ` — ${patientRow.name}` : ''}`"
    width="700px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form :model="medicationForm" label-width="130px">
      <el-form-item label="每日服药记录">
        <MedicationCalendar
          v-model="medicationForm.dayMarks"
          :disabled="readOnly"
        />
      </el-form-item>
      <el-form-item label="开始治疗日期">
        <el-date-picker
          v-model="medicationForm.startTreatmentDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="首次标记服药日后自动生成"
          style="width: 100%"
          :disabled="readOnly"
          @change="handleStartTreatmentDateChange"
        />
      </el-form-item>
      <el-form-item label="管理方式">
        <el-select
          v-model="medicationForm.managementMethod"
          style="width: 100%"
          disabled
        >
          <el-option
            v-for="item in MANAGEMENT_METHOD_OPTIONS"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="督导人员">
        <el-select
          v-model="medicationForm.supervisor"
          placeholder="来自首次随访，可修改"
          style="width: 100%"
          :disabled="readOnly"
        >
          <el-option
            v-for="item in supervisorOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="治疗前痰菌检查">
        <el-select
          v-model="medicationForm.sputumResult"
          placeholder="来自首次随访痰菌情况，可修改"
          style="width: 100%"
          :disabled="readOnly"
        >
          <el-option
            v-for="item in sputumResultOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="停止完成时间">
        <el-date-picker
          v-model="medicationForm.stopDate"
          type="date"
          placeholder="填写后患者将归档"
          value-format="YYYY-MM-DD"
          style="width: 100%"
          :disabled="readOnly"
        />
      </el-form-item>
      <el-alert v-if="!readOnly && medicationForm.stopDate" type="warning" :closable="false">
        已填写停止完成时间。点击「保存草稿」仅暂存数据；点击「完成并归档」后，患者将从在管列表移入历史患者。
      </el-alert>
    </el-form>
    <template #footer>
      <el-button @click="close">
        {{ readOnly ? "关闭" : "取消" }}
      </el-button>
      <el-button @click="printVisible = true">
        打印治疗记录卡
      </el-button>
      <template v-if="!readOnly">
        <el-button type="primary" plain :loading="draftSaving" :disabled="saving" @click="handleSaveDraft">
          保存草稿
        </el-button>
        <el-button type="primary" :loading="saving" :disabled="draftSaving" @click="handleSave">
          {{ medicationForm.stopDate ? "完成并归档" : "保存" }}
        </el-button>
      </template>
    </template>
  </el-dialog>

  <PrintMedication
    v-if="patientRow"
    :visible="printVisible"
    :patient-data="patientRow"
    :medication-data="medicationForm"
    @update:visible="printVisible = $event"
  />
</template>
