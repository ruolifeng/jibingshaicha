package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.FirstVisit;
import cn.luyou.model.FollowUpVisit;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.SupervisionForm;
import cn.luyou.model.User;
import cn.luyou.model.VisitSupervisionReminderLog;
import cn.luyou.model.vo.UpcomingVisitSupervisionVO;
import cn.luyou.model.vo.VisitSupervisionDispatchResultVO;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.SmsService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import cn.luyou.service.UserService;
import cn.luyou.service.VisitSupervisionReminderLogService;
import cn.luyou.service.VisitSupervisionReminderService;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.DepartmentFilterSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitSupervisionReminderServiceImpl implements VisitSupervisionReminderService {

    public static final String TYPE_FOLLOW_UP = "follow_up";
    public static final String TYPE_SUPERVISION = "supervision";
    public static final String MSG_FOLLOW_UP_DUE = "follow_up_due";
    public static final String MSG_SUPERVISION_DUE = "supervision_due";

    private static final int CHUNK = 400;
    private static final Set<Integer> LEAD_DAYS = Set.of(7, 3, 1);

    private final PatientService patientService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final LatentInfectionService latentInfectionService;
    private final SupervisionFormService supervisionFormService;
    private final NoticeService noticeService;
    private final SysMessageService sysMessageService;
    private final SmsService smsService;
    private final VisitSupervisionReminderLogService reminderLogService;
    private final UserService userService;
    private final DataScopeHelper dataScopeHelper;
    private final DepartmentFilterSupport departmentFilterSupport;

    private record Candidate(
            String type,
            Long bizId,
            String name,
            LocalDate dueDate,
            int leadDays,
            Long sourceId,
            Long filledBy,
            /** 督导表管理单位 / 备用展示 */
            String unitHint
    ) {}

    private record LatestVisit(LocalDate visitDate, LocalDate nextDate, Long sourceId, Long filledBy) {}

    @Override
    public List<UpcomingVisitSupervisionVO> listUpcoming(List<Long> filterDeptIds) {
        List<Candidate> candidates = collectDue(LocalDate.now(), filterDeptIds, true, false).stream()
                .sorted(Comparator.comparingInt(Candidate::leadDays)
                        .thenComparing(Candidate::dueDate)
                        .thenComparing(Candidate::name, Comparator.nullsLast(String::compareTo)))
                .toList();
        Map<String, String> orgByCandidateKey = resolveManagerOrgNames(candidates);
        return candidates.stream()
                .map(c -> UpcomingVisitSupervisionVO.builder()
                        .type(c.type())
                        .bizId(c.bizId())
                        .name(c.name())
                        .dueDate(c.dueDate())
                        .leadDays(c.leadDays())
                        .managerOrgName(orgByCandidateKey.get(candidateKey(c)))
                        .build())
                .toList();
    }

    private static String candidateKey(Candidate c) {
        return c.type() + ":" + c.bizId();
    }

    /**
     * 管理人对应机构：优先通知单接收人所属机构，其次填写人所属机构，再回退管理单位/服药管理单位。
     */
    private Map<String, String> resolveManagerOrgNames(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return Map.of();
        }
        Map<String, Notice> noticeByKey = loadLatestNotices(candidates);
        Set<Long> userIds = new HashSet<>();
        for (Candidate c : candidates) {
            Notice notice = noticeByKey.get(candidateKey(c));
            if (notice != null && notice.getReceiverOrgId() != null) {
                userIds.add(notice.getReceiverOrgId());
            }
            if (c.filledBy() != null) {
                userIds.add(c.filledBy());
            }
        }
        Map<Long, String> orgByUserId = loadUserOrgNames(userIds);
        Map<String, String> result = new HashMap<>();
        for (Candidate c : candidates) {
            Notice notice = noticeByKey.get(candidateKey(c));
            String org = null;
            if (notice != null && notice.getReceiverOrgId() != null) {
                org = orgByUserId.get(notice.getReceiverOrgId());
            }
            if (StrUtil.isBlank(org) && c.filledBy() != null) {
                org = orgByUserId.get(c.filledBy());
            }
            if (StrUtil.isBlank(org) && notice != null) {
                org = StrUtil.trim(notice.getMedicationManagementUnit());
            }
            if (StrUtil.isBlank(org)) {
                org = StrUtil.trim(c.unitHint());
            }
            if (StrUtil.isNotBlank(org)) {
                result.put(candidateKey(c), org);
            }
        }
        return result;
    }

    private Map<String, Notice> loadLatestNotices(List<Candidate> candidates) {
        Map<String, Notice> map = new HashMap<>();
        Map<String, List<Long>> bizIdsByType = candidates.stream()
                .collect(Collectors.groupingBy(Candidate::type,
                        Collectors.mapping(Candidate::bizId, Collectors.toList())));
        for (Map.Entry<String, List<Long>> entry : bizIdsByType.entrySet()) {
            String noticeType = TYPE_FOLLOW_UP.equals(entry.getKey()) ? "patient" : "latent";
            List<Long> ids = entry.getValue().stream().distinct().toList();
            for (Notice notice : listInChunks(ids, chunk -> noticeService.lambdaQuery()
                    .eq(Notice::getNoticeType, noticeType)
                    .in(Notice::getBizId, chunk)
                    .ge(Notice::getStatus, 1)
                    .orderByDesc(Notice::getId)
                    .list())) {
                String key = entry.getKey() + ":" + notice.getBizId();
                map.putIfAbsent(key, notice);
            }
        }
        return map;
    }

    private Map<Long, String> loadUserOrgNames(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>();
        for (User user : userService.listByIds(userIds)) {
            if (user != null && user.getId() != null && StrUtil.isNotBlank(user.getOrgName())) {
                map.put(user.getId(), user.getOrgName().trim());
            }
        }
        return map;
    }

    @Override
    public VisitSupervisionDispatchResultVO dispatchMessages() {
        return dispatchInternal(true, false, true);
    }

    @Override
    public VisitSupervisionDispatchResultVO dispatchSms() {
        return dispatchInternal(false, true, false);
    }

    private VisitSupervisionDispatchResultVO dispatchInternal(boolean sendMessage, boolean sendSms, boolean useLog) {
        List<Candidate> due = collectDue(LocalDate.now(), null, false, true);
        int followUp = 0;
        int supervision = 0;
        int messages = 0;
        int sms = 0;
        for (Candidate item : due) {
            List<Long> receivers = resolveReceivers(item);
            if (receivers.isEmpty()) {
                log.warn("随访/督导到期无接收人 type={} bizId={} name={}", item.type(), item.bizId(), item.name());
                continue;
            }
            if (useLog && !markSent(item)) {
                continue;
            }
            if (TYPE_FOLLOW_UP.equals(item.type())) {
                followUp++;
            } else {
                supervision++;
            }
            String title = TYPE_FOLLOW_UP.equals(item.type())
                    ? "后续随访提醒（提前" + item.leadDays() + "天）"
                    : "督导表提醒（提前" + item.leadDays() + "天）";
            String content = buildContent(item);
            for (Long receiverId : receivers) {
                if (sendMessage) {
                    sysMessageService.sendMessage(receiverId, title, content,
                            TYPE_FOLLOW_UP.equals(item.type()) ? MSG_FOLLOW_UP_DUE : MSG_SUPERVISION_DUE,
                            item.bizId());
                    messages++;
                }
                if (sendSms) {
                    smsService.sendReminderSmsAsync(receiverId, content);
                    sms++;
                }
            }
        }
        log.info("随访/督导到期提醒：随访 {} 条，督导 {} 条，消息 {}，短信 {}", followUp, supervision, messages, sms);
        return VisitSupervisionDispatchResultVO.builder()
                .followUpCount(followUp)
                .supervisionCount(supervision)
                .messageCount(messages)
                .smsCount(sms)
                .build();
    }

    private String buildContent(Candidate item) {
        if (TYPE_FOLLOW_UP.equals(item.type())) {
            return String.format("患者【%s】下次随访时间为 %s，距今 %d 天，请按时完成后续随访。",
                    StrUtil.blankToDefault(item.name(), "患者"), item.dueDate(), item.leadDays());
        }
        return String.format("潜伏感染者【%s】下次督导时间为 %s，距今 %d 天，请按时完成后续督导表。",
                StrUtil.blankToDefault(item.name(), "潜伏感染者"), item.dueDate(), item.leadDays());
    }

    private boolean markSent(Candidate item) {
        boolean exists = reminderLogService.lambdaQuery()
                .eq(VisitSupervisionReminderLog::getBizType, item.type())
                .eq(VisitSupervisionReminderLog::getBizId, item.bizId())
                .eq(VisitSupervisionReminderLog::getDueDate, item.dueDate())
                .eq(VisitSupervisionReminderLog::getLeadDays, item.leadDays())
                .exists();
        if (exists) {
            return false;
        }
        try {
            reminderLogService.save(VisitSupervisionReminderLog.builder()
                    .bizType(item.type())
                    .bizId(item.bizId())
                    .sourceId(item.sourceId())
                    .dueDate(item.dueDate())
                    .leadDays(item.leadDays())
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("随访/督导提醒记录已存在，跳过重复发送 type={} bizId={} leadDays={}",
                    item.type(), item.bizId(), item.leadDays());
            return false;
        }
    }

    private List<Long> resolveReceivers(Candidate item) {
        Set<Long> ids = new HashSet<>();
        if (item.filledBy() != null) {
            ids.add(item.filledBy());
        }
        String noticeType = TYPE_FOLLOW_UP.equals(item.type()) ? "patient" : "latent";
        Notice notice = noticeService.lambdaQuery()
                .eq(Notice::getNoticeType, noticeType)
                .eq(Notice::getBizId, item.bizId())
                .ge(Notice::getStatus, 1)
                .orderByDesc(Notice::getId)
                .last("LIMIT 1")
                .one();
        if (notice != null && notice.getReceiverOrgId() != null) {
            ids.add(notice.getReceiverOrgId());
        }
        return new ArrayList<>(ids);
    }

    private List<Candidate> collectDue(LocalDate today, List<Long> filterDeptIds, boolean applyDataScope, boolean exactLeadDays) {
        List<Candidate> list = new ArrayList<>();
        list.addAll(collectFollowUpDue(today, filterDeptIds, applyDataScope, exactLeadDays));
        list.addAll(collectSupervisionDue(today, filterDeptIds, applyDataScope, exactLeadDays));
        return list;
    }

    /** 首页展示未来 7 天内；站内/短信仅在正好提前 7/3/1 天发送 */
    private Integer leadDaysOf(LocalDate today, LocalDate due, boolean exactLeadDays) {
        if (due == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(today, due);
        if (days < 0 || days > 7) {
            return null;
        }
        if (exactLeadDays && !LEAD_DAYS.contains((int) days)) {
            return null;
        }
        return (int) days;
    }

    private List<Candidate> collectFollowUpDue(LocalDate today, List<Long> filterDeptIds, boolean applyDataScope, boolean exactLeadDays) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<Patient>()
                .eq(Patient::getArchived, 0)
                .isNull(Patient::getSourcePatientId)
                .select(Patient::getId, Patient::getName, Patient::getDepartmentId);
        if (applyDataScope) {
            dataScopeHelper.applyPatientScope(wrapper);
        }
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, Patient::getDepartmentId, filterDeptIds);
        List<Patient> patients = patientService.list(wrapper);
        if (patients.isEmpty()) {
            return List.of();
        }
        Map<Long, Patient> patientMap = patients.stream().collect(Collectors.toMap(Patient::getId, p -> p, (a, b) -> a));
        List<Long> ids = new ArrayList<>(patientMap.keySet());

        Map<Long, LatestVisit> latest = new HashMap<>();
        for (FirstVisit fv : listInChunks(ids, chunk -> firstVisitService.lambdaQuery()
                .in(FirstVisit::getPatientId, chunk)
                .eq(FirstVisit::getStatus, 1)
                .orderByAsc(FirstVisit::getVisitDate)
                .orderByAsc(FirstVisit::getId)
                .list())) {
            latest.put(fv.getPatientId(), new LatestVisit(
                    fv.getVisitDate() != null ? fv.getVisitDate() : LocalDate.MIN,
                    fv.getNextVisitDate(),
                    fv.getId(),
                    fv.getFilledBy()));
        }
        for (FollowUpVisit fu : listInChunks(ids, chunk -> followUpVisitService.lambdaQuery()
                .in(FollowUpVisit::getPatientId, chunk)
                .eq(FollowUpVisit::getStatus, 1)
                .orderByAsc(FollowUpVisit::getVisitDate)
                .orderByAsc(FollowUpVisit::getId)
                .list())) {
            LocalDate visitDate = fu.getVisitDate() != null ? fu.getVisitDate() : LocalDate.MIN;
            LatestVisit cur = latest.get(fu.getPatientId());
            if (cur == null || !visitDate.isBefore(cur.visitDate())) {
                latest.put(fu.getPatientId(), new LatestVisit(visitDate, fu.getNextVisitDate(), fu.getId(), fu.getFilledBy()));
            }
        }

        List<Candidate> result = new ArrayList<>();
        for (Map.Entry<Long, LatestVisit> entry : latest.entrySet()) {
            Integer lead = leadDaysOf(today, entry.getValue().nextDate(), exactLeadDays);
            if (lead == null) {
                continue;
            }
            Patient patient = patientMap.get(entry.getKey());
            if (patient == null) {
                continue;
            }
            result.add(new Candidate(TYPE_FOLLOW_UP, patient.getId(), patient.getName(),
                    entry.getValue().nextDate(), lead, entry.getValue().sourceId(), entry.getValue().filledBy(), null));
        }
        return result;
    }

    private List<Candidate> collectSupervisionDue(LocalDate today, List<Long> filterDeptIds, boolean applyDataScope, boolean exactLeadDays) {
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .eq(LatentInfection::getArchived, 0)
                .isNull(LatentInfection::getSourceLatentId)
                .select(LatentInfection::getId, LatentInfection::getName, LatentInfection::getDepartmentId);
        if (applyDataScope) {
            dataScopeHelper.applyLatentScope(wrapper);
        }
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, LatentInfection::getDepartmentId, filterDeptIds);
        List<LatentInfection> latents = latentInfectionService.list(wrapper);
        if (latents.isEmpty()) {
            return List.of();
        }
        Map<Long, LatentInfection> latentMap = latents.stream()
                .collect(Collectors.toMap(LatentInfection::getId, l -> l, (a, b) -> a));
        List<Long> ids = new ArrayList<>(latentMap.keySet());

        Map<Long, SupervisionForm> latest = new HashMap<>();
        for (SupervisionForm form : listInChunks(ids, chunk -> supervisionFormService.lambdaQuery()
                .in(SupervisionForm::getLatentInfectionId, chunk)
                .ge(SupervisionForm::getStatus, 1)
                .orderByAsc(SupervisionForm::getFormSeq)
                .orderByAsc(SupervisionForm::getId)
                .list())) {
            SupervisionForm cur = latest.get(form.getLatentInfectionId());
            if (cur == null || compareSupervision(form, cur) > 0) {
                latest.put(form.getLatentInfectionId(), form);
            }
        }

        List<Candidate> result = new ArrayList<>();
        for (Map.Entry<Long, SupervisionForm> entry : latest.entrySet()) {
            SupervisionForm form = entry.getValue();
            Integer lead = leadDaysOf(today, form.getNextSupervisionDate(), exactLeadDays);
            if (lead == null) {
                continue;
            }
            LatentInfection latent = latentMap.get(entry.getKey());
            if (latent == null) {
                continue;
            }
            String unitHint = StrUtil.blankToDefault(form.getManagingUnit(), form.getPreventiveManager());
            result.add(new Candidate(TYPE_SUPERVISION, latent.getId(), latent.getName(),
                    form.getNextSupervisionDate(), lead, form.getId(), form.getFilledBy(),
                    StrUtil.trim(unitHint)));
        }
        return result;
    }

    private <T> List<T> listInChunks(List<Long> ids, java.util.function.Function<List<Long>, List<T>> loader) {
        List<T> all = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += CHUNK) {
            all.addAll(loader.apply(ids.subList(i, Math.min(i + CHUNK, ids.size()))));
        }
        return all;
    }

    private static int compareSupervision(SupervisionForm a, SupervisionForm b) {
        int seqA = a.getFormSeq() != null ? a.getFormSeq() : -1;
        int seqB = b.getFormSeq() != null ? b.getFormSeq() : -1;
        if (seqA != seqB) {
            return Integer.compare(seqA, seqB);
        }
        long idA = a.getId() != null ? a.getId() : 0L;
        long idB = b.getId() != null ? b.getId() : 0L;
        return Long.compare(idA, idB);
    }
}
