import type { RouteTagTitleMatch, RouteTagTitleSource } from "@@/utils/route-tag-title"
import { resolveRouteTagTitle } from "@@/utils/route-tag-title"
import { describe, expect, it } from "vitest"

function mockRoute(
  meta: RouteTagTitleSource["meta"],
  matched: RouteTagTitleMatch[]
): RouteTagTitleSource {
  return {
    meta,
    matched
  }
}

describe("resolveRouteTagTitle", () => {
  it("单级菜单直接显示标题", () => {
    const route = mockRoute({ title: "首页" }, [{ meta: { title: "首页" } }])
    expect(resolveRouteTagTitle(route)).toBe("首页")
  })

  it("筛查管理子路由自动拼接父级前缀", () => {
    const route = mockRoute(
      { title: "学生人群" },
      [
        { name: "Screening", meta: { title: "筛查管理" } },
        { name: "ScreeningStudent", meta: { title: "学生人群" } }
      ]
    )
    expect(resolveRouteTagTitle(route)).toBe("筛查-学生人群")
  })

  it("已配置带连字符的 tagTitle 优先使用", () => {
    const route = mockRoute(
      { title: "潜伏感染者在管总览", tagTitle: "潜伏感染者在管总览" },
      [
        { name: "LatentManagement", meta: { title: "潜伏感染者管理" } },
        { meta: { title: "潜伏感染者在管总览" } }
      ]
    )
    expect(resolveRouteTagTitle(route)).toBe("潜伏感染者在管总览")
  })

  it("系统管理子路由自动拼接", () => {
    const route = mockRoute(
      { title: "用户管理" },
      [
        { name: "System", meta: { title: "系统管理" } },
        { name: "SystemUsers", meta: { title: "用户管理" } }
      ]
    )
    expect(resolveRouteTagTitle(route)).toBe("系统-用户管理")
  })

  it("密接路由使用配置的缩写 tagTitle", () => {
    const route = mockRoute(
      { title: "密接个案表", tagTitle: "密接-个案表" },
      [
        { name: "CloseContact", meta: { title: "密接人群管理" } },
        { meta: { title: "密接个案表" } }
      ]
    )
    expect(resolveRouteTagTitle(route)).toBe("密接-个案表")
  })

  it("无 matched 时回退到 meta.title", () => {
    const route = { meta: { title: "统计分析" }, matched: [] }
    expect(resolveRouteTagTitle(route)).toBe("统计分析")
  })
})
