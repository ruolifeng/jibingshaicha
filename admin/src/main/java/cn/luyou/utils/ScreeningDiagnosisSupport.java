package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.List;
import java.util.Set;

/**
 * 筛查导入/待诊断分流：诊断结果与感染筛查结果的判定工具。
 */
public final class ScreeningDiagnosisSupport {

    public static final String SUSPECTED_TB_DIAGNOSIS = "疑似结核";
    private static final String LEGACY_SUSPECTED_TB_DIAGNOSIS = "疑似肺结核";

    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
            "PPD+", "PPD++", "PPD+++", "EC阳性", "IGRA阳性"
    );

    /** 重点/疫情/密接官方「结果判定」阳性档 */
    private static final Set<String> OFFICIAL_POSITIVE_RESULTS = Set.of(
            "一般阳性", "中度阳性", "强阳性", "阳性"
    );

    /** 终态「正常」类诊断：不进入待诊断，导入后直接结束流程 */
    private static final Set<String> NORMAL_TERMINAL_DIAGNOSIS = Set.of(
            "正常", "排除", "其他", "其它"
    );

    /** 需进入后续流程的诊断 */
    private static final Set<String> ACTIONABLE_DIAGNOSIS = Set.of(
            SUSPECTED_TB_DIAGNOSIS, LEGACY_SUSPECTED_TB_DIAGNOSIS, "潜伏感染者",
            "确诊患者", "确诊结核", "在治患者"
    );

    /** 重点人群 / 疫情筛查 — 诊断结果官方取值 */
    public static final List<String> KEY_POPULATION_DIAGNOSES = List.of(
            "排除", "正常", SUSPECTED_TB_DIAGNOSIS, "确诊结核", "潜伏感染者", "在治患者"
    );

    private static final Set<String> KEY_POPULATION_DIAGNOSIS_SET = Set.copyOf(KEY_POPULATION_DIAGNOSES);

    /** 确诊类文案（筛查列表标红、不建潜伏记录） */
    private static final Set<String> CONFIRMED_PATIENT_DIAGNOSES = Set.of(
            "确诊患者", "确诊结核", "在治患者"
    );

    private ScreeningDiagnosisSupport() {
    }

    public static boolean isPositiveInfection(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return false;
        }
        String trimmed = infectionResult.trim();
        // 2026 秋季表判定结果：感染
        if ("感染".equals(trimmed) || "1".equals(trimmed)) {
            return true;
        }
        // 重点人群/疫情/密接官方结果判定
        if (OFFICIAL_POSITIVE_RESULTS.contains(trimmed)) {
            return true;
        }
        return POSITIVE_KEYWORDS.stream().anyMatch(trimmed::contains);
    }

    /** 感染筛查结果为阴性/未感染 */
    public static boolean isNormalInfection(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return false;
        }
        String trimmed = infectionResult.trim();
        if (OFFICIAL_POSITIVE_RESULTS.contains(trimmed) || "未判读".equals(trimmed)) {
            return false;
        }
        return "未感染".equals(trimmed) || "0".equals(trimmed) || "阴性".equals(trimmed) || trimmed.contains("阴性");
    }

    public static boolean isNormalTerminalDiagnosis(String diagnosis) {
        if (StrUtil.isBlank(diagnosis)) {
            return false;
        }
        return NORMAL_TERMINAL_DIAGNOSIS.contains(diagnosis.trim());
    }

    public static boolean isActionableDiagnosis(String diagnosis) {
        if (StrUtil.isBlank(diagnosis)) {
            return false;
        }
        return ACTIONABLE_DIAGNOSIS.contains(diagnosis.trim());
    }

    /**
     * 是否应标记 isLatent=1（筛查列表状态/筛选用）。
     * 诊断结果为正常/排除/其它时不进入；感染筛查阴性且无后续诊断需求时不进入。
     */
    public static boolean shouldMarkLatent(String infectionResult,
                                           String chestXrayResult,
                                           String hasChestXray,
                                           String diagnosisFirst) {
        if (isNormalTerminalDiagnosis(diagnosisFirst)) {
            return false;
        }
        if (isActionableDiagnosis(diagnosisFirst)) {
            return true;
        }
        if (hasDirectXrayAndActionableDiagnosis(infectionResult, chestXrayResult, hasChestXray, diagnosisFirst)) {
            return true;
        }
        if (isPositiveInfection(infectionResult)) {
            return true;
        }
        return false;
    }

    /**
     * 是否应创建潜伏感染/待诊断记录。
     * <p>筛查结果码 1（确诊患者）：仅在筛查列表标红保留，不建潜伏记录、不进患者管理。
     * 筛查结果码 3（潜伏感染者）及其他需跟进情形：创建并分流。
     */
    public static boolean shouldCreateLatentRecord(String infectionResult,
                                                   String chestXrayResult,
                                                   String hasChestXray,
                                                   String diagnosisFirst) {
        if (isConfirmedPatientDiagnosis(diagnosisFirst)) {
            return false;
        }
        return shouldMarkLatent(infectionResult, chestXrayResult, hasChestXray, diagnosisFirst);
    }

    /**
     * 确诊类诊断：学校口径「确诊患者」；重点/疫情「确诊结核」「在治患者」。
     * 仅筛查列表标红保留，不建潜伏记录。
     */
    public static boolean isConfirmedPatientDiagnosis(String diagnosis) {
        if (StrUtil.isBlank(diagnosis)) {
            return false;
        }
        String trimmed = diagnosis.trim();
        if (CONFIRMED_PATIENT_DIAGNOSES.contains(trimmed)) {
            return true;
        }
        String school = normalizeDiagnosis(trimmed);
        if (CONFIRMED_PATIENT_DIAGNOSES.contains(school)) {
            return true;
        }
        String key = normalizeKeyPopulationDiagnosis(trimmed);
        return key != null && CONFIRMED_PATIENT_DIAGNOSES.contains(key);
    }

    /** 重点人群/疫情筛查诊断是否合法（空白允许） */
    public static boolean isValidKeyPopulationDiagnosis(String raw) {
        return StrUtil.isBlank(raw) || normalizeKeyPopulationDiagnosis(raw) != null;
    }

    /**
     * 重点人群/疫情筛查诊断归一：排除、正常、疑似结核、确诊结核、潜伏感染者、在治患者。
     * 无法识别返回 null。
     */
    public static String normalizeKeyPopulationDiagnosis(String diagnosis) {
        if (StrUtil.isBlank(diagnosis)) {
            return null;
        }
        String trimmed = diagnosis.trim();
        if (trimmed.matches("\\d+(\\.0+)?")) {
            trimmed = trimmed.replaceAll("\\.0+$", "");
        }
        if (KEY_POPULATION_DIAGNOSIS_SET.contains(trimmed)) {
            return trimmed;
        }
        return switch (trimmed) {
            case "0", "未发现异常" -> "排除";
            case "1", "活动性肺结核", "确诊患者", "确诊肺结核" -> "确诊结核";
            case "2", "疑似肺结核" -> SUSPECTED_TB_DIAGNOSIS;
            case "3" -> "潜伏感染者";
            default -> isSuspectedTbDiagnosis(trimmed) ? SUSPECTED_TB_DIAGNOSIS : null;
        };
    }

    /** 入库前：合法则归一写入三个诊断字段 */
    public static void applyNormalizedKeyPopulationDiagnosis(java.util.function.Consumer<String> firstSetter,
                                                             String first,
                                                             java.util.function.Consumer<String> halfSetter,
                                                             String half,
                                                             java.util.function.Consumer<String> oneYearSetter,
                                                             String oneYear) {
        applyOneKeyPopulationDiagnosis(firstSetter, first);
        applyOneKeyPopulationDiagnosis(halfSetter, half);
        applyOneKeyPopulationDiagnosis(oneYearSetter, oneYear);
    }

    private static void applyOneKeyPopulationDiagnosis(java.util.function.Consumer<String> setter, String raw) {
        if (StrUtil.isBlank(raw) || setter == null) {
            return;
        }
        String normalized = normalizeKeyPopulationDiagnosis(raw);
        if (normalized != null) {
            setter.accept(normalized);
        }
    }

    /** 筛查结果码 3 → 潜伏感染者 */
    public static boolean isLatentInfectionDiagnosis(String diagnosis) {
        return "潜伏感染者".equals(normalizeDiagnosis(diagnosis));
    }

    /**
     * Excel 已含胸片与诊断，且诊断需跟进（非正常/排除/其它）。
     * 感染阴性 + 胸片正常时视为流程结束，不进入待诊断。
     */
    public static boolean hasDirectXrayAndActionableDiagnosis(String infectionResult,
                                                              String chestXrayResult,
                                                              String hasChestXray,
                                                              String diagnosisFirst) {
        if (!"是".equals(StrUtil.trim(hasChestXray)) || StrUtil.isBlank(diagnosisFirst)) {
            return false;
        }
        if (isNormalTerminalDiagnosis(diagnosisFirst)) {
            return false;
        }
        String chestForJudge = chestXrayResult;
        if ("未见异常".equals(chestXrayResult)) {
            chestForJudge = "正常";
        } else if (chestXrayResult != null && chestXrayResult.startsWith("异常")) {
            chestForJudge = "异常";
        }
        if (!isPositiveInfection(infectionResult) && "正常".equals(chestForJudge)) {
            return false;
        }
        return isActionableDiagnosis(diagnosisFirst);
    }

    public static boolean isSuspectedTbDiagnosis(String diagnosis) {
        if (StrUtil.isBlank(diagnosis)) {
            return false;
        }
        String trimmed = diagnosis.trim();
        return SUSPECTED_TB_DIAGNOSIS.equals(trimmed) || LEGACY_SUSPECTED_TB_DIAGNOSIS.equals(trimmed);
    }

    /**
     * 待诊断列表「纳入原因」说明（用于对账：待诊断 ≠ 筛查诊断=疑似结核）。
     * 进入待诊断的条件见 {@link #shouldCreateLatentRecord}。
     */
    public static String resolvePendingEntryReason(String infectionResult,
                                                   String chestXrayResult,
                                                   String hasChestXray,
                                                   String diagnosisFirst) {
        if (isSuspectedTbDiagnosis(diagnosisFirst)) {
            return "诊断结果：疑似结核";
        }
        if (isLatentInfectionDiagnosis(diagnosisFirst)) {
            return "诊断结果：潜伏感染者";
        }
        if (isActionableDiagnosis(diagnosisFirst) && !isConfirmedPatientDiagnosis(diagnosisFirst)) {
            return "诊断结果：" + diagnosisFirst.trim();
        }
        if (isPositiveInfection(infectionResult)) {
            return "感染筛查阳性（" + infectionResult.trim() + "）";
        }
        if (hasDirectXrayAndActionableDiagnosis(infectionResult, chestXrayResult, hasChestXray, diagnosisFirst)) {
            return "胸片+诊断需跟进";
        }
        if (StrUtil.isNotBlank(chestXrayResult)
                && (chestXrayResult.contains("异常") || "异常".equals(chestXrayResult.trim()))) {
            return "胸片异常待确认";
        }
        return "其他/历史数据";
    }

    /**
     * 统一诊断文案：官方筛查结果码/文案 → 系统口径。
     * 未发现异常→排除；活动性肺结核→确诊患者；疑似肺结核→疑似结核；其它→其他。
     */
    public static String normalizeDiagnosis(String diagnosis) {
        if (StrUtil.isBlank(diagnosis)) {
            return diagnosis;
        }
        String trimmed = diagnosis.trim();
        if (trimmed.matches("\\d+(\\.0+)?")) {
            trimmed = trimmed.replaceAll("\\.0+$", "");
        }
        return switch (trimmed) {
            case "0", "未发现异常", "正常" -> "排除";
            case "1", "活动性肺结核" -> "确诊患者";
            case "2", "疑似肺结核", SUSPECTED_TB_DIAGNOSIS -> SUSPECTED_TB_DIAGNOSIS;
            case "3", "潜伏感染者" -> "潜伏感染者";
            case "4", "其它", "其他" -> "其他";
            default -> isSuspectedTbDiagnosis(trimmed) ? SUSPECTED_TB_DIAGNOSIS : trimmed;
        };
    }

    /**
     * 筛查列表诊断结果筛选：「正常」匹配终态正常类诊断或未进入待诊断流程的记录。
     */
    public static <T> void applyScreeningDiagnosisFilter(LambdaQueryWrapper<T> wrapper,
                                                         SFunction<T, Integer> isLatentColumn,
                                                         SFunction<T, String> diagnosisColumn,
                                                         String diagnosisFirst) {
        if (StrUtil.isBlank(diagnosisFirst)) {
            return;
        }
        if ("正常".equals(diagnosisFirst)) {
            // 与「排除」并列的独立选项：精确匹配「正常」；兼容旧筛选项「未进待诊断」口径
            wrapper.and(w -> w.eq(isLatentColumn, 0)
                    .or()
                    .eq(diagnosisColumn, "正常"));
            return;
        }
        if (isSuspectedTbDiagnosis(diagnosisFirst)) {
            wrapper.in(diagnosisColumn, SUSPECTED_TB_DIAGNOSIS, LEGACY_SUSPECTED_TB_DIAGNOSIS);
            return;
        }
        if ("确诊结核".equals(diagnosisFirst) || "确诊患者".equals(diagnosisFirst)) {
            wrapper.in(diagnosisColumn, "确诊结核", "确诊患者", "活动性肺结核");
            return;
        }
        wrapper.eq(diagnosisColumn, diagnosisFirst);
    }

    /**
     * 表头「诊断结果」筛选：与顶部筛选项口径一致；多选（逗号分隔）时为 OR。
     */
    public static <T> void applyScreeningDiagnosisColumnFilter(LambdaQueryWrapper<T> wrapper,
                                                               SFunction<T, Integer> isLatentColumn,
                                                               SFunction<T, String> diagnosisColumn,
                                                               String value) {
        List<String> values = ColumnFilterSupport.splitValues(value).stream().toList();
        if (values.isEmpty()) {
            return;
        }
        if (values.size() == 1) {
            applyScreeningDiagnosisFilter(wrapper, isLatentColumn, diagnosisColumn, values.get(0));
            return;
        }
        wrapper.and(outer -> {
            boolean first = true;
            for (String diagnosis : values) {
                if (first) {
                    outer.nested(sub -> applyScreeningDiagnosisFilter(sub, isLatentColumn, diagnosisColumn, diagnosis));
                    first = false;
                } else {
                    outer.or(sub -> applyScreeningDiagnosisFilter(sub, isLatentColumn, diagnosisColumn, diagnosis));
                }
            }
        });
    }

    /**
     * 潜伏/待诊断列表诊断结果筛选：「正常」匹配终态正常类诊断。
     */
    public static <T> void applyDiagnosisFirstFilter(LambdaQueryWrapper<T> wrapper,
                                                     SFunction<T, String> diagnosisColumn,
                                                     String diagnosisFirst) {
        if (StrUtil.isBlank(diagnosisFirst)) {
            return;
        }
        if ("正常".equals(diagnosisFirst)) {
            wrapper.eq(diagnosisColumn, "正常");
            return;
        }
        if (isSuspectedTbDiagnosis(diagnosisFirst)) {
            wrapper.in(diagnosisColumn, SUSPECTED_TB_DIAGNOSIS, LEGACY_SUSPECTED_TB_DIAGNOSIS);
            return;
        }
        if ("确诊结核".equals(diagnosisFirst) || "确诊患者".equals(diagnosisFirst)) {
            wrapper.in(diagnosisColumn, "确诊结核", "确诊患者", "活动性肺结核");
            return;
        }
        wrapper.eq(diagnosisColumn, diagnosisFirst);
    }

    /**
     * 密接筛查诊断结果筛选：将统一诊断选项映射为 finalScreeningResult 存储值。
     */
    public static List<String> resolveCloseContactDiagnosisFilterValues(String diagnosisResult) {
        if (StrUtil.isBlank(diagnosisResult)) {
            return List.of();
        }
        return switch (diagnosisResult) {
            case "排除", "正常" -> List.of("未发现异常");
            case "疑似结核", "疑似肺结核" -> List.of(SUSPECTED_TB_DIAGNOSIS, LEGACY_SUSPECTED_TB_DIAGNOSIS);
            case "确诊患者", "确诊结核", "在治患者" -> List.of("活动性肺结核", "确诊结核", "确诊患者", "在治患者");
            case "潜伏感染者" -> List.of("潜伏感染者");
            default -> List.of(diagnosisResult);
        };
    }
}
