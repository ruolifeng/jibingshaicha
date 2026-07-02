<script lang="ts" setup>
import type { InputInstance } from "element-plus"
import { REFERRAL_TRACKING_DIAGNOSIS_OPTIONS } from "@@/constants/disease"
import { saveDiagnosisApi } from "@/pages/referral-management/apis"

const props = defineProps<{
  visible: boolean
  recordId?: number | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const diagnosisResult = ref("")
const diagnosisRemark = ref("")
const remarkInputRef = ref<InputInstance>()
const saving = ref(false)

watch(() => props.visible, (val) => {
  if (!val) return
  diagnosisResult.value = ""
  diagnosisRemark.value = ""
})

watch(diagnosisResult, async (val) => {
  if (val !== "其他") {
    diagnosisRemark.value = ""
    return
  }
  await nextTick()
  remarkInputRef.value?.focus()
})

function close() {
  emit("update:visible", false)
}

async function handleSave() {
  if (!props.recordId || saving.value) return
  if (!diagnosisResult.value) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  if (diagnosisResult.value === "其他" && !diagnosisRemark.value.trim()) {
    ElMessage.warning("选择其他时请填写备注")
    return
  }

  saving.value = true
  try {
    await saveDiagnosisApi(
      props.recordId,
      diagnosisResult.value,
      diagnosisRemark.value.trim() || undefined
    )
    ElMessage.success(
      diagnosisResult.value === "确诊患者"
        ? "诊断结果已保存，该记录已标红结案"
        : "诊断结果已保存"
    )
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
    title="录入诊断结果"
    width="480px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form label-width="100px">
      <el-form-item label="诊断结果">
        <el-radio-group v-model="diagnosisResult">
          <el-radio
            v-for="item in REFERRAL_TRACKING_DIAGNOSIS_OPTIONS"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="diagnosisResult === '其他'" label="备注" required>
        <el-input
          ref="remarkInputRef"
          v-model="diagnosisRemark"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入其他诊断结果说明"
        />
      </el-form-item>
      <el-alert
        v-if="diagnosisResult === '确诊患者'"
        title="确诊患者将标红结案，不进入【患者管理】模块"
        type="warning"
        :closable="false"
        style="margin-top: 8px"
      />
      <el-alert
        v-if="diagnosisResult === '潜伏感染者'"
        title="潜伏感染者将自动进入【潜伏感染者管理】模块（populationType=referral）"
        type="info"
        :closable="false"
        style="margin-top: 8px"
      />
    </el-form>
    <template #footer>
      <el-button @click="close">
        取消
      </el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        确认诊断
      </el-button>
    </template>
  </el-dialog>
</template>
