package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 感染筛查方法归一化：兼容密接完整选项名与短码（PPD/EC/IGRA）。
 */
public final class ScreeningMethodSupport {

    private ScreeningMethodSupport() {
    }

    /**
     * 归一化为短码；无法识别时保留原值（避免提交非三选项时被清空）。
     */
    public static String normalize(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String v = raw.trim();
        if ("PPD".equalsIgnoreCase(v)) {
            return "PPD";
        }
        if ("EC".equalsIgnoreCase(v)) {
            return "EC";
        }
        if ("IGRA".equalsIgnoreCase(v)) {
            return "IGRA";
        }
        if ("未做".equals(v) || "未查".equals(v)) {
            return "未查";
        }
        String upper = v.toUpperCase();
        // 先匹配更长/更明确的关键字，避免误判
        if (upper.contains("IGRA") || v.contains("干扰素")) {
            return "IGRA";
        }
        if (upper.contains("EC") || v.contains("结核抗原")) {
            return "EC";
        }
        if (upper.contains("PPD") || v.contains("结核菌素")) {
            return "PPD";
        }
        return v;
    }

    /** 从感染筛查结果推断方法（仅作兜底，真实方法字段优先） */
    public static String inferFromInfectionResult(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return null;
        }
        String v = infectionResult.trim();
        String upper = v.toUpperCase();
        if (upper.startsWith("IGRA") || upper.contains("IGRA")) {
            return "IGRA";
        }
        if (upper.startsWith("EC") || upper.contains("EC阳性") || upper.contains("EC阴性")) {
            return "EC";
        }
        if (upper.startsWith("PPD") || upper.contains("PPD")) {
            return "PPD";
        }
        return null;
    }

    /** 展示用：优先方法字段，否则从结果推断 */
    public static String display(String screenMethod, String infectionResult) {
        String normalized = normalize(screenMethod);
        if (StrUtil.isNotBlank(normalized)) {
            return normalized;
        }
        String inferred = inferFromInfectionResult(infectionResult);
        return StrUtil.blankToDefault(inferred, "");
    }
}
