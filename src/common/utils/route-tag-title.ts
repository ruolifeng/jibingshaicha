import type { TagView } from "@/pinia/stores/tags-view"

/** 父级路由在标签栏中的简称（去掉「管理」等后缀） */
const PARENT_TAG_PREFIX: Record<string, string> = {
  Screening: "筛查",
  LatentManagement: "潜伏",
  PatientManagement: "患者",
  ReferralManagement: "推介追踪",
  CloseContact: "密接",
  System: "系统"
}

function stripManagementSuffix(title: string): string {
  return title.replace(/管理$/, "") || title
}

function getParentTagPrefix(parentName: string | symbol | null | undefined, parentTitle: string): string {
  if (typeof parentName === "string" && PARENT_TAG_PREFIX[parentName]) {
    return PARENT_TAG_PREFIX[parentName]
  }
  return stripManagementSuffix(parentTitle)
}

/** 标签标题解析所需的最小路由结构（兼容 normalized / resolved / 测试 mock） */
export interface RouteTagTitleMatch {
  meta?: {
    title?: string
    hidden?: boolean
    tagTitle?: string
    tagTitlePrefix?: string
  }
  name?: string | symbol | null
}

export interface RouteTagTitleSource {
  meta?: RouteTagTitleMatch["meta"] & Record<string, unknown>
  matched?: RouteTagTitleMatch[]
}

/**
 * 解析标签页标题：父菜单-子菜单
 * - 已配置且含「-」的 tagTitle 直接使用（兼容历史缩写）
 * - 否则根据 matched 自动拼接父级与当前页 title
 */
export function resolveRouteTagTitle(route: RouteTagTitleSource): string {
  const explicit = route.meta?.tagTitle
  if (explicit?.includes("-")) {
    return explicit
  }

  const titledMatches = route.matched?.filter(
    m => m.meta?.title && !m.meta?.hidden
  ) ?? []

  if (titledMatches.length === 0) {
    return explicit ?? route.meta?.title ?? ""
  }

  if (titledMatches.length === 1) {
    return explicit ?? titledMatches[0].meta!.title!
  }

  const parentRecord = titledMatches[titledMatches.length - 2]
  const childRecord = titledMatches[titledMatches.length - 1]
  const parentTitle = parentRecord.meta!.title!
  const childTitle = childRecord.meta!.title!

  if (parentTitle === childTitle) {
    return explicit ?? childTitle
  }

  const prefixMeta = parentRecord.meta?.tagTitlePrefix
  const prefix = prefixMeta ?? getParentTagPrefix(parentRecord.name, parentTitle)
  return `${prefix}-${childTitle}`
}

/** 写入标签页展示标题 */
export function withResolvedTagTitle(route: RouteTagTitleSource): TagView {
  const tagTitle = resolveRouteTagTitle(route)
  return {
    ...(route as TagView),
    meta: {
      ...route.meta,
      tagTitle
    }
  } as TagView
}
