<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { uploadScreeningSchoolApi, getScreeningSchoolListApi, exportScreeningSchoolApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  schoolName: "",
  district: "",
  isLatent: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getScreeningSchoolListApi({
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
  searchForm.schoolName = ""
  searchForm.district = ""
  searchForm.isLatent = undefined
  handleSearch()
}

/** Excel 上传 */
const uploadRef = ref()
const importResultVisible = ref(false)
const importResult = ref<{ successCount: number; errors: string[] }>({ successCount: 0, errors: [] })

async function handleUpload(uploadFile: any) {
  try {
    const { data } = await uploadScreeningSchoolApi(uploadFile.raw)
    importResult.value = data
    importResultVisible.value = true
    fetchData()
  } catch {
    ElMessage.error("上传失败")
  }
}

/** 导出 Excel */
async function handleExport() {
  try {
    const res = await exportScreeningSchoolApi()
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "学校人群筛查数据.xlsx"
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
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
        <el-form-item label="学校名称">
          <el-input v-model="searchForm.schoolName" placeholder="请输入学校名称" clearable />
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
          <span class="text-lg font-bold">学校人群筛查数据</span>
          <div class="flex gap-2">
            <el-button @click="handleExport">导出数据</el-button>
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept=".xlsx,.xls"
              :on-change="handleUpload"
            >
              <el-button v-permission="'screening:upload'" type="primary">上传 Excel</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <!-- V4：移除胸片/诊断/痰涂片/分子生物学列（已移至潜伏感染追踪阶段录入），新增预防性治疗完成情况列 -->
      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="district" label="区县" />
        <el-table-column prop="schoolName" label="学校名称" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="schoolType" label="学校类型" />
        <el-table-column prop="ethnicity" label="民族" />
        <el-table-column prop="tbHistory" label="既往结核病史" />
        <el-table-column prop="closeContactHistory" label="密切接触史" />
        <el-table-column prop="suspiciousSymptoms" label="可疑症状" />
        <el-table-column prop="hasInfectionScreen" label="是否进行感染筛" />
        <el-table-column prop="screenDate" label="感染筛查日期" />
        <el-table-column prop="screenMethod" label="筛查方法" />
        <el-table-column prop="screenResult" label="筛查结果" />
        <el-table-column prop="infectionResult" label="感染筛查结果" />
        <!-- 预防性治疗情况（督导表归档后同步） -->
        <el-table-column prop="preventivePlan" label="预防性治疗方案" />
        <el-table-column prop="preventiveStartDate" label="治疗开始时间" />
        <el-table-column prop="preventiveEndDate" label="治疗完成时间" />
        <el-table-column prop="preventiveResult" label="治疗结果" />
        <el-table-column prop="preventiveManager" label="随访管理人员" show-overflow-tooltip />
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

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="560px">
      <el-alert
        :title="`成功导入 ${importResult.successCount} 条数据`"
        type="success"
        :closable="false"
        class="mb-3"
      />
      <template v-if="importResult.errors.length > 0">
        <el-alert
          :title="`发现 ${importResult.errors.length} 条数据存在格式问题（已照常导入，请核查）`"
          type="warning"
          :closable="false"
          class="mb-3"
        />
        <el-table :data="importResult.errors.map((e, i) => ({ index: i + 1, msg: e }))" border max-height="300">
          <el-table-column prop="index" label="#" width="50" />
          <el-table-column prop="msg" label="错误信息" />
        </el-table>
      </template>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
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
