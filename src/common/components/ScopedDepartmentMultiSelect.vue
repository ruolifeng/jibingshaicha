<script lang="ts" setup>
import type { DepartmentFilterOption } from "@@/apis/department"
import { getDepartmentFilterOptionsApi } from "@@/apis/department"

defineOptions({ name: "ScopedDepartmentMultiSelect" })

withDefaults(defineProps<{
  modelValue?: number[]
  placeholder?: string
  width?: string
}>(), {
  modelValue: () => [],
  placeholder: "全部部门",
  width: "280px"
})

const emit = defineEmits<{
  "update:modelValue": [value: number[]]
}>()

const loading = ref(false)
const options = ref<DepartmentFilterOption[]>([])
const visible = ref(false)

const treeData = computed(() => options.value)

async function loadOptions() {
  loading.value = true
  try {
    const { data } = await getDepartmentFilterOptionsApi()
    options.value = data || []
    visible.value = options.value.length > 0
  } catch {
    options.value = []
    visible.value = false
  } finally {
    loading.value = false
  }
}

function handleChange(value: number[] | number | undefined) {
  if (Array.isArray(value)) {
    emit("update:modelValue", value)
    return
  }
  if (value == null) {
    emit("update:modelValue", [])
    return
  }
  emit("update:modelValue", [value])
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
