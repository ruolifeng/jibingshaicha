package cn.luyou.model;

import cn.luyou.utils.ExcelTextStringConverter;
import cn.luyou.utils.FlexibleIntegerConverter;
import cn.luyou.utils.FlexibleLocalDateConverter;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 学生/学校人群筛查 Excel 导出行（39 列：业务列与导入模板对齐，末尾追加录入用户/录入时间）。
 */
@Data
public class SchoolScreeningExcelExportRow {

    private static final DateTimeFormatter CREATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExcelProperty(index = 0, converter = FlexibleIntegerConverter.class)
    private Integer seq;

    @ExcelProperty(index = 1)
    private String year;

    @ExcelProperty(index = 2)
    private String city;

    @ExcelProperty(index = 3)
    private String district;

    @ExcelProperty(index = 4)
    private String name;

    @ExcelProperty(index = 5)
    private String gender;

    @ExcelProperty(index = 6, converter = FlexibleLocalDateConverter.class)
    private LocalDate birthDate;

    @ExcelProperty(index = 7, converter = FlexibleIntegerConverter.class)
    private Integer age;

    @ExcelProperty(index = 8)
    private String idType;

    @ExcelProperty(index = 9, converter = ExcelTextStringConverter.class)
    private String idNumber;

    @ExcelProperty(index = 10)
    private String ethnicity;

    @ExcelProperty(index = 11, converter = ExcelTextStringConverter.class)
    private String phone;

    @ExcelProperty(index = 12)
    private String householdAddress;

    @ExcelProperty(index = 13)
    private String currentAddress;

    @ExcelProperty(index = 14)
    private String schoolType;

    @ExcelProperty(index = 15)
    private String schoolName;

    @ExcelProperty(index = 16)
    private String className;

    @ExcelProperty(index = 17)
    private String tbHistory;

    @ExcelProperty(index = 18)
    private String closeContactHistory;

    @ExcelProperty(index = 19)
    private String suspiciousSymptoms;

    @ExcelProperty(index = 20)
    private String hasInfectionScreen;

    @ExcelProperty(index = 21, converter = FlexibleLocalDateConverter.class)
    private LocalDate screenDate;

    @ExcelProperty(index = 22)
    private String screenMethod;

    @ExcelProperty(index = 23)
    private String screenResult;

    @ExcelProperty(index = 24)
    private String infectionResult;

    @ExcelProperty(index = 25)
    private String hasChestXray;

    @ExcelProperty(index = 26, converter = FlexibleLocalDateConverter.class)
    private LocalDate chestXrayDate;

    @ExcelProperty(index = 27)
    private String chestXrayResult;

    @ExcelProperty(index = 28)
    private String sputumSmearResult;

    @ExcelProperty(index = 29)
    private String molecularBiologyResult;

    @ExcelProperty(index = 30)
    private String diagnosisFirst;

    @ExcelProperty(index = 31)
    private String hasPreventiveTreatment;

    @ExcelProperty(index = 32)
    private String preventivePlan;

    @ExcelProperty(index = 33, converter = FlexibleLocalDateConverter.class)
    private LocalDate preventiveStartDate;

    @ExcelProperty(index = 34, converter = FlexibleLocalDateConverter.class)
    private LocalDate preventiveEndDate;

    @ExcelProperty(index = 35)
    private String preventiveResult;

    @ExcelProperty(index = 36)
    private String preventiveManager;

    @ExcelProperty(index = 37)
    private String creatorUsername;

    @ExcelProperty(index = 38)
    private String createTime;

    public static SchoolScreeningExcelExportRow from(ScreeningSchool source, int seq) {
        SchoolScreeningExcelExportRow row = new SchoolScreeningExcelExportRow();
        row.setSeq(seq);
        row.setYear(source.getYear());
        row.setCity(source.getCity());
        row.setDistrict(source.getDistrict());
        row.setName(source.getName());
        row.setGender(source.getGender());
        row.setBirthDate(source.getBirthDate());
        row.setAge(source.getAge());
        row.setIdType(source.getIdType());
        row.setIdNumber(source.getIdNumber());
        row.setEthnicity(source.getEthnicity());
        row.setPhone(source.getPhone());
        row.setHouseholdAddress(source.getHouseholdAddress());
        row.setCurrentAddress(source.getCurrentAddress());
        row.setSchoolType(source.getSchoolType());
        row.setSchoolName(source.getSchoolName());
        row.setClassName(source.getClassName());
        row.setTbHistory(source.getTbHistory());
        row.setCloseContactHistory(source.getCloseContactHistory());
        row.setSuspiciousSymptoms(source.getSuspiciousSymptoms());
        row.setHasInfectionScreen(source.getHasInfectionScreen());
        row.setScreenDate(source.getScreenDate());
        row.setScreenMethod(source.getScreenMethod());
        row.setScreenResult(source.getScreenResult());
        row.setInfectionResult(source.getInfectionResult());
        row.setHasChestXray(source.getHasChestXray());
        row.setChestXrayDate(source.getChestXrayDate());
        row.setChestXrayResult(source.getChestXrayResult());
        row.setSputumSmearResult(source.getSputumSmearResult());
        row.setMolecularBiologyResult(source.getMolecularBiologyResult());
        row.setDiagnosisFirst(source.getDiagnosisFirst());
        row.setHasPreventiveTreatment(source.getHasPreventiveTreatment());
        row.setPreventivePlan(source.getPreventivePlan());
        row.setPreventiveStartDate(source.getPreventiveStartDate());
        row.setPreventiveEndDate(source.getPreventiveEndDate());
        row.setPreventiveResult(source.getPreventiveResult());
        row.setPreventiveManager(source.getPreventiveManager());
        row.setCreatorUsername(source.getCreatorUsername());
        row.setCreateTime(formatCreateTime(source.getCreateTime()));
        return row;
    }

    private static String formatCreateTime(LocalDateTime time) {
        return time == null ? null : time.format(CREATE_TIME_FMT);
    }
}
