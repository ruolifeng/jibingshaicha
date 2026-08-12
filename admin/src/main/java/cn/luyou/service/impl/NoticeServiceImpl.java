package cn.luyou.service.impl;

import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.Notice;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.model.vo.SentNoticeVO;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.NoticePartyFillSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
        implements NoticeService {

    private final SysMessageService sysMessageService;
    private final DataScopeHelper dataScopeHelper;
    private final PatientService patientService;
    private final LatentInfectionService latentInfectionService;
    private final NoticePartyFillSupport noticePartyFillSupport;

    public NoticeServiceImpl(
            SysMessageService sysMessageService,
            DataScopeHelper dataScopeHelper,
            PatientService patientService,
            @Lazy LatentInfectionService latentInfectionService,
            NoticePartyFillSupport noticePartyFillSupport) {
        this.sysMessageService = sysMessageService;
        this.dataScopeHelper = dataScopeHelper;
        this.patientService = patientService;
        this.latentInfectionService = latentInfectionService;
        this.noticePartyFillSupport = noticePartyFillSupport;
    }

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
        }
        ensureSenderId(notice);
        if (existing != null) {
            notice.setStatus(0);
            updateById(notice);
        } else {
            notice.setStatus(0);
            save(notice);
        }
        syncLatentRegistrationNo(notice);
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
        ensureSenderId(notice);
        notice.setStatus(1);
        notice.setSentTime(LocalDateTime.now());
        notice.setTimeoutNotified(0);
        if (existing != null) {
            updateById(notice);
        } else {
            save(notice);
        }
        syncLatentRegistrationNo(notice);
        // 发送后给接收方创建待接收消息
        if (notice.getReceiverOrgId() != null) {
            String noticeTypeText = "patient".equals(notice.getNoticeType()) ? "患者通知单" : "潜伏者通知单";
            String title = "待接收" + noticeTypeText;
            String content = String.format("【%s】%s，发送方已下发，请在消息管理中点击接收通知单完成接收。", noticeTypeText, notice.getPatientName());
            sysMessageService.sendMessage(notice.getReceiverOrgId(), title, content, "notice_receive", notice.getId());
        }
    }

    /** 潜伏感染者通知单：登记号回写潜伏感染主表 */
    private void syncLatentRegistrationNo(Notice notice) {
        if (notice == null || !"latent".equals(notice.getNoticeType()) || notice.getBizId() == null) {
            return;
        }
        latentInfectionService.syncRegistrationNoFromNotice(notice.getBizId(), notice.getRegistrationNo());
    }

    @Override
    public IPage<SentNoticeVO> sentPage(Long senderId, int pageNum, int size) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Notice::getStatus, 1);
        // 转出副本挂在 source_* 非空业务上，已发送列表只保留真实发送记录
        excludeTransferCopiedNotices(wrapper);
        applySentNoticeScope(wrapper, senderId);
        wrapper.orderByDesc(Notice::getSentTime);
        IPage<Notice> noticePage = page(new Page<>(pageNum, size), wrapper);

        // 已发送列表同样补全姓名（含系统消息回退），与通知单管理口径一致
        noticePartyFillSupport.fillPartyNames(noticePage.getRecords());

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
            vo.setSenderName(n.getSenderName());
            vo.setSenderOrgName(n.getSenderOrgName());
            vo.setReceiverName(n.getReceiverName());
            vo.setReceiverOrgName(n.getReceiverOrgName());
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
        noticePartyFillSupport.fillPartyNames(notices);
        return notices;
    }

    @Override
    public Notice getDetailWithUsers(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            return null;
        }
        noticePartyFillSupport.fillPartyNames(List.of(notice));
        return notice;
    }

    @Override
    public void remind(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单不存在");
        }
        assertSentNoticeAccessible(notice.getId());
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

    /**
     * 已发送通知单列表：五级仅看自己发送的；市/县/社区等上级按辖区范围（与统计看板一致）。
     */
    private void applySentNoticeScope(LambdaQueryWrapper<Notice> wrapper, Long currentUserId) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 6) {
            wrapper.eq(Notice::getSenderId, currentUserId);
            return;
        }
        dataScopeHelper.applyNoticeScope(wrapper);
    }

    /**
     * 排除挂在「转出副本」业务上的通知单（patient.source_patient_id / latent.source_latent_id 非空）。
     * 原发送记录仍保留；副本仅供接收方业务详情使用，不应出现在「已发送通知单」。
     */
    private void excludeTransferCopiedNotices(LambdaQueryWrapper<Notice> wrapper) {
        wrapper.and(w -> w
                .nested(n -> n.eq(Notice::getNoticeType, "patient")
                        .notInSql(Notice::getBizId,
                                "SELECT id FROM patient WHERE source_patient_id IS NOT NULL AND deleted = 0"))
                .or()
                .nested(n -> n.eq(Notice::getNoticeType, "latent")
                        .notInSql(Notice::getBizId,
                                "SELECT id FROM latent_infection WHERE source_latent_id IS NOT NULL AND deleted = 0"))
                .or()
                .notIn(Notice::getNoticeType, "patient", "latent"));
    }

    /** 催促前校验：仅允许查看辖区内已发送通知单的用户操作 */
    private void assertSentNoticeAccessible(Long noticeId) {
        if (noticeId == null || BaseContext.isSuperAdmin()) {
            return;
        }
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getId, noticeId);
        wrapper.ge(Notice::getStatus, 1);
        applySentNoticeScope(wrapper, BaseContext.getCurrentId());
        if (count(wrapper) == 0) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权限操作该通知单");
        }
    }

    /** 保存/发送时补全填写人，便于列表按录入者检索（更新草稿时保留原填写人） */
    private void ensureSenderId(Notice notice) {
        if (notice == null || notice.getSenderId() != null) {
            return;
        }
        if (notice.getId() != null) {
            Notice existing = getById(notice.getId());
            if (existing != null && existing.getSenderId() != null) {
                notice.setSenderId(existing.getSenderId());
                return;
            }
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            notice.setSenderId(currentId);
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
            patientService.assertPatientOperable(bizId);
        } else if ("latent".equals(noticeType)) {
            dataScopeHelper.assertLatentAccessible(bizId);
            latentInfectionService.assertLatentOperable(bizId);
        }
    }
}
