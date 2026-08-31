<script lang="ts" setup>
import type { MedicationOrderStopPeriod } from "@@/utils/medicationManagement"
import type { MedicationDayMark, MedicationRecordsMap } from "@@/utils/medicationRecords"
import { isDateInOrderStopPeriods } from "@@/utils/medicationManagement"
import {
  countMedicationMarkedDays,
  formatMedicationDayMark,
  getMedicationDayMark,
  getMedicationRecordYears

} from "@@/utils/medicationRecords"
import { printElement } from "@@/utils/print"

/** 肺结核患者治疗记录卡打印组件（12个月服药情况表） */
const props = defineProps<{
  visible: boolean
  /** 患者基本信息 */
  patientData: Record<string, any> | null
  /** 服药管理表单数据 */
  medicationData: {
    managementMethod: string
    supervisor: string
    sputumResult: string
    startTreatmentDate: string
    stopDate: string
    dayMarks: MedicationRecordsMap
    orderStopPeriods?: MedicationOrderStopPeriod[]
  }
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const orderStopPeriods = computed(() => props.medicationData.orderStopPeriods || [])

const availableYears = computed(() => {
  const years = new Set(getMedicationRecordYears(props.medicationData.dayMarks))
  for (const p of orderStopPeriods.value) {
    for (const d of [p.startDate, p.endDate]) {
      if (d && /^\d{4}/.test(d)) years.add(Number(d.slice(0, 4)))
    }
  }
  if (!years.size) years.add(new Date().getFullYear())
  return Array.from(years).sort((a, b) => a - b)
})

const selectedYear = ref<number>(new Date().getFullYear())

watchEffect(() => {
  if (availableYears.value.length > 0) {
    selectedYear.value = availableYears.value[availableYears.value.length - 1]
  }
})

/** 构建 12 行 × 31 列的服药记录网格 */
const monthGrid = computed(() => {
  return Array.from({ length: 12 }, (_, i) => {
    const month = i + 1
    const daysInMonth = new Date(selectedYear.value, month, 0).getDate()
    const days = Array.from({ length: 31 }, (_, j) => {
      const day = j + 1
      if (day > daysInMonth) {
        return { day, valid: false, mark: "" as MedicationDayMark | "", stopped: false }
      }
      const dateStr = `${selectedYear.value}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`
      const stopped = isDateInOrderStopPeriods(dateStr, orderStopPeriods.value)
      return {
        day,
        valid: true,
        stopped,
        mark: stopped ? ("" as MedicationDayMark | "") : getMedicationDayMark(props.medicationData.dayMarks, dateStr)
      }
    })
    return { month, days }
  })
})

const totalCheckedDays = computed(() =>
  countMedicationMarkedDays(props.medicationData.dayMarks, selectedYear.value)
)

/** 表格下方展示的医嘱停药原因（多段用分号拼接） */
const orderStopReasonText = computed(() => {
  const lines = orderStopPeriods.value
    .map((p) => {
      const range = [p.startDate, p.endDate].filter(Boolean).join(" 至 ")
      const reason = (p.reason || "").trim()
      if (!range && !reason) return ""
      if (range && reason) return `${range}  ${reason}`
      return range || reason
    })
    .filter(Boolean)
  return lines.join("；")
})

function handlePrint() {
  printElement(
    "print-medication-content",
    "肺结核患者治疗记录卡",
    "@page { size: A4 landscape; margin: 0; } @media print { html, body { padding: 8mm 10mm; } }"
  )
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="预览 — 肺结核患者治疗记录卡"
    width="1100px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div
      v-if="availableYears.length > 1"
      class="year-selector"
    >
      <span>选择年份：</span>
      <el-radio-group v-model="selectedYear">
        <el-radio-button
          v-for="y in availableYears"
          :key="y"
          :value="y"
        >
          {{ y }}年
        </el-radio-button>
      </el-radio-group>
    </div>

    <div id="print-medication-content" class="print-area print-area--landscape">
      <h2 class="print-title">
        肺结核患者治疗记录卡
      </h2>

      <table class="info-table" border="1" cellspacing="0" cellpadding="0">
        <tbody>
          <tr>
            <th>姓名</th>
            <td>{{ patientData?.name }}</td>
            <th>性别</th>
            <td>{{ patientData?.gender }}</td>
            <th>年龄</th>
            <td>{{ patientData?.age }}</td>
            <th>年份</th>
            <td>{{ selectedYear }}年</td>
          </tr>
          <tr>
            <th>身份证号</th>
            <td colspan="3">
              {{ patientData?.idNumber }}
            </td>
            <th>联系电话</th>
            <td colspan="3">
              {{ patientData?.phone }}
            </td>
          </tr>
          <tr>
            <th>现住址</th>
            <td colspan="7">
              {{ patientData?.currentAddress || "——" }}
            </td>
          </tr>
          <tr>
            <th>开始治疗日期</th>
            <td>{{ medicationData.startTreatmentDate || "——" }}</td>
            <th>管理方式</th>
            <td>{{ medicationData.managementMethod }}</td>
            <th>督导人员</th>
            <td colspan="3">
              {{ medicationData.supervisor || "——" }}
            </td>
          </tr>
          <tr>
            <th>治疗前痰菌检查</th>
            <td colspan="3">
              {{ medicationData.sputumResult || "——" }}
            </td>
            <th>停止完成时间</th>
            <td colspan="3">
              {{ medicationData.stopDate || "——" }}
            </td>
          </tr>
        </tbody>
      </table>

      <table class="med-table" border="1" cellspacing="0" cellpadding="0">
        <thead>
          <tr>
            <th class="th-month">
              月序<br>日期
            </th>
            <th
              v-for="d in 31"
              :key="d"
              class="th-day"
            >
              {{ d }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in monthGrid"
            :key="row.month"
          >
            <td class="td-month">
              {{ row.month }}
            </td>
            <td
              v-for="(cell, idx) in row.days"
              :key="idx"
              class="td-day"
              :class="{
                'td-invalid': !cell.valid,
                'td-mark-x': cell.valid && !cell.stopped && cell.mark === 'x',
                'td-mark-circled': cell.valid && !cell.stopped && cell.mark === 'circled',
                'td-mark-stop': cell.valid && cell.stopped,
              }"
            >
              <template v-if="cell.valid && cell.stopped">
                停药
              </template>
              <template v-else-if="cell.valid && cell.mark">
                {{ formatMedicationDayMark(cell.mark) }}
              </template>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="orderStopReasonText" class="print-order-stop">
        医嘱停药原因：{{ orderStopReasonText }}
      </div>

      <div class="print-footer">
        <div>本年度累计服药天数：<strong>{{ totalCheckedDays }}</strong> 天</div>
        <div>管理人员签名：___________</div>
        <div>填表日期：___________</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button type="primary" @click="handlePrint">
        打印 / 保存PDF
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.year-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 14px;
}
</style>

<style lang="scss">
@import "@@/assets/styles/print-forms.css";
</style>
