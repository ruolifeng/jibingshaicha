package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.constant.EpidemicTrackImportHeaders;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.ColumnFilterSupport;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.FlexibleDateParseUtil;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.StatYearPeriod;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.ReferralTrackingMapper;
import cn.luyou.model.Department;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.ReferralTracking;
import cn.luyou.model.User;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.ReferralTrackingService;
import cn.luyou.service.SysMessageService;
import cn.luyou.service.UserService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralTrackingServiceImpl extends ServiceImpl<ReferralTrackingMapper, ReferralTracking>
        implements ReferralTrackingService {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final LatentInfectionMapper latentInfectionMapper;
    private final SysMessageService sysMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReferralTracking create(Map<String, Object> params) {
        String bizMode = getStr(params, "bizMode");
        if (StrUtil.isBlank(bizMode)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "bizMode 不能为空（recommend/track）");
        }
        Long currentUserId = BaseContext.getCurrentId();
        User currentUser = userService.getById(currentUserId);
        params.put("idNumber", ImportIdentitySupport.normalizeIdNumber(getStr(params, "idNumber")));

        ReferralTracking record = ReferralTracking.builder()
                .bizMode(bizMode)
                .sourceType("manual")
                .name(getStr(params, "name"))
                .gender(getStr(params, "gender"))
                .birthDate(parseDate(params.get("birthDate")))
                .age(getInt(params, "age"))
                .idType(getStr(params, "idType"))
                .idNumber(getStr(params, "idNumber"))
                .ethnicity(getStr(params, "ethnicity"))
                .phone(getStr(params, "phone"))
                .householdAddress(getStr(params, "householdAddress"))
                .currentAddress(getStr(params, "currentAddress"))
                .crowdCategory(getStr(params, "crowdCategory"))
                .screenDate(parseDate(params.get("screenDate")))
                .screenMethod(getStr(params, "screenMethod"))
                .infectionResult(getStr(params, "infectionResult"))
                .chestXrayDate(parseDate(params.get("chestXrayDate")))
                .chestXrayResult(getStr(params, "chestXrayResult"))
                .trackingStatus(0)
                .notInPlaceCount(0)
                .archived(0)
                .creatorId(currentUserId)
                .departmentId(currentUser != null ? currentUser.getDepartmentId() : null)
                .build();

        if ("recommend".equals(bizMode)) {
            Integer role = currentUser != null ? currentUser.getRole() : null;
            if (role == null || (role != 1 && (role < 2 || role > 6))) {
                throw new ServiceException(StatusEnum.FORBIDDEN, "仅超级管理员或一至五级用户可发起推介");
            }
            validateRecommendRequired(params);
            Long receiverUserId = getLong(params, "receiverUserId");
            User receiver = userService.getById(receiverUserId);
            if (receiver == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "接收人不存在");
            }
            if (receiver.getRole() == null || receiver.getRole() < 2 || receiver.getRole() > 6) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "推介接收人须为一至五级用户");
            }
            record.setReceiverUserId(receiverUserId);
            record.setReceiverDeptId(receiver.getDepartmentId());
            record.setRecommendStatus(0);
            record.setRecommendReason(getStr(params, "recommendReason"));
            record.setRecommendUnitName(resolveRecommendUnitName(currentUser));
            record.setFillUserName(resolveFillUserName(currentUser));
        } else if ("track".equals(bizMode)) {
            validateTrackRequired(params);
            record.setTrackReason(getStr(params, "trackReason"));
        }

        assertDuplicateAllowed(bizMode, record.getIdNumber(), record.getName(), params);

        save(record);
        if ("recommend".equals(bizMode)) {
            sendRecommend(record.getId());
            record = getById(record.getId());
        }
        return record;
    }

    @Override
    public ReferralTracking getDetail(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        assertCanAccessRecord(record);
        fillDisplayNames(record);
        return record;
    }

    private static final Set<String> COLUMN_FILTER_WHITELIST = Set.of(
            "name", "gender", "idNumber", "phone", "currentAddress", "township",
            "crowdCategory", "caseCategory", "diseaseName", "reportUnit",
            "diagnosisResult", "sourceType", "cardId", "workplace", "epidemicRemark",
            "creatorUserName", "creatorUsername"
    );

    private static final DateTimeFormatter EXPORT_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public IPage<ReferralTracking> queryPage(int page, int size, String bizMode,
                                              String name, String idNumber,
                                              Integer trackingStatus, Integer archived,
                                              String phone, String township,
                                              String dateFrom, String dateTo, String sourceType,
                                              String creatorOrEntryUnit, String columnFilters,
                                              String createTimeFrom, String createTimeTo) {
        Integer role = BaseContext.getCurrentRole();
        boolean level5RecommendView = "recommend".equals(bizMode) && Integer.valueOf(6).equals(role);

        LambdaQueryWrapper<ReferralTracking> wrapper = buildQueryWrapper(
                bizMode, name, idNumber, trackingStatus, archived,
                phone, township, dateFrom, dateTo, sourceType);
        applyCreateTimeFilter(wrapper, createTimeFrom, createTimeTo);
        applyCreatorOrEntryUnitFilter(wrapper, creatorOrEntryUnit);
        applyColumnFilters(wrapper, columnFilters);
        applyUserScopeFilter(wrapper, bizMode, level5RecommendView);

        IPage<ReferralTracking> pageResult = page(new Page<>(page, size), wrapper);

        pageResult.getRecords().forEach(this::fillDisplayNames);

        return pageResult;
    }

    private void applyColumnFilters(LambdaQueryWrapper<ReferralTracking> wrapper, String columnFilters) {
        Map<String, String> filters = ColumnFilterSupport.parse(columnFilters);
        ColumnFilterSupport.applyLambda(filters, COLUMN_FILTER_WHITELIST, (field, value) -> {
            switch (field) {
                case "name" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getName, value);
                case "gender" -> ColumnFilterSupport.eqOrIn(wrapper, ReferralTracking::getGender, value);
                case "idNumber" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getIdNumber, value);
                case "phone" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getPhone, value);
                case "currentAddress" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getCurrentAddress, value);
                case "township" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getTownship, value);
                case "crowdCategory" -> ColumnFilterSupport.eqOrIn(wrapper, ReferralTracking::getCrowdCategory, value);
                case "caseCategory" -> ColumnFilterSupport.eqOrIn(wrapper, ReferralTracking::getCaseCategory, value);
                case "diseaseName" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getDiseaseName, value);
                case "reportUnit" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getReportUnit, value);
                case "diagnosisResult" -> ColumnFilterSupport.eqOrIn(wrapper, ReferralTracking::getDiagnosisResult, value);
                case "sourceType" -> ColumnFilterSupport.eqOrIn(wrapper, ReferralTracking::getSourceType, value);
                case "cardId" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getCardId, value);
                case "workplace" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getWorkplace, value);
                case "epidemicRemark" -> ColumnFilterSupport.like(wrapper, ReferralTracking::getEpidemicRemark, value);
                case "creatorUserName", "creatorUsername" -> applyCreatorOrEntryUnitFilter(wrapper, value);
                default -> { }
            }
        });
    }

    @Override
    public boolean existsByIdNumberAndName(String bizMode, String idNumber, String name) {
        String normalizedId = ImportIdentitySupport.normalizeIdNumber(idNumber);
        if (StrUtil.isBlank(bizMode) || StrUtil.isBlank(normalizedId) || StrUtil.isBlank(name)) {
            return false;
        }
        return lambdaQuery()
                .eq(ReferralTracking::getBizMode, bizMode)
                .eq(ReferralTracking::getIdNumber, normalizedId)
                .eq(ReferralTracking::getName, name.trim())
                .exists();
    }

    private void assertDuplicateAllowed(String bizMode, String idNumber, String name, Map<String, Object> params) {
        if (Boolean.TRUE.equals(params.get("confirmDuplicate"))) {
            return;
        }
        if (existsByIdNumberAndName(bizMode, idNumber, name)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "患者信息已经导入过，请确认是否增加新记录");
        }
    }

    @Override
    public Map<String, Object> previewEpidemicImport(MultipartFile file) {
        List<EpidemicImportRow> rows = parseEpidemicImportRows(file);
        List<Map<String, String>> duplicates = new ArrayList<>();
        int newCount = 0;
        int updateCount = 0;
        int skipped = 0;
        Long currentDeptId = BaseContext.getCurrentDepartmentId();
        Integer role = BaseContext.getCurrentRole();

        for (EpidemicImportRow row : rows) {
            Long targetDeptId = resolveTrackDepartmentId(row.township(), currentDeptId);
            if (Integer.valueOf(6).equals(role) && currentDeptId != null
                    && targetDeptId != null && !currentDeptId.equals(targetDeptId)) {
                skipped++;
                continue;
            }
            ReferralTracking existingByCard = findEpidemicRecordByCardId(row.cardId(), targetDeptId);
            if (existingByCard != null) {
                updateCount++;
                continue;
            }
            if (existsByIdNumberAndName("track", row.idNumber(), row.name())) {
                duplicates.add(Map.of(
                        "name", row.name(),
                        "idNumber", row.idNumber()
                ));
            } else {
                newCount++;
            }
        }

        return Map.of(
                "duplicateCount", duplicates.size(),
                "newCount", newCount,
                "updateCount", updateCount,
                "skipped", skipped,
                "duplicates", duplicates
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importEpidemic(MultipartFile file, boolean addDuplicateRecords) {
        String batchNo = IdUtil.fastSimpleUUID();
        List<EpidemicImportRow> rows = parseEpidemicImportRows(file);
        if (rows.isEmpty()) {
            return Map.of("count", 0, "updated", 0, "skipped", 0, "batchNo", batchNo);
        }

        Long currentUserId = BaseContext.getCurrentId();
        Long currentDeptId = BaseContext.getCurrentDepartmentId();
        Integer role = BaseContext.getCurrentRole();
        int count = 0;
        int updated = 0;
        int skipped = 0;

        for (EpidemicImportRow row : rows) {
            // 按乡镇字段归属部门，避免全县 Excel 全部挂到导入人部门导致镇级数据串扰
            Long targetDeptId = resolveTrackDepartmentId(row.township(), currentDeptId);
            if (Integer.valueOf(6).equals(role) && currentDeptId != null
                    && targetDeptId != null && !currentDeptId.equals(targetDeptId)) {
                // 五级仅可写入本镇数据，跳过其它乡镇行
                skipped++;
                continue;
            }

            ReferralTracking existingByCard = findEpidemicRecordByCardId(row.cardId(), targetDeptId);
            if (existingByCard != null) {
                if (mergeEpidemicImportFields(existingByCard, row.reportCardTime(), row.currentAddress(),
                        row.township(), currentUserId, targetDeptId, row.importRowNo())) {
                    updateById(existingByCard);
                    updated++;
                }
                continue;
            }

            if (existsByIdNumberAndName("track", row.idNumber(), row.name())) {
                if (!addDuplicateRecords) {
                    skipped++;
                    continue;
                }
            }

            ReferralTracking entity = buildEpidemicEntity(row, batchNo, currentUserId, targetDeptId);
            save(entity);
            count++;
        }

        log.info("大疫情导入追踪记录完成，created={}, updated={}, skipped={}, addDuplicateRecords={}, batchNo={}",
                count, updated, skipped, addDuplicateRecords, batchNo);
        return Map.of("count", count, "updated", updated, "skipped", skipped, "batchNo", batchNo);
    }

    private List<EpidemicImportRow> parseEpidemicImportRows(MultipartFile file) {
        List<Map<Integer, Object>> allRows = readExcelRows(file);
        if (allRows.size() < 2) {
            return List.of();
        }

        int headerRowIndex = resolveHeaderRowIndex(allRows);
        Map<String, Integer> headerIndex = buildHeaderIndex(allRows.get(headerRowIndex));
        log.info("大疫情表表头解析（第{}行）：{}", headerRowIndex + 1, headerIndex.keySet());

        List<EpidemicImportRow> rows = new ArrayList<>();
        List<Map<Integer, Object>> dataRows = allRows.subList(headerRowIndex + 1, allRows.size());
        for (int ri = 0; ri < dataRows.size(); ri++) {
            Map<Integer, Object> row = dataRows.get(ri);
            int importRowNo = headerRowIndex + 2 + ri;
            String cardId = getFieldByHeader(row, headerIndex, "卡片ID");
            String name = getFieldByHeader(row, headerIndex, "患者姓名", "姓名");
            String idNumber = ImportIdentitySupport.normalizeIdNumber(
                    getFieldByHeader(row, headerIndex, "有效证件号", "证件号", "身份证号", "身份证"));
            if (StrUtil.isBlank(name) && StrUtil.isBlank(idNumber) && StrUtil.isBlank(cardId)) {
                continue;
            }

            String currentAddress = getFieldByHeader(row, headerIndex,
                    "现住详细地址", "现详细住址", "现住地址区现住详细", "现住址", "现住地址");
            String township = extractTownship(currentAddress);
            if (StrUtil.isBlank(township)) {
                township = getFieldByHeader(row, headerIndex, "乡镇");
            }

            Object reportCardTimeCell = getReportCardTimeCell(row, headerIndex);
            LocalDateTime reportCardTime = parseDateTimeCell(reportCardTimeCell);

            rows.add(new EpidemicImportRow(
                    cardId,
                    name,
                    idNumber,
                    getFieldByHeader(row, headerIndex, "患儿家长姓名"),
                    getFieldByHeader(row, headerIndex, "性别"),
                    parseDateCell(getFieldCellValue(row, headerIndex, "出生日期")),
                    parseInt(getFieldByHeader(row, headerIndex, "年龄")),
                    getFieldByHeader(row, headerIndex, "患者工作单位"),
                    getFieldByHeader(row, headerIndex, "联系电话", "电话"),
                    currentAddress,
                    township,
                    getFieldByHeader(row, headerIndex, "人群分类"),
                    getFieldByHeader(row, headerIndex, "病例分类"),
                    getFieldByHeader(row, headerIndex, "疾病名称"),
                    getFieldByHeader(row, headerIndex, "报告单位"),
                    reportCardTime,
                    getFieldByHeader(row, headerIndex, "备注"),
                    importRowNo
            ));
        }
        return rows;
    }

    private ReferralTracking buildEpidemicEntity(EpidemicImportRow row, String batchNo,
                                                 Long currentUserId, Long currentDeptId) {
        return ReferralTracking.builder()
                .bizMode("track")
                .sourceType("epidemic")
                .cardId(row.cardId())
                .name(row.name())
                .parentName(row.parentName())
                .idNumber(row.idNumber())
                .gender(row.gender())
                .birthDate(row.birthDate())
                .age(row.age())
                .workplace(row.workplace())
                .phone(row.phone())
                .currentAddress(row.currentAddress())
                .township(row.township())
                .crowdCategory(row.crowdCategory())
                .caseCategory(row.caseCategory())
                .diseaseName(row.diseaseName())
                .reportUnit(row.reportUnit())
                .reportCardTime(row.reportCardTime())
                .epidemicRemark(row.epidemicRemark())
                .trackReason("大疫情导入")
                .trackingStatus(0)
                .notInPlaceCount(0)
                .archived(0)
                .uploadBatch(batchNo)
                .importRowNo(row.importRowNo())
                .departmentId(currentDeptId)
                .creatorId(currentUserId)
                .build();
    }

    private record EpidemicImportRow(
            String cardId,
            String name,
            String idNumber,
            String parentName,
            String gender,
            LocalDate birthDate,
            Integer age,
            String workplace,
            String phone,
            String currentAddress,
            String township,
            String crowdCategory,
            String caseCategory,
            String diseaseName,
            String reportUnit,
            LocalDateTime reportCardTime,
            String epidemicRemark,
            Integer importRowNo
    ) {
    }

    @Override
    public void exportTrack(HttpServletResponse response, String bizMode,
                            String name, String idNumber, String phone, String township,
                            String dateFrom, String dateTo, String sourceType,
                            String creatorOrEntryUnit, List<Long> ids,
                            String createTimeFrom, String createTimeTo) {
        Integer role = BaseContext.getCurrentRole();
        boolean level5RecommendView = "recommend".equals(bizMode) && Integer.valueOf(6).equals(role);
        LambdaQueryWrapper<ReferralTracking> wrapper;
        if (ids != null && !ids.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ReferralTracking::getId, ids);
            applyBizModeFilter(wrapper, bizMode);
            applyUserScopeFilter(wrapper, bizMode, level5RecommendView);
        } else {
            wrapper = buildQueryWrapper(
                    bizMode, name, idNumber, null, null, phone, township, dateFrom, dateTo, sourceType);
            applyCreateTimeFilter(wrapper, createTimeFrom, createTimeTo);
            applyCreatorOrEntryUnitFilter(wrapper, creatorOrEntryUnit);
            applyUserScopeFilter(wrapper, bizMode, level5RecommendView);
        }
        List<ReferralTracking> records = list(wrapper);

        boolean recommendExport = "recommend".equals(bizMode);
        List<List<String>> head = buildExportHead(recommendExport);
        List<List<Object>> rows = new ArrayList<>();
        for (ReferralTracking r : records) {
            fillDisplayNames(r);
            rows.add(buildExportRow(r, recommendExport));
        }

        String sheetTitle = recommendExport ? "推介记录" : "追踪记录";
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode(sheetTitle + "导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream()).head(head).sheet(sheetTitle).doWrite(rows);
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "导出失败");
        }
    }

    /**
     * 按卡片ID查找大疫情追踪记录。
     * 必须限定部门，避免跨镇命中后在合并时改写 department_id。
     */
    private ReferralTracking findEpidemicRecordByCardId(String cardId, Long departmentId) {
        if (StrUtil.isBlank(cardId)) {
            return null;
        }
        return lambdaQuery()
                .eq(ReferralTracking::getBizMode, "track")
                .eq(ReferralTracking::getSourceType, "epidemic")
                .eq(ReferralTracking::getCardId, cardId)
                .eq(departmentId != null, ReferralTracking::getDepartmentId, departmentId)
                .isNull(departmentId == null, ReferralTracking::getDepartmentId)
                .last("LIMIT 1")
                .one();
    }

    /**
     * 按乡镇名称匹配部门；同名多条时优先与当前用户同区县的下级乡镇。
     * 匹配不到时回退到导入人部门。
     */
    private Long resolveTrackDepartmentId(String township, Long fallbackDeptId) {
        if (StrUtil.isBlank(township)) {
            return fallbackDeptId;
        }
        String name = township.trim();
        List<Department> matches = departmentService.lambdaQuery()
                .eq(Department::getName, name)
                .list();
        if (matches.isEmpty()) {
            return fallbackDeptId;
        }
        if (matches.size() == 1) {
            return matches.get(0).getId();
        }
        Long preferredCountyId = resolveCountyId(fallbackDeptId);
        if (preferredCountyId != null) {
            for (Department dept : matches) {
                if (preferredCountyId.equals(dept.getParentId())) {
                    return dept.getId();
                }
            }
        }
        return matches.get(0).getId();
    }

    /** 解析所属区县 ID：区县本身返回自身，乡镇返回 parentId */
    private Long resolveCountyId(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Department dept = departmentService.getById(deptId);
        if (dept == null) {
            return null;
        }
        if (Integer.valueOf(2).equals(dept.getLevel())) {
            return dept.getId();
        }
        return dept.getParentId();
    }

    private List<List<String>> buildExportHead(boolean recommendExport) {
        if (recommendExport) {
            return Arrays.asList(
                    List.of("姓名"), List.of("性别"), List.of("出生日期"), List.of("年龄"),
                    List.of("证件类型"), List.of("有效证件号"), List.of("民族"), List.of("联系电话"),
                    List.of("户籍地址"), List.of("现住详细地址"), List.of("人群分类"),
                    List.of("感染筛查时间"), List.of("感染筛查方法"), List.of("感染筛查结果"),
                    List.of("胸片筛查时间"), List.of("胸片筛查结果"),
                    List.of("推介单位名称"), List.of("填写用户名称"), List.of("推介原因"),
                    List.of("推介接收人"), List.of("推介状态"), List.of("追踪状态"), List.of("未到位次数"),
                    List.of("诊断结果"), List.of("诊断备注"), List.of("推介时间"), List.of("最新追踪时间"),
                    List.of("到位时间"), List.of("追踪过程明细"), List.of("未到位原因汇总"),
                    List.of("录入用户"), List.of("录入时间")
            );
        }
        return Arrays.asList(
                List.of("数据来源"), List.of("卡片ID"), List.of("患者姓名"), List.of("患儿家长姓名"),
                List.of("有效证件号"), List.of("性别"), List.of("出生日期"), List.of("年龄"),
                List.of("患者工作单位"), List.of("联系电话"), List.of("乡镇"), List.of("现住详细地址"),
                List.of("人群分类"), List.of("病例分类"), List.of("疾病名称"), List.of("报告单位"),
                List.of("报告卡录入时间"), List.of("备注"), List.of("追踪原因"), List.of("追踪状态"),
                List.of("未到位次数"), List.of("诊断结果"), List.of("诊断备注"), List.of("创建时间"),
                List.of("最新追踪时间"), List.of("到位时间"), List.of("追踪过程明细"), List.of("未到位原因汇总"),
                List.of("录入用户"), List.of("录入时间")
        );
    }

    private List<Object> buildExportRow(ReferralTracking r, boolean recommendExport) {
        String historyDetail = formatTrackingHistoryDetail(r.getTrackingHistoryJson());
        String failureReasons = formatFailureReasons(r.getTrackingHistoryJson());
        String latestTrackTime = formatLatestTrackTime(r.getTrackingHistoryJson());
        String creatorName = StrUtil.blankToDefault(r.getCreatorUserName(), "");
        String entryTime = formatExportDateTime(r.getCreateTime());
        if (recommendExport) {
            List<Object> row = new ArrayList<>(Arrays.asList(
                    r.getName(), r.getGender(),
                    r.getBirthDate() != null ? r.getBirthDate().toString() : "",
                    r.getAge(), r.getIdType(), r.getIdNumber(), r.getEthnicity(), r.getPhone(),
                    r.getHouseholdAddress(), r.getCurrentAddress(), r.getCrowdCategory(),
                    r.getScreenDate() != null ? r.getScreenDate().toString() : "",
                    r.getScreenMethod(), r.getInfectionResult(),
                    r.getChestXrayDate() != null ? r.getChestXrayDate().toString() : "",
                    r.getChestXrayResult(),
                    r.getRecommendUnitName(), r.getFillUserName(),
                    r.getRecommendReason(), r.getReceiverUserName(), recommendStatusLabel(r.getRecommendStatus()),
                    trackingStatusLabel(r.getTrackingStatus()),
                    r.getNotInPlaceCount() != null ? r.getNotInPlaceCount() : 0,
                    r.getDiagnosisResult(), r.getDiagnosisRemark(), formatRecommendTime(r),
                    latestTrackTime,
                    formatArrivalTime(r),
                    historyDetail, failureReasons
            ));
            row.add(creatorName);
            row.add(entryTime);
            return row;
        }
        List<Object> row = new ArrayList<>(Arrays.asList(
                "epidemic".equals(r.getSourceType()) ? "大疫情导入" : "手动录入",
                r.getCardId(), r.getName(), r.getParentName(), r.getIdNumber(), r.getGender(),
                r.getBirthDate() != null ? r.getBirthDate().toString() : "",
                r.getAge(), r.getWorkplace(), r.getPhone(), r.getTownship(), r.getCurrentAddress(),
                r.getCrowdCategory(), r.getCaseCategory(), r.getDiseaseName(), r.getReportUnit(),
                formatExportDateTime(r.getReportCardTime()),
                r.getEpidemicRemark(), r.getTrackReason(), trackingStatusLabel(r.getTrackingStatus()),
                r.getNotInPlaceCount() != null ? r.getNotInPlaceCount() : 0,
                r.getDiagnosisResult(), r.getDiagnosisRemark(),
                formatExportDateTime(r.getCreateTime()),
                latestTrackTime,
                formatArrivalTime(r),
                historyDetail, failureReasons
        ));
        row.add(creatorName);
        row.add(entryTime);
        return row;
    }

    private static String formatExportDateTime(LocalDateTime value) {
        return value != null ? value.format(EXPORT_DATETIME_FMT) : "";
    }

    /** 列表/导出展示：优先真实到位日期，否则回退系统到位时间 */
    private String formatArrivalTime(ReferralTracking r) {
        if (r.getActualArrivalDate() != null) {
            return r.getActualArrivalDate().toString();
        }
        return r.getArrivalTime() != null ? r.getArrivalTime().toString() : "";
    }

    private String formatRecommendTime(ReferralTracking r) {
        if (r.getRecommendSentTime() != null) {
            return formatExportDateTime(r.getRecommendSentTime());
        }
        return formatExportDateTime(r.getCreateTime());
    }

    private String formatTrackingHistoryDetail(String json) {
        List<Map<String, Object>> history = parseTrackingHistory(json);
        if (history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> entry : history) {
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append("第").append(entry.get("attempt")).append("次 ")
                    .append(entry.get("trackTime") != null ? entry.get("trackTime") : "")
                    .append(" ").append(trackingAttemptStatusLabel(toInteger(entry.get("status"))));
            Object reason = entry.get("reason");
            if (reason != null && StrUtil.isNotBlank(reason.toString())) {
                sb.append(" 原因：").append(reason);
            }
        }
        return sb.toString();
    }

    private String formatFailureReasons(String json) {
        List<Map<String, Object>> history = parseTrackingHistory(json);
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> entry : history) {
            if (!Integer.valueOf(2).equals(toInteger(entry.get("status")))) {
                continue;
            }
            Object reason = entry.get("reason");
            if (reason == null || StrUtil.isBlank(reason.toString())) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append("第").append(entry.get("attempt")).append("次 ")
                    .append(entry.get("trackTime") != null ? entry.get("trackTime") : "")
                    .append("：").append(reason);
        }
        return sb.toString();
    }

    private String formatLatestTrackTime(String json) {
        List<Map<String, Object>> history = parseTrackingHistory(json);
        if (history.isEmpty()) {
            return "";
        }
        Object trackTime = history.get(history.size() - 1).get("trackTime");
        return trackTime != null ? trackTime.toString() : "";
    }

    private String trackingAttemptStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 1 -> "到位";
            case 2 -> "未到位";
            case 3 -> "其他";
            default -> "";
        };
    }

    private String recommendStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "未发送";
            case 1 -> "待接收";
            case 2 -> "已接受";
            case 3 -> "已拒绝";
            default -> "";
        };
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, Map<String, Object> params) {
        ReferralTracking record = getAndCheckExist(id);
        if (isConfirmedReceivedRecommend(record)) {
            checkConfirmedRecommendReceiverOnly(record);
        } else {
            assertCanMutateRecord(record);
        }
        if (getStr(params, "name") != null) record.setName(getStr(params, "name"));
        if (getStr(params, "gender") != null) record.setGender(getStr(params, "gender"));
        if (params.get("birthDate") != null && StrUtil.isNotBlank(params.get("birthDate").toString())) {
            record.setBirthDate(parseDate(params.get("birthDate")));
        }
        if (getInt(params, "age") != null) record.setAge(getInt(params, "age"));
        if (getStr(params, "idType") != null) record.setIdType(getStr(params, "idType"));
        if (getStr(params, "idNumber") != null) {
            String idNumber = ImportIdentitySupport.normalizeIdNumber(getStr(params, "idNumber"));
            if (StrUtil.isNotBlank(idNumber) && !idNumber.matches("\\d{17}[\\dXx]")) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
            }
            record.setIdNumber(idNumber);
        }
        if (getStr(params, "ethnicity") != null) record.setEthnicity(getStr(params, "ethnicity"));
        if (getStr(params, "phone") != null) record.setPhone(getStr(params, "phone"));
        if (getStr(params, "householdAddress") != null) record.setHouseholdAddress(getStr(params, "householdAddress"));
        if (getStr(params, "currentAddress") != null) record.setCurrentAddress(getStr(params, "currentAddress"));
        if (getStr(params, "crowdCategory") != null) record.setCrowdCategory(getStr(params, "crowdCategory"));
        if (getStr(params, "trackReason") != null) record.setTrackReason(getStr(params, "trackReason"));
        if (getStr(params, "cardId") != null) record.setCardId(getStr(params, "cardId"));
        if (getStr(params, "parentName") != null) record.setParentName(getStr(params, "parentName"));
        if (getStr(params, "workplace") != null) record.setWorkplace(getStr(params, "workplace"));
        if (getStr(params, "township") != null) record.setTownship(getStr(params, "township"));
        if (getStr(params, "caseCategory") != null) record.setCaseCategory(getStr(params, "caseCategory"));
        if (getStr(params, "diseaseName") != null) record.setDiseaseName(getStr(params, "diseaseName"));
        if (getStr(params, "reportUnit") != null) record.setReportUnit(getStr(params, "reportUnit"));
        if (params.get("reportCardTime") != null && StrUtil.isNotBlank(params.get("reportCardTime").toString())) {
            record.setReportCardTime(parseDateTime(params.get("reportCardTime").toString()));
        }
        if (getStr(params, "epidemicRemark") != null) record.setEpidemicRemark(getStr(params, "epidemicRemark"));

        // 诊断结果修正（仅更新展示字段，不重新触发分流）
        boolean clearDiagnosisRemark = false;
        if (params.containsKey("diagnosisResult")) {
            String diagnosisResult = getStr(params, "diagnosisResult");
            String diagnosisRemark = getStr(params, "diagnosisRemark");
            if (StrUtil.isBlank(diagnosisResult)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
            }
            if (!Set.of("排除", "确诊患者", "潜伏感染者", "其他").contains(diagnosisResult)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID,
                        "无效的诊断结果，有效值：排除/确诊患者/潜伏感染者/其他");
            }
            if ("其他".equals(diagnosisResult) && StrUtil.isBlank(diagnosisRemark)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "选择其他时请填写备注");
            }
            if (StrUtil.isBlank(record.getDiagnosisResult())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "首次录入诊断请使用「录入诊断」功能");
            }
            record.setDiagnosisResult(diagnosisResult);
            if ("其他".equals(diagnosisResult)) {
                record.setDiagnosisRemark(diagnosisRemark.trim());
            } else {
                record.setDiagnosisRemark(null);
                clearDiagnosisRemark = true;
            }
        }

        // 追踪过程备注修正：按 attempt 回写 reason，并同步最新 trackingRemark
        if (params.containsKey("trackingHistory")) {
            applyTrackingHistoryRemarkUpdates(record, params.get("trackingHistory"));
        }

        if ("recommend".equals(record.getBizMode())) {
            if (Integer.valueOf(1).equals(record.getArchived())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "已归档记录不可编辑");
            }
            if (record.getRecommendStatus() != null && record.getRecommendStatus() >= 2) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "推介已接受或已拒绝，不可编辑");
            }
            if (getStr(params, "recommendReason") != null) {
                record.setRecommendReason(getStr(params, "recommendReason"));
            }
            if (params.containsKey("screenDate")) {
                record.setScreenDate(parseDate(params.get("screenDate")));
            }
            if (getStr(params, "screenMethod") != null) {
                record.setScreenMethod(getStr(params, "screenMethod"));
            }
            if (getStr(params, "infectionResult") != null) {
                record.setInfectionResult(getStr(params, "infectionResult"));
            }
            if (params.containsKey("chestXrayDate")) {
                record.setChestXrayDate(parseDate(params.get("chestXrayDate")));
            }
            if (getStr(params, "chestXrayResult") != null) {
                record.setChestXrayResult(getStr(params, "chestXrayResult"));
            }
            Long newReceiverId = getLong(params, "receiverUserId");
            if (newReceiverId != null && Integer.valueOf(0).equals(record.getRecommendStatus())) {
                User receiver = userService.getById(newReceiverId);
                if (receiver == null) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "接收人不存在");
                }
                if (receiver.getRole() == null || receiver.getRole() < 2 || receiver.getRole() > 6) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "推介接收人须为一至五级用户");
                }
                record.setReceiverUserId(newReceiverId);
                record.setReceiverDeptId(receiver.getDepartmentId());
            }
        }
        updateById(record);
        // updateById 默认忽略 null，切换「其他」以外结果时需显式清空诊断备注
        if (clearDiagnosisRemark) {
            lambdaUpdate()
                    .eq(ReferralTracking::getId, id)
                    .set(ReferralTracking::getDiagnosisRemark, null)
                    .update();
        }
    }

    /**
     * 按 attempt 合并前端提交的追踪备注，保留原有状态/时间等字段。
     */
    @SuppressWarnings("unchecked")
    private void applyTrackingHistoryRemarkUpdates(ReferralTracking record, Object trackingHistoryParam) {
        if (trackingHistoryParam == null) {
            return;
        }
        List<Map<String, Object>> existing = parseTrackingHistory(record.getTrackingHistoryJson());
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
        Map<Integer, String> reasonByAttempt = new HashMap<>();
        for (Map<String, Object> item : updates) {
            if (item == null) continue;
            Integer attempt = toInteger(item.get("attempt"));
            if (attempt == null) continue;
            Object reasonObj = item.get("reason");
            String reason = reasonObj == null ? "" : reasonObj.toString().trim();
            if (StrUtil.isBlank(reason)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "第" + attempt + "次追踪备注不能为空");
            }
            reasonByAttempt.put(attempt, reason);
        }
        if (reasonByAttempt.isEmpty()) {
            return;
        }
        for (Map<String, Object> entry : existing) {
            Integer attempt = toInteger(entry.get("attempt"));
            if (attempt != null && reasonByAttempt.containsKey(attempt)) {
                entry.put("reason", reasonByAttempt.get(attempt));
            }
        }
        record.setTrackingHistoryJson(JSONUtil.toJsonStr(existing));
        Object lastReason = existing.get(existing.size() - 1).get("reason");
        if (lastReason != null) {
            record.setTrackingRemark(lastReason.toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendRecommend(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        assertCanMutateRecord(record);
        if (!"recommend".equals(record.getBizMode())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅推介模式可发送推介通知");
        }
        if (record.getRecommendStatus() != null && record.getRecommendStatus() > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "推介通知已发送，不可重复发送");
        }
        if (record.getReceiverUserId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "未指定接收人");
        }

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getRecommendStatus, 1)
                .set(ReferralTracking::getRecommendSentTime, LocalDateTime.now())
                .update();

        // 向接收人发送系统消息
        String title = "新推介通知单待接收";
        String reasonPart = StrUtil.isNotBlank(record.getRecommendReason())
                ? "，推介原因：" + record.getRecommendReason() : "";
        String content = String.format("收到「%s」的推介通知单（人群分类：%s%s），请尽快确认接收。",
                StrUtil.blankToDefault(record.getName(), "（未知姓名）"),
                StrUtil.blankToDefault(record.getCrowdCategory(), "-"),
                reasonPart);
        sysMessageService.sendMessage(record.getReceiverUserId(), title, content, "referral_tracking_receive", id);
        log.info("推介通知单已发送，recordId={}, receiverUserId={}", id, record.getReceiverUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRecommend(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        if (isConfirmedRecommend(record)) {
            repairRecommendBizModeIfNeeded(id);
            syncReceiverRecommendMessage(getById(id), true, null);
            log.info("推介已确认（幂等），同步消息，recordId={}", id);
            return;
        }
        if (isRejectedRecommend(record)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该推介已被拒绝，不可再确认");
        }
        if (!isPendingRecommendReceive(record)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前状态无法确认（须为已发送状态）");
        }
        checkRecommendReceiver(record);

        String trackReason = StrUtil.blankToDefault(record.getRecommendReason(), "推介追踪");
        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getRecommendStatus, 2)
                .set(ReferralTracking::getRecommendConfirmTime, LocalDateTime.now())
                .set(ReferralTracking::getTrackReason, trackReason)
                .set(ReferralTracking::getBizMode, "recommend")
                .update();

        syncReceiverRecommendMessage(getById(id), true, null);

        // 通知推介发起人
        if (record.getCreatorId() != null) {
            String name = StrUtil.blankToDefault(record.getName(), "（未知姓名）");
            sysMessageService.sendMessage(record.getCreatorId(), "推介通知单已确认接收",
                    String.format("「%s」的推介通知单已被接收方确认，待接收方在推介页点击「追踪」开启共同追踪后，您也可参与追踪。", name),
                    "referral_tracking_confirmed", id);
        }
        log.info("推介通知单已确认接收，recordId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRecommend(Long id, String reason) {
        ReferralTracking record = getAndCheckExist(id);
        if (isRejectedRecommend(record)) {
            repairRecommendBizModeIfNeeded(id);
            syncReceiverRecommendMessage(getById(id), false, record.getRejectedReason());
            log.info("推介已拒绝（幂等），同步消息，recordId={}", id);
            return;
        }
        if (isConfirmedRecommend(record)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该推介已确认接收，无法拒绝");
        }
        if (!isPendingRecommendReceive(record)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前状态无法拒绝（须为已发送状态）");
        }
        checkRecommendReceiver(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getRecommendStatus, 3)
                .set(ReferralTracking::getRejectedReason, reason)
                .set(ReferralTracking::getRecommendConfirmTime, LocalDateTime.now())
                .set(ReferralTracking::getArchived, 1)
                .set(ReferralTracking::getBizMode, "recommend")
                .update();

        syncReceiverRecommendMessage(getById(id), false, reason);

        // 通知推介发起人
        if (record.getCreatorId() != null) {
            String name = StrUtil.blankToDefault(record.getName(), "（未知姓名）");
            sysMessageService.sendMessage(record.getCreatorId(), "推介通知单已被拒绝",
                    String.format("「%s」的推介通知单被接收方拒绝，原因：%s",
                            name, StrUtil.blankToDefault(reason, "（未填写）")),
                    "referral_tracking_rejected", id);
        }
        log.info("推介通知单已被拒绝，recordId={}, reason={}", id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableJointTracking(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        if (!isConfirmedRecommend(record)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅已确认接收的推介可开启共同追踪");
        }
        if (record.getArchived() != null && record.getArchived() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已归档，无法开启共同追踪");
        }
        if (Integer.valueOf(1).equals(record.getJointTracking())) {
            log.info("共同追踪已开启（幂等），recordId={}", id);
            return;
        }
        checkRecommendReceiver(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getJointTracking, 1)
                .set(ReferralTracking::getJointTrackingTime, LocalDateTime.now())
                .update();

        if (record.getCreatorId() != null) {
            String name = StrUtil.blankToDefault(record.getName(), "（未知姓名）");
            sysMessageService.sendMessage(record.getCreatorId(), "共同追踪已开启",
                    String.format("「%s」的推介已由接收方开启共同追踪，您可前往「推介」页面参与追踪。", name),
                    "referral_tracking_joint", id);
        }
        log.info("共同追踪已开启，recordId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void track(Long id, Integer status, String remark, LocalDate actualArrivalDate) {
        ReferralTracking record = getAndCheckExist(id);
        if (record.getRecommendSentTime() != null
                && !Integer.valueOf(2).equals(record.getRecommendStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "推介通知单尚未被接收方确认，暂不可追踪");
        }

        checkTrackOperatorOrCreator(record);

        // 已归档/已完成流程则不允许再操作
        if (record.getArchived() != null && record.getArchived() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已归档，无法继续追踪");
        }

        // 仅待追踪或未到位状态可继续追踪
        Integer trackingStatus = record.getTrackingStatus();
        if (trackingStatus != null && trackingStatus != 0 && trackingStatus != 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "当前追踪状态不允许继续操作");
        }

        if (status == null || status < 1 || status > 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "追踪状态值无效（1到位 2未到位 3其他）");
        }

        // 每次追踪必须填写备注
        if (StrUtil.isBlank(remark)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写追踪备注");
        }
        if (Integer.valueOf(1).equals(status) && actualArrivalDate == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择到位时间");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> history = parseTrackingHistory(record.getTrackingHistoryJson());

        switch (status) {
            case 1 -> {
                // 到位：记录系统到位时间与手动录入的真实到位日期
                Map<String, Object> entry = new HashMap<>();
                entry.put("attempt", history.size() + 1);
                entry.put("status", 1);
                entry.put("trackTime", now.toString());
                entry.put("actualArrivalDate", actualArrivalDate.toString());
                entry.put("reason", remark);
                history.add(entry);

                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTrackingStatus, 1)
                        .set(ReferralTracking::getArrivalTime, now)
                        .set(ReferralTracking::getActualArrivalDate, actualArrivalDate)
                        .set(ReferralTracking::getTrackingRemark, remark)
                        .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                        .update();
                log.info("推介追踪到位，recordId={}", id);
            }
            case 2 -> {
                // 未到位：累计次数；已确认推介（共同追踪）4 次强制结束，原生追踪 3 次
                int forceEndThreshold = isConfirmedReceivedRecommend(record) ? 4 : 3;
                int newCount = (record.getNotInPlaceCount() == null ? 0 : record.getNotInPlaceCount()) + 1;

                Map<String, Object> entry = new HashMap<>();
                entry.put("attempt", history.size() + 1);
                entry.put("status", 2);
                entry.put("trackTime", now.toString());
                entry.put("reason", remark);
                history.add(entry);

                if (newCount >= forceEndThreshold) {
                    lambdaUpdate()
                            .eq(ReferralTracking::getId, id)
                            .set(ReferralTracking::getTrackingStatus, 4)
                            .set(ReferralTracking::getNotInPlaceCount, newCount)
                            .set(ReferralTracking::getTrackingRemark, remark)
                            .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                            .set(ReferralTracking::getArchived, 1)
                            .update();
                    log.info("推介追踪{}次未到位强制结束，recordId={}", forceEndThreshold, id);
                } else {
                    lambdaUpdate()
                            .eq(ReferralTracking::getId, id)
                            .set(ReferralTracking::getTrackingStatus, 2)
                            .set(ReferralTracking::getNotInPlaceCount, newCount)
                            .set(ReferralTracking::getTrackingRemark, remark)
                            .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                            .update();
                }
            }
            case 3 -> {
                // 其他：归档
                Map<String, Object> entry = new HashMap<>();
                entry.put("attempt", history.size() + 1);
                entry.put("status", 3);
                entry.put("trackTime", now.toString());
                entry.put("reason", remark);
                history.add(entry);

                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTrackingStatus, 3)
                        .set(ReferralTracking::getTrackingRemark, remark)
                        .set(ReferralTracking::getTrackingHistoryJson, JSONUtil.toJsonStr(history))
                        .set(ReferralTracking::getArchived, 1)
                        .update();
                log.info("推介追踪选择其他，已归档，recordId={}", id);
            }
        }
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
    public void saveScreening(Long id, Map<String, Object> params) {
        ReferralTracking record = getAndCheckExist(id);
        if (!Integer.valueOf(1).equals(record.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后才可录入筛查信息");
        }
        checkTrackOperatorOrCreator(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(getStr(params, "hasInfectionScreen") != null,
                        ReferralTracking::getHasInfectionScreen, getStr(params, "hasInfectionScreen"))
                .set(params.get("screenDate") != null,
                        ReferralTracking::getScreenDate, parseDate(params.get("screenDate")))
                .set(getStr(params, "screenMethod") != null,
                        ReferralTracking::getScreenMethod, getStr(params, "screenMethod"))
                .set(getStr(params, "screenResult") != null,
                        ReferralTracking::getScreenResult, getStr(params, "screenResult"))
                .set(getStr(params, "infectionResult") != null,
                        ReferralTracking::getInfectionResult, getStr(params, "infectionResult"))
                .set(getStr(params, "hasChestXray") != null,
                        ReferralTracking::getHasChestXray, getStr(params, "hasChestXray"))
                .set(params.get("chestXrayDate") != null,
                        ReferralTracking::getChestXrayDate, parseDate(params.get("chestXrayDate")))
                .set(getStr(params, "chestXrayResult") != null,
                        ReferralTracking::getChestXrayResult, getStr(params, "chestXrayResult"))
                .set(getStr(params, "symptomsJson") != null,
                        ReferralTracking::getSymptomsJson, getStr(params, "symptomsJson"))
                .update();

        log.info("推介追踪筛查信息已保存，recordId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDiagnosis(Long id, String diagnosisResult, String diagnosisRemark) {
        if (StrUtil.isBlank(diagnosisResult)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "诊断结果不能为空");
        }
        if ("其他".equals(diagnosisResult) && StrUtil.isBlank(diagnosisRemark)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "选择其他时请填写备注");
        }
        ReferralTracking record = getAndCheckExist(id);
        if (!Integer.valueOf(1).equals(record.getTrackingStatus())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅追踪到位后才可录入诊断结果");
        }
        if (record.getArchived() != null && record.getArchived() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "该记录已归档，无法修改诊断结果");
        }
        checkTrackOperatorOrCreator(record);

        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getDiagnosisResult, diagnosisResult)
                .set(ReferralTracking::getDiagnosisRemark,
                        "其他".equals(diagnosisResult) ? diagnosisRemark.trim() : null)
                .set(ReferralTracking::getDiagnosisTime, LocalDateTime.now())
                .update();

        ReferralTracking updated = getById(id);

        switch (diagnosisResult) {
            case "排除", "其他" -> {
                archiveTrackingDiagnosis(id);
                log.info("推介追踪诊断归档（{}），recordId={}", diagnosisResult, id);
            }
            case "确诊患者" -> {
                // 确诊患者仅标红结案，不进入患者管理（患者管理数据仅来自专病信息表导入）
                archiveTrackingDiagnosis(id);
                log.info("推介追踪确诊患者结案，recordId={}", id);
            }
            case "潜伏感染者" -> {
                Long latentId = createLatentFromTracking(updated);
                lambdaUpdate()
                        .eq(ReferralTracking::getId, id)
                        .set(ReferralTracking::getTargetLatentId, latentId)
                        .set(ReferralTracking::getArchived, 1)
                        .update();
                log.info("推介追踪潜伏感染者，已创建潜伏记录 latentId={}，recordId={}", latentId, id);
            }
            default -> throw new ServiceException(StatusEnum.PARAM_INVALID,
                    "无效的诊断结果，有效值：排除/确诊患者/潜伏感染者/其他");
        }
    }

    /** 诊断归档：标记结案，不再分流至其他模块 */
    private void archiveTrackingDiagnosis(Long id) {
        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .set(ReferralTracking::getArchived, 1)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        ReferralTracking record = getAndCheckExist(id);
        assertCanDeleteRecord(record);
        removeById(record.getId());
        log.info("推介追踪记录已删除，recordId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            deleteRecord(id);
            count++;
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByFilter(String bizMode, String name, String idNumber, Integer trackingStatus, Integer archived,
                               String phone, String township, String dateFrom, String dateTo, String sourceType,
                               String creatorOrEntryUnit, String columnFilters,
                               String createTimeFrom, String createTimeTo) {
        Integer role = BaseContext.getCurrentRole();
        boolean level5RecommendView = "recommend".equals(bizMode) && Integer.valueOf(6).equals(role);
        LambdaQueryWrapper<ReferralTracking> wrapper = buildQueryWrapper(
                bizMode, name, idNumber, trackingStatus, archived, phone, township, dateFrom, dateTo, sourceType);
        applyCreateTimeFilter(wrapper, createTimeFrom, createTimeTo);
        applyCreatorOrEntryUnitFilter(wrapper, creatorOrEntryUnit);
        applyColumnFilters(wrapper, columnFilters);
        applyUserScopeFilter(wrapper, bizMode, level5RecommendView);
        wrapper.select(ReferralTracking::getId);
        List<Long> ids = list(wrapper).stream().map(ReferralTracking::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return batchDelete(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAll(String bizMode) {
        return deleteByFilter(bizMode, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    // ===== 私有工具方法 =====

    private ReferralTracking getAndCheckExist(Long id) {
        ReferralTracking record = getById(id);
        if (record == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "推介追踪记录不存在");
        }
        return record;
    }

    /** 推介模式：仅接收方可确认/拒绝 */
    private void checkRecommendReceiver(ReferralTracking record) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null || !userId.equals(record.getReceiverUserId())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅推介接收方可确认或拒绝");
        }
    }

    /** 已确认推介：仅接收方可追踪；其余记录按辖区/创建人/接收人规则 */
    private void checkTrackOperatorOrCreator(ReferralTracking record) {
        if (isConfirmedReceivedRecommend(record)) {
            checkConfirmedRecommendReceiverOnly(record);
            return;
        }
        if (!canOperateRecord(record)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无权进行此操作");
        }
    }

    private boolean isConfirmedReceivedRecommend(ReferralTracking record) {
        return isConfirmedRecommend(record) && record.getRecommendSentTime() != null;
    }

    private void checkConfirmedRecommendReceiverOnly(ReferralTracking record) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long userId = BaseContext.getCurrentId();
        if (userId != null && userId.equals(record.getReceiverUserId())) {
            return;
        }
        if (Integer.valueOf(1).equals(record.getJointTracking())
                && userId != null && userId.equals(record.getCreatorId())) {
            return;
        }
        throw new ServiceException(StatusEnum.PARAM_INVALID, "该推介已由接收方承接追踪，仅接收方可操作");
    }

    /** 编辑：创建人、接收人或辖区一至五级用户 */
    private void assertCanMutateRecord(ReferralTracking record) {
        if (!canOperateRecord(record)) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权操作该记录");
        }
    }

    /** 删除：须具备 referralManagement:delete 权限；推介模块列表可见用户均可删（各推介/追踪状态）；追踪模块按辖区规则 */
    private void assertCanDeleteRecord(ReferralTracking record) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权删除该记录");
        }
        if ("recommend".equals(record.getBizMode())) {
            if (canOperateRecord(record)) {
                return;
            }
            Integer role = BaseContext.getCurrentRole();
            if (role != null && role >= 2 && role <= 6) {
                return;
            }
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权删除该记录");
        }
        if (canOperateRecord(record)) {
            return;
        }
        throw new ServiceException(StatusEnum.FORBIDDEN, "无权删除该记录");
    }

    /** 按角色与部门辖区过滤可见范围（上级可查看下级推介/追踪全过程） */
    private void applyUserScopeFilter(LambdaQueryWrapper<ReferralTracking> wrapper, String bizMode,
                                      boolean level5RecommendView) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        Long userId = BaseContext.getCurrentId();
        if (role == null || userId == null) {
            wrapper.apply("1 = 0");
            return;
        }
        if ("recommend".equals(bizMode) && level5RecommendView) {
            wrapper.and(w -> w.eq(ReferralTracking::getCreatorId, userId)
                    .or()
                    .eq(ReferralTracking::getReceiverUserId, userId));
            return;
        }
        if (role == 6) {
            Long deptId = BaseContext.getCurrentDepartmentId();
            wrapper.and(w -> {
                w.eq(ReferralTracking::getCreatorId, userId)
                        .or()
                        .eq(ReferralTracking::getReceiverUserId, userId);
                if (deptId != null) {
                    w.or().nested(w2 -> w2.eq(ReferralTracking::getBizMode, "track")
                            .eq(ReferralTracking::getDepartmentId, deptId));
                }
            });
            return;
        }
        applyManagerScopeFilter(wrapper, userId);
    }

    /** 一至四级：可见本部门及下级部门相关记录（含本人发起或接收的跨级/同级推介） */
    private void applyManagerScopeFilter(LambdaQueryWrapper<ReferralTracking> wrapper, Long userId) {
        List<Long> deptIds = resolveScopedDepartmentIds();
        if (deptIds.isEmpty()) {
            wrapper.and(w -> w.eq(ReferralTracking::getCreatorId, userId)
                    .or()
                    .eq(ReferralTracking::getReceiverUserId, userId));
            return;
        }
        List<Long> userIdsInScope = userService.lambdaQuery()
                .select(User::getId)
                .in(User::getDepartmentId, deptIds)
                .list()
                .stream()
                .map(User::getId)
                .toList();
        wrapper.and(w -> {
            w.in(ReferralTracking::getDepartmentId, deptIds)
                    .or()
                    .in(ReferralTracking::getReceiverDeptId, deptIds);
            if (!userIdsInScope.isEmpty()) {
                w.or().in(ReferralTracking::getCreatorId, userIdsInScope)
                        .or()
                        .in(ReferralTracking::getReceiverUserId, userIdsInScope);
            }
        });
    }

    private List<Long> resolveScopedDepartmentIds() {
        Long deptId = BaseContext.getCurrentDepartmentId();
        if (deptId == null) {
            return List.of();
        }
        List<Long> deptIds = departmentService.getDescendantIds(deptId);
        return deptIds != null ? deptIds : List.of();
    }

    /** 详情/操作前校验当前用户是否有权访问该记录 */
    private void assertCanAccessRecord(ReferralTracking record) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权查看该记录");
        }
        if (userId.equals(record.getCreatorId()) || userId.equals(record.getReceiverUserId())) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 6 && "track".equals(record.getBizMode())) {
            Long deptId = BaseContext.getCurrentDepartmentId();
            if (deptId != null && deptId.equals(record.getDepartmentId())) {
                return;
            }
        }
        if (role != null && role >= 2 && role <= 5 && canAccessViaDepartmentScope(record, userId)) {
            return;
        }
        throw new ServiceException(StatusEnum.FORBIDDEN, "无权查看该记录");
    }

    /** 追踪/大疫情：创建人、接收人或辖区一至五级用户可操作 */
    private boolean canOperateRecord(ReferralTracking record) {
        if (BaseContext.isSuperAdmin()) {
            return true;
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return false;
        }
        if (userId.equals(record.getCreatorId())) {
            return true;
        }
        if (record.getReceiverUserId() != null && userId.equals(record.getReceiverUserId())) {
            return true;
        }
        Integer role = BaseContext.getCurrentRole();
        if (role == null || role < 2 || role > 6) {
            return false;
        }
        if (role == 6 && "track".equals(record.getBizMode())) {
            Long deptId = BaseContext.getCurrentDepartmentId();
            return deptId != null && deptId.equals(record.getDepartmentId());
        }
        return canAccessViaDepartmentScope(record, userId);
    }

    private boolean canAccessViaDepartmentScope(ReferralTracking record, Long userId) {
        if (userId.equals(record.getCreatorId()) || userId.equals(record.getReceiverUserId())) {
            return true;
        }
        List<Long> deptIds = resolveScopedDepartmentIds();
        if (deptIds.isEmpty()) {
            return false;
        }
        if (record.getDepartmentId() != null && deptIds.contains(record.getDepartmentId())) {
            return true;
        }
        if (record.getReceiverDeptId() != null && deptIds.contains(record.getReceiverDeptId())) {
            return true;
        }
        if (record.getCreatorId() != null) {
            User creator = userService.getById(record.getCreatorId());
            if (creator != null && creator.getDepartmentId() != null
                    && deptIds.contains(creator.getDepartmentId())) {
                return true;
            }
        }
        if (record.getReceiverUserId() != null) {
            User receiver = userService.getById(record.getReceiverUserId());
            if (receiver != null && receiver.getDepartmentId() != null
                    && deptIds.contains(receiver.getDepartmentId())) {
                return true;
            }
        }
        return false;
    }

    /** 从推介追踪记录创建潜伏感染记录（populationType='referral'） */
    private Long createLatentFromTracking(ReferralTracking r) {
        if (r.getTargetLatentId() != null) return r.getTargetLatentId();

        LatentInfection latent = LatentInfection.builder()
                .screeningId(null)
                .populationType("referral")
                .name(r.getName())
                .idNumber(r.getIdNumber())
                .gender(r.getGender())
                .age(r.getAge())
                .phone(r.getPhone())
                .infectionScreenDate(r.getScreenDate())
                .infectionResult(r.getInfectionResult())
                .trackingStatus(1)
                .notInPlaceCount(0)
                .hasChestXray(r.getHasChestXray())
                .chestXrayDate(r.getChestXrayDate())
                .chestXrayResult(r.getChestXrayResult())
                .diagnosisFirst("潜伏感染者")
                .diagnosisResult("潜伏感染者")
                .referralResult("latent")
                .treatmentPhase(0)
                .archived(0)
                .departmentId(r.getDepartmentId())
                .creatorId(resolveTrackingCreatorId(r))
                .build();

        latentInfectionMapper.insert(latent);
        return latent.getId();
    }

    /** 推介追踪分流时继承创建人，便于五级录入者在下游模块继续可见 */
    private Long resolveTrackingCreatorId(ReferralTracking r) {
        if (r.getCreatorId() != null) {
            return r.getCreatorId();
        }
        return BaseContext.getCurrentId();
    }

    /** 追踪模式必填项校验 */
    private void validateTrackRequired(Map<String, Object> params) {
        String idNumber = ImportIdentitySupport.normalizeIdNumber(getStr(params, "idNumber"));
        params.put("idNumber", idNumber);
        if (StrUtil.isNotBlank(idNumber) && !idNumber.matches("\\d{17}[\\dXx]")) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isBlank(getStr(params, "phone"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写联系电话");
        }
        if (StrUtil.isBlank(getStr(params, "currentAddress"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写现住址");
        }
        if (StrUtil.isBlank(getStr(params, "crowdCategory"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择人群分类");
        }
        if (StrUtil.isBlank(getStr(params, "trackReason"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写追踪原因");
        }
    }

    private String formatReceiverDisplayName(User receiver) {
        String username = StrUtil.blankToDefault(receiver.getUsername(), "-");
        String orgName = StrUtil.blankToDefault(receiver.getOrgName(), "未填写单位");
        return username + "（" + orgName + "）";
    }

    /** 填充接收人、录入者、录入单位等展示字段 */
    private void fillDisplayNames(ReferralTracking record) {
        if (record.getReceiverUserId() != null) {
            User receiver = userService.getById(record.getReceiverUserId());
            if (receiver != null) {
                record.setReceiverUserName(formatReceiverDisplayName(receiver));
            }
        }
        User creator = record.getCreatorId() != null ? userService.getById(record.getCreatorId()) : null;
        if (creator != null) {
            record.setCreatorUserName(StrUtil.blankToDefault(creator.getRealName(), creator.getUsername()));
        }
        if (record.getDepartmentId() != null) {
            Department dept = departmentService.getById(record.getDepartmentId());
            if (dept != null) {
                record.setEntryUnitName(dept.getName());
            }
        }
        if (StrUtil.isBlank(record.getEntryUnitName()) && creator != null) {
            record.setEntryUnitName(creator.getOrgName());
        }
        if (StrUtil.isBlank(record.getRecommendUnitName()) && creator != null) {
            record.setRecommendUnitName(resolveRecommendUnitName(creator));
        }
        if (StrUtil.isBlank(record.getFillUserName()) && creator != null) {
            record.setFillUserName(resolveFillUserName(creator));
        }
    }

    private String resolveRecommendUnitName(User user) {
        if (user == null) {
            return "";
        }
        return StrUtil.blankToDefault(user.getOrgName(), "");
    }

    private String resolveFillUserName(User user) {
        if (user == null) {
            return "";
        }
        return StrUtil.blankToDefault(user.getRealName(), user.getUsername());
    }

    /** 录入者或录入单位：匹配创建人姓名/用户名、用户所属单位名称，或录入部门名称 */
    private void applyCreatorOrEntryUnitFilter(LambdaQueryWrapper<ReferralTracking> wrapper, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        List<Long> creatorUserIds = userService.lambdaQuery()
                .and(w -> w.like(User::getRealName, keyword)
                        .or().like(User::getUsername, keyword)
                        .or().like(User::getOrgName, keyword))
                .list()
                .stream()
                .map(User::getId)
                .distinct()
                .toList();
        List<Long> deptIds = departmentService.resolveIdsByNameLike(keyword);
        if (creatorUserIds.isEmpty() && deptIds.isEmpty()) {
            wrapper.eq(ReferralTracking::getId, -1L);
            return;
        }
        wrapper.and(w -> {
            boolean added = false;
            if (!creatorUserIds.isEmpty()) {
                w.in(ReferralTracking::getCreatorId, creatorUserIds);
                added = true;
            }
            if (!deptIds.isEmpty()) {
                if (added) {
                    w.or();
                }
                w.in(ReferralTracking::getDepartmentId, deptIds);
            }
        });
    }

    /** 推介模式必填项校验 */
    private void validateRecommendRequired(Map<String, Object> params) {
        String idNumber = ImportIdentitySupport.normalizeIdNumber(getStr(params, "idNumber"));
        params.put("idNumber", idNumber);
        if (StrUtil.isNotBlank(idNumber) && !idNumber.matches("\\d{17}[\\dXx]")) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "身份证号格式不正确");
        }
        if (StrUtil.isBlank(getStr(params, "phone"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写联系电话");
        }
        if (StrUtil.isBlank(getStr(params, "currentAddress"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写现住址");
        }
        if (StrUtil.isBlank(getStr(params, "crowdCategory"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择人群分类");
        }
        if (StrUtil.isBlank(getStr(params, "recommendReason"))) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请填写推介原因");
        }
        if (getLong(params, "receiverUserId") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择推介接收人");
        }
    }

    private String getStr(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val == null ? null : val.toString();
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) return null;
        return Integer.valueOf(val.toString());
    }

    private Long getLong(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) return null;
        return Long.valueOf(val.toString());
    }

    private LocalDate parseDate(Object val) {
        return FlexibleDateParseUtil.parse(val);
    }

    private LocalDateTime parseDateTime(String text) {
        if (StrUtil.isBlank(text)) return null;
        String val = normalizeExcelCellText(text.trim());

        LocalDateTime fromSerial = parseExcelSerialDateTime(val);
        if (fromSerial != null) {
            return fromSerial;
        }

        for (String pattern : new String[]{
                "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss",
                "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy.MM.dd HH:mm",
                "yyyy-M-d HH:mm:ss", "yyyy-M-d HH:mm", "yyyy-M-d H:mm:ss", "yyyy-M-d H:mm",
                "yyyy/M/d HH:mm:ss", "yyyy/M/d HH:mm", "M/d/yy H:mm:ss", "M/d/yy H:mm",
                "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd", "yyyy-M-d", "yyyy/M/d", "yyyyMMdd",
                "yyyy年MM月dd日 HH:mm:ss", "yyyy年MM月dd日 HH:mm", "yyyy年MM月dd日"
        }) {
            try {
                if (pattern.contains("HH")) {
                    return LocalDateTime.parse(val, DateTimeFormatter.ofPattern(pattern));
                }
                LocalDate date = LocalDate.parse(val, DateTimeFormatter.ofPattern(pattern));
                return date.atStartOfDay();
            } catch (Exception ignored) {
            }
        }

        LocalDate dateOnly = parseDate(val);
        if (dateOnly != null) {
            return dateOnly.atStartOfDay();
        }
        return null;
    }

    /** 兼容 Excel 日期/时间单元格（Date、LocalDateTime、数值序列号、ReadCellData、字符串等） */
    private LocalDateTime parseDateTimeCell(Object val) {
        val = unwrapExcelCellValue(val);
        if (val == null) {
            return null;
        }
        if (val instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (val instanceof LocalDate ld) {
            return ld.atStartOfDay();
        }
        if (val instanceof java.util.Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (val instanceof Number n) {
            LocalDateTime fromSerial = parseExcelSerialDateTime(n.doubleValue());
            if (fromSerial != null) {
                return fromSerial;
            }
        }
        return parseDateTime(cellToText(val));
    }

    private LocalDate parseDateCell(Object val) {
        val = unwrapExcelCellValue(val);
        return FlexibleDateParseUtil.parse(val);
    }

    private LocalDateTime parseExcelSerialDateTime(double serial) {
        if (serial <= 59) {
            return null;
        }
        long days = (long) Math.floor(serial);
        LocalDate date = LocalDate.of(1899, 12, 30).plusDays(days);
        double fraction = serial - days;
        if (fraction > 0) {
            int seconds = (int) Math.round(fraction * 86400);
            return date.atStartOfDay().plusSeconds(seconds);
        }
        return date.atStartOfDay();
    }

    private LocalDateTime parseExcelSerialDateTime(String val) {
        if (!val.matches("^\\d+(\\.\\d+)?$")) {
            return null;
        }
        try {
            return parseExcelSerialDateTime(Double.parseDouble(val));
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 规范化 Excel 单元格文本（科学计数法、整数型小数等） */
    private String normalizeExcelCellText(String val) {
        if (StrUtil.isBlank(val)) {
            return "";
        }
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

    private Integer parseInt(String text) {
        if (StrUtil.isBlank(text)) return null;
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Map<Integer, Object>> readExcelRows(MultipartFile file) {
        List<Map<Integer, Object>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, Object>>() {
                @Override
                public void invoke(Map<Integer, Object> data, AnalysisContext context) {
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
        return allRows;
    }

    private Map<String, Integer> buildHeaderIndex(Map<Integer, Object> headerRow) {
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, Object> entry : headerRow.entrySet()) {
            String header = normalizeHeader(cellToText(entry.getValue()));
            if (StrUtil.isNotBlank(header)) {
                headerIndex.put(header, entry.getKey());
            }
        }
        for (Map.Entry<String, String> alias : EpidemicTrackImportHeaders.HEADER_ALIASES.entrySet()) {
            Integer idx = headerIndex.get(normalizeHeader(alias.getKey()));
            if (idx != null) {
                headerIndex.putIfAbsent(normalizeHeader(alias.getValue()), idx);
            }
        }
        return headerIndex;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return null;
        }
        return header.replace('\u00A0', ' ')
                .replace("\n", "")
                .replace("\r", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private String cellToText(Object val) {
        val = unwrapExcelCellValue(val);
        if (val == null) {
            return null;
        }
        if (val instanceof String s) {
            return StrUtil.isBlank(s) ? null : normalizeExcelCellText(s.trim());
        }
        if (val instanceof java.util.Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (val instanceof LocalDateTime ldt) {
            return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (val instanceof LocalDate ld) {
            return ld.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (val instanceof Number n) {
            return normalizeExcelCellText(n.toString());
        }
        String text = val.toString().trim();
        return StrUtil.isBlank(text) ? null : normalizeExcelCellText(text);
    }

    private Object getFieldCellValue(Map<Integer, Object> row, Map<String, Integer> headerIndex, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String normalizedField = normalizeHeader(fieldName);
            Integer idx = headerIndex.get(normalizedField);
            if (idx != null) {
                Object val = unwrapExcelCellValue(row.get(idx));
                if (val != null && !(val instanceof String s && StrUtil.isBlank(s))) {
                    return val;
                }
            }
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                if (normalizedField.length() <= 2) {
                    if (entry.getKey().equals(normalizedField)) {
                        Object val = unwrapExcelCellValue(row.get(entry.getValue()));
                        if (val != null && !(val instanceof String s && StrUtil.isBlank(s))) {
                            return val;
                        }
                    }
                } else if (entry.getKey().contains(normalizedField)) {
                    Object val = unwrapExcelCellValue(row.get(entry.getValue()));
                    if (val != null && !(val instanceof String s && StrUtil.isBlank(s))) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    /** EasyExcel 4.x Map 读取时单元格可能为 ReadCellData，需解包后再解析 */
    private Object unwrapExcelCellValue(Object val) {
        if (!(val instanceof ReadCellData<?> cellData)) {
            return val;
        }
        if (cellData.getData() != null) {
            return cellData.getData();
        }
        // DATE / NUMBER 等类型均可能携带 Excel 序列号
        if (cellData.getNumberValue() != null) {
            return cellData.getNumberValue().doubleValue();
        }
        if (cellData.getOriginalNumberValue() != null) {
            return cellData.getOriginalNumberValue().doubleValue();
        }
        if (StrUtil.isNotBlank(cellData.getStringValue())) {
            return cellData.getStringValue().trim();
        }
        return null;
    }

    private String getFieldByHeader(Map<Integer, Object> row, Map<String, Integer> headerIndex, String... fieldNames) {
        return cellToText(getFieldCellValue(row, headerIndex, fieldNames));
    }

    private ReferralTracking findEpidemicRecord(String cardId, String idNumber, String name, String birthDateText, String phone) {
        if (StrUtil.isNotBlank(cardId)) {
            return lambdaQuery()
                    .eq(ReferralTracking::getBizMode, "track")
                    .eq(ReferralTracking::getSourceType, "epidemic")
                    .eq(ReferralTracking::getCardId, cardId)
                    .last("LIMIT 1")
                    .one();
        }
        LocalDate birthDate = parseDate(birthDateText);
        String normalizedId = ImportIdentitySupport.normalizeIdNumber(idNumber);
        return lambdaQuery()
                .eq(ReferralTracking::getBizMode, "track")
                .eq(ReferralTracking::getSourceType, "epidemic")
                .and(w -> {
                    if (StrUtil.isNotBlank(normalizedId)) {
                        w.eq(ReferralTracking::getIdNumber, normalizedId);
                    } else {
                        w.eq(ReferralTracking::getName, name)
                                .eq(birthDate != null, ReferralTracking::getBirthDate, birthDate)
                                .eq(StrUtil.isNotBlank(phone), ReferralTracking::getPhone, phone);
                    }
                })
                .last("LIMIT 1")
                .one();
    }

    /** 重复导入时补全报告卡录入时间、录入人等信息（不得跨镇改写 department_id） */
    private boolean mergeEpidemicImportFields(ReferralTracking existing, LocalDateTime reportCardTime,
                                              String currentAddress, String township,
                                              Long currentUserId, Long targetDeptId, Integer importRowNo) {
        boolean changed = false;
        if (reportCardTime != null && existing.getReportCardTime() == null) {
            existing.setReportCardTime(reportCardTime);
            changed = true;
        }
        if (StrUtil.isNotBlank(currentAddress) && !currentAddress.equals(existing.getCurrentAddress())) {
            existing.setCurrentAddress(currentAddress);
            changed = true;
        }
        if (StrUtil.isNotBlank(township) && !township.equals(existing.getTownship())) {
            existing.setTownship(township);
            changed = true;
        }
        if (existing.getCreatorId() == null && currentUserId != null) {
            existing.setCreatorId(currentUserId);
            changed = true;
        }
        if (importRowNo != null && !importRowNo.equals(existing.getImportRowNo())) {
            existing.setImportRowNo(importRowNo);
            changed = true;
        }
        // 仅补齐空部门，禁止把其它镇已归属记录抢到本镇
        if (existing.getDepartmentId() == null && targetDeptId != null) {
            existing.setDepartmentId(targetDeptId);
            changed = true;
        }
        return changed;
    }

    /** 定位表头行（兼容大疫情表标题行） */
    private int resolveHeaderRowIndex(List<Map<Integer, Object>> allRows) {
        int maxScan = Math.min(allRows.size(), 6);
        for (int i = 0; i < maxScan; i++) {
            Map<String, Integer> idx = buildHeaderIndex(allRows.get(i));
            if (idx.containsKey(normalizeHeader("卡片ID"))
                    || idx.containsKey(normalizeHeader("患者姓名"))
                    || idx.containsKey(normalizeHeader("有效证件号"))) {
                return i;
            }
        }
        return 0;
    }

    /** 优先精确匹配报告卡录入时间表头，避免误匹配其它「录入时间」列 */
    private Object getReportCardTimeCell(Map<Integer, Object> row, Map<String, Integer> headerIndex) {
        Object val = getFieldCellValue(row, headerIndex,
                "报告卡录入时间", "报告卡录入日期", "录卡时间", "报告卡录卡时间", "录卡日期", "医生填卡日期");
        if (val != null) {
            return val;
        }
        return getFieldCellValue(row, headerIndex, "录入时间");
    }

    private String extractTownship(String address) {
        if (StrUtil.isBlank(address)) return null;
        String normalized = address.replace('\u00A0', ' ').replaceAll("\\s+", "").trim();
        String township = extractAdministrativeUnit(normalized, "街道");
        if (StrUtil.isNotBlank(township)) return township;
        township = extractAdministrativeUnit(normalized, "乡");
        if (StrUtil.isNotBlank(township)) return township;
        township = extractAdministrativeUnit(normalized, "镇");
        if (StrUtil.isNotBlank(township)) return township;
        township = extractAdministrativeUnit(normalized, "区");
        if (StrUtil.isNotBlank(township)) return township;
        return extractAdministrativeUnit(normalized, "县");
    }

    private String extractAdministrativeUnit(String address, String suffix) {
        int idx = address.lastIndexOf(suffix);
        if (idx <= 0) return null;
        int start = -1;
        for (String separator : List.of("省", "市", "州", "县", "区")) {
            start = Math.max(start, address.lastIndexOf(separator, idx - 1));
        }
        String unit = address.substring(start + 1, idx + suffix.length()).trim();
        if (unit.length() <= suffix.length() || unit.length() > 20) return null;
        return unit;
    }

    private LambdaQueryWrapper<ReferralTracking> buildQueryWrapper(
            String bizMode, String name, String idNumber, Integer trackingStatus, Integer archived,
            String phone, String township, String dateFrom, String dateTo, String sourceType) {
        LocalDateTime from = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
        LocalDateTime to = QueryDateRangeUtil.parseDateTimeTo(dateTo);

        LambdaQueryWrapper<ReferralTracking> wrapper = new LambdaQueryWrapper<>();
        applyBizModeFilter(wrapper, bizMode);
        wrapper
                .like(StrUtil.isNotBlank(name), ReferralTracking::getName, name)
                .like(StrUtil.isNotBlank(idNumber), ReferralTracking::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), ReferralTracking::getPhone, phone)
                .like(StrUtil.isNotBlank(township), ReferralTracking::getTownship, township)
                .eq(trackingStatus != null, ReferralTracking::getTrackingStatus, trackingStatus)
                .eq(archived != null, ReferralTracking::getArchived, archived)
                .eq(StrUtil.isNotBlank(sourceType), ReferralTracking::getSourceType, sourceType);
        if ("track".equals(bizMode)) {
            wrapper.ge(from != null, ReferralTracking::getReportCardTime, from)
                    .le(to != null, ReferralTracking::getReportCardTime, to);
            ImportRowOrderSupport.applyWithBatch(wrapper);
        } else {
            wrapper.ge(from != null, ReferralTracking::getCreateTime, from)
                    .le(to != null, ReferralTracking::getCreateTime, to)
                    .orderByDesc(ReferralTracking::getCreateTime);
        }
        return wrapper;
    }

    /** 录入时间（create_time）筛选，推介与追踪均可叠加使用 */
    private void applyCreateTimeFilter(LambdaQueryWrapper<ReferralTracking> wrapper,
                                       String createTimeFrom, String createTimeTo) {
        LocalDateTime from = QueryDateRangeUtil.parseDateTimeFrom(createTimeFrom);
        LocalDateTime to = QueryDateRangeUtil.parseDateTimeTo(createTimeTo);
        wrapper.ge(from != null, ReferralTracking::getCreateTime, from)
                .le(to != null, ReferralTracking::getCreateTime, to);
    }

    private String trackingStatusLabel(Integer status) {
        if (status == null) return "待追踪";
        return switch (status) {
            case 1 -> "到位";
            case 2 -> "未到位";
            case 3 -> "其他";
            case 4 -> "强制结束";
            default -> "待追踪";
        };
    }

    /**
     * 推介列表：biz_mode=recommend（发起方与接收方确认后均保留在本模块开展共同追踪）。
     * 追踪列表：仅原生 biz_mode=track（不含已确认推介，推介追踪统一在推介模块完成）。
     */
    private void applyBizModeFilter(LambdaQueryWrapper<ReferralTracking> wrapper, String bizMode) {
        if (StrUtil.isBlank(bizMode)) {
            return;
        }
        if ("track".equals(bizMode)) {
            wrapper.eq(ReferralTracking::getBizMode, "track")
                    .and(t -> t.isNull(ReferralTracking::getRecommendSentTime)
                            .or().ne(ReferralTracking::getRecommendStatus, 2));
            return;
        }
        if ("recommend".equals(bizMode)) {
            wrapper.and(w -> w.eq(ReferralTracking::getBizMode, "recommend")
                    .or(or -> or.eq(ReferralTracking::getBizMode, "track")
                            .isNotNull(ReferralTracking::getRecommendSentTime)
                            .eq(ReferralTracking::getRecommendStatus, 2)));
            return;
        }
        wrapper.eq(ReferralTracking::getBizMode, bizMode);
    }

    /** 已发送、待接收方确认的推介（与 biz_mode 无关，兼容历史误标为 track） */
    private boolean isPendingRecommendReceive(ReferralTracking record) {
        return record.getRecommendSentTime() != null
                && Integer.valueOf(1).equals(record.getRecommendStatus());
    }

    private boolean isConfirmedRecommend(ReferralTracking record) {
        if (Integer.valueOf(2).equals(record.getRecommendStatus())) {
            return true;
        }
        return record.getRecommendConfirmTime() != null
                && record.getRecommendSentTime() != null
                && !Integer.valueOf(3).equals(record.getRecommendStatus());
    }

    private boolean isRejectedRecommend(ReferralTracking record) {
        return Integer.valueOf(3).equals(record.getRecommendStatus());
    }

    /** 历史确认逻辑曾将 biz_mode 写成 track，纠正为 recommend */
    private void repairRecommendBizModeIfNeeded(Long id) {
        lambdaUpdate()
                .eq(ReferralTracking::getId, id)
                .eq(ReferralTracking::getBizMode, "track")
                .isNotNull(ReferralTracking::getRecommendSentTime)
                .set(ReferralTracking::getBizMode, "recommend")
                .update();
    }

    /** 接收方待确认推介消息在确认/拒绝后同步更新 */
    private void syncReceiverRecommendMessage(ReferralTracking record, boolean confirmed, String rejectReason) {
        if (record == null) {
            return;
        }
        String name = StrUtil.blankToDefault(record.getName(), "（未知姓名）");
        if (confirmed) {
            sysMessageService.updatePendingMessageByBizId(
                    record.getId(),
                    "referral_tracking_receive",
                    "referral_tracking_confirmed",
                    "推介已接收",
                    String.format("「%s」的推介通知单您已确认接收，请在本页开展追踪。", name));
        } else {
            sysMessageService.updatePendingMessageByBizId(
                    record.getId(),
                    "referral_tracking_receive",
                    "referral_tracking_rejected",
                    "推介已被拒绝",
                    String.format("「%s」的推介通知单您已拒绝，原因：%s",
                            name, StrUtil.blankToDefault(rejectReason, "（未填写）")));
        }
    }

    @Override
    public long countRecommendSentForDashboard(Integer statYear, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ReferralTracking> wrapper = buildRecommendDashboardWrapper(filterDeptIds);
        wrapper.isNotNull(ReferralTracking::getRecommendSentTime);
        applyStatYearTimeFilter(wrapper, ReferralTracking::getRecommendSentTime, statYear);
        return count(wrapper);
    }

    @Override
    public long countRecommendArrivedForDashboard(Integer statYear, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ReferralTracking> wrapper = buildRecommendDashboardWrapper(filterDeptIds);
        wrapper.isNotNull(ReferralTracking::getRecommendSentTime)
                .isNotNull(ReferralTracking::getArrivalTime);
        applyStatYearTimeFilter(wrapper, ReferralTracking::getRecommendSentTime, statYear);
        return count(wrapper);
    }

    /** 首页推介统计：与推介管理列表一致的业务范围与数据权限 */
    private LambdaQueryWrapper<ReferralTracking> buildRecommendDashboardWrapper(List<Long> filterDeptIds) {
        LambdaQueryWrapper<ReferralTracking> wrapper = new LambdaQueryWrapper<>();
        applyBizModeFilter(wrapper, "recommend");
        Integer role = BaseContext.getCurrentRole();
        boolean level5RecommendView = Integer.valueOf(6).equals(role);
        applyUserScopeFilter(wrapper, "recommend", level5RecommendView);
        applyDashboardDepartmentFilter(wrapper, filterDeptIds);
        return wrapper;
    }

    @Override
    public long countPendingTrackingForDashboard(List<Long> filterDeptIds) {
        LambdaQueryWrapper<ReferralTracking> wrapper = new LambdaQueryWrapper<>();
        applyBizModeFilter(wrapper, "track");
        applyUserScopeFilter(wrapper, "track", false);
        applyDashboardDepartmentFilter(wrapper, filterDeptIds);
        wrapper.eq(ReferralTracking::getTrackingStatus, 0)
                .eq(ReferralTracking::getArchived, 0);
        return count(wrapper);
    }

    @Override
    public Map<String, Object> getTrackDashboardStats(Integer statYear, List<Long> filterDeptIds) {
        int year = statYear != null ? statYear : StatYearPeriod.current().statYear();
        StatYearPeriod period = StatYearPeriod.of(year);
        long trackingCount = count(buildTrackDashboardWrapper(period, filterDeptIds));
        LambdaQueryWrapper<ReferralTracking> arrivedWrapper = buildTrackDashboardWrapper(period, filterDeptIds);
        arrivedWrapper.isNotNull(ReferralTracking::getArrivalTime);
        long trackingArrivedCount = count(arrivedWrapper);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("trackingStatYear", period.statYear());
        stats.put("trackingPeriodFrom", period.start().toString());
        stats.put("trackingPeriodTo", period.end().toString());
        stats.put("trackingCount", trackingCount);
        stats.put("trackingArrivedCount", trackingArrivedCount);
        stats.put("trackingArrivalRate", trackingCount > 0
                ? Math.round(trackingArrivedCount * 1000.0 / trackingCount) / 10.0
                : 0.0);
        return stats;
    }

    /** 首页追踪统计：与追踪管理列表一致的业务范围、数据权限及创建时间周期 */
    private LambdaQueryWrapper<ReferralTracking> buildTrackDashboardWrapper(StatYearPeriod period,
                                                                            List<Long> filterDeptIds) {
        LambdaQueryWrapper<ReferralTracking> wrapper = new LambdaQueryWrapper<>();
        applyBizModeFilter(wrapper, "track");
        applyUserScopeFilter(wrapper, "track", false);
        applyDashboardDepartmentFilter(wrapper, filterDeptIds);
        wrapper.ge(ReferralTracking::getCreateTime, period.start().atStartOfDay())
                .le(ReferralTracking::getCreateTime, period.end().atTime(23, 59, 59));
        return wrapper;
    }

    private void applyDashboardDepartmentFilter(LambdaQueryWrapper<ReferralTracking> wrapper,
                                                List<Long> filterDeptIds) {
        if (filterDeptIds == null || filterDeptIds.isEmpty()) {
            return;
        }
        wrapper.and(w -> w.in(ReferralTracking::getDepartmentId, filterDeptIds)
                .or()
                .in(ReferralTracking::getReceiverDeptId, filterDeptIds));
    }

    private void applyStatYearTimeFilter(LambdaQueryWrapper<ReferralTracking> wrapper,
                                         com.baomidou.mybatisplus.core.toolkit.support.SFunction<ReferralTracking, LocalDateTime> column,
                                         Integer statYear) {
        if (statYear == null) {
            return;
        }
        StatYearPeriod period = StatYearPeriod.of(statYear);
        wrapper.ge(column, period.start().atStartOfDay())
                .le(column, period.end().atTime(23, 59, 59));
    }
}
