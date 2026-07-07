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
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.UploadBatchSupport;
import cn.luyou.utils.ScreeningDiagnosisSupport;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Override
    public Map<String, Object> previewUpload(MultipartFile file, String sourceType) {
        final String resolvedSourceType = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
        ImportResult parseResult = new ImportResult();
        List<ScreeningKeyPopulation> dataList = parseExcelFile(file, resolvedSourceType, null, parseResult);

        List<Map<String, String>> duplicates = new ArrayList<>();
        int newCount = 0;
        for (ScreeningKeyPopulation data : dataList) {
            if (StrUtil.isBlank(data.getIdNumber())) {
                newCount++;
                continue;
            }
            ScreeningKeyPopulation existing = findExistingByIdNumber(data.getIdNumber(), resolvedSourceType);
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

    @Override
    public ImportResult uploadAndParse(MultipartFile file, String sourceType, boolean overwrite) {
        final String resolvedSourceType = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
        String batchId = UploadBatchSupport.newBatchId("重点人群筛查");
        ImportResult result = new ImportResult();
        List<ScreeningKeyPopulation> dataList = parseExcelFile(file, resolvedSourceType, batchId, result);

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        List<ScreeningKeyPopulation> toInsert = new ArrayList<>();
        List<ScreeningKeyPopulation> toUpdate = new ArrayList<>();
        int skippedCount = 0;
        int duplicateCount = 0;

        for (ScreeningKeyPopulation d : dataList) {
            if (StrUtil.isNotBlank(d.getDiagnosisFirst())) {
                d.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(d.getDiagnosisFirst()));
            }
            if (StrUtil.isBlank(d.getIdNumber())) {
                toInsert.add(d);
                continue;
            }
            ScreeningKeyPopulation existing = findExistingByIdNumber(d.getIdNumber(), resolvedSourceType);
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

        // 仅对新插入且感染筛查阳性、或已含胸片+首次诊断的记录自动创建潜伏感染记录。
        // 注意：diagnosisResult 不在此处预填，需操作员在"待诊断"页面点击"诊断"后由
        // referral 流程写入；否则会被潜伏列表的 diagnosisResult 过滤器排除，
        // 导致导入的确诊/疑似记录在"待诊断"中不可见。
        List<LatentInfection> latentList = toInsert.stream()
                .filter(d -> d.getIsLatent() == 1)
                .map(d -> buildLatentInfection(d))
                .toList();
        // 更新的记录中，若 isLatent 变为1且尚无潜伏感染记录，则补创建
        List<LatentInfection> latentFromUpdated = toUpdate.stream()
                .filter(d -> d.getIsLatent() == 1)
                .filter(d -> !latentInfectionService.lambdaQuery()
                        .eq(LatentInfection::getScreeningId, d.getId())
                        .in(LatentInfection::getPopulationType, "keyPopulation", "regular")
                        .exists())
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
                                                      ImportResult result) {
        List<ScreeningKeyPopulation> dataList = new ArrayList<>();
        AtomicInteger rowNum = new AtomicInteger(5); // 数据从第5行开始

        try {
            // 重点人群模板：第1行大分组，第2行字段名，第3行子字段细项，第4行空行，数据从第5行开始
            EasyExcel.read(file.getInputStream(), ScreeningKeyPopulation.class, new ReadListener<ScreeningKeyPopulation>() {
                @Override
                public void invoke(ScreeningKeyPopulation data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "身份证号格式不正确");
                    }
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "手机号格式不正确");
                    }
                    if (StrUtil.isNotBlank(batchId)) {
                        data.setUploadBatch(batchId);
                    }
                    data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
                    data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
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
            }).sheet().headRowNumber(4).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        return dataList;
    }

    private ScreeningKeyPopulation findExistingByIdNumber(String idNumber, String sourceType) {
        return lambdaQuery()
                .eq(ScreeningKeyPopulation::getIdNumber, idNumber)
                .eq(ScreeningKeyPopulation::getSourceType, sourceType)
                .last("LIMIT 1")
                .one();
    }

    private void mergeIntoExisting(ScreeningKeyPopulation existing, ScreeningKeyPopulation imported) {
        if (StrUtil.isNotBlank(imported.getName())) existing.setName(imported.getName());
        if (StrUtil.isNotBlank(imported.getPhone())) existing.setPhone(imported.getPhone());
        if (StrUtil.isNotBlank(imported.getCurrentAddress())) existing.setCurrentAddress(imported.getCurrentAddress());
        if (StrUtil.isNotBlank(imported.getInfectionResult())) existing.setInfectionResult(imported.getInfectionResult());
        if (StrUtil.isNotBlank(imported.getHasChestXray())) existing.setHasChestXray(imported.getHasChestXray());
        if (imported.getChestXrayDate() != null) existing.setChestXrayDate(imported.getChestXrayDate());
        if (StrUtil.isNotBlank(imported.getChestXrayResult())) existing.setChestXrayResult(imported.getChestXrayResult());
        if (StrUtil.isNotBlank(imported.getDiagnosisFirst())) {
            existing.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(imported.getDiagnosisFirst()));
        }
        if (StrUtil.isNotBlank(imported.getRemark())) existing.setRemark(imported.getRemark());
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
            if (d.getIsLatent() != 1) {
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
                    .eq(LatentInfection::getId, latent.getId())
                    .set(LatentInfection::getName, d.getName())
                    .set(LatentInfection::getIdNumber, d.getIdNumber())
                    .set(LatentInfection::getGender, d.getGender())
                    .set(LatentInfection::getAge, d.getAge())
                    .set(LatentInfection::getPhone, d.getPhone())
                    .set(LatentInfection::getInfectionResult, d.getInfectionResult())
                    .set(LatentInfection::getHasChestXray, d.getHasChestXray())
                    .set(LatentInfection::getChestXrayDate, d.getChestXrayDate())
                    .set(LatentInfection::getChestXrayResult, d.getChestXrayResult())
                    .set(LatentInfection::getDiagnosisFirst, latentDiagnosisFirst(d));
            update.update();
            latent.setDiagnosisFirst(latentDiagnosisFirst(d));
            latentInfectionService.autoReferralForDirectDiagnosis(List.of(latent));
        }
    }

    @Override
    public IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                                    String phone, String district, String townshipCommunity,
                                                    String crowdCategory, String screenMethod, Integer isLatent,
                                                    String sourceType, String diagnosisFirst,
                                                    String dateFrom, String dateTo, String entryUnit,
                                                    String createTimeFrom, String createTimeTo) {
        LocalDate screenFrom = QueryDateRangeUtil.parseLocalDate(dateFrom);
        LocalDate screenTo = QueryDateRangeUtil.parseLocalDate(dateTo);
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(createTimeFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(createTimeTo);
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        // sourceType 为空时默认只查 keyPopulation（向后兼容），传 regular 时查常规
        String resolvedSource = StrUtil.isBlank(sourceType) ? "keyPopulation" : sourceType;
        wrapper.eq(ScreeningKeyPopulation::getSourceType, resolvedSource)
                .like(StrUtil.isNotBlank(name), ScreeningKeyPopulation::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningKeyPopulation::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), ScreeningKeyPopulation::getPhone, phone)
                .eq(StrUtil.isNotBlank(district), ScreeningKeyPopulation::getDistrict, district)
                .like(StrUtil.isNotBlank(townshipCommunity), ScreeningKeyPopulation::getTownshipCommunity, townshipCommunity)
                .like(StrUtil.isNotBlank(screenMethod), ScreeningKeyPopulation::getScreenMethod, screenMethod)
                .eq(isLatent != null, ScreeningKeyPopulation::getIsLatent, isLatent)
                .ge(screenFrom != null, ScreeningKeyPopulation::getScreenDate, screenFrom)
                .le(screenTo != null, ScreeningKeyPopulation::getScreenDate, screenTo)
                .ge(createFrom != null, ScreeningKeyPopulation::getCreateTime, createFrom)
                .le(createTo != null, ScreeningKeyPopulation::getCreateTime, createTo);
        ScreeningDiagnosisSupport.applyScreeningDiagnosisFilter(
                wrapper, ScreeningKeyPopulation::getIsLatent, ScreeningKeyPopulation::getDiagnosisFirst, diagnosisFirst);
        applyEntryUnitFilter(wrapper, entryUnit);
        applyCrowdCategoryFilter(wrapper, crowdCategory);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        wrapper.orderByDesc(ScreeningKeyPopulation::getCreateTime);
        return page(new Page<>(page, size), wrapper);
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

    /** 人群分类：支持逗号分隔多选，同时满足所选分类（AND） */
    private void applyCrowdCategoryFilter(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper, String crowdCategory) {
        if (StrUtil.isBlank(crowdCategory)) {
            return;
        }
        for (String category : crowdCategory.split(",")) {
            if (StrUtil.isNotBlank(category)) {
                applySingleCrowdCategoryFilter(wrapper, category.trim());
            }
        }
    }

    private void applySingleCrowdCategoryFilter(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper, String crowdCategory) {
        switch (crowdCategory) {
            case "密接" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryClose, "是");
            case "学生" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryStudent, "是");
            case "教职工" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryTeacher, "是");
            case "老年人" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryElder, "是");
            case "糖尿病" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryDiabetes, "是");
            case "双感" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryDual, "是");
            case "既往结核史" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryTbHist, "是");
            case "非重点人群" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryNormal, "是");
            default -> {}
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScreening(ScreeningKeyPopulation data) {
        if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }

        if (StrUtil.isNotBlank(data.getDiagnosisFirst())) {
            data.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst()));
        }
        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
        save(data);

        if (data.getIsLatent() == 1) {
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
        return ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst());
    }

    private boolean shouldMarkLatent(ScreeningKeyPopulation data) {
        if (data == null) return false;
        return ScreeningDiagnosisSupport.shouldMarkLatent(
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
        if (StrUtil.isNotBlank(data.getDiagnosisFirst())) {
            data.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst()));
        }
        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        if (StrUtil.isBlank(data.getSourceType())) {
            data.setSourceType(existing.getSourceType());
        }
        if (data.getDepartmentId() == null) {
            data.setDepartmentId(existing.getDepartmentId());
        }
        updateById(data);
        ScreeningKeyPopulation updated = getById(data.getId());
        syncLatentFromScreening(List.of(updated));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScreeningCascade(Long id) {
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
}
