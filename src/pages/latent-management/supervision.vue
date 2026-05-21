<script lang="ts" setup>
import SupervisionFormDialog from "@@/components/SupervisionFormDialog.vue"
import { usePagination } from "@@/composables/usePagination"
import { getPopulationTypeLabel, getPopulationTypeTagType, getSuspectedConfirmDiagnosisLabel } from "@@/constants/disease"
import { getLatentAggregateListApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const FETCH_ALL_SIZE = 10000

const searchForm = reactive({
  name: "",
  idNumber: "",
  archived: undefined as number | undefined,
  populationType: ""
})

/** 督导表管理：仅展示经诊断分流为「潜伏感染者」的记录 */
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
  searchForm.archived = undefined
  searchForm.populationType = ""
  handleSearch()
}

onMounted(fetchData)
watch([() => paginationData.currentPage, () => paginationData.pageSize], fetchData)

function getSupervisionStatusLabel(status?: number) {
  if (status === 2) return "已归档"
  if (status === 1) return "已提交"
  return "待填写"
}

function getSupervisionStatusType(status?: number): "success" | "warning" | "info" {
  if (status === 2) return "success"
  if (status === 1) return "info"
  return "warning"
}

const supervisionDialogVisible = ref(false)
const supervisionRow = ref<any>(null)

function openSupervision(row: any) {
  supervisionRow.value = row
  supervisionDialogVisible.value = true
}
</script>

<template>
  <div class="app-container">
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
            <el-option label="大疫情" value="epidemic" />
            <el-option label="推介" value="referral" />
          </el-select>
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

    <el-card shadow="never" style="margin-top:10px">
      <el-table v-loading="loading" :data="tableData" border stripe>
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
        <el-table-column label="确认诊断">
          <template #default="{ row }">
            {{ getSuspectedConfirmDiagnosisLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="督导表状态">
          <template #default="{ row }">
            <el-tag :type="getSupervisionStatusType(row.supervisionStatus)" size="small">
              {{ getSupervisionStatusLabel(row.supervisionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'latentManagement:supervision'"
              type="primary"
              link
              size="small"
              :disabled="row.archived === 1"
              @click="openSupervision(row)"
            >
              {{ row.supervisionStatus >= 1 ? "查看/编辑督导表" : "填写督导表" }}
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

    <SupervisionFormDialog
      v-model="supervisionDialogVisible"
      :latent-row="supervisionRow"
      @success="fetchData"
    />
  </div>
</template>
