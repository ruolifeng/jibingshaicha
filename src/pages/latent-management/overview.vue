<script lang="ts" setup>
import LatentRecordDetailDialog from "@@/components/LatentRecordDetailDialog.vue"
import LatentRecordEditDialog from "@@/components/LatentRecordEditDialog.vue"
import { TRACKING_STATUS_MAP, getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { exportAllLatentApi } from "./apis"
import { useLatentOverviewList } from "./composables/useLatentOverviewList"

const {
  paginationData, handleCurrentChange, handleSizeChange,
  loading, tableData, total, searchForm, fetchData, handleSearch, handleReset
} = useLatentOverviewList()

const detailVisible = ref(false)
const editVisible = ref(false)
const currentId = ref<number | null>(null)
const exporting = ref(false)

function openDetail(row: any) {
  currentId.value = row.id
  detailVisible.value = true
}

function openEdit(row: any) {
  currentId.value = row.id
  editVisible.value = true
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportAllLatentApi({
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      populationType: searchForm.populationType || undefined
    })
    downloadBlob(blob as unknown as Blob, "在管潜伏感染者信息总表.xlsx")
    ElMessage.success("导出成功")
  } catch {
    ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
      :title="`当前在管潜伏感染者共 ${total} 人`"
    />

    <el-card class="search-wrapper" shadow="never">
      <el-form inline>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="searchForm.idNumber" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width: 140px">
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

    <el-card shadow="never" style="margin-top: 10px">
      <div style="margin-bottom: 12px">
        <el-button
          v-permission="'latentManagement:overview'"
          type="success"
          :loading="exporting"
          @click="handleExport"
        >
          导出
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="idNumber" label="证件号" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="infectionResult" label="感染筛查结果" show-overflow-tooltip />
        <el-table-column label="追踪状态">
          <template #default="{ row }">
            <el-tag
              :type="row.trackingStatus === 1 ? 'success' : row.trackingStatus === 2 ? 'warning' : 'info'"
              size="small"
            >
              {{ TRACKING_STATUS_MAP[row.trackingStatus] ?? "待追踪" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisFirst" label="诊断结果" show-overflow-tooltip />
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-tag :type="row.noticeSent ? 'success' : 'info'" size="small">
              {{ row.noticeSent ? "已发送" : "未发送" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据来源">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-permission="'latentManagement:edit'"
              type="warning"
              link
              size="small"
              @click="openEdit(row)"
            >
              修改
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

    <LatentRecordDetailDialog
      v-model:visible="detailVisible"
      :latent-id="currentId"
    />
    <LatentRecordEditDialog
      v-model:visible="editVisible"
      :latent-id="currentId"
      @success="fetchData"
    />
  </div>
</template>
