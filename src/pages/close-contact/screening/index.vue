<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { uploadScreeningCloseContactApi, getScreeningCloseContactListApi, exportScreeningCloseContactApi } from "./apis"
import { ACTIVE_ROUND_MAP } from "@@/constants/disease"

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
    const { data } = await getScreeningCloseContactListApi({
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

function handleSearch() { paginationData.currentPage = 1; fetchData() }
function handleReset() {
  searchForm.name = ""; searchForm.idNumber = ""; searchForm.district = ""; searchForm.isLatent = undefined
  handleSearch()
}

async function handleUpload(uploadFile: any) {
  try {
    const { data } = await uploadScreeningCloseContactApi(uploadFile.raw)
    ElMessage.success(`成功导入 ${data} 条数据`)
    fetchData()
  } catch {
    ElMessage.error("上传失败")
  }
}

/** 导出 Excel */
async function handleExport() {
  try {
    const res = await exportScreeningCloseContactApi()
    const blob = new Blob([res as any], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "密接人群筛查数据.xlsx"
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

function getLatentTag(isLatent: number) { return isLatent === 1 ? "danger" : "success" }
function getActiveRoundTag(round: number) {
  if (round === 1) return "success"
  if (round === 2) return "warning"
  return "danger"
}

/** 三轮筛查详情弹窗 */
const detailVisible = ref(false)
const detailRow = ref<any>(null)
function viewDetail(row: any) { detailRow.value = row; detailVisible.value = true }

watch(() => [paginationData.currentPage, paginationData.pageSize], fetchData, { immediate: true })
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="姓名"><el-input v-model="searchForm.name" placeholder="请输入姓名" clearable /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="searchForm.idNumber" placeholder="请输入证件号" clearable /></el-form-item>
        <el-form-item label="区县"><el-input v-model="searchForm.district" placeholder="请输入区县" clearable /></el-form-item>
        <el-form-item label="判定结果">
          <el-select v-model="searchForm.isLatent" placeholder="全部" clearable style="width: 120px">
            <el-option label="潜伏管理者" :value="1" /><el-option label="非潜伏管理者" :value="0" />
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
          <span class="text-lg font-bold">密接人群筛查数据（V4 三轮）</span>
          <div class="flex gap-2">
            <el-button @click="handleExport">导出数据</el-button>
            <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleUpload">
              <el-button type="primary" v-permission="'screening:upload'">上传 Excel</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <!-- V4：按三轮折叠展示，点击"查看详情"弹窗展示三轮完整字段 -->
      <el-table v-loading="loading" :data="tableData" border stripe max-height="600">
        <el-table-column prop="name" label="姓名" fixed />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="district" label="区县" />
        <el-table-column prop="ethnicity" label="民族" />
        <el-table-column prop="occupation" label="职业" />
        <el-table-column prop="contactType" label="接触类型" />
        <el-table-column prop="sourcePatientName" label="原患者姓名" />
        <el-table-column prop="sourcePatientConfirmDate" label="原患者确诊日期" />
        <el-table-column prop="sourcePatientIdNumber" label="原患者身份证号" />

        <!-- 三轮感染筛查摘要 -->
        <el-table-column label="首次感染结果">
          <template #default="{ row }">
            <el-tag v-if="row.firstInfectionResult" :type="row.firstInfectionResult.includes('阳') || row.firstInfectionResult.includes('PPD+') ? 'danger' : 'success'" size="small">
              {{ row.firstInfectionResult || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="半年后感染结果">
          <template #default="{ row }">
            <el-tag v-if="row.halfYearInfectionResult" :type="row.halfYearInfectionResult.includes('阳') || row.halfYearInfectionResult.includes('PPD+') ? 'danger' : 'success'" size="small">
              {{ row.halfYearInfectionResult || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="一年后感染结果">
          <template #default="{ row }">
            <el-tag v-if="row.oneYearInfectionResult" :type="row.oneYearInfectionResult.includes('阳') || row.oneYearInfectionResult.includes('PPD+') ? 'danger' : 'success'" size="small">
              {{ row.oneYearInfectionResult || "-" }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 阳性轮次 -->
        <el-table-column label="阳性轮次">
          <template #default="{ row }">
            <el-tag v-if="row.activeRound" :type="getActiveRoundTag(row.activeRound)" size="small">
              {{ ACTIVE_ROUND_MAP[row.activeRound] }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <!-- 预防性治疗 -->
        <el-table-column prop="preventivePlan" label="预防性治疗方案" />
        <el-table-column prop="preventiveResult" label="治疗结果" />
        <el-table-column prop="benefitMethod" label="惠民方式" />

        <el-table-column label="潜伏判定" fixed="right">
          <template #default="{ row }">
            <el-tag :type="getLatentTag(row.isLatent)" size="small">
              {{ row.isLatent === 1 ? "潜伏管理者" : "正常" }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">查看详情</el-button>
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

    <!-- 三轮筛查详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`${detailRow?.name} — 筛查详情`" width="800px">
      <el-descriptions v-if="detailRow" :column="2" border class="mb-4">
        <el-descriptions-item label="姓名">{{ detailRow.name }}</el-descriptions-item>
        <el-descriptions-item label="证件号">{{ detailRow.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ detailRow.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detailRow.age }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ detailRow.ethnicity }}</el-descriptions-item>
        <el-descriptions-item label="职业">{{ detailRow.occupation }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailRow.phone }}</el-descriptions-item>
        <el-descriptions-item label="接触类型">{{ detailRow.contactType }}</el-descriptions-item>
        <el-descriptions-item label="原患者姓名">{{ detailRow.sourcePatientName }}</el-descriptions-item>
        <el-descriptions-item label="原患者确诊日期">{{ detailRow.sourcePatientConfirmDate }}</el-descriptions-item>
        <el-descriptions-item label="原患者身份证号">{{ detailRow.sourcePatientIdNumber }}</el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">{{ detailRow.householdAddress }}</el-descriptions-item>
        <el-descriptions-item label="现住址" :span="2">{{ detailRow.currentAddress }}</el-descriptions-item>
      </el-descriptions>

      <!-- 三轮筛查分栏 -->
      <el-tabs v-if="detailRow">
        <el-tab-pane label="首次筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="筛查日期">{{ detailRow.firstScreenDate }}</el-descriptions-item>
            <el-descriptions-item label="症状筛查结果">{{ detailRow.firstSymptomResult }}</el-descriptions-item>
            <el-descriptions-item label="感染检查方法">{{ detailRow.firstInfectionMethod }}</el-descriptions-item>
            <el-descriptions-item label="感染检查结果">{{ detailRow.firstScreenResult }}</el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">
              <el-tag :type="detailRow.firstInfectionResult?.includes('阳') || detailRow.firstInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                {{ detailRow.firstInfectionResult || "-" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否进行胸片">{{ detailRow.firstHasChestXray }}</el-descriptions-item>
            <el-descriptions-item label="胸片日期">{{ detailRow.firstChestXrayDate }}</el-descriptions-item>
            <el-descriptions-item label="胸片结果">{{ detailRow.firstChestXrayResult }}</el-descriptions-item>
            <el-descriptions-item label="诊断结果" :span="2">
              <el-tag v-if="detailRow.firstDiagnosis" size="small">{{ detailRow.firstDiagnosis }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="半年后筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="筛查日期">{{ detailRow.halfYearScreenDate }}</el-descriptions-item>
            <el-descriptions-item label="症状筛查结果">{{ detailRow.halfYearSymptomResult }}</el-descriptions-item>
            <el-descriptions-item label="感染检查方法">{{ detailRow.halfYearInfectionMethod }}</el-descriptions-item>
            <el-descriptions-item label="感染检查结果">{{ detailRow.halfYearScreenResult }}</el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">
              <el-tag :type="detailRow.halfYearInfectionResult?.includes('阳') || detailRow.halfYearInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                {{ detailRow.halfYearInfectionResult || "-" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否进行胸片">{{ detailRow.halfYearHasChestXray }}</el-descriptions-item>
            <el-descriptions-item label="胸片日期">{{ detailRow.halfYearChestXrayDate }}</el-descriptions-item>
            <el-descriptions-item label="胸片结果">{{ detailRow.halfYearChestXrayResult }}</el-descriptions-item>
            <el-descriptions-item label="诊断结果" :span="2">
              <el-tag v-if="detailRow.halfYearDiagnosis" size="small">{{ detailRow.halfYearDiagnosis }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="一年后筛查">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="筛查日期">{{ detailRow.oneYearScreenDate }}</el-descriptions-item>
            <el-descriptions-item label="症状筛查结果">{{ detailRow.oneYearSymptomResult }}</el-descriptions-item>
            <el-descriptions-item label="感染检查方法">{{ detailRow.oneYearInfectionMethod }}</el-descriptions-item>
            <el-descriptions-item label="感染检查结果">{{ detailRow.oneYearScreenResult }}</el-descriptions-item>
            <el-descriptions-item label="感染筛查结果">
              <el-tag :type="detailRow.oneYearInfectionResult?.includes('阳') || detailRow.oneYearInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                {{ detailRow.oneYearInfectionResult || "-" }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否进行胸片">{{ detailRow.oneYearHasChestXray }}</el-descriptions-item>
            <el-descriptions-item label="胸片日期">{{ detailRow.oneYearChestXrayDate }}</el-descriptions-item>
            <el-descriptions-item label="胸片结果">{{ detailRow.oneYearChestXrayResult }}</el-descriptions-item>
            <el-descriptions-item label="诊断结果" :span="2">
              <el-tag v-if="detailRow.oneYearDiagnosis" size="small">{{ detailRow.oneYearDiagnosis }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="预防性治疗情况">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="是否进行预防性治疗">{{ detailRow.hasPreventiveTreatment }}</el-descriptions-item>
            <el-descriptions-item label="预防性治疗方案">{{ detailRow.preventivePlan }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ detailRow.preventiveStartDate }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ detailRow.preventiveEndDate }}</el-descriptions-item>
            <el-descriptions-item label="治疗结果">{{ detailRow.preventiveResult }}</el-descriptions-item>
            <el-descriptions-item label="随访管理人员">{{ detailRow.preventiveManager }}</el-descriptions-item>
            <el-descriptions-item label="惠民方式">{{ detailRow.benefitMethod }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detailRow.remark }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="flex items-center justify-between">
          <div>
            <span v-if="detailRow?.activeRound" class="text-sm text-gray-500">
              阳性轮次：
              <el-tag :type="getActiveRoundTag(detailRow.activeRound)" size="small">{{ ACTIVE_ROUND_MAP[detailRow.activeRound] }}</el-tag>
            </span>
            <span v-else-if="detailRow?.isLatent === 0 && detailRow?.oneYearInfectionResult" class="text-sm text-gray-400">三轮均阴性，已归档</span>
          </div>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
</style>
