package cn.luyou.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchoolScreeningCodeSupportTest {

    @Test
    void formatSchoolTypeUsesHintLabel() {
        assertEquals("1=托幼机构", SchoolScreeningCodeSupport.formatSchoolType("托幼机构"));
        assertEquals("1=托幼机构", SchoolScreeningCodeSupport.formatSchoolType("1"));
        assertEquals("1=托幼机构", SchoolScreeningCodeSupport.formatSchoolType("1=托幼机构"));
        assertEquals("7=其他（培训学校、特殊教育学校和专门学校等）",
                SchoolScreeningCodeSupport.formatSchoolType("其他"));
    }

    @Test
    void reimportCodeEqualsLabelStoresChinese() {
        assertEquals("托幼机构", SchoolScreeningCodeSupport.toSchoolType("1=托幼机构"));
        assertEquals("寄宿制", SchoolScreeningCodeSupport.toBoardingType("1=寄宿制"));
        assertEquals("PPD", SchoolScreeningCodeSupport.toScreenMethod("1=结核菌素纯蛋白衍生物（PPD）"));
        assertEquals("未感染", SchoolScreeningCodeSupport.toInfectionResult("0=未感染"));
        assertEquals("排除", SchoolScreeningCodeSupport.toDiagnosis("0=未发现异常"));
    }

    @Test
    void formatScreenMethodUsesFullHintName() {
        assertEquals("1=结核菌素纯蛋白衍生物（PPD）", SchoolScreeningCodeSupport.formatScreenMethod("PPD"));
        assertEquals("2=重组结核分枝杆菌融合蛋白（EC）", SchoolScreeningCodeSupport.formatScreenMethod("EC"));
        assertEquals("3=γ-干扰素释放试验（IGRA）",
                SchoolScreeningCodeSupport.formatScreenMethod("γ干扰素释放试验_IGRA"));
        assertEquals("4=未查", SchoolScreeningCodeSupport.formatScreenMethod("未做"));
    }

    @Test
    void formatDiagnosisUsesOfficialHint() {
        assertEquals("0=未发现异常", SchoolScreeningCodeSupport.formatDiagnosis("排除"));
        assertEquals("1=活动性肺结核", SchoolScreeningCodeSupport.formatDiagnosis("确诊患者"));
        assertEquals("2=疑似肺结核", SchoolScreeningCodeSupport.formatDiagnosis("疑似结核"));
        assertEquals("3=潜伏感染者", SchoolScreeningCodeSupport.formatDiagnosis("潜伏感染者"));
        assertEquals("4=其他（需注明）", SchoolScreeningCodeSupport.formatDiagnosis("其他"));
        assertEquals("4=其他（发热待查）", SchoolScreeningCodeSupport.formatDiagnosis("其他（发热待查）"));
    }

    @Test
    void formatOtherCodedFields() {
        assertEquals("1=寄宿制", SchoolScreeningCodeSupport.formatBoardingType("寄宿制"));
        assertEquals("1=感染", SchoolScreeningCodeSupport.formatInfectionResult("感染"));
        assertEquals("1=胸部X线", SchoolScreeningCodeSupport.formatChestXrayMethod("胸部X线"));
        assertEquals("3=其他（需注明）", SchoolScreeningCodeSupport.formatChestXrayMethod("其他"));
        assertEquals("0=未见异常", SchoolScreeningCodeSupport.formatChestXrayResult("正常"));
        assertEquals("1=异常（疑似活动性结核病变）", SchoolScreeningCodeSupport.formatChestXrayResult("异常"));
        assertEquals("0=阴性", SchoolScreeningCodeSupport.formatLabResult("阴性"));
        assertEquals("", SchoolScreeningCodeSupport.formatSchoolType(""));
        assertEquals("自定义", SchoolScreeningCodeSupport.formatSchoolType("自定义"));
    }

    @Test
    void symptomNotInquiredNormalizedAndSummarized() {
        assertEquals("未询问", SchoolScreeningCodeSupport.normalizeSymptomValue("未问"));
        assertEquals("未询问", SchoolScreeningCodeSupport.normalizeSymptomValue("未询问"));
        assertEquals("有", SchoolScreeningCodeSupport.normalizeSymptomValue("有"));
        assertEquals("未询问", SchoolScreeningCodeSupport.summarizeSuspiciousSymptoms("未询问", "未询问", "未询问"));
        assertEquals("无", SchoolScreeningCodeSupport.summarizeSuspiciousSymptoms("无", "未询问", ""));
        assertEquals("有", SchoolScreeningCodeSupport.summarizeSuspiciousSymptoms("有", "未询问", "无"));
        assertEquals(List.of("咳嗽咳痰≥两周", "其他"),
                SchoolScreeningCodeSupport.notInquiredSymptomFields("未问", "无", "未询问"));
    }
}
