package cn.luyou.task;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.SysMessage;
import cn.luyou.model.User;
import cn.luyou.service.CloseContactCaseService;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.SysMessageReminderConfigService;
import cn.luyou.service.SysMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 密接随访/复查提醒：
 * - 密接个案表：按 6/12/24 月随访日期提醒对应录入者
 * - 密接筛查表：保留 6/12 月窗口提醒，接收人改为录入者（不再广播四级）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloseContactReminderTask {

    public static final String REMINDER_CASE_6 = "close_contact_case_followup_6";
    public static final String REMINDER_CASE_12 = "close_contact_case_followup_12";
    public static final String REMINDER_CASE_24 = "close_contact_case_followup_24";
    public static final String REMINDER_SCREENING_6 = "close_contact_screening_review_6";
    public static final String REMINDER_SCREENING_12 = "close_contact_screening_review_12";

    private static final String MSG_TYPE = "review_reminder";
    /** 到期前后各 15 天窗口内提醒 */
    private static final int LEAD_DAYS = 15;

    private final CloseContactCaseService closeContactCaseService;
    private final ScreeningCloseContactService screeningCloseContactService;
    private final SysMessageService sysMessageService;
    private final SysMessageReminderConfigService reminderConfigService;
    private final UserMapper userMapper;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendReviewReminders() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(LEAD_DAYS);
        LocalDate windowEnd = today.plusDays(LEAD_DAYS);

        int case6 = sendCaseDueReminders(windowStart, windowEnd, REMINDER_CASE_6,
                "密接个案6月随访提醒",
                "密接个案【%s】的 6 月随访日期为 %s，请尽快完成随访录入。",
                true, false, false);
        int case12 = sendCaseDueReminders(windowStart, windowEnd, REMINDER_CASE_12,
                "密接个案12月随访提醒",
                "密接个案【%s】的 12 月随访日期为 %s，请尽快完成随访录入。",
                false, true, false);
        int case24 = sendCaseDueReminders(windowStart, windowEnd, REMINDER_CASE_24,
                "密接个案24月随访提醒",
                "密接个案【%s】的 24 月随访日期为 %s，请尽快完成随访录入。",
                false, false, true);

        int screening6 = 0;
        int screening12 = 0;
        if (reminderConfigService.isEnabled(REMINDER_SCREENING_6)) {
            List<ScreeningCloseContact> halfYearDue = screeningCloseContactService.list(
                    new LambdaQueryWrapper<ScreeningCloseContact>()
                            .notIn(ScreeningCloseContact::getCcStatus, 1, 2, 3, 5, 7, 8, 9)
                            .isNotNull(ScreeningCloseContact::getFirstScreenDate)
                            .isNull(ScreeningCloseContact::getFollowup6ScreenDate)
                            .le(ScreeningCloseContact::getFirstScreenDate, today.minusDays(165))
                            .ge(ScreeningCloseContact::getFirstScreenDate, today.minusDays(195))
            );
            screening6 = sendScreeningReminders(halfYearDue, "6月后复查提醒",
                    "密接人群【%s】首次筛查已满165天，请尽快安排6月后复查（原筛查日期：%s）",
                    false);
        }
        if (reminderConfigService.isEnabled(REMINDER_SCREENING_12)) {
            List<ScreeningCloseContact> oneYearDue = screeningCloseContactService.list(
                    new LambdaQueryWrapper<ScreeningCloseContact>()
                            .notIn(ScreeningCloseContact::getCcStatus, 1, 2, 3, 5, 7, 8, 9)
                            .isNotNull(ScreeningCloseContact::getFollowup6ScreenDate)
                            .isNull(ScreeningCloseContact::getFollowup12ScreenDate)
                            .le(ScreeningCloseContact::getFollowup6ScreenDate, today.minusDays(350))
                            .ge(ScreeningCloseContact::getFollowup6ScreenDate, today.minusDays(380))
            );
            screening12 = sendScreeningReminders(oneYearDue, "12月后复查提醒",
                    "密接人群【%s】6月后筛查已满350天，请尽快安排12月后复查（6月筛查日期：%s）",
                    true);
        }

        log.info("密接随访提醒完成：个案6/12/24月={}/{}/{}，筛查6/12月={}/{}",
                case6, case12, case24, screening6, screening12);
    }

    private int sendCaseDueReminders(LocalDate windowStart, LocalDate windowEnd, String reminderCode,
                                     String title, String contentTemplate,
                                     boolean month6, boolean month12, boolean month24) {
        if (!reminderConfigService.isEnabled(reminderCode)) {
            return 0;
        }
        LambdaQueryWrapper<CloseContactCase> wrapper = new LambdaQueryWrapper<>();
        if (month6) {
            wrapper.isNotNull(CloseContactCase::getFollowup6DueDate)
                    .isNull(CloseContactCase::getFollowup6ScreenDate)
                    .ge(CloseContactCase::getFollowup6DueDate, windowStart)
                    .le(CloseContactCase::getFollowup6DueDate, windowEnd);
        } else if (month12) {
            wrapper.isNotNull(CloseContactCase::getFollowup12DueDate)
                    .isNull(CloseContactCase::getFollowup12ScreenDate)
                    .ge(CloseContactCase::getFollowup12DueDate, windowStart)
                    .le(CloseContactCase::getFollowup12DueDate, windowEnd);
        } else if (month24) {
            wrapper.isNotNull(CloseContactCase::getFollowup24DueDate)
                    .isNull(CloseContactCase::getFollowup24ScreenDate)
                    .ge(CloseContactCase::getFollowup24DueDate, windowStart)
                    .le(CloseContactCase::getFollowup24DueDate, windowEnd);
        } else {
            return 0;
        }
        List<CloseContactCase> list = closeContactCaseService.list(wrapper);
        if (list.isEmpty()) {
            return 0;
        }
        Map<String, Long> usernameCache = new HashMap<>();
        int sent = 0;
        for (CloseContactCase record : list) {
            LocalDate dueDate = month6 ? record.getFollowup6DueDate()
                    : month12 ? record.getFollowup12DueDate()
                    : record.getFollowup24DueDate();
            Long receiverId = resolveCaseCreatorId(record, usernameCache);
            if (receiverId == null) {
                log.warn("密接个案随访提醒跳过：无录入者 caseId={} name={}", record.getId(), record.getName());
                continue;
            }
            if (alreadySentToday(receiverId, record.getId(), title)) {
                continue;
            }
            String content = String.format(contentTemplate,
                    StrUtil.blankToDefault(record.getName(), "未知"),
                    dueDate != null ? dueDate.toString() : "未知");
            sysMessageService.sendMessage(receiverId, title, content, MSG_TYPE, record.getId());
            sent++;
        }
        return sent;
    }

    private int sendScreeningReminders(List<ScreeningCloseContact> list, String title,
                                       String contentTemplate, boolean isOneYearReminder) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        Map<String, Long> usernameCache = new HashMap<>();
        int sent = 0;
        for (ScreeningCloseContact record : list) {
            Long receiverId = resolveScreeningCreatorId(record, usernameCache);
            if (receiverId == null) {
                log.warn("密接筛查复查提醒跳过：无录入者 screeningId={} name={}", record.getId(), record.getName());
                continue;
            }
            if (alreadySentToday(receiverId, record.getId(), title)) {
                continue;
            }
            LocalDate refDate = isOneYearReminder ? record.getFollowup6ScreenDate() : record.getFirstScreenDate();
            String content = String.format(contentTemplate,
                    StrUtil.blankToDefault(record.getName(), "未知"),
                    refDate != null ? refDate.toString() : "未知");
            sysMessageService.sendMessage(receiverId, title, content, MSG_TYPE, record.getId());
            sent++;
        }
        return sent;
    }

    private Long resolveScreeningCreatorId(ScreeningCloseContact record, Map<String, Long> cache) {
        if (record.getCreatorId() != null) {
            return record.getCreatorId();
        }
        return resolveUserIdByUsername(record.getCreatorUsername(), cache);
    }

    private Long resolveCaseCreatorId(CloseContactCase record, Map<String, Long> cache) {
        return resolveUserIdByUsername(record.getCreatorUsername(), cache);
    }

    private Long resolveUserIdByUsername(String username, Map<String, Long> cache) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        String key = username.trim();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, key).or().eq(User::getRealName, key))
                .last("LIMIT 1"));
        Long id = user != null ? user.getId() : null;
        cache.put(key, id);
        return id;
    }

    private boolean alreadySentToday(Long receiverId, Long bizId, String title) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return sysMessageService.count(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getBizId, bizId)
                .eq(SysMessage::getType, MSG_TYPE)
                .eq(SysMessage::getTitle, title)
                .ge(SysMessage::getCreateTime, start)) > 0;
    }
}
