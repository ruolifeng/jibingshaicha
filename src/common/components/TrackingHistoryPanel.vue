<script lang="ts" setup>
import { TRACK_STATUS_LABEL, parseTrackingHistory } from "@@/utils/referralTracking"
import { formatDateTime } from "@@/utils/datetime"

const props = withDefaults(defineProps<{
  historyJson?: string
  emptyText?: string
}>(), {
  emptyText: "暂无追踪记录"
})

const historyList = computed(() => parseTrackingHistory(props.historyJson))
</script>

<template>
  <div v-if="historyList.length === 0" class="tracking-history-empty">
    {{ emptyText }}
  </div>
  <div v-else class="tracking-history">
    <div v-for="item in historyList" :key="item.attempt" class="tracking-history-item">
      <span class="tracking-history-attempt">第{{ item.attempt }}次</span>
      <el-tag :type="item.status === 1 ? 'success' : item.status === 2 ? 'warning' : 'info'" size="small">
        {{ TRACK_STATUS_LABEL[item.status] }}
      </el-tag>
      <span class="tracking-history-time">{{ formatDateTime(item.trackTime) }}</span>
      <span v-if="item.reason" class="tracking-history-reason">备注：{{ item.reason }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.tracking-history {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.tracking-history-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
}

.tracking-history-attempt {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.tracking-history-time {
  color: var(--el-text-color-secondary);
}

.tracking-history-reason {
  flex: 1 1 100%;
  color: var(--el-text-color-regular);
}

.tracking-history-empty {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
