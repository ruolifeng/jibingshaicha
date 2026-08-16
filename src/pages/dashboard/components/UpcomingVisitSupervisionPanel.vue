<script lang="ts" setup>
import type { UpcomingVisitSupervisionItem } from "../apis"
import { getUpcomingVisitSupervisionApi } from "../apis"

const props = defineProps<{
  departmentIds?: string[]
}>()

const router = useRouter()
const loading = ref(false)
const tableData = ref<UpcomingVisitSupervisionItem[]>([])

const followUpCount = computed(() => tableData.value.filter(item => item.type === "follow_up").length)
const supervisionCount = computed(() => tableData.value.filter(item => item.type === "supervision").length)

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getUpcomingVisitSupervisionApi(props.departmentIds)
    tableData.value = data ?? []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function leadTagType(days: number) {
  if (days <= 1) return "danger"
  if (days <= 3) return "warning"
  return "info"
}

function leadLabel(days: number) {
  if (days === 0) return "今天"
  return `${days} 天`
}

function goFill(row: UpcomingVisitSupervisionItem) {
  if (row.type === "follow_up") {
    router.push("/patient-management/follow-up")
    return
  }
  router.push("/latent-management/supervision")
}

watch(() => props.departmentIds, () => fetchData(), { deep: true })

onMounted(() => {
  fetchData()
})

defineExpose({ refresh: fetchData })
</script>

<template>
  <div class="upcoming-panel">
    <div class="section-label">
      <span class="label-bar" />随访 / 督导到期提醒
      <span class="section-hint">展示未来 7 天内到期项；系统在提前 7/3/1 天发送站内提醒</span>
    </div>
    <div class="summary-row">
      <el-tag type="warning" effect="plain">
        后续随访 {{ followUpCount }} 人
      </el-tag>
      <el-tag type="success" effect="plain">
        后续督导 {{ supervisionCount }} 人
      </el-tag>
    </div>
    <el-table v-loading="loading" :data="tableData" border stripe size="small">
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="row.type === 'follow_up' ? 'warning' : 'success'" size="small">
            {{ row.type === "follow_up" ? "后续随访" : "后续督导" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" min-width="120" />
      <el-table-column prop="dueDate" label="计划日期" width="130" />
      <el-table-column label="剩余天数" width="110">
        <template #default="{ row }">
          <el-tag :type="leadTagType(row.leadDays)" size="small">
            {{ leadLabel(row.leadDays) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="goFill(row)">
            前往填写
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="近 7/3/1 天内暂无到期随访或督导" :image-size="64" />
      </template>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.upcoming-panel {
  margin: 8px 0 24px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);

  .label-bar {
    width: 4px;
    height: 16px;
    border-radius: 2px;
    background: var(--el-color-primary);
  }

  .section-hint {
    font-size: 12px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
  }
}

.summary-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
</style>
