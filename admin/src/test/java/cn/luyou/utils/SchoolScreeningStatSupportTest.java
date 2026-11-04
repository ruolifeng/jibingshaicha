package cn.luyou.utils;

import cn.luyou.model.ScreeningSchool;
import cn.luyou.utils.SchoolScreeningStatSupport.ReportCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
