<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import {
  getReferralTrackingListApi,
  createReferralTrackingApi,
  trackReferralApi,
  saveScreeningInfoApi,
  saveDiagnosisApi,
  deleteReferralTrackingApi
} from "../apis/index"

// ===== 列表 =====
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const searchForm = reactive({ name: "", idNumber: "" })
const paginationData = reactive({ currentPage: 1, pageSize: 20 })

async function fetchList() {
  loading.value = true
  try {
    const res = await getReferralTrackingListApi({
      bizMode: "track",
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined
    })
    tableData.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)

function handleSearch() {
  paginationData.currentPage = 1
  fetchList()
}

function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  handleSearch()
}

// ===== 新增追踪 =====
const createDialogVisible = ref(false)
const createForm = reactive({
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
  crowdCategory: ""
})
const createFormRef = ref()

function openCreateDialog() {
  Object.assign(createForm, {
    name: "", gender: "", birthDate: "", age: undefined, idType: "居民身份证",
    idNumber: "", ethnicity: "", phone: "",
    householdAddress: "", currentAddress: "", crowdCategory: ""
  })
  createDialogVisible.value = true
}

async function handleCreate() {
  await createFormRef.value?.validate()
  await createReferralTrackingApi({ ...createForm, bizMode: "track" })
  ElMessage.success("追踪记录创建成功")
  createDialogVisible.value = false
  fetchList()
}

// ===== 追踪操作 =====
const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const trackForm = reactive({ status: undefined as number | undefined, remark: "" })

function openTrackDialog(row: any) {
  trackRow.value = row
  Object.assign(trackForm, { status: undefined, remark: "" })
  trackDialogVisible.value = true
}

async function handleTrack() {
  if (!trackForm.status) {
    ElMessage.warning("请选择追踪状态")
    return
  }
  await trackReferralApi(trackRow.value.id, trackForm.status, trackForm.remark)
  ElMessage.success("追踪状态已更新")
  trackDialogVisible.value = false
  fetchList()
}

// ===== 筛查信息 =====
const screeningDialogVisible = ref(false)
const screeningRow = ref<any>(null)
const screeningForm = reactive({
  hasInfectionScreen: "",
  screenDate: "",
  screenMethod: "",
  screenResult: "",
  infectionResult: "",
  hasChestXray: "",
  chestXrayDate: "",
  chestXrayResult: ""
})

function openScreeningDialog(row: any) {
  screeningRow.value = row
  Object.assign(screeningForm, {
    hasInfectionScreen: row.hasInfectionScreen ?? "",
    screenDate: row.screenDate ?? "",
    screenMethod: row.screenMethod ?? "",
    screenResult: row.screenResult ?? "",
    infectionResult: row.infectionResult ?? "",
    hasChestXray: row.hasChestXray ?? "",
    chestXrayDate: row.chestXrayDate ?? "",
    chestXrayResult: row.chestXrayResult ?? ""
  })
  screeningDialogVisible.value = true
}

async function handleSaveScreening() {
  await saveScreeningInfoApi(screeningRow.value.id, { ...screeningForm })
  ElMessage.success("筛查信息已保存")
  screeningDialogVisible.value = false
  fetchList()
}

// ===== 诊断 =====
const diagnosisDialogVisible = ref(false)
const diagnosisRow = ref<any>(null)
const diagnosisResult = ref("")

function openDiagnosisDialog(row: any) {
  diagnosisRow.value = row
  diagnosisResult.value = ""
  diagnosisDialogVisible.value = true
}

async function handleSaveDiagnosis() {
  if (!diagnosisResult.value) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  await saveDiagnosisApi(diagnosisRow.value.id, diagnosisResult.value)
  ElMessage.success("诊断结果已保存")
  diagnosisDialogVisible.value = false
  fetchList()
}

// ===== 删除 =====
async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.name}」的追踪记录？`, "删除确认", { type: "warning" })
  await deleteReferralTrackingApi(row.id)
  ElMessage.success("删除成功")
  fetchList()
}

// ===== 状态标签辅助 =====
const TRACKING_STATUS_MAP: Record<number, { label: string; type: string }> = {
  0: { label: "待追踪", type: "info" },
  1: { label: "到位", type: "success" },
  2: { label: "未到位", type: "warning" },
  3: { label: "其他", type: "" },
  4: { label: "强制结束", type: "danger" }
}
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card class="search-wrapper" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="toolbar-wrapper" style="margin-bottom: 12px">
        <el-button type="primary" @click="openCreateDialog">新增追踪</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="idNumber" label="证件号" min-width="160" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column label="追踪状态" width="90">
          <template #default="{ row }">
            <el-tag
              :type="TRACKING_STATUS_MAP[row.trackingStatus]?.type as any"
              size="small"
            >
              {{ TRACKING_STATUS_MAP[row.trackingStatus]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="追踪次数" width="80">
          <template #default="{ row }">
            {{ row.notInPlaceCount > 0 ? `${row.notInPlaceCount}次未到位` : "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisResult" label="诊断结果" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <!-- 追踪：待追踪或未到位 -->
            <el-button
              v-if="[0, 2].includes(row.trackingStatus) && !row.archived"
              type="warning" link size="small"
              @click="openTrackDialog(row)"
            >追踪</el-button>
            <!-- 筛查信息：已到位 -->
            <el-button
              v-if="row.trackingStatus === 1 && !row.diagnosisResult"
              type="primary" link size="small"
              @click="openScreeningDialog(row)"
            >录入筛查</el-button>
            <!-- 诊断：已到位 -->
            <el-button
              v-if="row.trackingStatus === 1 && !row.diagnosisResult"
              type="success" link size="small"
              @click="openDiagnosisDialog(row)"
            >录入诊断</el-button>
            <!-- 删除 -->
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 14px; justify-content: flex-end"
        layout="total, sizes, prev, pager, next"
        :total="total || 0"
        :page-size="paginationData.pageSize || 20"
        :current-page="paginationData.currentPage || 1"
        @size-change="(val: number) => { paginationData.pageSize = val; fetchList() }"
        @current-change="(val: number) => { paginationData.currentPage = val; fetchList() }"
      />
    </el-card>

    <!-- 新增追踪弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增追踪记录" width="620px">
      <el-form ref="createFormRef" :model="createForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name" :rules="[{ required: true, message: '请输入姓名' }]">
              <el-input v-model="createForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="createForm.gender" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="createForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="createForm.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件类型">
              <el-input v-model="createForm.idType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号">
              <el-input v-model="createForm.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="民族">
              <el-input v-model="createForm.ethnicity" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="createForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="户籍地址">
              <el-input v-model="createForm.householdAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现住址">
              <el-input v-model="createForm.currentAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人群分类">
              <el-input v-model="createForm.crowdCategory" placeholder="如：密接/糖尿病等" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 追踪操作弹窗 -->
    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="420px">
      <el-form label-width="100px">
        <el-form-item label="追踪状态">
          <el-radio-group v-model="trackForm.status">
            <el-radio :value="1">到位</el-radio>
            <el-radio :value="2">未到位</el-radio>
            <el-radio :value="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTrack">确认</el-button>
      </template>
    </el-dialog>

    <!-- 录入筛查信息弹窗 -->
    <el-dialog v-model="screeningDialogVisible" title="录入筛查信息" width="600px">
      <el-form :model="screeningForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否感染筛查">
              <el-select v-model="screeningForm.hasInfectionScreen" style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查日期">
              <el-date-picker v-model="screeningForm.screenDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查方法">
              <el-input v-model="screeningForm.screenMethod" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筛查结果">
              <el-input v-model="screeningForm.screenResult" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染筛查结果">
              <el-select v-model="screeningForm.infectionResult" style="width: 100%">
                <el-option label="阴性" value="阴性" />
                <el-option label="PPD+" value="PPD+" />
                <el-option label="PPD++" value="PPD++" />
                <el-option label="PPD+++" value="PPD+++" />
                <el-option label="EC阳性" value="EC阳性" />
                <el-option label="IGRA阳性" value="IGRA阳性" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否胸片检查">
              <el-select v-model="screeningForm.hasChestXray" style="width: 100%">
                <el-option label="是" value="是" />
                <el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片检查日期">
              <el-date-picker v-model="screeningForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="胸片检查结果">
              <el-select v-model="screeningForm.chestXrayResult" style="width: 100%">
                <el-option label="正常" value="正常" />
                <el-option label="异常" value="异常" />
                <el-option label="未查" value="未查" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="screeningDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveScreening">保存</el-button>
      </template>
    </el-dialog>

    <!-- 录入诊断弹窗 -->
    <el-dialog v-model="diagnosisDialogVisible" title="录入诊断结果" width="420px">
      <el-form label-width="100px">
        <el-form-item label="诊断结果">
          <el-radio-group v-model="diagnosisResult">
            <el-radio value="排除">排除</el-radio>
            <el-radio value="确诊患者">确诊患者</el-radio>
            <el-radio value="潜伏感染者">潜伏感染者</el-radio>
            <el-radio value="其他">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="diagnosisResult === '确诊患者'"
          title="确诊患者将自动进入【患者管理】模块（populationType=referral）"
          type="info" :closable="false" style="margin-top: 8px"
        />
        <el-alert
          v-if="diagnosisResult === '潜伏感染者'"
          title="潜伏感染者将自动进入【潜伏感染者管理】模块（populationType=referral）"
          type="info" :closable="false" style="margin-top: 8px"
        />
      </el-form>
      <template #footer>
        <el-button @click="diagnosisDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDiagnosis">确认诊断</el-button>
      </template>
    </el-dialog>
  </div>
</template>
