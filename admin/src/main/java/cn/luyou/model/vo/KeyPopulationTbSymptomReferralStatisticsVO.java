package cn.luyou.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重点人群肺结核可疑症状筛查和推介情况报表（按区县）。
 * 字段口径见《自贡市重点人群结核病筛查季度报表（模块及数据统计来源）》。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyPopulationTbSymptomReferralStatisticsVO {

    /** 地区（区/县） */
    private String district;

    // ========== 老年人 ==========

    /** 老年人数（老年人及老年人+糖尿病） */
    private Long elderCount;

    /** 参加年度体检人数 */
    private Long elderAnnualExamCount;

    /** 进行症状筛查人数 */
    private Long elderSymptomScreenCount;

    /** 开展胸部影像学筛查人数 */
    private Long elderChestXrayCount;

    /** 开展感染筛查人数 */
    private Long elderInfectionScreenCount;

    /** 肺结核可疑症状人数 */
    private Long elderSuspiciousSymptomCount;

    /** 胸部影像学筛查异常人数 */
    private Long elderChestXrayAbnormalCount;

    /** 开展感染筛查异常人数 */
    private Long elderInfectionAbnormalCount;

    /** 开具推介转诊单人数（首次诊断为疑似结核） */
    private Long elderReferralFormCount;

    /** 到结核病定点医疗机构就诊人数（推介追踪到位） */
    private Long elderArrivedCount;

    /** 诊断为肺结核的人数（首次诊断为确诊结核/确诊患者） */
    private Long elderConfirmedTbCount;

    // ========== 糖尿病患者 ==========

    /** 管理的糖尿病患者数（单选糖尿病） */
    private Long diabetesManagedCount;

    /** 完成糖尿病管理季度随访的患者数 */
    private Long diabetesQuarterFollowCount;

    /** 进行症状筛查人数 */
    private Long diabetesSymptomScreenCount;

    /** 开展胸部影像学筛查人数 */
    private Long diabetesChestXrayCount;

    /** 开展感染筛查人数 */
    private Long diabetesInfectionScreenCount;

    /** 肺结核可疑症状人数 */
    private Long diabetesSuspiciousSymptomCount;

    /** 胸部影像学筛查异常人数 */
    private Long diabetesChestXrayAbnormalCount;

    /** 开展感染筛查异常人数 */
    private Long diabetesInfectionAbnormalCount;

    /** 开具推介转诊单人数 */
    private Long diabetesReferralFormCount;

    /** 到结核病定点医疗机构就诊人数 */
    private Long diabetesArrivedCount;

    /** 诊断为肺结核的人数 */
    private Long diabetesConfirmedTbCount;
}
