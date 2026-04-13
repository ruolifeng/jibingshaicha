package cn.luyou.task;

import cn.luyou.model.FirstVisit;
import cn.luyou.model.Notice;
import cn.luyou.service.FirstVisitService;
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
 * 首次随访超时提醒
 * 确认接收患者通知单后 72h 内未完成首次随访 → 提醒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitTimeoutTask {

    private final NoticeService noticeService;
    private final FirstVisitService firstVisitService;
    private final SysMessageService sysMessageService;

    @Scheduled(fixedRate = 3600000)
    public void checkFirstVisitTimeout() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(72);

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, 2)
                .eq(Notice::getNoticeType, "patient")
                .le(Notice::getConfirmedTime, threshold);

        List<Notice> notices = noticeService.list(wrapper);
        for (Notice notice : notices) {
            LambdaQueryWrapper<FirstVisit> fvWrapper = new LambdaQueryWrapper<>();
            fvWrapper.eq(FirstVisit::getPatientId, notice.getBizId());
            long count = firstVisitService.count(fvWrapper);

            if (count == 0 && notice.getReceiverOrgId() != null) {
                sysMessageService.sendMessage(
                        notice.getReceiverOrgId(),
                        "首次随访填写提醒",
                        "请尽快完成肺结核患者第一次入户随访记录表：" + notice.getPatientName(),
                        "visit_timeout",
                        notice.getBizId()
                );
            }
        }
    }
}
