package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * 筛查数据覆盖导入：按身份证号匹配时，用最新 Excel 行覆盖已有记录的业务字段。
 * <p>规则：Excel 空单元格直接覆盖为库内空值（与页面编辑清空一致）；
 * 不修改录入人（由调用方 fillMissingCreator 保留/补缺）；部门仅缺失时补齐。
 */
public final class ScreeningImportMergeSupport {

    private ScreeningImportMergeSupport() {
    }

    public static void mergeKeyPopulation(ScreeningKeyPopulation existing, ScreeningKeyPopulation incoming) {
        overwriteString(incoming.getYear(), existing::setYear);
        overwriteString(incoming.getCity(), existing::setCity);
        overwriteString(incoming.getDistrict(), existing::setDistrict);
        overwriteString(incoming.getName(), existing::setName);
        overwriteString(incoming.getGender(), existing::setGender);
        overwriteDate(incoming.getBirthDate(), existing::setBirthDate);
        overwriteInteger(incoming.getAge(), existing::setAge);
        overwriteString(incoming.getIdType(), existing::setIdType);
        overwriteString(incoming.getEthnicity(), existing::setEthnicity);
        overwriteString(incoming.getPhone(), existing::setPhone);
        overwriteString(incoming.getHouseholdAddress(), existing::setHouseholdAddress);
        overwriteString(incoming.getTownshipCommunity(), existing::setTownshipCommunity);
        overwriteString(incoming.getCurrentAddress(), existing::setCurrentAddress);

        overwriteString(incoming.getCrowdCategoryClose(), existing::setCrowdCategoryClose);
        overwriteString(incoming.getCrowdCategoryStudent(), existing::setCrowdCategoryStudent);
        overwriteString(incoming.getCrowdCategoryTeacher(), existing::setCrowdCategoryTeacher);
        overwriteString(incoming.getCrowdCategoryElder(), existing::setCrowdCategoryElder);
        overwriteString(incoming.getCrowdCategoryDiabetes(), existing::setCrowdCategoryDiabetes);
        overwriteString(incoming.getCrowdCategoryDual(), existing::setCrowdCategoryDual);
        overwriteString(incoming.getCrowdCategoryTbHist(), existing::setCrowdCategoryTbHist);
        overwriteString(incoming.getCrowdCategoryNormal(), existing::setCrowdCategoryNormal);

        overwriteString(incoming.getHasSuspiciousSymptoms(), existing::setHasSuspiciousSymptoms);
        overwriteString(incoming.getCough(), existing::setCough);
        overwriteString(incoming.getHemoptysis(), existing::setHemoptysis);
        overwriteString(incoming.getFever(), existing::setFever);
        overwriteString(incoming.getChestPain(), existing::setChestPain);
        overwriteString(incoming.getNightSweats(), existing::setNightSweats);
        overwriteString(incoming.getAppetiteLoss(), existing::setAppetiteLoss);
        overwriteString(incoming.getFatigue(), existing::setFatigue);
        overwriteString(incoming.getWeightLoss(), existing::setWeightLoss);

        overwriteString(incoming.getHasInfectionScreen(), existing::setHasInfectionScreen);
        overwriteDate(incoming.getScreenDate(), existing::setScreenDate);
        overwriteString(incoming.getScreenMethod(), existing::setScreenMethod);
        overwriteString(incoming.getScreenResult(), existing::setScreenResult);
        overwriteString(incoming.getInfectionResult(), existing::setInfectionResult);

        overwriteString(incoming.getHasChestXray(), existing::setHasChestXray);
        overwriteDate(incoming.getChestXrayDate(), existing::setChestXrayDate);
        overwriteString(incoming.getChestXrayResult(), existing::setChestXrayResult);
        overwriteDiagnosis(incoming.getDiagnosisFirst(), existing::setDiagnosisFirst);
        // 半年/一年诊断与预防治疗由系统回写，覆盖导入不覆盖（避免 Excel 空列误清督导数据）

        // remark 为 ExcelIgnore，模板不读该列，覆盖导入时保留原备注
        overwriteString(incoming.getUploadBatch(), existing::setUploadBatch);
        overwriteInteger(incoming.getImportRowNo(), existing::setImportRowNo);
        // 部门仅缺失时补齐，覆盖导入不抢归属；录入人由 fillMissingCreator 保留
        if (existing.getDepartmentId() == null && incoming.getDepartmentId() != null) {
            existing.setDepartmentId(incoming.getDepartmentId());
        }
    }

    public static void mergeSchool(ScreeningSchool existing, ScreeningSchool incoming) {
        overwriteString(incoming.getYear(), existing::setYear);
        overwriteString(incoming.getCity(), existing::setCity);
        overwriteString(incoming.getDistrict(), existing::setDistrict);
        overwriteString(incoming.getName(), existing::setName);
        overwriteString(incoming.getGender(), existing::setGender);
        overwriteDate(incoming.getBirthDate(), existing::setBirthDate);
        overwriteInteger(incoming.getAge(), existing::setAge);
        overwriteString(incoming.getIdType(), existing::setIdType);
        overwriteString(incoming.getEthnicity(), existing::setEthnicity);
        overwriteString(incoming.getPhone(), existing::setPhone);
        overwriteString(incoming.getHouseholdAddress(), existing::setHouseholdAddress);
        overwriteString(incoming.getCurrentAddress(), existing::setCurrentAddress);
        overwriteString(incoming.getSchoolType(), existing::setSchoolType);
        overwriteString(incoming.getSchoolName(), existing::setSchoolName);
        overwriteString(incoming.getClassName(), existing::setClassName);
        overwriteString(incoming.getTbHistory(), existing::setTbHistory);
        overwriteString(incoming.getCloseContactHistory(), existing::setCloseContactHistory);
        overwriteString(incoming.getSuspiciousSymptoms(), existing::setSuspiciousSymptoms);

        overwriteString(incoming.getHasInfectionScreen(), existing::setHasInfectionScreen);
        overwriteDate(incoming.getScreenDate(), existing::setScreenDate);
        overwriteString(incoming.getScreenMethod(), existing::setScreenMethod);
        overwriteString(incoming.getScreenResult(), existing::setScreenResult);
        overwriteString(incoming.getInfectionResult(), existing::setInfectionResult);

        overwriteString(incoming.getHasChestXray(), existing::setHasChestXray);
        overwriteDate(incoming.getChestXrayDate(), existing::setChestXrayDate);
        overwriteString(incoming.getChestXrayResult(), existing::setChestXrayResult);
        overwriteString(incoming.getSputumSmearResult(), existing::setSputumSmearResult);
        overwriteString(incoming.getMolecularBiologyResult(), existing::setMolecularBiologyResult);
        overwriteDiagnosis(incoming.getDiagnosisFirst(), existing::setDiagnosisFirst);

        // remark 为 ExcelIgnore，模板不读该列，覆盖导入时保留原备注
        overwriteString(incoming.getUploadBatch(), existing::setUploadBatch);
        overwriteInteger(incoming.getImportRowNo(), existing::setImportRowNo);
        // 部门仅缺失时补齐，覆盖导入不抢归属；录入人由 fillMissingCreator 保留
        if (existing.getDepartmentId() == null && incoming.getDepartmentId() != null) {
            existing.setDepartmentId(incoming.getDepartmentId());
        }
    }

    /** 密接筛查：按官方模板列全量覆盖（含空单元格清空）；手动录入的 ExcelIgnore 字段保留 */
    public static void mergeCloseContact(ScreeningCloseContact existing, ScreeningCloseContact incoming) {
        overwriteString(incoming.getCity(), existing::setCity);
        overwriteString(incoming.getDistrict(), existing::setDistrict);
        overwriteString(incoming.getSourcePatientName(), existing::setSourcePatientName);
        overwriteString(incoming.getSourcePatientCaseNo(), existing::setSourcePatientCaseNo);
        overwriteString(incoming.getSourcePatientBacteriologyResult(), existing::setSourcePatientBacteriologyResult);
        overwriteString(incoming.getSourcePatientPhone(), existing::setSourcePatientPhone);
        overwriteDate(incoming.getReportDate(), existing::setReportDate);
        overwriteDate(incoming.getRegistrationDate(), existing::setRegistrationDate);
        if (incoming.getRegistrationDate() != null) {
            existing.setYear(String.valueOf(incoming.getRegistrationDate().getYear()));
        } else {
            existing.setYear(null);
        }

        overwriteString(incoming.getName(), existing::setName);
        overwriteInteger(incoming.getAge(), existing::setAge);
        overwriteString(incoming.getPhone(), existing::setPhone);
        overwriteString(incoming.getContactType(), existing::setContactType);
        overwriteString(incoming.getContactPlace(), existing::setContactPlace);

        overwriteDate(incoming.getFirstScreenDate(), existing::setFirstScreenDate);
        overwriteString(incoming.getSymptom1(), existing::setSymptom1);
        overwriteString(incoming.getSymptom2(), existing::setSymptom2);
        overwriteDate(incoming.getInfectionCheckDate(), existing::setInfectionCheckDate);
        overwriteString(incoming.getInfectionCheckMethod(), existing::setInfectionCheckMethod);
        overwriteString(incoming.getInfectionCheckResult(), existing::setInfectionCheckResult);
        overwriteDate(incoming.getImagingDate(), existing::setImagingDate);
        overwriteString(incoming.getImagingMethod(), existing::setImagingMethod);
        overwriteString(incoming.getImagingResult(), existing::setImagingResult);
        overwriteDate(incoming.getSputumCheckDate(), existing::setSputumCheckDate);
        overwriteString(incoming.getSputumCheckMethod(), existing::setSputumCheckMethod);
        overwriteString(incoming.getSputumCheckResult(), existing::setSputumCheckResult);
        overwriteDiagnosis(incoming.getFinalScreeningResult(), existing::setFinalScreeningResult);

        overwriteString(incoming.getHasContraindication(), existing::setHasContraindication);
        overwriteString(incoming.getNoTreatmentReason(), existing::setNoTreatmentReason);
        overwriteString(incoming.getContraindicationRemark(), existing::setContraindicationRemark);
        overwriteString(incoming.getHasPreventiveTreatment(), existing::setHasPreventiveTreatment);
        overwriteString(incoming.getPreventivePlan(), existing::setPreventivePlan);
        overwriteString(incoming.getPreventivePlanRemark(), existing::setPreventivePlanRemark);
        overwriteString(incoming.getTreatmentCompleted(), existing::setTreatmentCompleted);
        overwriteString(incoming.getIncompleteReason(), existing::setIncompleteReason);

        overwriteDate(incoming.getFollowup6DueDate(), existing::setFollowup6DueDate);
        overwriteDate(incoming.getFollowup6ScreenDate(), existing::setFollowup6ScreenDate);
        overwriteString(incoming.getFollowup6Symptom1(), existing::setFollowup6Symptom1);
        overwriteString(incoming.getFollowup6Symptom2(), existing::setFollowup6Symptom2);
        overwriteDate(incoming.getFollowup6ImagingDate(), existing::setFollowup6ImagingDate);
        overwriteString(incoming.getFollowup6ImagingMethod(), existing::setFollowup6ImagingMethod);
        overwriteString(incoming.getFollowup6ImagingResult(), existing::setFollowup6ImagingResult);
        overwriteDate(incoming.getFollowup6SputumDate(), existing::setFollowup6SputumDate);
        overwriteString(incoming.getFollowup6SputumMethod(), existing::setFollowup6SputumMethod);
        overwriteString(incoming.getFollowup6SputumResult(), existing::setFollowup6SputumResult);
        overwriteString(incoming.getFollowup6Result(), existing::setFollowup6Result);

        overwriteDate(incoming.getFollowup12DueDate(), existing::setFollowup12DueDate);
        overwriteDate(incoming.getFollowup12ScreenDate(), existing::setFollowup12ScreenDate);
        overwriteString(incoming.getFollowup12Symptom1(), existing::setFollowup12Symptom1);
        overwriteString(incoming.getFollowup12Symptom2(), existing::setFollowup12Symptom2);
        overwriteDate(incoming.getFollowup12ImagingDate(), existing::setFollowup12ImagingDate);
        overwriteString(incoming.getFollowup12ImagingMethod(), existing::setFollowup12ImagingMethod);
        overwriteString(incoming.getFollowup12ImagingResult(), existing::setFollowup12ImagingResult);
        overwriteDate(incoming.getFollowup12SputumDate(), existing::setFollowup12SputumDate);
        overwriteString(incoming.getFollowup12SputumMethod(), existing::setFollowup12SputumMethod);
        overwriteString(incoming.getFollowup12SputumResult(), existing::setFollowup12SputumResult);
        overwriteString(incoming.getFollowup12Result(), existing::setFollowup12Result);

        overwriteDate(incoming.getFollowup24DueDate(), existing::setFollowup24DueDate);
        overwriteDate(incoming.getFollowup24ScreenDate(), existing::setFollowup24ScreenDate);
        overwriteString(incoming.getFollowup24Symptom1(), existing::setFollowup24Symptom1);
        overwriteString(incoming.getFollowup24Symptom2(), existing::setFollowup24Symptom2);
        overwriteDate(incoming.getFollowup24ImagingDate(), existing::setFollowup24ImagingDate);
        overwriteString(incoming.getFollowup24ImagingMethod(), existing::setFollowup24ImagingMethod);
        overwriteString(incoming.getFollowup24ImagingResult(), existing::setFollowup24ImagingResult);
        overwriteDate(incoming.getFollowup24SputumDate(), existing::setFollowup24SputumDate);
        overwriteString(incoming.getFollowup24SputumMethod(), existing::setFollowup24SputumMethod);
        overwriteString(incoming.getFollowup24SputumResult(), existing::setFollowup24SputumResult);
        overwriteString(incoming.getFollowup24Result(), existing::setFollowup24Result);

        overwriteString(incoming.getRemark(), existing::setRemark);
        overwriteString(incoming.getUploadBatch(), existing::setUploadBatch);
        overwriteInteger(incoming.getImportRowNo(), existing::setImportRowNo);
        if (existing.getDepartmentId() == null && incoming.getDepartmentId() != null) {
            existing.setDepartmentId(incoming.getDepartmentId());
        }
    }

    /** 密接个案：与筛查模板列一致，空单元格直接清空 */
    public static void mergeCloseContactCase(CloseContactCase existing, CloseContactCase incoming) {
        overwriteString(incoming.getCity(), existing::setCity);
        overwriteString(incoming.getDistrict(), existing::setDistrict);
        overwriteString(incoming.getSourcePatientName(), existing::setSourcePatientName);
        overwriteString(incoming.getSourcePatientCaseNo(), existing::setSourcePatientCaseNo);
        overwriteString(incoming.getSourcePatientBacteriologyResult(), existing::setSourcePatientBacteriologyResult);
        overwriteString(incoming.getSourcePatientPhone(), existing::setSourcePatientPhone);
        overwriteDate(incoming.getReportDate(), existing::setReportDate);
        overwriteDate(incoming.getRegistrationDate(), existing::setRegistrationDate);
        if (incoming.getRegistrationDate() != null) {
            existing.setYear(String.valueOf(incoming.getRegistrationDate().getYear()));
        } else {
            existing.setYear(null);
        }

        overwriteString(incoming.getName(), existing::setName);
        overwriteInteger(incoming.getAge(), existing::setAge);
        overwriteString(incoming.getPhone(), existing::setPhone);
        overwriteString(incoming.getContactType(), existing::setContactType);
        overwriteString(incoming.getContactPlace(), existing::setContactPlace);

        overwriteDate(incoming.getFirstScreenDate(), existing::setFirstScreenDate);
        overwriteString(incoming.getSymptom1(), existing::setSymptom1);
        overwriteString(incoming.getSymptom2(), existing::setSymptom2);
        overwriteDate(incoming.getInfectionCheckDate(), existing::setInfectionCheckDate);
        overwriteString(incoming.getInfectionCheckMethod(), existing::setInfectionCheckMethod);
        overwriteString(incoming.getInfectionCheckResult(), existing::setInfectionCheckResult);
        overwriteDate(incoming.getImagingDate(), existing::setImagingDate);
        overwriteString(incoming.getImagingMethod(), existing::setImagingMethod);
        overwriteString(incoming.getImagingResult(), existing::setImagingResult);
        overwriteDate(incoming.getSputumCheckDate(), existing::setSputumCheckDate);
        overwriteString(incoming.getSputumCheckMethod(), existing::setSputumCheckMethod);
        overwriteString(incoming.getSputumCheckResult(), existing::setSputumCheckResult);
        overwriteDiagnosis(incoming.getFinalScreeningResult(), existing::setFinalScreeningResult);

        overwriteString(incoming.getHasContraindication(), existing::setHasContraindication);
        overwriteString(incoming.getNoTreatmentReason(), existing::setNoTreatmentReason);
        overwriteString(incoming.getContraindicationRemark(), existing::setContraindicationRemark);
        overwriteString(incoming.getHasPreventiveTreatment(), existing::setHasPreventiveTreatment);
        overwriteString(incoming.getPreventivePlan(), existing::setPreventivePlan);
        overwriteString(incoming.getPreventivePlanRemark(), existing::setPreventivePlanRemark);
        overwriteString(incoming.getTreatmentCompleted(), existing::setTreatmentCompleted);
        overwriteString(incoming.getIncompleteReason(), existing::setIncompleteReason);

        overwriteDate(incoming.getFollowup6DueDate(), existing::setFollowup6DueDate);
        overwriteDate(incoming.getFollowup6ScreenDate(), existing::setFollowup6ScreenDate);
        overwriteString(incoming.getFollowup6Symptom1(), existing::setFollowup6Symptom1);
        overwriteString(incoming.getFollowup6Symptom2(), existing::setFollowup6Symptom2);
        overwriteDate(incoming.getFollowup6ImagingDate(), existing::setFollowup6ImagingDate);
        overwriteString(incoming.getFollowup6ImagingMethod(), existing::setFollowup6ImagingMethod);
        overwriteString(incoming.getFollowup6ImagingResult(), existing::setFollowup6ImagingResult);
        overwriteDate(incoming.getFollowup6SputumDate(), existing::setFollowup6SputumDate);
        overwriteString(incoming.getFollowup6SputumMethod(), existing::setFollowup6SputumMethod);
        overwriteString(incoming.getFollowup6SputumResult(), existing::setFollowup6SputumResult);
        overwriteString(incoming.getFollowup6Result(), existing::setFollowup6Result);

        overwriteDate(incoming.getFollowup12DueDate(), existing::setFollowup12DueDate);
        overwriteDate(incoming.getFollowup12ScreenDate(), existing::setFollowup12ScreenDate);
        overwriteString(incoming.getFollowup12Symptom1(), existing::setFollowup12Symptom1);
        overwriteString(incoming.getFollowup12Symptom2(), existing::setFollowup12Symptom2);
        overwriteDate(incoming.getFollowup12ImagingDate(), existing::setFollowup12ImagingDate);
        overwriteString(incoming.getFollowup12ImagingMethod(), existing::setFollowup12ImagingMethod);
        overwriteString(incoming.getFollowup12ImagingResult(), existing::setFollowup12ImagingResult);
        overwriteDate(incoming.getFollowup12SputumDate(), existing::setFollowup12SputumDate);
        overwriteString(incoming.getFollowup12SputumMethod(), existing::setFollowup12SputumMethod);
        overwriteString(incoming.getFollowup12SputumResult(), existing::setFollowup12SputumResult);
        overwriteString(incoming.getFollowup12Result(), existing::setFollowup12Result);

        overwriteDate(incoming.getFollowup24DueDate(), existing::setFollowup24DueDate);
        overwriteDate(incoming.getFollowup24ScreenDate(), existing::setFollowup24ScreenDate);
        overwriteString(incoming.getFollowup24Symptom1(), existing::setFollowup24Symptom1);
        overwriteString(incoming.getFollowup24Symptom2(), existing::setFollowup24Symptom2);
        overwriteDate(incoming.getFollowup24ImagingDate(), existing::setFollowup24ImagingDate);
        overwriteString(incoming.getFollowup24ImagingMethod(), existing::setFollowup24ImagingMethod);
        overwriteString(incoming.getFollowup24ImagingResult(), existing::setFollowup24ImagingResult);
        overwriteDate(incoming.getFollowup24SputumDate(), existing::setFollowup24SputumDate);
        overwriteString(incoming.getFollowup24SputumMethod(), existing::setFollowup24SputumMethod);
        overwriteString(incoming.getFollowup24SputumResult(), existing::setFollowup24SputumResult);
        overwriteString(incoming.getFollowup24Result(), existing::setFollowup24Result);

        overwriteString(incoming.getRemark(), existing::setRemark);
        overwriteString(incoming.getUploadBatch(), existing::setUploadBatch);
        overwriteInteger(incoming.getImportRowNo(), existing::setImportRowNo);
        if (existing.getDepartmentId() == null && incoming.getDepartmentId() != null) {
            existing.setDepartmentId(incoming.getDepartmentId());
        }
    }

    /** Excel 空串/空白 → null，非空直接覆盖 */
    public static void overwriteString(String value, Consumer<String> setter) {
        setter.accept(StrUtil.isBlank(value) ? null : value.trim());
    }

    public static void overwriteInteger(Integer value, Consumer<Integer> setter) {
        setter.accept(value);
    }

    public static void overwriteDate(LocalDate value, Consumer<LocalDate> setter) {
        setter.accept(value);
    }

    public static void overwriteDiagnosis(String value, Consumer<String> setter) {
        if (StrUtil.isBlank(value)) {
            setter.accept(null);
            return;
        }
        setter.accept(ScreeningDiagnosisSupport.normalizeDiagnosis(value));
    }
}
