<script lang="ts" setup>
/* eslint-disable vue/no-mutating-props */
import type { QuestionItem } from "../../apis/type"
import { Delete, List, Plus } from "@element-plus/icons-vue"
import { DISEASE_CODE_OPTIONS } from "../diseaseCodes"
import { evalFormula, extractFormulaRefs, remapFormulaRefs } from "../utils/formula"
import ImageUploadInput from "./ImageUploadInput.vue"

interface OptionObj { label: string, value: string, imageUrl?: string, contentLimit?: string, rangeMin?: number, rangeMax?: number, decimalPlaces?: number, inputHidden?: boolean, hasInput?: boolean, dropdownOptions?: string[] }
interface TypeOption { label: string, value: string, group: string }

const props = defineProps<{
  question: QuestionItem
  questions: QuestionItem[]
  questionIndex: number
  typeOptions: TypeOption[]
}>()
const layoutTypes = new Set(["divider", "page_break"])
/** 不依赖 contentLimit 即可作为公式变量的题型 */
const formulaNumericTypes = new Set(["number", "slider", "rating", "nps", "formula"])
/** input 题视为数字的 contentLimit */
const formulaInputContentLimits = new Set(["number", "integer", "decimal"])

/** 与 design/index.vue 的 getStableKey 保持一致 */
function getStableKey(q: QuestionItem): string {
  return q.id != null ? String(q.id) : `t${q._tempKey}`
}

/** 判断某题是否可作为公式变量（数值类） */
function isFormulaCompatible(q: QuestionItem): boolean {
  if (formulaNumericTypes.has(q.type)) return true
  if (q.type === "input") {
    try {
      const v = q.validationRules ? JSON.parse(q.validationRules) : {}
      return formulaInputContentLimits.has(String(v.contentLimit || ""))
    } catch {
      return false
    }
  }
  return false
}

/** 内容限制下拉选项（供 input 题目和 dynamic_table 列共用） */
const CONTENT_LIMIT_OPTIONS = [
  { label: "不限", value: "none" },
  { label: "文本", value: "text" },
  { label: "日期格式（YYYY-MM-DD）", value: "date_format" },
  { label: "数字", value: "number" },
  { label: "整数", value: "integer" },
  { label: "小数", value: "decimal" },
  { label: "身份证号（自动校验18位）", value: "id_card" },
  { label: "手机号（自动校验11位）", value: "phone" },
  { label: "邮箱", value: "email" },
  { label: "省份（下拉选择）", value: "province" },
  { label: "下拉单选", value: "dropdown_select" }
] as const

const nonRequiredTypes = ["description", "divider", "image_display", "page_break", "formula"]

function parseOptions(): OptionObj[] {
  if (!props.question.options) return []
  try {
    return JSON.parse(props.question.options)
  } catch {
    return []
  }
}

function updateOptionLabel(index: number, value: string) {
  const opts = parseOptions()
  if (opts[index]) {
    opts[index].label = value
    props.question.options = JSON.stringify(opts)
  }
}

function updateOptionImageUrl(index: number, value: string) {
  const opts = parseOptions()
  if (opts[index]) {
    opts[index].imageUrl = value
    props.question.options = JSON.stringify(opts)
  }
}

/** 更新选项的任意字段（用于 dynamic_table 列内容限制等扩展字段） */
function updateOptionField(index: number, key: keyof OptionObj, value: any) {
  const opts = parseOptions()
  if (!opts[index]) return
  if (value === undefined || value === null) delete opts[index][key]
  else (opts[index] as any)[key] = value
  props.question.options = JSON.stringify(opts)
}

/** dynamic_table 列切换内容限制时同步小数位数默认值 */
function onOptionContentLimitChange(index: number, v: string) {
  updateOptionField(index, "contentLimit", v === "none" ? undefined : v)
  if (v === "number" || v === "decimal") {
    const opt = parseOptions()[index]
    if (opt && opt.decimalPlaces == null) updateOptionField(index, "decimalPlaces", 2)
  } else if (v === "integer" || v === "none") {
    updateOptionField(index, "decimalPlaces", undefined)
  }
}

function nextOptionValue(opts: OptionObj[]): string {
  const existing = new Set(opts.map(o => o.value))
  let i = opts.length + 1
  while (existing.has(String(i))) i++
  return String(i)
}

function addOption() {
  const opts = parseOptions()
  const isImageChoice = props.question.type === "image_choice"
  const val = nextOptionValue(opts)
  opts.push({ label: `选项${val}`, value: val, ...(isImageChoice ? { imageUrl: "" } : {}) })
  props.question.options = JSON.stringify(opts)
}

function removeOption(idx: number) {
  const opts = parseOptions()
  opts.splice(idx, 1)
  props.question.options = JSON.stringify(opts)
}

// 批量添加选项
const batchPopoverVisible = ref(false)
const batchText = ref("")

function confirmBatchAdd() {
  const lines = batchText.value
    .split("\n")
    .map((l: string) => l.trim())
    .filter((l: string) => l.length > 0)
  if (!lines.length) {
    ElMessage.warning("请至少输入一行选项")
    return
  }
  const opts = parseOptions()
  for (const label of lines) {
    const val = nextOptionValue(opts)
    opts.push({ label, value: val })
  }
  props.question.options = JSON.stringify(opts)
  batchText.value = ""
  batchPopoverVisible.value = false
  ElMessage.success(`已批量添加 ${lines.length} 个选项`)
}

/* ============================================================
 * 下拉单选选项编辑弹窗（供 dynamic_table 列 / matrix_input 列 / input 共用）
 * ============================================================ */

interface DropdownSelectDialogState {
  visible: boolean
  /** col: dynamic_table 列；matrixCol: matrix_input 列；input: input 题型 */
  source: "col" | "matrixCol" | "input"
  colIdx: number
  /** 弹窗内临时编辑的选项列表 */
  items: string[]
}

const dropdownSelectDialog = reactive<DropdownSelectDialogState>({
  visible: false,
  source: "col",
  colIdx: -1,
  items: []
})

/** 读取 dynamic_table 列的 dropdownOptions */
function getColDropdownOptions(oi: number): string[] {
  const opts = parseOptions()
  return opts[oi]?.dropdownOptions ?? []
}

/** 读取 matrix_input 列的 dropdownOptions */
function getMatrixColDropdownOptions(ci: number): string[] {
  const m = parseMatrixOpts()
  return m.cols[ci]?.dropdownOptions ?? []
}

/** 读取 input 题型的 dropdown_select 选项（存于 validationRules.dropdownSelectOptions） */
function getInputDropdownSelectOptions(): string[] {
  return parseValidation().dropdownSelectOptions ?? []
}

/** 打开下拉单选弹窗 */
function openDropdownSelectDialog(source: DropdownSelectDialogState["source"], colIdx = -1) {
  dropdownSelectDialog.source = source
  dropdownSelectDialog.colIdx = colIdx
  if (source === "col") {
    dropdownSelectDialog.items = [...getColDropdownOptions(colIdx)]
  } else if (source === "matrixCol") {
    dropdownSelectDialog.items = [...getMatrixColDropdownOptions(colIdx)]
  } else {
    dropdownSelectDialog.items = [...getInputDropdownSelectOptions()]
  }
  dropdownSelectDialog.visible = true
}

/** 弹窗新增一行 */
function addDropdownSelectItem() {
  dropdownSelectDialog.items.push("")
}

/** 弹窗删除一行 */
function removeDropdownSelectItem(idx: number) {
  dropdownSelectDialog.items.splice(idx, 1)
}

/** 确认保存下拉单选选项 */
function confirmDropdownSelectDialog() {
  const items = dropdownSelectDialog.items.map(s => s.trim()).filter(s => s.length > 0)
  if (!items.length) {
    ElMessage.warning("请至少添加一个选项")
    return
  }

  if (dropdownSelectDialog.source === "col") {
    updateOptionField(dropdownSelectDialog.colIdx, "dropdownOptions", items)
  } else if (dropdownSelectDialog.source === "matrixCol") {
    updateMatrixColField(dropdownSelectDialog.colIdx, "dropdownOptions", items)
  } else {
    setV("dropdownSelectOptions", items)
  }
  dropdownSelectDialog.visible = false
}

/** 批量粘贴：一行一个选项 */
const dsDialogBatchText = ref("")
const dsDialogBatchVisible = ref(false)

function confirmDsBatch() {
  const lines = dsDialogBatchText.value.split("\n").map(l => l.trim()).filter(l => l.length > 0)
  if (!lines.length) {
    ElMessage.warning("请至少输入一行")
    return
  }
  for (const l of lines) dropdownSelectDialog.items.push(l)
  dsDialogBatchText.value = ""
  dsDialogBatchVisible.value = false
}

function parseMatrixOpts(): { rows: OptionObj[], cols: OptionObj[] } {
  if (!props.question.options) return { rows: [], cols: [] }
  try {
    return JSON.parse(props.question.options)
  } catch {
    return { rows: [], cols: [] }
  }
}

function updateMatrixRowLabel(ri: number, value: string) {
  const m = parseMatrixOpts()
  m.rows[ri].label = value
  props.question.options = JSON.stringify(m)
}
function addMatrixRow() {
  const m = parseMatrixOpts()
  const existing = new Set(m.rows.map(r => r.value))
  let i = m.rows.length + 1
  while (existing.has(`r${i}`)) i++
  m.rows.push({ label: `行${i}`, value: `r${i}` })
  props.question.options = JSON.stringify(m)
}
function removeMatrixRow(ri: number) {
  const m = parseMatrixOpts()
  m.rows.splice(ri, 1)
  props.question.options = JSON.stringify(m)
}
function updateMatrixColLabel(ci: number, value: string) {
  const m = parseMatrixOpts()
  m.cols[ci].label = value
  props.question.options = JSON.stringify(m)
}
function addMatrixCol() {
  const m = parseMatrixOpts()
  const existing = new Set(m.cols.map(c => c.value))
  let i = m.cols.length + 1
  while (existing.has(`c${i}`)) i++
  m.cols.push({ label: `列${i}`, value: `c${i}` })
  props.question.options = JSON.stringify(m)
}
function removeMatrixCol(ci: number) {
  const m = parseMatrixOpts()
  m.cols.splice(ci, 1)
  props.question.options = JSON.stringify(m)
}
/** 更新矩阵填空列的任意字段（用于内容限制等扩展配置） */
function updateMatrixColField(ci: number, key: keyof OptionObj, value: any) {
  const m = parseMatrixOpts()
  if (!m.cols[ci]) return
  if (value === undefined || value === null) delete (m.cols[ci] as any)[key]
  else (m.cols[ci] as any)[key] = value
  props.question.options = JSON.stringify(m)
}

/** matrix_input 列切换内容限制时同步小数位数默认值 */
function onMatrixColContentLimitChange(ci: number, v: string) {
  updateMatrixColField(ci, "contentLimit", v === "none" ? undefined : v)
  if (v === "number" || v === "decimal") {
    const col = parseMatrixOpts().cols[ci]
    if (col && col.decimalPlaces == null) updateMatrixColField(ci, "decimalPlaces", 2)
  } else if (v === "integer" || v === "none") {
    updateMatrixColField(ci, "decimalPlaces", undefined)
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
function updateScaleRowLabel(ri: number, value: string) {
  const s = parseScaleOpts()
  s.rows[ri].label = value
  props.question.options = JSON.stringify(s)
}
function addScaleRow() {
  const s = parseScaleOpts()
  const existing = new Set(s.rows.map(r => r.value))
  let i = s.rows.length + 1
  while (existing.has(`r${i}`)) i++
  s.rows.push({ label: `行${i}`, value: `r${i}` })
  props.question.options = JSON.stringify(s)
}
function removeScaleRow(ri: number) {
  const s = parseScaleOpts()
  s.rows.splice(ri, 1)
  props.question.options = JSON.stringify(s)
}
function updateScaleField(field: "scaleMin" | "scaleMax", value: number | undefined) {
  if (value == null) return
  const s = parseScaleOpts()
  s[field] = value
  props.question.options = JSON.stringify(s)
}
function updateScaleLabel(n: number, value: string) {
  const s = parseScaleOpts()
  if (!s.scaleLabels || typeof s.scaleLabels !== "object") s.scaleLabels = {}
  // 保留空字符串，避免编辑预置标签（如首尾列）时被“删键回退”覆盖
  s.scaleLabels[String(n)] = value ?? ""
  props.question.options = JSON.stringify(s)
}
/** 根据当前 scaleMin/scaleMax 生成列序号数组 */
function scaleRange(): number[] {
  const s = parseScaleOpts()
  return Array.from({ length: s.scaleMax - s.scaleMin + 1 }, (_, i) => i + s.scaleMin)
}

function parseImageDisplayOpts(): { imageUrl: string, alt: string } {
  if (!props.question.options) return { imageUrl: "", alt: "" }
  try {
    return JSON.parse(props.question.options)
  } catch {
    return { imageUrl: "", alt: "" }
  }
}
function updateImageDisplay(field: "imageUrl" | "alt", value: string) {
  const o = parseImageDisplayOpts()
  o[field] = value
  props.question.options = JSON.stringify(o)
}

/* ============================================================
 * 复合矩阵（matrix_complex）题型专用
 * ============================================================ */
interface MCRowDef { label: string, value: string }
interface MCColShowWhen { column: string, op: "eq" | "neq" | "selected" | "not_empty", value?: string }
interface MCColDef {
  key: string
  label: string
  type: "radio" | "checkbox" | "input" | "freq"
  width?: string
  options?: { label: string, value: string }[]
  units?: { label: string, value: string }[]
  inputType?: string
  suffix?: string
  placeholder?: string
  showWhen?: MCColShowWhen
}
interface MCOpts { rows: MCRowDef[], columns: MCColDef[] }

function parseMCOpts(): MCOpts {
  const fallback: MCOpts = { rows: [], columns: [] }
  if (!props.question.options) return fallback
  try {
    const p = JSON.parse(props.question.options)
    return { rows: Array.isArray(p.rows) ? p.rows : [], columns: Array.isArray(p.columns) ? p.columns : [] }
  } catch {
    return fallback
  }
}
function saveMCOpts(opts: MCOpts) {
  props.question.options = JSON.stringify(opts)
}

function mcAddRow() {
  const o = parseMCOpts()
  const existing = new Set(o.rows.map(r => r.value))
  let i = o.rows.length + 1
  while (existing.has(`r${i}`)) i++
  o.rows.push({ label: `项目${i}`, value: `r${i}` })
  saveMCOpts(o)
}
function mcRemoveRow(idx: number) {
  const o = parseMCOpts()
  o.rows.splice(idx, 1)
  saveMCOpts(o)
}
function mcUpdateRow(idx: number, key: keyof MCRowDef, value: string) {
  const o = parseMCOpts()
  if (!o.rows[idx]) return
  o.rows[idx][key] = value
  saveMCOpts(o)
}

function mcDefaultColByType(type: MCColDef["type"]): Partial<MCColDef> {
  if (type === "radio" || type === "checkbox") {
    return { options: [{ label: "选项1", value: "1" }, { label: "选项2", value: "2" }] }
  }
  if (type === "freq") {
    return { units: [
      { label: "次/天", value: "day" },
      { label: "次/周", value: "week" },
      { label: "次/月", value: "month" },
      { label: "次/年", value: "year" }
    ] }
  }
  if (type === "input") {
    return { inputType: "decimal", suffix: "" }
  }
  return {}
}

function mcAddColumn() {
  const o = parseMCOpts()
  const existing = new Set(o.columns.map(c => c.key))
  let i = o.columns.length + 1
  while (existing.has(`c${i}`)) i++
  const col: MCColDef = { key: `c${i}`, label: `列${i}`, type: "input", ...mcDefaultColByType("input") }
  o.columns.push(col)
  saveMCOpts(o)
}
function mcRemoveColumn(idx: number) {
  const o = parseMCOpts()
  const removed = o.columns[idx]
  o.columns.splice(idx, 1)
  // 清理其它列对已删除列的依赖引用
  if (removed) {
    for (const c of o.columns) {
      if (c.showWhen?.column === removed.key) delete c.showWhen
    }
  }
  saveMCOpts(o)
}
function mcUpdateCol(idx: number, key: keyof MCColDef, value: any) {
  const o = parseMCOpts()
  const col = o.columns[idx]
  if (!col) return
  // 修改列 key 时，同步其它列对该 key 的引用，避免依赖断裂
  if (key === "key" && typeof value === "string" && value && value !== col.key) {
    const oldKey = col.key
    for (const c of o.columns) {
      if (c.showWhen?.column === oldKey) c.showWhen.column = value
    }
  }
  if (value === undefined || value === null || value === "") delete col[key]
  else (col as any)[key] = value
  saveMCOpts(o)
}
function mcChangeColType(idx: number, type: MCColDef["type"]) {
  const o = parseMCOpts()
  if (!o.columns[idx]) return
  const col = o.columns[idx]
  col.type = type
  delete col.options
  delete col.units
  delete col.inputType
  delete col.suffix
  Object.assign(col, mcDefaultColByType(type))
  saveMCOpts(o)
}

/** 列内选项操作（radio/checkbox） */
function mcAddColOption(colIdx: number) {
  const o = parseMCOpts()
  const col = o.columns[colIdx]
  if (!col) return
  if (!col.options) col.options = []
  const opts = col.options
  const existing = new Set(opts.map(x => x.value))
  let i = opts.length + 1
  while (existing.has(String(i))) i++
  opts.push({ label: `选项${i}`, value: String(i) })
  saveMCOpts(o)
}
function mcRemoveColOption(colIdx: number, optIdx: number) {
  const o = parseMCOpts()
  o.columns[colIdx]?.options?.splice(optIdx, 1)
  saveMCOpts(o)
}
function mcUpdateColOption(colIdx: number, optIdx: number, key: "label" | "value", value: string) {
  const o = parseMCOpts()
  const opt = o.columns[colIdx]?.options?.[optIdx]
  if (opt) {
    opt[key] = value
    saveMCOpts(o)
  }
}

/** 列内单位操作（freq） */
function mcAddColUnit(colIdx: number) {
  const o = parseMCOpts()
  const col = o.columns[colIdx]
  if (!col) return
  if (!col.units) col.units = []
  const units = col.units
  const existing = new Set(units.map(u => u.value))
  let i = units.length + 1
  while (existing.has(`u${i}`)) i++
  units.push({ label: `单位${i}`, value: `u${i}` })
  saveMCOpts(o)
}
function mcRemoveColUnit(colIdx: number, unitIdx: number) {
  const o = parseMCOpts()
  o.columns[colIdx]?.units?.splice(unitIdx, 1)
  saveMCOpts(o)
}
function mcUpdateColUnit(colIdx: number, unitIdx: number, key: "label" | "value", value: string) {
  const o = parseMCOpts()
  const u = o.columns[colIdx]?.units?.[unitIdx]
  if (u) {
    u[key] = value
    saveMCOpts(o)
  }
}

/** 显示依赖 showWhen 操作 */
function mcGetShowWhen(colIdx: number): MCColShowWhen | undefined {
  return parseMCOpts().columns[colIdx]?.showWhen
}
function mcToggleShowWhen(colIdx: number, enabled: boolean) {
  const o = parseMCOpts()
  const col = o.columns[colIdx]
  if (!col) return
  if (enabled) {
    if (!col.showWhen) {
      const firstOther = o.columns.find((_, i) => i !== colIdx)
      col.showWhen = { column: firstOther?.key || "", op: "eq", value: "" }
    }
  } else {
    delete col.showWhen
  }
  saveMCOpts(o)
}
function mcUpdateShowWhen<K extends keyof MCColShowWhen>(colIdx: number, key: K, value: MCColShowWhen[K]) {
  const o = parseMCOpts()
  const sw = o.columns[colIdx]?.showWhen
  if (!sw) return
  sw[key] = value
  saveMCOpts(o)
}

/** 用于"依赖列"下拉的可选项（排除当前列自身） */
function mcOtherColumns(colIdx: number) {
  return parseMCOpts().columns.filter((_, i) => i !== colIdx)
}

/** 根据依赖列类型，给出 value 输入提示（仅展示用） */
function mcShowWhenValueOptions(colIdx: number) {
  const sw = mcGetShowWhen(colIdx)
  if (!sw) return [] as { label: string, value: string }[]
  const target = parseMCOpts().columns.find(c => c.key === sw.column)
  if (!target) return []
  if (target.type === "radio" || target.type === "checkbox") return target.options || []
  if (target.type === "freq") return (target.units || []).map(u => ({ label: u.label, value: u.value }))
  return []
}

// ---- input / textarea 下拉选项 ----

/** 读取下拉是否启用（存于 validationRules.dropdownEnabled） */
const dropdownEnabled = computed(() => parseValidation().dropdownEnabled === true)

function toggleDropdown(val: boolean) {
  setV("dropdownEnabled", val)
  if (!val) {
    // 关闭时清空 options，避免残留
    props.question.options = undefined
  }
}

/** 下拉选项存在 question.options（复用已有字段） */
function parseDropdownOptions(): OptionObj[] {
  if (!props.question.options) return []
  try {
    const p = JSON.parse(props.question.options)
    return Array.isArray(p) ? p : []
  } catch {
    return []
  }
}

function addDropdownOption() {
  const opts = parseDropdownOptions()
  const val = nextOptionValue(opts)
  opts.push({ label: `选项${val}`, value: val })
  props.question.options = JSON.stringify(opts)
}

function updateDropdownOptionLabel(index: number, value: string) {
  const opts = parseDropdownOptions()
  if (opts[index]) {
    opts[index].label = value
    props.question.options = JSON.stringify(opts)
  }
}

function removeDropdownOption(idx: number) {
  const opts = parseDropdownOptions()
  opts.splice(idx, 1)
  props.question.options = opts.length ? JSON.stringify(opts) : undefined
}

/** 批量添加下拉选项 */
const dropdownBatchVisible = ref(false)
const dropdownBatchText = ref("")

function confirmDropdownBatch() {
  const lines = dropdownBatchText.value
    .split("\n")
    .map((l: string) => l.trim())
    .filter((l: string) => l.length > 0)
  if (!lines.length) {
    ElMessage.warning("请至少输入一行选项")
    return
  }
  const opts = parseDropdownOptions()
  for (const label of lines) {
    const val = nextOptionValue(opts)
    opts.push({ label, value: val })
  }
  props.question.options = JSON.stringify(opts)
  dropdownBatchText.value = ""
  dropdownBatchVisible.value = false
  ElMessage.success(`已批量添加 ${lines.length} 个选项`)
}

/** 从疾病编码表导入所有条目 */
function importDiseaseCodeOptions() {
  ElMessageBox.confirm(
    `将从 ICD 疾病编码表中导入 ${DISEASE_CODE_OPTIONS.length} 条疾病名称作为下拉选项，这将覆盖当前已有选项，是否继续？`,
    "导入疾病编码",
    { confirmButtonText: "确认导入", cancelButtonText: "取消", type: "warning" }
  ).then(() => {
    props.question.options = JSON.stringify(
      DISEASE_CODE_OPTIONS.map(item => ({ label: `${item.value} ${item.label}`, value: item.value }))
    )
    ElMessage.success(`已导入 ${DISEASE_CODE_OPTIONS.length} 条疾病编码`)
  }).catch(() => { /* 用户取消 */ })
}

function parseValidation(): Record<string, any> {
  if (!props.question.validationRules) return {}
  try {
    return JSON.parse(props.question.validationRules)
  } catch {
    return {}
  }
}
function getVNum(key: string, fallback: number): number {
  const v = parseValidation()
  return typeof v[key] === "number" ? v[key] : fallback
}
function setV(key: string, val: any) {
  const v = parseValidation()
  if (val === undefined || val === null) delete v[key]
  else v[key] = val
  props.question.validationRules = JSON.stringify(v)
}

/* ============================================================
 * 自动计算（formula）题型专用
 * ============================================================ */

/** 题号映射：questions 数组下标 → 用户可见题号（跳过布局题） */
const questionNumByIndex = computed(() => {
  const map: Record<number, number> = {}
  let num = 0
  props.questions.forEach((q, idx) => {
    if (!layoutTypes.has(q.type)) map[idx] = ++num
  })
  return map
})

/** stableKey → 题号 / 题号 → stableKey 双向映射 */
const stableKeyToNum = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {}
  props.questions.forEach((q, idx) => {
    const num = questionNumByIndex.value[idx]
    if (num) map[getStableKey(q)] = num
  })
  return map
})

const numToStableKey = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const [k, v] of Object.entries(stableKeyToNum.value)) map[v as number] = k
  return map
})

/**
 * 公式编辑器显示值：将底层的 `{stableKey}` 转为用户友好的 `{Q<题号>}`
 * v-model 双向绑定，输入时再转回 stableKey 存储
 */
const formulaDisplay = computed({
  get: () => {
    const raw = parseValidation().formula || ""
    return remapFormulaRefs(raw, (k) => {
      const num = stableKeyToNum.value[k]
      return num ? `Q${num}` : null
    })
  },
  set: (v: string) => {
    const raw = remapFormulaRefs(v, (k) => {
      const m = /^Q(\d+)$/i.exec(k)
      if (!m) return null
      const n = Number(m[1])
      return numToStableKey.value[n] ?? null
    })
    setV("formula", raw)
  }
})

/** 当前可作为公式变量的题目（数值类：数字 / 滑块 / 评分 / NPS / 数字限制的单行文本 / 自动计算；自身除外） */
const formulaSourceQuestions = computed(() => {
  return props.questions
    .map((q, idx) => ({ q, idx, num: questionNumByIndex.value[idx] }))
    .filter(item =>
      item.idx !== props.questionIndex
      && item.num != null
      && isFormulaCompatible(item.q)
    )
})

/** 在公式末尾插入题目变量占位符 */
function insertFormulaRef(targetQ: QuestionItem) {
  const num = stableKeyToNum.value[getStableKey(targetQ)]
  if (!num) return
  const placeholder = `{Q${num}}`
  const cur = formulaDisplay.value
  formulaDisplay.value = cur ? `${cur}${placeholder}` : placeholder
}

function insertFormulaSymbol(sym: string) {
  formulaDisplay.value = (formulaDisplay.value || "") + sym
}

/** 公式静态校验（不带答案，仅检查语法/非法字符） */
const formulaCheck = computed<{ ok: boolean, msg: string }>(() => {
  const raw = (parseValidation().formula || "").trim()
  if (!raw) return { ok: true, msg: "" }
  const refs = extractFormulaRefs(raw)
  // 用 1 替换所有变量做一次静态求值
  const result = evalFormula(raw, () => 1)
  if (result.error) return { ok: false, msg: result.error }
  // 检查是否引用了不存在的题
  const invalidRefs = refs.filter(k => !stableKeyToNum.value[k])
  if (invalidRefs.length) return { ok: false, msg: `存在失效的题目引用，请重新插入` }
  return { ok: true, msg: result.value != null ? `语法正确（用 1 代入示例值 = ${result.value}）` : "" }
})
</script>

<template>
  <el-form label-width="80px" size="small">
    <el-form-item label="题目标题">
      <el-input v-model="question.title" type="textarea" :rows="2" placeholder="请输入题目标题" />
    </el-form-item>
    <el-form-item label="题目说明">
      <el-input v-model="question.description" placeholder="可选" />
    </el-form-item>
    <el-form-item label="题型">
      <el-select v-model="question.type" style="width: 100%">
        <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
    </el-form-item>
    <el-form-item v-if="!nonRequiredTypes.includes(question.type)" label="是否必填">
      <el-switch v-model="question.required" :active-value="1" :inactive-value="0" />
    </el-form-item>

    <!-- 选项列表：radio / checkbox / dropdown / sort -->
    <template v-if="['radio', 'checkbox', 'dropdown', 'sort'].includes(question.type)">
      <el-form-item label="选项">
        <div style="width: 100%">
          <div v-for="(opt, oi) in parseOptions()" :key="oi" style="display: flex; gap: 6px; margin-bottom: 6px; align-items: center">
            <el-input :model-value="opt.label" size="small" placeholder="选项文本" @update:model-value="(v) => updateOptionLabel(oi, String(v))" />
            <!-- radio/checkbox 支持选项后附加填空（适用于"其他，请注明"场景） -->
            <el-tooltip v-if="['radio', 'checkbox'].includes(question.type)" content="选中该选项时显示填空框（适用于其他请注明场景）" placement="top">
              <el-checkbox
                :model-value="opt.hasInput === true"
                size="small"
                style="white-space: nowrap; flex-shrink: 0"
                @change="(v) => updateOptionField(oi, 'hasInput', v || undefined)"
              >
                附加填空
              </el-checkbox>
            </el-tooltip>
            <el-button :icon="Delete" size="small" type="danger" plain @click="removeOption(oi)" />
          </div>
          <div style="display: flex; gap: 6px; margin-top: 2px">
            <el-button size="small" :icon="Plus" @click="addOption">
              添加选项
            </el-button>
            <!-- 批量添加 Popover -->
            <el-popover
              v-model:visible="batchPopoverVisible"
              placement="bottom-start"
              :width="260"
              trigger="click"
              @hide="batchText = ''"
            >
              <template #reference>
                <el-button size="small" :icon="List">
                  批量添加
                </el-button>
              </template>
              <div>
                <div style="font-size: 12px; color: #606266; margin-bottom: 6px">
                  每行一个选项，点击确认后追加到已有选项末尾
                </div>
                <el-input
                  v-model="batchText"
                  type="textarea"
                  :rows="6"
                  placeholder="选项A&#10;选项B&#10;选项C"
                  style="margin-bottom: 8px"
                />
                <div style="display: flex; justify-content: flex-end; gap: 8px">
                  <el-button size="small" @click="batchPopoverVisible = false; batchText = ''">
                    取消
                  </el-button>
                  <el-button size="small" type="primary" @click="confirmBatchAdd">
                    确认添加
                  </el-button>
                </div>
              </div>
            </el-popover>
          </div>
        </div>
      </el-form-item>
    </template>

    <!-- 图片选择 -->
    <template v-if="question.type === 'image_choice'">
      <el-form-item label="选择模式">
        <el-switch :model-value="parseValidation().multiple === true" active-text="多选" inactive-text="单选" @change="(v) => setV('multiple', v)" />
      </el-form-item>
      <el-form-item label="选项">
        <div style="width: 100%">
          <div v-for="(opt, oi) in parseOptions()" :key="oi" style="margin-bottom: 8px; padding: 8px; border: 1px solid var(--el-border-color-lighter); border-radius: 4px">
            <div style="display: flex; gap: 6px; margin-bottom: 6px">
              <el-input :model-value="opt.label" size="small" placeholder="选项文本" @update:model-value="(v) => updateOptionLabel(oi, String(v))" />
              <el-button :icon="Delete" size="small" type="danger" plain @click="removeOption(oi)" />
            </div>
            <ImageUploadInput :model-value="opt.imageUrl ?? ''" @update:model-value="(v) => updateOptionImageUrl(oi, v)" />
          </div>
          <el-button size="small" :icon="Plus" @click="addOption">
            添加选项
          </el-button>
        </div>
      </el-form-item>
    </template>

    <!-- 级联选项 JSON -->
    <template v-if="question.type === 'cascader'">
      <el-form-item label="级联选项">
        <el-input v-model="question.options" type="textarea" :rows="6" placeholder="[{&quot;label&quot;:&quot;一级&quot;,&quot;value&quot;:&quot;1&quot;,&quot;children&quot;:[...]}]" />
      </el-form-item>
    </template>

    <!-- 字段列表：multi_input / inline_input / dynamic_table -->
    <template v-if="['multi_input', 'inline_input', 'dynamic_table'].includes(question.type)">
      <el-form-item :label="question.type === 'inline_input' ? '文字段' : '字段'">
        <div style="width: 100%">
          <template v-for="(opt, oi) in parseOptions()" :key="oi">
            <!-- dynamic_table 列：展示字段名 + 内容限制配置 -->
            <div
              v-if="question.type === 'dynamic_table'"
              style="margin-bottom: 10px; padding: 8px; border: 1px solid var(--el-border-color-lighter); border-radius: 6px"
            >
              <div style="display: flex; gap: 6px; margin-bottom: 6px; align-items: center">
                <span style="font-size: 12px; color: #909399; white-space: nowrap">字段名</span>
                <el-input :model-value="opt.label" size="small" placeholder="字段名" @update:model-value="(v) => updateOptionLabel(oi, String(v))" />
                <el-button :icon="Delete" size="small" type="danger" plain @click="removeOption(oi)" />
              </div>
              <div style="display: flex; gap: 6px; align-items: center; margin-bottom: 4px">
                <span style="font-size: 12px; color: #909399; white-space: nowrap; min-width: 48px">内容限制</span>
                <el-select
                  :model-value="opt.contentLimit || 'none'"
                  size="small"
                  style="flex: 1"
                  @change="(v) => onOptionContentLimitChange(oi, v)"
                >
                  <el-option v-for="cl in CONTENT_LIMIT_OPTIONS" :key="cl.value" :label="cl.label" :value="cl.value" />
                </el-select>
              </div>
              <!-- 数字类型的范围 -->
              <template v-if="['number', 'integer', 'decimal'].includes(opt.contentLimit || '')">
                <div class="range-limit-config">
                  <div class="range-limit-item">
                    <span class="range-limit-label">最小值</span>
                    <el-input-number
                      :model-value="opt.rangeMin"
                      size="small"
                      controls-position="right"
                      class="range-limit-input"
                      placeholder="不限"
                      :precision="opt.contentLimit === 'integer' ? 0 : (opt.decimalPlaces ?? 6)"
                      @update:model-value="(v) => updateOptionField(oi, 'rangeMin', v != null ? v : undefined)"
                    />
                  </div>
                  <div class="range-limit-item">
                    <span class="range-limit-label">最大值</span>
                    <el-input-number
                      :model-value="opt.rangeMax"
                      size="small"
                      controls-position="right"
                      class="range-limit-input"
                      placeholder="不限"
                      :precision="opt.contentLimit === 'integer' ? 0 : (opt.decimalPlaces ?? 6)"
                      @update:model-value="(v) => updateOptionField(oi, 'rangeMax', v != null ? v : undefined)"
                    />
                  </div>
                  <div v-if="['number', 'decimal'].includes(opt.contentLimit || '')" class="range-limit-item">
                    <span class="range-limit-label">小数位</span>
                    <el-input-number
                      :model-value="opt.decimalPlaces ?? 2"
                      size="small"
                      controls-position="right"
                      class="range-limit-input"
                      :min="0"
                      :max="6"
                      @update:model-value="(v) => updateOptionField(oi, 'decimalPlaces', v != null ? v : undefined)"
                    />
                  </div>
                </div>
              </template>
              <!-- 下拉单选：设置选项 -->
              <template v-if="opt.contentLimit === 'dropdown_select'">
                <div style="display: flex; gap: 6px; align-items: center; margin-top: 4px">
                  <span style="font-size: 12px; color: #909399; white-space: nowrap; min-width: 48px">下拉设置</span>
                  <el-button size="small" link type="primary" @click="openDropdownSelectDialog('col', oi)">
                    点击设置（{{ (opt.dropdownOptions || []).length }} 个选项）
                  </el-button>
                </div>
              </template>
            </div>
            <!-- inline_input：文字段 + 纯文字（后缀）切换 -->
            <div v-else-if="question.type === 'inline_input'" style="display: flex; gap: 6px; margin-bottom: 6px; align-items: center">
              <el-input
                :model-value="opt.label"
                size="small"
                :placeholder="opt.inputHidden ? '后缀文字，如：天、小时' : '文字内容'"
                @update:model-value="(v) => updateOptionLabel(oi, String(v))"
              />
              <el-tooltip content="勾选后此项仅显示文字，不含输入框（可用作后缀）" placement="top">
                <el-checkbox
                  :model-value="opt.inputHidden === true"
                  size="small"
                  style="white-space: nowrap; flex-shrink: 0"
                  @change="(v) => updateOptionField(oi, 'inputHidden', v || undefined)"
                >
                  纯文字
                </el-checkbox>
              </el-tooltip>
              <el-button :icon="Delete" size="small" type="danger" plain @click="removeOption(oi)" />
            </div>
            <!-- multi_input：只显示字段名 -->
            <div v-else style="display: flex; gap: 6px; margin-bottom: 6px">
              <el-input :model-value="opt.label" size="small" placeholder="字段名" @update:model-value="(v) => updateOptionLabel(oi, String(v))" />
              <el-button :icon="Delete" size="small" type="danger" plain @click="removeOption(oi)" />
            </div>
          </template>
          <el-button size="small" :icon="Plus" @click="addOption">
            添加字段
          </el-button>
        </div>
      </el-form-item>
    </template>

    <!-- 多选题 - 可选数量限制 -->
    <template v-if="question.type === 'checkbox'">
      <el-form-item label="最少可选">
        <el-input-number
          :model-value="parseValidation().minSelect ?? undefined"
          :min="1"
          :max="parseValidation().maxSelect || 999"
          placeholder="不限"
          controls-position="right"
          style="width: 140px"
          @update:model-value="(v) => setV('minSelect', v != null ? v : undefined)"
        />
        <span style="font-size: 12px; color: #909399; margin-left: 8px">项（不填则不限）</span>
      </el-form-item>
      <el-form-item label="最多可选">
        <el-input-number
          :model-value="parseValidation().maxSelect ?? undefined"
          :min="parseValidation().minSelect || 1"
          placeholder="不限"
          controls-position="right"
          style="width: 140px"
          @update:model-value="(v) => setV('maxSelect', v != null ? v : undefined)"
        />
        <span style="font-size: 12px; color: #909399; margin-left: 8px">项（不填则不限）</span>
      </el-form-item>
    </template>

    <!-- 单行文本 - 内容限制 -->
    <template v-if="question.type === 'input'">
      <el-form-item label="内容限制">
        <el-select
          :model-value="parseValidation().contentLimit || 'none'"
          style="width: 100%"
          @change="(v) => {
            setV('contentLimit', v === 'none' ? undefined : v); if (v === 'number' || v === 'decimal') { if (parseValidation().decimalPlaces == null) setV('decimalPlaces', 2) }
            else { setV('decimalPlaces', undefined) }; if (v !== 'none' && v !== 'dropdown_select') { setV('dropdownEnabled', undefined); setV('dropdownSelectOptions', undefined); question.options = undefined }
          }"
        >
          <el-option v-for="cl in CONTENT_LIMIT_OPTIONS" :key="cl.value" :label="cl.label" :value="cl.value" />
        </el-select>
      </el-form-item>
      <!-- 下拉单选：设置选项 -->
      <el-form-item v-if="parseValidation().contentLimit === 'dropdown_select'" label="下拉设置">
        <el-button size="small" link type="primary" @click="openDropdownSelectDialog('input')">
          点击设置（{{ getInputDropdownSelectOptions().length }} 个选项）
        </el-button>
      </el-form-item>
      <!-- 数字类型的范围限制 -->
      <template v-if="['number', 'integer', 'decimal'].includes(parseValidation().contentLimit || '')">
        <el-form-item label="范围强校验">
          <el-tooltip content="开启后提交时强制校验数值范围" placement="top">
            <el-switch :model-value="parseValidation().rangeStrict === true" @change="(v) => setV('rangeStrict', v || undefined)" />
          </el-tooltip>
        </el-form-item>
        <el-form-item label="最小值">
          <el-input-number
            :model-value="parseValidation().rangeMin"
            placeholder="不限"
            controls-position="right"
            style="width: 100%"
            :precision="parseValidation().contentLimit === 'integer' ? 0 : (parseValidation().decimalPlaces ?? 6)"
            @update:model-value="(v) => setV('rangeMin', v != null ? v : undefined)"
          />
        </el-form-item>
        <el-form-item label="最大值">
          <el-input-number
            :model-value="parseValidation().rangeMax"
            placeholder="不限"
            controls-position="right"
            style="width: 100%"
            :precision="parseValidation().contentLimit === 'integer' ? 0 : (parseValidation().decimalPlaces ?? 6)"
            @update:model-value="(v) => setV('rangeMax', v != null ? v : undefined)"
          />
        </el-form-item>
        <el-form-item v-if="['number', 'decimal'].includes(parseValidation().contentLimit || '')" label="小数位数">
          <el-input-number
            :model-value="parseValidation().decimalPlaces ?? 2"
            :min="0"
            :max="6"
            controls-position="right"
            style="width: 140px"
            @update:model-value="(v) => setV('decimalPlaces', v != null ? v : undefined)"
          />
          <span style="font-size: 12px; color: #909399; margin-left: 8px">填写时限制小数位数（0～6）</span>
        </el-form-item>
      </template>
    </template>

    <!-- 单行文本 / 多行文本 - 可选下拉选项 -->
    <template v-if="['input', 'textarea'].includes(question.type)">
      <!-- input 类型：仅在无内容限制时才显示下拉选项 -->
      <template v-if="question.type === 'textarea' || !parseValidation().contentLimit">
        <el-form-item label="下拉选项">
          <el-switch
            :model-value="dropdownEnabled"
            active-text="启用"
            inactive-text="关闭"
            @change="(v) => toggleDropdown(Boolean(v))"
          />
        </el-form-item>
        <template v-if="dropdownEnabled">
          <el-form-item label="选项列表">
            <div style="width: 100%">
              <div
                v-for="(opt, oi) in parseDropdownOptions()"
                :key="oi"
                style="display: flex; gap: 6px; margin-bottom: 6px"
              >
                <el-input
                  :model-value="opt.label"
                  size="small"
                  placeholder="选项文本"
                  @update:model-value="(v) => updateDropdownOptionLabel(oi, String(v))"
                />
                <el-button :icon="Delete" size="small" type="danger" plain @click="removeDropdownOption(oi)" />
              </div>
              <div style="display: flex; gap: 6px; flex-wrap: wrap; margin-top: 2px">
                <el-button size="small" :icon="Plus" @click="addDropdownOption">
                  添加选项
                </el-button>
                <!-- 批量添加 -->
                <el-popover
                  v-model:visible="dropdownBatchVisible"
                  placement="bottom-start"
                  :width="260"
                  trigger="click"
                  @hide="dropdownBatchText = ''"
                >
                  <template #reference>
                    <el-button size="small" :icon="List">
                      批量添加
                    </el-button>
                  </template>
                  <div>
                    <div style="font-size: 12px; color: #606266; margin-bottom: 6px">
                      每行一个选项，追加到末尾
                    </div>
                    <el-input
                      v-model="dropdownBatchText"
                      type="textarea"
                      :rows="6"
                      placeholder="选项A&#10;选项B&#10;选项C"
                      style="margin-bottom: 8px"
                    />
                    <div style="display: flex; justify-content: flex-end; gap: 8px">
                      <el-button size="small" @click="dropdownBatchVisible = false; dropdownBatchText = ''">
                        取消
                      </el-button>
                      <el-button size="small" type="primary" @click="confirmDropdownBatch">
                        确认添加
                      </el-button>
                    </div>
                  </div>
                </el-popover>
                <!-- 从疾病编码表导入 -->
                <el-button size="small" type="success" plain @click="importDiseaseCodeOptions">
                  导入疾病编码表
                </el-button>
              </div>
              <div style="font-size: 11px; color: #909399; margin-top: 6px">
                共 {{ parseDropdownOptions().length }} 个选项，填写时支持搜索
              </div>
            </div>
          </el-form-item>
        </template>
      </template><!-- /input 无内容限制时才显示下拉 -->
    </template>

    <!-- 评分 -->
    <template v-if="question.type === 'rating'">
      <el-form-item label="最高分">
        <el-input-number :model-value="getVNum('max', 5)" :min="1" :max="10" @change="(v) => setV('max', v)" />
      </el-form-item>
    </template>

    <!-- NPS -->
    <template v-if="question.type === 'nps'">
      <el-form-item label="最低分">
        <el-input-number :model-value="getVNum('min', 0)" :min="0" :max="5" @change="(v) => setV('min', v)" />
      </el-form-item>
      <el-form-item label="最高分">
        <el-input-number :model-value="getVNum('max', 10)" :min="5" :max="10" @change="(v) => setV('max', v)" />
      </el-form-item>
    </template>

    <!-- 数字组件 -->
    <template v-if="question.type === 'number'">
      <el-form-item label="最小值">
        <el-input-number :model-value="getVNum('min', 0)" @change="(v) => setV('min', v)" />
      </el-form-item>
      <el-form-item label="最大值">
        <el-input-number :model-value="getVNum('max', 100)" @change="(v) => setV('max', v)" />
      </el-form-item>
      <el-form-item label="步长">
        <el-input-number :model-value="getVNum('step', 1)" :min="0.01" @change="(v) => setV('step', v)" />
      </el-form-item>
      <el-form-item label="小数位">
        <el-input-number :model-value="getVNum('precision', 0)" :min="0" :max="4" @change="(v) => setV('precision', v)" />
      </el-form-item>
    </template>

    <!-- 日期时间 -->
    <template v-if="question.type === 'date'">
      <el-form-item label="日期类型">
        <el-select :model-value="parseValidation().dateType || 'date'" style="width: 100%" @change="(v) => setV('dateType', v)">
          <el-option label="日期" value="date" />
          <el-option label="日期时间" value="datetime" />
          <el-option label="日期范围" value="daterange" />
        </el-select>
      </el-form-item>
    </template>

    <!-- 滑块 -->
    <template v-if="question.type === 'slider'">
      <el-form-item label="最小值">
        <el-input-number :model-value="getVNum('min', 0)" @change="(v) => setV('min', v)" />
      </el-form-item>
      <el-form-item label="最大值">
        <el-input-number :model-value="getVNum('max', 100)" @change="(v) => setV('max', v)" />
      </el-form-item>
      <el-form-item label="步长">
        <el-input-number :model-value="getVNum('step', 1)" :min="1" @change="(v) => setV('step', v)" />
      </el-form-item>
      <el-form-item label="显示输入框">
        <el-switch :model-value="parseValidation().showInput === true" @change="(v) => setV('showInput', v)" />
      </el-form-item>
    </template>

    <!-- 矩阵：matrix_radio / matrix_checkbox / matrix_input -->
    <template v-if="['matrix_radio', 'matrix_checkbox', 'matrix_input'].includes(question.type)">
      <el-form-item label="行标签">
        <div style="width: 100%">
          <div v-for="(r, ri) in parseMatrixOpts().rows" :key="`r${ri}`" style="display: flex; gap: 6px; margin-bottom: 6px">
            <el-input :model-value="r.label" size="small" placeholder="行名" @update:model-value="(v) => updateMatrixRowLabel(ri, String(v))" />
            <el-button :icon="Delete" size="small" type="danger" plain @click="removeMatrixRow(ri)" />
          </div>
          <el-button size="small" :icon="Plus" @click="addMatrixRow">
            添加行
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="列标签">
        <div style="width: 100%">
          <template v-for="(c, ci) in parseMatrixOpts().cols" :key="`c${ci}`">
            <!-- matrix_input：列配置卡片，支持内容限制 -->
            <div
              v-if="question.type === 'matrix_input'"
              style="margin-bottom: 10px; padding: 8px; border: 1px solid var(--el-border-color-lighter); border-radius: 6px"
            >
              <div style="display: flex; gap: 6px; margin-bottom: 6px; align-items: center">
                <span style="font-size: 12px; color: #909399; white-space: nowrap">列名</span>
                <el-input :model-value="c.label" size="small" placeholder="列名" @update:model-value="(v) => updateMatrixColLabel(ci, String(v))" />
                <el-button :icon="Delete" size="small" type="danger" plain @click="removeMatrixCol(ci)" />
              </div>
              <div style="display: flex; gap: 6px; align-items: center; margin-bottom: 4px">
                <span style="font-size: 12px; color: #909399; white-space: nowrap; min-width: 48px">内容限制</span>
                <el-select
                  :model-value="c.contentLimit || 'none'"
                  size="small"
                  style="flex: 1"
                  @change="(v) => onMatrixColContentLimitChange(ci, v)"
                >
                  <el-option v-for="cl in CONTENT_LIMIT_OPTIONS" :key="cl.value" :label="cl.label" :value="cl.value" />
                </el-select>
              </div>
              <!-- 数字类型的范围 -->
              <template v-if="['number', 'integer', 'decimal'].includes(c.contentLimit || '')">
                <div class="range-limit-config">
                  <div class="range-limit-item">
                    <span class="range-limit-label">最小值</span>
                    <el-input-number
                      :model-value="c.rangeMin"
                      size="small"
                      controls-position="right"
                      class="range-limit-input"
                      placeholder="不限"
                      :precision="c.contentLimit === 'integer' ? 0 : (c.decimalPlaces ?? 6)"
                      @update:model-value="(v) => updateMatrixColField(ci, 'rangeMin', v != null ? v : undefined)"
                    />
                  </div>
                  <div class="range-limit-item">
                    <span class="range-limit-label">最大值</span>
                    <el-input-number
                      :model-value="c.rangeMax"
                      size="small"
                      controls-position="right"
                      class="range-limit-input"
                      placeholder="不限"
                      :precision="c.contentLimit === 'integer' ? 0 : (c.decimalPlaces ?? 6)"
                      @update:model-value="(v) => updateMatrixColField(ci, 'rangeMax', v != null ? v : undefined)"
                    />
                  </div>
                  <div v-if="['number', 'decimal'].includes(c.contentLimit || '')" class="range-limit-item">
                    <span class="range-limit-label">小数位</span>
                    <el-input-number
                      :model-value="c.decimalPlaces ?? 2"
                      size="small"
                      controls-position="right"
                      class="range-limit-input"
                      :min="0"
                      :max="6"
                      @update:model-value="(v) => updateMatrixColField(ci, 'decimalPlaces', v != null ? v : undefined)"
                    />
                  </div>
                </div>
              </template>
              <!-- 下拉单选：设置选项 -->
              <template v-if="c.contentLimit === 'dropdown_select'">
                <div style="display: flex; gap: 6px; align-items: center; margin-top: 4px">
                  <span style="font-size: 12px; color: #909399; white-space: nowrap; min-width: 48px">下拉设置</span>
                  <el-button size="small" link type="primary" @click="openDropdownSelectDialog('matrixCol', ci)">
                    点击设置（{{ (c.dropdownOptions || []).length }} 个选项）
                  </el-button>
                </div>
              </template>
            </div>
            <!-- matrix_radio / matrix_checkbox：只显示列名 -->
            <div v-else style="display: flex; gap: 6px; margin-bottom: 6px">
              <el-input :model-value="c.label" size="small" placeholder="列名" @update:model-value="(v) => updateMatrixColLabel(ci, String(v))" />
              <el-button :icon="Delete" size="small" type="danger" plain @click="removeMatrixCol(ci)" />
            </div>
          </template>
          <el-button size="small" :icon="Plus" @click="addMatrixCol">
            添加列
          </el-button>
        </div>
      </el-form-item>
    </template>

    <!-- 复合矩阵 matrix_complex -->
    <template v-if="question.type === 'matrix_complex'">
      <el-alert
        type="info"
        :closable="false"
        title="每行一个项目，每行包含多列；列与列之间支持显示依赖（行内逻辑跳转）"
        style="margin-bottom: 8px"
      />
      <el-form-item label="行项目">
        <div style="width: 100%">
          <div v-for="(r, ri) in parseMCOpts().rows" :key="`mcr${ri}`" style="display: flex; gap: 6px; margin-bottom: 6px">
            <el-input
              :model-value="r.value"
              size="small"
              style="width: 90px"
              placeholder="编号"
              @update:model-value="(v) => mcUpdateRow(ri, 'value', String(v))"
            />
            <el-input
              :model-value="r.label"
              size="small"
              placeholder="行名（如：粮谷类）"
              @update:model-value="(v) => mcUpdateRow(ri, 'label', String(v))"
            />
            <el-button :icon="Delete" size="small" type="danger" plain @click="mcRemoveRow(ri)" />
          </div>
          <el-button size="small" :icon="Plus" @click="mcAddRow">
            添加行
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="列定义">
        <div style="width: 100%">
          <div
            v-for="(col, ci) in parseMCOpts().columns"
            :key="`mcc${ci}`"
            class="mc-col-card"
          >
            <!-- 列基础信息 -->
            <div style="display: flex; gap: 6px; align-items: center; margin-bottom: 6px">
              <el-input
                :model-value="col.key"
                size="small"
                style="width: 80px"
                placeholder="key"
                @update:model-value="(v) => mcUpdateCol(ci, 'key', String(v))"
              />
              <el-input
                :model-value="col.label"
                size="small"
                placeholder="列标题"
                @update:model-value="(v) => mcUpdateCol(ci, 'label', String(v))"
              />
              <el-button :icon="Delete" size="small" type="danger" plain @click="mcRemoveColumn(ci)" />
            </div>

            <div style="display: flex; gap: 6px; align-items: center; margin-bottom: 6px">
              <span style="font-size: 12px; color: #909399; min-width: 48px">类型</span>
              <el-select
                :model-value="col.type"
                size="small"
                style="flex: 1"
                @change="(v) => mcChangeColType(ci, v)"
              >
                <el-option label="单选" value="radio" />
                <el-option label="多选" value="checkbox" />
                <el-option label="填空" value="input" />
                <el-option label="频率（单位互斥+数字）" value="freq" />
              </el-select>
            </div>

            <!-- radio/checkbox：选项编辑 -->
            <template v-if="col.type === 'radio' || col.type === 'checkbox'">
              <div style="font-size: 12px; color: #909399; margin: 4px 0">
                选项
              </div>
              <div v-for="(opt, oi) in (col.options || [])" :key="`co${oi}`" style="display: flex; gap: 4px; margin-bottom: 4px">
                <el-input
                  :model-value="opt.value"
                  size="small"
                  style="width: 70px"
                  placeholder="值"
                  @update:model-value="(v) => mcUpdateColOption(ci, oi, 'value', String(v))"
                />
                <el-input
                  :model-value="opt.label"
                  size="small"
                  placeholder="文本"
                  @update:model-value="(v) => mcUpdateColOption(ci, oi, 'label', String(v))"
                />
                <el-button :icon="Delete" size="small" type="danger" plain @click="mcRemoveColOption(ci, oi)" />
              </div>
              <el-button size="small" :icon="Plus" @click="mcAddColOption(ci)">
                添加选项
              </el-button>
            </template>

            <!-- freq：单位编辑 -->
            <template v-if="col.type === 'freq'">
              <div style="font-size: 12px; color: #909399; margin: 4px 0">
                频率单位（互斥单选 + 数字填空）
              </div>
              <div v-for="(u, ui) in (col.units || [])" :key="`cu${ui}`" style="display: flex; gap: 4px; margin-bottom: 4px">
                <el-input
                  :model-value="u.value"
                  size="small"
                  style="width: 70px"
                  placeholder="值"
                  @update:model-value="(v) => mcUpdateColUnit(ci, ui, 'value', String(v))"
                />
                <el-input
                  :model-value="u.label"
                  size="small"
                  placeholder="如：次/天"
                  @update:model-value="(v) => mcUpdateColUnit(ci, ui, 'label', String(v))"
                />
                <el-button :icon="Delete" size="small" type="danger" plain @click="mcRemoveColUnit(ci, ui)" />
              </div>
              <el-button size="small" :icon="Plus" @click="mcAddColUnit(ci)">
                添加单位
              </el-button>
            </template>

            <!-- input：输入类型 + 后缀 -->
            <template v-if="col.type === 'input'">
              <div style="display: flex; gap: 6px; align-items: center; margin-bottom: 4px">
                <span style="font-size: 12px; color: #909399; min-width: 48px">内容</span>
                <el-select
                  :model-value="col.inputType || 'text'"
                  size="small"
                  style="flex: 1"
                  @change="(v) => mcUpdateCol(ci, 'inputType', v)"
                >
                  <el-option label="文本" value="text" />
                  <el-option label="数字" value="number" />
                  <el-option label="整数" value="integer" />
                  <el-option label="小数" value="decimal" />
                  <el-option label="日期" value="date" />
                </el-select>
              </div>
              <div style="display: flex; gap: 6px; align-items: center">
                <span style="font-size: 12px; color: #909399; min-width: 48px">后缀</span>
                <el-input
                  :model-value="col.suffix || ''"
                  size="small"
                  placeholder="如：两、克、毫升"
                  @update:model-value="(v) => mcUpdateCol(ci, 'suffix', String(v))"
                />
              </div>
            </template>

            <!-- 显示依赖 -->
            <el-divider style="margin: 8px 0" />
            <div style="display: flex; gap: 6px; align-items: center">
              <el-checkbox
                :model-value="!!col.showWhen"
                size="small"
                @change="(v) => mcToggleShowWhen(ci, Boolean(v))"
              >
                条件显示
              </el-checkbox>
            </div>
            <template v-if="col.showWhen">
              <div style="display: flex; gap: 4px; margin-top: 4px; flex-wrap: wrap">
                <span style="font-size: 12px; color: #909399; line-height: 24px">当</span>
                <el-select
                  :model-value="col.showWhen.column"
                  size="small"
                  style="width: 90px"
                  placeholder="依赖列"
                  @change="(v) => mcUpdateShowWhen(ci, 'column', v)"
                >
                  <el-option v-for="oc in mcOtherColumns(ci)" :key="oc.key" :label="oc.label || oc.key" :value="oc.key" />
                </el-select>
                <el-select
                  :model-value="col.showWhen.op"
                  size="small"
                  style="width: 90px"
                  @change="(v) => mcUpdateShowWhen(ci, 'op', v)"
                >
                  <el-option label="等于" value="eq" />
                  <el-option label="不等于" value="neq" />
                  <el-option label="包含" value="selected" />
                  <el-option label="非空" value="not_empty" />
                </el-select>
                <el-select
                  v-if="col.showWhen.op !== 'not_empty' && mcShowWhenValueOptions(ci).length"
                  :model-value="col.showWhen.value"
                  size="small"
                  style="flex: 1; min-width: 90px"
                  placeholder="选值"
                  @change="(v) => mcUpdateShowWhen(ci, 'value', v)"
                >
                  <el-option v-for="vo in mcShowWhenValueOptions(ci)" :key="vo.value" :label="vo.label" :value="vo.value" />
                </el-select>
                <el-input
                  v-else-if="col.showWhen.op !== 'not_empty'"
                  :model-value="col.showWhen.value"
                  size="small"
                  placeholder="值"
                  style="flex: 1; min-width: 90px"
                  @update:model-value="(v) => mcUpdateShowWhen(ci, 'value', String(v))"
                />
              </div>
            </template>
          </div>
          <el-button size="small" :icon="Plus" @click="mcAddColumn">
            添加列
          </el-button>
        </div>
      </el-form-item>
    </template>

    <!-- 矩阵量表 -->
    <template v-if="question.type === 'matrix_scale'">
      <el-form-item label="行标签">
        <div style="width: 100%">
          <div v-for="(r, ri) in parseScaleOpts().rows" :key="`sr${ri}`" style="display: flex; gap: 6px; margin-bottom: 6px">
            <el-input :model-value="r.label" size="small" placeholder="行名" @update:model-value="(v) => updateScaleRowLabel(ri, String(v))" />
            <el-button :icon="Delete" size="small" type="danger" plain @click="removeScaleRow(ri)" />
          </div>
          <el-button size="small" :icon="Plus" @click="addScaleRow">
            添加行
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="量表最小值">
        <el-input-number :model-value="parseScaleOpts().scaleMin" :min="0" @change="(v) => updateScaleField('scaleMin', v)" />
      </el-form-item>
      <el-form-item label="量表最大值">
        <el-input-number :model-value="parseScaleOpts().scaleMax" :min="1" :max="10" @change="(v) => updateScaleField('scaleMax', v)" />
      </el-form-item>
      <el-form-item label="列标签">
        <div style="width: 100%">
          <div style="font-size: 12px; color: #909399; margin-bottom: 6px">
            可为每列设置自定义显示名称，留空则显示数字
          </div>
          <div v-for="n in scaleRange()" :key="n" style="display: flex; gap: 6px; margin-bottom: 6px; align-items: center">
            <span style="font-size: 12px; color: #606266; min-width: 28px; text-align: right">{{ n }}</span>
            <el-input
              :model-value="parseScaleOpts().scaleLabels[String(n)] || ''"
              size="small"
              :placeholder="`列 ${n} 的显示名称`"
              @update:model-value="(v) => updateScaleLabel(n, String(v))"
            />
          </div>
        </div>
      </el-form-item>
    </template>

    <!-- 文件上传 -->
    <template v-if="question.type === 'file_upload'">
      <el-form-item label="文件类型">
        <el-input :model-value="parseValidation().accept || ''" placeholder="如 .pdf,.doc,.xlsx 留空不限" @update:model-value="(v) => setV('accept', v)" />
      </el-form-item>
      <el-form-item label="大小限制">
        <div style="display: flex; align-items: center; gap: 8px">
          <el-input-number :model-value="getVNum('maxSize', 10)" :min="1" :max="100" @change="(v) => setV('maxSize', v)" />
          <span style="color: #909399; font-size: 12px">MB</span>
        </div>
      </el-form-item>
      <el-form-item label="最大数量">
        <el-input-number :model-value="getVNum('maxCount', 1)" :min="1" :max="10" @change="(v) => setV('maxCount', v)" />
      </el-form-item>
    </template>

    <!-- 图片上传 -->
    <template v-if="question.type === 'image_upload'">
      <el-form-item label="大小限制">
        <div style="display: flex; align-items: center; gap: 8px">
          <el-input-number :model-value="getVNum('maxSize', 5)" :min="1" :max="50" @change="(v) => setV('maxSize', v)" />
          <span style="color: #909399; font-size: 12px">MB</span>
        </div>
      </el-form-item>
      <el-form-item label="最大数量">
        <el-input-number :model-value="getVNum('maxCount', 3)" :min="1" :max="9" @change="(v) => setV('maxCount', v)" />
      </el-form-item>
    </template>

    <!-- 图片展示 -->
    <template v-if="question.type === 'image_display'">
      <el-form-item label="图片">
        <ImageUploadInput :model-value="parseImageDisplayOpts().imageUrl" @update:model-value="(v) => updateImageDisplay('imageUrl', v)" />
      </el-form-item>
      <el-form-item label="替代文字">
        <el-input :model-value="parseImageDisplayOpts().alt" placeholder="图片说明（可选）" @update:model-value="(v) => updateImageDisplay('alt', String(v))" />
      </el-form-item>
    </template>

    <!-- 电子签名 -->
    <template v-if="question.type === 'signature'">
      <el-form-item label="提示文字">
        <el-input :model-value="parseValidation().hint || ''" placeholder="请在此处手写签名" @update:model-value="(v) => setV('hint', v)" />
      </el-form-item>
      <el-form-item label="笔触颜色">
        <div style="display: flex; align-items: center; gap: 8px">
          <el-color-picker :model-value="parseValidation().penColor || '#222222'" @update:model-value="(v) => setV('penColor', v)" />
          <span style="font-size: 12px; color: #909399">{{ parseValidation().penColor || '#222222' }}</span>
        </div>
      </el-form-item>
      <el-form-item label="笔触粗细">
        <el-input-number :model-value="getVNum('penWidth', 2)" :min="1" :max="8" @change="(v) => setV('penWidth', v)" />
      </el-form-item>
      <el-form-item label="画板高度">
        <div style="display: flex; align-items: center; gap: 8px">
          <el-input-number :model-value="getVNum('height', 160)" :min="80" :max="400" :step="20" @change="(v) => setV('height', v)" />
          <span style="font-size: 12px; color: #909399">px</span>
        </div>
      </el-form-item>
    </template>

    <!-- 自动计算 -->
    <template v-if="question.type === 'formula'">
      <el-alert
        type="info"
        :closable="false"
        title="根据其他题的答案按公式自动计算结果，无需填写者输入"
        style="margin-bottom: 8px"
      />

      <el-form-item label="公式">
        <div style="width: 100%">
          <el-input
            v-model="formulaDisplay"
            type="textarea"
            :rows="3"
            placeholder="例：{Q1}*100/30/{Q2}"
            spellcheck="false"
            class="formula-input"
          />
          <div class="formula-tools">
            <span class="formula-tools-label">运算符：</span>
            <el-button-group size="small">
              <el-button @click="insertFormulaSymbol('+')">
                +
              </el-button>
              <el-button @click="insertFormulaSymbol('-')">
                −
              </el-button>
              <el-button @click="insertFormulaSymbol('*')">
                ×
              </el-button>
              <el-button @click="insertFormulaSymbol('/')">
                ÷
              </el-button>
              <el-button @click="insertFormulaSymbol('(')">
                (
              </el-button>
              <el-button @click="insertFormulaSymbol(')')">
                )
              </el-button>
            </el-button-group>
          </div>
          <div v-if="formulaCheck.msg" class="formula-check" :class="formulaCheck.ok ? 'ok' : 'err'">
            {{ formulaCheck.msg }}
          </div>
          <div class="formula-hint">
            语法：<code>{Q1}*100/(30*{Q2})</code>。变量 <code>{Q数字}</code> 表示对应题号的答案，请通过下方按钮插入；运算符仅支持 + − × ÷ ( ) %
          </div>
        </div>
      </el-form-item>

      <el-form-item label="变量插入">
        <div style="width: 100%">
          <div v-if="!formulaSourceQuestions.length" class="formula-empty">
            暂无可引用题目。仅以下题型可作为公式变量：数字 / 滑块 / 评分 / NPS / 自动计算 / 单行文本（内容限制为数字、整数或小数）
          </div>
          <div v-else class="formula-vars">
            <el-tag
              v-for="item in formulaSourceQuestions"
              :key="item.idx"
              type="primary"
              effect="plain"
              size="default"
              class="formula-var-tag"
              @click="insertFormulaRef(item.q)"
            >
              <span class="formula-var-num">Q{{ item.num }}</span>
              <span class="formula-var-title">{{ item.q.title || "(未设置)" }}</span>
              <el-icon size="12">
                <Plus />
              </el-icon>
            </el-tag>
          </div>
          <div class="formula-hint">
            点击题目标签将自动插入到公式末尾
          </div>
        </div>
      </el-form-item>

      <el-form-item label="小数位数">
        <el-input-number :model-value="getVNum('precision', 2)" :min="0" :max="6" @change="(v) => setV('precision', v)" />
        <span style="font-size: 12px; color: #909399; margin-left: 8px">结果保留几位小数</span>
      </el-form-item>

      <el-form-item label="单位">
        <el-input
          :model-value="parseValidation().unit || ''"
          placeholder="可选，例：克 / 元 / %"
          style="width: 200px"
          @update:model-value="(v) => setV('unit', v ? String(v) : undefined)"
        />
      </el-form-item>

      <el-divider content-position="left" style="margin: 12px 0 8px">
        范围警告（可选）
      </el-divider>

      <el-form-item label="提示下限">
        <el-input-number
          :model-value="parseValidation().warningMin"
          placeholder="不限"
          controls-position="right"
          @change="(v) => setV('warningMin', v != null ? v : undefined)"
        />
        <el-input
          :model-value="parseValidation().warningMinText || ''"
          placeholder="低于下限时的提示文字"
          style="margin-left: 8px; flex: 1"
          @update:model-value="(v) => setV('warningMinText', v ? String(v) : undefined)"
        />
      </el-form-item>

      <el-form-item label="提示上限">
        <el-input-number
          :model-value="parseValidation().warningMax"
          placeholder="不限"
          controls-position="right"
          @change="(v) => setV('warningMax', v != null ? v : undefined)"
        />
        <el-input
          :model-value="parseValidation().warningMaxText || ''"
          placeholder="超过上限时的提示文字"
          style="margin-left: 8px; flex: 1"
          @update:model-value="(v) => setV('warningMaxText', v ? String(v) : undefined)"
        />
      </el-form-item>
    </template>

    <!-- 文字描述 -->
    <template v-if="question.type === 'description'">
      <el-form-item label="描述内容">
        <el-input v-model="question.description" type="textarea" :rows="4" placeholder="输入要展示的说明文字" />
      </el-form-item>
    </template>

    <!-- 页码 -->
    <el-form-item v-if="question.type !== 'page_break'" label="所在页码">
      <el-input-number v-model="question.pageNumber" :min="1" :max="20" />
    </el-form-item>
  </el-form>

  <!-- 下拉单选选项编辑弹窗 -->
  <el-dialog
    v-model="dropdownSelectDialog.visible"
    title="选项编辑"
    width="420px"
    append-to-body
    destroy-on-close
  >
    <div style="max-height: 360px; overflow-y: auto; padding-right: 4px">
      <div
        v-for="(item, idx) in dropdownSelectDialog.items"
        :key="idx"
        style="display: flex; gap: 6px; margin-bottom: 8px; align-items: center"
      >
        <el-input
          v-model="dropdownSelectDialog.items[idx]"
          size="small"
          :placeholder="`选项 ${idx + 1}`"
        />
        <el-button :icon="Delete" size="small" type="danger" plain @click="removeDropdownSelectItem(idx)" />
      </div>
      <div style="margin-top: 4px; display: flex; gap: 8px; align-items: center">
        <el-button size="small" :icon="Plus" @click="addDropdownSelectItem">
          添加选项
        </el-button>
        <!-- 批量粘贴 -->
        <el-popover v-model:visible="dsDialogBatchVisible" placement="top" :width="260" trigger="click">
          <template #reference>
            <el-button size="small" link>
              批量粘贴
            </el-button>
          </template>
          <el-input
            v-model="dsDialogBatchText"
            type="textarea"
            :rows="5"
            placeholder="每行一个选项，粘贴后点击确认"
          />
          <div style="margin-top: 8px; display: flex; gap: 6px; justify-content: flex-end">
            <el-button size="small" @click="dsDialogBatchVisible = false; dsDialogBatchText = ''">
              取消
            </el-button>
            <el-button size="small" type="primary" @click="confirmDsBatch">
              确认
            </el-button>
          </div>
        </el-popover>
      </div>
    </div>
    <template #footer>
      <el-button @click="dropdownSelectDialog.visible = false">
        取消
      </el-button>
      <el-button type="primary" @click="confirmDropdownSelectDialog">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.formula-input :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 13px;
  line-height: 1.6;
}
.formula-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}
.formula-tools-label {
  font-size: 12px;
  color: #909399;
}
.formula-check {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
}
.formula-check.ok {
  color: var(--el-color-success);
}
.formula-check.err {
  color: var(--el-color-danger);
}
.formula-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.formula-hint code {
  background: #f5f7fa;
  padding: 0 4px;
  border-radius: 3px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--el-color-primary);
}
.formula-empty {
  font-size: 12px;
  color: #909399;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
  line-height: 1.6;
}
.formula-vars {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.formula-var-tag {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  transition: all 0.15s;
}
.formula-var-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}
.formula-var-num {
  font-weight: 600;
  margin-right: 2px;
}
.formula-var-title {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 子表单 / 矩阵列：数字范围限制 */
.range-limit-config {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}
.range-limit-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.range-limit-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  min-width: 48px;
  flex-shrink: 0;
}
.range-limit-input {
  flex: 1;
  width: 0;
  min-width: 0;
}
.range-limit-input :deep(.el-input__wrapper) {
  width: 100%;
}

/* 复合矩阵列卡片 */
.mc-col-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 10px;
  background: #fafafa;
}
</style>
