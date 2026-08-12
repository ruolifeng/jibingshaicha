package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.constant.disease.SchoolScreeningCodes;

/**
 * 学生筛查 Excel 数字码 ↔ 中文入库值 互转。
 */
public final class SchoolScreeningCodeSupport {

    private SchoolScreeningCodeSupport() {
    }

    public static String toSchoolType(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.SCHOOL_TYPE);
    }

    public static String fromSchoolType(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.SCHOOL_TYPE);
    }

    public static String toBoardingType(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.BOARDING_TYPE);
    }

    public static String fromBoardingType(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.BOARDING_TYPE);
    }

    public static String toScreenMethod(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.SCREEN_METHOD);
    }

    public static String fromScreenMethod(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.SCREEN_METHOD);
    }

    public static String toInfectionResult(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.INFECTION_JUDGE);
    }

    public static String fromInfectionResult(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.INFECTION_JUDGE);
    }

    public static String toChestXrayMethod(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.CHEST_METHOD);
    }

    public static String fromChestXrayMethod(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.CHEST_METHOD);
    }

    /** 入库展示文案（细分类）；判定用见 {@link #toChestXrayResultForJudge} */
    public static String toChestXrayResult(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.CHEST_RESULT);
    }

    public static String fromChestXrayResult(String label) {
        if (StrUtil.isBlank(label)) return "";
        String trimmed = label.trim();
        if ("正常".equals(trimmed) || "未见异常".equals(trimmed)) return "0";
        if ("异常".equals(trimmed)) return "1";
        return reverseOrPassthrough(trimmed, SchoolScreeningCodes.CHEST_RESULT);
    }

    /** 供潜伏判定：未见异常→正常；异常两类→异常 */
    public static String toChestXrayResultForJudge(String storedOrRaw) {
        if (StrUtil.isBlank(storedOrRaw)) return "";
        String label = toChestXrayResult(storedOrRaw);
        if ("未见异常".equals(label) || "正常".equals(label)) return "正常";
        if (label.startsWith("异常") || "异常".equals(label)) return "异常";
        if ("未查".equals(label)) return "未查";
        return label;
    }

    public static String toLabResult(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.LAB_RESULT);
    }

    public static String fromLabResult(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.LAB_RESULT);
    }

    public static String toDiagnosis(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String trimmed = normalizeToken(raw.trim());
        // 数字码：先按官方说明，再按系统入库文案
        String official = SchoolScreeningCodes.SCREENING_RESULT_OFFICIAL.get(trimmed);
        if (official != null) {
            return ScreeningDiagnosisSupport.normalizeDiagnosis(official);
        }
        String mapped = SchoolScreeningCodes.SCREENING_RESULT.get(trimmed);
        if (mapped != null) {
            return ScreeningDiagnosisSupport.normalizeDiagnosis(mapped);
        }
        for (String label : SchoolScreeningCodes.SCREENING_RESULT_OFFICIAL.values()) {
            if (trimmed.startsWith(label + "（") || trimmed.startsWith(label + "(")) {
                return ScreeningDiagnosisSupport.normalizeDiagnosis(label);
            }
        }
        for (String label : SchoolScreeningCodes.SCREENING_RESULT.values()) {
            if (trimmed.startsWith(label + "（") || trimmed.startsWith(label + "(")) {
                return ScreeningDiagnosisSupport.normalizeDiagnosis(label);
            }
        }
        return ScreeningDiagnosisSupport.normalizeDiagnosis(raw.trim());
    }

    public static String fromDiagnosis(String label) {
        if (StrUtil.isBlank(label)) return "";
        String normalized = ScreeningDiagnosisSupport.normalizeDiagnosis(label.trim());
        return switch (normalized) {
            case "排除", "正常", "未发现异常" -> "0";
            case "确诊患者", "活动性肺结核" -> "1";
            case "疑似结核", "疑似肺结核" -> "2";
            case "潜伏感染者" -> "3";
            case "其他", "其它" -> "4";
            default -> reverseOrPassthrough(normalized, SchoolScreeningCodes.SCREENING_RESULT);
        };
    }

    public static String deriveHasInfectionScreen(String screenMethodLabel) {
        if (StrUtil.isBlank(screenMethodLabel)) return "";
        return "未查".equals(screenMethodLabel.trim()) ? "否" : "是";
    }

    public static String deriveHasChestXray(String chestMethodLabel) {
        if (StrUtil.isBlank(chestMethodLabel)) return "";
        return "未查".equals(chestMethodLabel.trim()) ? "否" : "是";
    }

    public static String summarizeSuspiciousSymptoms(String cough, String hemoptysis, String other) {
        if (isYes(cough) || isYes(hemoptysis) || isYes(other)) return "有";
        if (StrUtil.isNotBlank(cough) || StrUtil.isNotBlank(hemoptysis) || StrUtil.isNotBlank(other)) {
            return "无";
        }
        return "";
    }

    private static boolean isYes(String value) {
        if (StrUtil.isBlank(value)) return false;
        String v = value.trim();
        return "有".equals(v) || "是".equals(v) || "1".equals(v);
    }

    private static String mapCodeOrPassthrough(String raw, java.util.Map<String, String> codeMap) {
        if (StrUtil.isBlank(raw)) return "";
        String trimmed = normalizeToken(raw.trim());
        String mapped = codeMap.get(trimmed);
        if (mapped != null) {
            return mapped;
        }
        if (codeMap.containsValue(trimmed)) {
            return trimmed;
        }
        // 其他（需注明）/其他（培训学校…）等说明后缀 → 取码表标准文案
        for (String label : codeMap.values()) {
            if (trimmed.startsWith(label + "（") || trimmed.startsWith(label + "(")) {
                return label;
            }
        }
        return raw.trim();
    }

    private static String reverseOrPassthrough(String label, java.util.Map<String, String> codeMap) {
        if (StrUtil.isBlank(label)) return "";
        String trimmed = normalizeToken(label.trim());
        for (var e : codeMap.entrySet()) {
            if (e.getValue().equals(trimmed)) return e.getKey();
            if (trimmed.startsWith(e.getValue() + "（") || trimmed.startsWith(e.getValue() + "(")) {
                return e.getKey();
            }
        }
        return label.trim();
    }

    /** Excel 数值 1.0、以及「其他（需注明）」类说明后缀归一化 */
    private static String normalizeToken(String value) {
        String trimmed = value.trim();
        if (trimmed.matches("\\d+(\\.0+)?")) {
            trimmed = trimmed.replaceAll("\\.0+$", "");
        }
        return trimmed.replaceAll("[（(]需注明[）)]", "").trim();
    }
}
