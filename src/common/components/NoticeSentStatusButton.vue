<script lang="ts" setup>
withDefaults(defineProps<{
  link?: boolean
  size?: "large" | "default" | "small"
}>(), {
  link: true,
  size: "small"
})
</script>

<template>
  <el-button
    type="info"
    :link="link"
    :size="size"
    disabled
    class="notice-sent-status-btn"
  >
    已发送通知单
  </el-button>
</template>

<style scoped>
.notice-sent-status-btn.is-disabled,
.notice-sent-status-btn.is-disabled:hover {
  color: var(--el-text-color-primary) !important;
  cursor: not-allowed;
}
</style>
