package cn.luyou.task;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.User;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.SysMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 密接人群6月/12月复查提醒定时任务
 * - 每天 08:00 执行
 * - 提前 15 天提醒：
 *   6月复查：首次筛查日期距今 ≥165 天且未做6月复查
 *   12月复查：6月复查日期距今 ≥350 天且未做12月复查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloseContactReminderTask {

    private final ScreeningCloseContactService closeContactService;
    private final SysMessageService sysMessageService;
    private final UserMapper userMapper;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendReviewReminders() {
        LocalDate today = LocalDate.now();

        // 6月后复查提醒：首次筛查已过 165 天，且尚未进行 6月随访（followup6ScreenDate 为空）
        // ccStatus 排除已确认为患者(1)或潜伏感染者已归档(3)的记录
        List<ScreeningCloseContact> halfYearDue = closeContactService.list(
                new LambdaQueryWrapper<ScreeningCloseContact>()
                        .notIn(ScreeningCloseContact::getCcStatus, 1, 2, 3, 5, 7, 8)
                        .isNotNull(ScreeningCloseContact::getFirstScreenDate)
                        .isNull(ScreeningCloseContact::getFollowup6ScreenDate)
                        .le(ScreeningCloseContact::getFirstScreenDate, today.minusDays(165))
                        .ge(ScreeningCloseContact::getFirstScreenDate, today.minusDays(195))
        );

        sendBatchReminders(halfYearDue, "6月后复查提醒",
                "密接人群【%s】首次筛查已满165天，请尽快安排6月后复查（原筛查日期：%s）",
                false);

        // 12月后复查提醒：6月随访日期距今 ≥350 天，且尚未进行 12月随访
        List<ScreeningCloseContact> oneYearDue = closeContactService.list(
                new LambdaQueryWrapper<ScreeningCloseContact>()
                        .notIn(ScreeningCloseContact::getCcStatus, 1, 2, 3, 5, 7, 8)
                        .isNotNull(ScreeningCloseContact::getFollowup6ScreenDate)
                        .isNull(ScreeningCloseContact::getFollowup12ScreenDate)
                        .le(ScreeningCloseContact::getFollowup6ScreenDate, today.minusDays(350))
                        .ge(ScreeningCloseContact::getFollowup6ScreenDate, today.minusDays(380))
        );

        sendBatchReminders(oneYearDue, "12月后复查提醒",
                "密接人群【%s】6月后筛查已满350天，请尽快安排12月后复查（6月筛查日期：%s）",
                true);

        log.info("密接人群复查提醒：6月到期 {} 人，12月到期 {} 人", halfYearDue.size(), oneYearDue.size());
    }

    /**
     * @param isOneYearReminder true=12月提醒（取followup6ScreenDate作为提示日期），false=6月提醒（取firstScreenDate）
     */
    private void sendBatchReminders(List<ScreeningCloseContact> list, String title,
                                    String contentTemplate, boolean isOneYearReminder) {
        if (list.isEmpty()) return;

        // 向所有4级用户发送提醒
        List<User> level4Users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, 5) // role=5 对应四级
        );

        for (ScreeningCloseContact record : list) {
            LocalDate refDate = isOneYearReminder ? record.getFollowup6ScreenDate() : record.getFirstScreenDate();
            String dateStr = refDate != null ? refDate.toString() : "未知";
            String content = String.format(contentTemplate, record.getName(), dateStr);

            for (User user : level4Users) {
                if (StrUtil.isBlank(user.getOrgName())) continue;
                sysMessageService.sendMessage(user.getId(), title, content, "review_reminder", record.getId());
            }
        }
    }
}
