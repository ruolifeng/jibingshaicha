<script lang="ts" setup>
import type { MedicationRecordsMap } from "@@/utils/medicationRecords"
/** 潜伏感染者服药管理弹窗：字段抓取自督导表 */
import MedicationCalendar from "@@/components/MedicationCalendar.vue"
import PrintMedication from "@@/components/PrintMedication.vue"
import {
  MANAGEMENT_METHOD_OPTIONS,
  SPUTUM_RESULT_OPTIONS,
  SUPERVISOR_OPTIONS
} from "@@/constants/disease"
import { confirmEditChange } from "@@/utils/listToolbar"
import {
  applyLatentMedicationFormDefaults,
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
  completeLatentMedicationApi,
  getLatentMedicationApi,
  getSupervisionDetailApi,
  saveLatentMedicationApi
} from "@/pages/latent-management/apis"

const props = defineProps<{
  visible: boolean
  latentRow: Record<string, any> | null
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
const formId = ref<string | undefined>(undefined)
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
  if (!props.latentRow) return
  resetForm()
  let saved: Record<string, any> | null = null
  try {
    const { data } = await getLatentMedicationApi(props.latentRow.id)
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

  let supervision: Record<string, any> | null = null
  try {
    const { data } = await getSupervisionDetailApi(props.latentRow.id)
    supervision = data
  } catch { /* 无督导表 */ }

  applyLatentMedicationFormDefaults(medicationForm, { saved, supervision })
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
    latentInfectionId: props.latentRow!.id,
    populationType: props.latentRow!.populationType,
    managementMethod: MEDICATION_LOCKED_MANAGEMENT_METHOD,
    supervisor: medicationForm.supervisor,
    sputumResult: medicationForm.sputumResult,
    startTreatmentDate: medicationForm.startTreatmentDate || null,
    stopDate: medicationForm.stopDate,
    medicationRecords: serializeMedicationRecords(medicationForm.dayMarks)
  }
}

async function handleSaveDraft() {
  if (!props.latentRow || draftSaving.value) return
  draftSaving.value = true
  try {
    await saveLatentMedicationApi(buildSaveData())
    ElMessage.success("服药管理草稿已保存")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    draftSaving.value = false
  }
}

async function handleSave() {
  if (!props.latentRow || saving.value) return
  if (formId.value) {
    const name = props.latentRow.name?.trim() || "该潜伏感染者"
    const confirmed = await confirmEditChange(`「${name}」的服药管理`)
    if (!confirmed) return
  }
  saving.value = true
  try {
    const saveData = buildSaveData()
    if (medicationForm.stopDate) {
      await completeLatentMedicationApi(saveData)
      ElMessage.success("服药管理完成，已归档至历史患者")
    } else {
      await saveLatentMedicationApi(saveData)
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
    :title="`${readOnly ? '查看服药管理' : '服药管理'}${latentRow?.name ? ` — ${latentRow.name}` : ''}`"
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
          placeholder="优先取督导表开始治疗日期"
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
          placeholder="来自督导表，可修改"
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
          placeholder="请选择（可选）"
          style="width: 100%"
          clearable
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
          placeholder="填写后将归档至历史患者"
          value-format="YYYY-MM-DD"
          style="width: 100%"
          :disabled="readOnly"
        />
      </el-form-item>
      <el-alert v-if="!readOnly && medicationForm.stopDate" type="warning" :closable="false">
        已填写停止完成时间。点击「保存草稿」仅暂存数据；点击「完成并归档」后，将从在管列表移入历史患者。
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
    v-if="latentRow"
    :visible="printVisible"
    :patient-data="latentRow"
    :medication-data="medicationForm"
    @update:visible="printVisible = $event"
  />
</template>
