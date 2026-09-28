package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 密接个案表 6/12/24 月复查提醒资格：
 * 仅最终筛查结果为潜伏感染者，且按预防性治疗开关分别发送；其余人员不提醒。
 */
public final class CloseContactCaseFollowupReminderSupport {

    private CloseContactCaseFollowupReminderSupport() {
    }

    /** 最终筛查结果是否为潜伏感染者（兼容筛查结果码 3） */
    public static boolean isLatentInfection(String finalScreeningResult) {
        return ScreeningDiagnosisSupport.isLatentInfectionDiagnosis(finalScreeningResult);
    }

    /**
     * @return true=已开展，false=未开展，null=无法归类（跳过）
     */
    public static Boolean classifyPreventiveTreatment(String raw) {
        if (StrUtil.isBlank(raw)) {
            return false;
        }
        String value = raw.trim();
        if ("开展".equals(value) || "是".equals(value)) {
            return true;
        }
        if ("未开展".equals(value) || "否".equals(value) || "不服药".equals(value)) {
            return false;
        }
        return null;
    }

    /**
     * 是否应发送个案随访提醒。
     * 非潜伏感染者一律不发；已开展/未开展再分别看对应开关。
     */
    public static boolean shouldSendCaseFollowupReminder(String finalScreeningResult,
                                                         String hasPreventiveTreatment,
                                                         boolean remindNoPreventive,
                                                         boolean remindWithPreventive) {
        if (!isLatentInfection(finalScreeningResult)) {
            return false;
        }
        Boolean withPreventive = classifyPreventiveTreatment(hasPreventiveTreatment);
        if (withPreventive == null) {
            return false;
        }
        return withPreventive ? remindWithPreventive : remindNoPreventive;
    }
}
