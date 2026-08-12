package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ScreeningSchool;

import java.util.Arrays;
import java.util.List;

/**
 * 学生筛查统计：接受检查 / 规范检查口径（《学生统计报表》）。
 * <ul>
 *   <li>可疑症状：咳嗽/咯血/其他三列中，任一非空且非「未询问」→ 已做</li>
 *   <li>感染筛查：判定结果为 0/1/2（未感染/感染/无法判读）→ 已做</li>
 *   <li>胸部影像学：结果为 0/1/2/3 → 已做</li>
 *   <li>接受检查：三者任一已做</li>
 *   <li>规范检查：按学校分类规则判定</li>
 * </ul>
 */
public final class SchoolScreeningStatSupport {

    private SchoolScreeningStatSupport() {
    }

    /** 报表学校分类（对齐《学生统计报表》行序） */
    public enum ReportCategory {
        KINDERGARTEN_PRIMARY("幼儿园、小学"),
        NON_BOARDING_JUNIOR("非寄宿制初中"),
        HIGH_AND_BOARDING_JUNIOR("高中和寄宿制初中"),
        UNIVERSITY("大学"),
        STAFF_OTHER("其他（教职工）");

        private final String label;

        ReportCategory(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static List<ReportCategory> ordered() {
            return Arrays.asList(values());
        }

        public static ReportCategory fromLabel(String label) {
            if (StrUtil.isBlank(label)) {
                return null;
            }
            String trimmed = label.trim();
            for (ReportCategory c : values()) {
                if (c.label.equals(trimmed)) {
                    return c;
                }
            }
            return null;
        }
    }

    /** 三个检查方法中，任一项已做 → 接受检查 */
    public static boolean isAcceptedExamined(ScreeningSchool row) {
        return isSymptomDone(row) || isInfectionDone(row) || isChestImagingDone(row);
    }

    /** 按学校分类规则判定是否计入规范检查人数 */
    public static boolean isStandardizedExamined(ScreeningSchool row) {
        return isStandardizedExamined(row, resolveReportCategory(row));
    }

    public static boolean isStandardizedExamined(ScreeningSchool row, ReportCategory category) {
        if (category == null) {
            category = ReportCategory.STAFF_OTHER;
        }
        return switch (category) {
            case KINDERGARTEN_PRIMARY -> isSymptomDone(row);
            case NON_BOARDING_JUNIOR, HIGH_AND_BOARDING_JUNIOR -> isJuniorSeniorStandardized(row);
            case UNIVERSITY -> isSymptomDone(row) && isChestImagingDone(row);
            // 教职工/其他：症状 + 胸部影像学（与大学一致），各类合并汇总
            case STAFF_OTHER -> isSymptomDone(row) && isChestImagingDone(row);
        };
    }

    /**
     * 从表字段「类型/是否寄宿制」映射报表学校分类。
     */
    public static ReportCategory resolveReportCategory(ScreeningSchool row) {
        String type = StrUtil.blankToDefault(row.getSchoolType(), "").trim();
        String boarding = StrUtil.blankToDefault(row.getBoardingType(), "").trim();

        if (type.contains("教职工") || "其他".equals(type) || type.contains("培训") || type.contains("特殊教育")) {
            return ReportCategory.STAFF_OTHER;
        }
        if (type.contains("高等") || type.contains("大学") || "大学".equals(boarding)) {
            return ReportCategory.UNIVERSITY;
        }
        if (type.contains("高中")) {
            return ReportCategory.HIGH_AND_BOARDING_JUNIOR;
        }
        if (type.contains("初中")) {
            if ("寄宿制".equals(boarding)) {
                return ReportCategory.HIGH_AND_BOARDING_JUNIOR;
            }
            return ReportCategory.NON_BOARDING_JUNIOR;
        }
        if (type.contains("托幼") || type.contains("幼儿") || type.contains("小学")) {
            return ReportCategory.KINDERGARTEN_PRIMARY;
        }
        return ReportCategory.STAFF_OTHER;
    }

    /**
     * 初中&高中：同时做过可疑症状 + 感染筛查；
     * 若感染判定为 1（感染），还须胸部影像学已做。
     */
    private static boolean isJuniorSeniorStandardized(ScreeningSchool row) {
        if (!isSymptomDone(row) || !isInfectionDone(row)) {
            return false;
        }
        if (isInfectionPositive(row) && !isChestImagingDone(row)) {
            return false;
        }
        return true;
    }

    /**
     * 可疑症状已做：三列中任一非空且非「未询问」。
     */
    public static boolean isSymptomDone(ScreeningSchool row) {
        return isSymptomFieldDone(row.getSymptomCough())
                || isSymptomFieldDone(row.getSymptomHemoptysis())
                || isSymptomFieldDone(row.getSymptomOther());
    }

    private static boolean isSymptomFieldDone(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        String trimmed = value.trim();
        return !"未询问".equals(trimmed) && !"未问".equals(trimmed);
    }

    /**
     * 感染筛查已做：判定结果为 0/1/2（未感染/感染/无法判读）。
     */
    public static boolean isInfectionDone(ScreeningSchool row) {
        String code = infectionJudgeCode(row.getInfectionResult());
        return "0".equals(code) || "1".equals(code) || "2".equals(code);
    }

    /** 感染判定为 1（感染） */
    public static boolean isInfectionPositive(ScreeningSchool row) {
        return "1".equals(infectionJudgeCode(row.getInfectionResult()));
    }

    /**
     * 胸部影像学已做：结果为 0/1/2/3（未见异常/异常两类/其他）。
     */
    public static boolean isChestImagingDone(ScreeningSchool row) {
        String code = chestResultCode(row.getChestXrayResult());
        return "0".equals(code) || "1".equals(code) || "2".equals(code) || "3".equals(code);
    }

    private static String infectionJudgeCode(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return "";
        }
        String code = SchoolScreeningCodeSupport.fromInfectionResult(infectionResult.trim());
        if ("0".equals(code) || "1".equals(code) || "2".equals(code) || "3".equals(code)) {
            return code;
        }
        String trimmed = infectionResult.trim();
        if ("未感染".equals(trimmed)) return "0";
        if ("感染".equals(trimmed) || ScreeningDiagnosisSupport.isPositiveInfection(trimmed)) return "1";
        if ("无法判读".equals(trimmed) || "未判读".equals(trimmed)) return "2";
        if ("未查".equals(trimmed) || "未做".equals(trimmed)) return "3";
        return "";
    }

    private static String chestResultCode(String chestResult) {
        if (StrUtil.isBlank(chestResult)) {
            return "";
        }
        String code = SchoolScreeningCodeSupport.fromChestXrayResult(chestResult.trim());
        if ("0".equals(code) || "1".equals(code) || "2".equals(code) || "3".equals(code) || "4".equals(code)) {
            return code;
        }
        String trimmed = chestResult.trim();
        if ("未见异常".equals(trimmed) || "正常".equals(trimmed)) return "0";
        if (trimmed.startsWith("异常") || "异常".equals(trimmed)) return "1";
        if ("其他".equals(trimmed) || "其它".equals(trimmed)) return "3";
        if ("未查".equals(trimmed) || "未做".equals(trimmed)) return "4";
        return "";
    }

    /** 发现肺结核患者（筛查结果为确诊患者/活动性肺结核） */
    public static boolean isTbPatientFound(ScreeningSchool row) {
        if (row == null || StrUtil.isBlank(row.getDiagnosisFirst())) {
            return false;
        }
        if (ScreeningDiagnosisSupport.isConfirmedPatientDiagnosis(row.getDiagnosisFirst())) {
            return true;
        }
        String normalized = ScreeningDiagnosisSupport.normalizeDiagnosis(row.getDiagnosisFirst());
        return "活动性肺结核".equals(normalized)
                || (normalized != null && normalized.contains("活动性肺结核"));
    }
}
