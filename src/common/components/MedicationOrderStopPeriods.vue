<script lang="ts" setup>
/**
 * 医嘱停药：可多段填写起止时间与原因
 */
import type { MedicationOrderStopPeriod } from "@@/utils/medicationManagement"
import { createEmptyOrderStopPeriod } from "@@/utils/medicationManagement"

const props = withDefaults(defineProps<{
  modelValue: MedicationOrderStopPeriod[]
  disabled?: boolean
}>(), {
  disabled: false
})

const emit = defineEmits<{
  (e: "update:modelValue", v: MedicationOrderStopPeriod[]): void
}>()

function rangeOf(row: MedicationOrderStopPeriod): [string, string] | "" {
  if (row.startDate && row.endDate) return [row.startDate, row.endDate]
  return ""
}

function onRangeChange(index: number, value: [string, string] | null | undefined) {
  const next = props.modelValue.map((item, i) => {
    if (i !== index) return item
    if (!value || value.length < 2) {
      return { ...item, startDate: "", endDate: "" }
    }
    return { ...item, startDate: value[0] || "", endDate: value[1] || "" }
  })
  emit("update:modelValue", next)
}

function onReasonChange(index: number, reason: string) {
  const next = props.modelValue.map((item, i) =>
    i === index ? { ...item, reason } : item
  )
  emit("update:modelValue", next)
}

function addRow() {
  emit("update:modelValue", [...props.modelValue, createEmptyOrderStopPeriod()])
}

function removeRow(index: number) {
  const next = props.modelValue.filter((_, i) => i !== index)
  emit("update:modelValue", next)
}
</script>

<template>
  <div class="order-stop-periods">
    <div
      v-for="(row, index) in modelValue"
      :key="index"
      class="order-stop-row"
    >
      <div class="order-stop-field">
        <span class="order-stop-label">医嘱停药时间</span>
        <el-date-picker
          :model-value="rangeOf(row)"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :disabled="disabled"
          style="width: 260px"
          @update:model-value="(v: any) => onRangeChange(index, v)"
        />
      </div>
      <div class="order-stop-field reason-field">
        <span class="order-stop-label">医嘱停药原因</span>
        <el-input
          :model-value="row.reason"
          placeholder="请填写停药原因"
          :disabled="disabled"
          clearable
          @update:model-value="(v: string) => onReasonChange(index, v)"
        />
      </div>
      <el-button
        v-if="!disabled"
        type="danger"
        link
        @click="removeRow(index)"
      >
        删除
      </el-button>
    </div>
    <el-button
      v-if="!disabled"
      type="primary"
      link
      @click="addRow"
    >
      + 添加医嘱停药
    </el-button>
    <div v-else-if="!modelValue.length" class="order-stop-empty">
      暂无医嘱停药记录
    </div>
  </div>
</template>

<style lang="scss" scoped>
.order-stop-periods {
  width: 100%;
}

.order-stop-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 10px;
}

.order-stop-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reason-field {
  flex: 1;
  min-width: 220px;
}

.order-stop-label {
  flex-shrink: 0;
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.order-stop-empty {
  color: #909399;
  font-size: 13px;
}
</style>
