import type { RouteRecordRaw } from "vue-router"
import { createRouter } from "vue-router"
import { routerConfig } from "@/router/config"
import { registerNavigationGuard } from "@/router/guard"
import { flatMultiLevelRoutes } from "./helper"

const Layouts = () => import("@/layouts/index.vue")

/**
 * @name 常驻路由
 * @description 除了 redirect/403/404/login 等隐藏页面，其他页面建议设置唯一的 Name 属性
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: "/redirect",
    component: Layouts,
    meta: { hidden: true },
    children: [{ path: ":path(.*)", component: () => import("@/pages/redirect/index.vue") }]
  },
  {
    path: "/403",
    component: () => import("@/pages/error/403.vue"),
    meta: { hidden: true }
  },
  {
    path: "/404",
    component: () => import("@/pages/error/404.vue"),
    meta: { hidden: true },
    alias: "/:pathMatch(.*)*"
  },
  {
    path: "/login",
    component: () => import("@/pages/login/index.vue"),
    meta: { hidden: true }
  },
  {
    path: "/",
    component: Layouts,
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        component: () => import("@/pages/dashboard/index.vue"),
        name: "Dashboard",
        meta: { title: "首页", svgIcon: "dashboard", affix: true }
      }
    ]
  },

  // ==================== V2 新增：筛查管理（聚合 4 个子模块） ====================
  {
    path: "/screening",
    component: Layouts,
    redirect: "/screening/school/screening",
    name: "Screening",
    meta: { title: "筛查管理", elIcon: "Search", permission: "screening" },
    children: [
      // ── 学生筛查 ──
      {
        path: "school/screening",
        component: () => import("@/pages/school/screening/index.vue"),
        name: "ScreeningSchool",
        meta: { title: "筛查导入", tagTitle: "学生-筛查导入", keepAlive: true, permission: "school:screening" }
      },
      {
        path: "school/suspected",
        component: () => import("@/pages/school/suspected/index.vue"),
        name: "ScreeningSchoolSuspected",
        meta: { title: "待诊断", tagTitle: "学生-待诊断", keepAlive: true, permission: "school:suspected" }
      },
      // ── 重点人群筛查 ──
      {
        path: "key-population/screening",
        component: () => import("@/pages/key-population/screening/index.vue"),
        name: "ScreeningKeyPopulation",
        meta: { title: "筛查导入", tagTitle: "重点-筛查导入", keepAlive: true, permission: "keyPopulation:screening" }
      },
      {
        path: "key-population/suspected",
        component: () => import("@/pages/key-population/suspected/index.vue"),
        name: "ScreeningKeyPopulationSuspected",
        meta: { title: "待诊断", tagTitle: "重点-待诊断", keepAlive: true, permission: "keyPopulation:suspected" }
      },
      // ── 常规筛查（新增） ──
      {
        path: "regular/screening",
        component: () => import("@/pages/regular/screening/index.vue"),
        name: "ScreeningRegular",
        meta: { title: "筛查导入", tagTitle: "常规-筛查导入", keepAlive: true, permission: "regular:screening" }
      },
      {
        path: "regular/suspected",
        component: () => import("@/pages/regular/suspected/index.vue"),
        name: "ScreeningRegularSuspected",
        meta: { title: "待诊断", tagTitle: "常规-待诊断", keepAlive: true, permission: "regular:suspected" }
      },
      // ── 大疫情导入筛查（占位，新增） ──
      {
        path: "epidemic",
        component: () => import("@/pages/epidemic/index.vue"),
        name: "ScreeningEpidemic",
        meta: { title: "大疫情导入筛查", tagTitle: "大疫情导入筛查", keepAlive: true, permission: "epidemic:screening" }
      }
    ]
  },

  // ==================== V2 新增：潜伏感染者管理（聚合，不含密接） ====================
  {
    path: "/latent-management",
    component: Layouts,
    redirect: "/latent-management/notice",
    name: "LatentManagement",
    meta: { title: "潜伏感染者管理", elIcon: "Timer", permission: "latentManagement" },
    children: [
      {
        path: "notice",
        component: () => import("@/pages/latent-management/notice.vue"),
        name: "LatentManagementNotice",
        meta: { title: "通知单管理", tagTitle: "潜伏-通知单管理", keepAlive: true, permission: "latentManagement:notice" }
      },
      {
        path: "supervision",
        component: () => import("@/pages/latent-management/supervision.vue"),
        name: "LatentManagementSupervision",
        meta: { title: "督导表管理", tagTitle: "潜伏-督导表管理", keepAlive: true, permission: "latentManagement:supervision" }
      }
    ]
  },

  // ==================== V2 新增：患者管理（聚合，每个功能独立子菜单） ====================
  {
    path: "/patient-management",
    component: Layouts,
    redirect: "/patient-management/notice",
    name: "PatientManagement",
    meta: { title: "患者管理", elIcon: "User", permission: "patientManagement" },
    children: [
      {
        path: "notice",
        component: () => import("@/pages/patient-management/notice.vue"),
        name: "PatientManagementNotice",
        meta: { title: "通知单管理", tagTitle: "患者-通知单管理", keepAlive: true, permission: "patientManagement:notice" }
      },
      {
        path: "first-visit",
        component: () => import("@/pages/patient-management/first-visit.vue"),
        name: "PatientManagementFirstVisit",
        meta: { title: "首次随访", tagTitle: "患者-首次随访", keepAlive: true, permission: "patientManagement:firstVisit" }
      },
      {
        path: "follow-up",
        component: () => import("@/pages/patient-management/follow-up.vue"),
        name: "PatientManagementFollowUp",
        meta: { title: "后续随访", tagTitle: "患者-后续随访", keepAlive: true, permission: "patientManagement:followUp" }
      },
      {
        path: "medication",
        component: () => import("@/pages/patient-management/medication.vue"),
        name: "PatientManagementMedication",
        meta: { title: "服药管理", tagTitle: "患者-服药管理", keepAlive: true, permission: "patientManagement:medication" }
      },
      {
        path: "special-disease",
        component: () => import("@/pages/patient-management/special-disease.vue"),
        name: "PatientManagementSpecialDisease",
        meta: { title: "专病网导入", tagTitle: "患者-专病网导入", keepAlive: true, permission: "patientManagement:specialDisease" }
      },
      {
        path: "history",
        component: () => import("@/pages/patient-management/history.vue"),
        name: "PatientManagementHistory",
        meta: { title: "历史患者", tagTitle: "患者-历史患者", keepAlive: true, permission: "patientManagement:history" }
      }
    ]
  },

  // ==================== V17 新增：推介追踪管理（推介 + 追踪两个子菜单） ====================
  {
    path: "/referral-management",
    component: Layouts,
    redirect: "/referral-management/recommend",
    name: "ReferralManagement",
    meta: { title: "推介追踪管理", elIcon: "Share", permission: "referralManagement" },
    children: [
      {
        path: "recommend",
        component: () => import("@/pages/referral-management/recommend/index.vue"),
        name: "ReferralRecommend",
        meta: { title: "推介", tagTitle: "推介追踪-推介", keepAlive: true, permission: "referralManagement:recommend" }
      },
      {
        path: "track",
        component: () => import("@/pages/referral-management/track/index.vue"),
        name: "ReferralTrack",
        meta: { title: "追踪", tagTitle: "推介追踪-追踪", keepAlive: true, permission: "referralManagement:track" }
      }
    ]
  },

  // ==================== 密接人群管理（保留独立主线，去掉患者管理/历史患者子菜单，确诊患者汇入聚合患者管理） ====================
  {
    path: "/close-contact",
    component: Layouts,
    redirect: "/close-contact/screening",
    name: "CloseContact",
    meta: { title: "密接人群管理", elIcon: "Connection", permission: "closeContact" },
    children: [
      {
        path: "screening",
        component: () => import("@/pages/close-contact/screening/index.vue"),
        name: "CloseContactScreening",
        meta: { title: "筛查管理", tagTitle: "密接-筛查管理", keepAlive: true, permission: "closeContact:screening" }
      },
      {
        path: "latent",
        component: () => import("@/pages/close-contact/latent/index.vue"),
        name: "CloseContactLatent",
        meta: { title: "潜伏感染", tagTitle: "密接-潜伏感染", keepAlive: true, permission: "closeContact:latent" }
      },
      {
        path: "monitoring",
        component: () => import("@/pages/close-contact/suspected/index.vue"),
        name: "CloseContactMonitoring",
        meta: { title: "监测随访", tagTitle: "密接-监测随访", keepAlive: true, permission: "closeContact:followUp" }
      }
    ]
  },

  // ==================== 旧路由保留（向后兼容，菜单隐藏）====================
  // 保留 /school、/key-population 旧路径以防历史 URL 直链失效
  {
    path: "/school",
    component: Layouts,
    redirect: "/school/screening",
    name: "School",
    meta: { hidden: true, title: "学校人群（旧）", permission: "school" },
    children: [
      { path: "screening", component: () => import("@/pages/school/screening/index.vue"), name: "SchoolScreening", meta: { hidden: true, title: "筛查管理", permission: "school:screening" } },
      { path: "suspected", component: () => import("@/pages/school/suspected/index.vue"), name: "SchoolSuspected", meta: { hidden: true, title: "待诊断", permission: "school:suspected" } },
      { path: "latent", component: () => import("@/pages/school/latent/index.vue"), name: "SchoolLatent", meta: { hidden: true, title: "潜伏感染", permission: "school:latent" } },
      { path: "patient", component: () => import("@/pages/school/patient/index.vue"), name: "SchoolPatient", meta: { hidden: true, title: "患者管理", permission: "school:patient" } },
      { path: "patient/history", component: () => import("@/pages/school/patient/history.vue"), name: "SchoolPatientHistory", meta: { hidden: true, title: "历史患者", permission: "school:history" } }
    ]
  },
  {
    path: "/key-population",
    component: Layouts,
    redirect: "/key-population/screening",
    name: "KeyPopulation",
    meta: { hidden: true, title: "重点人群（旧）", permission: "keyPopulation" },
    children: [
      { path: "screening", component: () => import("@/pages/key-population/screening/index.vue"), name: "KeyPopulationScreening", meta: { hidden: true, title: "筛查管理", permission: "keyPopulation:screening" } },
      { path: "suspected", component: () => import("@/pages/key-population/suspected/index.vue"), name: "KeyPopulationSuspected", meta: { hidden: true, title: "待诊断", permission: "keyPopulation:suspected" } },
      { path: "latent", component: () => import("@/pages/key-population/latent/index.vue"), name: "KeyPopulationLatent", meta: { hidden: true, title: "潜伏感染", permission: "keyPopulation:latent" } },
      { path: "patient", component: () => import("@/pages/key-population/patient/index.vue"), name: "KeyPopulationPatient", meta: { hidden: true, title: "患者管理", permission: "keyPopulation:patient" } },
      { path: "patient/history", component: () => import("@/pages/key-population/patient/history.vue"), name: "KeyPopulationPatientHistory", meta: { hidden: true, title: "历史患者", permission: "keyPopulation:history" } }
    ]
  },

  // ==================== 统计分析 ====================
  {
    path: "/statistics",
    component: Layouts,
    meta: { permission: "statistics" },
    children: [
      {
        path: "",
        component: () => import("@/pages/statistics/index.vue"),
        name: "Statistics",
        meta: { title: "统计分析", elIcon: "DataAnalysis", permission: "statistics" }
      }
    ]
  },
  // ==================== 系统消息 ====================
  {
    path: "/message",
    component: Layouts,
    meta: { permission: "message" },
    children: [
      {
        path: "",
        component: () => import("@/pages/message/index.vue"),
        name: "Message",
        meta: { title: "系统消息", elIcon: "Bell", permission: "message" }
      }
    ]
  },
  // ==================== 数据清洗 ====================
  {
    path: "/data-cleaning",
    component: Layouts,
    meta: { permission: "dataCleaning" },
    children: [
      {
        path: "",
        component: () => import("@/pages/data-cleaning/index.vue"),
        name: "DataCleaning",
        meta: { title: "数据清洗", elIcon: "MagicStick", permission: "dataCleaning" }
      }
    ]
  }
]

/**
 * @name 动态路由
 * @description 用来放置有权限 (Roles 属性) 的路由
 * @description 必须带有唯一的 Name 属性
 */
export const dynamicRoutes: RouteRecordRaw[] = [
  {
    path: "/system",
    component: Layouts,
    name: "System",
    meta: {
      title: "系统管理",
      elIcon: "Setting",
      // roles 限制移至各子路由，父路由仅通过 permission 码控制可见性
      // 一二级用户拥有 system 权限后可见此菜单（仅展示其有权访问的子项）
      permission: "system"
    },
    children: [
      {
        path: "users",
        component: () => import("@/pages/system/users.vue"),
        name: "SystemUsers",
        meta: { title: "用户管理", roles: ["admin"], permission: "system:users" }
      },
      {
        path: "department",
        component: () => import("@/pages/system/department/index.vue"),
        name: "SystemDepartment",
        meta: { title: "部门管理", roles: ["admin"], permission: "system:department" }
      },
      {
        path: "permissions",
        component: () => import("@/pages/system/permissions.vue"),
        name: "SystemPermissions",
        // 不设 roles 限制，由 permission 码控制：一二级用户拥有 system:permissions 权限即可访问
        meta: { title: "权限管理", permission: "system:permissions" }
      },
      {
        path: "backup",
        component: () => import("@/pages/system/backup.vue"),
        name: "SystemBackup",
        meta: { title: "数据备份", roles: ["admin"], permission: "system:backup" }
      },
      {
        path: "operation-log",
        component: () => import("@/pages/system/operation-log.vue"),
        name: "SystemOperationLog",
        // 不设 roles，由 permission 码控制可见性（一/二级用户也可访问）
        meta: { title: "操作日志", permission: "system:operationLog" }
      }
    ]
  }
]

/** 路由实例 */
export const router = createRouter({
  history: routerConfig.history,
  routes: routerConfig.thirdLevelRouteCache ? flatMultiLevelRoutes(constantRoutes) : constantRoutes
})

/** 重置路由 */
export function resetRouter() {
  try {
    // 注意：所有动态路由路由必须带有 Name 属性，否则可能会不能完全重置干净
    router.getRoutes().forEach((route) => {
      const { name, meta } = route
      if (name && meta.roles?.length) {
        router.hasRoute(name) && router.removeRoute(name)
      }
    })
  } catch {
    // 强制刷新浏览器也行，只是交互体验不是很好
    location.reload()
  }
}

// 注册路由导航守卫
registerNavigationGuard(router)
