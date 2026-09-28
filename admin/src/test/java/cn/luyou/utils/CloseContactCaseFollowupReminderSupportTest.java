package cn.luyou.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseContactCaseFollowupReminderSupportTest {

    @Test
    void nonLatentNeverRemindedEvenIfNoPreventive() {
        assertFalse(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "未发现异常", "未开展", true, true));
        assertFalse(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "活动性肺结核", "", true, true));
        assertFalse(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "疑似肺结核", "未开展", true, true));
        assertFalse(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "其他（需注明）", "未开展", true, true));
        assertFalse(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                null, "未开展", true, true));
    }

    @Test
    void latentWithoutTreatmentRemindedWhenNoPreventiveSwitchOn() {
        assertTrue(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "潜伏感染者", "未开展", true, false));
        assertTrue(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "潜伏感染者", null, true, false));
        assertTrue(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "3", "否", true, false));
        assertFalse(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "潜伏感染者", "未开展", false, true));
    }

    @Test
    void latentWithTreatmentOnlyWhenWithPreventiveSwitchOn() {
        assertFalse(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "潜伏感染者", "开展", true, false));
        assertTrue(CloseContactCaseFollowupReminderSupport.shouldSendCaseFollowupReminder(
                "潜伏感染者", "是", false, true));
    }
}
