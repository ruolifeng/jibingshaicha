<script lang="ts" setup>
import { useAppStore } from "@/pinia/stores/app"

const appStore = useAppStore()
</script>

<template>
  <el-drawer v-model="appStore.rightPanelOpened" size="300px" :with-header="false">
    <slot />
  </el-drawer>
</template>
