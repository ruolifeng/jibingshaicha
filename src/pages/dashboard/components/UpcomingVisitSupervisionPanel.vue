<script lang="ts" setup>
import type { UpcomingVisitSupervisionItem } from "../apis"
import { Calendar, FirstAidKit } from "@element-plus/icons-vue"
import { getUpcomingVisitSupervisionApi } from "../apis"

const props = defineProps<{
  departmentIds?: string[]
}>()

type ReminderType = "follow_up" | "supervision"

const router = useRouter()
const loading = ref(false)
const tableData = ref<UpcomingVisitSupervisionItem[]>([])
/** 当前展开类型；null 表示不展示表格 */
const activeType = ref<ReminderType | null>(null)

const followUpCount = computed(() => tableData.value.filter(item => item.type === "follow_up").length)
const supervisionCount = computed(() => tableData.value.filter(item => item.type === "supervision").length)

const filteredData = computed(() => {
  if (!activeType.value) return []
  return tableData.value.filter(item => item.type === activeType.value)
})

const summaryCards = computed(() => [
  {
    type: "follow_up" as const,
    label: "后续随访",
    count: followUpCount.value,
    color: "#e6a23c",
    icon: FirstAidKit,
    bg: "#fffbf0"
  },
  {
    type: "supervision" as const,
    label: "后续督导",
    count: supervisionCount.value,
    color: "#67c23a",
    icon: Calendar,
    bg: "#f0fff4"
  }
])

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

/** 点击卡片：再次点击同一卡片收起，否则切换类型并展开表格 */
function toggleType(type: ReminderType) {
  activeType.value = activeType.value === type ? null : type
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

    <el-row v-loading="loading" :gutter="20" class="summary-cards">
      <el-col v-for="card in summaryCards" :key="card.type" :xs="24" :sm="12" :md="8">
        <div
          class="stat-card"
          :class="{ active: activeType === card.type }"
          :style="{ '--card-color': card.color, 'backgroundColor': card.bg }"
          @click="toggleType(card.type)"
        >
          <div class="stat-icon-wrap">
            <el-icon :size="24" :style="{ color: card.color }">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-num">
              {{ card.count }}
            </div>
            <div class="stat-label">
              {{ card.label }}
            </div>
          </div>
          <div class="stat-deco" :style="{ borderColor: card.color }" />
        </div>
      </el-col>
    </el-row>

    <el-table
      v-if="activeType"
      v-loading="loading"
      :data="filteredData"
      border
      stripe
      size="small"
      class="detail-table"
    >
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
        <el-empty
          :description="activeType === 'follow_up' ? '近 7 天内暂无到期随访' : '近 7 天内暂无到期督导'"
          :image-size="64"
        />
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

.summary-cards {
  margin-bottom: 4px;
}

.stat-card {
  border-radius: 14px;
  padding: 22px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
  position: relative;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  user-select: none;
  transition:
    transform 0.2s,
    box-shadow 0.2s,
    border-color 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  }

  &.active {
    border-color: var(--card-color);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  }

  .stat-icon-wrap {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .stat-body {
    .stat-num {
      font-size: 32px;
      font-weight: 700;
      color: var(--card-color);
      line-height: 1;
    }

    .stat-label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      margin-top: 6px;
      white-space: nowrap;
    }
  }

  .stat-deco {
    position: absolute;
    right: -10px;
    bottom: -10px;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    border: 10px solid;
    opacity: 0.12;
  }
}

.detail-table {
  margin-top: 4px;
}
</style>
