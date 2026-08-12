package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.constant.disease.SchoolScreeningCodes;
import cn.luyou.model.ScreeningSchool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学生筛查导入字段取值校验（对齐《2026年秋季新生入学结核病筛查记录表新》）。
 * <p>空值放行；有值时仅允许官方数字码或对应中文（及兼容历史文案）。
 */
public final class SchoolScreeningImportValidateSupport {

    private static final Set<String> GENDER = Set.of("男", "女");
    private static final Set<String> YES_NO = Set.of("是", "否");
    private static final Set<String> HAVE_OR_NOT = Set.of("有", "无");

    /** 感染判定：码表 + 历史感染结果文案 */
    private static final Set<String> INFECTION_RESULT_EXTRA = Set.of(
            "PPD阴性", "PPD+", "PPD++", "PPD+++",
            "EC阴性", "EC阳性", "IGRA阴性", "IGRA阳性"
    );

    /** 胸片结果兼容历史「正常/异常」 */
    private static final Set<String> CHEST_RESULT_EXTRA = Set.of("正常", "异常");

    /** 筛查结果：官方原文 + 系统入库文案 */
    private static final Set<String> DIAGNOSIS_EXTRA = Set.of(
            "未发现异常", "活动性肺结核", "疑似肺结核", "正常", "排除", "确诊患者", "疑似结核", "其它"
    );

    private SchoolScreeningImportValidateSupport() {
    }

    /**
     * @return 校验错误列表；空表示通过
     */
    public static List<String> validate(ScreeningSchool data) {
        List<String> errors = new ArrayList<>();
        if (data == null) {
            return errors;
        }
        check(errors, "性别", data.getGender(), GENDER);
        check(errors, "是否参加筛查", data.getParticipatedScreening(), YES_NO);
        check(errors, "既往结核病史", data.getTbHistory(), HAVE_OR_NOT);
        check(errors, "肺结核接触史", data.getCloseContactHistory(), HAVE_OR_NOT);
        check(errors, "咳嗽咳痰≥两周", data.getSymptomCough(), HAVE_OR_NOT);
        check(errors, "咯血或血痰", data.getSymptomHemoptysis(), HAVE_OR_NOT);
        check(errors, "可疑症状-其他", data.getSymptomOther(), HAVE_OR_NOT);

        checkCodeOrLabel(errors, "类型", data.getSchoolType(), SchoolScreeningCodes.SCHOOL_TYPE);
        checkCodeOrLabel(errors, "是否寄宿制", data.getBoardingType(), SchoolScreeningCodes.BOARDING_TYPE);
        checkCodeOrLabel(errors, "感染筛查方法", data.getScreenMethod(), SchoolScreeningCodes.SCREEN_METHOD);
        checkCodeOrLabel(errors, "判定结果", data.getInfectionResult(), SchoolScreeningCodes.INFECTION_JUDGE, INFECTION_RESULT_EXTRA);
        checkCodeOrLabel(errors, "胸部影像学方法", data.getChestXrayMethod(), SchoolScreeningCodes.CHEST_METHOD);
        checkCodeOrLabel(errors, "胸部影像学结果", data.getChestXrayResult(), SchoolScreeningCodes.CHEST_RESULT, CHEST_RESULT_EXTRA);
        checkCodeOrLabel(errors, "分子生物学结果", data.getMolecularBiologyResult(), SchoolScreeningCodes.LAB_RESULT);
        checkCodeOrLabel(errors, "痰培养结果", data.getSputumCultureResult(), SchoolScreeningCodes.LAB_RESULT);
        checkDiagnosis(errors, data.getDiagnosisFirst());
        checkScreenResult(errors, data.getScreenMethod(), data.getScreenResult());
        return errors;
    }

    private static void checkDiagnosis(List<String> errors, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        String trimmed = normalizeToken(value);
        if (SchoolScreeningCodes.SCREENING_RESULT.containsKey(trimmed)
                || SchoolScreeningCodes.SCREENING_RESULT.containsValue(trimmed)
                || SchoolScreeningCodes.SCREENING_RESULT_OFFICIAL.containsValue(trimmed)
                || DIAGNOSIS_EXTRA.contains(trimmed)) {
            return;
        }
        for (String label : SchoolScreeningCodes.SCREENING_RESULT_OFFICIAL.values()) {
            if (trimmed.startsWith(label + "（") || trimmed.startsWith(label + "(")) {
                return;
            }
        }
        for (String label : SchoolScreeningCodes.SCREENING_RESULT.values()) {
            if (trimmed.startsWith(label + "（") || trimmed.startsWith(label + "(")) {
                return;
            }
        }
        errors.add("筛查结果仅支持：0-4或未发现异常/活动性肺结核/疑似肺结核/潜伏感染者/其他（及系统诊断文案）");
    }

    /**
     * 感染筛查「结果」：EC/IGRA 限阳性/阴性；未查限「无」；PPD 为自由文本（横径×纵径等）。
     */
    private static void checkScreenResult(List<String> errors, String method, String result) {
        if (StrUtil.isBlank(result)) {
            return;
        }
        String m = StrUtil.trim(method);
        String r = result.trim();
        if ("EC".equals(m) || "IGRA".equals(m) || "2".equals(m) || "3".equals(m)) {
            if (!Set.of("阳性", "阴性").contains(r)) {
                errors.add("感染筛查结果（EC/IGRA）仅支持：阳性/阴性");
            }
            return;
        }
        if ("未查".equals(m) || "4".equals(m)) {
            if (!"无".equals(r)) {
                errors.add("感染筛查方法为未查时，结果仅支持填写「无」");
            }
        }
    }

    private static void checkCodeOrLabel(List<String> errors, String field,
                                         String value, Map<String, String> codeMap) {
        checkCodeOrLabel(errors, field, value, codeMap, Set.of());
    }

    private static void checkCodeOrLabel(List<String> errors, String field,
                                         String value, Map<String, String> codeMap,
                                         Set<String> extras) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        String trimmed = normalizeToken(value);
        if (codeMap.containsKey(trimmed) || codeMap.containsValue(trimmed) || extras.contains(trimmed)) {
            return;
        }
        for (String label : codeMap.values()) {
            if (trimmed.startsWith(label + "（") || trimmed.startsWith(label + "(")) {
                return;
            }
        }
        for (String extra : extras) {
            if (trimmed.startsWith(extra + "（") || trimmed.startsWith(extra + "(")) {
                return;
            }
        }
        errors.add(field + "仅支持：" + formatAllowed(codeMap, extras));
    }

    private static void check(List<String> errors, String field, String value, Set<String> allowed) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        if (!allowed.contains(value.trim())) {
            errors.add(field + "仅支持：" + String.join("/", allowed));
        }
    }

    private static String formatAllowed(Map<String, String> codeMap, Collection<String> extras) {
        Set<String> parts = new LinkedHashSet<>();
        codeMap.forEach((k, v) -> parts.add(k + "=" + v));
        if (extras != null) {
            parts.addAll(extras);
        }
        return String.join("，", parts);
    }

    private static String normalizeToken(String value) {
        String trimmed = value.trim();
        if (trimmed.matches("\\d+(\\.0+)?")) {
            trimmed = trimmed.replaceAll("\\.0+$", "");
        }
        return trimmed.replaceAll("[（(]需注明[）)]", "").trim();
    }
}
