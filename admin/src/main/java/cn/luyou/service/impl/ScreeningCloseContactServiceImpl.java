package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.Referral;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.SysMessage;
import cn.luyou.mapper.ScreeningCloseContactMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.MedicationPickupService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralService;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.ColumnFilterSupport;
import cn.luyou.utils.CreatorUserSupport;
import cn.luyou.utils.IdentityFormatFilterSupport;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import cn.luyou.utils.ImportDuplicateIdSupport;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.UploadBatchSupport;
import cn.luyou.utils.CloseContactCaseExcelDerivedSupport;
import cn.luyou.utils.CloseContactCaseExcelSupport;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.ScreeningScopeHelper;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 密接人群筛查 Service（官方 72 列模板，基于 finalScreeningResult 分类）
 *
 * 分类规则（AD列 = final_screening_result）：
 *  - 活动性肺结核  → ccStatus=1，标红结案（不进入患者管理）
 *  - 疑似结核    → ccStatus=9，标黄结案
 *  - 潜伏感染者    → ccStatus=2，进入密接潜伏感染专属流程
 *  - 未做          → ccStatus=4，进入6/12/24月随访监测
 *  - 未发现异常    → ccStatus=6，进入3月复查流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningCloseContactServiceImpl extends ServiceImpl<ScreeningCloseContactMapper, ScreeningCloseContact>
        implements ScreeningCloseContactService {

    private final DepartmentService departmentService;
    private final PatientService patientService;
    private final NoticeService noticeService;
    private final SupervisionFormService supervisionFormService;
    private final FirstVisitService firstVisitService;
    private final FollowUpVisitService followUpVisitService;
    private final MedicationManagementService medicationManagementService;
    private final MedicationPickupService medicationPickupService;
    private final EpidemicReportService epidemicReportService;
    private final SysMessageService sysMessageService;
    private final ReferralService referralService;
    private final ScreeningScopeHelper screeningScopeHelper;
    private final UserMapper userMapper;

    private static final Set<String> COLUMN_FILTER_WHITELIST = Set.of(
            "name", "year", "city", "district", "gender", "idNumber", "phone", "ethnicity",
            "currentAddress", "householdAddress", "sourcePatientName", "contactType", "contactPlace",
            "finalScreeningResult", "infectionCheckResult", "imagingResult", "sputumCheckResult",
            "hasPreventiveTreatment", "remark", "creatorUsername", "phoneContactRelation"
    );
    private static final Set<String> COLUMN_FILTER_EQ_FIELDS = Set.of(
            "gender", "year", "city", "district", "ethnicity", "contactType",
            "finalScreeningResult", "infectionCheckResult", "imagingResult", "sputumCheckResult",
            "hasPreventiveTreatment"
    );

    /** 活动性肺结核的最终筛查结果标识（模板中的文字） */
    private static final String RESULT_ACTIVE_TB = "活动性肺结核";
    /** 疑似结核 */
    private static final String RESULT_SUSPECTED_TB = ScreeningDiagnosisSupport.SUSPECTED_TB_DIAGNOSIS;
    /** 潜伏感染者 */
    private static final String RESULT_LATENT = "潜伏感染者";
    /** 未做 */
    private static final String RESULT_NOT_DONE = "未做";
    /** 未发现异常 */
    private static final String RESULT_NORMAL = "未发现异常";

    // ==================== 上传与导入 ====================

    @Override
    public ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid) {
        return uploadAndParse(file, confirmSkipInvalid, false);
    }

    @Override
    public ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile) {
        String batchId = UploadBatchSupport.newBatchId("密接筛查");
        final List<ScreeningCloseContact> parsedList = new ArrayList<>();
        ImportResult result = new ImportResult();
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }
        int headRowNumber;
        try {
            headRowNumber = CloseContactCaseExcelSupport.resolveHeadRowNumber(fileBytes);
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }
        AtomicInteger rowNum = new AtomicInteger(headRowNumber + 1);
        final CreatorUserSupport.CreatorSnapshot creator = CreatorUserSupport.resolveCurrentCreator(userMapper);
        final Long uploadDepartmentId = screeningScopeHelper.resolveUploadDepartmentId();

        try {
            EasyExcel.read(new java.io.ByteArrayInputStream(fileBytes), ScreeningCloseContact.class, new ReadListener<ScreeningCloseContact>() {
                @Override
                public void invoke(ScreeningCloseContact data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    if (isBlankCloseContactRow(data)) {
                        return;
                    }
                    if (ImportIdentitySupport.registerInvalidIdentity(
                            result, row, data.getName(), data.getIdNumber(), confirmSkipInvalid)) {
                        return;
                    }
                    if (ImportIdentitySupport.isMissingBasicIdentity(data.getName(), data.getIdNumber())) {
                        return;
                    }
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "接触者身份证号格式不正确");
                    }
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "接触者手机号格式不正确");
                    }
                    // 从登记日期提取年份
                    if (data.getRegistrationDate() != null) {
                        data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
                    }
                    data.setUploadBatch(batchId);
                    data.setImportRowNo(row);
                    CreatorUserSupport.applyCreator(creator, data::setCreatorId, data::setCreatorUsername);
                    data.setDepartmentId(uploadDepartmentId);
                    parsedList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("密接人群筛查数据解析完成，共 {} 条", parsedList.size());
                }
            }).sheet().headRowNumber(headRowNumber).doRead();
        } catch (Exception e) {
            log.error("密接筛查 Excel 解析失败", e);
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "Excel解析失败，请使用系统导出的模板或标准密接表（含表头）: " + e.getMessage());
        }

        if (ImportIdentitySupport.shouldBlockImport(result, confirmSkipInvalid)) {
            return result;
        }

        List<ScreeningCloseContact> dataList = ImportDuplicateIdSupport.handleDuplicateInFile(
                result,
                parsedList,
                d -> ImportDuplicateIdSupport.normalizeIdNumber(d.getIdNumber()),
                ScreeningCloseContact::getImportRowNo,
                ScreeningCloseContact::getIdNumber,
                ScreeningCloseContact::getName,
                confirmSkipDuplicateInFile);
        if (dataList == null) {
            return result;
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        // 增量导入：按接触者身份证号匹配已有记录（同一人多次导入时合并随访数据）
        List<ScreeningCloseContact> toInsert = new ArrayList<>();
        List<ScreeningCloseContact> toUpdate = new ArrayList<>();

        for (ScreeningCloseContact d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                determineStatus(d);
                if (Integer.valueOf(4).equals(d.getCcStatus())) {
                    ensureFollowupDueDates(d);
                }
                toInsert.add(d);
                continue;
            }
            ScreeningCloseContact existing = null;
            if (StrUtil.isNotBlank(d.getIdNumber())) {
                LambdaQueryWrapper<ScreeningCloseContact> dupWrapper = new LambdaQueryWrapper<>();
                dupWrapper.eq(ScreeningCloseContact::getIdNumber, d.getIdNumber()).last("LIMIT 1");
                screeningScopeHelper.applyImportDedupScope(dupWrapper, ScreeningCloseContact::getDepartmentId);
                existing = getOne(dupWrapper, false);
            }
            if (existing != null) {
                mergeFollowupData(existing, d);
                determineStatus(existing);
                if (Integer.valueOf(4).equals(existing.getCcStatus())) {
                    ensureFollowupDueDates(existing);
                }
                toUpdate.add(existing);
            } else {
                determineStatus(d);
                if (Integer.valueOf(4).equals(d.getCcStatus())) {
                    ensureFollowupDueDates(d);
                }
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        // 活动性肺结核（确诊）仅标记 ccStatus，不自动创建患者管理记录（患者管理数据仅来自专病信息表导入）
        result.setSuccessCount(dataList.size());
        return result;
    }

    /**
     * 基于随访数据合并（同一接触者多次导入时，补全后续月份随访字段）
     */
    private void mergeFollowupData(ScreeningCloseContact existing, ScreeningCloseContact incoming) {
        // 原患者信息可能更新
        if (StrUtil.isNotBlank(incoming.getSourcePatientName())) existing.setSourcePatientName(incoming.getSourcePatientName());

        // 基本信息始终以最新为准
        if (StrUtil.isNotBlank(incoming.getName())) existing.setName(incoming.getName());
        if (StrUtil.isNotBlank(incoming.getPhone())) existing.setPhone(incoming.getPhone());
        if (StrUtil.isNotBlank(incoming.getPhoneContactRelation())) existing.setPhoneContactRelation(incoming.getPhoneContactRelation());
        if (StrUtil.isNotBlank(incoming.getContactType())) existing.setContactType(incoming.getContactType());
        if (StrUtil.isNotBlank(incoming.getContactPlace())) existing.setContactPlace(incoming.getContactPlace());
        if (StrUtil.isNotBlank(incoming.getContactPlaceOther())) existing.setContactPlaceOther(incoming.getContactPlaceOther());
        if (StrUtil.isNotBlank(incoming.getCurrentAddress())) existing.setCurrentAddress(incoming.getCurrentAddress());

        // 初次筛查（若已有则保留，以防覆盖旧数据）
        if (StrUtil.isNotBlank(incoming.getFinalScreeningResult())) {
            existing.setFinalScreeningResult(
                    ScreeningDiagnosisSupport.normalizeDiagnosis(incoming.getFinalScreeningResult()));
            existing.setInfectionCheckResult(incoming.getInfectionCheckResult());
            existing.setImagingResult(incoming.getImagingResult());
            existing.setSputumCheckResult(incoming.getSputumCheckResult());
        }

        // 预防治疗情况
        if (StrUtil.isNotBlank(incoming.getHasPreventiveTreatment())) existing.setHasPreventiveTreatment(incoming.getHasPreventiveTreatment());
        if (StrUtil.isNotBlank(incoming.getPreventivePlan())) existing.setPreventivePlan(incoming.getPreventivePlan());
        if (StrUtil.isNotBlank(incoming.getTreatmentCompleted())) existing.setTreatmentCompleted(incoming.getTreatmentCompleted());
        if (StrUtil.isNotBlank(incoming.getIncompleteReason())) existing.setIncompleteReason(incoming.getIncompleteReason());

        // 6月随访
        if (StrUtil.isNotBlank(incoming.getFollowup6Result())) {
            existing.setFollowup6DueDate(incoming.getFollowup6DueDate());
            existing.setFollowup6ScreenDate(incoming.getFollowup6ScreenDate());
            existing.setFollowup6Symptom1(incoming.getFollowup6Symptom1());
            existing.setFollowup6ImagingResult(incoming.getFollowup6ImagingResult());
            existing.setFollowup6SputumResult(incoming.getFollowup6SputumResult());
            existing.setFollowup6Result(incoming.getFollowup6Result());
        }
        // 12月随访
        if (StrUtil.isNotBlank(incoming.getFollowup12Result())) {
            existing.setFollowup12DueDate(incoming.getFollowup12DueDate());
            existing.setFollowup12ScreenDate(incoming.getFollowup12ScreenDate());
            existing.setFollowup12Symptom1(incoming.getFollowup12Symptom1());
            existing.setFollowup12ImagingResult(incoming.getFollowup12ImagingResult());
            existing.setFollowup12SputumResult(incoming.getFollowup12SputumResult());
            existing.setFollowup12Result(incoming.getFollowup12Result());
        }
        // 24月随访
        if (StrUtil.isNotBlank(incoming.getFollowup24Result())) {
            existing.setFollowup24DueDate(incoming.getFollowup24DueDate());
            existing.setFollowup24ScreenDate(incoming.getFollowup24ScreenDate());
            existing.setFollowup24Symptom1(incoming.getFollowup24Symptom1());
            existing.setFollowup24ImagingResult(incoming.getFollowup24ImagingResult());
            existing.setFollowup24SputumResult(incoming.getFollowup24SputumResult());
            existing.setFollowup24Result(incoming.getFollowup24Result());
        }

        if (StrUtil.isNotBlank(incoming.getRemark())) existing.setRemark(incoming.getRemark());
        existing.setUploadBatch(incoming.getUploadBatch());
        if (incoming.getImportRowNo() != null) existing.setImportRowNo(incoming.getImportRowNo());
        // 覆盖导入只更新业务字段与行号，保留首次录入人；历史空值则补当前导入人
        CreatorUserSupport.fillMissingCreator(
                existing.getCreatorId(),
                existing.getCreatorUsername(),
                new CreatorUserSupport.CreatorSnapshot(incoming.getCreatorId(), incoming.getCreatorUsername()),
                existing::setCreatorId,
                existing::setCreatorUsername);
        existing.setDepartmentId(incoming.getDepartmentId());
    }

    /**
     * 根据 finalScreeningResult 设置系统流程状态 ccStatus
     * 注意：若当前 ccStatus 已经处于 "进行中/已归档" 等高级状态，则不降级（仅对初始状态赋值）
     */
    private void determineStatus(ScreeningCloseContact data) {
        String result = data.getFinalScreeningResult();
        // 活动性/疑似肺结核：始终结案（标红/标黄），允许从待处理状态升级
        if (RESULT_ACTIVE_TB.equals(result)) {
            data.setCcStatus(1);
            return;
        }
        if (ScreeningDiagnosisSupport.isSuspectedTbDiagnosis(result)) {
            data.setCcStatus(9);
            return;
        }
        if (data.getCcStatus() != null && data.getCcStatus() > 0) return; // 已有业务状态不覆盖
        if (RESULT_LATENT.equals(result)) {
            data.setCcStatus(2); // 潜伏感染者-管理中
        } else if (RESULT_NOT_DONE.equals(result)) {
            data.setCcStatus(4); // 随访监测中
            ensureFollowupDueDates(data);
        } else if (RESULT_NORMAL.equals(result)) {
            data.setCcStatus(6); // 未发现异常-待3月复查
        } else {
            data.setCcStatus(0); // 待处理
        }
    }

    private Patient buildPatient(ScreeningCloseContact d) {
        return Patient.builder()
                .screeningId(d.getId())
                .populationType("closeContact")
                .name(d.getName())
                .gender(d.getGender())
                .age(d.getAge())
                .idNumber(d.getIdNumber())
                .phone(d.getPhone())
                .householdAddress(d.getHouseholdAddress())
                .currentAddress(d.getCurrentAddress())
                .diagnosisResult("活动性肺结核")
                .source("confirmed")
                .archived(0)
                .departmentId(d.getDepartmentId())
                .build();
    }

    // ==================== 查询 ====================

    /** 诊断结果筛选：支持统一选项（排除/正常/疑似等）映射为 finalScreeningResult */
    private void applyFinalScreeningResultFilter(LambdaQueryWrapper<ScreeningCloseContact> wrapper,
                                                 String finalScreeningResult) {
        if (StrUtil.isBlank(finalScreeningResult)) {
            return;
        }
        List<String> mapped = ScreeningDiagnosisSupport.resolveCloseContactDiagnosisFilterValues(finalScreeningResult);
        if (mapped.isEmpty()) {
            wrapper.eq(ScreeningCloseContact::getFinalScreeningResult, finalScreeningResult);
        } else if (mapped.size() == 1) {
            wrapper.eq(ScreeningCloseContact::getFinalScreeningResult, mapped.get(0));
        } else {
            wrapper.in(ScreeningCloseContact::getFinalScreeningResult, mapped);
        }
    }

    @Override
    public IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                                   String district, Integer ccStatus, String finalScreeningResult,
                                                   String phone, String dateFrom, String dateTo,
                                                   String createTimeFrom, String createTimeTo,
                                                   String creatorUsername, String columnFilters, String formatIssue) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = buildListWrapper(
                name, idNumber, district, ccStatus, finalScreeningResult, phone, dateFrom, dateTo,
                createTimeFrom, createTimeTo, creatorUsername, columnFilters, formatIssue);
        ImportRowOrderSupport.applyWithBatch(wrapper);
        IPage<ScreeningCloseContact> result = page(new Page<>(page, size), wrapper);

        // 补充通知单发送状态，用于前端控制"发送通知单"按钮的显示
        List<ScreeningCloseContact> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            CreatorUserSupport.fillMissingUsernames(
                    userMapper,
                    records,
                    ScreeningCloseContact::getCreatorId,
                    ScreeningCloseContact::getCreatorUsername,
                    ScreeningCloseContact::setCreatorUsername);
            List<Long> ids = records.stream()
                    .map(ScreeningCloseContact::getId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            Set<Long> sentBizIds = new HashSet<>();
            if (!ids.isEmpty()) {
                sentBizIds.addAll(noticeService.lambdaQuery()
                        .in(Notice::getBizId, ids)
                        .eq(Notice::getNoticeType, "latent")
                        .eq(Notice::getPopulationType, "closeContact")
                        .ge(Notice::getStatus, 1)
                        .list()
                        .stream().map(Notice::getBizId).toList());
            }
            records.forEach(r -> r.setNoticeSent(sentBizIds.contains(r.getId())));
            // 补全随访到期日（含潜伏感染者未完成治疗转入随访监测的记录）
            List<ScreeningCloseContact> dueDateUpdates = new ArrayList<>();
            for (ScreeningCloseContact r : records) {
                if (r.getCcStatus() != null && (r.getCcStatus() == 4 || r.getCcStatus() == 5)) {
                    if (ensureFollowupDueDates(r)) {
                        dueDateUpdates.add(r);
                    }
                }
            }
            if (!dueDateUpdates.isEmpty()) {
                updateBatchById(dueDateUpdates, 500);
            }
            CloseContactCaseExcelDerivedSupport.applyAllScreening(records);
        }
        return result;
    }

    @Override
    public List<ScreeningCloseContact> listForExport(String name, String idNumber, String district,
                                                      Integer ccStatus, String finalScreeningResult, String phone,
                                                      String dateFrom, String dateTo, String createTimeFrom,
                                                      String createTimeTo, String creatorUsername,
                                                      String columnFilters, String formatIssue, List<Long> ids) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper;
        if (ids != null && !ids.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ScreeningCloseContact::getId, ids);
            applyDepartmentScope(wrapper);
        } else {
            wrapper = buildListWrapper(
                    name, idNumber, district, ccStatus, finalScreeningResult, phone, dateFrom, dateTo,
                    createTimeFrom, createTimeTo, creatorUsername, columnFilters, formatIssue);
        }
        ImportRowOrderSupport.applyWithBatch(wrapper);
        return list(wrapper);
    }

    private LambdaQueryWrapper<ScreeningCloseContact> buildListWrapper(
            String name, String idNumber, String district, Integer ccStatus, String finalScreeningResult,
            String phone, String dateFrom, String dateTo, String createTimeFrom, String createTimeTo,
            String creatorUsername, String columnFilters, String formatIssue) {
        LocalDate screenFrom = QueryDateRangeUtil.parseLocalDate(dateFrom);
        LocalDate screenTo = QueryDateRangeUtil.parseLocalDate(dateTo);
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(createTimeFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(createTimeTo);
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningCloseContact::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningCloseContact::getIdNumber, idNumber)
                .eq(StrUtil.isNotBlank(district), ScreeningCloseContact::getDistrict, district)
                .like(StrUtil.isNotBlank(phone), ScreeningCloseContact::getPhone, phone)
                .eq(ccStatus != null, ScreeningCloseContact::getCcStatus, ccStatus)
                .like(StrUtil.isNotBlank(creatorUsername), ScreeningCloseContact::getCreatorUsername, creatorUsername);
        applyFinalScreeningResultFilter(wrapper, finalScreeningResult);
        applyColumnFilters(wrapper, columnFilters);
        wrapper.ge(screenFrom != null, ScreeningCloseContact::getFirstScreenDate, screenFrom)
                .le(screenTo != null, ScreeningCloseContact::getFirstScreenDate, screenTo)
                .ge(createFrom != null, ScreeningCloseContact::getCreateTime, createFrom)
                .le(createTo != null, ScreeningCloseContact::getCreateTime, createTo);
        applyDepartmentScope(wrapper);
        IdentityFormatFilterSupport.apply(wrapper, formatIssue, "id_number", "phone");
        return wrapper;
    }

    @Override
    public ScreeningCloseContact getEnrichedById(Long id) {
        ScreeningCloseContact record = getById(id);
        if (record == null) {
            return null;
        }
        if (record.getCcStatus() != null && (record.getCcStatus() == 4 || record.getCcStatus() == 5)) {
            if (ensureFollowupDueDates(record)) {
                updateById(record);
            }
        }
        CloseContactCaseExcelDerivedSupport.apply(record);
        return record;
    }

    /**
     * 根据登记日期补全 6/12/24 月随访到期日（仅补缺失项）。
     *
     * @return 是否发生了字段变更
     */
    private boolean ensureFollowupDueDates(ScreeningCloseContact record) {
        if (record.getRegistrationDate() == null) {
            return false;
        }
        boolean changed = false;
        if (record.getFollowup6DueDate() == null) {
            record.setFollowup6DueDate(record.getRegistrationDate().plusMonths(6));
            changed = true;
        }
        if (record.getFollowup12DueDate() == null) {
            record.setFollowup12DueDate(record.getRegistrationDate().plusMonths(12));
            changed = true;
        }
        if (record.getFollowup24DueDate() == null) {
            record.setFollowup24DueDate(record.getRegistrationDate().plusMonths(24));
            changed = true;
        }
        return changed;
    }

    // ==================== 单条增删改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScreening(ScreeningCloseContact data) {
        validateContactBasicInfo(data);
        validateFirstScreenInfo(data);
        if (StrUtil.isNotBlank(data.getFinalScreeningResult())) {
            data.setFinalScreeningResult(ScreeningDiagnosisSupport.normalizeDiagnosis(data.getFinalScreeningResult()));
        }
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        }
        determineStatus(data);
        data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
        CreatorUserSupport.fillCurrentCreator(userMapper, data::setCreatorId, data::setCreatorUsername);
        save(data);
    }

    private void applyColumnFilters(LambdaQueryWrapper<ScreeningCloseContact> wrapper, String columnFilters) {
        Map<String, String> filters = ColumnFilterSupport.parse(columnFilters);
        ColumnFilterSupport.applyLambda(filters, COLUMN_FILTER_WHITELIST, (field, value) -> {
            switch (field) {
                case "name" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getName, value);
                case "year" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getYear, value);
                case "city" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getCity, value);
                case "district" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getDistrict, value);
                case "gender" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getGender, value);
                case "idNumber" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getIdNumber, value);
                case "phone" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getPhone, value);
                case "ethnicity" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getEthnicity, value);
                case "currentAddress" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getCurrentAddress, value);
                case "householdAddress" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getHouseholdAddress, value);
                case "sourcePatientName" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getSourcePatientName, value);
                case "contactType" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getContactType, value);
                case "contactPlace" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getContactPlace, value);
                case "finalScreeningResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getFinalScreeningResult, value);
                case "infectionCheckResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getInfectionCheckResult, value);
                case "imagingResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getImagingResult, value);
                case "sputumCheckResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getSputumCheckResult, value);
                case "hasPreventiveTreatment" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningCloseContact::getHasPreventiveTreatment, value);
                case "remark" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getRemark, value);
                case "creatorUsername" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getCreatorUsername, value);
                case "phoneContactRelation" -> ColumnFilterSupport.like(wrapper, ScreeningCloseContact::getPhoneContactRelation, value);
                default -> { }
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScreening(ScreeningCloseContact data) {
        ScreeningCloseContact existing = getById(data.getId());
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        validateContactBasicInfo(data);
        validateFirstScreenInfo(data);
        if (StrUtil.isNotBlank(data.getFinalScreeningResult())) {
            data.setFinalScreeningResult(ScreeningDiagnosisSupport.normalizeDiagnosis(data.getFinalScreeningResult()));
        }
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        } else if (existing.getRegistrationDate() != null) {
            data.setRegistrationDate(existing.getRegistrationDate());
        }
        determineStatus(data);
        Integer effectiveCcStatus = data.getCcStatus() != null ? data.getCcStatus() : existing.getCcStatus();
        if (Integer.valueOf(4).equals(effectiveCcStatus)) {
            ensureFollowupDueDates(data);
        }
        // 录入用户与部门不可被前端覆盖
        data.setCreatorId(existing.getCreatorId());
        data.setCreatorUsername(existing.getCreatorUsername());
        data.setDepartmentId(existing.getDepartmentId());
        updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScreeningCascade(Long id) {
        doDeleteScreeningCascade(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCascade(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            doDeleteScreeningCascade(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByFilter(String name, String idNumber, String district, Integer ccStatus,
                               String finalScreeningResult, String phone, String dateFrom, String dateTo,
                               String createTimeFrom, String createTimeTo, String creatorUsername,
                               String columnFilters, String formatIssue) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = buildListWrapper(
                name, idNumber, district, ccStatus, finalScreeningResult, phone, dateFrom, dateTo,
                createTimeFrom, createTimeTo, creatorUsername, columnFilters, formatIssue);
        wrapper.select(ScreeningCloseContact::getId);
        List<Long> ids = list(wrapper).stream().map(ScreeningCloseContact::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        batchDeleteCascade(ids);
        return ids.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAll() {
        return deleteByFilter(null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private void doDeleteScreeningCascade(Long id) {
        ScreeningCloseContact existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        // 级联删除患者管理数据
        List<Patient> patientList = patientService.lambdaQuery()
                .eq(Patient::getScreeningId, id)
                .eq(Patient::getPopulationType, "closeContact")
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
        // 删除潜伏感染者的督导表等
        supervisionFormService.lambdaUpdate()
                .eq(cn.luyou.model.SupervisionForm::getLatentInfectionId, id).remove();
        // 删除密接筛查本体关联的通知单及消息（密接潜伏通知单 bizId 为筛查记录ID）
        deleteNoticeAndMessages(id, "latent");
        deleteReferralsAndMessages(id);
        removeById(id);
        log.info("级联删除密接人群筛查记录 id={}", id);
    }

    /** 删除指定业务ID的通知单及关联系统消息 */
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

    /** 删除指定业务ID的分级诊疗记录及关联系统消息 */
    private void deleteReferralsAndMessages(Long bizId) {
        List<Long> referralIds = referralService.lambdaQuery()
                .eq(Referral::getBizId, bizId)
                .list().stream().map(Referral::getId).toList();
        if (!referralIds.isEmpty()) {
            sysMessageService.lambdaUpdate().in(SysMessage::getBizId, referralIds).remove();
            referralService.lambdaUpdate().eq(Referral::getBizId, bizId).remove();
        }
    }

    // ==================== 密接专属业务操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setExpectedTreatmentEndDate(Long id, LocalDate expectedDate) {
        ScreeningCloseContact record = getById(id);
        if (record == null) throw new ServiceException(StatusEnum.PARAM_INVALID, "记录不存在");
        record.setExpectedTreatmentEndDate(expectedDate);
        updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTreatmentDone(Long id, boolean done) {
        ScreeningCloseContact record = getById(id);
        if (record == null) throw new ServiceException(StatusEnum.PARAM_INVALID, "记录不存在");
        if (done) {
            // 完成：归档
            record.setCcStatus(3);
            record.setTreatmentCompleted("是");
        } else {
            // 未完成：进入随访监测
            record.setCcStatus(4);
            record.setTreatmentCompleted("否");
            ensureFollowupDueDates(record);
        }
        updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitThreeMonthCheck(Long id, LocalDate checkDate, String checkResult, String finalResult) {
        ScreeningCloseContact record = getById(id);
        if (record == null) throw new ServiceException(StatusEnum.PARAM_INVALID, "记录不存在");
        record.setThreeMonthCheckDate(checkDate);
        record.setThreeMonthCheckResult(checkResult);
        record.setThreeMonthFinalResult(finalResult);
        if ("阴性".equals(finalResult)) {
            record.setCcStatus(7); // 3月复查阴性，结束
        } else if ("阳性".equals(finalResult)) {
            // 转入潜伏感染者流程
            record.setFinalScreeningResult(RESULT_LATENT);
            record.setCcStatus(2);
        }
        updateById(record);
    }

    @Override
    public Map<String, Long> countByFinalResult() {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ScreeningCloseContact::getFinalScreeningResult)
                .isNotNull(ScreeningCloseContact::getFinalScreeningResult);
        applyDepartmentScope(wrapper);
        List<ScreeningCloseContact> all = list(wrapper);
        return all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getFinalScreeningResult() == null ? "未分类" : s.getFinalScreeningResult(),
                        Collectors.counting()
                ));
    }

    /** 非超管按部门隔离；未绑定部门时仅看 department_id 为空的记录，避免 IN () SQL 异常 */
    private void applyDepartmentScope(LambdaQueryWrapper<ScreeningCloseContact> wrapper) {
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
    }

    // ==================== 工具方法 ====================

    private static final String CONTACT_PLACE_OTHER = "其他（需手工录入）";
    private static final String SCREENING_FIELD_OTHER = "其他（需手工录入）";

    /** 接触者基本信息必填校验（手动新增/编辑） */
    private void validateContactBasicInfo(ScreeningCloseContact data) {
        if (StrUtil.isBlank(data.getName())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "接触者姓名不能为空");
        }
        if (StrUtil.isBlank(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号不能为空");
        }
        if (StrUtil.isBlank(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "联系电话不能为空");
        }
        if (CONTACT_PLACE_OTHER.equals(data.getContactPlace()) && StrUtil.isBlank(data.getContactPlaceOther())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "接触场所选择「其他」时请填写具体内容");
        }
    }

    /** 初次筛查「其他」手工录入校验 */
    private void validateFirstScreenInfo(ScreeningCloseContact data) {
        requireOtherText(data.getImagingMethod(), data.getImagingMethodOther(), "影像方法");
        requireOtherText(data.getImagingResult(), data.getImagingResultOther(), "影像结果");
        requireOtherText(data.getSputumCheckMethod(), data.getSputumCheckMethodOther(), "痰检方法");
        requireOtherText(data.getSputumCheckResult(), data.getSputumCheckResultOther(), "痰检结果");
        requireOtherText(data.getFinalScreeningResult(), data.getFinalScreeningResultOther(), "最终筛查结果");
    }

    private void requireOtherText(String main, String other, String label) {
        if (SCREENING_FIELD_OTHER.equals(main) && StrUtil.isBlank(other)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, label + "选择「其他」时请填写具体内容");
        }
    }

    private boolean isValidIdCard(String id) {
        // 仅校验格式（18位 + 字符规则）。Excel 以数值型存储身份证号时会丢失浮点精度，导致校验码错误，故不做校验码验证。
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private boolean isBlankCloseContactRow(ScreeningCloseContact data) {
        return data == null || (StrUtil.isBlank(data.getName()) && StrUtil.isBlank(data.getIdNumber()));
    }
}
