<script lang="ts" setup>
import PrintNotice from "@@/components/PrintNotice.vue"
import {
  applyPatientNoticeTreatmentPlan,
  DRUG_RESISTANCE_OPTIONS,
  isPatientOtherSensitivePlan,
  MOLECULAR_PATHOLOGY_RESULT_OPTIONS,
  normalizeNoticeSputumCulture,
  NOTICE_STATUS_MAP,
  noticeSputumCultureSelectOptions,
  resolvePatientTreatmentPlanForSave,
  TREATMENT_PLAN_OPTIONS
} from "@@/constants/disease"
import { formatNoticeSentTime, isPatientTransferLocked } from "@@/utils/patient"
import { validatePhone } from "@@/utils/validate"
import {
  confirmNoticeApi,
  getNoticeDetailApi,
  getNoticeDistrictLevel3UsersApi,
  getNoticeListByBizApi,
  updateNoticeContactApi,
  updateNoticeCultureResistanceApi
} from "@/pages/patient-management/apis"
import { useUserStore } from "@/pinia/stores/user"

const props = defineProps<{
  visible: boolean
  patientRow: Record<string, any> | null
  startEdit?: boolean
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
const sputumCulture = ref("")
const drugResistance = ref("")
const treatmentPlan = ref("")
const customPlanDetail = ref("")
const molecularTest = ref("")
const pathologyTest = ref("")
const phone = ref("")
const currentAddress = ref("")
const householdAddress = ref("")
const level3Users = ref<any[]>([])
const receiverUserIds = ref<string[]>([])

const canEditCulture = computed(() => {
  if (isPatientTransferLocked(props.patientRow) || props.patientRow?.archived === 1) return false
  const status = noticeDetailData.value?.status
  return status === 1 || status === 2
})

/** 编辑下拉：标准三项 + 当前历史值（若不在标准内） */
function molecularPathologySelectOptions(current: string) {
  const opts = [...MOLECULAR_PATHOLOGY_RESULT_OPTIONS]
  if (current && !opts.includes(current)) opts.push(current)
  return opts
}

function formatLevel3UserLabel(u: { realName?: string, username?: string, departmentName?: string, orgName?: string }) {
  const name = u.realName || u.username || ""
  const extra = u.departmentName || u.orgName
  return extra ? `${name}（${extra}）` : name
}

function resetEdit() {
  editing.value = false
  submitting.value = false
  sputumCulture.value = ""
  drugResistance.value = ""
  treatmentPlan.value = ""
  customPlanDetail.value = ""
  molecularTest.value = ""
  pathologyTest.value = ""
  phone.value = ""
  currentAddress.value = ""
  householdAddress.value = ""
  level3Users.value = []
  receiverUserIds.value = []
}

/** 分子/病理历史值兼容：未出结果、未知 → 无结果 */
function normalizeMolecularPathologyResult(value?: string | null) {
  const v = (value || "").trim()
  if (!v) return ""
  if (v === "未出结果" || v === "未知") return "无结果"
  return v
}

async function loadNotice() {
  if (!props.patientRow) return
  try {
    if (props.patientRow.noticeId) {
      const { data } = await getNoticeDetailApi(props.patientRow.noticeId)
      if (data) {
        noticeDetailData.value = data
        return
      }
    }
    const { data } = await getNoticeListByBizApi(props.patientRow.id, "patient")
    if (data?.length > 0) {
      noticeDetailData.value = data[0]
    } else {
      noticeDetailData.value = null
      ElMessage.info("暂无患者通知单")
      emit("update:visible", false)
    }
  } catch { /* handled */ }
}

async function beginEdit() {
  if (!noticeDetailData.value || !canEditCulture.value) return
  sputumCulture.value = normalizeNoticeSputumCulture(noticeDetailData.value.sputumCulture)
  drugResistance.value = noticeDetailData.value.drugResistance || ""
  molecularTest.value = normalizeMolecularPathologyResult(noticeDetailData.value.molecularTest)
  pathologyTest.value = normalizeMolecularPathologyResult(noticeDetailData.value.pathologyTest)
  phone.value = noticeDetailData.value.phone || ""
  currentAddress.value = noticeDetailData.value.currentAddress || ""
  householdAddress.value = noticeDetailData.value.householdAddress || ""
  const planForm = { treatmentPlan: "", customPlanDetail: "" }
  applyPatientNoticeTreatmentPlan(planForm, noticeDetailData.value.treatmentPlan, noticeDetailData.value.customPlanDetail)
  treatmentPlan.value = planForm.treatmentPlan
  customPlanDetail.value = planForm.customPlanDetail
  receiverUserIds.value = []
  editing.value = true
  try {
    const { data } = await getNoticeDistrictLevel3UsersApi(noticeDetailData.value.id)
    level3Users.value = data ?? []
  } catch {
    level3Users.value = []
  }
}

watch(
  () => props.visible,
  async (val) => {
    if (!val) {
      resetEdit()
      return
    }
    await loadNotice()
    if (props.startEdit) {
      await beginEdit()
    }
  }
)

async function handleConfirmNotice(noticeId: string) {
  try {
    await ElMessageBox.confirm("确认接收此患者通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收")
    emit("update:visible", false)
    emit("success")
  } catch { /* cancelled or handled */ }
}

async function handleSaveCulture() {
  if (!noticeDetailData.value) return
  const phoneVal = phone.value.trim()
  if (phoneVal && !validatePhone(phoneVal)) {
    ElMessage.warning("手机号格式不正确（需11位）")
    return
  }
  if (isPatientOtherSensitivePlan(treatmentPlan.value) && !customPlanDetail.value?.trim()) {
    ElMessage.warning("请填写其它敏感方案的具体方案")
    return
  }
  if (level3Users.value.length > 0 && receiverUserIds.value.length === 0) {
    ElMessage.warning("请选择本区县对应的三级用户发送通知")
    return
  }
  if (level3Users.value.length === 0) {
    try {
      await ElMessageBox.confirm("本区县暂无三级用户，将保存修改并通知原接收方，是否继续？", "提示", { type: "warning" })
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    await updateNoticeContactApi(noticeDetailData.value.id, {
      phone: phoneVal,
      currentAddress: currentAddress.value.trim(),
      householdAddress: householdAddress.value.trim()
    })
    await updateNoticeCultureResistanceApi(noticeDetailData.value.id, {
      sputumCulture: sputumCulture.value || "",
      drugResistance: drugResistance.value || "",
      treatmentPlan: resolvePatientTreatmentPlanForSave(treatmentPlan.value, customPlanDetail.value),
      molecularTest: molecularTest.value || "",
      pathologyTest: pathologyTest.value || "",
      receiverUserIds: receiverUserIds.value
    })
    ElMessage.success("已保存修改")
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
    title="患者通知单详情"
    width="700px"
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
          v-if="editing"
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
      <el-descriptions-item label="人群分类">
        {{ noticeDetailData.crowdCategory || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="现居住地址" :span="2">
        <el-input
          v-if="editing"
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
          v-if="editing"
          v-model="householdAddress"
          maxlength="200"
          clearable
          placeholder="请填写户籍地址"
        />
        <template v-else>
          {{ noticeDetailData.householdAddress || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="患者类型">
        {{ noticeDetailData.patientType || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="管理方式">
        {{ noticeDetailData.managementMethod || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="胸片检查时间">
        {{ noticeDetailData.chestXrayDate || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="胸片检查结果">
        {{ noticeDetailData.chestXrayResult || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="治疗方案">
        <template v-if="editing">
          <el-select v-model="treatmentPlan" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
          <el-input
            v-if="isPatientOtherSensitivePlan(treatmentPlan)"
            v-model="customPlanDetail"
            class="mt-2"
            type="textarea"
            :rows="2"
            placeholder="请注明详细的抗结核治疗方案"
          />
        </template>
        <template v-else>
          {{ noticeDetailData.treatmentPlan || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="耐药情况">
        <el-select
          v-if="editing"
          v-model="drugResistance"
          clearable
          placeholder="请选择"
          style="width: 100%"
        >
          <el-option v-for="item in DRUG_RESISTANCE_OPTIONS" :key="item" :label="item" :value="item" />
        </el-select>
        <template v-else>
          {{ noticeDetailData.drugResistance || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="痰涂片">
        {{ noticeDetailData.sputumSmear || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="痰培养">
        <el-select
          v-if="editing"
          v-model="sputumCulture"
          filterable
          allow-create
          default-first-option
          placeholder="请选择或输入"
          style="width: 100%"
        >
          <el-option v-for="item in noticeSputumCultureSelectOptions(sputumCulture)" :key="item" :label="item" :value="item" />
        </el-select>
        <template v-else>
          {{ normalizeNoticeSputumCulture(noticeDetailData.sputumCulture) || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="分子检查">
        <el-select
          v-if="editing"
          v-model="molecularTest"
          clearable
          placeholder="请选择"
          style="width: 100%"
        >
          <el-option
            v-for="item in molecularPathologySelectOptions(molecularTest)"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
        <template v-else>
          {{ noticeDetailData.molecularTest || "-" }}
        </template>
      </el-descriptions-item>
      <el-descriptions-item label="病理学检查">
        <el-select
          v-if="editing"
          v-model="pathologyTest"
          clearable
          placeholder="请选择"
          style="width: 100%"
        >
          <el-option
            v-for="item in molecularPathologySelectOptions(pathologyTest)"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
        <template v-else>
          {{ noticeDetailData.pathologyTest || "-" }}
        </template>
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
      <el-descriptions-item label="备注" :span="2">
        {{ noticeDetailData.remark || "-" }}
      </el-descriptions-item>
      <el-descriptions-item v-if="noticeDetailData.otherNotes" label="其他注意事项" :span="2">
        {{ noticeDetailData.otherNotes }}
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
        {{ formatNoticeSentTime(noticeDetailData.sentTime) || "-" }}
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="noticeDetailData.status === 2 ? 'success' : noticeDetailData.status === 0 ? 'info' : 'warning'" size="small">
          {{ noticeDetailData.status === 0 ? "草稿" : (NOTICE_STATUS_MAP[noticeDetailData.status] || "-") }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="noticeDetailData.confirmedTime" label="接收时间">
        {{ formatNoticeSentTime(noticeDetailData.confirmedTime) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="editing" label="通知三级用户" :span="2">
        <el-select
          v-model="receiverUserIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="请选择本区县三级用户"
          style="width: 100%"
        >
          <el-option
            v-for="u in level3Users"
            :key="u.id"
            :label="formatLevel3UserLabel(u)"
            :value="String(u.id)"
          />
        </el-select>
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
        <el-button type="primary" :loading="submitting" @click="handleSaveCulture">
          保存并通知
        </el-button>
      </template>
      <el-button
        v-else-if="noticeDetailData && canEditCulture"
        v-permission="'patientManagement:notice:fill'"
        type="warning"
        @click="beginEdit"
      >
        修改
      </el-button>
      <el-button
        v-if="noticeDetailData && noticeDetailData.status === 1 && userStore.userRole === 6 && !editing"
        v-permission="'patientManagement:notice'"
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
    notice-type="patient"
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
