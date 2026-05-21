import type { RouteLocationNormalizedGeneric, RouteRecordNameGeneric } from "vue-router"

/** 免登录白名单（匹配路由 path） */
const whiteListByPath: string[] = ["/login", "/questionnaire/school", "/questionnaire"]

/** 免登录白名单（匹配路由 name） */
const whiteListByName: RouteRecordNameGeneric[] = ["QuestionnaireFill"]

/** 判断是否在白名单 */
export function isWhiteList(to: RouteLocationNormalizedGeneric) {
  if (whiteListByPath.includes(to.path) || whiteListByName.includes(to.name)) {
    return true
  }
  return to.path.startsWith("/questionnaire/")
}
