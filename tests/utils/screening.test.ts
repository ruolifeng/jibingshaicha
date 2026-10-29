import { formatScreenResultDisplay } from "@@/utils/screening"
import { describe, expect, it } from "vitest"

describe("formatScreenResultDisplay", () => {
  it("学生筛查已带 (mm) 时不再追加 mm*mm", () => {
    expect(formatScreenResultDisplay("0 × 0 (mm)", "PPD")).toBe("0 × 0 (mm)")
    expect(formatScreenResultDisplay("0×0（mm）", "1")).toBe("0×0（mm）")
    expect(formatScreenResultDisplay("0×0 (mm)", "结核菌素纯蛋白衍生物（PPD）")).toBe("0×0 (mm)")
  })

  it("已带 mm*mm 时保持原样", () => {
    expect(formatScreenResultDisplay("3*6 (mm*mm)", "PPD")).toBe("3*6 (mm*mm)")
  })

  it("pPD 无单位测量值补充 mm*mm", () => {
    expect(formatScreenResultDisplay("0×0", "PPD")).toBe("0×0 (mm*mm)")
    expect(formatScreenResultDisplay("3*6", "PPD")).toBe("3*6 (mm*mm)")
  })

  it("非 PPD 结果不追加单位", () => {
    expect(formatScreenResultDisplay("阳性", "EC")).toBe("阳性")
    expect(formatScreenResultDisplay("无", "未查")).toBe("无")
  })
})
