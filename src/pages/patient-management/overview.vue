<script lang="ts" setup>
import PatientRecordDetailDialog from "@@/components/PatientRecordDetailDialog.vue"
import PatientRecordEditDialog from "@@/components/PatientRecordEditDialog.vue"
import { NOTICE_STATUS_MAP } from "@@/constants/disease"
import { getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { exportAllPatientsApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"

const {
  paginationData, handleCurrentChange, handleSizeChange,
  loading, tableData, total, searchForm, fetchData, handleSearch, handleReset
} = usePatientList(0)

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
    const blob = await exportAllPatientsApi({
      name: searchForm.name || undefined,
      idNumber: searchForm.idNumber || undefined,
      populationType: searchForm.populationType || undefined
    })
    downloadBlob(blob as unknown as Blob, "在管患者信息总表.xlsx")
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
      :title="`当前在管患者共 ${total} 人`"
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
            <el-option label="密接" value="closeContact" />
            <el-option label="专病网" value="specialDisease" />
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
          v-permission="'patientManagement:overview'"
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
        <el-table-column prop="diagnosisResult" label="诊断结果" show-overflow-tooltip />
        <el-table-column label="通知单">
          <template #default="{ row }">
            <el-tag v-if="row.noticeStatus === 2" type="success" size="small">
              已确认
            </el-tag>
            <el-tag v-else-if="row.noticeStatus === 1" type="warning" size="small">
              {{ NOTICE_STATUS_MAP[1] }}
            </el-tag>
            <el-tag v-else-if="row.noticeStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <span v-else class="text-gray-400">未发送</span>
          </template>
        </el-table-column>
        <el-table-column label="首次随访">
          <template #default="{ row }">
            <el-tag :type="row.hasFirstVisit ? 'success' : 'warning'" size="small">
              {{ row.hasFirstVisit ? "已完成" : "待填写" }}
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
              v-permission="'patientManagement:edit'"
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

    <PatientRecordDetailDialog
      v-model:visible="detailVisible"
      :patient-id="currentId"
    />
    <PatientRecordEditDialog
      v-model:visible="editVisible"
      :patient-id="currentId"
      @success="fetchData"
    />
  </div>
</template>
