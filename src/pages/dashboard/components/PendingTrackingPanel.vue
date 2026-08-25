<script lang="ts" setup>
import { getReferralTrackingListApi } from "@/pages/referral-management/apis"

const props = defineProps<{
  departmentIds?: string[]
  /** 是否展开明细表；由父级「待追踪」卡片控制 */
  expanded: boolean
}>()

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])

async function fetchData() {
  if (!props.expanded) return
  loading.value = true
  try {
    // 与首页 pendingTracking 口径一致：bizMode=track 且待追踪、未归档
    const { data } = await getReferralTrackingListApi({
      bizMode: "track",
      trackingStatus: 0,
      archived: 0,
      page: 1,
      size: 100
    })
    tableData.value = data?.records ?? []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function goTrack(row: { id?: string, name?: string }) {
  router.push({
    path: "/referral-management/track",
    query: {
      name: row.name || undefined,
      id: row.id || undefined
    }
  })
}

watch(
  () => [props.expanded, props.departmentIds] as const,
  ([expanded]) => {
    if (expanded) fetchData()
  },
  { deep: true, immediate: true }
)

defineExpose({ refresh: fetchData })
</script>

<template>
  <div v-if="expanded" class="pending-tracking-panel">
    <el-table v-loading="loading" :data="tableData" border stripe size="small">
      <el-table-column label="类型" width="100">
        <template #default>
          <el-tag type="danger" size="small">
            待追踪
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" min-width="110" show-overflow-tooltip />
      <el-table-column prop="township" label="乡镇/街道" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.township || "—" }}
        </template>
      </el-table-column>
      <el-table-column prop="entryUnit" label="录入单位" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.entryUnit || "—" }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="登记时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="goTrack(row)">
            前往追踪
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无待追踪人员" :image-size="64" />
      </template>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.pending-tracking-panel {
  margin: 0 0 20px;
}
</style>
