package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.constant.disease.SchoolScreeningCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 学生筛查 Excel 数字码 ↔ 中文入库值 互转；列表/导出展示为「数字=填写说明」。
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

    public static String formatSchoolType(String stored) {
        return formatHint(stored, SchoolScreeningCodes.SCHOOL_TYPE_HINT, SchoolScreeningCodeSupport::fromSchoolType);
    }

    public static String toBoardingType(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.BOARDING_TYPE);
    }

    public static String fromBoardingType(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.BOARDING_TYPE);
    }

    public static String formatBoardingType(String stored) {
        return formatHint(stored, SchoolScreeningCodes.BOARDING_TYPE_HINT, SchoolScreeningCodeSupport::fromBoardingType);
    }

    public static String toScreenMethod(String raw) {
        String mapped = mapScreenMethodCode(raw);
        if (mapped != null) {
            return SchoolScreeningCodes.SCREEN_METHOD.get(mapped);
        }
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.SCREEN_METHOD);
    }

    public static String fromScreenMethod(String label) {
        String mapped = mapScreenMethodCode(label);
        if (mapped != null) {
            return mapped;
        }
        return reverseOrPassthrough(label, SchoolScreeningCodes.SCREEN_METHOD);
    }

    public static String formatScreenMethod(String stored) {
        return formatHint(stored, SchoolScreeningCodes.SCREEN_METHOD_HINT, SchoolScreeningCodeSupport::fromScreenMethod);
    }

    public static String toInfectionResult(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.INFECTION_JUDGE);
    }

    public static String fromInfectionResult(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.INFECTION_JUDGE);
    }

    public static String formatInfectionResult(String stored) {
        return formatHint(stored, SchoolScreeningCodes.INFECTION_JUDGE_HINT, SchoolScreeningCodeSupport::fromInfectionResult);
    }

    public static String toChestXrayMethod(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.CHEST_METHOD);
    }

    public static String fromChestXrayMethod(String label) {
        return reverseOrPassthrough(label, SchoolScreeningCodes.CHEST_METHOD);
    }

    public static String formatChestXrayMethod(String stored) {
        return formatHint(stored, SchoolScreeningCodes.CHEST_METHOD_HINT, SchoolScreeningCodeSupport::fromChestXrayMethod);
    }

    /** 入库展示文案（细分类）；判定用见 {@link #toChestXrayResultForJudge} */
    public static String toChestXrayResult(String raw) {
        return mapCodeOrPassthrough(raw, SchoolScreeningCodes.CHEST_RESULT);
    }

    public static String fromChestXrayResult(String label) {
        if (StrUtil.isBlank(label)) return "";
        String trimmed = peelCodeEquals(label.trim(), SchoolScreeningCodes.CHEST_RESULT);
        trimmed = normalizeToken(trimmed);
        if ("正常".equals(trimmed) || "未见异常".equals(trimmed)) return "0";
        if ("异常".equals(trimmed)) return "1";
        return reverseOrPassthrough(trimmed, SchoolScreeningCodes.CHEST_RESULT);
    }

    public static String formatChestXrayResult(String stored) {
        return formatHint(stored, SchoolScreeningCodes.CHEST_RESULT_HINT, SchoolScreeningCodeSupport::fromChestXrayResult);
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

    public static String formatLabResult(String stored) {
        return formatHint(stored, SchoolScreeningCodes.LAB_RESULT_HINT, SchoolScreeningCodeSupport::fromLabResult);
    }

    public static String toDiagnosis(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String peeled = peelCodeEquals(raw.trim(), SchoolScreeningCodes.SCREENING_RESULT_OFFICIAL);
        String trimmed = normalizeToken(peeled);
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
        String peeled = peelCodeEquals(label.trim(), SchoolScreeningCodes.SCREENING_RESULT_HINT);
        String normalized = ScreeningDiagnosisSupport.normalizeDiagnosis(peeled);
        return switch (normalized) {
            case "排除", "正常", "未发现异常" -> "0";
            case "确诊患者", "活动性肺结核" -> "1";
            case "疑似结核", "疑似肺结核" -> "2";
            case "潜伏感染者" -> "3";
            case "其他", "其它" -> "4";
            default -> reverseOrPassthrough(normalized, SchoolScreeningCodes.SCREENING_RESULT);
        };
    }

    public static String formatDiagnosis(String stored) {
        String formatted = formatHint(stored, SchoolScreeningCodes.SCREENING_RESULT_HINT, SchoolScreeningCodeSupport::fromDiagnosis);
        if (formatted.startsWith("4=") && StrUtil.isNotBlank(stored)) {
            String trimmed = stored.trim();
            if (!isStandardOtherDiagnosis(trimmed)) {
                String peeled = peelCodeEquals(trimmed, SchoolScreeningCodes.SCREENING_RESULT_HINT);
                if (peeled.contains("其他") || peeled.contains("其它")) {
                    return "4=" + peeled;
                }
            }
        }
        return formatted;
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
        if (isNone(cough) || isNone(hemoptysis) || isNone(other)) return "无";
        if (isNotInquired(cough) || isNotInquired(hemoptysis) || isNotInquired(other)) return "未询问";
        if (StrUtil.isNotBlank(cough) || StrUtil.isNotBlank(hemoptysis) || StrUtil.isNotBlank(other)) {
            return "无";
        }
        return "";
    }

    /** 导入归一：未问 → 未询问；其余原样 */
    public static String normalizeSymptomValue(String raw) {
        if (StrUtil.isBlank(raw)) return "";
        String trimmed = raw.trim();
        return isNotInquired(trimmed) ? "未询问" : trimmed;
    }

    public static boolean isNotInquired(String value) {
        if (StrUtil.isBlank(value)) return false;
        String v = value.trim();
        return "未询问".equals(v) || "未问".equals(v);
    }

    /**
     * @return 填写为未询问的症状列名，空表示无需提醒
     */
    public static List<String> notInquiredSymptomFields(String cough, String hemoptysis, String other) {
        List<String> fields = new ArrayList<>();
        if (isNotInquired(cough)) fields.add("咳嗽咳痰≥两周");
        if (isNotInquired(hemoptysis)) fields.add("咯血或血痰");
        if (isNotInquired(other)) fields.add("其他");
        return fields;
    }

    private static boolean isYes(String value) {
        if (StrUtil.isBlank(value)) return false;
        String v = value.trim();
        return "有".equals(v) || "是".equals(v) || "1".equals(v);
    }

    private static boolean isNone(String value) {
        if (StrUtil.isBlank(value)) return false;
        String v = value.trim();
        return "无".equals(v) || "否".equals(v) || "0".equals(v);
    }

    private static boolean isStandardOtherDiagnosis(String value) {
        String trimmed = value.trim();
        return "4".equals(trimmed)
                || "其他".equals(trimmed)
                || "其它".equals(trimmed)
                || "其他（需注明）".equals(trimmed)
                || "其它（需注明）".equals(trimmed)
                || "4=其他（需注明）".equals(trimmed);
    }

    private static String mapCodeOrPassthrough(String raw, Map<String, String> codeMap) {
        if (StrUtil.isBlank(raw)) return "";
        String trimmed = normalizeToken(peelCodeEquals(raw.trim(), codeMap));
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

    private static String reverseOrPassthrough(String label, Map<String, String> codeMap) {
        if (StrUtil.isBlank(label)) return "";
        String trimmed = normalizeToken(peelCodeEquals(label.trim(), codeMap));
        if (codeMap.containsKey(trimmed)) {
            return trimmed;
        }
        for (var e : codeMap.entrySet()) {
            if (e.getValue().equals(trimmed)) return e.getKey();
            if (trimmed.startsWith(e.getValue() + "（") || trimmed.startsWith(e.getValue() + "(")) {
                return e.getKey();
            }
        }
        return label.trim();
    }

    /**
     * 列表/导出展示：能映射到填写说明则输出「数字=说明」，否则保留原文。
     */
    private static String formatHint(String stored, Map<String, String> hintMap, Function<String, String> toCode) {
        if (StrUtil.isBlank(stored)) return "";
        String trimmed = stored.trim();
        String code = extractLeadingCode(trimmed);
        if (code == null || !hintMap.containsKey(code)) {
            code = toCode.apply(trimmed);
        }
        String hintLabel = hintMap.get(normalizeToken(code == null ? "" : code));
        if (hintLabel != null) {
            return normalizeToken(code) + "=" + hintLabel;
        }
        return trimmed;
    }

    /** 「1=托幼机构」优先取数字码，便于再导入与展示 */
    private static String peelCodeEquals(String raw, Map<String, String> codeMap) {
        String code = extractLeadingCode(raw);
        if (code != null && (codeMap == null || codeMap.containsKey(code) || code.matches("\\d+"))) {
            return code;
        }
        int eq = indexOfEquals(raw);
        if (eq > 0) {
            String right = raw.substring(eq + 1).trim();
            if (StrUtil.isNotBlank(right)) {
                return right;
            }
        }
        return raw;
    }

    private static String extractLeadingCode(String raw) {
        int eq = indexOfEquals(raw);
        if (eq <= 0) return null;
        String left = normalizeToken(raw.substring(0, eq).trim());
        return left.matches("\\d+") ? left : null;
    }

    private static int indexOfEquals(String value) {
        int ascii = value.indexOf('=');
        int fullwidth = value.indexOf('＝');
        if (ascii < 0) return fullwidth;
        if (fullwidth < 0) return ascii;
        return Math.min(ascii, fullwidth);
    }

    /** 感染筛查方法：短码/官方下拉/填写说明全称 → 1–4 */
    private static String mapScreenMethodCode(String raw) {
        if (StrUtil.isBlank(raw)) return null;
        String trimmed = peelCodeEquals(raw.trim(), SchoolScreeningCodes.SCREEN_METHOD);
        trimmed = normalizeToken(trimmed);
        String upper = trimmed.toUpperCase();
        if ("1".equals(trimmed) || "PPD".equals(upper) || trimmed.contains("PPD") || trimmed.contains("结核菌素")) {
            return "1";
        }
        if ("2".equals(trimmed) || "EC".equals(upper) || trimmed.contains("（EC）") || trimmed.contains("(EC)")
                || trimmed.contains("融合蛋白") || trimmed.contains("结核抗原")) {
            return "2";
        }
        if ("3".equals(trimmed) || "IGRA".equals(upper) || trimmed.contains("IGRA") || trimmed.contains("干扰素")) {
            return "3";
        }
        if ("4".equals(trimmed) || "未查".equals(trimmed) || "未做".equals(trimmed)) {
            return "4";
        }
        return null;
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
