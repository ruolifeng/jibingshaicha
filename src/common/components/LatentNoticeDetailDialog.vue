<script lang="ts" setup>
import PrintNotice from "@@/components/PrintNotice.vue"
import { displayInfectionJudgeResult, normalizeLatentTreatmentPlan, NOTICE_STATUS_MAP } from "@@/constants/disease"
import { confirmNoticeApi, getNoticeDetailApi, getNoticeListByBizApi } from "@/pages/latent-management/apis"
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

watch(
  () => props.visible,
  (val) => {
    if (val) loadNotice()
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
        {{ noticeDetailData.phone || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="民族">
        {{ noticeDetailData.ethnicity || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="登记号">
        {{ noticeDetailData.registrationNo || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="人群分类">
        {{ noticeDetailData.crowdCategory || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="现居住地址" :span="2">
        {{ noticeDetailData.currentAddress || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="户籍地址" :span="2">
        {{ noticeDetailData.householdAddress || "-" }}
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
      <el-button
        v-if="noticeDetailData && noticeDetailData.status === 1 && userStore.userRole === 6"
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
