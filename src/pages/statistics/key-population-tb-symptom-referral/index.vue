<script lang="ts" setup>
/** 旧路由兼容：统计分析下的报表已迁至重点人群模块 */
const router = useRouter()
onMounted(() => {
  router.replace({ path: "/screening/key-population", query: { view: "tbSymptomReferral" } })
})
</script>

<template>
  <div />
</template>
