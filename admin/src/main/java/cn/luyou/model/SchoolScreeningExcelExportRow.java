package cn.luyou.model;

import cn.luyou.utils.ExcelTextStringConverter;
import cn.luyou.utils.FlexibleIntegerConverter;
import cn.luyou.utils.FlexibleLocalDateConverter;
import cn.luyou.utils.SchoolScreeningCodeSupport;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 学生筛查 Excel 导出行（官方数字码列；末尾追加录入用户/录入时间）。
 * 对齐《2026年秋季新生入学结核病筛查记录表新》。
 */
@Data
public class SchoolScreeningExcelExportRow {

    private static final DateTimeFormatter CREATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExcelProperty(index = 0)
    private String reportingOrg;

    @ExcelProperty(index = 1)
    private String city;

    @ExcelProperty(index = 2)
    private String district;

    @ExcelProperty(index = 3)
    private String township;

    @ExcelProperty(index = 4)
    private String schoolType;

    @ExcelProperty(index = 5)
    private String boardingType;

    @ExcelProperty(index = 6)
    private String schoolName;

    @ExcelProperty(index = 7)
    private String name;

    @ExcelProperty(index = 8)
    private String year;

    @ExcelProperty(index = 9)
    private String gender;

    @ExcelProperty(index = 10, converter = ExcelTextStringConverter.class)
    private String idNumber;

    @ExcelProperty(index = 11, converter = FlexibleIntegerConverter.class)
    private Integer age;

    @ExcelProperty(index = 12)
    private String householdAddress;

    @ExcelProperty(index = 13)
    private String gradeName;

    @ExcelProperty(index = 14)
    private String className;

    @ExcelProperty(index = 15)
    private String ethnicity;

    @ExcelProperty(index = 16)
    private String participatedScreening;

    @ExcelProperty(index = 17)
    private String tbHistory;

    @ExcelProperty(index = 18)
    private String closeContactHistory;

    @ExcelProperty(index = 19)
    private String symptomCough;

    @ExcelProperty(index = 20)
    private String symptomHemoptysis;

    @ExcelProperty(index = 21)
    private String symptomOther;

    @ExcelProperty(index = 22, converter = FlexibleLocalDateConverter.class)
    private LocalDate screenDate;

    @ExcelProperty(index = 23)
    private String screenMethod;

    @ExcelProperty(index = 24)
    private String screenResult;

    @ExcelProperty(index = 25)
    private String infectionResult;

    @ExcelProperty(index = 26, converter = FlexibleLocalDateConverter.class)
    private LocalDate chestXrayDate;

    @ExcelProperty(index = 27)
    private String chestXrayMethod;

    @ExcelProperty(index = 28)
    private String chestXrayResult;

    @ExcelProperty(index = 29)
    private String molecularBiologyResult;

    @ExcelProperty(index = 30)
    private String sputumCultureResult;

    @ExcelProperty(index = 31)
    private String diagnosisFirst;

    @ExcelProperty(index = 32)
    private String remark;

    @ExcelProperty(index = 33)
    private String creatorUsername;

    @ExcelProperty(index = 34)
    private String createTime;

    public static SchoolScreeningExcelExportRow from(ScreeningSchool source) {
        SchoolScreeningExcelExportRow row = new SchoolScreeningExcelExportRow();
        row.setReportingOrg(source.getReportingOrg());
        row.setCity(source.getCity());
        row.setDistrict(source.getDistrict());
        row.setTownship(source.getTownship());
        row.setSchoolType(SchoolScreeningCodeSupport.fromSchoolType(source.getSchoolType()));
        row.setBoardingType(SchoolScreeningCodeSupport.fromBoardingType(source.getBoardingType()));
        row.setSchoolName(source.getSchoolName());
        row.setName(source.getName());
        row.setYear(source.getYear());
        row.setGender(source.getGender());
        row.setIdNumber(source.getIdNumber());
        row.setAge(source.getAge());
        row.setHouseholdAddress(source.getHouseholdAddress());
        row.setGradeName(source.getGradeName());
        row.setClassName(source.getClassName());
        row.setEthnicity(source.getEthnicity());
        row.setParticipatedScreening(source.getParticipatedScreening());
        row.setTbHistory(source.getTbHistory());
        row.setCloseContactHistory(source.getCloseContactHistory());
        row.setSymptomCough(source.getSymptomCough());
        row.setSymptomHemoptysis(source.getSymptomHemoptysis());
        row.setSymptomOther(source.getSymptomOther());
        row.setScreenDate(source.getScreenDate());
        row.setScreenMethod(SchoolScreeningCodeSupport.fromScreenMethod(source.getScreenMethod()));
        row.setScreenResult(source.getScreenResult());
        row.setInfectionResult(SchoolScreeningCodeSupport.fromInfectionResult(source.getInfectionResult()));
        row.setChestXrayDate(source.getChestXrayDate());
        row.setChestXrayMethod(SchoolScreeningCodeSupport.fromChestXrayMethod(source.getChestXrayMethod()));
        row.setChestXrayResult(SchoolScreeningCodeSupport.fromChestXrayResult(source.getChestXrayResult()));
        row.setMolecularBiologyResult(SchoolScreeningCodeSupport.fromLabResult(source.getMolecularBiologyResult()));
        row.setSputumCultureResult(SchoolScreeningCodeSupport.fromLabResult(source.getSputumCultureResult()));
        row.setDiagnosisFirst(SchoolScreeningCodeSupport.fromDiagnosis(source.getDiagnosisFirst()));
        row.setRemark(source.getRemark());
        row.setCreatorUsername(source.getCreatorUsername());
        row.setCreateTime(formatCreateTime(source.getCreateTime()));
        return row;
    }

    private static String formatCreateTime(LocalDateTime time) {
        return time == null ? null : time.format(CREATE_TIME_FMT);
    }
}
