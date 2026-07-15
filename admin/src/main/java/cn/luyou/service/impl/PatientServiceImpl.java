package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.constant.PatientImportHeaders;
import cn.luyou.constant.PatientManualImportHeaders;
import cn.luyou.model.EpidemicReport;
import cn.luyou.model.FirstVisit;
import cn.luyou.model.FollowUpVisit;
import cn.luyou.model.ImportResult;
import cn.luyou.model.MedicationManagement;
import cn.luyou.model.MedicationPickup;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.Department;
import cn.luyou.model.vo.PatientDistributionHeatmapVO;
import cn.luyou.mapper.FirstVisitMapper;
import cn.luyou.mapper.FollowUpVisitMapper;
import cn.luyou.mapper.MedicationManagementMapper;
import cn.luyou.mapper.MedicationPickupMapper;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.mapper.PatientMapper;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.DepartmentFilterSupport;
import cn.luyou.utils.ColumnFilterSupport;
import cn.luyou.utils.CreatorUserSupport;
import cn.luyou.utils.ImportDuplicateIdSupport;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.KeyPopulationCrowdCategoryQuerySupport;
import cn.luyou.utils.FlexibleDateParseUtil;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.PatientAddressRegionParser;
import cn.luyou.utils.StatYearPeriod;
import cn.luyou.utils.ZigongTownshipCatalog;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient>
        implements PatientService {

    private static final Set<String> MANUAL_POPULATION_TYPES = Set.of(
            "school", "keyPopulation", "regular", "epidemic", "referral", "closeContact", "specialDisease"
    );

    /** 专病表多列命中时，按业务优先级取一项作为通知单人群分类 */
    private static final List<String> CROWD_CATEGORY_PRIORITY = List.of(
            "密接", "学生", "教职工", "老年人", "糖尿病", "双感", "既往结核", "非重点人群"
    );

    private static final String EPIDEMIC_JSON_REGISTRATION_DATE = "$.\"登记日期\"";
    private static final String EPIDEMIC_JSON_PATHOGEN_RESULT = "$.\"病原学结果\"";
    private static final String EPIDEMIC_JSON_DIAGNOSIS_RESULT = "$.\"诊断结果\"";
    private static final String[] PATHOGEN_RESULT_POSITIVE_VALUES = {
            "阳性", "病原学阳性", "病原学结果阳性"
    };
    private static final String EPIDEMIC_JSON_MEDICATION_UNIT = "$.\"服药管理单位\"";
    /** 后续随访中停止治疗原因为「完成疗程」的患者（去重） */
    private static final String TREATMENT_SUCCESS_FOLLOW_UP_SQL =
            "SELECT DISTINCT patient_id FROM follow_up_visit WHERE deleted = 0 "
                    + "AND stop_treatment = '是' AND stop_treatment_reason = '完成疗程' AND patient_id IS NOT NULL";
    /** 取前 10 位以兼容 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss */
    private static final String REGISTRATION_DATE_SQL_EXPR =
            "STR_TO_DATE(LEFT(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(epidemic_data, '"
                    + EPIDEMIC_JSON_REGISTRATION_DATE + "')), '/', '-'), 10), '%Y-%m-%d')";

    /** 手动新增/导入：前端 camelCase 字段 → epidemicData 中文键 */
    private static final List<String[]> MANUAL_EPIDEMIC_MAPPINGS = List.of(
            new String[]{"crowdCategory", "人群分类"},
            new String[]{"currentManagementUnit", "现管单位"},
            new String[]{"registrationNo", "登记号"},
            new String[]{"contactName", "联系人姓名"},
            new String[]{"contactRelation", "联系人监护人与本人关系"},
            new String[]{"contactGuardianPhone", "联系人监护人电话号码"},
            new String[]{"comorbidity", "合并症"},
            new String[]{"treatmentClass", "治疗分类"},
            new String[]{"medicationManagementUnit", "服药管理单位"},
            new String[]{"patientRemark", "备注"},
            new String[]{"firstTreatmentPlan", "首次治疗方案"},
            new String[]{"drugSensitivityR", "药敏结果：利福平（R）"},
            new String[]{"drugSensitivityH", "药敏结果：异烟肼（H）"},
            new String[]{"cultureResult", "培养结果"}
    );

    private final DataScopeHelper dataScopeHelper;
    private final EpidemicReportService epidemicReportService;
    private final ObjectMapper objectMapper;
    private final NoticeMapper noticeMapper;
    private final FirstVisitMapper firstVisitMapper;
    private final FollowUpVisitMapper followUpVisitMapper;
    private final MedicationManagementMapper medicationManagementMapper;
    private final MedicationPickupMapper medicationPickupMapper;
    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ScreeningCloseContactMapper screeningCloseContactMapper;
    private final UserMapper userMapper;
    private final ReferralService referralService;
    private final DepartmentService departmentService;
    private final DepartmentFilterSupport departmentFilterSupport;

    private static final Set<String> COLUMN_FILTER_WHITELIST = Set.of(
            "name", "gender", "idNumber", "phone", "currentAddress", "householdAddress",
            "diagnosisResult", "populationType", "ethnicity", "idType", "creatorUsername", "source"
    );

    public PatientServiceImpl(
            DataScopeHelper dataScopeHelper,
            EpidemicReportService epidemicReportService,
            ObjectMapper objectMapper,
            NoticeMapper noticeMapper,
            FirstVisitMapper firstVisitMapper,
            FollowUpVisitMapper followUpVisitMapper,
            MedicationManagementMapper medicationManagementMapper,
            MedicationPickupMapper medicationPickupMapper,
            ScreeningSchoolMapper screeningSchoolMapper,
            ScreeningKeyPopulationMapper screeningKeyPopulationMapper,
            ScreeningCloseContactMapper screeningCloseContactMapper,
            UserMapper userMapper,
            @Lazy ReferralService referralService,
            DepartmentService departmentService,
            DepartmentFilterSupport departmentFilterSupport) {
        this.dataScopeHelper = dataScopeHelper;
        this.epidemicReportService = epidemicReportService;
        this.objectMapper = objectMapper;
        this.noticeMapper = noticeMapper;
        this.firstVisitMapper = firstVisitMapper;
        this.followUpVisitMapper = followUpVisitMapper;
        this.medicationManagementMapper = medicationManagementMapper;
        this.medicationPickupMapper = medicationPickupMapper;
        this.screeningSchoolMapper = screeningSchoolMapper;
        this.screeningKeyPopulationMapper = screeningKeyPopulationMapper;
        this.screeningCloseContactMapper = screeningCloseContactMapper;
        this.userMapper = userMapper;
        this.referralService = referralService;
        this.departmentService = departmentService;
        this.departmentFilterSupport = departmentFilterSupport;
    }

    @Override
    public IPage<Patient> queryPage(int page, int size, String populationType,
                                     String name, String idNumber, String phone, String currentAddress,
                                     String diagnosisResult, Integer archived, String dateFrom, String dateTo,
                                     String dateFilterBy, String medicationManagementUnit, String crowdCategory,
                                     String creatorUsername, String columnFilters) {
        LambdaQueryWrapper<Patient> wrapper = buildPatientQueryWrapper(
                populationType, name, idNumber, phone, currentAddress, diagnosisResult, archived,
                dateFrom, dateTo, null, null, dateFilterBy, medicationManagementUnit, crowdCategory);
        applyCreatorUsernameFilter(wrapper, creatorUsername);
        applyColumnFilters(wrapper, columnFilters);
        ImportRowOrderSupport.applyWithoutBatch(wrapper);
        IPage<Patient> result = page(new Page<>(page, size), wrapper);
        fillCreatorUsernames(result.getRecords());
        fillNoticeStatus(result.getRecords(), populationType);
        fillFirstVisitStatus(result.getRecords());
        fillFollowUpCount(result.getRecords());
        fillMedicationManagementStatus(result.getRecords());
        fillMedicationPickupSummary(result.getRecords());
        fillScreeningXrayData(result.getRecords(), populationType);
        fillEpidemicExtraFields(result.getRecords());
        return result;
    }

    @Override
    public List<Patient> listForExport(String populationType, String name, String idNumber,
                                        String phone, String currentAddress, String diagnosisResult,
                                        Integer archived, String dateFrom, String dateTo,
                                        String startTime, String endTime,
                                        String dateFilterBy, String medicationManagementUnit,
                                        String crowdCategory) {
        LambdaQueryWrapper<Patient> wrapper = buildPatientQueryWrapper(
                populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                archived, dateFrom, dateTo, startTime, endTime, dateFilterBy, medicationManagementUnit, crowdCategory);
        if (Integer.valueOf(1).equals(archived)) {
            wrapper.orderByAsc(Patient::getPopulationType).orderByDesc(Patient::getArchivedTime);
        } else {
            ImportRowOrderSupport.applyWithoutBatch(wrapper);
        }
        List<Patient> patients = list(wrapper);
        fillEpidemicExtraFields(patients);
        return patients;
    }


    private void applyCreatorUsernameFilter(LambdaQueryWrapper<Patient> wrapper, String creatorUsername) {
        if (StrUtil.isBlank(creatorUsername)) {
            return;
        }
        List<Long> ids = CreatorUserSupport.resolveUserIdsByKeyword(userMapper, creatorUsername);
        if (ids.isEmpty()) {
            return;
        }
        wrapper.in(Patient::getCreatorId, ids);
    }

    private void applyColumnFilters(LambdaQueryWrapper<Patient> wrapper, String columnFilters) {
        Map<String, String> filters = ColumnFilterSupport.parse(columnFilters);
        ColumnFilterSupport.applyLambda(filters, COLUMN_FILTER_WHITELIST, (field, value) -> {
            switch (field) {
                case "name" -> ColumnFilterSupport.like(wrapper, Patient::getName, value);
                case "gender" -> ColumnFilterSupport.eqOrIn(wrapper, Patient::getGender, value);
                case "idNumber" -> ColumnFilterSupport.like(wrapper, Patient::getIdNumber, value);
                case "phone" -> ColumnFilterSupport.like(wrapper, Patient::getPhone, value);
                case "currentAddress" -> ColumnFilterSupport.like(wrapper, Patient::getCurrentAddress, value);
                case "householdAddress" -> ColumnFilterSupport.like(wrapper, Patient::getHouseholdAddress, value);
                case "diagnosisResult" -> ColumnFilterSupport.eqOrIn(wrapper, Patient::getDiagnosisResult, value);
                case "populationType" -> ColumnFilterSupport.eqOrIn(wrapper, Patient::getPopulationType, value);
                case "ethnicity" -> ColumnFilterSupport.eqOrIn(wrapper, Patient::getEthnicity, value);
                case "idType" -> ColumnFilterSupport.eqOrIn(wrapper, Patient::getIdType, value);
                case "source" -> ColumnFilterSupport.eqOrIn(wrapper, Patient::getSource, value);
                case "creatorUsername" -> applyCreatorUsernameFilter(wrapper, value);
                default -> { }
            }
        });
    }

    private void fillCreatorUsernames(List<Patient> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream()
                .map(Patient::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (User u : userMapper.selectBatchIds(ids)) {
            if (u != null) {
                nameMap.put(u.getId(), StrUtil.blankToDefault(u.getRealName(), u.getUsername()));
            }
        }
        for (Patient p : records) {
            if (p.getCreatorId() != null) {
                p.setCreatorUsername(nameMap.get(p.getCreatorId()));
            }
        }
    }

    private LambdaQueryWrapper<Patient> buildPatientQueryWrapper(String populationType, String name,
                                                                  String idNumber, String phone,
                                                                  String currentAddress, String diagnosisResult,
                                                                  Integer archived,
                                                                  String dateFrom, String dateTo,
                                                                  String startTime, String endTime,
                                                                  String dateFilterBy,
                                                                  String medicationManagementUnit,
                                                                  String crowdCategory) {
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(dateTo);
        boolean registrationDateFilter = "registrationDate".equals(dateFilterBy);
        boolean noticeFillFilter = "noticeFill".equals(dateFilterBy);
        boolean firstVisitFillFilter = "firstVisitFill".equals(dateFilterBy);
        boolean followUpFillFilter = "followUpFill".equals(dateFilterBy);
        boolean hasRegistrationDateRange = registrationDateFilter
                && (createFrom != null || createTo != null);
        boolean hasNoticeFillDateRange = noticeFillFilter
                && (createFrom != null || createTo != null);
        boolean hasFirstVisitFillDateRange = firstVisitFillFilter
                && (createFrom != null || createTo != null);
        boolean hasFollowUpFillDateRange = followUpFillFilter
                && (createFrom != null || createTo != null);
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), Patient::getPhone, phone)
                .like(StrUtil.isNotBlank(currentAddress), Patient::getCurrentAddress, currentAddress)
                .eq(archived != null, Patient::getArchived, archived);
        // 在管列表排除「已转出」（兼容历史误留在 archived=0 的数据）
        if (archived == null || Integer.valueOf(0).equals(archived)) {
            wrapper.and(w -> w.isNull(Patient::getArchiveRemark)
                    .or()
                    .ne(Patient::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT));
        }
        applyDiagnosisResultFilter(wrapper, diagnosisResult);
        if (hasFirstVisitFillDateRange) {
            applyPatientIdFilter(wrapper,
                    resolvePatientFirstVisitDateBizIds(populationType, createFrom, createTo));
        } else if (hasFollowUpFillDateRange) {
            applyPatientIdFilter(wrapper,
                    resolvePatientFollowUpDateBizIds(populationType, createFrom, createTo));
        } else if (hasNoticeFillDateRange) {
            applyPatientIdFilter(wrapper,
                    resolvePatientNoticeDateBizIds(populationType, createFrom, createTo));
        } else if (Integer.valueOf(1).equals(archived)
                && (StrUtil.isNotBlank(startTime) || StrUtil.isNotBlank(endTime))) {
            wrapper.ge(StrUtil.isNotBlank(startTime), Patient::getArchivedTime, startTime)
                    .le(StrUtil.isNotBlank(endTime), Patient::getArchivedTime, endTime + " 23:59:59");
        } else if (hasRegistrationDateRange) {
            applyRegistrationDateRange(wrapper, createFrom, createTo);
        } else if (!registrationDateFilter && !noticeFillFilter
                && !firstVisitFillFilter && !followUpFillFilter) {
            wrapper.ge(createFrom != null, Patient::getCreateTime, createFrom)
                    .le(createTo != null, Patient::getCreateTime, createTo);
        }
        applyMedicationManagementUnitFilter(wrapper, populationType, medicationManagementUnit);
        KeyPopulationCrowdCategoryQuerySupport.applyPatientFilter(
                wrapper, populationType, crowdCategory, screeningKeyPopulationMapper);
        applyPatientScopeFilter(wrapper);
        return wrapper;
    }

    /** 按通知单首次填写时间（notice.create_time）筛选 */
    private Set<Long> resolvePatientNoticeDateBizIds(String populationType,
                                                     LocalDateTime noticeFrom, LocalDateTime noticeTo) {
        LambdaQueryWrapper<Notice> noticeWrapper = new LambdaQueryWrapper<>();
        noticeWrapper.eq(Notice::getNoticeType, "patient")
                .eq(StrUtil.isNotBlank(populationType), Notice::getPopulationType, populationType)
                .ge(noticeFrom != null, Notice::getCreateTime, noticeFrom)
                .le(noticeTo != null, Notice::getCreateTime, noticeTo);
        return noticeMapper.selectList(noticeWrapper.select(Notice::getBizId)).stream()
                .map(Notice::getBizId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 按首次随访填写时间（first_visit.create_time）筛选 */
    private Set<Long> resolvePatientFirstVisitDateBizIds(String populationType,
                                                         LocalDateTime visitFrom, LocalDateTime visitTo) {
        LambdaQueryWrapper<FirstVisit> visitWrapper = new LambdaQueryWrapper<>();
        visitWrapper.eq(StrUtil.isNotBlank(populationType), FirstVisit::getPopulationType, populationType)
                .ge(visitFrom != null, FirstVisit::getCreateTime, visitFrom)
                .le(visitTo != null, FirstVisit::getCreateTime, visitTo);
        return firstVisitMapper.selectList(visitWrapper.select(FirstVisit::getPatientId)).stream()
                .map(FirstVisit::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 按后续随访填写时间（follow_up_visit.create_time）筛选 */
    private Set<Long> resolvePatientFollowUpDateBizIds(String populationType,
                                                       LocalDateTime visitFrom, LocalDateTime visitTo) {
        LambdaQueryWrapper<FollowUpVisit> visitWrapper = new LambdaQueryWrapper<>();
        visitWrapper.eq(StrUtil.isNotBlank(populationType), FollowUpVisit::getPopulationType, populationType)
                .ge(visitFrom != null, FollowUpVisit::getCreateTime, visitFrom)
                .le(visitTo != null, FollowUpVisit::getCreateTime, visitTo);
        return followUpVisitMapper.selectList(visitWrapper.select(FollowUpVisit::getPatientId)).stream()
                .map(FollowUpVisit::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void applyPatientIdFilter(LambdaQueryWrapper<Patient> wrapper, Set<Long> patientIds) {
        if (patientIds.isEmpty()) {
            wrapper.eq(Patient::getId, -1L);
        } else {
            wrapper.in(Patient::getId, patientIds);
        }
    }

    /** 按病案/导入信息中的登记日期（epidemic_data.登记日期）筛选 */
    private void applyRegistrationDateRange(LambdaQueryWrapper<Patient> wrapper,
                                          LocalDateTime from, LocalDateTime to) {
        wrapper.isNotNull(Patient::getEpidemicData)
                .apply(REGISTRATION_DATE_SQL_EXPR + " IS NOT NULL");
        if (from != null) {
            wrapper.apply(REGISTRATION_DATE_SQL_EXPR + " >= {0}", from.toLocalDate());
        }
        if (to != null) {
            wrapper.apply(REGISTRATION_DATE_SQL_EXPR + " <= {0}", to.toLocalDate());
        }
    }

    /** 服药管理单位：病案 JSON 或患者通知单 */
    private void applyMedicationManagementUnitFilter(LambdaQueryWrapper<Patient> wrapper,
                                                     String populationType,
                                                     String medicationManagementUnit) {
        if (StrUtil.isBlank(medicationManagementUnit)) {
            return;
        }
        String like = "%" + medicationManagementUnit.trim() + "%";
        LambdaQueryWrapper<Notice> noticeWrapper = new LambdaQueryWrapper<>();
        noticeWrapper.eq(Notice::getNoticeType, "patient")
                .like(Notice::getMedicationManagementUnit, medicationManagementUnit.trim())
                .eq(StrUtil.isNotBlank(populationType), Notice::getPopulationType, populationType)
                .select(Notice::getBizId);
        List<Long> noticePatientIds = noticeMapper.selectList(noticeWrapper).stream()
                .map(Notice::getBizId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        wrapper.and(w -> {
            w.apply("JSON_UNQUOTE(JSON_EXTRACT(epidemic_data, '" + EPIDEMIC_JSON_MEDICATION_UNIT + "')) LIKE {0}", like);
            if (!noticePatientIds.isEmpty()) {
                w.or().in(Patient::getId, noticePatientIds);
            }
        });
    }

    /** 与列表查询保持一致的数据权限过滤 */
    private void applyPatientScopeFilter(LambdaQueryWrapper<Patient> wrapper) {
        dataScopeHelper.applyPatientScope(wrapper);
    }

    @Override
    public long countManagedPatientsForDashboard(Integer statYear, List<Long> filterDeptIds) {
        return count(buildManagedPatientDashboardWrapper(statYear, filterDeptIds));
    }

    @Override
    public long countPathogenPositivePatientsForDashboard(Integer statYear, List<Long> filterDeptIds) {
        LambdaQueryWrapper<Patient> wrapper = buildManagedPatientDashboardWrapper(statYear, filterDeptIds);
        applyPathogenResultPositiveFilter(wrapper);
        return count(wrapper);
    }

    @Override
    public long countTreatmentSuccessForDashboard(Integer statYear, List<Long> filterDeptIds) {
        // 分母：statYear 年度管理患者；分子：其中任意时间完成疗程者（可跨年，不按 stop_treatment_date 限年度）
        LambdaQueryWrapper<Patient> wrapper = buildManagedPatientDashboardWrapper(statYear, filterDeptIds);
        wrapper.inSql(Patient::getId, TREATMENT_SUCCESS_FOLLOW_UP_SQL);
        return count(wrapper);
    }

    private static final List<String> ZIGONG_DISTRICT_NAMES = List.of(
            "自流井区", "贡井区", "大安区", "沿滩区", "荣县", "富顺县"
    );

    private static final Map<String, String> ZIGONG_DISTRICT_ADCODE = Map.of(
            "自流井区", "510302",
            "贡井区", "510303",
            "大安区", "510304",
            "沿滩区", "510311",
            "荣县", "510321",
            "富顺县", "510322"
    );

    @Override
    public PatientDistributionHeatmapVO buildPatientDistributionHeatmap(Integer statYear, String districtName,
                                                                        List<Long> filterDeptIds) {
        assertHeatmapRoleAllowed();
        int year = statYear != null ? statYear : StatYearPeriod.current().statYear();
        StatYearPeriod period = StatYearPeriod.of(year);

        Map<String, Map<String, Long>> matrix = buildHeatmapDistrictMatrix(year, filterDeptIds);
        long total = matrix.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToLong(Long::longValue)
                .sum();

        if (StrUtil.isBlank(districtName)) {
            return buildCityLevelHeatmap(year, period, matrix, total);
        }
        String resolvedDistrict = resolveDistrictKey(matrix, districtName);
        if (resolvedDistrict == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "未找到区县：" + districtName);
        }
        return buildDistrictLevelHeatmap(year, period, resolvedDistrict, matrix, total);
    }

    private static final String HEATMAP_UNASSIGNED_DISTRICT = "未分配";
    private static final String HEATMAP_UNPARSED_TOWNSHIP = "未识别乡镇";

    private Map<String, Map<String, Long>> buildHeatmapDistrictMatrix(int year, List<Long> filterDeptIds) {
        LambdaQueryWrapper<Patient> wrapper = buildManagedPatientDashboardWrapper(year, filterDeptIds);
        wrapper.select(Patient::getCurrentAddress, Patient::getEpidemicData);
        List<Patient> patients = list(wrapper);

        Map<String, Map<String, Long>> matrix = new LinkedHashMap<>();
        seedHeatmapMatrixFromTownshipCatalog(matrix);

        for (Patient patient : patients) {
            String address = resolvePatientHeatmapAddress(patient);
            PatientAddressRegionParser.ParsedRegion region = PatientAddressRegionParser.parse(address);
            String district = canonicalHeatmapDistrict(region.county());
            String township = canonicalHeatmapTownship(district, region.township());

            matrix.computeIfAbsent(district, k -> new LinkedHashMap<>())
                    .merge(township, 1L, Long::sum);
        }
        return matrix;
    }

    /** 热力图现住址：优先主表现住址，其次 epidemic_data 中的现住址字段 */
    private String resolvePatientHeatmapAddress(Patient patient) {
        if (patient == null) {
            return "";
        }
        if (StrUtil.isNotBlank(patient.getCurrentAddress())) {
            return normalizeSpecialDiseaseCurrentAddress(patient.getCurrentAddress().trim());
        }
        if (StrUtil.isBlank(patient.getEpidemicData())) {
            return "";
        }
        try {
            JSONObject json = JSONUtil.parseObj(patient.getEpidemicData());
            for (String key : List.of("现住址", "现地址", "现地址详细", "现详细住址", "现住详细地址", "现住地址")) {
                String value = json.getStr(key);
                if (StrUtil.isNotBlank(value)) {
                    return normalizeSpecialDiseaseCurrentAddress(value.trim());
                }
            }
        } catch (Exception ignored) {
            // 忽略非法 JSON
        }
        return "";
    }

    private String canonicalHeatmapDistrict(String parsedCounty) {
        if (StrUtil.isBlank(parsedCounty)) {
            return HEATMAP_UNASSIGNED_DISTRICT;
        }
        for (String canonical : ZIGONG_DISTRICT_NAMES) {
            if (districtNamesMatch(canonical, parsedCounty)) {
                return canonical;
            }
        }
        return HEATMAP_UNASSIGNED_DISTRICT;
    }

    private String canonicalHeatmapTownship(String district, String parsedTownship) {
        if (HEATMAP_UNASSIGNED_DISTRICT.equals(district)) {
            return "—";
        }
        if (StrUtil.isBlank(parsedTownship)) {
            return HEATMAP_UNPARSED_TOWNSHIP;
        }
        for (String township : ZigongTownshipCatalog.getTownships(district)) {
            if (townshipNamesMatch(township, parsedTownship)) {
                return township;
            }
        }
        return parsedTownship.trim();
    }

    private boolean townshipNamesMatch(String a, String b) {
        if (StrUtil.isBlank(a) || StrUtil.isBlank(b)) {
            return false;
        }
        String na = normalizeTownshipLabel(a);
        String nb = normalizeTownshipLabel(b);
        if (na.equals(nb)) {
            return true;
        }
        return na.contains(nb) || nb.contains(na);
    }

    private String normalizeTownshipLabel(String name) {
        return name.trim()
                .replace("街道办事处", "街道")
                .replace("民族乡", "乡");
    }

    /** 预置自贡各区县乡镇格子（无患者时地图仍展示完整边界） */
    private void seedHeatmapMatrixFromTownshipCatalog(Map<String, Map<String, Long>> matrix) {
        for (String district : ZIGONG_DISTRICT_NAMES) {
            Map<String, Long> communities = matrix.computeIfAbsent(district, k -> new LinkedHashMap<>());
            for (String township : ZigongTownshipCatalog.getTownships(district)) {
                communities.putIfAbsent(township, 0L);
            }
        }
    }

    /** 下钻区县时补全乡镇格子 */
    private void enrichDistrictCommunitiesFromCatalog(String districtName, Map<String, Long> communities) {
        String canonical = toCanonicalDistrictName(districtName);
        for (String township : ZigongTownshipCatalog.getTownships(canonical)) {
            communities.putIfAbsent(township, 0L);
        }
    }

    private PatientDistributionHeatmapVO buildCityLevelHeatmap(int year,
                                                             StatYearPeriod period,
                                                             Map<String, Map<String, Long>> matrix,
                                                             long total) {
        List<PatientDistributionHeatmapVO.MapRegion> regions = new ArrayList<>();
        int maxCount = 0;
        for (String district : ZIGONG_DISTRICT_NAMES) {
            String key = resolveDistrictKey(matrix, district);
            int value = key != null ? sumDistrictCount(matrix.get(key)) : 0;
            maxCount = Math.max(maxCount, value);
            regions.add(PatientDistributionHeatmapVO.MapRegion.builder()
                    .name(district)
                    .adcode(ZIGONG_DISTRICT_ADCODE.get(district))
                    .value(value)
                    .build());
        }
        if (matrix.containsKey("未分配")) {
            int unassigned = sumDistrictCount(matrix.get("未分配"));
            if (unassigned > 0) {
                maxCount = Math.max(maxCount, unassigned);
                regions.add(PatientDistributionHeatmapVO.MapRegion.builder()
                        .name("未分配")
                        .value(unassigned)
                        .build());
            }
        }
        return PatientDistributionHeatmapVO.builder()
                .managementYear(year)
                .statPeriodFrom(period.start().toString())
                .statPeriodTo(period.end().toString())
                .total((int) Math.min(total, Integer.MAX_VALUE))
                .maxCount(maxCount)
                .mapLevel("city")
                .regions(regions)
                .build();
    }

    private PatientDistributionHeatmapVO buildDistrictLevelHeatmap(int year,
                                                                     StatYearPeriod period,
                                                                     String districtName,
                                                                     Map<String, Map<String, Long>> matrix,
                                                                     long total) {
        Map<String, Long> communities = new LinkedHashMap<>(matrix.getOrDefault(districtName, Map.of()));
        enrichDistrictCommunitiesFromCatalog(districtName, communities);
        List<String> labels = sortCommunityLabels(communities.keySet());
        List<PatientDistributionHeatmapVO.MapRegion> regions = new ArrayList<>();
        int maxCount = 0;
        int districtTotal = 0;
        for (String label : labels) {
            int value = communities.getOrDefault(label, 0L).intValue();
            districtTotal += value;
            maxCount = Math.max(maxCount, value);
            regions.add(PatientDistributionHeatmapVO.MapRegion.builder()
                    .name(label)
                    .value(value)
                    .build());
        }
        return PatientDistributionHeatmapVO.builder()
                .managementYear(year)
                .statPeriodFrom(period.start().toString())
                .statPeriodTo(period.end().toString())
                .total(districtTotal)
                .maxCount(maxCount)
                .mapLevel("district")
                .districtName(toCanonicalDistrictName(districtName))
                .districtAdcode(ZIGONG_DISTRICT_ADCODE.get(toCanonicalDistrictName(districtName)))
                .regions(regions)
                .build();
    }

    private int sumDistrictCount(Map<String, Long> communities) {
        if (communities == null || communities.isEmpty()) {
            return 0;
        }
        return (int) Math.min(communities.values().stream().mapToLong(Long::longValue).sum(), Integer.MAX_VALUE);
    }

    private String resolveDistrictKey(Map<String, Map<String, Long>> matrix, String districtName) {
        if (StrUtil.isBlank(districtName)) {
            return null;
        }
        if (matrix.containsKey(districtName)) {
            return districtName;
        }
        for (String key : matrix.keySet()) {
            if (districtNamesMatch(key, districtName)) {
                return key;
            }
        }
        for (String canonical : ZIGONG_DISTRICT_NAMES) {
            if (districtNamesMatch(canonical, districtName)) {
                for (String key : matrix.keySet()) {
                    if (districtNamesMatch(key, canonical)) {
                        return key;
                    }
                }
                // 地图下钻：即使该区县暂无患者数据，也允许进入乡镇视图
                return canonical;
            }
        }
        return null;
    }

    /** 将部门树中的区县名规范为地图 GeoJSON 使用的标准名称 */
    private String toCanonicalDistrictName(String districtKey) {
        if (StrUtil.isBlank(districtKey)) {
            return districtKey;
        }
        for (String canonical : ZIGONG_DISTRICT_NAMES) {
            if (districtNamesMatch(canonical, districtKey)) {
                return canonical;
            }
        }
        return districtKey;
    }

    private boolean districtNamesMatch(String a, String b) {
        if (StrUtil.isBlank(a) || StrUtil.isBlank(b)) {
            return false;
        }
        String na = normalizeDistrictLabel(a);
        String nb = normalizeDistrictLabel(b);
        if (na.equals(nb)) {
            return true;
        }
        return stripDistrictAdminSuffix(na).equals(stripDistrictAdminSuffix(nb));
    }

    private String stripDistrictAdminSuffix(String name) {
        if (StrUtil.isBlank(name)) {
            return "";
        }
        if (name.endsWith("区") || name.endsWith("县")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    private String normalizeDistrictLabel(String name) {
        if (StrUtil.isBlank(name)) {
            return "";
        }
        return name.trim()
                .replace("自贡市", "")
                .replaceAll("\\s+", "");
    }

    private List<String> sortDistrictLabels(Set<String> districts) {
        List<String> sorted = districts.stream()
                .filter(d -> !"未分配".equals(d))
                .sorted(String::compareTo)
                .collect(Collectors.toCollection(ArrayList::new));
        if (districts.contains("未分配")) {
            sorted.add("未分配");
        }
        return sorted;
    }

    private List<String> sortCommunityLabels(Set<String> communities) {
        List<String> sorted = communities.stream()
                .filter(c -> !"—".equals(c)
                        && !HEATMAP_UNPARSED_TOWNSHIP.equals(c))
                .sorted(String::compareTo)
                .collect(Collectors.toCollection(ArrayList::new));
        if (communities.contains(HEATMAP_UNPARSED_TOWNSHIP)) {
            sorted.add(HEATMAP_UNPARSED_TOWNSHIP);
        }
        if (communities.contains("—")) {
            sorted.add("—");
        }
        return sorted;
    }

    /** 仅超级管理员及一级、二级、三级用户（role ≤ 4）可查看热力图 */
    private void assertHeatmapRoleAllowed() {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        if (role == null || role > 4) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "仅三级及以上用户可查看患者分布热力图");
        }
    }

    private LambdaQueryWrapper<Patient> buildManagedPatientDashboardWrapper(Integer statYear, List<Long> filterDeptIds) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Patient::getArchived, 0, 1);
        dataScopeHelper.applyPatientScope(wrapper);
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, Patient::getDepartmentId, filterDeptIds);
        if (statYear != null) {
            applyStatYearPatientFilter(wrapper, StatYearPeriod.of(statYear));
        }
        return wrapper;
    }

    /** 统计年度内患者：登记日期在周期内，或无登记日期时按创建时间 */
    private void applyStatYearPatientFilter(LambdaQueryWrapper<Patient> wrapper, StatYearPeriod period) {
        LocalDateTime from = period.start().atStartOfDay();
        LocalDateTime to = period.end().atTime(23, 59, 59);
        wrapper.and(w -> w
                .and(w1 -> w1.isNotNull(Patient::getEpidemicData)
                        .apply(REGISTRATION_DATE_SQL_EXPR + " IS NOT NULL")
                        .apply(REGISTRATION_DATE_SQL_EXPR + " >= {0}", period.start())
                        .apply(REGISTRATION_DATE_SQL_EXPR + " <= {0}", period.end()))
                .or(w2 -> w2.ge(Patient::getCreateTime, from).le(Patient::getCreateTime, to)));
    }

    /**
     * 病原学结果筛选：兼容主表 diagnosisResult 与 epidemic_data 中的「病原学结果」「诊断结果」。
     * 专病网等导入数据常存为「病原学阳性/阴性」，筛选项为「阳性/阴性」时需一并匹配。
     */
    private void applyDiagnosisResultFilter(LambdaQueryWrapper<Patient> wrapper, String diagnosisResult) {
        if (StrUtil.isBlank(diagnosisResult)) {
            return;
        }
        if ("阳性".equals(diagnosisResult)) {
            applyPathogenPositiveFilter(wrapper);
            return;
        }
        if ("病原学结果阳性".equals(diagnosisResult)) {
            applyPathogenResultPositiveFilter(wrapper);
            return;
        }
        if ("阴性".equals(diagnosisResult)) {
            applyPathogenValueFilter(wrapper, "阴性", "病原学阴性");
            return;
        }
        applyPathogenValueFilter(wrapper, diagnosisResult);
    }

    /** 病原学阳性（列表筛选项「阳性」）：兼容主表与 epidemic_data 中「病原学结果」「诊断结果」 */
    private void applyPathogenPositiveFilter(LambdaQueryWrapper<Patient> wrapper) {
        applyPathogenValueFilter(wrapper, "阳性", "病原学阳性");
    }

    /**
     * 病原学结果阳性（工作台统计 / 列表筛选项「病原学结果阳性」）：
     * 仅以主表 diagnosisResult 与 epidemic_data「病原学结果」为准，不含「诊断结果」字段。
     */
    private void applyPathogenResultPositiveFilter(LambdaQueryWrapper<Patient> wrapper) {
        applyPathogenResultFieldFilter(wrapper, PATHOGEN_RESULT_POSITIVE_VALUES);
    }

    private void applyPathogenResultFieldFilter(LambdaQueryWrapper<Patient> wrapper, String... values) {
        if (values == null || values.length == 0) {
            return;
        }
        String inClause = Arrays.stream(values)
                .map(v -> "'" + v.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        wrapper.and(w -> w.in(Patient::getDiagnosisResult, (Object[]) values)
                .or().apply("JSON_UNQUOTE(JSON_EXTRACT(epidemic_data, '"
                        + EPIDEMIC_JSON_PATHOGEN_RESULT + "')) IN (" + inClause + ")"));
    }

    private void applyPathogenValueFilter(LambdaQueryWrapper<Patient> wrapper, String... values) {
        if (values == null || values.length == 0) {
            return;
        }
        String inClause = Arrays.stream(values)
                .map(v -> "'" + v.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        wrapper.and(w -> w.in(Patient::getDiagnosisResult, (Object[]) values)
                .or().apply("JSON_UNQUOTE(JSON_EXTRACT(epidemic_data, '"
                        + EPIDEMIC_JSON_PATHOGEN_RESULT + "')) IN (" + inClause + ")")
                .or().apply("JSON_UNQUOTE(JSON_EXTRACT(epidemic_data, '"
                        + EPIDEMIC_JSON_DIAGNOSIS_RESULT + "')) IN (" + inClause + ")"));
    }

    /** 五级用户已完成首次随访的可编辑天数 */
    private static final int FIRST_VISIT_EDIT_DAYS_LEVEL5 = 10;

    /** 批量查询首次随访状态并填充到每条记录 */
    private void fillFirstVisitStatus(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> patientIds = patients.stream().map(Patient::getId).collect(Collectors.toList());
        LambdaQueryWrapper<FirstVisit> fw = new LambdaQueryWrapper<>();
        fw.in(FirstVisit::getPatientId, patientIds)
                .select(FirstVisit::getPatientId, FirstVisit::getStatus, FirstVisit::getCreateTime,
                        FirstVisit::getSputumCulture, FirstVisit::getSputumCultureSupplementStatus);
        Map<Long, FirstVisit> visitMap = firstVisitMapper.selectList(fw).stream()
                .collect(Collectors.toMap(FirstVisit::getPatientId, v -> v, (a, b) -> a));
        Integer currentRole = BaseContext.getCurrentRole();
        patients.forEach(p -> {
            FirstVisit visit = visitMap.get(p.getId());
            if (visit == null) {
                p.setHasFirstVisit(false);
                p.setFirstVisitStatus(null);
                p.setFirstVisitEditable(true);
                p.setFirstVisitSputumCulture(null);
                p.setSputumCultureSupplementStatus(null);
                return;
            }
            boolean completed = Integer.valueOf(1).equals(visit.getStatus());
            p.setHasFirstVisit(completed);
            p.setFirstVisitStatus(visit.getStatus());
            p.setFirstVisitEditable(isFirstVisitEditable(currentRole, visit));
            p.setFirstVisitSputumCulture(visit.getSputumCulture());
            p.setSputumCultureSupplementStatus(visit.getSputumCultureSupplementStatus());
        });
    }

    /** 批量统计已完成后续随访次数 */
    private void fillFollowUpCount(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) {
            return;
        }
        List<Long> patientIds = patients.stream().map(Patient::getId).filter(Objects::nonNull).toList();
        if (patientIds.isEmpty()) {
            return;
        }
        Map<Long, Integer> countMap = new HashMap<>();
        followUpVisitMapper.selectList(new LambdaQueryWrapper<FollowUpVisit>()
                        .in(FollowUpVisit::getPatientId, patientIds)
                        .eq(FollowUpVisit::getStatus, 1)
                        .select(FollowUpVisit::getPatientId))
                .forEach(v -> countMap.merge(v.getPatientId(), 1, Integer::sum));
        patients.forEach(p -> p.setFollowUpCount(countMap.getOrDefault(p.getId(), 0)));
    }

    /**
     * 批量填充服药管理完成情况：
     * 待填写（无记录）/ 进行中（有记录未停止）/ 已完成（已填停止完成时间）
     */
    private void fillMedicationManagementStatus(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) {
            return;
        }
        List<Long> patientIds = patients.stream().map(Patient::getId).filter(Objects::nonNull).toList();
        if (patientIds.isEmpty()) {
            return;
        }
        Map<Long, MedicationManagement> latestMap = new HashMap<>();
        medicationManagementMapper.selectList(new LambdaQueryWrapper<MedicationManagement>()
                        .in(MedicationManagement::getPatientId, patientIds)
                        .orderByDesc(MedicationManagement::getId))
                .forEach(m -> latestMap.putIfAbsent(m.getPatientId(), m));
        patients.forEach(p -> {
            MedicationManagement med = latestMap.get(p.getId());
            if (med == null) {
                p.setMedicationManagementStatus("待填写");
            } else if (med.getStopDate() != null) {
                p.setMedicationManagementStatus("已完成");
            } else {
                p.setMedicationManagementStatus("进行中");
            }
        });
    }

    /** 批量查询领药记录摘要并填充到每条记录 */
    private void fillMedicationPickupSummary(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> patientIds = patients.stream().map(Patient::getId).collect(Collectors.toList());
        LambdaQueryWrapper<MedicationPickup> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MedicationPickup::getPatientId, patientIds)
                .orderByAsc(MedicationPickup::getCreateTime);
        List<MedicationPickup> pickups = medicationPickupMapper.selectList(wrapper);
        Map<Long, List<MedicationPickup>> grouped = pickups.stream()
                .collect(Collectors.groupingBy(MedicationPickup::getPatientId));
        patients.forEach(p -> {
            List<MedicationPickup> list = grouped.get(p.getId());
            if (list == null || list.isEmpty()) {
                p.setMedicationPickupCount(0);
                p.setMedicationPickTime(null);
                p.setMedicationChemotherapy(null);
                p.setMedicationDrugForm(null);
                return;
            }
            p.setMedicationPickupCount(list.size());
            MedicationPickup latest = list.get(list.size() - 1);
            p.setMedicationPickTime(latest.getPickupTime() != null ? latest.getPickupTime().toString() : null);
            p.setMedicationChemotherapy(formatDrugNames(latest.getDrugs()));
            p.setMedicationDrugForm(formatDrugQuantities(latest.getDrugs(), latest.getQuantity(), latest.getQuantityUnit()));
        });
    }

    private String formatDrugQuantities(String drugsJson, java.math.BigDecimal legacyQuantity, String legacyUnit) {
        if (StrUtil.isNotBlank(drugsJson)) {
            try {
                JSONArray array = JSONUtil.parseArray(drugsJson);
                String joined = array.stream()
                        .map(item -> {
                            if (!(item instanceof JSONObject obj)) {
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
                // fallback to legacy fields
            }
        }
        if (legacyQuantity != null && StrUtil.isNotBlank(legacyUnit)) {
            return legacyQuantity.stripTrailingZeros().toPlainString() + legacyUnit;
        }
        return null;
    }

    private String formatDrugNames(String drugsJson) {
        if (StrUtil.isBlank(drugsJson)) return null;
        try {
            JSONArray array = JSONUtil.parseArray(drugsJson);
            return array.stream()
                    .map(item -> {
                        if (item instanceof JSONObject obj) {
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

    /** 五级用户：已完成首次随访创建后 10 天内可改；管理员（非五级）随时可改 */
    private boolean isFirstVisitEditable(Integer role, FirstVisit visit) {
        if (visit == null || !Integer.valueOf(1).equals(visit.getStatus())) {
            return true;
        }
        if (role == null || role != 6) {
            return true;
        }
        if (visit.getCreateTime() == null) {
            return true;
        }
        return !visit.getCreateTime().plusDays(FIRST_VISIT_EDIT_DAYS_LEVEL5).isBefore(LocalDateTime.now());
    }

    /** 从筛查表关联填充胸片检查日期和结果（仅转诊确诊患者有 screeningId） */
    private void fillScreeningXrayData(List<Patient> patients, String populationType) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> screeningIds = patients.stream()
                .filter(p -> p.getScreeningId() != null)
                .map(Patient::getScreeningId)
                .distinct()
                .collect(Collectors.toList());
        if (screeningIds.isEmpty()) return;

        if ("school".equals(populationType)) {
            List<ScreeningSchool> schools = screeningSchoolMapper.selectBatchIds(screeningIds);
            Map<Long, ScreeningSchool> schoolMap = schools.stream()
                    .collect(Collectors.toMap(ScreeningSchool::getId, s -> s));
            patients.forEach(p -> {
                ScreeningSchool s = schoolMap.get(p.getScreeningId());
                if (s != null) {
                    p.setChestXrayDate(s.getChestXrayDate());
                    p.setChestXrayResult(s.getChestXrayResult());
                    p.setScreenDate(s.getScreenDate());
                    p.setScreenMethod(s.getScreenMethod());
                    p.setInfectionResult(s.getInfectionResult());
                }
            });
        } else if ("keyPopulation".equals(populationType) || "regular".equals(populationType)) {
            // regular 筛查数据也存储在 screening_key_population 表中（通过 source_type 区分）
            List<ScreeningKeyPopulation> keyPops = screeningKeyPopulationMapper.selectBatchIds(screeningIds);
            Map<Long, ScreeningKeyPopulation> keyPopMap = keyPops.stream()
                    .collect(Collectors.toMap(ScreeningKeyPopulation::getId, k -> k));
            patients.forEach(p -> {
                ScreeningKeyPopulation k = keyPopMap.get(p.getScreeningId());
                if (k != null) {
                    p.setChestXrayDate(k.getChestXrayDate());
                    p.setChestXrayResult(k.getChestXrayResult());
                    p.setScreenDate(k.getScreenDate());
                    p.setScreenMethod(k.getScreenMethod());
                    p.setInfectionResult(k.getInfectionResult());
                }
            });
        } else if ("closeContact".equals(populationType)) {
            List<ScreeningCloseContact> closeContacts = screeningCloseContactMapper.selectBatchIds(screeningIds);
            Map<Long, ScreeningCloseContact> closeContactMap = closeContacts.stream()
                    .collect(Collectors.toMap(ScreeningCloseContact::getId, c -> c));
            patients.forEach(p -> {
                ScreeningCloseContact c = closeContactMap.get(p.getScreeningId());
                if (c != null) {
                    // 密接人群胸片字段为 imagingDate/imagingResult，统一映射到 Patient 的 chestXrayDate/chestXrayResult
                    p.setChestXrayDate(c.getImagingDate());
                    p.setChestXrayResult(c.getImagingResult());
                }
            });
        }
    }

    /** 批量查询患者通知单状态并填充到每条记录 */
    private void fillNoticeStatus(List<Patient> patients, String populationType) {
        if (patients == null || patients.isEmpty()) return;
        List<Long> patientIds = patients.stream().map(Patient::getId).collect(Collectors.toList());
        LambdaQueryWrapper<Notice> nw = new LambdaQueryWrapper<>();
        nw.in(Notice::getBizId, patientIds)
          .eq(Notice::getNoticeType, "patient")
          .eq(StrUtil.isNotBlank(populationType), Notice::getPopulationType, populationType);
        List<Notice> notices = noticeMapper.selectList(nw);
        // 每个 bizId 保留最新一条（取 id 最大的，因为插入顺序即为时间顺序）
        Map<Long, Notice> noticeMap = notices.stream()
                .collect(Collectors.toMap(
                        Notice::getBizId,
                        n -> n,
                        (a, b) -> a.getId() > b.getId() ? a : b
                ));
        Set<Long> userIds = noticeMap.values().stream()
                .flatMap(n -> Stream.of(n.getSenderId(), n.getReceiverOrgId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        patients.forEach(p -> {
            Notice n = noticeMap.get(p.getId());
            if (n != null) {
                p.setNoticeStatus(n.getStatus());
                p.setNoticeId(n.getId());
                p.setNoticeSentTime(n.getSentTime());
                p.setNoticeConfirmedTime(n.getConfirmedTime());
                p.setNoticeMedicationUnit(n.getMedicationManagementUnit());
                p.setNoticeRemark(n.getRemark());
                p.setNoticeSenderName(resolveUserDisplayName(userMap.get(n.getSenderId())));
                p.setNoticeReceiverName(resolveUserDisplayName(userMap.get(n.getReceiverOrgId())));
            }
        });
    }

    private static String resolveUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        return StrUtil.blankToDefault(user.getRealName(), user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importEpidemic(MultipartFile file, String populationType) {
        String batchId = IdUtil.fastSimpleUUID();
        // headRowNumber(0) 使首行（表头）也作为数据行读入，以便构建列索引映射
        List<Map<Integer, String>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    allRows.add(new LinkedHashMap<>(data));
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("大疫情表解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel读取失败");
        }

        if (allRows.size() < 2) {
            log.warn("大疫情表无数据行，跳过导入");
            return 0;
        }

        // 解析第一行表头，构建 字段名 -> 列索引 映射
        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }
        log.info("大疫情表表头解析：{}", headerIndex.keySet());

        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());

        int matchCount = 0;
        for (int ri = 0; ri < dataRows.size(); ri++) {
            Map<Integer, String> row = dataRows.get(ri);
            int importRowNo = ri + 2;
            String nameVal = getFieldByHeader(row, headerIndex, "姓名");
            String idNumberVal = getFieldByHeader(row, headerIndex, "证件号", "身份证号", "身份证");

            // 跳过姓名和证件号均为空的空行
            if (StrUtil.isBlank(nameVal) && StrUtil.isBlank(idNumberVal)) {
                continue;
            }

            Map<String, String> namedRow = buildNamedRowFields(row, headerIndex);
            String rawJson;
            try {
                rawJson = objectMapper.writeValueAsString(namedRow);
            } catch (Exception e) {
                rawJson = namedRow.toString();
            }

            // 优先按证件号精确匹配，再按姓名模糊匹配（仅在当前用户辖区内匹配）
            Patient matched = null;
            if (StrUtil.isNotBlank(idNumberVal)) {
                LambdaQueryWrapper<Patient> idWrapper = new LambdaQueryWrapper<>();
                idWrapper.eq(Patient::getPopulationType, populationType)
                        .eq(Patient::getIdNumber, idNumberVal)
                        .last("LIMIT 1");
                dataScopeHelper.applyImportDedupScope(idWrapper, Patient::getDepartmentId, Patient::getCreatorId);
                matched = getOne(idWrapper, false);
            }
            if (matched == null && StrUtil.isNotBlank(nameVal)) {
                LambdaQueryWrapper<Patient> nameWrapper = new LambdaQueryWrapper<>();
                nameWrapper.eq(Patient::getPopulationType, populationType)
                        .like(Patient::getName, nameVal)
                        .last("LIMIT 1");
                dataScopeHelper.applyImportDedupScope(nameWrapper, Patient::getDepartmentId, Patient::getCreatorId);
                matched = getOne(nameWrapper, false);
            }

            EpidemicReport report = EpidemicReport.builder()
                    .populationType(populationType)
                    .rawData(rawJson)
                    .uploadBatch(batchId)
                    .build();

            if (matched != null) {
                matched.setEpidemicData(rawJson);
                matched.setImportRowNo(importRowNo);
                updateById(matched);
                report.setPatientId(matched.getId());
                report.setMatched(1);
                matchCount++;
            } else {
                Patient newPatient = Patient.builder()
                        .populationType(populationType)
                        .name(nameVal)
                        .idNumber(idNumberVal)
                        .source("epidemic")
                        .archived(0)
                        .epidemicData(rawJson)
                        .importRowNo(importRowNo)
                        .departmentId(BaseContext.getCurrentDepartmentId())
                        .creatorId(BaseContext.getCurrentId())
                        .build();
                save(newPatient);
                report.setPatientId(newPatient.getId());
                report.setMatched(0);
            }

            epidemicReportService.save(report);
        }

        log.info("大疫情导入完成：共 {} 条数据，匹配 {} 条", dataRows.size(), matchCount);
        return dataRows.size();
    }

    @Override
    public void archivePatient(Long id) {
        archivePatient(id, null);
    }

    @Override
    public void archivePatient(Long id, String archiveRemark) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        assertPatientNotTransferLocked(patient);
        if (Integer.valueOf(1).equals(patient.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者已归档");
        }
        if (ARCHIVE_REMARK_TRANSFERRED_OUT.equals(archiveRemark)
                || ARCHIVE_REMARK_TRANSFER_PENDING.equals(archiveRemark)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请使用转出流程标记转出状态");
        }
        patient.setArchived(1);
        patient.setArchivedTime(LocalDateTime.now());
        if (StrUtil.isNotBlank(archiveRemark)) {
            patient.setArchiveRemark(archiveRemark);
        }
        updateById(patient);
    }

    @Override
    public void markTransferPending(Long id) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        if (Integer.valueOf(1).equals(patient.getArchived())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "已归档患者不可转出");
        }
        if (ARCHIVE_REMARK_TRANSFERRED_OUT.equals(patient.getArchiveRemark())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该患者已转出，不可再次发起");
        }
        if (ARCHIVE_REMARK_TRANSFER_PENDING.equals(patient.getArchiveRemark())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该患者已有待确认的转出申请");
        }
        patient.setArchiveRemark(ARCHIVE_REMARK_TRANSFER_PENDING);
        patient.setArchived(0);
        patient.setArchivedTime(null);
        updateById(patient);
    }

    @Override
    public void markTransferredOut(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        // 转出确认后原记录退出在管，保证全系统在管仅保留接收方一条
        boolean updated = lambdaUpdate()
                .eq(Patient::getId, id)
                .set(Patient::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT)
                .set(Patient::getArchived, 1)
                .set(Patient::getArchivedTime, LocalDateTime.now())
                .update();
        if (!updated) {
            patient.setArchiveRemark(ARCHIVE_REMARK_TRANSFERRED_OUT);
            patient.setArchived(1);
            patient.setArchivedTime(LocalDateTime.now());
            updateById(patient);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyPatientForTransferOut(Long sourcePatientId, Long receiverUserId) {
        Patient source = getById(sourcePatientId);
        if (source == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "源患者不存在");
        }
        User receiver = userMapper.selectById(receiverUserId);
        if (receiver == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "接收人不存在");
        }
        if (receiver.getDepartmentId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "接收人未关联部门，无法同步");
        }
        assertNoDuplicateInReceiverDept(source, receiver.getDepartmentId());

        Patient copy = new Patient();
        BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime",
                "creatorId", "departmentId", "sourcePatientId", "archiveRemark", "archived", "archivedTime");
        copy.setSourcePatientId(sourcePatientId);
        copy.setCreatorId(receiverUserId);
        copy.setDepartmentId(receiver.getDepartmentId());
        copy.setArchived(0);
        copy.setArchiveRemark(null);
        copy.setArchivedTime(null);
        save(copy);
        Long newPatientId = copy.getId();

        copyPatientNotices(sourcePatientId, newPatientId, receiverUserId, receiver);
        copyPatientFirstVisits(sourcePatientId, newPatientId);
        copyPatientFollowUpVisits(sourcePatientId, newPatientId);
        copyPatientMedicationManagement(sourcePatientId, newPatientId);
        copyPatientMedicationPickups(sourcePatientId, newPatientId, receiverUserId);

        // 兜底：确保接收方记录为可操作的在管状态（可填通知单/首次随访/后续随访）
        lambdaUpdate()
                .eq(Patient::getId, newPatientId)
                .set(Patient::getArchived, 0)
                .set(Patient::getArchiveRemark, null)
                .set(Patient::getArchivedTime, null)
                .update();

        log.info("转出同步：已复制患者 sourceId={} -> newId={}, receiverUserId={}, deptId={}",
                sourcePatientId, newPatientId, receiverUserId, receiver.getDepartmentId());
        return newPatientId;
    }

    private void assertNoDuplicateInReceiverDept(Patient source, Long receiverDeptId) {
        if (StrUtil.isBlank(source.getIdNumber())) {
            return;
        }
        long count = lambdaQuery()
                .eq(Patient::getDepartmentId, receiverDeptId)
                .eq(Patient::getIdNumber, source.getIdNumber())
                .eq(Patient::getArchived, 0)
                .and(w -> w.isNull(Patient::getArchiveRemark)
                        .or()
                        .ne(Patient::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT))
                .ne(Patient::getId, source.getId())
                .count();
        if (count > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "接收方部门已存在相同证件号的在管患者，无法转出");
        }
    }

    private void copyPatientNotices(Long sourcePatientId, Long newPatientId,
                                    Long receiverUserId, User receiver) {
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getBizId, sourcePatientId)
                .eq(Notice::getNoticeType, "patient"));
        String receiverName = receiver.getRealName() != null ? receiver.getRealName() : receiver.getUsername();
        for (Notice source : notices) {
            Notice copy = new Notice();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "bizId",
                    "senderId", "receiverOrgId", "senderName", "senderOrgName",
                    "receiverName", "receiverOrgName");
            copy.setBizId(newPatientId);
            copy.setSenderId(receiverUserId);
            copy.setSenderName(receiverName);
            copy.setSenderOrgName(receiver.getOrgName());
            copy.setReceiverOrgId(null);
            copy.setReceiverName(null);
            copy.setReceiverOrgName(null);
            // 待确认通知单随转出一并视为已完成，避免接收方重复确认
            if (Integer.valueOf(1).equals(source.getStatus())) {
                copy.setStatus(2);
            }
            noticeMapper.insert(copy);
        }
    }

    private void copyPatientFirstVisits(Long sourcePatientId, Long newPatientId) {
        List<FirstVisit> records = firstVisitMapper.selectList(new LambdaQueryWrapper<FirstVisit>()
                .eq(FirstVisit::getPatientId, sourcePatientId));
        for (FirstVisit source : records) {
            FirstVisit copy = new FirstVisit();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "patientId");
            copy.setPatientId(newPatientId);
            firstVisitMapper.insert(copy);
        }
    }

    private void copyPatientFollowUpVisits(Long sourcePatientId, Long newPatientId) {
        List<FollowUpVisit> records = followUpVisitMapper.selectList(new LambdaQueryWrapper<FollowUpVisit>()
                .eq(FollowUpVisit::getPatientId, sourcePatientId));
        for (FollowUpVisit source : records) {
            FollowUpVisit copy = new FollowUpVisit();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "patientId");
            copy.setPatientId(newPatientId);
            followUpVisitMapper.insert(copy);
        }
    }

    private void copyPatientMedicationManagement(Long sourcePatientId, Long newPatientId) {
        List<MedicationManagement> records = medicationManagementMapper.selectList(
                new LambdaQueryWrapper<MedicationManagement>().eq(MedicationManagement::getPatientId, sourcePatientId));
        for (MedicationManagement source : records) {
            MedicationManagement copy = new MedicationManagement();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "patientId");
            copy.setPatientId(newPatientId);
            medicationManagementMapper.insert(copy);
        }
    }

    private void copyPatientMedicationPickups(Long sourcePatientId, Long newPatientId, Long receiverUserId) {
        List<MedicationPickup> records = medicationPickupMapper.selectList(
                new LambdaQueryWrapper<MedicationPickup>().eq(MedicationPickup::getPatientId, sourcePatientId));
        for (MedicationPickup source : records) {
            MedicationPickup copy = new MedicationPickup();
            BeanUtils.copyProperties(source, copy, "id", "createTime", "updateTime", "patientId", "filledBy");
            copy.setPatientId(newPatientId);
            copy.setFilledBy(receiverUserId);
            medicationPickupMapper.insert(copy);
        }
    }

    @Override
    public void assertPatientOperable(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        assertPatientNotTransferLocked(patient);
    }

    private void assertPatientNotTransferLocked(Patient patient) {
        if (PatientService.isTransferLocked(patient)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    ARCHIVE_REMARK_TRANSFERRED_OUT.equals(patient.getArchiveRemark())
                            ? "该患者已转出，不可操作"
                            : "该患者转出待确认，不可操作");
        }
    }

    @Override
    public void restoreTransferredPatient(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            return;
        }
        if (!ARCHIVE_REMARK_TRANSFER_PENDING.equals(patient.getArchiveRemark())) {
            return;
        }
        patient.setArchiveRemark(null);
        updateById(patient);
    }

    @Override
    public void unarchivePatientFromStopTreatment(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        if (!Integer.valueOf(1).equals(patient.getArchived())) {
            return;
        }
        if (!PatientService.isStopTreatmentArchiveRemark(patient.getArchiveRemark())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅停止治疗归档的患者可解锁");
        }
        patient.setArchived(0);
        patient.setArchivedTime(null);
        patient.setArchiveRemark(null);
        updateById(patient);
    }

    @Override
    public IPage<Patient> queryHistoryPage(int page, int size, String populationType,
                                            String name, String idNumber, String phone,
                                            String diagnosisResult, String startTime, String endTime,
                                            String stopTreatmentReason) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
                .eq(Patient::getArchived, 1)
                // 已转出记录不进入历史患者（仅保留最新在管链路）
                .and(w -> w.isNull(Patient::getArchiveRemark)
                        .or()
                        .ne(Patient::getArchiveRemark, ARCHIVE_REMARK_TRANSFERRED_OUT))
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), Patient::getPhone, phone)
                .ge(StrUtil.isNotBlank(startTime), Patient::getArchivedTime, startTime)
                .le(StrUtil.isNotBlank(endTime), Patient::getArchivedTime, endTime + " 23:59:59")
                .orderByDesc(Patient::getArchivedTime);
        applyDiagnosisResultFilter(wrapper, diagnosisResult);
        if (StrUtil.isNotBlank(stopTreatmentReason)) {
            List<Long> matchedPatientIds = findPatientIdsByPreferredStopTreatmentReason(stopTreatmentReason);
            if (matchedPatientIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            wrapper.in(Patient::getId, matchedPatientIds);
        }
        dataScopeHelper.applyPatientScope(wrapper);
        IPage<Patient> result = page(new Page<>(page, size), wrapper);
        fillNoticeStatus(result.getRecords(), populationType);
        fillFirstVisitStatus(result.getRecords());
        fillMedicationPickupSummary(result.getRecords());
        fillEpidemicExtraFields(result.getRecords());
        fillStopTreatmentReason(result.getRecords());
        return result;
    }

    private void fillStopTreatmentReason(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) {
            return;
        }
        List<Long> patientIds = patients.stream()
                .map(Patient::getId)
                .filter(Objects::nonNull)
                .toList();
        if (patientIds.isEmpty()) {
            return;
        }
        Map<Long, FollowUpVisit> preferredMap = followUpVisitMapper.selectList(
                new LambdaQueryWrapper<FollowUpVisit>()
                        .in(FollowUpVisit::getPatientId, patientIds)
                        .eq(FollowUpVisit::getStatus, 1)
                        .eq(FollowUpVisit::getStopTreatment, "是")
                        .orderByDesc(FollowUpVisit::getVisitDate)
                        .orderByDesc(FollowUpVisit::getId)
        ).stream().collect(Collectors.groupingBy(
                FollowUpVisit::getPatientId,
                Collectors.collectingAndThen(Collectors.toList(), this::selectPreferredStopTreatmentVisit)
        ));
        patients.forEach(patient -> {
            FollowUpVisit visit = preferredMap.get(patient.getId());
            if (visit != null) {
                patient.setStopTreatmentReason(visit.getStopTreatmentReason());
                patient.setStopTreatmentReasonOther(visit.getStopTreatmentReasonOther());
            }
        });
    }

    private FollowUpVisit selectPreferredStopTreatmentVisit(List<FollowUpVisit> visits) {
        if (visits == null || visits.isEmpty()) {
            return null;
        }
        return visits.stream()
                .filter(v -> "是".equals(v.getStopTreatment()) && Integer.valueOf(1).equals(v.getStatus()))
                .max(Comparator.comparing(FollowUpVisit::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    @Override
    public List<Long> findPatientIdsByPreferredStopTreatmentReason(String stopTreatmentReason) {
        if (StrUtil.isBlank(stopTreatmentReason)) {
            return List.of();
        }
        return followUpVisitMapper.selectList(
                new LambdaQueryWrapper<FollowUpVisit>()
                        .eq(FollowUpVisit::getStatus, 1)
                        .eq(FollowUpVisit::getStopTreatment, "是")
                        .isNotNull(FollowUpVisit::getPatientId)
        ).stream()
                .collect(Collectors.groupingBy(FollowUpVisit::getPatientId))
                .entrySet().stream()
                .filter(entry -> {
                    FollowUpVisit preferred = selectPreferredStopTreatmentVisit(entry.getValue());
                    return preferred != null && stopTreatmentReason.equals(preferred.getStopTreatmentReason());
                })
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 从 epidemicData JSON 解析导入扩展字段（人群分类、现管单位、治疗分类及完整导入字段） */
    private void fillEpidemicExtraFields(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) return;
        for (Patient patient : patients) {
            Map<String, String> fields = parseImportFields(patient);
            patient.setImportFields(fields);
            patient.setRegistrationNo(fields.getOrDefault("登记号", ""));
            patient.setTreatmentClass(fields.getOrDefault("治疗分类", ""));
            if ("specialDisease".equals(patient.getPopulationType()) && hasKeyPopulationColumns(fields)) {
                patient.setCrowdCategory(resolveCrowdCategoryFromImportFields(fields));
            } else {
                String crowdCategory = fields.get("人群分类");
                if (StrUtil.isNotBlank(crowdCategory)) {
                    patient.setCrowdCategory(crowdCategory);
                }
            }
            String currentUnit = fields.getOrDefault("现管单位", fields.get("现管理单位"));
            if (StrUtil.isNotBlank(currentUnit)) {
                patient.setCurrentManagementUnit(currentUnit);
            }
        }
    }

    private boolean hasKeyPopulationColumns(Map<String, String> fields) {
        return fields.keySet().stream().anyMatch(k -> k.startsWith("重点人群"));
    }

    /** 从已解析的导入字段 Map 推导专病网人群分类 */
    private String resolveCrowdCategoryFromImportFields(Map<String, String> fields) {
        List<String> matched = new ArrayList<>();
        if ("是".equals(fields.get("重点人群-A.密切接触者"))) matched.add("密接");
        if ("是".equals(fields.get("重点人群-E.学校托幼机构人员"))) matched.add("学生");
        if ("是".equals(fields.get("重点人群-D.医务人员"))) matched.add("教职工");
        if ("是".equals(fields.get("重点人群-J.养老院居住者"))
                || "是".equals(fields.get("重点人群-K.福利院居住者"))) {
            matched.add("老年人");
        }
        if ("是".equals(fields.get("重点人群-C.糖尿病患者"))) matched.add("糖尿病");
        if ("是".equals(fields.get("重点人群-B.HIV/AIDS患者"))) matched.add("双感");

        String keyPopulation = fields.get("重点人群");
        if (StrUtil.isNotBlank(keyPopulation) && !"否".equals(keyPopulation.trim())) {
            String mapped = mapKeyPopulationLabel(keyPopulation);
            if (mapped != null && !matched.contains(mapped)) {
                matched.add(mapped);
            }
        }

        if (matched.isEmpty()) {
            String stored = fields.get("人群分类");
            if (StrUtil.isNotBlank(stored) && CROWD_CATEGORY_PRIORITY.contains(stored)) {
                return stored;
            }
            return StrUtil.isNotBlank(stored) ? stored : "非重点人群";
        }
        for (String category : CROWD_CATEGORY_PRIORITY) {
            if (matched.contains(category)) {
                return category;
            }
        }
        return matched.get(0);
    }

    /** 解析 epidemicData：支持表头键名 JSON 及 legacy 列索引 JSON */
    private Map<String, String> parseImportFields(Patient patient) {
        if (StrUtil.isBlank(patient.getEpidemicData())) {
            return Collections.emptyMap();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(patient.getEpidemicData(), Map.class);
            if (raw.isEmpty()) {
                return Collections.emptyMap();
            }
            boolean indexKeys = raw.keySet().stream().allMatch(k -> k.matches("\\d+"));
            Map<String, String> fields = new LinkedHashMap<>();
            if (indexKeys) {
                List<String> headers = "specialDisease".equals(patient.getPopulationType())
                        ? PatientImportHeaders.SPECIAL_DISEASE
                        : PatientImportHeaders.EPIDEMIC_REPORT;
                raw.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(
                                Integer.parseInt(a.getKey()), Integer.parseInt(b.getKey())))
                        .forEach(entry -> {
                            int idx = Integer.parseInt(entry.getKey());
                            if (idx >= 0 && idx < headers.size() && entry.getValue() != null
                                    && StrUtil.isNotBlank(entry.getValue().toString())) {
                                fields.put(headers.get(idx), entry.getValue().toString().trim());
                            }
                        });
            } else {
                raw.forEach((key, value) -> {
                    if (value != null && StrUtil.isNotBlank(value.toString())) {
                        fields.put(key, value.toString().trim());
                    }
                });
            }
            return fields;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    /** 将 Excel 行按表头映射为 字段名 -> 值（日期列自动归一化为 yyyy-MM-dd） */
    private Map<String, String> buildNamedRowFields(Map<Integer, String> row, Map<String, Integer> headerIndex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            String val = row.get(entry.getValue());
            if (StrUtil.isNotBlank(val)) {
                String header = entry.getKey();
                String normalized = header != null && header.contains("日期")
                        ? FlexibleDateParseUtil.normalizeToStandardString(val.trim())
                        : val.trim();
                fields.put(header, normalized);
            }
        }
        return fields;
    }

    /**
     * 专病表人群分类：T 列「人群分类」为职业，实际分类来自 U 列「重点人群」及 V-AE 各子列。
     */
    private String resolveSpecialDiseaseCrowdCategory(Map<Integer, String> row, Map<String, Integer> headerIndex) {
        List<String> matched = new ArrayList<>();
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-A.密切接触者"))) {
            matched.add("密接");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-E.学校托幼机构人员"))) {
            matched.add("学生");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-D.医务人员"))) {
            matched.add("教职工");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-J.养老院居住者"))
                || "是".equals(getFieldByHeader(row, headerIndex, "重点人群-K.福利院居住者"))) {
            matched.add("老年人");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-C.糖尿病患者"))) {
            matched.add("糖尿病");
        }
        if ("是".equals(getFieldByHeader(row, headerIndex, "重点人群-B.HIV/AIDS患者"))) {
            matched.add("双感");
        }

        String keyPopulation = getFieldByHeader(row, headerIndex, "重点人群");
        if (StrUtil.isNotBlank(keyPopulation) && !"否".equals(keyPopulation.trim())) {
            String mapped = mapKeyPopulationLabel(keyPopulation);
            if (mapped != null && !matched.contains(mapped)) {
                matched.add(mapped);
            }
        }

        if (matched.isEmpty()) {
            return "非重点人群";
        }
        for (String category : CROWD_CATEGORY_PRIORITY) {
            if (matched.contains(category)) {
                return category;
            }
        }
        return matched.get(0);
    }

    private String mapKeyPopulationLabel(String raw) {
        if (StrUtil.isBlank(raw)) return null;
        String val = raw.trim();
        if ("否".equals(val)) return null;
        if (val.contains("密切") || val.contains("密接")) return "密接";
        if (val.contains("学校") || val.contains("托幼")) return "学生";
        if (val.contains("医务人员") || val.contains("教职工")) return "教职工";
        if (val.contains("养老") || val.contains("福利院")) return "老年人";
        if (val.contains("糖尿病")) return "糖尿病";
        if (val.contains("HIV") || val.contains("AIDS") || val.contains("双感")) return "双感";
        if (val.contains("既往") && val.contains("结核")) return "既往结核";
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePatient(Long id) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        if (BaseContext.isSuperAdmin() && PatientService.isTransferLocked(patient)) {
            referralService.deleteReferralsAndMessagesByBizId(id);
        } else {
            assertPatientNotTransferLocked(patient);
        }
        // 级联软删：首次随访
        firstVisitMapper.delete(new LambdaQueryWrapper<FirstVisit>()
                .eq(FirstVisit::getPatientId, id));
        // 级联软删：后续随访
        followUpVisitMapper.delete(new LambdaQueryWrapper<FollowUpVisit>()
                .eq(FollowUpVisit::getPatientId, id));
        // 级联软删：服药管理
        medicationManagementMapper.delete(new LambdaQueryWrapper<MedicationManagement>()
                .eq(MedicationManagement::getPatientId, id));
        // 级联软删：领药记录
        medicationPickupMapper.delete(new LambdaQueryWrapper<MedicationPickup>()
                .eq(MedicationPickup::getPatientId, id));
        // 级联软删：通知单
        noticeMapper.delete(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getBizId, id)
                .eq(Notice::getNoticeType, "patient"));
        // 删除患者本体（MyBatis-Plus 逻辑删除：设 deleted=1）
        removeById(id);
        log.info("删除患者 id={} 及其级联数据", id);
    }

    /**
     * 根据表头名称从数据行中提取字段值（仅精确匹配）。
     */
    private String getFieldByHeaderExact(Map<Integer, String> row, Map<String, Integer> headerIndex,
                                         String... fieldNames) {
        for (String fieldName : fieldNames) {
            Integer idx = headerIndex.get(fieldName);
            if (idx == null) continue;
            String val = row.get(idx);
            if (StrUtil.isNotBlank(val)) {
                return val.trim();
            }
        }
        return null;
    }

    /**
     * 专病网现住址：去掉省、市级前缀，仅保留区县及后续详细地址。
     * 例：四川省自贡市富顺县代寺镇… → 富顺县代寺镇…
     */
    private String normalizeSpecialDiseaseCurrentAddress(String address) {
        if (StrUtil.isBlank(address)) return address;
        String normalized = address.trim();
        normalized = normalized.replaceFirst("^[^省\\s]+省", "");
        normalized = normalized.replaceFirst("^[^市\\s]+市", "");
        return normalized.trim();
    }

    /**
     * 根据表头名称从数据行中提取字段值。
     * 支持多个候选字段名，先精确匹配，再按"表头包含关键字"模糊匹配。
     */
    private String getFieldByHeader(Map<Integer, String> row, Map<String, Integer> headerIndex,
                                    String... fieldNames) {
        for (String fieldName : fieldNames) {
            // 精确匹配
            Integer idx = headerIndex.get(fieldName);
            if (idx != null) {
                String val = row.get(idx);
                if (StrUtil.isNotBlank(val)) {
                    return val.trim();
                }
            }
            // 模糊匹配（表头中包含目标字段名）
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                if (entry.getKey().contains(fieldName)) {
                    String val = row.get(entry.getValue());
                    if (StrUtil.isNotBlank(val)) {
                        return val.trim();
                    }
                }
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importSpecialDisease(MultipartFile file) {
        List<Map<Integer, String>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new com.alibaba.excel.read.listener.ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, com.alibaba.excel.context.AnalysisContext context) {
                    allRows.add(new LinkedHashMap<>(data));
                }
                @Override
                public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
                    log.info("专病表解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (java.io.IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "专病表Excel读取失败");
        }

        if (allRows.size() < 2) {
            log.warn("专病表无数据行，跳过导入");
            return 0;
        }

        // 解析表头
        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }
        log.info("专病表表头解析：{}", headerIndex.keySet());

        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());
        int count = 0;
        for (int ri = 0; ri < dataRows.size(); ri++) {
            Map<Integer, String> row = dataRows.get(ri);
            int importRowNo = ri + 2;
            String name = getFieldByHeader(row, headerIndex, "患者姓名", "姓名");
            String idNumber = getFieldByHeader(row, headerIndex, "身份证号", "有效证件号", "证件号");
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) continue;

            String gender = getFieldByHeader(row, headerIndex, "性别");
            String birthDateStr = getFieldByHeader(row, headerIndex, "出生日期");
            String ageStr = getFieldByHeader(row, headerIndex, "年龄");
            String phone = getFieldByHeader(row, headerIndex, "患者联系电话", "联系电话", "电话");
            // 专病表地址须精确匹配「详细」列，避免误取「现地址类型」「户籍地址类别」
            String currentAddress = normalizeSpecialDiseaseCurrentAddress(
                    getFieldByHeaderExact(row, headerIndex, "现地址详细", "现详细住址"));
            String householdAddress = getFieldByHeaderExact(row, headerIndex, "户籍地址详细");
            String diagnosisResult = getFieldByHeader(row, headerIndex, "诊断结果");
            String crowdCategory = resolveSpecialDiseaseCrowdCategory(row, headerIndex);
            String currentUnit = getFieldByHeader(row, headerIndex, "现管理单位", "现管单位", "首管理单位");

            // 将全部专病网导入字段存入 epidemicData JSON
            Map<String, String> extraFields = buildNamedRowFields(row, headerIndex);
            if (StrUtil.isNotBlank(crowdCategory)) extraFields.put("人群分类", crowdCategory);
            if (StrUtil.isNotBlank(currentUnit)) {
                extraFields.put("现管单位", currentUnit);
                extraFields.put("现管理单位", currentUnit);
            }
            String extraJson = null;
            try {
                extraJson = objectMapper.writeValueAsString(extraFields);
            } catch (Exception ignored) {}

            java.time.LocalDate birthDate = null;
            if (StrUtil.isNotBlank(birthDateStr)) {
                birthDate = FlexibleDateParseUtil.parse(birthDateStr);
            }
            Integer age = null;
            if (StrUtil.isNotBlank(ageStr)) {
                try { age = Integer.parseInt(ageStr.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }

            Patient patient = Patient.builder()
                    .populationType("specialDisease")
                    .source("specialDisease")
                    .name(name)
                    .idType("居民身份证")
                    .idNumber(idNumber)
                    .gender(gender)
                    .birthDate(birthDate)
                    .age(age)
                    .phone(phone)
                    .currentAddress(currentAddress)
                    .householdAddress(householdAddress)
                    .diagnosisResult(diagnosisResult)
                    .epidemicData(extraJson)
                    .archived(0)
                    .importRowNo(importRowNo)
                    .departmentId(BaseContext.getCurrentDepartmentId())
                    .creatorId(BaseContext.getCurrentId())
                    .build();

            save(patient);
            count++;
        }

        log.info("专病表导入完成：成功创建 {} 条患者记录", count);
        return count;
    }

    @Override
    public Patient getDetail(Long id) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者记录不存在");
        }
        fillNoticeStatus(List.of(patient), patient.getPopulationType());
        fillFirstVisitStatus(List.of(patient));
        fillFollowUpCount(List.of(patient));
        fillMedicationManagementStatus(List.of(patient));
        fillMedicationPickupSummary(List.of(patient));
        fillScreeningXrayData(List.of(patient), patient.getPopulationType());
        fillEpidemicExtraFields(List.of(patient));
        return patient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBasicInfo(Long id, Map<String, Object> body) {
        dataScopeHelper.assertPatientAccessible(id);
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者记录不存在");
        }
        assertPatientNotTransferLocked(patient);
        if (body.get("populationType") != null) {
            String populationType = body.get("populationType").toString().trim();
            if (StrUtil.isNotBlank(populationType)) {
                if (!MANUAL_POPULATION_TYPES.contains(populationType)) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的数据来源");
                }
                patient.setPopulationType(populationType);
            }
        }
        if (body.get("name") != null) patient.setName(body.get("name").toString());
        if (body.get("gender") != null) patient.setGender(body.get("gender").toString());
        if (body.containsKey("birthDate")) {
            String bd = body.get("birthDate") == null ? "" : body.get("birthDate").toString();
            patient.setBirthDate(StrUtil.isNotBlank(bd) ? FlexibleDateParseUtil.parse(bd) : null);
        }
        if (body.containsKey("age")) {
            Object ageVal = body.get("age");
            patient.setAge(ageVal == null || "".equals(String.valueOf(ageVal)) ? null : Integer.valueOf(ageVal.toString()));
        }
        if (body.get("idType") != null) patient.setIdType(body.get("idType").toString());
        if (body.get("idNumber") != null) {
            String idNumber = body.get("idNumber").toString().trim();
            if (StrUtil.isNotBlank(idNumber) && !isValidIdCard(idNumber)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
            }
            patient.setIdNumber(idNumber);
        }
        if (body.get("ethnicity") != null) patient.setEthnicity(body.get("ethnicity").toString());
        if (body.get("phone") != null) {
            String phone = body.get("phone").toString().trim();
            if (StrUtil.isNotBlank(phone) && !isValidPhone(phone)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
            }
            patient.setPhone(phone);
        }
        if (body.get("householdAddress") != null) patient.setHouseholdAddress(body.get("householdAddress").toString());
        if (body.get("currentAddress") != null) patient.setCurrentAddress(body.get("currentAddress").toString());
        if (body.get("diagnosisResult") != null) patient.setDiagnosisResult(body.get("diagnosisResult").toString());
        mergeEpidemicExtraFields(patient, body);
        updateById(patient);
        updateLinkedScreening(patient, body);
    }

    /** 合并手动录入扩展字段到 epidemicData JSON */
    private void mergeEpidemicExtraFields(Patient patient, Map<String, Object> body) {
        boolean hasManualField = MANUAL_EPIDEMIC_MAPPINGS.stream()
                .anyMatch(pair -> body.containsKey(pair[0]));
        if (!hasManualField) {
            return;
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(patient.getEpidemicData())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> existing = objectMapper.readValue(patient.getEpidemicData(), Map.class);
                extra.putAll(existing);
            } catch (Exception ignored) {
                // 原 JSON 无效时重建
            }
        }
        for (String[] pair : MANUAL_EPIDEMIC_MAPPINGS) {
            String bodyKey = pair[0];
            String jsonKey = pair[1];
            if (!body.containsKey(bodyKey)) {
                continue;
            }
            String value = body.get(bodyKey) == null ? "" : body.get(bodyKey).toString().trim();
            if (StrUtil.isNotBlank(value)) {
                extra.put(jsonKey, value);
            } else {
                extra.remove(jsonKey);
            }
        }
        // 培养结果手动录入后统一写入「培养结果」，避免与专病网「0月序培养结果」双键并存
        if (body.containsKey("cultureResult")) {
            String culture = body.get("cultureResult") == null ? "" : body.get("cultureResult").toString().trim();
            if (StrUtil.isNotBlank(culture)) {
                extra.put("培养结果", culture);
                extra.remove("0月序培养结果");
            } else {
                extra.remove("培养结果");
                extra.remove("0月序培养结果");
            }
        }
        try {
            patient.setEpidemicData(extra.isEmpty() ? null : objectMapper.writeValueAsString(extra));
        } catch (Exception e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "扩展字段保存失败");
        }
    }

    /** 同步更新关联筛查记录中的筛查/胸片字段 */
    private void updateLinkedScreening(Patient patient, Map<String, Object> body) {
        if (patient.getScreeningId() == null) return;
        Long screeningId = patient.getScreeningId();
        String populationType = patient.getPopulationType();
        LocalDate screenDate = body.containsKey("screenDate") ? parseLocalDateField(body.get("screenDate")) : null;
        String screenMethod = body.containsKey("screenMethod") ? stringField(body.get("screenMethod")) : null;
        String infectionResult = body.containsKey("infectionResult") ? stringField(body.get("infectionResult")) : null;
        LocalDate chestXrayDate = body.containsKey("chestXrayDate") ? parseLocalDateField(body.get("chestXrayDate")) : null;
        String chestXrayResult = body.containsKey("chestXrayResult") ? stringField(body.get("chestXrayResult")) : null;

        if ("school".equals(populationType)) {
            ScreeningSchool screening = screeningSchoolMapper.selectById(screeningId);
            if (screening == null) return;
            if (body.containsKey("screenDate")) screening.setScreenDate(screenDate);
            if (body.containsKey("screenMethod")) screening.setScreenMethod(screenMethod);
            if (body.containsKey("infectionResult")) screening.setInfectionResult(infectionResult);
            if (body.containsKey("chestXrayDate")) screening.setChestXrayDate(chestXrayDate);
            if (body.containsKey("chestXrayResult")) screening.setChestXrayResult(chestXrayResult);
            screeningSchoolMapper.updateById(screening);
        } else if ("keyPopulation".equals(populationType) || "regular".equals(populationType)) {
            ScreeningKeyPopulation screening = screeningKeyPopulationMapper.selectById(screeningId);
            if (screening == null) return;
            if (body.containsKey("screenDate")) screening.setScreenDate(screenDate);
            if (body.containsKey("screenMethod")) screening.setScreenMethod(screenMethod);
            if (body.containsKey("infectionResult")) screening.setInfectionResult(infectionResult);
            if (body.containsKey("chestXrayDate")) screening.setChestXrayDate(chestXrayDate);
            if (body.containsKey("chestXrayResult")) screening.setChestXrayResult(chestXrayResult);
            screeningKeyPopulationMapper.updateById(screening);
        } else if ("closeContact".equals(populationType)) {
            ScreeningCloseContact screening = screeningCloseContactMapper.selectById(screeningId);
            if (screening == null) return;
            if (body.containsKey("chestXrayDate")) screening.setImagingDate(chestXrayDate);
            if (body.containsKey("chestXrayResult")) screening.setImagingResult(chestXrayResult);
            screeningCloseContactMapper.updateById(screening);
        }
    }

    private LocalDate parseLocalDateField(Object val) {
        return FlexibleDateParseUtil.parse(val);
    }

    private String stringField(Object val) {
        return val == null ? null : val.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(Map<String, Object> body) {
        String name = body.getOrDefault("name", "").toString().trim();
        String idNumber = body.getOrDefault("idNumber", "").toString().trim();
        String populationType = body.getOrDefault("populationType", "").toString().trim();
        String phone = body.getOrDefault("phone", "").toString().trim();

        if (StrUtil.isBlank(name)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "姓名不能为空");
        }
        if (StrUtil.isBlank(idNumber)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "证件号不能为空");
        }
        if (!isValidIdCard(idNumber)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(phone) && !isValidPhone(phone)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }
        if (!MANUAL_POPULATION_TYPES.contains(populationType)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无效的数据来源");
        }

        Patient patient = Patient.builder()
                .populationType(populationType)
                .name(name)
                .idNumber(idNumber)
                .phone(phone)
                .gender(body.getOrDefault("gender", "").toString())
                .age(parseIntegerField(body.get("age")))
                .idType(body.getOrDefault("idType", "居民身份证").toString())
                .ethnicity(body.getOrDefault("ethnicity", "").toString())
                .householdAddress(body.getOrDefault("householdAddress", "").toString())
                .currentAddress(body.getOrDefault("currentAddress", "").toString())
                .diagnosisResult(body.getOrDefault("diagnosisResult", "").toString())
                .source("manual")
                .archived(0)
                .departmentId(BaseContext.getCurrentDepartmentId())
                .creatorId(BaseContext.getCurrentId())
                .build();

        String birthDate = body.getOrDefault("birthDate", "").toString().trim();
        if (StrUtil.isNotBlank(birthDate)) {
            patient.setBirthDate(FlexibleDateParseUtil.parse(birthDate));
        }

        save(patient);
        mergeEpidemicExtraFields(patient, body);
        if (StrUtil.isNotBlank(patient.getEpidemicData())) {
            updateById(patient);
        }
        log.info("手动新增在管患者 id={}, populationType={}", patient.getId(), populationType);
        return patient.getId();
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
                    log.info("在管患者批量导入解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败：" + e.getMessage());
        }

        if (allRows.size() < 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }

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
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) {
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
            String idNumber = normalizeExcelCellText(getImportField(row, headerIndex, "证件号"));
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) {
                continue;
            }
            if (ImportIdentitySupport.isMissingBasicIdentity(name, idNumber)) {
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
                String idNumber = normalizeExcelCellText(getImportField(row, headerIndex, "证件号"));
                if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber)) {
                    continue;
                }
                if (ImportIdentitySupport.isMissingBasicIdentity(name, idNumber)) {
                    continue;
                }

                String populationTypeRaw = getImportField(row, headerIndex, "数据来源");
                String populationType = resolvePopulationType(populationTypeRaw);
                String phone = normalizeExcelCellText(getImportField(row, headerIndex, "联系电话"));

                boolean hasError = false;
                if (!isValidIdCard(idNumber)) {
                    result.addError(rowNum, name, "身份证号格式不正确");
                    hasError = true;
                }
                if (StrUtil.isBlank(populationType)) {
                    result.addError(rowNum, name, "数据来源无效");
                    hasError = true;
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

                LambdaQueryWrapper<Patient> dupWrapper = new LambdaQueryWrapper<>();
                dupWrapper.eq(Patient::getIdNumber, idNumber)
                        .eq(Patient::getPopulationType, populationType)
                        .eq(Patient::getArchived, 0);
                dataScopeHelper.applyImportDedupScope(
                        dupWrapper, Patient::getDepartmentId, Patient::getCreatorId);
                if (count(dupWrapper) > 0) {
                    result.addError(rowNum, name, "该证件号在此数据来源下已存在");
                    continue;
                }

                Map<String, String> epidemicFields = buildEpidemicFieldsFromImportRow(row, headerIndex);
                String epidemicJson = epidemicFields.isEmpty()
                        ? null
                        : objectMapper.writeValueAsString(epidemicFields);

                Patient patient = Patient.builder()
                        .populationType(populationType)
                        .name(name)
                        .idNumber(idNumber)
                        .phone(phone)
                        .gender(getImportField(row, headerIndex, "性别"))
                        .age(parseIntegerField(getImportField(row, headerIndex, "年龄")))
                        .idType(StrUtil.blankToDefault(getImportField(row, headerIndex, "证件类型"), "居民身份证"))
                        .ethnicity(getImportField(row, headerIndex, "民族"))
                        .householdAddress(getImportField(row, headerIndex, "户籍地址"))
                        .currentAddress(getImportField(row, headerIndex, "现住址"))
                        .diagnosisResult(getImportField(row, headerIndex, "诊断结果"))
                        .epidemicData(epidemicJson)
                        .source("manual")
                        .archived(0)
                        .importRowNo(rowNum)
                        .departmentId(BaseContext.getCurrentDepartmentId())
                        .creatorId(BaseContext.getCurrentId())
                        .build();

                String birthDate = getImportField(row, headerIndex, "出生日期");
                if (StrUtil.isNotBlank(birthDate)) {
                    LocalDate parsedBirthDate = parseLocalDateField(birthDate);
                    if (parsedBirthDate == null) {
                        result.addError(rowNum, name, "出生日期格式不正确");
                        continue;
                    }
                    patient.setBirthDate(parsedBirthDate);
                }

                save(patient);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.addError(rowNum, getImportField(row, headerIndex, "姓名"), "数据解析失败：" + e.getMessage());
            }
        }

        if (result.getSuccessCount() == 0 && result.getErrors().isEmpty()) {
            result.addError(0, "", "未找到有效数据行，请确认已填写姓名和证件号");
        }

        log.info("在管患者批量导入完成，成功 {} 条，错误 {} 条", result.getSuccessCount(), result.getErrors().size());
        return result;
    }

    private Map<String, String> buildEpidemicFieldsFromImportRow(
            Map<Integer, String> row, Map<String, Integer> headerIndex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String header : PatientManualImportHeaders.FIELDS) {
            if ("数据来源".equals(header) || "姓名".equals(header) || "性别".equals(header)
                    || "出生日期".equals(header) || "年龄".equals(header) || "证件类型".equals(header)
                    || "证件号".equals(header) || "民族".equals(header) || "联系电话".equals(header)
                    || "户籍地址".equals(header) || "现住址".equals(header) || "诊断结果".equals(header)) {
                continue;
            }
            String value = getImportField(row, headerIndex, header);
            if ("联系人监护人电话号码".equals(header)) {
                value = normalizeExcelCellText(value);
            }
            if (StrUtil.isNotBlank(value)) {
                fields.put(header, value);
            }
        }
        return fields;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePatients(List<Long> ids) {
        for (Long id : ids) {
            deletePatient(id);
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

    private String resolvePopulationType(String raw) {
        if (StrUtil.isBlank(raw)) return "";
        String v = raw.trim();
        if (MANUAL_POPULATION_TYPES.contains(v)) return v;
        return switch (v) {
            case "学生筛查" -> "school";
            case "重点人群" -> "keyPopulation";
            case "疫情筛查", "常规筛查" -> "regular";
            case "大疫情" -> "epidemic";
            case "推介" -> "referral";
            case "密接" -> "closeContact";
            case "专病网" -> "specialDisease";
            default -> "";
        };
    }
}
