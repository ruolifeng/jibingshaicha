package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 感染筛查方法归一化：统一为官方下拉
 * （结核菌素皮肤试验_PPD / 结核抗原皮肤试验_EC / γ干扰素释放试验_IGRA / 未做），
 * 兼容历史短码 PPD/EC/IGRA/未查。
 */
public final class ScreeningMethodSupport {

    private ScreeningMethodSupport() {
    }

    /**
     * 归一为官方方法文案；无法识别时保留原值（避免提交非标准值时被清空）。
     */
    public static String normalize(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String official = InfectionScreenFieldSupport.normalizeMethod(raw);
        if (official != null) {
            return official;
        }
        return raw.trim();
    }

    /** 筛选时展开官方方法 → 兼容短码/历史文案（学生筛查表仍可能存 PPD 等） */
    public static List<String> expandFilterVariants(String method) {
        String official = normalize(method);
        if (StrUtil.isBlank(official)) {
            return List.of();
        }
        List<String> variants = new ArrayList<>();
        variants.add(official);
        switch (official) {
            case "结核菌素皮肤试验_PPD" -> {
                variants.add("PPD");
                variants.add("1");
            }
            case "结核抗原皮肤试验_EC" -> {
                variants.add("EC");
                variants.add("2");
            }
            case "γ干扰素释放试验_IGRA" -> {
                variants.add("IGRA");
                variants.add("3");
            }
            case "未做" -> {
                variants.add("未查");
                variants.add("4");
            }
            default -> {
            }
        }
        return variants.stream().distinct().toList();
    }

    /** 从感染筛查结果推断方法（仅作兜底，真实方法字段优先） */
    public static String inferFromInfectionResult(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return null;
        }
        String v = infectionResult.trim();
        String upper = v.toUpperCase();
        if (upper.contains("IGRA")) {
            return "γ干扰素释放试验_IGRA";
        }
        if (upper.startsWith("EC") || upper.contains("EC阳性") || upper.contains("EC阴性")
                || v.contains("结核抗原")) {
            return "结核抗原皮肤试验_EC";
        }
        if (upper.startsWith("PPD") || upper.contains("PPD") || v.contains("结核菌素")) {
            return "结核菌素皮肤试验_PPD";
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
