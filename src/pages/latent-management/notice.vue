<script lang="ts" setup>
import ReferralDialog from "@@/components/ReferralDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { TRACKING_STATUS_MAP, REFERRAL_RESULT_OPTIONS, getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import {
  getLatentAggregateListApi,
  trackLatentApi,
  referralLatentApi,
  sendNoticeApi,
  confirmNoticeApi,
  getNoticeDetailApi,
  getNoticeListByBizApi,
  submitXrayOnlyApi,
  submitDiagnosisApi,
  closeCaseApi
} from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  trackingStatus: undefined as number | undefined,
  archived: undefined as number | undefined,
  populationType: "" // 留空 = 全部来源（后端不过滤）
})

/** 潜伏感染列表仅显示 populationType 不为 closeContact 的记录
 *  前端侧过滤：若后端支持 exclude 参数则可改为后端过滤。
 *  当前策略：传 populationType="" 获取全部，再在前端标注来源。
 */
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      referralResult: "pending",
      ...searchForm
    }
    // 不传 populationType 或传空则返回全部；若用户选择某来源则过滤
    if (!params.populationType) delete params.populationType
    const { data } = await getLatentAggregateListApi(params)
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
  searchForm.populationType = ""
  handleSearch()
}

onMounted(fetchData)
watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

// ==================== 追踪 ====================
const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const trackForm = reactive({ status: undefined as number | undefined, remark: "" })

function openTrack(row: any) {
  trackRow.value = row
  trackForm.status = undefined
  trackForm.remark = ""
  trackDialogVisible.value = true
}
async function handleTrackSubmit() {
  if (trackForm.status === undefined) { ElMessage.warning("请选择追踪状态"); return }
  await trackLatentApi({ id: trackRow.value.id, status: trackForm.status, remark: trackForm.remark })
  ElMessage.success("追踪状态已更新")
  trackDialogVisible.value = false
  fetchData()
}

// ==================== 转诊 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
function openReferral(row: any) {
  referralRow.value = row
  referralDialogVisible.value = true
}

// ==================== 录入胸片 ====================
const xrayDialogVisible = ref(false)
const xrayRow = ref<any>(null)
const xrayForm = reactive({ hasChestXray: "", chestXrayDate: "", chestXrayResult: "" })
function openXray(row: any) {
  xrayRow.value = row
  xrayForm.hasChestXray = row.hasChestXray ?? ""
  xrayForm.chestXrayDate = row.chestXrayDate ?? ""
  xrayForm.chestXrayResult = row.chestXrayResult ?? ""
  xrayDialogVisible.value = true
}
async function handleXraySubmit() {
  await submitXrayOnlyApi({ id: xrayRow.value.id, ...xrayForm })
  ElMessage.success("胸片结果已保存")
  xrayDialogVisible.value = false
  fetchData()
}

// ==================== 录入诊断 ====================
const diagDialogVisible = ref(false)
const diagRow = ref<any>(null)
const diagForm = reactive({ diagnosisFirst: "" })
const DIAGNOSIS_OPTIONS = ["排除", "疑似肺结核", "潜伏感染者", "确诊患者", "其他"]
function openDiag(row: any) {
  diagRow.value = row
  diagForm.diagnosisFirst = row.diagnosisFirst ?? ""
  diagDialogVisible.value = true
}
async function handleDiagSubmit() {
  if (!diagForm.diagnosisFirst) { ElMessage.warning("请选择诊断结果"); return }
  await submitDiagnosisApi({ id: diagRow.value.id, diagnosisFirst: diagForm.diagnosisFirst })
  ElMessage.success("诊断结果已保存")
  diagDialogVisible.value = false
  fetchData()
}

// ==================== 发送通知单 ====================
import { TREATMENT_PLAN_OPTIONS, CROWD_CATEGORY_OPTIONS } from "@@/constants/disease"
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const noticeForm = reactive({
  currentAddress: "", householdAddress: "", idNumber: "", gender: "", birthDate: "",
  age: "", ethnicity: "", crowdCategory: "", treatmentPlan: ""
})
function openNotice(row: any) {
  noticeRow.value = row
  Object.assign(noticeForm, {
    currentAddress: row.currentAddress ?? "",
    householdAddress: row.householdAddress ?? "",
    idNumber: row.idNumber ?? "",
    gender: row.gender ?? "",
    birthDate: row.birthDate ?? "",
    age: row.age ?? "",
    ethnicity: row.ethnicity ?? "",
    crowdCategory: "",
    treatmentPlan: ""
  })
  noticeDialogVisible.value = true
}
async function handleNoticeSend() {
  if (!noticeForm.treatmentPlan) { ElMessage.warning("请选择治疗方案"); return }
  await sendNoticeApi({
    bizId: noticeRow.value.id,
    noticeType: "latent",
    populationType: noticeRow.value.populationType,
    name: noticeRow.value.name,
    ...noticeForm
  })
  ElMessage.success("通知单已发送")
  noticeDialogVisible.value = false
  fetchData()
}

// ==================== 查看通知单 ====================
const noticeDetailVisible = ref(false)
const noticeDetail = ref<any>(null)
async function viewNotice(row: any) {
  const notices = (await getNoticeListByBizApi(row.id, "latent")).data
  if (notices && notices.length > 0) {
    noticeDetail.value = notices[0]
    noticeDetailVisible.value = true
  } else {
    ElMessage.info("暂无通知单记录")
  }
}

// ==================== 结案归档 ====================
async function handleCloseCase(row: any) {
  await ElMessageBox.confirm(`确认将 ${row.name} 数据结案归档？`, "提示", { type: "warning" })
  await closeCaseApi(row.id)
  ElMessage.success("已归档")
  fetchData()
}
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card class="search-wrapper" shadow="never">
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="追踪状态">
          <el-select v-model="searchForm.trackingStatus" placeholder="全部" clearable style="width:120px">
            <el-option v-for="(label, val) in TRACKING_STATUS_MAP" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width:140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="常规筛查" value="regular" />
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" style="margin-top:10px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="数据来源" width="100">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="idNumber" label="证件号" width="170" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="infectionResult" label="感染筛查结果" width="130" />
        <el-table-column label="追踪状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.trackingStatus === 1 ? 'success' : row.trackingStatus === 2 ? 'danger' : 'info'" size="small">
              {{ TRACKING_STATUS_MAP[row.trackingStatus] ?? "待追踪" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisFirst" label="诊断结果" width="110" />
        <el-table-column label="通知单" width="100">
          <template #default="{ row }">
            <el-button v-if="row.noticeSent" type="primary" link size="small" @click="viewNotice(row)">
              {{ row.name }}通知单
            </el-button>
            <span v-else class="text-gray-400">未发送</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'latentManagement:track'" type="primary" link size="small"
              :disabled="row.trackingStatus === 1 || row.archived === 1"
              @click="openTrack(row)">追踪</el-button>
            <el-button v-permission="'latentManagement:xray'" type="primary" link size="small"
              :disabled="row.trackingStatus !== 1 || row.archived === 1"
              @click="openXray(row)">录入胸片</el-button>
            <el-button v-permission="'latentManagement:diagnosis'" type="warning" link size="small"
              :disabled="!row.hasChestXray || row.archived === 1"
              @click="openDiag(row)">录入诊断</el-button>
            <el-button v-permission="'latentManagement:notice'" type="success" link size="small"
              :disabled="row.trackingStatus !== 1 || row.archived === 1"
              @click="openNotice(row)">发送通知单</el-button>
            <el-button v-permission="'latentManagement:referral'" type="info" link size="small"
              @click="openReferral(row)">转诊</el-button>
            <el-button v-permission="'latentManagement:close'" type="danger" link size="small"
              :disabled="row.archived === 1"
              @click="handleCloseCase(row)">归档</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="pagination-container"
        :current-page="paginationData.currentPage || 1"
        :page-sizes="paginationData.pageSizes"
        :page-size="paginationData.pageSize || 10"
        :total="total || 0"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 追踪弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="420px" append-to-body>
      <el-form :model="trackForm" label-width="90px">
        <el-form-item label="追踪状态" required>
          <el-radio-group v-model="trackForm.status">
            <el-radio :value="1">到位</el-radio>
            <el-radio :value="2">未到位</el-radio>
            <el-radio :value="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="trackForm.status === 3 || trackForm.status === 2" label="备注">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTrackSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 录入胸片弹窗 -->
    <el-dialog v-model="xrayDialogVisible" title="录入胸片结果" width="480px" append-to-body>
      <el-form :model="xrayForm" label-width="120px">
        <el-form-item label="是否进行胸片检查">
          <el-select v-model="xrayForm.hasChestXray" placeholder="请选择">
            <el-option label="是" value="是" />
            <el-option label="否" value="否" />
          </el-select>
        </el-form-item>
        <el-form-item label="胸片检查日期">
          <el-date-picker v-model="xrayForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="胸片结果">
          <el-input v-model="xrayForm.chestXrayResult" placeholder="请输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="xrayDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleXraySubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 录入诊断弹窗 -->
    <el-dialog v-model="diagDialogVisible" title="录入诊断结果" width="400px" append-to-body>
      <el-form :model="diagForm" label-width="100px">
        <el-form-item label="诊断结果" required>
          <el-select v-model="diagForm.diagnosisFirst" placeholder="请选择">
            <el-option v-for="opt in DIAGNOSIS_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="diagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDiagSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发送通知单弹窗 -->
    <el-dialog v-model="noticeDialogVisible" :title="`发送潜伏者通知单 - ${noticeRow?.name ?? ''}`" width="580px" append-to-body>
      <el-form :model="noticeForm" label-width="110px">
        <el-form-item label="现居住地址">
          <el-input v-model="noticeForm.currentAddress" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="户籍地址">
          <el-input v-model="noticeForm.householdAddress" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="noticeForm.idNumber" placeholder="请输入" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="noticeForm.gender">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input v-model="noticeForm.age" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="出生日期">
          <el-date-picker v-model="noticeForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="民族">
          <el-input v-model="noticeForm.ethnicity" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="人群分类">
          <el-select v-model="noticeForm.crowdCategory" placeholder="请选择">
            <el-option v-for="opt in CROWD_CATEGORY_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗方案" required>
          <el-select v-model="noticeForm.treatmentPlan" placeholder="请选择">
            <el-option v-for="opt in TREATMENT_PLAN_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleNoticeSend">发送</el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情弹窗 -->
    <el-dialog v-model="noticeDetailVisible" title="潜伏者通知单详情" width="580px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ noticeDetail?.name }}</el-descriptions-item>
        <el-descriptions-item label="人群分类">{{ noticeDetail?.crowdCategory }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ noticeDetail?.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ noticeDetail?.gender }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ noticeDetail?.birthDate }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ noticeDetail?.age }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ noticeDetail?.ethnicity }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案">{{ noticeDetail?.treatmentPlan }}</el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">{{ noticeDetail?.currentAddress }}</el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">{{ noticeDetail?.householdAddress }}</el-descriptions-item>
        <el-descriptions-item label="发送状态">
          <el-tag :type="noticeDetail?.status === 1 ? 'success' : 'warning'">
            {{ noticeDetail?.status === 1 ? '已接收' : '待接收' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ noticeDetail?.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="referralRow"
      v-model="referralDialogVisible"
      :biz-id="referralRow.id"
      biz-type="latent_aggregate"
      module-type="latent"
      :population-type="referralRow.populationType"
      :subject-name="referralRow.name || ''"
    />
  </div>
</template>
