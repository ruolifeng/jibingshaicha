<script lang="ts" setup>
import { downloadBlob } from "@@/utils/download"
import { cleanScreeningDataApi, downloadCleaningResultApi, matchSchoolDataApi } from "./apis/index"
import { downloadTemplateApi } from "@/pages/patient-management/apis/index"

const templateDownloading = ref<string | null>(null)

const TEMPLATE_OPTIONS = [
  { label: "学生筛查数据模板", type: "school" },
  { label: "重点人群筛查数据模板", type: "keyPopulation" },
  { label: "疫情筛查数据模板", type: "regular" }
]

async function handleDownloadTemplate(type: string, label: string) {
  templateDownloading.value = type
  try {
    const blob = await downloadTemplateApi(type)
    downloadBlob(blob as unknown as Blob, `${label}.xlsx`)
    ElMessage.success("模板下载成功")
  } catch {
    ElMessage.error("模板下载失败")
  } finally {
    templateDownloading.value = null
  }
}

type PopulationType = "school" | "keyPopulation" | "closeContact"

const loading = ref(false)
const matching = ref(false)
const selectedType = ref<PopulationType>("school")
const selectedFile = ref<File | null>(null)
const matchFile = ref<File | null>(null)
const result = ref<{
  totalCount: number
  abnormalCount: number
  fileId: string
  fileName: string
  errors: string[]
} | null>(null)

const typeOptions: Array<{ label: string, value: PopulationType }> = [
  { label: "学校人群", value: "school" },
  { label: "重点人群", value: "keyPopulation" },
  { label: "密接人群", value: "closeContact" }
]

function onFileChange(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) return
  if (!(file.name.endsWith(".xlsx") || file.name.endsWith(".xls"))) {
    ElMessage.error("请上传 .xlsx 或 .xls 文件")
    return
  }
  selectedFile.value = file
  result.value = null
}

function onMatchFileChange(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) return
  if (!(file.name.endsWith(".xlsx") || file.name.endsWith(".xls"))) {
    ElMessage.error("请上传 .xlsx 或 .xls 文件")
    return
  }
  matchFile.value = file
}

async function handleMatchSchool() {
  if (!matchFile.value) {
    ElMessage.warning("请先选择需要匹配的学生筛查 Excel")
    return
  }
  matching.value = true
  try {
    const { data } = await matchSchoolDataApi(matchFile.value)
    const blob = await downloadCleaningResultApi(data.fileId)
    downloadBlob(blob as unknown as Blob, data.fileName || "学生筛查数据匹配结果.xlsx")
    ElMessage.success(`数据匹配完成，共生成 ${data.totalCount} 条`)
  } catch {
    ElMessage.error("数据匹配失败")
  } finally {
    matching.value = false
  }
}

async function handleClean() {
  if (!selectedFile.value) {
    ElMessage.warning("请先选择需要清洗的 Excel 文件")
    return
  }
  loading.value = true
  try {
    const { data } = await cleanScreeningDataApi(selectedType.value, selectedFile.value)
    result.value = data
    ElMessage.success("数据清洗完成")
  } finally {
    loading.value = false
  }
}

async function handleDownload() {
  if (!result.value?.fileId) return
  try {
    const blob = await downloadCleaningResultApi(result.value.fileId)
    downloadBlob(blob as unknown as Blob, result.value.fileName || "数据清洗结果.xlsx")
    ElMessage.success("下载成功")
  } catch {
    ElMessage.error("下载失败")
  }
}
</script>

<template>
  <div class="app-container">
    <!-- ==================== 模板下载区（P7 新增） ==================== -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="text-lg font-bold">数据导入模板下载</span>
      </template>
      <el-alert
        type="info"
        :closable="false"
        title="下载标准模板后，按列头格式填写数据，再到对应筛查管理模块上传即可。"
        class="mb-4"
      />
      <div style="display: flex; gap: 12px; flex-wrap: wrap">
        <el-card
          v-for="tpl in TEMPLATE_OPTIONS"
          :key="tpl.type"
          shadow="hover"
          style="min-width: 200px; cursor: pointer; border: 1px solid #e4e7ed"
        >
          <div style="text-align: center; padding: 8px 0">
            <el-icon style="font-size: 32px; color: #67c23a; margin-bottom: 8px">
              <document />
            </el-icon>
            <p style="font-size: 14px; color: #303133; margin: 4px 0 12px">{{ tpl.label }}</p>
            <el-button
              type="success"
              size="small"
              :loading="templateDownloading === tpl.type"
              @click="handleDownloadTemplate(tpl.type, tpl.label)"
            >
              下载模板
            </el-button>
          </div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="text-lg font-bold">数据匹配</span>
      </template>
      <el-alert
        type="info"
        :closable="false"
        title="用于将其它学生筛查 Excel 按表头匹配为本系统学生筛查模板，生成后可到筛查管理上传。目前仅支持学生筛查。"
        class="mb-4"
      />
      <el-form label-width="120px">
        <el-form-item label="匹配类型">
          <el-tag type="success">
            学生筛查
          </el-tag>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            accept=".xlsx,.xls"
            :limit="1"
            :on-change="onMatchFileChange"
          >
            <el-button type="primary">
              选择 Excel
            </el-button>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="matching" @click="handleMatchSchool">
            开始匹配并下载
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 数据清洗区（原有功能） ==================== -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="text-lg font-bold">数据清洗</span>
      </template>

      <el-alert
        type="info"
        :closable="false"
        title="用于筛查数据上传前的异常识别：系统会校验特殊字段并将异常单元格标黄，同时新增“异常原因”列，方便导出后筛选修正。"
        class="mb-4"
      />

      <el-form label-width="120px">
        <el-form-item label="数据类型">
          <el-radio-group v-model="selectedType">
            <el-radio-button v-for="item in typeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            accept=".xlsx,.xls"
            :limit="1"
            :on-change="onFileChange"
          >
            <el-button type="primary">
              选择 Excel
            </el-button>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="loading" @click="handleClean">
            开始清洗与验证
          </el-button>
          <el-button :disabled="!result" @click="handleDownload">
            下载清洗结果
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" shadow="never">
      <template #header>
        <span class="text-lg font-bold">清洗结果</span>
      </template>

      <el-descriptions :column="3" border class="mb-4">
        <el-descriptions-item label="总数据行数">
          {{ result.totalCount }}
        </el-descriptions-item>
        <el-descriptions-item label="异常行数">
          <el-tag :type="result.abnormalCount > 0 ? 'warning' : 'success'">
            {{ result.abnormalCount }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="结果文件">
          {{ result.fileName }}
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        :title="result.abnormalCount > 0 ? '已识别异常并完成标黄，可下载后按“异常原因”列筛选修正。' : '未发现异常数据。'"
        :type="result.abnormalCount > 0 ? 'warning' : 'success'"
        :closable="false"
        class="mb-3"
      />

      <el-table v-if="result.errors.length > 0" :data="result.errors.map((msg, idx) => ({ idx: idx + 1, msg }))" border max-height="360">
        <el-table-column prop="idx" label="#" />
        <el-table-column prop="msg" label="异常详情" />
      </el-table>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.mb-3 {
  margin-bottom: 12px;
}
.mb-4 {
  margin-bottom: 16px;
}
</style>
