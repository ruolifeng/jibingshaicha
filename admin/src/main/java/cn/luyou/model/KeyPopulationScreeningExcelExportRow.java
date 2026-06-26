package cn.luyou.model;

import cn.luyou.utils.ExcelTextStringConverter;
import cn.luyou.utils.FlexibleIntegerConverter;
import cn.luyou.utils.FlexibleLocalDateConverter;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 重点人群筛查 Excel 导出行（49 列，与官方模板列序一致，仅用于导出/模板下载）。
 */
@Data
public class KeyPopulationScreeningExcelExportRow {

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
    private String townshipCommunity;

    @ExcelProperty(index = 14)
    private String currentAddress;

    @ExcelProperty(index = 15)
    private String crowdCategoryClose;

    @ExcelProperty(index = 16)
    private String crowdCategoryStudent;

    @ExcelProperty(index = 17)
    private String crowdCategoryTeacher;

    @ExcelProperty(index = 18)
    private String crowdCategoryElder;

    @ExcelProperty(index = 19)
    private String crowdCategoryDiabetes;

    @ExcelProperty(index = 20)
    private String crowdCategoryDual;

    @ExcelProperty(index = 21)
    private String crowdCategoryTbHist;

    @ExcelProperty(index = 22)
    private String crowdCategoryNormal;

    @ExcelProperty(index = 23)
    private String hasSuspiciousSymptoms;

    @ExcelProperty(index = 24)
    private String cough;

    @ExcelProperty(index = 25)
    private String hemoptysis;

    @ExcelProperty(index = 26)
    private String fever;

    @ExcelProperty(index = 27)
    private String chestPain;

    @ExcelProperty(index = 28)
    private String nightSweats;

    @ExcelProperty(index = 29)
    private String appetiteLoss;

    @ExcelProperty(index = 30)
    private String fatigue;

    @ExcelProperty(index = 31)
    private String weightLoss;

    @ExcelProperty(index = 32)
    private String hasInfectionScreen;

    @ExcelProperty(index = 33, converter = FlexibleLocalDateConverter.class)
    private LocalDate screenDate;

    @ExcelProperty(index = 34)
    private String screenMethod;

    @ExcelProperty(index = 35)
    private String screenResult;

    @ExcelProperty(index = 36)
    private String infectionResult;

    @ExcelProperty(index = 37)
    private String hasChestXray;

    @ExcelProperty(index = 38, converter = FlexibleLocalDateConverter.class)
    private LocalDate chestXrayDate;

    @ExcelProperty(index = 39)
    private String chestXrayResult;

    @ExcelProperty(index = 40)
    private String diagnosisFirst;

    @ExcelProperty(index = 41)
    private String diagnosisHalfYear;

    @ExcelProperty(index = 42)
    private String diagnosisOneYear;

    @ExcelProperty(index = 43)
    private String hasPreventiveTreatment;

    @ExcelProperty(index = 44)
    private String preventivePlan;

    @ExcelProperty(index = 45, converter = FlexibleLocalDateConverter.class)
    private LocalDate preventiveStartDate;

    @ExcelProperty(index = 46, converter = FlexibleLocalDateConverter.class)
    private LocalDate preventiveEndDate;

    @ExcelProperty(index = 47)
    private String preventiveResult;

    @ExcelProperty(index = 48)
    private String preventiveManager;

    public static KeyPopulationScreeningExcelExportRow from(ScreeningKeyPopulation source, int seq) {
        KeyPopulationScreeningExcelExportRow row = new KeyPopulationScreeningExcelExportRow();
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
        row.setTownshipCommunity(source.getTownshipCommunity());
        row.setCurrentAddress(source.getCurrentAddress());
        row.setCrowdCategoryClose(source.getCrowdCategoryClose());
        row.setCrowdCategoryStudent(source.getCrowdCategoryStudent());
        row.setCrowdCategoryTeacher(source.getCrowdCategoryTeacher());
        row.setCrowdCategoryElder(source.getCrowdCategoryElder());
        row.setCrowdCategoryDiabetes(source.getCrowdCategoryDiabetes());
        row.setCrowdCategoryDual(source.getCrowdCategoryDual());
        row.setCrowdCategoryTbHist(source.getCrowdCategoryTbHist());
        row.setCrowdCategoryNormal(source.getCrowdCategoryNormal());
        row.setHasSuspiciousSymptoms(source.getHasSuspiciousSymptoms());
        row.setCough(source.getCough());
        row.setHemoptysis(source.getHemoptysis());
        row.setFever(source.getFever());
        row.setChestPain(source.getChestPain());
        row.setNightSweats(source.getNightSweats());
        row.setAppetiteLoss(source.getAppetiteLoss());
        row.setFatigue(source.getFatigue());
        row.setWeightLoss(source.getWeightLoss());
        row.setHasInfectionScreen(source.getHasInfectionScreen());
        row.setScreenDate(source.getScreenDate());
        row.setScreenMethod(source.getScreenMethod());
        row.setScreenResult(source.getScreenResult());
        row.setInfectionResult(source.getInfectionResult());
        row.setHasChestXray(source.getHasChestXray());
        row.setChestXrayDate(source.getChestXrayDate());
        row.setChestXrayResult(source.getChestXrayResult());
        row.setDiagnosisFirst(source.getDiagnosisFirst());
        row.setDiagnosisHalfYear(source.getDiagnosisHalfYear());
        row.setDiagnosisOneYear(source.getDiagnosisOneYear());
        row.setHasPreventiveTreatment(source.getHasPreventiveTreatment());
        row.setPreventivePlan(source.getPreventivePlan());
        row.setPreventiveStartDate(source.getPreventiveStartDate());
        row.setPreventiveEndDate(source.getPreventiveEndDate());
        row.setPreventiveResult(source.getPreventiveResult());
        row.setPreventiveManager(source.getPreventiveManager());
        return row;
    }
}
