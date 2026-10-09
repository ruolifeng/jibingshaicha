<script lang="ts" setup>
import type { Component } from "vue"
import { useUserStore } from "@/pinia/stores/user"

type ViewTab = "screening" | "suspected" | "tbSymptomReferral"
type SourceTab = "school" | "keyPopulation" | "regular"

const props = defineProps<{
  source: SourceTab
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const PERM_MAP: Record<SourceTab, {
  screening: string
  suspected: string
  tbSymptomReferral?: string
}> = {
  school: { screening: "school:screening", suspected: "school:suspected" },
  keyPopulation: {
    screening: "keyPopulation:screening",
    suspected: "keyPopulation:suspected",
    tbSymptomReferral: "keyPopulation:tbSymptomReferral"
  },
  regular: { screening: "regular:screening", suspected: "regular:suspected" }
}

const viewTab = ref<ViewTab>("screening")

const suspectedTabLabel = computed(() =>
  props.source === "school" ? "学生报表统计" : "待诊断"
)

function canAccessView(view: ViewTab) {
  const perms = PERM_MAP[props.source]
  if (view === "screening") return userStore.hasPermission(perms.screening)
  if (view === "suspected") return userStore.hasPermission(perms.suspected)
  if (view === "tbSymptomReferral") {
    return !!perms.tbSymptomReferral && userStore.hasPermission(perms.tbSymptomReferral)
  }
  return false
}

const visibleViews = computed(() => {
  const views: ViewTab[] = []
  if (canAccessView("screening")) views.push("screening")
  if (canAccessView("suspected")) views.push("suspected")
  if (canAccessView("tbSymptomReferral")) views.push("tbSymptomReferral")
  return views
})

function ensureActiveView() {
  if (!visibleViews.value.length) return
  if (!visibleViews.value.includes(viewTab.value)) {
    viewTab.value = visibleViews.value[0]
  }
}

watch(visibleViews, () => ensureActiveView(), { immediate: true })

const COMPONENT_MAP: Record<string, Component> = {
  "school-screening": defineAsyncComponent(() => import("@/pages/school/screening/index.vue")),
  "school-suspected": defineAsyncComponent(() => import("@/pages/school/suspected/index.vue")),
  "keyPopulation-screening": defineAsyncComponent(() => import("@/pages/key-population/screening/index.vue")),
  "keyPopulation-suspected": defineAsyncComponent(() => import("@/pages/key-population/suspected/index.vue")),
  "keyPopulation-tbSymptomReferral": defineAsyncComponent(() => import("@/pages/key-population/tb-symptom-referral/index.vue")),
  "regular-screening": defineAsyncComponent(() => import("@/pages/regular/screening/index.vue")),
  "regular-suspected": defineAsyncComponent(() => import("@/pages/regular/suspected/index.vue"))
}

const activeComponent = computed(() => COMPONENT_MAP[`${props.source}-${viewTab.value}`])
const componentKey = computed(() => `${props.source}-${viewTab.value}`)

function syncFromQuery() {
  const view = route.query.view as string
  if (view === "screening" || view === "suspected" || view === "tbSymptomReferral") {
    viewTab.value = view
  }
  ensureActiveView()
}

watch(viewTab, (view) => {
  if (route.query.view !== view) {
    router.replace({ query: { ...route.query, view } })
  }
})

onMounted(syncFromQuery)
watch(() => route.query.view, syncFromQuery)
</script>

<template>
  <div class="screening-source">
    <el-tabs v-model="viewTab" type="border-card" class="view-tabs">
      <el-tab-pane v-if="canAccessView('screening')" label="筛查数据" name="screening" />
      <el-tab-pane v-if="canAccessView('suspected')" :label="suspectedTabLabel" name="suspected" />
      <el-tab-pane
        v-if="canAccessView('tbSymptomReferral')"
        label="结核症状筛查推介"
        name="tbSymptomReferral"
      />
    </el-tabs>

    <el-empty v-if="!visibleViews.length" description="暂无可用权限" />

    <keep-alive v-if="visibleViews.length" :max="3">
      <component :is="activeComponent" :key="componentKey" class="source-panel" />
    </keep-alive>
  </div>
</template>

<style lang="scss" scoped>
.screening-source {
  .view-tabs {
    :deep(.el-tabs__content) {
      display: none;
    }
  }

  .source-panel {
    margin-top: 16px;

    :deep(> .app-container) {
      padding: 0;
    }
  }

  :deep(.screening-data-table) {
    th.el-table__cell {
      text-align: center;
    }

    th.el-table__cell > .cell {
      justify-content: center;
      text-align: center;
    }
  }
}
</style>
