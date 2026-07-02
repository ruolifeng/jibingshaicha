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
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import cn.luyou.utils.UploadBatchSupport;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public ImportResult uploadAndParse(MultipartFile file) {
        String batchId = UploadBatchSupport.newBatchId("学校筛查");
        List<ScreeningSchool> dataList = new ArrayList<>();
        ImportResult result = new ImportResult();

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

            Map<String, Integer> headerIndex = buildSchoolHeaderIndex(rows);
            if (!headerIndex.containsKey("姓名") || !headerIndex.containsKey("证件号")) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "模板表头不正确，请使用学生筛查模板或先进行数据匹配");
            }
            for (int i = 2; i < rows.size(); i++) {
                int rowNum = i + 1;
                ScreeningSchool data = mapSchoolRow(rows.get(i), headerIndex);
                if (isBlankSchoolRow(data)) {
                    continue;
                }
                if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                    result.addError(rowNum, data.getName(), "身份证号格式不正确");
                }
                if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                    result.addError(rowNum, data.getName(), "手机号格式不正确");
                }
                data.setUploadBatch(batchId);
                data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
                data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
                dataList.add(data);
            }
            log.info("学校人群筛查数据解析完成，共 {} 条", dataList.size());
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
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
            ScreeningSchool existing = lambdaQuery()
                    .eq(ScreeningSchool::getIdNumber, d.getIdNumber())
                    .last("LIMIT 1")
                    .one();
            if (existing != null) {
                // 合并基本信息，以最新导入为准
                mergeSchoolImportFields(existing, d);
                existing.setIsLatent(shouldMarkLatent(existing) ? 1 : 0);
                toUpdate.add(existing);
            } else {
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        result.setSuccessCount(dataList.size());

        // 仅对新插入且感染筛查阳性、或已含胸片+首次诊断的记录自动创建潜伏感染记录。
        // 注意：diagnosisResult 不在此处预填，需操作员在"待诊断"页面点击"诊断"后由
        // referral 流程写入；否则会被潜伏列表的 diagnosisResult 过滤器排除，
        // 导致导入的确诊/疑似记录在"待诊断"中不可见。
        // 无论导入数据是否已含胸片与诊断，trackingStatus 始终初始化为 0（待追踪）。
        // 操作员需手动完成追踪流程后，才可进行诊断。
        // 若导入数据已含 diagnosisFirst（保存在筛查表），待诊断页确认后才会写入 latent 并分流。
        List<LatentInfection> latentList = toInsert.stream()
                .filter(d -> d.getIsLatent() == 1)
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
        // 更新的记录中，若 isLatent 变为1且尚无潜伏感染记录，则补创建
        List<LatentInfection> latentFromUpdated = toUpdate.stream()
                .filter(d -> d.getIsLatent() == 1)
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
     */
    private void syncLatentFromScreening(List<ScreeningSchool> records, String populationType) {
        for (ScreeningSchool d : records) {
            if (d.getId() == null) continue;
            if (d.getIsLatent() != 1) {
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

    /** 年度筛选兜底：year 字段为空时，按感染筛查日期或胸片检查日期年份匹配 */
    private static final String SCREEN_YEAR_SQL_EXPR =
            "((screen_date IS NOT NULL AND YEAR(screen_date) = {0})"
                    + " OR (screen_date IS NULL AND chest_xray_date IS NOT NULL AND YEAR(chest_xray_date) = {0}))";

    @Override
    public IPage<ScreeningSchool> queryPage(int page, int size, String name, String idNumber,
                                             String schoolName, String district, Integer isLatent, String diagnosisFirst,
                                             String phone, String year, String entryUnit,
                                             String createTimeFrom, String createTimeTo) {
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(createTimeFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(createTimeTo);
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningSchool::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningSchool::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(schoolName), ScreeningSchool::getSchoolName, schoolName)
                .eq(StrUtil.isNotBlank(district), ScreeningSchool::getDistrict, district)
                .like(StrUtil.isNotBlank(phone), ScreeningSchool::getPhone, phone)
                .eq(isLatent != null, ScreeningSchool::getIsLatent, isLatent)
                .ge(createFrom != null, ScreeningSchool::getCreateTime, createFrom)
                .le(createTo != null, ScreeningSchool::getCreateTime, createTo);
        ScreeningDiagnosisSupport.applyScreeningDiagnosisFilter(
                wrapper, ScreeningSchool::getIsLatent, ScreeningSchool::getDiagnosisFirst, diagnosisFirst);
        applyScreenYearFilter(wrapper, year);
        applyEntryUnitFilter(wrapper, entryUnit);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        wrapper.orderByDesc(ScreeningSchool::getCreateTime);
        return page(new Page<>(page, size), wrapper);
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
            // diagnosisResult 不在此预填，由"待诊断"页面诊断后由 referral 流程写入。
            // trackingStatus 始终初始化为 0，操作员需手动完成追踪后才可进行诊断。
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

    /** 诊断结果写入潜伏表；疑似结核由分流逻辑保留在待诊断，不再归档。 */
    private String latentDiagnosisFirst(ScreeningSchool data) {
        if (data == null) {
            return null;
        }
        return ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst());
    }

    /** 增量导入：按证件号匹配时，用最新 Excel 行覆盖筛查表字段。 */
    private void mergeSchoolImportFields(ScreeningSchool existing, ScreeningSchool incoming) {
        if (StrUtil.isNotBlank(incoming.getYear())) existing.setYear(incoming.getYear());
        if (StrUtil.isNotBlank(incoming.getCity())) existing.setCity(incoming.getCity());
        if (StrUtil.isNotBlank(incoming.getDistrict())) existing.setDistrict(incoming.getDistrict());
        if (StrUtil.isNotBlank(incoming.getName())) existing.setName(incoming.getName());
        if (StrUtil.isNotBlank(incoming.getGender())) existing.setGender(incoming.getGender());
        if (incoming.getBirthDate() != null) existing.setBirthDate(incoming.getBirthDate());
        if (incoming.getAge() != null) existing.setAge(incoming.getAge());
        if (StrUtil.isNotBlank(incoming.getIdType())) existing.setIdType(incoming.getIdType());
        if (StrUtil.isNotBlank(incoming.getEthnicity())) existing.setEthnicity(incoming.getEthnicity());
        if (StrUtil.isNotBlank(incoming.getPhone())) existing.setPhone(incoming.getPhone());
        if (StrUtil.isNotBlank(incoming.getHouseholdAddress())) existing.setHouseholdAddress(incoming.getHouseholdAddress());
        if (StrUtil.isNotBlank(incoming.getCurrentAddress())) existing.setCurrentAddress(incoming.getCurrentAddress());
        if (StrUtil.isNotBlank(incoming.getSchoolType())) existing.setSchoolType(incoming.getSchoolType());
        if (StrUtil.isNotBlank(incoming.getSchoolName())) existing.setSchoolName(incoming.getSchoolName());
        if (StrUtil.isNotBlank(incoming.getClassName())) existing.setClassName(incoming.getClassName());
        if (StrUtil.isNotBlank(incoming.getTbHistory())) existing.setTbHistory(incoming.getTbHistory());
        if (StrUtil.isNotBlank(incoming.getCloseContactHistory())) existing.setCloseContactHistory(incoming.getCloseContactHistory());
        if (StrUtil.isNotBlank(incoming.getSuspiciousSymptoms())) existing.setSuspiciousSymptoms(incoming.getSuspiciousSymptoms());
        if (StrUtil.isNotBlank(incoming.getHasInfectionScreen())) existing.setHasInfectionScreen(incoming.getHasInfectionScreen());
        if (incoming.getScreenDate() != null) existing.setScreenDate(incoming.getScreenDate());
        if (StrUtil.isNotBlank(incoming.getScreenMethod())) existing.setScreenMethod(incoming.getScreenMethod());
        if (StrUtil.isNotBlank(incoming.getScreenResult())) existing.setScreenResult(incoming.getScreenResult());
        if (StrUtil.isNotBlank(incoming.getInfectionResult())) existing.setInfectionResult(incoming.getInfectionResult());
        if (StrUtil.isNotBlank(incoming.getHasChestXray())) existing.setHasChestXray(incoming.getHasChestXray());
        if (incoming.getChestXrayDate() != null) existing.setChestXrayDate(incoming.getChestXrayDate());
        if (StrUtil.isNotBlank(incoming.getChestXrayResult())) existing.setChestXrayResult(incoming.getChestXrayResult());
        if (StrUtil.isNotBlank(incoming.getSputumSmearResult())) existing.setSputumSmearResult(incoming.getSputumSmearResult());
        if (StrUtil.isNotBlank(incoming.getMolecularBiologyResult())) existing.setMolecularBiologyResult(incoming.getMolecularBiologyResult());
        if (StrUtil.isNotBlank(incoming.getDiagnosisFirst())) {
            existing.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(incoming.getDiagnosisFirst()));
        }
        if (StrUtil.isNotBlank(incoming.getRemark())) existing.setRemark(incoming.getRemark());
    }

    private Map<String, Integer> buildSchoolHeaderIndex(List<Map<Integer, String>> rows) {
        Map<String, Integer> headerIndex = new HashMap<>();
        int headerRows = Math.min(2, rows.size());
        for (int rowIdx = 0; rowIdx < headerRows; rowIdx++) {
            Map<Integer, String> row = rows.get(rowIdx);
            for (Map.Entry<Integer, String> entry : row.entrySet()) {
                putHeaderAlias(headerIndex, entry.getValue(), entry.getKey());
            }
        }
        return headerIndex;
    }

    private void putHeaderAlias(Map<String, Integer> headerIndex, String rawHeader, Integer index) {
        if (index == null || StrUtil.isBlank(rawHeader)) return;
        String header = normalizeHeader(rawHeader);
        if (StrUtil.isBlank(header)) return;
        headerIndex.putIfAbsent(header, index);
        switch (header) {
            case "年度" -> headerIndex.putIfAbsent("年份", index);
            case "区县", "县市区", "县市、区" -> headerIndex.putIfAbsent("县市区", index);
            case "身份证号", "身份证号码" -> headerIndex.putIfAbsent("证件号", index);
            case "现地址" -> headerIndex.putIfAbsent("现住址", index);
            case "有无可疑症状" -> headerIndex.putIfAbsent("结核病可疑症状", index);
            case "是否感染筛查" -> headerIndex.putIfAbsent("是否进行感染筛", index);
            case "判定结果", "感染筛查结果学校人群感染筛查情况" -> headerIndex.putIfAbsent("感染筛查结果", index);
            case "胸部DR", "胸片检查结果" -> headerIndex.putIfAbsent("胸片结果", index);
            case "痰涂片" -> headerIndex.putIfAbsent("痰涂片结果", index);
            case "分子生物学" -> headerIndex.putIfAbsent("分子生物学结果", index);
            case "诊断" -> headerIndex.putIfAbsent("诊断结果", index);
            default -> {
            }
        }
    }

    private String normalizeHeader(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\s\\n\\r（）()：:；;、/]", "").trim();
    }

    private ScreeningSchool mapSchoolRow(Map<Integer, String> row, Map<String, Integer> headerIndex) {
        ScreeningSchool data = new ScreeningSchool();
        data.setYear(normalizeYearValue(field(row, headerIndex, "年份", "年度")));
        data.setCity(field(row, headerIndex, "市州"));
        data.setDistrict(field(row, headerIndex, "县市区", "区县"));
        data.setName(field(row, headerIndex, "姓名"));
        data.setGender(field(row, headerIndex, "性别"));
        data.setBirthDate(parseDate(field(row, headerIndex, "出生日期")));
        data.setAge(parseInteger(field(row, headerIndex, "年龄")));
        data.setIdType(field(row, headerIndex, "证件类型"));
        data.setIdNumber(normalizeExcelCellText(field(row, headerIndex, "证件号", "身份证号", "身份证号码")));
        data.setEthnicity(field(row, headerIndex, "民族"));
        data.setPhone(normalizeExcelCellText(field(row, headerIndex, "联系电话")));
        data.setHouseholdAddress(field(row, headerIndex, "户籍所在地XX市XX县区", "户籍所在地"));
        data.setCurrentAddress(field(row, headerIndex, "现住址", "现地址"));
        data.setSchoolType(field(row, headerIndex, "学校类型"));
        data.setSchoolName(field(row, headerIndex, "学校名称"));
        data.setClassName(field(row, headerIndex, "班级院系"));
        data.setTbHistory(field(row, headerIndex, "既往结核病史"));
        data.setCloseContactHistory(field(row, headerIndex, "密切接触史"));
        data.setSuspiciousSymptoms(field(row, headerIndex, "结核病可疑症状"));
        data.setHasInfectionScreen(field(row, headerIndex, "是否进行感染筛"));
        data.setScreenDate(parseDate(field(row, headerIndex, "感染筛查日期")));
        data.setScreenMethod(field(row, headerIndex, "方法", "感染筛查方法"));
        data.setScreenResult(field(row, headerIndex, "结果PPDmmXmmEC及IGRA阳性阴性"));
        data.setInfectionResult(field(row, headerIndex, "感染筛查结果", "判定结果"));
        data.setHasChestXray(field(row, headerIndex, "是否进行胸片检查"));
        data.setChestXrayDate(parseDate(field(row, headerIndex, "胸片检查日期")));
        data.setChestXrayResult(field(row, headerIndex, "胸片结果", "胸部DR", "胸片检查结果"));
        data.setSputumSmearResult(field(row, headerIndex, "痰涂片结果", "痰涂片"));
        data.setMolecularBiologyResult(field(row, headerIndex, "分子生物学结果", "分子生物学"));
        data.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(
                field(row, headerIndex, "诊断结果", "诊断")));
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
        return data == null || (StrUtil.isBlank(data.getName()) && StrUtil.isBlank(data.getIdNumber()));
    }

    private LocalDate parseDate(String value) {
        if (StrUtil.isBlank(value)) return null;
        String text = value.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
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
    public void createFromQuestionnaire(ScreeningSchool data) {
        if (isIdCardType(data.getIdType()) && StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "手机号格式不正确");
        }

        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        data.setDepartmentId(null);
        save(data);

        if (data.getIsLatent() == 1) {
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
                    .departmentId(null)
                    .creatorId(BaseContext.getCurrentId())
                    .build();
            latentInfectionService.save(latent);
            latentInfectionService.autoReferralForDirectDiagnosis(List.of(latent));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScreening(ScreeningSchool data) {
        ScreeningSchool existing = getById(data.getId());
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "筛查记录不存在");
        }
        // 根据感染筛查结果与诊断结果重新计算潜伏判定
        if (StrUtil.isNotBlank(data.getDiagnosisFirst())) {
            data.setDiagnosisFirst(ScreeningDiagnosisSupport.normalizeDiagnosis(data.getDiagnosisFirst()));
        }
        data.setIsLatent(shouldMarkLatent(data) ? 1 : 0);
        if (data.getDepartmentId() == null) {
            data.setDepartmentId(existing.getDepartmentId());
        }
        updateById(data);
        ScreeningSchool updated = getById(data.getId());
        syncLatentFromScreening(List.of(updated), "school");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScreeningCascade(Long id) {
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
}
