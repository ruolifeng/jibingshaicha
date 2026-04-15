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
 * 密接人群半年/一年复查提醒定时任务
 * - 每天 08:00 执行
 * - 提前 15 天提醒：
 *   半年复查：active_round=1，首次筛查日期距今 ≥165 天
 *   一年复查：active_round=2，半年筛查日期距今 ≥350 天
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

        // 半年后复查提醒（首次 165 天 = 6个月 - 15天提前提醒）
        List<ScreeningCloseContact> halfYearDue = closeContactService.list(
                new LambdaQueryWrapper<ScreeningCloseContact>()
                        .eq(ScreeningCloseContact::getActiveRound, 0) // 0 表示首次阶段未变
                        .eq(ScreeningCloseContact::getIsLatent, 0)
                        .isNotNull(ScreeningCloseContact::getFirstScreenDate)
                        .le(ScreeningCloseContact::getFirstScreenDate, today.minusDays(165))
                        .ge(ScreeningCloseContact::getFirstScreenDate, today.minusDays(195)) // 30天窗口避免重复
        );

        sendBatchReminders(halfYearDue, "半年后复查提醒",
                "密接人群【%s】首次筛查已满165天，请尽快安排半年后复查（原筛查日期：%s）");

        // 一年后复查提醒（半年后筛查 350 天 = 12个月 - 15天提前提醒）
        List<ScreeningCloseContact> oneYearDue = closeContactService.list(
                new LambdaQueryWrapper<ScreeningCloseContact>()
                        .eq(ScreeningCloseContact::getActiveRound, 2)
                        .eq(ScreeningCloseContact::getIsLatent, 0)
                        .isNotNull(ScreeningCloseContact::getHalfYearScreenDate)
                        .le(ScreeningCloseContact::getHalfYearScreenDate, today.minusDays(350))
                        .ge(ScreeningCloseContact::getHalfYearScreenDate, today.minusDays(380))
        );

        sendBatchReminders(oneYearDue, "一年后复查提醒",
                "密接人群【%s】半年后筛查已满350天，请尽快安排一年后复查（半年筛查日期：%s）");

        log.info("密接人群复查提醒：半年到期 {} 人，一年到期 {} 人", halfYearDue.size(), oneYearDue.size());
    }

    private void sendBatchReminders(List<ScreeningCloseContact> list, String title, String contentTemplate) {
        if (list.isEmpty()) return;

        // 向所有4级用户发送提醒
        List<User> level4Users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, 5) // role=5 对应四级
        );

        for (ScreeningCloseContact record : list) {
            String dateStr = record.getActiveRound() != null && record.getActiveRound() == 2
                    ? (record.getHalfYearScreenDate() != null ? record.getHalfYearScreenDate().toString() : "未知")
                    : (record.getFirstScreenDate() != null ? record.getFirstScreenDate().toString() : "未知");
            String content = String.format(contentTemplate, record.getName(), dateStr);

            for (User user : level4Users) {
                if (StrUtil.isBlank(user.getOrgName())) continue;
                sysMessageService.sendMessage(user.getId(), title, content, "review_reminder", record.getId());
            }
        }
    }
}
