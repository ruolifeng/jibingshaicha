package cn.luyou.service.impl;

import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.Notice;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.service.NoticeService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
        implements NoticeService {

    private final SysMessageService sysMessageService;

    @Override
    public void send(Notice notice) {
        boolean alreadySent = lambdaQuery()
                .eq(Notice::getBizId, notice.getBizId())
                .eq(Notice::getNoticeType, notice.getNoticeType())
                .eq(Notice::getPopulationType, notice.getPopulationType())
                .exists();
        if (alreadySent) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该通知单已发送，请勿重复发送");
        }
        notice.setStatus(1);
        notice.setSentTime(LocalDateTime.now());
        notice.setTimeoutNotified(0);
        save(notice);
        // 发送后，给接收方创建“待接收通知单”消息，接收方在消息管理中点击接收
        if (notice.getReceiverOrgId() != null) {
            String noticeTypeText = "patient".equals(notice.getNoticeType()) ? "患者通知单" : "潜伏者通知单";
            String title = "待接收" + noticeTypeText;
            String content = String.format("【%s】%s，发送方已下发，请在消息管理中点击“接收通知单”完成接收。", noticeTypeText, notice.getPatientName());
            sysMessageService.sendMessage(notice.getReceiverOrgId(), title, content, "notice_receive", notice.getId());
        }
    }

    @Override
    public void confirm(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单不存在");
        }
        if (notice.getStatus() == 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单已确认");
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (notice.getReceiverOrgId() != null && currentUserId != null
                && !notice.getReceiverOrgId().equals(currentUserId)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅通知单接收方可确认接收");
        }
        notice.setStatus(2);
        notice.setConfirmedTime(LocalDateTime.now());
        updateById(notice);
        // 接收后，给发送方回执消息，便于查看接收状态
        if (notice.getSenderId() != null) {
            String noticeTypeText = "patient".equals(notice.getNoticeType()) ? "患者通知单" : "潜伏者通知单";
            String title = noticeTypeText + "已接收";
            String content = String.format("【%s】%s，接收方已确认接收。", noticeTypeText, notice.getPatientName());
            sysMessageService.sendMessage(notice.getSenderId(), title, content, "notice_confirmed", notice.getId());
        }
    }
}
