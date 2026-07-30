<script lang="ts" setup>
import { formatNoticeSentTime } from "@@/utils/patient"
import { printElement } from "@@/utils/print"
import "@@/assets/styles/print-forms.css"

/** 通用通知单打印组件（潜伏者通知单 / 患者通知单） */
const props = defineProps<{
  visible: boolean
  noticeData: Record<string, any> | null
  noticeType?: "latent" | "patient"
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

const title = computed(() => props.noticeType === "patient" ? "肺结核患者管理通知单" : "结核病潜伏感染者预防性治疗通知单")
const isPatient = computed(() => props.noticeType === "patient")

/** 姓名（单位）展示，与详情弹窗一致 */
function formatParty(name?: string | null, org?: string | null): string {
  const n = (name || "").trim()
  const o = (org || "").trim()
  if (n && o) return `${n}（${o}）`
  return n || o || ""
}

const senderParty = computed(() => formatParty(props.noticeData?.senderName, props.noticeData?.senderOrgName))
const receiverParty = computed(() => formatParty(props.noticeData?.receiverName, props.noticeData?.receiverOrgName))

function handlePrint() {
  printElement("print-notice-content", title.value)
}
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
      <table class="notice-table" border="1" cellspacing="0" cellpadding="0">
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
            <td colspan="3" class="party-cell">
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
            <td colspan="3" class="party-cell">
              {{ noticeData?.currentAddress }}
            </td>
          </tr>
          <tr>
            <th>户籍地址</th>
            <td colspan="3" class="party-cell">
              {{ noticeData?.householdAddress }}
            </td>
          </tr>
          <tr>
            <th>人群分类</th>
            <td :colspan="isPatient ? 3 : 1">
              {{ noticeData?.crowdCategory }}
            </td>
            <template v-if="!isPatient">
              <th>治疗方案</th>
              <td class="party-cell">
                {{ noticeData?.treatmentPlan }}
              </td>
            </template>
          </tr>
          <tr v-if="isPatient">
            <th>治疗方案</th>
            <td class="party-cell">
              {{ noticeData?.treatmentPlan }}
            </td>
            <th>耐药情况</th>
            <td>
              {{ noticeData?.drugResistance }}
            </td>
          </tr>
          <template v-if="isPatient">
            <tr>
              <th>患者类型</th>
              <td>{{ noticeData?.patientType }}</td>
              <th>管理方式</th>
              <td>{{ noticeData?.managementMethod }}</td>
            </tr>
            <tr>
              <th>痰涂片</th>
              <td>{{ noticeData?.sputumSmear }}</td>
              <th>痰培养</th>
              <td>{{ noticeData?.sputumCulture }}</td>
            </tr>
            <tr>
              <th>分子检查</th>
              <td>{{ noticeData?.molecularTest }}</td>
              <th>病理学检查</th>
              <td>{{ noticeData?.pathologyTest }}</td>
            </tr>
          </template>
          <template v-else>
            <tr>
              <th>感染检查日期</th>
              <td>{{ noticeData?.infectionDate }}</td>
              <th>检查方法</th>
              <td>{{ noticeData?.infectionMethod }}</td>
            </tr>
            <tr>
              <th>检查结果</th>
              <td class="party-cell">
                {{ noticeData?.infectionResultValue }}
              </td>
              <th>胸片日期</th>
              <td>{{ noticeData?.chestXrayDate }}</td>
            </tr>
          </template>
          <tr>
            <th>胸片结果</th>
            <td>{{ noticeData?.chestXrayResult }}</td>
            <th>治疗机构</th>
            <td class="party-cell">
              {{ noticeData?.treatmentInstitution }}
            </td>
          </tr>
          <tr v-if="isPatient">
            <th>服药管理单位</th>
            <td colspan="3" class="party-cell">
              {{ noticeData?.medicationManagementUnit }}
            </td>
          </tr>
          <tr>
            <th>下发时间</th>
            <td>{{ noticeData?.issuedTime }}</td>
            <th>发送时间</th>
            <td>{{ formatNoticeSentTime(noticeData?.sentTime) }}</td>
          </tr>
          <tr>
            <th>下发人</th>
            <td class="party-cell">
              {{ senderParty || "—" }}
            </td>
            <th>接收人</th>
            <td class="party-cell">
              {{ receiverParty || "—" }}
            </td>
          </tr>
          <tr v-if="noticeData?.confirmedTime">
            <th>接收时间</th>
            <td colspan="3">
              {{ formatNoticeSentTime(noticeData?.confirmedTime) }}
            </td>
          </tr>
          <tr v-if="isPatient">
            <th>备注</th>
            <td colspan="3" class="party-cell">
              {{ noticeData?.remark || "" }}
            </td>
          </tr>
          <tr v-if="noticeData?.otherNotes">
            <th>其他注意事项</th>
            <td colspan="3" class="party-cell">
              {{ noticeData?.otherNotes }}
            </td>
          </tr>
        </tbody>
      </table>
      <div class="notice-footer">
        <div class="notice-footer__item notice-footer__item--wide">
          <span class="footer-label">发送单位：</span>
          <span class="party-text">{{ noticeData?.senderOrgName || senderParty || "___________" }}</span>
        </div>
        <div class="notice-footer__item notice-footer__item--wide">
          <span class="footer-label">接收单位签收：</span>
          <span class="party-text">{{ noticeData?.receiverOrgName || receiverParty || "___________" }}</span>
        </div>
        <div class="notice-footer__item notice-footer__item--date">
          <span class="footer-label">签收日期：</span>
          <span class="party-text">{{ formatNoticeSentTime(noticeData?.confirmedTime) || "___________" }}</span>
        </div>
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
  table-layout: fixed;

  th,
  td {
    border: 1px solid #333;
    padding: 8px 10px;
    font-size: 13px;
    vertical-align: middle;
  }

  th {
    background: #f5f7fa;
    width: 88px;
    white-space: nowrap;
  }

  /* 避免中文姓名/单位被逐字拆断换行 */
  .party-cell {
    word-break: keep-all;
    overflow-wrap: break-word;
  }
}

.notice-footer {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 24px;
  font-size: 12px;
  line-height: 1.5;
}

.notice-footer__item {
  min-width: 0;
}

.notice-footer__item--wide {
  flex: 1 1 0;
}

.notice-footer__item--date {
  flex: 0 0 150px;
}

.footer-label {
  white-space: nowrap;
  font-weight: 500;
}

.party-text {
  word-break: keep-all;
  overflow-wrap: break-word;
}
</style>
