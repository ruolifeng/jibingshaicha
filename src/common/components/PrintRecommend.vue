<script lang="ts" setup>
import { printElement } from "@@/utils/print"

/** 推介记录打印 / 保存 PDF */
const props = defineProps<{
  visible: boolean
  data: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

function handlePrint() {
  printElement("print-recommend-content", "推介记录")
}

function val(key: string) {
  const v = props.data?.[key]
  return v == null || v === "" ? "—" : String(v)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="预览 — 推介记录"
    width="760px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div id="print-recommend-content" class="print-area">
      <h2 class="print-title">
        推介记录
      </h2>
      <table class="form-table">
        <tbody>
          <tr>
            <th>姓名</th>
            <td>{{ val("name") }}</td>
            <th>性别</th>
            <td>{{ val("gender") }}</td>
          </tr>
          <tr>
            <th>出生日期</th>
            <td>{{ val("birthDate") }}</td>
            <th>年龄</th>
            <td>{{ val("age") }}</td>
          </tr>
          <tr>
            <th>证件类型</th>
            <td>{{ val("idType") }}</td>
            <th>证件号</th>
            <td>{{ val("idNumber") }}</td>
          </tr>
          <tr>
            <th>民族</th>
            <td>{{ val("ethnicity") }}</td>
            <th>联系电话</th>
            <td>{{ val("phone") }}</td>
          </tr>
          <tr>
            <th>户籍地址</th>
            <td colspan="3">
              {{ val("householdAddress") }}
            </td>
          </tr>
          <tr>
            <th>现住址</th>
            <td colspan="3">
              {{ val("currentAddress") }}
            </td>
          </tr>
          <tr>
            <th>人群分类</th>
            <td>{{ val("crowdCategory") }}</td>
            <th>推介接收人</th>
            <td>{{ val("receiverUserName") }}</td>
          </tr>
          <tr>
            <th>感染筛查时间</th>
            <td>{{ val("screenDate") }}</td>
            <th>感染筛查方法</th>
            <td>{{ val("screenMethod") }}</td>
          </tr>
          <tr>
            <th>感染筛查结果</th>
            <td>{{ val("infectionResult") }}</td>
            <th>胸片筛查时间</th>
            <td>{{ val("chestXrayDate") }}</td>
          </tr>
          <tr>
            <th>胸片筛查结果</th>
            <td colspan="3">
              {{ val("chestXrayResult") }}
            </td>
          </tr>
          <tr>
            <th>推介单位名称</th>
            <td>{{ val("recommendUnitName") }}</td>
            <th>填写用户名称</th>
            <td>{{ val("fillUserName") }}</td>
          </tr>
          <tr>
            <th>推介原因</th>
            <td colspan="3">
              {{ val("recommendReason") }}
            </td>
          </tr>
          <tr v-if="data?.recommendSentTime || data?.createTime">
            <th>推介时间</th>
            <td colspan="3">
              {{ val("recommendSentTime") !== "—" ? val("recommendSentTime") : val("createTime") }}
            </td>
          </tr>
        </tbody>
      </table>
      <div class="print-footer">
        <div>推介单位（盖章）：___________</div>
        <div>接收单位签收：___________</div>
        <div>签收日期：___________</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">
        关闭
      </el-button>
      <el-button type="primary" @click="handlePrint">
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

.form-table {
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    border: 1px solid #ddd;
    padding: 8px 12px;
    font-size: 14px;
    vertical-align: top;
  }

  th {
    background: #f5f7fa;
    width: 110px;
    white-space: nowrap;
  }
}

.print-footer {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 32px;
  font-size: 14px;
  color: #303133;
}
</style>
