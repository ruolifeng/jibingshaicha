package cn.luyou.task;

import cn.luyou.model.Notice;
import cn.luyou.service.NoticeService;
import cn.luyou.service.SysMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知单超时提醒定时任务
 * 48h 未确认通知单 → 发送提醒消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeTimeoutTask {

    private final NoticeService noticeService;
    private final SysMessageService sysMessageService;

    @Scheduled(fixedRate = 3600000) // 每小时检查一次
    public void checkNoticeTimeout() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, 1) // 已发送未确认
                .eq(Notice::getTimeoutNotified, 0)
                .le(Notice::getSentTime, threshold);

        List<Notice> timeoutNotices = noticeService.list(wrapper);
        for (Notice notice : timeoutNotices) {
            String typeLabel = "latent".equals(notice.getNoticeType()) ? "潜伏者" : "患者";

            // 提醒发送方（4级）
            sysMessageService.sendMessage(
                    notice.getSenderId(),
                    "通知单超时提醒",
                    "对方（5级）未接收" + typeLabel + "通知单：" + notice.getPatientName(),
                    "notice_timeout",
                    notice.getId()
            );

            // 提醒接收方（5级）
            if (notice.getReceiverOrgId() != null) {
                sysMessageService.sendMessage(
                        notice.getReceiverOrgId(),
                        "通知单待接收",
                        "有" + typeLabel + "通知单待接收：" + notice.getPatientName(),
                        "notice_timeout",
                        notice.getId()
                );
            }

            notice.setTimeoutNotified(1);
            noticeService.updateById(notice);
        }

        if (!timeoutNotices.isEmpty()) {
            log.info("通知单超时提醒：处理了 {} 条超时通知单", timeoutNotices.size());
        }
    }
}
