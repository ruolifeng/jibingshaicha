package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.Referral;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.SysMessage;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.service.DepartmentService;
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
import cn.luyou.service.ScreeningSchoolService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.ColumnDistinctSupport;
import cn.luyou.utils.ColumnFilterSupport;
import cn.luyou.utils.CreatorUserSupport;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import cn.luyou.utils.ScreeningImportMergeSupport;
import cn.luyou.utils.FlexibleDateParseUtil;
import cn.luyou.utils.IdentityFormatFilterSupport;
import cn.luyou.utils.ImportDuplicateIdSupport;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.SchoolScreeningImportValidateSupport;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.ListSortSupport;
import cn.luyou.utils.UploadBatchSupport;
import cn.luyou.utils.ScreeningScopeHelper;
import cn.luyou.utils.SchoolScreeningCodeSupport;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningSchoolServiceImpl extends ServiceImpl<ScreeningSchoolMapper, ScreeningSchool>
        implements ScreeningSchoolService {

    private final DepartmentService departmentService;
    private final LatentInfectionService latentInfectionService;
    private final PatientService patientService;
    private final NoticeService noticeService;
    private final SupervisionFormService supervisionFormService;
    private final LatentFollowUpService latentFollowUpService;
    private final LatentCheckService latentCheckService;
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
            "name", "year", "city", "district", "township", "gender", "idNumber", "phone", "ethnicity",
            "reportingOrg", "schoolName", "gradeName", "className", "schoolType", "boardingType",
            "currentAddress", "householdAddress",
            "screenMethod", "infectionResult", "diagnosisFirst", "hasChestXray", "chestXrayMethod", "chestXrayResult",
            "sputumSmearResult", "molecularBiologyResult", "sputumCultureResult",
            "remark", "creatorUsername", "idType", "tbHistory", "closeContactHistory",
            "suspiciousSymptoms", "symptomCough", "symptomHemoptysis", "symptomOther",
            "hasInfectionScreen", "screenResult", "participatedScreening"
    );
    private static final Set<String> COLUMN_FILTER_EQ_FIELDS = Set.of(
            "gender", "year", "city", "district", "township", "ethnicity", "idType", "schoolType", "boardingType",
            "screenMethod", "infectionResult", "diagnosisFirst", "hasChestXray", "chestXrayMethod", "chestXrayResult",
            "tbHistory", "closeContactHistory", "suspiciousSymptoms", "hasInfectionScreen", "screenResult",
            "participatedScreening", "sputumCultureResult", "molecularBiologyResult"
    );

    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("importRowNo", "import_row_no"),
            Map.entry("createTime", "create_time"),
            Map.entry("name", "name"),
            Map.entry("district", "district"),
            Map.entry("screenDate", "screen_date"),
            Map.entry("age", "age"),
            Map.entry("idNumber", "id_number"),
            Map.entry("uploadBatch", "upload_batch"),
            Map.entry("year", "year"),
            Map.entry("city", "city"),
            Map.entry("phone", "phone"),
            Map.entry("schoolName", "school_name"),
            Map.entry("creatorUsername", "creator_username")
    );

    @Override
    public ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, false);
    }

    @Override
    public ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid) {
        return uploadAndParse(file, confirmSkipInvalid, false);
    }

    @Override
    public ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile) {
        String batchId = UploadBatchSupport.newBatchId("学校筛查");
        List<ScreeningSchool> dataList = new ArrayList<>();
        ImportResult result = new ImportResult();
        final CreatorUserSupport.CreatorSnapshot creator = CreatorUserSupport.resolveCurrentCreator(userMapper);
        final Long uploadDepartmentId = screeningScopeHelper.resolveUploadDepartmentId();

        try {
            List<Map<Integer, String>> rows = new ArrayList<>();
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> row, AnalysisContext context) {
                    rows.add(new LinkedHashMap<>(row));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("学校人群筛查原始数据解析完成，共 {} 行", rows.size());
                }
            }).sheet().headRowNumber(0).doRead();

            SchoolHeaderLayout layout = locateSchoolHeaderLayout(rows);
            Map<String, Integer> headerIndex = buildSchoolHeaderIndex(rows, layout);
            if (!headerIndex.containsKey("姓名") || !headerIndex.containsKey("证件号")) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "模板表头不正确，请使用学生筛查模板或先进行数据匹配");
            }
            for (int i = layout.dataStartRow(); i < rows.size(); i++) {
                int rowNum = i + 1;
                ScreeningSchool data = mapSchoolRow(rows.get(i), headerIndex);
                if (isBlankSchoolRow(data)) {
                    continue;
                }
                if (ImportIdentitySupport.registerInvalidIdentity(
                        result, rowNum, data.getName(), data.getIdNumber(), confirmSkipInvalid)) {
                    continue;
                }
                if (ImportIdentitySupport.isMissingBasicIdentity(data.getName(), data.getIdNumber())) {
                    continue;
                }
                data.setIdNumber(ImportIdentitySupport.normalizeIdNumber(data.getIdNumber()));
                if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                    result.addError(rowNum, data.getName(), "身份证号格式不正确");
                }
                if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                    result.addError(rowNum, data.getName(), "手机号格式不正确");
                }
                List<String> fieldErrors = SchoolScreeningImportValidateSupport.validate(data);
                if (!fieldErrors.isEmpty()) {
                    for (String err : fieldErrors) {
                        result.addError(rowNum, data.getName(), err);
                    }
                    continue;
                }
                data.setUploadBatch(batchId);
                data.setImportRowNo(rowNum);
                CreatorUserSupport.applyCreator(creator, data::setCreatorId, data::setCreatorUsername);
                data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
                data.setDepartmentId(uploadDepartmentId);
                dataList.add(data);
            }
            log.info("学校人群筛查数据解析完成，共 {} 条", dataList.size());
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        if (ImportIdentitySupport.shouldBlockImport(result, confirmSkipInvalid)) {
            return result;
        }

        dataList = ImportDuplicateIdSupport.handleDuplicateInFile(
                result,
                dataList,
                d -> ImportDuplicateIdSupport.normalizeIdNumber(d.getIdNumber()),
                ScreeningSchool::getImportRowNo,
                ScreeningSchool::getIdNumber,
                ScreeningSchool::getName,
                confirmSkipDuplicateInFile);
        if (dataList == null) {
            return result;
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        // 增量导入：按身份证号去重，同一人多次导入时更新而非重复插入
        List<ScreeningSchool> toInsert = new ArrayList<>();
        List<ScreeningSchool> toUpdate = new ArrayList<>();

        for (ScreeningSchool d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                toInsert.add(d);
                continue;
            }
            ScreeningSchool existing = null;
            if (StrUtil.isNotBlank(d.getIdNumber())) {
                LambdaQueryWrapper<ScreeningSchool> dupWrapper = new LambdaQueryWrapper<>();
                dupWrapper.eq(ScreeningSchool::getIdNumber, d.getIdNumber()).last("LIMIT 1");
                screeningScopeHelper.applyImportDedupScope(dupWrapper, ScreeningSchool::getDepartmentId);
                existing = getOne(dupWrapper, false);
            }
            if (existing != null) {
                // 合并基本信息，以最新导入为准
                mergeSchoolImportFields(existing, d);
                CreatorUserSupport.fillMissingCreator(
                        existing.getCreatorId(),
                        existing.getCreatorUsername(),
                        new CreatorUserSupport.CreatorSnapshot(d.getCreatorId(), d.getCreatorUsername()),
                        existing::setCreatorId,
                        existing::setCreatorUsername);
                existing.setIsLatent(shouldMarkLatent(existing) ? 1 : 0);
                toUpdate.add(existing);
            } else {
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        result.setSuccessCount(dataList.size());
        for (ScreeningSchool d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                ImportIdentitySupport.registerMissingIdWarning(
                        result, d.getImportRowNo() == null ? 0 : d.getImportRowNo(), d.getName());
            }
        }

        // 仅对新插入且需跟进的记录自动创建潜伏感染记录。
        // 筛查结果码 1（确诊患者）：仅筛查列表标红保留，不建潜伏记录。
        // 筛查结果码 3（潜伏感染者）：创建后自动分流进入潜伏感染管理。
        // 注意：diagnosisResult 对「待诊断」情形不在此处预填，需操作员在待诊断页确认；
        // 对已明确为潜伏感染者/确诊患者的导入结果，由 autoReferralForDirectDiagnosis 分流。
        List<LatentInfection> latentList = toInsert.stream()
                .filter(this::shouldCreateLatentRecord)
                .map(d -> LatentInfection.builder()
                            .screeningId(d.getId())
                            .populationType("school")
                            .name(d.getName())
                            .idNumber(d.getIdNumber())
                            .gender(d.getGender())
                            .age(d.getAge())
                            .phone(d.getPhone())
                            .infectionResult(d.getInfectionResult())
                            .trackingStatus(0)
                            .notInPlaceCount(0)
                            .archived(0)
                            .hasChestXray(d.getHasChestXray())
                            .chestXrayDate(d.getChestXrayDate())
                            .chestXrayResult(d.getChestXrayResult())
                            .diagnosisFirst(latentDiagnosisFirst(d))
                            .departmentId(d.getDepartmentId())
                            .creatorId(BaseContext.getCurrentId())
                            .build())
                .toList();
        // 更新的记录中，若需建潜伏且尚无记录，则补创建
        List<LatentInfection> latentFromUpdated = toUpdate.stream()
                .filter(this::shouldCreateLatentRecord)
                .filter(d -> !latentInfectionService.lambdaQuery()
                        .eq(LatentInfection::getScreeningId, d.getId())
                        .eq(LatentInfection::getPopulationType, "school")
                        .exists())
                .map(d -> LatentInfection.builder()
                            .screeningId(d.getId())
                            .populationType("school")
                            .name(d.getName())
                            .idNumber(d.getIdNumber())
                            .gender(d.getGender())
                            .age(d.getAge())
                            .phone(d.getPhone())
                            .infectionResult(d.getInfectionResult())
                            .trackingStatus(0)
                            .notInPlaceCount(0)
                            .archived(0)
                            .hasChestXray(d.getHasChestXray())
                            .chestXrayDate(d.getChestXrayDate())
                            .chestXrayResult(d.getChestXrayResult())
                            .diagnosisFirst(latentDiagnosisFirst(d))
                            .departmentId(d.getDepartmentId())
                            .creatorId(BaseContext.getCurrentId())
                            .build())
                .toList();
        List<LatentInfection> allLatent = new ArrayList<>(latentList);
        allLatent.addAll(latentFromUpdated);
        if (!allLatent.isEmpty()) {
            latentInfectionService.saveBatch(allLatent, 500);
            latentInfectionService.autoReferralForDirectDiagnosis(allLatent);
            log.info("自动创建学校人群潜伏感染记录 {} 条", allLatent.size());
        }
        syncLatentFromScreening(toUpdate, "school");

        return result;
    }

    /**
     * 增量导入时，将胸片与首次诊断同步到已存在的潜伏感染记录，
     * 避免筛查表已更新但待诊断列表仍为空的情况。
     * <p>确诊患者仅筛查列表标红保留：归档已有待办记录，不新建潜伏记录。
     */
    private void syncLatentFromScreening(List<ScreeningSchool> records, String populationType) {
        for (ScreeningSchool d : records) {
            if (d.getId() == null) continue;
            // 确诊患者：保留在筛查界面标红即可，不进入潜伏/患者管理
            if (ScreeningDiagnosisSupport.isConfirmedPatientDiagnosis(d.getDiagnosisFirst())) {
                latentInfectionService.archivePendingLatentFromScreening(
                        d.getId(), populationType, d.getDiagnosisFirst());
                continue;
            }
            if (!shouldCreateLatentRecord(d)) {
                latentInfectionService.archivePendingLatentFromScreening(
                        d.getId(), populationType, d.getDiagnosisFirst());
                continue;
            }
            LatentInfection latent = latentInfectionService.lambdaQuery()
                    .eq(LatentInfection::getScreeningId, d.getId())
                    .eq(LatentInfection::getPopulationType, populationType)
                    .eq(LatentInfection::getArchived, 0)
                    .last("LIMIT 1")
                    .one();
            if (latent == null) {
                latent = LatentInfection.builder()
                        .screeningId(d.getId())
                        .populationType(populationType)
                        .name(d.getName())
                        .idNumber(d.getIdNumber())
                        .gender(d.getGender())
                        .age(d.getAge())
                        .phone(d.getPhone())
                        .infectionResult(d.getInfectionResult())
                        .trackingStatus(0)
                        .notInPlaceCount(0)
                        .archived(0)
                        .hasChestXray(d.getHasChestXray())
                        .chestXrayDate(d.getChestXrayDate())
                        .chestXrayResult(d.getChestXrayResult())
                        .diagnosisFirst(latentDiagnosisFirst(d))
                        .departmentId(d.getDepartmentId())
                        .creatorId(BaseContext.getCurrentId())
                        .build();
                latentInfectionService.save(latent);
                latentInfectionService.autoReferralForDirectDiagnosis(List.of(latent));
                continue;
            }

            var update = latentInfectionService.lambdaUpdate()
                    .eq(LatentInfection::getId, latent.getId());
            boolean changed = false;
            // 覆盖导入：筛查表字段（含 Excel 清空）同步到潜伏表
            if (StrUtil.isNotBlank(d.getName())) {
                update.set(LatentInfection::getName, d.getName());
                changed = true;
            }
            if (StrUtil.isNotBlank(d.getIdNumber())) {
                update.set(LatentInfection::getIdNumber, d.getIdNumber());
                changed = true;
            }
            if (StrUtil.isNotBlank(d.getGender())) {
                update.set(LatentInfection::getGender, d.getGender());
                changed = true;
            }
            if (d.getAge() != null) {
                update.set(LatentInfection::getAge, d.getAge());
                changed = true;
            }
            if (StrUtil.isNotBlank(d.getPhone())) {
                update.set(LatentInfection::getPhone, d.getPhone());
                changed = true;
            }
            update.set(LatentInfection::getInfectionResult, StrUtil.isBlank(d.getInfectionResult()) ? null : d.getInfectionResult().trim());
            update.set(LatentInfection::getHasChestXray, StrUtil.isBlank(d.getHasChestXray()) ? null : d.getHasChestXray().trim());
            update.set(LatentInfection::getChestXrayDate, d.getChestXrayDate());
            update.set(LatentInfection::getChestXrayResult, StrUtil.isBlank(d.getChestXrayResult()) ? null : d.getChestXrayResult().trim());
            String diagnosisFirst = latentDiagnosisFirst(d);
            update.set(LatentInfection::getDiagnosisFirst, diagnosisFirst);
            latent.setDiagnosisFirst(diagnosisFirst);
            changed = true;
            if (changed) {
                update.update();
            }
            latentInfectionService.autoReferralForDirectDiagnosis(List.of(latent));
        }
    }

    /** 年度筛选兜底：year 字段为空时，按感染筛查日期或胸片检查日期年份匹配 */
    private static final String SCREEN_YEAR_SQL_EXPR =
            "((screen_date IS NOT NULL AND YEAR(screen_date) = {0})"
                    + " OR (screen_date IS NULL AND chest_xray_date IS NOT NULL AND YEAR(chest_xray_date) = {0}))";

    @Override
    public IPage<ScreeningSchool> queryPage(int page, int size, String name, String idNumber,
                                             String schoolName, String district, Integer isLatent, String diagnosisFirst,
                                             String phone, String year, String entryUnit,
                                             String createTimeFrom, String createTimeTo,
                                             String creatorUsername, String hasChestXray, String chestXrayResult,
                                             String sputumSmearResult, String molecularBiologyResult, String sputumCultureResult,
                                             String columnFilters, String formatIssue, String sortField, String sortOrder) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = buildListWrapper(
                name, idNumber, schoolName, district, isLatent, diagnosisFirst, phone, year, entryUnit,
                createTimeFrom, createTimeTo, creatorUsername, hasChestXray, chestXrayResult,
                sputumSmearResult, molecularBiologyResult, sputumCultureResult, columnFilters, formatIssue);
        applyListOrder(wrapper, sortField, sortOrder);
        IPage<ScreeningSchool> result = page(new Page<>(page, size), wrapper);
        CreatorUserSupport.fillMissingUsernames(
                userMapper,
                result.getRecords(),
                ScreeningSchool::getCreatorId,
                ScreeningSchool::getCreatorUsername,
                ScreeningSchool::setCreatorUsername);
        return result;
    }

    @Override
    public List<ScreeningSchool> listForExport(String name, String idNumber, String schoolName, String district,
                                                Integer isLatent, String diagnosisFirst, String phone, String year,
                                                String entryUnit, String createTimeFrom, String createTimeTo,
                                                String creatorUsername, String hasChestXray, String chestXrayResult,
                                                String sputumSmearResult, String molecularBiologyResult, String sputumCultureResult,
                                                String columnFilters, String formatIssue, String sortField, String sortOrder,
                                                List<Long> ids) {
        LambdaQueryWrapper<ScreeningSchool> wrapper;
        if (ids != null && !ids.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ScreeningSchool::getId, ids);
            screeningScopeHelper.applyDepartmentScope(
                    wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        } else {
            wrapper = buildListWrapper(
                    name, idNumber, schoolName, district, isLatent, diagnosisFirst, phone, year, entryUnit,
                    createTimeFrom, createTimeTo, creatorUsername, hasChestXray, chestXrayResult,
                    sputumSmearResult, molecularBiologyResult, sputumCultureResult, columnFilters, formatIssue);
        }
        applyListOrder(wrapper, sortField, sortOrder);
        List<ScreeningSchool> records = list(wrapper);
        CreatorUserSupport.fillMissingUsernames(
                userMapper,
                records,
                ScreeningSchool::getCreatorId,
                ScreeningSchool::getCreatorUsername,
                ScreeningSchool::setCreatorUsername);
        return records;
    }

    private LambdaQueryWrapper<ScreeningSchool> buildListWrapper(
            String name, String idNumber, String schoolName, String district, Integer isLatent,
            String diagnosisFirst, String phone, String year, String entryUnit,
            String createTimeFrom, String createTimeTo, String creatorUsername,
            String hasChestXray, String chestXrayResult, String sputumSmearResult, String molecularBiologyResult, String sputumCultureResult,
            String columnFilters, String formatIssue) {
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(createTimeFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(createTimeTo);
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningSchool::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningSchool::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(schoolName), ScreeningSchool::getSchoolName, schoolName)
                .eq(StrUtil.isNotBlank(district), ScreeningSchool::getDistrict, district)
                .like(StrUtil.isNotBlank(phone), ScreeningSchool::getPhone, phone)
                .eq(isLatent != null, ScreeningSchool::getIsLatent, isLatent)
                .like(StrUtil.isNotBlank(creatorUsername), ScreeningSchool::getCreatorUsername, creatorUsername)
                .eq(StrUtil.isNotBlank(hasChestXray), ScreeningSchool::getHasChestXray, hasChestXray)
                .like(StrUtil.isNotBlank(sputumSmearResult), ScreeningSchool::getSputumSmearResult, sputumSmearResult)
                .eq(StrUtil.isNotBlank(molecularBiologyResult), ScreeningSchool::getMolecularBiologyResult, molecularBiologyResult)
                .eq(StrUtil.isNotBlank(sputumCultureResult), ScreeningSchool::getSputumCultureResult, sputumCultureResult)
                .ge(createFrom != null, ScreeningSchool::getCreateTime, createFrom)
                .le(createTo != null, ScreeningSchool::getCreateTime, createTo);
        if (StrUtil.isNotBlank(chestXrayResult)) {
            applyChestXrayResultFilter(wrapper, chestXrayResult);
        }
        ScreeningDiagnosisSupport.applyScreeningDiagnosisFilter(
                wrapper, ScreeningSchool::getIsLatent, ScreeningSchool::getDiagnosisFirst, diagnosisFirst);
        applyScreenYearFilter(wrapper, year);
        applyEntryUnitFilter(wrapper, entryUnit);
        applyColumnFilters(wrapper, columnFilters);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        IdentityFormatFilterSupport.apply(wrapper, formatIssue, "id_number", "phone");
        return wrapper;
    }

    private void applyListOrder(LambdaQueryWrapper<ScreeningSchool> wrapper, String sortField, String sortOrder) {
        ListSortSupport.apply(wrapper, sortField, sortOrder, SORT_COLUMNS, ImportRowOrderSupport.WITH_BATCH);
    }

    private void applyColumnFilters(LambdaQueryWrapper<ScreeningSchool> wrapper, String columnFilters) {
        Map<String, String> filters = ColumnFilterSupport.parse(columnFilters);
        ColumnFilterSupport.applyLambda(filters, COLUMN_FILTER_WHITELIST, (field, value) -> {
            switch (field) {
                case "name" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getName, value);
                case "year" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getYear, value);
                case "city" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getCity, value);
                case "district" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getDistrict, value);
                case "township" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getTownship, value);
                case "reportingOrg" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getReportingOrg, value);
                case "gender" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getGender, value);
                case "idNumber" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getIdNumber, value);
                case "phone" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getPhone, value);
                case "ethnicity" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getEthnicity, value);
                case "participatedScreening" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getParticipatedScreening, value);
                case "schoolName" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getSchoolName, value);
                case "gradeName" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getGradeName, value);
                case "className" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getClassName, value);
                case "schoolType" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getSchoolType, value);
                case "boardingType" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getBoardingType, value);
                case "currentAddress" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getCurrentAddress, value);
                case "householdAddress" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getHouseholdAddress, value);
                case "screenMethod" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getScreenMethod, value);
                case "infectionResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getInfectionResult, value);
                case "diagnosisFirst" -> ScreeningDiagnosisSupport.applyScreeningDiagnosisColumnFilter(
                        wrapper, ScreeningSchool::getIsLatent, ScreeningSchool::getDiagnosisFirst, value);
                case "hasChestXray" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getHasChestXray, value);
                case "chestXrayMethod" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getChestXrayMethod, value);
                case "chestXrayResult" -> applyChestXrayResultFilter(wrapper, value);
                case "sputumSmearResult" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getSputumSmearResult, value);
                case "molecularBiologyResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getMolecularBiologyResult, value);
                case "sputumCultureResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getSputumCultureResult, value);
                case "remark" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getRemark, value);
                case "creatorUsername" -> ColumnFilterSupport.like(wrapper, ScreeningSchool::getCreatorUsername, value);
                case "idType" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getIdType, value);
                case "tbHistory" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getTbHistory, value);
                case "closeContactHistory" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getCloseContactHistory, value);
                case "suspiciousSymptoms" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getSuspiciousSymptoms, value);
                case "symptomCough" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getSymptomCough, value);
                case "symptomHemoptysis" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getSymptomHemoptysis, value);
                case "symptomOther" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getSymptomOther, value);
                case "hasInfectionScreen" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getHasInfectionScreen, value);
                case "screenResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningSchool::getScreenResult, value);
                default -> { }
            }
        });
    }

    /** 胸片结果筛选：兼容「正常/未见异常」「异常」与细分类文案 */
    private void applyChestXrayResultFilter(LambdaQueryWrapper<ScreeningSchool> wrapper, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        List<String> values = ColumnFilterSupport.splitValues(value).stream().toList();
        if (values.isEmpty()) {
            return;
        }
        wrapper.and(outer -> {
            boolean first = true;
            for (String item : values) {
                String trimmed = item.trim();
                if (StrUtil.isBlank(trimmed)) {
                    continue;
                }
                if (first) {
                    outer.nested(sub -> appendChestXrayResultCondition(sub, trimmed));
                    first = false;
                } else {
                    outer.or(sub -> appendChestXrayResultCondition(sub, trimmed));
                }
            }
        });
    }

    private void appendChestXrayResultCondition(LambdaQueryWrapper<ScreeningSchool> wrapper, String trimmed) {
        if ("正常".equals(trimmed) || "未见异常".equals(trimmed)) {
            wrapper.in(ScreeningSchool::getChestXrayResult, "正常", "未见异常");
            return;
        }
        if ("异常".equals(trimmed)) {
            wrapper.and(w -> w.eq(ScreeningSchool::getChestXrayResult, "异常")
                    .or().likeRight(ScreeningSchool::getChestXrayResult, "异常"));
            return;
        }
        wrapper.eq(ScreeningSchool::getChestXrayResult, trimmed);
    }

    /** 年度筛选：优先匹配 Excel「年度/年份」列，无年度字段时按筛查日期兜底 */
    private void applyScreenYearFilter(LambdaQueryWrapper<ScreeningSchool> wrapper, String year) {
        if (StrUtil.isBlank(year)) {
            return;
        }
        String trimmed = year.trim();
        try {
            int yearInt = Integer.parseInt(trimmed);
            wrapper.and(w -> w.eq(ScreeningSchool::getYear, trimmed)
                    .or().apply("LEFT(TRIM(year), 4) = {0}", trimmed)
                    .or(nested -> nested.and(emptyYear -> emptyYear.isNull(ScreeningSchool::getYear)
                            .or().eq(ScreeningSchool::getYear, ""))
                            .apply(SCREEN_YEAR_SQL_EXPR, yearInt)));
        } catch (NumberFormatException ignored) {
            wrapper.and(w -> w.eq(ScreeningSchool::getYear, trimmed)
                    .or().apply("LEFT(TRIM(year), 4) = {0}", trimmed));
        }
    }

    /** 录入单位：按部门名称模糊匹配 department_id */
    private void applyEntryUnitFilter(LambdaQueryWrapper<ScreeningSchool> wrapper, String entryUnit) {
        if (StrUtil.isBlank(entryUnit)) {
            return;
        }
        List<Long> deptIds = departmentService.resolveIdsByNameLike(entryUnit);
        if (deptIds.isEmpty()) {
            wrapper.eq(ScreeningSchool::getId, -1L);
        } else {
            wrapper.in(ScreeningSchool::getDepartmentId, deptIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScreening(ScreeningSchool data) {
        data.setIdNumber(ImportIdentitySupport.normalizeIdNumber(data.getIdNumber()));
        if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }

        prepareSchoolDerivedFields(data);
        if (StrUtil.isNotBlank(data.getDiagnosisFirst())) {
            data.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst()));
        }
        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
        CreatorUserSupport.fillCurrentCreator(userMapper, data::setCreatorId, data::setCreatorUsername);
        save(data);

        if (shouldCreateLatentRecord(data)) {
            // 潜伏感染者（码3）等：创建后由 autoReferral 分流进入潜伏感染管理。
            // 确诊患者（码1）不走此分支，仅筛查列表标红保留。
            LatentInfection latent = LatentInfection.builder()
                    .screeningId(data.getId())
                    .populationType("school")
                    .name(data.getName())
                    .idNumber(data.getIdNumber())
                    .gender(data.getGender())
                    .age(data.getAge())
                    .phone(data.getPhone())
                    .infectionResult(data.getInfectionResult())
                    .trackingStatus(0)
                    .notInPlaceCount(0)
                    .archived(0)
                    .hasChestXray(data.getHasChestXray())
                    .chestXrayDate(data.getChestXrayDate())
                    .chestXrayResult(data.getChestXrayResult())
                    .diagnosisFirst(latentDiagnosisFirst(data))
                    .departmentId(data.getDepartmentId())
                    .creatorId(BaseContext.getCurrentId())
                    .build();
            latentInfectionService.save(latent);
            latentInfectionService.autoReferralForDirectDiagnosis(List.of(latent));
        }
    }

    private boolean shouldMarkLatent(ScreeningSchool data) {
        if (data == null) return false;
        return ScreeningDiagnosisSupport.shouldMarkLatent(
                data.getInfectionResult(),
                data.getChestXrayResult(),
                data.getHasChestXray(),
                data.getDiagnosisFirst());
    }

    /** 是否创建潜伏/待诊断记录（确诊患者除外，仅筛查列表标红） */
    private boolean shouldCreateLatentRecord(ScreeningSchool data) {
        if (data == null) return false;
        return ScreeningDiagnosisSupport.shouldCreateLatentRecord(
                data.getInfectionResult(),
                data.getChestXrayResult(),
                data.getHasChestXray(),
                data.getDiagnosisFirst());
    }

    /** 由表单/导入字段推导是否感染筛、是否胸片、可疑症状汇总 */
    private void prepareSchoolDerivedFields(ScreeningSchool data) {
        if (data == null) return;
        String summarized = SchoolScreeningCodeSupport.summarizeSuspiciousSymptoms(
                data.getSymptomCough(), data.getSymptomHemoptysis(), data.getSymptomOther());
        if (StrUtil.isNotBlank(summarized)) {
            data.setSuspiciousSymptoms(summarized);
        }
        if (StrUtil.isBlank(data.getHasInfectionScreen()) && StrUtil.isNotBlank(data.getScreenMethod())) {
            data.setHasInfectionScreen(SchoolScreeningCodeSupport.deriveHasInfectionScreen(data.getScreenMethod()));
        }
        if (StrUtil.isBlank(data.getHasChestXray()) && StrUtil.isNotBlank(data.getChestXrayMethod())) {
            data.setHasChestXray(SchoolScreeningCodeSupport.deriveHasChestXray(data.getChestXrayMethod()));
        }
    }

    /** 诊断结果写入潜伏表；疑似结核由分流逻辑保留在待诊断，不再归档。 */
    private String latentDiagnosisFirst(ScreeningSchool data) {
        if (data == null) {
            return null;
        }
        return ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst());
    }

    /** 增量导入：按证件号匹配时，用最新 Excel 行覆盖筛查表字段。 */
    private void mergeSchoolImportFields(ScreeningSchool existing, ScreeningSchool incoming) {
        ScreeningImportMergeSupport.mergeSchool(existing, incoming);
    }

    private record SchoolHeaderLayout(int headerRow, int subHeaderRow, int dataStartRow) {
    }

    /**
     * 定位学生筛查表头：含「姓名」+「身份证号/证件号」的行；跳过标题行与填写说明行。
     */
    private SchoolHeaderLayout locateSchoolHeaderLayout(List<Map<Integer, String>> rows) {
        int headerRow = -1;
        for (int i = 0; i < Math.min(rows.size(), 12); i++) {
            String joined = rowJoinedText(rows.get(i));
            if (joined.contains("姓名") && (joined.contains("身份证号") || joined.contains("证件号"))) {
                headerRow = i;
                break;
            }
        }
        if (headerRow < 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "模板表头不正确，请使用学生筛查模板或先进行数据匹配");
        }
        int subHeaderRow = headerRow;
        if (headerRow + 1 < rows.size()) {
            String next = rowJoinedText(rows.get(headerRow + 1));
            if (next.contains("方法") || next.contains("咳嗽") || next.contains("感染筛查时间") || next.contains("判定结果")) {
                subHeaderRow = headerRow + 1;
            }
        }
        int dataStart = subHeaderRow + 1;
        // 跳过合并空行、填写说明行
        while (dataStart < rows.size()) {
            String text = rowJoinedText(rows.get(dataStart));
            if (StrUtil.isBlank(text)
                    || text.contains("填写数字")
                    || text.contains("PPD填写")
                    || text.contains("托幼机构")
                    || text.contains("寄宿制")) {
                dataStart++;
                continue;
            }
            break;
        }
        return new SchoolHeaderLayout(headerRow, subHeaderRow, dataStart);
    }

    private String rowJoinedText(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String v : row.values()) {
            if (StrUtil.isNotBlank(v)) sb.append(v);
        }
        return sb.toString();
    }

    private Map<String, Integer> buildSchoolHeaderIndex(List<Map<Integer, String>> rows, SchoolHeaderLayout layout) {
        Map<String, Integer> headerIndex = new HashMap<>();
        Map<Integer, String> mainRow = rows.get(layout.headerRow());
        Map<Integer, String> subRow = layout.subHeaderRow() != layout.headerRow()
                ? rows.get(layout.subHeaderRow()) : Map.of();

        int maxCol = 0;
        for (Integer k : mainRow.keySet()) {
            if (k != null && k > maxCol) maxCol = k;
        }
        for (Integer k : subRow.keySet()) {
            if (k != null && k > maxCol) maxCol = k;
        }

        String carriedGroup = "";
        Integer nameCol = null;
        for (int col = 0; col <= maxCol; col++) {
            String main = StrUtil.trim(mainRow.get(col));
            String sub = StrUtil.trim(subRow.get(col));
            if (StrUtil.isNotBlank(main)
                    && !main.contains("咳嗽")
                    && !main.contains("咯血")
                    && !"其他".equals(main)
                    && !main.contains("感染筛查时间")
                    && !main.contains("胸片检查时间")
                    && !"方法".equals(main)
                    && !"结果".equals(main)
                    && !"判定结果".equals(main)) {
                // 分组标题或单列表头
                if ("结核病可疑症状".equals(main) || "感染筛查".equals(main) || "胸部影像学".equals(main)
                        || main.contains("感染筛查情况") || main.contains("胸片检查")) {
                    carriedGroup = main;
                } else if (StrUtil.isBlank(sub)) {
                    carriedGroup = "";
                }
            }
            String resolved = resolveSchoolHeaderKey(main, sub, carriedGroup);
            if (StrUtil.isNotBlank(resolved)) {
                putHeaderAlias(headerIndex, resolved, col);
                if ("姓名".equals(normalizeHeader(resolved))) {
                    nameCol = col;
                }
            }
        }

        // 年份：优先取姓名右侧的「年份」列（忽略旧表最左侧年度列）
        resolveYearColumn(headerIndex, mainRow, subRow, maxCol, nameCol);
        return headerIndex;
    }

    private void resolveYearColumn(Map<String, Integer> headerIndex,
                                   Map<Integer, String> mainRow,
                                   Map<Integer, String> subRow,
                                   int maxCol,
                                   Integer nameCol) {
        Integer preferred = null;
        Integer fallback = null;
        for (int col = 0; col <= maxCol; col++) {
            String main = normalizeHeader(mainRow.get(col));
            String sub = normalizeHeader(subRow.get(col));
            boolean isYear = "年份".equals(main) || "年度".equals(main)
                    || "年份".equals(sub) || "年度".equals(sub);
            if (!isYear) continue;
            fallback = col;
            if (nameCol != null && col > nameCol) {
                preferred = col;
                break;
            }
        }
        Integer yearCol = preferred != null ? preferred : fallback;
        if (yearCol != null) {
            headerIndex.put("年份", yearCol);
        }
    }

    private String resolveSchoolHeaderKey(String main, String sub, String carriedGroup) {
        String m = StrUtil.blankToDefault(main, "");
        String s = StrUtil.blankToDefault(sub, "");
        String g = StrUtil.blankToDefault(carriedGroup, "");
        if (StrUtil.isNotBlank(s)) {
            if ("方法".equals(s)) {
                if (g.contains("感染") || m.contains("感染")) return "感染筛查方法";
                if (g.contains("胸") || m.contains("胸")) return "胸部影像学方法";
                return "方法";
            }
            if ("结果".equals(s) || s.startsWith("结果")) {
                if (g.contains("感染") || m.contains("感染")) return "感染筛查结果值";
                if (g.contains("胸") || m.contains("胸")) return "胸片结果";
                return s;
            }
            if (s.contains("咳嗽")) return "咳嗽咳痰";
            if (s.contains("咯血")) return "咯血或血痰";
            if ("其他".equals(s) && (g.contains("可疑") || m.contains("可疑"))) return "可疑症状其他";
            if (s.contains("感染筛查时间") || s.contains("感染筛查日期")) return "感染筛查日期";
            if (s.contains("胸片检查时间") || s.contains("胸片检查日期")) return "胸片检查日期";
            if ("判定结果".equals(s) || s.contains("感染筛查结果")) return "判定结果";
            if ("是否进行感染筛".equals(s) || "是否进行感染筛查".equals(s)) return "是否进行感染筛";
            if ("是否进行胸片检查".equals(s)) return "是否进行胸片检查";
            return s;
        }
        if (StrUtil.isNotBlank(m)
                && !"结核病可疑症状".equals(m)
                && !"感染筛查".equals(m)
                && !"胸部影像学".equals(m)
                && !m.contains("感染筛查情况")
                && !m.contains("胸片检查")
                && !m.contains("潜伏感染者管理")) {
            return m;
        }
        return "";
    }

    private void putHeaderAlias(Map<String, Integer> headerIndex, String rawHeader, Integer index) {
        if (index == null || StrUtil.isBlank(rawHeader)) return;
        String header = normalizeHeader(rawHeader);
        if (StrUtil.isBlank(header)) return;
        headerIndex.putIfAbsent(header, index);
        switch (header) {
            case "年度" -> { /* 年份由 resolveYearColumn 处理 */ }
            case "区县", "县市区", "县区" -> headerIndex.putIfAbsent("县市区", index);
            case "身份证号", "身份证号码" -> headerIndex.putIfAbsent("证件号", index);
            case "学校名称全称" -> headerIndex.putIfAbsent("学校名称", index);
            case "类型" -> headerIndex.putIfAbsent("学校类型", index);
            case "是否寄宿制" -> headerIndex.putIfAbsent("是否寄宿制", index);
            case "班级院系", "班级" -> headerIndex.putIfAbsent("班级院系", index);
            case "年级" -> headerIndex.putIfAbsent("年级", index);
            case "现地址" -> headerIndex.putIfAbsent("现住址", index);
            case "有无可疑症状" -> headerIndex.putIfAbsent("结核病可疑症状", index);
            case "有无既往结核病史" -> headerIndex.putIfAbsent("既往结核病史", index);
            case "有无肺结核接触史", "密切接触史" -> headerIndex.putIfAbsent("密切接触史", index);
            case "是否感染筛查" -> headerIndex.putIfAbsent("是否进行感染筛", index);
            case "判定结果", "感染筛查结果学校人群感染筛查情况" -> headerIndex.putIfAbsent("感染筛查结果", index);
            case "胸部DR", "胸片检查结果" -> headerIndex.putIfAbsent("胸片结果", index);
            case "痰涂片" -> headerIndex.putIfAbsent("痰涂片结果", index);
            case "分子生物学" -> headerIndex.putIfAbsent("分子生物学结果", index);
            case "痰培养", "痰培养结果" -> headerIndex.putIfAbsent("痰培养结果", index);
            case "诊断", "筛查结果" -> headerIndex.putIfAbsent("诊断结果", index);
            case "填报机构" -> headerIndex.putIfAbsent("填报机构", index);
            case "乡镇街道", "乡镇社区" -> headerIndex.putIfAbsent("乡镇街道", index);
            case "是否参加筛查" -> headerIndex.putIfAbsent("是否参加筛查", index);
            case "咳嗽咳痰两周", "咳嗽咳痰≥两周", "咳嗽咳痰" -> headerIndex.putIfAbsent("咳嗽咳痰", index);
            case "可疑症状其他" -> headerIndex.putIfAbsent("可疑症状其他", index);
            case "感染筛查方法" -> headerIndex.putIfAbsent("感染筛查方法", index);
            case "方法" -> headerIndex.putIfAbsent("感染筛查方法", index);
            case "感染筛查结果值" -> headerIndex.putIfAbsent("感染筛查结果值", index);
            case "结果PPDmmXmmEC及IGRA阳性阴性" -> headerIndex.putIfAbsent("感染筛查结果值", index);
            case "胸部影像学方法" -> headerIndex.putIfAbsent("胸部影像学方法", index);
            default -> {
            }
        }
    }

    private String normalizeHeader(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\s\\n\\r（）()：:；;、/≥]", "").trim();
    }

    private ScreeningSchool mapSchoolRow(Map<Integer, String> row, Map<String, Integer> headerIndex) {
        ScreeningSchool data = new ScreeningSchool();
        data.setYear(normalizeYearValue(field(row, headerIndex, "年份", "年度")));
        data.setReportingOrg(field(row, headerIndex, "填报机构"));
        data.setCity(field(row, headerIndex, "市州"));
        data.setDistrict(field(row, headerIndex, "县市区", "区县", "县区"));
        data.setTownship(field(row, headerIndex, "乡镇街道", "乡镇社区"));
        data.setName(field(row, headerIndex, "姓名"));
        data.setGender(field(row, headerIndex, "性别"));
        data.setBirthDate(FlexibleDateParseUtil.parse(field(row, headerIndex, "出生日期")));
        data.setAge(parseInteger(field(row, headerIndex, "年龄")));
        data.setIdType(field(row, headerIndex, "证件类型"));
        data.setIdNumber(ImportIdentitySupport.normalizeIdNumber(
                normalizeExcelCellText(field(row, headerIndex, "证件号", "身份证号", "身份证号码"))));
        data.setEthnicity(field(row, headerIndex, "民族"));
        data.setParticipatedScreening(field(row, headerIndex, "是否参加筛查"));
        data.setPhone(normalizeExcelCellText(field(row, headerIndex, "联系电话")));
        data.setHouseholdAddress(field(row, headerIndex, "户籍所在地XX市XX县区", "户籍所在地"));
        data.setCurrentAddress(field(row, headerIndex, "现住址", "现地址"));
        data.setSchoolType(SchoolScreeningCodeSupport.toSchoolType(
                field(row, headerIndex, "学校类型", "类型")));
        data.setBoardingType(SchoolScreeningCodeSupport.toBoardingType(
                field(row, headerIndex, "是否寄宿制")));
        data.setSchoolName(field(row, headerIndex, "学校名称", "学校名称全称"));
        data.setGradeName(field(row, headerIndex, "年级"));
        data.setClassName(field(row, headerIndex, "班级院系", "班级"));
        data.setTbHistory(field(row, headerIndex, "既往结核病史", "有无既往结核病史"));
        data.setCloseContactHistory(field(row, headerIndex, "密切接触史", "有无肺结核接触史"));

        String cough = field(row, headerIndex, "咳嗽咳痰", "咳嗽咳痰两周");
        String hemoptysis = field(row, headerIndex, "咯血或血痰");
        String symptomOther = field(row, headerIndex, "可疑症状其他");
        data.setSymptomCough(cough);
        data.setSymptomHemoptysis(hemoptysis);
        data.setSymptomOther(symptomOther);
        String suspicious = SchoolScreeningCodeSupport.summarizeSuspiciousSymptoms(cough, hemoptysis, symptomOther);
        if (StrUtil.isBlank(suspicious)) {
            suspicious = field(row, headerIndex, "结核病可疑症状", "有无可疑症状");
        }
        data.setSuspiciousSymptoms(suspicious);

        data.setScreenDate(FlexibleDateParseUtil.parse(
                field(row, headerIndex, "感染筛查日期", "感染筛查时间")));
        String screenMethod = SchoolScreeningCodeSupport.toScreenMethod(
                field(row, headerIndex, "感染筛查方法"));
        data.setScreenMethod(screenMethod);
        data.setScreenResult(field(row, headerIndex, "感染筛查结果值", "结果PPDmmXmmEC及IGRA阳性阴性"));
        data.setInfectionResult(SchoolScreeningCodeSupport.toInfectionResult(
                field(row, headerIndex, "感染筛查结果", "判定结果")));

        String hasInfection = field(row, headerIndex, "是否进行感染筛", "是否感染筛查");
        if (StrUtil.isBlank(hasInfection) && StrUtil.isNotBlank(screenMethod)) {
            hasInfection = SchoolScreeningCodeSupport.deriveHasInfectionScreen(screenMethod);
        }
        data.setHasInfectionScreen(hasInfection);

        String chestMethod = SchoolScreeningCodeSupport.toChestXrayMethod(
                field(row, headerIndex, "胸部影像学方法"));
        data.setChestXrayMethod(chestMethod);
        data.setChestXrayDate(FlexibleDateParseUtil.parse(
                field(row, headerIndex, "胸片检查日期", "胸片检查时间")));
        String chestResultRaw = field(row, headerIndex, "胸片结果", "胸部DR", "胸片检查结果");
        String chestResult = SchoolScreeningCodeSupport.toChestXrayResult(chestResultRaw);
        if (StrUtil.isBlank(chestResult)) {
            chestResult = chestResultRaw;
        }
        data.setChestXrayResult(chestResult);
        String hasChest = field(row, headerIndex, "是否进行胸片检查");
        if (StrUtil.isBlank(hasChest) && StrUtil.isNotBlank(chestMethod)) {
            hasChest = SchoolScreeningCodeSupport.deriveHasChestXray(chestMethod);
        }
        data.setHasChestXray(hasChest);

        data.setSputumSmearResult(field(row, headerIndex, "痰涂片结果", "痰涂片"));
        data.setMolecularBiologyResult(SchoolScreeningCodeSupport.toLabResult(
                field(row, headerIndex, "分子生物学结果", "分子生物学")));
        data.setSputumCultureResult(SchoolScreeningCodeSupport.toLabResult(
                field(row, headerIndex, "痰培养结果", "痰培养")));
        data.setDiagnosisFirst(SchoolScreeningCodeSupport.toDiagnosis(
                field(row, headerIndex, "诊断结果", "筛查结果", "诊断")));
        data.setRemark(field(row, headerIndex, "备注"));
        return data;
    }

    private String field(Map<Integer, String> row, Map<String, Integer> headerIndex, String... headers) {
        for (String header : headers) {
            Integer idx = headerIndex.get(normalizeHeader(header));
            if (idx == null) continue;
            String value = row.get(idx);
            if (StrUtil.isNotBlank(value)) return value.trim();
        }
        return "";
    }

    private boolean isBlankSchoolRow(ScreeningSchool data) {
        return data == null || (StrUtil.isBlank(data.getName())
                && ImportIdentitySupport.isBlankOrPlaceholder(data.getIdNumber()));
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) return null;
        try {
            String digits = value.trim().replaceAll("[^0-9]", "");
            return StrUtil.isBlank(digits) ? null : Integer.parseInt(digits);
        } catch (Exception ignored) {
            return null;
        }
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

    /** 统一年度字段：兼容 Excel 数值型（2026.0）、带后缀（2026年）等格式 */
    private String normalizeYearValue(String value) {
        String text = normalizeExcelCellText(value);
        if (StrUtil.isBlank(text)) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{4})").matcher(text);
        return matcher.find() ? matcher.group(1) : text;
    }

    /** 18位身份证格式 + 校验位验证 */
    private boolean isValidIdCard(String id) {
        // 仅校验格式（18位 + 字符规则）。Excel 以数值型存储身份证号时会丢失浮点精度，导致校验码错误，故不做校验码验证。
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    /** 11位手机号验证 */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    /** 证件类型为居民身份证（或未填）时按身份证规则校验 */
    private boolean isIdCardType(String idType) {
        return StrUtil.isBlank(idType) || "居民身份证".equals(idType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScreening(ScreeningSchool data) {
        ScreeningSchool existing = getById(data.getId());
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        data.setIdNumber(ImportIdentitySupport.normalizeIdNumber(data.getIdNumber()));
        if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }
        prepareSchoolDerivedFields(data);
        // 根据感染筛查结果与诊断结果重新计算潜伏判定
        if (StrUtil.isNotBlank(data.getDiagnosisFirst())) {
            data.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst()));
        } else {
            data.setDiagnosisFirst(null);
        }
        if (StrUtil.isBlank(data.getChestXrayResult())) {
            data.setChestXrayResult(null);
        }
        if (StrUtil.isBlank(data.getInfectionResult())) {
            data.setInfectionResult(null);
        }
        if (StrUtil.isBlank(data.getScreenMethod())) {
            data.setScreenMethod(null);
        }
        if (StrUtil.isBlank(data.getScreenResult())) {
            data.setScreenResult(null);
        }
        if (StrUtil.isBlank(data.getSputumSmearResult())) {
            data.setSputumSmearResult(null);
        }
        if (StrUtil.isBlank(data.getMolecularBiologyResult())) {
            data.setMolecularBiologyResult(null);
        }
        if (StrUtil.isBlank(data.getSputumCultureResult())) {
            data.setSputumCultureResult(null);
        }
        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        // 录入用户与部门不可被前端覆盖；历史两边都空则补当前用户，有 id 缺名则按用户表补名
        data.setCreatorId(existing.getCreatorId());
        data.setCreatorUsername(existing.getCreatorUsername());
        data.setDepartmentId(existing.getDepartmentId());
        CreatorUserSupport.fillMissingCreator(
                data.getCreatorId(),
                data.getCreatorUsername(),
                CreatorUserSupport.resolveCurrentCreator(userMapper),
                data::setCreatorId,
                data::setCreatorUsername);
        CreatorUserSupport.fillMissingUsernames(
                userMapper,
                List.of(data),
                ScreeningSchool::getCreatorId,
                ScreeningSchool::getCreatorUsername,
                ScreeningSchool::setCreatorUsername);
        updateById(data);
        ScreeningSchool updated = getById(data.getId());
        syncLatentFromScreening(List.of(updated), "school");
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
    public int deleteByFilter(String name, String idNumber, String schoolName, String district,
                               Integer isLatent, String diagnosisFirst, String phone, String year,
                               String entryUnit, String createTimeFrom, String createTimeTo,
                               String creatorUsername, String hasChestXray, String chestXrayResult,
                               String sputumSmearResult, String molecularBiologyResult, String sputumCultureResult, String columnFilters,
                               String formatIssue) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = buildListWrapper(
                name, idNumber, schoolName, district, isLatent, diagnosisFirst, phone, year, entryUnit,
                createTimeFrom, createTimeTo, creatorUsername, hasChestXray, chestXrayResult,
                sputumSmearResult, molecularBiologyResult, sputumCultureResult, columnFilters, formatIssue);
        wrapper.select(ScreeningSchool::getId);
        List<Long> ids = list(wrapper).stream().map(ScreeningSchool::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        batchDeleteCascade(ids);
        return ids.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAll() {
        return deleteByFilter(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
    }

    private void doDeleteScreeningCascade(Long id) {
        if (getById(id) == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        // 查找关联的潜伏感染记录
        List<LatentInfection> latentList = latentInfectionService.lambdaQuery()
                .eq(LatentInfection::getScreeningId, id)
                .eq(LatentInfection::getPopulationType, "school")
                .list();
        for (LatentInfection latent : latentList) {
            deleteCascadeFromLatent(latent.getId());
        }
        // 删除筛查记录本体
        removeById(id);
        log.info("级联删除学校人群筛查记录 id={}", id);
    }

    /**
     * 从潜伏感染记录开始向下级联删除所有关联数据
     */
    private void deleteCascadeFromLatent(Long latentId) {
        // 删除关联患者及患者下游数据
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
        // 删除督导表、潜伏随访、按期检查
        supervisionFormService.lambdaUpdate()
                .eq(cn.luyou.model.SupervisionForm::getLatentInfectionId, latentId).remove();
        latentFollowUpService.lambdaUpdate()
                .eq(cn.luyou.model.LatentFollowUp::getLatentInfectionId, latentId).remove();
        latentCheckService.lambdaUpdate()
                .eq(cn.luyou.model.LatentCheck::getLatentInfectionId, latentId).remove();
        // 删除潜伏通知单及关联消息
        deleteNoticeAndMessages(latentId, "latent");
        // 删除潜伏分级诊疗记录及关联消息
        deleteReferralsAndMessages(latentId);
        // 删除潜伏感染记录本体
        latentInfectionService.removeById(latentId);
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

    @Override
    public List<String> listDistinctColumnValues(String field) {
        if (StrUtil.isBlank(field) || !COLUMN_FILTER_EQ_FIELDS.contains(field)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        applyDistinctSelect(wrapper, field);
        return ColumnDistinctSupport.normalize(list(wrapper).stream()
                .map(row -> extractDistinctValue(row, field))
                .toList());
    }

    private void applyDistinctSelect(LambdaQueryWrapper<ScreeningSchool> wrapper, String field) {
        switch (field) {
            case "gender" -> wrapper.select(ScreeningSchool::getGender)
                    .isNotNull(ScreeningSchool::getGender).ne(ScreeningSchool::getGender, "")
                    .groupBy(ScreeningSchool::getGender);
            case "year" -> wrapper.select(ScreeningSchool::getYear)
                    .isNotNull(ScreeningSchool::getYear).ne(ScreeningSchool::getYear, "")
                    .groupBy(ScreeningSchool::getYear);
            case "city" -> wrapper.select(ScreeningSchool::getCity)
                    .isNotNull(ScreeningSchool::getCity).ne(ScreeningSchool::getCity, "")
                    .groupBy(ScreeningSchool::getCity);
            case "district" -> wrapper.select(ScreeningSchool::getDistrict)
                    .isNotNull(ScreeningSchool::getDistrict).ne(ScreeningSchool::getDistrict, "")
                    .groupBy(ScreeningSchool::getDistrict);
            case "township" -> wrapper.select(ScreeningSchool::getTownship)
                    .isNotNull(ScreeningSchool::getTownship).ne(ScreeningSchool::getTownship, "")
                    .groupBy(ScreeningSchool::getTownship);
            case "ethnicity" -> wrapper.select(ScreeningSchool::getEthnicity)
                    .isNotNull(ScreeningSchool::getEthnicity).ne(ScreeningSchool::getEthnicity, "")
                    .groupBy(ScreeningSchool::getEthnicity);
            case "idType" -> wrapper.select(ScreeningSchool::getIdType)
                    .isNotNull(ScreeningSchool::getIdType).ne(ScreeningSchool::getIdType, "")
                    .groupBy(ScreeningSchool::getIdType);
            case "schoolType" -> wrapper.select(ScreeningSchool::getSchoolType)
                    .isNotNull(ScreeningSchool::getSchoolType).ne(ScreeningSchool::getSchoolType, "")
                    .groupBy(ScreeningSchool::getSchoolType);
            case "boardingType" -> wrapper.select(ScreeningSchool::getBoardingType)
                    .isNotNull(ScreeningSchool::getBoardingType).ne(ScreeningSchool::getBoardingType, "")
                    .groupBy(ScreeningSchool::getBoardingType);
            case "screenMethod" -> wrapper.select(ScreeningSchool::getScreenMethod)
                    .isNotNull(ScreeningSchool::getScreenMethod).ne(ScreeningSchool::getScreenMethod, "")
                    .groupBy(ScreeningSchool::getScreenMethod);
            case "infectionResult" -> wrapper.select(ScreeningSchool::getInfectionResult)
                    .isNotNull(ScreeningSchool::getInfectionResult).ne(ScreeningSchool::getInfectionResult, "")
                    .groupBy(ScreeningSchool::getInfectionResult);
            case "diagnosisFirst" -> wrapper.select(ScreeningSchool::getDiagnosisFirst)
                    .isNotNull(ScreeningSchool::getDiagnosisFirst).ne(ScreeningSchool::getDiagnosisFirst, "")
                    .groupBy(ScreeningSchool::getDiagnosisFirst);
            case "hasChestXray" -> wrapper.select(ScreeningSchool::getHasChestXray)
                    .isNotNull(ScreeningSchool::getHasChestXray).ne(ScreeningSchool::getHasChestXray, "")
                    .groupBy(ScreeningSchool::getHasChestXray);
            case "chestXrayMethod" -> wrapper.select(ScreeningSchool::getChestXrayMethod)
                    .isNotNull(ScreeningSchool::getChestXrayMethod).ne(ScreeningSchool::getChestXrayMethod, "")
                    .groupBy(ScreeningSchool::getChestXrayMethod);
            case "chestXrayResult" -> wrapper.select(ScreeningSchool::getChestXrayResult)
                    .isNotNull(ScreeningSchool::getChestXrayResult).ne(ScreeningSchool::getChestXrayResult, "")
                    .groupBy(ScreeningSchool::getChestXrayResult);
            case "tbHistory" -> wrapper.select(ScreeningSchool::getTbHistory)
                    .isNotNull(ScreeningSchool::getTbHistory).ne(ScreeningSchool::getTbHistory, "")
                    .groupBy(ScreeningSchool::getTbHistory);
            case "closeContactHistory" -> wrapper.select(ScreeningSchool::getCloseContactHistory)
                    .isNotNull(ScreeningSchool::getCloseContactHistory).ne(ScreeningSchool::getCloseContactHistory, "")
                    .groupBy(ScreeningSchool::getCloseContactHistory);
            case "suspiciousSymptoms" -> wrapper.select(ScreeningSchool::getSuspiciousSymptoms)
                    .isNotNull(ScreeningSchool::getSuspiciousSymptoms).ne(ScreeningSchool::getSuspiciousSymptoms, "")
                    .groupBy(ScreeningSchool::getSuspiciousSymptoms);
            case "hasInfectionScreen" -> wrapper.select(ScreeningSchool::getHasInfectionScreen)
                    .isNotNull(ScreeningSchool::getHasInfectionScreen).ne(ScreeningSchool::getHasInfectionScreen, "")
                    .groupBy(ScreeningSchool::getHasInfectionScreen);
            case "participatedScreening" -> wrapper.select(ScreeningSchool::getParticipatedScreening)
                    .isNotNull(ScreeningSchool::getParticipatedScreening).ne(ScreeningSchool::getParticipatedScreening, "")
                    .groupBy(ScreeningSchool::getParticipatedScreening);
            case "screenResult" -> wrapper.select(ScreeningSchool::getScreenResult)
                    .isNotNull(ScreeningSchool::getScreenResult).ne(ScreeningSchool::getScreenResult, "")
                    .groupBy(ScreeningSchool::getScreenResult);
            case "sputumCultureResult" -> wrapper.select(ScreeningSchool::getSputumCultureResult)
                    .isNotNull(ScreeningSchool::getSputumCultureResult).ne(ScreeningSchool::getSputumCultureResult, "")
                    .groupBy(ScreeningSchool::getSputumCultureResult);
            case "molecularBiologyResult" -> wrapper.select(ScreeningSchool::getMolecularBiologyResult)
                    .isNotNull(ScreeningSchool::getMolecularBiologyResult).ne(ScreeningSchool::getMolecularBiologyResult, "")
                    .groupBy(ScreeningSchool::getMolecularBiologyResult);
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
    }

    private String extractDistinctValue(ScreeningSchool row, String field) {
        return switch (field) {
            case "gender" -> row.getGender();
            case "year" -> row.getYear();
            case "city" -> row.getCity();
            case "district" -> row.getDistrict();
            case "township" -> row.getTownship();
            case "ethnicity" -> row.getEthnicity();
            case "idType" -> row.getIdType();
            case "schoolType" -> row.getSchoolType();
            case "boardingType" -> row.getBoardingType();
            case "screenMethod" -> row.getScreenMethod();
            case "infectionResult" -> row.getInfectionResult();
            case "diagnosisFirst" -> row.getDiagnosisFirst();
            case "hasChestXray" -> row.getHasChestXray();
            case "chestXrayMethod" -> row.getChestXrayMethod();
            case "chestXrayResult" -> row.getChestXrayResult();
            case "tbHistory" -> row.getTbHistory();
            case "closeContactHistory" -> row.getCloseContactHistory();
            case "suspiciousSymptoms" -> row.getSuspiciousSymptoms();
            case "hasInfectionScreen" -> row.getHasInfectionScreen();
            case "participatedScreening" -> row.getParticipatedScreening();
            case "screenResult" -> row.getScreenResult();
            case "sputumCultureResult" -> row.getSputumCultureResult();
            case "molecularBiologyResult" -> row.getMolecularBiologyResult();
            default -> null;
        };
    }
}
