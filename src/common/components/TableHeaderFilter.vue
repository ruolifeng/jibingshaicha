<script lang="ts" setup>
import { Filter } from "@element-plus/icons-vue"
import TableHeaderHint from "./TableHeaderHint.vue"

export interface HeaderFilterOption {
  text: string
  value: string
}

const props = withDefaults(defineProps<{
  label: string
  modelValue?: string
  /** text=模糊输入；select=枚举/实际内容多选 */
  type?: "text" | "select"
  options?: HeaderFilterOption[]
  /** 按实际内容补充的去重值（如当前页或服务端 distinct） */
  sourceValues?: string[]
  placeholder?: string
  /** 打开时加载实际内容选项 */
  loadOptions?: () => void | Promise<void>
  /** 点击列名展示的数字码/填写说明 */
  hint?: string
}>(), {
  modelValue: "",
  type: "text",
  options: () => [],
  sourceValues: () => [],
  placeholder: "输入后筛选",
  hint: ""
})

const emit = defineEmits<{
  "update:modelValue": [value: string]
  "change": [value: string]
}>()

const visible = ref(false)
const draftText = ref("")
const draftSelect = ref<string[]>([])
const optionKeyword = ref("")
const loadingOptions = ref(false)

const isActive = computed(() => !!props.modelValue && props.modelValue.trim() !== "")

/** 预设选项 ∪ 实际内容去重，按出现顺序保留 */
const mergedOptions = computed<HeaderFilterOption[]>(() => {
  const seen = new Set<string>()
  const list: HeaderFilterOption[] = []
  const push = (text: string, value: string) => {
    const key = value.trim()
    if (!key || seen.has(key)) return
    seen.add(key)
    list.push({ text: text || key, value: key })
  }
  for (const opt of props.options || []) {
    push(opt.text, opt.value)
  }
  for (const raw of props.sourceValues || []) {
    const v = String(raw ?? "").trim()
    if (v) push(v, v)
  }
  return list
})

const filteredOptions = computed(() => {
  const kw = optionKeyword.value.trim().toLowerCase()
  if (!kw) return mergedOptions.value
  return mergedOptions.value.filter(opt =>
    opt.text.toLowerCase().includes(kw) || opt.value.toLowerCase().includes(kw)
  )
})

watch(visible, async (open) => {
  if (!open) return
  optionKeyword.value = ""
  if (props.type === "select") {
    draftSelect.value = props.modelValue
      ? props.modelValue.split(",").map(s => s.trim()).filter(Boolean)
      : []
    if (props.loadOptions) {
      loadingOptions.value = true
      try {
        await props.loadOptions()
      } catch {
        // 加载失败时仍展示预设 options
      } finally {
        loadingOptions.value = false
      }
    }
  } else {
    draftText.value = props.modelValue || ""
  }
})

function apply() {
  const next = props.type === "select"
    ? draftSelect.value.filter(Boolean).join(",")
    : draftText.value.trim()
  emit("update:modelValue", next)
  emit("change", next)
  visible.value = false
}

function clear() {
  draftText.value = ""
  draftSelect.value = []
  emit("update:modelValue", "")
  emit("change", "")
  visible.value = false
}

function onTextKeydown(e: Event | KeyboardEvent) {
  if (e instanceof KeyboardEvent && e.key === "Enter") apply()
}
</script>

<template>
  <span class="table-header-filter">
    <TableHeaderHint :label="label" :hint="hint" />
    <el-popover v-model:visible="visible" placement="bottom" :width="240" trigger="click">
      <template #reference>
        <el-icon
          class="table-header-filter__icon"
          :class="{ 'is-active': isActive }"
          @click.stop
        >
          <Filter />
        </el-icon>
      </template>
      <div v-loading="loadingOptions" class="table-header-filter__panel">
        <template v-if="type === 'select'">
          <el-input
            v-model="optionKeyword"
            clearable
            size="small"
            placeholder="搜索选项"
          />
          <el-checkbox-group v-model="draftSelect" class="table-header-filter__checks">
            <el-checkbox
              v-for="opt in filteredOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.text }}
            </el-checkbox>
            <div v-if="filteredOptions.length === 0" class="table-header-filter__empty">
              暂无可选项
            </div>
          </el-checkbox-group>
        </template>
        <el-input
          v-else
          v-model="draftText"
          clearable
          size="small"
          :placeholder="placeholder"
          @keydown="onTextKeydown"
        />
        <div class="table-header-filter__actions">
          <el-button size="small" @click="clear">
            清空
          </el-button>
          <el-button type="primary" size="small" @click="apply">
            确定
          </el-button>
        </div>
      </div>
    </el-popover>
  </span>
</template>

<style scoped lang="scss">
.table-header-filter {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
}

.table-header-filter__icon {
  flex-shrink: 0;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  vertical-align: middle;

  &:hover,
  &.is-active {
    color: var(--el-color-primary);
  }
}

.table-header-filter__panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 80px;
}

.table-header-filter__checks {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 220px;
  overflow: auto;
}

.table-header-filter__empty {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  padding: 8px 0;
  text-align: center;
}

.table-header-filter__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
