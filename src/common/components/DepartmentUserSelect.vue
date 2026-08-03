<script lang="ts" setup>
import type { DepartmentUserSelectNode } from "@@/utils/userDepartmentTree"
import { getDepartmentListApi } from "@@/apis/department"
import { getReferralReceiverUsersApi } from "@@/apis/users"
import {
  buildDepartmentUserSelectTree

} from "@@/utils/userDepartmentTree"

const props = withDefaults(defineProps<{
  placeholder?: string
  /** 弹窗打开时置为 true，触发加载/刷新选项 */
  active?: boolean
}>(), {
  placeholder: "请选择部门下的接收用户",
  active: true
})

const modelValue = defineModel<string | undefined>()

const loading = ref(false)
const treeData = ref<DepartmentUserSelectNode[]>([])

async function loadOptions() {
  loading.value = true
  try {
    const [{ data: users }, { data: departments }] = await Promise.all([
      getReferralReceiverUsersApi(),
      getDepartmentListApi()
    ])
    treeData.value = buildDepartmentUserSelectTree(departments ?? [], users ?? [])
  } catch {
    treeData.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadOptions)

watch(
  () => props.active,
  (val) => {
    if (val) loadOptions()
  }
)
</script>

<template>
  <el-tree-select
    v-model="modelValue"
    :data="treeData"
    :loading="loading"
    :placeholder="props.placeholder"
    filterable
    check-strictly
    :render-after-expand="false"
    default-expand-all
    style="width: 100%"
    :props="{
      label: 'label',
      value: 'value',
      children: 'children',
      disabled: 'disabled',
    }"
  />
</template>
