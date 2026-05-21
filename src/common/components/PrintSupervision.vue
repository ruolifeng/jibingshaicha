<script lang="ts" setup>
import { Printer } from "@element-plus/icons-vue"
import { printElement } from "@@/utils/print"

/** 结核病潜伏感染者预防性治疗督导表打印/PDF 预览组件 */
defineProps<{
  visible: boolean
  data: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

function handlePrint() {
  printElement("print-supervision-content", "结核病潜伏感染者预防性治疗督导表")
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="预览 — 结核病潜伏感染者预防性治疗督导表"
    width="820px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div id="print-supervision-content" class="print-area">
      <h2 class="print-title">
        结核病潜伏感染者预防性治疗督导表
      </h2>

      <table class="sup-table">
        <tbody>
          <tr>
            <th>姓名</th>
            <td>{{ data?.patientName || "-" }}</td>
            <th>人群分类</th>
            <td>{{ data?.category || "-" }}</td>
          </tr>
          <tr>
            <th>现居住地址</th>
            <td>{{ data?.currentAddress || "-" }}</td>
            <th>户籍地址</th>
            <td>{{ data?.householdAddress || "-" }}</td>
          </tr>
          <tr>
            <th>身份证</th>
            <td>{{ data?.idNumber || "-" }}</td>
            <th>性别</th>
            <td>{{ data?.gender || "-" }}</td>
          </tr>
          <tr>
            <th>出生日期</th>
            <td>{{ data?.birthDate || "-" }}</td>
            <th>民族</th>
            <td>{{ data?.ethnicity || "-" }}</td>
          </tr>
          <tr>
            <th>是否开始预防性治疗</th>
            <td>{{ data?.hasPreventiveTreatment || "-" }}</td>
            <th>治疗方案</th>
            <td>{{ data?.treatmentPlan || "-" }}</td>
          </tr>
          <tr>
            <th>治疗开始时间</th>
            <td>{{ data?.treatmentStartDate || "-" }}</td>
            <th>治疗结束时间</th>
            <td>{{ data?.treatmentEndDate || "-" }}</td>
          </tr>
          <tr>
            <th>管理单位</th>
            <td>{{ data?.managingUnit || "-" }}</td>
            <th>督导医生</th>
            <td>{{ data?.supervisingDoctor || "-" }}</td>
          </tr>
          <tr>
            <th>备注</th>
            <td colspan="3">
              {{ data?.remark || "-" }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button type="primary" :icon="Printer" @click="handlePrint">
        打印 / 保存PDF
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.print-area {
  padding: 8px;
}

.print-title {
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 16px;
}

.sup-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;

  th,
  td {
    border: 1px solid #ddd;
    padding: 7px 10px;
    font-size: 13px;
  }

  th {
    background: #f5f7fa;
    white-space: nowrap;
    width: 130px;
  }
}
</style>
