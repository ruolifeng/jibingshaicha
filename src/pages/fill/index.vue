<script lang="ts" setup>
import type { AnswerItem, QuestionItem, QuestionnaireItem } from "@/pages/questionnaire/apis/type"
import { validateContentLimitNumber, validateIdCard } from "@@/utils/validate"
import { request } from "@/http/axios"
import QuestionFillRenderer from "./components/QuestionFillRenderer.vue"

const route = useRoute()
const questionnaireId = computed(() => route.params.id as string)

const questionnaire = ref<QuestionnaireItem | null>(null)
const questions = ref<QuestionItem[]>([])
const responseId = ref<string | number | null>(null)
const accessToken = ref<string | null>(null)
const answers = ref<Record<string, string>>({})
/** 任意题目答案变更时自增，供公式题建立可靠响应式依赖 */
const answersTick = ref(0)
const submitted = ref(false)
const loading = ref(true)
const submitting = ref(false)
const errorMsg = ref("")
/** 每次翻页/提交校验失败时自增，驱动各 QuestionFillRenderer 强制显示内联错误（比 boolean 更可靠，重复失败也能触发 watch） */
const submitAttemptCount = ref(0)

/**
 * 不接受用户输入的题型（不参与必填校验）：
 *  - 展示类（description / divider / image_display / page_break）
 *  - 自动计算（formula）：值由系统按公式计算后写入答案，但仍参与提交
 */
const nonFillableTypes = ["description", "divider", "image_display", "page_break", "formula"]
const layoutTypes = new Set(["divider", "page_break"])
function isLayoutType(type: string) {
  return layoutTypes.has(type)
}

const questionNumMap = computed(() => {
  const map: Record<string, number> = {}
  let num = 0
  for (const q of visibleQuestions.value) {
    if (!isLayoutType(q.type)) map[qid(q)] = ++num
  }
  return map
})

function qid(q: QuestionItem): string {
  return String(q.id ?? "")
}

function setAnswer(q: QuestionItem, v: string) {
  const id = qid(q)
  if (answers.value[id] === v) return
  answers.value[id] = v
  answersTick.value++
}

/* ====== 逻辑规则求值 ====== */
interface _CondItem { source: number, op: string, value: string }
interface _CondGroup { relation: "and" | "or", conditions: _CondItem[] }
interface _JumpRule extends _CondGroup { target: string }
interface _LogicRules { display?: _CondGroup, jumps?: _JumpRule[] }

function parseLogicRulesData(q: QuestionItem): _LogicRules {
  if (!q.logicRules) return {}
  try {
    return JSON.parse(q.logicRules)
  } catch {
    return {}
  }
}

function getAnswerBySort(sortOrder: number): string {
  const q = questions.value[sortOrder - 1]
  return q ? (answers.value[qid(q)] ?? "") : ""
}

/**
 * 兼容"附加填空"答案格式：当单选/多选题含有附加填空选项时，
 * 答案以 {"selected": "值" | ["值"], "inputs": {...}} 存储。
 * 逻辑条件求值时需提取实际选中值，否则字符串比较永远不匹配。
 *   - radio  → 返回 selected 字符串
 *   - checkbox → 返回 selected 数组的 JSON 字符串（与原有格式一致）
 */
function resolveAnswer(raw: string): string {
  if (!raw) return raw
  try {
    const p = JSON.parse(raw)
    if (p && typeof p === "object" && !Array.isArray(p) && "selected" in p) {
      const sel = p.selected
      return Array.isArray(sel) ? JSON.stringify(sel) : String(sel ?? "")
    }
  } catch { /* 非 JSON，直接返回原始值 */ }
  return raw
}

function evalCond(c: _CondItem): boolean {
  if (c.source <= 0 || c.source > questions.value.length) return false
  const raw = getAnswerBySort(c.source)
  const a = resolveAnswer(raw)
  switch (c.op) {
    case "eq": return a === c.value
    case "neq": return a !== c.value
    case "contains": return a.includes(c.value)
    case "not_contains": return !a.includes(c.value)
    case "gt": return Number(a) > Number(c.value)
    case "lt": return Number(a) < Number(c.value)
    case "gte": return Number(a) >= Number(c.value)
    case "lte": return Number(a) <= Number(c.value)
    case "empty": return !a || a === "[]" || a === "{}"
    case "not_empty": return !!a && a !== "[]" && a !== "{}"
    case "selected": { try {
      return (JSON.parse(a) as string[]).includes(c.value)
    } catch {
      return false
    } }
    case "not_selected": { try {
      return !(JSON.parse(a) as string[]).includes(c.value)
    } catch {
      return true
    } }
    default: return false
  }
}

function evalGroup(g: _CondGroup): boolean {
  if (!g.conditions.length) return true
  return g.relation === "and" ? g.conditions.every(evalCond) : g.conditions.some(evalCond)
}

const visibleIndices = computed(() => {
  const indices: number[] = []
  const visited = new Set<number>()
  let i = 0
  while (i < questions.value.length) {
    if (visited.has(i)) break
    visited.add(i)
    const q = questions.value[i]
    const rules = parseLogicRulesData(q)

    if (rules.display && rules.display.conditions.length > 0 && !evalGroup(rules.display)) {
      i++
      continue
    }

    indices.push(i)

    let jumped = false
    if (rules.jumps && rules.jumps.length > 0) {
      for (const jr of rules.jumps) {
        if (jr.target && evalGroup(jr)) {
          if (jr.target === "end") return indices
          const ti = Number(jr.target) - 1
          if (!Number.isNaN(ti) && ti > i && ti < questions.value.length) {
            i = ti
            jumped = true
            break
          }
        }
      }
    }
    if (jumped) continue
    i++
  }
  return indices
})

const visibleQuestions = computed(() => visibleIndices.value.map(i => questions.value[i]))

// ====== 分页 ======
const pages = computed(() => {
  const result: QuestionItem[][] = [[]]
  for (const q of visibleQuestions.value) {
    if (q.type === "page_break") {
      if (result[result.length - 1].length > 0) result.push([])
    } else {
      result[result.length - 1].push(q)
    }
  }
  return result.filter(p => p.length > 0)
})
const currentPage = ref(0)
const totalPages = computed(() => pages.value.length)
const currentPageQuestions = computed(() => pages.value[currentPage.value] || [])
const isLastPage = computed(() => currentPage.value >= totalPages.value - 1)
const hasMultiplePages = computed(() => totalPages.value > 1)

watch(visibleQuestions, () => {
  if (currentPage.value >= totalPages.value) currentPage.value = Math.max(0, totalPages.value - 1)
})

/** 校验单个字段的内容限制格式，返回错误信息或 null */
function checkCellLimit(limit: string, val: string, label: string): string | null {
  if (!val) return null
  if (limit === "phone" && !/^1[3-9]\d{9}$/.test(val)) return `${label}手机号格式不正确（需11位）`
  if (limit === "id_card" && !validateIdCard(val)) return `${label}身份证号码无效，请检查位数、出生日期及校验码`
  if (limit === "email" && !/^[^\s@]+@[^\s@][^\s.@]*\.[^\s@]+$/.test(val)) return `${label}邮箱格式不正确`
  if (limit === "date_format" && !/^\d{4}-\d{2}-\d{2}$/.test(val)) return `${label}日期格式应为 YYYY-MM-DD`
  return null
}

/** 校验 input 类型的内容限制格式 */
function checkInputContentLimit(q: QuestionItem, val: string): string | null {
  if (!q.validationRules || !val) return null
  let rules: Record<string, any> = {}
  try {
    rules = JSON.parse(q.validationRules)
  } catch {
    return null
  }
  const limit: string = rules.contentLimit || "none"
  const label = `「${q.title}」`

  const err = checkCellLimit(limit, val, label)
  if (err) return err

  return validateContentLimitNumber(limit, val, {
    decimalPlaces: rules.decimalPlaces,
    rangeMin: rules.rangeMin,
    rangeMax: rules.rangeMax
  }, label)
}

/** 校验 matrix_input 类型各单元格的内容限制（answer 为 JSON Object，key 格式 rowValue__colValue） */
function checkMatrixInputContentLimit(q: QuestionItem, ans: string): string | null {
  let opts: { rows: { value: string, label: string }[], cols: { value: string, label: string, contentLimit?: string, rangeMin?: number, rangeMax?: number, decimalPlaces?: number }[] }
  try {
    opts = JSON.parse(q.options || "{}")
  } catch {
    return null
  }
  if (!opts.cols?.length) return null
  let cells: Record<string, string> = {}
  try {
    cells = JSON.parse(ans)
  } catch {
    return null
  }
  const label = `「${q.title}」`
  for (const col of opts.cols) {
    if (!col.contentLimit) continue
    for (const row of (opts.rows || [])) {
      const cellLabel = `${label}（${row.label} - ${col.label}）`
      const val = cells[`${row.value}__${col.value}`] || ""
      const err = checkCellLimit(col.contentLimit, val, cellLabel)
        || validateContentLimitNumber(col.contentLimit, val, {
          decimalPlaces: col.decimalPlaces,
          rangeMin: col.rangeMin,
          rangeMax: col.rangeMax
        }, cellLabel)
      if (err) return err
    }
  }
  return null
}

/** 校验 dynamic_table 类型各单元格的内容限制（answer 为 JSON Array，每项为行对象） */
function checkDynamicTableContentLimit(q: QuestionItem, ans: string): string | null {
  let cols: { value: string, label: string, contentLimit?: string, rangeMin?: number, rangeMax?: number, decimalPlaces?: number }[]
  try {
    cols = JSON.parse(q.options || "[]")
  } catch {
    return null
  }
  if (!cols.length) return null
  let rows: Record<string, string>[]
  try {
    rows = JSON.parse(ans)
  } catch {
    return null
  }
  const label = `「${q.title}」`
  for (let ri = 0; ri < rows.length; ri++) {
    for (const col of cols) {
      if (!col.contentLimit) continue
      const cellLabel = `${label}第${ri + 1}行（${col.label}）`
      const val = rows[ri][col.value] || ""
      const err = checkCellLimit(col.contentLimit, val, cellLabel)
        || validateContentLimitNumber(col.contentLimit, val, {
          decimalPlaces: col.decimalPlaces,
          rangeMin: col.rangeMin,
          rangeMax: col.rangeMax
        }, cellLabel)
      if (err) return err
    }
  }
  return null
}

/** 检查矩阵多选题是否至少有一行选中了选项（用于必填校验） */
function isMatrixCheckboxEmpty(ans: string): boolean {
  try {
    const obj = JSON.parse(ans)
    if (!obj || typeof obj !== "object") return true
    return Object.values(obj).every(v => !Array.isArray(v) || (v as string[]).length === 0)
  } catch {
    return true
  }
}

/** 校验多选题的可选数量限制 */
function checkCheckboxSelectLimit(q: QuestionItem, ans: string): string | null {
  let rules: Record<string, any> = {}
  try {
    rules = JSON.parse(q.validationRules || "{}")
  } catch {
    return null
  }
  const minSelect: number | undefined = typeof rules.minSelect === "number" ? rules.minSelect : undefined
  const maxSelect: number | undefined = typeof rules.maxSelect === "number" ? rules.maxSelect : undefined
  if (minSelect === undefined && maxSelect === undefined) return null

  let selected: string[] = []
  try {
    const p = JSON.parse(ans)
    if (Array.isArray(p)) selected = p
    else if (p && typeof p === "object" && Array.isArray(p.selected)) selected = p.selected
  } catch {
    return null
  }

  const count = selected.length
  if (minSelect !== undefined && count < minSelect) return `「${q.title}」至少需要选择 ${minSelect} 项`
  if (maxSelect !== undefined && count > maxSelect) return `「${q.title}」最多只能选择 ${maxSelect} 项`
  return null
}

function validatePage(pageIdx: number): string | null {
  const qs = pages.value[pageIdx] || []
  for (const q of qs) {
    if (nonFillableTypes.includes(q.type)) continue
    const ans = answers.value[qid(q)]
    if (q.required === 1) {
      if (!ans || String(ans).trim() === "" || ans === "{}" || ans === "[]") return `请完成「${q.title}」`
      if (q.type === "matrix_checkbox" && isMatrixCheckboxEmpty(String(ans))) return `请完成「${q.title}」`
    }
    if (!ans) continue
    if (q.type === "checkbox") {
      const err = checkCheckboxSelectLimit(q, String(ans))
      if (err) return err
    }
    if (q.type === "input") {
      const err = checkInputContentLimit(q, String(ans))
      if (err) return err
    }
    if (q.type === "matrix_input") {
      const err = checkMatrixInputContentLimit(q, String(ans))
      if (err) return err
    }
    if (q.type === "dynamic_table") {
      const err = checkDynamicTableContentLimit(q, String(ans))
      if (err) return err
    }
  }
  return null
}

function nextPage() {
  const err = validatePage(currentPage.value)
  if (err) {
    submitAttemptCount.value++
    ElMessage.warning(err)
    return
  }
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
    window.scrollTo({ top: 0, behavior: "smooth" })
  }
}

function prevPage() {
  if (currentPage.value > 0) {
    currentPage.value--
    window.scrollTo({ top: 0, behavior: "smooth" })
  }
}

const progressPercent = computed(() => {
  const fillable = visibleQuestions.value.filter(q => !nonFillableTypes.includes(q.type))
  if (fillable.length === 0) return 0
  const answered = fillable.filter((q) => {
    const a = answers.value[qid(q)]
    if (a == null || String(a).trim() === "" || a === "{}" || a === "[]") return false
    if (q.type === "matrix_checkbox") return !isMatrixCheckboxEmpty(String(a))
    return true
  }).length
  return Math.round((answered / fillable.length) * 100)
})

async function init() {
  try {
    const { data } = await request<ApiResponseData<{
      responseId: number
      accessToken?: string
      questionnaire: QuestionnaireItem
      questions: QuestionItem[]
    }>>({ url: `public/fill/${questionnaireId.value}`, method: "get" })

    questionnaire.value = data.questionnaire
    questions.value = data.questions
    responseId.value = data.responseId
    accessToken.value = data.accessToken || null
  } catch (e: any) {
    errorMsg.value = e?.message || "问卷加载失败"
  } finally {
    loading.value = false
  }
}

function validate(): string | null {
  for (const q of visibleQuestions.value) {
    if (nonFillableTypes.includes(q.type)) continue
    const ans = answers.value[qid(q)]
    if (q.required === 1) {
      if (!ans || String(ans).trim() === "" || ans === "{}" || ans === "[]") return `请完成第${q.sortOrder}题「${q.title}」`
      if (q.type === "matrix_checkbox" && isMatrixCheckboxEmpty(String(ans))) return `请完成第${q.sortOrder}题「${q.title}」`
    }
    if (!ans) continue
    if (q.type === "checkbox") {
      const err = checkCheckboxSelectLimit(q, String(ans))
      if (err) return err
    }
    if (q.type === "input") {
      const err = checkInputContentLimit(q, String(ans))
      if (err) return err
    }
    if (q.type === "matrix_input") {
      const err = checkMatrixInputContentLimit(q, String(ans))
      if (err) return err
    }
    if (q.type === "dynamic_table") {
      const err = checkDynamicTableContentLimit(q, String(ans))
      if (err) return err
    }
  }
  return null
}

async function handleSubmit() {
  const err = validate()
  if (err) {
    submitAttemptCount.value++
    ElMessage.warning(err)
    return
  }
  submitting.value = true
  try {
    const visibleIds = new Set(visibleQuestions.value.map(q => q.id))
    const answerList: AnswerItem[] = questions.value
      .filter(q => visibleIds.has(q.id) && answers.value[qid(q)] != null)
      .map(q => ({ questionId: qid(q), answerValue: String(answers.value[qid(q)]) }))

    await request<ApiResponseData<null>>({
      url: `public/fill/${responseId.value}/submit`,
      method: "post",
      data: answerList,
      params: accessToken.value ? { token: accessToken.value } : undefined
    })
    submitted.value = true
    clearDraft()
  } catch {
    ElMessage.error("提交失败，请重试")
  } finally {
    submitting.value = false
  }
}

// ====== 草稿 ======
const draftKey = computed(() => `questionnaire_draft_${questionnaireId.value}`)

function saveDraft() {
  if (submitted.value || !questions.value.length) return
  try {
    localStorage.setItem(draftKey.value, JSON.stringify(answers.value))
  } catch { /* full */ }
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(draftKey.value)
    if (raw) {
      const saved = JSON.parse(raw) as Record<number, string>
      for (const [k, v] of Object.entries(saved)) {
        if (!answers.value[Number(k)]) answers.value[Number(k)] = v
      }
      answersTick.value++
    }
  } catch { /* empty */ }
}

function clearDraft() {
  try {
    localStorage.removeItem(draftKey.value)
  } catch { /* empty */ }
}

// ====== 计时器 ======
const elapsedSeconds = ref(0)
let timerInterval: ReturnType<typeof setInterval> | null = null

const elapsedTimeText = computed(() => {
  const h = Math.floor(elapsedSeconds.value / 3600)
  const m = Math.floor((elapsedSeconds.value % 3600) / 60)
  const s = elapsedSeconds.value % 60
  if (h > 0) return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
})

function startTimer() {
  if (timerInterval) return
  timerInterval = setInterval(() => {
    elapsedSeconds.value++
  }, 1000)
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

let draftTimer: ReturnType<typeof setInterval> | null = null
watch(submitted, (val) => {
  if (val) {
    clearDraft()
    stopTimer()
    if (draftTimer) clearInterval(draftTimer)
  }
})
onMounted(() => {
  draftTimer = setInterval(saveDraft, 30000)
})
onUnmounted(() => {
  if (draftTimer) clearInterval(draftTimer)
  stopTimer()
})

async function initWithDraft() {
  await init()
  loadDraft()
  if (!errorMsg.value) startTimer()
}
initWithDraft()
</script>

<template>
  <div class="fill-container">
    <div v-if="loading" class="fill-loading">
      <el-icon class="is-loading" :size="40">
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M512 64a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32" /></svg>
      </el-icon>
      <p>问卷加载中...</p>
    </div>

    <div v-else-if="errorMsg" class="fill-error">
      <el-result icon="warning" :title="errorMsg" sub-title="请检查链接是否正确" />
    </div>

    <div v-else-if="submitted" class="fill-success">
      <el-result icon="success" title="提交成功" sub-title="感谢您的参与！" />
    </div>

    <template v-else-if="questionnaire">
      <div class="fill-header">
        <div class="fill-header-top">
          <h2>{{ questionnaire.title }}</h2>
          <div class="fill-timer">
            <el-icon><svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M512 896a384 384 0 1 0 0-768 384 384 0 0 0 0 768m0 64a448 448 0 1 1 0-896 448 448 0 0 1 0 896" /><path fill="currentColor" d="M480 256a32 32 0 0 1 32 32v256a32 32 0 0 1-64 0V288a32 32 0 0 1 32-32" /><path fill="currentColor" d="M480 512h192a32 32 0 1 1 0 64H480a32 32 0 0 1 0-64" /></svg></el-icon>
            <span>{{ elapsedTimeText }}</span>
          </div>
        </div>
        <p v-if="questionnaire.description">
          {{ questionnaire.description }}
        </p>
        <el-progress :percentage="progressPercent" :stroke-width="8" style="margin-top: 8px" />
      </div>

      <div class="fill-questions">
        <template v-for="q in currentPageQuestions" :key="q.id">
          <!-- 布局元素：分割线 -->
          <div v-if="q.type === 'divider'" class="fill-layout">
            <el-divider />
          </div>
          <!-- 布局元素：图片展示 -->
          <div v-else-if="q.type === 'image_display'" class="fill-question">
            <div v-if="q.title" class="fill-question-title">
              {{ q.title }}
            </div>
            <div v-if="q.description" class="fill-question-desc">
              {{ q.description }}
            </div>
            <QuestionFillRenderer
              :question="q"
              :model-value="answers[qid(q)] ?? ''"
              :all-questions="questions"
              :answers-map="answers"
              :answers-tick="answersTick"
              :resolve-answer="(qq) => answers[qid(qq)] ?? ''"
              formula-key-type="sortOrder"
              @update:model-value="(v) => setAnswer(q, v)"
            />
          </div>
          <!-- 布局元素：文字描述 -->
          <div v-else-if="q.type === 'description'" class="fill-question">
            <div v-if="q.title" class="fill-question-title">
              {{ q.title }}
            </div>
            <QuestionFillRenderer
              :question="q"
              :model-value="answers[qid(q)] ?? ''"
              :all-questions="questions"
              :answers-map="answers"
              :answers-tick="answersTick"
              :resolve-answer="(qq) => answers[qid(qq)] ?? ''"
              formula-key-type="sortOrder"
              @update:model-value="(v) => setAnswer(q, v)"
            />
          </div>
          <!-- 普通题目 -->
          <div v-else class="fill-question">
            <div class="fill-question-title">
              <span class="fill-question-num">{{ questionNumMap[qid(q)] }}.</span>
              {{ q.title }}
              <span v-if="q.required === 1" class="fill-required">*</span>
            </div>
            <div v-if="q.description && q.type !== 'description'" class="fill-question-desc">
              {{ q.description }}
            </div>
            <QuestionFillRenderer
              :question="q"
              :model-value="answers[qid(q)] ?? ''"
              :all-questions="questions"
              :answers-map="answers"
              :answers-tick="answersTick"
              :resolve-answer="(qq) => answers[qid(qq)] ?? ''"
              formula-key-type="sortOrder"
              :submit-attempt-count="submitAttemptCount"
              @update:model-value="(v) => setAnswer(q, v)"
            />
          </div>
        </template>
      </div>

      <div class="fill-footer">
        <div v-if="hasMultiplePages" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
          <span style="color: #909399; font-size: 13px">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
        </div>
        <div style="display: flex; gap: 12px">
          <el-button v-if="hasMultiplePages && currentPage > 0" size="large" style="flex: 1" @click="prevPage">
            上一页
          </el-button>
          <el-button v-if="hasMultiplePages && !isLastPage" type="primary" size="large" style="flex: 1" @click="nextPage">
            下一页
          </el-button>
          <el-button v-if="!hasMultiplePages || isLastPage" type="primary" size="large" :loading="submitting" style="flex: 1" @click="handleSubmit">
            提交问卷
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.fill-container {
  max-width: 640px;
  margin: 0 auto;
  padding: 20px 16px;
  min-height: 100vh;
  background: #f5f7fa;
}
.fill-loading,
.fill-error,
.fill-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #909399;
}
.fill-header {
  background: #fff;
  border-radius: 12px;
  padding: 24px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.fill-header-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.fill-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
  flex: 1;
}
.fill-timer {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
  padding: 4px 10px;
  background: #f5f7fa;
  border-radius: 20px;
  flex-shrink: 0;
}
.fill-header p {
  margin: 0 0 4px;
  color: #909399;
  font-size: 14px;
}
.fill-questions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.fill-layout {
  padding: 0 8px;
}
.fill-question {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.fill-question-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}
.fill-question-num {
  color: var(--el-color-primary);
  font-weight: 600;
  margin-right: 4px;
}
.fill-required {
  color: var(--el-color-danger);
  margin-left: 2px;
}
.fill-question-desc {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.fill-footer {
  margin-top: 24px;
  padding-bottom: 40px;
}
</style>
