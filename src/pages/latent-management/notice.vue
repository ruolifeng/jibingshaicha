<script lang="ts" setup>
import LatentNoticeDetailDialog from "@@/components/LatentNoticeDetailDialog.vue"
import LatentNoticeFormDialog from "@@/components/LatentNoticeFormDialog.vue"
import ReferralDialog from "@@/components/ReferralDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { TRACKING_STATUS_MAP, getPopulationTypeLabel, getPopulationTypeTagType, getSuspectedConfirmDiagnosisLabel } from "@@/constants/disease"
import {
  getLatentAggregateListApi,
  closeCaseApi
} from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const FETCH_ALL_SIZE = 10000

const searchForm = reactive({
  name: "",
  idNumber: "",
  trackingStatus: undefined as number | undefined,
  archived: undefined as number | undefined,
  populationType: "" // 留空 = 全部来源（后端不过滤）
})

/** 聚合潜伏感染者通知单管理：仅展示经筛查诊断分流为「潜伏感染者」的记录（referralResult=latent）。
 *  不含密接来源；追踪/胸片/确认诊断在筛查管理「待诊断」完成，本页只负责通知单与后续流转。
 */
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: 1,
      size: FETCH_ALL_SIZE,
      referralResult: "latent",
      ...searchForm
    }
    if (!params.populationType) delete params.populationType
    const { data } = await getLatentAggregateListApi(params)
    const filtered = (data.records ?? []).filter((r: any) => r.populationType !== "closeContact")
    const start = (paginationData.currentPage - 1) * paginationData.pageSize
    const end = start + paginationData.pageSize
    tableData.value = filtered.slice(start, end)
    total.value = filtered.length
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

// ==================== 转诊 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
function openReferral(row: any) {
  referralRow.value = row
  referralDialogVisible.value = true
}

// ==================== 通知单 ====================
const noticeFormVisible = ref(false)
const noticeDetailVisible = ref(false)
const noticeRow = ref<any>(null)

function openNotice(row: any) {
  noticeRow.value = row
  noticeFormVisible.value = true
}

function viewNotice(row: any) {
  noticeRow.value = row
  noticeDetailVisible.value = true
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
        <el-table-column type="index" label="#" />
        <el-table-column label="数据来源">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" />
        <el-table-column label="追踪状态">
          <template #default="{ row }">
            <el-tag :type="row.trackingStatus === 1 ? 'success' : row.trackingStatus === 2 ? 'danger' : 'info'" size="small">
              {{ TRACKING_STATUS_MAP[row.trackingStatus] ?? "待追踪" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="确认诊断">
          <template #default="{ row }">
            {{ getSuspectedConfirmDiagnosisLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-button v-if="row.noticeSent" type="primary" link size="small" @click="viewNotice(row)">
              {{ row.name }}通知单
            </el-button>
            <span v-else class="text-gray-400">未发送</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button
              v-if="!row.noticeSent"
              v-permission="'latentManagement:notice'"
              type="success"
              link
              size="small"
              :disabled="row.archived === 1"
              @click="openNotice(row)"
            >
              发送通知单
            </el-button>
            <el-button
              v-if="row.noticeSent"
              v-permission="'latentManagement:notice'"
              type="primary"
              link
              size="small"
              @click="viewNotice(row)"
            >
              查看通知单
            </el-button>
            <el-button
              v-permission="'latentManagement:referral'"
              type="info"
              link
              size="small"
              :disabled="row.archived === 1"
              @click="openReferral(row)"
            >
              转诊
            </el-button>
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

    <LatentNoticeFormDialog
      v-model:visible="noticeFormVisible"
      :latent-row="noticeRow"
      @success="fetchData"
    />
    <LatentNoticeDetailDialog
      v-model:visible="noticeDetailVisible"
      :latent-row="noticeRow"
      @success="fetchData"
    />

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
