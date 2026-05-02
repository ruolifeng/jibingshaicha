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
    meta: {
      hidden: true
    },
    children: [
      {
        path: ":path(.*)",
        component: () => import("@/pages/redirect/index.vue")
      }
    ]
  },
  {
    path: "/403",
    component: () => import("@/pages/error/403.vue"),
    meta: {
      hidden: true
    }
  },
  {
    path: "/404",
    component: () => import("@/pages/error/404.vue"),
    meta: {
      hidden: true
    },
    alias: "/:pathMatch(.*)*"
  },
  {
    path: "/login",
    component: () => import("@/pages/login/index.vue"),
    meta: {
      hidden: true
    }
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
        meta: {
          title: "首页",
          svgIcon: "dashboard",
          affix: true
        }
      }
    ]
  },
  {
    path: "/school",
    component: Layouts,
    redirect: "/school/screening",
    name: "School",
    meta: {
      title: "学校人群",
      elIcon: "School",
      permission: "school"
    },
    children: [
      {
        path: "screening",
        component: () => import("@/pages/school/screening/index.vue"),
        name: "SchoolScreening",
        meta: { title: "筛查管理", keepAlive: true, permission: "school:screening" }
      },
      {
        path: "suspected",
        component: () => import("@/pages/school/suspected/index.vue"),
        name: "SchoolSuspected",
        meta: { title: "待诊断", keepAlive: true, permission: "school:suspected" }
      },
      {
        path: "latent",
        component: () => import("@/pages/school/latent/index.vue"),
        name: "SchoolLatent",
        meta: { title: "潜伏感染", keepAlive: true, permission: "school:latent" }
      },
      {
        path: "patient",
        component: () => import("@/pages/school/patient/index.vue"),
        name: "SchoolPatient",
        meta: { title: "患者管理", keepAlive: true, permission: "school:patient" }
      },
      {
        path: "patient/history",
        component: () => import("@/pages/school/patient/history.vue"),
        name: "SchoolPatientHistory",
        meta: { title: "历史患者", keepAlive: true, permission: "school:history" }
      }
    ]
  },
  {
    path: "/key-population",
    component: Layouts,
    redirect: "/key-population/screening",
    name: "KeyPopulation",
    meta: {
      title: "重点人群",
      elIcon: "UserFilled",
      permission: "keyPopulation"
    },
    children: [
      {
        path: "screening",
        component: () => import("@/pages/key-population/screening/index.vue"),
        name: "KeyPopulationScreening",
        meta: { title: "筛查管理", keepAlive: true, permission: "keyPopulation:screening" }
      },
      {
        path: "suspected",
        component: () => import("@/pages/key-population/suspected/index.vue"),
        name: "KeyPopulationSuspected",
        meta: { title: "待诊断", keepAlive: true, permission: "keyPopulation:suspected" }
      },
      {
        path: "latent",
        component: () => import("@/pages/key-population/latent/index.vue"),
        name: "KeyPopulationLatent",
        meta: { title: "潜伏感染", keepAlive: true, permission: "keyPopulation:latent" }
      },
      {
        path: "patient",
        component: () => import("@/pages/key-population/patient/index.vue"),
        name: "KeyPopulationPatient",
        meta: { title: "患者管理", keepAlive: true, permission: "keyPopulation:patient" }
      },
      {
        path: "patient/history",
        component: () => import("@/pages/key-population/patient/history.vue"),
        name: "KeyPopulationPatientHistory",
        meta: { title: "历史患者", keepAlive: true, permission: "keyPopulation:history" }
      }
    ]
  },
  {
    path: "/close-contact",
    component: Layouts,
    redirect: "/close-contact/screening",
    name: "CloseContact",
    meta: {
      title: "密接人群",
      elIcon: "Connection",
      permission: "closeContact"
    },
    children: [
      {
        path: "screening",
        component: () => import("@/pages/close-contact/screening/index.vue"),
        name: "CloseContactScreening",
        meta: { title: "筛查管理", keepAlive: true, permission: "closeContact:screening" }
      },
      {
        path: "suspected",
        component: () => import("@/pages/close-contact/suspected/index.vue"),
        name: "CloseContactSuspected",
        meta: { title: "待诊断", keepAlive: true, permission: "closeContact:suspected" }
      },
      {
        path: "latent",
        component: () => import("@/pages/close-contact/latent/index.vue"),
        name: "CloseContactLatent",
        meta: { title: "潜伏感染", keepAlive: true, permission: "closeContact:latent" }
      },
      {
        path: "patient",
        component: () => import("@/pages/close-contact/patient/index.vue"),
        name: "CloseContactPatient",
        meta: { title: "患者管理", keepAlive: true, permission: "closeContact:patient" }
      },
      {
        path: "patient/history",
        component: () => import("@/pages/close-contact/patient/history.vue"),
        name: "CloseContactPatientHistory",
        meta: { title: "历史患者", keepAlive: true, permission: "closeContact:history" }
      }
    ]
  },
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
      roles: ["admin"],
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
        meta: { title: "权限管理", roles: ["admin"], permission: "system:permissions" }
      },
      {
        path: "backup",
        component: () => import("@/pages/system/backup.vue"),
        name: "SystemBackup",
        meta: { title: "数据备份", roles: ["admin"], permission: "system:backup" }
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
