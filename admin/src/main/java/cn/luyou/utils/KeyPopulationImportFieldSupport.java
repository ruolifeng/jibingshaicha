package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 重点人群 Excel 导入字段校验：与页面「编辑筛查记录」下拉口径一致。
 * <p>不匹配时返回明确字段错误；调用方应跳过该行并要求用户修改后重新上传。</p>
 */
public final class KeyPopulationImportFieldSupport {

    private static final Set<String> YES_NO = Set.of("是", "否");
    private static final Set<String> HAVE_NONE = Set.of("有", "无");
    private static final Set<String> GENDER = Set.of("男", "女");
    private static final Set<String> CHEST_RESULT = Set.of("正常", "异常", "未查");
    private static final Set<String> YES_MARKS = Set.of("√", "✓", "✔");

    private KeyPopulationImportFieldSupport() {
    }

    /**
     * 校验并对齐可归一字段；有阻断性错误时返回 false（该行不应入库）。
     */
    public static boolean validateAndNormalize(ScreeningKeyPopulation data, int row, ImportResult result) {
        if (data == null) {
            return false;
        }
        String name = data.getName();
        List<String> errors = new ArrayList<>();

        if (StrUtil.isNotBlank(data.getGender())) {
            String gender = data.getGender().trim();
            if (!GENDER.contains(gender)) {
                errors.add(fieldError("性别", "男/女", data.getGender()));
            } else {
                data.setGender(gender);
            }
        }

        if (StrUtil.isNotBlank(data.getPhone()) && !ImportIdentitySupport.isValidPhoneOrPlaceholder(data.getPhone())) {
            errors.add("「联系电话」填写错误，仅支持11位手机号或占位「无」，当前值：" + trimDisplay(data.getPhone()));
        }

        data.setCrowdCategoryClose(checkYesNo(errors, "人群分类-密接", data.getCrowdCategoryClose()));
        data.setCrowdCategoryStudent(checkYesNo(errors, "人群分类-学生", data.getCrowdCategoryStudent()));
        data.setCrowdCategoryTeacher(checkYesNo(errors, "人群分类-教职工", data.getCrowdCategoryTeacher()));
        data.setCrowdCategoryElder(checkYesNo(errors, "人群分类-老年人", data.getCrowdCategoryElder()));
        data.setCrowdCategoryDiabetes(checkYesNo(errors, "人群分类-糖尿病", data.getCrowdCategoryDiabetes()));
        data.setCrowdCategoryDual(checkYesNo(errors, "人群分类-双感", data.getCrowdCategoryDual()));
        data.setCrowdCategoryTbHist(checkYesNo(errors, "人群分类-既往结核史", data.getCrowdCategoryTbHist()));
        data.setCrowdCategoryNormal(checkYesNo(errors, "人群分类-非重点人群", data.getCrowdCategoryNormal()));

        data.setHasSuspiciousSymptoms(checkHaveNone(errors, "是否有可疑症状", data.getHasSuspiciousSymptoms()));
        data.setCough(checkYesNo(errors, "咳嗽咳痰", data.getCough()));
        data.setHemoptysis(checkYesNo(errors, "咯血或血痰", data.getHemoptysis()));
        data.setFever(checkYesNo(errors, "发热", data.getFever()));
        data.setChestPain(checkYesNo(errors, "胸痛", data.getChestPain()));
        data.setNightSweats(checkYesNo(errors, "夜间盗汗", data.getNightSweats()));
        data.setAppetiteLoss(checkYesNo(errors, "食欲不振", data.getAppetiteLoss()));
        data.setFatigue(checkYesNo(errors, "乏力", data.getFatigue()));
        data.setWeightLoss(checkYesNo(errors, "体重减轻", data.getWeightLoss()));

        data.setHasInfectionScreen(checkYesNo(errors, "是否进行感染筛", data.getHasInfectionScreen()));
        data.setHasChestXray(checkYesNo(errors, "是否进行胸片检查", data.getHasChestXray()));

        if (StrUtil.isNotBlank(data.getChestXrayResult())) {
            String chest = data.getChestXrayResult().trim();
            if (!CHEST_RESULT.contains(chest)) {
                errors.add(fieldError("胸片结果", "正常/异常/未查", data.getChestXrayResult()));
            } else {
                data.setChestXrayResult(chest);
            }
        }

        if (!InfectionScreenFieldSupport.isValidMethod(data.getScreenMethod())) {
            errors.add(fieldError("感染筛查方法",
                    String.join("/", InfectionScreenFieldSupport.METHODS) + "（兼容PPD/EC/IGRA/未查）",
                    data.getScreenMethod()));
        }
        if (!InfectionScreenFieldSupport.isValidResult(data.getInfectionResult())) {
            errors.add(fieldError("感染筛查结果",
                    String.join("/", InfectionScreenFieldSupport.RESULTS),
                    data.getInfectionResult()));
        }
        if (!ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisFirst())) {
            errors.add(fieldError("首次诊断结果",
                    "排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者",
                    data.getDiagnosisFirst()));
        }
        if (!ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisHalfYear())) {
            errors.add(fieldError("半年诊断结果",
                    "排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者",
                    data.getDiagnosisHalfYear()));
        }
        if (!ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisOneYear())) {
            errors.add(fieldError("一年诊断结果",
                    "排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者",
                    data.getDiagnosisOneYear()));
        }

        if (!errors.isEmpty()) {
            for (String err : errors) {
                result.addError(row, name, err + "；请修改后重新上传");
            }
            return false;
        }

        InfectionScreenFieldSupport.applyNormalized(
                data::setScreenMethod, data.getScreenMethod(),
                data::setInfectionResult, data.getInfectionResult());
        ScreeningDiagnosisSupport.applyNormalizedKeyPopulationDiagnosis(
                data::setDiagnosisFirst, data.getDiagnosisFirst(),
                data::setDiagnosisHalfYear, data.getDiagnosisHalfYear(),
                data::setDiagnosisOneYear, data.getDiagnosisOneYear());
        return true;
    }

    /** 是/否；勾选符号归一为「是」；空白保留 */
    private static String checkYesNo(List<String> errors, String field, String raw) {
        if (StrUtil.isBlank(raw)) {
            return raw;
        }
        String t = raw.trim();
        if (YES_MARKS.contains(t)) {
            return "是";
        }
        if (YES_NO.contains(t)) {
            return t;
        }
        errors.add(fieldError(field, "是/否", raw));
        return raw;
    }

    /** 有/无；空白保留 */
    private static String checkHaveNone(List<String> errors, String field, String raw) {
        if (StrUtil.isBlank(raw)) {
            return raw;
        }
        String t = raw.trim();
        if (HAVE_NONE.contains(t)) {
            return t;
        }
        errors.add(fieldError(field, "有/无", raw));
        return raw;
    }

    private static String fieldError(String field, String allowed, String actual) {
        return "「" + field + "」填写错误，仅支持：" + allowed + "，当前值：" + trimDisplay(actual);
    }

    private static String trimDisplay(String value) {
        if (value == null) {
            return "";
        }
        String t = value.trim();
        return t.isEmpty() ? "（空）" : t;
    }
}
