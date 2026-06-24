<script lang="ts" setup>
import { useLayoutMode } from "@@/composables/useLayoutMode"
import { APP_TITLE } from "@@/constants/app"

interface Props {
  collapse?: boolean
}

const { collapse = true } = defineProps<Props>()

const { isTop } = useLayoutMode()
</script>

<template>
  <div class="layout-logo-container" :class="{ 'collapse': collapse, 'layout-mode-top': isTop }">
    <router-link to="/" class="logo-link">
      <span v-if="!collapse" class="logo-text">{{ APP_TITLE }}</span>
      <!-- 折叠时显示首字缩写 -->
      <span v-else class="logo-icon">自</span>
    </router-link>
  </div>
</template>

<style lang="scss" scoped>
.layout-logo-container {
  position: relative;
  width: 100%;
  height: var(--v3-header-height);
  line-height: var(--v3-header-height);
  text-align: center;
  overflow: hidden;

  .logo-link {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    text-decoration: none;
  }

  .logo-text {
    font-size: 14px;
    font-weight: 700;
    color: #fff;
    letter-spacing: 1px;
    white-space: nowrap;
    padding: 0 8px;
  }

  .logo-icon {
    font-size: 20px;
    font-weight: 700;
    color: #fff;
  }
}

.layout-mode-top {
  height: var(--v3-navigationbar-height);
  line-height: var(--v3-navigationbar-height);
}
</style>
