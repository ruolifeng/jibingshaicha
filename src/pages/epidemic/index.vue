<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { TRACKING_STATUS_MAP } from "@@/constants/disease"
import {
  getEpidemicListApi,
  importEpidemicDataApi,
  submitEpidemicDiagnosisApi,
  submitEpidemicXrayApi,
  trackEpidemicApi
} from "./apis"

defineOptions({ name: "EpidemicScreening" })

const activeTab = ref<"import" | "suspected">("import")

const uploading = ref(false)
const importResult = ref<{ count: number, batchNo: string } | null>(null)

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  name: "",
  idNumber: "",
  trackingStatus: undefined as number | undefined,
  archived: 0 as number | undefined
})

const trackDialogVisible = ref(false)
const trackRow = ref<any>(null)
const trackForm = reactive({
  status: undefined as number | undefined,
  remark: ""
})

const xrayDialogVisible = ref(false)
const xrayRow = ref<any>(null)
const xrayForm = reactive({
  hasChestXray: "",
  chestXrayDate: "",
  chestXrayResult: ""
})

const diagDialogVisible = ref(false)
const diagRow = ref<any>(null)
const diagForm = reactive({
  diagnosisResult: ""
})

const diagnosisOptions = ["排除", "疑似肺结核", "潜伏感染者", "确诊患者", "其他"]

function diagnosisDesc(result: string) {
  if (result === "排除") return "将归档"
  if (result === "疑似肺结核") return "保留在待诊断列表"
  if (result === "潜伏感染者") return "分流到潜伏感染者管理"
  if (result === "确诊患者") return "分流到患者管理（来源：大疫情）"
  if (result === "其他") return "归档"
  return ""
}

async function handleFileChange(uploadFile: any) {
  const file = uploadFile?.raw as File
  if (!file) {
    return
  }
  if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
    ElMessage.error("请上传 .xlsx 或 .xls 格式的大疫情表文件")
    return
  }
  uploading.value = true
  importResult.value = null
  try {
    const { data } = await importEpidemicDataApi(file)
    importResult.value = data
    ElMessage.success(`导入成功，共创建 ${data.count} 条待诊断记录`)
    activeTab.value = "suspected"
    await fetchData()
  } catch {
    ElMessage.error("导入失败，请确认文件格式是否符合大疫情表模板")
  } finally {
    uploading.value = false
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...searchForm
    }
    if (params.trackingStatus === undefined) {
      delete params.trackingStatus
    }
    if (params.archived === undefined) {
      delete params.archived
    }
    const { data } = await getEpidemicListApi(params)
    tableData.value = data.records ?? []
    total.value = data.total ?? 0
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
  searchForm.archived = 0
  handleSearch()
}

function openTrack(row: any) {
  trackRow.value = row
  trackForm.status = undefined
  trackForm.remark = ""
  trackDialogVisible.value = true
}

async function handleTrackSubmit() {
  if (trackForm.status === undefined) {
    ElMessage.warning("请选择追踪状态")
    return
  }
  await trackEpidemicApi({
    id: trackRow.value.id,
    status: trackForm.status,
    remark: trackForm.remark
  })
  ElMessage.success("追踪状态已更新")
  trackDialogVisible.value = false
  fetchData()
}

function openXray(row: any) {
  xrayRow.value = row
  xrayForm.hasChestXray = row.hasChestXray ?? ""
  xrayForm.chestXrayDate = row.chestXrayDate ?? ""
  xrayForm.chestXrayResult = row.chestXrayResult ?? ""
  xrayDialogVisible.value = true
}

async function handleXraySubmit() {
  if (!xrayForm.hasChestXray) {
    ElMessage.warning("请选择是否进行胸片检查")
    return
  }
  await submitEpidemicXrayApi({
    id: xrayRow.value.id,
    hasChestXray: xrayForm.hasChestXray,
    chestXrayDate: xrayForm.chestXrayDate,
    chestXrayResult: xrayForm.chestXrayResult
  })
  ElMessage.success("胸片结果已保存")
  xrayDialogVisible.value = false
  fetchData()
}

function openDiag(row: any) {
  diagRow.value = row
  diagForm.diagnosisResult = row.diagnosisResult ?? ""
  diagDialogVisible.value = true
}

async function handleDiagSubmit() {
  if (!diagForm.diagnosisResult) {
    ElMessage.warning("请选择诊断结果")
    return
  }
  await submitEpidemicDiagnosisApi({
    id: diagRow.value.id,
    diagnosisResult: diagForm.diagnosisResult
  })
  ElMessage.success("诊断结果已保存，数据已按结果自动分流")
  diagDialogVisible.value = false
  fetchData()
}

onMounted(fetchData)
watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)
</script>

<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="导入大疫情表" name="import">
        <el-card shadow="never">
          <el-alert
            type="info"
            :closable="false"
            style="margin-bottom: 20px"
          >
            <template #title>
              上传大疫情网（报告卡）导出文件（.xlsx / .xls），系统自动提取字段并创建待诊断记录。
            </template>
            <template #default>
              <ul style="margin: 6px 0 0 16px; line-height: 1.9; color: #606266; font-size: 13px">
                <li>提取字段：姓名、证件号、性别、出生日期、年龄、联系电话、现详细住址、病例分类、疾病名称、报告单位</li>
                <li>导入后进入「待诊断列表」，执行：追踪 -> 录入胸片 -> 录入诊断 -> 自动分流</li>
              </ul>
            </template>
          </el-alert>

          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            drag
            style="width: 100%"
          >
            <el-icon style="font-size: 48px; color: #409eff; margin-bottom: 12px">
              <upload-filled />
            </el-icon>
            <div style="font-size: 16px; color: #606266">
              拖拽大疫情表文件到此处，或
              <span style="color: #409eff; cursor: pointer">点击上传</span>
            </div>
            <div style="font-size: 12px; color: #909399; margin-top: 8px">
              支持 .xlsx / .xls 格式
            </div>
          </el-upload>

          <div v-if="uploading" style="text-align: center; margin-top: 16px">
            <el-icon class="is-loading" style="font-size: 24px">
              <loading />
            </el-icon>
            <span style="margin-left: 8px; color: #606266">正在解析并导入，请稍候...</span>
          </div>
        </el-card>

        <el-card v-if="importResult" shadow="never" style="margin-top: 16px">
          <template #header>
            <span class="text-lg font-bold">导入结果</span>
          </template>
          <el-result
            icon="success"
            :title="`成功导入 ${importResult.count} 条待诊断记录`"
            :sub-title="`批次号：${importResult.batchNo}`"
          >
            <template #extra>
              <el-button type="primary" @click="activeTab = 'suspected'">
                前往待诊断列表
              </el-button>
            </template>
          </el-result>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="待诊断列表" name="suspected">
        <el-card class="search-wrapper" shadow="never">
          <el-form inline>
            <el-form-item label="姓名">
              <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="证件号">
              <el-input v-model="searchForm.idNumber" placeholder="请输入" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="追踪状态">
              <el-select v-model="searchForm.trackingStatus" placeholder="全部" clearable style="width: 120px">
                <el-option
                  v-for="(label, val) in TRACKING_STATUS_MAP"
                  :key="val"
                  :label="label"
                  :value="Number(val)"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="归档状态">
              <el-select v-model="searchForm.archived" placeholder="全部" clearable style="width: 100px">
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

        <el-card shadow="never" style="margin-top: 10px">
          <el-table :data="tableData" v-loading="loading" border stripe>
            <el-table-column type="index" label="#" />
            <el-table-column prop="name" label="患者姓名" />
            <el-table-column prop="gender" label="性别" />
            <el-table-column prop="age" label="年龄" />
            <el-table-column prop="idNumber" label="证件号" />
            <el-table-column prop="phone" label="联系电话" />
            <el-table-column prop="caseCategory" label="病例分类" />
            <el-table-column prop="diseaseName" label="疾病名称" />
            <el-table-column prop="reportUnit" label="报告单位" show-overflow-tooltip />
            <el-table-column label="追踪状态">
              <template #default="{ row }">
                <el-tag
                  :type="row.trackingStatus === 1 ? 'success' : row.trackingStatus === 2 ? 'danger' : row.trackingStatus === 4 ? 'info' : 'warning'"
                  size="small"
                >
                  {{ TRACKING_STATUS_MAP[row.trackingStatus] ?? "待追踪" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="diagnosisResult" label="诊断结果" />
            <el-table-column label="归档">
              <template #default="{ row }">
                <el-tag :type="row.archived ? 'success' : 'info'" size="small">
                  {{ row.archived ? "已归档" : "进行中" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  size="small"
                  :disabled="row.trackingStatus === 1 || row.archived === 1"
                  @click="openTrack(row)"
                >
                  追踪
                </el-button>
                <el-button
                  type="primary"
                  link
                  size="small"
                  :disabled="row.trackingStatus !== 1 || row.archived === 1"
                  @click="openXray(row)"
                >
                  录入胸片
                </el-button>
                <el-button
                  type="warning"
                  link
                  size="small"
                  :disabled="!row.hasChestXray || row.archived === 1"
                  @click="openDiag(row)"
                >
                  录入诊断
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
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="trackDialogVisible" title="追踪操作" width="420px" append-to-body>
      <el-form :model="trackForm" label-width="90px">
        <el-form-item label="追踪状态" required>
          <el-radio-group v-model="trackForm.status">
            <el-radio :value="1">到位</el-radio>
            <el-radio :value="2">未到位</el-radio>
            <el-radio :value="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="trackForm.status === 2 || trackForm.status === 3" label="备注">
          <el-input v-model="trackForm.remark" type="textarea" :rows="3" placeholder="请填写原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTrackSubmit">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="xrayDialogVisible" title="录入胸片结果" width="480px" append-to-body>
      <el-form :model="xrayForm" label-width="140px">
        <el-form-item label="是否进行胸片检查" required>
          <el-radio-group v-model="xrayForm.hasChestXray">
            <el-radio value="是">是</el-radio>
            <el-radio value="否">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="xrayForm.hasChestXray === '是'">
          <el-form-item label="胸片检查日期">
            <el-date-picker v-model="xrayForm.chestXrayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="胸片结果">
            <el-select v-model="xrayForm.chestXrayResult" placeholder="请选择" style="width: 100%">
              <el-option label="正常" value="正常" />
              <el-option label="异常" value="异常" />
              <el-option label="未查" value="未查" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="xrayDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleXraySubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="diagDialogVisible" title="录入诊断结果" width="440px" append-to-body>
      <el-form :model="diagForm" label-width="100px">
        <el-form-item label="诊断结果" required>
          <el-select v-model="diagForm.diagnosisResult" placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in diagnosisOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="diagForm.diagnosisResult" label="分流说明">
          <el-text type="info">{{ diagnosisDesc(diagForm.diagnosisResult) }}</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="diagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDiagSubmit">保存并分流</el-button>
      </template>
    </el-dialog>
  </div>
</template>
