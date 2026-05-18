<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { TREATMENT_PLAN_OPTIONS, CROWD_CATEGORY_OPTIONS, getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { getLatentAggregateListApi, saveSupervisionApi, getSupervisionDetailApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  archived: undefined as number | undefined,
  populationType: ""
})

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      trackingStatus: 1, // 仅到位的记录才需要督导表
      referralResult: "latent",
      ...searchForm
    }
    if (!params.populationType) delete params.populationType
    const { data } = await getLatentAggregateListApi(params)
    tableData.value = (data.records as any[]).filter((r: any) => r.populationType !== "closeContact")
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { paginationData.currentPage = 1; fetchData() }
function handleReset() {
  searchForm.name = ""
  searchForm.idNumber = ""
  searchForm.archived = undefined
  searchForm.populationType = ""
  handleSearch()
}

onMounted(fetchData)
watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

// ==================== 督导表 ====================
const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)
const isViewMode = ref(false)
const supervisionForm = reactive({
  latentInfectionId: 0,
  currentAddress: "",
  householdAddress: "",
  idNumber: "",
  gender: "",
  birthDate: "",
  age: "",
  ethnicity: "",
  crowdCategory: "",
  treatmentPlan: "",
  startDate: "",
  endDate: "",
  managingUnit: "",
  supervisingDoctor: "",
  remark: "",
  status: 0
})

async function openSupervision(row: any, viewOnly = false) {
  supervisionRow.value = row
  isViewMode.value = viewOnly
  // 尝试加载已有督导表
  try {
    const { data } = await getSupervisionDetailApi(row.id)
    if (data) {
      Object.assign(supervisionForm, data)
    } else {
      Object.assign(supervisionForm, {
        latentInfectionId: row.id,
        currentAddress: row.currentAddress ?? "",
        householdAddress: row.householdAddress ?? "",
        idNumber: row.idNumber ?? "",
        gender: row.gender ?? "",
        birthDate: row.birthDate ?? "",
        age: row.age ?? "",
        ethnicity: row.ethnicity ?? "",
        crowdCategory: "",
        treatmentPlan: "",
        startDate: "",
        endDate: "",
        managingUnit: "",
        supervisingDoctor: "",
        remark: "",
        status: 0
      })
    }
  } catch {
    supervisionForm.latentInfectionId = row.id
  }
  supervisionDialogVisible.value = true
}

async function handleSupervisionSave(archive = false) {
  supervisionForm.latentInfectionId = supervisionRow.value.id
  if (archive) supervisionForm.status = 2
  await saveSupervisionApi({ ...supervisionForm })
  ElMessage.success(archive ? "督导表已归档" : "督导表已保存")
  supervisionDialogVisible.value = false
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
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width:140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="常规筛查" value="regular" />
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
        <el-table-column prop="diagnosisFirst" label="诊断结果" width="110" />
        <el-table-column label="督导表状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.supervisionDone ? 'success' : 'warning'" size="small">
              {{ row.supervisionDone ? "已完成" : "待填写" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'latentManagement:supervision'" type="primary" link size="small"
              :disabled="row.archived === 1"
              @click="openSupervision(row, false)">
              {{ row.supervisionDone ? "查看/编辑督导表" : "填写督导表" }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 督导表弹窗 -->
    <el-dialog
      v-model="supervisionDialogVisible"
      :title="`预防性治疗督导表 - ${supervisionRow?.name ?? ''}`"
      width="680px"
      append-to-body
    >
      <el-form :model="supervisionForm" label-width="120px" :disabled="isViewMode">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="现居住地址">
              <el-input v-model="supervisionForm.currentAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="户籍地址">
              <el-input v-model="supervisionForm.householdAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证">
              <el-input v-model="supervisionForm.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="supervisionForm.gender">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="supervisionForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="民族">
              <el-input v-model="supervisionForm.ethnicity" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人群分类">
              <el-select v-model="supervisionForm.crowdCategory">
                <el-option v-for="opt in CROWD_CATEGORY_OPTIONS" :key="opt" :label="opt" :value="opt" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="治疗方案">
              <el-select v-model="supervisionForm.treatmentPlan">
                <el-option v-for="opt in TREATMENT_PLAN_OPTIONS" :key="opt" :label="opt" :value="opt" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="治疗开始时间">
              <el-date-picker v-model="supervisionForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="治疗结束时间">
              <el-date-picker v-model="supervisionForm.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理单位">
              <el-input v-model="supervisionForm.managingUnit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="督导医生">
              <el-input v-model="supervisionForm.supervisingDoctor" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="supervisionForm.remark" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer v-if="!isViewMode">
        <el-button @click="supervisionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSupervisionSave(false)">保存草稿</el-button>
        <el-button type="success" @click="handleSupervisionSave(true)">归档</el-button>
      </template>
      <template #footer v-else>
        <el-button @click="supervisionDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
