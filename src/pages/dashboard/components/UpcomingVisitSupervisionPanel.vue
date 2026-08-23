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

/** 跳转对应填写页，并带上姓名便于列表预筛 */
function goFill(row: UpcomingVisitSupervisionItem) {
  const query = {
    name: row.name || undefined,
    id: row.bizId || undefined
  }
  if (row.type === "follow_up") {
    router.push({ path: "/patient-management/follow-up", query })
    return
  }
  router.push({ path: "/latent-management/supervision", query })
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
      <el-tag type="warning" effect="dark" class="summary-tag">
        后续随访 {{ followUpCount }} 人
      </el-tag>
      <el-tag type="success" effect="plain" class="summary-tag">
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
      <el-table-column prop="name" label="姓名" min-width="110" show-overflow-tooltip />
      <el-table-column prop="managerOrgName" label="管理人对应机构" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.managerOrgName || "—" }}
        </template>
      </el-table-column>
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
        <el-empty description="近 7 天内暂无到期随访或督导" :image-size="64" />
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

.summary-tag {
  font-size: 13px;
  padding: 0 14px;
  height: 28px;
  line-height: 26px;
}
</style>
