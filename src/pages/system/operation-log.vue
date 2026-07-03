<script lang="ts" setup>
import type { OperationLogItem } from "./apis/operation-log"
import { usePagination } from "@@/composables/usePagination"
import {
  OP_LOG_MODULE_OPTIONS,
  OP_LOG_TYPE_LABEL,
  OP_LOG_TYPE_OPTIONS,
  ROLE_MAP
} from "@@/constants/disease"
import { Download } from "@element-plus/icons-vue"
import { exportOperationLogApi, getOperationLogListApi } from "./apis/operation-log"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<OperationLogItem[]>([])
const total = ref(0)

const searchForm = reactive({
  opType: undefined as string | undefined,
  opModule: undefined as string | undefined,
  userName: "",
  keyword: "",
  // 默认查近 3 个月（按方案 v1.2 §10.3 决策）
  timeRange: getDefaultTimeRange() as [Date, Date] | undefined
})

function getDefaultTimeRange(): [Date, Date] {
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 3)
  return [start, end]
}

function buildParams() {
  const range = searchForm.timeRange
  const startTime = range?.[0]
  const endTime = range?.[1]
  return {
    opType: searchForm.opType,
    opModule: searchForm.opModule,
    userName: searchForm.userName?.trim() || undefined,
    keyword: searchForm.keyword?.trim() || undefined,
    startTime: startTime ? formatDateTime(startTime) : undefined,
    endTime: endTime ? formatDateTime(endTime) : undefined
  }
}

function formatDateTime(d: Date) {
  const pad = (n: number) => String(n).padStart(2, "0")
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getOperationLogListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      ...buildParams()
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
  searchForm.opType = undefined
  searchForm.opModule = undefined
  searchForm.userName = ""
  searchForm.keyword = ""
  searchForm.timeRange = getDefaultTimeRange()
  handleSearch()
}

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailRow = ref<OperationLogItem | null>(null)
const detailParams = ref("")

function viewDetail(row: OperationLogItem) {
  detailRow.value = row
  try {
    detailParams.value = row.requestParams ? JSON.stringify(JSON.parse(row.requestParams), null, 2) : ""
  } catch {
    detailParams.value = row.requestParams ?? ""
  }
  detailVisible.value = true
}

// ==================== 导出 ====================
const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportOperationLogApi(buildParams())
    const url = URL.createObjectURL(blob as unknown as Blob)
    const link = document.createElement("a")
    link.href = url
    link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success("导出成功")
  } finally {
    exporting.value = false
  }
}

function getOpTypeTagType(opType: string) {
  return OP_LOG_TYPE_OPTIONS.find(o => o.value === opType)?.tagType ?? "info"
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.opType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in OP_LOG_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务模块">
          <el-select v-model="searchForm.opModule" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in OP_LOG_MODULE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="searchForm.userName" placeholder="用户名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="动作或URL" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">操作日志</span>
          <el-button
            v-permission="'operationLog:export'"
            :icon="Download"
            type="success"
            :loading="exporting"
            @click="handleExport"
          >
            导出
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="createTime" label="时间" />
        <el-table-column label="操作人">
          <template #default="{ row }">
            <div>
              <div>{{ row.realName || "-" }}</div>
              <div class="text-xs text-gray-400">
                {{ row.userName }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag size="small">
              {{ ROLE_MAP[row.role] || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型">
          <template #default="{ row }">
            <el-tag :type="getOpTypeTagType(row.opType)" size="small">
              {{ OP_LOG_TYPE_LABEL[row.opType] || row.opType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opModule" label="模块" />
        <el-table-column prop="opAction" label="动作描述" show-overflow-tooltip />
        <el-table-column prop="requestUrl" label="URL" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" />
        <el-table-column label="结果">
          <template #default="{ row }">
            <el-tag :type="row.resultStatus === 1 ? 'success' : 'danger'" size="small">
              {{ row.resultStatus === 1 ? "成功" : "失败" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时">
          <template #default="{ row }">
            <span v-if="row.costMs != null">{{ row.costMs }} ms</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :total="total"
          :page-sizes="paginationData.pageSizes"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="720px">
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item label="时间">
          {{ detailRow.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ detailRow.realName }} ({{ detailRow.userName }})
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ OP_LOG_TYPE_LABEL[detailRow.opType] }}
        </el-descriptions-item>
        <el-descriptions-item label="模块">
          {{ detailRow.opModule || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="动作描述" :span="2">
          {{ detailRow.opAction || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="HTTP">
          {{ detailRow.requestMethod }} {{ detailRow.requestUrl }}
        </el-descriptions-item>
        <el-descriptions-item label="IP">
          {{ detailRow.ip }}
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">
          <span class="text-xs">{{ detailRow.userAgent || "-" }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="detailRow.resultStatus === 1 ? 'success' : 'danger'" size="small">
            {{ detailRow.resultStatus === 1 ? "成功" : "失败" }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ detailRow.costMs }} ms
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRow.errorMessage" label="错误信息" :span="2">
          <pre class="error-text">{{ detailRow.errorMessage }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailParams" label="请求参数" :span="2">
          <pre class="param-text">{{ detailParams }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.error-text,
.param-text {
  white-space: pre-wrap;
  word-break: break-all;
  background: #f5f7fa;
  padding: 8px 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 320px;
  overflow: auto;
}
.error-text {
  color: var(--el-color-danger);
}
</style>
