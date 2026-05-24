<script lang="ts" setup>
import PatientMedicationDialog from "@@/components/PatientMedicationDialog.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { resolveRegistrationNo } from "@@/utils/patient"
import { usePatientList } from "./composables/usePatientList"

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0)

const medicationDialogVisible = ref(false)
const medicationRow = ref<any>(null)

function openMedication(row: any) {
  medicationRow.value = row
  medicationDialogVisible.value = true
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
            <el-option label="密接" value="closeContact" />
            <el-option label="专病网" value="specialDisease" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

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
        <el-table-column label="登记号" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveRegistrationNo(row) || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别" />
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="诊断结果" />
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button
              v-permission="'patientManagement:medication'"
              type="primary"
              link
              size="small"
              :disabled="row.archived === 1"
              @click="openMedication(row)"
            >
              服药管理
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="取药情况" min-width="200" fixed="right">
          <template #default="{ row }">
            <div
              v-if="row.medicationPickTime || row.medicationChemotherapy || row.medicationDrugForm"
              class="medication-pickup-cell"
            >
              <div v-if="row.medicationPickTime">
                时间：{{ row.medicationPickTime }}
              </div>
              <div v-if="row.medicationChemotherapy">
                药品：{{ row.medicationChemotherapy }}
              </div>
              <div v-if="row.medicationDrugForm">
                数量：{{ row.medicationDrugForm }}
              </div>
            </div>
            <span v-else class="text-gray-400">-</span>
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

    <PatientMedicationDialog
      v-model:visible="medicationDialogVisible"
      :patient-row="medicationRow"
      @success="fetchData"
    />
  </div>
</template>

<style lang="scss" scoped>
.medication-pickup-cell {
  line-height: 1.6;
  font-size: 13px;
}
</style>
