package cn.luyou.service.impl;

import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.ReferralMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.Referral;
import cn.luyou.model.User;
import cn.luyou.model.vo.ReferralDetailVO;
import cn.luyou.model.vo.SentReferralVO;
import cn.luyou.service.ReferralService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
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
public class ReferralServiceImpl extends ServiceImpl<ReferralMapper, Referral>
        implements ReferralService {

    private final SysMessageService sysMessageService;
    private final UserMapper userMapper;

    private static final Map<String, String> MODULE_LABEL = Map.of(
            "screening", "筛查管理",
            "suspected", "疑似结核管理",
            "latent", "潜伏感染者管理",
            "patient", "患者管理"
    );

    private static final Map<String, String> POPULATION_LABEL = Map.of(
            "school", "学校人群",
            "key", "重点人群",
            "close", "密接人群"
    );

    @Override
    public void send(Referral referral) {
        referral.setSenderId(BaseContext.getCurrentId());
        referral.setStatus(1);
        referral.setSentTime(LocalDateTime.now());
        save(referral);

        // 向接收方推送消息通知
        if (referral.getReceiverOrgId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "待确认分级诊疗";
            String content = String.format("【%s - %s】%s，发送方已发起分级诊疗推送，请在消息管理中确认接收。",
                    popLabel, moduleLabel, referral.getSubjectName());
            sysMessageService.sendMessage(referral.getReceiverOrgId(), title, content,
                    "referral_receive", referral.getId());
        }
    }

    @Override
    public void confirm(Long id) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分级诊疗记录不存在");
        }
        if (referral.getStatus() == 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已确认接收，请勿重复操作");
        }
        if (referral.getStatus() == 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已拒绝，不可再确认");
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (referral.getReceiverOrgId() != null && currentUserId != null
                && !referral.getReceiverOrgId().equals(currentUserId)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅接收方可确认");
        }
        referral.setStatus(2);
        referral.setConfirmedTime(LocalDateTime.now());
        updateById(referral);

        // 回执给发送方
        if (referral.getSenderId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "分级诊疗已接收";
            String content = String.format("【%s - %s】%s，接收方已确认接收分级诊疗信息。",
                    popLabel, moduleLabel, referral.getSubjectName());
            sysMessageService.sendMessage(referral.getSenderId(), title, content,
                    "referral_confirmed", referral.getId());
        }
    }

    @Override
    public void reject(Long id, String rejectReason) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分级诊疗记录不存在");
        }
        if (referral.getStatus() == 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已确认接收，无法拒绝");
        }
        if (referral.getStatus() == 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已拒绝，无需重复操作");
        }
        referral.setStatus(3);
        referral.setRejectedTime(LocalDateTime.now());
        referral.setRejectReason(rejectReason);
        updateById(referral);

        // 通知发送方被拒绝
        if (referral.getSenderId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "分级诊疗已被拒绝";
            String reason = rejectReason != null && !rejectReason.isBlank() ? "，原因：" + rejectReason : "";
            String content = String.format("【%s - %s】%s，接收方已拒绝分级诊疗%s，您可重新发起。",
                    popLabel, moduleLabel, referral.getSubjectName(), reason);
            sysMessageService.sendMessage(referral.getSenderId(), title, content,
                    "referral_rejected", referral.getId());
        }
    }

    @Override
    public void resend(Long id) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分级诊疗记录不存在");
        }
        if (referral.getStatus() != 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅拒绝状态的分级诊疗可重新发起");
        }
        referral.setStatus(1);
        referral.setSentTime(LocalDateTime.now());
        referral.setRejectedTime(null);
        referral.setRejectReason(null);
        updateById(referral);

        if (referral.getReceiverOrgId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "待确认分级诊疗（重新发起）";
            String content = String.format("【%s - %s】%s，发送方重新发起了分级诊疗推送，请在消息管理中确认接收。",
                    popLabel, moduleLabel, referral.getSubjectName());
            sysMessageService.sendMessage(referral.getReceiverOrgId(), title, content,
                    "referral_receive", referral.getId());
        }
    }

    @Override
    public ReferralDetailVO detail(Long id) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "分级诊疗记录不存在");
        }

        ReferralDetailVO vo = new ReferralDetailVO();
        vo.setId(referral.getId());
        vo.setBizType(referral.getBizType());
        vo.setPopulationType(referral.getPopulationType());
        vo.setModuleType(referral.getModuleType());
        vo.setSubjectName(referral.getSubjectName());
        vo.setSummary(referral.getSummary());
        vo.setSenderId(referral.getSenderId());
        vo.setReceiverOrgId(referral.getReceiverOrgId());
        vo.setStatus(referral.getStatus());
        vo.setSentTime(referral.getSentTime());
        vo.setConfirmedTime(referral.getConfirmedTime());
        vo.setRejectedTime(referral.getRejectedTime());
        vo.setRejectReason(referral.getRejectReason());

        // 填充发送方与接收方用户信息
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (referral.getSenderId() != null) ids.add(referral.getSenderId());
        if (referral.getReceiverOrgId() != null) ids.add(referral.getReceiverOrgId());
        if (!ids.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            User sender = userMap.get(referral.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
                vo.setSenderOrgName(sender.getOrgName());
            }
            User receiver = userMap.get(referral.getReceiverOrgId());
            if (receiver != null) {
                vo.setReceiverName(receiver.getRealName() != null ? receiver.getRealName() : receiver.getUsername());
                vo.setReceiverOrgName(receiver.getOrgName());
            }
        }
        return vo;
    }

    @Override
    public List<Referral> listByBiz(Long bizId, String bizType) {
        return lambdaQuery()
                .eq(Referral::getBizId, bizId)
                .eq(Referral::getBizType, bizType)
                .orderByDesc(Referral::getSentTime)
                .list();
    }

    @Override
    public IPage<SentReferralVO> sentPage(Long senderId, int pageNum, int size) {
        LambdaQueryWrapper<Referral> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Referral::getSenderId, senderId)
                .orderByDesc(Referral::getSentTime);
        IPage<Referral> page = page(new Page<>(pageNum, size), wrapper);

        Set<Long> userIds = page.getRecords().stream()
                .flatMap(r -> {
                    java.util.stream.Stream.Builder<Long> b = java.util.stream.Stream.builder();
                    if (r.getSenderId() != null) b.accept(r.getSenderId());
                    if (r.getReceiverOrgId() != null) b.accept(r.getReceiverOrgId());
                    return b.build();
                })
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<SentReferralVO> voList = page.getRecords().stream().map(r -> {
            SentReferralVO vo = new SentReferralVO();
            vo.setId(r.getId());
            vo.setBizType(r.getBizType());
            vo.setPopulationType(r.getPopulationType());
            vo.setModuleType(r.getModuleType());
            vo.setSubjectName(r.getSubjectName());
            vo.setSenderId(r.getSenderId());
            vo.setReceiverOrgId(r.getReceiverOrgId());
            vo.setStatus(r.getStatus());
            vo.setSentTime(r.getSentTime());
            vo.setConfirmedTime(r.getConfirmedTime());
            vo.setRejectedTime(r.getRejectedTime());
            vo.setRejectReason(r.getRejectReason());
            User sender = userMap.get(r.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
                vo.setSenderOrgName(sender.getOrgName());
            }
            User receiver = userMap.get(r.getReceiverOrgId());
            if (receiver != null) {
                vo.setReceiverName(receiver.getRealName() != null ? receiver.getRealName() : receiver.getUsername());
                vo.setReceiverOrgName(receiver.getOrgName());
            }
            return vo;
        }).collect(Collectors.toList());

        IPage<SentReferralVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;
    }
}
