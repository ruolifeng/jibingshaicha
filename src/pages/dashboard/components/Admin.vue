<script lang="ts" setup>
import { Search, FirstAidKit, Bell, Calendar, Refresh, School, User, Connection } from "@element-plus/icons-vue"
import { getDashboardSummaryApi } from "../apis"

const summary = ref<Record<string, number>>({})
const loading = ref(false)

async function fetchSummary() {
  loading.value = true
  try {
    const { data } = await getDashboardSummaryApi()
    summary.value = data || {}
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

onMounted(() => { fetchSummary() })

const statCards = [
  { label: "待追踪人数",      key: "pendingTracking",  color: "#f56c6c", icon: Search     },
  { label: "在管患者数",      key: "pendingVisit",     color: "#e6a23c", icon: FirstAidKit },
  { label: "待确认通知单",    key: "pendingNotice",    color: "#409eff", icon: Bell       },
  { label: "复查（15天内）", key: "upcomingReview", color: "#67c23a", icon: Calendar   }
]

const populationCards = [
  { label: "学校人群潜伏", key: "totalSchool",        color: "#409eff", icon: School     },
  { label: "重点人群潜伏", key: "totalKeyPopulation", color: "#e6a23c", icon: User       },
  { label: "密接人群潜伏", key: "totalCloseContact",  color: "#f56c6c", icon: Connection }
]

/** 将 hex 颜色加 alpha 用作半透明背景，兼容亮色/暗色主题 */
function alphaColor(hex: string, alpha = "20") {
  return hex + alpha
}
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <!-- 标题栏 -->
    <div class="dashboard-header">
      <div>
        <div class="dashboard-title">工作台</div>
        <div class="dashboard-subtitle">欢迎使用疾病监控管理系统</div>
      </div>
      <el-button :icon="Refresh" circle @click="fetchSummary" />
    </div>

    <!-- 待处理事项 -->
    <div class="section-label">待处理事项</div>
    <el-row :gutter="24">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="12" :md="6">
        <div class="stat-card" :style="{ borderTopColor: card.color }">
          <div class="stat-icon-wrap" :style="{ backgroundColor: alphaColor(card.color) }">
            <el-icon :size="22" :style="{ color: card.color }">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-num" :style="{ color: card.color }">
              {{ summary[card.key] ?? "—" }}
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 人群分布 -->
    <div class="section-label" style="margin-top: 44px">潜伏感染人群分布</div>
    <el-row :gutter="24">
      <el-col v-for="card in populationCards" :key="card.key" :xs="24" :sm="8">
        <div class="pop-card" :style="{ backgroundColor: alphaColor(card.color, '18') }">
          <el-icon :size="36" :style="{ color: card.color }">
            <component :is="card.icon" />
          </el-icon>
          <div class="pop-num" :style="{ color: card.color }">{{ summary[card.key] ?? 0 }}</div>
          <div class="pop-label">{{ card.label }}</div>
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 36px;
}

.dashboard-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.3;
}

.dashboard-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 3px solid var(--el-color-primary);
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

  &:hover {
    box-shadow: var(--el-box-shadow);
  }

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

.pop-card {
  border-radius: 12px;
  padding: 36px 24px;
  text-align: center;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;

  .pop-num {
    font-size: 42px;
    font-weight: 700;
    line-height: 1;
  }

  .pop-label {
    font-size: 14px;
    color: var(--el-text-color-regular);
  }
}
</style>
