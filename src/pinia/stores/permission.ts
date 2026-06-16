import type { RouteRecordRaw } from "vue-router"
import { pinia } from "@/pinia"
import { constantRoutes, dynamicRoutes } from "@/router"
import { routerConfig } from "@/router/config"
import { flatMultiLevelRoutes } from "@/router/helper"
import { useUserStore } from "./user"

function hasPermission(roles: string[], route: RouteRecordRaw) {
  const routeRoles = route.meta?.roles
  if (routeRoles) {
    return roles.some(role => routeRoles.includes(role))
  }
  return true
}

/** 基于权限编码过滤路由 */
function hasMenuPermission(route: RouteRecordRaw): boolean {
  const userStore = useUserStore()
  const anyPerms = route.meta?.anyPermission
  if (anyPerms?.length) {
    return anyPerms.some(code => userStore.hasPermission(code))
  }
  const permCode = route.meta?.permission as string | undefined
  if (!permCode) return true
  return userStore.hasPermission(permCode)
}

function filterRoutes(routes: RouteRecordRaw[], roles: string[]) {
  const res: RouteRecordRaw[] = []
  routes.forEach((route) => {
    const tempRoute = { ...route }
    if (hasPermission(roles, tempRoute) && hasMenuPermission(tempRoute)) {
      if (tempRoute.children) {
        tempRoute.children = filterRoutes(tempRoute.children, roles)
        // 父级菜单下无任何可见子项时不展示（避免空「系统管理」）
        if (tempRoute.children.length === 0) {
          return
        }
      }
      res.push(tempRoute)
    }
  })
  return res
}

export const usePermissionStore = defineStore("permission", () => {
  const routes = ref<RouteRecordRaw[]>([])
  const addRoutes = ref<RouteRecordRaw[]>([])

  const setRoutes = (roles: string[]) => {
    const filteredConstant = filterRoutes(constantRoutes, roles)
    const filteredDynamic = filterRoutes(dynamicRoutes, roles)
    routes.value = filteredConstant.concat(filteredDynamic)
    addRoutes.value = routerConfig.thirdLevelRouteCache ? flatMultiLevelRoutes(filteredDynamic) : filteredDynamic
  }

  const setAllRoutes = () => {
    routes.value = constantRoutes.concat(dynamicRoutes)
    addRoutes.value = routerConfig.thirdLevelRouteCache ? flatMultiLevelRoutes(dynamicRoutes) : dynamicRoutes
  }

  return { routes, addRoutes, setRoutes, setAllRoutes }
})

export function usePermissionStoreOutside() {
  return usePermissionStore(pinia)
}
