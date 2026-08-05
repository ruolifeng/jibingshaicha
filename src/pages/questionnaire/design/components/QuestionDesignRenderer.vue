<script lang="ts" setup>
import type { QuestionItem } from "../../apis/type"
import { Edit, Plus } from "@element-plus/icons-vue"
import { remapFormulaRefs } from "../utils/formula"

interface OptionObj { label: string, value: string, imageUrl?: string, contentLimit?: string, dropdownOptions?: string[] }

/**
 * 可选传入完整题目列表，用于将公式中的 stableKey 占位符（`{12}`/`{t3}`）
 * 转换为用户可读的题号（`{Q1}`）。未传则原样显示。
 */
const props = defineProps<{
  question: QuestionItem
  index: number
  questions?: QuestionItem[]
}>()

const layoutTypes = new Set(["divider", "page_break"])

/** 与 design/index.vue 的 getStableKey 保持一致 */
function getStableKey(q: QuestionItem): string {
  return q.id != null ? String(q.id) : `t${q._tempKey}`
}

const optionsList = computed(() => {
  if (!props.question.options) return [] as OptionObj[]
  try {
    return JSON.parse(props.question.options) as OptionObj[]
  } catch {
    return [] as OptionObj[]
  }
})

const cascaderOptions = computed(() => {
  if (!props.question.options) return []
  try {
    return JSON.parse(props.question.options)
  } catch {
    return []
  }
})

const matrixOpts = computed(() => {
  if (!props.question.options) return { rows: [] as OptionObj[], cols: [] as OptionObj[] }
  try {
    return JSON.parse(props.question.options)
  } catch {
    return { rows: [], cols: [] }
  }
})

const scaleOpts = computed(() => {
  if (!props.question.options) return { rows: [] as OptionObj[], scaleMin: 1, scaleMax: 5, scaleLabels: {} as Record<string, string> }
  try {
    const p = JSON.parse(props.question.options)
    return { rows: p.rows || [], scaleMin: p.scaleMin ?? 1, scaleMax: p.scaleMax ?? 5, scaleLabels: p.scaleLabels || {} }
  } catch {
    return { rows: [], scaleMin: 1, scaleMax: 5, scaleLabels: {} }
  }
})

const imageDisplayOpts = computed(() => {
  if (!props.question.options) return { imageUrl: "", alt: "" }
  try {
    return JSON.parse(props.question.options)
  } catch {
    return { imageUrl: "", alt: "" }
  }
})

interface MCColShowWhen { column: string, op: string, value?: string }
interface MCCol {
  key: string
  label: string
  type: "radio" | "checkbox" | "input" | "freq"
  width?: string
  options?: { label: string, value: string }[]
  units?: { label: string, value: string }[]
  inputType?: string
  suffix?: string
  showWhen?: MCColShowWhen
}
interface MCOpts { rows: { label: string, value: string }[], columns: MCCol[] }

const matrixComplexOpts = computed<MCOpts>(() => {
  if (!props.question.options) return { rows: [], columns: [] }
  try {
    const p = JSON.parse(props.question.options)
    return { rows: Array.isArray(p.rows) ? p.rows : [], columns: Array.isArray(p.columns) ? p.columns : [] }
  } catch {
    return { rows: [], columns: [] }
  }
})

const validation = computed((): Record<string, any> => {
  if (!props.question.validationRules) return {}
  try {
    return JSON.parse(props.question.validationRules)
  } catch {
    return {}
  }
})

const npsRange = computed(() => {
  const min = validation.value.min ?? 0
  const max = validation.value.max ?? 10
  return Array.from({ length: max - min + 1 }, (_, i) => i + min)
})

const scaleRange = computed(() =>
  Array.from({ length: scaleOpts.value.scaleMax - scaleOpts.value.scaleMin + 1 }, (_, i) => i + scaleOpts.value.scaleMin)
)

/** stableKey → 题号（用于公式显示文本中的占位符替换） */
const stableKeyToNum = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {}
  if (!props.questions) return map
  let num = 0
  for (const q of props.questions) {
    if (layoutTypes.has(q.type)) continue
    num++
    map[getStableKey(q)] = num
  }
  return map
})

/** 用户可读的公式（formula 题专用，未传 questions 时原样显示） */
const formulaDisplayText = computed(() => {
  if (props.question.type !== "formula") return ""
  const raw = String(validation.value.formula || "")
  if (!raw) return ""
  if (!props.questions) return raw
  return remapFormulaRefs(raw, (k) => {
    const num = stableKeyToNum.value[k]
    return num ? `Q${num}` : null
  })
})
</script>

<template>
  <!-- 单选 -->
  <template v-if="question.type === 'radio'">
    <el-radio-group disabled style="display: flex; flex-direction: column; align-items: flex-start; gap: 6px; width: 100%">
      <el-radio v-for="o in optionsList" :key="o.value" :value="o.value">
        {{ o.label }}
      </el-radio>
    </el-radio-group>
  </template>

  <!-- 多选 -->
  <template v-else-if="question.type === 'checkbox'">
    <el-checkbox-group disabled style="display: flex; flex-direction: column; align-items: flex-start; gap: 6px; width: 100%">
      <el-checkbox v-for="o in optionsList" :key="o.value" :value="o.value">
        {{ o.label }}
      </el-checkbox>
    </el-checkbox-group>
  </template>

  <!-- 下拉 -->
  <template v-else-if="question.type === 'dropdown'">
    <el-select disabled placeholder="请选择" style="width: 100%">
      <el-option v-for="o in optionsList" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>
  </template>

  <!-- 级联 -->
  <template v-else-if="question.type === 'cascader'">
    <el-cascader disabled placeholder="请选择" style="width: 100%" :options="cascaderOptions" :props="{ label: 'label', value: 'value', children: 'children' }" />
  </template>

  <!-- 单行文本 -->
  <template v-else-if="question.type === 'input'">
    <el-select v-if="validation.dropdownEnabled" disabled placeholder="请选择或搜索" style="width: 100%" filterable>
      <el-option v-for="o in optionsList" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>
    <!-- 下拉单选内容限制：预览为禁用下拉 -->
    <el-select v-else-if="validation.contentLimit === 'dropdown_select'" disabled placeholder="请选择" style="width: 100%">
      <el-option v-for="opt in (validation.dropdownSelectOptions || [])" :key="opt" :label="opt" :value="opt" />
    </el-select>
    <el-input v-else disabled placeholder="请输入" />
  </template>

  <!-- 多行文本 -->
  <template v-else-if="question.type === 'textarea'">
    <el-select v-if="validation.dropdownEnabled" disabled placeholder="请选择或搜索" style="width: 100%" filterable>
      <el-option v-for="o in optionsList" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>
    <el-input v-else type="textarea" :rows="3" disabled placeholder="请输入" />
  </template>

  <!-- 数字 -->
  <template v-else-if="question.type === 'number'">
    <el-input-number disabled :min="validation.min" :max="validation.max" :step="validation.step ?? 1" :precision="validation.precision ?? 0" />
  </template>

  <!-- 日期时间 -->
  <template v-else-if="question.type === 'date'">
    <el-date-picker disabled :type="validation.dateType || 'date'" placeholder="选择日期" style="width: 100%" />
  </template>

  <!-- 滑块 -->
  <template v-else-if="question.type === 'slider'">
    <el-slider disabled :min="validation.min ?? 0" :max="validation.max ?? 100" :step="validation.step ?? 1" :show-input="validation.showInput === true" />
  </template>

  <!-- 多项填空 -->
  <template v-else-if="question.type === 'multi_input'">
    <div style="display: flex; flex-direction: column; gap: 8px">
      <div v-for="f in optionsList" :key="f.value" style="display: flex; align-items: center; gap: 8px">
        <span style="font-size: 13px; white-space: nowrap; color: #606266">{{ f.label }}：</span>
        <el-input disabled placeholder="请输入" size="small" />
      </div>
    </div>
  </template>

  <!-- 横向填空 -->
  <template v-else-if="question.type === 'inline_input'">
    <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap">
      <template v-for="f in optionsList" :key="f.value">
        <span style="font-size: 13px; color: #606266">{{ f.label }}</span>
        <el-input disabled placeholder="填写" size="small" style="width: 100px" />
      </template>
    </div>
  </template>

  <!-- 评分 -->
  <template v-else-if="question.type === 'rating'">
    <el-rate disabled :max="validation.max ?? 5" />
  </template>

  <!-- NPS -->
  <template v-else-if="question.type === 'nps'">
    <div style="display: flex; gap: 4px; flex-wrap: wrap">
      <el-button v-for="n in npsRange" :key="n" size="small" disabled>
        {{ n }}
      </el-button>
    </div>
    <div style="display: flex; justify-content: space-between; font-size: 12px; color: #909399; margin-top: 4px">
      <span>极不推荐</span><span>极力推荐</span>
    </div>
  </template>

  <!-- 矩阵单选 / 矩阵多选 / 矩阵填空 -->
  <template v-else-if="['matrix_radio', 'matrix_checkbox', 'matrix_input'].includes(question.type)">
    <div style="overflow-x: auto">
      <table style="width: 100%; border-collapse: collapse; font-size: 13px">
        <thead>
          <tr>
            <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa" />
            <th v-for="c in matrixOpts.cols" :key="c.value" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; text-align: center">
              {{ c.label }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in matrixOpts.rows" :key="r.value">
            <td style="border: 1px solid #ebeef5; padding: 8px; font-weight: 500">
              {{ r.label }}
            </td>
            <td v-for="c in matrixOpts.cols" :key="c.value" style="border: 1px solid #ebeef5; padding: 8px; text-align: center">
              <el-radio v-if="question.type === 'matrix_radio'" disabled :value="c.value" />
              <el-checkbox v-else-if="question.type === 'matrix_checkbox'" disabled :value="c.value" />
              <el-input v-else disabled size="small" placeholder="-" style="width: 80px" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <!-- 复合矩阵 -->
  <template v-else-if="question.type === 'matrix_complex'">
    <div style="overflow-x: auto">
      <table style="width: 100%; border-collapse: collapse; font-size: 13px">
        <thead>
          <tr>
            <th style="border: 1px solid #ebeef5; padding: 6px 8px; background: #f5f7fa; text-align: left; min-width: 120px">
              项目
            </th>
            <th
              v-for="col in matrixComplexOpts.columns"
              :key="col.key"
              :style="{ border: '1px solid #ebeef5', padding: '6px 8px', background: '#f5f7fa', textAlign: 'center', minWidth: col.width || (col.type === 'freq' ? '220px' : '140px') }"
            >
              {{ col.label || col.key }}
              <span v-if="col.showWhen" style="color: var(--el-color-warning); margin-left: 4px; font-size: 11px" title="条件显示">⓵</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in matrixComplexOpts.rows" :key="r.value">
            <td style="border: 1px solid #ebeef5; padding: 6px 8px; font-weight: 500; white-space: nowrap">
              <span style="color: #909399; margin-right: 4px">{{ r.value }}</span>{{ r.label }}
            </td>
            <td
              v-for="col in matrixComplexOpts.columns"
              :key="col.key + r.value"
              style="border: 1px solid #ebeef5; padding: 6px 8px; text-align: center"
            >
              <!-- 单选 -->
              <div v-if="col.type === 'radio'" style="display: flex; gap: 8px; flex-wrap: wrap; justify-content: center">
                <span v-for="o in (col.options || [])" :key="o.value" style="font-size: 12px; color: #606266; display: inline-flex; align-items: center; gap: 2px">
                  <el-radio disabled :value="o.value" />{{ o.label }}
                </span>
              </div>
              <!-- 多选 -->
              <div v-else-if="col.type === 'checkbox'" style="display: flex; gap: 8px; flex-wrap: wrap; justify-content: center">
                <span v-for="o in (col.options || [])" :key="o.value" style="font-size: 12px; color: #606266; display: inline-flex; align-items: center; gap: 2px">
                  <el-checkbox disabled :value="o.value" />{{ o.label }}
                </span>
              </div>
              <!-- 频率（单位互斥单选 + 数字） -->
              <div v-else-if="col.type === 'freq'" style="display: flex; flex-direction: column; gap: 4px">
                <div v-for="u in (col.units || [])" :key="u.value" style="display: flex; align-items: center; gap: 4px; justify-content: center">
                  <el-radio disabled :value="u.value" />
                  <span style="font-size: 12px; color: #606266; min-width: 40px">{{ u.label }}</span>
                  <el-input disabled size="small" placeholder="-" style="width: 70px" />
                </div>
              </div>
              <!-- 填空 -->
              <div v-else-if="col.type === 'input'" style="display: flex; align-items: center; gap: 4px; justify-content: center">
                <el-input disabled size="small" placeholder="—" style="flex: 1; min-width: 60px" />
                <span v-if="col.suffix" style="font-size: 12px; color: #909399; flex-shrink: 0">{{ col.suffix }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <!-- 矩阵量表 -->
  <template v-else-if="question.type === 'matrix_scale'">
    <div style="overflow-x: auto">
      <table style="width: 100%; border-collapse: collapse; font-size: 13px">
        <thead>
          <tr>
            <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa" />
            <th v-for="n in scaleRange" :key="n" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa; text-align: center">
              {{ scaleOpts.scaleLabels[String(n)] || n }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in scaleOpts.rows" :key="r.value">
            <td style="border: 1px solid #ebeef5; padding: 8px; font-weight: 500">
              {{ r.label }}
            </td>
            <td v-for="n in scaleRange" :key="n" style="border: 1px solid #ebeef5; padding: 8px; text-align: center">
              <el-radio disabled :value="String(n)" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>

  <!-- 子表单 / 自增表格 -->
  <template v-else-if="question.type === 'dynamic_table'">
    <div style="overflow-x: auto">
      <table style="width: 100%; border-collapse: collapse; font-size: 13px">
        <thead>
          <tr>
            <th style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa">
              #
            </th>
            <th v-for="col in optionsList" :key="col.value" style="border: 1px solid #ebeef5; padding: 8px; background: #f5f7fa">
              {{ col.label }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td style="border: 1px solid #ebeef5; padding: 8px; text-align: center">
              1
            </td>
            <td v-for="col in optionsList" :key="col.value" style="border: 1px solid #ebeef5; padding: 8px">
              <el-select v-if="col.contentLimit === 'dropdown_select'" disabled size="small" placeholder="请选择" style="width: 100%">
                <el-option v-for="opt in (col.dropdownOptions || [])" :key="opt" :label="opt" :value="opt" />
              </el-select>
              <el-input v-else disabled size="small" placeholder="请输入" />
            </td>
          </tr>
        </tbody>
      </table>
      <el-button size="small" disabled style="margin-top: 8px" :icon="Plus">
        添加一行
      </el-button>
    </div>
  </template>

  <!-- 排序 -->
  <template v-else-if="question.type === 'sort'">
    <div style="display: flex; flex-direction: column; gap: 6px">
      <div v-for="(opt, oi) in optionsList" :key="opt.value" style="padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 6px; display: flex; align-items: center; gap: 8px; background: #fafafa">
        <span style="color: #c0c4cc; cursor: default">⠿</span>
        <span>{{ oi + 1 }}. {{ opt.label }}</span>
      </div>
    </div>
  </template>

  <!-- 图片选择 -->
  <template v-else-if="question.type === 'image_choice'">
    <div style="display: flex; gap: 12px; flex-wrap: wrap">
      <div v-for="opt in optionsList" :key="opt.value" style="width: 120px; border: 1px solid #dcdfe6; border-radius: 8px; overflow: hidden; text-align: center; cursor: default">
        <img v-if="opt.imageUrl" :src="opt.imageUrl" style="width: 100%; height: 80px; object-fit: cover">
        <div v-else style="width: 100%; height: 80px; background: #f5f7fa; display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px">
          无图片
        </div>
        <div style="padding: 4px 6px; font-size: 12px; color: #606266">
          {{ opt.label }}
        </div>
      </div>
    </div>
  </template>

  <!-- 文件上传 -->
  <template v-else-if="question.type === 'file_upload'">
    <el-button disabled size="small">
      选择文件
    </el-button>
    <div style="font-size: 12px; color: #909399; margin-top: 4px">
      {{ validation.accept ? `支持 ${validation.accept}，` : '' }}最大 {{ validation.maxSize ?? 10 }}MB
    </div>
  </template>

  <!-- 图片上传 -->
  <template v-else-if="question.type === 'image_upload'">
    <div style="width: 80px; height: 80px; border: 1px dashed #dcdfe6; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: #c0c4cc; cursor: default">
      <el-icon :size="24">
        <Plus />
      </el-icon>
    </div>
    <div style="font-size: 12px; color: #909399; margin-top: 4px">
      最多 {{ validation.maxCount ?? 3 }} 张，每张最大 {{ validation.maxSize ?? 5 }}MB
    </div>
  </template>

  <!-- 文字描述 -->
  <template v-else-if="question.type === 'description'">
    <div style="color: #606266; font-size: 14px; line-height: 1.6; white-space: pre-wrap">
      {{ question.description || '(暂无描述内容)' }}
    </div>
  </template>

  <!-- 分割线 -->
  <template v-else-if="question.type === 'divider'">
    <el-divider />
  </template>

  <!-- 图片展示 -->
  <template v-else-if="question.type === 'image_display'">
    <img v-if="imageDisplayOpts.imageUrl" :src="imageDisplayOpts.imageUrl" :alt="imageDisplayOpts.alt" style="max-width: 100%; border-radius: 4px">
    <span v-else style="color: #909399; font-size: 13px">（未设置图片）</span>
  </template>

  <!-- 电子签名 -->
  <template v-else-if="question.type === 'signature'">
    <div style="border: 1.5px dashed var(--el-border-color); border-radius: 8px; height: 100px; display: flex; align-items: center; justify-content: center; background: #fafafa; color: #c0c4cc; font-size: 13px; gap: 6px">
      <el-icon><Edit /></el-icon>
      <span>签名区域（{{ validation.hint || '请在此处手写签名' }}）</span>
    </div>
  </template>

  <!-- 自动计算 -->
  <template v-else-if="question.type === 'formula'">
    <div class="formula-preview">
      <div class="formula-preview-row">
        <el-input disabled placeholder="自动计算结果" style="flex: 1" />
        <span v-if="validation.unit" class="formula-preview-unit">{{ validation.unit }}</span>
      </div>
      <div v-if="formulaDisplayText" class="formula-preview-expr">
        公式：<code>{{ formulaDisplayText }}</code>
      </div>
      <div v-else class="formula-preview-expr placeholder">
        尚未配置公式
      </div>
    </div>
  </template>

  <!-- 分页 -->
  <template v-else-if="question.type === 'page_break'">
    <el-divider content-position="center">
      <span style="color: #909399; font-size: 12px">—— 分页 ——</span>
    </el-divider>
  </template>

  <!-- 兜底 -->
  <template v-else>
    <el-input disabled placeholder="文本输入" />
  </template>
</template>

<style scoped>
.formula-preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.formula-preview-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.formula-preview-unit {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}
.formula-preview-expr {
  font-size: 12px;
  color: #909399;
}
.formula-preview-expr code {
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 3px;
  color: var(--el-color-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}
.formula-preview-expr.placeholder {
  font-style: italic;
}
</style>
