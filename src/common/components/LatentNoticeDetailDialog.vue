<script lang="ts" setup>
import PrintNotice from "@@/components/PrintNotice.vue"
import { displayInfectionJudgeResult, normalizeLatentTreatmentPlan, NOTICE_STATUS_MAP } from "@@/constants/disease"
import { isLatentTransferLocked } from "@@/utils/latent"
import { validatePhone } from "@@/utils/validate"
import {
  confirmNoticeApi,
  getNoticeDetailApi,
  getNoticeListByBizApi,
  updateNoticeContactApi,
  updateNoticeRegistrationNoApi
} from "@/pages/latent-management/apis"
import { useUserStore } from "@/pinia/stores/user"

const props = defineProps<{
  visible: boolean
  latentRow: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
  (e: "success"): void
}>()

const userStore = useUserStore()
const noticeDetailData = ref<Record<string, any> | null>(null)
const printVisible = ref(false)
const editing = ref(false)
const submitting = ref(false)
const registrationNo = ref("")
const phone = ref("")
const currentAddress = ref("")
const householdAddress = ref("")

/** 待确认/已确认且未转出：可改联系电话与地址 */
const canEditContact = computed(() => {
  if (!noticeDetailData.value || isLatentTransferLocked(props.latentRow) || props.latentRow?.archived === 1) return false
  const status = noticeDetailData.value.status
  return status === 1 || status === 2
})

/** 三级(role=4)、四级(role=5)；超管放行 */
const canEditRegistrationNo = computed(() => {
  if (!canEditContact.value) return false
  const role = userStore.userRole
  return role === 1 || role === 4 || role === 5
})

const canEdit = computed(() => canEditContact.value || canEditRegistrationNo.value)

function resetEdit() {
  editing.value = false
  submitting.value = false
  registrationNo.value = ""
  phone.value = ""
  currentAddress.value = ""
  householdAddress.value = ""
}

async function loadNotice() {
  if (!props.latentRow) return
  try {
    // 优先按通知单 ID 查详情（与系统消息一致）；否则按业务 ID 列表取最新一条
    if (props.latentRow.noticeId) {
      const { data } = await getNoticeDetailApi(props.latentRow.noticeId)
      if (data) {
        noticeDetailData.value = data
        return
      }
    }
    const { data } = await getNoticeListByBizApi(props.latentRow.id, "latent")
    if (data?.length > 0) {
      noticeDetailData.value = data[0]
    } else {
      noticeDetailData.value = null
      ElMessage.info("暂无通知单记录")
      emit("update:visible", false)
    }
  } catch { /* handled */ }
}

function beginEdit() {
  if (!noticeDetailData.value || !canEdit.value) return
  registrationNo.value = noticeDetailData.value.registrationNo || ""
  phone.value = noticeDetailData.value.phone || ""
  currentAddress.value = noticeDetailData.value.currentAddress || ""
  householdAddress.value = noticeDetailData.value.householdAddress || ""
  editing.value = true
}

watch(
  () => props.visible,
  (val) => {
    if (!val) {
      resetEdit()
      return
    }
    loadNotice()
  }
)

async function handleConfirmNotice(noticeId: string) {
  try {
    await ElMessageBox.confirm("确认接收此通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收")
    emit("update:visible", false)
    emit("success")
  } catch { /* cancelled or handled */ }
}

async function handleSaveEdit() {
  if (!noticeDetailData.value) return
  const phoneVal = phone.value.trim()
  if (phoneVal && !validatePhone(phoneVal)) {
    ElMessage.warning("手机号格式不正确（需11位）")
    return
  }
  submitting.value = true
  try {
    if (canEditContact.value) {
      await updateNoticeContactApi(noticeDetailData.value.id, {
        phone: phoneVal,
        currentAddress: currentAddress.value.trim(),
        householdAddress: householdAddress.value.trim()
      })
    }
    if (canEditRegistrationNo.value) {
      await updateNoticeRegistrationNoApi(noticeDetailData.value.id, registrationNo.value.trim())
    }
    ElMessage.success("已保存并同步")
    editing.value = false
    await loadNotice()
    emit("success")
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="潜伏感染者通知单详情"
    width="680px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-descriptions v-if="noticeDetailData" :column="2" border>
      <el-descriptions-item label="姓名">
        {{ noticeDetailData.patientName }}
      </el-descriptions-item>
      <el-descriptions-item label="身份证">
        {{ noticeDetailData.idNumber }}
      </el-descriptions-item>
      <el-descriptions-item label="性别">
        {{ noticeDetailData.gender }}
      </el-descriptions-item>
      <el-descriptions-item label="年龄">
        {{ noticeDetailData.age }}
      </el-descriptions-item>
      <el-descriptions-item label="联系方式">
        <el-input
          v-if="editing && canEditContact"
          v-model="phone"
          maxlength="11"
          clearable
          placeholder="请填写联系电话"
        />
        <template v-else>
          {{ noticeDetailData.phone || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="民族">
        {{ noticeDetailData.ethnicity || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="登记号">
        <el-input
          v-if="editing && canEditRegistrationNo"
          v-model="registrationNo"
          maxlength="64"
          clearable
          placeholder="请填写登记号"
        />
        <template v-else>
          {{ noticeDetailData.registrationNo || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="人群分类">
        {{ noticeDetailData.crowdCategory || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="现居住地址" :span="2">
        <el-input
          v-if="editing && canEditContact"
          v-model="currentAddress"
          maxlength="200"
          clearable
          placeholder="请填写现居住地址"
        />
        <template v-else>
          {{ noticeDetailData.currentAddress || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="户籍地址" :span="2">
        <el-input
          v-if="editing && canEditContact"
          v-model="householdAddress"
          maxlength="200"
          clearable
          placeholder="请填写户籍地址"
        />
        <template v-else>
          {{ noticeDetailData.householdAddress || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="感染检测时间">
        {{ noticeDetailData.infectionDate || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="感染检查方法">
        {{ noticeDetailData.infectionMethod || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="感染检查结果" :span="2">
        {{ displayInfectionJudgeResult(noticeDetailData.infectionResultValue) }}
      </el-descriptions-item>
      <el-descriptions-item label="胸片检查时间">
        {{ noticeDetailData.chestXrayDate || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="胸片检查结果">
        {{ noticeDetailData.chestXrayResult || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="治疗方案" :span="2">
        {{ normalizeLatentTreatmentPlan(noticeDetailData.treatmentPlan) || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="治疗机构">
        {{ noticeDetailData.treatmentInstitution || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="服药管理单位">
        {{ noticeDetailData.medicationManagementUnit || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="下发时间">
        {{ noticeDetailData.issuedTime || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="下发人">
        <span class="party-inline">
          {{ noticeDetailData.senderName || "-" }}<template v-if="noticeDetailData.senderOrgName">（{{ noticeDetailData.senderOrgName }}）</template>
        </span>
      </el-descriptions-item>
      <el-descriptions-item label="接收人">
        <span class="party-inline">
          {{ noticeDetailData.receiverName || "-" }}<template v-if="noticeDetailData.receiverOrgName">（{{ noticeDetailData.receiverOrgName }}）</template>
        </span>
      </el-descriptions-item>
      <el-descriptions-item label="发送时间">
        {{ noticeDetailData.sentTime }}
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="noticeDetailData.status === 2 ? 'success' : 'warning'" size="small">
          {{ NOTICE_STATUS_MAP[noticeDetailData.status] }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="noticeDetailData.confirmedTime" label="接收时间">
        {{ noticeDetailData.confirmedTime }}
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button v-if="noticeDetailData" @click="printVisible = true">
        打印预览
      </el-button>
      <template v-if="editing">
        <el-button @click="resetEdit">
          取消修改
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveEdit">
          保存
        </el-button>
      </template>
      <el-button
        v-else-if="noticeDetailData && canEdit"
        v-permission="'latentManagement:notice'"
        type="warning"
        @click="beginEdit"
      >
        修改
      </el-button>
      <el-button
        v-if="noticeDetailData && noticeDetailData.status === 1 && userStore.userRole === 6 && !editing"
        v-permission="'latentManagement:notice'"
        type="primary"
        @click="handleConfirmNotice(noticeDetailData.id)"
      >
        确认接收
      </el-button>
    </template>
  </el-dialog>

  <PrintNotice
    v-if="noticeDetailData"
    :visible="printVisible"
    notice-type="latent"
    :notice-data="noticeDetailData"
    @update:visible="printVisible = $event"
  />
</template>

<style scoped>
.party-inline {
  word-break: keep-all;
  overflow-wrap: break-word;
}
</style>
