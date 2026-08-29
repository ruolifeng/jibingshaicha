<script lang="ts" setup>
/**
 * 服药日历：点击循环标记 空白 → × → Ⓧ → 空白；医嘱停药区间显示「停药」
 */
import type { MedicationOrderStopPeriod } from "@@/utils/medicationManagement"
import type { MedicationRecordsMap } from "@@/utils/medicationRecords"
import { isDateInOrderStopPeriods } from "@@/utils/medicationManagement"
import {
  countMedicationMarkedDays,
  formatMedicationDayMark,
  getMedicationDayMark,

  toggleMedicationDayMark
} from "@@/utils/medicationRecords"

const props = withDefaults(defineProps<{
  modelValue: MedicationRecordsMap
  disabled?: boolean
  /** 医嘱停药多段：落在区间内的日期显示「停药」 */
  orderStopPeriods?: MedicationOrderStopPeriod[]
}>(), {
  disabled: false,
  orderStopPeriods: () => []
})

const emit = defineEmits<{
  (e: "update:modelValue", v: MedicationRecordsMap): void
}>()

const calendarMonth = ref(new Date())

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

const markedDays = computed(() => countMedicationMarkedDays(props.modelValue))

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

function isOrderStop(dateStr: string) {
  return isDateInOrderStopPeriods(dateStr, props.orderStopPeriods)
}

function handleToggle(dateStr: string) {
  if (props.disabled) return
  // 医嘱停药日不允许手动改服药标记，避免与停药冲突
  if (isOrderStop(dateStr)) return
  emit("update:modelValue", toggleMedicationDayMark(props.modelValue, dateStr))
}

function cellMark(dateStr: string) {
  return getMedicationDayMark(props.modelValue, dateStr)
}
</script>

<template>
  <div class="med-calendar">
    <div class="med-calendar-header">
      <el-button text @click="prevMonth">
        &lt;
      </el-button>
      <span class="med-calendar-title">{{ calendarTitle }}</span>
      <el-button text @click="nextMonth">
        &gt;
      </el-button>
    </div>
    <div class="med-calendar-weekdays">
      <span v-for="w in ['日', '一', '二', '三', '四', '五', '六']" :key="w">{{ w }}</span>
    </div>
    <div class="med-calendar-grid">
      <div
        v-for="(cell, idx) in calendarDays"
        :key="idx"
        class="med-calendar-cell"
        :class="{
          'blank': cell.blank,
          'readonly': disabled || (!cell.blank && isOrderStop(cell.date)),
          'mark-x': !cell.blank && !isOrderStop(cell.date) && cellMark(cell.date) === 'x',
          'mark-circled': !cell.blank && !isOrderStop(cell.date) && cellMark(cell.date) === 'circled',
          'mark-stop': !cell.blank && isOrderStop(cell.date),
        }"
        @click="!cell.blank && handleToggle(cell.date)"
      >
        <template v-if="!cell.blank">
          <span class="day-num">{{ cell.day }}</span>
          <span v-if="isOrderStop(cell.date)" class="mark-stop-text">
            停药
          </span>
          <span v-else-if="cellMark(cell.date)" class="mark-symbol">
            {{ formatMedicationDayMark(cellMark(cell.date)) }}
          </span>
        </template>
      </div>
    </div>
    <div class="med-calendar-summary">
      已服药 <strong>{{ markedDays }}</strong> 天
      <span v-if="!disabled" class="med-calendar-hint">（点击循环：× → Ⓧ → 空白；仅 Ⓧ 计入已服药）</span>
    </div>
  </div>
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
    transition: border-color 0.2s;
    position: relative;
    min-height: 44px;

    &.blank {
      border-color: transparent;
      cursor: default;
    }

    &.readonly {
      cursor: default;

      &:not(.blank):hover {
        border-color: #e4e7ed;
      }
    }

    &:not(.blank):not(.readonly):hover {
      border-color: #409eff;
    }

    .day-num {
      font-size: 11px;
      line-height: 1.2;
      color: #909399;
    }

    .mark-symbol {
      font-size: 18px;
      font-weight: bold;
      line-height: 1;
      color: #303133;
    }

    &.mark-circled .mark-symbol {
      font-size: 20px;
    }

    &.mark-stop {
      background: #fdf6ec;
      border-color: #f5dab1;
    }

    .mark-stop-text {
      font-size: 12px;
      font-weight: 600;
      line-height: 1.2;
      color: #e6a23c;
    }
  }

  &-summary {
    margin-top: 8px;
    text-align: center;
    font-size: 14px;
    color: #606266;
  }

  &-hint {
    margin-left: 8px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
