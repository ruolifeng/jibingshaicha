<script lang="ts" setup>
import { onMounted } from "vue"
import { useRouter } from "vue-router"

/** 旧「筛查管理-大疫情导入」入口，已迁移至推介追踪-追踪 */
defineOptions({ name: "ScreeningEpidemicLegacy" })

const router = useRouter()

onMounted(() => {
  router.replace("/referral-management/track")
})
</script>

<template>
  <div class="app-container" style="padding: 48px; text-align: center; color: #909399">
    正在跳转到「推介追踪 → 追踪」…
  </div>
</template>
