<script lang="ts" setup>
import { ArrowDown } from "@element-plus/icons-vue"
import {
  exportAllLatentApi,
  exportAllPatientsApi,
  exportCategoryTableApi,
  exportCustomApi,
  exportDistrictStatisticsApi,
  exportSchoolStatisticsApi,
  exportWideTableApi,
  getDistrictOptionsApi,
  getDistrictStatisticsApi,
  getSchoolStatisticsApi
} from "./apis"
import QuestionnairePanel from "./components/QuestionnairePanel.vue"

defineOptions({ name: "Statistics" })

const activeTab = ref("school")

// ==================== 筛选条件 ====================
const filterForm = reactive({
  year: String(new Date().getFullYear()),
  district: ""
})

const districtOptions = ref<string[]>([])
const yearOptions = Array.from({ length: 10 }, (_, i) => String(new Date().getFullYear() - i))

async function loadDistrictOptions() {
  try {
    const { data } = await getDistrictOptionsApi()
    districtOptions.value = data || []
  } catch { /* ignore */ }
}

// ==================== 学校人群统计 ====================
const schoolLoading = ref(false)
const schoolData = ref<any[]>([])

async function fetchSchoolStatistics() {
  schoolLoading.value = true
  try {
    const { data } = await getSchoolStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    schoolData.value = data || []
  } catch { /* handled */ } finally {
    schoolLoading.value = false
  }
}

// ==================== 区县统计 ====================
const districtLoading = ref(false)
const districtData = ref<any[]>([])

async function fetchDistrictStatistics() {
  districtLoading.value = true
  try {
    const { data } = await getDistrictStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    districtData.value = data || []
  } catch { /* handled */ } finally {
    districtLoading.value = false
  }
}

// ==================== 搜索与重置 ====================
function handleSearch() {
  if (activeTab.value === "school") {
    fetchSchoolStatistics()
  } else {
    fetchDistrictStatistics()
  }
}

function handleReset() {
  filterForm.year = String(new Date().getFullYear())
  filterForm.district = ""
  handleSearch()
}

// ==================== 导出 ====================
function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

async function handleExportSchool() {
  try {
    const data = await exportSchoolStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    downloadBlob(data as unknown as Blob, `学校人群统计总表_${filterForm.year}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

async function handleExportDistrict() {
  try {
    const data = await exportDistrictStatisticsApi({
      year: filterForm.year,
      district: filterForm.district
    })
    downloadBlob(data as unknown as Blob, `区县统计表_${filterForm.year}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

// ==================== 大汇总/分类/自定义导出 ====================
async function handleExportWide() {
  try {
    const data = await exportWideTableApi(filterForm.year)
    downloadBlob(data as unknown as Blob, `大汇总表_${filterForm.year}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

const categoryPopType = ref("school")
const categoryPopOptions = [
  { label: "学校人群", value: "school" },
  { label: "重点人群", value: "keyPopulation" },
  { label: "密接人群", value: "closeContact" }
]
async function handleExportCategory() {
  try {
    const data = await exportCategoryTableApi(categoryPopType.value, filterForm.year)
    const label = categoryPopOptions.find(o => o.value === categoryPopType.value)?.label || "人群"
    downloadBlob(data as unknown as Blob, `${label}汇总表_${filterForm.year}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

// 自定义字段选择
const customDialogVisible = ref(false)
const customPopType = ref("school")
const CUSTOM_FIELD_OPTIONS = [
  { label: "年份", value: "year" },
  { label: "市州", value: "city" },
  { label: "县区", value: "district" },
  { label: "姓名", value: "name" },
  { label: "性别", value: "gender" },
  { label: "年龄", value: "age" },
  { label: "证件号", value: "idNumber" },
  { label: "联系电话", value: "phone" },
  { label: "感染筛查结果", value: "infectionResult" },
  { label: "胸片结果", value: "chestXrayResult" },
  { label: "诊断结果", value: "diagnosisResult" }
]
const selectedCustomFields = ref<string[]>(["name", "gender", "age", "idNumber", "infectionResult"])

async function handleExportCustom() {
  if (!selectedCustomFields.value.length) {
    ElMessage.warning("请至少选择一个字段")
    return
  }
  try {
    const data = await exportCustomApi(customPopType.value, selectedCustomFields.value.join(","), filterForm.year)
    downloadBlob(data as unknown as Blob, `自定义导出_${filterForm.year}.xlsx`)
    customDialogVisible.value = false
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

// ==================== P6 新增：信息总表导出 ====================
/** 患者总表筛选条件 */
const patientFilterForm = reactive({
  populationType: "",
  name: "",
  idNumber: "",
  archived: undefined as number | undefined
})

/** 潜伏感染者总表筛选条件 */
const latentFilterForm = reactive({
  populationType: "",
  name: "",
  idNumber: "",
  archived: undefined as number | undefined
})

/** 兼容旧引用 */
const aggregateFilterForm = patientFilterForm

/** 患者总表筛选来源（含密接、专病网） */
const PATIENT_POP_OPTIONS = [
  { label: "全部来源", value: "" },
  { label: "学生筛查", value: "school" },
  { label: "重点人群", value: "keyPopulation" },
  { label: "常规筛查", value: "regular" },
  { label: "大疫情", value: "epidemic" },
  { label: "推介", value: "referral" },
  { label: "专病网", value: "specialDisease" },
  { label: "密接", value: "closeContact" }
]

/** 潜伏感染者总表筛选来源（不含密接、专病网，密接潜伏由密接人群管理独立维护） */
const LATENT_POP_OPTIONS = [
  { label: "全部来源", value: "" },
  { label: "学生筛查", value: "school" },
  { label: "重点人群", value: "keyPopulation" },
  { label: "常规筛查", value: "regular" },
  { label: "大疫情", value: "epidemic" },
  { label: "推介", value: "referral" }
]

/** 兼容旧变量名（用于已有 label 查找） */
const AGGREGATE_POP_OPTIONS = PATIENT_POP_OPTIONS

async function handleExportAllPatients() {
  try {
    const params: Record<string, any> = {}
    if (aggregateFilterForm.populationType) params.populationType = aggregateFilterForm.populationType
    if (aggregateFilterForm.name) params.name = aggregateFilterForm.name
    if (aggregateFilterForm.idNumber) params.idNumber = aggregateFilterForm.idNumber
    if (aggregateFilterForm.archived !== undefined) params.archived = aggregateFilterForm.archived
    const data = await exportAllPatientsApi(params)
    const label = AGGREGATE_POP_OPTIONS.find(o => o.value === aggregateFilterForm.populationType)?.label || "全部来源"
    downloadBlob(data as unknown as Blob, `患者信息总表_${label}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

async function handleExportAllLatent() {
  try {
    const params: Record<string, any> = {}
    if (latentFilterForm.populationType) params.populationType = latentFilterForm.populationType
    if (latentFilterForm.name) params.name = latentFilterForm.name
    if (latentFilterForm.idNumber) params.idNumber = latentFilterForm.idNumber
    if (latentFilterForm.archived !== undefined) params.archived = latentFilterForm.archived
    const data = await exportAllLatentApi(params)
    const label = LATENT_POP_OPTIONS.find(o => o.value === (latentFilterForm.populationType || ""))?.label || "全部来源"
    downloadBlob(data as unknown as Blob, `潜伏感染者信息总表_${label}.xlsx`)
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  }
}

// ==================== Tab 切换时自动加载数据 ====================
function handleTabChange(tab: string | number) {
  if (tab === "school") {
    fetchSchoolStatistics()
  } else if (tab === "district") {
    fetchDistrictStatistics()
  }
}

onMounted(() => {
  loadDistrictOptions()
  fetchSchoolStatistics()
})
</script>

<template>
  <div class="app-container">
    <!-- 筛选条件（问卷 Tab 不需要年份/区县筛选） -->
    <el-card v-if="activeTab !== 'questionnaire'" shadow="never" class="mb-4">
      <el-form :model="filterForm" inline>
        <el-form-item label="年份">
          <el-select v-model="filterForm.year" placeholder="选择年份" clearable style="width: 120px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县">
          <el-select v-model="filterForm.district" placeholder="全部区县" clearable style="width: 160px">
            <el-option v-for="d in districtOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-dropdown>
            <el-button type="warning">
              高级导出 <el-icon class="el-icon--right">
                <ArrowDown />
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleExportWide">
                  大汇总表（三类合并）
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <span style="font-size:12px;color:#909399">分类汇总：</span>
                </el-dropdown-item>
                <el-dropdown-item v-for="opt in categoryPopOptions" :key="opt.value" @click="categoryPopType = opt.value; handleExportCategory()">
                  {{ opt.label }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="customDialogVisible = true">
                  自定义字段导出
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Tab 切换 -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 学校人群统计总表 -->
        <el-tab-pane label="辖区教育机构统计总表" name="school">
          <div class="mb-3 flex justify-end">
            <el-button type="success" v-permission="'statistics:export'" @click="handleExportSchool">
              导出 Excel
            </el-button>
          </div>
          <el-table v-loading="schoolLoading" :data="schoolData" border stripe max-height="600" show-summary>
            <el-table-column prop="district" label="区县" fixed />
            <el-table-column prop="schoolName" label="学校名称" />
            <el-table-column prop="shouldScreenCount" label="应筛查人数" />
            <el-table-column prop="actualScreenCount" label="实际筛查人数" />
            <el-table-column prop="closeContactCount" label="与肺结核患者密切接触的人数" />
            <el-table-column prop="suspiciousSymptomCount" label="有肺结核可疑症状者人数" />
            <el-table-column prop="chestXrayCount" label="胸片检查人数" />
            <el-table-column prop="chestXrayAbnormalCount" label="胸片异常人数" />
            <el-table-column prop="ppdTestCount" label="结核菌素试验检测人数" />
            <el-table-column prop="ppdPositive1" label="PPD+人数" />
            <el-table-column prop="ppdPositive2" label="PPD++人数" />
            <el-table-column prop="ppdPositive3" label="PPD+++人数" />
            <el-table-column prop="ppdPositiveTotal" label="PPD阳性总人数（+、++、+++合计）" />
            <el-table-column prop="ecNegative" label="EC阴性人数" />
            <el-table-column prop="ecPositive" label="EC阳性人数" />
            <el-table-column prop="igraPositive" label="IGRA阳性人数" />
            <el-table-column prop="igraNegative" label="IGRA阴性人数" />
            <el-table-column prop="tbPatientCount" label="肺结核/疑似肺结核患者人数" />
            <el-table-column prop="remark" label="备注" />
          </el-table>
        </el-tab-pane>

        <!-- 区县统计表 -->
        <el-tab-pane label="区县统计表" name="district">
          <div class="mb-3 flex justify-end">
            <el-button type="success" v-permission="'statistics:export'" @click="handleExportDistrict">
              导出 Excel
            </el-button>
          </div>
          <el-table v-loading="districtLoading" :data="districtData" border stripe max-height="600" show-summary>
            <el-table-column prop="district" label="区/县" fixed />
            <el-table-column prop="actualScreenCount" label="实际筛查人数" />
            <el-table-column prop="closeContactCount" label="与肺结核患者密切接触的人数" />
            <el-table-column prop="suspiciousSymptomCount" label="有肺结核可疑症状者人数" />
            <el-table-column prop="chestXrayCount" label="胸片检查人数" />
            <el-table-column prop="chestXrayAbnormalCount" label="胸片异常人数" />
            <el-table-column prop="ppdTestCount" label="结核菌素试验检测人数" />
            <el-table-column prop="ppdPositive1" label="PPD+人数" />
            <el-table-column prop="ppdPositive2" label="PPD++人数" />
            <el-table-column prop="ppdPositive3" label="PPD+++人数" />
            <el-table-column prop="ppdPositiveTotal" label="PPD阳性总人数（+、++、+++合计）" />
            <el-table-column prop="ecNegative" label="EC阴性人数" />
            <el-table-column prop="ecPositive" label="EC阳性人数" />
            <el-table-column prop="igraPositive" label="IGRA阳性人数" />
            <el-table-column prop="igraNegative" label="IGRA阴性人数" />
            <el-table-column prop="tbPatientCount" label="肺结核/疑似肺结核患者人数" />
            <el-table-column prop="remark" label="备注" />
          </el-table>
        </el-tab-pane>

        <!-- P6 新增：信息总表导出 -->
        <el-tab-pane label="信息总表导出" name="aggregate">
          <el-alert
            title="信息总表聚合全部来源数据（学生/重点/常规/大疫情/推介/密接），导出 Excel 文件。默认导出全部来源，可按数据来源、姓名、证件号筛选。"
            type="info" :closable="false" style="margin-bottom: 16px"
          />
          <!-- 患者信息总表 -->
          <el-card shadow="hover" style="margin-bottom: 16px">
            <template #header>
              <span style="font-weight: 600">患者信息总表</span>
              <span style="font-size: 12px; color: #909399; margin-left: 8px">来源：学生 / 重点 / 常规 / 大疫情 / 推介 / 专病网 / 密接</span>
            </template>
            <el-form :model="patientFilterForm" inline style="margin-bottom: 12px">
              <el-form-item label="数据来源">
                <el-select v-model="patientFilterForm.populationType" style="width: 140px">
                  <el-option v-for="opt in PATIENT_POP_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="patientFilterForm.name" placeholder="姓名筛选" clearable style="width: 120px" />
              </el-form-item>
              <el-form-item label="证件号">
                <el-input v-model="patientFilterForm.idNumber" placeholder="证件号筛选" clearable style="width: 180px" />
              </el-form-item>
              <el-form-item label="归档">
                <el-select v-model="patientFilterForm.archived" placeholder="全部" clearable style="width: 90px">
                  <el-option label="未归档" :value="0" />
                  <el-option label="已归档" :value="1" />
                </el-select>
              </el-form-item>
            </el-form>
            <p style="color: #606266; font-size: 13px; margin-bottom: 12px">
              包含字段：数据来源、基本信息、诊断结果、通知单状态、首次随访、后续随访次数、服药管理、归档状态、归档时间
            </p>
            <el-button type="primary" @click="handleExportAllPatients">导出患者信息总表</el-button>
          </el-card>

          <!-- 潜伏感染者信息总表 -->
          <el-card shadow="hover">
            <template #header>
              <span style="font-weight: 600">潜伏感染者信息总表</span>
              <span style="font-size: 12px; color: #909399; margin-left: 8px">来源：学生 / 重点 / 常规 / 大疫情 / 推介（密接潜伏独立管理，不计入此表）</span>
            </template>
            <el-form :model="latentFilterForm" inline style="margin-bottom: 12px">
              <el-form-item label="数据来源">
                <el-select v-model="latentFilterForm.populationType" style="width: 140px">
                  <el-option v-for="opt in LATENT_POP_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="latentFilterForm.name" placeholder="姓名筛选" clearable style="width: 120px" />
              </el-form-item>
              <el-form-item label="证件号">
                <el-input v-model="latentFilterForm.idNumber" placeholder="证件号筛选" clearable style="width: 180px" />
              </el-form-item>
              <el-form-item label="归档">
                <el-select v-model="latentFilterForm.archived" placeholder="全部" clearable style="width: 90px">
                  <el-option label="未归档" :value="0" />
                  <el-option label="已归档" :value="1" />
                </el-select>
              </el-form-item>
            </el-form>
            <p style="color: #606266; font-size: 13px; margin-bottom: 12px">
              包含字段：数据来源、基本信息、感染筛查结果、追踪状态、诊断结果、通知单状态、督导表状态、预防性治疗信息、治疗阶段、归档状态
            </p>
            <el-button type="primary" @click="handleExportAllLatent">导出潜伏感染者信息总表</el-button>
          </el-card>
        </el-tab-pane>

        <!-- 筛查问卷 -->
        <el-tab-pane label="筛查问卷" name="questionnaire">
          <QuestionnairePanel />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 自定义字段导出弹窗 -->
    <el-dialog v-model="customDialogVisible" title="自定义字段导出" width="480px">
      <el-form label-width="90px">
        <el-form-item label="人群类型">
          <el-select v-model="customPopType" style="width: 100%">
            <el-option v-for="opt in categoryPopOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择字段">
          <el-checkbox-group v-model="selectedCustomFields">
            <div class="field-grid">
              <el-checkbox v-for="f in CUSTOM_FIELD_OPTIONS" :key="f.value" :label="f.value">
                {{ f.label }}
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customDialogVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="handleExportCustom">
          导出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.mb-3 {
  margin-bottom: 12px;
}
.mb-4 {
  margin-bottom: 16px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
</style>
