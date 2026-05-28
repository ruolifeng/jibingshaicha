import type { RouteLocationGeneric } from "vue-router"
import { useUserStore } from "@/pinia/stores/user"

const SCREENING_ENTRIES = [
  { path: "/screening/student", perms: ["school:screening", "school:suspected"] },
  { path: "/screening/key-population", perms: ["keyPopulation:screening", "keyPopulation:suspected"] },
  { path: "/screening/regular", perms: ["regular:screening", "regular:suspected"] }
] as const

const SOURCE_PATH_MAP: Record<string, string> = {
  school: "/screening/student",
  keyPopulation: "/screening/key-population",
  regular: "/screening/regular"
}

/** 按权限返回首个可访问的筛查子菜单路径 */
export function resolveScreeningDefaultPath(): string {
  const userStore = useUserStore()
  const found = SCREENING_ENTRIES.find(entry =>
    entry.perms.some(code => userStore.hasPermission(code))
  )
  return found?.path ?? "/screening/student"
}

const CLOSE_CONTACT_ENTRIES = [
  { path: "/close-contact/case", perms: ["closeContact:case"] },
  { path: "/close-contact/screening", perms: ["closeContact:screening"] },
  { path: "/close-contact/latent", perms: ["closeContact:latent"] },
  { path: "/close-contact/monitoring", perms: ["closeContact:followUp"] }
] as const

/** 按权限返回首个可访问的密接人群子菜单（优先个案表） */
export function resolveCloseContactDefaultPath(): string {
  const userStore = useUserStore()
  const found = CLOSE_CONTACT_ENTRIES.find(entry =>
    entry.perms.some(code => userStore.hasPermission(code))
  )
  return found?.path ?? "/close-contact/case"
}

/** 兼容旧「人群筛查」聚合页 URL（含 source / view 参数） */
export function resolvePopulationLegacyRedirect(to: RouteLocationGeneric) {
  const source = to.query.source as string
  const view = to.query.view as string
  const path = SOURCE_PATH_MAP[source] ?? resolveScreeningDefaultPath()
  if (view === "screening" || view === "suspected") {
    return { path, query: { view } }
  }
  return path
}
