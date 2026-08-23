<script lang="ts" setup>
import { getLevel5UsersApi } from "@@/apis/users"
import {
  applyPatientNoticeTreatmentPlan,
  CHEST_XRAY_RESULT_OPTIONS,
  CROWD_CATEGORY_OPTIONS,
  DRUG_RESISTANCE_OPTIONS,
  isPatientOtherSensitivePlan,
  PATHOGEN_RESULT_OPTIONS,
  PATIENT_MANAGEMENT_METHOD_OPTIONS,
  PATIENT_OTHER_SENSITIVE_PLAN,
  PATIENT_TYPE_OPTIONS,
  resolvePatientTreatmentPlanForSave,
  TREATMENT_PLAN_OPTIONS
} from "@@/constants/disease"
import {
  resolveFirstTreatmentPlan,
  resolveMedicationManagementUnit,
  resolveNoticePatientType,
  resolveNoticeSputumSmearFromPatient,
  resolvePatientCrowdCategory,
  resolvePatientCurrentUnit,
  resolvePatientEthnicity
} from "@@/utils/patient"
import { idCardRule } from "@@/utils/validate"
import { getNoticeListByBizApi } from "@/pages/patient-management/apis"
import { saveNoticeDraftApi, sendNoticeApi } from "@/pages/school/latent/apis"
import { useUserStore } from "@/pinia/stores/user"

const props = defineProps<{
  visible: boolean
  patientRow: Record<string, any> | null
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
  idNumber: [idCardRule()],
  patientType: [{ required: true, message: "请选择患者类型", trigger: "change" }],
  managementMethod: [{ required: true, message: "请选择管理方式", trigger: "change" }],
  receiverOrgId: [{ required: true, message: "请选择接收单位", trigger: "change" }]
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
  chestXrayDate: "",
  chestXrayResult: "",
  treatmentInstitution: "",
  issuedTime: "",
  patientType: "",
  managementMethod: "",
  treatmentPlan: "",
  drugResistance: "",
  customPlanDetail: "",
  sputumSmear: "",
  sputumCulture: "",
  molecularTest: "",
  pathologyTest: "",
  otherNotes: "",
  medicationManagementUnit: "",
  remark: "",
  receiverOrgId: undefined as string | undefined
})

function resolveNoticeDrugResistance(row: Record<string, any>): string {
  return row.firstVisitDrugResistance || row.drugResistance || ""
}

function resetFormFromRow(row: Record<string, any>) {
  Object.assign(noticeForm, {
    idNumber: row.idNumber || "",
    gender: row.gender || "",
    birthDate: row.birthDate || "",
    age: row.age ?? null,
    ethnicity: resolvePatientEthnicity(row),
    phone: row.phone || "",
    crowdCategory: resolvePatientCrowdCategory(row, CROWD_CATEGORY_OPTIONS),
    currentAddress: row.currentAddress || "",
    householdAddress: row.householdAddress || "",
    chestXrayDate: row.chestXrayDate || "",
    chestXrayResult: row.chestXrayResult || "",
    treatmentInstitution: resolvePatientCurrentUnit(row),
    issuedTime: new Date().toISOString().slice(0, 10),
    patientType: resolveNoticePatientType(row),
    managementMethod: "",
    treatmentPlan: "",
    drugResistance: resolveNoticeDrugResistance(row),
    customPlanDetail: "",
    sputumSmear: resolveNoticeSputumSmearFromPatient(row),
    sputumCulture: "",
    molecularTest: "",
    pathologyTest: "",
    otherNotes: "",
    medicationManagementUnit: resolveMedicationManagementUnit(row),
    remark: "",
    receiverOrgId: userStore.userRole === 6 ? userStore.userId : undefined
  })
  applyPatientNoticeTreatmentPlan(noticeForm, resolveFirstTreatmentPlan(row))
}

function assignFormFromNotice(notice: Record<string, any>, row: Record<string, any>) {
  Object.assign(noticeForm, {
    idNumber: notice.idNumber || "",
    gender: notice.gender || "",
    birthDate: notice.birthDate || "",
    age: notice.age ?? null,
    ethnicity: notice.ethnicity || resolvePatientEthnicity(row),
    phone: notice.phone || "",
    crowdCategory: notice.crowdCategory || resolvePatientCrowdCategory(row, CROWD_CATEGORY_OPTIONS),
    currentAddress: notice.currentAddress || "",
    householdAddress: notice.householdAddress || "",
    chestXrayDate: notice.chestXrayDate || "",
    chestXrayResult: notice.chestXrayResult || "",
    treatmentInstitution: notice.treatmentInstitution || resolvePatientCurrentUnit(row),
    issuedTime: notice.issuedTime || new Date().toISOString().slice(0, 10),
    patientType: notice.patientType || resolveNoticePatientType(row),
    managementMethod: notice.managementMethod || "",
    drugResistance: notice.drugResistance || resolveNoticeDrugResistance(row),
    sputumSmear: notice.sputumSmear || resolveNoticeSputumSmearFromPatient(row),
    sputumCulture: notice.sputumCulture || "",
    molecularTest: notice.molecularTest || "",
    pathologyTest: notice.pathologyTest || "",
    otherNotes: notice.otherNotes || "",
    medicationManagementUnit: notice.medicationManagementUnit || resolveMedicationManagementUnit(row),
    remark: notice.remark || "",
    receiverOrgId: notice.receiverOrgId || undefined
  })
  const plan = notice.treatmentPlan || resolveFirstTreatmentPlan(row)
  applyPatientNoticeTreatmentPlan(noticeForm, plan, notice.customPlanDetail)
}

async function loadDraftIfNeeded(row: Record<string, any>) {
  if (row.noticeStatus === 0 || row.noticeStatus === 2) {
    try {
      const { data } = await getNoticeListByBizApi(row.id, "patient")
      const notice = data?.[0]
      if (notice) {
        assignFormFromNotice(notice, row)
        return
      }
    } catch { /* ignore */ }
  }
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
    if (val && props.patientRow) {
      await loadDraftIfNeeded(props.patientRow)
    }
  }
)

onMounted(loadLevel5Users)

function buildPayload() {
  const row = props.patientRow!
  return {
    noticeType: "patient",
    populationType: row.populationType,
    bizId: row.id,
    patientName: row.name,
    ...noticeForm,
    drugResistance: noticeForm.drugResistance || "",
    treatmentPlan: resolvePatientTreatmentPlanForSave(noticeForm.treatmentPlan, noticeForm.customPlanDetail),
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
  if (isPatientOtherSensitivePlan(noticeForm.treatmentPlan) && !noticeForm.customPlanDetail?.trim()) {
    ElMessage.warning("请填写方案详情")
    return
  }
  submitting.value = true
  try {
    await sendNoticeApi(buildPayload())
    ElMessage.success("患者通知单发送成功")
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
    title="填写患者通知单"
    width="780px"
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
            <el-input :model-value="patientRow?.name" disabled />
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
          <el-form-item label="联系方式">
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
            <el-input v-model="noticeForm.currentAddress" placeholder="请输入现居住地址" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="户籍地址">
            <el-input v-model="noticeForm.householdAddress" placeholder="请输入户籍地址" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="患者类型" prop="patientType" required>
            <el-select v-model="noticeForm.patientType" style="width: 100%">
              <el-option v-for="item in PATIENT_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="管理方式" prop="managementMethod" required>
            <el-select v-model="noticeForm.managementMethod" style="width: 100%">
              <el-option v-for="item in PATIENT_MANAGEMENT_METHOD_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
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
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="治疗方案">
            <el-select v-model="noticeForm.treatmentPlan" style="width: 100%">
              <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="耐药情况">
            <el-select v-model="noticeForm.drugResistance" clearable style="width: 100%">
              <el-option v-for="item in DRUG_RESISTANCE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item v-if="isPatientOtherSensitivePlan(noticeForm.treatmentPlan)" label="方案详情" required>
        <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="2" placeholder="请注明详细的抗结核治疗方案" />
      </el-form-item>

      <el-divider content-position="left">
        病原学检查
      </el-divider>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="痰涂片">
            <el-select v-model="noticeForm.sputumSmear" style="width: 100%">
              <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="痰培养">
            <el-select
              v-model="noticeForm.sputumCulture"
              style="width: 100%"
              placeholder="请选择或输入"
              filterable
              allow-create
              default-first-option
            >
              <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="分子检查">
            <el-select v-model="noticeForm.molecularTest" style="width: 100%">
              <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="病理学检查">
        <el-select v-model="noticeForm.pathologyTest" style="width: 100%">
          <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
        </el-select>
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
            <el-date-picker v-model="noticeForm.issuedTime" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="服药管理单位">
        <el-input v-model="noticeForm.medicationManagementUnit" placeholder="来自病案信息，可手动调整" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="noticeForm.remark" type="textarea" :rows="2" placeholder="手动填写备注信息，打印时将显示" />
      </el-form-item>
      <el-form-item label="其他注意事项">
        <el-input v-model="noticeForm.otherNotes" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="接收单位" prop="receiverOrgId" required>
        <el-select v-model="noticeForm.receiverOrgId" placeholder="请选择五级机构" filterable style="width: 100%">
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
        发送
      </el-button>
    </template>
  </el-dialog>
</template>
