package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * 筛查数据增量导入：按身份证号匹配时，用最新 Excel 行覆盖已有记录的业务字段。
 */
public final class ScreeningImportMergeSupport {

    private ScreeningImportMergeSupport() {
    }

    public static void mergeKeyPopulation(ScreeningKeyPopulation existing, ScreeningKeyPopulation incoming) {
        mergeString(incoming.getYear(), existing::setYear);
        mergeString(incoming.getCity(), existing::setCity);
        mergeString(incoming.getDistrict(), existing::setDistrict);
        mergeString(incoming.getName(), existing::setName);
        mergeString(incoming.getGender(), existing::setGender);
        mergeDate(incoming.getBirthDate(), existing::setBirthDate);
        mergeInteger(incoming.getAge(), existing::setAge);
        mergeString(incoming.getIdType(), existing::setIdType);
        mergeString(incoming.getEthnicity(), existing::setEthnicity);
        mergeString(incoming.getPhone(), existing::setPhone);
        mergeString(incoming.getHouseholdAddress(), existing::setHouseholdAddress);
        mergeString(incoming.getTownshipCommunity(), existing::setTownshipCommunity);
        mergeString(incoming.getCurrentAddress(), existing::setCurrentAddress);

        mergeString(incoming.getCrowdCategoryClose(), existing::setCrowdCategoryClose);
        mergeString(incoming.getCrowdCategoryStudent(), existing::setCrowdCategoryStudent);
        mergeString(incoming.getCrowdCategoryTeacher(), existing::setCrowdCategoryTeacher);
        mergeString(incoming.getCrowdCategoryElder(), existing::setCrowdCategoryElder);
        mergeString(incoming.getCrowdCategoryDiabetes(), existing::setCrowdCategoryDiabetes);
        mergeString(incoming.getCrowdCategoryDual(), existing::setCrowdCategoryDual);
        mergeString(incoming.getCrowdCategoryTbHist(), existing::setCrowdCategoryTbHist);
        mergeString(incoming.getCrowdCategoryNormal(), existing::setCrowdCategoryNormal);

        mergeString(incoming.getHasSuspiciousSymptoms(), existing::setHasSuspiciousSymptoms);
        mergeString(incoming.getCough(), existing::setCough);
        mergeString(incoming.getHemoptysis(), existing::setHemoptysis);
        mergeString(incoming.getFever(), existing::setFever);
        mergeString(incoming.getChestPain(), existing::setChestPain);
        mergeString(incoming.getNightSweats(), existing::setNightSweats);
        mergeString(incoming.getAppetiteLoss(), existing::setAppetiteLoss);
        mergeString(incoming.getFatigue(), existing::setFatigue);
        mergeString(incoming.getWeightLoss(), existing::setWeightLoss);

        mergeString(incoming.getHasInfectionScreen(), existing::setHasInfectionScreen);
        mergeDate(incoming.getScreenDate(), existing::setScreenDate);
        mergeString(incoming.getScreenMethod(), existing::setScreenMethod);
        mergeString(incoming.getScreenResult(), existing::setScreenResult);
        mergeString(incoming.getInfectionResult(), existing::setInfectionResult);

        mergeString(incoming.getHasChestXray(), existing::setHasChestXray);
        mergeDate(incoming.getChestXrayDate(), existing::setChestXrayDate);
        mergeString(incoming.getChestXrayResult(), existing::setChestXrayResult);
        if (StrUtil.isNotBlank(incoming.getDiagnosisFirst())) {
            existing.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(incoming.getDiagnosisFirst()));
        }
        mergeString(incoming.getDiagnosisHalfYear(), existing::setDiagnosisHalfYear);
        mergeString(incoming.getDiagnosisOneYear(), existing::setDiagnosisOneYear);

        mergeString(incoming.getHasPreventiveTreatment(), existing::setHasPreventiveTreatment);
        mergeString(incoming.getPreventivePlan(), existing::setPreventivePlan);
        mergeDate(incoming.getPreventiveStartDate(), existing::setPreventiveStartDate);
        mergeDate(incoming.getPreventiveEndDate(), existing::setPreventiveEndDate);
        mergeString(incoming.getPreventiveResult(), existing::setPreventiveResult);
        mergeString(incoming.getPreventiveManager(), existing::setPreventiveManager);

        mergeString(incoming.getRemark(), existing::setRemark);
        mergeString(incoming.getUploadBatch(), existing::setUploadBatch);
        mergeInteger(incoming.getImportRowNo(), existing::setImportRowNo);
        existing.setDepartmentId(incoming.getDepartmentId());
    }

    public static void mergeSchool(ScreeningSchool existing, ScreeningSchool incoming) {
        mergeString(incoming.getYear(), existing::setYear);
        mergeString(incoming.getCity(), existing::setCity);
        mergeString(incoming.getDistrict(), existing::setDistrict);
        mergeString(incoming.getName(), existing::setName);
        mergeString(incoming.getGender(), existing::setGender);
        mergeDate(incoming.getBirthDate(), existing::setBirthDate);
        mergeInteger(incoming.getAge(), existing::setAge);
        mergeString(incoming.getIdType(), existing::setIdType);
        mergeString(incoming.getEthnicity(), existing::setEthnicity);
        mergeString(incoming.getPhone(), existing::setPhone);
        mergeString(incoming.getHouseholdAddress(), existing::setHouseholdAddress);
        mergeString(incoming.getCurrentAddress(), existing::setCurrentAddress);
        mergeString(incoming.getSchoolType(), existing::setSchoolType);
        mergeString(incoming.getSchoolName(), existing::setSchoolName);
        mergeString(incoming.getClassName(), existing::setClassName);
        mergeString(incoming.getTbHistory(), existing::setTbHistory);
        mergeString(incoming.getCloseContactHistory(), existing::setCloseContactHistory);
        mergeString(incoming.getSuspiciousSymptoms(), existing::setSuspiciousSymptoms);

        mergeString(incoming.getHasInfectionScreen(), existing::setHasInfectionScreen);
        mergeDate(incoming.getScreenDate(), existing::setScreenDate);
        mergeString(incoming.getScreenMethod(), existing::setScreenMethod);
        mergeString(incoming.getScreenResult(), existing::setScreenResult);
        mergeString(incoming.getInfectionResult(), existing::setInfectionResult);

        mergeString(incoming.getHasChestXray(), existing::setHasChestXray);
        mergeDate(incoming.getChestXrayDate(), existing::setChestXrayDate);
        mergeString(incoming.getChestXrayResult(), existing::setChestXrayResult);
        mergeString(incoming.getSputumSmearResult(), existing::setSputumSmearResult);
        mergeString(incoming.getMolecularBiologyResult(), existing::setMolecularBiologyResult);
        if (StrUtil.isNotBlank(incoming.getDiagnosisFirst())) {
            existing.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(incoming.getDiagnosisFirst()));
        }

        mergeString(incoming.getRemark(), existing::setRemark);
        mergeString(incoming.getUploadBatch(), existing::setUploadBatch);
        mergeInteger(incoming.getImportRowNo(), existing::setImportRowNo);
        existing.setDepartmentId(incoming.getDepartmentId());
    }

    private static void mergeString(String value, Consumer<String> setter) {
        if (StrUtil.isNotBlank(value)) {
            setter.accept(value);
        }
    }

    private static void mergeInteger(Integer value, Consumer<Integer> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private static void mergeDate(LocalDate value, Consumer<LocalDate> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
