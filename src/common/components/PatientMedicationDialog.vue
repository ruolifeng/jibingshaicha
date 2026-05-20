<script lang="ts" setup>
/** 服药管理弹窗（含每日服药日历 + 治疗记录卡打印） */
import PrintMedication from "@@/components/PrintMedication.vue"
import {
  MANAGEMENT_METHOD_OPTIONS,
  SPUTUM_RESULT_OPTIONS,
  SUPERVISOR_OPTIONS
} from "@@/constants/disease"
import { completeMedicationApi, getMedicationApi, saveMedicationApi } from "@/pages/school/patient/apis"

const props = defineProps<{
  visible: boolean
  patientRow: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const medicationForm = reactive({
  managementMethod: "",
  supervisor: "",
  sputumResult: "",
  stopDate: "",
  checkedDates: [] as string[]
})

const calendarMonth = ref(new Date())
const saving = ref(false)
const printVisible = ref(false)

const calendarDays = computed(() => {
  const year = calendarMonth.value.getFullYear()
  const month = calendarMonth.value.getMonth()
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const days: { date: string, day: number, blank: boolean }[] = []
  for (let i = 0; i < firstDay; i++) days.push({ date: "", day: 0, blank: true })
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`
    days.push({ date: dateStr, day: d, blank: false })
  }
  return days
})

const calendarTitle = computed(() => {
  const y = calendarMonth.value.getFullYear()
  const m = calendarMonth.value.getMonth() + 1
  return `${y}年${m}月`
})

function prevMonth() {
  const d = new Date(calendarMonth.value)
  d.setMonth(d.getMonth() - 1)
  calendarMonth.value = d
}

function nextMonth() {
  const d = new Date(calendarMonth.value)
  d.setMonth(d.getMonth() + 1)
  calendarMonth.value = d
}

function toggleDate(dateStr: string) {
  const idx = medicationForm.checkedDates.indexOf(dateStr)
  if (idx >= 0) medicationForm.checkedDates.splice(idx, 1)
  else medicationForm.checkedDates.push(dateStr)
}

function isDateChecked(dateStr: string) {
  return medicationForm.checkedDates.includes(dateStr)
}

function resetForm() {
  medicationForm.managementMethod = ""
  medicationForm.supervisor = ""
  medicationForm.sputumResult = ""
  medicationForm.stopDate = ""
  medicationForm.checkedDates = []
  calendarMonth.value = new Date()
}

async function loadMedication() {
  if (!props.patientRow) return
  resetForm()
  try {
    const { data } = await getMedicationApi(props.patientRow.id)
    if (data) {
      medicationForm.managementMethod = data.managementMethod || ""
      medicationForm.supervisor = data.supervisor || ""
      medicationForm.sputumResult = data.sputumResult || ""
      medicationForm.stopDate = data.stopDate || ""
      try {
        medicationForm.checkedDates = data.medicationRecords
          ? (typeof data.medicationRecords === "string"
              ? JSON.parse(data.medicationRecords)
              : data.medicationRecords)
          : []
      } catch {
        medicationForm.checkedDates = []
      }
    }
  } catch { /* 首次填写 */ }
}

watch(
  () => props.visible,
  (val) => {
    if (val) loadMedication()
  }
)

function close() {
  emit("update:visible", false)
}

async function handleSave() {
  if (!props.patientRow || saving.value) return
  saving.value = true
  try {
    const saveData: Record<string, any> = {
      patientId: props.patientRow.id,
      populationType: props.patientRow.populationType,
      managementMethod: medicationForm.managementMethod,
      supervisor: medicationForm.supervisor,
      sputumResult: medicationForm.sputumResult,
      stopDate: medicationForm.stopDate,
      medicationRecords: JSON.stringify([...medicationForm.checkedDates].sort())
    }
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
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`服药管理${patientRow?.name ? ' — ' + patientRow.name : ''}`"
    width="700px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form :model="medicationForm" label-width="130px">
      <el-form-item label="每日服药记录">
        <div class="med-calendar">
          <div class="med-calendar-header">
            <el-button text @click="prevMonth">&lt;</el-button>
            <span class="med-calendar-title">{{ calendarTitle }}</span>
            <el-button text @click="nextMonth">&gt;</el-button>
          </div>
          <div class="med-calendar-weekdays">
            <span v-for="w in ['日', '一', '二', '三', '四', '五', '六']" :key="w">{{ w }}</span>
          </div>
          <div class="med-calendar-grid">
            <div
              v-for="(cell, idx) in calendarDays"
              :key="idx"
              class="med-calendar-cell"
              :class="{ blank: cell.blank, checked: !cell.blank && isDateChecked(cell.date) }"
              @click="!cell.blank && toggleDate(cell.date)"
            >
              <template v-if="!cell.blank">
                <span class="day-num">{{ cell.day }}</span>
                <span v-if="isDateChecked(cell.date)" class="check-mark">✓</span>
              </template>
            </div>
          </div>
          <div class="med-calendar-summary">
            已服药 <strong>{{ medicationForm.checkedDates.length }}</strong> 天
          </div>
        </div>
      </el-form-item>
      <el-form-item label="管理方式">
        <el-select v-model="medicationForm.managementMethod" placeholder="请选择" style="width: 100%">
          <el-option v-for="item in MANAGEMENT_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="督导人员">
        <el-select v-model="medicationForm.supervisor" placeholder="请选择" style="width: 100%">
          <el-option v-for="item in SUPERVISOR_OPTIONS" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="治疗前痰菌检查">
        <el-select v-model="medicationForm.sputumResult" placeholder="请选择" style="width: 100%">
          <el-option v-for="item in SPUTUM_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="停止完成时间">
        <el-date-picker
          v-model="medicationForm.stopDate"
          type="date"
          placeholder="填写后患者将归档"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-alert v-if="medicationForm.stopDate" type="warning" :closable="false">
        填写停止完成时间后，该患者将从患者管理列表移除，放入历史患者。
      </el-alert>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button @click="printVisible = true">打印治疗记录卡</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        {{ medicationForm.stopDate ? "完成并归档" : "保存" }}
      </el-button>
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

<style lang="scss" scoped>
.med-calendar {
  width: 100%;

  &-header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    margin-bottom: 8px;
  }

  &-title {
    font-size: 16px;
    font-weight: bold;
  }

  &-weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    text-align: center;
    font-size: 13px;
    color: #909399;
    margin-bottom: 4px;
  }

  &-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
  }

  &-cell {
    aspect-ratio: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;
    position: relative;

    &.blank {
      border-color: transparent;
      cursor: default;
    }

    &.checked {
      background: #67c23a;
      border-color: #67c23a;
      color: #fff;
    }

    &:not(.blank):hover {
      border-color: #409eff;
    }

    .check-mark {
      font-size: 16px;
      font-weight: bold;
      line-height: 1;
    }

    .day-num {
      line-height: 1.2;
    }
  }

  &-summary {
    margin-top: 8px;
    text-align: center;
    font-size: 14px;
    color: #606266;
  }
}
</style>
