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

    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
            "PPD+", "PPD++", "PPD+++", "EC阳性", "IGRA阳性"
    );

    /** 终态「正常」类诊断：不进入待诊断，导入后直接结束流程 */
    private static final Set<String> NORMAL_TERMINAL_DIAGNOSIS = Set.of(
            "正常", "排除", "其他", "其它"
    );

    /** 需进入后续流程的诊断 */
    private static final Set<String> ACTIONABLE_DIAGNOSIS = Set.of(
            "疑似肺结核", "潜伏感染者", "确诊患者"
    );

    private ScreeningDiagnosisSupport() {
    }

    public static boolean isPositiveInfection(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return false;
        }
        return POSITIVE_KEYWORDS.stream().anyMatch(infectionResult::contains);
    }

    /** 感染筛查结果为阴性（含「阴性」文案） */
    public static boolean isNormalInfection(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return false;
        }
        return infectionResult.contains("阴性");
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
     * 是否应标记 isLatent=1 并创建潜伏/待诊断记录。
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
        if (!isPositiveInfection(infectionResult) && "正常".equals(chestXrayResult)) {
            return false;
        }
        return isActionableDiagnosis(diagnosisFirst);
    }

    /** 统一「其它」与「其他」 */
    public static String normalizeDiagnosis(String diagnosis) {
        if (StrUtil.isBlank(diagnosis)) {
            return diagnosis;
        }
        return "其它".equals(diagnosis.trim()) ? "其他" : diagnosis.trim();
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
            wrapper.and(w -> w.eq(isLatentColumn, 0)
                    .or()
                    .in(diagnosisColumn, NORMAL_TERMINAL_DIAGNOSIS));
            return;
        }
        wrapper.eq(diagnosisColumn, diagnosisFirst);
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
            wrapper.in(diagnosisColumn, NORMAL_TERMINAL_DIAGNOSIS);
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
            case "疑似肺结核" -> List.of("疑似肺结核");
            case "确诊患者" -> List.of("活动性肺结核");
            case "潜伏感染者" -> List.of("潜伏感染者");
            default -> List.of(diagnosisResult);
        };
    }
}
