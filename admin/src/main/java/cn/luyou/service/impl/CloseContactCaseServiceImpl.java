package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.CloseContactCaseMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ImportResult;
import cn.luyou.service.CloseContactCaseService;
import cn.luyou.service.DepartmentService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.CloseContactCaseExcelDerivedSupport;
import cn.luyou.utils.CloseContactCaseExcelSupport;
import cn.luyou.utils.ColumnDistinctSupport;
import cn.luyou.utils.ColumnFilterSupport;
import cn.luyou.utils.CreatorUserSupport;
import cn.luyou.utils.IdentityFormatFilterSupport;
import cn.luyou.utils.ImportDuplicateIdSupport;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.ScreeningImportMergeSupport;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloseContactCaseServiceImpl extends ServiceImpl<CloseContactCaseMapper, CloseContactCase>
        implements CloseContactCaseService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final DepartmentService departmentService;
    private final UserMapper userMapper;
    private final ScreeningScopeHelper screeningScopeHelper;

    private static final Set<String> COLUMN_FILTER_WHITELIST = Set.of(
            "name", "year", "city", "district", "gender", "idNumber", "phone",
            "sourcePatientName", "sourcePatientBacteriologyResult", "finalScreeningResult",
            "infectionCheckResult", "imagingResult", "sputumCheckResult", "hasPreventiveTreatment",
            "remark", "creatorUsername"
    );
    /** 表头 Excel 式下拉：仅枚举/导入内容类字段 */
    private static final Set<String> COLUMN_DISTINCT_FIELDS = Set.of(
            "district", "city", "year", "gender", "sourcePatientBacteriologyResult",
            "finalScreeningResult", "infectionCheckResult", "imagingResult",
            "sputumCheckResult", "hasPreventiveTreatment"
    );

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
        String batchId = IdUtil.fastSimpleUUID();
        final String creatorUsername = CreatorUserSupport.resolveCurrentUsername(userMapper);
        final List<CloseContactCase> parsedList = new ArrayList<>();
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

        try {
            EasyExcel.read(new ByteArrayInputStream(fileBytes), CloseContactCase.class, new ReadListener<CloseContactCase>() {
                @Override
                public void invoke(CloseContactCase data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    if (isBlankDataRow(data)) {
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
                    if (data.getRegistrationDate() != null) {
                        data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
                    }
                    data.setUploadBatch(batchId);
                    data.setImportRowNo(row);
                    data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
                    data.setCreatorUsername(creatorUsername);
                    parsedList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("密接个案表数据解析完成，共 {} 条", parsedList.size());
                }
            }).sheet().headRowNumber(headRowNumber).doRead();
        } catch (Exception e) {
            log.error("密接个案表 Excel 解析失败", e);
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "Excel解析失败，请使用系统导出的模板或标准密接表（含表头）: " + e.getMessage());
        }

        if (ImportIdentitySupport.shouldBlockImport(result, confirmSkipInvalid)) {
            return result;
        }

        List<CloseContactCase> dataList = ImportDuplicateIdSupport.handleDuplicateInFile(
                result,
                parsedList,
                d -> ImportDuplicateIdSupport.normalizeIdNumber(d.getIdNumber()),
                CloseContactCase::getImportRowNo,
                CloseContactCase::getIdNumber,
                CloseContactCase::getName,
                confirmSkipDuplicateInFile);
        if (dataList == null) {
            return result;
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        List<CloseContactCase> toInsert = new ArrayList<>();
        List<CloseContactCase> toUpdate = new ArrayList<>();

        for (CloseContactCase d : dataList) {
            if (StrUtil.isBlank(d.getIdNumber())) {
                toInsert.add(d);
                continue;
            }
            CloseContactCase existing = null;
            if (StrUtil.isNotBlank(d.getIdNumber())) {
                LambdaQueryWrapper<CloseContactCase> dupWrapper = new LambdaQueryWrapper<>();
                dupWrapper.eq(CloseContactCase::getIdNumber, d.getIdNumber()).last("LIMIT 1");
                screeningScopeHelper.applyImportDedupScope(
                        dupWrapper, CloseContactCase::getDepartmentId, CloseContactCase::getCreatorUsername, null);
                existing = getOne(dupWrapper, false);
            }
            if (existing != null) {
                mergeCaseData(existing, d);
                toUpdate.add(existing);
            } else {
                toInsert.add(d);
            }
        }

        if (!toInsert.isEmpty()) saveBatch(toInsert, 500);
        if (!toUpdate.isEmpty()) updateBatchById(toUpdate, 500);

        result.setSuccessCount(dataList.size());
        return result;
    }

    private void mergeCaseData(CloseContactCase existing, CloseContactCase incoming) {
        // 覆盖导入：Excel 空单元格直接清空已有字段
        ScreeningImportMergeSupport.mergeCloseContactCase(existing, incoming);
        // 覆盖导入只更新业务字段与行号，保留首次录入人；历史空值则补当前导入人
        if (StrUtil.isBlank(existing.getCreatorUsername()) && StrUtil.isNotBlank(incoming.getCreatorUsername())) {
            existing.setCreatorUsername(incoming.getCreatorUsername());
        }
        if (existing.getDepartmentId() == null && incoming.getDepartmentId() != null) {
            existing.setDepartmentId(incoming.getDepartmentId());
        }
    }

    @Override
    public IPage<CloseContactCase> queryPage(int page, int size, String name, String idNumber,
                                              String district, String phone, String creatorUsername,
                                              String diagnosisResult, String sourcePatientBacteriologyResult,
                                              String reportQuarter,
                                              String createTimeFrom, String createTimeTo,
                                              String columnFilters, String formatIssue) {
        LambdaQueryWrapper<CloseContactCase> wrapper = buildQueryWrapper(
                name, idNumber, district, phone, creatorUsername, diagnosisResult,
                sourcePatientBacteriologyResult, reportQuarter,
                createTimeFrom, createTimeTo, formatIssue);
        applyColumnFilters(wrapper, columnFilters);
        ImportRowOrderSupport.applyWithBatch(wrapper);
        applyDepartmentFilter(wrapper);
        IPage<CloseContactCase> result = page(new Page<>(page, size), wrapper);
        CloseContactCaseExcelDerivedSupport.applyAll(result.getRecords());
        return result;
    }

    private void applyColumnFilters(LambdaQueryWrapper<CloseContactCase> wrapper, String columnFilters) {
        Map<String, String> filters = ColumnFilterSupport.parse(columnFilters);
        ColumnFilterSupport.applyLambda(filters, COLUMN_FILTER_WHITELIST, (field, value) -> {
            switch (field) {
                case "name" -> ColumnFilterSupport.like(wrapper, CloseContactCase::getName, value);
                case "year" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getYear, value);
                case "city" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getCity, value);
                case "district" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getDistrict, value);
                case "gender" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getGender, value);
                case "idNumber" -> ColumnFilterSupport.like(wrapper, CloseContactCase::getIdNumber, value);
                case "phone" -> ColumnFilterSupport.like(wrapper, CloseContactCase::getPhone, value);
                case "sourcePatientName" -> ColumnFilterSupport.like(wrapper, CloseContactCase::getSourcePatientName, value);
                case "sourcePatientBacteriologyResult" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getSourcePatientBacteriologyResult, value);
                case "finalScreeningResult" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getFinalScreeningResult, value);
                case "infectionCheckResult" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getInfectionCheckResult, value);
                case "imagingResult" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getImagingResult, value);
                case "sputumCheckResult" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getSputumCheckResult, value);
                case "hasPreventiveTreatment" -> ColumnFilterSupport.eqOrIn(wrapper, CloseContactCase::getHasPreventiveTreatment, value);
                case "remark" -> ColumnFilterSupport.like(wrapper, CloseContactCase::getRemark, value);
                case "creatorUsername" -> ColumnFilterSupport.like(wrapper, CloseContactCase::getCreatorUsername, value);
                default -> { }
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCase(CloseContactCase data) {
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        }
        data.setDepartmentId(screeningScopeHelper.resolveUploadDepartmentId());
        data.setCreatorUsername(CreatorUserSupport.resolveCurrentUsername(userMapper));
        save(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCase(CloseContactCase data) {
        CloseContactCase existing = requireAccessibleCase(data.getId());
        if (data.getRegistrationDate() != null) {
            data.setYear(String.valueOf(data.getRegistrationDate().getYear()));
        }
        // 录入用户与部门不可被前端覆盖
        data.setCreatorUsername(existing.getCreatorUsername());
        data.setDepartmentId(existing.getDepartmentId());
        updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long id) {
        requireAccessibleCase(id);
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            requireAccessibleCase(id);
        }
        removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByFilter(String name, String idNumber, String district, String phone,
                               String creatorUsername, String diagnosisResult,
                               String sourcePatientBacteriologyResult, String reportQuarter,
                               String createTimeFrom, String createTimeTo, String columnFilters,
                               String formatIssue) {
        LambdaQueryWrapper<CloseContactCase> wrapper = buildQueryWrapper(
                name, idNumber, district, phone, creatorUsername, diagnosisResult,
                sourcePatientBacteriologyResult, reportQuarter,
                createTimeFrom, createTimeTo, formatIssue);
        applyColumnFilters(wrapper, columnFilters);
        applyDepartmentFilter(wrapper);
        wrapper.select(CloseContactCase::getId);
        List<Long> ids = list(wrapper).stream().map(CloseContactCase::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        batchDelete(ids);
        return ids.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAll() {
        return deleteByFilter(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public List<CloseContactCase> listForExport(String name, String idNumber, String district,
                                                 String phone, String creatorUsername, String diagnosisResult,
                                                 String sourcePatientBacteriologyResult, String reportQuarter,
                                                 List<Long> ids,
                                                 String createTimeFrom, String createTimeTo,
                                                 String columnFilters, String formatIssue) {
        LambdaQueryWrapper<CloseContactCase> wrapper;
        if (ids != null && !ids.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.in(CloseContactCase::getId, ids);
        } else {
            wrapper = buildQueryWrapper(name, idNumber, district, phone, creatorUsername, diagnosisResult,
                    sourcePatientBacteriologyResult, reportQuarter, createTimeFrom, createTimeTo, formatIssue);
            applyColumnFilters(wrapper, columnFilters);
        }
        ImportRowOrderSupport.applyWithBatch(wrapper);
        applyDepartmentFilter(wrapper);
        List<CloseContactCase> list = list(wrapper);
        CloseContactCaseExcelDerivedSupport.applyAll(list);
        return list;
    }

    private LambdaQueryWrapper<CloseContactCase> buildQueryWrapper(String name, String idNumber,
                                                                    String district, String phone,
                                                                    String creatorUsername, String diagnosisResult,
                                                                    String sourcePatientBacteriologyResult,
                                                                    String reportQuarter,
                                                                    String createTimeFrom, String createTimeTo,
                                                                    String formatIssue) {
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(createTimeFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(createTimeTo);
        LambdaQueryWrapper<CloseContactCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), CloseContactCase::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), CloseContactCase::getIdNumber, idNumber)
                .eq(StrUtil.isNotBlank(district), CloseContactCase::getDistrict, district)
                .like(StrUtil.isNotBlank(phone), CloseContactCase::getPhone, phone)
                .like(StrUtil.isNotBlank(creatorUsername), CloseContactCase::getCreatorUsername, creatorUsername)
                .eq(StrUtil.isNotBlank(diagnosisResult), CloseContactCase::getFinalScreeningResult, diagnosisResult)
                .eq(StrUtil.isNotBlank(sourcePatientBacteriologyResult),
                        CloseContactCase::getSourcePatientBacteriologyResult, sourcePatientBacteriologyResult)
                .ge(createFrom != null, CloseContactCase::getCreateTime, createFrom)
                .le(createTo != null, CloseContactCase::getCreateTime, createTo);
        applyReportQuarterFilter(wrapper, reportQuarter);
        IdentityFormatFilterSupport.apply(wrapper, formatIssue, "id_number", "phone");
        return wrapper;
    }

    /** 报表填报季度按密切接触者登记日期所在季度筛选（衍生列不落库）。 */
    private void applyReportQuarterFilter(LambdaQueryWrapper<CloseContactCase> wrapper, String reportQuarter) {
        if (StrUtil.isBlank(reportQuarter)) {
            return;
        }
        if (CloseContactCaseExcelDerivedSupport.isEmptyRegistrationQuarter(reportQuarter)) {
            wrapper.isNull(CloseContactCase::getRegistrationDate);
            return;
        }
        java.time.LocalDate[] range = CloseContactCaseExcelDerivedSupport.resolveReportQuarterDateRange(reportQuarter);
        if (range == null) {
            wrapper.eq(CloseContactCase::getId, -1L);
            return;
        }
        wrapper.ge(CloseContactCase::getRegistrationDate, range[0])
                .le(CloseContactCase::getRegistrationDate, range[1]);
    }

    private void applyDepartmentFilter(LambdaQueryWrapper<CloseContactCase> wrapper) {
        screeningScopeHelper.applyCloseContactCaseScope(
                wrapper, CloseContactCase::getDepartmentId, CloseContactCase::getCreatorUsername);
    }

    @Override
    public CloseContactCase getAccessibleById(Long id) {
        CloseContactCase existing = requireAccessibleCase(id);
        CloseContactCaseExcelDerivedSupport.apply(existing);
        return existing;
    }

    private CloseContactCase requireAccessibleCase(Long id) {
        CloseContactCase existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "个案记录不存在");
        }
        if (!BaseContext.isSuperAdmin()) {
            Long currentDeptId = BaseContext.getCurrentDepartmentId();
            List<Long> deptIds = departmentService.getDescendantIds(currentDeptId);
            if (deptIds.isEmpty()) {
                String username = CreatorUserSupport.resolveCurrentUsername(userMapper);
                if (StrUtil.isBlank(username)
                        || !username.equals(existing.getCreatorUsername())) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "无权操作该个案记录");
                }
                return existing;
            }
            if (existing.getDepartmentId() == null || !deptIds.contains(existing.getDepartmentId())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "无权操作该个案记录");
            }
        }
        return existing;
    }

    private boolean isValidIdCard(String id) {
        // 与密接筛查导入一致：仅校验 18 位格式，避免 Excel 数值型身份证号导致校验码误判
        String trimmed = id == null ? "" : id.trim();
        return trimmed.length() == 18 && trimmed.matches("\\d{17}[\\dXx]");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private boolean isBlankDataRow(CloseContactCase data) {
        return data == null
                || (StrUtil.isBlank(data.getName())
                && StrUtil.isBlank(data.getIdNumber())
                && StrUtil.isBlank(data.getCity())
                && StrUtil.isBlank(data.getDistrict()));
    }

    @Override
    public List<String> listDistinctColumnValues(String field) {
        if (StrUtil.isBlank(field) || !COLUMN_DISTINCT_FIELDS.contains(field)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
        LambdaQueryWrapper<CloseContactCase> wrapper = new LambdaQueryWrapper<>();
        applyDepartmentFilter(wrapper);
        applyDistinctSelect(wrapper, field);
        return ColumnDistinctSupport.normalize(list(wrapper).stream()
                .map(row -> extractDistinctValue(row, field))
                .toList());
    }

    private void applyDistinctSelect(LambdaQueryWrapper<CloseContactCase> wrapper, String field) {
        switch (field) {
            case "district" -> wrapper.select(CloseContactCase::getDistrict)
                    .isNotNull(CloseContactCase::getDistrict).ne(CloseContactCase::getDistrict, "")
                    .groupBy(CloseContactCase::getDistrict);
            case "city" -> wrapper.select(CloseContactCase::getCity)
                    .isNotNull(CloseContactCase::getCity).ne(CloseContactCase::getCity, "")
                    .groupBy(CloseContactCase::getCity);
            case "year" -> wrapper.select(CloseContactCase::getYear)
                    .isNotNull(CloseContactCase::getYear).ne(CloseContactCase::getYear, "")
                    .groupBy(CloseContactCase::getYear);
            case "gender" -> wrapper.select(CloseContactCase::getGender)
                    .isNotNull(CloseContactCase::getGender).ne(CloseContactCase::getGender, "")
                    .groupBy(CloseContactCase::getGender);
            case "sourcePatientBacteriologyResult" -> wrapper.select(CloseContactCase::getSourcePatientBacteriologyResult)
                    .isNotNull(CloseContactCase::getSourcePatientBacteriologyResult)
                    .ne(CloseContactCase::getSourcePatientBacteriologyResult, "")
                    .groupBy(CloseContactCase::getSourcePatientBacteriologyResult);
            case "finalScreeningResult" -> wrapper.select(CloseContactCase::getFinalScreeningResult)
                    .isNotNull(CloseContactCase::getFinalScreeningResult).ne(CloseContactCase::getFinalScreeningResult, "")
                    .groupBy(CloseContactCase::getFinalScreeningResult);
            case "infectionCheckResult" -> wrapper.select(CloseContactCase::getInfectionCheckResult)
                    .isNotNull(CloseContactCase::getInfectionCheckResult).ne(CloseContactCase::getInfectionCheckResult, "")
                    .groupBy(CloseContactCase::getInfectionCheckResult);
            case "imagingResult" -> wrapper.select(CloseContactCase::getImagingResult)
                    .isNotNull(CloseContactCase::getImagingResult).ne(CloseContactCase::getImagingResult, "")
                    .groupBy(CloseContactCase::getImagingResult);
            case "sputumCheckResult" -> wrapper.select(CloseContactCase::getSputumCheckResult)
                    .isNotNull(CloseContactCase::getSputumCheckResult).ne(CloseContactCase::getSputumCheckResult, "")
                    .groupBy(CloseContactCase::getSputumCheckResult);
            case "hasPreventiveTreatment" -> wrapper.select(CloseContactCase::getHasPreventiveTreatment)
                    .isNotNull(CloseContactCase::getHasPreventiveTreatment).ne(CloseContactCase::getHasPreventiveTreatment, "")
                    .groupBy(CloseContactCase::getHasPreventiveTreatment);
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID, "不支持的筛选字段: " + field);
        }
    }

    private String extractDistinctValue(CloseContactCase row, String field) {
        return switch (field) {
            case "district" -> row.getDistrict();
            case "city" -> row.getCity();
            case "year" -> row.getYear();
            case "gender" -> row.getGender();
            case "sourcePatientBacteriologyResult" -> row.getSourcePatientBacteriologyResult();
            case "finalScreeningResult" -> row.getFinalScreeningResult();
            case "infectionCheckResult" -> row.getInfectionCheckResult();
            case "imagingResult" -> row.getImagingResult();
            case "sputumCheckResult" -> row.getSputumCheckResult();
            case "hasPreventiveTreatment" -> row.getHasPreventiveTreatment();
            default -> null;
        };
    }
}
