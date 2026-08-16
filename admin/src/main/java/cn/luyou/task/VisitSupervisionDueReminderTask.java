package cn.luyou.task;

import cn.luyou.service.VisitSupervisionReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 患者后续随访、潜伏感染者后续督导：按系统日期在下次日期前 7/3/1 天发送站内提醒。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitSupervisionDueReminderTask {

    private final VisitSupervisionReminderService visitSupervisionReminderService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDueReminders() {
        try {
            visitSupervisionReminderService.dispatchMessages();
        } catch (Exception e) {
            log.error("随访/督导到期提醒任务失败: {}", e.getMessage(), e);
        }
    }
}
