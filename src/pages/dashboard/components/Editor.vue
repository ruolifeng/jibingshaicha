<script lang="ts" setup>
import { Search, FirstAidKit, Bell, Calendar } from "@element-plus/icons-vue"
import { getDashboardSummaryApi } from "../apis"

const summary = ref<Record<string, number>>({})

async function fetchSummary() {
  try {
    const { data } = await getDashboardSummaryApi()
    summary.value = data || {}
  } catch { /* handled */ }
}

onMounted(() => { fetchSummary() })

const cards = [
  { label: "待追踪人数",        key: "pendingTracking",  color: "#f56c6c", icon: Search      },
  { label: "在管患者数",        key: "pendingVisit",     color: "#e6a23c", icon: FirstAidKit  },
  { label: "待确认通知单",      key: "pendingNotice",    color: "#409eff", icon: Bell        },
  { label: "复查（15天内）", key: "upcomingReview",  color: "#67c23a", icon: Calendar    }
]

function alphaColor(hex: string, alpha = "20") {
  return hex + alpha
}
</script>

<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div class="dashboard-title">我的工作台</div>
      <div class="dashboard-subtitle">欢迎使用疾病监控管理系统</div>
    </div>

    <el-row :gutter="24">
      <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="12" :md="6">
        <div class="stat-card" :style="{ borderTopColor: card.color }">
          <div class="stat-icon-wrap" :style="{ backgroundColor: alphaColor(card.color) }">
            <el-icon :size="22" :style="{ color: card.color }">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-num" :style="{ color: card.color }">{{ summary[card.key] ?? "—" }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  padding: 40px 48px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  margin-bottom: 36px;
}

.dashboard-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.dashboard-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}

.stat-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  border-top: 3px solid transparent;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: var(--el-box-shadow-light);
  margin-bottom: 24px;
  transition: box-shadow 0.2s;

  &:hover { box-shadow: var(--el-box-shadow); }

  .stat-icon-wrap {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-body {
    .stat-num {
      font-size: 34px;
      font-weight: 700;
      line-height: 1;
    }
    .stat-label {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      margin-top: 6px;
      white-space: nowrap;
    }
  }
}
</style>
