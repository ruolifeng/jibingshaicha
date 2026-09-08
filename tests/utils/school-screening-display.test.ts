import {
  formatSchoolBoardingTypeDisplay,
  formatSchoolChestMethodDisplay,
  formatSchoolChestResultDisplay,
  formatSchoolDiagnosisDisplay,
  formatSchoolInfectionJudgeDisplay,
  formatSchoolLabResultDisplay,
  formatSchoolScreenMethodDisplay,
  formatSchoolTypeDisplay,
  isSchoolSymptomNotInquired
} from "@@/constants/disease"
import { describe, expect, it } from "vitest"

describe("学生筛查填写说明展示", () => {
  it("类型显示数字=说明", () => {
    expect(formatSchoolTypeDisplay("托幼机构")).toBe("1=托幼机构")
    expect(formatSchoolTypeDisplay("1")).toBe("1=托幼机构")
    expect(formatSchoolTypeDisplay("1=托幼机构")).toBe("1=托幼机构")
    expect(formatSchoolTypeDisplay("其他")).toBe("7=其他（培训学校、特殊教育学校和专门学校等）")
  })

  it("感染筛查方法显示填写说明全称", () => {
    expect(formatSchoolScreenMethodDisplay("PPD")).toBe("1=结核菌素纯蛋白衍生物（PPD）")
    expect(formatSchoolScreenMethodDisplay("EC")).toBe("2=重组结核分枝杆菌融合蛋白（EC）")
    expect(formatSchoolScreenMethodDisplay("未查")).toBe("4=未查")
  })

  it("筛查结果显示官方说明文案", () => {
    expect(formatSchoolDiagnosisDisplay("排除")).toBe("0=未发现异常")
    expect(formatSchoolDiagnosisDisplay("确诊患者")).toBe("1=活动性肺结核")
    expect(formatSchoolDiagnosisDisplay("潜伏感染者")).toBe("3=潜伏感染者")
    expect(formatSchoolDiagnosisDisplay("其他（发热待查）")).toBe("4=其他（发热待查）")
  })

  it("其余码表字段", () => {
    expect(formatSchoolBoardingTypeDisplay("寄宿制")).toBe("1=寄宿制")
    expect(formatSchoolInfectionJudgeDisplay("未感染")).toBe("0=未感染")
    expect(formatSchoolChestMethodDisplay("胸部X线")).toBe("1=胸部X线")
    expect(formatSchoolChestResultDisplay("未见异常")).toBe("0=未见异常")
    expect(formatSchoolLabResultDisplay("阳性")).toBe("1=阳性")
    expect(formatSchoolTypeDisplay("")).toBe("")
    expect(formatSchoolTypeDisplay("自定义")).toBe("自定义")
  })
})

describe("学生筛查可疑症状未询问", () => {
  it("识别未询问别名", () => {
    expect(isSchoolSymptomNotInquired("未询问")).toBe(true)
    expect(isSchoolSymptomNotInquired("未问")).toBe(true)
    expect(isSchoolSymptomNotInquired("无")).toBe(false)
    expect(isSchoolSymptomNotInquired("")).toBe(false)
  })
})
