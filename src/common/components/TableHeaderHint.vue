<script lang="ts" setup>
/**
 * 表头名称：有 hint 时可点击查看数字码/填写说明（对齐 Excel 说明行）
 */
const { label, hint = "" } = defineProps<{
  label: string
  /** 数字码或填写说明；为空则仅展示名称 */
  hint?: string
}>()

const visible = ref(false)

/** 将「1=xxx，2=yyy」拆成多行，便于阅读 */
const hintLines = computed(() => {
  const text = hint.trim()
  if (!text) return []
  // 先按中文/英文分号拆段，再按「数字=」切分
  const parts = text
    .split(/[；;]/)
    .flatMap(segment => segment.split(/(?=，?\d+=)/))
    .map(s => s.replace(/^，/, "").trim())
    .filter(Boolean)
  return parts.length > 1 ? parts : [text]
})
</script>

<template>
  <span class="table-header-hint">
    <el-popover
      v-if="hint"
      v-model:visible="visible"
      placement="bottom"
      :width="320"
      trigger="click"
    >
      <template #reference>
        <span
          class="table-header-hint__label is-clickable"
          title="点击查看填写说明"
          @click.stop
        >
          {{ label }}
        </span>
      </template>
      <div class="table-header-hint__body">
        <div class="table-header-hint__title">
          {{ label }} — 填写说明
        </div>
        <p
          v-for="(line, idx) in hintLines"
          :key="idx"
          class="table-header-hint__line"
        >
          {{ line }}
        </p>
      </div>
    </el-popover>
    <span v-else class="table-header-hint__label">
      {{ label }}
    </span>
  </span>
</template>

<style scoped lang="scss">
.table-header-hint {
  display: inline-flex;
  max-width: 100%;
  vertical-align: middle;
}

.table-header-hint__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.is-clickable {
    cursor: pointer;
    border-bottom: 1px dashed var(--el-text-color-secondary);
    color: var(--el-text-color-regular);

    &:hover {
      color: var(--el-color-primary);
      border-bottom-color: var(--el-color-primary);
    }
  }
}

.table-header-hint__body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  line-height: 1.5;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.table-header-hint__title {
  font-weight: 600;
  margin-bottom: 2px;
  color: var(--el-text-color-primary);
}

.table-header-hint__line {
  margin: 0;
  color: var(--el-text-color-regular);
  word-break: break-word;
}
</style>
