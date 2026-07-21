<script lang="ts" setup>
/** 肺结核患者第一次入户随访记录表打印组件 */
import PrintAttachmentImages from "@@/components/PrintAttachmentImages.vue"
import { EDUCATION_ITEMS, SYMPTOM_OPTIONS } from "@@/constants/disease"
import { formatFirstVisitMethod } from "@@/utils/firstVisit"
import { printElement } from "@@/utils/print"

const props = defineProps<{
  visible: boolean
  visitData: Record<string, any> | null
  patientName?: string
}>()

const emit = defineEmits<{
  (e: "update:visible", v: boolean): void
}>()

/** 将存储的症状 value 列表（逗号分隔）转换为可读标签 */
const symptomLabels = computed(() => {
  const raw = props.visitData?.symptoms
  if (!raw) return "-"
  const valueMap = Object.fromEntries(SYMPTOM_OPTIONS.map(o => [o.value, o.label]))
  return raw
    .split(",")
    .map((v: string) => valueMap[v.trim()] ?? v.trim())
    .filter(Boolean)
    .join("、") || "-"
})

/** 解析教育项目（存储为 JSON 字符串），按标准项顺序输出 */
const parsedEducationItems = computed<[string, string][]>(() => {
  const raw = props.visitData?.educationItems
  if (!raw) return []
  let obj: Record<string, string> = {}
  if (typeof raw === "string") {
    try {
      obj = JSON.parse(raw)
    } catch {
      return []
    }
  } else {
    obj = raw as Record<string, string>
  }
  const known = new Set(EDUCATION_ITEMS)
  const ordered: [string, string][] = EDUCATION_ITEMS
    .filter(key => key in obj)
    .map(key => [key, obj[key]])
  const extra = Object.entries(obj).filter(([key]) => !known.has(key)) as [string, string][]
  return [...ordered, ...extra]
})

/** 将教育项目每两个分为一组，用于双列渲染 */
const educationRows = computed<[string, string][][]>(() => {
  const items = parsedEducationItems.value
  const rows: [string, string][][] = []
  for (let i = 0; i < items.length; i += 2) {
    rows.push(items.slice(i, i + 2) as [string, string][])
  }
  return rows
})

function handlePrint() {
  printElement("print-visit-content", "肺结核患者第一次入户随访记录表")
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="预览 — 肺结核患者第一次入户随访记录表"
    width="860px"
    @update:model-value="emit('update:visible', $event)"
  >
    <div id="print-visit-content" class="print-area">
      <div class="print-header">
        <h2 class="print-title">
          肺结核患者第一次入户随访记录表
        </h2>
        <div class="print-form-no">
          编号：{{ visitData?.formNo || "" }}
        </div>
      </div>
      <table class="visit-table" border="1" cellspacing="0" cellpadding="0">
        <tbody>
          <!-- 基本信息 -->
          <tr class="section-header">
            <td colspan="6">
              基本信息
            </td>
          </tr>
          <tr>
            <th>患者姓名</th>
            <td>{{ patientName || visitData?.patientName }}</td>
            <th>随访时间</th>
            <td>{{ visitData?.visitDate }}</td>
            <th>随访方式</th>
            <td>{{ formatFirstVisitMethod(visitData?.visitMethod, visitData?.visitMethodOther) }}</td>
          </tr>
          <tr>
            <th>患者类型</th>
            <td>{{ visitData?.patientType }}</td>
            <th>痰菌情况</th>
            <td>{{ visitData?.sputumStatus }}</td>
            <th>痰培养</th>
            <td>{{ visitData?.sputumCulture || "—" }}</td>
          </tr>
          <tr>
            <th>耐药情况</th>
            <td colspan="5">
              {{ visitData?.drugResistance }}
            </td>
          </tr>
          <tr>
            <th>症状及体征</th>
            <td colspan="5">
              {{ symptomLabels }}
            </td>
          </tr>
          <tr>
            <th>其他症状</th>
            <td colspan="5">
              {{ visitData?.otherSymptoms || "-" }}
            </td>
          </tr>

          <!-- 用药情况 -->
          <tr class="section-header">
            <td colspan="6">
              用药情况
            </td>
          </tr>
          <tr>
            <th>化疗方案</th>
            <td colspan="5">
              {{ visitData?.chemotherapy }}
            </td>
          </tr>
          <tr>
            <th>用法</th>
            <td>{{ visitData?.medicationUsage }}</td>
            <th>督导人员</th>
            <td>{{ visitData?.supervisor }}</td>
            <th>药品剂型</th>
            <td>{{ visitData?.drugForm }}</td>
          </tr>

          <!-- 居住环境与生活方式 -->
          <tr class="section-header">
            <td colspan="6">
              居住环境与生活方式
            </td>
          </tr>
          <tr>
            <th>单独居室</th>
            <td>{{ visitData?.separateRoom }}</td>
            <th>通风情况</th>
            <td>{{ visitData?.ventilation }}</td>
            <th>吸烟(支/天)</th>
            <td>{{ visitData?.smokingAmount }}</td>
          </tr>
          <tr>
            <th>饮酒(两/天)</th>
            <td>{{ visitData?.drinkingAmount }}</td>
            <th>取药地点</th>
            <td>{{ visitData?.medicationLocation }}</td>
            <th>取药时间</th>
            <td>{{ visitData?.medicationPickTime }}</td>
          </tr>

          <!-- 健康教育及培训 -->
          <tr class="section-header">
            <td colspan="6">
              健康教育及培训
            </td>
          </tr>
          <template v-if="educationRows.length > 0">
            <!-- 标签占 2 列，避免长文案在窄 th + nowrap 下溢出叠字 -->
            <tr v-for="(row, rIdx) in educationRows" :key="rIdx" class="edu-row">
              <th colspan="2">
                {{ row[0][0] }}
              </th>
              <td>{{ row[0][1] }}</td>
              <template v-if="row[1]">
                <th colspan="2">
                  {{ row[1][0] }}
                </th>
                <td>{{ row[1][1] }}</td>
              </template>
              <template v-else>
                <td colspan="3" />
              </template>
            </tr>
          </template>
          <tr v-else>
            <td colspan="6" class="empty-cell">
              —
            </td>
          </tr>

          <!-- 其他 -->
          <tr class="section-header">
            <td colspan="6">
              其他
            </td>
          </tr>
          <tr>
            <th>下次随访时间</th>
            <td colspan="2">
              {{ visitData?.nextVisitDate }}
            </td>
            <th>评估医生签名</th>
            <td colspan="2">
              {{ visitData?.doctorSignature }}
            </td>
          </tr>
          <tr>
            <th>备注</th>
            <td colspan="5">
              {{ visitData?.remarks || "-" }}
            </td>
          </tr>
        </tbody>
      </table>
      <PrintAttachmentImages :urls="visitData?.attachmentUrls" title="附件照片" />
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

<style lang="scss">
@import "@@/assets/styles/print-forms.css";
</style>
