package cn.luyou.constant.disease;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 2026 秋季新生入学学生筛查表：数字码 → 中文。
 */
public final class SchoolScreeningCodes {

    private SchoolScreeningCodes() {
    }

    /** 类型：1–7 */
    public static final Map<String, String> SCHOOL_TYPE = Map.of(
            "1", "托幼机构",
            "2", "小学",
            "3", "初中",
            "4", "高中阶段教育学校",
            "5", "高等教育学校",
            "6", "教职工",
            "7", "其他"
    );

    /** 是否寄宿制：1–4（对齐新表说明：1-寄宿制，2-非寄宿制，3=大学，4=其他） */
    public static final Map<String, String> BOARDING_TYPE = Map.of(
            "1", "寄宿制",
            "2", "非寄宿制",
            "3", "大学",
            "4", "其他"
    );

    /** 感染筛查方法：1–4 */
    public static final Map<String, String> SCREEN_METHOD = Map.of(
            "1", "PPD",
            "2", "EC",
            "3", "IGRA",
            "4", "未查"
    );

    /** 判定结果：0–3 */
    public static final Map<String, String> INFECTION_JUDGE = Map.of(
            "0", "未感染",
            "1", "感染",
            "2", "无法判读",
            "3", "未查"
    );

    /** 胸部影像学方法：1–4 */
    public static final Map<String, String> CHEST_METHOD = Map.of(
            "1", "胸部X线",
            "2", "胸部CT",
            "3", "其他",
            "4", "未查"
    );

    /** 胸部影像学结果：0–4（入库保留细分类文案） */
    public static final Map<String, String> CHEST_RESULT = ordered(
            "0", "未见异常",
            "1", "异常（疑似活动性结核病变）",
            "2", "异常（非活动性结核病变）",
            "3", "其他",
            "4", "未查"
    );

    /** 分子生物学 / 痰培养：0–3 */
    public static final Map<String, String> LAB_RESULT = Map.of(
            "0", "阴性",
            "1", "阳性",
            "2", "无法判读",
            "3", "未查"
    );

    /**
     * 筛查结果：0–4。
     * 入库前再经 ScreeningDiagnosisSupport 规范：未发现异常→排除，活动性肺结核→确诊患者，疑似肺结核→疑似结核。
     */
    public static final Map<String, String> SCREENING_RESULT = ordered(
            "0", "排除",
            "1", "确诊患者",
            "2", "疑似结核",
            "3", "潜伏感染者",
            "4", "其他"
    );

    /** 官方原文对照（导入说明用，不直接入库） */
    public static final Map<String, String> SCREENING_RESULT_OFFICIAL = ordered(
            "0", "未发现异常",
            "1", "活动性肺结核",
            "2", "疑似肺结核",
            "3", "潜伏感染者",
            "4", "其他"
    );

    private static Map<String, String> ordered(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return Map.copyOf(map);
    }
}
