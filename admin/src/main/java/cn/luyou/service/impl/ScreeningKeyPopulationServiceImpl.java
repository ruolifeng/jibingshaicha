package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.Referral;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.SysMessage;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
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
import cn.luyou.service.ScreeningKeyPopulationService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.ColumnDistinctSupport;
import cn.luyou.utils.ColumnFilterSupport;
import cn.luyou.utils.CreatorUserSupport;
import cn.luyou.utils.IdentityFormatFilterSupport;
import cn.luyou.utils.ImportDuplicateIdSupport;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.InfectionScreenFieldSupport;
import cn.luyou.utils.ListSortSupport;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.UploadBatchSupport;
import cn.luyou.utils.ScreeningCrowdCategoryFilterSupport;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import cn.luyou.utils.ScreeningImportMergeSupport;
import cn.luyou.utils.ScreeningScopeHelper;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningKeyPopulationServiceImpl extends ServiceImpl<ScreeningKeyPopulationMapper, ScreeningKeyPopulation>
        implements ScreeningKeyPopulationService {

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
            "name", "year", "city", "district", "gender", "idNumber", "phone", "ethnicity",
            "townshipCommunity", "currentAddress", "screenMethod", "infectionResult",
            "diagnosisFirst", "hasChestXray", "chestXrayResult", "remark", "creatorUsername",
            "householdAddress", "idType", "crowdCategoryClose", "crowdCategoryStudent",
            "crowdCategoryTeacher", "crowdCategoryElder", "crowdCategoryDiabetes",
            "crowdCategoryDual", "crowdCategoryTbHist", "crowdCategoryNormal",
            "hasSuspiciousSymptoms", "screenResult"
    );
    private static final Set<String> COLUMN_FILTER_EQ_FIELDS = Set.of(
            "gender", "diagnosisFirst", "infectionResult", "hasChestXray", "chestXrayResult",
            "screenMethod", "year", "city", "district", "ethnicity", "idType", "townshipCommunity",
            "crowdCategoryClose", "crowdCategoryStudent", "crowdCategoryTeacher",
            "crowdCategoryElder", "crowdCategoryDiabetes", "crowdCategoryDual",
            "crowdCategoryTbHist", "crowdCategoryNormal", "hasSuspiciousSymptoms", "screenResult"
    );
    /** 表头 Excel 式下拉：仅枚举/导入内容类字段 */
    private static final Set<String> COLUMN_DISTINCT_FIELDS = Set.of(
            "gender", "district", "city", "year", "ethnicity", "idType", "screenMethod",
            "infectionResult", "diagnosisFirst", "hasChestXray", "chestXrayResult", "townshipCommunity",
            "creatorUsername", "entryUnit"
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
            Map.entry("creatorUsername", "creator_username")
    );

    @Override
    public Map<String, Object> previewUpload(MultipartFile file, String sourceType) {
        final String resolvedSourceType = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
        ImportResult parseResult = new ImportResult();
        List<ScreeningKeyPopulation> dataList = parseExcelFile(file, resolvedSourceType, null, parseResult);

        Map<String, ScreeningKeyPopulation> existingByIdNumber = findExistingByIdNumbers(
                dataList.stream().map(ScreeningKeyPopulation::getIdNumber).toList(),
                resolvedSourceType);

        List<Map<String, String>> duplicates = new ArrayList<>();
        int newCount = 0;
        for (ScreeningKeyPopulation data : dataList) {
            if (StrUtil.isBlank(data.getIdNumber())) {
                newCount++;
                continue;
            }
            ScreeningKeyPopulation existing = existingByIdNumber.get(data.getIdNumber());
            if (existing != null) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("name", StrUtil.blankToDefault(data.getName(), existing.getName()));
                item.put("idNumber", data.getIdNumber());
                duplicates.add(item);
            } else {
                newCount++;
            }
        }

        Map<String, Object> preview = new HashMap<>();
        preview.put("duplicateCount", duplicates.size());
        preview.put("newCount", newCount);
        preview.put("duplicates", duplicates);
        return preview;
    }

    public ImportResult uploadAndParse(MultipartFile file, String sourceType, boolean overwrite) {
        return uploadAndParse(file, sourceType, overwrite, false);
    }

    @Override
    public ImportResult uploadAndParse(MultipartFile file, String sourceType, boolean overwrite, boolean confirmSkipInvalid) {
        return uploadAndParse(file, sourceType, overwrite, confirmSkipInvalid, false);
    }

    @Override
    public ImportResult uploadAndParse(MultipartFile file, String sourceType, boolean overwrite,
                                       boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile) {
        final String resolvedSourceType = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
        String batchId = UploadBatchSupport.newBatchId("重点人群筛查");
        ImportResult result = new ImportResult();
        List<ScreeningKeyPopulation> dataList = parseExcelFile(file, resolvedSourceType, batchId, result, confirmSkipInvalid);

        if (ImportIdentitySupport.shouldBlockImport(result, confirmSkipInvalid)) {
            return result;
        }

        dataList = ImportDuplicateIdSupport.handleDuplicateInFile(
                result,
                dataList,
                d -> ImportDuplicateIdSupport.normalizeIdNumber(d.getIdNumber()),
                ScreeningKeyPopulation::getImportRowNo,
                ScreeningKeyPopulation::getIdNumber,
                ScreeningKeyPopulation::getName,
                confirmSkipDuplicateInFile);
        if (dataList == null) {
            return result;
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        Map<String, ScreeningKeyPopulation> existingByIdNumber = findExistingByIdNumbers(
                dataList.stream().map(ScreeningKeyPopulation::getIdNumber).toList(),
                resolvedSourceType);

        List<ScreeningKeyPopulation> toInsert = new ArrayList<>();
        List<ScreeningKeyPopulation> toUpdate = new ArrayList<>();
        int skippedCount = 0;
        int duplicateCount = 0;

        for (ScreeningKeyPopulation d : dataList) {
            ScreeningDiagnosisSupport.applyNormalizedKeyPopulationDiagnosis(
                    d::setDiagnosisFirst, d.getDiagnosisFirst(),
                    d::setDiagnosisHalfYear, d.getDiagnosisHalfYear(),
                    d::setDiagnosisOneYear, d.getDiagnosisOneYear());
            if (StrUtil.isBlank(d.getIdNumber())) {
                toInsert.add(d);
                continue;
            }
            ScreeningKeyPopulation existing = existingByIdNumber.get(d.getIdNumber());
            if (existing != null) {
                duplicateCount++;
                if (!overwrite) {
                    skippedCount++;
                    continue;
                }
                mergeIntoExisting(existing, d);
                toUpdate.add(existing);
            } else {
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        result.setInsertCount(toInsert.size());
        result.setUpdateCount(toUpdate.size());
        result.setSkippedCount(skippedCount);
        result.setDuplicateCount(duplicateCount);
        result.setSuccessCount(toInsert.size() + toUpdate.size());
        for (ScreeningKeyPopulation d : toInsert) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                ImportIdentitySupport.registerMissingIdWarning(
                        result, d.getImportRowNo() == null ? 0 : d.getImportRowNo(), d.getName());
            }
        }
        for (ScreeningKeyPopulation d : toUpdate) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                ImportIdentitySupport.registerMissingIdWarning(
                        result, d.getImportRowNo() == null ? 0 : d.getImportRowNo(), d.getName());
            }
        }

        // 仅对新插入且感染筛查阳性、或已含胸片+首次诊断的记录自动创建潜伏感染记录。
        // 注意：diagnosisResult 不在此处预填，需操作员在"待诊断"页面点击"诊断"后由
        // referral 流程写入；否则会被潜伏列表的 diagnosisResult 过滤器排除，
        // 导致导入的确诊/疑似记录在"待诊断"中不可见。
        List<LatentInfection> latentList = toInsert.stream()
                .filter(d -> d.getIsLatent() == 1)
                .filter(this::shouldCreateLatentRecord)
                .map(d -> buildLatentInfection(d))
                .toList();
        // 更新的记录中，若 isLatent 变为1且尚无潜伏感染记录，则补创建（批量查已有 latent，避免逐条 exists）
        Set<Long> existingLatentScreeningIds = findExistingLatentScreeningIds(
                toUpdate.stream()
                        .filter(d -> d.getIsLatent() == 1 && d.getId() != null)
                        .filter(this::shouldCreateLatentRecord)
                        .map(ScreeningKeyPopulation::getId)
                        .toList());
        List<LatentInfection> latentFromUpdated = toUpdate.stream()
                .filter(d -> d.getIsLatent() == 1 && d.getId() != null)
                .filter(this::shouldCreateLatentRecord)
                .filter(d -> !existingLatentScreeningIds.contains(d.getId()))
                .map(this::buildLatentInfection)
                .toList();
        List<LatentInfection> allLatent = new ArrayList<>(latentList);
        allLatent.addAll(latentFromUpdated);
        if (!allLatent.isEmpty()) {
            latentInfectionService.saveBatch(allLatent, 500);
            latentInfectionService.autoReferralForDirectDiagnosis(allLatent);
            log.info("自动创建重点人群潜伏感染记录 {} 条", allLatent.size());
        }
        syncLatentFromScreening(toUpdate);

        return result;
    }

    private List<ScreeningKeyPopulation> parseExcelFile(MultipartFile file, String sourceType, String batchId,
                                                      ImportResult result, boolean confirmSkipInvalid) {
        List<ScreeningKeyPopulation> dataList = new ArrayList<>();
        // 在 Excel 解析前固定当前录入人，避免回调路径取不到登录上下文
        final CreatorUserSupport.CreatorSnapshot creator = CreatorUserSupport.resolveCurrentCreator(userMapper);
        final Long uploadDepartmentId = screeningScopeHelper.resolveUploadDepartmentId();

        try {
            // 重点人群模板：第1行大分组，第2行字段名/子分组，第3行子字段细项，数据从第4行开始
            EasyExcel.read(file.getInputStream(), ScreeningKeyPopulation.class, new ReadListener<ScreeningKeyPopulation>() {
                @Override
                public void invoke(ScreeningKeyPopulation data, AnalysisContext context) {
                    int row = context.readRowHolder().getRowIndex() + 1;
                    if (isBlankKeyPopulationRow(data)) {
                        return;
                    }
                    if (ImportIdentitySupport.registerInvalidIdentity(
                            result, row, data.getName(), data.getIdNumber(), confirmSkipInvalid)) {
                        return;
                    }
                    if (ImportIdentitySupport.isMissingBasicIdentity(data.getName(), data.getIdNumber())) {
                        return;
                    }
                    data.setIdNumber(ImportIdentitySupport.normalizeIdNumber(data.getIdNumber()));
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "身份证号格式不正确");
                    }
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "手机号格式不正确");
                    }
                    if (!InfectionScreenFieldSupport.isValidMethod(data.getScreenMethod())) {
                        result.addError(row, data.getName(),
                                "感染筛查方法仅支持：结核菌素皮肤试验_PPD/结核抗原皮肤试验_EC/γ干扰素释放试验_IGRA/未做（兼容PPD/EC/IGRA/未查）");
                        return;
                    }
                    if (!InfectionScreenFieldSupport.isValidResult(data.getInfectionResult())) {
                        result.addError(row, data.getName(),
                                "感染筛查结果仅支持：一般阳性/中度阳性/强阳性/阳性/阴性/未判读");
                        return;
                    }
                    if (!ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisFirst())
                            || !ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisHalfYear())
                            || !ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisOneYear())) {
                        result.addError(row, data.getName(),
                                "诊断结果仅支持：排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者");
                        return;
                    }
                    InfectionScreenFieldSupport.applyNormalized(
                            data::setScreenMethod, data.getScreenMethod(),
                            data::setInfectionResult, data.getInfectionResult());
                    ScreeningDiagnosisSupport.applyNormalizedKeyPopulationDiagnosis(
                            data::setDiagnosisFirst, data.getDiagnosisFirst(),
                            data::setDiagnosisHalfYear, data.getDiagnosisHalfYear(),
                            data::setDiagnosisOneYear, data.getDiagnosisOneYear());
                    if (StrUtil.isNotBlank(batchId)) {
                        data.setUploadBatch(batchId);
                    }
                    data.setImportRowNo(row);
                    CreatorUserSupport.applyCreator(creator, data::setCreatorId, data::setCreatorUsername);
                    data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
                    data.setDepartmentId(uploadDepartmentId);
                    data.setSourceType(sourceType);
                    dataList.add(data);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("重点人群筛查数据解析完成，共 {} 条", dataList.size());
                }
                @Override
                public void onException(Exception exception, AnalysisContext context) throws Exception {
                    if (exception instanceof ExcelDataConvertException e) {
                        int rowIdx = e.getRowIndex() + 1;
                        int colIdx = e.getColumnIndex() + 1;
                        log.warn("重点人群第{}行第{}列数据转换失败，已跳过该行: {}", rowIdx, colIdx, e.getMessage());
                        result.addError(rowIdx, "第" + rowIdx + "行", "第" + colIdx + "列数据格式不正确，已跳过");
                    } else {
                        throw exception;
                    }
                }
            }).sheet().headRowNumber(3).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        return dataList;
    }

    private List<ScreeningKeyPopulation> parseExcelFile(MultipartFile file, String sourceType, String batchId,
                                                      ImportResult result) {
        return parseExcelFile(file, sourceType, batchId, result, false);
    }

    private static final int IMPORT_ID_LOOKUP_BATCH = 500;

    /**
     * 按证件号批量查重：数千行导入时避免逐条 SELECT。
     * 同一证件号多条时保留 id 最小的一条（与 LIMIT 1 行为接近）。
     */
    private Map<String, ScreeningKeyPopulation> findExistingByIdNumbers(List<String> idNumbers, String sourceType) {
        Map<String, ScreeningKeyPopulation> map = new HashMap<>();
        if (idNumbers == null || idNumbers.isEmpty()) {
            return map;
        }
        List<String> uniqueIds = idNumbers.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        for (int i = 0; i < uniqueIds.size(); i += IMPORT_ID_LOOKUP_BATCH) {
            List<String> chunk = uniqueIds.subList(i, Math.min(i + IMPORT_ID_LOOKUP_BATCH, uniqueIds.size()));
            LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ScreeningKeyPopulation::getIdNumber, chunk)
                    .eq(ScreeningKeyPopulation::getSourceType, sourceType)
                    .orderByAsc(ScreeningKeyPopulation::getId);
            screeningScopeHelper.applyImportDedupScope(wrapper, ScreeningKeyPopulation::getDepartmentId);
            for (ScreeningKeyPopulation row : list(wrapper)) {
                if (StrUtil.isNotBlank(row.getIdNumber())) {
                    map.putIfAbsent(row.getIdNumber().trim(), row);
                }
            }
        }
        return map;
    }

    /** 批量查询已存在潜伏记录的 screeningId，供覆盖导入补建 latent 时去重 */
    private Set<Long> findExistingLatentScreeningIds(List<Long> screeningIds) {
        if (screeningIds == null || screeningIds.isEmpty()) {
            return Set.of();
        }
        HashSet<Long> found = new HashSet<>();
        List<Long> unique = screeningIds.stream().filter(Objects::nonNull).distinct().toList();
        for (int i = 0; i < unique.size(); i += IMPORT_ID_LOOKUP_BATCH) {
            List<Long> chunk = unique.subList(i, Math.min(i + IMPORT_ID_LOOKUP_BATCH, unique.size()));
            List<LatentInfection> rows = latentInfectionService.lambdaQuery()
                    .select(LatentInfection::getScreeningId)
                    .in(LatentInfection::getScreeningId, chunk)
                    .in(LatentInfection::getPopulationType, "keyPopulation", "regular")
                    .list();
            for (LatentInfection row : rows) {
                if (row.getScreeningId() != null) {
                    found.add(row.getScreeningId());
                }
            }
        }
        return found;
    }

    private void mergeIntoExisting(ScreeningKeyPopulation existing, ScreeningKeyPopulation imported) {
        ScreeningImportMergeSupport.mergeKeyPopulation(existing, imported);
        // 覆盖导入只更新业务字段与行号，保留首次录入人；历史空值则补当前导入人
        CreatorUserSupport.fillMissingCreator(
                existing.getCreatorId(),
                existing.getCreatorUsername(),
                new CreatorUserSupport.CreatorSnapshot(imported.getCreatorId(), imported.getCreatorUsername()),
                existing::setCreatorId,
                existing::setCreatorUsername);
        existing.setIsLatent(shouldMarkLatent(existing) ? 1 : 0);
    }

    private LatentInfection buildLatentInfection(ScreeningKeyPopulation d) {
        String popType = StrUtil.isBlank(d.getSourceType()) ? "keyPopulation" : d.getSourceType();
        return LatentInfection.builder()
                .screeningId(d.getId())
                .populationType(popType)
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
    }

    /**
     * 增量导入时，将胸片与首次诊断同步到已存在的潜伏感染记录，
     * 避免筛查表已更新但待诊断列表仍为空的情况。
     */
    private void syncLatentFromScreening(List<ScreeningKeyPopulation> records) {
        for (ScreeningKeyPopulation d : records) {
            if (d.getId() == null) continue;
            String popType = StrUtil.isBlank(d.getSourceType()) ? "keyPopulation" : d.getSourceType();
            if (d.getIsLatent() != 1 || !shouldCreateLatentRecord(d)) {
                latentInfectionService.archivePendingLatentFromScreening(
                        d.getId(), popType, d.getDiagnosisFirst());
                continue;
            }
            LatentInfection latent = latentInfectionService.lambdaQuery()
                    .eq(LatentInfection::getScreeningId, d.getId())
                    .eq(LatentInfection::getPopulationType, popType)
                    .eq(LatentInfection::getArchived, 0)
                    .last("LIMIT 1")
                    .one();
            if (latent == null) {
                if (!shouldCreateLatentRecord(d)) {
                    continue;
                }
                latent = LatentInfection.builder()
                        .screeningId(d.getId())
                        .populationType(popType)
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

    @Override
    public IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                                    String phone, String district, String townshipCommunity,
                                                    String crowdCategory, String screenMethod, Integer isLatent,
                                                    String sourceType, String diagnosisFirst,
                                                    String dateFrom, String dateTo, String entryUnit,
                                                    String createTimeFrom, String createTimeTo,
                                                    String creatorUsername, String hasChestXray,
                                                    String chestXrayResult, String columnFilters,
                                                    String formatIssue, String sortField, String sortOrder) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = buildListWrapper(
                name, idNumber, phone, district, townshipCommunity, crowdCategory, screenMethod, isLatent,
                sourceType, diagnosisFirst, dateFrom, dateTo, entryUnit, createTimeFrom, createTimeTo,
                creatorUsername, hasChestXray, chestXrayResult, columnFilters, formatIssue);
        applyListOrder(wrapper, sortField, sortOrder);
        IPage<ScreeningKeyPopulation> result = page(new Page<>(page, size), wrapper);
        CreatorUserSupport.fillMissingUsernames(
                userMapper,
                result.getRecords(),
                ScreeningKeyPopulation::getCreatorId,
                ScreeningKeyPopulation::getCreatorUsername,
                ScreeningKeyPopulation::setCreatorUsername);
        return result;
    }

    @Override
    public List<ScreeningKeyPopulation> listForExport(String name, String idNumber,
                                                       String phone, String district, String townshipCommunity,
                                                       String crowdCategory, String screenMethod, Integer isLatent,
                                                       String sourceType, String diagnosisFirst,
                                                       String dateFrom, String dateTo, String entryUnit,
                                                       String createTimeFrom, String createTimeTo,
                                                       String creatorUsername, String hasChestXray,
                                                       String chestXrayResult, String columnFilters,
                                                       String formatIssue, String sortField, String sortOrder,
                                                       List<Long> ids) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper;
        if (ids != null && !ids.isEmpty()) {
            String resolvedSource = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
            wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ScreeningKeyPopulation::getId, ids)
                    .eq(ScreeningKeyPopulation::getSourceType, resolvedSource);
            screeningScopeHelper.applyDepartmentScope(
                    wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        } else {
            wrapper = buildListWrapper(
                    name, idNumber, phone, district, townshipCommunity, crowdCategory, screenMethod, isLatent,
                    sourceType, diagnosisFirst, dateFrom, dateTo, entryUnit, createTimeFrom, createTimeTo,
                    creatorUsername, hasChestXray, chestXrayResult, columnFilters, formatIssue);
        }
        applyListOrder(wrapper, sortField, sortOrder);
        List<ScreeningKeyPopulation> records = list(wrapper);
        CreatorUserSupport.fillMissingUsernames(
                userMapper,
                records,
                ScreeningKeyPopulation::getCreatorId,
                ScreeningKeyPopulation::getCreatorUsername,
                ScreeningKeyPopulation::setCreatorUsername);
        return records;
    }

    private LambdaQueryWrapper<ScreeningKeyPopulation> buildListWrapper(
            String name, String idNumber, String phone, String district, String townshipCommunity,
            String crowdCategory, String screenMethod, Integer isLatent, String sourceType,
            String diagnosisFirst, String dateFrom, String dateTo, String entryUnit,
            String createTimeFrom, String createTimeTo, String creatorUsername,
            String hasChestXray, String chestXrayResult, String columnFilters, String formatIssue) {
        LocalDate screenFrom = QueryDateRangeUtil.parseLocalDate(dateFrom);
        LocalDate screenTo = QueryDateRangeUtil.parseLocalDate(dateTo);
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(createTimeFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(createTimeTo);
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        String resolvedSource = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
        wrapper.eq(ScreeningKeyPopulation::getSourceType, resolvedSource)
                .like(StrUtil.isNotBlank(name), ScreeningKeyPopulation::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningKeyPopulation::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), ScreeningKeyPopulation::getPhone, phone)
                .eq(StrUtil.isNotBlank(district), ScreeningKeyPopulation::getDistrict, district)
                .eq(StrUtil.isNotBlank(townshipCommunity), ScreeningKeyPopulation::getTownshipCommunity, townshipCommunity)
                .eq(StrUtil.isNotBlank(screenMethod), ScreeningKeyPopulation::getScreenMethod, screenMethod)
                .eq(isLatent != null, ScreeningKeyPopulation::getIsLatent, isLatent)
                .like(StrUtil.isNotBlank(creatorUsername), ScreeningKeyPopulation::getCreatorUsername, creatorUsername)
                .eq(StrUtil.isNotBlank(hasChestXray), ScreeningKeyPopulation::getHasChestXray, hasChestXray)
                .eq(StrUtil.isNotBlank(chestXrayResult), ScreeningKeyPopulation::getChestXrayResult, chestXrayResult)
                .ge(screenFrom != null, ScreeningKeyPopulation::getScreenDate, screenFrom)
                .le(screenTo != null, ScreeningKeyPopulation::getScreenDate, screenTo)
                .ge(createFrom != null, ScreeningKeyPopulation::getCreateTime, createFrom)
                .le(createTo != null, ScreeningKeyPopulation::getCreateTime, createTo);
        ScreeningDiagnosisSupport.applyScreeningDiagnosisFilter(
                wrapper, ScreeningKeyPopulation::getIsLatent, ScreeningKeyPopulation::getDiagnosisFirst, diagnosisFirst);
        applyEntryUnitFilter(wrapper, entryUnit);
        ScreeningCrowdCategoryFilterSupport.applyFilter(wrapper, crowdCategory);
        applyColumnFilters(wrapper, columnFilters);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        IdentityFormatFilterSupport.apply(wrapper, formatIssue, "id_number", "phone");
        return wrapper;
    }

    private void applyListOrder(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper,
                                String sortField, String sortOrder) {
        ListSortSupport.apply(wrapper, sortField, sortOrder, SORT_COLUMNS, ImportRowOrderSupport.WITH_BATCH);
    }

    private void applyColumnFilters(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper, String columnFilters) {
        Map<String, String> filters = ColumnFilterSupport.parse(columnFilters);
        ColumnFilterSupport.applyLambda(filters, COLUMN_FILTER_WHITELIST, (field, value) -> {
            switch (field) {
                case "name" -> ColumnFilterSupport.like(wrapper, ScreeningKeyPopulation::getName, value);
                case "year" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getYear, value);
                case "city" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCity, value);
                case "district" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getDistrict, value);
                case "gender" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getGender, value);
                case "idNumber" -> ColumnFilterSupport.like(wrapper, ScreeningKeyPopulation::getIdNumber, value);
                case "phone" -> ColumnFilterSupport.like(wrapper, ScreeningKeyPopulation::getPhone, value);
                case "ethnicity" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getEthnicity, value);
                case "townshipCommunity" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getTownshipCommunity, value);
                case "currentAddress" -> ColumnFilterSupport.like(wrapper, ScreeningKeyPopulation::getCurrentAddress, value);
                case "householdAddress" -> ColumnFilterSupport.like(wrapper, ScreeningKeyPopulation::getHouseholdAddress, value);
                case "idType" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getIdType, value);
                case "screenMethod" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getScreenMethod, value);
                case "infectionResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getInfectionResult, value);
                case "diagnosisFirst" -> ScreeningDiagnosisSupport.applyScreeningDiagnosisColumnFilter(
                        wrapper, ScreeningKeyPopulation::getIsLatent, ScreeningKeyPopulation::getDiagnosisFirst, value);
                case "hasChestXray" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getHasChestXray, value);
                case "chestXrayResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getChestXrayResult, value);
                case "remark" -> ColumnFilterSupport.like(wrapper, ScreeningKeyPopulation::getRemark, value);
                case "creatorUsername" -> ColumnFilterSupport.like(wrapper, ScreeningKeyPopulation::getCreatorUsername, value);
                case "crowdCategoryClose" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryClose, value);
                case "crowdCategoryStudent" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryStudent, value);
                case "crowdCategoryTeacher" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryTeacher, value);
                case "crowdCategoryElder" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryElder, value);
                case "crowdCategoryDiabetes" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryDiabetes, value);
                case "crowdCategoryDual" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryDual, value);
                case "crowdCategoryTbHist" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryTbHist, value);
                case "crowdCategoryNormal" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getCrowdCategoryNormal, value);
                case "hasSuspiciousSymptoms" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getHasSuspiciousSymptoms, value);
                case "screenResult" -> ColumnFilterSupport.eqOrIn(wrapper, ScreeningKeyPopulation::getScreenResult, value);
                default -> { }
            }
        });
    }

    /** 录入单位：按部门名称模糊匹配 department_id */
    private void applyEntryUnitFilter(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper, String entryUnit) {
        if (StrUtil.isBlank(entryUnit)) {
            return;
        }
        List<Long> deptIds = departmentService.resolveIdsByNameLike(entryUnit);
        if (deptIds.isEmpty()) {
            wrapper.eq(ScreeningKeyPopulation::getId, -1L);
        } else {
            wrapper.in(ScreeningKeyPopulation::getDepartmentId, deptIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScreening(ScreeningKeyPopulation data) {
        data.setIdNumber(ImportIdentitySupport.normalizeIdNumber(data.getIdNumber()));
        if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }

        if (!ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisFirst())
                || !ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisHalfYear())
                || !ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(data.getDiagnosisOneYear())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "诊断结果仅支持：排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者");
        }
        ScreeningDiagnosisSupport.applyNormalizedKeyPopulationDiagnosis(
                data::setDiagnosisFirst, data.getDiagnosisFirst(),
                data::setDiagnosisHalfYear, data.getDiagnosisHalfYear(),
                data::setDiagnosisOneYear, data.getDiagnosisOneYear());
        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
        CreatorUserSupport.fillCurrentCreator(userMapper, data::setCreatorId, data::setCreatorUsername);
        save(data);

        if (data.getIsLatent() == 1 && shouldCreateLatentRecord(data)) {
            // diagnosisResult 不在此预填，由"待诊断"页面诊断后由 referral 流程写入
            String popType = StrUtil.isBlank(data.getSourceType()) ? "keyPopulation" : data.getSourceType();
            LatentInfection latent = LatentInfection.builder()
                    .screeningId(data.getId())
                    .populationType(popType)
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

    private String latentDiagnosisFirst(ScreeningKeyPopulation data) {
        if (data == null) {
            return null;
        }
        if (StrUtil.isBlank(data.getDiagnosisFirst())) {
            return null;
        }
        String normalized = ScreeningDiagnosisSupport.normalizeKeyPopulationDiagnosis(data.getDiagnosisFirst());
        return normalized != null ? normalized : data.getDiagnosisFirst();
    }

    private boolean shouldMarkLatent(ScreeningKeyPopulation data) {
        if (data == null) return false;
        return ScreeningDiagnosisSupport.shouldMarkLatent(
                data.getInfectionResult(),
                data.getChestXrayResult(),
                data.getHasChestXray(),
                data.getDiagnosisFirst());
    }

    /** 确诊结核/在治患者仅筛查列表标红，不建潜伏记录 */
    private boolean shouldCreateLatentRecord(ScreeningKeyPopulation data) {
        if (data == null) return false;
        return ScreeningDiagnosisSupport.shouldCreateLatentRecord(
                data.getInfectionResult(),
                data.getChestXrayResult(),
                data.getHasChestXray(),
                data.getDiagnosisFirst());
    }

    private boolean isValidIdCard(String id) {
        // 仅校验格式（18位 + 字符规则）。Excel 以数值型存储身份证号时会丢失浮点精度，导致校验码错误，故不做校验码验证。
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScreening(ScreeningKeyPopulation data) {
        ScreeningKeyPopulation existing = getById(data.getId());
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
        if (StrUtil.isNotBlank(data.getDiagnosisFirst())) {
            String normalized = ScreeningDiagnosisSupport.normalizeKeyPopulationDiagnosis(data.getDiagnosisFirst());
            if (normalized == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID,
                        "诊断结果仅支持：排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者");
            }
            data.setDiagnosisFirst(normalized);
        } else {
            // 显式清空：与前端 clearable 提交的 null/"" 对齐，避免保留旧诊断
            data.setDiagnosisFirst(null);
        }
        if (StrUtil.isNotBlank(data.getDiagnosisHalfYear())) {
            String normalized = ScreeningDiagnosisSupport.normalizeKeyPopulationDiagnosis(data.getDiagnosisHalfYear());
            if (normalized == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID,
                        "半年诊断结果仅支持：排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者");
            }
            data.setDiagnosisHalfYear(normalized);
        }
        if (StrUtil.isNotBlank(data.getDiagnosisOneYear())) {
            String normalized = ScreeningDiagnosisSupport.normalizeKeyPopulationDiagnosis(data.getDiagnosisOneYear());
            if (normalized == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID,
                        "一年诊断结果仅支持：排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者");
            }
            data.setDiagnosisOneYear(normalized);
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
        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        if (StrUtil.isBlank(data.getSourceType())) {
            data.setSourceType(existing.getSourceType());
        }
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
                ScreeningKeyPopulation::getCreatorId,
                ScreeningKeyPopulation::getCreatorUsername,
                ScreeningKeyPopulation::setCreatorUsername);
        updateById(data);
        ScreeningKeyPopulation updated = getById(data.getId());
        syncLatentFromScreening(List.of(updated));
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
    public int deleteByFilter(String name, String idNumber, String phone, String district, String townshipCommunity,
                               String crowdCategory, String screenMethod, Integer isLatent, String sourceType,
                               String diagnosisFirst, String dateFrom, String dateTo, String entryUnit,
                               String createTimeFrom, String createTimeTo, String creatorUsername,
                               String hasChestXray, String chestXrayResult, String columnFilters, String formatIssue) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = buildListWrapper(
                name, idNumber, phone, district, townshipCommunity, crowdCategory, screenMethod, isLatent,
                sourceType, diagnosisFirst, dateFrom, dateTo, entryUnit, createTimeFrom, createTimeTo,
                creatorUsername, hasChestXray, chestXrayResult, columnFilters, formatIssue);
        wrapper.select(ScreeningKeyPopulation::getId);
        List<Long> ids = list(wrapper).stream().map(ScreeningKeyPopulation::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        batchDeleteCascade(ids);
        return ids.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAll(String sourceType) {
        return deleteByFilter(null, null, null, null, null, null, null, null, sourceType,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private void doDeleteScreeningCascade(Long id) {
        if (getById(id) == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        List<LatentInfection> latentList = latentInfectionService.lambdaQuery()
                .eq(LatentInfection::getScreeningId, id)
                .eq(LatentInfection::getPopulationType, "keyPopulation")
                .list();
        for (LatentInfection latent : latentList) {
            deleteCascadeFromLatent(latent.getId());
        }
        removeById(id);
        log.info("级联删除重点人群筛查记录 id={}", id);
    }

    private void deleteCascadeFromLatent(Long latentId) {
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
                .eq(cn.luyou.model.SupervisionForm::getLatentInfectionId, latentId).remove();
        latentFollowUpService.lambdaUpdate()
                .eq(cn.luyou.model.LatentFollowUp::getLatentInfectionId, latentId).remove();
        latentCheckService.lambdaUpdate()
                .eq(cn.luyou.model.LatentCheck::getLatentInfectionId, latentId).remove();
        deleteNoticeAndMessages(latentId, "latent");
        deleteReferralsAndMessages(latentId);
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

    private boolean isBlankKeyPopulationRow(ScreeningKeyPopulation data) {
        return data == null || (StrUtil.isBlank(data.getName())
                && ImportIdentitySupport.isBlankOrPlaceholder(data.getIdNumber()));
    }

    @Override
    public List<String> listDistinctColumnValues(String field, String sourceType) {
        if (StrUtil.isBlank(field) || !COLUMN_DISTINCT_FIELDS.contains(field)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
        String resolvedSource = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScreeningKeyPopulation::getSourceType, resolvedSource);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        if ("entryUnit".equals(field)) {
            wrapper.select(ScreeningKeyPopulation::getDepartmentId)
                    .isNotNull(ScreeningKeyPopulation::getDepartmentId)
                    .groupBy(ScreeningKeyPopulation::getDepartmentId);
            List<Long> deptIds = list(wrapper).stream()
                    .map(ScreeningKeyPopulation::getDepartmentId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (deptIds.isEmpty()) {
                return List.of();
            }
            return ColumnDistinctSupport.normalize(
                    departmentService.listByIds(deptIds).stream()
                            .map(dept -> dept == null ? null : dept.getName())
                            .toList()
            );
        }
        applyDistinctSelect(wrapper, field);
        return ColumnDistinctSupport.normalize(list(wrapper).stream()
                .map(row -> extractDistinctValue(row, field))
                .toList());
    }

    private void applyDistinctSelect(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper, String field) {
        switch (field) {
            case "gender" -> wrapper.select(ScreeningKeyPopulation::getGender)
                    .isNotNull(ScreeningKeyPopulation::getGender).ne(ScreeningKeyPopulation::getGender, "")
                    .groupBy(ScreeningKeyPopulation::getGender);
            case "district" -> wrapper.select(ScreeningKeyPopulation::getDistrict)
                    .isNotNull(ScreeningKeyPopulation::getDistrict).ne(ScreeningKeyPopulation::getDistrict, "")
                    .groupBy(ScreeningKeyPopulation::getDistrict);
            case "city" -> wrapper.select(ScreeningKeyPopulation::getCity)
                    .isNotNull(ScreeningKeyPopulation::getCity).ne(ScreeningKeyPopulation::getCity, "")
                    .groupBy(ScreeningKeyPopulation::getCity);
            case "year" -> wrapper.select(ScreeningKeyPopulation::getYear)
                    .isNotNull(ScreeningKeyPopulation::getYear).ne(ScreeningKeyPopulation::getYear, "")
                    .groupBy(ScreeningKeyPopulation::getYear);
            case "ethnicity" -> wrapper.select(ScreeningKeyPopulation::getEthnicity)
                    .isNotNull(ScreeningKeyPopulation::getEthnicity).ne(ScreeningKeyPopulation::getEthnicity, "")
                    .groupBy(ScreeningKeyPopulation::getEthnicity);
            case "idType" -> wrapper.select(ScreeningKeyPopulation::getIdType)
                    .isNotNull(ScreeningKeyPopulation::getIdType).ne(ScreeningKeyPopulation::getIdType, "")
                    .groupBy(ScreeningKeyPopulation::getIdType);
            case "screenMethod" -> wrapper.select(ScreeningKeyPopulation::getScreenMethod)
                    .isNotNull(ScreeningKeyPopulation::getScreenMethod).ne(ScreeningKeyPopulation::getScreenMethod, "")
                    .groupBy(ScreeningKeyPopulation::getScreenMethod);
            case "infectionResult" -> wrapper.select(ScreeningKeyPopulation::getInfectionResult)
                    .isNotNull(ScreeningKeyPopulation::getInfectionResult).ne(ScreeningKeyPopulation::getInfectionResult, "")
                    .groupBy(ScreeningKeyPopulation::getInfectionResult);
            case "diagnosisFirst" -> wrapper.select(ScreeningKeyPopulation::getDiagnosisFirst)
                    .isNotNull(ScreeningKeyPopulation::getDiagnosisFirst).ne(ScreeningKeyPopulation::getDiagnosisFirst, "")
                    .groupBy(ScreeningKeyPopulation::getDiagnosisFirst);
            case "hasChestXray" -> wrapper.select(ScreeningKeyPopulation::getHasChestXray)
                    .isNotNull(ScreeningKeyPopulation::getHasChestXray).ne(ScreeningKeyPopulation::getHasChestXray, "")
                    .groupBy(ScreeningKeyPopulation::getHasChestXray);
            case "chestXrayResult" -> wrapper.select(ScreeningKeyPopulation::getChestXrayResult)
                    .isNotNull(ScreeningKeyPopulation::getChestXrayResult).ne(ScreeningKeyPopulation::getChestXrayResult, "")
                    .groupBy(ScreeningKeyPopulation::getChestXrayResult);
            case "townshipCommunity" -> wrapper.select(ScreeningKeyPopulation::getTownshipCommunity)
                    .isNotNull(ScreeningKeyPopulation::getTownshipCommunity).ne(ScreeningKeyPopulation::getTownshipCommunity, "")
                    .groupBy(ScreeningKeyPopulation::getTownshipCommunity);
            case "creatorUsername" -> wrapper.select(ScreeningKeyPopulation::getCreatorUsername)
                    .isNotNull(ScreeningKeyPopulation::getCreatorUsername).ne(ScreeningKeyPopulation::getCreatorUsername, "")
                    .groupBy(ScreeningKeyPopulation::getCreatorUsername);
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
    }

    private String extractDistinctValue(ScreeningKeyPopulation row, String field) {
        return switch (field) {
            case "gender" -> row.getGender();
            case "district" -> row.getDistrict();
            case "city" -> row.getCity();
            case "year" -> row.getYear();
            case "ethnicity" -> row.getEthnicity();
            case "idType" -> row.getIdType();
            case "screenMethod" -> row.getScreenMethod();
            case "infectionResult" -> row.getInfectionResult();
            case "diagnosisFirst" -> row.getDiagnosisFirst();
            case "hasChestXray" -> row.getHasChestXray();
            case "chestXrayResult" -> row.getChestXrayResult();
            case "townshipCommunity" -> row.getTownshipCommunity();
            case "creatorUsername" -> row.getCreatorUsername();
            default -> null;
        };
    }
}
