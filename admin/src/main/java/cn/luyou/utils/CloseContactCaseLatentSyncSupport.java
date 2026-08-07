package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.CloseContactCaseMapper;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.User;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.utils.ImportIdentitySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 密接个案表 → 潜伏感染在管：终筛「潜伏感染者」时创建或补充手工在管记录。
 * <p>
 * 不设置 screeningId，保证进入聚合「潜伏感染者管理/在管」列表；
 * 已有手工录入记录按证件号匹配，仅补空白字段，不覆盖已填内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloseContactCaseLatentSyncSupport {

    private final LatentInfectionMapper latentInfectionMapper;
    private final CloseContactCaseMapper closeContactCaseMapper;
    private final UserMapper userMapper;

    /** 终筛结果是否为潜伏感染者 */
    public static boolean isLatentFinalResult(String finalScreeningResult) {
        if (StrUtil.isBlank(finalScreeningResult)) {
            return false;
        }
        String normalized = ScreeningDiagnosisSupport.normalizeDiagnosis(finalScreeningResult.trim());
        return "潜伏感染者".equals(normalized) || finalScreeningResult.contains("潜伏感染者");
    }

    /**
     * 单条个案同步：创建或补充在管潜伏记录。
     *
     * @return 新建或更新的潜伏记录；非潜伏终筛返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public LatentInfection syncFromCase(CloseContactCase caze) {
        if (caze == null
                || ImportIdentitySupport.isBlankOrPlaceholder(caze.getIdNumber())
                || !isLatentFinalResult(caze.getFinalScreeningResult())) {
            return null;
        }
        String normalizedId = ImportIdentitySupport.normalizeIdNumber(caze.getIdNumber());
        caze.setIdNumber(normalizedId);
        LatentInfection existing = findManualCloseContactLatent(normalizedId, caze.getDepartmentId());
        if (existing == null) {
            if (StrUtil.isBlank(caze.getName())) {
                log.warn("密接个案同步：缺少姓名，跳过新建 caseId={} idNumber={}", caze.getId(), caze.getIdNumber());
                return null;
            }
            LatentInfection created = buildNewLatent(caze);
            latentInfectionMapper.insert(created);
            log.info("密接个案同步：新建在管潜伏 latentId={} caseId={} idNumber={}",
                    created.getId(), caze.getId(), caze.getIdNumber());
            return created;
        }
        if (LatentInfectionService.isTransferLocked(existing)) {
            log.info("密接个案同步：跳过已转出/待确认 latentId={} caseId={}", existing.getId(), caze.getId());
            return existing;
        }
        if (supplementBlankFields(existing, caze)) {
            latentInfectionMapper.updateById(existing);
            log.info("密接个案同步：补充在管潜伏 latentId={} caseId={}", existing.getId(), caze.getId());
        }
        return existing;
    }

    /** 批量回填：所有终筛为潜伏感染者的个案（单条失败不阻断其余） */
    public int syncAllLatentCases() {
        List<CloseContactCase> cases = closeContactCaseMapper.selectList(new LambdaQueryWrapper<CloseContactCase>()
                .like(CloseContactCase::getFinalScreeningResult, "潜伏感染者"));
        int count = 0;
        for (CloseContactCase caze : cases) {
            try {
                if (syncFromCase(caze) != null) {
                    count++;
                }
            } catch (Exception e) {
                log.warn("密接个案同步失败 caseId={} idNumber={}: {}",
                        caze.getId(), caze.getIdNumber(), e.getMessage());
            }
        }
        log.info("密接个案同步：批量处理成功 {} / {} 条潜伏终筛个案", count, cases.size());
        return count;
    }

    /**
     * 详情展示：按证件号回填个案扩展字段（不落库）。
     */
    public void fillCaseDetailFields(LatentInfection latent) {
        if (latent == null || !"closeContact".equals(latent.getPopulationType())
                || ImportIdentitySupport.isBlankOrPlaceholder(latent.getIdNumber())) {
            return;
        }
        CloseContactCase caze = findBestCase(latent.getIdNumber(), latent.getDepartmentId());
        if (caze == null || !isLatentFinalResult(caze.getFinalScreeningResult())) {
            return;
        }
        applyCaseTransientFields(latent, caze);
        // 持久字段展示兜底（仅内存，不写库）
        if (StrUtil.isBlank(latent.getInfectionResult()) && StrUtil.isNotBlank(caze.getInfectionCheckResult())) {
            latent.setInfectionResult(caze.getInfectionCheckResult());
        }
        if (latent.getInfectionScreenDate() == null) {
            LocalDate screenDate = caze.getInfectionCheckDate() != null
                    ? caze.getInfectionCheckDate() : caze.getFirstScreenDate();
            if (screenDate != null) {
                latent.setInfectionScreenDate(screenDate);
                if (latent.getScreenDate() == null) {
                    latent.setScreenDate(screenDate);
                }
            }
        }
        if (StrUtil.isBlank(latent.getScreenMethod()) && StrUtil.isNotBlank(caze.getInfectionCheckMethod())) {
            latent.setScreenMethod(ScreeningMethodSupport.normalize(caze.getInfectionCheckMethod()));
        }
        if (StrUtil.isBlank(latent.getScreenResult()) && StrUtil.isNotBlank(caze.getInfectionCheckResult())) {
            latent.setScreenResult(caze.getInfectionCheckResult());
        }
        if (StrUtil.isBlank(latent.getPreventivePlan()) && StrUtil.isNotBlank(caze.getPreventivePlan())) {
            latent.setPreventivePlan(caze.getPreventivePlan());
        }
        if (StrUtil.isBlank(latent.getCrowdCategory())) {
            String type = normalizeContactType(caze.getContactType());
            if (StrUtil.isNotBlank(type)) {
                latent.setCrowdCategory(type);
            }
        }
        if (StrUtil.isBlank(latent.getEthnicity()) && StrUtil.isNotBlank(caze.getEthnicity())) {
            latent.setEthnicity(caze.getEthnicity());
        }
        if (StrUtil.isBlank(latent.getGender())) {
            String gender = StrUtil.blankToDefault(caze.getGender(), genderFromIdNumber(caze.getIdNumber()));
            if (StrUtil.isNotBlank(gender)) {
                latent.setGender(gender);
            }
        }
        if (StrUtil.isBlank(latent.getPhone()) && StrUtil.isNotBlank(caze.getPhone())) {
            latent.setPhone(caze.getPhone());
        }
        if (StrUtil.isBlank(latent.getHouseholdAddress()) && StrUtil.isNotBlank(caze.getHouseholdAddress())) {
            latent.setHouseholdAddress(caze.getHouseholdAddress());
        }
        if (StrUtil.isBlank(latent.getCurrentAddress()) && StrUtil.isNotBlank(caze.getCurrentAddress())) {
            latent.setCurrentAddress(caze.getCurrentAddress());
        }
    }

    private LatentInfection findManualCloseContactLatent(String idNumber, Long departmentId) {
        String normalizedId = normalizeIdNumber(idNumber);
        if (StrUtil.isBlank(normalizedId)) {
            return null;
        }
        LambdaQueryWrapper<LatentInfection> wrapper = baseManualCloseContactWrapper(normalizedId, idNumber);
        if (departmentId != null) {
            wrapper.eq(LatentInfection::getDepartmentId, departmentId);
        }
        LatentInfection found = latentInfectionMapper.selectOne(wrapper);
        if (found != null || departmentId == null) {
            return found;
        }
        // 部门不一致时再按证件号兜底匹配（前期手工录入可能部门不同）
        return latentInfectionMapper.selectOne(baseManualCloseContactWrapper(normalizedId, idNumber));
    }

    private LambdaQueryWrapper<LatentInfection> baseManualCloseContactWrapper(String normalizedId, String rawId) {
        return new LambdaQueryWrapper<LatentInfection>()
                .eq(LatentInfection::getPopulationType, "closeContact")
                .eq(LatentInfection::getArchived, 0)
                .isNull(LatentInfection::getScreeningId)
                .and(w -> w.isNull(LatentInfection::getArchiveRemark)
                        .or()
                        .notIn(LatentInfection::getArchiveRemark,
                                LatentInfectionService.ARCHIVE_REMARK_TRANSFERRED_OUT,
                                LatentInfectionService.ARCHIVE_REMARK_TRANSFER_PENDING))
                .and(w -> w.eq(LatentInfection::getIdNumber, normalizedId)
                        .or()
                        .eq(LatentInfection::getIdNumber, rawId))
                .orderByDesc(LatentInfection::getId)
                .last("LIMIT 1");
    }

    private CloseContactCase findBestCase(String idNumber, Long departmentId) {
        String normalizedId = normalizeIdNumber(idNumber);
        LambdaQueryWrapper<CloseContactCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(CloseContactCase::getFinalScreeningResult, "潜伏感染者")
                .and(w -> w.eq(CloseContactCase::getIdNumber, normalizedId)
                        .or()
                        .eq(CloseContactCase::getIdNumber, idNumber))
                .orderByDesc(CloseContactCase::getId)
                .last("LIMIT 1");
        if (departmentId != null) {
            wrapper.eq(CloseContactCase::getDepartmentId, departmentId);
        }
        CloseContactCase found = closeContactCaseMapper.selectOne(wrapper);
        if (found != null || departmentId == null) {
            return found;
        }
        return closeContactCaseMapper.selectOne(new LambdaQueryWrapper<CloseContactCase>()
                .like(CloseContactCase::getFinalScreeningResult, "潜伏感染者")
                .and(w -> w.eq(CloseContactCase::getIdNumber, normalizedId)
                        .or()
                        .eq(CloseContactCase::getIdNumber, idNumber))
                .orderByDesc(CloseContactCase::getId)
                .last("LIMIT 1"));
    }

    private LatentInfection buildNewLatent(CloseContactCase caze) {
        String contactType = normalizeContactType(caze.getContactType());
        LocalDate infectionDate = caze.getInfectionCheckDate() != null
                ? caze.getInfectionCheckDate() : caze.getFirstScreenDate();
        String normalizedId = normalizeIdNumber(caze.getIdNumber());
        String gender = StrUtil.blankToDefault(caze.getGender(), genderFromIdNumber(normalizedId));
        Integer treatmentPhase = resolveTreatmentPhase(caze);
        return LatentInfection.builder()
                .populationType("closeContact")
                .crowdCategory(contactType)
                .name(caze.getName())
                .idNumber(normalizedId)
                .gender(gender)
                .age(caze.getAge())
                .phone(caze.getPhone())
                .householdAddress(caze.getHouseholdAddress())
                .currentAddress(caze.getCurrentAddress())
                .infectionScreenDate(infectionDate)
                .infectionResult(caze.getInfectionCheckResult())
                .screenMethod(ScreeningMethodSupport.normalize(caze.getInfectionCheckMethod()))
                .hasChestXray(StrUtil.isNotBlank(caze.getImagingResult()) ? "是" : null)
                .chestXrayDate(caze.getImagingDate())
                .chestXrayResult(caze.getImagingResult())
                .diagnosisFirst("潜伏感染者")
                .diagnosisResult("潜伏感染者")
                .referralResult("latent")
                .trackingStatus(1)
                .notInPlaceCount(0)
                .treatmentPhase(treatmentPhase == null ? 0 : treatmentPhase)
                .remark(caze.getRemark())
                .archived(0)
                .departmentId(caze.getDepartmentId())
                .creatorId(resolveCreatorId(caze))
                .build();
    }

    /** 优先当前登录用户；无登录上下文时按个案录入用户名解析 */
    private Long resolveCreatorId(CloseContactCase caze) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            return currentId;
        }
        if (caze == null || StrUtil.isBlank(caze.getCreatorUsername())) {
            return null;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, caze.getCreatorUsername().trim())
                .last("LIMIT 1"));
        if (user != null) {
            return user.getId();
        }
        // 兼容展示名=真实姓名
        user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRealName, caze.getCreatorUsername().trim())
                .last("LIMIT 1"));
        return user != null ? user.getId() : null;
    }

    /** @return 是否有字段被补充 */
    private boolean supplementBlankFields(LatentInfection latent, CloseContactCase caze) {
        boolean changed = false;
        if (StrUtil.isBlank(latent.getName()) && StrUtil.isNotBlank(caze.getName())) {
            latent.setName(caze.getName());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getGender())) {
            String gender = StrUtil.blankToDefault(caze.getGender(), genderFromIdNumber(normalizeIdNumber(caze.getIdNumber())));
            if (StrUtil.isNotBlank(gender)) {
                latent.setGender(gender);
                changed = true;
            }
        }
        if (latent.getAge() == null && caze.getAge() != null) {
            latent.setAge(caze.getAge());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getPhone()) && StrUtil.isNotBlank(caze.getPhone())) {
            latent.setPhone(caze.getPhone());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getHouseholdAddress()) && StrUtil.isNotBlank(caze.getHouseholdAddress())) {
            latent.setHouseholdAddress(caze.getHouseholdAddress());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getCurrentAddress()) && StrUtil.isNotBlank(caze.getCurrentAddress())) {
            latent.setCurrentAddress(caze.getCurrentAddress());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getCrowdCategory())) {
            String type = normalizeContactType(caze.getContactType());
            if (StrUtil.isNotBlank(type)) {
                latent.setCrowdCategory(type);
                changed = true;
            }
        }
        if (latent.getInfectionScreenDate() == null) {
            LocalDate infectionDate = caze.getInfectionCheckDate() != null
                    ? caze.getInfectionCheckDate() : caze.getFirstScreenDate();
            if (infectionDate != null) {
                latent.setInfectionScreenDate(infectionDate);
                changed = true;
            }
        }
        if (StrUtil.isBlank(latent.getInfectionResult()) && StrUtil.isNotBlank(caze.getInfectionCheckResult())) {
            latent.setInfectionResult(caze.getInfectionCheckResult());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getScreenMethod()) && StrUtil.isNotBlank(caze.getInfectionCheckMethod())) {
            latent.setScreenMethod(ScreeningMethodSupport.normalize(caze.getInfectionCheckMethod()));
            changed = true;
        }
        if (StrUtil.isBlank(latent.getHasChestXray()) && StrUtil.isNotBlank(caze.getImagingResult())) {
            latent.setHasChestXray("是");
            changed = true;
        }
        if (latent.getChestXrayDate() == null && caze.getImagingDate() != null) {
            latent.setChestXrayDate(caze.getImagingDate());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getChestXrayResult()) && StrUtil.isNotBlank(caze.getImagingResult())) {
            latent.setChestXrayResult(caze.getImagingResult());
            changed = true;
        }
        if (StrUtil.isBlank(latent.getDiagnosisFirst())) {
            latent.setDiagnosisFirst("潜伏感染者");
            changed = true;
        }
        if (StrUtil.isBlank(latent.getDiagnosisResult())) {
            latent.setDiagnosisResult("潜伏感染者");
            changed = true;
        }
        if (StrUtil.isBlank(latent.getReferralResult())) {
            latent.setReferralResult("latent");
            changed = true;
        }
        if (StrUtil.isBlank(latent.getRemark()) && StrUtil.isNotBlank(caze.getRemark())) {
            latent.setRemark(caze.getRemark());
            changed = true;
        }
        if (latent.getTreatmentPhase() == null || latent.getTreatmentPhase() == 0) {
            Integer phase = resolveTreatmentPhase(caze);
            if (phase != null && phase > 0) {
                latent.setTreatmentPhase(phase);
                changed = true;
            }
        }
        if (latent.getDepartmentId() == null && caze.getDepartmentId() != null) {
            latent.setDepartmentId(caze.getDepartmentId());
            changed = true;
        }
        if (latent.getCreatorId() == null) {
            Long creatorId = resolveCreatorId(caze);
            if (creatorId != null) {
                latent.setCreatorId(creatorId);
                changed = true;
            }
        }
        return changed;
    }

    private void applyCaseTransientFields(LatentInfection latent, CloseContactCase caze) {
        latent.setCloseContactCaseId(caze.getId());
        latent.setSourcePatientName(caze.getSourcePatientName());
        latent.setSourcePatientCaseNo(caze.getSourcePatientCaseNo());
        latent.setSourcePatientBacteriologyResult(caze.getSourcePatientBacteriologyResult());
        latent.setSourcePatientPhone(caze.getSourcePatientPhone());
        latent.setContactType(caze.getContactType());
        latent.setContactPlace(caze.getContactPlace());
        latent.setRegistrationDate(caze.getRegistrationDate());
        latent.setReportDate(caze.getReportDate());
        latent.setCity(caze.getCity());
        latent.setDistrict(caze.getDistrict());
        latent.setHasContraindication(caze.getHasContraindication());
        latent.setNoTreatmentReason(caze.getNoTreatmentReason());
        latent.setContraindicationRemark(caze.getContraindicationRemark());
        latent.setHasPreventiveTreatment(caze.getHasPreventiveTreatment());
        if (StrUtil.isBlank(latent.getPreventivePlan())) {
            latent.setPreventivePlan(caze.getPreventivePlan());
        }
        latent.setPreventivePlanRemark(caze.getPreventivePlanRemark());
        latent.setTreatmentCompleted(caze.getTreatmentCompleted());
        latent.setIncompleteReason(caze.getIncompleteReason());
        latent.setSputumCheckDate(caze.getSputumCheckDate());
        latent.setSputumCheckMethod(caze.getSputumCheckMethod());
        latent.setSputumCheckResult(caze.getSputumCheckResult());
        latent.setImagingMethod(caze.getImagingMethod());
        latent.setFollowup6Result(caze.getFollowup6Result());
        latent.setFollowup12Result(caze.getFollowup12Result());
        latent.setFollowup24Result(caze.getFollowup24Result());
        latent.setFinalScreeningResult(caze.getFinalScreeningResult());
    }

    private static String normalizeContactType(String contactType) {
        if (StrUtil.isBlank(contactType)) {
            return null;
        }
        String v = contactType.trim();
        if (v.contains("家庭内")) {
            return "家庭内";
        }
        if (v.contains("家庭外")) {
            return "家庭外";
        }
        return null;
    }

    private static Integer resolveTreatmentPhase(CloseContactCase caze) {
        if ("是".equals(caze.getTreatmentCompleted()) || "完成".equals(caze.getTreatmentCompleted())) {
            return 2;
        }
        if ("开展".equals(caze.getHasPreventiveTreatment()) || "是".equals(caze.getHasPreventiveTreatment())) {
            return 1;
        }
        return 0;
    }

    private static String genderFromIdNumber(String idNumber) {
        String id = normalizeIdNumber(idNumber);
        if (StrUtil.isBlank(id) || id.length() < 17) {
            return null;
        }
        char c = id.charAt(16);
        if (!Character.isDigit(c)) {
            return null;
        }
        return ((c - '0') % 2 == 1) ? "男" : "女";
    }

    private static String normalizeIdNumber(String idNumber) {
        return ImportIdentitySupport.normalizeIdNumber(idNumber);
    }
}
