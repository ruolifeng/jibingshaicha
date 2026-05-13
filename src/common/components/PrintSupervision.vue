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

/** 解析督导记录 JSON 字符串，兜底返回空数组 */
function parseRecords(raw: string | undefined): { time: string, method: string, content: string, remark: string }[] {
  if (!raw) return []
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
}

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

      <!-- 基本信息 -->
      <table class="sup-table">
        <tbody>
          <tr>
            <th>姓名</th>
            <td>{{ data?.patientName }}</td>
            <th>类别</th>
            <td>{{ data?.category || "-" }}</td>
          </tr>
          <tr>
            <th>性别</th>
            <td>{{ data?.gender || "-" }}</td>
            <th>年龄</th>
            <td>{{ data?.age ?? "-" }}</td>
          </tr>
          <tr>
            <th>电话号码</th>
            <td>{{ data?.phone || "-" }}</td>
            <th>现住址</th>
            <td>{{ data?.currentAddress || "-" }}</td>
          </tr>
          <tr>
            <th>治疗方案</th>
            <td colspan="3">
              {{ data?.treatmentPlan || "-" }}
            </td>
          </tr>
          <tr>
            <th>开始治疗时间</th>
            <td>{{ data?.treatmentStartDate || "-" }}</td>
            <th>结束疗程时间</th>
            <td>{{ data?.treatmentEndDate || "-" }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 督导记录 -->
      <h3 class="section-title">
        督导记录
      </h3>
      <table class="sup-table">
        <thead>
          <tr>
            <th style="width:120px">
              督导时间
            </th>
            <th style="width:120px">
              督导方式
            </th>
            <th>督导内容</th>
            <th style="width:150px">
              备注
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(rec, i) in parseRecords(data?.supervisionRecords)" :key="i">
            <td>{{ rec.time || "-" }}</td>
            <td>{{ rec.method || "-" }}</td>
            <td>{{ rec.content || "-" }}</td>
            <td>{{ rec.remark || "-" }}</td>
          </tr>
          <tr v-if="!parseRecords(data?.supervisionRecords).length">
            <td colspan="4" style="text-align:center;color:#999">
              暂无督导记录
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 全疗程规律治疗评价 -->
      <h3 class="section-title">
        全疗程规律治疗评价
      </h3>
      <table class="sup-table">
        <tbody>
          <tr>
            <th>中断用药</th>
            <td>{{ data?.interruptMedication || "-" }}</td>
            <th>中断次数</th>
            <td>{{ data?.interruptCount ?? "-" }}</td>
          </tr>
          <tr>
            <th>全程应用药次数</th>
            <td>{{ data?.totalDoses ?? "-" }}</td>
            <th>实际用药次数</th>
            <td>{{ data?.actualDoses ?? "-" }}</td>
          </tr>
          <tr>
            <th>用药率</th>
            <td colspan="3">
              {{ data?.medicationRate || "-" }}
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 督导管理人员 -->
      <h3 class="section-title">
        督导管理人员
      </h3>
      <table class="sup-table">
        <tbody>
          <tr>
            <th>管理人员类型</th>
            <td>{{ data?.managerType || "-" }}</td>
            <th>管理人员姓名</th>
            <td>{{ data?.managerName || "-" }}</td>
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

.section-title {
  font-size: 14px;
  font-weight: bold;
  margin: 14px 0 6px;
  padding-left: 4px;
  border-left: 3px solid #409eff;
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

  thead th {
    text-align: center;
    width: auto;
  }
}
</style>

