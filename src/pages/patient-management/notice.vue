<script lang="ts" setup>
import ReferralDialog from "@@/components/ReferralDialog.vue"
import { TREATMENT_PLAN_OPTIONS, CROWD_CATEGORY_OPTIONS, getPopulationTypeLabel, getPopulationTypeTagType } from "@@/constants/disease"
import { usePatientList } from "./composables/usePatientList"
import { sendPatientNoticeApi, getNoticeListByBizApi, deletePatientApi } from "./apis"

const { paginationData, handleCurrentChange, handleSizeChange, loading, tableData, total, searchForm, fetchData, handleSearch, handleReset } = usePatientList(0)

// ==================== 发送通知单 ====================
const noticeDialogVisible = ref(false)
const noticeRow = ref<any>(null)
const noticeForm = reactive({
  currentAddress: "", householdAddress: "", idNumber: "", gender: "",
  birthDate: "", age: "", ethnicity: "", crowdCategory: "", treatmentPlan: ""
})
function openNotice(row: any) {
  noticeRow.value = row
  Object.assign(noticeForm, {
    currentAddress: row.currentAddress ?? "",
    householdAddress: row.householdAddress ?? "",
    idNumber: row.idNumber ?? "",
    gender: row.gender ?? "",
    birthDate: row.birthDate ?? "",
    age: row.age ?? "",
    ethnicity: row.ethnicity ?? "",
    crowdCategory: "",
    treatmentPlan: ""
  })
  noticeDialogVisible.value = true
}
async function handleNoticeSend() {
  if (!noticeForm.treatmentPlan) { ElMessage.warning("请选择治疗方案"); return }
  await sendPatientNoticeApi({
    bizId: noticeRow.value.id,
    noticeType: "patient",
    populationType: noticeRow.value.populationType,
    name: noticeRow.value.name,
    ...noticeForm
  })
  ElMessage.success("患者通知单已发送")
  noticeDialogVisible.value = false
  fetchData()
}

// ==================== 查看通知单 ====================
const noticeDetailVisible = ref(false)
const noticeDetail = ref<any>(null)
async function viewNotice(row: any) {
  const notices = (await getNoticeListByBizApi(row.id, "patient")).data
  if (notices && notices.length > 0) {
    noticeDetail.value = notices[0]
    noticeDetailVisible.value = true
  } else {
    ElMessage.info("暂无通知单记录")
  }
}

// ==================== 删除 ====================
async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确认删除患者 ${row.name} 及其所有关联数据？此操作不可恢复。`, "警告", { type: "error" })
  await deletePatientApi(row.id)
  ElMessage.success("已删除")
  fetchData()
}

// ==================== 转诊 ====================
const referralDialogVisible = ref(false)
const referralRow = ref<any>(null)
function openReferral(row: any) { referralRow.value = row; referralDialogVisible.value = true }
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
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="数据来源" width="100">
          <template #default="{ row }">
            <el-tag :type="getPopulationTypeTagType(row.populationType)" size="small">
              {{ getPopulationTypeLabel(row.populationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="idNumber" label="证件号" width="170" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="diagnosisResult" label="诊断结果" width="110" />
        <el-table-column label="患者通知单" width="130">
          <template #default="{ row }">
            <el-button v-if="row.noticeSent" type="primary" link size="small" @click="viewNotice(row)">
              {{ row.name }}通知单
            </el-button>
            <span v-else class="text-gray-400">未发送</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'patientManagement:notice'" type="primary" link size="small"
              :disabled="row.archived === 1"
              @click="openNotice(row)">发送通知单</el-button>
            <el-button v-permission="'patientManagement:referral'" type="info" link size="small"
              @click="openReferral(row)">转诊</el-button>
            <el-button v-permission="'patientManagement:delete'" type="danger" link size="small"
              @click="handleDelete(row)">删除</el-button>
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

    <!-- 发送通知单弹窗 -->
    <el-dialog v-model="noticeDialogVisible" :title="`发送患者通知单 - ${noticeRow?.name ?? ''}`" width="580px" append-to-body>
      <el-form :model="noticeForm" label-width="110px">
        <el-form-item label="现居住地址">
          <el-input v-model="noticeForm.currentAddress" />
        </el-form-item>
        <el-form-item label="户籍地址">
          <el-input v-model="noticeForm.householdAddress" />
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="noticeForm.idNumber" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="noticeForm.gender">
                <el-option label="男" value="男" /><el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input v-model="noticeForm.age" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="出生日期">
          <el-date-picker v-model="noticeForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="民族">
          <el-input v-model="noticeForm.ethnicity" />
        </el-form-item>
        <el-form-item label="人群分类">
          <el-select v-model="noticeForm.crowdCategory">
            <el-option v-for="opt in CROWD_CATEGORY_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗方案" required>
          <el-select v-model="noticeForm.treatmentPlan">
            <el-option v-for="opt in TREATMENT_PLAN_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleNoticeSend">发送</el-button>
      </template>
    </el-dialog>

    <!-- 通知单详情弹窗 -->
    <el-dialog v-model="noticeDetailVisible" title="患者通知单详情" width="580px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ noticeDetail?.name }}</el-descriptions-item>
        <el-descriptions-item label="人群分类">{{ noticeDetail?.crowdCategory }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ noticeDetail?.idNumber }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ noticeDetail?.gender }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ noticeDetail?.birthDate }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ noticeDetail?.age }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ noticeDetail?.ethnicity }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案">{{ noticeDetail?.treatmentPlan }}</el-descriptions-item>
        <el-descriptions-item label="现居住地址" :span="2">{{ noticeDetail?.currentAddress }}</el-descriptions-item>
        <el-descriptions-item label="户籍地址" :span="2">{{ noticeDetail?.householdAddress }}</el-descriptions-item>
        <el-descriptions-item label="接收状态">
          <el-tag :type="noticeDetail?.status === 1 ? 'success' : 'warning'">
            {{ noticeDetail?.status === 1 ? '已接收' : '待接收' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ noticeDetail?.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 转诊弹窗 -->
    <ReferralDialog
      v-if="referralRow"
      v-model="referralDialogVisible"
      :biz-id="referralRow.id"
      biz-type="patient_aggregate"
      module-type="patient"
      :population-type="referralRow.populationType"
      :subject-name="referralRow.name || ''"
    />
  </div>
</template>
