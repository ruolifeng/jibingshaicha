package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.LatentFollowUp;
import cn.luyou.model.LatentCheck;
import cn.luyou.model.MedicationManagement;
import cn.luyou.model.MedicationPickup;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.Referral;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.SysMessage;
import cn.luyou.constant.LatentImportHeaders;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.mapper.SupervisionFormMapper;
import cn.luyou.model.SupervisionForm;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.LatentCheckService;
import cn.luyou.service.LatentFollowUpService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.MedicationPickupService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.FlexibleDateParseUtil;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.DepartmentFilterSupport;
import cn.luyou.utils.ColumnDistinctSupport;
import cn.luyou.utils.ColumnFilterSupport;
import cn.luyou.utils.CreatorUserSupport;
import cn.luyou.utils.CloseContactCaseLatentSyncSupport;
import cn.luyou.utils.ImportDuplicateIdSupport;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.IdentityFormatFilterSupport;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.InfectionScreenFieldSupport;
import cn.luyou.utils.KeyPopulationCrowdCategoryQuerySupport;
import cn.luyou.utils.LatentScreeningLinkSupport;
import cn.luyou.utils.NoticePartyFillSupport;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import cn.luyou.utils.ScreeningMethodSupport;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LatentInfectionServiceImpl extends ServiceImpl<LatentInfectionMapper, LatentInfection>
        implements LatentInfectionService {

    private final DataScopeHelper dataScopeHelper;
    private final DepartmentFilterSupport departmentFilterSupport;
    private final PatientService patientService;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;
    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;
    private final SupervisionFormMapper supervisionFormMapper;
    private final LatentFollowUpService latentFollowUpService;
    private final LatentCheckService latentCheckService;
    private final SupervisionFormService supervisionFormService;
    private final NoticeService noticeService;
    private final ReferralService referralService;
    private final SysMessageService sysMessageService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;
    private final MedicationPickupService medicationPickupService;
    private final EpidemicReportService epidemicReportService;
    private final NoticePartyFillSupport noticePartyFillSupport;
    private final CloseContactCaseLatentSyncSupport closeContactCaseLatentSyncSupport;

    private static final Set<String> COLUMN_FILTER_WHITELIST = Set.of(
            "name", "registrationNo", "gender", "idNumber", "phone", "currentAddress", "householdAddress",
            "infectionResult", "screenMethod", "diagnosisFirst", "diagnosisResult", "populationType",
            "hasChestXray", "chestXrayResult", "creatorUsername", "crowdCategory", "remark",
            "noticeConfirmStatus", "medicationManagementUnit"
    );
    /** 表头 Excel 式下拉：仅枚举/导入内容类字段 */
    private static final Set<String> COLUMN_DISTINCT_FIELDS = Set.of(
            "gender", "populationType", "infectionResult", "screenMethod", "diagnosisFirst", "diagnosisResult",
            "hasChestXray", "chestXrayResult", "crowdCategory", "creatorUsername"
    );
    /** 感染筛查方法筛选白名单（与筛查表/前端选项一致） */
    private static final Set<String> SCREEN_METHOD_FILTER_VALUES = Set.copyOf(InfectionScreenFieldSupport.METHODS);

    private static final Set<String> MANUAL_POPULATION_TYPES = Set.of(
            "school", "keyPopulation", "regular", "epidemic", "referral", "closeContact", "other"
    );

    private static final List<String> KEY_POPULATION_SUB_CATEGORIES = List.of("老年人", "糖尿病", "双感");
    private static final List<String> CLOSE_CONTACT_TYPES = List.of("家庭内", "家庭外");

    /**
     * 首次诊断结果（diagnosisFirst）→ 转诊编码（referralResult）映射。
     * 录入胸片诊断或批量导入胸片诊断后，根据该映射自动驱动转诊流程，
     * 与"诊断"按钮 referral() 方法的语义保持一致。
     */
    private static final Map<String, String> DIAGNOSIS_TO_REFERRAL;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("排除", "excluded");
        m.put("其他", "other");
        m.put("其它", "other");
        m.put("正常", "excluded");
        m.put("确诊患者", "confirmed");
        m.put("确诊结核", "confirmed");
        m.put("在治患者", "confirmed");
        m.put(ScreeningDiagnosisSupport.SUSPECTED_TB_DIAGNOSIS, "suspected");
        m.put("疑似肺结核", "suspected");
        m.put("潜伏感染者", "latent");
        DIAGNOSIS_TO_REFERRAL = java.util.Collections.unmodifiableMap(m);
    }

    @Override
    public IPage<LatentInfection> queryPage(int page, int size, String populationType,
                                             String name, String idNumber, Integer trackingStatus, Integer archived,
                                             String referralResult, String diagnosisFirst,
                                             String phone, String dateFrom, String dateTo,
                                             String dateFilterBy, String creatorName, String crowdCategory,
                                             List<Long> filterDepartmentIds, String columnFilters, String formatIssue) {
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(dateTo);
        boolean noticeFillFilter = "noticeFill".equals(dateFilterBy);
        boolean supervisionFillFilter = "supervisionFill".equals(dateFilterBy);
        boolean hasSpecialDateRange = (noticeFillFilter || supervisionFillFilter)
                && (createFrom != null || createTo != null);
        boolean hasCreatorFilter = StrUtil.isNotBlank(creatorName);
        Set<Long> filterBizIds = null;
        if (hasSpecialDateRange) {
            filterBizIds = supervisionFillFilter
                    ? resolveSupervisionDateBizIds(populationType, createFrom, createTo)
                    : resolveNoticeDateBizIds(populationType, createFrom, createTo);
            if (filterBizIds.isEmpty()) {
                return new Page<>(page, size);
            }
        }
        if (hasCreatorFilter) {
            Set<Long> creatorBizIds = supervisionFillFilter
                    ? resolveSupervisionCreatorBizIds(populationType, creatorName)
                    : noticeFillFilter
                            ? resolveNoticeCreatorBizIds(populationType, creatorName)
                            : resolveOverviewCreatorBizIds(populationType, creatorName);
            if (creatorBizIds.isEmpty()) {
                return new Page<>(page, size);
            }
            if (filterBizIds == null) {
                filterBizIds = creatorBizIds;
            } else {
                filterBizIds.retainAll(creatorBizIds);
                if (filterBizIds.isEmpty()) {
                    return new Page<>(page, size);
                }
            }
        }
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(populationType)) {
            wrapper.eq(LatentInfection::getPopulationType, populationType);
        } else {
            // 聚合查询：排除密接筛查同步数据，保留在管总览手动新增的密接
            wrapper.and(w -> w.ne(LatentInfection::getPopulationType, "closeContact")
                    .or()
                    .isNull(LatentInfection::getScreeningId));
        }
        wrapper.like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), LatentInfection::getPhone, phone)
                .eq(trackingStatus != null, LatentInfection::getTrackingStatus, trackingStatus)
                .eq(archived != null, LatentInfection::getArchived, archived);
        // 在管列表排除「已转出」（兼容历史误留在 archived=0 的数据）
        // 同时排除 referral 已确认接收的源记录，避免转出单位在管总览仍可见
        if (archived == null || Integer.valueOf(0).equals(archived)) {
            wrapper.and(w -> w.isNull(LatentInfection::getArchiveRemark)
                    .or()
                    .ne(LatentInfection::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT));
            wrapper.notInSql(LatentInfection::getId,
                    "SELECT r.biz_id FROM referral r WHERE r.module_type = 'latent'"
                            + " AND r.status = 2 AND r.deleted = 0"
                            + " AND r.biz_id IS NOT NULL AND r.target_biz_id IS NOT NULL");
        }
        ScreeningDiagnosisSupport.applyDiagnosisFirstFilter(
                wrapper, LatentInfection::getDiagnosisFirst, diagnosisFirst);
        wrapper
                // 潜伏感染列表始终排除确诊患者；疑似肺结核保持 diagnosisResult 为空，继续留在待诊断。
                // 注意：SQL 中 NULL NOT IN (...) 结果为 NULL（即被过滤掉），
                // 必须显式放行 diagnosisResult 为 NULL 的记录（导入后未录入诊断的待诊断数据）。
                .and(w -> w.isNull(LatentInfection::getDiagnosisResult)
                        .or()
                        .notIn(LatentInfection::getDiagnosisResult, "确诊患者", "确诊结核", "在治患者"));

        // referralResult 过滤：pending = 查尚未转诊的记录；具体值 = 精确匹配
        if ("pending".equals(referralResult)) {
            wrapper.isNull(LatentInfection::getReferralResult);
        } else if (StrUtil.isNotBlank(referralResult)) {
            wrapper.eq(LatentInfection::getReferralResult, referralResult);
        }

        KeyPopulationCrowdCategoryQuerySupport.applyLatentFilter(
                wrapper, populationType, crowdCategory, screeningKeyPopulationMapper);

        LatentScreeningLinkSupport.applyLinkedScreeningExistsFilter(wrapper);

        if (filterBizIds != null) {
            wrapper.in(LatentInfection::getId, filterBizIds);
        } else if (!hasSpecialDateRange) {
            wrapper.ge(createFrom != null, LatentInfection::getCreateTime, createFrom)
                    .le(createTo != null, LatentInfection::getCreateTime, createTo);
        }
        applyColumnFilters(wrapper, columnFilters);
        IdentityFormatFilterSupport.apply(wrapper, formatIssue, "id_number", "phone");
        ImportRowOrderSupport.applyWithoutBatch(wrapper);
        departmentFilterSupport.applyDepartmentIdFilter(
                wrapper, LatentInfection::getDepartmentId, filterDepartmentIds);
        dataScopeHelper.applyLatentScope(wrapper);
        IPage<LatentInfection> result = page(new Page<>(page, size), wrapper);

        // 补充通知单发送状态：用于前端控制“发送通知单”禁用和督导表启用
        List<LatentInfection> records = result.getRecords();
        if (records == null || records.isEmpty()) return result;

        fillCreatorUsernames(records);
        fillScreeningDiagnosisDraft(records);
        fillPendingEntryReason(records);
        records.forEach(this::fillNoticeAutoFields);
        fillNoticeAndSupervisionStatus(records, populationType);
        fillMedicationPickupSummary(records);

        return result;
    }

    /** 待诊断对账：标注每条记录因何进入待诊断（感染阳性 / 疑似结核等） */
    private void fillPendingEntryReason(List<LatentInfection> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (LatentInfection record : records) {
            String diagnosis = StrUtil.blankToDefault(record.getDiagnosisFirst(), record.getScreeningDiagnosisFirst());
            record.setPendingEntryReason(ScreeningDiagnosisSupport.resolvePendingEntryReason(
                    record.getInfectionResult(),
                    record.getChestXrayResult(),
                    record.getHasChestXray(),
                    diagnosis));
        }
    }

    /** 批量查询领药记录摘要并填充到潜伏感染列表 */
    private void fillMedicationPickupSummary(List<LatentInfection> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> latentIds = records.stream().map(LatentInfection::getId).filter(Objects::nonNull).toList();
        if (latentIds.isEmpty()) {
            return;
        }
        List<MedicationPickup> pickups = medicationPickupService.lambdaQuery()
                .in(MedicationPickup::getLatentInfectionId, latentIds)
                .orderByAsc(MedicationPickup::getCreateTime)
                .list();
        Map<Long, List<MedicationPickup>> grouped = pickups.stream()
                .collect(Collectors.groupingBy(MedicationPickup::getLatentInfectionId));
        records.forEach(r -> {
            List<MedicationPickup> list = grouped.get(r.getId());
            if (list == null || list.isEmpty()) {
                r.setMedicationPickupCount(0);
                r.setMedicationPickTime(null);
                r.setMedicationChemotherapy(null);
                r.setMedicationDrugForm(null);
                r.setMedicationEntryUnit(null);
                r.setMedicationEntryPerson(null);
                return;
            }
            r.setMedicationPickupCount(list.size());
            MedicationPickup latest = list.get(list.size() - 1);
            r.setMedicationPickTime(latest.getPickupTime() != null ? latest.getPickupTime().toString() : null);
            r.setMedicationChemotherapy(formatLatentDrugNames(latest.getDrugs()));
            r.setMedicationDrugForm(formatLatentDrugQuantities(
                    latest.getDrugs(), latest.getQuantity(), latest.getQuantityUnit()));
            r.setMedicationEntryUnit(latest.getEntryUnit());
            r.setMedicationEntryPerson(latest.getEntryPerson());
        });
    }

    private String formatLatentDrugNames(String drugsJson) {
        if (StrUtil.isBlank(drugsJson)) {
            return null;
        }
        try {
            cn.hutool.json.JSONArray array = JSONUtil.parseArray(drugsJson);
            return array.stream()
                    .map(item -> {
                        if (item instanceof cn.hutool.json.JSONObject obj) {
                            return obj.getStr("name");
                        }
                        return null;
                    })
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining("、"));
        } catch (Exception e) {
            return null;
        }
    }

    private String formatLatentDrugQuantities(String drugsJson, java.math.BigDecimal legacyQuantity, String legacyUnit) {
        if (StrUtil.isNotBlank(drugsJson)) {
            try {
                cn.hutool.json.JSONArray array = JSONUtil.parseArray(drugsJson);
                String joined = array.stream()
                        .map(item -> {
                            if (!(item instanceof cn.hutool.json.JSONObject obj)) {
                                return null;
                            }
                            Object quantity = obj.get("quantity");
                            if (quantity == null) {
                                return null;
                            }
                            String name = obj.getStr("name");
                            String unit = obj.getStr("quantityUnit");
                            return (StrUtil.isNotBlank(name) ? name : "药品")
                                    + new java.math.BigDecimal(quantity.toString()).stripTrailingZeros().toPlainString()
                                    + (StrUtil.isNotBlank(unit) ? unit : "");
                        })
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining("；"));
                if (StrUtil.isNotBlank(joined)) {
                    return joined;
                }
            } catch (Exception ignored) {
                // fallback
            }
        }
        if (legacyQuantity != null && StrUtil.isNotBlank(legacyUnit)) {
            return legacyQuantity.stripTrailingZeros().toPlainString() + legacyUnit;
        }
        return null;
    }

    private void applyColumnFilters(LambdaQueryWrapper<LatentInfection> wrapper, String columnFilters) {
        Map<String, String> filters = ColumnFilterSupport.parse(columnFilters);
        ColumnFilterSupport.applyLambda(filters, COLUMN_FILTER_WHITELIST, (field, value) -> {
            switch (field) {
                case "name" -> ColumnFilterSupport.like(wrapper, LatentInfection::getName, value);
                case "registrationNo" -> ColumnFilterSupport.like(wrapper, LatentInfection::getRegistrationNo, value);
                case "gender" -> ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getGender, value);
                case "idNumber" -> ColumnFilterSupport.like(wrapper, LatentInfection::getIdNumber, value);
                case "phone" -> ColumnFilterSupport.like(wrapper, LatentInfection::getPhone, value);
                case "currentAddress" -> ColumnFilterSupport.like(wrapper, LatentInfection::getCurrentAddress, value);
                case "householdAddress" -> ColumnFilterSupport.like(wrapper, LatentInfection::getHouseholdAddress, value);
                case "infectionResult" -> applyInfectionResultFilter(wrapper, value);
                case "screenMethod" -> applyScreenMethodFilter(wrapper, value);
                case "diagnosisFirst" -> ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getDiagnosisFirst, value);
                case "diagnosisResult" -> ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getDiagnosisResult, value);
                case "populationType" -> ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getPopulationType, value);
                case "hasChestXray" -> ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getHasChestXray, value);
                case "chestXrayResult" -> ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getChestXrayResult, value);
                case "crowdCategory" -> ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getCrowdCategory, value);
                case "remark" -> ColumnFilterSupport.like(wrapper, LatentInfection::getRemark, value);
                case "creatorUsername" -> {
                    List<Long> ids = resolveCreatorIdsByFilterValue(value);
                    if (!ids.isEmpty()) {
                        wrapper.in(LatentInfection::getCreatorId, ids);
                    }
                }
                case "noticeConfirmStatus" -> applyNoticeConfirmStatusFilter(wrapper, value);
                case "medicationManagementUnit" -> applyMedicationManagementUnitFilter(wrapper, value);
                default -> { }
            }
        });
    }

    /**
     * 服药管理单位筛选：通知单服药管理单位，或督导表管理单位。
     */
    private void applyMedicationManagementUnitFilter(LambdaQueryWrapper<LatentInfection> wrapper, String value) {
        Collection<String> values = ColumnFilterSupport.splitValues(value);
        if (values.isEmpty()) {
            return;
        }
        Set<Long> matchedIds = new HashSet<>();
        for (String raw : values) {
            String keyword = raw == null ? "" : raw.trim();
            if (keyword.isEmpty()) {
                continue;
            }
            noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                            .eq(Notice::getNoticeType, "latent")
                            .like(Notice::getMedicationManagementUnit, keyword)
                            .select(Notice::getBizId))
                    .forEach(n -> {
                        if (n.getBizId() != null) {
                            matchedIds.add(n.getBizId());
                        }
                    });
            supervisionFormMapper.selectList(new LambdaQueryWrapper<SupervisionForm>()
                            .like(SupervisionForm::getManagingUnit, keyword)
                            .select(SupervisionForm::getLatentInfectionId))
                    .forEach(f -> {
                        if (f.getLatentInfectionId() != null) {
                            matchedIds.add(f.getLatentInfectionId());
                        }
                    });
        }
        if (matchedIds.isEmpty()) {
            wrapper.apply("1=0");
        } else {
            wrapper.in(LatentInfection::getId, matchedIds);
        }
    }

    /**
     * 通知单确认状态筛选（与前端「通知单确认状态」列一致）：
     * 2/已确认、1/待确认、none/未确认（无通知单或草稿）。
     */
    private void applyNoticeConfirmStatusFilter(LambdaQueryWrapper<LatentInfection> wrapper, String value) {
        Set<String> statuses = new LinkedHashSet<>(ColumnFilterSupport.splitValues(value));
        if (statuses.isEmpty()) {
            return;
        }
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getNoticeType, "latent")
                .select(Notice::getId, Notice::getBizId, Notice::getStatus));
        Map<Long, Notice> latest = new HashMap<>();
        for (Notice n : notices) {
            if (n.getBizId() == null) continue;
            Notice prev = latest.get(n.getBizId());
            if (prev == null || (n.getId() != null && prev.getId() != null && n.getId() > prev.getId())) {
                latest.put(n.getBizId(), n);
            }
        }
        Set<Long> matched = new HashSet<>();
        boolean includeNone = statuses.contains("none")
                || statuses.contains("未确认")
                || statuses.contains("—")
                || statuses.contains("-");
        boolean wantConfirmed = statuses.contains("2") || statuses.contains("已确认");
        boolean wantPending = statuses.contains("1") || statuses.contains("待确认");
        for (Map.Entry<Long, Notice> e : latest.entrySet()) {
            Integer st = e.getValue().getStatus();
            if (wantConfirmed && st != null && st == 2) {
                matched.add(e.getKey());
            } else if (wantPending && st != null && st == 1) {
                matched.add(e.getKey());
            }
        }
        Set<Long> confirmedOrPending = latest.entrySet().stream()
                .filter(e -> e.getValue().getStatus() != null
                        && (e.getValue().getStatus() == 1 || e.getValue().getStatus() == 2))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (includeNone) {
            wrapper.and(w -> {
                boolean started = false;
                if (!matched.isEmpty()) {
                    w.in(LatentInfection::getId, matched);
                    started = true;
                }
                if (started) {
                    w.or();
                }
                if (!confirmedOrPending.isEmpty()) {
                    w.notIn(LatentInfection::getId, confirmedOrPending);
                } else {
                    w.apply("1=1");
                }
            });
            return;
        }
        if (matched.isEmpty()) {
            wrapper.apply("1=0");
        } else {
            wrapper.in(LatentInfection::getId, matched);
        }
    }

    /** 感染筛查结果筛选：官方下拉同时匹配学校历史文案 */
    private void applyInfectionResultFilter(LambdaQueryWrapper<LatentInfection> wrapper, String value) {
        List<String> variants = ColumnFilterSupport.splitValues(value).stream()
                .flatMap(v -> InfectionScreenFieldSupport.expandFilterVariants(v).stream())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (variants.isEmpty()) {
            return;
        }
        ColumnFilterSupport.eqOrIn(wrapper, LatentInfection::getInfectionResult, String.join(",", variants));
    }

    /**
     * 感染筛查方法筛选：优先匹配 latent.screen_method，再按关联筛查表方法，
     * 手动录入且方法为空时才用感染筛查结果前缀兜底。
     */
    private void applyScreenMethodFilter(LambdaQueryWrapper<LatentInfection> wrapper, String value) {
        List<String> methods = ColumnFilterSupport.splitValues(value).stream()
                .map(ScreeningMethodSupport::normalize)
                .filter(StrUtil::isNotBlank)
                .filter(SCREEN_METHOD_FILTER_VALUES::contains)
                .distinct()
                .toList();
        if (methods.isEmpty()) {
            return;
        }

        Set<String> methodVariants = methods.stream()
                .flatMap(m -> ScreeningMethodSupport.expandFilterVariants(m).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Long> schoolIds = screeningSchoolMapper.selectList(new LambdaQueryWrapper<ScreeningSchool>()
                        .select(ScreeningSchool::getId)
                        .in(ScreeningSchool::getScreenMethod, methodVariants))
                .stream().map(ScreeningSchool::getId).filter(Objects::nonNull).toList();
        List<Long> keyIds = screeningKeyPopulationMapper.selectList(new LambdaQueryWrapper<ScreeningKeyPopulation>()
                        .select(ScreeningKeyPopulation::getId)
                        .in(ScreeningKeyPopulation::getScreenMethod, methodVariants))
                .stream().map(ScreeningKeyPopulation::getId).filter(Objects::nonNull).toList();

        LambdaQueryWrapper<ScreeningCloseContact> closeWrapper = new LambdaQueryWrapper<ScreeningCloseContact>()
                .select(ScreeningCloseContact::getId);
        closeWrapper.and(q -> {
            boolean first = true;
            for (String method : methodVariants) {
                if (!first) {
                    q.or();
                }
                q.and(inner -> inner.like(ScreeningCloseContact::getInfectionCheckMethod, method)
                        .or().like(ScreeningCloseContact::getFollowup6ImagingMethod, method)
                        .or().like(ScreeningCloseContact::getFollowup12ImagingMethod, method));
                first = false;
            }
        });
        List<Long> closeIds = screeningCloseContactMapper.selectList(closeWrapper).stream()
                .map(ScreeningCloseContact::getId).filter(Objects::nonNull).toList();

        wrapper.and(w -> {
            boolean first = true;
            // 持久化字段直接匹配（含密接个案同步/手动新增；兼容短码与官方文案）
            w.in(LatentInfection::getScreenMethod, methodVariants);
            first = false;
            if (!schoolIds.isEmpty()) {
                if (!first) {
                    w.or();
                }
                w.and(s -> s.eq(LatentInfection::getPopulationType, "school")
                        .in(LatentInfection::getScreeningId, schoolIds));
                first = false;
            }
            if (!keyIds.isEmpty()) {
                if (!first) {
                    w.or();
                }
                w.and(k -> k.in(LatentInfection::getPopulationType, "keyPopulation", "regular")
                        .in(LatentInfection::getScreeningId, keyIds));
                first = false;
            }
            if (!closeIds.isEmpty()) {
                if (!first) {
                    w.or();
                }
                w.and(c -> c.eq(LatentInfection::getPopulationType, "closeContact")
                        .in(LatentInfection::getScreeningId, closeIds));
                first = false;
            }
            if (!first) {
                w.or();
            }
            // 仅方法为空的手动记录按感染筛查结果前缀推断（兼容短码）
            w.and(ir -> {
                ir.and(blank -> blank.isNull(LatentInfection::getScreenMethod)
                        .or().eq(LatentInfection::getScreenMethod, ""));
                ir.isNull(LatentInfection::getScreeningId);
                ir.and(prefix -> {
                    boolean irFirst = true;
                    for (String method : methodVariants) {
                        if (!irFirst) {
                            prefix.or();
                        }
                        prefix.likeRight(LatentInfection::getInfectionResult, method);
                        irFirst = false;
                    }
                });
            });
        });
    }

    private void fillCreatorUsernames(List<LatentInfection> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream()
                .map(LatentInfection::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (User u : userMapper.selectBatchIds(ids)) {
            if (u != null) {
                String display = StrUtil.blankToDefault(u.getRealName(), u.getUsername());
                if (StrUtil.isNotBlank(display)) {
                    nameMap.put(u.getId(), display.trim());
                }
            }
        }
        for (LatentInfection r : records) {
            if (r.getCreatorId() != null) {
                r.setCreatorUsername(nameMap.get(r.getCreatorId()));
            }
        }
    }

    /** 按通知单首次填写时间（notice.create_time）筛选 */
    private Set<Long> resolveNoticeDateBizIds(String populationType,
                                              LocalDateTime noticeFrom, LocalDateTime noticeTo) {
        LambdaQueryWrapper<Notice> noticeWrapper = new LambdaQueryWrapper<>();
        noticeWrapper.eq(Notice::getNoticeType, "latent")
                .eq(StrUtil.isNotBlank(populationType), Notice::getPopulationType, populationType)
                .ge(noticeFrom != null, Notice::getCreateTime, noticeFrom)
                .le(noticeTo != null, Notice::getCreateTime, noticeTo);
        return noticeMapper.selectList(noticeWrapper.select(Notice::getBizId)).stream()
                .map(Notice::getBizId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 按督导表填写时间（supervision_form.create_time）筛选 */
    private Set<Long> resolveSupervisionDateBizIds(String populationType,
                                                   LocalDateTime supervisionFrom, LocalDateTime supervisionTo) {
        LambdaQueryWrapper<SupervisionForm> supervisionWrapper = new LambdaQueryWrapper<>();
        supervisionWrapper.eq(StrUtil.isNotBlank(populationType), SupervisionForm::getPopulationType, populationType)
                .ge(supervisionFrom != null, SupervisionForm::getCreateTime, supervisionFrom)
                .le(supervisionTo != null, SupervisionForm::getCreateTime, supervisionTo);
        return supervisionFormMapper.selectList(supervisionWrapper.select(SupervisionForm::getLatentInfectionId)).stream()
                .map(SupervisionForm::getLatentInfectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 督导表录入者：匹配督导表填写人（filled_by）或病例录入人（creator_id）
     */
    private Set<Long> resolveSupervisionCreatorBizIds(String populationType, String creatorName) {
        List<Long> userIds = resolveUserIdsByCreatorName(creatorName);
        if (userIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> result = new HashSet<>();
        LambdaQueryWrapper<SupervisionForm> supervisionWrapper = new LambdaQueryWrapper<>();
        supervisionWrapper.eq(StrUtil.isNotBlank(populationType), SupervisionForm::getPopulationType, populationType)
                .in(SupervisionForm::getFilledBy, userIds);
        supervisionFormMapper.selectList(supervisionWrapper.select(SupervisionForm::getLatentInfectionId)).stream()
                .map(SupervisionForm::getLatentInfectionId)
                .filter(Objects::nonNull)
                .forEach(result::add);

        LambdaQueryWrapper<LatentInfection> latentWrapper = applyPopulationScope(new LambdaQueryWrapper<>(), populationType);
        latentWrapper.in(LatentInfection::getCreatorId, userIds);
        baseMapper.selectList(latentWrapper.select(LatentInfection::getId)).stream()
                .map(LatentInfection::getId)
                .filter(Objects::nonNull)
                .forEach(result::add);
        return result;
    }

    /** 在管总览录入者：匹配病例录入人（latent.creator_id） */
    private Set<Long> resolveOverviewCreatorBizIds(String populationType, String creatorName) {
        List<Long> userIds = resolveUserIdsByCreatorName(creatorName);
        if (userIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<LatentInfection> latentWrapper = applyPopulationScope(new LambdaQueryWrapper<>(), populationType);
        latentWrapper.in(LatentInfection::getCreatorId, userIds);
        return baseMapper.selectList(latentWrapper.select(LatentInfection::getId)).stream()
                .map(LatentInfection::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 通知单录入者：匹配病例录入人（latent.creator_id）或通知单填写人（notice.sender_id）
     */
    private Set<Long> resolveNoticeCreatorBizIds(String populationType, String creatorName) {
        List<Long> userIds = resolveUserIdsByCreatorName(creatorName);
        if (userIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> result = new HashSet<>();
        LambdaQueryWrapper<Notice> noticeWrapper = new LambdaQueryWrapper<>();
        noticeWrapper.eq(Notice::getNoticeType, "latent")
                .eq(StrUtil.isNotBlank(populationType), Notice::getPopulationType, populationType)
                .in(Notice::getSenderId, userIds);
        noticeMapper.selectList(noticeWrapper.select(Notice::getBizId)).stream()
                .map(Notice::getBizId)
                .filter(Objects::nonNull)
                .forEach(result::add);

        LambdaQueryWrapper<LatentInfection> latentWrapper = applyPopulationScope(new LambdaQueryWrapper<>(), populationType);
        latentWrapper.in(LatentInfection::getCreatorId, userIds);
        baseMapper.selectList(latentWrapper.select(LatentInfection::getId)).stream()
                .map(LatentInfection::getId)
                .filter(Objects::nonNull)
                .forEach(result::add);
        return result;
    }

    private LambdaQueryWrapper<LatentInfection> applyPopulationScope(LambdaQueryWrapper<LatentInfection> wrapper,
                                                                     String populationType) {
        if (StrUtil.isNotBlank(populationType)) {
            wrapper.eq(LatentInfection::getPopulationType, populationType);
        } else {
            wrapper.and(w -> w.ne(LatentInfection::getPopulationType, "closeContact")
                    .or()
                    .isNull(LatentInfection::getScreeningId));
        }
        return wrapper;
    }

    private List<Long> resolveUserIdsByCreatorName(String creatorName) {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .and(w -> w.like(User::getRealName, creatorName)
                                .or()
                                .like(User::getUsername, creatorName))
                        .select(User::getId))
                .stream()
                .map(User::getId)
                .toList();
    }

    /** 待诊断阶段：从关联筛查表读取已导入、尚未确认的首次诊断，供列表展示与弹窗预填 */
    private void fillScreeningDiagnosisDraft(List<LatentInfection> records) {
        if (records == null || records.isEmpty()) return;
        for (LatentInfection record : records) {
            if (record.getScreeningId() == null || StrUtil.isNotBlank(record.getDiagnosisFirst())) continue;
            String draft = loadScreeningDiagnosisFirst(record);
            if (StrUtil.isNotBlank(draft)) {
                record.setScreeningDiagnosisFirst(draft);
            }
        }
    }

    private String loadScreeningDiagnosisFirst(LatentInfection entity) {
        if (entity.getScreeningId() == null) return null;
        String popType = entity.getPopulationType();
        if ("school".equals(popType)) {
            ScreeningSchool s = screeningSchoolMapper.selectById(entity.getScreeningId());
            return s != null ? s.getDiagnosisFirst() : null;
        }
        if ("keyPopulation".equals(popType) || "regular".equals(popType)) {
            ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(entity.getScreeningId());
            return k != null ? k.getDiagnosisFirst() : null;
        }
        if ("closeContact".equals(popType)) {
            ScreeningCloseContact c = screeningCloseContactMapper.selectById(entity.getScreeningId());
            return c != null ? c.getFinalScreeningResult() : null;
        }
        return null;
    }

    /** 补充通知单发送状态与督导表状态（列表/详情共用） */
    private void fillNoticeAndSupervisionStatus(List<LatentInfection> records, String populationType) {
        if (records == null || records.isEmpty()) return;

        List<Long> latentIds = records.stream()
                .map(LatentInfection::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (latentIds.isEmpty()) {
            records.forEach(r -> {
                r.setNoticeSent(false);
                r.setNoticeStatus(null);
                r.setNoticeId(null);
                r.setNoticeSenderName(null);
                r.setNoticeReceiverName(null);
                r.setNoticeMedicationUnit(null);
                r.setSupervisionCompleted(false);
                r.setSupervisionStatus(0);
                r.setTreatmentCompletionStatus(null);
            });
            return;
        }

        Map<Long, cn.luyou.model.Notice> noticeMap = noticeMapper.selectList(
                new LambdaQueryWrapper<cn.luyou.model.Notice>()
                        .in(cn.luyou.model.Notice::getBizId, latentIds)
                        .eq(cn.luyou.model.Notice::getNoticeType, "latent")
                        .eq(StrUtil.isNotBlank(populationType), cn.luyou.model.Notice::getPopulationType, populationType)
                        .orderByDesc(cn.luyou.model.Notice::getId)
        ).stream().collect(java.util.stream.Collectors.toMap(
                cn.luyou.model.Notice::getBizId,
                n -> n,
                (a, b) -> a,
                java.util.LinkedHashMap::new
        ));

        List<cn.luyou.model.Notice> notices = new ArrayList<>(noticeMap.values());
        noticePartyFillSupport.fillPartyNames(notices);

        records.forEach(r -> {
            cn.luyou.model.Notice notice = noticeMap.get(r.getId());
            if (notice != null) {
                r.setNoticeStatus(notice.getStatus());
                r.setNoticeId(notice.getId());
                r.setNoticeSent(notice.getStatus() != null && notice.getStatus() >= 1);
                r.setNoticeSenderName(notice.getSenderName());
                r.setNoticeReceiverName(notice.getReceiverName());
                if (StrUtil.isNotBlank(notice.getMedicationManagementUnit())) {
                    r.setNoticeMedicationUnit(notice.getMedicationManagementUnit().trim());
                } else {
                    r.setNoticeMedicationUnit(null);
                }
                // 列表登记号以通知单为准（数据来源：通知单）
                if (StrUtil.isNotBlank(notice.getRegistrationNo())) {
                    r.setRegistrationNo(notice.getRegistrationNo().trim());
                }
            } else {
                r.setNoticeStatus(null);
                r.setNoticeId(null);
                r.setNoticeSent(false);
                r.setNoticeSenderName(null);
                r.setNoticeReceiverName(null);
                r.setNoticeMedicationUnit(null);
            }
        });

        Map<Long, Integer> supervisionStatusMap = supervisionFormMapper.selectList(
                new LambdaQueryWrapper<SupervisionForm>()
                        .in(SupervisionForm::getLatentInfectionId, latentIds)
                        .orderByDesc(SupervisionForm::getCreateTime)
        ).stream().collect(java.util.stream.Collectors.groupingBy(
                SupervisionForm::getLatentInfectionId,
                java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        forms -> {
                            if (forms.isEmpty()) return 0;
                            boolean hasArchived = forms.stream().anyMatch(f -> Integer.valueOf(2).equals(f.getStatus()));
                            if (hasArchived) return 2;
                            boolean hasSubmitted = forms.stream().anyMatch(f -> Integer.valueOf(1).equals(f.getStatus()));
                            if (hasSubmitted) return 1;
                            return 0;
                        }
                )
        ));
        Map<Long, SupervisionForm> preferredSupervisionMap = supervisionFormMapper.selectList(
                new LambdaQueryWrapper<SupervisionForm>()
                        .in(SupervisionForm::getLatentInfectionId, latentIds)
                        .orderByDesc(SupervisionForm::getCreateTime)
        ).stream().collect(java.util.stream.Collectors.groupingBy(
                SupervisionForm::getLatentInfectionId,
                java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        this::selectPreferredSupervisionForm
                )
        ));
        records.forEach(r -> {
            Integer status = supervisionStatusMap.get(r.getId());
            r.setSupervisionStatus(status != null ? status : 0);
            r.setSupervisionCompleted(Integer.valueOf(2).equals(status));
            SupervisionForm preferred = preferredSupervisionMap.get(r.getId());
            r.setTreatmentCompletionStatus(preferred != null ? preferred.getTreatmentCompletionStatus() : null);
            if (StrUtil.isBlank(r.getNoticeMedicationUnit())
                    && preferred != null
                    && StrUtil.isNotBlank(preferred.getManagingUnit())) {
                r.setNoticeMedicationUnit(preferred.getManagingUnit().trim());
            }
        });
    }

    private SupervisionForm selectPreferredSupervisionForm(List<SupervisionForm> forms) {
        if (forms == null || forms.isEmpty()) {
            return null;
        }
        return forms.stream()
                .sorted((a, b) -> {
                    int aArchived = Integer.valueOf(2).equals(a.getStatus()) ? 1 : 0;
                    int bArchived = Integer.valueOf(2).equals(b.getStatus()) ? 1 : 0;
                    if (aArchived != bArchived) {
                        return Integer.compare(bArchived, aArchived);
                    }
                    if (a.getCreateTime() != null && b.getCreateTime() != null) {
                        return b.getCreateTime().compareTo(a.getCreateTime());
                    }
                    long aId = a.getId() != null ? a.getId() : 0L;
                    long bId = b.getId() != null ? b.getId() : 0L;
                    return Long.compare(bId, aId);
                })
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Long> findLatentIdsByPreferredTreatmentCompletionStatus(String treatmentCompletionStatus) {
        return supervisionFormMapper.selectList(
                new LambdaQueryWrapper<SupervisionForm>()
                        .isNotNull(SupervisionForm::getLatentInfectionId)
        ).stream()
                .collect(java.util.stream.Collectors.groupingBy(SupervisionForm::getLatentInfectionId))
                .entrySet().stream()
                .filter(entry -> {
                    SupervisionForm preferred = selectPreferredSupervisionForm(entry.getValue());
                    return preferred != null
                            && treatmentCompletionStatus.equals(preferred.getTreatmentCompletionStatus());
                })
                .map(java.util.Map.Entry::getKey)
                .toList();
    }

    private void fillNoticeAutoFields(LatentInfection latent) {
        if (latent == null || StrUtil.isBlank(latent.getPopulationType())) return;
        if (latent.getScreeningId() == null) {
            if (StrUtil.isNotBlank(latent.getScreenMethod())) {
                latent.setScreenMethod(ScreeningMethodSupport.normalize(latent.getScreenMethod()));
            } else if (StrUtil.isNotBlank(latent.getInfectionResult())) {
                // 无方法时不在列表强制推断写入，展示层可兜底；此处保持为空避免误标 PPD
            }
            if ("closeContact".equals(latent.getPopulationType()) && StrUtil.isBlank(latent.getCrowdCategory())) {
                latent.setCrowdCategory("密接");
            }
            return;
        }
        switch (latent.getPopulationType()) {
            case "school" -> {
                ScreeningSchool s = screeningSchoolMapper.selectById(latent.getScreeningId());
                if (s == null) return;
                latent.setBirthDate(s.getBirthDate());
                latent.setEthnicity(s.getEthnicity());
                if (StrUtil.isNotBlank(s.getPhone())) {
                    latent.setPhone(s.getPhone());
                }
                if (StrUtil.isNotBlank(s.getCurrentAddress())) {
                    latent.setCurrentAddress(s.getCurrentAddress());
                }
                latent.setHouseholdAddress(s.getHouseholdAddress());
                latent.setScreenDate(s.getScreenDate());
                latent.setScreenMethod(ScreeningMethodSupport.normalize(s.getScreenMethod()));
                latent.setScreenResult(s.getScreenResult());
                if (StrUtil.isNotBlank(s.getPreventivePlan())) {
                    latent.setPreventivePlan(s.getPreventivePlan());
                }
                // 学校人群通知单人群分类默认“学生”
                latent.setCrowdCategory("学生");
            }
            // 疫情筛查与重点人群共用 ScreeningKeyPopulation 表
            case "keyPopulation", "regular" -> {
                ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(latent.getScreeningId());
                if (k == null) return;
                latent.setBirthDate(k.getBirthDate());
                latent.setEthnicity(k.getEthnicity());
                if (StrUtil.isNotBlank(k.getPhone())) {
                    latent.setPhone(k.getPhone());
                }
                if (StrUtil.isNotBlank(k.getCurrentAddress())) {
                    latent.setCurrentAddress(k.getCurrentAddress());
                }
                latent.setHouseholdAddress(k.getHouseholdAddress());
                latent.setScreenDate(k.getScreenDate());
                latent.setScreenMethod(ScreeningMethodSupport.normalize(k.getScreenMethod()));
                latent.setScreenResult(k.getScreenResult());
                if (StrUtil.isNotBlank(k.getPreventivePlan())) {
                    latent.setPreventivePlan(k.getPreventivePlan());
                }
                latent.setCrowdCategory(resolveKeyPopulationCrowdCategory(k));
            }
            case "closeContact" -> {
                ScreeningCloseContact c = screeningCloseContactMapper.selectById(latent.getScreeningId());
                if (c == null) return;
                // ScreeningCloseContact 无独立 birthDate 字段，通知单出生日期留空由前端手填
                latent.setEthnicity(c.getEthnicity());
                if (StrUtil.isNotBlank(c.getPhone())) {
                    latent.setPhone(c.getPhone());
                }
                if (StrUtil.isNotBlank(c.getCurrentAddress())) {
                    latent.setCurrentAddress(c.getCurrentAddress());
                }
                latent.setHouseholdAddress(c.getHouseholdAddress());
                if (StrUtil.isNotBlank(c.getPreventivePlan())) {
                    latent.setPreventivePlan(c.getPreventivePlan());
                }
                latent.setCrowdCategory("密接");
                Integer round = latent.getActiveRound() == null ? 1 : latent.getActiveRound();
                switch (round) {
                    case 1 -> {
                        // 首次筛查：index 18=firstScreenDate, 22=infectionCheckMethod, 23=infectionCheckResult
                        latent.setScreenDate(c.getFirstScreenDate());
                        latent.setScreenMethod(ScreeningMethodSupport.normalize(c.getInfectionCheckMethod()));
                        latent.setScreenResult(c.getInfectionCheckResult());
                    }
                    case 2 -> {
                        // 6月随访：index 40=followup6ScreenDate, 44=followup6ImagingMethod, 49=followup6Result
                        latent.setScreenDate(c.getFollowup6ScreenDate());
                        latent.setScreenMethod(ScreeningMethodSupport.normalize(c.getFollowup6ImagingMethod()));
                        latent.setScreenResult(c.getFollowup6Result());
                    }
                    case 3 -> {
                        // 12月随访：index 51=followup12ScreenDate, 55=followup12ImagingMethod, 60=followup12Result
                        latent.setScreenDate(c.getFollowup12ScreenDate());
                        latent.setScreenMethod(ScreeningMethodSupport.normalize(c.getFollowup12ImagingMethod()));
                        latent.setScreenResult(c.getFollowup12Result());
                    }
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
    }

    private String resolveKeyPopulationCrowdCategory(ScreeningKeyPopulation k) {
        if ("是".equals(k.getCrowdCategoryClose())) return "密接";
        if ("是".equals(k.getCrowdCategoryStudent())) return "学生";
        if ("是".equals(k.getCrowdCategoryTeacher())) return "教职工";
        if ("是".equals(k.getCrowdCategoryElder())) return "老年人";
        if ("是".equals(k.getCrowdCategoryDiabetes())) return "糖尿病";
        if ("是".equals(k.getCrowdCategoryDual())) return "双感";
        // 与推介人群分类选项「既往结核史」保持一致
        if ("是".equals(k.getCrowdCategoryTbHist())) return "既往结核史";
        if ("是".equals(k.getCrowdCategoryNormal())) return "非重点人群";
        return "";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void track(Long id, Integer status, String remark, LocalDate actualArrivalDate) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        assertLatentNotTransferLocked(entity);

        if (Integer.valueOf(1).equals(entity.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "记录已归档，不能再追踪");
        }
        Integer currentStatus = entity.getTrackingStatus();
        if (currentStatus != null && currentStatus != 0 && currentStatus != 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前追踪状态不允许继续操作");
        }

        if (status == null || status < 1 || status > 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的追踪状态");
        }
        if (StrUtil.isBlank(remark)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写追踪备注");
        }
        if (Integer.valueOf(1).equals(status) && actualArrivalDate == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择到位时间");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> history = parseTrackingHistory(entity.getTrackingHistoryJson());

        Map<String, Object> entry = new HashMap<>();
        entry.put("attempt", history.size() + 1);
        entry.put("status", status);
        entry.put("trackTime", now.toString());
        entry.put("reason", remark);
        if (Integer.valueOf(1).equals(status)) {
            entry.put("actualArrivalDate", actualArrivalDate.toString());
        }
        history.add(entry);

        switch (status) {
            case 1 -> {
                entity.setTrackingStatus(1);
                entity.setActualArrivalDate(actualArrivalDate);
            }
            case 2 -> {
                // 未到位
                int count = (entity.getNotInPlaceCount() == null ? 0 : entity.getNotInPlaceCount()) + 1;
                entity.setNotInPlaceCount(count);
                if (count >= 3) {
                    entity.setTrackingStatus(4); // 强制结束
                    entity.setArchived(1);
                    entity.setArchivedTime(now);
                } else {
                    entity.setTrackingStatus(2);
                }
            }
            case 3 -> {
                // 其他
                entity.setTrackingStatus(3);
                entity.setArchived(1);
                entity.setArchivedTime(now);
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的追踪状态");
        }

        entity.setTrackingRemark(remark);
        entity.setTrackingHistoryJson(JSONUtil.toJsonStr(history));
        updateById(entity);
    }

    /** 解析追踪历史 JSON */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseTrackingHistory(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return (List<Map<String, Object>>) (List<?>) JSONUtil.toList(json, Map.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveXrayAndDiagnosis(Long id, Map<String, Object> data) {
        // 兼容入口：批量导入与旧前端继续走此方法（一次性同时传胸片+诊断）。
        // 内部拆分为两步以复用 V13 的拆分逻辑。
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        assertLatentNotTransferLocked(entity);
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入胸片与诊断结果");
        }
        if (StrUtil.isNotBlank(entity.getDiagnosisFirst())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "胸片与诊断已录入，不可重复操作");
        }
        String diagnosisFirst = data.getOrDefault("diagnosisFirst", "").toString();
        if (StrUtil.isBlank(diagnosisFirst)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
        }
        // 先写胸片（不校验"已录入"，沿用旧行为：覆盖式写入）
        doSaveXray(entity, data, /* skipExistsCheck */ true);
        // 重新加载后写诊断并触发转诊
        LatentInfection refreshed = getById(id);
        doSaveDiagnosis(refreshed, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveXrayOnly(Long id, Map<String, Object> data) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        assertLatentNotTransferLocked(entity);
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入胸片结果");
        }
        if (StrUtil.isNotBlank(entity.getChestXrayResult())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "胸片结果已录入，不可重复操作");
        }
        doSaveXray(entity, data, /* skipExistsCheck */ false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDiagnosisOnly(Long id, Map<String, Object> data) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        assertLatentNotTransferLocked(entity);
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后可录入诊断结果");
        }
        if (StrUtil.isNotBlank(entity.getDiagnosisFirst())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果已录入，不可重复操作");
        }
        doSaveDiagnosis(entity, data);
    }

    /** 内部：写胸片字段 + 回写筛查表。skipExistsCheck=true 时不做"已录入"校验（兼容老接口）。 */
    private void doSaveXray(LatentInfection entity, Map<String, Object> data, boolean skipExistsCheck) {
        String hasXray = data.getOrDefault("hasChestXray", "").toString();
        String xrayResult = data.getOrDefault("chestXrayResult", "").toString();
        LocalDate xrayDate = null;
        Object xrayDateObj = data.get("chestXrayDate");
        if (xrayDateObj != null && StrUtil.isNotBlank(xrayDateObj.toString())) {
            xrayDate = LocalDate.parse(xrayDateObj.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        lambdaUpdate()
                .eq(LatentInfection::getId, entity.getId())
                .set(LatentInfection::getHasChestXray, hasXray)
                .set(LatentInfection::getChestXrayDate, xrayDate)
                .set(LatentInfection::getChestXrayResult, xrayResult)
                .update();
        // 同步回写到筛查表（不传 diagnosisFirst）
        writeBackXrayToScreening(entity, hasXray, xrayDate, xrayResult, null);
    }

    /** 内部：写诊断字段 + 回写筛查表 + 触发转诊映射 */
    private void doSaveDiagnosis(LatentInfection entity, Map<String, Object> data) {
        String diagnosisFirst = normalizeLatentDiagnosis(entity.getPopulationType(),
                data.getOrDefault("diagnosisFirst", "").toString());
        if (StrUtil.isBlank(diagnosisFirst)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
        }
        lambdaUpdate()
                .eq(LatentInfection::getId, entity.getId())
                .set(LatentInfection::getDiagnosisFirst, diagnosisFirst)
                .update();
        // 同步回写筛查表诊断字段（不动胸片）
        writeBackXrayToScreening(entity, null, null, null, diagnosisFirst);

        // 根据首次诊断自动驱动转诊（与 referral() 语义一致）
        String referralCode = DIAGNOSIS_TO_REFERRAL.get(diagnosisFirst);
        if (referralCode != null) {
            LatentInfection refreshed = getById(entity.getId());
            applyReferralOutcome(refreshed, referralCode, null);
        }
    }

    /** 重点/疫情保留「正常」「确诊结核」等口径；学生仍走学校归一 */
    private String normalizeLatentDiagnosis(String populationType, String raw) {
        if (StrUtil.isBlank(raw)) {
            return raw;
        }
        if ("keyPopulation".equals(populationType) || "regular".equals(populationType)) {
            String key = ScreeningDiagnosisSupport.normalizeKeyPopulationDiagnosis(raw);
            if (key != null) {
                return key;
            }
        }
        return ScreeningDiagnosisSupport.normalizeDiagnosis(raw);
    }

    private static final Set<String> SCHOOL_SCREEN_METHODS = Set.of("PPD", "EC", "IGRA", "未查");
    private static final Set<String> SCHOOL_INFECTION_RESULTS = Set.of("未感染", "感染", "无法判读", "未查");

    private void applyInfectionMethodUpdate(LatentInfection latent, String method) {
        if (StrUtil.isBlank(method)) {
            latent.setScreenMethod(null);
            return;
        }
        validateInfectionFieldsForPopulation(latent.getPopulationType(), method, null);
        latent.setScreenMethod(normalizeInfectionMethodForPopulation(latent.getPopulationType(), method));
    }

    private void applyInfectionResultUpdate(LatentInfection latent, String result) {
        if (StrUtil.isBlank(result)) {
            latent.setInfectionResult(null);
            return;
        }
        validateInfectionFieldsForPopulation(latent.getPopulationType(), null, result);
        latent.setInfectionResult(normalizeInfectionResultForPopulation(latent.getPopulationType(), result));
    }

    private void validateInfectionFieldsForPopulation(String populationType, String method, String result) {
        if (StrUtil.isNotBlank(method) && !isValidInfectionMethodForPopulation(populationType, method)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "school".equals(populationType)
                            ? "感染筛查方法仅支持：PPD/EC/IGRA/未查"
                            : "感染筛查方法仅支持：结核菌素皮肤试验_PPD/结核抗原皮肤试验_EC/γ干扰素释放试验_IGRA/未做");
        }
        if (StrUtil.isNotBlank(result) && !isValidInfectionResultForPopulation(populationType, result)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "结果判定仅支持：一般阳性/中度阳性/强阳性/阳性/阴性/未判读");
        }
    }

    private boolean isValidInfectionMethodForPopulation(String populationType, String method) {
        if (StrUtil.isBlank(method)) {
            return true;
        }
        String trimmed = method.trim();
        if ("school".equals(populationType)) {
            return SCHOOL_SCREEN_METHODS.contains(trimmed) || InfectionScreenFieldSupport.isValidMethod(trimmed);
        }
        return InfectionScreenFieldSupport.isValidMethod(trimmed);
    }

    private boolean isValidInfectionResultForPopulation(String populationType, String result) {
        if (StrUtil.isBlank(result)) {
            return true;
        }
        String trimmed = result.trim();
        if ("school".equals(populationType)) {
            return SCHOOL_INFECTION_RESULTS.contains(trimmed) || InfectionScreenFieldSupport.isValidResult(trimmed);
        }
        return InfectionScreenFieldSupport.isValidResult(trimmed);
    }

    private String normalizeInfectionMethodForPopulation(String populationType, String method) {
        if (StrUtil.isBlank(method)) {
            return null;
        }
        String trimmed = method.trim();
        if ("school".equals(populationType)) {
            if (SCHOOL_SCREEN_METHODS.contains(trimmed)) {
                return trimmed;
            }
            // 兼容官方全称回写到学校短码
            String official = InfectionScreenFieldSupport.normalizeMethod(trimmed);
            if (official == null) {
                return trimmed;
            }
            return switch (official) {
                case "结核菌素皮肤试验_PPD" -> "PPD";
                case "结核抗原皮肤试验_EC" -> "EC";
                case "γ干扰素释放试验_IGRA" -> "IGRA";
                case "未做" -> "未查";
                default -> trimmed;
            };
        }
        return ScreeningMethodSupport.normalize(trimmed);
    }

    private String normalizeInfectionResultForPopulation(String populationType, String result) {
        if (StrUtil.isBlank(result)) {
            return null;
        }
        String trimmed = result.trim();
        String official = InfectionScreenFieldSupport.normalizeResult(trimmed);
        if (official != null) {
            return official;
        }
        if ("school".equals(populationType) && SCHOOL_INFECTION_RESULTS.contains(trimmed)) {
            return trimmed;
        }
        return trimmed;
    }

    /**
     * 将胸片与诊断数据回写到对应的筛查管理表。
     * 使用 LambdaUpdateWrapper 精确更新目标字段，避免 updateById 回写其他无关字段。
     */
    private void writeBackXrayToScreening(LatentInfection entity, String hasXray,
                                          LocalDate xrayDate, String xrayResult, String diagnosis) {
        Long sid = entity.getScreeningId();
        if (sid == null) return;
        String type = entity.getPopulationType();
        if (StrUtil.isBlank(type)) return;

        switch (type) {
            case "school" -> {
                var update = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ScreeningSchool>()
                        .eq(ScreeningSchool::getId, sid);
                if (StrUtil.isNotBlank(hasXray)) update.set(ScreeningSchool::getHasChestXray, hasXray);
                if (xrayDate != null) update.set(ScreeningSchool::getChestXrayDate, xrayDate);
                if (StrUtil.isNotBlank(xrayResult)) update.set(ScreeningSchool::getChestXrayResult, xrayResult);
                if (StrUtil.isNotBlank(diagnosis)) update.set(ScreeningSchool::getDiagnosisFirst, diagnosis);
                screeningSchoolMapper.update(null, update);
            }
            case "keyPopulation", "regular" -> {
                var update = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ScreeningKeyPopulation>()
                        .eq(ScreeningKeyPopulation::getId, sid);
                if (StrUtil.isNotBlank(hasXray)) update.set(ScreeningKeyPopulation::getHasChestXray, hasXray);
                if (xrayDate != null) update.set(ScreeningKeyPopulation::getChestXrayDate, xrayDate);
                if (StrUtil.isNotBlank(xrayResult)) update.set(ScreeningKeyPopulation::getChestXrayResult, xrayResult);
                if (StrUtil.isNotBlank(diagnosis)) update.set(ScreeningKeyPopulation::getDiagnosisFirst, diagnosis);
                screeningKeyPopulationMapper.update(null, update);
            }
            case "closeContact" -> {
                // 密接人群的胸片回写到 latent_infection 表已完成，无需再回写到筛查表
                // （密接筛查表结构与胸片字段不对应，由各轮次随访字段承载）
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importXrayBatch(MultipartFile file, String populationType) {
        // 与各人群主导入的 headRowNumber 保持一致
        int headerRows = switch (populationType) {
            case "keyPopulation", "regular" -> 3;
            default -> 2;
        };

        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream())
                    .headRowNumber(headerRows)
                    .sheet()
                    .doReadSync()
                    .forEach(row -> rows.add((Map<String, Object>) row));
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败：" + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        int updated = 0;
        for (Map<String, Object> row : rows) {
            // 密接人群证件号在列 11（71 列官方模板），其余人群在列 9
            int idNumberIdx = "closeContact".equals(populationType) ? 11 : 9;
            String idNumber = getStrCell(row, idNumberIdx);
            if (ImportIdentitySupport.isBlankOrPlaceholder(idNumber)) continue;
            idNumber = ImportIdentitySupport.normalizeIdNumber(idNumber);

            LambdaQueryWrapper<LatentInfection> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(LatentInfection::getIdNumber, idNumber)
                    .eq(LatentInfection::getPopulationType, populationType)
                    .eq(LatentInfection::getArchived, 0)
                    .last("LIMIT 1");
            dataScopeHelper.applyImportDedupScope(
                    dupWrapper, LatentInfection::getDepartmentId, LatentInfection::getCreatorId);
            LatentInfection entity = getOne(dupWrapper, false);
            if (entity == null || !Integer.valueOf(1).equals(entity.getTrackingStatus())) continue;
            if (StrUtil.isNotBlank(entity.getDiagnosisFirst())) continue;

            // 根据人群类型确定 Excel 中胸片/诊断字段的列索引（与模型 @ExcelProperty(index=N) 对齐）
            int hasXrayIdx, xrayDateIdx, xrayResultIdx, diagnosisIdx;
            switch (populationType) {
                case "school" -> {
                    // 学校人群：hasChestXray(25) chestXrayDate(26) chestXrayResult(27) diagnosisFirst(30)
                    hasXrayIdx = 25; xrayDateIdx = 26; xrayResultIdx = 27; diagnosisIdx = 30;
                }
                case "keyPopulation", "regular" -> {
                    // 重点/常规筛查：hasChestXray(37) chestXrayDate(38) chestXrayResult(39) diagnosisFirst(40)
                    hasXrayIdx = 37; xrayDateIdx = 38; xrayResultIdx = 39; diagnosisIdx = 40;
                }
                case "closeContact" -> {
                    // 密接人群：按阳性轮次读取对应列组（71 列官方模板）
                    Integer round = entity.getActiveRound();
                    if (round == null) round = 1;
                    switch (round) {
                        // 首次筛查：imagingDate(23) imagingMethod(24) imagingResult(25) finalScreeningResult(29)
                        case 1 -> { hasXrayIdx = 23; xrayDateIdx = 24; xrayResultIdx = 25; diagnosisIdx = 29; }
                        // 6月随访：followup6ImagingDate(42) followup6ImagingMethod(43) followup6ImagingResult(44) followup6Result(48)
                        case 2 -> { hasXrayIdx = 42; xrayDateIdx = 43; xrayResultIdx = 44; diagnosisIdx = 48; }
                        // 12月随访：followup12ImagingDate(53) followup12ImagingMethod(54) followup12ImagingResult(55) followup12Result(59)
                        case 3 -> { hasXrayIdx = 53; xrayDateIdx = 54; xrayResultIdx = 55; diagnosisIdx = 59; }
                        default -> { continue; }
                    }
                }
                default -> { continue; }
            }

            String diagnosisFirst = getStrCell(row, diagnosisIdx);
            String hasXray = getStrCell(row, hasXrayIdx);
            LocalDate xrayDate = parseDateCell(row.get(xrayDateIdx));
            String xrayResult = getStrCell(row, xrayResultIdx);

            // 至少包含胸片结果或确认诊断才更新（支持仅导入胸片结果）
            if (StrUtil.isBlank(xrayResult) && StrUtil.isBlank(diagnosisFirst)) continue;

            // 写入胸片字段；诊断仅回写筛查表，须待诊断页确认后才分流
            var updateWrapper = lambdaUpdate()
                    .eq(LatentInfection::getId, entity.getId())
                    .set(StrUtil.isNotBlank(hasXray), LatentInfection::getHasChestXray, hasXray)
                    .set(xrayDate != null, LatentInfection::getChestXrayDate, xrayDate)
                    .set(StrUtil.isNotBlank(xrayResult), LatentInfection::getChestXrayResult, xrayResult);
            updateWrapper.update();

            writeBackXrayToScreening(entity, hasXray, xrayDate, xrayResult,
                    StrUtil.isNotBlank(diagnosisFirst) ? diagnosisFirst : null);
            updated++;
        }
        log.info("批量导入胸片结果，populationType={}，成功更新 {} 条", populationType, updated);
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void referral(Long id, String result, String remark, LocalDate actualReferralDate) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        assertLatentNotTransferLocked(entity);
        if (!Integer.valueOf(1).equals(entity.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先完成追踪到位操作后再进行转诊");
        }
        if (StrUtil.isBlank(entity.getDiagnosisFirst())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先录入胸片检查与诊断结果后再进行转诊");
        }
        if (StrUtil.isNotBlank(entity.getReferralResult())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已完成转诊，不可重复操作");
        }
        if (actualReferralDate == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择转诊时间");
        }

        // 胸片诊断为确诊患者/疑似肺结核时，不允许转诊为潜伏感染者
        if ("latent".equals(result) &&
                (ScreeningDiagnosisSupport.isConfirmedPatientDiagnosis(entity.getDiagnosisFirst())
                        || ScreeningDiagnosisSupport.isSuspectedTbDiagnosis(entity.getDiagnosisFirst()))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "胸片诊断结果为「" + entity.getDiagnosisFirst() + "」，不可转诊为潜伏感染者，请选择正确的转诊结果");
        }

        entity.setActualReferralDate(actualReferralDate);
        applyReferralOutcome(entity, result, remark);
    }

    /**
     * 执行转诊后的状态变更：写入 referralResult/diagnosisResult，并按结果归档或进入潜伏感染管理。
     */
    private void applyReferralOutcome(LatentInfection entity, String result, String remark) {
        entity.setReferralResult(result);
        entity.setReferralRemark(remark);

        switch (result) {
            case "excluded" -> {
                entity.setDiagnosisResult("排除");
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "other" -> {
                entity.setDiagnosisResult("其他");
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "confirmed" -> {
                // 筛查确诊仅结案归档，不进入患者管理（患者管理数据仅来自专病信息表导入）
                String confirmedLabel = StrUtil.blankToDefault(entity.getDiagnosisFirst(), "确诊患者");
                entity.setDiagnosisResult(confirmedLabel);
                entity.setArchived(1);
                entity.setArchivedTime(LocalDateTime.now());
            }
            case "suspected" -> {
                // 疑似肺结核不归档、不进入潜伏感染者管理，保留在待诊断阶段继续处理。
                entity.setReferralResult(null);
                entity.setReferralRemark(null);
                entity.setDiagnosisResult(null);
                entity.setArchived(0);
                entity.setArchivedTime(null);
            }
            case "latent" -> entity.setDiagnosisResult("潜伏感染者");
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的转诊结果");
        }

        updateById(entity);
    }

    /**
     * 根据潜伏感染记录创建对应的患者档案，并从筛查表补全人口学字段。
     * 幂等：若该 latentInfectionId 已存在患者记录则直接跳过。
     */
    private void createPatientFromLatent(LatentInfection entity) {
        boolean alreadyExists = patientService.lambdaQuery()
                .eq(Patient::getLatentInfectionId, entity.getId())
                .exists();
        if (alreadyExists) return;

        Patient.PatientBuilder pb = Patient.builder()
                .screeningId(entity.getScreeningId())
                .latentInfectionId(entity.getId())
                .populationType(entity.getPopulationType())
                .name(entity.getName())
                .gender(entity.getGender())
                .age(entity.getAge())
                .idNumber(entity.getIdNumber())
                .phone(entity.getPhone())
                .diagnosisResult(entity.getDiagnosisResult())
                .source("confirmed")
                .archived(0)
                .departmentId(entity.getDepartmentId());

        String popType = entity.getPopulationType();
        if ("school".equals(popType) && entity.getScreeningId() != null) {
            ScreeningSchool s = screeningSchoolMapper.selectById(entity.getScreeningId());
            if (s != null) {
                pb.birthDate(s.getBirthDate()).idType(s.getIdType())
                  .ethnicity(s.getEthnicity())
                  .householdAddress(s.getHouseholdAddress())
                  .currentAddress(s.getCurrentAddress());
            }
        } else if ("keyPopulation".equals(popType) && entity.getScreeningId() != null) {
            ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(entity.getScreeningId());
            if (k != null) {
                pb.birthDate(k.getBirthDate()).idType(k.getIdType())
                  .ethnicity(k.getEthnicity())
                  .householdAddress(k.getHouseholdAddress())
                  .currentAddress(k.getCurrentAddress());
            }
        } else if ("closeContact".equals(popType) && entity.getScreeningId() != null) {
            ScreeningCloseContact c = screeningCloseContactMapper.selectById(entity.getScreeningId());
            if (c != null) {
                // ScreeningCloseContact 无 birthDate/idType 字段，患者档案中留空
                pb.ethnicity(c.getEthnicity())
                  .householdAddress(c.getHouseholdAddress())
                  .currentAddress(c.getCurrentAddress());
            }
        }

        patientService.save(pb.build());
    }

    @Override
    public void setMedicationStatus(Long id, Integer medicationStatus) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        assertLatentNotTransferLocked(entity);
        if (entity.getTreatmentPhase() == null || entity.getTreatmentPhase() != 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前记录不在预防治疗阶段");
        }
        entity.setMedicationStatus(medicationStatus);
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeCase(Long id) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        assertLatentNotTransferLocked(entity);
        entity.setTreatmentPhase(2);
        entity.setArchived(1);
        entity.setArchivedTime(LocalDateTime.now());
        updateById(entity);
        // 进入历史患者后，再将督导表预防性治疗数据回写到筛查管理
        syncPreventiveTreatmentToScreening(entity);
    }

    /**
     * 结案进入历史患者后：将优先督导表中的预防性治疗字段回写到对应筛查管理表。
     * 学校 → screening_school；重点/疫情 → screening_key_population；密接 → screening_close_contact
     */
    private void syncPreventiveTreatmentToScreening(LatentInfection latent) {
        if (latent == null || latent.getId() == null || latent.getScreeningId() == null) {
            return;
        }
        String type = latent.getPopulationType();
        if (StrUtil.isBlank(type)) {
            return;
        }
        List<SupervisionForm> forms = supervisionFormMapper.selectList(
                new LambdaQueryWrapper<SupervisionForm>()
                        .eq(SupervisionForm::getLatentInfectionId, latent.getId())
                        .orderByDesc(SupervisionForm::getCreateTime));
        SupervisionForm form = selectPreferredSupervisionForm(forms);
        if (form == null) {
            return;
        }
        // 与督导表归档时一致：补齐供筛查回写的兼容字段
        if (StrUtil.isBlank(form.getHasPreventiveTreatment()) && StrUtil.isNotBlank(form.getTreatmentPlan())) {
            form.setHasPreventiveTreatment("不服药".equals(form.getTreatmentPlan()) ? "否" : "是");
        }
        if (StrUtil.isBlank(form.getPreventiveManager()) && StrUtil.isNotBlank(form.getManagingUnit())) {
            form.setPreventiveManager(form.getManagingUnit());
        }
        if (StrUtil.isBlank(form.getPreventiveManager())
                && (StrUtil.isNotBlank(form.getManagerType()) || StrUtil.isNotBlank(form.getManagerName()))) {
            StringBuilder manager = new StringBuilder();
            if (StrUtil.isNotBlank(form.getManagerType())) {
                manager.append(form.getManagerType());
            }
            if (StrUtil.isNotBlank(form.getManagerName())) {
                if (manager.length() > 0) {
                    manager.append(" - ");
                }
                manager.append(form.getManagerName());
            }
            form.setPreventiveManager(manager.toString());
        }
        if (StrUtil.isBlank(form.getPreventiveResult())
                && "无".equals(form.getInterruptMedication())
                && form.getTreatmentEndDate() != null) {
            form.setPreventiveResult("规范完成");
        }

        Long screeningId = latent.getScreeningId();
        switch (type) {
            case "school" -> {
                ScreeningSchool s = screeningSchoolMapper.selectById(screeningId);
                if (s != null) {
                    s.setHasPreventiveTreatment(form.getHasPreventiveTreatment());
                    s.setPreventivePlan(form.getTreatmentPlan());
                    s.setPreventiveStartDate(form.getTreatmentStartDate());
                    s.setPreventiveEndDate(form.getTreatmentEndDate());
                    s.setPreventiveResult(form.getPreventiveResult());
                    s.setPreventiveManager(form.getPreventiveManager());
                    screeningSchoolMapper.updateById(s);
                }
            }
            case "keyPopulation", "regular" -> {
                ScreeningKeyPopulation k = screeningKeyPopulationMapper.selectById(screeningId);
                if (k != null) {
                    k.setHasPreventiveTreatment(form.getHasPreventiveTreatment());
                    k.setPreventivePlan(form.getTreatmentPlan());
                    k.setPreventiveStartDate(form.getTreatmentStartDate());
                    k.setPreventiveEndDate(form.getTreatmentEndDate());
                    k.setPreventiveResult(form.getPreventiveResult());
                    k.setPreventiveManager(form.getPreventiveManager());
                    screeningKeyPopulationMapper.updateById(k);
                }
            }
            case "closeContact" -> {
                ScreeningCloseContact c = screeningCloseContactMapper.selectById(screeningId);
                if (c != null) {
                    c.setHasPreventiveTreatment(StrUtil.blankToDefault(form.getHasPreventiveTreatment(), "是"));
                    c.setPreventivePlan(form.getTreatmentPlan());
                    screeningCloseContactMapper.updateById(c);
                }
            }
            default -> { /* 未知类型不处理 */ }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unarchiveFromCloseCase(Long id) {
        LatentInfection entity = getById(id);
        if (entity == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "数据不存在");
        }
        if (!Integer.valueOf(1).equals(entity.getArchived())) {
            return;
        }
        if (LatentInfectionService.isTransferLocked(entity)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "转出相关档案不可解锁");
        }
        entity.setArchived(0);
        entity.setArchivedTime(null);
        // 结案归档会将治疗阶段置为已结案；解锁后恢复为预防治疗中，便于继续督导/服药
        if (Integer.valueOf(2).equals(entity.getTreatmentPhase())) {
            entity.setTreatmentPhase(1);
        }
        updateById(entity);
    }

    @Override
    public IPage<LatentInfection> queryHistoryPage(int page, int size, String populationType,
                                                    String name, String idNumber, String phone,
                                                    String startTime, String endTime,
                                                    String treatmentCompletionStatus, String columnFilters) {
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LatentInfection::getArchived, 1)
                // 已转出记录不进入历史患者（仅保留最新在管链路）
                .and(w -> w.isNull(LatentInfection::getArchiveRemark)
                        .or()
                        .ne(LatentInfection::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT))
                .and(w -> w.ne(LatentInfection::getPopulationType, "closeContact")
                        .or()
                        .isNull(LatentInfection::getScreeningId))
                .eq(StrUtil.isNotBlank(populationType), LatentInfection::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), LatentInfection::getPhone, phone)
                .ge(StrUtil.isNotBlank(startTime), LatentInfection::getArchivedTime, startTime)
                .le(StrUtil.isNotBlank(endTime), LatentInfection::getArchivedTime, endTime + " 23:59:59");
        applyColumnFilters(wrapper, columnFilters);
        if (StrUtil.isNotBlank(treatmentCompletionStatus)) {
            List<Long> matchedLatentIds = findLatentIdsByPreferredTreatmentCompletionStatus(treatmentCompletionStatus);
            if (matchedLatentIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            wrapper.in(LatentInfection::getId, matchedLatentIds);
        }
        wrapper.orderByDesc(LatentInfection::getArchivedTime);
        dataScopeHelper.applyLatentScope(wrapper);
        IPage<LatentInfection> result = page(new Page<>(page, size), wrapper);
        List<LatentInfection> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            fillScreeningDiagnosisDraft(records);
            records.forEach(this::fillNoticeAutoFields);
            fillNoticeAndSupervisionStatus(records, populationType);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoReferralForDirectDiagnosis(List<LatentInfection> latents) {
        for (LatentInfection entity : latents) {
            if (entity == null || entity.getId() == null) continue;
            String diagnosisFirst = normalizeLatentDiagnosis(entity.getPopulationType(), entity.getDiagnosisFirst());
            String referralResult = DIAGNOSIS_TO_REFERRAL.get(diagnosisFirst);
            if (StrUtil.isBlank(referralResult)) continue;

            LatentInfection current = getById(entity.getId());
            if (current == null || StrUtil.isNotBlank(current.getReferralResult())) continue;

            boolean archived = !"latent".equals(referralResult);
            boolean keepPendingDiagnosis = "suspected".equals(referralResult);
            lambdaUpdate()
                    .eq(LatentInfection::getId, entity.getId())
                    .set(StrUtil.isBlank(current.getDiagnosisFirst()), LatentInfection::getDiagnosisFirst, diagnosisFirst)
                    .set(LatentInfection::getReferralResult, keepPendingDiagnosis ? null : referralResult)
                    .set(LatentInfection::getDiagnosisResult, keepPendingDiagnosis ? null : diagnosisFirst)
                    .set(LatentInfection::getArchived, archived && !keepPendingDiagnosis ? 1 : 0)
                    .set(LatentInfection::getArchivedTime, archived && !keepPendingDiagnosis ? LocalDateTime.now() : null)
                    .update();

            log.info("导入时自动分流 latentId={} diagnosisFirst={} referralResult={}", entity.getId(), diagnosisFirst, referralResult);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archivePendingLatentFromScreening(Long screeningId, String populationType, String diagnosisFirst) {
        if (screeningId == null || StrUtil.isBlank(populationType)) {
            return;
        }
        LatentInfection latent = lambdaQuery()
                .eq(LatentInfection::getScreeningId, screeningId)
                .eq(LatentInfection::getPopulationType, populationType)
                .eq(LatentInfection::getArchived, 0)
                .isNull(LatentInfection::getReferralResult)
                .last("LIMIT 1")
                .one();
        if (latent == null) {
            return;
        }
        String normalizedDiagnosis = normalizeLatentDiagnosis(populationType,
                StrUtil.isNotBlank(diagnosisFirst) ? diagnosisFirst : latent.getDiagnosisFirst());
        if (StrUtil.isBlank(normalizedDiagnosis)) {
            normalizedDiagnosis = "正常";
        }
        String referralResult = DIAGNOSIS_TO_REFERRAL.get(normalizedDiagnosis);
        if (referralResult == null) {
            referralResult = "excluded";
        }
        if ("suspected".equals(referralResult) || "latent".equals(referralResult)) {
            return;
        }
        lambdaUpdate()
                .eq(LatentInfection::getId, latent.getId())
                .set(LatentInfection::getDiagnosisFirst, normalizedDiagnosis)
                .set(LatentInfection::getReferralResult, referralResult)
                .set(LatentInfection::getDiagnosisResult, normalizedDiagnosis)
                .set(LatentInfection::getArchived, 1)
                .set(LatentInfection::getArchivedTime, LocalDateTime.now())
                .update();
        log.info("筛查不再需待诊断，归档 latentId={} screeningId={} diagnosisFirst={}",
                latent.getId(), screeningId, normalizedDiagnosis);
    }

    private String getStrCell(Map<String, Object> row, int index) {
        Object val = row.get(index);
        return val == null ? "" : val.toString().trim();
    }

    /**
     * 兼容 Excel 日期单元格的多种返回类型（Date、LocalDateTime、字符串等）
     */
    private LocalDate parseDateCell(Object val) {
        return FlexibleDateParseUtil.parse(val);
    }

    @Override
    public LatentInfection getDetail(Long id) {
        dataScopeHelper.assertLatentAccessible(id);
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        fillNoticeAutoFields(latent);
        if (latent.getScreeningId() == null && latent.getInfectionScreenDate() != null) {
            latent.setScreenDate(latent.getInfectionScreenDate());
        }
        closeContactCaseLatentSyncSupport.fillCaseDetailFields(latent);
        fillNoticeAndSupervisionStatus(List.of(latent), latent.getPopulationType());
        return latent;
    }

    @Override
    public void applyOverviewColumnFilters(LambdaQueryWrapper<LatentInfection> wrapper, String columnFilters) {
        applyColumnFilters(wrapper, columnFilters);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBasicInfo(Long id, Map<String, Object> body) {
        dataScopeHelper.assertLatentAccessible(id);
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        assertLatentNotTransferLocked(latent);
        if (body.get("name") != null) latent.setName(body.get("name").toString());
        if (body.get("gender") != null) latent.setGender(body.get("gender").toString());
        if (body.get("age") != null) {
            Object ageVal = body.get("age");
            latent.setAge(ageVal == null || "".equals(ageVal.toString()) ? null : Integer.valueOf(ageVal.toString()));
        }
        if (body.get("idNumber") != null) {
            String idNumber = ImportIdentitySupport.normalizeIdNumber(body.get("idNumber").toString());
            if (StrUtil.isNotBlank(idNumber) && !isValidIdCard(idNumber)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
            }
            latent.setIdNumber(idNumber);
        }
        if (body.get("phone") != null) latent.setPhone(body.get("phone").toString());
        if (body.get("phoneContactRelation") != null) {
            latent.setPhoneContactRelation(body.get("phoneContactRelation").toString());
        }
        if (body.get("householdAddress") != null) latent.setHouseholdAddress(body.get("householdAddress").toString());
        if (body.get("currentAddress") != null) latent.setCurrentAddress(body.get("currentAddress").toString());
        if (body.get("infectionScreenDate") != null) {
            latent.setInfectionScreenDate(parseDateCell(body.get("infectionScreenDate")));
        }
        if (body.get("screenMethod") != null) {
            String method = body.get("screenMethod").toString();
            applyInfectionMethodUpdate(latent, method);
        }
        if (body.get("infectionResult") != null) {
            String result = body.get("infectionResult").toString();
            applyInfectionResultUpdate(latent, result);
        }
        if (body.get("diagnosisFirst") != null) {
            String diagnosisFirst = normalizeLatentDiagnosis(
                    latent.getPopulationType(), body.get("diagnosisFirst").toString());
            latent.setDiagnosisFirst(StrUtil.isBlank(diagnosisFirst)
                    ? null
                    : diagnosisFirst);
        }
        if (body.get("hasChestXray") != null) latent.setHasChestXray(body.get("hasChestXray").toString());
        if (body.get("chestXrayDate") != null) {
            latent.setChestXrayDate(parseDateCell(body.get("chestXrayDate")));
        }
        if (body.get("chestXrayResult") != null) latent.setChestXrayResult(body.get("chestXrayResult").toString());
        if (body.containsKey("trackingHistory")) {
            applyTrackingHistoryRemarkUpdates(latent, body.get("trackingHistory"));
        } else if (body.get("trackingRemark") != null) {
            latent.setTrackingRemark(body.get("trackingRemark").toString());
        }
        if (body.get("remark") != null) latent.setRemark(body.get("remark").toString());
        if (latent.getScreeningId() == null && body.get("crowdCategory") != null) {
            applyManualCrowdCategory(latent, body.get("crowdCategory").toString());
        }
        updateById(latent);
    }

    /**
     * 按 attempt 合并前端提交的追踪备注（可改状态），保留原有时间等字段，并回写 trackingRemark / trackingStatus。
     */
    @SuppressWarnings("unchecked")
    private void applyTrackingHistoryRemarkUpdates(LatentInfection latent, Object trackingHistoryParam) {
        if (trackingHistoryParam == null) {
            return;
        }
        List<Map<String, Object>> existing = parseTrackingHistory(latent.getTrackingHistoryJson());
        if (existing.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "暂无追踪过程可编辑");
        }
        List<Map<String, Object>> updates;
        if (trackingHistoryParam instanceof List<?> list) {
            updates = (List<Map<String, Object>>) (List<?>) list;
        } else if (trackingHistoryParam instanceof String json && StrUtil.isNotBlank(json)) {
            updates = parseTrackingHistory(json);
        } else {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "追踪过程格式无效");
        }
        Map<Integer, Map<String, Object>> updateByAttempt = new HashMap<>();
        for (Map<String, Object> item : updates) {
            if (item == null) continue;
            Integer attempt = toInteger(item.get("attempt"));
            if (attempt == null) continue;
            Object reasonObj = item.get("reason");
            String reason = reasonObj == null ? "" : reasonObj.toString().trim();
            if (StrUtil.isBlank(reason)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "第" + attempt + "次追踪备注不能为空");
            }
            Integer status = toInteger(item.get("status"));
            if (status != null && (status < 1 || status > 3)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "第" + attempt + "次追踪状态无效");
            }
            Map<String, Object> patch = new HashMap<>();
            patch.put("reason", reason);
            if (status != null) {
                patch.put("status", status);
            }
            updateByAttempt.put(attempt, patch);
        }
        if (updateByAttempt.isEmpty()) {
            return;
        }
        for (Map<String, Object> entry : existing) {
            Integer attempt = toInteger(entry.get("attempt"));
            if (attempt == null || !updateByAttempt.containsKey(attempt)) {
                continue;
            }
            Map<String, Object> patch = updateByAttempt.get(attempt);
            entry.put("reason", patch.get("reason"));
            if (patch.get("status") != null) {
                entry.put("status", patch.get("status"));
            }
        }
        latent.setTrackingHistoryJson(JSONUtil.toJsonStr(existing));
        Map<String, Object> last = existing.get(existing.size() - 1);
        Object lastReason = last.get("reason");
        if (lastReason != null) {
            latent.setTrackingRemark(lastReason.toString());
        }
        Integer lastStatus = toInteger(last.get("status"));
        if (lastStatus != null && lastStatus >= 1 && lastStatus <= 3
                && (latent.getTrackingStatus() == null || latent.getTrackingStatus() != 4)) {
            // 强制结束(4)不因编辑回溯；其余与末次追踪状态对齐，同步列表「追踪状态」
            latent.setTrackingStatus(lastStatus);
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(Map<String, Object> body) {
        String name = body.getOrDefault("name", "").toString().trim();
        String idNumber = ImportIdentitySupport.normalizeIdNumber(
                body.getOrDefault("idNumber", "").toString().trim());
        String populationType = body.getOrDefault("populationType", "").toString().trim();
        String phone = body.getOrDefault("phone", "").toString().trim();

        if (StrUtil.isBlank(name)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "姓名不能为空");
        }
        if (StrUtil.isNotBlank(idNumber) && !isValidIdCard(idNumber)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(phone) && !isValidPhone(phone)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }
        if (!MANUAL_POPULATION_TYPES.contains(populationType)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的数据来源");
        }

        String crowdCategory = resolveManualCrowdCategory(populationType, body.get("crowdCategory"));

        String screenMethodRaw = body.getOrDefault("screenMethod", "").toString();
        String infectionResultRaw = body.getOrDefault("infectionResult", "").toString();
        validateInfectionFieldsForPopulation(populationType, screenMethodRaw, infectionResultRaw);
        String screenMethodNormalized = normalizeInfectionMethodForPopulation(populationType, screenMethodRaw);
        String infectionResultNormalized = normalizeInfectionResultForPopulation(populationType, infectionResultRaw);

        String diagnosisRaw = body.getOrDefault("diagnosisFirst", "").toString();
        String diagnosisFirst = normalizeLatentDiagnosis(populationType, diagnosisRaw);
        if (StrUtil.isBlank(diagnosisFirst)) {
            diagnosisFirst = "潜伏感染者";
        }
        String referralCode = DIAGNOSIS_TO_REFERRAL.getOrDefault(diagnosisFirst, "latent");
        boolean keepPendingDiagnosis = "suspected".equals(referralCode);
        boolean archived = !"latent".equals(referralCode) && !keepPendingDiagnosis;

        LatentInfection latent = LatentInfection.builder()
                .populationType(populationType)
                .crowdCategory(crowdCategory)
                .name(name)
                .idNumber(idNumber)
                .phone(phone)
                .phoneContactRelation(body.getOrDefault("phoneContactRelation", "").toString())
                .householdAddress(body.getOrDefault("householdAddress", "").toString())
                .currentAddress(body.getOrDefault("currentAddress", "").toString())
                .infectionScreenDate(parseDateCell(body.get("infectionScreenDate")))
                .gender(body.getOrDefault("gender", "").toString())
                .age(parseIntegerField(body.get("age")))
                .screenMethod(screenMethodNormalized)
                .infectionResult(infectionResultNormalized)
                .diagnosisFirst(diagnosisFirst)
                .hasChestXray(body.getOrDefault("hasChestXray", "").toString())
                .chestXrayDate(parseDateCell(body.get("chestXrayDate")))
                .chestXrayResult(body.getOrDefault("chestXrayResult", "").toString())
                .trackingRemark(body.getOrDefault("trackingRemark", "").toString())
                .remark(body.getOrDefault("remark", "").toString())
                .trackingStatus(0)
                .notInPlaceCount(0)
                .archived(archived ? 1 : 0)
                .archivedTime(archived ? LocalDateTime.now() : null)
                .referralResult(keepPendingDiagnosis ? null : referralCode)
                .diagnosisResult(keepPendingDiagnosis ? null : diagnosisFirst)
                .departmentId(BaseContext.getCurrentDepartmentId())
                .creatorId(BaseContext.getCurrentId())
                .build();
        save(latent);
        log.info("手动新增潜伏感染记录 id={}, populationType={}", latent.getId(), populationType);
        return latent.getId();
    }

    public ImportResult importManualBatch(MultipartFile file) {
        return importManualBatch(file, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importManualBatch(MultipartFile file, boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile) {
        List<Map<Integer, String>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    allRows.add(new LinkedHashMap<>(data));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("潜伏感染者批量导入解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败：" + e.getMessage());
        }

        if (allRows.size() < 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = buildImportHeaderIndex(headerRow);

        if (!headerIndex.containsKey("姓名") || !headerIndex.containsKey("证件号") || !headerIndex.containsKey("数据来源")) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "模板表头不正确，请下载最新模板后重试");
        }

        ImportResult result = new ImportResult();
        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());
        for (int i = 0; i < dataRows.size(); i++) {
            Map<Integer, String> row = dataRows.get(i);
            int rowNum = i + 2;
            String name = getImportField(row, headerIndex, "姓名");
            String idNumber = normalizeExcelCellText(getImportField(row, headerIndex, "证件号"));
            if (StrUtil.isBlank(name) && ImportIdentitySupport.isBlankOrPlaceholder(idNumber)) {
                continue;
            }
            ImportIdentitySupport.registerInvalidIdentity(result, rowNum, name, idNumber, confirmSkipInvalid);
        }
        if (ImportIdentitySupport.shouldBlockImport(result, confirmSkipInvalid)) {
            return result;
        }

        List<ImportDuplicateIdSupport.IdentityRow> duplicateScanRows = new ArrayList<>();
        for (int i = 0; i < dataRows.size(); i++) {
            Map<Integer, String> row = dataRows.get(i);
            int rowNum = i + 2;
            String name = getImportField(row, headerIndex, "姓名");
            String idNumber = ImportIdentitySupport.normalizeIdNumber(
                    normalizeExcelCellText(getImportField(row, headerIndex, "证件号")));
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) {
                continue;
            }
            if (ImportIdentitySupport.isMissingBasicIdentity(name, idNumber)) {
                continue;
            }
            if (StrUtil.isBlank(idNumber)) {
                continue;
            }
            String populationType = resolvePopulationType(getImportField(row, headerIndex, "数据来源"));
            if (StrUtil.isBlank(populationType)) {
                continue;
            }
            duplicateScanRows.add(new ImportDuplicateIdSupport.IdentityRow(
                    rowNum,
                    populationType + ":" + ImportDuplicateIdSupport.normalizeIdNumber(idNumber),
                    idNumber,
                    name));
        }
        if (ImportDuplicateIdSupport.blockIfDuplicateInFile(result, duplicateScanRows, confirmSkipDuplicateInFile)) {
            return result;
        }

        Map<Integer, Integer> skipDuplicateRows = result.getDuplicateInFileCount() > 0
                ? ImportDuplicateIdSupport.resolveSkipRowsKeepLast(duplicateScanRows)
                : Map.of();

        for (int i = 0; i < dataRows.size(); i++) {
            Map<Integer, String> row = dataRows.get(i);
            int rowNum = i + 2;
            try {
                String name = getImportField(row, headerIndex, "姓名");
                String rawIdNumber = normalizeExcelCellText(getImportField(row, headerIndex, "证件号"));
                if (StrUtil.isBlank(name) && ImportIdentitySupport.isBlankOrPlaceholder(rawIdNumber)) {
                    continue;
                }
                if (ImportIdentitySupport.isMissingBasicIdentity(name, rawIdNumber)) {
                    continue;
                }
                String idNumber = ImportIdentitySupport.normalizeIdNumber(rawIdNumber);
                boolean missingId = ImportIdentitySupport.isBlankOrPlaceholder(rawIdNumber);

                String populationTypeRaw = getImportField(row, headerIndex, "数据来源");
                String crowdCategoryRaw = getImportField(row, headerIndex, "人群分类");
                String populationType = resolvePopulationType(populationTypeRaw);
                // 数据来源已含细分（如「密接-家庭内」「重点人群-老年人」）时直接带出，不必再填人群分类
                String embeddedCrowd = extractCrowdCategoryFromPopulationLabel(populationTypeRaw);
                if (StrUtil.isNotBlank(embeddedCrowd)) {
                    crowdCategoryRaw = embeddedCrowd;
                } else {
                    String embeddedFromCrowdCol = extractCrowdCategoryFromPopulationLabel(crowdCategoryRaw);
                    if (StrUtil.isNotBlank(embeddedFromCrowdCol)) {
                        crowdCategoryRaw = embeddedFromCrowdCol;
                    }
                }
                String phone = normalizeExcelCellText(getImportField(row, headerIndex, "联系电话"));

                boolean hasError = false;
                if (StrUtil.isNotBlank(idNumber) && !isValidIdCard(idNumber)) {
                    result.addError(rowNum, name, "身份证号格式不正确");
                    hasError = true;
                }
                if (StrUtil.isBlank(populationType)) {
                    result.addError(rowNum, name, "数据来源无效（请填写：学生筛查/重点人群/疫情筛查/大疫情/推介/密接/其它）");
                    hasError = true;
                }
                String crowdCategory = null;
                if (!hasError && StrUtil.isNotBlank(populationType)) {
                    try {
                        crowdCategory = resolveManualCrowdCategory(populationType, crowdCategoryRaw);
                    } catch (ServiceException e) {
                        result.addError(rowNum, name, e.getMessage());
                        hasError = true;
                    }
                }
                if (StrUtil.isNotBlank(phone) && !isValidPhone(phone)) {
                    result.addError(rowNum, name, "手机号格式不正确");
                    hasError = true;
                }
                if (hasError) {
                    continue;
                }

                Integer keptRow = skipDuplicateRows.get(rowNum);
                if (keptRow != null) {
                    result.addDuplicateInFileWarning(rowNum, name, idNumber,
                            "本表重复身份证，已保留第" + keptRow + "行");
                    continue;
                }

                if (StrUtil.isNotBlank(idNumber)) {
                    LambdaQueryWrapper<LatentInfection> dupWrapper = new LambdaQueryWrapper<>();
                    dupWrapper.eq(LatentInfection::getIdNumber, idNumber)
                            .eq(LatentInfection::getPopulationType, populationType)
                            .eq(LatentInfection::getArchived, 0);
                    dataScopeHelper.applyImportDedupScope(
                            dupWrapper, LatentInfection::getDepartmentId, LatentInfection::getCreatorId);
                    if (count(dupWrapper) > 0) {
                        result.addError(rowNum, name, "该证件号在此数据来源下已存在");
                        continue;
                    }
                }

                String importScreenMethod = getImportField(row, headerIndex, "感染筛查方法");
                String importInfectionResult = getImportField(row, headerIndex, "感染筛查结果");
                if (StrUtil.isBlank(importInfectionResult)) {
                    importInfectionResult = getImportField(row, headerIndex, "结果判定");
                }
                if (StrUtil.isNotBlank(importScreenMethod) && !InfectionScreenFieldSupport.isValidMethod(importScreenMethod)) {
                    result.addError(rowNum, name,
                            "感染筛查方法仅支持：结核菌素皮肤试验_PPD/结核抗原皮肤试验_EC/γ干扰素释放试验_IGRA/未做");
                    continue;
                }
                if (StrUtil.isNotBlank(importInfectionResult) && !InfectionScreenFieldSupport.isValidResult(importInfectionResult)) {
                    result.addError(rowNum, name,
                            "感染筛查结果仅支持：一般阳性/中度阳性/强阳性/阳性/阴性/未判读");
                    continue;
                }
                String importInfectionNormalized = InfectionScreenFieldSupport.normalizeResult(importInfectionResult);

                LatentInfection latent = LatentInfection.builder()
                        .populationType(populationType)
                        .crowdCategory(crowdCategory)
                        .name(name)
                        .idNumber(idNumber)
                        .phone(phone)
                        .phoneContactRelation(getImportField(row, headerIndex, "联系电话与联系人关系"))
                        .householdAddress(getImportField(row, headerIndex, "户籍地址"))
                        .currentAddress(getImportField(row, headerIndex, "现住地址"))
                        .infectionScreenDate(parseDateCell(getImportField(row, headerIndex, "感染筛查日期")))
                        .gender(getImportField(row, headerIndex, "性别"))
                        .age(parseIntegerField(getImportField(row, headerIndex, "年龄")))
                        .screenMethod(ScreeningMethodSupport.normalize(importScreenMethod))
                        .infectionResult(importInfectionNormalized != null
                                ? importInfectionNormalized
                                : (StrUtil.isBlank(importInfectionResult) ? null : importInfectionResult))
                        .diagnosisFirst(getImportField(row, headerIndex, "首次诊断"))
                        .hasChestXray(getImportField(row, headerIndex, "是否胸片检查"))
                        .chestXrayDate(parseDateCell(getImportField(row, headerIndex, "胸片检查日期")))
                        .chestXrayResult(getImportField(row, headerIndex, "胸片检查结果"))
                        .trackingRemark(getImportField(row, headerIndex, "追踪情况"))
                        .remark(getImportField(row, headerIndex, "备注"))
                        .trackingStatus(0)
                        .notInPlaceCount(0)
                        .archived(0)
                        .importRowNo(rowNum)
                        .referralResult("latent")
                        .diagnosisResult("潜伏感染者")
                        .departmentId(BaseContext.getCurrentDepartmentId())
                        .creatorId(BaseContext.getCurrentId())
                        .build();
                save(latent);
                result.setSuccessCount(result.getSuccessCount() + 1);
                if (missingId) {
                    ImportIdentitySupport.registerMissingIdWarning(result, rowNum, name);
                }
            } catch (Exception e) {
                result.addError(rowNum, getImportField(row, headerIndex, "姓名"), "数据解析失败：" + e.getMessage());
            }
        }

        if (result.getSuccessCount() == 0 && result.getErrors().isEmpty()) {
            result.addError(0, "", "未找到有效数据行，请确认已填写姓名");
        }

        log.info("潜伏感染者批量导入完成，成功 {} 条，错误 {} 条，缺证件号 {} 条",
                result.getSuccessCount(), result.getErrors().size(), result.getMissingIdCount());
        return result;
    }

    @Override
    public void markTransferPending(Long id) {
        dataScopeHelper.assertLatentAccessible(id);
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        if (Integer.valueOf(1).equals(latent.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已归档记录不可转出");
        }
        if (ARCHIVE_REMARK_TRANSFERRED_OUT.equals(latent.getArchiveRemark())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已转出，不可再次发起");
        }
        if (ARCHIVE_REMARK_TRANSFER_PENDING.equals(latent.getArchiveRemark())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已有待确认的转出申请");
        }
        latent.setArchiveRemark(ARCHIVE_REMARK_TRANSFER_PENDING);
        latent.setArchived(0);
        latent.setArchivedTime(null);
        updateById(latent);
    }

    @Override
    public void markTransferredOut(Long id) {
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        // 已转出记录退出在管：发送方/上级不再看到；全系统在管仅保留接收方复制后的一条
        boolean updated = lambdaUpdate()
                .eq(LatentInfection::getId, id)
                .set(LatentInfection::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT)
                .set(LatentInfection::getArchived, 1)
                .set(LatentInfection::getArchivedTime, LocalDateTime.now())
                .update();
        if (!updated) {
            latent.setArchiveRemark(ARCHIVE_REMARK_TRANSFERRED_OUT);
            latent.setArchived(1);
            latent.setArchivedTime(LocalDateTime.now());
            updateById(latent);
        }
    }

    @Override
    public void restoreTransferredLatent(Long id) {
        LatentInfection latent = getById(id);
        if (latent == null) {
            return;
        }
        if (!ARCHIVE_REMARK_TRANSFER_PENDING.equals(latent.getArchiveRemark())) {
            return;
        }
        latent.setArchiveRemark(null);
        updateById(latent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyLatentForTransferOut(Long sourceLatentId, Long receiverUserId) {
        LatentInfection source = getById(sourceLatentId);
        if (source == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "源潜伏感染记录不存在");
        }
        User receiver = userMapper.selectById(receiverUserId);
        if (receiver == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "接收人不存在");
        }
        if (receiver.getDepartmentId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "接收人未关联部门，无法同步");
        }
        assertNoDuplicateLatentInReceiverInstitution(source, receiverUserId);

        LatentInfection copy = new LatentInfection();
        BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime",
                "creatorId", "departmentId", "sourceLatentId", "archiveRemark", "archived", "archivedTime");
        copy.setSourceLatentId(sourceLatentId);
        copy.setCreatorId(receiverUserId);
        copy.setDepartmentId(receiver.getDepartmentId());
        copy.setArchived(0);
        copy.setArchiveRemark(null);
        copy.setArchivedTime(null);
        save(copy);
        Long newLatentId = copy.getId();

        copyLatentNotices(sourceLatentId, newLatentId);
        copyLatentSupervisionForms(sourceLatentId, newLatentId, receiverUserId);
        copyLatentFollowUps(sourceLatentId, newLatentId);
        copyLatentChecks(sourceLatentId, newLatentId);
        copyLatentMedication(sourceLatentId, newLatentId, receiverUserId);

        // 兜底：确保接收方记录为可操作的在管状态
        lambdaUpdate()
                .eq(LatentInfection::getId, newLatentId)
                .set(LatentInfection::getArchived, 0)
                .set(LatentInfection::getArchiveRemark, null)
                .set(LatentInfection::getArchivedTime, null)
                .update();

        log.info("转出同步：已复制潜伏感染 sourceId={} -> newId={}, receiverUserId={}, deptId={}",
                sourceLatentId, newLatentId, receiverUserId, receiver.getDepartmentId());
        return newLatentId;
    }

    /** 去重按接收机构（录入人），同一乡镇下其他机构的在管记录不阻挡转出 */
    private void assertNoDuplicateLatentInReceiverInstitution(LatentInfection source, Long receiverUserId) {
        if (ImportIdentitySupport.isBlankOrPlaceholder(source.getIdNumber()) || receiverUserId == null) {
            return;
        }
        long count = lambdaQuery()
                .eq(LatentInfection::getCreatorId, receiverUserId)
                .eq(LatentInfection::getIdNumber, source.getIdNumber())
                .eq(LatentInfection::getArchived, 0)
                .and(w -> w.isNull(LatentInfection::getArchiveRemark)
                        .or()
                        .ne(LatentInfection::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT))
                .ne(LatentInfection::getId, source.getId())
                .count();
        if (count > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "接收方机构已存在相同证件号的在管潜伏感染记录，无法转出");
        }
    }

    private void copyLatentNotices(Long sourceLatentId, Long newLatentId) {
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getBizId, sourceLatentId)
                .eq(Notice::getNoticeType, "latent"));
        for (Notice source : notices) {
            Notice copy = new Notice();
            // 保留原始发送人/接收人，便于通知单管理展示；已发送列表会按 source_latent_id 排除副本。
            // 注意：DataScopeHelper 对转出副本不做 notice 扩权，避免原辖区用户继续看见已转出业务。
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "bizId",
                    "senderName", "senderOrgName", "receiverName", "receiverOrgName");
            copy.setBizId(newLatentId);
            if (Integer.valueOf(1).equals(source.getStatus())) {
                copy.setStatus(2);
            }
            noticeMapper.insert(copy);
        }
    }

    private void copyLatentSupervisionForms(Long sourceLatentId, Long newLatentId, Long receiverUserId) {
        List<SupervisionForm> records = supervisionFormMapper.selectList(
                new LambdaQueryWrapper<SupervisionForm>().eq(SupervisionForm::getLatentInfectionId, sourceLatentId));
        for (SupervisionForm source : records) {
            SupervisionForm copy = new SupervisionForm();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime",
                    "latentInfectionId", "filledBy");
            copy.setLatentInfectionId(newLatentId);
            copy.setFilledBy(receiverUserId);
            supervisionFormMapper.insert(copy);
        }
    }

    private void copyLatentFollowUps(Long sourceLatentId, Long newLatentId) {
        List<LatentFollowUp> records = latentFollowUpService.lambdaQuery()
                .eq(LatentFollowUp::getLatentInfectionId, sourceLatentId).list();
        for (LatentFollowUp source : records) {
            LatentFollowUp copy = new LatentFollowUp();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "latentInfectionId");
            copy.setLatentInfectionId(newLatentId);
            latentFollowUpService.save(copy);
        }
    }

    private void copyLatentChecks(Long sourceLatentId, Long newLatentId) {
        List<LatentCheck> records = latentCheckService.lambdaQuery()
                .eq(LatentCheck::getLatentInfectionId, sourceLatentId).list();
        for (LatentCheck source : records) {
            LatentCheck copy = new LatentCheck();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "latentInfectionId");
            copy.setLatentInfectionId(newLatentId);
            latentCheckService.save(copy);
        }
    }

    /** 转出同步：复制服药管理与领药记录到接收方副本 */
    private void copyLatentMedication(Long sourceLatentId, Long newLatentId, Long receiverUserId) {
        List<MedicationManagement> meds = medicationManagementService.lambdaQuery()
                .eq(MedicationManagement::getLatentInfectionId, sourceLatentId)
                .list();
        for (MedicationManagement source : meds) {
            MedicationManagement copy = new MedicationManagement();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime",
                    "patientId", "latentInfectionId");
            copy.setPatientId(null);
            copy.setLatentInfectionId(newLatentId);
            medicationManagementService.save(copy);
        }
        List<MedicationPickup> pickups = medicationPickupService.lambdaQuery()
                .eq(MedicationPickup::getLatentInfectionId, sourceLatentId)
                .orderByAsc(MedicationPickup::getPickupSeq)
                .list();
        for (MedicationPickup source : pickups) {
            MedicationPickup copy = new MedicationPickup();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime",
                    "patientId", "latentInfectionId", "filledBy");
            copy.setPatientId(null);
            copy.setLatentInfectionId(newLatentId);
            copy.setFilledBy(receiverUserId);
            medicationPickupService.save(copy);
        }
    }

    @Override
    public void assertLatentOperable(Long id) {
        dataScopeHelper.assertLatentAccessible(id);
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        assertLatentNotTransferLocked(latent);
    }

    @Override
    public void assertLatentAccessible(Long id) {
        dataScopeHelper.assertLatentAccessible(id);
    }

    @Override
    public void syncRegistrationNoFromNotice(Long latentId, String registrationNo) {
        if (latentId == null) {
            return;
        }
        LatentInfection entity = getById(latentId);
        if (entity == null) {
            return;
        }
        String value = StrUtil.trim(registrationNo);
        String normalized = StrUtil.isBlank(value) ? null : value;
        // 使用 set 显式写入，保证清空登记号时主表也能同步为 null
        lambdaUpdate()
                .eq(LatentInfection::getId, latentId)
                .set(LatentInfection::getRegistrationNo, normalized)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncContactFromNotice(Long latentId, String phone, String currentAddress, String householdAddress) {
        if (latentId == null) {
            return;
        }
        LatentInfection entity = getById(latentId);
        if (entity == null) {
            return;
        }
        lambdaUpdate()
                .eq(LatentInfection::getId, latentId)
                .set(LatentInfection::getPhone, phone)
                .set(LatentInfection::getCurrentAddress, currentAddress)
                .set(LatentInfection::getHouseholdAddress, householdAddress)
                .update();
        syncContactToLinkedScreening(entity, phone, currentAddress, householdAddress);
    }

    /** 列表展示会从筛查表回填电话/地址，修改通知单后需同步筛查来源，避免列表仍显示旧值 */
    private void syncContactToLinkedScreening(
            LatentInfection latent, String phone, String currentAddress, String householdAddress) {
        if (latent.getScreeningId() == null || StrUtil.isBlank(latent.getPopulationType())) {
            return;
        }
        switch (latent.getPopulationType()) {
            case "school" -> screeningSchoolMapper.update(null, new LambdaUpdateWrapper<ScreeningSchool>()
                    .eq(ScreeningSchool::getId, latent.getScreeningId())
                    .set(ScreeningSchool::getPhone, phone)
                    .set(ScreeningSchool::getCurrentAddress, currentAddress)
                    .set(ScreeningSchool::getHouseholdAddress, householdAddress));
            case "keyPopulation", "regular" -> screeningKeyPopulationMapper.update(null, new LambdaUpdateWrapper<ScreeningKeyPopulation>()
                    .eq(ScreeningKeyPopulation::getId, latent.getScreeningId())
                    .set(ScreeningKeyPopulation::getPhone, phone)
                    .set(ScreeningKeyPopulation::getCurrentAddress, currentAddress)
                    .set(ScreeningKeyPopulation::getHouseholdAddress, householdAddress));
            case "closeContact" -> screeningCloseContactMapper.update(null, new LambdaUpdateWrapper<ScreeningCloseContact>()
                    .eq(ScreeningCloseContact::getId, latent.getScreeningId())
                    .set(ScreeningCloseContact::getPhone, phone)
                    .set(ScreeningCloseContact::getCurrentAddress, currentAddress)
                    .set(ScreeningCloseContact::getHouseholdAddress, householdAddress));
            default -> { }
        }
    }

    private void assertLatentNotTransferLocked(LatentInfection latent) {
        if (LatentInfectionService.isTransferLocked(latent)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    ARCHIVE_REMARK_TRANSFERRED_OUT.equals(latent.getArchiveRemark())
                            ? "该记录已转出，不可操作"
                            : "该记录转出待确认，不可操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCascade(Long id) {
        dataScopeHelper.assertLatentAccessible(id);
        LatentInfection latent = getById(id);
        if (latent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染记录不存在");
        }
        assertLatentNotTransferLocked(latent);
        if ("closeContact".equals(latent.getPopulationType()) && latent.getScreeningId() != null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "密接筛查同步记录请在密接人群管理模块操作");
        }
        doDeleteCascade(id);
        log.info("级联删除潜伏感染记录 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCascade(List<Long> ids) {
        for (Long id : ids) {
            deleteCascade(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByFilter(String populationType, String name, String idNumber, Integer trackingStatus,
                              Integer archived, String referralResult, String diagnosisFirst,
                              String phone, String dateFrom, String dateTo, String dateFilterBy,
                              String creatorName, String crowdCategory, String columnFilters, String formatIssue) {
        final int pageSize = 2000;
        int pageNum = 1;
        List<Long> allIds = new ArrayList<>();
        while (true) {
            IPage<LatentInfection> page = queryPage(
                    pageNum, pageSize, populationType, name, idNumber, trackingStatus, archived,
                    referralResult, diagnosisFirst, phone, dateFrom, dateTo, dateFilterBy,
                    creatorName, crowdCategory, null, columnFilters, formatIssue);
            if (page.getRecords() == null || page.getRecords().isEmpty()) {
                break;
            }
            for (LatentInfection record : page.getRecords()) {
                if (record.getId() != null) {
                    allIds.add(record.getId());
                }
            }
            if ((long) pageNum * pageSize >= page.getTotal()) {
                break;
            }
            pageNum++;
        }
        if (allIds.isEmpty()) {
            return 0;
        }
        batchDeleteCascade(allIds);
        return allIds.size();
    }

    private void doDeleteCascade(Long latentId) {
        List<Patient> patientList = patientService.lambdaQuery()
                .eq(Patient::getLatentInfectionId, latentId)
                .list();
        for (Patient patient : patientList) {
            Long pid = patient.getId();
            firstVisitService.lambdaUpdate().eq(cn.luyou.model.FirstVisit::getPatientId, pid).remove();
            followUpVisitService.lambdaUpdate().eq(cn.luyou.model.FollowUpVisit::getPatientId, pid).remove();
            medicationManagementService.lambdaUpdate().eq(cn.luyou.model.MedicationManagement::getPatientId, pid).remove();
            medicationPickupService.lambdaUpdate().eq(cn.luyou.model.MedicationPickup::getPatientId, pid).remove();
            epidemicReportService.lambdaUpdate().eq(cn.luyou.model.EpidemicReport::getPatientId, pid).remove();
            deleteNoticeAndMessages(pid, "patient");
            deleteReferralsAndMessages(pid);
            patientService.removeById(pid);
        }
        supervisionFormService.lambdaUpdate()
                .eq(SupervisionForm::getLatentInfectionId, latentId).remove();
        medicationManagementService.lambdaUpdate()
                .eq(MedicationManagement::getLatentInfectionId, latentId).remove();
        medicationPickupService.lambdaUpdate()
                .eq(MedicationPickup::getLatentInfectionId, latentId).remove();
        latentFollowUpService.lambdaUpdate()
                .eq(cn.luyou.model.LatentFollowUp::getLatentInfectionId, latentId).remove();
        latentCheckService.lambdaUpdate()
                .eq(cn.luyou.model.LatentCheck::getLatentInfectionId, latentId).remove();
        deleteNoticeAndMessages(latentId, "latent");
        deleteReferralsAndMessages(latentId);
        removeById(latentId);
    }

    private void deleteNoticeAndMessages(Long bizId, String noticeType) {
        List<Long> noticeIds = noticeService.lambdaQuery()
                .eq(Notice::getBizId, bizId)
                .eq(Notice::getNoticeType, noticeType)
                .list().stream().map(Notice::getId).toList();
        if (!noticeIds.isEmpty()) {
            sysMessageService.lambdaUpdate().in(SysMessage::getBizId, noticeIds).remove();
        }
        noticeService.lambdaUpdate()
                .eq(Notice::getBizId, bizId)
                .eq(Notice::getNoticeType, noticeType)
                .remove();
    }

    private void deleteReferralsAndMessages(Long bizId) {
        List<Long> referralIds = referralService.lambdaQuery()
                .eq(Referral::getBizId, bizId)
                .list().stream().map(Referral::getId).toList();
        if (!referralIds.isEmpty()) {
            sysMessageService.lambdaUpdate().in(SysMessage::getBizId, referralIds).remove();
            referralService.lambdaUpdate().eq(Referral::getBizId, bizId).remove();
        }
    }

    private Integer parseIntegerField(Object val) {
        if (val == null || StrUtil.isBlank(val.toString())) return null;
        try {
            String digits = val.toString().trim().replaceAll("[^0-9]", "");
            if (StrUtil.isBlank(digits)) return null;
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isValidIdCard(String id) {
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private String getImportField(Map<Integer, String> row, Map<String, Integer> headerIndex, String... headers) {
        for (String header : headers) {
            Integer idx = headerIndex.get(header);
            if (idx == null) continue;
            String val = row.get(idx);
            if (StrUtil.isNotBlank(val)) return val.trim();
        }
        return "";
    }

    /** 兼容 Excel 数值单元格（科学计数法、末尾 .0） */
    private String normalizeExcelCellText(String val) {
        if (StrUtil.isBlank(val)) return "";
        String text = val.trim();
        if (text.matches(".*[eE].*") || text.matches("\\d+\\.0+")) {
            try {
                return new java.math.BigDecimal(text).toPlainString();
            } catch (Exception ignored) {
                return text;
            }
        }
        return text;
    }

    /** 统一数据来源分隔符（兼容 Excel 中各类横线及两侧空格） */
    private String normalizePopulationLabel(String raw) {
        if (StrUtil.isBlank(raw)) return "";
        String v = raw.trim()
                .replace('－', '-')
                .replace('—', '-')
                .replace('–', '-')
                .replace('/', '-')
                .replace('／', '-');
        // 「密接 - 家庭内」「密接- 家庭外」等 → 「密接-家庭内」
        return v.replaceAll("\\s*-\\s*", "-");
    }

    private String resolvePopulationType(String raw) {
        if (StrUtil.isBlank(raw)) return "";
        String v = normalizePopulationLabel(raw);
        if (MANUAL_POPULATION_TYPES.contains(v)) return v;
        if (v.startsWith("重点人群-")) return "keyPopulation";
        if (v.startsWith("密接-")) return "closeContact";
        return switch (v) {
            case "学生筛查" -> "school";
            case "重点人群" -> "keyPopulation";
            case "疫情筛查", "常规筛查" -> "regular";
            case "大疫情" -> "epidemic";
            case "推介" -> "referral";
            case "密接" -> "closeContact";
            case "其它", "其他" -> "other";
            default -> "";
        };
    }

    private String extractCrowdCategoryFromPopulationLabel(String raw) {
        if (StrUtil.isBlank(raw)) return "";
        String v = normalizePopulationLabel(raw);
        if (v.startsWith("重点人群-")) {
            return v.substring("重点人群-".length()).trim();
        }
        if (v.startsWith("密接-")) {
            return v.substring("密接-".length()).trim();
        }
        return "";
    }

    private void applyManualCrowdCategory(LatentInfection latent, String crowdCategoryInput) {
        if (latent == null || StrUtil.isBlank(latent.getPopulationType())) return;
        latent.setCrowdCategory(resolveManualCrowdCategory(latent.getPopulationType(), crowdCategoryInput));
    }

    private String resolveManualCrowdCategory(String populationType, Object crowdCategoryInput) {
        String crowdCategory = crowdCategoryInput == null ? "" : crowdCategoryInput.toString().trim();
        // 兼容人群分类误填「密接-家庭内」「重点人群-老年人」
        String embedded = extractCrowdCategoryFromPopulationLabel(crowdCategory);
        if (StrUtil.isNotBlank(embedded)) {
            crowdCategory = embedded;
        }
        if ("keyPopulation".equals(populationType)) {
            if (StrUtil.isBlank(crowdCategory)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择重点人群分类（老年人、糖尿病、双感）");
            }
            List<String> selected = parseCrowdCategoryParts(crowdCategory);
            if (selected.isEmpty()) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择重点人群分类（老年人、糖尿病、双感）");
            }
            for (String part : selected) {
                if (!KEY_POPULATION_SUB_CATEGORIES.contains(part)) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的重点人群分类：" + part);
                }
            }
            return KEY_POPULATION_SUB_CATEGORIES.stream()
                    .filter(selected::contains)
                    .collect(Collectors.joining("、"));
        }
        if ("closeContact".equals(populationType)) {
            if (!CLOSE_CONTACT_TYPES.contains(crowdCategory)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择密接类型（家庭内/家庭外）；数据来源也可直接填「密接-家庭内」或「密接-家庭外」");
            }
            return crowdCategory;
        }
        return StrUtil.isBlank(crowdCategory) ? null : crowdCategory;
    }

    private List<String> parseCrowdCategoryParts(String crowdCategory) {
        return Arrays.stream(crowdCategory.split("[、,，/]"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private Map<String, Integer> buildImportHeaderIndex(Map<Integer, String> headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isBlank(entry.getValue())) continue;
            String header = entry.getValue().trim();
            headerIndex.put(header, entry.getKey());
            String alias = LatentImportHeaders.HEADER_ALIASES.get(header);
            if (alias != null) {
                headerIndex.putIfAbsent(alias, entry.getKey());
            }
        }
        for (Map.Entry<String, String> alias : LatentImportHeaders.HEADER_ALIASES.entrySet()) {
            Integer idx = headerIndex.get(alias.getKey());
            if (idx != null) {
                headerIndex.putIfAbsent(alias.getValue(), idx);
            }
        }
        return headerIndex;
    }

    @Override
    public List<String> listDistinctColumnValues(String field, String populationType, Integer archived, String referralResult) {
        if ("medicationManagementUnit".equals(field)) {
            return listDistinctMedicationManagementUnits(populationType, archived, referralResult);
        }
        if (StrUtil.isBlank(field) || !COLUMN_DISTINCT_FIELDS.contains(field)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
        // 感染筛查方法为固定枚举（也可存自定义值，筛选项仍给标准三项）
        if ("screenMethod".equals(field)) {
            return List.of("PPD", "EC", "IGRA");
        }
        int resolvedArchived = archived != null ? archived : 0;
        LambdaQueryWrapper<LatentInfection> wrapper = buildDistinctScopeWrapper(populationType, resolvedArchived, referralResult);
        // 录入用户非表字段：按 creator_id 去重后再解析展示名
        if ("creatorUsername".equals(field)) {
            wrapper.select(LatentInfection::getCreatorId)
                    .isNotNull(LatentInfection::getCreatorId)
                    .groupBy(LatentInfection::getCreatorId);
            List<Long> creatorIds = list(wrapper).stream()
                    .map(LatentInfection::getCreatorId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (creatorIds.isEmpty()) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            for (User u : userMapper.selectBatchIds(creatorIds)) {
                if (u == null) {
                    continue;
                }
                String display = StrUtil.blankToDefault(u.getRealName(), u.getUsername());
                if (StrUtil.isNotBlank(display)) {
                    names.add(display.trim());
                }
            }
            return ColumnDistinctSupport.normalize(names);
        }
        applyDistinctSelect(wrapper, field);
        return ColumnDistinctSupport.normalize(list(wrapper).stream()
                .map(row -> extractDistinctValue(row, field))
                .toList());
    }

    /** 服药管理单位去重：通知单 + 督导表管理单位 */
    private List<String> listDistinctMedicationManagementUnits(String populationType, Integer archived, String referralResult) {
        int resolvedArchived = archived != null ? archived : 0;
        LambdaQueryWrapper<LatentInfection> scope = buildDistinctScopeWrapper(populationType, resolvedArchived, referralResult);
        scope.select(LatentInfection::getId);
        List<Long> latentIds = list(scope).stream()
                .map(LatentInfection::getId)
                .filter(Objects::nonNull)
                .toList();
        if (latentIds.isEmpty()) {
            return List.of();
        }
        Set<String> units = new TreeSet<>();
        noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getNoticeType, "latent")
                        .in(Notice::getBizId, latentIds)
                        .isNotNull(Notice::getMedicationManagementUnit)
                        .ne(Notice::getMedicationManagementUnit, "")
                        .select(Notice::getMedicationManagementUnit))
                .forEach(n -> {
                    if (StrUtil.isNotBlank(n.getMedicationManagementUnit())) {
                        units.add(n.getMedicationManagementUnit().trim());
                    }
                });
        supervisionFormMapper.selectList(new LambdaQueryWrapper<SupervisionForm>()
                        .in(SupervisionForm::getLatentInfectionId, latentIds)
                        .isNotNull(SupervisionForm::getManagingUnit)
                        .ne(SupervisionForm::getManagingUnit, "")
                        .select(SupervisionForm::getManagingUnit))
                .forEach(f -> {
                    if (StrUtil.isNotBlank(f.getManagingUnit())) {
                        units.add(f.getManagingUnit().trim());
                    }
                });
        return new ArrayList<>(units);
    }

    /**
     * 表头录入用户筛选：支持多选逗号分隔。
     * 每个选项优先按姓名/账号精确匹配（与列表展示名一致），无精确结果时再模糊。
     */
    private List<Long> resolveCreatorIdsByFilterValue(String value) {
        Collection<String> names = ColumnFilterSupport.splitValues(value);
        if (names.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String name : names) {
            List<User> exactUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .and(w -> w.eq(User::getRealName, name).or().eq(User::getUsername, name)));
            if (exactUsers != null && !exactUsers.isEmpty()) {
                exactUsers.stream()
                        .map(User::getId)
                        .filter(Objects::nonNull)
                        .forEach(ids::add);
                continue;
            }
            ids.addAll(CreatorUserSupport.resolveUserIdsByKeyword(userMapper, name));
        }
        return new ArrayList<>(ids);
    }

    /** distinct 查询的基础权限与列表范围（与 queryPage 在管列表一致） */
    private LambdaQueryWrapper<LatentInfection> buildDistinctScopeWrapper(String populationType, int archived, String referralResult) {
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(populationType)) {
            wrapper.eq(LatentInfection::getPopulationType, populationType);
        } else {
            wrapper.and(w -> w.ne(LatentInfection::getPopulationType, "closeContact")
                    .or()
                    .isNull(LatentInfection::getScreeningId));
        }
        wrapper.eq(LatentInfection::getArchived, archived);
        if (archived == 0) {
            wrapper.and(w -> w.isNull(LatentInfection::getArchiveRemark)
                    .or()
                    .ne(LatentInfection::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT));
        }
        wrapper.and(w -> w.isNull(LatentInfection::getDiagnosisResult)
                .or()
                .notIn(LatentInfection::getDiagnosisResult, "确诊患者", "确诊结核", "在治患者"));
        if ("pending".equals(referralResult)) {
            wrapper.isNull(LatentInfection::getReferralResult);
        } else if (StrUtil.isNotBlank(referralResult)) {
            wrapper.eq(LatentInfection::getReferralResult, referralResult);
        }
        LatentScreeningLinkSupport.applyLinkedScreeningExistsFilter(wrapper);
        dataScopeHelper.applyLatentScope(wrapper);
        return wrapper;
    }

    private void applyDistinctSelect(LambdaQueryWrapper<LatentInfection> wrapper, String field) {
        switch (field) {
            case "gender" -> wrapper.select(LatentInfection::getGender)
                    .isNotNull(LatentInfection::getGender).ne(LatentInfection::getGender, "")
                    .groupBy(LatentInfection::getGender);
            case "populationType" -> wrapper.select(LatentInfection::getPopulationType)
                    .isNotNull(LatentInfection::getPopulationType).ne(LatentInfection::getPopulationType, "")
                    .groupBy(LatentInfection::getPopulationType);
            case "infectionResult" -> wrapper.select(LatentInfection::getInfectionResult)
                    .isNotNull(LatentInfection::getInfectionResult).ne(LatentInfection::getInfectionResult, "")
                    .groupBy(LatentInfection::getInfectionResult);
            case "diagnosisFirst" -> wrapper.select(LatentInfection::getDiagnosisFirst)
                    .isNotNull(LatentInfection::getDiagnosisFirst).ne(LatentInfection::getDiagnosisFirst, "")
                    .groupBy(LatentInfection::getDiagnosisFirst);
            case "diagnosisResult" -> wrapper.select(LatentInfection::getDiagnosisResult)
                    .isNotNull(LatentInfection::getDiagnosisResult).ne(LatentInfection::getDiagnosisResult, "")
                    .groupBy(LatentInfection::getDiagnosisResult);
            case "hasChestXray" -> wrapper.select(LatentInfection::getHasChestXray)
                    .isNotNull(LatentInfection::getHasChestXray).ne(LatentInfection::getHasChestXray, "")
                    .groupBy(LatentInfection::getHasChestXray);
            case "chestXrayResult" -> wrapper.select(LatentInfection::getChestXrayResult)
                    .isNotNull(LatentInfection::getChestXrayResult).ne(LatentInfection::getChestXrayResult, "")
                    .groupBy(LatentInfection::getChestXrayResult);
            case "crowdCategory" -> wrapper.select(LatentInfection::getCrowdCategory)
                    .isNotNull(LatentInfection::getCrowdCategory).ne(LatentInfection::getCrowdCategory, "")
                    .groupBy(LatentInfection::getCrowdCategory);
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
    }

    private String extractDistinctValue(LatentInfection row, String field) {
        return switch (field) {
            case "gender" -> row.getGender();
            case "populationType" -> row.getPopulationType();
            case "infectionResult" -> {
                String raw = row.getInfectionResult();
                String official = InfectionScreenFieldSupport.normalizeResult(raw);
                yield official != null ? official : raw;
            }
            case "diagnosisFirst" -> row.getDiagnosisFirst();
            case "diagnosisResult" -> row.getDiagnosisResult();
            case "hasChestXray" -> row.getHasChestXray();
            case "chestXrayResult" -> row.getChestXrayResult();
            case "crowdCategory" -> row.getCrowdCategory();
            default -> null;
        };
    }
}
