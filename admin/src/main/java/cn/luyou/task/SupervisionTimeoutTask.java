package cn.luyou.task;

import cn.luyou.model.Notice;
import cn.luyou.model.SupervisionForm;
import cn.luyou.service.NoticeService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 督导表超时提醒定时任务
 * 通知单确认后 72h 内未完成督导表 → 发送提醒消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisionTimeoutTask {

    private final NoticeService noticeService;
    private final SupervisionFormService supervisionFormService;
    private final SysMessageService sysMessageService;

    @Scheduled(fixedRate = 3600000)
    public void checkSupervisionTimeout() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(72);

        // 查找已确认但超过72h、且尚未发过督导超时提醒的潜伏者通知单
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, 2) // 已确认
                .eq(Notice::getNoticeType, "latent")
                .eq(Notice::getSupervisionTimeoutNotified, 0) // 防止重复发送
                .le(Notice::getConfirmedTime, threshold);

        List<Notice> confirmedNotices = noticeService.list(wrapper);
        for (Notice notice : confirmedNotices) {
            // 检查是否有对应的已完成督导表
            LambdaQueryWrapper<SupervisionForm> sfWrapper = new LambdaQueryWrapper<>();
            sfWrapper.eq(SupervisionForm::getLatentInfectionId, notice.getBizId())
                    .ge(SupervisionForm::getStatus, 1);
            long count = supervisionFormService.count(sfWrapper);

            if (count == 0 && notice.getReceiverOrgId() != null) {
                sysMessageService.sendMessage(
                        notice.getReceiverOrgId(),
                        "督导表填写提醒",
                        "请尽快完成结核病潜伏感染者预防性治疗督导表：" + notice.getPatientName(),
                        "supervision_timeout",
                        notice.getBizId()
                );
                // 标记已提醒，防止重复推送
                notice.setSupervisionTimeoutNotified(1);
                noticeService.updateById(notice);
            }
        }
    }
}
