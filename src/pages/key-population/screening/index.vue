<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { uploadScreeningKeyPopulationApi, getScreeningKeyPopulationListApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  district: "",
  isLatent: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getScreeningKeyPopulationListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
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
  searchForm.district = ""
  searchForm.isLatent = undefined
  handleSearch()
}

/** Excel 上传 */
const uploadRef = ref()

async function handleUpload(uploadFile: any) {
  try {
    const { data } = await uploadScreeningKeyPopulationApi(uploadFile.raw)
    ElMessage.success(`成功导入 ${data} 条数据`)
    fetchData()
  } catch {
    ElMessage.error("上传失败")
  }
}

/** 判定结果标签颜色 */
function getLatentTag(isLatent: number) {
  return isLatent === 1 ? "danger" : "success"
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
        <el-form-item label="区县">
          <el-input v-model="searchForm.district" placeholder="请输入区县" clearable />
        </el-form-item>
        <el-form-item label="判定结果">
          <el-select v-model="searchForm.isLatent" placeholder="全部" clearable style="width: 120px">
            <el-option label="潜伏管理者" :value="1" />
            <el-option label="非潜伏管理者" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">重点人群筛查数据</span>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-change="handleUpload"
          >
            <el-button type="primary" v-permission="'screening:upload'">上传 Excel</el-button>
          </el-upload>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="district" label="区县" />
        <el-table-column prop="ethnicity" label="民族" />
        <el-table-column prop="crowdCategory" label="人群分类" />
        <el-table-column prop="hasSuspiciousSymptoms" label="可疑症状" />
        <el-table-column prop="cough" label="咳嗽咳痰" />
        <el-table-column prop="hemoptysis" label="咯血或血痰" />
        <el-table-column prop="fever" label="发热" />
        <el-table-column prop="chestPain" label="胸痛" />
        <el-table-column prop="nightSweats" label="夜间盗汗" />
        <el-table-column prop="appetiteLoss" label="食欲不振" />
        <el-table-column prop="fatigue" label="乏力" />
        <el-table-column prop="weightLoss" label="体重减轻" />
        <el-table-column prop="hasInfectionScreen" label="是否感染筛" />
        <el-table-column prop="screenMethod" label="筛查方法" />
        <el-table-column prop="screenResult" label="筛查结果" />
        <el-table-column prop="infectionResult" label="感染筛查结果" />
        <el-table-column prop="hasChestXray" label="胸片检查" />
        <el-table-column prop="chestXrayResult" label="胸片结果" />
        <el-table-column prop="resultJudgment" label="结果判定" />
        <el-table-column prop="isReferred" label="是否转诊" />
        <el-table-column prop="diagnosisResult" label="诊断结果" />
        <el-table-column prop="isEligibleForPrevention" label="符合预防性治疗" />
        <el-table-column prop="hasPreventiveTreatment" label="是否预防性治疗" />
        <el-table-column prop="preventionCompleted" label="规范完成" />
        <el-table-column label="潜伏判定" fixed="right">
          <template #default="{ row }">
            <el-tag :type="getLatentTag(row.isLatent)" size="small">
              {{ row.isLatent === 1 ? "潜伏管理者" : "正常" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <!-- 分页 -->
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
