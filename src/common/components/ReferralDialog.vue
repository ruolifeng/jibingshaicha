<script lang="ts" setup>
import { getLevel5UsersApi } from "@@/apis/users"
import {
  sendReferralApi,
  getReferralListApi,
  resendReferralApi,
  type ReferralRecord
} from "@@/apis/referral"

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
const POPULATION_LABEL: Record<string, string> = {
  school: "学校人群",
  key: "重点人群",
  close: "密接人群"
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

// ====== 接收方用户列表 ======
const receiverUsers = ref<{ id: number; realName: string; username: string; orgName: string }[]>([])

async function loadReceiverUsers() {
  try {
    const { data } = await getLevel5UsersApi()
    receiverUsers.value = data
  } catch { /* ignored */ }
}

// ====== 发起推送表单 ======
const sendForm = reactive({ receiverOrgId: undefined as number | undefined })
const sending = ref(false)

async function handleSend() {
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
      receiverOrgId: sendForm.receiverOrgId
    })
    ElMessage.success("分级诊疗推送已发送")
    sendForm.receiverOrgId = undefined
    await loadHistory()
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
    ElMessage.success("已重新发起分级诊疗")
    await loadHistory()
  } catch { /* handled */ }
}

// 打开弹窗时加载数据
watch(visible, (val: boolean) => {
  if (val) {
    loadReceiverUsers()
    loadHistory()
  }
})
</script>

<template>
  <el-dialog v-model="visible" title="分级诊疗" width="700px" append-to-body>
    <!-- 业务信息摘要 -->
    <el-descriptions :column="2" border size="small" class="mb-4">
      <el-descriptions-item label="对象姓名">{{ subjectName }}</el-descriptions-item>
      <el-descriptions-item label="人群类型">{{ POPULATION_LABEL[populationType] || populationType }}</el-descriptions-item>
      <el-descriptions-item label="当前模块">{{ MODULE_LABEL[moduleType] || moduleType }}</el-descriptions-item>
    </el-descriptions>

    <!-- 发起推送 -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="font-semibold">发起分级诊疗推送</span>
      </template>
      <el-form :model="sendForm" label-width="100px" size="small">
        <el-form-item label="接收部门" required>
          <el-select
            v-model="sendForm.receiverOrgId"
            placeholder="请选择接收部门"
            filterable
            style="width: 280px"
          >
            <el-option
              v-for="u in receiverUsers"
              :key="u.id"
              :value="u.id"
              :label="`${u.orgName}（${u.realName || u.username}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="sending" @click="handleSend">发起推送</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 历史推送记录 -->
    <el-card shadow="never">
      <template #header>
        <span class="font-semibold">推送记录</span>
      </template>
      <el-table v-loading="historyLoading" :data="historyList" border size="small">
        <el-table-column prop="sentTime" label="推送时间" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_TYPE[row.status]" size="small">
              {{ STATUS_LABEL[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confirmedTime" label="接收时间" width="170">
          <template #default="{ row }">{{ row.confirmedTime || "—" }}</template>
        </el-table-column>
        <el-table-column prop="rejectedTime" label="拒绝时间" width="170">
          <template #default="{ row }">{{ row.rejectedTime || "—" }}</template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="120">
          <template #default="{ row }">{{ row.rejectReason || "—" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
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
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>
