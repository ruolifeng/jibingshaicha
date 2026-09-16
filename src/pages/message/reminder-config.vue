<script lang="ts" setup>
import { getMessageReminderListApi, updateMessageReminderEnabledApi } from "./apis"

defineOptions({ name: "MessageReminderConfig" })

interface ReminderItem {
  id: string
  code: string
  name: string
  description?: string
  scheduleHint?: string
  enabled: number
  sort?: number
}

const loading = ref(false)
const tableData = ref<ReminderItem[]>([])
const switchingCodes = ref<Set<string>>(new Set())

async function fetchList() {
  loading.value = true
  try {
    const { data } = await getMessageReminderListApi()
    tableData.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

async function handleToggle(row: ReminderItem, enabled: boolean) {
  switchingCodes.value.add(row.code)
  try {
    await updateMessageReminderEnabledApi(row.code, enabled)
    row.enabled = enabled ? 1 : 0
    ElMessage.success(enabled ? `已开启「${row.name}」` : `已关闭「${row.name}」`)
  } catch {
    // 回滚开关展示
    row.enabled = enabled ? 0 : 1
  } finally {
    switchingCodes.value.delete(row.code)
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="app-container">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="text-lg font-bold">消息提醒配置</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="mb-4"
        title="此处列出系统中的定时/超时消息提醒。关闭后对应任务不再发送站内消息（短信联动也一并停止）。业务即时通知（如推介确认、转出接收）不受此开关影响。"
      />

      <el-table :data="tableData" border stripe>
        <el-table-column prop="name" label="提醒名称" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="280" show-overflow-tooltip />
        <el-table-column prop="scheduleHint" label="触发时间" width="120" />
        <el-table-column prop="code" label="编码" min-width="200" show-overflow-tooltip />
        <el-table-column label="开关" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="Number(row.enabled) === 1"
              :loading="switchingCodes.has(row.code)"
              inline-prompt
              active-text="开"
              inactive-text="关"
              @change="(val: string | number | boolean) => handleToggle(row, Boolean(val))"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
