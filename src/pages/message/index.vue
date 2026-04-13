<script lang="ts" setup>
import { usePagination } from "@@/composables/usePagination"
import { getMessageListApi, markMessageReadApi } from "./apis"

defineOptions({ name: "Message" })

const { paginationData, handleCurrentChange, handleSizeChange } = usePagination()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const isReadFilter = ref<number | undefined>(undefined)

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getMessageListApi({
      page: paginationData.currentPage,
      size: paginationData.pageSize,
      isRead: isReadFilter.value
    })
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(row: any) {
  try {
    await markMessageReadApi(row.id)
    row.isRead = 1
    ElMessage.success("已标记为已读")
  } catch { /* handled */ }
}

watch(
  () => [paginationData.currentPage, paginationData.pageSize],
  fetchData,
  { immediate: true }
)
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">系统消息</span>
          <el-radio-group v-model="isReadFilter" @change="fetchData">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">未读</el-radio-button>
            <el-radio-button :value="1">已读</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <span :class="{ 'font-bold': !row.isRead }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300" />
        <el-table-column prop="type" label="类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'notice_timeout' ? 'danger' : 'warning'">
              {{ { notice_timeout: "通知单超时", supervision_timeout: "督导表超时", visit_timeout: "随访超时" }[row.type as string] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'info' : 'success'" size="small">
              {{ row.isRead ? "已读" : "未读" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isRead" type="primary" size="small" link @click="handleMarkRead(row)">
              标为已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="paginationData.currentPage"
          v-model:page-size="paginationData.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.mt-4 { margin-top: 16px; }
</style>
