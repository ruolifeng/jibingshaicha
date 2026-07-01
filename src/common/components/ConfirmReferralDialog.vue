<script lang="ts" setup>
import dayjs from "dayjs"

const props = defineProps<{
  modelValue: boolean
  subjectName?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: "update:modelValue", val: boolean): void
  (e: "confirm", actualReferralDate: string): void
}>()

const actualReferralDate = ref("")

watch(() => props.modelValue, (val) => {
  if (val) {
    actualReferralDate.value = dayjs().format("YYYY-MM-DD")
  }
})

function handleConfirm() {
  if (!actualReferralDate.value) {
    ElMessage.warning("请选择转诊时间")
    return
  }
  emit("confirm", actualReferralDate.value)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="确认接收转出"
    width="460px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-width="100px">
      <el-form-item v-if="subjectName" label="对象姓名">
        <span>{{ subjectName }}</span>
      </el-form-item>
      <el-form-item label="转诊时间" required>
        <el-date-picker
          v-model="actualReferralDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择患者真实转诊日期"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        取消
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        确认接收
      </el-button>
    </template>
  </el-dialog>
</template>
