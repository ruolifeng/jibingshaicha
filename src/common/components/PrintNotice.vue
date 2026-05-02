<script lang="ts" setup>
/** 通用通知单打印组件（潜伏者通知单 / 患者通知单） */
const props = defineProps<{
  visible: boolean
  noticeData: Record<string, any> | null
  noticeType?: "latent" | "patient"
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

function handlePrint() {
  window.print()
}

const title = computed(() => props.noticeType === "patient" ? "肺结核患者管理通知单" : "结核病潜伏感染者预防性治疗通知单")
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`预览 — ${title}`"
    width="700px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div id="print-notice-content" class="print-area">
      <h2 class="print-title">
        {{ title }}
      </h2>
      <table class="notice-table">
        <tbody>
          <tr>
            <th>姓名</th>
            <td>{{ noticeData?.patientName }}</td>
            <th>性别</th>
            <td>{{ noticeData?.gender }}</td>
          </tr>
          <tr>
            <th>出生日期</th>
            <td>{{ noticeData?.birthDate }}</td>
            <th>年龄</th>
            <td>{{ noticeData?.age }}</td>
          </tr>
          <tr>
            <th>身份证号</th>
            <td colspan="3">
              {{ noticeData?.idNumber }}
            </td>
          </tr>
          <tr>
            <th>民族</th>
            <td>{{ noticeData?.ethnicity }}</td>
            <th>联系电话</th>
            <td>{{ noticeData?.phone }}</td>
          </tr>
          <tr>
            <th>现居住地址</th>
            <td colspan="3">
              {{ noticeData?.currentAddress }}
            </td>
          </tr>
          <tr>
            <th>户籍地址</th>
            <td colspan="3">
              {{ noticeData?.householdAddress }}
            </td>
          </tr>
          <tr>
            <th>人群分类</th>
            <td>{{ noticeData?.crowdCategory }}</td>
            <th>治疗方案</th>
            <td>{{ noticeData?.treatmentPlan }}</td>
          </tr>
          <tr>
            <th>感染检查日期</th>
            <td>{{ noticeData?.infectionDate }}</td>
            <th>检查方法</th>
            <td>{{ noticeData?.infectionMethod }}</td>
          </tr>
          <tr>
            <th>检查结果</th>
            <td>{{ noticeData?.infectionResultValue }}</td>
            <th>胸片日期</th>
            <td>{{ noticeData?.chestXrayDate }}</td>
          </tr>
          <tr>
            <th>胸片结果</th>
            <td>{{ noticeData?.chestXrayResult }}</td>
            <th>治疗机构</th>
            <td>{{ noticeData?.treatmentInstitution }}</td>
          </tr>
          <tr>
            <th>下发时间</th>
            <td colspan="3">
              {{ noticeData?.issuedTime }}
            </td>
          </tr>
        </tbody>
      </table>
      <div class="print-footer">
        <div>发送单位：___________</div>
        <div>接收单位签收：___________</div>
        <div>签收日期：___________</div>
      </div>
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

.notice-table {
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
    width: 120px;
    white-space: nowrap;
  }
}

.print-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 32px;
  font-size: 14px;
  color: #303133;
}
</style>

<style lang="scss">
@media print {
  body > *:not(#print-notice-content) {
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
  #print-notice-content {
    display: block !important;
  }
}
</style>
