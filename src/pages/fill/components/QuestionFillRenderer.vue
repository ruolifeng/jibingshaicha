<script lang="ts" setup>
import type { QuestionItem } from "@/pages/questionnaire/apis/type"
import { getContentLimitPrecision, validateContentLimitNumber, validateIdCard } from "@@/utils/validate"
import { Plus } from "@element-plus/icons-vue"
import draggable from "vuedraggable"
import { request } from "@/http/axios"
import { answerToNumber, evalFormula, extractFormulaRefs, extractTitleCode } from "@/pages/questionnaire/design/utils/formula"
import SignaturePad from "./SignaturePad.vue"

interface OptionObj { label: string, value: string, imageUrl?: string, contentLimit?: string, rangeMin?: number, rangeMax?: number, decimalPlaces?: number, inputHidden?: boolean, hasInput?: boolean, dropdownOptions?: string[] }

/**
 * 自动计算题（formula）的可选 props：
 *  - allQuestions：完整题目列表，用于按变量 key 查找其它题目
 *  - resolveAnswer：回调，根据题目实例返回当前答案字符串（父级负责答案存储）
 *  - formulaKeyType：变量 key 的语义；默认 "sortOrder"（填写端使用），
 *    设计端测试预览时传入 "stableKey"
 */
const props = defineProps<{
  question: QuestionItem
  modelValue: string
  allQuestions?: QuestionItem[]
  /** 父级答案表（questionId → 答案），公式题依赖此 prop 建立响应式 */
  answersMap?: Record<string, string>
  /** 父级答案变更计数，确保公式题随其它题目填写实时重算 */
  answersTick?: number
  resolveAnswer?: (q: QuestionItem) => string
  formulaKeyType?: "sortOrder" | "stableKey"
  /** 父级每次校验失败时自增，触发各字段强制显示内联错误（每次都能触发 watch） */
  submitAttemptCount?: number
}>()

const emit = defineEmits<{ "update:modelValue": [value: string] }>()

/* ---------- 解析工具 ---------- */
function parseOptions(): OptionObj[] {
  if (!props.question.options) return []
  try {
    const p = JSON.parse(props.question.options)
    return Array.isArray(p) ? p : []
  } catch {
    return []
  }
}
function parseCascaderOptions(): any[] {
  if (!props.question.options) return []
  try {
    return JSON.parse(props.question.options)
  } catch {
    return []
  }
}
function parseMatrixOpts(): { rows: OptionObj[], cols: OptionObj[] } {
  if (!props.question.options) return { rows: [], cols: [] }
  try {
    return JSON.parse(props.question.options)
  } catch {
    return { rows: [], cols: [] }
  }
}
function parseScaleOpts(): { rows: OptionObj[], scaleMin: number, scaleMax: number, scaleLabels: Record<string, string> } {
  if (!props.question.options) return { rows: [], scaleMin: 1, scaleMax: 5, scaleLabels: {} }
  try {
    const p = JSON.parse(props.question.options)
    return { rows: p.rows || [], scaleMin: p.scaleMin ?? 1, scaleMax: p.scaleMax ?? 5, scaleLabels: p.scaleLabels || {} }
  } catch {
    return { rows: [], scaleMin: 1, scaleMax: 5, scaleLabels: {} }
  }
}
function parseValidation(): Record<string, any> {
  if (!props.question.validationRules) return {}
  try {
    return JSON.parse(props.question.validationRules)
  } catch {
    return {}
  }
}
function parseImageDisplayOpts(): { imageUrl: string, alt: string } {
  if (!props.question.options) return { imageUrl: "", alt: "" }
  try {
    return JSON.parse(props.question.options)
  } catch {
    return { imageUrl: "", alt: "" }
  }
}

/* ---------- 简单值绑定 ---------- */
const stringValue = computed({
  get: () => props.modelValue ?? "",
  set: v => emit("update:modelValue", String(v ?? ""))
})
const numberValue = computed({
  get: () => {
    if (!props.modelValue && props.modelValue !== "0") return undefined
    const n = Number(props.modelValue)
    return Number.isNaN(n) ? undefined : n
  },
  set: (v: number | undefined) => emit("update:modelValue", v != null ? String(v) : "")
})

/* ---------- radio / checkbox 附加填空支持 ---------- */

/** 该题是否有任意选项配置了附加填空 */
const hasOptionInput = computed(() => parseOptions().some(o => o.hasInput === true))

/**
 * 解析带附加填空的复合值格式：{ selected: string | string[], inputs: Record<string, string> }
 * 兼容旧格式（纯字符串 / 数组 JSON）
 */
function parseOptInputVal(): { selected: string | string[], inputs: Record<string, string> } {
  if (!props.modelValue) return { selected: "", inputs: {} }
  try {
    const p = JSON.parse(props.modelValue)
    if (p && typeof p === "object" && !Array.isArray(p) && "selected" in p) {
      return { selected: p.selected ?? "", inputs: p.inputs ?? {} }
    }
    // 旧格式：数组（checkbox）或字符串（radio）
    return { selected: p, inputs: {} }
  } catch {
    return { selected: props.modelValue, inputs: {} }
  }
}

/** 单选值（无附加填空时退化为简单字符串） */
const radioValue = computed({
  get: () => {
    if (!hasOptionInput.value) return props.modelValue ?? ""
    return parseOptInputVal().selected as string
  },
  set: (v: string) => {
    if (!hasOptionInput.value) {
      emit("update:modelValue", v)
      return
    }
    const { inputs } = parseOptInputVal()
    emit("update:modelValue", JSON.stringify({ selected: v, inputs }))
  }
})

/** 多选值（无附加填空时退化为简单数组） */
const cbValue = computed({
  get: () => {
    if (!hasOptionInput.value) {
      if (!props.modelValue) return []
      try {
        return JSON.parse(props.modelValue) as string[]
      } catch {
        return []
      }
    }
    const sel = parseOptInputVal().selected
    return Array.isArray(sel) ? sel : []
  },
  set: (v: string[]) => {
    if (!hasOptionInput.value) {
      emit("update:modelValue", JSON.stringify(v))
      return
    }
    const { inputs } = parseOptInputVal()
    emit("update:modelValue", JSON.stringify({ selected: v, inputs }))
  }
})

/** 多选题：最少/最多可选数（来自 validationRules） */
const cbMinSelect = computed<number | undefined>(() => {
  const v = parseValidation().minSelect
  return typeof v === "number" && v > 0 ? v : undefined
})
const cbMaxSelect = computed<number | undefined>(() => {
  const v = parseValidation().maxSelect
  return typeof v === "number" && v > 0 ? v : undefined
})

function toggleCb(value: string) {
  const current = cbValue.value.slice()
  const idx = current.indexOf(value)
  if (idx >= 0) {
    current.splice(idx, 1)
  } else {
    if (cbMaxSelect.value !== undefined && current.length >= cbMaxSelect.value) {
      ElMessage.warning(`最多只能选 ${cbMaxSelect.value} 项`)
      return
    }
    current.push(value)
  }
  cbValue.value = current
}

/** 读取某选项的附加填空文字 */
function getOptionInputText(optValue: string): string {
  return (parseOptInputVal().inputs as Record<string, string>)[optValue] ?? ""
}

/** 更新某选项的附加填空文字 */
function setOptionInputText(optValue: string, text: string) {
  const { selected, inputs } = parseOptInputVal()
  ;(inputs as Record<string, string>)[optValue] = text
  emit("update:modelValue", JSON.stringify({ selected, inputs }))
}

/* ---------- cascader ---------- */
const cascaderValue = computed({
  get: () => {
    if (!props.modelValue) return []
    try {
      return JSON.parse(props.modelValue)
    } catch {
      return []
    }
  },
  set: (v: any) => emit("update:modelValue", JSON.stringify(v))
})

/* ---------- date range ---------- */
const dateRangeValue = computed({
  get: () => {
    if (!props.modelValue) return undefined
    try {
      const a = JSON.parse(props.modelValue)
      return Array.isArray(a) ? a : undefined
    } catch {
      return undefined
    }
  },
  set: (v: unknown) => emit("update:modelValue", v ? JSON.stringify(v) : "")
})

/* ---------- sub-answer (multi_input / inline_input / matrix) ---------- */
function getSubAnswer(key: string): string {
  if (!props.modelValue) return ""
  try {
    const obj = JSON.parse(props.modelValue)
    return obj[key] || ""
  } catch {
    return ""
  }
}
function setSubAnswer(key: string, val: string) {
  let obj: Record<string, string> = {}
  try {
    if (props.modelValue) obj = JSON.parse(props.modelValue)
  } catch { /* empty */ }
  obj[key] = val
  emit("update:modelValue", JSON.stringify(obj))
}
function getMatrixCell(row: string, col: string): string {
  return getSubAnswer(`${row}__${col}`)
}
function setMatrixCell(row: string, col: string, val: string) {
  setSubAnswer(`${row}__${col}`, val)
}

/* ---------- matrix_checkbox ---------- */
/** 读取某行已选中的列 value 数组 */
function getMcbRow(rowVal: string): string[] {
  if (!props.modelValue) return []
  try {
    const obj = JSON.parse(props.modelValue)
    const v = obj[rowVal]
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}
/** 切换某行某列的勾选状态 */
function toggleMcbCell(rowVal: string, colVal: string) {
  let obj: Record<string, string[]> = {}
  try {
    if (props.modelValue) obj = JSON.parse(props.modelValue)
  } catch { /* empty */ }
  const current = Array.isArray(obj[rowVal]) ? [...obj[rowVal]] : []
  const idx = current.indexOf(colVal)
  if (idx >= 0) current.splice(idx, 1)
  else current.push(colVal)
  obj[rowVal] = current
  emit("update:modelValue", JSON.stringify(obj))
}

/* ============================================================
 * 复合矩阵 matrix_complex
 *   答案存储格式：{ rowValue: { columnKey: value } }
 *   - radio:    columnKey -> "选项 value"
 *   - checkbox: columnKey -> ["v1","v2"]
 *   - input:    columnKey -> "文本"
 *   - freq:     columnKey -> { unit: "day", value: "3" }
 * ========================================================== */
interface MCColShowWhen { column: string, op: string, value?: string }
interface MCCol {
  key: string
  label: string
  type: "radio" | "checkbox" | "input" | "freq"
  options?: { label: string, value: string }[]
  units?: { label: string, value: string }[]
  inputType?: string
  suffix?: string
  showWhen?: MCColShowWhen
}
interface MCOpts { rows: { label: string, value: string }[], columns: MCCol[] }

function parseMCOpts(): MCOpts {
  if (!props.question.options) return { rows: [], columns: [] }
  try {
    const p = JSON.parse(props.question.options)
    return { rows: Array.isArray(p.rows) ? p.rows : [], columns: Array.isArray(p.columns) ? p.columns : [] }
  } catch {
    return { rows: [], columns: [] }
  }
}

/** 整体答案对象：{ [rowValue]: { [colKey]: any } } */
function parseMCAnswers(): Record<string, Record<string, any>> {
  if (!props.modelValue) return {}
  try {
    const p = JSON.parse(props.modelValue)
    return p && typeof p === "object" && !Array.isArray(p) ? p : {}
  } catch {
    return {}
  }
}

function setMCCell(rowValue: string, colKey: string, value: any) {
  const all = parseMCAnswers()
  if (!all[rowValue]) all[rowValue] = {}
  if (value === undefined || value === null || value === "" || (Array.isArray(value) && value.length === 0)) {
    delete all[rowValue][colKey]
    if (Object.keys(all[rowValue]).length === 0) delete all[rowValue]
  } else {
    all[rowValue][colKey] = value
  }
  emit("update:modelValue", Object.keys(all).length ? JSON.stringify(all) : "")
}

function getMCCell(rowValue: string, colKey: string): any {
  return parseMCAnswers()[rowValue]?.[colKey]
}

/** 评估某列的 showWhen 是否满足（使用同一行内其它列的值） */
function evalMCShow(rowValue: string, col: MCCol): boolean {
  if (!col.showWhen) return true
  const sw = col.showWhen
  const dep = getMCCell(rowValue, sw.column)
  switch (sw.op) {
    case "eq": {
      if (Array.isArray(dep)) return dep.length === 1 && dep[0] === sw.value
      if (dep && typeof dep === "object" && "unit" in dep) return dep.unit === sw.value
      return String(dep ?? "") === String(sw.value ?? "")
    }
    case "neq": {
      if (Array.isArray(dep)) return !dep.includes(String(sw.value ?? ""))
      if (dep && typeof dep === "object" && "unit" in dep) return dep.unit !== sw.value
      return String(dep ?? "") !== String(sw.value ?? "")
    }
    case "selected": {
      if (Array.isArray(dep)) return dep.includes(String(sw.value ?? ""))
      return String(dep ?? "") === String(sw.value ?? "")
    }
    case "not_empty": {
      if (Array.isArray(dep)) return dep.length > 0
      if (dep && typeof dep === "object") return Object.keys(dep).length > 0
      return dep != null && String(dep) !== ""
    }
    default: return true
  }
}

/** checkbox 列的当前选中数组 */
function mcCbValue(rowValue: string, colKey: string): string[] {
  const v = getMCCell(rowValue, colKey)
  return Array.isArray(v) ? v : []
}
function mcToggleCb(rowValue: string, colKey: string, optValue: string) {
  const cur = mcCbValue(rowValue, colKey).slice()
  const idx = cur.indexOf(optValue)
  if (idx >= 0) cur.splice(idx, 1)
  else cur.push(optValue)
  setMCCell(rowValue, colKey, cur)
}

/** freq 列：{ unit, value } 互斥单选+数字 */
function mcFreqVal(rowValue: string, colKey: string): { unit: string, value: string } {
  const v = getMCCell(rowValue, colKey)
  if (v && typeof v === "object" && "unit" in v) return { unit: String(v.unit ?? ""), value: String(v.value ?? "") }
  return { unit: "", value: "" }
}
function mcSetFreqUnit(rowValue: string, colKey: string, unit: string) {
  const cur = mcFreqVal(rowValue, colKey)
  if (cur.unit === unit) {
    setMCCell(rowValue, colKey, "")
  } else {
    setMCCell(rowValue, colKey, { unit, value: "" })
  }
}
function mcSetFreqValue(rowValue: string, colKey: string, value: string) {
  const cur = mcFreqVal(rowValue, colKey)
  if (!cur.unit) return
  setMCCell(rowValue, colKey, { unit: cur.unit, value })
}

/** input 列输入处理（按 inputType 校验） */
function mcInputType(col: MCCol): string {
  return col.inputType || "text"
}
function mcInputPlaceholder(col: MCCol): string {
  const m: Record<string, string> = { number: "数字", integer: "整数", decimal: "小数", date: "YYYY-MM-DD", text: "请输入" }
  return m[mcInputType(col)] || "请输入"
}

/* ---------- dynamic table ---------- */
const tableRows = ref<Record<string, string>[]>([{}])
watch(() => props.modelValue, (v) => {
  if (props.question.type !== "dynamic_table") return
  if (!v) {
    tableRows.value = [{}]
    return
  }
  try {
    tableRows.value = JSON.parse(v)
  } catch {
    tableRows.value = [{}]
  }
}, { immediate: true })
function syncTable() {
  emit("update:modelValue", JSON.stringify(tableRows.value))
}
function addTableRow() {
  const cols = parseOptions()
  const row: Record<string, string> = {}
  cols.forEach(c => (row[c.value] = ""))
  tableRows.value.push(row)
  syncTable()
}
function removeTableRow(idx: number) {
  tableRows.value.splice(idx, 1)
  if (tableRows.value.length === 0) tableRows.value.push({})
  syncTable()
}

/* ---------- sort ---------- */
const sortItems = ref<OptionObj[]>([])
watch([() => props.modelValue, () => props.question.options], () => {
  if (props.question.type !== "sort") return
  const opts = parseOptions()
  if (props.modelValue) {
    try {
      const order = JSON.parse(props.modelValue) as string[]
      sortItems.value = order.map(v => opts.find(o => o.value === v) || { label: v, value: v })
    } catch {
      sortItems.value = [...opts]
    }
  } else {
    sortItems.value = [...opts]
    if (opts.length > 0) {
      emit("update:modelValue", JSON.stringify(opts.map(o => o.value)))
    }
  }
}, { immediate: true })
function onSortEnd() {
  emit("update:modelValue", JSON.stringify(sortItems.value.map(o => o.value)))
}

/* ---------- image choice ---------- */
const isMultiple = computed(() => parseValidation().multiple === true)
function toggleImageChoice(value: string) {
  if (isMultiple.value) {
    let sel: string[] = []
    try {
      sel = JSON.parse(props.modelValue || "[]")
    } catch { /* empty */ }
    const idx = sel.indexOf(value)
    if (idx >= 0) sel.splice(idx, 1)
    else sel.push(value)
    emit("update:modelValue", JSON.stringify(sel))
  } else {
    emit("update:modelValue", value)
  }
}
function isImageSelected(value: string): boolean {
  if (isMultiple.value) {
    try {
      return (JSON.parse(props.modelValue || "[]") as string[]).includes(value)
    } catch {
      return false
    }
  }
  return props.modelValue === value
}

/* ---------- NPS / scale helpers ---------- */
const npsRange = computed(() => {
  const v = parseValidation()
  return Array.from({ length: (v.max ?? 10) - (v.min ?? 0) + 1 }, (_, i) => i + (v.min ?? 0))
})
const scaleRange = computed(() => {
  const s = parseScaleOpts()
  return Array.from({ length: s.scaleMax - s.scaleMin + 1 }, (_, i) => i + s.scaleMin)
})

/* ---------- validation shortcuts ---------- */
const dateType = computed(() => parseValidation().dateType || "date")
const rateMax = computed(() => parseValidation().max ?? 5)
const sliderMin = computed(() => parseValidation().min ?? 0)
const sliderMax = computed(() => parseValidation().max ?? 100)
const sliderStep = computed(() => parseValidation().step ?? 1)
const sliderShowInput = computed(() => parseValidation().showInput === true)
const numMin = computed(() => parseValidation().min)
const numMax = computed(() => parseValidation().max)
const numStep = computed(() => parseValidation().step ?? 1)
const numPrecision = computed(() => parseValidation().precision ?? 0)
const uploadAccept = computed(() => parseValidation().accept || "")
const uploadMaxSize = computed(() => parseValidation().maxSize ?? 10)
const uploadMaxCount = computed(() => parseValidation().maxCount ?? 1)

/* ---------- 内容限制（input 类型专用） ---------- */
const contentLimit = computed(() => parseValidation().contentLimit || "none")
const rangeMin = computed<number | undefined>(() => parseValidation().rangeMin)
const rangeMax = computed<number | undefined>(() => parseValidation().rangeMax)
const contentLimitDecimalPlaces = computed<number | undefined>(() => parseValidation().decimalPlaces)

/** 数字类型（input 内容限制）对应 el-input-number 的精度 */
const contentLimitPrecision = computed(() =>
  getContentLimitPrecision(contentLimit.value, contentLimitDecimalPlaces.value)
)

/** 中国省份列表 */
const PROVINCES = [
  "北京",
  "天津",
  "河北",
  "山西",
  "内蒙古",
  "辽宁",
  "吉林",
  "黑龙江",
  "上海",
  "江苏",
  "浙江",
  "安徽",
  "福建",
  "江西",
  "山东",
  "河南",
  "湖北",
  "湖南",
  "广东",
  "广西",
  "海南",
  "重庆",
  "四川",
  "贵州",
  "云南",
  "西藏",
  "陕西",
  "甘肃",
  "青海",
  "宁夏",
  "新疆",
  "台湾",
  "香港",
  "澳门"
]

/** input 占位文字 */
const inputPlaceholder = computed(() => {
  const map: Record<string, string> = {
    phone: "请输入11位手机号",
    id_card: "请输入18位身份证号",
    email: "请输入邮箱地址",
    number: "请输入数字",
    integer: "请输入整数",
    decimal: "请输入小数",
    date_format: "请输入日期（YYYY-MM-DD）"
  }
  return map[contentLimit.value] || "请输入"
})

/** 内容限制实时校验错误 */
const inputError = ref("")

function checkContentLimit(val: string): string {
  if (!val) return ""
  const limit = contentLimit.value
  if (limit === "phone") return /^1[3-9]\d{9}$/.test(val) ? "" : "请输入有效的11位手机号"
  if (limit === "id_card") return validateIdCard(val) ? "" : "身份证号码无效，请检查位数、出生日期及校验码"
  if (limit === "email") return /^[^\s@]+@[^\s@][^\s.@]*\.[^\s@]+$/.test(val) ? "" : "请输入有效的邮箱地址"
  if (limit === "date_format") return /^\d{4}-\d{2}-\d{2}$/.test(val) ? "" : "日期格式应为 YYYY-MM-DD"
  return ""
}

function onInputBlur() {
  inputError.value = checkContentLimit(stringValue.value)
}

function onInputChange() {
  if (inputError.value) inputError.value = checkContentLimit(stringValue.value)
}

/** 提交/翻页校验失败时，父级自增 submitAttemptCount，触发所有字段强制显示内联错误 */
watch(() => props.submitAttemptCount, (v) => {
  if (!v) return
  if (props.question.type === "input") {
    inputError.value = checkContentLimit(stringValue.value)
  }
  if (props.question.type === "matrix_input") {
    const { rows, cols } = parseMatrixOpts()
    for (const col of cols) {
      if (!col.contentLimit) continue
      for (const row of rows) {
        const key = `${row.value}__${col.value}`
        matrixCellErrors.value[key] = checkCellContentLimit(col.contentLimit, getMatrixCell(row.value, col.value), col)
      }
    }
  }
  if (props.question.type === "dynamic_table") {
    const cols = parseOptions()
    tableRows.value.forEach((rowData, ri) => {
      for (const col of cols) {
        if (!col.contentLimit) continue
        const key = `${ri}__${col.value}`
        tableCellErrors.value[key] = checkCellContentLimit(col.contentLimit, rowData[col.value] || "", col)
      }
    })
  }
})

/** 返回列的占位文字（供 dynamic_table / matrix_input 共用） */
function getColPlaceholder(limit?: string): string {
  const map: Record<string, string> = {
    phone: "请输入11位手机号",
    id_card: "请输入18位身份证号",
    email: "请输入邮箱地址",
    text: "请输入",
    date_format: "YYYY-MM-DD"
  }
  return (limit && map[limit]) ? map[limit] : "请输入"
}

/** 单元格内容限制校验（供 matrix_input / dynamic_table 共用） */
function checkCellContentLimit(
  limit: string | undefined,
  val: string,
  opts?: { decimalPlaces?: number, rangeMin?: number, rangeMax?: number }
): string {
  if (!limit || !val) return ""
  if (limit === "phone") return /^1[3-9]\d{9}$/.test(val) ? "" : "请输入有效的11位手机号"
  if (limit === "id_card") return validateIdCard(val) ? "" : "身份证号码无效，请检查位数、出生日期及校验码"
  if (limit === "email") return /^[^\s@]+@[^\s@][^\s.@]*\.[^\s@]+$/.test(val) ? "" : "请输入有效的邮箱地址"
  if (limit === "date_format") return /^\d{4}-\d{2}-\d{2}$/.test(val) ? "" : "日期格式应为 YYYY-MM-DD"
  return validateContentLimitNumber(limit, val, opts) || ""
}

/** matrix_input 单元格校验错误，key 格式：rowValue__colValue */
const matrixCellErrors = ref<Record<string, string>>({})

function onMatrixCellBlur(rowValue: string, colValue: string, col: OptionObj) {
  const key = `${rowValue}__${colValue}`
  matrixCellErrors.value[key] = checkCellContentLimit(col.contentLimit, getMatrixCell(rowValue, colValue), col)
}
function onMatrixCellInput(rowValue: string, colValue: string, col: OptionObj) {
  const key = `${rowValue}__${colValue}`
  if (matrixCellErrors.value[key]) {
    matrixCellErrors.value[key] = checkCellContentLimit(col.contentLimit, getMatrixCell(rowValue, colValue), col)
  }
}

/** dynamic_table 单元格校验错误，key 格式：rowIndex__colValue */
const tableCellErrors = ref<Record<string, string>>({})

function onTableCellBlur(ri: number, colValue: string, col: OptionObj, val: string) {
  const key = `${ri}__${colValue}`
  tableCellErrors.value[key] = checkCellContentLimit(col.contentLimit, val, col)
}
function onTableCellInput(ri: number, colValue: string, col: OptionObj, val: string) {
  const key = `${ri}__${colValue}`
  if (tableCellErrors.value[key]) tableCellErrors.value[key] = checkCellContentLimit(col.contentLimit, val, col)
}

/* ---------- file / image upload ---------- */
const uploadFileList = ref<{ name: string, url: string }[]>([])
watch(() => props.modelValue, (v) => {
  if (props.question.type !== "file_upload" && props.question.type !== "image_upload") return
  if (!v) {
    uploadFileList.value = []
    return
  }
  try {
    const parsed = JSON.parse(v)
    if (Array.isArray(parsed)) {
      uploadFileList.value = parsed.map((url: string, i: number) => ({ name: `文件${i + 1}`, url }))
      return
    }
  } catch { /* not json */ }
  uploadFileList.value = v ? [{ name: v.split("/").pop() || "文件", url: v }] : []
}, { immediate: true })

function addFileUrl(url: string) {
  if (uploadMaxCount.value <= 1) {
    emit("update:modelValue", url)
    return
  }
  let urls: string[] = []
  try {
    urls = JSON.parse(props.modelValue || "[]")
  } catch { /* empty */ }
  urls.push(url)
  emit("update:modelValue", JSON.stringify(urls))
}
function beforeUpload(file: File): boolean {
  if (file.size > uploadMaxSize.value * 1024 * 1024) {
    ElMessage.error(`文件大小超过限制（${uploadMaxSize.value}MB）`)
    return false
  }
  return true
}
async function handleUpload(options: { file: File, onSuccess: (r: any) => void, onError: (e: any) => void }) {
  const fd = new FormData()
  fd.append("file", options.file)
  try {
    const { data } = await request<ApiResponseData<string>>({ url: "public/fill/upload", method: "post", data: fd, headers: { "Content-Type": "multipart/form-data" }, timeout: 60000 })
    options.onSuccess(data)
    addFileUrl(data)
  } catch (err: any) {
    options.onError(err)
    ElMessage.error("上传失败")
  }
}
function handleRemove(_file: any, fileList: any[]) {
  const urls = fileList.map((f: any) => f.url || f.response).filter(Boolean)
  if (urls.length === 0) emit("update:modelValue", "")
  else if (uploadMaxCount.value <= 1) emit("update:modelValue", urls[0])
  else emit("update:modelValue", JSON.stringify(urls))
}

/* ============================================================
 * 自动计算（formula）题型
 * ============================================================ */

const FORMULA_LAYOUT_TYPES = new Set(["divider", "page_break"])

function getQuestionAnswerKey(q: QuestionItem): string {
  return String(q.id ?? q._tempKey ?? "")
}

/** 读取某题当前答案（优先 answersMap，保证公式 computed 能追踪父级变更） */
function getAnswerForQuestion(q: QuestionItem): string {
  const key = getQuestionAnswerKey(q)
  if (props.answersMap) return props.answersMap[key] ?? ""
  return props.resolveAnswer?.(q) ?? ""
}

/** 根据 formulaKeyType 取出对应题目的 stableKey */
function getQuestionFormulaKey(q: QuestionItem, idx: number): string {
  if (props.formulaKeyType === "stableKey") {
    return q.id != null ? String(q.id) : `t${q._tempKey}`
  }
  // 默认按 sortOrder（1-based）；优先用题目自身的 sortOrder，回退到数组下标
  return String(q.sortOrder || idx + 1)
}

/** 兼容旧数据：无论 formulaKeyType 传什么，都补充 stableKey / sortOrder / 可见题号 / 标题编码 映射 */
function getQuestionCompatFormulaKeys(q: QuestionItem, idx: number, allQuestions: QuestionItem[]): string[] {
  const keys = new Set<string>()
  keys.add(getQuestionFormulaKey(q, idx))
  if (q.id != null) keys.add(String(q.id))
  if (q._tempKey != null) keys.add(`t${q._tempKey}`)
  keys.add(String(q.sortOrder || idx + 1))
  // 数组下标（与保存时 convertFormulaToSortOrders 的 i+1 一致）
  keys.add(String(idx + 1))
  // 可见题号（跳过 divider / page_break，与填写页题号一致）
  let fillableNum = 0
  for (let i = 0; i <= idx; i++) {
    const qq = allQuestions[i]
    if (!qq || FORMULA_LAYOUT_TYPES.has(qq.type)) continue
    fillableNum++
  }
  if (fillableNum > 0) {
    keys.add(`Q${fillableNum}`)
  }
  const titleCode = extractTitleCode(q.title)
  if (titleCode) {
    keys.add(titleCode)
    keys.add(titleCode.toUpperCase())
  }
  return Array.from(keys)
}

/** 公式中变量 key → 题目实例 */
const formulaKeyToQuestion = computed<Record<string, QuestionItem>>(() => {
  const map: Record<string, QuestionItem> = {}
  if (props.question.type !== "formula" || !props.allQuestions) return map
  props.allQuestions.forEach((q, idx) => {
    for (const key of getQuestionCompatFormulaKeys(q, idx, props.allQuestions!)) {
      map[key] = q
    }
  })
  return map
})

/** 公式题的配置 */
const formulaConfig = computed(() => {
  const v = parseValidation()
  return {
    formula: typeof v.formula === "string" ? v.formula : "",
    precision: typeof v.precision === "number" ? v.precision : 2,
    unit: typeof v.unit === "string" ? v.unit : "",
    warningMin: typeof v.warningMin === "number" ? v.warningMin : undefined,
    warningMax: typeof v.warningMax === "number" ? v.warningMax : undefined,
    warningMinText: typeof v.warningMinText === "string" ? v.warningMinText : "",
    warningMaxText: typeof v.warningMaxText === "string" ? v.warningMaxText : ""
  }
})

/** 公式当前依赖到的题目（用于响应式重算） */
const formulaDeps = computed(() => {
  if (props.question.type !== "formula") return [] as string[]
  return extractFormulaRefs(formulaConfig.value.formula)
})

/** 解析单个变量 key 为数值 */
function resolveFormulaVar(key: string): number | null {
  const k = key.trim()
  const q = formulaKeyToQuestion.value[k] ?? formulaKeyToQuestion.value[k.toUpperCase()]
  if (!q) return null
  return answerToNumber(getAnswerForQuestion(q))
}

/** 依赖题答案快照，驱动公式随填写实时重算 */
const formulaDepSnapshot = computed(() => {
  if (props.question.type !== "formula") return ""
  // 显式依赖父级答案变更计数（比动态 key 追踪更可靠，尤其移动端）
  void props.answersTick
  return formulaDeps.value.map((k) => {
    const refKey = k.trim()
    const q = formulaKeyToQuestion.value[refKey]
      ?? formulaKeyToQuestion.value[refKey.toUpperCase()]
    return q ? getAnswerForQuestion(q) : ""
  }).join("\0")
})

/** 当前公式计算结果 */
const formulaResult = computed(() => {
  if (props.question.type !== "formula") return { value: null, pending: false, error: null, display: "" }
  void props.answersTick
  void formulaDepSnapshot.value
  const r = evalFormula(formulaConfig.value.formula, resolveFormulaVar)
  let display = ""
  if (r.value != null) {
    const p = Math.max(0, Math.min(6, formulaConfig.value.precision))
    display = r.value.toFixed(p)
  }
  return { ...r, display }
})

/** 范围警告（仅在能计算出结果时生效） */
const formulaWarning = computed<string>(() => {
  const r = formulaResult.value
  if (r.value == null) return ""
  const cfg = formulaConfig.value
  if (cfg.warningMin != null && r.value < cfg.warningMin) {
    return cfg.warningMinText || `结果偏低（< ${cfg.warningMin}），请核实`
  }
  if (cfg.warningMax != null && r.value > cfg.warningMax) {
    return cfg.warningMaxText || `结果偏高（> ${cfg.warningMax}），请核实`
  }
  return ""
})

/** 同步计算结果到 modelValue（避免回写循环） */
watch(
  () => formulaResult.value.display,
  (next) => {
    if (props.question.type !== "formula") return
    const cur = props.modelValue ?? ""
    if (next !== cur) emit("update:modelValue", next)
  },
  { immediate: true }
)
</script>

<template>
  <!-- 单选：自定义卡片实现，避免移动端 el-radio-group inject 上下文丢失导致可多选 -->
  <div v-if="question.type === 'radio'" class="fill-options">
    <div
      v-for="opt in parseOptions()"
      :key="opt.value"
      class="fill-option-wrap" :class="[{ 'is-selected': radioValue === opt.value }]"
      @click="radioValue = opt.value"
    >
      <span class="fill-radio-dot" :class="{ active: radioValue === opt.value }" />
      <span class="fill-option-label">{{ opt.label }}</span>
    </div>
    <!-- 选中的选项若配置了附加填空，在选项列表末尾显示输入框 -->
    <template v-if="hasOptionInput">
      <template v-for="opt in parseOptions()" :key="`ri-${opt.value}`">
        <el-input
          v-if="opt.hasInput && radioValue === opt.value"
          :model-value="getOptionInputText(opt.value)"
          placeholder="请注明"
          class="fill-option-input"
          @update:model-value="(v) => setOptionInputText(opt.value, String(v))"
        />
      </template>
    </template>
  </div>

  <!-- 多选：自定义卡片实现，点击整个卡片区域均可触发勾选/取消 -->
  <div v-else-if="question.type === 'checkbox'" class="fill-options">
    <div
      v-for="opt in parseOptions()"
      :key="opt.value"
      class="fill-option-wrap" :class="[{ 'is-selected': cbValue.includes(opt.value), 'is-disabled': !cbValue.includes(opt.value) && cbMaxSelect !== undefined && cbValue.length >= cbMaxSelect }]"
      @click="toggleCb(opt.value)"
    >
      <span class="fill-checkbox-box" :class="{ active: cbValue.includes(opt.value) }">
        <svg v-if="cbValue.includes(opt.value)" viewBox="0 0 12 10" fill="none" xmlns="http://www.w3.org/2000/svg" class="fill-checkbox-check">
          <path d="M1 5l3.5 3.5L11 1" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
      <span class="fill-option-label">{{ opt.label }}</span>
    </div>
    <!-- 已勾选且配置了附加填空的选项，在选项列表末尾显示对应输入框 -->
    <template v-if="hasOptionInput">
      <template v-for="opt in parseOptions()" :key="`ci-${opt.value}`">
        <el-input
          v-if="opt.hasInput && cbValue.includes(opt.value)"
          :model-value="getOptionInputText(opt.value)"
          placeholder="请注明"
          class="fill-option-input"
          @update:model-value="(v) => setOptionInputText(opt.value, String(v))"
        />
      </template>
    </template>
    <!-- 可选数量提示 -->
    <div v-if="cbMinSelect !== undefined || cbMaxSelect !== undefined" class="cb-select-hint">
      <template v-if="cbMinSelect !== undefined && cbMaxSelect !== undefined">
        请选 {{ cbMinSelect }}～{{ cbMaxSelect }} 项（已选 {{ cbValue.length }} 项）
      </template>
      <template v-else-if="cbMaxSelect !== undefined">
        最多选 {{ cbMaxSelect }} 项（已选 {{ cbValue.length }} / {{ cbMaxSelect }} 项）
      </template>
      <template v-else-if="cbMinSelect !== undefined">
        至少选 {{ cbMinSelect }} 项（已选 {{ cbValue.length }} 项）
      </template>
    </div>
  </div>

  <!-- 下拉 -->
  <el-select v-else-if="question.type === 'dropdown'" v-model="stringValue" placeholder="请选择" style="width: 100%">
    <el-option v-for="opt in parseOptions()" :key="opt.value" :label="opt.label" :value="opt.value" />
  </el-select>

  <!-- 级联 -->
  <el-cascader v-else-if="question.type === 'cascader'" v-model="cascaderValue" :options="parseCascaderOptions()" :props="{ label: 'label', value: 'value', children: 'children' }" placeholder="请选择" style="width: 100%" />

  <!-- 单行文本 -->
  <template v-else-if="question.type === 'input'">
    <!-- 省份下拉 -->
    <el-select v-if="contentLimit === 'province'" v-model="stringValue" placeholder="请选择省份" style="width: 100%" filterable clearable>
      <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
    </el-select>
    <!-- 下拉单选（自定义选项） -->
    <el-select
      v-else-if="contentLimit === 'dropdown_select'"
      v-model="stringValue"
      placeholder="请选择"
      style="width: 100%"
      clearable
      filterable
    >
      <el-option
        v-for="opt in (parseValidation().dropdownSelectOptions || [])"
        :key="opt"
        :label="opt"
        :value="opt"
      />
    </el-select>
    <!-- 数字 / 整数 / 小数 -->
    <template v-else-if="['number', 'integer', 'decimal'].includes(contentLimit)">
      <el-input-number
        v-model="numberValue"
        :min="rangeMin"
        :max="rangeMax"
        :precision="contentLimitPrecision"
        :step="contentLimit === 'integer' ? 1 : 0.1"
        style="width: 100%"
      />
      <div v-if="rangeMin !== undefined || rangeMax !== undefined || ['number', 'decimal'].includes(contentLimit)" style="font-size: 12px; color: #909399; margin-top: 4px">
        <span v-if="rangeMin !== undefined || rangeMax !== undefined">
          范围：{{ rangeMin !== undefined ? rangeMin : '不限' }} ～ {{ rangeMax !== undefined ? rangeMax : '不限' }}
        </span>
        <span v-if="['number', 'decimal'].includes(contentLimit)" :style="{ marginLeft: rangeMin !== undefined || rangeMax !== undefined ? '12px' : '0' }">
          最多 {{ contentLimitPrecision }} 位小数
        </span>
      </div>
    </template>
    <!-- 自定义下拉（无内容限制时才生效） -->
    <el-select
      v-else-if="parseValidation().dropdownEnabled"
      v-model="stringValue"
      filterable
      clearable
      placeholder="请选择或输入关键词搜索"
      style="width: 100%"
    >
      <el-option v-for="opt in parseOptions()" :key="opt.value" :label="opt.label" :value="opt.label" />
    </el-select>
    <!-- 普通文本输入（含格式校验：手机号/身份证/邮箱/日期格式/文本） -->
    <div v-else>
      <el-input
        v-model="stringValue"
        :placeholder="inputPlaceholder"
        :status="inputError ? 'error' : ''"
        @blur="onInputBlur"
        @input="onInputChange"
      />
      <div v-if="inputError" class="fill-input-error">
        {{ inputError }}
      </div>
    </div>
  </template>

  <!-- 多行文本 -->
  <template v-else-if="question.type === 'textarea'">
    <el-select
      v-if="parseValidation().dropdownEnabled"
      v-model="stringValue"
      filterable
      clearable
      placeholder="请选择或输入关键词搜索"
      style="width: 100%"
    >
      <el-option v-for="opt in parseOptions()" :key="opt.value" :label="opt.label" :value="opt.label" />
    </el-select>
    <el-input v-else v-model="stringValue" type="textarea" :rows="3" placeholder="请输入" />
  </template>

  <!-- 数字 -->
  <el-input-number
    v-else-if="question.type === 'number'"
    v-model="numberValue"
    :min="numMin"
    :max="numMax"
    :step="numStep"
    :precision="numPrecision"
    :controls="true"
    style="width: 100%"
    @change="(v) => emit('update:modelValue', v != null ? String(v) : '')"
  />

  <!-- 日期 -->
  <template v-else-if="question.type === 'date'">
    <el-date-picker v-if="dateType === 'daterange'" v-model="dateRangeValue" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 100%" />
    <el-date-picker v-else-if="dateType === 'datetime'" v-model="stringValue" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择日期时间" style="width: 100%" />
    <el-date-picker v-else v-model="stringValue" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
  </template>

  <!-- 滑块 -->
  <el-slider v-else-if="question.type === 'slider'" v-model="numberValue" :min="sliderMin" :max="sliderMax" :step="sliderStep" :show-input="sliderShowInput" />

  <!-- 评分 -->
  <el-rate v-else-if="question.type === 'rating'" :model-value="Number(modelValue) || 0" :max="rateMax" @update:model-value="(v) => emit('update:modelValue', String(v))" />

  <!-- NPS -->
  <div v-else-if="question.type === 'nps'">
    <div style="display: flex; gap: 4px; flex-wrap: wrap">
      <el-button v-for="n in npsRange" :key="n" size="small" :type="modelValue === String(n) ? 'primary' : 'default'" @click="emit('update:modelValue', String(n))">
        {{ n }}
      </el-button>
    </div>
    <div style="display: flex; justify-content: space-between; font-size: 12px; color: #909399; margin-top: 6px">
      <span>极不推荐</span><span>极力推荐</span>
    </div>
  </div>

  <!-- 多项填空 -->
  <div v-else-if="question.type === 'multi_input'" style="display: flex; flex-direction: column; gap: 10px">
    <div v-for="f in parseOptions()" :key="f.value" style="display: flex; align-items: center; gap: 8px">
      <span style="font-size: 14px; white-space: nowrap; color: #303133; min-width: 60px">{{ f.label }}：</span>
      <el-input :model-value="getSubAnswer(f.value)" placeholder="请输入" @update:model-value="(v) => setSubAnswer(f.value, String(v))" />
    </div>
  </div>

  <!-- 横向填空 -->
  <div v-else-if="question.type === 'inline_input'" style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap; line-height: 2">
    <template v-for="f in parseOptions()" :key="f.value">
      <span v-if="f.label" style="font-size: 14px; color: #303133">{{ f.label }}</span>
      <!-- inputHidden 为 true 时该项为纯文字（后缀），不渲染输入框 -->
      <el-input v-if="!f.inputHidden" :model-value="getSubAnswer(f.value)" placeholder="填写" style="width: 120px" @update:model-value="(v) => setSubAnswer(f.value, String(v))" />
    </template>
  </div>

  <!-- 矩阵单选 -->
  <div v-else-if="question.type === 'matrix_radio'" style="overflow-x: auto">
    <table style="width: 100%; border-collapse: collapse; font-size: 14px">
      <thead>
        <tr>
          <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa" />
          <th v-for="c in parseMatrixOpts().cols" :key="c.value" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; text-align: center">
            {{ c.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in parseMatrixOpts().rows" :key="r.value">
          <td style="border: 1px solid #ebeef5; padding: 8px; font-weight: 500">
            {{ r.label }}
          </td>
          <td
            v-for="c in parseMatrixOpts().cols"
            :key="c.value"
            style="border: 1px solid #ebeef5; padding: 8px; text-align: center; cursor: pointer"
            @click="setSubAnswer(r.value, c.value)"
          >
            <span class="fill-radio-dot" :class="{ active: getSubAnswer(r.value) === c.value }" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- 矩阵多选 -->
  <div v-else-if="question.type === 'matrix_checkbox'" style="overflow-x: auto">
    <table style="width: 100%; border-collapse: collapse; font-size: 14px">
      <thead>
        <tr>
          <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa" />
          <th v-for="c in parseMatrixOpts().cols" :key="c.value" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; text-align: center">
            {{ c.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in parseMatrixOpts().rows" :key="r.value">
          <td style="border: 1px solid #ebeef5; padding: 8px; font-weight: 500">
            {{ r.label }}
          </td>
          <td
            v-for="c in parseMatrixOpts().cols"
            :key="c.value"
            style="border: 1px solid #ebeef5; padding: 8px; text-align: center; cursor: pointer"
            @click="toggleMcbCell(r.value, c.value)"
          >
            <span class="fill-checkbox-box" :class="{ active: getMcbRow(r.value).includes(c.value) }" style="margin: 0 auto">
              <svg v-if="getMcbRow(r.value).includes(c.value)" viewBox="0 0 12 10" fill="none" xmlns="http://www.w3.org/2000/svg" class="fill-checkbox-check">
                <path d="M1 5l3.5 3.5L11 1" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- 矩阵填空 -->
  <div v-else-if="question.type === 'matrix_input'" style="overflow-x: auto">
    <table style="width: 100%; border-collapse: collapse; font-size: 14px">
      <thead>
        <tr>
          <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa" />
          <th v-for="c in parseMatrixOpts().cols" :key="c.value" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; text-align: center">
            {{ c.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in parseMatrixOpts().rows" :key="r.value">
          <td style="border: 1px solid #ebeef5; padding: 8px; font-weight: 500">
            {{ r.label }}
          </td>
          <td v-for="c in parseMatrixOpts().cols" :key="c.value" style="border: 1px solid #ebeef5; padding: 8px">
            <!-- 省份下拉 -->
            <el-select
              v-if="c.contentLimit === 'province'"
              :model-value="getMatrixCell(r.value, c.value)"
              size="small"
              placeholder="请选择省份"
              filterable
              clearable
              style="width: 100%"
              @update:model-value="(v) => setMatrixCell(r.value, c.value, String(v || ''))"
            >
              <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
            </el-select>
            <!-- 数字/整数/小数 -->
            <el-input-number
              v-else-if="['number', 'integer', 'decimal'].includes(c.contentLimit || '')"
              :model-value="getMatrixCell(r.value, c.value) !== '' ? Number(getMatrixCell(r.value, c.value)) : undefined"
              size="small"
              style="width: 100%"
              :min="c.rangeMin"
              :max="c.rangeMax"
              :precision="getContentLimitPrecision(c.contentLimit || '', c.decimalPlaces)"
              :step="c.contentLimit === 'integer' ? 1 : 0.1"
              :controls="false"
              @update:model-value="(v) => setMatrixCell(r.value, c.value, v != null ? String(v) : '')"
            />
            <!-- 日期格式 -->
            <el-date-picker
              v-else-if="c.contentLimit === 'date_format'"
              :model-value="getMatrixCell(r.value, c.value)"
              type="date"
              size="small"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
              @update:model-value="(v) => setMatrixCell(r.value, c.value, v || '')"
            />
            <!-- 下拉单选（自定义选项） -->
            <el-select
              v-else-if="c.contentLimit === 'dropdown_select'"
              :model-value="getMatrixCell(r.value, c.value)"
              size="small"
              placeholder="请选择"
              clearable
              filterable
              style="width: 100%"
              @update:model-value="(v) => setMatrixCell(r.value, c.value, String(v || ''))"
            >
              <el-option
                v-for="opt in (c.dropdownOptions || [])"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
            <!-- 普通文本/手机/身份证/邮箱等（含格式校验） -->
            <div v-else>
              <el-input
                :model-value="getMatrixCell(r.value, c.value)"
                size="small"
                :placeholder="getColPlaceholder(c.contentLimit)"
                :status="matrixCellErrors[`${r.value}__${c.value}`] ? 'error' : ''"
                @update:model-value="(v) => { setMatrixCell(r.value, c.value, String(v)); onMatrixCellInput(r.value, c.value, c) }"
                @blur="onMatrixCellBlur(r.value, c.value, c)"
              />
              <div v-if="matrixCellErrors[`${r.value}__${c.value}`]" class="fill-input-error">
                {{ matrixCellErrors[`${r.value}__${c.value}`] }}
              </div>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- 复合矩阵 -->
  <div v-else-if="question.type === 'matrix_complex'" style="overflow-x: auto" class="mc-fill-table-wrap">
    <table class="mc-fill-table">
      <thead>
        <tr>
          <th class="mc-fill-th mc-fill-th-row">
            项目
          </th>
          <th
            v-for="col in parseMCOpts().columns"
            :key="col.key"
            class="mc-fill-th"
            :style="{ minWidth: col.type === 'freq' ? '220px' : '140px' }"
          >
            {{ col.label || col.key }}
          </th>
        </tr>
      </thead>
      <tbody>
        <!-- ri 作为兜底 key，防止 r.value 重复或为空时多行共享同一存储槽 -->
        <tr v-for="(r, ri) in parseMCOpts().rows" :key="r.value ? `${r.value}_${ri}` : String(ri)">
          <td class="mc-fill-td mc-fill-td-row">
            <span class="mc-fill-row-code">{{ r.value }}</span>
            <span class="mc-fill-row-label">{{ r.label }}</span>
          </td>
          <td
            v-for="col in parseMCOpts().columns"
            :key="`${col.key}_${ri}`"
            class="mc-fill-td"
          >
            <!-- 不满足显示条件：占位 -->
            <span v-if="!evalMCShow(r.value || String(ri), col)" class="mc-fill-disabled">—</span>

            <!-- 单选：用 div 替代 label，避免移动端 label 触发合成点击导致多行联动 -->
            <div v-else-if="col.type === 'radio'" class="mc-fill-options-h">
              <div
                v-for="o in (col.options || [])"
                :key="o.value"
                class="mc-fill-radio" :class="[{ active: getMCCell(r.value || String(ri), col.key) === o.value }]"
                @click.stop="setMCCell(r.value || String(ri), col.key, getMCCell(r.value || String(ri), col.key) === o.value ? '' : o.value)"
              >
                <span class="mc-fill-dot" :class="{ active: getMCCell(r.value || String(ri), col.key) === o.value }" />
                <span class="mc-fill-radio-label">{{ o.label }}</span>
              </div>
            </div>

            <!-- 多选：同样用 div 替代 label -->
            <div v-else-if="col.type === 'checkbox'" class="mc-fill-options-h">
              <div
                v-for="o in (col.options || [])"
                :key="o.value"
                class="mc-fill-radio" :class="[{ active: mcCbValue(r.value || String(ri), col.key).includes(o.value) }]"
                @click.stop="mcToggleCb(r.value || String(ri), col.key, o.value)"
              >
                <span class="mc-fill-square" :class="{ active: mcCbValue(r.value || String(ri), col.key).includes(o.value) }">
                  <svg v-if="mcCbValue(r.value || String(ri), col.key).includes(o.value)" viewBox="0 0 12 10" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M1 5l3.5 3.5L11 1" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </span>
                <span class="mc-fill-radio-label">{{ o.label }}</span>
              </div>
            </div>

            <!-- 频率（互斥单选 + 数字） -->
            <div v-else-if="col.type === 'freq'" class="mc-fill-freq">
              <div
                v-for="u in (col.units || [])"
                :key="u.value"
                class="mc-fill-freq-row"
              >
                <span
                  class="mc-fill-dot" :class="[{ active: mcFreqVal(r.value || String(ri), col.key).unit === u.value }]"
                  @click.stop="mcSetFreqUnit(r.value || String(ri), col.key, u.value)"
                />
                <span class="mc-fill-freq-label" @click.stop="mcSetFreqUnit(r.value || String(ri), col.key, u.value)">{{ u.label }}</span>
                <el-input
                  :model-value="mcFreqVal(r.value || String(ri), col.key).unit === u.value ? mcFreqVal(r.value || String(ri), col.key).value : ''"
                  size="small"
                  type="number"
                  :disabled="mcFreqVal(r.value || String(ri), col.key).unit !== u.value"
                  placeholder="—"
                  style="width: 80px"
                  @update:model-value="(v) => mcSetFreqValue(r.value || String(ri), col.key, String(v))"
                />
              </div>
            </div>

            <!-- 填空 -->
            <div v-else-if="col.type === 'input'" class="mc-fill-input-wrap">
              <el-input-number
                v-if="['number', 'integer', 'decimal'].includes(mcInputType(col))"
                :model-value="getMCCell(r.value || String(ri), col.key) !== undefined && getMCCell(r.value || String(ri), col.key) !== '' ? Number(getMCCell(r.value || String(ri), col.key)) : undefined"
                size="small"
                :precision="mcInputType(col) === 'integer' ? 0 : 6"
                :step="mcInputType(col) === 'integer' ? 1 : 0.1"
                style="flex: 1; min-width: 80px"
                :controls="false"
                @change="(v) => setMCCell(r.value || String(ri), col.key, v != null ? String(v) : '')"
              />
              <el-date-picker
                v-else-if="mcInputType(col) === 'date'"
                :model-value="getMCCell(r.value || String(ri), col.key)"
                type="date"
                size="small"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="flex: 1"
                @update:model-value="(v) => setMCCell(r.value || String(ri), col.key, v || '')"
              />
              <el-input
                v-else
                :model-value="getMCCell(r.value || String(ri), col.key) || ''"
                size="small"
                :placeholder="mcInputPlaceholder(col)"
                style="flex: 1; min-width: 80px"
                @update:model-value="(v) => setMCCell(r.value || String(ri), col.key, String(v))"
              />
              <span v-if="col.suffix" class="mc-fill-suffix">{{ col.suffix }}</span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- 矩阵量表 -->
  <div v-else-if="question.type === 'matrix_scale'" style="overflow-x: auto">
    <table style="width: 100%; border-collapse: collapse; font-size: 14px">
      <thead>
        <tr>
          <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa" />
          <th v-for="n in scaleRange" :key="n" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; text-align: center">
            {{ parseScaleOpts().scaleLabels[String(n)] || n }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in parseScaleOpts().rows" :key="r.value">
          <td style="border: 1px solid #ebeef5; padding: 8px; font-weight: 500">
            {{ r.label }}
          </td>
          <td
            v-for="n in scaleRange"
            :key="n"
            style="border: 1px solid #ebeef5; padding: 8px; text-align: center; cursor: pointer"
            @click="setSubAnswer(r.value, String(n))"
          >
            <span class="fill-radio-dot" :class="{ active: getSubAnswer(r.value) === String(n) }" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- 子表单 / 自增表格 -->
  <div v-else-if="question.type === 'dynamic_table'" style="overflow-x: auto">
    <table style="width: 100%; border-collapse: collapse; font-size: 14px">
      <thead>
        <tr>
          <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; width: 40px">
            #
          </th>
          <th v-for="col in parseOptions()" :key="col.value" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa">
            {{ col.label }}
          </th>
          <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; width: 60px">
            操作
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, ri) in tableRows" :key="ri">
          <td style="border: 1px solid #ebeef5; padding: 8px; text-align: center">
            {{ ri + 1 }}
          </td>
          <td v-for="col in parseOptions()" :key="col.value" style="border: 1px solid #ebeef5; padding: 8px">
            <!-- 省份下拉 -->
            <el-select
              v-if="col.contentLimit === 'province'"
              v-model="row[col.value]"
              size="small"
              placeholder="请选择省份"
              filterable
              clearable
              style="width: 100%"
              @change="syncTable"
            >
              <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
            </el-select>
            <!-- 数字 / 整数 / 小数 -->
            <el-input-number
              v-else-if="['number', 'integer', 'decimal'].includes(col.contentLimit || '')"
              :model-value="row[col.value] !== '' ? Number(row[col.value]) : undefined"
              size="small"
              style="width: 100%"
              :min="col.rangeMin"
              :max="col.rangeMax"
              :precision="getContentLimitPrecision(col.contentLimit || '', col.decimalPlaces)"
              :step="col.contentLimit === 'integer' ? 1 : 0.1"
              @update:model-value="(v) => { row[col.value] = v != null ? String(v) : ''; syncTable() }"
            />
            <!-- 日期格式 -->
            <el-date-picker
              v-else-if="col.contentLimit === 'date_format'"
              :model-value="row[col.value]"
              type="date"
              size="small"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
              @update:model-value="(v) => { row[col.value] = v || ''; syncTable() }"
            />
            <!-- 下拉单选（自定义选项） -->
            <el-select
              v-else-if="col.contentLimit === 'dropdown_select'"
              v-model="row[col.value]"
              size="small"
              placeholder="请选择"
              clearable
              filterable
              style="width: 100%"
              @change="syncTable"
            >
              <el-option
                v-for="opt in (col.dropdownOptions || [])"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
            <!-- 其他（普通文本、手机号、身份证、邮箱等文本类限制，含格式校验） -->
            <div v-else>
              <el-input
                v-model="row[col.value]"
                size="small"
                :placeholder="getColPlaceholder(col.contentLimit)"
                :status="tableCellErrors[`${ri}__${col.value}`] ? 'error' : ''"
                @change="syncTable"
                @input="onTableCellInput(ri, col.value, col, row[col.value])"
                @blur="onTableCellBlur(ri, col.value, col, row[col.value])"
              />
              <div v-if="tableCellErrors[`${ri}__${col.value}`]" class="fill-input-error">
                {{ tableCellErrors[`${ri}__${col.value}`] }}
              </div>
            </div>
          </td>
          <td style="border: 1px solid #ebeef5; padding: 8px; text-align: center">
            <el-button size="small" type="danger" plain @click="removeTableRow(ri)">
              删除
            </el-button>
          </td>
        </tr>
      </tbody>
    </table>
    <el-button size="small" style="margin-top: 8px" @click="addTableRow">
      + 添加一行
    </el-button>
  </div>

  <!-- 排序 -->
  <div v-else-if="question.type === 'sort'">
    <draggable v-model="sortItems" item-key="value" animation="200" ghost-class="sort-ghost" @end="onSortEnd">
      <template #item="{ element, index: si }">
        <div class="sort-item">
          <span class="sort-handle">⠿</span>
          <span>{{ si + 1 }}. {{ element.label }}</span>
        </div>
      </template>
    </draggable>
    <div style="font-size: 12px; color: #909399; margin-top: 6px">
      拖拽排序
    </div>
  </div>

  <!-- 图片选择 -->
  <div v-else-if="question.type === 'image_choice'" style="display: flex; gap: 12px; flex-wrap: wrap">
    <div
      v-for="opt in parseOptions()"
      :key="opt.value"
      class="img-choice-card" :class="[{ active: isImageSelected(opt.value) }]"
      @click="toggleImageChoice(opt.value)"
    >
      <img v-if="opt.imageUrl" :src="opt.imageUrl" class="img-choice-pic">
      <div v-else class="img-choice-placeholder">
        无图片
      </div>
      <div class="img-choice-label">
        {{ opt.label }}
      </div>
    </div>
  </div>

  <!-- 文件上传 -->
  <div v-else-if="question.type === 'file_upload'">
    <el-upload
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      :file-list="uploadFileList"
      :limit="uploadMaxCount"
      :accept="uploadAccept"
    >
      <el-button type="primary" size="small">
        选择文件
      </el-button>
      <template #tip>
        <div class="el-upload__tip">
          {{ uploadAccept ? `支持 ${uploadAccept}，` : '' }}最大 {{ uploadMaxSize }}MB
        </div>
      </template>
    </el-upload>
  </div>

  <!-- 图片上传 -->
  <div v-else-if="question.type === 'image_upload'">
    <el-upload
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      :file-list="uploadFileList"
      :limit="uploadMaxCount"
      accept="image/*"
      list-type="picture-card"
    >
      <el-icon :size="20">
        <Plus />
      </el-icon>
    </el-upload>
    <div style="font-size: 12px; color: #909399; margin-top: 4px">
      最多 {{ uploadMaxCount }} 张，每张最大 {{ uploadMaxSize }}MB
    </div>
  </div>

  <!-- 文字描述 -->
  <div v-else-if="question.type === 'description'" style="color: #606266; font-size: 14px; line-height: 1.6; white-space: pre-wrap">
    {{ question.description || '' }}
  </div>

  <!-- 分割线 -->
  <el-divider v-else-if="question.type === 'divider'" />

  <!-- 图片展示 -->
  <div v-else-if="question.type === 'image_display'">
    <img v-if="parseImageDisplayOpts().imageUrl" :src="parseImageDisplayOpts().imageUrl" :alt="parseImageDisplayOpts().alt" style="max-width: 100%; border-radius: 8px">
  </div>

  <!-- 电子签名 -->
  <div v-else-if="question.type === 'signature'">
    <SignaturePad
      :model-value="modelValue"
      :pen-color="parseValidation().penColor || '#222222'"
      :pen-width="parseValidation().penWidth || 2"
      :height="parseValidation().height || 160"
      @update:model-value="(v) => emit('update:modelValue', v)"
    />
  </div>

  <!-- 自动计算 -->
  <div v-else-if="question.type === 'formula'" class="formula-result">
    <div class="formula-result-row">
      <div class="formula-result-box" :class="{ filled: !!formulaResult.display, error: !!formulaResult.error }">
        <span v-if="formulaResult.display" class="formula-result-num">{{ formulaResult.display }}</span>
        <span v-else-if="formulaResult.error" class="formula-result-tip err">{{ formulaResult.error }}</span>
        <span v-else class="formula-result-tip">完成相关题目后将自动计算结果</span>
      </div>
      <span v-if="formulaConfig.unit" class="formula-result-unit">{{ formulaConfig.unit }}</span>
    </div>
    <div v-if="formulaWarning" class="formula-result-warning">
      ⚠ {{ formulaWarning }}
    </div>
  </div>

  <!-- 分页标记（由父组件处理，此处不渲染） -->
  <template v-else-if="question.type === 'page_break'" />

  <!-- 兜底 -->
  <el-input v-else v-model="stringValue" placeholder="请输入" />
</template>

<style scoped>
.fill-options {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
  gap: 8px;
}
.fill-option-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1.5px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px 14px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s;
  background: #fff;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}
.fill-option-wrap:active {
  background: var(--el-color-primary-light-9);
}
.fill-option-wrap.is-selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

/* 自定义单选圆点 */
.fill-radio-dot {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid var(--el-border-color);
  background: #fff;
  transition:
    border-color 0.2s,
    background 0.2s;
  position: relative;
}
.fill-radio-dot.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
}
.fill-radio-dot.active::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
}
.fill-option-label {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  white-space: normal;
}

/* 自定义多选方框 */
.fill-checkbox-box {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 2px solid var(--el-border-color);
  background: #fff;
  transition:
    border-color 0.2s,
    background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.fill-checkbox-box.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
}
.fill-checkbox-check {
  width: 10px;
  height: 8px;
  display: block;
}
/* 达到上限后未选中的选项置灰，pointer-events: none 从 DOM 层彻底屏蔽点击 */
.fill-option-wrap.is-disabled {
  opacity: 0.45;
  cursor: not-allowed;
  pointer-events: none;
}
/* 可选数量提示 */
.cb-select-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  padding: 0 2px;
}

.sort-item {
  padding: 10px 14px;
  border: 1.5px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  cursor: grab;
  transition: border-color 0.2s;
}
.sort-item:hover {
  border-color: var(--el-color-primary-light-5);
}
.sort-handle {
  color: #c0c4cc;
  font-size: 16px;
}
.sort-ghost {
  opacity: 0.4;
  background: var(--el-color-primary-light-9);
  border: 1px dashed var(--el-color-primary);
}

.img-choice-card {
  width: 140px;
  border: 2px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}
.img-choice-card:hover {
  border-color: var(--el-color-primary-light-5);
}
.img-choice-card.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}
.img-choice-pic {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}
.img-choice-placeholder {
  width: 100%;
  height: 100px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
}
.img-choice-label {
  padding: 6px 8px;
  font-size: 13px;
  color: #303133;
}

/* 内容限制错误提示 */
.fill-input-error {
  font-size: 12px;
  color: var(--el-color-danger);
  margin-top: 4px;
}

/* 选项后附加填空输入框 */
.fill-option-input {
  margin-top: 4px;
}

/* 自动计算结果区 */
.formula-result {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.formula-result-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.formula-result-box {
  flex: 1;
  min-height: 38px;
  padding: 8px 12px;
  border: 1.5px dashed var(--el-border-color);
  border-radius: 8px;
  background: #fafbfc;
  display: flex;
  align-items: center;
  font-size: 16px;
  color: #909399;
  transition:
    border-color 0.2s,
    background 0.2s;
}
.formula-result-box.filled {
  border-style: solid;
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}
.formula-result-box.error {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}
.formula-result-num {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 18px;
  letter-spacing: 0.5px;
}
.formula-result-tip {
  font-size: 13px;
  font-weight: normal;
}
.formula-result-tip.err {
  color: var(--el-color-danger);
}
.formula-result-unit {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
  flex-shrink: 0;
}
.formula-result-warning {
  font-size: 12px;
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 4px;
  padding: 6px 10px;
  line-height: 1.5;
}

/* ===== 复合矩阵填写 ===== */
.mc-fill-table-wrap {
  width: 100%;
}
.mc-fill-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.mc-fill-th {
  border: 1px solid #ebeef5;
  padding: 8px 10px;
  background: #f5f7fa;
  text-align: center;
  font-weight: 600;
  color: #606266;
}
.mc-fill-th-row {
  text-align: left;
  min-width: 130px;
}
.mc-fill-td {
  border: 1px solid #ebeef5;
  padding: 8px 10px;
  vertical-align: middle;
}
.mc-fill-td-row {
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}
.mc-fill-row-code {
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}
.mc-fill-row-label {
  font-size: 13px;
}
.mc-fill-disabled {
  color: #c0c4cc;
  font-size: 12px;
}

.mc-fill-options-h {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: center;
}
.mc-fill-radio {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 16px;
  border: 1.5px solid var(--el-border-color-lighter);
  cursor: pointer;
  user-select: none;
  background: #fff;
  transition:
    border-color 0.15s,
    background 0.15s;
}
.mc-fill-radio.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.mc-fill-dot {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid var(--el-border-color);
  background: #fff;
  position: relative;
  cursor: pointer;
}
.mc-fill-dot.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
}
.mc-fill-dot.active::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #fff;
}
.mc-fill-square {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  border: 2px solid var(--el-border-color);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mc-fill-square.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
}
.mc-fill-square svg {
  width: 9px;
  height: 7px;
}
.mc-fill-radio-label {
  font-size: 13px;
  color: #303133;
}

.mc-fill-freq {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: stretch;
}
.mc-fill-freq-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.mc-fill-freq-label {
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  min-width: 48px;
  text-align: left;
}

.mc-fill-input-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
}
.mc-fill-suffix {
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
</style>
