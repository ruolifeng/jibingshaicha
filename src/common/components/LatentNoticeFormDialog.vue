<script lang="ts" setup>
import { getLevel5UsersApi } from "@@/apis/users"
import {
  CHEST_XRAY_RESULT_OPTIONS,
  CROWD_CATEGORY_OPTIONS,
  formatLatentNoticeTreatmentPlan,
  INFECTION_METHOD_OPTIONS,
  isLatentIndividualPlan,
  LATENT_TREATMENT_PLAN_OPTIONS,
  parseLatentNoticeTreatmentPlan
} from "@@/constants/disease"
import { idCardRule, phoneRule } from "@@/utils/validate"
import { getNoticeListByBizApi, saveNoticeDraftApi, sendNoticeApi } from "@/pages/latent-management/apis"
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
const level5Users = ref<any[]>([])
const submitting = ref(false)
const noticeFormRef = ref()

const noticeFormRules = {
  receiverOrgId: [{ required: true, message: "请选择接收单位", trigger: "change" }],
  idNumber: [idCardRule()],
  phone: [phoneRule()]
}

const noticeForm = reactive({
  idNumber: "",
  gender: "",
  birthDate: "",
  age: null as number | null,
  ethnicity: "",
  phone: "",
  crowdCategory: "",
  currentAddress: "",
  householdAddress: "",
  infectionDate: "",
  infectionMethod: "",
  infectionResultValue: "",
  chestXrayDate: "",
  chestXrayResult: "",
  treatmentPlan: "",
  customPlanDetail: "",
  treatmentInstitution: "",
  issuedTime: "",
  receiverOrgId: undefined as number | undefined
})

function getNowDateStr() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, "0")
  const day = String(now.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function resetFormFromRow(row: Record<string, any>) {
  const parsedPlan = parseLatentNoticeTreatmentPlan(row.preventivePlan || "")
  Object.assign(noticeForm, {
    idNumber: row.idNumber || "",
    gender: row.gender || "",
    birthDate: row.birthDate || "",
    age: row.age ?? null,
    phone: row.phone || "",
    ethnicity: row.ethnicity || "",
    crowdCategory: row.crowdCategory || "",
    currentAddress: row.currentAddress || "",
    householdAddress: row.householdAddress || "",
    infectionDate: row.screenDate || row.infectionDate || "",
    infectionMethod: row.screenMethod || row.infectionMethod || "",
    infectionResultValue: row.screenResult || row.infectionResult || row.infectionResultValue || "",
    chestXrayDate: row.chestXrayDate || "",
    chestXrayResult: row.chestXrayResult || "",
    treatmentPlan: parsedPlan.treatmentPlan,
    customPlanDetail: parsedPlan.customPlanDetail,
    treatmentInstitution: "",
    issuedTime: getNowDateStr(),
    receiverOrgId: userStore.userRole === 6 ? userStore.userId : undefined
  })
}

function assignFormFromNotice(notice: Record<string, any>, row: Record<string, any>) {
  Object.assign(noticeForm, {
    idNumber: notice.idNumber || row.idNumber || "",
    gender: notice.gender || row.gender || "",
    birthDate: notice.birthDate || row.birthDate || "",
    age: notice.age ?? row.age ?? null,
    phone: notice.phone || row.phone || "",
    ethnicity: notice.ethnicity || row.ethnicity || "",
    crowdCategory: notice.crowdCategory || row.crowdCategory || "",
    currentAddress: notice.currentAddress || row.currentAddress || "",
    householdAddress: notice.householdAddress || row.householdAddress || "",
    infectionDate: notice.infectionDate || row.screenDate || "",
    infectionMethod: notice.infectionMethod || row.screenMethod || "",
    infectionResultValue: notice.infectionResultValue || row.infectionResult || "",
    chestXrayDate: notice.chestXrayDate || row.chestXrayDate || "",
    chestXrayResult: notice.chestXrayResult || row.chestXrayResult || "",
    treatmentInstitution: notice.treatmentInstitution || "",
    issuedTime: notice.issuedTime || getNowDateStr(),
    receiverOrgId: notice.receiverOrgId || undefined
  })
  const parsed = parseLatentNoticeTreatmentPlan(
    notice.treatmentPlan || row.preventivePlan,
    notice.customPlanDetail
  )
  noticeForm.treatmentPlan = parsed.treatmentPlan
  noticeForm.customPlanDetail = parsed.customPlanDetail
}

async function loadDraftIfNeeded(row: Record<string, any>) {
  if (row.noticeSent) {
    resetFormFromRow(row)
    return
  }
  try {
    const { data } = await getNoticeListByBizApi(row.id, "latent")
    const notice = data?.[0]
    if (notice && (notice.status === 0 || notice.status === 2)) {
      assignFormFromNotice(notice, row)
      return
    }
  } catch { /* ignore */ }
  resetFormFromRow(row)
}

async function loadLevel5Users() {
  try {
    const { data } = await getLevel5UsersApi()
    level5Users.value = data || []
  } catch { /* handled */ }
}

watch(
  () => props.visible,
  async (val) => {
    if (val && props.latentRow) {
      await loadDraftIfNeeded(props.latentRow)
    }
  }
)

onMounted(loadLevel5Users)

function buildPayload() {
  const row = props.latentRow!
  return {
    noticeType: "latent",
    populationType: row.populationType,
    bizId: row.id,
    patientName: row.name,
    ...noticeForm,
    treatmentPlan: formatLatentNoticeTreatmentPlan(noticeForm.treatmentPlan, noticeForm.customPlanDetail),
    senderId: userStore.userId
  }
}

function close() {
  emit("update:visible", false)
}

async function handleSendNotice() {
  if (submitting.value) return
  try {
    await noticeFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    noticeForm.issuedTime = getNowDateStr()
    await sendNoticeApi(buildPayload())
    ElMessage.success("通知单发送成功")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

async function handleSaveDraft() {
  if (submitting.value) return
  submitting.value = true
  try {
    await saveNoticeDraftApi(buildPayload())
    ElMessage.success("通知单草稿已保存")
    close()
    emit("success")
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="填写潜伏感染者通知单"
    width="680px"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="noticeFormRef" :model="noticeForm" :rules="noticeFormRules" label-width="110px">
      <el-divider content-position="left">
        基本信息
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="姓名">
            <el-input :model-value="latentRow?.name" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="身份证" prop="idNumber">
            <el-input v-model="noticeForm.idNumber" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="性别">
            <el-select v-model="noticeForm.gender" style="width: 100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="年龄">
            <el-input-number v-model="noticeForm.age" :min="0" :max="150" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="出生日期">
            <el-date-picker v-model="noticeForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="联系方式" prop="phone">
            <el-input v-model="noticeForm.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="民族">
            <el-input v-model="noticeForm.ethnicity" placeholder="如：汉族" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="24">
          <el-form-item label="人群分类">
            <el-select v-model="noticeForm.crowdCategory" style="width: 100%">
              <el-option v-for="item in CROWD_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="24">
          <el-form-item label="现居住地址">
            <el-input v-model="noticeForm.currentAddress" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="户籍地址">
            <el-input v-model="noticeForm.householdAddress" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">
        感染检查
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="感染检测时间">
            <el-date-picker v-model="noticeForm.infectionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="检查方法">
            <el-select v-model="noticeForm.infectionMethod" style="width: 100%">
              <el-option v-for="item in INFECTION_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="检查结果">
            <el-input v-model="noticeForm.infectionResultValue" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">
        胸片检查
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="胸片检查时间">
            <el-date-picker v-model="noticeForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="胸片检查结果">
            <el-select v-model="noticeForm.chestXrayResult" style="width: 100%">
              <el-option v-for="item in CHEST_XRAY_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">
        治疗方案
      </el-divider>
      <el-form-item label="治疗方案">
        <el-select v-model="noticeForm.treatmentPlan" style="width: 100%" placeholder="请选择治疗方案">
          <el-option v-for="item in LATENT_TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isLatentIndividualPlan(noticeForm.treatmentPlan)" label="方案详情">
        <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="3" placeholder="请手动录入个体治疗方案详情" />
      </el-form-item>
      <el-divider content-position="left">
        机构信息
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="治疗机构">
            <el-input v-model="noticeForm.treatmentInstitution" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="下发时间">
            <el-input :model-value="noticeForm.issuedTime" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="接收单位" prop="receiverOrgId">
        <el-select v-model="noticeForm.receiverOrgId" placeholder="请选择接收单位（必填）" filterable style="width: 100%">
          <el-option
            v-for="u in level5Users"
            :key="u.id"
            :label="`${u.realName || u.username} - ${u.orgName || '未设置机构'}`"
            :value="u.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">
        取消
      </el-button>
      <el-button :loading="submitting" @click="handleSaveDraft">
        保存草稿
      </el-button>
      <el-button type="primary" :loading="submitting" @click="handleSendNotice">
        发送通知单
      </el-button>
    </template>
  </el-dialog>
</template>
