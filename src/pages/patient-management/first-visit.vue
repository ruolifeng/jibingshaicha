<script lang="ts" setup>
import FirstVisitDetailDialog from "@@/components/FirstVisitDetailDialog.vue"
import PatientFirstVisitFormDialog from "@@/components/PatientFirstVisitFormDialog.vue"
import PrintFirstVisit from "@@/components/PrintFirstVisit.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_OPTIONS } from "@@/constants/disease"
import { downloadBlob } from "@@/utils/download"
import { isPatientTransferLocked, getPatientTransferStatusLabel } from "@@/utils/patient"
import { exportPatientFirstVisitsApi, getFirstVisitDetailApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"
import { useUserStore } from "@/pinia/stores/user"

const userStore = useUserStore()
const canEditFirstVisitPerm = computed(() => userStore.hasPermission("patientManagement:firstVisit:edit"))

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0, { firstVisitSearch: true })

const selectedRows = ref<any[]>([])
const exporting = ref(false)

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function handleExportSelected() {
  const ids = selectedRows.value.map(r => r.id).filter(Boolean)
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的患者")
    return
  }
  try {
    await ElMessageBox.confirm(`确认导出选中的 ${ids.length} 位患者的首次入户随访信息吗？`, "导出确认", {
      confirmButtonText: "确认导出",
      cancelButtonText: "取消",
      type: "warning"
    })
    exporting.value = true
    const blob = await exportPatientFirstVisitsApi(ids)
    downloadBlob(blob as unknown as Blob, "首次入户随访.xlsx")
    ElMessage.success("导出成功")
  } catch (err: any) {
    if (err !== "cancel") ElMessage.error("导出失败")
  } finally {
    exporting.value = false
  }
}

const firstVisitDialogVisible = ref(false)
const firstVisitDetailVisible = ref(false)
const firstVisitRow = ref<any>(null)
const firstVisitDetailData = ref<Record<string, any> | null>(null)
const printVisitVisible = ref(false)
const printVisitData = ref<Record<string, any> | null>(null)

function openFirstVisit(row: any) {
  firstVisitRow.value = row
  firstVisitDialogVisible.value = true
}

async function viewFirstVisit(row: any) {
  try {
    const { data } = await getFirstVisitDetailApi(row.id)
    if (data) {
      firstVisitRow.value = row
      firstVisitDetailData.value = data
      firstVisitDetailVisible.value = true
    } else {
      ElMessage.info("暂无首次随访记录")
    }
  } catch { /* handled */ }
}

async function openPrintFirstVisit(row: any) {
  try {
    const { data } = await getFirstVisitDetailApi(row.id)
    if (!data) {
      ElMessage.info("暂无首次随访记录")
      return
    }
    firstVisitRow.value = row
    printVisitData.value = data
    printVisitVisible.value = true
  } catch { /* handled */ }
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
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="病原学结果">
          <el-select v-model="searchForm.diagnosisResult" placeholder="全部" clearable filterable style="width:140px">
            <el-option v-for="item in PATHOGEN_RESULT_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="填写时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="服药管理单位">
          <el-input
            v-model="searchForm.medicationManagementUnit"
            placeholder="请输入"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="searchForm.populationType" placeholder="全部" clearable style="width:140px">
            <el-option label="学生筛查" value="school" />
            <el-option label="重点人群" value="keyPopulation" />
            <el-option label="疫情筛查" value="regular" />
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

    <el-card shadow="never" style="margin-top:10px">
      <div style="margin-bottom: 10px">
        <el-button
          type="success"
          :loading="exporting"
          :disabled="!selectedRows.length"
          @click="handleExportSelected"
        >
          导出
        </el-button>
      </div>
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
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
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="病原学结果" />
        <el-table-column label="首次随访">
          <template #default="{ row }">
            <el-tag v-if="row.firstVisitStatus === 1" type="success" size="small">
              已完成
            </el-tag>
            <el-tag v-else-if="row.firstVisitStatus === 0" type="info" size="small">
              草稿
            </el-tag>
            <el-tag v-else type="warning" size="small">
              待填写
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <template v-if="!isPatientTransferLocked(row)">
              <el-button
                v-if="!row.hasFirstVisit"
                v-permission="'patientManagement:firstVisit'"
                type="primary"
                link
                size="small"
                :disabled="row.archived === 1"
                @click="openFirstVisit(row)"
              >
                填写首次随访
              </el-button>
              <el-button
                v-else-if="row.firstVisitEditable !== false && canEditFirstVisitPerm"
                type="primary"
                link
                size="small"
                :disabled="row.archived === 1"
                @click="openFirstVisit(row)"
              >
                编辑首次随访
              </el-button>
              <el-button
                v-else
                type="primary"
                link
                size="small"
                @click="viewFirstVisit(row)"
              >
                查看首次随访
              </el-button>
              <template v-if="row.hasFirstVisit">
                <el-button type="info" link size="small" @click="viewFirstVisit(row)">
                  查看
                </el-button>
                <el-button type="warning" link size="small" @click="openPrintFirstVisit(row)">
                  打印
                </el-button>
              </template>
            </template>
            <template v-else>
              <el-button v-if="row.hasFirstVisit" type="info" link size="small" @click="viewFirstVisit(row)">
                查看
              </el-button>
              <el-tag :type="row.archiveRemark === '已转出' ? 'info' : 'warning'" size="small">
                {{ getPatientTransferStatusLabel(row.archiveRemark) }}
              </el-tag>
            </template>
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

    <PatientFirstVisitFormDialog
      v-model:visible="firstVisitDialogVisible"
      :patient-row="firstVisitRow"
      @success="fetchData"
    />

    <FirstVisitDetailDialog
      v-model:visible="firstVisitDetailVisible"
      :visit-data="firstVisitDetailData"
      :patient-name="firstVisitRow?.name"
    />

    <PrintFirstVisit
      v-if="printVisitData"
      :visible="printVisitVisible"
      :visit-data="printVisitData"
      :patient-name="firstVisitRow?.name"
      @update:visible="printVisitVisible = $event"
    />
  </div>
</template>
