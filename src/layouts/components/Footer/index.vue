<script lang="ts" setup>
const ICP_NUMBER = "桂ICP备2026008858号-1"
const ICP_LINK = "https://beian.miit.gov.cn/"
</script>

<template>
  <footer class="layout-footer">
    <span>备案号：</span>
    <a
      class="icp-link"
      :href="ICP_LINK"
      target="_blank"
      rel="noopener noreferrer"
    >{{ ICP_NUMBER }}</a>
  </footer>
</template>

<style lang="scss" scoped>
.layout-footer {
  width: 100%;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-placeholder);

  .icp-link {
    color: inherit;
    text-decoration: none;

    &:hover {
      color: var(--el-color-primary);
      text-decoration: underline;
    }
  }
}
</style>
