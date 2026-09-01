package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.FirstVisit;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.User;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.model.vo.SentNoticeVO;
import cn.luyou.model.vo.UpdateNoticeContactDTO;
import cn.luyou.model.vo.UpdateNoticeCultureResistanceDTO;
import cn.luyou.model.vo.UpdateNoticeRegistrationNoDTO;
import cn.luyou.model.vo.UserInfoVO;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.SysMessageService;
import cn.luyou.service.UserService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.FirstVisitSputumCultureSupport;
import cn.luyou.utils.NoticePartyFillSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
        implements NoticeService {

    public static final String MSG_TYPE_CULTURE_RESISTANCE_CHANGED = "culture_resistance_changed";

    private final SysMessageService sysMessageService;
    private final DataScopeHelper dataScopeHelper;
    private final PatientService patientService;
    private final LatentInfectionService latentInfectionService;
    private final NoticePartyFillSupport noticePartyFillSupport;
    private final FirstVisitService firstVisitService;
    private final FirstVisitSputumCultureSupport firstVisitSputumCultureSupport;
    private final UserService userService;
    private final DepartmentService departmentService;

    public NoticeServiceImpl(
            SysMessageService sysMessageService,
            DataScopeHelper dataScopeHelper,
            PatientService patientService,
            @Lazy LatentInfectionService latentInfectionService,
            NoticePartyFillSupport noticePartyFillSupport,
            FirstVisitService firstVisitService,
            FirstVisitSputumCultureSupport firstVisitSputumCultureSupport,
            UserService userService,
            DepartmentService departmentService) {
        this.sysMessageService = sysMessageService;
        this.dataScopeHelper = dataScopeHelper;
        this.patientService = patientService;
        this.latentInfectionService = latentInfectionService;
        this.noticePartyFillSupport = noticePartyFillSupport;
        this.firstVisitService = firstVisitService;
        this.firstVisitSputumCultureSupport = firstVisitSputumCultureSupport;
        this.userService = userService;
        this.departmentService = departmentService;
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
        // 仅校验查阅权限：已转出源记录对转出单位不可见；转出待确认仍可查看
        if ("patient".equals(notice.getNoticeType())) {
            dataScopeHelper.assertPatientAccessible(notice.getBizId());
        } else if ("latent".equals(notice.getNoticeType())) {
            dataScopeHelper.assertLatentAccessible(notice.getBizId());
        }
        noticePartyFillSupport.fillPartyNames(List.of(notice));
        return notice;
    }

    @Override
    public List<UserInfoVO> listDistrictLevel3Users(Long noticeId) {
        Notice notice = requirePatientNotice(noticeId);
        Long districtId = resolvePatientDistrictId(notice.getBizId());
        return userService.getLevel3UsersInDistrict(districtId);
    }

    @Override
    @Transactional
    public void updateCultureAndResistance(Long noticeId, UpdateNoticeCultureResistanceDTO dto) {
        if (dto == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少更新内容");
        }
        Notice notice = requirePatientNotice(noticeId);
        String previousSputumCulture = notice.getSputumCulture();
        String previousDrugResistance = notice.getDrugResistance();
        String sputumCulture = normalizeNoticeSputumCulture(dto.getSputumCulture());
        String drugResistance = StrUtil.trimToNull(dto.getDrugResistance());
        String molecularTest = StrUtil.trimToNull(dto.getMolecularTest());
        String pathologyTest = StrUtil.trimToNull(dto.getPathologyTest());
        // 显式 set 写入，避免 updateById 忽略 null / 部分字段未回写导致接收方看到旧值
        var updater = lambdaUpdate()
                .eq(Notice::getId, notice.getId())
                .set(Notice::getSputumCulture, sputumCulture)
                .set(Notice::getDrugResistance, drugResistance)
                .set(Notice::getMolecularTest, molecularTest)
                .set(Notice::getPathologyTest, pathologyTest);
        if (dto.getTreatmentPlan() != null) {
            String plan = StrUtil.trimToNull(dto.getTreatmentPlan());
            updater.set(Notice::getTreatmentPlan, plan)
                    .set(Notice::getCustomPlanDetail, null);
            notice.setTreatmentPlan(plan);
            notice.setCustomPlanDetail(null);
        }
        updater.update();
        notice.setSputumCulture(sputumCulture);
        notice.setDrugResistance(drugResistance);
        notice.setMolecularTest(molecularTest);
        notice.setPathologyTest(pathologyTest);
        syncFirstVisitCultureAndResistance(notice.getBizId(), notice.getSputumCulture(), notice.getDrugResistance());
        sendCultureResistanceChangedMessages(notice, previousSputumCulture, previousDrugResistance, dto.getReceiverUserIds());
    }

    @Override
    @Transactional
    public void updateRegistrationNo(Long noticeId, UpdateNoticeRegistrationNoDTO dto) {
        if (dto == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少更新内容");
        }
        assertLevel3Or4ForRegistrationNo();
        Notice notice = requireLatentNotice(noticeId);
        String registrationNo = StrUtil.trimToNull(dto.getRegistrationNo());
        // 使用 set 显式写入，避免 updateById 忽略 null 导致无法清空登记号
        lambdaUpdate()
                .eq(Notice::getId, notice.getId())
                .set(Notice::getRegistrationNo, registrationNo)
                .update();
        notice.setRegistrationNo(registrationNo);
        syncLatentRegistrationNo(notice);
    }

    @Override
    @Transactional
    public void updateContactInfo(Long noticeId, UpdateNoticeContactDTO dto) {
        if (dto == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少更新内容");
        }
        Notice notice = requireNoticeForContact(noticeId);
        String phone = StrUtil.trimToNull(dto.getPhone());
        if (phone != null && !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }
        String currentAddress = StrUtil.trimToNull(dto.getCurrentAddress());
        String householdAddress = StrUtil.trimToNull(dto.getHouseholdAddress());
        lambdaUpdate()
                .eq(Notice::getId, notice.getId())
                .set(Notice::getPhone, phone)
                .set(Notice::getCurrentAddress, currentAddress)
                .set(Notice::getHouseholdAddress, householdAddress)
                .update();
        if ("patient".equals(notice.getNoticeType())) {
            patientService.syncContactFromNotice(notice.getBizId(), phone, currentAddress, householdAddress);
        } else if ("latent".equals(notice.getNoticeType())) {
            latentInfectionService.syncContactFromNotice(notice.getBizId(), phone, currentAddress, householdAddress);
        }
    }

    /** 登记号完善：仅三级(role=4)、四级(role=5)；超管放行 */
    private void assertLevel3Or4ForRegistrationNo() {
        Integer role = BaseContext.getCurrentRole();
        if (role == null || (role != 1 && role != 4 && role != 5)) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "仅三级、四级用户可修改登记号");
        }
    }

    private Notice requirePatientNotice(Long noticeId) {
        Notice notice = getById(noticeId);
        if (notice == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单不存在");
        }
        if (!"patient".equals(notice.getNoticeType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅患者通知单可修改痰培养、耐药情况和治疗方案");
        }
        assertBizAccessible(notice.getBizId(), notice.getNoticeType());
        return notice;
    }

    private Notice requireLatentNotice(Long noticeId) {
        Notice notice = getById(noticeId);
        if (notice == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单不存在");
        }
        if (!"latent".equals(notice.getNoticeType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅潜伏感染者通知单可修改登记号");
        }
        assertBizAccessible(notice.getBizId(), notice.getNoticeType());
        return notice;
    }

    private Notice requireNoticeForContact(Long noticeId) {
        Notice notice = getById(noticeId);
        if (notice == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单不存在");
        }
        if (!"patient".equals(notice.getNoticeType()) && !"latent".equals(notice.getNoticeType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅患者或潜伏感染者通知单可修改联系方式及地址");
        }
        assertBizAccessible(notice.getBizId(), notice.getNoticeType());
        return notice;
    }

    private Long resolvePatientDistrictId(Long patientId) {
        Patient patient = patientService.getById(patientId);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        Long districtId = departmentService.resolveDistrictId(patient.getDepartmentId());
        if (districtId == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无法确定患者所属区县，请检查患者部门归属");
        }
        return districtId;
    }

    private void syncFirstVisitCultureAndResistance(Long patientId, String sputumCulture, String drugResistance) {
        if (patientId == null) {
            return;
        }
        FirstVisit existing = firstVisitService.lambdaQuery()
                .eq(FirstVisit::getPatientId, patientId)
                .one();
        if (existing == null) {
            return;
        }
        FirstVisit before = new FirstVisit();
        before.setSputumCulture(existing.getSputumCulture());
        before.setDrugResistance(existing.getDrugResistance());
        before.setSputumCultureSupplementStatus(existing.getSputumCultureSupplementStatus());
        before.setStatus(existing.getStatus());
        before.setPatientId(existing.getPatientId());
        before.setFilledBy(existing.getFilledBy());
        existing.setSputumCulture(sputumCulture);
        existing.setDrugResistance(drugResistance);
        firstVisitSputumCultureSupport.prepareSupplementStatus(existing, before);
        firstVisitService.lambdaUpdate()
                .eq(FirstVisit::getId, existing.getId())
                .set(FirstVisit::getSputumCulture, existing.getSputumCulture())
                .set(FirstVisit::getDrugResistance, existing.getDrugResistance())
                .set(FirstVisit::getSputumCultureSupplementStatus, existing.getSputumCultureSupplementStatus())
                .update();
        firstVisitSputumCultureSupport.syncMessages(existing, before);
    }

    private void sendCultureResistanceChangedMessages(
            Notice notice,
            String previousSputumCulture,
            String previousDrugResistance,
            List<Long> receiverUserIds) {
        Long districtId = resolvePatientDistrictId(notice.getBizId());
        Set<Long> allowedIds = userService.getLevel3UsersInDistrict(districtId).stream()
                .map(UserInfoVO::getId)
                .collect(Collectors.toCollection(HashSet::new));
        List<Long> validIds = new ArrayList<>();
        boolean hadLevel3Selection = receiverUserIds != null && !receiverUserIds.isEmpty();
        if (hadLevel3Selection) {
            for (Long id : receiverUserIds) {
                if (id != null && allowedIds.contains(id) && !validIds.contains(id)) {
                    validIds.add(id);
                }
            }
            if (validIds.isEmpty()) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择本区县对应的三级用户");
            }
        }
        // 同步通知原接收方（五级），保证其打开通知单即为最新内容
        Long originalReceiverId = notice.getReceiverOrgId();
        if (originalReceiverId != null && !validIds.contains(originalReceiverId)) {
            validIds.add(originalReceiverId);
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            validIds.remove(currentId);
        }
        if (validIds.isEmpty()) {
            return;
        }
        String operatorName = resolveCurrentUserDisplayName();
        String patientName = StrUtil.blankToDefault(notice.getPatientName(), "患者");
        String title = resolveCultureResistanceChangedTitle(
                previousSputumCulture, notice.getSputumCulture(),
                previousDrugResistance, notice.getDrugResistance());
        String content = operatorName + "对" + patientName + "患者" + title;
        for (Long receiverId : validIds) {
            sysMessageService.sendMessage(receiverId, title, content, MSG_TYPE_CULTURE_RESISTANCE_CHANGED, notice.getId());
        }
    }

    /**
     * 按实际变更生成消息标题：痰培养优先；改为阳性时明确提醒。
     * 「未知」与「无结果」视为同一选项，避免仅重命名时误报修改培养。
     */
    private String resolveCultureResistanceChangedTitle(
            String previousSputumCulture,
            String sputumCulture,
            String previousDrugResistance,
            String drugResistance) {
        boolean cultureChanged = !StrUtil.equals(
                cultureCompareKey(previousSputumCulture),
                cultureCompareKey(sputumCulture));
        boolean resistanceChanged = !StrUtil.equals(
                StrUtil.trimToEmpty(previousDrugResistance),
                StrUtil.trimToEmpty(drugResistance));
        if (cultureChanged) {
            String cultureTitle = "阳性".equals(StrUtil.trim(sputumCulture))
                    ? "修改痰培养结果为阳性"
                    : "修改培养";
            if (resistanceChanged) {
                return cultureTitle + "，并" + resolveResistanceChangedTitle(drugResistance);
            }
            return cultureTitle;
        }
        if (resistanceChanged) {
            return resolveResistanceChangedTitle(drugResistance);
        }
        return "通知单信息变更";
    }

    private String resolveResistanceChangedTitle(String drugResistance) {
        String trimmed = StrUtil.trim(drugResistance);
        return StrUtil.isNotBlank(trimmed) ? "修改为" + trimmed : "耐药情况变更";
    }

    /** 通知单痰培养：未知 → 无结果 */
    private String normalizeNoticeSputumCulture(String value) {
        String v = StrUtil.trimToNull(value);
        if ("未知".equals(v)) {
            return "无结果";
        }
        return v;
    }

    private String cultureCompareKey(String value) {
        String v = StrUtil.trimToEmpty(value);
        if ("未知".equals(v)) {
            return "无结果";
        }
        return v;
    }

    private String resolveCurrentUserDisplayName() {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            return "系统用户";
        }
        User user = userService.getById(currentId);
        if (user == null) {
            return "系统用户";
        }
        return StrUtil.blankToDefault(user.getRealName(), user.getUsername());
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
