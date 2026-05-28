package cn.luyou.service.impl;

import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.Notice;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.model.User;
import cn.luyou.model.vo.SentNoticeVO;
import cn.luyou.service.NoticeService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DataScopeHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
        implements NoticeService {

    private final SysMessageService sysMessageService;
    private final UserMapper userMapper;
    private final DataScopeHelper dataScopeHelper;

    @Override
    public void saveAsDraft(Notice notice) {
        assertBizAccessible(notice);
        Notice existing = lambdaQuery()
                .eq(Notice::getBizId, notice.getBizId())
                .eq(Notice::getNoticeType, notice.getNoticeType())
                .eq(Notice::getPopulationType, notice.getPopulationType())
                .one();
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == 1) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单已发送，等待接收方确认，无法保存草稿");
            }
            notice.setId(existing.getId());
            notice.setStatus(0);
            updateById(notice);
        } else {
            notice.setStatus(0);
            save(notice);
        }
    }

    @Override
    public void send(Notice notice) {
        assertBizAccessible(notice);
        Notice existing = lambdaQuery()
                .eq(Notice::getBizId, notice.getBizId())
                .eq(Notice::getNoticeType, notice.getNoticeType())
                .eq(Notice::getPopulationType, notice.getPopulationType())
                .one();
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == 1) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单已发送，等待接收方确认，请勿重复发送");
            }
            // 草稿(0)或已确认(2)均可更新后重新发送
            notice.setId(existing.getId());
        }
        notice.setStatus(1);
        notice.setSentTime(LocalDateTime.now());
        notice.setTimeoutNotified(0);
        if (existing != null) {
            updateById(notice);
        } else {
            save(notice);
        }
        // 发送后给接收方创建待接收消息
        if (notice.getReceiverOrgId() != null) {
            String noticeTypeText = "patient".equals(notice.getNoticeType()) ? "患者通知单" : "潜伏者通知单";
            String title = "待接收" + noticeTypeText;
            String content = String.format("【%s】%s，发送方已下发，请在消息管理中点击接收通知单完成接收。", noticeTypeText, notice.getPatientName());
            sysMessageService.sendMessage(notice.getReceiverOrgId(), title, content, "notice_receive", notice.getId());
        }
    }

    @Override
    public IPage<SentNoticeVO> sentPage(Long senderId, int pageNum, int size) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getSenderId, senderId)
                .orderByDesc(Notice::getSentTime);
        IPage<Notice> noticePage = page(new Page<>(pageNum, size), wrapper);

        // 批量查询发送者和接收者用户信息，避免 N+1 查询
        Set<Long> userIds = noticePage.getRecords().stream()
                .flatMap(n -> {
                    java.util.stream.Stream.Builder<Long> b = java.util.stream.Stream.builder();
                    if (n.getSenderId() != null) b.accept(n.getSenderId());
                    if (n.getReceiverOrgId() != null) b.accept(n.getReceiverOrgId());
                    return b.build();
                })
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<SentNoticeVO> voList = noticePage.getRecords().stream().map(n -> {
            SentNoticeVO vo = new SentNoticeVO();
            vo.setId(n.getId());
            vo.setNoticeType(n.getNoticeType());
            vo.setPopulationType(n.getPopulationType());
            vo.setPatientName(n.getPatientName());
            vo.setSenderId(n.getSenderId());
            vo.setReceiverOrgId(n.getReceiverOrgId());
            vo.setStatus(n.getStatus());
            vo.setSentTime(n.getSentTime());
            vo.setConfirmedTime(n.getConfirmedTime());
            User sender = userMap.get(n.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
                vo.setSenderOrgName(sender.getOrgName());
            }
            User receiver = userMap.get(n.getReceiverOrgId());
            if (receiver != null) {
                vo.setReceiverName(receiver.getRealName() != null ? receiver.getRealName() : receiver.getUsername());
                vo.setReceiverOrgName(receiver.getOrgName());
            }
            return vo;
        }).collect(Collectors.toList());

        IPage<SentNoticeVO> result = new Page<>(noticePage.getCurrent(), noticePage.getSize(), noticePage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public List<Notice> listByBizWithUsers(Long bizId, String noticeType) {
        assertBizAccessible(bizId, noticeType);
        List<Notice> notices = lambdaQuery()
                .eq(Notice::getBizId, bizId)
                .eq(Notice::getNoticeType, noticeType)
                .orderByDesc(Notice::getCreateTime)
                .list();
        if (notices.isEmpty()) {
            return notices;
        }
        // 批量查询下发人与接收人信息
        Set<Long> userIds = notices.stream()
                .flatMap(n -> {
                    java.util.stream.Stream.Builder<Long> b = java.util.stream.Stream.builder();
                    if (n.getSenderId() != null) b.accept(n.getSenderId());
                    if (n.getReceiverOrgId() != null) b.accept(n.getReceiverOrgId());
                    return b.build();
                })
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        notices.forEach(n -> {
            User sender = userMap.get(n.getSenderId());
            if (sender != null) {
                n.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
                n.setSenderOrgName(sender.getOrgName());
            }
            User receiver = userMap.get(n.getReceiverOrgId());
            if (receiver != null) {
                n.setReceiverName(receiver.getRealName() != null ? receiver.getRealName() : receiver.getUsername());
                n.setReceiverOrgName(receiver.getOrgName());
            }
        });
        return notices;
    }

    @Override
    public Notice getDetailWithUsers(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            return null;
        }
        Set<Long> userIds = new java.util.HashSet<>();
        if (notice.getSenderId() != null) userIds.add(notice.getSenderId());
        if (notice.getReceiverOrgId() != null) userIds.add(notice.getReceiverOrgId());
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            User sender = userMap.get(notice.getSenderId());
            if (sender != null) {
                notice.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
                notice.setSenderOrgName(sender.getOrgName());
            }
            User receiver = userMap.get(notice.getReceiverOrgId());
            if (receiver != null) {
                notice.setReceiverName(receiver.getRealName() != null ? receiver.getRealName() : receiver.getUsername());
                notice.setReceiverOrgName(receiver.getOrgName());
            }
        }
        return notice;
    }

    @Override
    public void remind(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单不存在");
        }
        if (notice.getStatus() == 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单已确认接收，无需催促");
        }
        if (notice.getReceiverOrgId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单无接收方");
        }
        String noticeTypeText = "patient".equals(notice.getNoticeType()) ? "患者通知单" : "潜伏者通知单";
        String title = "催促：待接收" + noticeTypeText;
        String content = String.format("【催促】【%s】%s，发送方再次提醒，请尽快在消息管理中点击接收通知单完成接收。", noticeTypeText, notice.getPatientName());
        sysMessageService.sendMessage(notice.getReceiverOrgId(), title, content, "notice_receive", notice.getId());
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
        // 接收后给发送方回执消息
        if (notice.getSenderId() != null) {
            String noticeTypeText = "patient".equals(notice.getNoticeType()) ? "患者通知单" : "潜伏者通知单";
            String title = noticeTypeText + "已接收";
            String content = String.format("【%s】%s，接收方已确认接收。", noticeTypeText, notice.getPatientName());
            sysMessageService.sendMessage(notice.getSenderId(), title, content, "notice_confirmed", notice.getId());
        }
    }

    private void assertBizAccessible(Notice notice) {
        if (notice == null) {
            return;
        }
        assertBizAccessible(notice.getBizId(), notice.getNoticeType());
    }

    private void assertBizAccessible(Long bizId, String noticeType) {
        if ("patient".equals(noticeType)) {
            dataScopeHelper.assertPatientAccessible(bizId);
        } else if ("latent".equals(noticeType)) {
            dataScopeHelper.assertLatentAccessible(bizId);
        }
    }
}
