<script setup lang="ts">
/**
 * 筛查管理 → 推介：抓取行基本信息预填后发送推介通知单
 */
import PrintRecommend from "@@/components/PrintRecommend.vue"
import { REFERRAL_CROWD_CATEGORY_OPTIONS } from "@@/constants/disease"
import {
  REFERRAL_CHEST_XRAY_RESULT_OPTIONS,
  REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS,
  REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS,
  referralSelectOptionsWithLegacy
} from "@@/constants/referral-tracking"
import { idCardRule, phoneRule } from "@@/utils/validate"
import { ElMessage } from "element-plus"
import { nextTick, reactive, ref, watch } from "vue"
import { getLevel34UsersApi } from "@/pages/referral-management/apis"
import {
  createReferralWithDuplicateConfirm,
  isReferralDuplicateCancel
} from "@/pages/referral-management/composables/useReferralDuplicateConfirm"
import { useUserStore } from "@/pinia/stores/user"

const props = defineProps<{
  modelValue: boolean
  /** 筛查/待诊断行数据，用于预填 */
  source?: Record<string, any> | null
  /** 默认人群分类（如学生人群固定为「学生」） */
  defaultCrowdCategory?: string
}>()

const emit = defineEmits<{
  (e: "update:modelValue", v: boolean): void
  (e: "success"): void
}>()

const userStore = useUserStore()
const formRef = ref()
const level34Users = ref<any[]>([])
const submitting = ref(false)
const printVisible = ref(false)
const printData = ref<Record<string, any> | null>(null)

const form = reactive(createEmptyForm())

const formRules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idNumber: [idCardRule(true)],
  phone: [phoneRule(true)],
  currentAddress: [{ required: true, message: "请填写现住址", trigger: "blur" }],
  crowdCategory: [{ required: true, message: "请选择人群分类", trigger: "change" }],
  recommendReason: [{ required: true, message: "请填写推介原因", trigger: "blur" }],
  receiverUserId: [{ required: true, message: "请选择推介接收人", trigger: "change" }]
}

function createEmptyForm() {
  return {
    name: "",
    gender: "",
    birthDate: "",
    age: undefined as number | undefined,
    idType: "居民身份证",
    idNumber: "",
    ethnicity: "",
    phone: "",
    householdAddress: "",
    currentAddress: "",
    crowdCategory: "",
    screenDate: "",
    screenMethod: "",
    infectionResult: "",
    chestXrayDate: "",
    chestXrayResult: "",
    recommendUnitName: "",
    fillUserName: "",
    recommendReason: "",
    receiverUserId: undefined as string | undefined
  }
}

function toDateStr(v: unknown): string {
  if (v == null || v === "") return ""
  const s = String(v)
  return s.length >= 10 ? s.slice(0, 10) : s
}

/** 兼容后端/其它模块的人群分类别名 → 推介选项 */
function normalizeCrowdCategory(raw?: string): string {
  if (!raw) return ""
  if ((REFERRAL_CROWD_CATEGORY_OPTIONS as readonly string[]).includes(raw)) return raw
  if (raw === "既往结核") return "既往结核史"
  return ""
}

/** 重点人群多选标签 → 推介人群分类（优先级与后端 resolveKeyPopulationCrowdCategory 一致） */
function resolveCrowdCategoryFromFlags(source: Record<string, any>): string {
  const pairs: Array<[string, string]> = [
    ["crowdCategoryClose", "密接"],
    ["crowdCategoryStudent", "学生"],
    ["crowdCategoryTeacher", "教职工"],
    ["crowdCategoryElder", "老年人"],
    ["crowdCategoryDiabetes", "糖尿病"],
    ["crowdCategoryDual", "双感"],
    ["crowdCategoryTbHist", "既往结核史"],
    ["crowdCategoryNormal", "非重点人群"]
  ]
  for (const [key, label] of pairs) {
    if (source[key] === "是") return label
  }
  return ""
}

function inferScreenMethod(infectionResult: string, screenMethod?: string): string {
  if (screenMethod) return screenMethod
  if (!infectionResult) return ""
  if (infectionResult.startsWith("PPD")) return "PPD"
  if (infectionResult.startsWith("EC")) return "EC"
  if (infectionResult.startsWith("IGRA")) return "IGRA"
  return ""
}

function mapSourceToForm(source: Record<string, any>, defaultCrowdCategory?: string) {
  const infectionResult = source.infectionResult || source.tbScreenResult || source.screenResult || ""
  const crowdFromSource
    = normalizeCrowdCategory(source.crowdCategory) || resolveCrowdCategoryFromFlags(source)

  return {
    name: source.name || "",
    gender: source.gender || "",
    birthDate: toDateStr(source.birthDate),
    age: source.age != null && source.age !== "" ? Number(source.age) : undefined,
    idType: source.idType || "居民身份证",
    idNumber: source.idNumber || "",
    ethnicity: source.ethnicity || "",
    phone: source.phone || "",
    householdAddress: source.householdAddress || "",
    currentAddress: source.currentAddress || "",
    crowdCategory: defaultCrowdCategory || crowdFromSource || "",
    screenDate: toDateStr(source.screenDate || source.infectionScreenDate),
    screenMethod: inferScreenMethod(infectionResult, source.screenMethod),
    infectionResult,
    chestXrayDate: toDateStr(source.chestXrayDate),
    chestXrayResult: source.chestXrayResult || "",
    recommendUnitName: userStore.orgName || "",
    fillUserName: userStore.realName || userStore.username || "",
    recommendReason: "",
    receiverUserId: undefined as string | undefined
  }
}

function formatLevel34UserLabel(u: any) {
  const unit = u.orgName?.trim() || "未填写单位"
  return `${u.username}（${unit}）`
}

function resolveReceiverUserName(receiverUserId?: string) {
  if (!receiverUserId) return ""
  const receiver = level34Users.value.find(u => String(u.id) === String(receiverUserId))
  return receiver ? formatLevel34UserLabel(receiver) : ""
}

async function ensureReceivers() {
  if (level34Users.value.length > 0) return
  const res = await getLevel34UsersApi()
  level34Users.value = res.data ?? []
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    await ensureReceivers()
    Object.assign(form, mapSourceToForm(props.source || {}, props.defaultCrowdCategory))
    nextTick(() => formRef.value?.clearValidate())
  }
)

function close() {
  emit("update:modelValue", false)
}

function openPrint() {
  printData.value = {
    ...form,
    receiverUserName: resolveReceiverUserName(form.receiverUserId)
  }
  printVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await createReferralWithDuplicateConfirm({ ...form, bizMode: "recommend" })
    ElMessage.success("推介通知单已发送")
    close()
    emit("success")
  } catch (err) {
    // 业务/HTTP 错误已由 axios 拦截器提示；用户取消重复确认无需提示
    if (isReferralDuplicateCancel(err)) return
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="新增推介记录"
    width="720px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别">
            <el-select v-model="form.gender" style="width: 100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="出生日期">
            <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="年龄">
            <el-input-number v-model="form.age" :min="0" :max="150" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证件类型">
            <el-input v-model="form.idType" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证件号" prop="idNumber">
            <el-input v-model="form.idNumber" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="民族">
            <el-input v-model="form.ethnicity" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="户籍地址">
            <el-input v-model="form.householdAddress" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="现住址" prop="currentAddress">
            <el-input v-model="form.currentAddress" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="人群分类" prop="crowdCategory">
            <el-select v-model="form.crowdCategory" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="item in REFERRAL_CROWD_CATEGORY_OPTIONS"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="推介接收人" prop="receiverUserId">
            <el-select
              v-model="form.receiverUserId"
              filterable
              placeholder="选择一至五级用户"
              style="width: 100%"
            >
              <el-option
                v-for="u in level34Users"
                :key="u.id"
                :label="formatLevel34UserLabel(u)"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-divider content-position="left">
            筛查信息（选填）
          </el-divider>
        </el-col>
        <el-col :span="12">
          <el-form-item label="感染筛查时间">
            <el-date-picker
              v-model="form.screenDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="感染筛查方法">
            <el-select v-model="form.screenMethod" placeholder="请选择" clearable style="width: 100%">
              <el-option
                v-for="opt in referralSelectOptionsWithLegacy(REFERRAL_INFECTION_SCREEN_METHOD_OPTIONS, form.screenMethod)"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="感染筛查结果">
            <el-select v-model="form.infectionResult" placeholder="请选择" clearable style="width: 100%">
              <el-option
                v-for="opt in referralSelectOptionsWithLegacy(REFERRAL_INFECTION_SCREEN_RESULT_OPTIONS, form.infectionResult)"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="胸片筛查时间">
            <el-date-picker
              v-model="form.chestXrayDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="胸片筛查结果">
            <el-select v-model="form.chestXrayResult" placeholder="请选择" clearable style="width: 100%">
              <el-option
                v-for="opt in referralSelectOptionsWithLegacy(REFERRAL_CHEST_XRAY_RESULT_OPTIONS, form.chestXrayResult)"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="推介单位名称">
            <el-input v-model="form.recommendUnitName" readonly placeholder="系统自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="填写用户名称">
            <el-input v-model="form.fillUserName" readonly placeholder="系统自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="推介原因" prop="recommendReason">
            <el-input
              v-model="form.recommendReason"
              type="textarea"
              :rows="3"
              placeholder="请填写推介原因"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <div class="recommend-dialog-footer">
        <el-button @click="openPrint">
          打印 / 保存PDF
        </el-button>
        <div class="recommend-dialog-footer__actions">
          <el-button @click="close">
            取消
          </el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            发送推介
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>

  <PrintRecommend v-model:visible="printVisible" :data="printData" />
</template>

<style scoped lang="scss">
.recommend-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  &__actions {
    display: flex;
    gap: 8px;
  }
}
</style>
