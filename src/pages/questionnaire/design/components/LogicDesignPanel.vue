<script lang="ts" setup>
/* eslint-disable vue/no-mutating-props */
import type { QuestionItem } from "../../apis/type"
import { Delete, Plus } from "@element-plus/icons-vue"

/** source / target 均使用 stableKey（已保存题目为 String(id)，未保存为 "t{_tempKey}"） */
interface ConditionItem { source: string, op: string, value: string }
interface ConditionGroup { relation: "and" | "or", conditions: ConditionItem[] }
interface JumpRule extends ConditionGroup { target: string }
interface LogicRulesData { display?: ConditionGroup, jumps?: JumpRule[] }

const props = defineProps<{
  question: QuestionItem
  questions: QuestionItem[]
  questionIndex: number
}>()

const layoutTypes = new Set(["divider", "page_break"])
const nonFillableTypes = new Set(["description", "divider", "image_display", "page_break"])
const numericTypes = new Set(["number", "slider", "rating", "nps"])
const optionTypes = new Set(["radio", "checkbox", "dropdown", "image_choice", "sort"])

const isLayout = computed(() => nonFillableTypes.has(props.question.type))

const typeLabels: Record<string, string> = {
  input: "单行文本",
  textarea: "多行文本",
  number: "数字",
  date: "日期",
  slider: "滑块",
  radio: "单选",
  checkbox: "多选",
  dropdown: "下拉",
  cascader: "级联",
  image_choice: "图片选择",
  sort: "排序",
  multi_input: "多项填空",
  inline_input: "横向填空",
  rating: "评分",
  nps: "NPS",
  matrix_radio: "矩阵单选",
  matrix_checkbox: "矩阵多选",
  matrix_input: "矩阵填空",
  matrix_scale: "矩阵量表",
  matrix_complex: "复合矩阵",
  formula: "自动计算",
  dynamic_table: "子表单",
  file_upload: "文件上传",
  image_upload: "图片上传",
  signature: "电子签名",
  description: "文字描述",
  divider: "分割线",
  image_display: "图片展示",
  page_break: "分页"
}

/** 获取题目的稳定 key（与 index.vue 中的 getStableKey 保持一致） */
function getStableKey(q: QuestionItem): string {
  return q.id != null ? String(q.id) : `t${q._tempKey}`
}

/** 当前题目自身的 stableKey */
const currentKey = computed(() => getStableKey(props.question))

const questionNumMap = computed(() => {
  const map: Record<number, number> = {}
  let num = 0
  props.questions.forEach((q, idx) => {
    if (!layoutTypes.has(q.type)) map[idx] = ++num
  })
  return map
})

/** 当前题目之前（不含自身）且可作为条件来源的题目 */
const displaySourceQuestions = computed(() =>
  props.questions
    .map((q, idx) => ({ key: getStableKey(q), num: questionNumMap.value[idx], question: q }))
    .filter((item) => {
      const idx = props.questions.indexOf(item.question)
      return idx < props.questionIndex && !nonFillableTypes.has(item.question.type)
    })
)

/** 当前题目及之前（含自身）且可作为条件来源的题目 */
const jumpSourceQuestions = computed(() =>
  props.questions
    .map((q, idx) => ({ key: getStableKey(q), num: questionNumMap.value[idx], question: q }))
    .filter((item) => {
      const idx = props.questions.indexOf(item.question)
      return idx <= props.questionIndex && !nonFillableTypes.has(item.question.type)
    })
)

/** 当前题目之后可以跳转到的目标选项 */
const jumpTargetOptions = computed(() => {
  const targets: { value: string, label: string }[] = []
  props.questions.forEach((q, idx) => {
    if (idx <= props.questionIndex) return
    const num = questionNumMap.value[idx]
    if (num) targets.push({ value: getStableKey(q), label: `Q${num}. ${q.title || "(未设置)"}` })
  })
  targets.push({ value: "end", label: "结束问卷" })
  return targets
})

/* ====== parse / save ====== */
function parseRules(): LogicRulesData {
  if (!props.question.logicRules) return {}
  try {
    return JSON.parse(props.question.logicRules)
  } catch {
    return {}
  }
}

function saveRules(rules: LogicRulesData) {
  const hasDisplay = rules.display && rules.display.conditions.length > 0
  const hasJumps = rules.jumps && rules.jumps.length > 0
  if (!hasDisplay && !hasJumps) {
    props.question.logicRules = undefined
    return
  }
  if (!hasDisplay) delete rules.display
  if (!hasJumps) delete rules.jumps
  props.question.logicRules = JSON.stringify(rules)
}

/* ====== 显示条件 ====== */
const displayEnabled = computed({
  get: () => {
    const r = parseRules()
    return !!(r.display && r.display.conditions.length > 0)
  },
  set: (val: boolean) => {
    const r = parseRules()
    if (val) {
      const src = displaySourceQuestions.value[0]?.key || ""
      r.display = { relation: "and", conditions: [{ source: src, op: "eq", value: "" }] }
    } else {
      delete r.display
    }
    saveRules(r)
  }
})

const displayRelation = computed({
  get: () => parseRules().display?.relation || "and",
  set: (val: "and" | "or") => {
    const r = parseRules()
    if (r.display) r.display.relation = val
    saveRules(r)
  }
})

const displayConditions = computed(() => parseRules().display?.conditions || [])

function addDisplayCond() {
  const r = parseRules()
  if (!r.display) r.display = { relation: "and", conditions: [] }
  r.display.conditions.push({ source: displaySourceQuestions.value[0]?.key || "", op: "eq", value: "" })
  saveRules(r)
}

function updateDisplayCond(ci: number, field: keyof ConditionItem, val: any) {
  const r = parseRules()
  if (!r.display?.conditions[ci]) return
  const c = r.display.conditions[ci]
  if (field === "source") {
    c.source = val
    c.op = "eq"
    c.value = ""
  } else if (field === "op") {
    c.op = val
    if (["empty", "not_empty"].includes(val)) c.value = ""
  } else {
    c.value = val
  }
  saveRules(r)
}

function removeDisplayCond(ci: number) {
  const r = parseRules()
  if (!r.display) return
  r.display.conditions.splice(ci, 1)
  if (r.display.conditions.length === 0) delete r.display
  saveRules(r)
}

/* ====== 跳转规则 ====== */
const jumpRules = computed(() => parseRules().jumps || [])

function addJumpRule() {
  const r = parseRules()
  if (!r.jumps) r.jumps = []
  r.jumps.push({ relation: "and", conditions: [{ source: currentKey.value, op: "eq", value: "" }], target: "" })
  saveRules(r)
}

function removeJumpRule(ri: number) {
  const r = parseRules()
  if (!r.jumps) return
  r.jumps.splice(ri, 1)
  if (r.jumps.length === 0) delete r.jumps
  saveRules(r)
}

function updateJumpRelation(ri: number, val: "and" | "or") {
  const r = parseRules()
  if (r.jumps?.[ri]) {
    r.jumps[ri].relation = val
    saveRules(r)
  }
}

function updateJumpTarget(ri: number, val: string) {
  const r = parseRules()
  if (r.jumps?.[ri]) {
    r.jumps[ri].target = val
    saveRules(r)
  }
}

function addJumpCond(ri: number) {
  const r = parseRules()
  if (!r.jumps?.[ri]) return
  r.jumps[ri].conditions.push({ source: currentKey.value, op: "eq", value: "" })
  saveRules(r)
}

function updateJumpCond(ri: number, ci: number, field: keyof ConditionItem, val: any) {
  const r = parseRules()
  if (!r.jumps?.[ri]?.conditions[ci]) return
  const c = r.jumps[ri].conditions[ci]
  if (field === "source") {
    c.source = val
    c.op = "eq"
    c.value = ""
  } else if (field === "op") {
    c.op = val
    if (["empty", "not_empty"].includes(val)) c.value = ""
  } else {
    c.value = val
  }
  saveRules(r)
}

function removeJumpCond(ri: number, ci: number) {
  const r = parseRules()
  if (!r.jumps?.[ri]) return
  r.jumps[ri].conditions.splice(ci, 1)
  if (r.jumps[ri].conditions.length === 0) {
    r.jumps.splice(ri, 1)
    if (r.jumps.length === 0) delete r.jumps
  }
  saveRules(r)
}

/* ====== 辅助函数（按 stableKey 查找题目） ====== */
function findQuestionByKey(key: string): QuestionItem | undefined {
  return props.questions.find(q => getStableKey(q) === key)
}

function getQuestionType(key: string): string {
  return findQuestionByKey(key)?.type || ""
}

function getOperators(key: string) {
  const type = getQuestionType(key)
  const ops: { value: string, label: string }[] = [{ value: "eq", label: "等于" }, { value: "neq", label: "不等于" }]
  if (["input", "textarea", "checkbox"].includes(type)) {
    ops.push({ value: "contains", label: "包含" }, { value: "not_contains", label: "不包含" })
  }
  if (numericTypes.has(type)) {
    ops.push({ value: "gt", label: "大于" }, { value: "lt", label: "小于" }, { value: "gte", label: "≥" }, { value: "lte", label: "≤" })
  }
  if (type === "checkbox") {
    ops.push({ value: "selected", label: "已选中" }, { value: "not_selected", label: "未选中" })
  }
  ops.push({ value: "empty", label: "为空" }, { value: "not_empty", label: "不为空" })
  return ops
}

function getSourceOptions(key: string): { label: string, value: string }[] {
  const q = findQuestionByKey(key)
  if (!q?.options) return []
  try {
    const p = JSON.parse(q.options)
    return Array.isArray(p) ? p : []
  } catch {
    return []
  }
}

function isOptionType(key: string): boolean {
  return optionTypes.has(getQuestionType(key))
}
function isNumericType(key: string): boolean {
  return numericTypes.has(getQuestionType(key))
}
function needsValue(op: string): boolean {
  return !["empty", "not_empty"].includes(op)
}
</script>

<template>
  <div class="logic-panel">
    <div class="logic-qinfo">
      <el-tag size="small" type="info">
        {{ typeLabels[question.type] || question.type }}
      </el-tag>
      <span class="logic-qtitle">{{ question.title || "(未设置标题)" }}</span>
    </div>

    <el-alert v-if="isLayout" type="info" :closable="false" title="展示类题型不支持逻辑设计" style="margin-top: 12px" />

    <template v-else>
      <!-- ==================== 显示条件 ==================== -->
      <el-divider content-position="left">
        显示条件
      </el-divider>
      <div class="logic-section">
        <div class="logic-section-head">
          <span class="logic-section-desc">满足条件时才显示此题</span>
          <el-switch v-model="displayEnabled" size="small" />
        </div>

        <template v-if="displayEnabled">
          <el-alert v-if="displaySourceQuestions.length === 0" type="warning" :closable="false" title="前方无可用条件题目" style="margin-top: 8px" />
          <template v-else>
            <div class="logic-relation-row">
              满足以下
              <el-select v-model="displayRelation" size="small" style="width: 72px; margin: 0 4px">
                <el-option value="and" label="全部" />
                <el-option value="or" label="任一" />
              </el-select>
              条件
            </div>

            <div v-for="(cond, ci) in displayConditions" :key="ci" class="logic-cond-card">
              <div class="logic-cond-row">
                <el-select :model-value="cond.source" size="small" placeholder="选择题目" style="flex: 1" filterable @change="(v) => updateDisplayCond(ci, 'source', v)">
                  <el-option v-for="sq in displaySourceQuestions" :key="sq.key" :value="sq.key" :label="`Q${sq.num}. ${sq.question.title || '(未设置)'}`" />
                </el-select>
                <el-button :icon="Delete" size="small" type="danger" plain circle @click="removeDisplayCond(ci)" />
              </div>
              <div v-if="cond.source" class="logic-cond-row">
                <el-select :model-value="cond.op" size="small" style="width: 100px" @change="(v) => updateDisplayCond(ci, 'op', v)">
                  <el-option v-for="o in getOperators(cond.source)" :key="o.value" :value="o.value" :label="o.label" />
                </el-select>
                <template v-if="needsValue(cond.op)">
                  <el-select v-if="isOptionType(cond.source)" :model-value="cond.value" size="small" style="flex: 1" placeholder="选择值" filterable clearable @change="(v) => updateDisplayCond(ci, 'value', v ?? '')">
                    <el-option v-for="opt in getSourceOptions(cond.source)" :key="opt.value" :value="opt.value" :label="opt.label" />
                  </el-select>
                  <el-input-number v-else-if="isNumericType(cond.source)" :model-value="Number(cond.value) || 0" size="small" style="flex: 1" controls-position="right" @change="(v) => updateDisplayCond(ci, 'value', String(v ?? 0))" />
                  <el-input v-else :model-value="cond.value" size="small" style="flex: 1" placeholder="输入值" @update:model-value="(v) => updateDisplayCond(ci, 'value', v)" />
                </template>
              </div>
            </div>

            <el-button size="small" :icon="Plus" style="margin-top: 6px" @click="addDisplayCond">
              添加条件
            </el-button>
          </template>
        </template>
      </div>

      <!-- ==================== 跳转规则 ==================== -->
      <el-divider content-position="left">
        跳转规则
      </el-divider>
      <div class="logic-section">
        <div class="logic-section-desc" style="margin-bottom: 8px">
          回答此题后，按条件跳转到指定题目
        </div>

        <div v-for="(rule, ri) in jumpRules" :key="ri" class="logic-jump-card">
          <div class="logic-jump-head">
            <span class="logic-jump-label">规则 {{ ri + 1 }}</span>
            <el-button :icon="Delete" size="small" type="danger" text @click="removeJumpRule(ri)" />
          </div>

          <div class="logic-relation-row">
            满足以下
            <el-select :model-value="rule.relation" size="small" style="width: 72px; margin: 0 4px" @change="(v) => updateJumpRelation(ri, v)">
              <el-option value="and" label="全部" />
              <el-option value="or" label="任一" />
            </el-select>
            条件
          </div>

          <div v-for="(cond, ci) in rule.conditions" :key="ci" class="logic-cond-card">
            <div class="logic-cond-row">
              <el-select :model-value="cond.source" size="small" placeholder="选择题目" style="flex: 1" filterable @change="(v) => updateJumpCond(ri, ci, 'source', v)">
                <el-option v-for="sq in jumpSourceQuestions" :key="sq.key" :value="sq.key" :label="`Q${sq.num}. ${sq.question.title || '(未设置)'}`" />
              </el-select>
              <el-button :icon="Delete" size="small" type="danger" plain circle @click="removeJumpCond(ri, ci)" />
            </div>
            <div v-if="cond.source" class="logic-cond-row">
              <el-select :model-value="cond.op" size="small" style="width: 100px" @change="(v) => updateJumpCond(ri, ci, 'op', v)">
                <el-option v-for="o in getOperators(cond.source)" :key="o.value" :value="o.value" :label="o.label" />
              </el-select>
              <template v-if="needsValue(cond.op)">
                <el-select v-if="isOptionType(cond.source)" :model-value="cond.value" size="small" style="flex: 1" placeholder="选择值" filterable clearable @change="(v) => updateJumpCond(ri, ci, 'value', v ?? '')">
                  <el-option v-for="opt in getSourceOptions(cond.source)" :key="opt.value" :value="opt.value" :label="opt.label" />
                </el-select>
                <el-input-number v-else-if="isNumericType(cond.source)" :model-value="Number(cond.value) || 0" size="small" style="flex: 1" controls-position="right" @change="(v) => updateJumpCond(ri, ci, 'value', String(v ?? 0))" />
                <el-input v-else :model-value="cond.value" size="small" style="flex: 1" placeholder="输入值" @update:model-value="(v) => updateJumpCond(ri, ci, 'value', v)" />
              </template>
            </div>
          </div>

          <el-button size="small" :icon="Plus" text style="margin-top: 4px" @click="addJumpCond(ri)">
            添加条件
          </el-button>

          <div class="logic-jump-target">
            <span>→ 跳转到</span>
            <el-select :model-value="rule.target" size="small" style="flex: 1" placeholder="选择目标" @change="(v) => updateJumpTarget(ri, v)">
              <el-option v-for="t in jumpTargetOptions" :key="t.value" :value="t.value" :label="t.label" />
            </el-select>
          </div>
        </div>

        <el-button size="small" :icon="Plus" style="margin-top: 6px" @click="addJumpRule">
          添加跳转规则
        </el-button>
      </div>

      <!-- ==================== 说明 ==================== -->
      <el-divider />
      <div class="logic-tips">
        <p><b>显示条件</b>：设置后，此题仅在前面的题目答案满足条件时才会显示给填写者。</p>
        <p><b>跳转规则</b>：填写者回答此题后，如果答案满足条件，将跳过中间题目直接到达目标题。未命中任何规则则顺序进入下一题。</p>
        <p><b>条件关系</b>：「全部」= AND，「任一」= OR。</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.logic-panel {
  font-size: 13px;
}
.logic-qinfo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.logic-qtitle {
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.logic-section {
  margin-bottom: 4px;
}
.logic-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.logic-section-desc {
  color: #909399;
  font-size: 12px;
}
.logic-relation-row {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.logic-cond-card {
  background: #f9fafb;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 6px;
}
.logic-cond-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.logic-cond-row:last-child {
  margin-bottom: 0;
}
.logic-jump-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  background: #fafbfc;
}
.logic-jump-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.logic-jump-label {
  font-weight: 600;
  color: var(--el-color-primary);
  font-size: 13px;
}
.logic-jump-target {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--el-border-color-lighter);
  font-weight: 500;
  color: #303133;
}
.logic-tips {
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
}
.logic-tips p {
  margin: 0 0 4px;
}
.logic-tips b {
  color: #606266;
}
</style>
