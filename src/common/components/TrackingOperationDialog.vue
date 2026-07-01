<script lang="ts" setup>
import TrackingHistoryPanel from "@@/components/TrackingHistoryPanel.vue"
import { parseTrackingHistory } from "@@/utils/referralTracking"
import dayjs from "dayjs"

export interface TrackConfirmPayload {
  status: number
  remark: string
  actualArrivalDate?: string
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  historyJson?: string
  notInPlaceCount?: number
  loading?: boolean
  forceEndThreshold?: number
}>(), {
  notInPlaceCount: 0,
  loading: false,
  forceEndThreshold: 3
})

const emit = defineEmits<{
  (e: "update:modelValue", val: boolean): void
  (e: "confirm", payload: TrackConfirmPayload): void
}>()

const hasHistory = computed(() => parseTrackingHistory(props.historyJson).length > 0)
const status = ref<number | undefined>(undefined)
const remark = ref("")
const actualArrivalDate = ref("")

const nextAttemptNo = computed(() => {
  try {
    const history = props.historyJson ? JSON.parse(props.historyJson) : []
    return (Array.isArray(history) ? history.length : 0) + 1
  } catch {
    return 1
  }
})

function resetForm() {
  status.value = undefined
  remark.value = ""
  actualArrivalDate.value = dayjs().format("YYYY-MM-DD")
}

watch(() => props.modelValue, (val) => {
  if (val) resetForm()
})

function handleConfirm() {
  if (!status.value) {
    ElMessage.warning("请选择追踪状态")
    return
  }
  if (!remark.value.trim()) {
    ElMessage.warning("请填写追踪备注")
    return
  }
  if (status.value === 1 && !actualArrivalDate.value) {
    ElMessage.warning("请选择到位时间")
    return
  }
  emit("confirm", {
    status: status.value,
    remark: remark.value.trim(),
    actualArrivalDate: status.value === 1 ? actualArrivalDate.value : undefined
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="追踪操作"
    width="520px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-width="100px">
      <el-form-item v-if="hasHistory" label="追踪记录">
        <TrackingHistoryPanel :history-json="historyJson" />
      </el-form-item>
      <el-form-item label="追踪状态">
        <el-radio-group v-model="status">
          <el-radio :value="1">
            到位
          </el-radio>
          <el-radio :value="2">
            未到位
          </el-radio>
          <el-radio :value="3">
            其他
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="status === 1" label="到位时间" required>
        <el-date-picker
          v-model="actualArrivalDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择患者真实到位日期"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注" required>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="3"
          placeholder="请填写本次追踪备注"
        />
      </el-form-item>
      <el-alert
        v-if="status === 2"
        :title="`第 ${nextAttemptNo} 次追踪，当前已未到位 ${notInPlaceCount} 次，${forceEndThreshold} 次未到位将自动结束追踪`"
        type="warning"
        :closable="false"
        show-icon
      />
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        取消
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>
