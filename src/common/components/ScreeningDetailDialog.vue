<script lang="ts" setup>
/**
 * 通用筛查数据详情弹窗
 * 支持学校人群（school）、重点人群（keyPopulation）、密接人群（closeContact）三种类型
 */
import { ACTIVE_ROUND_MAP } from "@@/constants/disease"

const props = defineProps<{
  visible: boolean
  type: "school" | "keyPopulation" | "closeContact"
  data: any
}>()

const emit = defineEmits<{
  (e: "update:visible", val: boolean): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: val => emit("update:visible", val)
})

function getActiveRoundTag(round: number) {
  if (round === 1) return "success"
  if (round === 2) return "warning"
  return "danger"
}

const dialogTitle = computed(() => {
  if (!props.data) return "筛查详情"
  return `${props.data.name || ""} — 筛查详情`
})
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px">
    <template v-if="data">
      <!-- 学校人群详情 -->
      <template v-if="type === 'school'">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="年份">
            {{ data.year }}
          </el-descriptions-item>
          <el-descriptions-item label="市（州）">
            {{ data.city }}
          </el-descriptions-item>
          <el-descriptions-item label="区县">
            {{ data.district }}
          </el-descriptions-item>
          <el-descriptions-item label="姓名">
            {{ data.name }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ data.gender }}
          </el-descriptions-item>
          <el-descriptions-item label="出生日期">
            {{ data.birthDate }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ data.age }}
          </el-descriptions-item>
          <el-descriptions-item label="证件类型">
            {{ data.idType }}
          </el-descriptions-item>
          <el-descriptions-item label="证件号">
            {{ data.idNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="民族">
            {{ data.ethnicity }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ data.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="学校类型">
            {{ data.schoolType }}
          </el-descriptions-item>
          <el-descriptions-item label="学校名称">
            {{ data.schoolName }}
          </el-descriptions-item>
          <el-descriptions-item label="班级（院系）">
            {{ data.className }}
          </el-descriptions-item>
          <el-descriptions-item label="既往结核病史">
            {{ data.tbHistory }}
          </el-descriptions-item>
          <el-descriptions-item label="密切接触史">
            {{ data.closeContactHistory }}
          </el-descriptions-item>
          <el-descriptions-item label="可疑症状">
            {{ data.suspiciousSymptoms }}
          </el-descriptions-item>
          <el-descriptions-item label="是否进行感染筛">
            {{ data.hasInfectionScreen }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查日期">
            {{ data.screenDate }}
          </el-descriptions-item>
          <el-descriptions-item label="筛查方法">
            {{ data.screenMethod }}
          </el-descriptions-item>
          <el-descriptions-item label="筛查结果">
            {{ data.screenResult }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查结果">
            {{ data.infectionResult }}
          </el-descriptions-item>
          <el-descriptions-item label="感染判定">
            {{ data.isLatent === 1 ? "阳性（潜伏管理）" : "阴性" }}
          </el-descriptions-item>
          <el-descriptions-item label="是否进行胸片">
            {{ data.hasChestXray || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="胸片检查日期">
            {{ data.chestXrayDate || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="胸片检查结果">
            {{ data.chestXrayResult || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="首次诊断结果" :span="3">
            <el-tag v-if="data.diagnosisFirst" :type="data.diagnosisFirst?.includes('确诊') || data.diagnosisFirst?.includes('疑似') ? 'danger' : 'info'" size="small">
              {{ data.diagnosisFirst }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="户籍地址" :span="3">
            {{ data.householdAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="现住址" :span="3">
            {{ data.currentAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">
            {{ data.remark || "-" }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <!-- 重点人群详情 -->
      <template v-else-if="type === 'keyPopulation'">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="年份">
            {{ data.year }}
          </el-descriptions-item>
          <el-descriptions-item label="市（州）">
            {{ data.city }}
          </el-descriptions-item>
          <el-descriptions-item label="区县">
            {{ data.district }}
          </el-descriptions-item>
          <el-descriptions-item label="乡镇/社区">
            {{ data.townshipCommunity }}
          </el-descriptions-item>
          <el-descriptions-item label="姓名">
            {{ data.name }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ data.gender }}
          </el-descriptions-item>
          <el-descriptions-item label="出生日期">
            {{ data.birthDate }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ data.age }}
          </el-descriptions-item>
          <el-descriptions-item label="证件类型">
            {{ data.idType }}
          </el-descriptions-item>
          <el-descriptions-item label="证件号">
            {{ data.idNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="民族">
            {{ data.ethnicity }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ data.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="人群分类" :span="3">
            <span v-if="data.crowdCategoryClose === '是'">密接 </span>
            <span v-if="data.crowdCategoryStudent === '是'">学生 </span>
            <span v-if="data.crowdCategoryTeacher === '是'">教职工 </span>
            <span v-if="data.crowdCategoryElder === '是'">老年人 </span>
            <span v-if="data.crowdCategoryDiabetes === '是'">糖尿病 </span>
            <span v-if="data.crowdCategoryDual === '是'">双感 </span>
            <span v-if="data.crowdCategoryTbHist === '是'">既往结核史 </span>
            <span v-if="data.crowdCategoryNormal === '是'">非重点人群 </span>
          </el-descriptions-item>
          <el-descriptions-item label="可疑症状">
            {{ data.hasSuspiciousSymptoms }}
          </el-descriptions-item>
          <el-descriptions-item label="咳嗽咳痰">
            {{ data.cough }}
          </el-descriptions-item>
          <el-descriptions-item label="咯血或血痰">
            {{ data.hemoptysis }}
          </el-descriptions-item>
          <el-descriptions-item label="发热">
            {{ data.fever }}
          </el-descriptions-item>
          <el-descriptions-item label="胸痛">
            {{ data.chestPain }}
          </el-descriptions-item>
          <el-descriptions-item label="夜间盗汗">
            {{ data.nightSweats }}
          </el-descriptions-item>
          <el-descriptions-item label="食欲不振">
            {{ data.appetiteLoss }}
          </el-descriptions-item>
          <el-descriptions-item label="乏力">
            {{ data.fatigue }}
          </el-descriptions-item>
          <el-descriptions-item label="体重减轻">
            {{ data.weightLoss }}
          </el-descriptions-item>
          <el-descriptions-item label="是否进行感染筛">
            {{ data.hasInfectionScreen }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查日期">
            {{ data.screenDate }}
          </el-descriptions-item>
          <el-descriptions-item label="筛查方法">
            {{ data.screenMethod }}
          </el-descriptions-item>
          <el-descriptions-item label="筛查结果">
            {{ data.screenResult }}
          </el-descriptions-item>
          <el-descriptions-item label="感染筛查结果">
            {{ data.infectionResult }}
          </el-descriptions-item>
          <el-descriptions-item label="感染判定">
            {{ data.isLatent === 1 ? "阳性（潜伏管理）" : "阴性" }}
          </el-descriptions-item>
          <el-descriptions-item label="是否进行胸片">
            {{ data.hasChestXray || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="胸片检查日期">
            {{ data.chestXrayDate || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="胸片检查结果">
            {{ data.chestXrayResult || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="首次诊断结果" :span="3">
            <el-tag v-if="data.diagnosisFirst" :type="data.diagnosisFirst?.includes('确诊') || data.diagnosisFirst?.includes('疑似') ? 'danger' : 'info'" size="small">
              {{ data.diagnosisFirst }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="户籍地址" :span="3">
            {{ data.householdAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="现住址" :span="3">
            {{ data.currentAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">
            {{ data.remark || "-" }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <!-- 密接人群详情 -->
      <template v-else-if="type === 'closeContact'">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="年份">
            {{ data.year }}
          </el-descriptions-item>
          <el-descriptions-item label="市（州）">
            {{ data.city }}
          </el-descriptions-item>
          <el-descriptions-item label="区县">
            {{ data.district }}
          </el-descriptions-item>
          <el-descriptions-item label="姓名">
            {{ data.name }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ data.gender }}
          </el-descriptions-item>
          <el-descriptions-item label="出生日期">
            {{ data.birthDate }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ data.age }}
          </el-descriptions-item>
          <el-descriptions-item label="证件类型">
            {{ data.idType }}
          </el-descriptions-item>
          <el-descriptions-item label="证件号">
            {{ data.idNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="民族">
            {{ data.ethnicity }}
          </el-descriptions-item>
          <el-descriptions-item label="职业">
            {{ data.occupation }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ data.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="接触类型">
            {{ data.contactType }}
          </el-descriptions-item>
          <el-descriptions-item label="原患者姓名">
            {{ data.sourcePatientName }}
          </el-descriptions-item>
          <el-descriptions-item label="原患者确诊日期">
            {{ data.sourcePatientConfirmDate }}
          </el-descriptions-item>
          <el-descriptions-item label="原患者身份证号">
            {{ data.sourcePatientIdNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="户籍地址" :span="2">
            {{ data.householdAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="现住址" :span="2">
            {{ data.currentAddress }}
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs>
          <el-tab-pane label="首次筛查">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="筛查日期">
                {{ data.firstScreenDate }}
              </el-descriptions-item>
              <el-descriptions-item label="症状筛查结果">
                {{ data.firstSymptomResult }}
              </el-descriptions-item>
              <el-descriptions-item label="感染检查方法">
                {{ data.firstInfectionMethod }}
              </el-descriptions-item>
              <el-descriptions-item label="感染检查结果">
                {{ data.firstScreenResult }}
              </el-descriptions-item>
              <el-descriptions-item label="感染筛查结果">
                <el-tag :type="data.firstInfectionResult?.includes('阳') || data.firstInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                  {{ data.firstInfectionResult || "-" }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="是否进行胸片">
                {{ data.firstHasChestXray }}
              </el-descriptions-item>
              <el-descriptions-item label="胸片日期">
                {{ data.firstChestXrayDate }}
              </el-descriptions-item>
              <el-descriptions-item label="胸片结果">
                {{ data.firstChestXrayResult }}
              </el-descriptions-item>
              <el-descriptions-item label="诊断结果" :span="2">
                <el-tag v-if="data.firstDiagnosis" size="small">
                  {{ data.firstDiagnosis }}
                </el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="半年后筛查">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="筛查日期">
                {{ data.halfYearScreenDate }}
              </el-descriptions-item>
              <el-descriptions-item label="症状筛查结果">
                {{ data.halfYearSymptomResult }}
              </el-descriptions-item>
              <el-descriptions-item label="感染检查方法">
                {{ data.halfYearInfectionMethod }}
              </el-descriptions-item>
              <el-descriptions-item label="感染检查结果">
                {{ data.halfYearScreenResult }}
              </el-descriptions-item>
              <el-descriptions-item label="感染筛查结果">
                <el-tag :type="data.halfYearInfectionResult?.includes('阳') || data.halfYearInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                  {{ data.halfYearInfectionResult || "-" }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="是否进行胸片">
                {{ data.halfYearHasChestXray }}
              </el-descriptions-item>
              <el-descriptions-item label="胸片日期">
                {{ data.halfYearChestXrayDate }}
              </el-descriptions-item>
              <el-descriptions-item label="胸片结果">
                {{ data.halfYearChestXrayResult }}
              </el-descriptions-item>
              <el-descriptions-item label="诊断结果" :span="2">
                <el-tag v-if="data.halfYearDiagnosis" size="small">
                  {{ data.halfYearDiagnosis }}
                </el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="一年后筛查">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="筛查日期">
                {{ data.oneYearScreenDate }}
              </el-descriptions-item>
              <el-descriptions-item label="症状筛查结果">
                {{ data.oneYearSymptomResult }}
              </el-descriptions-item>
              <el-descriptions-item label="感染检查方法">
                {{ data.oneYearInfectionMethod }}
              </el-descriptions-item>
              <el-descriptions-item label="感染检查结果">
                {{ data.oneYearScreenResult }}
              </el-descriptions-item>
              <el-descriptions-item label="感染筛查结果">
                <el-tag :type="data.oneYearInfectionResult?.includes('阳') || data.oneYearInfectionResult?.includes('PPD+') ? 'danger' : 'success'" size="small">
                  {{ data.oneYearInfectionResult || "-" }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="是否进行胸片">
                {{ data.oneYearHasChestXray }}
              </el-descriptions-item>
              <el-descriptions-item label="胸片日期">
                {{ data.oneYearChestXrayDate }}
              </el-descriptions-item>
              <el-descriptions-item label="胸片结果">
                {{ data.oneYearChestXrayResult }}
              </el-descriptions-item>
              <el-descriptions-item label="诊断结果" :span="2">
                <el-tag v-if="data.oneYearDiagnosis" size="small">
                  {{ data.oneYearDiagnosis }}
                </el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="预防性治疗">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="是否进行预防性治疗">
                {{ data.hasPreventiveTreatment }}
              </el-descriptions-item>
              <el-descriptions-item label="预防性治疗方案">
                {{ data.preventivePlan }}
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">
                {{ data.preventiveStartDate }}
              </el-descriptions-item>
              <el-descriptions-item label="完成时间">
                {{ data.preventiveEndDate }}
              </el-descriptions-item>
              <el-descriptions-item label="治疗结果">
                {{ data.preventiveResult }}
              </el-descriptions-item>
              <el-descriptions-item label="随访管理人员">
                {{ data.preventiveManager }}
              </el-descriptions-item>
              <el-descriptions-item label="惠民方式">
                {{ data.benefitMethod }}
              </el-descriptions-item>
              <el-descriptions-item label="备注">
                {{ data.remark || "-" }}
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>

        <div v-if="data.activeRound" class="mt-3 text-sm text-gray-500">
          阳性轮次：
          <el-tag :type="getActiveRoundTag(data.activeRound)" size="small">
            {{ ACTIVE_ROUND_MAP[data.activeRound] }}
          </el-tag>
        </div>
      </template>
    </template>

    <template #footer>
      <el-button @click="dialogVisible = false">
        关闭
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
.mt-3 {
  margin-top: 12px;
}
</style>
