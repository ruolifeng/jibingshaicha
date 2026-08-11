<script lang="ts" setup>
import LatentMedicationDialog from "@@/components/LatentMedicationDialog.vue"
import LatentMedicationPickupDialog from "@@/components/LatentMedicationPickupDialog.vue"
import PatientMedicationPickupDetailDialog from "@@/components/PatientMedicationPickupDetailDialog.vue"
import {
  getLatentPopulationDisplayLabel,
  getPopulationTypeTagType,
  LATENT_MANUAL_POPULATION_TYPE_OPTIONS
} from "@@/constants/disease"
import { getLatentTransferStatusLabel, isLatentTransferLocked } from "@@/utils/latent"
import {
  canEditMedicationPickup,
  formatMedicationPickupDrugs,
  formatMedicationPickupQuantities,
  LATENT_MEDICATION_PAGE_PERMISSIONS,
  LATENT_MEDICATION_PICKUP_PERMISSIONS,
  LATENT_MEDICATION_PICKUP_VIEW_PERMISSIONS
} from "@@/utils/medicationPickup"
import { useUserStore } from "@/pinia/stores/user"
import { getLatentMedicationPickupListApi } from "./apis"
import { useLatentOverviewList } from "./composables/useLatentOverviewList"

const userStore = useUserStore()

/** 填写/修改领药 */
const canManagePickup = computed(() =>
  LATENT_MEDICATION_PICKUP_PERMISSIONS.some(code => userStore.hasPermission(code))
)

/** 查看领药摘要与记录（服药管理或填写领药） */
const canViewPickup = computed(() =>
  LATENT_MEDICATION_PICKUP_VIEW_PERMISSIONS.some(code => userStore.hasPermission(code))
)

const {
  paginationData,
  handleCurrentChange,
  handleSizeChange,
  getTableIndex,
  loading,
  tableData,
  total,
  searchForm,
  fetchData,
  handleSearch,
  handleReset
} = useLatentOverviewList()

const medicationDialogVisible = ref(false)
const medicationRow = ref<any>(null)

const pickupDialogVisible = ref(false)
const pickupRow = ref<any>(null)
const editPickup = ref<Record<string, any> | null>(null)

const historyVisible = ref(false)
const historyList = ref<any[]>([])
const historyLatentName = ref("")
const historyLatent = ref<any>(null)
const historyDialogTitle = computed(() => `${historyLatentName.value} - 领药记录`)

const detailVisible = ref(false)
const detailRecord = ref<Record<string, any> | null>(null)

function openMedication(row: any) {
  medicationRow.value = row
  medicationDialogVisible.value = true
}

function hasPickupData(row: Record<string, any>) {
  return (row.medicationPickupCount ?? 0) > 0
}

function canAddPickup(row: Record<string, any>) {
  return row.archived !== 1 && !isLatentTransferLocked(row)
}

function openPickup(row: any) {
  pickupRow.value = row
  editPickup.value = null
  pickupDialogVisible.value = true
}

async function viewHistory(row: any) {
  historyLatent.value = row
  historyLatentName.value = row.name
  const { data } = await getLatentMedicationPickupListApi(row.id)
  historyList.value = data || []
  historyVisible.value = true
}

async function refreshHistoryList() {
  if (!historyLatent.value) return
  const { data } = await getLatentMedicationPickupListApi(historyLatent.value.id)
  historyList.value = data || []
}

function openEdit(record: Record<string, any>) {
  editPickup.value = record
  pickupRow.value = historyLatent.value
  pickupDialogVisible.value = true
}

async function onPickupSaved() {
  await refreshHistoryList()
  fetchData()
}

function viewDetail(record: Record<string, any>) {
  detailRecord.value = record
  detailVisible.value = true
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
            <el-option
              v-for="item in LATENT_MANUAL_POPULATION_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
        <el-table-column type="index" label="#" :index="getTableIndex" />
        <el-table-column label="数据来源" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getLatentPopulationDisplayLabel(row.populationType, row.crowdCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="90" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="idNumber" label="证件号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" min-width="120" />
        <el-table-column prop="infectionResult" label="感染筛查结果" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" :width="canViewPickup ? 100 : 120">
          <template #default="{ row }">
            <template v-if="!isLatentTransferLocked(row)">
              <el-button
                v-permission="[...LATENT_MEDICATION_PAGE_PERMISSIONS]"
                type="primary"
                link
                size="small"
                :disabled="row.archived === 1"
                @click="openMedication(row)"
              >
                服药管理
              </el-button>
            </template>
            <el-tag
              v-else
              :type="row.archiveRemark === '已转出' ? 'info' : 'warning'"
              size="small"
            >
              {{ getLatentTransferStatusLabel(row.archiveRemark) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canViewPickup" label="领药情况" min-width="260" fixed="right">
          <template #default="{ row }">
            <div v-if="hasPickupData(row)" class="medication-pickup-cell">
              <div>共 {{ row.medicationPickupCount }} 次</div>
              <div v-if="row.medicationPickTime">
                最近：{{ row.medicationPickTime }}
              </div>
              <div v-if="row.medicationChemotherapy">
                药品：{{ row.medicationChemotherapy }}
              </div>
              <div v-if="row.medicationDrugForm">
                数量：{{ row.medicationDrugForm }}
              </div>
            </div>
            <span v-else class="text-gray-400">未录入</span>
            <div class="pickup-actions">
              <el-button
                v-if="canManagePickup && canAddPickup(row)"
                v-permission="[...LATENT_MEDICATION_PICKUP_PERMISSIONS]"
                type="primary"
                link
                size="small"
                @click="openPickup(row)"
              >
                填写领药
              </el-button>
              <el-button
                v-if="hasPickupData(row)"
                type="info"
                link
                size="small"
                @click="viewHistory(row)"
              >
                查看记录
              </el-button>
            </div>
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

    <LatentMedicationDialog
      v-model:visible="medicationDialogVisible"
      :latent-row="medicationRow"
      @success="fetchData"
    />

    <LatentMedicationPickupDialog
      v-if="pickupRow"
      v-model:visible="pickupDialogVisible"
      :latent-row="pickupRow"
      :initial-data="editPickup"
      @success="onPickupSaved"
      @update:visible="(v) => { if (!v) editPickup = null }"
    />

    <el-dialog
      v-model="historyVisible"
      :title="historyDialogTitle"
      width="920px"
      append-to-body
    >
      <el-table :data="historyList" border stripe>
        <el-table-column prop="pickupSeq" label="第几次" width="80" />
        <el-table-column prop="pickupTime" label="领取时间" width="120" />
        <el-table-column label="药品及用量" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatMedicationPickupDrugs(row.drugs) }}
          </template>
        </el-table-column>
        <el-table-column label="领取数量" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatMedicationPickupQuantities(row.drugs, row.quantity, row.quantityUnit) || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="dispensingUnit" label="发药单位" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="canEditMedicationPickup(userStore.userRole, row) && !isLatentTransferLocked(historyLatent)"
              v-permission="[...LATENT_MEDICATION_PICKUP_PERMISSIONS]"
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
      <el-empty v-if="!historyList.length" description="暂无领药记录" />
    </el-dialog>

    <PatientMedicationPickupDetailDialog
      v-model:visible="detailVisible"
      :record="detailRecord"
      :patient-name="historyLatentName"
    />
  </div>
</template>

<style lang="scss" scoped>
.medication-pickup-cell {
  line-height: 1.6;
  font-size: 13px;
  margin-bottom: 4px;
}

.pickup-actions {
  margin-top: 2px;
}
</style>
