<script lang="ts" setup>
import type { ReferralRecord } from "@@/apis/referral"
import {
  getReferralListApi,

  resendReferralApi,
  sendReferralApi
} from "@@/apis/referral"
import DepartmentUserSelect from "@@/components/DepartmentUserSelect.vue"
import { getPopulationTypeLabel } from "@@/constants/disease"
import { formatReferralDisplay } from "@@/utils/referralTracking"

interface Props {
  /** 是否显示弹窗 */
  modelValue: boolean
  /** 业务记录ID */
  bizId: number
  /** 业务类型，格式：{module}_{populationType}，如 screening_school */
  bizType: string
  /** 人群类型：school / key / close */
  populationType: string
  /** 模块类型：screening / suspected / latent / patient */
  moduleType: string
  /** 对象姓名 */
  subjectName: string
  /** 业务摘要（可选，JSON 字符串） */
  summary?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: "update:modelValue", val: boolean): void
  (e: "success"): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit("update:modelValue", val)
})

const MODULE_LABEL: Record<string, string> = {
  screening: "筛查管理",
  suspected: "疑似结核管理",
  latent: "潜伏感染者管理",
  patient: "患者管理"
}
/** 筛查/潜伏等旧模块使用的短码 */
const LEGACY_POPULATION_LABEL: Record<string, string> = {
  school: "学校人群",
  key: "重点人群",
  close: "密接人群",
  keyPopulation: "重点人群",
  closeContact: "密接人群"
}
const STATUS_LABEL: Record<number, string> = {
  1: "待确认",
  2: "已接收",
  3: "已拒绝"
}
const STATUS_TAG_TYPE: Record<number, "warning" | "success" | "danger"> = {
  1: "warning",
  2: "success",
  3: "danger"
}

const actionLabel = "转出"
const populationLabel = computed(() =>
  LEGACY_POPULATION_LABEL[props.populationType] ?? getPopulationTypeLabel(props.populationType)
)

// ====== 发起推送表单 ======
const sendForm = reactive({
  receiverOrgId: undefined as number | undefined,
  referralReason: ""
})
const sending = ref(false)

const hasPendingReferral = computed(() => historyList.value.some(r => r.status === 1))
const cannotSend = computed(() => hasPendingReferral.value || sending.value)

async function handleSend() {
  if (hasPendingReferral.value) {
    ElMessage.warning("已有待确认的转出申请，请等待接收方处理或拒绝后再发起")
    return
  }
  if (!sendForm.receiverOrgId) {
    ElMessage.warning("请选择接收部门")
    return
  }
  sending.value = true
  try {
    await sendReferralApi({
      bizId: props.bizId,
      bizType: props.bizType,
      populationType: props.populationType,
      moduleType: props.moduleType,
      subjectName: props.subjectName,
      summary: props.summary,
      referralReason: sendForm.referralReason.trim() || undefined,
      receiverOrgId: sendForm.receiverOrgId
    })
    ElMessage.success(`${actionLabel}推送已发送`)
    sendForm.receiverOrgId = undefined
    sendForm.referralReason = ""
    await loadHistory()
    emit("success")
  } finally {
    sending.value = false
  }
}

// ====== 历史推送记录 ======
const historyList = ref<ReferralRecord[]>([])
const historyLoading = ref(false)

async function loadHistory() {
  historyLoading.value = true
  try {
    const { data } = await getReferralListApi(props.bizId, props.bizType)
    historyList.value = data
  } finally {
    historyLoading.value = false
  }
}

// ====== 重新发起 ======
async function handleResend(record: ReferralRecord) {
  try {
    await resendReferralApi(record.id)
    ElMessage.success(`已重新发起${actionLabel}`)
    await loadHistory()
    emit("success")
  } catch { /* handled */ }
}

// 打开弹窗时加载数据
watch(visible, (val: boolean) => {
  if (val) {
    loadHistory()
  }
})
</script>

<template>
  <el-dialog v-model="visible" :title="actionLabel" width="700px" append-to-body>
    <!-- 业务信息摘要 -->
    <el-descriptions :column="2" border size="small" class="mb-4">
      <el-descriptions-item label="对象姓名">
        {{ subjectName }}
      </el-descriptions-item>
      <el-descriptions-item label="人群类型">
        {{ populationLabel }}
      </el-descriptions-item>
      <el-descriptions-item label="当前模块">
        {{ MODULE_LABEL[moduleType] || moduleType }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 发起推送 -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="font-semibold">发起{{ actionLabel }}推送</span>
      </template>
      <el-form :model="sendForm" label-width="100px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="接收部门" required>
              <DepartmentUserSelect
                v-model="sendForm.receiverOrgId"
                :active="visible"
                placeholder="请选择部门下的接收用户"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="`${actionLabel}原因`">
              <el-input
                v-model="sendForm.referralReason"
                :placeholder="`请填写${actionLabel}原因`"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="sending" :disabled="cannotSend" @click="handleSend">
            发起推送
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 历史推送记录 -->
    <el-card shadow="never">
      <template #header>
        <span class="font-semibold">推送记录</span>
      </template>
      <el-table v-loading="historyLoading" :data="historyList" border size="small">
        <el-table-column prop="sentTime" label="推送时间" />
        <el-table-column prop="referralReason" :label="`${actionLabel}原因`" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.referralReason || "—" }}
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_TYPE[row.status]" size="small">
              {{ STATUS_LABEL[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接收时间">
          <template #default="{ row }">
            {{ formatReferralDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="rejectedTime" label="拒绝时间">
          <template #default="{ row }">
            {{ row.rejectedTime || "—" }}
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因">
          <template #default="{ row }">
            {{ row.rejectReason || "—" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 3"
              type="primary"
              size="small"
              link
              @click="handleResend(row)"
            >
              重新发起
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <template #footer>
      <el-button @click="visible = false">
        关闭
      </el-button>
    </template>
  </el-dialog>
</template>
