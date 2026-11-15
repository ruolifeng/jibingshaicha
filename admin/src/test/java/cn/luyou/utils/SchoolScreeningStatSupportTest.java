package cn.luyou.utils;

import cn.luyou.model.ScreeningSchool;
import cn.luyou.utils.SchoolScreeningStatSupport.NonFreshmanGradeCategory;
import cn.luyou.utils.SchoolScreeningStatSupport.ReportCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolScreeningStatSupportTest {

    @Test
    void resolveReportCategoryNormalizesSchoolTypeAndBoardingCodes() {
        ScreeningSchool juniorNonBoarding = ScreeningSchool.builder()
                .schoolType("3")
                .boardingType("2")
                .build();
        assertEquals(ReportCategory.NON_BOARDING_JUNIOR,
                SchoolScreeningStatSupport.resolveReportCategory(juniorNonBoarding));

        ScreeningSchool juniorBoarding = ScreeningSchool.builder()
                .schoolType("3=初中")
                .boardingType("1=寄宿制")
                .build();
        assertEquals(ReportCategory.HIGH_AND_BOARDING_JUNIOR,
                SchoolScreeningStatSupport.resolveReportCategory(juniorBoarding));

        ScreeningSchool staff = ScreeningSchool.builder()
                .schoolType("6")
                .boardingType("4")
                .build();
        assertEquals(ReportCategory.STAFF_OTHER,
                SchoolScreeningStatSupport.resolveReportCategory(staff));
    }

    @Test
    void resolveNonFreshmanGradeCategoryByGradeName() {
        assertEquals(NonFreshmanGradeCategory.JUNIOR_2,
                SchoolScreeningStatSupport.resolveNonFreshmanGradeCategory(
                        ScreeningSchool.builder().gradeName("初二").build()));
        assertEquals(NonFreshmanGradeCategory.JUNIOR_2,
                SchoolScreeningStatSupport.resolveNonFreshmanGradeCategory(
                        ScreeningSchool.builder().gradeName("初二年级").build()));
        assertEquals(NonFreshmanGradeCategory.JUNIOR_3,
                SchoolScreeningStatSupport.resolveNonFreshmanGradeCategory(
                        ScreeningSchool.builder().gradeName("初中初三").build()));
        assertEquals(NonFreshmanGradeCategory.SENIOR_2,
                SchoolScreeningStatSupport.resolveNonFreshmanGradeCategory(
                        ScreeningSchool.builder().gradeName("高二").build()));
        assertEquals(NonFreshmanGradeCategory.SENIOR_3,
                SchoolScreeningStatSupport.resolveNonFreshmanGradeCategory(
                        ScreeningSchool.builder().gradeName("高三").build()));
        assertNull(SchoolScreeningStatSupport.resolveNonFreshmanGradeCategory(
                ScreeningSchool.builder().gradeName("初一").build()));
        // 避免 contains 误匹配：同时出现初二/初三字样时不应误入初二
        assertNull(SchoolScreeningStatSupport.resolveNonFreshmanGradeCategory(
                ScreeningSchool.builder().gradeName("初三补初二").build()));
    }

    @Test
    void nonFreshmanStandardizedExamRequiresSymptomAndInfection() {
        ScreeningSchool nonInfectionOk = ScreeningSchool.builder()
                .symptomCough("无")
                .infectionResult("未感染")
                .build();
        assertTrue(SchoolScreeningStatSupport.isNonFreshmanStandardizedExamined(nonInfectionOk));

        ScreeningSchool infectionNeedsChest = ScreeningSchool.builder()
                .symptomCough("有")
                .infectionResult("感染")
                .build();
        assertFalse(SchoolScreeningStatSupport.isNonFreshmanStandardizedExamined(infectionNeedsChest));

        ScreeningSchool infectionWithChest = ScreeningSchool.builder()
                .symptomCough("有")
                .infectionResult("感染")
                .chestXrayResult("未见异常")
                .build();
        assertTrue(SchoolScreeningStatSupport.isNonFreshmanStandardizedExamined(infectionWithChest));
    }
}
