package cn.luyou.service.impl;

import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.ReferralMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.Referral;
import cn.luyou.model.User;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.model.vo.ReferralDetailVO;
import cn.luyou.model.vo.SentReferralVO;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DataScopeHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReferralServiceImpl extends ServiceImpl<ReferralMapper, Referral>
        implements ReferralService {

    private final SysMessageService sysMessageService;
    private final UserMapper userMapper;
    private final PatientService patientService;
    private final LatentInfectionService latentInfectionService;
    private final DataScopeHelper dataScopeHelper;

    public ReferralServiceImpl(
            SysMessageService sysMessageService,
            UserMapper userMapper,
            PatientService patientService,
            @Lazy LatentInfectionService latentInfectionService,
            DataScopeHelper dataScopeHelper) {
        this.sysMessageService = sysMessageService;
        this.userMapper = userMapper;
        this.patientService = patientService;
        this.latentInfectionService = latentInfectionService;
        this.dataScopeHelper = dataScopeHelper;
    }

    private static final Map<String, String> MODULE_LABEL = Map.of(
            "screening", "筛查管理",
            "suspected", "疑似结核管理",
            "latent", "潜伏感染者管理",
            "patient", "患者管理"
    );

    private static final Map<String, String> POPULATION_LABEL = Map.of(
            "school", "学校人群",
            "close", "密接人群",
            "closeContact", "密接",
            "keyPopulation", "重点人群",
            "epidemic", "大疫情",
            "specialDisease", "专病网"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Referral referral) {
        assertNoPendingReferral(referral.getBizId(), referral.getBizType());
        markTransferPendingIfNeeded(referral);
        referral.setSenderId(BaseContext.getCurrentId());
        referral.setStatus(1);
        referral.setSentTime(LocalDateTime.now());
        save(referral);

        // 向接收方推送消息通知
        if (referral.getReceiverOrgId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "待确认转出";
            String reasonPart = referral.getReferralReason() != null && !referral.getReferralReason().isBlank()
                    ? "，转出原因：" + referral.getReferralReason() : "";
            String content = String.format("【%s - %s】%s，发送方已发起转出推送%s，请在消息管理中确认接收。",
                    popLabel, moduleLabel, referral.getSubjectName(), reasonPart);
            sysMessageService.sendMessage(referral.getReceiverOrgId(), title, content,
                    "referral_receive", referral.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, LocalDate actualReferralDate) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "转出记录不存在");
        }
        if (actualReferralDate == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择转诊时间");
        }
        if (referral.getStatus() == 2) {
            repairTransferIfNeeded(referral);
            syncReceiverTransferMessage(referral, true);
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已确认接收，请勿重复操作");
        }
        if (referral.getStatus() == 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已拒绝，不可再确认");
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (!BaseContext.isSuperAdmin()
                && referral.getReceiverOrgId() != null && currentUserId != null
                && !referral.getReceiverOrgId().equals(currentUserId)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅接收方可确认");
        }

        Long targetBizId = syncBizOnConfirm(referral);
        referral.setTargetBizId(targetBizId);
        referral.setStatus(2);
        referral.setConfirmedTime(LocalDateTime.now());
        referral.setActualReferralDate(actualReferralDate);
        updateById(referral);
        // 再次兜底：确保源记录已标记「已转出」并退出转出单位在管/随访列表
        ensureSourceMarkedTransferred(referral);

        syncReceiverTransferMessage(referral, true);

        // 回执给发送方
        if (referral.getSenderId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "转出已接收";
            String content = String.format("【%s - %s】%s，接收方已确认接收转出信息。",
                    popLabel, moduleLabel, referral.getSubjectName());
            sysMessageService.sendMessage(referral.getSenderId(), title, content,
                    "referral_confirmed", referral.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String rejectReason) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "转出记录不存在");
        }
        if (referral.getStatus() == 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已确认接收，无法拒绝");
        }
        if (referral.getStatus() == 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已拒绝，无需重复操作");
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (referral.getReceiverOrgId() != null && currentUserId != null
                && !referral.getReceiverOrgId().equals(currentUserId)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅接收方可拒绝");
        }
        referral.setStatus(3);
        referral.setRejectedTime(LocalDateTime.now());
        referral.setRejectReason(rejectReason);
        updateById(referral);
        restoreBizIfTransferRejected(referral);
        syncReceiverTransferMessage(referral, false);

        // 通知发送方被拒绝
        if (referral.getSenderId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "转出已被拒绝";
            String reason = rejectReason != null && !rejectReason.isBlank() ? "，原因：" + rejectReason : "";
            String content = String.format("【%s - %s】%s，接收方已拒绝转出%s，您可重新发起。",
                    popLabel, moduleLabel, referral.getSubjectName(), reason);
            sysMessageService.sendMessage(referral.getSenderId(), title, content,
                    "referral_rejected", referral.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resend(Long id) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "转出记录不存在");
        }
        if (referral.getStatus() != 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅拒绝状态的转出记录可重新发起");
        }
        markTransferPendingIfNeeded(referral);
        referral.setStatus(1);
        referral.setSentTime(LocalDateTime.now());
        referral.setRejectedTime(null);
        referral.setRejectReason(null);
        referral.setTargetBizId(null);
        updateById(referral);

        if (referral.getReceiverOrgId() != null) {
            String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
            String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
            String title = "待确认转出（重新发起）";
            String reasonPart = referral.getReferralReason() != null && !referral.getReferralReason().isBlank()
                    ? "，转出原因：" + referral.getReferralReason() : "";
            String content = String.format("【%s - %s】%s，发送方重新发起了转出推送%s，请在消息管理中确认接收。",
                    popLabel, moduleLabel, referral.getSubjectName(), reasonPart);
            sysMessageService.sendMessage(referral.getReceiverOrgId(), title, content,
                    "referral_receive", referral.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReferralDetailVO detail(Long id) {
        Referral referral = getById(id);
        if (referral == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "转出记录不存在");
        }
        if (Integer.valueOf(2).equals(referral.getStatus())) {
            repairTransferIfNeeded(referral);
            syncReceiverTransferMessage(referral, true);
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
        vo.setActualReferralDate(referral.getActualReferralDate());
        vo.setRejectedTime(referral.getRejectedTime());
        vo.setRejectReason(referral.getRejectReason());
        vo.setReferralReason(referral.getReferralReason());

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
        applySentReferralScope(wrapper, senderId);
        wrapper.orderByDesc(Referral::getSentTime);
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
            vo.setActualReferralDate(r.getActualReferralDate());
            vo.setRejectedTime(r.getRejectedTime());
            vo.setRejectReason(r.getRejectReason());
            vo.setReferralReason(r.getReferralReason());
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

    /**
     * 已发送转出列表：五级仅看自己发送的；市/县/社区等上级按辖区范围（与统计看板一致）。
     */
    private void applySentReferralScope(LambdaQueryWrapper<Referral> wrapper, Long currentUserId) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 6) {
            wrapper.eq(Referral::getSenderId, currentUserId);
            return;
        }
        dataScopeHelper.applyReferralScope(wrapper);
    }

    /** 发起/重新发起转出时标记转出待确认 */
    private void markTransferPendingIfNeeded(Referral referral) {
        if (referral.getBizId() == null) {
            return;
        }
        if ("patient".equals(referral.getModuleType())) {
            patientService.markTransferPending(referral.getBizId());
        } else if ("latent".equals(referral.getModuleType())) {
            latentInfectionService.markTransferPending(referral.getBizId());
        }
    }

    /** 接收确认后复制业务记录至接收方，并标记原记录已转出 */
    private Long syncBizOnConfirm(Referral referral) {
        if (referral.getBizId() == null || referral.getReceiverOrgId() == null) {
            if ("patient".equals(referral.getModuleType()) || "latent".equals(referral.getModuleType())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "未指定接收人");
            }
            return null;
        }
        if ("patient".equals(referral.getModuleType())) {
            Long newPatientId = patientService.copyPatientForTransferOut(
                    referral.getBizId(), referral.getReceiverOrgId());
            patientService.markTransferredOut(referral.getBizId());
            return newPatientId;
        }
        if ("latent".equals(referral.getModuleType())) {
            Long newLatentId = latentInfectionService.copyLatentForTransferOut(
                    referral.getBizId(), referral.getReceiverOrgId());
            latentInfectionService.markTransferredOut(referral.getBizId());
            return newLatentId;
        }
        return null;
    }

    private void assertNoPendingReferral(Long bizId, String bizType) {
        if (bizId == null || bizType == null) {
            return;
        }
        long pending = lambdaQuery()
                .eq(Referral::getBizId, bizId)
                .eq(Referral::getBizType, bizType)
                .eq(Referral::getStatus, 1)
                .count();
        if (pending > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已有待确认的转出申请，请勿重复发起");
        }
    }

    /** 转出被拒绝时，恢复为在管记录 */
    private void restoreBizIfTransferRejected(Referral referral) {
        if (referral.getBizId() == null) {
            return;
        }
        if ("patient".equals(referral.getModuleType())) {
            patientService.restoreTransferredPatient(referral.getBizId());
        } else if ("latent".equals(referral.getModuleType())) {
            latentInfectionService.restoreTransferredLatent(referral.getBizId());
        }
    }

    /** 已确认但尚未同步数据的转出记录补同步（兼容历史数据） */
    private void repairTransferIfNeeded(Referral referral) {
        if (referral.getBizId() == null) {
            return;
        }
        if (referral.getTargetBizId() == null && referral.getReceiverOrgId() != null) {
            if ("patient".equals(referral.getModuleType())) {
                repairPatientTransferIfNeeded(referral);
            } else if ("latent".equals(referral.getModuleType())) {
                repairLatentTransferIfNeeded(referral);
            }
        }
        ensureSourceMarkedTransferred(referral);
    }

    private void repairPatientTransferIfNeeded(Referral referral) {
        if (!"patient".equals(referral.getModuleType()) || referral.getBizId() == null) {
            return;
        }
        if (referral.getTargetBizId() != null || referral.getReceiverOrgId() == null) {
            return;
        }
        Long newPatientId = patientService.copyPatientForTransferOut(
                referral.getBizId(), referral.getReceiverOrgId());
        referral.setTargetBizId(newPatientId);
        updateById(referral);
    }

    private void repairLatentTransferIfNeeded(Referral referral) {
        if (!"latent".equals(referral.getModuleType()) || referral.getBizId() == null) {
            return;
        }
        if (referral.getTargetBizId() != null || referral.getReceiverOrgId() == null) {
            return;
        }
        Long newLatentId = latentInfectionService.copyLatentForTransferOut(
                referral.getBizId(), referral.getReceiverOrgId());
        referral.setTargetBizId(newLatentId);
        updateById(referral);
    }

    /** 确认接收后源记录必须标记已转出，转出单位在管总览/随访不再可见 */
    private void ensureSourceMarkedTransferred(Referral referral) {
        if (referral == null || referral.getBizId() == null) {
            return;
        }
        if ("patient".equals(referral.getModuleType())) {
            Patient source = patientService.getById(referral.getBizId());
            if (source != null && needsTransferArchive(source.getArchiveRemark(), source.getArchived())) {
                patientService.markTransferredOut(referral.getBizId());
            }
        } else if ("latent".equals(referral.getModuleType())) {
            LatentInfection source = latentInfectionService.getById(referral.getBizId());
            if (source != null && needsTransferArchive(source.getArchiveRemark(), source.getArchived())) {
                latentInfectionService.markTransferredOut(referral.getBizId());
            }
        }
    }

    private static boolean needsTransferArchive(String archiveRemark, Integer archived) {
        return !PatientService.ARCHIVE_REMARK_TRANSFERRED_OUT.equals(archiveRemark)
                || !Integer.valueOf(1).equals(archived);
    }

    private void syncReceiverTransferMessage(Referral referral, boolean confirmed) {
        String moduleLabel = MODULE_LABEL.getOrDefault(referral.getModuleType(), referral.getModuleType());
        String popLabel = POPULATION_LABEL.getOrDefault(referral.getPopulationType(), referral.getPopulationType());
        if (confirmed) {
            sysMessageService.updatePendingMessageByBizId(
                    referral.getId(),
                    "referral_receive",
                    "referral_confirmed",
                    "转出已接收",
                    String.format("【%s - %s】%s，您已确认接收转出信息。",
                            popLabel, moduleLabel, referral.getSubjectName()));
        } else {
            sysMessageService.updatePendingMessageByBizId(
                    referral.getId(),
                    "referral_receive",
                    "referral_rejected",
                    "转出已被拒绝",
                    String.format("【%s - %s】%s，您已拒绝转出。",
                            popLabel, moduleLabel, referral.getSubjectName()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReferralsAndMessagesByBizId(Long bizId) {
        if (bizId == null) {
            return;
        }
        List<Long> referralIds = lambdaQuery()
                .eq(Referral::getBizId, bizId)
                .list()
                .stream()
                .map(Referral::getId)
                .toList();
        if (referralIds.isEmpty()) {
            return;
        }
        sysMessageService.lambdaUpdate().in(cn.luyou.model.SysMessage::getBizId, referralIds).remove();
        lambdaUpdate().eq(Referral::getBizId, bizId).remove();
    }
}
