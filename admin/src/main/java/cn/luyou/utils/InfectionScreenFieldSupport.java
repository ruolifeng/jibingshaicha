package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 重点人群 / 疫情筛查 / 密接个案：感染筛查方法、感染检测结果（结果判定）取值限制。
 */
public final class InfectionScreenFieldSupport {

    /** 官方下拉：感染筛查方法 */
    public static final List<String> METHODS = List.of(
            "结核菌素皮肤试验_PPD",
            "结核抗原皮肤试验_EC",
            "γ干扰素释放试验_IGRA",
            "未做"
    );

    /** 官方下拉：感染检测结果 / 结果判定 */
    public static final List<String> RESULTS = List.of(
            "一般阳性",
            "中度阳性",
            "强阳性",
            "阳性",
            "阴性",
            "未判读"
    );

    private static final Set<String> METHOD_SET = Set.copyOf(METHODS);
    private static final Set<String> RESULT_SET = Set.copyOf(RESULTS);

    /** 方法短码 / 历史文案 → 官方值 */
    private static final Map<String, String> METHOD_ALIASES = new LinkedHashMap<>();

    /** 结果历史文案 → 官方值（无法映射的视为非法） */
    private static final Map<String, String> RESULT_ALIASES = new LinkedHashMap<>();

    static {
        METHOD_ALIASES.put("PPD", "结核菌素皮肤试验_PPD");
        METHOD_ALIASES.put("结核菌素皮肤试验", "结核菌素皮肤试验_PPD");
        METHOD_ALIASES.put("结核菌素纯蛋白衍生物（PPD）", "结核菌素皮肤试验_PPD");
        METHOD_ALIASES.put("EC", "结核抗原皮肤试验_EC");
        METHOD_ALIASES.put("结核抗原皮肤试验", "结核抗原皮肤试验_EC");
        METHOD_ALIASES.put("重组结核分枝杆菌融合蛋白（EC）", "结核抗原皮肤试验_EC");
        METHOD_ALIASES.put("IGRA", "γ干扰素释放试验_IGRA");
        METHOD_ALIASES.put("γ-干扰素释放试验", "γ干扰素释放试验_IGRA");
        METHOD_ALIASES.put("γ-干扰素释放试验（IGRA）", "γ干扰素释放试验_IGRA");
        METHOD_ALIASES.put("未查", "未做");
        METHOD_ALIASES.put("未做", "未做");

        RESULT_ALIASES.put("PPD阴性", "阴性");
        RESULT_ALIASES.put("EC阴性", "阴性");
        RESULT_ALIASES.put("IGRA阴性", "阴性");
        RESULT_ALIASES.put("PPD+", "一般阳性");
        RESULT_ALIASES.put("PPD++", "中度阳性");
        RESULT_ALIASES.put("PPD+++", "强阳性");
        RESULT_ALIASES.put("EC阳性", "阳性");
        RESULT_ALIASES.put("IGRA阳性", "阳性");
        RESULT_ALIASES.put("无法判读", "未判读");
        RESULT_ALIASES.put("未判读", "未判读");
        RESULT_ALIASES.put("未感染", "阴性");
        RESULT_ALIASES.put("感染", "阳性");
        RESULT_ALIASES.put("阴性", "阴性");
        RESULT_ALIASES.put("阳性", "阳性");
        RESULT_ALIASES.put("一般阳性", "一般阳性");
        RESULT_ALIASES.put("中度阳性", "中度阳性");
        RESULT_ALIASES.put("强阳性", "强阳性");
    }

    private InfectionScreenFieldSupport() {
    }

    public static boolean isValidMethod(String raw) {
        return StrUtil.isBlank(raw) || normalizeMethod(raw) != null;
    }

    public static boolean isValidResult(String raw) {
        return StrUtil.isBlank(raw) || normalizeResult(raw) != null;
    }

    /** 归一为官方方法文案；无法识别返回 null */
    public static String normalizeMethod(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String token = normalizeToken(raw);
        if (METHOD_SET.contains(token)) {
            return token;
        }
        String alias = METHOD_ALIASES.get(token);
        if (alias != null) {
            return alias;
        }
        String upper = token.toUpperCase(Locale.ROOT);
        return METHOD_ALIASES.get(upper);
    }

    /**
     * 筛选时展开官方结果 → 兼容历史文案（学生潜伏记录可能仍存「未感染/感染/无法判读」）。
     * 「阳性」不包含一般/中度/强阳性，避免筛选项串味。
     */
    public static List<String> expandFilterVariants(String result) {
        if (StrUtil.isBlank(result)) {
            return List.of();
        }
        String trimmed = result.trim();
        java.util.LinkedHashSet<String> variants = new java.util.LinkedHashSet<>();
        variants.add(trimmed);
        String official = normalizeResult(trimmed);
        if (official != null) {
            variants.add(official);
            for (Map.Entry<String, String> entry : RESULT_ALIASES.entrySet()) {
                if (official.equals(entry.getValue())) {
                    variants.add(entry.getKey());
                }
            }
        }
        return List.copyOf(variants);
    }

    /** 归一为官方结果文案；无法识别返回 null */
    public static String normalizeResult(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String token = normalizeToken(raw);
        if (RESULT_SET.contains(token)) {
            return token;
        }
        return RESULT_ALIASES.get(token);
    }

    /** 导入入库前：合法则归一，非法保持原值由调用方决定是否跳过 */
    public static void applyNormalized(java.util.function.Consumer<String> methodSetter, String method,
                                       java.util.function.Consumer<String> resultSetter, String result) {
        if (StrUtil.isNotBlank(method)) {
            String normalized = normalizeMethod(method);
            if (normalized != null) {
                methodSetter.accept(normalized);
            }
        }
        if (StrUtil.isNotBlank(result)) {
            String normalized = normalizeResult(result);
            if (normalized != null) {
                resultSetter.accept(normalized);
            }
        }
    }

    private static String normalizeToken(String value) {
        String trimmed = value.trim();
        if (trimmed.matches("\\d+(\\.0+)?")) {
            trimmed = trimmed.replaceAll("\\.0+$", "");
        }
        return trimmed.replaceAll("[（(]需注明[）)]", "").trim();
    }
}
