<script lang="ts" setup>
import type { DepartmentFilterOption } from "@@/apis/department"
import { getDepartmentFilterOptionsApi } from "@@/apis/department"

defineOptions({ name: "ScopedDepartmentMultiSelect" })

withDefaults(defineProps<{
  modelValue?: string[]
  placeholder?: string
  width?: string
}>(), {
  modelValue: () => [],
  placeholder: "全部部门",
  width: "280px"
})

const emit = defineEmits<{
  "update:modelValue": [value: string[]]
  /** 是否有可选部门（无选项时父级应隐藏「部门」表单项，避免只剩空标签） */
  "visibilityChange": [visible: boolean]
  /** 选中值变化（便于父级立即按部门刷新） */
  "change": [value: string[]]
}>()

const loading = ref(false)
const options = ref<DepartmentFilterOption[]>([])
const visible = ref(false)

const treeData = computed(() => options.value)

function normalizeOptionIds(nodes: DepartmentFilterOption[]): DepartmentFilterOption[] {
  return (nodes || []).map(node => ({
    ...node,
    id: String(node.id),
    parentId: node.parentId == null ? node.parentId : String(node.parentId),
    children: node.children ? normalizeOptionIds(node.children) : undefined
  }))
}

async function loadOptions() {
  loading.value = true
  try {
    const { data } = await getDepartmentFilterOptionsApi()
    options.value = normalizeOptionIds(data || [])
    visible.value = options.value.length > 0
  } catch {
    options.value = []
    visible.value = false
  } finally {
    loading.value = false
    emit("visibilityChange", visible.value)
  }
}

function handleChange(value: string[] | string | number[] | number | undefined) {
  let next: string[] = []
  if (Array.isArray(value)) {
    next = value.map(item => String(item))
  } else if (value != null) {
    next = [String(value)]
  }
  emit("update:modelValue", next)
  emit("change", next)
}

onMounted(loadOptions)

defineExpose({ reload: loadOptions, visible })
</script>

<template>
  <el-tree-select
    v-if="visible"
    :model-value="modelValue"
    :data="treeData"
    :props="{ label: 'name', value: 'id', children: 'children' }"
    :placeholder="placeholder"
    :style="{ width }"
    multiple
    check-strictly
    collapse-tags
    collapse-tags-tooltip
    clearable
    filterable
    :loading="loading"
    node-key="id"
    @update:model-value="handleChange"
  />
</template>
