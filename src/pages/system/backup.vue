<script lang="ts" setup>
import { request } from "@/http/axios"

defineOptions({ name: "Backup" })

const backupList = ref<Array<{ name: string, size: number, lastModified: number }>>([])
const loading = ref(false)
const downloading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const { data } = await request<ApiResponseData<any[]>>({ url: "backup/list", method: "get" })
    backupList.value = data || []
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function handleManualBackup() {
  downloading.value = true
  try {
    const blob = await request<Blob>({ url: "backup/download", method: "get", responseType: "blob" })
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, "")
    const url = window.URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `backup_${today}.sql`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success("备份下载成功")
    fetchList()
  } catch {
    ElMessage.error("备份失败，请检查服务器是否已安装 mysqldump")
  } finally {
    downloading.value = false
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

function formatTime(ts: number) {
  return new Date(ts).toLocaleString("zh-CN")
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">数据备份管理</span>
          <el-button type="primary" :loading="downloading" @click="handleManualBackup">
            立即备份并下载
          </el-button>
        </div>
      </template>
      <el-alert type="info" :closable="false" class="mb-4">
        系统每天凌晨 03:00 自动备份数据库，最多保留最近 30 份。可点击"立即备份并下载"手动触发并将备份文件下载到本地。
      </el-alert>

      <el-table v-loading="loading" :data="backupList" border stripe max-height="500">
        <el-table-column prop="name" label="文件名" />
        <el-table-column label="文件大小">
          <template #default="{ row }">
            {{ formatSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="备份时间">
          <template #default="{ row }">
            {{ formatTime(row.lastModified) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.mb-4 {
  margin-bottom: 16px;
}
</style>
