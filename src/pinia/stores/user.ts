import { getCurrentUserApi } from "@@/apis/users"
import { setToken as _setToken, getToken, removeToken } from "@@/utils/cache/cookies"
import { pinia } from "@/pinia"
import { resetRouter } from "@/router"
import { routerConfig } from "@/router/config"
import { useSettingsStore } from "./settings"
import { useTagsViewStore } from "./tags-view"

export const useUserStore = defineStore("user", () => {
  const token = ref<string>(getToken() || "")

  const roles = ref<string[]>([])

  const username = ref<string>("")

  const realName = ref<string>("")

  const userRole = ref<number>(0)

  const roleName = ref<string>("")

  const orgName = ref<string>("")

  const departmentName = ref<string>("")

  const phone = ref<string>("")

  const avatar = ref<string>("")

  const userId = ref<string>("")

  const permissions = ref<string[]>([])

  const tagsViewStore = useTagsViewStore()

  const settingsStore = useSettingsStore()

  // 设置 Token
  const setToken = (value: string) => {
    _setToken(value)
    token.value = value
  }

  // 获取用户详情
  const getInfo = async () => {
    const { data } = await getCurrentUserApi()
    username.value = data.username
    realName.value = data.realName || ""
    userRole.value = data.role || 0
    roleName.value = data.roleName || ""
    orgName.value = data.orgName || ""
    departmentName.value = data.departmentName || ""
    phone.value = data.phone || ""
    avatar.value = data.avatar || ""
    userId.value = data.id != null ? String(data.id) : ""
    permissions.value = data.permissions || []
    roles.value = data.roles?.length > 0 ? data.roles : routerConfig.defaultRoles
  }

  // 模拟角色变化
  const changeRoles = (role: string) => {
    const newToken = `token-${role}`
    token.value = newToken
    _setToken(newToken)
    // 用刷新页面代替重新登录
    location.reload()
  }

  // 登出
  const logout = () => {
    removeToken()
    token.value = ""
    roles.value = []
    permissions.value = []
    userRole.value = 0
    avatar.value = ""
    resetRouter()
    resetTagsView()
  }

  // 重置 Token
  const resetToken = () => {
    removeToken()
    token.value = ""
    roles.value = []
    permissions.value = []
    userRole.value = 0
    avatar.value = ""
  }

  // 重置 Visited Views 和 Cached Views
  const resetTagsView = () => {
    if (!settingsStore.cacheTagsView) {
      tagsViewStore.delAllVisitedViews()
      tagsViewStore.delAllCachedViews()
    }
  }

  /** 检查当前用户是否拥有指定权限（兼容历史权限码别名） */
  const hasPermission = (code: string) => {
    if (userRole.value === 1) return true
    if (permissions.value.includes(code)) return true
    if (code === "dataCleaning" && permissions.value.includes("dataClean")) return true
    return false
  }

  return { token, roles, username, realName, userRole, roleName, orgName, departmentName, phone, avatar, userId, permissions, hasPermission, setToken, getInfo, changeRoles, logout, resetToken }
})

/**
 * @description 在 SPA 应用中可用于在 pinia 实例被激活前使用 store
 * @description 在 SSR 应用中可用于在 setup 外使用 store
 */
export function useUserStoreOutside() {
  return useUserStore(pinia)
}
