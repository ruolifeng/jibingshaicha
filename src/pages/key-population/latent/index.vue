<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { TRACKING_STATUS_MAP, REFERRAL_RESULT_OPTIONS, CROWD_CATEGORY_OPTIONS, TREATMENT_PLAN_OPTIONS, NOTICE_STATUS_MAP } from "@@/constants/disease"
import { getLatentListApi, trackLatentApi, referralLatentApi, sendNoticeApi, confirmNoticeApi, getNoticeListByBizApi, saveSupervisionApi, getSupervisionDetailApi } from "./apis"
import { getLevel5UsersApi } from "@@/apis/users"
import { useUserStore } from "@/pinia/stores/user"

const userStore = useUserStore()
const level5Users = ref<any[]>([])

async function loadLevel5Users() {
  try {
    const { data } = await getLevel5UsersApi()
    level5Users.value = data || []
  } catch { /* handled */ }
}

onMounted(() => { loadLevel5Users() })

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  trackingStatus: undefined as number | undefined,
  archived: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getLatentListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      populationType: "keyPopulation",
      ...searchForm
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  paginationData.currentPage = 1
  fetchData()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.trackingStatus = undefined
  searchForm.archived = undefined
  handleSearch()
}

// ==================== 追踪弹窗 ====================
const trackDialogVisible = ref(false)
const trackingRow = ref<any>(null)
const trackForm = reactive({ status: 1, remark: "" })

function openTrackDialog(row: any) {
  trackingRow.value = row
  trackForm.status = 1
  trackForm.remark = ""
  trackDialogVisible.value = true
}

async function handleTrack() {
  try {
    await trackLatentApi({ id: trackingRow.value.id, status: trackForm.status, remark: trackForm.remark })
    ElMessage.success("操作成功")
    trackDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ }
}

// ==================== 转诊弹窗 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
const referralForm = reactive({ result: "", remark: "" })

function openReferralDialog(row: any) {
  referralRow.value = row
  referralForm.result = ""
  referralForm.remark = ""
  referralDialogVisible.value = true
}

async function handleReferral() {
  if (!referralForm.result) {
    ElMessage.warning("请选择转诊结果")
    return
  }
  try {
    await referralLatentApi({ id: referralRow.value.id, result: referralForm.result, remark: referralForm.remark })
    ElMessage.success("操作成功")
    referralDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ }
}

// ==================== 通知单弹窗 ====================
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const noticeForm = reactive({
  currentAddress: "",
  householdAddress: "",
  idNumber: "",
  gender: "",
  birthDate: "",
  age: null as number | null,
  ethnicity: "",
  crowdCategory: "",
  treatmentPlan: "",
  customPlanDetail: "",
  receiverOrgId: undefined as number | undefined
})

function openNoticeDialog(row: any) {
  noticeRow.value = row
  noticeForm.currentAddress = ""
  noticeForm.householdAddress = ""
  noticeForm.idNumber = row.idNumber || ""
  noticeForm.gender = row.gender || ""
  noticeForm.birthDate = ""
  noticeForm.age = row.age || null
  noticeForm.ethnicity = ""
  noticeForm.crowdCategory = ""
  noticeForm.treatmentPlan = ""
  noticeForm.customPlanDetail = ""
  noticeForm.receiverOrgId = undefined
  noticeDialogVisible.value = true
}

async function handleSendNotice() {
  try {
    await sendNoticeApi({
      noticeType: "latent",
      populationType: "keyPopulation",
      bizId: noticeRow.value.id,
      patientName: noticeRow.value.name,
      currentAddress: noticeForm.currentAddress,
      householdAddress: noticeForm.householdAddress,
      idNumber: noticeForm.idNumber,
      gender: noticeForm.gender,
      birthDate: noticeForm.birthDate,
      age: noticeForm.age,
      ethnicity: noticeForm.ethnicity,
      crowdCategory: noticeForm.crowdCategory,
      treatmentPlan: noticeForm.treatmentPlan === "个体化方案" ? noticeForm.customPlanDetail : noticeForm.treatmentPlan,
      receiverOrgId: noticeForm.receiverOrgId,
      senderId: userStore.userId
    })
    ElMessage.success("通知单发送成功")
    noticeDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ }
}

async function handleConfirmNotice(noticeId: number) {
  try {
    await ElMessageBox.confirm("确认接收此通知单吗？", "提示", { type: "info" })
    await confirmNoticeApi(noticeId)
    ElMessage.success("已确认接收")
    noticeDetailVisible.value = false
    fetchData()
  } catch { /* cancelled or handled */ }
}

// ==================== 通知单详情查看 ====================
const noticeDetailVisible = ref(false)
const noticeDetailData = ref<any>(null)

async function viewNotice(row: any) {
  try {
    const { data } = await getNoticeListByBizApi(row.id, "latent")
    if (data && data.length > 0) {
      noticeDetailData.value = data[0]
      noticeDetailVisible.value = true
    } else {
      ElMessage.info("暂无通知单")
    }
  } catch { /* handled by interceptor */ }
}

// ==================== 督导表弹窗 ====================
const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)
const supervisionForm = reactive({
  treatmentStartDate: "",
  treatmentPlan: "",
  supervisionContent: ""
})

function openSupervisionDialog(row: any) {
  supervisionRow.value = row
  supervisionForm.treatmentStartDate = ""
  supervisionForm.treatmentPlan = ""
  supervisionForm.supervisionContent = ""
  supervisionDialogVisible.value = true
}

async function handleSaveSupervision() {
  try {
    await saveSupervisionApi({
      latentInfectionId: supervisionRow.value.id,
      populationType: "keyPopulation",
      patientName: supervisionRow.value.name,
      treatmentStartDate: supervisionForm.treatmentStartDate,
      treatmentPlan: supervisionForm.treatmentPlan,
      supervisionContent: supervisionForm.supervisionContent,
      status: 2
    })
    ElMessage.success("督导表保存成功")
    supervisionDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ }
}

// ==================== 督导表查看 ====================
const supervisionDetailVisible = ref(false)
const supervisionDetailData = ref<any>(null)

async function viewSupervision(row: any) {
  try {
    const { data } = await getSupervisionDetailApi(row.id)
    if (data) {
      supervisionDetailData.value = data
      supervisionDetailVisible.value = true
    } else {
      ElMessage.info("暂无督导表")
    }
  } catch { /* handled by interceptor */ }
}

function getTrackingStatusType(status: number) {
  if (status === 1) return "success"
  if (status === 2 || status === 4) return "danger"
  if (status === 3) return "warning"
  return "info"
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item label="追踪状态">
          <el-select v-model="searchForm.trackingStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(label, key) in TRACKING_STATUS_MAP" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="归档状态">
          <el-select v-model="searchForm.archived" placeholder="全部" clearable style="width: 120px">
            <el-option label="未归档" :value="0" />
            <el-option label="已归档" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <template #header>
        <span class="text-lg font-bold">重点人群 — 潜伏感染管理</span>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" />
        <el-table-column label="追踪状态">
          <template #default="{ row }">
            <el-tag :type="getTrackingStatusType(row.trackingStatus)" size="small">
              {{ TRACKING_STATUS_MAP[row.trackingStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notInPlaceCount" label="未到位次数" />
        <el-table-column prop="trackingRemark" label="追踪备注" />
        <el-table-column prop="referralResult" label="转诊结果">
          <template #default="{ row }">
            {{ REFERRAL_RESULT_OPTIONS.find(o => o.value === row.referralResult)?.label || row.referralResult || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisResult" label="诊断结果" />
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-button v-if="row.referralResult === 'latent'" type="primary" link size="small" @click="viewNotice(row)">
              {{ row.name }}通知单
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="归档">
          <template #default="{ row }">
            <el-tag :type="row.archived ? 'info' : 'success'" size="small">
              {{ row.archived ? "已归档" : "进行中" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.trackingStatus === 0 || row.trackingStatus === 2"
              v-permission="'latent:track'"
              type="primary"
              size="small"
              @click="openTrackDialog(row)"
            >
              追踪
            </el-button>
            <el-button
              v-if="row.trackingStatus === 1 && !row.referralResult"
              v-permission="'latent:referral'"
              type="warning"
              size="small"
              @click="openReferralDialog(row)"
            >
              转诊
            </el-button>
            <el-button
              v-if="row.referralResult === 'latent'"
              v-permission="'latent:sendNotice'"
              type="success"
              size="small"
              @click="openNoticeDialog(row)"
            >
              发送通知单
            </el-button>
            <el-button
              v-if="row.referralResult === 'latent'"
              v-permission="'latent:supervision'"
              size="small"
              @click="openSupervisionDialog(row)"
            >
              填写督导表
            </el-button>
            <el-button
              v-if="row.referralResult === 'latent'"
              type="info"
              size="small"
              @click="viewSupervision(row)"
            >
              查看督导表
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 追踪弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="追踪状态">
          <el-radio-group v-model="trackForm.status">
            <el-radio :value="1">到位</el-radio>
            <el-radio :value="2">未到位</el-radio>
            <el-radio :value="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="trackForm.status === 3 || (trackForm.status === 2 && trackingRow?.notInPlaceCount >= 2)" label="备注原因">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" placeholder="请填写原因" />
        </el-form-item>
        <el-alert v-if="trackForm.status === 2 && trackingRow" :closable="false" class="mb-4">
          <template #default>当前已未到位 {{ trackingRow.notInPlaceCount }} 次，最多 3 次后自动归档</template>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTrack">确认</el-button>
      </template>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <el-dialog v-model="referralDialogVisible" title="转诊操作" width="450px">
      <el-form label-width="80px">
        <el-form-item label="转诊结果">
          <el-radio-group v-model="referralForm.result">
            <el-radio v-for="item in REFERRAL_RESULT_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="referralForm.result === 'other'" label="备注原因">
          <el-input v-model="referralForm.remark" type="textarea" :rows="3" placeholder="请填写原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="referralDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReferral">确认</el-button>
      </template>
    </el-dialog>

    <!-- 通知单弹窗 -->
    <el-dialog v-model="noticeDialogVisible" title="填写潜伏者通知单" width="600px">
      <el-form :model="noticeForm" label-width="100px">
        <el-form-item label="现居住地址">
          <el-input v-model="noticeForm.currentAddress" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="户籍地址">
          <el-input v-model="noticeForm.householdAddress" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="noticeForm.idNumber" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="noticeForm.gender" placeholder="请选择" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="noticeForm.birthDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="noticeForm.age" :min="0" :max="150" />
        </el-form-item>
        <el-form-item label="民族">
          <el-input v-model="noticeForm.ethnicity" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="人群分类">
          <el-select v-model="noticeForm.crowdCategory" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in CROWD_CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-select v-model="noticeForm.treatmentPlan" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="noticeForm.treatmentPlan === '个体化方案'" label="方案详情">
          <el-input v-model="noticeForm.customPlanDetail" type="textarea" :rows="3" placeholder="请注明详细的抗结核治疗方案" />
        </el-form-item>
        <el-form-item label="接收单位">
          <el-select v-model="noticeForm.receiverOrgId" placeholder="请选择五级机构" filterable style="width: 100%">
            <el-option v-for="u in level5Users" :key="u.id" :label="`${u.realName || u.username} - ${u.orgName || '未设置机构'}`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSendNotice">发送通知单</el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情弹窗 -->
    <el-dialog v-model="noticeDetailVisible" title="通知单详情" width="600px">
      <el-descriptions v-if="noticeDetailData" :column="2" border>
        <el-descriptions-item label="姓名">{{ noticeDetailData.patientName }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ noticeDetailData.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ noticeDetailData.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ noticeDetailData.age }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ noticeDetailData.ethnicity }}</el-descriptions-item>
        <el-descriptions-item label="人群分类">{{ noticeDetailData.crowdCategory }}</el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">{{ noticeDetailData.currentAddress }}</el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">{{ noticeDetailData.householdAddress }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ noticeDetailData.treatmentPlan }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ noticeDetailData.sentTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="noticeDetailData.status === 2 ? 'success' : 'warning'" size="small">
            {{ NOTICE_STATUS_MAP[noticeDetailData.status] }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="noticeDetailData.confirmedTime" label="确认时间">
          {{ noticeDetailData.confirmedTime }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button
          v-if="noticeDetailData && noticeDetailData.status === 1 && userStore.userRole === 6"
          v-permission="'latent:confirmNotice'"
          type="primary"
          @click="handleConfirmNotice(noticeDetailData.id)"
        >
          确认接收
        </el-button>
      </template>
    </el-dialog>

    <!-- 督导表填写弹窗 -->
    <el-dialog v-model="supervisionDialogVisible" title="填写预防性治疗督导表" width="600px">
      <el-form :model="supervisionForm" label-width="100px">
        <el-form-item label="治疗开始日期">
          <el-date-picker v-model="supervisionForm.treatmentStartDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-select v-model="supervisionForm.treatmentPlan" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in TREATMENT_PLAN_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="督导内容">
          <el-input v-model="supervisionForm.supervisionContent" type="textarea" :rows="5" placeholder="请填写督导内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supervisionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSupervision">保存并归档</el-button>
      </template>
    </el-dialog>

    <!-- 督导表详情弹窗 -->
    <el-dialog v-model="supervisionDetailVisible" title="督导表详情" width="600px">
      <el-descriptions v-if="supervisionDetailData" :column="2" border>
        <el-descriptions-item label="患者姓名">{{ supervisionDetailData.patientName }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案">{{ supervisionDetailData.treatmentPlan }}</el-descriptions-item>
        <el-descriptions-item label="治疗开始日期">{{ supervisionDetailData.treatmentStartDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="supervisionDetailData.status === 2 ? 'success' : 'info'" size="small">
            {{ supervisionDetailData.status === 2 ? "已归档" : "进行中" }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="督导内容" :span="2">{{ supervisionDetailData.supervisionContent }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
</style>
