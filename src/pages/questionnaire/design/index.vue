<script lang="ts" setup>
import type { QuestionItem, QuestionnaireItem } from "../apis/type"
import { ArrowLeft, CopyDocument, Delete, Document, Loading, Plus, Rank } from "@element-plus/icons-vue"
import draggable from "vuedraggable"
import QuestionFillRenderer from "../../fill/components/QuestionFillRenderer.vue"
import { getQuestionnaireDetailApi, getQuestionsApi, saveQuestionsApi } from "../apis"
import LogicDesignPanel from "./components/LogicDesignPanel.vue"
import QuestionConfigPanel from "./components/QuestionConfigPanel.vue"
import QuestionDesignRenderer from "./components/QuestionDesignRenderer.vue"
import { extractTitleCode, remapFormulaRefs } from "./utils/formula"

const route = useRoute()
const router = useRouter()
const qId = computed(() => route.params.id as string)

const questionnaire = ref<QuestionnaireItem | null>(null)
const questions = ref<QuestionItem[]>([])
const saving = ref(false)
const activeQuestion = ref<number | null>(null)
const rightTab = ref<"config" | "logic">("config")

const typeOptions = [
  { label: "单行文本", value: "input", group: "基础" },
  { label: "多行文本", value: "textarea", group: "基础" },
  { label: "数字组件", value: "number", group: "基础" },
  { label: "日期时间", value: "date", group: "基础" },
  { label: "滑块组件", value: "slider", group: "基础" },
  { label: "单选框组", value: "radio", group: "选择" },
  { label: "多选框组", value: "checkbox", group: "选择" },
  { label: "下拉选择", value: "dropdown", group: "选择" },
  { label: "级联选择", value: "cascader", group: "选择" },
  { label: "图片选择", value: "image_choice", group: "选择" },
  { label: "排序题型", value: "sort", group: "选择" },
  { label: "多项填空", value: "multi_input", group: "填空" },
  { label: "横向填空", value: "inline_input", group: "填空" },
  { label: "评分组件", value: "rating", group: "评分" },
  { label: "NPS题", value: "nps", group: "评分" },
  { label: "矩阵单选", value: "matrix_radio", group: "矩阵" },
  { label: "矩阵多选", value: "matrix_checkbox", group: "矩阵" },
  { label: "矩阵填空", value: "matrix_input", group: "矩阵" },
  { label: "矩阵量表", value: "matrix_scale", group: "矩阵" },
  { label: "复合矩阵", value: "matrix_complex", group: "矩阵" },
  { label: "子表单", value: "dynamic_table", group: "高级" },
  { label: "自动计算", value: "formula", group: "高级" },
  { label: "文件上传", value: "file_upload", group: "高级" },
  { label: "图片上传", value: "image_upload", group: "高级" },
  { label: "电子签名", value: "signature", group: "高级" },
  { label: "文字描述", value: "description", group: "展示" },
  { label: "分割线", value: "divider", group: "展示" },
  { label: "图片展示", value: "image_display", group: "展示" },
  { label: "分页", value: "page_break", group: "展示" }
]

const typeGroups = [...new Set(typeOptions.map(t => t.group))]
const typeLabel: Record<string, string> = Object.fromEntries(typeOptions.map(t => [t.value, t.label]))

const groupedTypes: Record<string, typeof typeOptions> = {}
for (const g of typeGroups) groupedTypes[g] = typeOptions.filter(t => t.group === g)

const layoutTypes = new Set(["divider", "page_break"])
function isLayoutType(type: string) {
  return layoutTypes.has(type)
}

/* ====== 稳定 key 工具（设计页内存用 stableKey，DB 存 sortOrder） ====== */
/** 已保存题目用 id 字符串，未保存新题用 "t{_tempKey}" */
function getStableKey(q: QuestionItem): string {
  return q.id != null ? String(q.id) : `t${q._tempKey}`
}

/**
 * 从 DB 加载后调用：将 logicRules 里的 sortOrder 数字引用转换为 stableKey 字符串
 * 以便移动题目时无需任何重映射
 */
function convertLogicToStableKeys(qs: QuestionItem[]) {
  const sortToKey = new Map<number, string>()
  qs.forEach((q, i) => sortToKey.set(i + 1, getStableKey(q)))

  for (const q of qs) {
    if (!q.logicRules) continue
    try {
      const rules = JSON.parse(q.logicRules) as { display?: any, jumps?: any[] }
      if (rules.display?.conditions) {
        for (const c of rules.display.conditions) {
          const k = sortToKey.get(Number(c.source))
          if (k) c.source = k
        }
      }
      if (rules.jumps) {
        for (const j of rules.jumps) {
          for (const c of j.conditions) {
            const k = sortToKey.get(Number(c.source))
            if (k) c.source = k
          }
          if (j.target && j.target !== "end") {
            const k = sortToKey.get(Number(j.target))
            if (k) j.target = k
          }
        }
      }
      q.logicRules = JSON.stringify(rules)
    } catch { /* skip */ }
  }
}

/**
 * 保存前调用（在深拷贝上）：将 stableKey 引用转回 sortOrder 数字，供 DB 和填写页使用
 */
function convertLogicToSortOrders(qs: QuestionItem[]) {
  const keyToSort = new Map<string, number>()
  qs.forEach((q, i) => keyToSort.set(getStableKey(q), i + 1))

  for (const q of qs) {
    if (!q.logicRules) continue
    try {
      const rules = JSON.parse(q.logicRules) as { display?: any, jumps?: any[] }
      if (rules.display?.conditions) {
        for (const c of rules.display.conditions) {
          if (typeof c.source === "string") c.source = keyToSort.get(c.source) ?? 0
        }
      }
      if (rules.jumps) {
        for (const j of rules.jumps) {
          for (const c of j.conditions) {
            if (typeof c.source === "string") c.source = keyToSort.get(c.source) ?? 0
          }
          if (j.target && j.target !== "end") {
            const sort = keyToSort.get(j.target)
            j.target = sort != null ? String(sort) : ""
          }
        }
      }
      q.logicRules = JSON.stringify(rules)
    } catch { /* skip */ }
  }
}

/**
 * 加载后调用：将自动计算题公式中的 sortOrder 占位符转为 stableKey
 * 之后无论题目如何拖拽移动，公式引用都不需要重写
 */
function convertFormulaToStableKeys(qs: QuestionItem[]) {
  const sortToKey = new Map<number, string>()
  const fillableNumToKey = new Map<number, string>()
  const titleCodeToKey = new Map<string, string>()
  let fillableNum = 0
  qs.forEach((q, i) => {
    const sk = getStableKey(q)
    sortToKey.set(i + 1, sk)
    if (!isLayoutType(q.type)) {
      fillableNum++
      fillableNumToKey.set(fillableNum, sk)
    }
    const code = extractTitleCode(q.title)
    if (code) {
      titleCodeToKey.set(code, sk)
      titleCodeToKey.set(code.toUpperCase(), sk)
    }
  })
  for (const q of qs) {
    if (q.type !== "formula" || !q.validationRules) continue
    try {
      const v = JSON.parse(q.validationRules)
      if (typeof v.formula === "string" && v.formula) {
        v.formula = remapFormulaRefs(v.formula, (k) => {
          const qMatch = /^Q(\d+)$/i.exec(k)
          if (qMatch) return fillableNumToKey.get(Number(qMatch[1])) ?? null
          const n = Number(k)
          if (!Number.isNaN(n)) return sortToKey.get(n) ?? null
          return titleCodeToKey.get(k) ?? titleCodeToKey.get(k.toUpperCase()) ?? null
        })
        q.validationRules = JSON.stringify(v)
      }
    } catch { /* skip */ }
  }
}

/** 保存前调用（在深拷贝上）：自动计算题公式中的 stableKey / Q题号 引用转为 sortOrder */
function convertFormulaToSortOrders(qs: QuestionItem[]) {
  const keyToSort = new Map<string, number>()
  const fillableNumToSort = new Map<number, number>()
  const titleCodeToSort = new Map<string, number>()
  let fillableNum = 0
  qs.forEach((q, i) => {
    keyToSort.set(getStableKey(q), i + 1)
    if (!isLayoutType(q.type)) {
      fillableNum++
      fillableNumToSort.set(fillableNum, i + 1)
    }
    const code = extractTitleCode(q.title)
    if (code) {
      titleCodeToSort.set(code, i + 1)
      titleCodeToSort.set(code.toUpperCase(), i + 1)
    }
  })
  for (const q of qs) {
    if (q.type !== "formula" || !q.validationRules) continue
    try {
      const v = JSON.parse(q.validationRules)
      if (typeof v.formula === "string" && v.formula) {
        v.formula = remapFormulaRefs(v.formula, (k) => {
          const qMatch = /^Q(\d+)$/i.exec(k)
          if (qMatch) {
            const sort = fillableNumToSort.get(Number(qMatch[1]))
            return sort != null ? String(sort) : null
          }
          const byTitle = titleCodeToSort.get(k) ?? titleCodeToSort.get(k.toUpperCase())
          if (byTitle != null) return String(byTitle)
          const n = keyToSort.get(k)
          return n != null ? String(n) : null
        })
        q.validationRules = JSON.stringify(v)
      }
    } catch { /* skip */ }
  }
}

const questionCount = computed(() => questions.value.filter(q => !isLayoutType(q.type)).length)

const questionNumMap = computed(() => {
  const map: Record<number, number> = {}
  let num = 0
  questions.value.forEach((q, idx) => {
    if (!isLayoutType(q.type)) map[idx] = ++num
  })
  return map
})

// 总页数 = page_break 数量 + 1
const totalPages = computed(() =>
  questions.value.filter(q => q.type === "page_break").length + 1
)

// 获取某个 page_break 元素所处的页码信息（结束哪页、开始哪页）
function getPageBreakInfo(idx: number) {
  let breakNum = 0
  for (let i = 0; i <= idx; i++) {
    if (questions.value[i].type === "page_break") breakNum++
  }
  return { endPage: breakNum, startPage: breakNum + 1 }
}

// 预览当前页（previewVisible 在下方声明，watch 放到 previewVisible 声明之后）
const previewPage = ref(1)

async function fetchData() {
  const { data: q } = await getQuestionnaireDetailApi(qId.value)
  questionnaire.value = q
  const { data: qs } = await getQuestionsApi(qId.value)
  // 加载后将 sortOrder-based logicRules / formula 转为 stableKey-based，移动题目时无需重映射
  convertLogicToStableKeys(qs)
  convertFormulaToStableKeys(qs)
  questions.value = qs
  if (qs.length > 0) activeQuestion.value = 0
}

let _tempKeyCounter = 0
function createQuestion(type: string): QuestionItem {
  const defaultOpts = JSON.stringify([{ label: "选项1", value: "1" }, { label: "选项2", value: "2" }])
  const nonRequired = ["description", "divider", "image_display", "page_break", "formula"]
  const q: QuestionItem = {
    sortOrder: questions.value.length + 1,
    type,
    title: "",
    required: nonRequired.includes(type) ? 0 : 1,
    pageNumber: 1,
    options: undefined,
    _tempKey: ++_tempKeyCounter
  }
  if (["radio", "checkbox", "dropdown"].includes(type)) {
    q.options = defaultOpts
  } else if (type === "cascader") {
    q.options = JSON.stringify([
      { label: "一级选项1", value: "1", children: [{ label: "二级选项1", value: "1-1" }, { label: "二级选项2", value: "1-2" }] },
      { label: "一级选项2", value: "2", children: [{ label: "二级选项1", value: "2-1" }] }
    ])
  } else if (type === "multi_input") {
    q.options = JSON.stringify([{ label: "字段1", value: "field1" }, { label: "字段2", value: "field2" }])
  } else if (type === "inline_input") {
    q.options = JSON.stringify([{ label: "前缀文字", value: "prefix" }, { label: "中间文字", value: "middle" }, { label: "后缀文字", value: "__suffix_1", inputHidden: true }])
  } else if (type === "rating") {
    q.validationRules = JSON.stringify({ max: 5 })
  } else if (type === "nps") {
    q.validationRules = JSON.stringify({ min: 0, max: 10 })
  } else if (type === "matrix_radio" || type === "matrix_input" || type === "matrix_checkbox") {
    q.options = JSON.stringify({
      rows: [{ label: "行1", value: "r1" }, { label: "行2", value: "r2" }],
      cols: [{ label: "列1", value: "c1" }, { label: "列2", value: "c2" }]
    })
  } else if (type === "matrix_complex") {
    q.options = JSON.stringify({
      rows: [
        { label: "粮谷类（按生重记录）", value: "D12" },
        { label: "粗杂粮类（按生重记录）", value: "D13" }
      ],
      columns: [
        {
          key: "a",
          label: "是否食用",
          type: "radio",
          width: "120px",
          options: [
            { label: "1 是", value: "1" },
            { label: "2 否", value: "2" }
          ]
        },
        {
          key: "b",
          label: "食用频率（只填其中一项）",
          type: "freq",
          showWhen: { column: "a", op: "eq", value: "1" },
          units: [
            { label: "次/天", value: "day" },
            { label: "次/周", value: "week" },
            { label: "次/月", value: "month" },
            { label: "次/年", value: "year" }
          ]
        },
        {
          key: "amount",
          label: "平均每次食用量",
          type: "input",
          inputType: "decimal",
          suffix: "两",
          showWhen: { column: "a", op: "eq", value: "1" }
        }
      ]
    })
  } else if (type === "dynamic_table") {
    q.options = JSON.stringify([{ label: "列1", value: "col1" }, { label: "列2", value: "col2" }])
  } else if (type === "number") {
    q.validationRules = JSON.stringify({ min: 0, max: 100, step: 1, precision: 0 })
  } else if (type === "date") {
    q.validationRules = JSON.stringify({ dateType: "date" })
  } else if (type === "slider") {
    q.validationRules = JSON.stringify({ min: 0, max: 100, step: 1, showInput: false })
  } else if (type === "sort") {
    q.options = JSON.stringify([{ label: "选项1", value: "1" }, { label: "选项2", value: "2" }, { label: "选项3", value: "3" }])
  } else if (type === "image_choice") {
    q.options = JSON.stringify([{ label: "选项1", value: "1", imageUrl: "" }, { label: "选项2", value: "2", imageUrl: "" }])
    q.validationRules = JSON.stringify({ multiple: false })
  } else if (type === "matrix_scale") {
    q.options = JSON.stringify({
      rows: [{ label: "行1", value: "r1" }, { label: "行2", value: "r2" }],
      scaleMin: 1,
      scaleMax: 5,
      scaleLabels: { 1: "非常不满意", 5: "非常满意" }
    })
  } else if (type === "image_display") {
    q.options = JSON.stringify({ imageUrl: "", alt: "" })
  } else if (type === "file_upload") {
    q.validationRules = JSON.stringify({ accept: "", maxSize: 10, maxCount: 1 })
  } else if (type === "image_upload") {
    q.validationRules = JSON.stringify({ maxSize: 5, maxCount: 3 })
  } else if (type === "signature") {
    q.validationRules = JSON.stringify({ penColor: "#222222", penWidth: 2, height: 160, hint: "请在此处手写签名" })
  } else if (type === "formula") {
    q.validationRules = JSON.stringify({ formula: "", precision: 2, unit: "" })
  }
  return q
}

function addQuestion(type: string) {
  const q = createQuestion(type)
  // 若有选中题目，插入到其后面；否则追加到末尾
  const insertAt = activeQuestion.value !== null ? activeQuestion.value + 1 : questions.value.length
  questions.value.splice(insertAt, 0, q)
  activeQuestion.value = insertAt
}

function cloneTypeToQuestion(typeOpt: { value: string }): QuestionItem {
  return createQuestion(typeOpt.value)
}

function onQuestionAdded(evt: { newIndex?: number }) {
  if (evt.newIndex != null) activeQuestion.value = evt.newIndex
}

function onDragEnd(evt: { oldIndex?: number, newIndex?: number }) {
  if (evt.newIndex != null) activeQuestion.value = evt.newIndex
}

function questionItemKey(el: QuestionItem) {
  return el.id || el._tempKey || el.sortOrder
}

function duplicateQuestion(idx: number) {
  const q = JSON.parse(JSON.stringify(questions.value[idx])) as QuestionItem
  q.id = undefined
  q.logicRules = undefined // 副本不继承逻辑规则
  q.sortOrder = questions.value.length + 1
  q._tempKey = ++_tempKeyCounter // 确保副本有唯一的 stableKey
  questions.value.splice(idx + 1, 0, q)
  activeQuestion.value = idx + 1
}

function removeQuestion(idx: number) {
  questions.value.splice(idx, 1)
  // stableKey 模式下只需清理失效引用，无需数字重映射
  cleanLogicRules(questions.value)
  if (activeQuestion.value !== null && activeQuestion.value >= questions.value.length) {
    activeQuestion.value = questions.value.length > 0 ? questions.value.length - 1 : null
  }
}

function moveQuestion(idx: number, direction: number) {
  const target = idx + direction
  if (target < 0 || target >= questions.value.length) return
  const temp = questions.value[idx]
  questions.value[idx] = questions.value[target]
  questions.value[target] = temp
  // stableKey 模式下移动题目不影响任何逻辑引用，无需重映射
  activeQuestion.value = target
}

const previewVisible = ref(false)
const previewMode = ref<"desktop" | "mobile">("desktop")
const previewTestMode = ref(false) // 是否启用测试填报模式
const previewAnswers = ref<Record<string, string>>({}) // 测试填报的答案

watch(previewVisible, (v) => {
  if (v) {
    previewPage.value = 1
    previewTestMode.value = false
    previewAnswers.value = {}
  }
})
// 切换测试/预览模式时重置填写状态
watch(previewTestMode, () => {
  previewAnswers.value = {}
  previewPage.value = 1
})

/* ====== 预览逻辑求值（使用 stableKey 查找题目答案） ====== */
function getPreviewAnswerByKey(stableKey: string): string {
  const q = questions.value.find(q => getStableKey(q) === stableKey)
  if (!q) return ""
  return previewAnswers.value[String(q.id ?? q._tempKey ?? "")] ?? ""
}

function evalPreviewCond(c: { source: string, op: string, value: string }): boolean {
  if (!c.source) return false
  const a = getPreviewAnswerByKey(String(c.source))
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

function evalPreviewGroup(g: { relation: string, conditions: { source: string, op: string, value: string }[] }): boolean {
  if (!g.conditions.length) return true
  return g.relation === "and" ? g.conditions.every(evalPreviewCond) : g.conditions.some(evalPreviewCond)
}

/** 预览中可见的题目下标（测试填报模式才应用逻辑规则） */
const previewVisibleIndices = computed(() => {
  if (!previewTestMode.value) return questions.value.map((_: QuestionItem, i: number) => i)
  const indices: number[] = []
  const visited = new Set<number>()
  let i = 0
  while (i < questions.value.length) {
    if (visited.has(i)) break
    visited.add(i)
    const q = questions.value[i]
    let rules: { display?: any, jumps?: any[] } = {}
    if (q.logicRules) {
      try {
        rules = JSON.parse(q.logicRules)
      } catch { /* */ }
    }
    // 不满足显示条件 → 跳过
    if (rules.display?.conditions?.length && !evalPreviewGroup(rules.display)) {
      i++
      continue
    }
    indices.push(i)
    // 命中跳转规则
    let jumped = false
    if (rules.jumps?.length) {
      for (const jr of rules.jumps) {
        if (jr.target && evalPreviewGroup(jr)) {
          if (jr.target === "end") return indices
          // stableKey 模式：按 stableKey 查找目标题目的下标
          const ti = questions.value.findIndex(q => getStableKey(q) === jr.target)
          if (ti > i && ti < questions.value.length) {
            i = ti
            jumped = true
            break
          }
        }
      }
    }
    if (!jumped) i++
  }
  return indices
})

/** 预览专用分页（仅可见题目，按 page_break 切页） */
const previewQuestionsByPage = computed(() => {
  const pages: Array<QuestionItem[]> = [[]]
  for (const idx of previewVisibleIndices.value) {
    const q = questions.value[idx]
    if (q.type === "page_break") {
      if (pages[pages.length - 1].length > 0) pages.push([])
    } else {
      pages[pages.length - 1].push(q)
    }
  }
  return pages.filter(p => p.length > 0)
})

const previewTotalPages = computed(() => Math.max(1, previewQuestionsByPage.value.length))

// 可见页数减少时收缩 previewPage
watch(previewTotalPages, (newTotal: number) => {
  if (previewPage.value > newTotal) previewPage.value = newTotal
})

/**
 * 内存中使用：按 stableKey 存在性清理无效引用（如删除题目后遗留的引用）
 */
function cleanLogicRules(qs: QuestionItem[]) {
  const validKeys = new Set(qs.map(getStableKey))
  for (const q of qs) {
    if (!q.logicRules) continue
    try {
      const rules = JSON.parse(q.logicRules)
      if (rules.display) {
        rules.display.conditions = rules.display.conditions.filter(
          (c: { source: string }) => validKeys.has(String(c.source))
        )
        if (rules.display.conditions.length === 0) delete rules.display
      }
      if (rules.jumps) {
        rules.jumps = rules.jumps.filter((j: { target: string, conditions: { source: string }[] }) => {
          j.conditions = j.conditions.filter((c: any) => validKeys.has(String(c.source)))
          return j.conditions.length > 0 && (j.target === "end" || validKeys.has(j.target))
        })
        if (rules.jumps.length === 0) delete rules.jumps
      }
      q.logicRules = (rules.display || rules.jumps) ? JSON.stringify(rules) : undefined
    } catch {
      q.logicRules = undefined
    }
  }
}

/**
 * 保存前用：在深拷贝上按 sortOrder 范围清理（此时 logicRules 已转回 sortOrder-based）
 */
function cleanLogicRulesForSave(qs: QuestionItem[]) {
  const total = qs.length
  for (const q of qs) {
    if (!q.logicRules) continue
    try {
      const rules = JSON.parse(q.logicRules)
      if (rules.display) {
        rules.display.conditions = rules.display.conditions.filter(
          (c: { source: number }) => c.source > 0 && c.source <= total
        )
        if (rules.display.conditions.length === 0) delete rules.display
      }
      if (rules.jumps) {
        rules.jumps = rules.jumps.filter((j: { target: string, conditions: { source: number }[] }) => {
          j.conditions = j.conditions.filter(c => c.source > 0 && c.source <= total)
          return j.conditions.length > 0 && j.target
        })
        if (rules.jumps.length === 0) delete rules.jumps
      }
      q.logicRules = (rules.display || rules.jumps) ? JSON.stringify(rules) : undefined
    } catch {
      q.logicRules = undefined
    }
  }
}

async function handleSave() {
  saving.value = true
  try {
    // 深拷贝，避免改动内存中的 stableKey-based logicRules
    const qsToSave = JSON.parse(JSON.stringify(questions.value)) as QuestionItem[]
    qsToSave.forEach((q, i) => {
      q.sortOrder = i + 1
    })
    // 将 stableKey 引用转回 sortOrder，供 DB 存储和填写页使用
    convertLogicToSortOrders(qsToSave)
    convertFormulaToSortOrders(qsToSave)
    cleanLogicRulesForSave(qsToSave)
    await saveQuestionsApi(qId.value, qsToSave)
    ElMessage.success("保存成功")
    fetchData()
  } catch {
    ElMessage.error("保存失败")
  } finally {
    saving.value = false
  }
}

fetchData()

// ====== 测试填报 ======
function handleTestSubmit() {
  const nonFillableTypes = ["description", "divider", "image_display", "page_break"]
  // 只验证逻辑上可见的题目（跳过被隐藏的题目）
  const fillableQuestions = previewVisibleIndices.value
    .map((i: number) => questions.value[i])
    .filter((q: QuestionItem) => !nonFillableTypes.includes(q.type))

  // 验证必填项
  for (const q of fillableQuestions) {
    if (q.required === 1) {
      const key = String(q.id ?? q._tempKey ?? "")
      const ans = previewAnswers.value[key]
      if (!ans || String(ans).trim() === "" || ans === "{}" || ans === "[]") {
        ElMessage.warning(`请完成必填项「${q.title}」`)
        return
      }
    }
  }

  // 统计填写情况
  const answeredCount = fillableQuestions.filter((q: QuestionItem) => {
    const key = String(q.id ?? q._tempKey ?? "")
    const ans = previewAnswers.value[key]
    return ans != null && String(ans).trim() !== "" && ans !== "{}" && ans !== "[]"
  }).length

  ElMessageBox.alert(
    `<div style="line-height: 1.8">
      <p><b>测试填报完成！</b></p>
      <p>共 ${fillableQuestions.length} 道题，已填写 ${answeredCount} 道</p>
      <p style="color: #909399; font-size: 13px; margin-top: 8px">注意：这是测试数据，不会保存到数据库</p>
    </div>`,
    "测试提交成功",
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: "关闭预览",
      callback: () => {
        previewVisible.value = false
      }
    }
  )
}

fetchData()
</script>

<template>
  <div class="app-container">
    <el-page-header @back="router.push('/questionnaire/list')">
      <template #icon>
        <el-icon><ArrowLeft /></el-icon>
      </template>
      <template #title>
        返回列表
      </template>
      <template #content>
        <span style="font-size: 16px; font-weight: 600">{{ questionnaire?.title || "问卷设计" }}</span>
      </template>
      <template #extra>
        <el-button @click="previewVisible = true">
          预览
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存题目
        </el-button>
      </template>
    </el-page-header>

    <div style="display: flex; gap: 16px; margin-top: 16px; align-items: flex-start">
      <!-- 左：题型面板（可拖拽到中间区域） -->
      <el-card shadow="never" style="width: 180px; flex-shrink: 0; max-height: calc(100vh - 160px); overflow-y: auto; position: sticky; top: 16px">
        <template #header>
          <span style="font-weight: 600">添加题目</span>
        </template>
        <div v-for="group in typeGroups" :key="group" style="margin-bottom: 12px">
          <div style="font-size: 12px; color: #909399; margin-bottom: 6px">
            {{ group }}
          </div>
          <draggable
            :list="groupedTypes[group]"
            :group="{ name: 'questions', pull: 'clone', put: false }"
            :sort="false"
            :clone="cloneTypeToQuestion"
            item-key="value"
            class="type-drag-list"
          >
            <template #item="{ element: t }">
              <div class="type-drag-item" @click="addQuestion(t.value)">
                <el-icon size="12">
                  <Plus />
                </el-icon>
                <span>{{ t.label }}</span>
              </div>
            </template>
          </draggable>
        </div>
      </el-card>

      <!-- 中：题目列表 -->
      <el-card shadow="never" style="flex: 1; min-width: 0">
        <template #header>
          <div style="display: flex; align-items: center; gap: 12px">
            <span style="font-weight: 600">题目列表（共{{ questionCount }}题）</span>
            <el-tag v-if="totalPages > 1" type="info" size="small">
              共 {{ totalPages }} 页
            </el-tag>
          </div>
        </template>

        <!-- 第一页页码标识 -->
        <div v-if="questions.length > 0 && totalPages > 1" class="page-num-badge first-page-badge">
          <span class="page-badge-dot" />
          <span>第 1 页 / 共 {{ totalPages }} 页</span>
        </div>

        <draggable
          v-model="questions"
          :item-key="questionItemKey"
          :group="{ name: 'questions' }"
          handle=".drag-handle"
          animation="200"
          ghost-class="drag-ghost"
          @add="onQuestionAdded"
          @end="onDragEnd"
        >
          <template #header>
            <el-empty v-if="questions.length === 0" description="从左侧拖入或点击添加题目" />
          </template>
          <template #item="{ element: q, index: idx }">
            <div :class="[isLayoutType(q.type) ? 'layout-item' : 'question-item', { active: activeQuestion === idx }]" @click="activeQuestion = idx">
              <!-- 布局元素：分割线 / 分页 -->
              <template v-if="isLayoutType(q.type)">
                <div class="question-header">
                  <span class="drag-handle" title="拖拽排序" style="cursor: grab; color: #c0c4cc; margin-right: 4px; font-size: 16px">⠿</span>
                  <el-tag size="small">
                    {{ typeLabel[q.type] }}
                  </el-tag>
                  <span style="flex: 1" />
                  <el-button-group size="small">
                    <el-button :icon="Rank" :disabled="idx === 0" @click.stop="moveQuestion(idx, -1)" />
                    <el-button :icon="Rank" :disabled="idx === questions.length - 1" @click.stop="moveQuestion(idx, 1)" />
                    <el-button :icon="Delete" type="danger" @click.stop="removeQuestion(idx)" />
                  </el-button-group>
                </div>
                <div class="layout-preview">
                  <el-divider v-if="q.type === 'divider'" />
                  <!-- Word 式分页分隔符 -->
                  <div v-else class="page-break-block">
                    <div class="page-break-end-row">
                      <div class="page-break-line" />
                      <span class="page-break-end-text">第 {{ getPageBreakInfo(idx).endPage }} 页 结束</span>
                      <div class="page-break-line" />
                    </div>
                    <div class="page-break-new-row">
                      <el-icon size="13">
                        <Document />
                      </el-icon>
                      <span>第 {{ getPageBreakInfo(idx).startPage }} 页 / 共 {{ totalPages }} 页</span>
                    </div>
                  </div>
                </div>
              </template>
              <!-- 普通题目 -->
              <template v-else>
                <div class="question-header">
                  <span class="drag-handle" title="拖拽排序" style="cursor: grab; color: #c0c4cc; margin-right: 4px; font-size: 16px">⠿</span>
                  <span class="question-num">Q{{ questionNumMap[idx] }}</span>
                  <el-tag size="small" type="info">
                    {{ typeLabel[q.type] || q.type }}
                  </el-tag>
                  <el-tag v-if="q.required === 1" size="small" type="danger">
                    必填
                  </el-tag>
                  <el-tag v-if="q.logicRules" size="small" type="warning">
                    逻辑
                  </el-tag>
                  <span style="flex: 1" />
                  <el-button-group size="small">
                    <el-button :icon="Rank" :disabled="idx === 0" @click.stop="moveQuestion(idx, -1)" />
                    <el-button :icon="Rank" :disabled="idx === questions.length - 1" @click.stop="moveQuestion(idx, 1)" />
                    <el-button :icon="CopyDocument" @click.stop="duplicateQuestion(idx)" />
                    <el-button :icon="Delete" type="danger" @click.stop="removeQuestion(idx)" />
                  </el-button-group>
                </div>
                <div class="question-title">
                  {{ q.title || "(未设置标题)" }}
                </div>
                <div v-if="q.description" class="question-desc">
                  {{ q.description }}
                </div>
                <div class="question-preview" @click.stop>
                  <QuestionDesignRenderer :question="q" :index="idx" :questions="questions" />
                </div>
              </template>
            </div>
          </template>
        </draggable>
      </el-card>

      <!-- 右：属性编辑 / 逻辑设计 -->
      <el-card v-if="activeQuestion !== null && questions[activeQuestion]" shadow="never" style="width: 360px; flex-shrink: 0; max-height: calc(100vh - 160px); overflow-y: auto; position: sticky; top: 16px">
        <template #header>
          <el-radio-group v-model="rightTab" size="small">
            <el-radio-button value="config">
              题目属性
            </el-radio-button>
            <el-radio-button value="logic">
              逻辑设计
            </el-radio-button>
          </el-radio-group>
        </template>
        <QuestionConfigPanel
          v-if="rightTab === 'config'"
          :question="questions[activeQuestion]"
          :questions="questions"
          :question-index="activeQuestion"
          :type-options="typeOptions"
        />
        <LogicDesignPanel
          v-else
          :question="questions[activeQuestion]"
          :questions="questions"
          :question-index="activeQuestion"
        />
      </el-card>
    </div>

    <!-- 预览弹窗（按页显示，Word 式翻页） -->
    <el-dialog v-model="previewVisible" title="问卷预览" :width="previewMode === 'mobile' ? '375px' : '700px'" :close-on-click-modal="true">
      <template #header>
        <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
          <span style="font-weight: 600">问卷预览</span>
          <el-radio-group v-model="previewMode" size="small">
            <el-radio-button value="desktop">
              桌面端
            </el-radio-button>
            <el-radio-button value="mobile">
              移动端
            </el-radio-button>
          </el-radio-group>
          <el-switch v-model="previewTestMode" active-text="测试填报" inactive-text="仅预览" size="small" />
          <span v-if="previewTotalPages > 1" style="font-size: 13px; color: #909399">
            第 {{ previewPage }} 页 / 共 {{ previewTotalPages }} 页
          </span>
        </div>
      </template>

      <!-- 测试模式提示 -->
      <el-alert v-if="previewTestMode" type="info" :closable="false" style="margin-bottom: 12px">
        <template #title>
          <span style="font-size: 13px">测试填报模式：逻辑跳转/显示条件已生效，答案不会保存到数据库</span>
        </template>
      </el-alert>

      <!-- 分页进度条 -->
      <div v-if="previewTotalPages > 1" class="preview-progress-bar">
        <div
          class="preview-progress-fill"
          :style="{ width: `${(previewPage / previewTotalPages) * 100}%` }"
        />
      </div>

      <div :style="{ maxHeight: '500px', overflowY: 'auto', padding: previewMode === 'mobile' ? '0 4px' : '0 10px', background: previewMode === 'mobile' ? '#f5f7fa' : 'transparent', borderRadius: '8px' }">
        <!-- 仅第一页显示问卷标题 -->
        <template v-if="previewPage === 1">
          <h3 style="text-align: center; margin-bottom: 16px">
            {{ questionnaire?.title }}
          </h3>
          <p v-if="questionnaire?.description" style="color: #909399; text-align: center; margin-bottom: 20px; font-size: 13px">
            {{ questionnaire.description }}
          </p>
        </template>

        <!-- 当前页题目 -->
        <template v-for="(q, qIdx) in previewQuestionsByPage[previewPage - 1]" :key="q.id || q._tempKey || qIdx">
          <div v-if="q.type === 'divider'" style="margin-bottom: 8px">
            <el-divider />
          </div>
          <div v-else-if="q.type === 'image_display'" :style="{ marginBottom: '16px', padding: previewMode === 'mobile' ? '12px 10px' : '12px', border: '1px solid #ebeef5', borderRadius: '8px', background: '#fff' }">
            <div v-if="q.title" style="font-weight: 600; margin-bottom: 8px">
              {{ q.title }}
            </div>
            <div v-if="q.description" style="color: #909399; font-size: 13px; margin-bottom: 8px">
              {{ q.description }}
            </div>
            <QuestionDesignRenderer :question="q" :index="qIdx" :questions="questions" />
          </div>
          <div v-else-if="q.type === 'description'" :style="{ marginBottom: '16px', padding: previewMode === 'mobile' ? '12px 10px' : '12px', border: '1px solid #ebeef5', borderRadius: '8px', background: '#fff' }">
            <div v-if="q.title" style="font-weight: 600; margin-bottom: 8px">
              {{ q.title }}
            </div>
            <QuestionDesignRenderer :question="q" :index="qIdx" :questions="questions" />
          </div>
          <div v-else :style="{ marginBottom: '16px', padding: previewMode === 'mobile' ? '12px 10px' : '12px', border: '1px solid #ebeef5', borderRadius: '8px', background: '#fff' }">
            <div style="font-weight: 600; margin-bottom: 8px">
              {{ q.title || '(未设置)' }}
              <el-tag v-if="q.required === 1" type="danger" size="small" style="margin-left: 4px">
                必填
              </el-tag>
              <el-tag v-if="q.logicRules && previewTestMode" type="warning" size="small" style="margin-left: 4px">
                逻辑
              </el-tag>
            </div>
            <div v-if="q.description" style="color: #909399; font-size: 13px; margin-bottom: 8px">
              {{ q.description }}
            </div>
            <QuestionDesignRenderer
              v-if="!previewTestMode"
              :question="q"
              :index="qIdx"
              :questions="questions"
            />
            <QuestionFillRenderer
              v-else
              :question="q"
              :model-value="previewAnswers[String(q.id ?? q._tempKey ?? '')] ?? ''"
              :all-questions="questions"
              :answers-map="previewAnswers"
              :resolve-answer="(qq) => previewAnswers[String(qq.id ?? qq._tempKey ?? '')] ?? ''"
              formula-key-type="stableKey"
              @update:model-value="(v) => { previewAnswers[String(q.id ?? q._tempKey ?? '')] = v }"
            />
          </div>
        </template>

        <el-empty v-if="!previewQuestionsByPage[previewPage - 1]?.filter(q => q.type !== 'divider').length" description="本页暂无题目" />
      </div>

      <template #footer>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <!-- 页码信息 -->
          <span v-if="previewTotalPages > 1" style="font-size: 13px; color: #606266">
            第 <b>{{ previewPage }}</b> 页，共 <b>{{ previewTotalPages }}</b> 页
          </span>
          <span v-else />
          <!-- 翻页按钮 -->
          <div style="display: flex; gap: 8px">
            <el-button :disabled="previewPage <= 1" @click="previewPage--">
              上一页
            </el-button>
            <el-button v-if="previewPage < previewTotalPages" type="primary" @click="previewPage++">
              下一页
            </el-button>
            <el-button v-else-if="previewTestMode" type="success" @click="handleTestSubmit">
              测试提交
            </el-button>
            <el-button v-else type="primary" @click="previewVisible = false">
              完成
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.question-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.question-item:hover {
  border-color: var(--el-color-primary-light-5);
}
.question-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.question-num {
  font-weight: 600;
  color: var(--el-color-primary);
}
.question-title {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}
.question-desc {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
  margin-bottom: 2px;
}
.question-preview {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
  pointer-events: none;
}
.layout-item {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  padding: 8px 16px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}
.layout-item:hover {
  border-color: var(--el-color-info-light-3);
}
.layout-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.layout-preview {
  pointer-events: none;
}
.drag-ghost {
  opacity: 0.4;
  background: var(--el-color-primary-light-9);
  border: 1px dashed var(--el-color-primary);
}
.drag-handle:active {
  cursor: grabbing;
}
.type-drag-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.type-drag-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: grab;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  border: 1px solid transparent;
  transition: all 0.15s;
  user-select: none;
}
.type-drag-item:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}
.type-drag-item:active {
  cursor: grabbing;
}

/* ===== 第一页页码标识 ===== */
.page-num-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 4px;
  padding: 4px 10px;
  margin-bottom: 6px;
}
.page-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  flex-shrink: 0;
}

/* ===== Word 式分页分隔符 ===== */
.page-break-block {
  padding: 4px 0 2px;
}
.page-break-end-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.page-break-line {
  flex: 1;
  height: 1px;
  background: #dcdfe6;
}
.page-break-end-text {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  flex-shrink: 0;
}
.page-break-new-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 13px;
  color: var(--el-color-primary);
  font-weight: 500;
  background: var(--el-color-primary-light-9);
  border: 1px dashed var(--el-color-primary-light-3);
  border-radius: 4px;
  padding: 5px 0;
}

/* ===== 预览进度条 ===== */
.preview-progress-bar {
  height: 3px;
  background: var(--el-border-color-lighter);
  border-radius: 2px;
  margin-bottom: 14px;
  overflow: hidden;
}
.preview-progress-fill {
  height: 100%;
  background: var(--el-color-primary);
  border-radius: 2px;
  transition: width 0.3s ease;
}
</style>
