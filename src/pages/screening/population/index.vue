<script lang="ts" setup>
import type { Component } from "vue"
import { useUserStore } from "@/pinia/stores/user"

defineOptions({ name: "ScreeningPopulation" })

type ViewTab = "screening" | "suspected"
type SourceTab = "school" | "keyPopulation" | "regular"

const route = useRoute()
const userStore = useUserStore()

const SOURCE_OPTIONS = [
  { label: "学生筛查", value: "school" as const, screeningPerm: "school:screening", suspectedPerm: "school:suspected" },
  { label: "重点人群筛查", value: "keyPopulation" as const, screeningPerm: "keyPopulation:screening", suspectedPerm: "keyPopulation:suspected" },
  { label: "常规筛查", value: "regular" as const, screeningPerm: "regular:screening", suspectedPerm: "regular:suspected" }
]

const viewTab = ref<ViewTab>("screening")
const sourceTab = ref<SourceTab>("school")

function canAccessSource(opt: typeof SOURCE_OPTIONS[number], view: ViewTab) {
  return userStore.hasPermission(view === "screening" ? opt.screeningPerm : opt.suspectedPerm)
}

const visibleSources = computed(() =>
  SOURCE_OPTIONS.filter(opt => canAccessSource(opt, viewTab.value))
)

function ensureActiveSource() {
  if (!visibleSources.value.length) return
  if (!visibleSources.value.some((s: typeof SOURCE_OPTIONS[number]) => s.value === sourceTab.value)) {
    sourceTab.value = visibleSources.value[0].value
  }
}

watch(viewTab, () => ensureActiveSource())
watch(visibleSources, () => ensureActiveSource(), { immediate: true })

const COMPONENT_MAP: Record<string, Component> = {
  "school-screening": defineAsyncComponent(() => import("@/pages/school/screening/index.vue")),
  "school-suspected": defineAsyncComponent(() => import("@/pages/school/suspected/index.vue")),
  "keyPopulation-screening": defineAsyncComponent(() => import("@/pages/key-population/screening/index.vue")),
  "keyPopulation-suspected": defineAsyncComponent(() => import("@/pages/key-population/suspected/index.vue")),
  "regular-screening": defineAsyncComponent(() => import("@/pages/regular/screening/index.vue")),
  "regular-suspected": defineAsyncComponent(() => import("@/pages/regular/suspected/index.vue"))
}

const activeComponent = computed(() => COMPONENT_MAP[`${sourceTab.value}-${viewTab.value}`])
const componentKey = computed(() => `${sourceTab.value}-${viewTab.value}`)

function syncFromQuery() {
  const view = route.query.view as string
  const source = route.query.source as string
  if (view === "screening" || view === "suspected") {
    viewTab.value = view
  }
  if (source === "school" || source === "keyPopulation" || source === "regular") {
    sourceTab.value = source
  }
  ensureActiveSource()
}

onMounted(syncFromQuery)
watch(() => route.query, syncFromQuery)
</script>

<template>
  <div class="screening-population">
    <el-tabs v-model="viewTab" type="border-card" class="view-tabs">
      <el-tab-pane label="筛查导入" name="screening" />
      <el-tab-pane label="待诊断" name="suspected" />
    </el-tabs>

    <div v-if="visibleSources.length" class="source-bar">
      <el-radio-group v-model="sourceTab" size="default">
        <el-radio-button
          v-for="opt in visibleSources"
          :key="opt.value"
          :value="opt.value"
        >
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-empty v-if="!visibleSources.length" description="暂无可用的人群筛查权限" />

    <keep-alive v-if="visibleSources.length" :max="6">
      <component :is="activeComponent" :key="componentKey" class="population-panel" />
    </keep-alive>
  </div>
</template>

<style lang="scss" scoped>
.screening-population {
  .view-tabs {
    :deep(.el-tabs__content) {
      display: none;
    }
  }

  .source-bar {
    margin: 16px 0;
  }

  .population-panel {
    :deep(> .app-container) {
      padding: 0;
    }
  }
}
</style>
