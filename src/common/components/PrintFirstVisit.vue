<script lang="ts" setup>
/** 肺结核患者第一次入户随访记录表打印组件 */
defineProps<{
  visible: boolean
  visitData: Record<string, any> | null
  patientName?: string
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

function handlePrint() {
  window.print()
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="预览 — 肺结核患者第一次入户随访记录表"
    width="760px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div id="print-visit-content" class="print-area">
      <h2 class="print-title">
        肺结核患者第一次入户随访记录表
      </h2>
      <table class="visit-table">
        <tbody>
          <tr>
            <th>患者姓名</th>
            <td>{{ patientName || visitData?.patientName }}</td>
            <th>随访日期</th>
            <td>{{ visitData?.visitDate }}</td>
          </tr>
          <tr>
            <th>随访方式</th>
            <td>{{ visitData?.visitMethod }}</td>
            <th>督导人员</th>
            <td>{{ visitData?.supervisorType }}</td>
          </tr>
          <tr>
            <th>症状及体征</th>
            <td colspan="3">
              {{ visitData?.symptoms }}
            </td>
          </tr>
          <tr>
            <th>化疗方案</th>
            <td>{{ visitData?.chemotherapyPlan }}</td>
            <th>用法</th>
            <td>{{ visitData?.medicationUsage }}</td>
          </tr>
          <tr>
            <th>药品剂型</th>
            <td>{{ visitData?.drugForm }}</td>
            <th>取药地点</th>
            <td>{{ visitData?.medicationLocation }}</td>
          </tr>
          <tr>
            <th>取药时间</th>
            <td>{{ visitData?.medicationPickTime }}</td>
            <th>居室通风</th>
            <td>{{ visitData?.ventilation }}</td>
          </tr>
          <tr>
            <th>痰菌情况</th>
            <td>{{ visitData?.sputumStatus }}</td>
            <th>耐药情况</th>
            <td>{{ visitData?.drugResistance }}</td>
          </tr>
          <tr>
            <th>健康教育内容</th>
            <td colspan="3">
              <span v-if="visitData?.educationItems">
                {{ typeof visitData.educationItems === 'string'
                  ? visitData.educationItems
                  : Object.entries(visitData.educationItems).map(([k, v]) => `${k}：${v}`).join('；') }}
              </span>
            </td>
          </tr>
          <tr>
            <th>下次随访时间</th>
            <td>{{ visitData?.nextVisitDate }}</td>
            <th>评估医生签名</th>
            <td>{{ visitData?.doctorSignature }}</td>
          </tr>
          <tr>
            <th>备注</th>
            <td colspan="3">
              {{ visitData?.remark }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button type="primary" @click="handlePrint">
        打印
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

.visit-table {
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    border: 1px solid #ddd;
    padding: 8px 12px;
    font-size: 14px;
  }

  th {
    background: #f5f7fa;
    width: 110px;
    white-space: nowrap;
  }
}
</style>

<style lang="scss">
@media print {
  body > *:not(#print-visit-content) {
    display: none !important;
  }
  .el-dialog__wrapper {
    position: static !important;
  }
  .el-dialog {
    box-shadow: none !important;
  }
  .el-dialog__header,
  .el-dialog__footer {
    display: none !important;
  }
  #print-visit-content {
    display: block !important;
  }
}
</style>
