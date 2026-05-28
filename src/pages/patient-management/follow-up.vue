<script lang="ts" setup>
import FollowUpVisitDetailDialog from "@@/components/FollowUpVisitDetailDialog.vue"
import FollowUpVisitDialog from "@@/components/FollowUpVisitDialog.vue"
import PrintFollowUp from "@@/components/PrintFollowUp.vue"
import { getPopulationTypeLabel, getPopulationTypeTagType, PATHOGEN_RESULT_OPTIONS } from "@@/constants/disease"
import { canEditFollowUpVisit } from "@@/utils/followUpVisit"
import { followUpFormatters } from "@@/utils/followUpVisitFormat"
import { useUserStore } from "@/pinia/stores/user"
import { getFollowUpVisitListApi } from "./apis"
import { usePatientList } from "./composables/usePatientList"

const userStore = useUserStore()

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0)

const followUpDialogVisible = ref(false)
const followUpPatient = ref<any>(null)

function openFollowUp(row: any) {
  followUpPatient.value = row
  followUpDialogVisible.value = true
}

const historyVisible = ref(false)
const historyList = ref<any[]>([])
const historyPatientName = ref("")
const historyPatient = ref<any>(null)
const historyDialogTitle = computed(() => `${historyPatientName.value} - 随访记录`)

const editDialogVisible = ref(false)
const editVisit = ref<Record<string, any> | null>(null)

const detailVisible = ref(false)
const detailData = ref<Record<string, any> | null>(null)

const printVisible = ref(false)
const printData = ref<Record<string, any> | null>(null)
const printPatientName = ref("")

async function viewHistory(row: any) {
  historyPatient.value = row
  historyPatientName.value = row.name
  const { data } = await getFollowUpVisitListApi(row.id)
  historyList.value = data || []
  historyVisible.value = true
}

async function refreshHistoryList() {
  if (!historyPatient.value) return
  const { data } = await getFollowUpVisitListApi(historyPatient.value.id)
  historyList.value = data || []
}

function openEdit(record: Record<string, any>) {
  editVisit.value = record
  editDialogVisible.value = true
}

async function onEditSaved() {
  await refreshHistoryList()
  fetchData()
}

function viewDetail(row: Record<string, any>) {
  detailData.value = row
  detailVisible.value = true
}

function openPrint(row: Record<string, any>) {
  printData.value = row
  printPatientName.value = historyPatientName.value
  printVisible.value = true
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
        <el-form-item label="时间段">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
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
        <el-table-column prop="idNumber" label="证件号" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="diagnosisResult" label="病原学结果" />
        <el-table-column label="操作" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'patientManagement:followUp'" type="primary" link size="small"
              :disabled="row.archived === 1"
              @click="openFollowUp(row)"
            >
              填写后续随访
            </el-button>
            <el-button type="info" link size="small" @click="viewHistory(row)">
              查看记录
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

    <FollowUpVisitDialog
      v-if="followUpPatient"
      v-model:visible="followUpDialogVisible"
      :patient-id="followUpPatient.id"
      :patient-name="followUpPatient.name"
      :patient-row="followUpPatient"
      :population-type="followUpPatient.populationType"
      @saved="fetchData"
    />

    <FollowUpVisitDialog
      v-if="historyPatient && editVisit"
      v-model:visible="editDialogVisible"
      :patient-id="historyPatient.id"
      :patient-name="historyPatient.name"
      :patient-row="historyPatient"
      :population-type="historyPatient.populationType"
      :initial-data="editVisit"
      @saved="onEditSaved"
    />

    <el-dialog
      v-model="historyVisible"
      :title="historyDialogTitle"
      width="900px"
      append-to-body
    >
      <el-table :data="historyList" border stripe>
        <el-table-column prop="visitSeq" label="第几次" />
        <el-table-column prop="visitDate" label="随访日期" />
        <el-table-column prop="treatmentMonth" label="治疗月序" />
        <el-table-column label="随访方式">
          <template #default="{ row }">
            {{ followUpFormatters.visitMethod(row.visitMethod, row.visitMethodOther) }}
          </template>
        </el-table-column>
        <el-table-column prop="missedDoses" label="漏服次数" />
        <el-table-column prop="nextVisitDate" label="下次随访" />
        <el-table-column prop="doctorSignature" label="医生签名" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="canEditFollowUpVisit(userStore.userRole, row)"
              v-permission="'patientManagement:followUp'"
              type="warning"
              link
              size="small"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button type="info" link size="small" @click="openPrint(row)">
              打印
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyList.length" description="暂无随访记录" />
    </el-dialog>

    <FollowUpVisitDetailDialog
      v-model:visible="detailVisible"
      :visit-data="detailData"
      :patient-name="historyPatientName"
    />

    <PrintFollowUp
      v-if="printData"
      :visible="printVisible"
      :visit-data="printData"
      :patient-name="printPatientName"
      @update:visible="printVisible = $event"
    />
  </div>
</template>
