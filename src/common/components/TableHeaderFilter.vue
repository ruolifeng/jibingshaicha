<script lang="ts" setup>
import { Filter } from "@element-plus/icons-vue"

export interface HeaderFilterOption {
  text: string
  value: string
}

const props = withDefaults(defineProps<{
  label: string
  modelValue?: string
  /** text=模糊输入；select=枚举多选 */
  type?: "text" | "select"
  options?: HeaderFilterOption[]
  placeholder?: string
}>(), {
  modelValue: "",
  type: "text",
  options: () => [],
  placeholder: "输入后筛选"
})

const emit = defineEmits<{
  "update:modelValue": [value: string]
  "change": [value: string]
}>()

const visible = ref(false)
const draftText = ref("")
const draftSelect = ref<string[]>([])

const isActive = computed(() => !!props.modelValue && props.modelValue.trim() !== "")

watch(visible, (open) => {
  if (!open) return
  if (props.type === "select") {
    draftSelect.value = props.modelValue
      ? props.modelValue.split(",").map(s => s.trim()).filter(Boolean)
      : []
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
    <span class="table-header-filter__label">{{ label }}</span>
    <el-popover v-model:visible="visible" placement="bottom" :width="220" trigger="click">
      <template #reference>
        <el-icon
          class="table-header-filter__icon"
          :class="{ 'is-active': isActive }"
          @click.stop
        >
          <Filter />
        </el-icon>
      </template>
      <div class="table-header-filter__panel">
        <template v-if="type === 'select'">
          <el-checkbox-group v-model="draftSelect" class="table-header-filter__checks">
            <el-checkbox
              v-for="opt in options"
              :key="opt.value"
              :label="opt.value"
            >
              {{ opt.text }}
            </el-checkbox>
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

.table-header-filter__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
}

.table-header-filter__checks {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 220px;
  overflow: auto;
}

.table-header-filter__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
