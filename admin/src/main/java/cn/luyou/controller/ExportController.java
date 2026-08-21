package cn.luyou.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.annotation.OperationLog;
import cn.luyou.mapper.FirstVisitMapper;
import cn.luyou.mapper.FollowUpVisitMapper;
import cn.luyou.mapper.MedicationManagementMapper;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.FirstVisit;
import cn.luyou.model.FollowUpVisit;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.MedicationManagement;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.SupervisionForm;
import cn.luyou.model.User;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.ScreeningKeyPopulationService;
import cn.luyou.service.ScreeningSchoolService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.DepartmentFilterSupport;
import cn.luyou.utils.IdentityFormatFilterSupport;
import cn.luyou.utils.KeyPopulationCrowdCategoryQuerySupport;
import cn.luyou.utils.LatentScreeningLinkSupport;
import cn.luyou.utils.NoticePartyFillSupport;
import cn.luyou.utils.QueryDateRangeUtil;
import cn.luyou.utils.ScreeningScopeHelper;
import cn.luyou.utils.ScreeningMethodSupport;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Tag(name = "数据导出")
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ScreeningSchoolService screeningSchoolService;
    private final ScreeningKeyPopulationService keyPopulationService;
    private final ScreeningCloseContactService closeContactService;
    private final LatentInfectionService latentInfectionService;
    private final PatientService patientService;
    private final SupervisionFormService supervisionFormService;
    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;
    private final FirstVisitMapper firstVisitMapper;
    private final FollowUpVisitMapper followUpVisitMapper;
    private final MedicationManagementMapper medicationManagementMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final DataScopeHelper dataScopeHelper;
    private final ScreeningScopeHelper screeningScopeHelper;
    private final DepartmentFilterSupport departmentFilterSupport;
    private final NoticePartyFillSupport noticePartyFillSupport;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 数据来源（populationType）→ 中文标签（与前端 POPULATION_TYPE_LABEL_MAP 保持一致） */
    private static final Map<String, String> POP_TYPE_LABEL = new HashMap<>();
    static {
        POP_TYPE_LABEL.put("school",         "学生筛查");
        POP_TYPE_LABEL.put("keyPopulation",  "重点人群");
        POP_TYPE_LABEL.put("regular",        "疫情筛查");
        POP_TYPE_LABEL.put("epidemic",       "大疫情");
        POP_TYPE_LABEL.put("referral",       "推介");
        POP_TYPE_LABEL.put("closeContact",   "密接");
        POP_TYPE_LABEL.put("specialDisease", "专病网");
        POP_TYPE_LABEL.put("other",          "其它");
    }

    private static String formatLatentPopulationLabel(String populationType, String crowdCategory) {
        String base = POP_TYPE_LABEL.getOrDefault(populationType, populationType);
        if (StrUtil.isBlank(crowdCategory)) {
            return base;
        }
        if ("keyPopulation".equals(populationType)) {
            return Arrays.stream(crowdCategory.split("[、,，/]"))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(part -> base + "-" + part)
                    .collect(Collectors.joining("、"));
        }
        if ("closeContact".equals(populationType)) {
            String type = crowdCategory.trim();
            if ("家庭内".equals(type) || "家庭外".equals(type)) {
                return base + "-" + type;
            }
        }
        return base;
    }

    /** 在管患者总表导出列（无数据时也输出表头） */
    private static final List<String> ALL_PATIENT_EXPORT_HEADERS = List.of(
            "序号", "数据来源", "登记号", "姓名", "性别", "出生日期", "年龄", "证件类型", "证件号",
            "民族", "联系电话", "户籍地址", "现住址", "服药管理单位", "病原学结果", "诊断结果",
            "通知单状态",
            "首次随访", "后续随访次数", "服药管理", "停止治疗原因", "是否归档", "归档备注", "归档时间", "创建时间"
    );

    /** 潜伏感染者信息总表导出列（无数据时也输出表头） */
    private static final List<String> ALL_LATENT_EXPORT_HEADERS = List.of(
            "序号", "数据来源", "登记号", "姓名", "性别", "年龄", "证件号", "联系电话", "联系电话与联系人关系",
            "户籍地址", "现住地址", "感染筛查日期", "感染筛查方法", "感染筛查结果", "追踪状态", "未到位次数",
            "首次诊断结果", "最终诊断结果", "是否胸片检查", "胸片检查日期", "胸片检查结果",
            "追踪情况", "备注", "通知单状态", "督导表状态", "预防性治疗方案",
            "预防性治疗开始时间", "预防性治疗完成时间", "预防性治疗结果", "治疗完成情况", "治疗阶段",
            "是否归档", "创建时间"
    );

    /** 大汇总表：三类人群筛查数据合并导出 */
    @Operation(summary = "大汇总表导出")
    @GetMapping("/wide-table")
    @OperationLog(type = "export", module = "statistics", action = "导出大汇总表")
    public void exportWideTable(
            @RequestParam(defaultValue = "") String year,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<Map<String, Object>> rows = new ArrayList<>();

        // 学校人群
        screeningSchoolService.list(buildScopedSchoolYearWrapper(year, filterDeptIds)).forEach(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("人群类型", "学校人群");
            row.put("年份", s.getYear());
            row.put("姓名", s.getName());
            row.put("性别", s.getGender());
            row.put("年龄", s.getAge());
            row.put("证件号", s.getIdNumber());
            row.put("联系电话", s.getPhone());
            row.put("学校名称", s.getSchoolName());
            row.put("感染筛查结果", s.getInfectionResult());
            row.put("胸片结果", s.getChestXrayResult());
            row.put("诊断结果", s.getDiagnosisFirst());
            rows.add(row);
        });

        // 重点人群
        keyPopulationService.list(buildScopedKeyPopulationYearWrapper(year, filterDeptIds)).forEach(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("人群类型", "重点人群");
            row.put("年份", s.getYear());
            row.put("姓名", s.getName());
            row.put("性别", s.getGender());
            row.put("年龄", s.getAge());
            row.put("证件号", s.getIdNumber());
            row.put("联系电话", s.getPhone());
            row.put("学校名称", "");
            row.put("感染筛查结果", s.getInfectionResult());
            row.put("胸片结果", s.getChestXrayResult());
            row.put("诊断结果", s.getDiagnosisFirst());
            rows.add(row);
        });

        // 密接人群
        closeContactService.list(buildScopedCloseContactYearWrapper(year, filterDeptIds)).forEach(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("人群类型", "密接人群");
            row.put("年份", s.getYear());
            row.put("姓名", s.getName());
            row.put("性别", s.getGender());
            row.put("年龄", s.getAge());
            row.put("证件号", s.getIdNumber());
            row.put("联系电话", s.getPhone());
            row.put("学校名称", "");
            row.put("感染筛查结果", s.getInfectionCheckResult());
            row.put("胸片结果", s.getImagingResult());
            row.put("诊断结果", s.getFinalScreeningResult());
            rows.add(row);
        });

        writeExcel(response, "大汇总表", rows);
    }

    /** 分类汇总表：按人群类型分别导出 */
    @Operation(summary = "分类汇总表导出")
    @GetMapping("/category-table")
    @OperationLog(type = "export", module = "statistics", action = "导出分类汇总表")
    public void exportCategoryTable(
            @RequestParam String populationType,
            @RequestParam(defaultValue = "") String year,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<Map<String, Object>> rows = new ArrayList<>();
        String fileName = "分类汇总表";

        switch (populationType) {
            case "school" -> {
                fileName = "学校人群汇总表";
                screeningSchoolService.list(buildScopedSchoolYearWrapper(year, filterDeptIds)).forEach(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("年份", s.getYear()); row.put("市州", s.getCity()); row.put("县区", s.getDistrict());
                    row.put("姓名", s.getName()); row.put("性别", s.getGender()); row.put("年龄", s.getAge());
                    row.put("证件号", s.getIdNumber()); row.put("电话", s.getPhone());
                    row.put("学校类型", s.getSchoolType()); row.put("学校名称", s.getSchoolName());
                    row.put("感染筛查结果", s.getInfectionResult()); row.put("胸片结果", s.getChestXrayResult());
                    row.put("诊断结果", s.getDiagnosisFirst());
                    rows.add(row);
                });
            }
            case "keyPopulation" -> {
                fileName = "重点人群汇总表";
                keyPopulationService.list(buildScopedKeyPopulationYearWrapper(year, filterDeptIds)).forEach(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("年份", s.getYear()); row.put("市州", s.getCity()); row.put("县区", s.getDistrict());
                    row.put("姓名", s.getName()); row.put("性别", s.getGender()); row.put("年龄", s.getAge());
                    row.put("证件号", s.getIdNumber()); row.put("电话", s.getPhone());
                    // 重点人群分类由多个独立字段组成，拼接非空项
                    String crowdCat = buildCrowdCategory(s);
                    row.put("人群分类", crowdCat); row.put("感染筛查结果", s.getInfectionResult());
                    row.put("胸片结果", s.getChestXrayResult()); row.put("诊断结果", s.getDiagnosisFirst());
                    rows.add(row);
                });
            }
            case "closeContact" -> {
                fileName = "密接人群汇总表";
                closeContactService.list(buildScopedCloseContactYearWrapper(year, filterDeptIds)).forEach(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("年份", s.getYear()); row.put("市州", s.getCity()); row.put("县区", s.getDistrict());
                    row.put("姓名", s.getName()); row.put("性别", s.getGender()); row.put("年龄", s.getAge());
                    row.put("证件号", s.getIdNumber()); row.put("电话", s.getPhone());
                    row.put("接触类型", s.getContactType());
                    row.put("感染筛查结果", s.getInfectionCheckResult());
                    row.put("胸片结果", s.getImagingResult());
                    row.put("诊断结果", s.getFinalScreeningResult());
                    rows.add(row);
                });
            }
        }

        writeExcel(response, fileName, rows);
    }

    /** 自定义字段导出 */
    @Operation(summary = "自定义字段导出")
    @GetMapping("/custom")
    @OperationLog(type = "export", module = "statistics", action = "自定义字段导出")
    public void exportCustom(
            @RequestParam String populationType,
            @RequestParam String fields, // 逗号分隔的字段名列表
            @RequestParam(defaultValue = "") String year,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<String> fieldList = Arrays.asList(fields.split(","));
        List<Map<String, Object>> allRows = new ArrayList<>();

        // 获取该人群类型的完整数据
        switch (populationType) {
            case "school" -> screeningSchoolService.list(buildScopedSchoolYearWrapper(year, filterDeptIds))
                    .forEach(s -> allRows.add(toMap(s)));
            case "keyPopulation" -> keyPopulationService.list(buildScopedKeyPopulationYearWrapper(year, filterDeptIds))
                    .forEach(s -> allRows.add(toMap(s)));
            case "closeContact" -> closeContactService.list(buildScopedCloseContactYearWrapper(year, filterDeptIds))
                    .forEach(s -> allRows.add(toMap(s)));
        }

        // 按用户选择的字段过滤
        List<Map<String, Object>> filteredRows = allRows.stream().map(row -> {
            Map<String, Object> filtered = new LinkedHashMap<>();
            fieldList.forEach(f -> filtered.put(f, row.getOrDefault(f, "")));
            return filtered;
        }).collect(Collectors.toList());

        writeExcel(response, populationType + "_自定义导出", filteredRows);
    }

    /** 潜伏感染管理列表导出（学校/重点人群），字段与模板表头保持一致 */
    @Operation(summary = "潜伏感染管理列表导出")
    @GetMapping("/latent-list")
    @OperationLog(type = "export", module = "statistics", action = "导出潜伏感染管理列表")
    public void exportLatentList(
            @RequestParam String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer archived,
            HttpServletResponse response) throws IOException {

        log.info("[导出] 潜伏感染列表 populationType={} name={} idNumber={} archived={}", populationType, name, idNumber, archived);
        try {
            LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
            LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(dateTo);
            LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                    .eq(LatentInfection::getPopulationType, populationType)
                    .eq(LatentInfection::getReferralResult, "latent")
                    .like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                    .like(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                    .like(StrUtil.isNotBlank(phone), LatentInfection::getPhone, phone)
                    .ge(createFrom != null, LatentInfection::getCreateTime, createFrom)
                    .le(createTo != null, LatentInfection::getCreateTime, createTo)
                    .eq(archived != null, LatentInfection::getArchived, archived)
                    .orderByDesc(LatentInfection::getCreateTime);
            dataScopeHelper.applyLatentScope(wrapper);

            List<LatentInfection> latentList = latentInfectionService.list(wrapper);

            // 批量查询筛查原表（减少 N+1 查询）
            List<Long> screeningIds = latentList.stream()
                    .filter(l -> l.getScreeningId() != null)
                    .map(LatentInfection::getScreeningId)
                    .distinct().collect(Collectors.toList());

            // 批量查询督导表（取每条潜伏记录最新的已归档督导表）
            List<Long> latentIds = latentList.stream().map(LatentInfection::getId).collect(Collectors.toList());
            Map<Long, SupervisionForm> supervisionMap = new HashMap<>();
            if (!latentIds.isEmpty()) {
                supervisionFormService.lambdaQuery()
                        .in(SupervisionForm::getLatentInfectionId, latentIds)
                        .eq(SupervisionForm::getStatus, 2)
                        .orderByDesc(SupervisionForm::getId)
                        .list()
                        .forEach(sv -> supervisionMap.putIfAbsent(sv.getLatentInfectionId(), sv));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            String popLabel = "school".equals(populationType) ? "学校人群" : "重点人群";

            if ("school".equals(populationType)) {
                Map<Long, ScreeningSchool> screeningMap = screeningIds.isEmpty() ? new HashMap<>() :
                        screeningSchoolService.listByIds(screeningIds).stream()
                                .collect(Collectors.toMap(ScreeningSchool::getId, s -> s));
                int seq = 1;
                for (LatentInfection r : latentList) {
                    ScreeningSchool s = r.getScreeningId() != null ? screeningMap.get(r.getScreeningId()) : null;
                    SupervisionForm sv = supervisionMap.get(r.getId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("序号", seq++);
                    row.put("年份", s != null ? s.getYear() : "");
                    row.put("市（州）", s != null ? s.getCity() : "");
                    row.put("县（市、区）", s != null ? s.getDistrict() : "");
                    row.put("姓名", r.getName());
                    row.put("性别", r.getGender());
                    row.put("出生日期", formatDate(s != null ? s.getBirthDate() : null));
                    row.put("年龄", r.getAge());
                    row.put("证件类型", s != null ? s.getIdType() : "");
                    row.put("证件号", r.getIdNumber());
                    row.put("民族", s != null ? s.getEthnicity() : "");
                    row.put("联系电话", r.getPhone());
                    row.put("户籍所在地（XX市XX县、区）", s != null ? s.getHouseholdAddress() : "");
                    row.put("现地址", s != null ? s.getCurrentAddress() : "");
                    row.put("学校类型", s != null ? s.getSchoolType() : "");
                    row.put("学校名称", s != null ? s.getSchoolName() : "");
                    row.put("班级（院系）", s != null ? s.getClassName() : "");
                    row.put("既往结核病史", s != null ? s.getTbHistory() : "");
                    row.put("密切接触史", s != null ? s.getCloseContactHistory() : "");
                    row.put("结核病可疑症状", s != null ? s.getSuspiciousSymptoms() : "");
                    row.put("是否进行感染筛", s != null ? s.getHasInfectionScreen() : "");
                    row.put("感染筛查日期", formatDate(s != null ? s.getScreenDate() : null));
                    row.put("方法", s != null ? s.getScreenMethod() : "");
                    row.put("结果（PPD：mmXmm；EC及IGRA：阳性/阴性）", s != null ? s.getScreenResult() : "");
                    row.put("感染筛查结果", s != null ? s.getInfectionResult() : r.getInfectionResult());
                    row.put("是否进行胸片检查", r.getHasChestXray());
                    row.put("胸片检查日期", formatDate(r.getChestXrayDate()));
                    row.put("胸片结果", r.getChestXrayResult());
                    row.put("诊断结果-首次", r.getDiagnosisFirst());
                    row.put("诊断结果-半年后", s != null ? s.getDiagnosisHalfYear() : "");
                    row.put("诊断结果-一年后", s != null ? s.getDiagnosisOneYear() : "");
                    row.put("是否进行预防性治疗", sv != null ? "是" : "");
                    row.put("预防性治疗方案", sv != null ? sv.getTreatmentPlan() : "");
                    row.put("预防性治疗开始时间", formatDate(sv != null ? sv.getTreatmentStartDate() : null));
                    row.put("预防性治疗完成时间", formatDate(sv != null ? sv.getTreatmentEndDate() : null));
                    row.put("预防性治疗结果", sv != null ? sv.getPreventiveResult() : "");
                    row.put("预防性治疗期间随访管理人员", sv != null ? sv.getPreventiveManager() : "");
                    rows.add(row);
                }
            } else {
                Map<Long, ScreeningKeyPopulation> screeningMap = screeningIds.isEmpty() ? new HashMap<>() :
                        keyPopulationService.listByIds(screeningIds).stream()
                                .collect(Collectors.toMap(ScreeningKeyPopulation::getId, s -> s));
                int seq = 1;
                for (LatentInfection r : latentList) {
                    ScreeningKeyPopulation s = r.getScreeningId() != null ? screeningMap.get(r.getScreeningId()) : null;
                    SupervisionForm sv = supervisionMap.get(r.getId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("序号", seq++);
                    row.put("年份", s != null ? s.getYear() : "");
                    row.put("市（州）", s != null ? s.getCity() : "");
                    row.put("县（市、区）", s != null ? s.getDistrict() : "");
                    row.put("姓名", r.getName());
                    row.put("性别", r.getGender());
                    row.put("出生日期", formatDate(s != null ? s.getBirthDate() : null));
                    row.put("年龄", r.getAge());
                    row.put("证件类型", s != null ? s.getIdType() : "");
                    row.put("证件号", r.getIdNumber());
                    row.put("民族", s != null ? s.getEthnicity() : "");
                    row.put("联系电话", r.getPhone());
                    row.put("户籍所在地（XX市XX县、区）", s != null ? s.getHouseholdAddress() : "");
                    row.put("乡镇（社区）", s != null ? s.getTownshipCommunity() : "");
                    row.put("现住址", s != null ? s.getCurrentAddress() : "");
                    row.put("密接", s != null ? s.getCrowdCategoryClose() : "");
                    row.put("学生", s != null ? s.getCrowdCategoryStudent() : "");
                    row.put("教职工", s != null ? s.getCrowdCategoryTeacher() : "");
                    row.put("老年人", s != null ? s.getCrowdCategoryElder() : "");
                    row.put("糖尿病", s != null ? s.getCrowdCategoryDiabetes() : "");
                    row.put("双感", s != null ? s.getCrowdCategoryDual() : "");
                    row.put("既往结核史", s != null ? s.getCrowdCategoryTbHist() : "");
                    row.put("非重点人群", s != null ? s.getCrowdCategoryNormal() : "");
                    row.put("是否有可疑症状", s != null ? s.getHasSuspiciousSymptoms() : "");
                    row.put("咳嗽咳痰", s != null ? s.getCough() : "");
                    row.put("咯血或血痰", s != null ? s.getHemoptysis() : "");
                    row.put("发热", s != null ? s.getFever() : "");
                    row.put("胸痛", s != null ? s.getChestPain() : "");
                    row.put("夜间盗汗", s != null ? s.getNightSweats() : "");
                    row.put("食欲不振", s != null ? s.getAppetiteLoss() : "");
                    row.put("乏力", s != null ? s.getFatigue() : "");
                    row.put("体重减轻", s != null ? s.getWeightLoss() : "");
                    row.put("是否进行感染筛", s != null ? s.getHasInfectionScreen() : "");
                    row.put("感染筛查日期", formatDate(s != null ? s.getScreenDate() : null));
                    row.put("感染筛查方法", s != null ? s.getScreenMethod() : "");
                    row.put("结果（PPD：mmXmm；EC及IGRA：阳性/阴性）", s != null ? s.getScreenResult() : "");
                    row.put("感染筛查结果", s != null ? s.getInfectionResult() : r.getInfectionResult());
                    row.put("是否进行胸片检查", r.getHasChestXray());
                    row.put("胸片检查日期", formatDate(r.getChestXrayDate()));
                    row.put("胸片结果", r.getChestXrayResult());
                    row.put("诊断结果-首次", r.getDiagnosisFirst());
                    row.put("诊断结果-半年后", s != null ? s.getDiagnosisHalfYear() : "");
                    row.put("诊断结果-一年后", s != null ? s.getDiagnosisOneYear() : "");
                    row.put("是否进行预防性治疗", sv != null ? "是" : "");
                    row.put("预防性治疗方案", sv != null ? sv.getTreatmentPlan() : "");
                    row.put("预防性治疗开始时间", formatDate(sv != null ? sv.getTreatmentStartDate() : null));
                    row.put("预防性治疗完成时间", formatDate(sv != null ? sv.getTreatmentEndDate() : null));
                    row.put("预防性治疗结果", sv != null ? sv.getPreventiveResult() : "");
                    row.put("预防性治疗期间随访管理人员", sv != null ? sv.getPreventiveManager() : "");
                    rows.add(row);
                }
            }

            log.info("[导出] 潜伏感染列表查询到 {} 条记录", rows.size());
            writeExcel(response, popLabel + "_潜伏感染管理", rows);
        } catch (Exception e) {
            log.error("[导出] 潜伏感染列表导出失败 populationType={}", populationType, e);
            throw e;
        }
    }

    /**
     * 待诊断列表导出（与待诊断页 latent/list + referralResult=pending 口径一致）。
     * 含「纳入待诊断原因」，便于与筛查页「诊断结果=疑似结核」对账。
     */
    @Operation(summary = "待诊断列表导出")
    @GetMapping("/suspected-list")
    @OperationLog(type = "export", module = "statistics", action = "导出待诊断列表")
    public void exportSuspectedList(
            @RequestParam String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String diagnosisFirst,
            @RequestParam(required = false) String referralResult,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {

        String resolvedReferral = referralResult;
        if (StrUtil.isBlank(resolvedReferral) && (archived == null || Integer.valueOf(0).equals(archived))) {
            resolvedReferral = "pending";
        }
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<LatentInfection> records = new ArrayList<>();
        int pageNum = 1;
        final int pageSize = 2000;
        while (true) {
            var page = latentInfectionService.queryPage(
                    pageNum, pageSize, populationType, name, idNumber, trackingStatus, archived,
                    resolvedReferral, diagnosisFirst, phone, dateFrom, dateTo, null,
                    null, null, filterDeptIds, columnFilters, null);
            if (page.getRecords() == null || page.getRecords().isEmpty()) {
                break;
            }
            records.addAll(page.getRecords());
            if ((long) pageNum * pageSize >= page.getTotal()) {
                break;
            }
            pageNum++;
        }

        String popLabel = switch (populationType) {
            case "school" -> "学校人群";
            case "regular" -> "疫情筛查";
            default -> "重点人群";
        };
        List<Map<String, Object>> rows = new ArrayList<>();
        int seq = 1;
        for (LatentInfection r : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("序号", seq++);
            row.put("姓名", r.getName());
            row.put("性别", r.getGender());
            row.put("年龄", r.getAge());
            row.put("证件号", r.getIdNumber());
            row.put("联系电话", r.getPhone());
            row.put("感染筛查结果", r.getInfectionResult());
            row.put("胸片结果", r.getChestXrayResult());
            row.put("确认诊断", StrUtil.blankToDefault(r.getDiagnosisFirst(), r.getScreeningDiagnosisFirst()));
            row.put("纳入待诊断原因", r.getPendingEntryReason());
            row.put("追踪状态", r.getTrackingStatus());
            row.put("未到位次数", r.getNotInPlaceCount());
            row.put("追踪备注", r.getTrackingRemark());
            row.put("归档", Integer.valueOf(1).equals(r.getArchived()) ? "已归档" : "进行中");
            rows.add(row);
        }
        log.info("[导出] 待诊断列表 populationType={} referral={} count={}", populationType, resolvedReferral, rows.size());
        writeExcel(response, popLabel + "_待诊断", rows);
    }

    /** 患者管理列表导出（学校/重点人群），字段与模板表头保持一致 */
    @Operation(summary = "患者管理列表导出")
    @GetMapping("/patient-list")
    @OperationLog(type = "export", module = "statistics", action = "导出患者管理列表")
    public void exportPatientList(
            @RequestParam String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            HttpServletResponse response) throws IOException {

        log.info("[导出] 患者列表 populationType={} name={} idNumber={}", populationType, name, idNumber);
        try {
            List<Patient> patientList = patientService.listForExport(
                    populationType, name, idNumber, phone, null, null, 0, dateFrom, dateTo, null, null, null, null, null);

            // 批量查询筛查原表
            List<Long> screeningIds = patientList.stream()
                    .filter(p -> p.getScreeningId() != null)
                    .map(Patient::getScreeningId)
                    .distinct().collect(Collectors.toList());

            List<Map<String, Object>> rows = new ArrayList<>();
            String popLabel = "school".equals(populationType) ? "学校人群" : "重点人群";

            if ("school".equals(populationType)) {
                Map<Long, ScreeningSchool> screeningMap = screeningIds.isEmpty() ? new HashMap<>() :
                        screeningSchoolService.listByIds(screeningIds).stream()
                                .collect(Collectors.toMap(ScreeningSchool::getId, s -> s));
                int seq = 1;
                for (Patient p : patientList) {
                    ScreeningSchool s = p.getScreeningId() != null ? screeningMap.get(p.getScreeningId()) : null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("序号", seq++);
                    row.put("年份", s != null ? s.getYear() : "");
                    row.put("市（州）", s != null ? s.getCity() : "");
                    row.put("县（市、区）", s != null ? s.getDistrict() : "");
                    row.put("姓名", p.getName());
                    row.put("性别", p.getGender());
                    row.put("出生日期", formatDate(p.getBirthDate() != null ? p.getBirthDate() : (s != null ? s.getBirthDate() : null)));
                    row.put("年龄", p.getAge());
                    row.put("证件类型", p.getIdType() != null ? p.getIdType() : (s != null ? s.getIdType() : ""));
                    row.put("证件号", p.getIdNumber());
                    row.put("民族", p.getEthnicity() != null ? p.getEthnicity() : (s != null ? s.getEthnicity() : ""));
                    row.put("联系电话", p.getPhone());
                    row.put("户籍所在地（XX市XX县、区）", p.getHouseholdAddress() != null ? p.getHouseholdAddress() : (s != null ? s.getHouseholdAddress() : ""));
                    row.put("现地址", p.getCurrentAddress() != null ? p.getCurrentAddress() : (s != null ? s.getCurrentAddress() : ""));
                    row.put("学校类型", s != null ? s.getSchoolType() : "");
                    row.put("学校名称", s != null ? s.getSchoolName() : "");
                    row.put("班级（院系）", s != null ? s.getClassName() : "");
                    row.put("既往结核病史", s != null ? s.getTbHistory() : "");
                    row.put("密切接触史", s != null ? s.getCloseContactHistory() : "");
                    row.put("结核病可疑症状", s != null ? s.getSuspiciousSymptoms() : "");
                    row.put("是否进行感染筛", s != null ? s.getHasInfectionScreen() : "");
                    row.put("感染筛查日期", formatDate(s != null ? s.getScreenDate() : null));
                    row.put("方法", s != null ? s.getScreenMethod() : "");
                    row.put("结果（PPD：mmXmm；EC及IGRA：阳性/阴性）", s != null ? s.getScreenResult() : "");
                    row.put("感染筛查结果", s != null ? s.getInfectionResult() : "");
                    row.put("是否进行胸片检查", s != null ? s.getHasChestXray() : "");
                    row.put("胸片检查日期", formatDate(s != null ? s.getChestXrayDate() : null));
                    row.put("胸片结果", s != null ? s.getChestXrayResult() : "");
                    row.put("诊断结果-首次", s != null ? s.getDiagnosisFirst() : "");
                    row.put("诊断结果-半年后", s != null ? s.getDiagnosisHalfYear() : "");
                    row.put("诊断结果-一年后", s != null ? s.getDiagnosisOneYear() : "");
                    row.put("最终诊断结果", p.getDiagnosisResult());
                    row.put("患者来源", "confirmed".equals(p.getSource()) ? "诊断确诊" : "大疫情导入");
                    row.put("是否归档", Integer.valueOf(1).equals(p.getArchived()) ? "已归档" : "未归档");
                    rows.add(row);
                }
            } else {
                Map<Long, ScreeningKeyPopulation> screeningMap = screeningIds.isEmpty() ? new HashMap<>() :
                        keyPopulationService.listByIds(screeningIds).stream()
                                .collect(Collectors.toMap(ScreeningKeyPopulation::getId, s -> s));
                int seq = 1;
                for (Patient p : patientList) {
                    ScreeningKeyPopulation s = p.getScreeningId() != null ? screeningMap.get(p.getScreeningId()) : null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("序号", seq++);
                    row.put("年份", s != null ? s.getYear() : "");
                    row.put("市（州）", s != null ? s.getCity() : "");
                    row.put("县（市、区）", s != null ? s.getDistrict() : "");
                    row.put("姓名", p.getName());
                    row.put("性别", p.getGender());
                    row.put("出生日期", formatDate(p.getBirthDate() != null ? p.getBirthDate() : (s != null ? s.getBirthDate() : null)));
                    row.put("年龄", p.getAge());
                    row.put("证件类型", p.getIdType() != null ? p.getIdType() : (s != null ? s.getIdType() : ""));
                    row.put("证件号", p.getIdNumber());
                    row.put("民族", p.getEthnicity() != null ? p.getEthnicity() : (s != null ? s.getEthnicity() : ""));
                    row.put("联系电话", p.getPhone());
                    row.put("户籍所在地（XX市XX县、区）", p.getHouseholdAddress() != null ? p.getHouseholdAddress() : (s != null ? s.getHouseholdAddress() : ""));
                    row.put("乡镇（社区）", s != null ? s.getTownshipCommunity() : "");
                    row.put("现住址", p.getCurrentAddress() != null ? p.getCurrentAddress() : (s != null ? s.getCurrentAddress() : ""));
                    row.put("密接", s != null ? s.getCrowdCategoryClose() : "");
                    row.put("学生", s != null ? s.getCrowdCategoryStudent() : "");
                    row.put("教职工", s != null ? s.getCrowdCategoryTeacher() : "");
                    row.put("老年人", s != null ? s.getCrowdCategoryElder() : "");
                    row.put("糖尿病", s != null ? s.getCrowdCategoryDiabetes() : "");
                    row.put("双感", s != null ? s.getCrowdCategoryDual() : "");
                    row.put("既往结核史", s != null ? s.getCrowdCategoryTbHist() : "");
                    row.put("非重点人群", s != null ? s.getCrowdCategoryNormal() : "");
                    row.put("是否有可疑症状", s != null ? s.getHasSuspiciousSymptoms() : "");
                    row.put("咳嗽咳痰", s != null ? s.getCough() : "");
                    row.put("咯血或血痰", s != null ? s.getHemoptysis() : "");
                    row.put("发热", s != null ? s.getFever() : "");
                    row.put("胸痛", s != null ? s.getChestPain() : "");
                    row.put("夜间盗汗", s != null ? s.getNightSweats() : "");
                    row.put("食欲不振", s != null ? s.getAppetiteLoss() : "");
                    row.put("乏力", s != null ? s.getFatigue() : "");
                    row.put("体重减轻", s != null ? s.getWeightLoss() : "");
                    row.put("是否进行感染筛", s != null ? s.getHasInfectionScreen() : "");
                    row.put("感染筛查日期", formatDate(s != null ? s.getScreenDate() : null));
                    row.put("感染筛查方法", s != null ? s.getScreenMethod() : "");
                    row.put("结果（PPD：mmXmm；EC及IGRA：阳性/阴性）", s != null ? s.getScreenResult() : "");
                    row.put("感染筛查结果", s != null ? s.getInfectionResult() : "");
                    row.put("是否进行胸片检查", s != null ? s.getHasChestXray() : "");
                    row.put("胸片检查日期", formatDate(s != null ? s.getChestXrayDate() : null));
                    row.put("胸片结果", s != null ? s.getChestXrayResult() : "");
                    row.put("诊断结果-首次", s != null ? s.getDiagnosisFirst() : "");
                    row.put("诊断结果-半年后", s != null ? s.getDiagnosisHalfYear() : "");
                    row.put("诊断结果-一年后", s != null ? s.getDiagnosisOneYear() : "");
                    row.put("最终诊断结果", p.getDiagnosisResult());
                    row.put("患者来源", "confirmed".equals(p.getSource()) ? "诊断确诊" : "大疫情导入");
                    row.put("是否归档", Integer.valueOf(1).equals(p.getArchived()) ? "已归档" : "未归档");
                    rows.add(row);
                }
            }

            log.info("[导出] 患者列表查询到 {} 条记录", rows.size());
            writeExcel(response, popLabel + "_患者管理", rows);
        } catch (Exception e) {
            log.error("[导出] 患者列表导出失败 populationType={}", populationType, e);
            throw e;
        }
    }

    // ==================== P6 新增：全来源患者/潜伏总表导出 ====================

    /**
     * 患者信息总表（全部来源，含来源标签）。
     * populationType 不传则导出全部来源（school/keyPopulation/regular/epidemic/referral/closeContact）。
     */
    @Operation(summary = "患者信息总表导出（全部来源，含来源标签）")
    @GetMapping("/all-patients")
    @OperationLog(type = "export", module = "statistics", action = "导出患者信息总表")
    public void exportAllPatients(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String currentAddress,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String medicationManagementUnit,
            @RequestParam(required = false) String stopTreatmentReason,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String departmentIds,
            @RequestParam(required = false) String formatIssue,
            HttpServletResponse response) throws IOException {

        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<Long> idList = parseIdList(ids);
        List<Patient> patientList;
        if (!idList.isEmpty()) {
            patientList = patientService.listByIdsForExport(idList, archived);
        } else {
            patientList = patientService.listForExport(
                    populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                    archived, dateFrom, dateTo, startTime, endTime, dateFilterBy, medicationManagementUnit,
                    crowdCategory, formatIssue);
        }
        if (departmentFilterSupport.hasActiveFilter(filterDeptIds)) {
            Set<Long> allowed = new HashSet<>(filterDeptIds);
            patientList = patientList.stream()
                    .filter(p -> p.getDepartmentId() != null && allowed.contains(p.getDepartmentId()))
                    .toList();
        }
        if (StrUtil.isNotBlank(stopTreatmentReason)) {
            List<Long> matchedPatientIds = patientService.findPatientIdsByPreferredStopTreatmentReason(stopTreatmentReason);
            if (matchedPatientIds.isEmpty()) {
                writeExcel(response, "患者信息总表", List.of(), ALL_PATIENT_EXPORT_HEADERS);
                return;
            }
            Set<Long> matchedSet = new HashSet<>(matchedPatientIds);
            patientList = patientList.stream().filter(p -> matchedSet.contains(p.getId())).toList();
        }
        List<Long> patientIds = patientList.stream().map(Patient::getId).collect(Collectors.toList());

        // 批量查询患者通知单（每患者取最新一条）
        Map<Long, Notice> noticeMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                            .in(Notice::getBizId, patientIds)
                            .eq(Notice::getNoticeType, "patient")
                            .orderByDesc(Notice::getId))
                    .forEach(n -> noticeMap.putIfAbsent(n.getBizId(), n));
        }

        // 批量判断是否完成首次随访
        Set<Long> firstVisitSet = new HashSet<>();
        if (!patientIds.isEmpty()) {
            firstVisitMapper.selectList(new LambdaQueryWrapper<FirstVisit>()
                            .in(FirstVisit::getPatientId, patientIds)
                            .eq(FirstVisit::getStatus, 1))
                    .forEach(fv -> firstVisitSet.add(fv.getPatientId()));
        }

        // 批量统计后续随访次数
        Map<Long, Long> followUpCountMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            followUpVisitMapper.selectList(new LambdaQueryWrapper<FollowUpVisit>()
                            .in(FollowUpVisit::getPatientId, patientIds)
                            .eq(FollowUpVisit::getStatus, 1))
                    .forEach(fv -> followUpCountMap.merge(fv.getPatientId(), 1L, Long::sum));
        }

        // 批量获取服药管理记录（每患者取最新一条）
        Map<Long, MedicationManagement> medicationMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            medicationManagementMapper.selectList(new LambdaQueryWrapper<MedicationManagement>()
                            .in(MedicationManagement::getPatientId, patientIds)
                            .orderByDesc(MedicationManagement::getId))
                    .forEach(m -> medicationMap.putIfAbsent(m.getPatientId(), m));
        }

        // 批量获取停止治疗原因（每患者取最新一条已完成且停止治疗的随访）
        Map<Long, FollowUpVisit> stopTreatmentVisitMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            followUpVisitMapper.selectList(new LambdaQueryWrapper<FollowUpVisit>()
                            .in(FollowUpVisit::getPatientId, patientIds)
                            .eq(FollowUpVisit::getStatus, 1)
                            .eq(FollowUpVisit::getStopTreatment, "是")
                            .orderByDesc(FollowUpVisit::getId))
                    .forEach(v -> stopTreatmentVisitMap.putIfAbsent(v.getPatientId(), v));
        }

        int seq = 1;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Patient p : patientList) {
            Notice notice = noticeMap.get(p.getId());
            MedicationManagement med = medicationMap.get(p.getId());
            FollowUpVisit stopVisit = stopTreatmentVisitMap.get(p.getId());
            String noticeStatusLabel = notice == null ? "未发送"
                    : (Integer.valueOf(2).equals(notice.getStatus()) ? "已确认"
                    : (Integer.valueOf(1).equals(notice.getStatus()) ? "已发送" : "草稿"));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("序号", seq++);
            row.put("数据来源", POP_TYPE_LABEL.getOrDefault(p.getPopulationType(), p.getPopulationType()));
            row.put("登记号", resolvePatientRegistrationNo(p));
            row.put("姓名", p.getName());
            row.put("性别", p.getGender());
            row.put("出生日期", formatDate(p.getBirthDate()));
            row.put("年龄", p.getAge());
            row.put("证件类型", p.getIdType());
            row.put("证件号", p.getIdNumber());
            row.put("民族", p.getEthnicity());
            row.put("联系电话", p.getPhone());
            row.put("户籍地址", p.getHouseholdAddress());
            row.put("现住址", p.getCurrentAddress());
            row.put("服药管理单位", resolvePatientMedicationUnit(p, notice));
            row.put("病原学结果", resolvePatientPathogenResultForExport(p));
            row.put("诊断结果", resolvePatientDiagnosisResultForExport(p));
            row.put("通知单状态", noticeStatusLabel);
            row.put("首次随访", firstVisitSet.contains(p.getId()) ? "已完成" : "未完成");
            row.put("后续随访次数", followUpCountMap.getOrDefault(p.getId(), 0L));
            row.put("服药管理", med != null ? "已录入" : "未录入");
            row.put("停止治疗原因", stopVisit != null
                    ? formatStopTreatmentReason(stopVisit.getStopTreatmentReason(), stopVisit.getStopTreatmentReasonOther())
                    : "");
            row.put("是否归档", Integer.valueOf(1).equals(p.getArchived()) ? "已归档" : "未归档");
            row.put("归档备注", p.getArchiveRemark() != null ? p.getArchiveRemark() : "");
            row.put("归档时间", p.getArchivedTime() != null ? p.getArchivedTime().format(DATETIME_FMT) : "");
            row.put("创建时间", p.getCreateTime() != null ? p.getCreateTime().format(DATETIME_FMT) : "");
            rows.add(row);
        }

        log.info("[导出] 患者信息总表 {} 条", rows.size());
        writeExcel(response, "患者信息总表", rows, ALL_PATIENT_EXPORT_HEADERS);
    }

    /** 首次/后续随访导出共用的患者基本列 */
    private static final List<String> PATIENT_VISIT_BASIC_HEADERS = List.of(
            "数据来源", "姓名", "性别", "证件号", "联系电话", "病原学结果", "诊断结果", "服药管理单位"
    );

    /** 首次入户随访导出列（不含附件） */
    private static final List<String> FIRST_VISIT_EXPORT_HEADERS = List.of(
            "数据来源", "姓名", "性别", "证件号", "联系电话", "病原学结果", "诊断结果", "服药管理单位",
            "编号", "随访时间", "随访方式", "患者类型", "痰菌情况", "痰培养", "痰培养补充", "耐药情况", "症状及体征", "其他症状",
            "化疗方案", "用法", "督导人员", "药品剂型", "单独居室", "通风情况", "吸烟(支/天)", "饮酒(两/天)",
            "取药地点", "取药时间", "健康教育及培训", "下次随访时间", "评估医生签名", "备注", "状态", "填写时间"
    );

    /** 后续随访导出列（不含附件；同一患者多条记录各占一行） */
    private static final List<String> FOLLOW_UP_VISIT_EXPORT_HEADERS = List.of(
            "数据来源", "姓名", "性别", "证件号", "联系电话", "病原学结果", "诊断结果", "服药管理单位",
            "第几次", "随访时间", "治疗月序", "督导人员", "随访方式", "症状及体征", "症状-其它",
            "吸烟(支/天)", "饮酒(两/天)", "化疗方案", "用法", "药品剂型", "漏服药次数",
            "药物不良反应", "不良反应详情", "并发症/合并症", "并发症详情",
            "转诊科别", "转诊原因", "2周内随访结果", "处理意见", "下次随访时间", "随访医生签名",
            "是否停止治疗", "停止治疗时间", "停止治疗原因", "应访视次数", "实际访视次数",
            "应服药次数", "实际服药次数", "服药率(%)", "评估医生签名", "备注", "状态", "填写时间"
    );

    /** 潜伏感染者督导表导出列（同一感染者多条记录各占一行） */
    private static final List<String> SUPERVISION_FORM_EXPORT_HEADERS = List.of(
            "数据来源", "姓名", "性别", "年龄", "证件号", "联系电话",
            "第几次", "类别", "管理单位", "督导医生",
            "是否开始预防性治疗", "治疗方案", "治疗开始时间", "治疗结束时间",
            "督导记录", "治疗完成情况", "中断用药", "中断次数", "用药率",
            "督导管理人员类型", "督导管理人员姓名", "备注", "状态", "填写时间"
    );

    /** 患者通知单导出列 */
    private static final List<String> PATIENT_NOTICE_EXPORT_HEADERS = List.of(
            "数据来源", "姓名", "性别", "年龄", "证件号", "联系电话", "民族", "人群分类",
            "现居住地址", "户籍地址", "病原学结果", "诊断结果",
            "患者类型", "管理方式", "胸片检查时间", "胸片检查结果", "治疗方案", "耐药情况",
            "痰涂片", "痰培养", "分子检查", "病理学检查",
            "治疗机构", "服药管理单位", "下发时间", "备注", "其他注意事项",
            "下发人", "接收人", "发送时间", "接收时间", "状态"
    );

    private static final Map<String, String> FIRST_VISIT_SYMPTOM_LABEL = Map.ofEntries(
            Map.entry("0", "没有症状"), Map.entry("1", "咳嗽咳痰"), Map.entry("2", "低热盗汗"),
            Map.entry("3", "咯血或血痰"), Map.entry("4", "胸痛消瘦"), Map.entry("5", "恶心纳差"),
            Map.entry("6", "头痛失眠"), Map.entry("7", "视物模糊"), Map.entry("8", "皮肤瘙痒、皮疹"),
            Map.entry("9", "耳鸣、听力下降")
    );

    private static final Map<String, String> FOLLOW_UP_SYMPTOM_LABEL = Map.ofEntries(
            Map.entry("0", "没有症状"), Map.entry("1", "咳嗽咳痰"), Map.entry("2", "低热盗汗"),
            Map.entry("3", "咯血或血痰"), Map.entry("4", "胸痛消瘦"), Map.entry("5", "恶心纳差"),
            Map.entry("6", "关节疼痛"), Map.entry("7", "头痛失眠"), Map.entry("8", "视物模糊"),
            Map.entry("9", "皮肤瘙痒、皮疹"), Map.entry("10", "耳鸣、听力下降"), Map.entry("11", "其它")
    );

    private static final Map<String, String> FOLLOW_UP_VISIT_METHOD_LABEL = Map.of(
            "1", "门诊", "2", "家庭", "3", "电话", "4", "其他"
    );

    private static final Map<String, String> FOLLOW_UP_SUPERVISOR_LABEL = Map.of(
            "1", "医生", "2", "家属", "3", "自服药", "4", "其他"
    );

    private static final Map<String, String> FOLLOW_UP_MEDICATION_USAGE_LABEL = Map.of(
            "1", "每日", "2", "间歇"
    );

    private static final Map<String, String> FOLLOW_UP_DRUG_FORM_LABEL = Map.of(
            "1", "固定剂量复合制剂", "2", "散装药", "3", "板式组合药", "4", "注射剂"
    );

    private static final Map<String, String> YES_NO_LABEL = Map.of("1", "无", "2", "有");

    @Operation(summary = "导出首次入户随访（ids 勾选；否则按当前筛选）")
    @GetMapping("/patient-first-visits")
    @OperationLog(type = "export", module = "statistics", action = "导出首次入户随访")
    public void exportPatientFirstVisits(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String currentAddress,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String medicationManagementUnit,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String formatIssue,
            @RequestParam(required = false) String sputumCulture,
            @RequestParam(required = false) String drugResistance,
            HttpServletResponse response) throws IOException {
        List<Patient> patients = loadPatientsForVisitExport(
                ids, archived, populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                dateFrom, dateTo, StrUtil.blankToDefault(dateFilterBy, "firstVisitFill"),
                medicationManagementUnit, crowdCategory, creatorUsername, columnFilters, formatIssue,
                sputumCulture, drugResistance);
        if (patients.isEmpty()) {
            writeExcel(response, "首次入户随访", List.of(), FIRST_VISIT_EXPORT_HEADERS);
            return;
        }
        List<Long> patientIds = patients.stream().map(Patient::getId).toList();
        Map<Long, Notice> noticeMap = loadLatestNoticeMap(patientIds, "patient");

        Map<Long, FirstVisit> visitMap = new HashMap<>();
        firstVisitMapper.selectList(new LambdaQueryWrapper<FirstVisit>()
                        .in(FirstVisit::getPatientId, patientIds)
                        .orderByDesc(FirstVisit::getId))
                .forEach(v -> visitMap.putIfAbsent(v.getPatientId(), v));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Patient p : patients) {
            FirstVisit v = visitMap.get(p.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            putPatientBasicColumns(row, p, noticeMap.get(p.getId()));
            if (v != null) {
                row.put("编号", nullToEmpty(v.getFormNo()));
                row.put("随访时间", formatDate(v.getVisitDate()));
                row.put("随访方式", formatFirstVisitMethod(v.getVisitMethod(), v.getVisitMethodOther()));
                row.put("患者类型", nullToEmpty(v.getPatientType()));
                row.put("痰菌情况", nullToEmpty(v.getSputumStatus()));
                row.put("痰培养", nullToEmpty(v.getSputumCulture()));
                row.put("痰培养补充", formatSputumCultureSupplementStatus(v.getSputumCultureSupplementStatus()));
                row.put("耐药情况", nullToEmpty(v.getDrugResistance()));
                row.put("症状及体征", formatSymptomCodes(v.getSymptoms(), FIRST_VISIT_SYMPTOM_LABEL));
                row.put("其他症状", nullToEmpty(v.getOtherSymptoms()));
                row.put("化疗方案", nullToEmpty(v.getChemotherapy()));
                row.put("用法", nullToEmpty(v.getMedicationUsage()));
                row.put("督导人员", nullToEmpty(v.getSupervisor()));
                row.put("药品剂型", nullToEmpty(v.getDrugForm()));
                row.put("单独居室", nullToEmpty(v.getSeparateRoom()));
                row.put("通风情况", nullToEmpty(v.getVentilation()));
                row.put("吸烟(支/天)", nullToEmpty(v.getSmokingAmount()));
                row.put("饮酒(两/天)", nullToEmpty(v.getDrinkingAmount()));
                row.put("取药地点", nullToEmpty(v.getMedicationLocation()));
                row.put("取药时间", nullToEmpty(v.getMedicationPickTime()));
                row.put("健康教育及培训", formatEducationItems(v.getEducationItems()));
                row.put("下次随访时间", formatDate(v.getNextVisitDate()));
                row.put("评估医生签名", nullToEmpty(v.getDoctorSignature()));
                row.put("备注", nullToEmpty(v.getRemarks()));
                row.put("状态", visitStatusLabel(v.getStatus()));
                row.put("填写时间", formatDateTime(v.getCreateTime()));
            } else {
                FIRST_VISIT_EXPORT_HEADERS.stream()
                        .filter(h -> !PATIENT_VISIT_BASIC_HEADERS.contains(h))
                        .forEach(h -> row.put(h, ""));
                row.put("状态", "待填写");
            }
            rows.add(row);
        }
        log.info("[导出] 首次入户随访 {} 条", rows.size());
        writeExcel(response, "首次入户随访", rows, FIRST_VISIT_EXPORT_HEADERS);
    }

    @Operation(summary = "导出后续随访（ids 勾选；否则按当前筛选）")
    @GetMapping("/patient-follow-up-visits")
    @OperationLog(type = "export", module = "statistics", action = "导出后续随访")
    public void exportPatientFollowUpVisits(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String currentAddress,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String medicationManagementUnit,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String formatIssue,
            HttpServletResponse response) throws IOException {
        List<Patient> patients = loadPatientsForVisitExport(
                ids, archived, populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                dateFrom, dateTo, StrUtil.blankToDefault(dateFilterBy, "followUpFill"),
                medicationManagementUnit, crowdCategory, creatorUsername, columnFilters, formatIssue,
                null, null);
        if (patients.isEmpty()) {
            writeExcel(response, "后续随访", List.of(), FOLLOW_UP_VISIT_EXPORT_HEADERS);
            return;
        }
        List<Long> patientIds = patients.stream().map(Patient::getId).toList();
        Map<Long, Notice> noticeMap = loadLatestNoticeMap(patientIds, "patient");

        Map<Long, List<FollowUpVisit>> visitMap = new HashMap<>();
        followUpVisitMapper.selectList(new LambdaQueryWrapper<FollowUpVisit>()
                        .in(FollowUpVisit::getPatientId, patientIds)
                        .orderByAsc(FollowUpVisit::getPatientId)
                        .orderByAsc(FollowUpVisit::getVisitSeq)
                        .orderByAsc(FollowUpVisit::getId))
                .forEach(v -> visitMap.computeIfAbsent(v.getPatientId(), k -> new ArrayList<>()).add(v));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Patient p : patients) {
            Notice notice = noticeMap.get(p.getId());
            List<FollowUpVisit> visits = visitMap.getOrDefault(p.getId(), List.of());
            if (visits.isEmpty()) {
                Map<String, Object> row = new LinkedHashMap<>();
                putPatientBasicColumns(row, p, notice);
                FOLLOW_UP_VISIT_EXPORT_HEADERS.stream()
                        .filter(h -> !PATIENT_VISIT_BASIC_HEADERS.contains(h))
                        .forEach(h -> row.put(h, ""));
                row.put("状态", "暂无记录");
                rows.add(row);
                continue;
            }
            for (FollowUpVisit v : visits) {
                rows.add(buildFollowUpExportRow(p, v, notice));
            }
        }
        log.info("[导出] 后续随访 {} 条", rows.size());
        writeExcel(response, "后续随访", rows, FOLLOW_UP_VISIT_EXPORT_HEADERS);
    }

    @Operation(summary = "导出患者通知单（ids 勾选；否则按当前筛选）")
    @GetMapping("/patient-notices")
    @OperationLog(type = "export", module = "statistics", action = "导出患者通知单")
    public void exportPatientNotices(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String currentAddress,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String medicationManagementUnit,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String formatIssue,
            HttpServletResponse response) throws IOException {
        List<Patient> patients = loadPatientsForVisitExport(
                ids, archived, populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                dateFrom, dateTo, StrUtil.blankToDefault(dateFilterBy, "noticeFill"),
                medicationManagementUnit, crowdCategory, creatorUsername, columnFilters, formatIssue,
                null, null);
        if (patients.isEmpty()) {
            writeExcel(response, "患者通知单", List.of(), PATIENT_NOTICE_EXPORT_HEADERS);
            return;
        }
        List<Long> patientIds = patients.stream().map(Patient::getId).toList();
        Map<Long, Notice> noticeMap = loadLatestNoticeMap(patientIds, "patient");
        noticePartyFillSupport.fillPartyNames(new ArrayList<>(noticeMap.values()));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Patient p : patients) {
            rows.add(buildPatientNoticeExportRow(p, noticeMap.get(p.getId())));
        }
        log.info("[导出] 患者通知单 {} 条", rows.size());
        writeExcel(response, "患者通知单", rows, PATIENT_NOTICE_EXPORT_HEADERS);
    }

    @Operation(summary = "导出潜伏感染者督导表（ids 勾选；否则按当前筛选）")
    @GetMapping("/latent-supervision-forms")
    @OperationLog(type = "export", module = "statistics", action = "导出督导表")
    public void exportLatentSupervisionForms(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) String referralResult,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String formatIssue,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<LatentInfection> latents = loadLatentsForSupervisionExport(
                ids, populationType, name, idNumber, phone, dateFrom, dateTo,
                StrUtil.blankToDefault(dateFilterBy, "supervisionFill"),
                creatorName, archived, trackingStatus != null ? trackingStatus : 1,
                StrUtil.blankToDefault(referralResult, "latent"),
                crowdCategory, columnFilters, formatIssue, departmentIds);
        if (latents.isEmpty()) {
            writeExcel(response, "督导表", List.of(), SUPERVISION_FORM_EXPORT_HEADERS);
            return;
        }
        List<Long> latentIds = latents.stream().map(LatentInfection::getId).toList();
        Map<Long, List<SupervisionForm>> formMap = new HashMap<>();
        supervisionFormService.lambdaQuery()
                .in(SupervisionForm::getLatentInfectionId, latentIds)
                .ne(SupervisionForm::getStatus, 0)
                .orderByAsc(SupervisionForm::getLatentInfectionId)
                .orderByAsc(SupervisionForm::getFormSeq)
                .orderByAsc(SupervisionForm::getId)
                .list()
                .forEach(f -> formMap.computeIfAbsent(f.getLatentInfectionId(), k -> new ArrayList<>()).add(f));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (LatentInfection r : latents) {
            List<SupervisionForm> forms = formMap.getOrDefault(r.getId(), List.of());
            if (forms.isEmpty()) {
                Map<String, Object> row = new LinkedHashMap<>();
                putLatentSupervisionBasicColumns(row, r);
                SUPERVISION_FORM_EXPORT_HEADERS.stream()
                        .filter(h -> !List.of("数据来源", "姓名", "性别", "年龄", "证件号", "联系电话").contains(h))
                        .forEach(h -> row.put(h, ""));
                row.put("状态", "暂无记录");
                rows.add(row);
                continue;
            }
            for (SupervisionForm f : forms) {
                rows.add(buildSupervisionExportRow(r, f));
            }
        }
        log.info("[导出] 督导表 {} 条", rows.size());
        writeExcel(response, "督导表", rows, SUPERVISION_FORM_EXPORT_HEADERS);
    }

    private List<Long> parseIdList(String ids) {
        if (StrUtil.isBlank(ids)) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                .map(Long::valueOf)
                .distinct()
                .toList();
    }

    private List<Patient> loadPatientsForVisitExport(
            String ids, Integer archived,
            String populationType, String name, String idNumber, String phone,
            String currentAddress, String diagnosisResult,
            String dateFrom, String dateTo, String dateFilterBy,
            String medicationManagementUnit, String crowdCategory,
            String creatorUsername, String columnFilters, String formatIssue,
            String sputumCulture, String drugResistance) {
        List<Long> idList = parseIdList(ids);
        if (!idList.isEmpty()) {
            idList.forEach(dataScopeHelper::assertPatientAccessible);
            Map<Long, Patient> map = patientService.listByIdsForExport(idList, archived).stream()
                    .collect(Collectors.toMap(Patient::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
            List<Patient> ordered = new ArrayList<>();
            for (Long id : idList) {
                Patient p = map.get(id);
                if (p != null) {
                    ordered.add(p);
                }
            }
            return ordered;
        }
        List<Patient> all = new ArrayList<>();
        int pageNum = 1;
        final int pageSize = 2000;
        Integer archivedVal = archived != null ? archived : 0;
        while (true) {
            var page = patientService.queryPage(
                    pageNum, pageSize, populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                    archivedVal, dateFrom, dateTo, dateFilterBy, medicationManagementUnit, crowdCategory,
                    creatorUsername, columnFilters, null, null, formatIssue, sputumCulture, drugResistance);
            if (page.getRecords() == null || page.getRecords().isEmpty()) {
                break;
            }
            all.addAll(page.getRecords());
            if ((long) pageNum * pageSize >= page.getTotal()) {
                break;
            }
            pageNum++;
        }
        return all;
    }

    private List<LatentInfection> loadLatentsForSupervisionExport(
            String ids, String populationType, String name, String idNumber, String phone,
            String dateFrom, String dateTo, String dateFilterBy, String creatorName,
            Integer archived, Integer trackingStatus, String referralResult,
            String crowdCategory, String columnFilters, String formatIssue, String departmentIds) {
        List<Long> idList = parseIdList(ids);
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        if (!idList.isEmpty()) {
            idList.forEach(dataScopeHelper::assertLatentAccessible);
            Map<Long, LatentInfection> map = latentInfectionService.listByIds(idList).stream()
                    .collect(Collectors.toMap(LatentInfection::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
            List<LatentInfection> ordered = new ArrayList<>();
            for (Long id : idList) {
                LatentInfection r = map.get(id);
                if (r != null) {
                    ordered.add(r);
                }
            }
            return ordered;
        }
        List<LatentInfection> all = new ArrayList<>();
        int pageNum = 1;
        final int pageSize = 2000;
        while (true) {
            var page = latentInfectionService.queryPage(
                    pageNum, pageSize, populationType, name, idNumber, trackingStatus,
                    archived != null ? archived : 0, referralResult, null, phone, dateFrom, dateTo,
                    dateFilterBy, creatorName, crowdCategory, filterDeptIds, columnFilters, formatIssue);
            if (page.getRecords() == null || page.getRecords().isEmpty()) {
                break;
            }
            all.addAll(page.getRecords());
            if ((long) pageNum * pageSize >= page.getTotal()) {
                break;
            }
            pageNum++;
        }
        return all;
    }

    private Map<Long, Notice> loadLatestNoticeMap(List<Long> bizIds, String noticeType) {
        Map<Long, Notice> noticeMap = new HashMap<>();
        if (bizIds == null || bizIds.isEmpty()) {
            return noticeMap;
        }
        noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .in(Notice::getBizId, bizIds)
                        .eq(Notice::getNoticeType, noticeType)
                        .orderByDesc(Notice::getId))
                .forEach(n -> noticeMap.putIfAbsent(n.getBizId(), n));
        return noticeMap;
    }

    private void putPatientBasicColumns(Map<String, Object> row, Patient p, Notice notice) {
        row.put("数据来源", POP_TYPE_LABEL.getOrDefault(p.getPopulationType(), p.getPopulationType()));
        row.put("姓名", nullToEmpty(p.getName()));
        row.put("性别", nullToEmpty(p.getGender()));
        row.put("证件号", nullToEmpty(p.getIdNumber()));
        row.put("联系电话", nullToEmpty(p.getPhone()));
        row.put("病原学结果", resolvePatientPathogenResultForExport(p));
        row.put("诊断结果", resolvePatientDiagnosisResultForExport(p));
        row.put("服药管理单位", resolvePatientMedicationUnit(p, notice));
    }

    private Map<String, Object> buildPatientNoticeExportRow(Patient p, Notice notice) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("数据来源", POP_TYPE_LABEL.getOrDefault(p.getPopulationType(), p.getPopulationType()));
        row.put("姓名", preferNoticeValue(notice != null ? notice.getPatientName() : null, p.getName()));
        row.put("性别", preferNoticeValue(notice != null ? notice.getGender() : null, p.getGender()));
        row.put("年龄", notice != null && notice.getAge() != null ? notice.getAge() : (p.getAge() != null ? p.getAge() : ""));
        row.put("证件号", preferNoticeValue(notice != null ? notice.getIdNumber() : null, p.getIdNumber()));
        row.put("联系电话", preferNoticeValue(notice != null ? notice.getPhone() : null, p.getPhone()));
        row.put("民族", preferNoticeValue(notice != null ? notice.getEthnicity() : null, p.getEthnicity()));
        row.put("人群分类", preferNoticeValue(notice != null ? notice.getCrowdCategory() : null, p.getCrowdCategory()));
        row.put("现居住地址", preferNoticeValue(notice != null ? notice.getCurrentAddress() : null, p.getCurrentAddress()));
        row.put("户籍地址", preferNoticeValue(notice != null ? notice.getHouseholdAddress() : null, p.getHouseholdAddress()));
        row.put("病原学结果", resolvePatientPathogenResultForExport(p));
        row.put("诊断结果", resolvePatientDiagnosisResultForExport(p));
        if (notice == null) {
            PATIENT_NOTICE_EXPORT_HEADERS.stream()
                    .filter(h -> !List.of(
                            "数据来源", "姓名", "性别", "年龄", "证件号", "联系电话", "民族", "人群分类",
                            "现居住地址", "户籍地址", "病原学结果", "诊断结果", "状态"
                    ).contains(h))
                    .forEach(h -> row.put(h, ""));
            row.put("服药管理单位", resolvePatientMedicationUnit(p, null));
            row.put("状态", "未发送");
            return row;
        }
        row.put("患者类型", nullToEmpty(notice.getPatientType()));
        row.put("管理方式", nullToEmpty(notice.getManagementMethod()));
        row.put("胸片检查时间", formatDate(notice.getChestXrayDate()));
        row.put("胸片检查结果", nullToEmpty(notice.getChestXrayResult()));
        row.put("治疗方案", nullToEmpty(notice.getTreatmentPlan()));
        row.put("耐药情况", nullToEmpty(notice.getDrugResistance()));
        row.put("痰涂片", nullToEmpty(notice.getSputumSmear()));
        row.put("痰培养", nullToEmpty(notice.getSputumCulture()));
        row.put("分子检查", nullToEmpty(notice.getMolecularTest()));
        row.put("病理学检查", nullToEmpty(notice.getPathologyTest()));
        row.put("治疗机构", nullToEmpty(notice.getTreatmentInstitution()));
        row.put("服药管理单位", resolvePatientMedicationUnit(p, notice));
        row.put("下发时间", formatDate(notice.getIssuedTime()));
        row.put("备注", nullToEmpty(notice.getRemark()));
        row.put("其他注意事项", nullToEmpty(notice.getOtherNotes()));
        row.put("下发人", formatNoticeParty(notice.getSenderName(), notice.getSenderOrgName()));
        row.put("接收人", formatNoticeParty(notice.getReceiverName(), notice.getReceiverOrgName()));
        row.put("发送时间", formatDateTime(notice.getSentTime()));
        row.put("接收时间", formatDateTime(notice.getConfirmedTime()));
        row.put("状态", patientNoticeStatusLabel(notice.getStatus()));
        return row;
    }

    private static String preferNoticeValue(String noticeValue, String fallback) {
        return StrUtil.isNotBlank(noticeValue) ? noticeValue.trim() : nullToEmpty(fallback);
    }

    private static String formatNoticeParty(String name, String orgName) {
        String displayName = nullToEmpty(name);
        if (StrUtil.isBlank(displayName)) {
            return "";
        }
        if (StrUtil.isNotBlank(orgName)) {
            return displayName + "（" + orgName.trim() + "）";
        }
        return displayName;
    }

    private String patientNoticeStatusLabel(Integer status) {
        if (status == null) {
            return "未发送";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已发送";
            case 2 -> "已确认";
            default -> String.valueOf(status);
        };
    }

    private void putLatentSupervisionBasicColumns(Map<String, Object> row, LatentInfection r) {
        row.put("数据来源", formatLatentPopulationLabel(r.getPopulationType(), r.getCrowdCategory()));
        row.put("姓名", nullToEmpty(r.getName()));
        row.put("性别", nullToEmpty(r.getGender()));
        row.put("年龄", r.getAge() != null ? r.getAge() : "");
        row.put("证件号", nullToEmpty(r.getIdNumber()));
        row.put("联系电话", nullToEmpty(r.getPhone()));
    }

    private Map<String, Object> buildSupervisionExportRow(LatentInfection r, SupervisionForm f) {
        Map<String, Object> row = new LinkedHashMap<>();
        putLatentSupervisionBasicColumns(row, r);
        row.put("第几次", f.getFormSeq() != null ? f.getFormSeq() : "");
        row.put("类别", nullToEmpty(f.getCategory()));
        row.put("管理单位", nullToEmpty(f.getManagingUnit()));
        row.put("督导医生", nullToEmpty(f.getSupervisingDoctor()));
        row.put("是否开始预防性治疗", nullToEmpty(f.getHasPreventiveTreatment()));
        row.put("治疗方案", nullToEmpty(f.getTreatmentPlan()));
        row.put("治疗开始时间", formatDate(f.getTreatmentStartDate()));
        row.put("治疗结束时间", formatDate(f.getTreatmentEndDate()));
        row.put("督导记录", formatSupervisionRecords(f.getSupervisionRecords()));
        row.put("治疗完成情况", nullToEmpty(f.getTreatmentCompletionStatus()));
        row.put("中断用药", nullToEmpty(f.getInterruptMedication()));
        row.put("中断次数", f.getInterruptCount() != null ? f.getInterruptCount() : "");
        row.put("用药率", nullToEmpty(f.getMedicationRate()));
        row.put("督导管理人员类型", nullToEmpty(f.getManagerType()));
        row.put("督导管理人员姓名", nullToEmpty(f.getManagerName()));
        row.put("备注", nullToEmpty(f.getRemark()));
        row.put("状态", supervisionStatusLabel(f.getStatus()));
        row.put("填写时间", formatDateTime(f.getCreateTime()));
        return row;
    }

    private String supervisionStatusLabel(Integer status) {
        if (status == null) {
            return "未填写";
        }
        return switch (status) {
            case 2 -> "已归档";
            case 1 -> "已提交";
            case 0 -> "草稿";
            default -> String.valueOf(status);
        };
    }

    private String formatSupervisionRecords(String json) {
        if (StrUtil.isBlank(json)) {
            return "";
        }
        try {
            JSONArray array = JSONUtil.parseArray(json);
            List<String> parts = new ArrayList<>();
            for (Object item : array) {
                if (!(item instanceof JSONObject obj)) {
                    continue;
                }
                String time = obj.getStr("time", obj.getStr("date", ""));
                String method = obj.getStr("method", "");
                String content = obj.getStr("content", "");
                String joined = Stream.of(time, method, content)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining("/"));
                if (StrUtil.isNotBlank(joined)) {
                    parts.add(joined);
                }
            }
            return String.join("；", parts);
        } catch (Exception ignored) {
            return json;
        }
    }

    private Map<String, Object> buildFollowUpExportRow(Patient p, FollowUpVisit v, Notice notice) {
        Map<String, Object> row = new LinkedHashMap<>();
        putPatientBasicColumns(row, p, notice);
        row.put("第几次", v.getVisitSeq() != null ? v.getVisitSeq() : "");
        row.put("随访时间", formatDate(v.getVisitDate()));
        row.put("治疗月序", v.getTreatmentMonth() != null ? v.getTreatmentMonth() : "");
        row.put("督导人员", formatFollowUpSupervisor(v.getSupervisor(), v.getSupervisorOther()));
        row.put("随访方式", formatFollowUpVisitMethod(v.getVisitMethod(), v.getVisitMethodOther()));
        row.put("症状及体征", formatSymptomCodes(v.getSymptoms(), FOLLOW_UP_SYMPTOM_LABEL));
        row.put("症状-其它", nullToEmpty(v.getSymptomsOther()));
        row.put("吸烟(支/天)", nullToEmpty(v.getSmokingAmount()));
        row.put("饮酒(两/天)", nullToEmpty(v.getDrinkingAmount()));
        row.put("化疗方案", nullToEmpty(v.getChemotherapyPlan()));
        row.put("用法", FOLLOW_UP_MEDICATION_USAGE_LABEL.getOrDefault(
                nullToEmpty(v.getMedicationUsage()), nullToEmpty(v.getMedicationUsage())));
        row.put("药品剂型", FOLLOW_UP_DRUG_FORM_LABEL.getOrDefault(
                nullToEmpty(v.getDrugForm()), nullToEmpty(v.getDrugForm())));
        row.put("漏服药次数", v.getMissedDoses() != null ? v.getMissedDoses() : "");
        row.put("药物不良反应", YES_NO_LABEL.getOrDefault(
                nullToEmpty(v.getAdverseReaction()), nullToEmpty(v.getAdverseReaction())));
        row.put("不良反应详情", nullToEmpty(v.getAdverseReactionDetail()));
        row.put("并发症/合并症", YES_NO_LABEL.getOrDefault(
                nullToEmpty(v.getComplication()), nullToEmpty(v.getComplication())));
        row.put("并发症详情", nullToEmpty(v.getComplicationDetail()));
        row.put("转诊科别", nullToEmpty(v.getReferralDepartment()));
        row.put("转诊原因", nullToEmpty(v.getReferralReason()));
        row.put("2周内随访结果", nullToEmpty(v.getReferralTwoWeekResult()));
        row.put("处理意见", nullToEmpty(v.getHandlingOpinion()));
        row.put("下次随访时间", formatDate(v.getNextVisitDate()));
        row.put("随访医生签名", nullToEmpty(v.getDoctorSignature()));
        row.put("是否停止治疗", resolveStopTreatment(v));
        row.put("停止治疗时间", formatDate(v.getStopTreatmentDate()));
        row.put("停止治疗原因", formatStopTreatmentReason(v.getStopTreatmentReason(), v.getStopTreatmentReasonOther()));
        row.put("应访视次数", v.getShouldVisitCount() != null ? v.getShouldVisitCount() : "");
        row.put("实际访视次数", v.getActualVisitCount() != null ? v.getActualVisitCount() : "");
        row.put("应服药次数", v.getShouldDoseCount() != null ? v.getShouldDoseCount() : "");
        row.put("实际服药次数", v.getActualDoseCount() != null ? v.getActualDoseCount() : "");
        row.put("服药率(%)", nullToEmpty(v.getMedicationRate()));
        row.put("评估医生签名", nullToEmpty(v.getEvaluatorSignature()));
        row.put("备注", nullToEmpty(v.getRemarks()));
        row.put("状态", visitStatusLabel(v.getStatus()));
        row.put("填写时间", formatDateTime(v.getCreateTime()));
        return row;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String visitStatusLabel(Integer status) {
        if (status == null) {
            return "待填写";
        }
        if (status == 0) {
            return "草稿";
        }
        if (status == 1) {
            return "已完成";
        }
        return String.valueOf(status);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "";
    }

    private String formatSputumCultureSupplementStatus(Integer status) {
        if (status == null) {
            return "";
        }
        if (status == 0) {
            return "未补充";
        }
        if (status == 1) {
            return "补充";
        }
        return "";
    }

    private String formatFirstVisitMethod(String method, String other) {
        if (StrUtil.isBlank(method)) {
            return "";
        }
        if ("其他".equals(method) && StrUtil.isNotBlank(other)) {
            return "其他（" + other.trim() + "）";
        }
        return method;
    }

    private String formatFollowUpVisitMethod(String method, String other) {
        if (StrUtil.isBlank(method)) {
            return "";
        }
        String label = FOLLOW_UP_VISIT_METHOD_LABEL.getOrDefault(method, method);
        if ("4".equals(method) && StrUtil.isNotBlank(other)) {
            return label + "（" + other.trim() + "）";
        }
        return label;
    }

    private String formatFollowUpSupervisor(String supervisor, String other) {
        if (StrUtil.isBlank(supervisor)) {
            return "";
        }
        String label = FOLLOW_UP_SUPERVISOR_LABEL.getOrDefault(supervisor, supervisor);
        if ("4".equals(supervisor) && StrUtil.isNotBlank(other)) {
            return label + "（" + other.trim() + "）";
        }
        return label;
    }

    private String formatSymptomCodes(String raw, Map<String, String> labelMap) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(code -> labelMap.getOrDefault(code, code))
                .collect(Collectors.joining("、"));
    }

    private String formatEducationItems(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        try {
            cn.hutool.json.JSONObject obj = cn.hutool.json.JSONUtil.parseObj(raw);
            List<String> parts = new ArrayList<>();
            obj.forEach((key, val) -> parts.add(key + "：" + val));
            return String.join("；", parts);
        } catch (Exception e) {
            return raw;
        }
    }

    private String formatStopTreatmentReason(String reason, String other) {
        if (StrUtil.isBlank(reason)) {
            return "";
        }
        if ("其它".equals(reason) && StrUtil.isNotBlank(other)) {
            return "其它（" + other.trim() + "）";
        }
        return reason;
    }

    private String resolveStopTreatment(FollowUpVisit v) {
        if (StrUtil.isNotBlank(v.getStopTreatment())) {
            return v.getStopTreatment();
        }
        if (v.getStopTreatmentDate() != null || StrUtil.isNotBlank(v.getStopTreatmentReason())) {
            return "是";
        }
        return "否";
    }

    /**
     * 潜伏感染者信息总表（全部来源，含来源标签）。
     * 与在管列表一致：仅 referralResult=latent；排除确诊患者、已转出（在管）、筛查已删孤儿。
     * populationType 不传时默认排除密接筛查同步数据；若明确传 closeContact 则包含。
     */
    @Operation(summary = "潜伏感染者信息总表导出（全部来源，含来源标签）")
    @GetMapping("/all-latent")
    public void exportAllLatent(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String treatmentCompletionStatus,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String departmentIds,
            @RequestParam(required = false) String formatIssue,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) Integer trackingStatus,
            HttpServletResponse response) throws IOException {

        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<Long> idList = parseIdList(ids);
        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(dateTo);
        List<Long> creatorUserIds = null;
        if (idList.isEmpty() && StrUtil.isNotBlank(creatorName)) {
            creatorUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .and(w -> w.like(User::getRealName, creatorName)
                                    .or()
                                    .like(User::getUsername, creatorName))
                            .select(User::getId))
                    .stream()
                    .map(User::getId)
                    .toList();
            if (creatorUserIds.isEmpty()) {
                writeExcel(response, "潜伏感染者信息总表", List.of(), ALL_LATENT_EXPORT_HEADERS);
                return;
            }
        }
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .eq(archived != null, LatentInfection::getArchived, archived)
                // 仅导出已确认为潜伏感染者的记录（筛查「疑似结核」等待诊断不纳入）
                .eq(LatentInfection::getReferralResult, "latent")
                // 与在管列表一致：排除确诊患者；NULL 诊断结果需显式放行
                .and(w -> w.isNull(LatentInfection::getDiagnosisResult)
                        .or()
                        .notIn(LatentInfection::getDiagnosisResult, "确诊患者", "确诊结核", "在治患者"));
        if (!idList.isEmpty()) {
            wrapper.in(LatentInfection::getId, idList);
        } else {
            wrapper.like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                    .like(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                    .like(StrUtil.isNotBlank(phone), LatentInfection::getPhone, phone)
                    .eq(trackingStatus != null, LatentInfection::getTrackingStatus, trackingStatus)
                    .in(creatorUserIds != null, LatentInfection::getCreatorId, creatorUserIds);
            if (StrUtil.isNotBlank(populationType)) {
                wrapper.eq(LatentInfection::getPopulationType, populationType);
            } else {
                wrapper.and(w -> w.ne(LatentInfection::getPopulationType, "closeContact")
                        .or()
                        .isNull(LatentInfection::getScreeningId));
            }
            // 在管导出排除「已转出」（兼容历史误留在 archived=0 的数据）
            if (archived == null || Integer.valueOf(0).equals(archived)) {
                wrapper.and(w -> w.isNull(LatentInfection::getArchiveRemark)
                        .or()
                        .ne(LatentInfection::getArchiveRemark, LatentInfectionService.ARCHIVE_REMARK_TRANSFERRED_OUT));
            }
            if (Integer.valueOf(1).equals(archived) && (StrUtil.isNotBlank(dateFrom) || StrUtil.isNotBlank(dateTo))) {
                wrapper.ge(StrUtil.isNotBlank(dateFrom), LatentInfection::getArchivedTime, dateFrom)
                        .le(StrUtil.isNotBlank(dateTo), LatentInfection::getArchivedTime, dateTo + " 23:59:59");
            } else {
                wrapper.ge(createFrom != null, LatentInfection::getCreateTime, createFrom)
                        .le(createTo != null, LatentInfection::getCreateTime, createTo);
            }
            KeyPopulationCrowdCategoryQuerySupport.applyLatentFilter(
                    wrapper, populationType, crowdCategory, screeningKeyPopulationMapper);
            IdentityFormatFilterSupport.apply(wrapper, formatIssue, "id_number", "phone");
            latentInfectionService.applyOverviewColumnFilters(wrapper, columnFilters);
        }
        // 排除筛查已删除但 latent 未清理的孤儿记录
        LatentScreeningLinkSupport.applyLinkedScreeningExistsFilter(wrapper);
        wrapper.orderByAsc(LatentInfection::getPopulationType);
        if (Integer.valueOf(1).equals(archived)) {
            wrapper.orderByDesc(LatentInfection::getArchivedTime);
        } else {
            wrapper.orderByDesc(LatentInfection::getCreateTime);
        }
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, LatentInfection::getDepartmentId, filterDeptIds);
        dataScopeHelper.applyLatentScope(wrapper);

        if (idList.isEmpty() && StrUtil.isNotBlank(treatmentCompletionStatus)) {
            List<Long> matchedLatentIds = latentInfectionService.findLatentIdsByPreferredTreatmentCompletionStatus(treatmentCompletionStatus);
            if (matchedLatentIds.isEmpty()) {
                writeExcel(response, "潜伏感染者信息总表", List.of(), ALL_LATENT_EXPORT_HEADERS);
                return;
            }
            wrapper.in(LatentInfection::getId, matchedLatentIds);
        }

        List<LatentInfection> latentList = latentInfectionService.list(wrapper);
        List<Long> latentIds = latentList.stream().map(LatentInfection::getId).collect(Collectors.toList());

        // 批量查询潜伏通知单
        Map<Long, Notice> noticeMap = new HashMap<>();
        if (!latentIds.isEmpty()) {
            noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                            .in(Notice::getBizId, latentIds)
                            .eq(Notice::getNoticeType, "latent")
                            .orderByDesc(Notice::getId))
                    .forEach(n -> noticeMap.putIfAbsent(n.getBizId(), n));
        }

        // 批量查询督导表
        Map<Long, SupervisionForm> supervisionMap = new HashMap<>();
        if (!latentIds.isEmpty()) {
            supervisionFormService.lambdaQuery()
                    .in(SupervisionForm::getLatentInfectionId, latentIds)
                    .orderByDesc(SupervisionForm::getId)
                    .list()
                    .forEach(sv -> supervisionMap.putIfAbsent(sv.getLatentInfectionId(), sv));
        }

        // 批量查询筛查原表，回填感染筛查方法（latent.screenMethod 为非持久化字段）
        Map<Long, String> screenMethodByLatentId = resolveLatentScreenMethodMap(latentList);

        int seq = 1;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LatentInfection r : latentList) {
            Notice notice = noticeMap.get(r.getId());
            SupervisionForm sv = supervisionMap.get(r.getId());

            String trackingLabel = switch (r.getTrackingStatus() != null ? r.getTrackingStatus() : 0) {
                case 1 -> "到位";
                case 2 -> "未到位";
                case 3 -> "其他";
                case 4 -> "强制结束";
                default -> "待追踪";
            };
            String noticeStatusLabel = notice == null ? "未发送"
                    : (Integer.valueOf(2).equals(notice.getStatus()) ? "已确认"
                    : (Integer.valueOf(1).equals(notice.getStatus()) ? "已发送" : "草稿"));
            String supervisionLabel = sv == null ? "未填写"
                    : switch (sv.getStatus() != null ? sv.getStatus() : 0) {
                        case 2 -> "已归档";
                        case 1 -> "已提交";
                        default -> "填写中";
                    };
            String treatmentLabel = r.getTreatmentPhase() == null ? "" : switch (r.getTreatmentPhase()) {
                case 1 -> "预防治疗中";
                case 2 -> "已结案";
                default -> "未开始";
            };

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("序号", seq++);
            row.put("数据来源", formatLatentPopulationLabel(r.getPopulationType(), r.getCrowdCategory()));
            row.put("登记号", StrUtil.blankToDefault(
                    notice != null && StrUtil.isNotBlank(notice.getRegistrationNo())
                            ? notice.getRegistrationNo().trim()
                            : r.getRegistrationNo(),
                    ""));
            row.put("姓名", r.getName());
            row.put("性别", r.getGender());
            row.put("年龄", r.getAge());
            row.put("证件号", r.getIdNumber());
            row.put("联系电话", r.getPhone());
            row.put("联系电话与联系人关系", r.getPhoneContactRelation());
            row.put("户籍地址", r.getHouseholdAddress());
            row.put("现住地址", r.getCurrentAddress());
            row.put("感染筛查日期", formatDate(r.getInfectionScreenDate()));
            row.put("感染筛查方法", resolveLatentScreenMethod(r, screenMethodByLatentId));
            row.put("感染筛查结果", r.getInfectionResult());
            row.put("追踪状态", trackingLabel);
            row.put("未到位次数", r.getNotInPlaceCount() != null ? r.getNotInPlaceCount() : 0);
            row.put("首次诊断结果", r.getDiagnosisFirst());
            row.put("最终诊断结果", r.getDiagnosisResult());
            row.put("是否胸片检查", r.getHasChestXray());
            row.put("胸片检查日期", formatDate(r.getChestXrayDate()));
            row.put("胸片检查结果", r.getChestXrayResult());
            row.put("追踪情况", r.getTrackingRemark());
            row.put("备注", r.getRemark());
            row.put("通知单状态", noticeStatusLabel);
            row.put("督导表状态", supervisionLabel);
            row.put("预防性治疗方案", sv != null ? sv.getTreatmentPlan() : "");
            row.put("预防性治疗开始时间", formatDate(sv != null ? sv.getTreatmentStartDate() : null));
            row.put("预防性治疗完成时间", formatDate(sv != null ? sv.getTreatmentEndDate() : null));
            row.put("预防性治疗结果", sv != null ? sv.getPreventiveResult() : "");
            row.put("治疗完成情况", sv != null ? sv.getTreatmentCompletionStatus() : "");
            row.put("治疗阶段", treatmentLabel);
            row.put("是否归档", isLatentArchived(r, sv) ? "已归档" : "未归档");
            row.put("创建时间", r.getCreateTime() != null ? r.getCreateTime().format(DATETIME_FMT) : "");
            rows.add(row);
        }

        log.info("[导出] 潜伏感染者信息总表 {} 条", rows.size());
        writeExcel(response, "潜伏感染者信息总表", rows, ALL_LATENT_EXPORT_HEADERS);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    /**
     * 批量解析潜伏感染者感染筛查方法（key = latentId）。
     * 学校/重点人群/疫情筛查从筛查表取；密接按 activeRound 取对应轮次方法。
     */
    private Map<Long, String> resolveLatentScreenMethodMap(List<LatentInfection> latentList) {
        Map<Long, String> result = new HashMap<>();
        if (latentList == null || latentList.isEmpty()) {
            return result;
        }

        List<Long> schoolIds = latentList.stream()
                .filter(r -> "school".equals(r.getPopulationType()) && r.getScreeningId() != null)
                .map(LatentInfection::getScreeningId).distinct().toList();
        Map<Long, String> schoolMethodMap = new HashMap<>();
        if (!schoolIds.isEmpty()) {
            screeningSchoolService.listByIds(schoolIds).forEach(s -> {
                if (StrUtil.isNotBlank(s.getScreenMethod())) {
                    schoolMethodMap.put(s.getId(), s.getScreenMethod());
                }
            });
        }

        List<Long> keyIds = latentList.stream()
                .filter(r -> ("keyPopulation".equals(r.getPopulationType()) || "regular".equals(r.getPopulationType()))
                        && r.getScreeningId() != null)
                .map(LatentInfection::getScreeningId).distinct().toList();
        Map<Long, String> keyMethodMap = new HashMap<>();
        if (!keyIds.isEmpty()) {
            keyPopulationService.listByIds(keyIds).forEach(k -> {
                if (StrUtil.isNotBlank(k.getScreenMethod())) {
                    keyMethodMap.put(k.getId(), k.getScreenMethod());
                }
            });
        }

        List<Long> closeIds = latentList.stream()
                .filter(r -> "closeContact".equals(r.getPopulationType()) && r.getScreeningId() != null)
                .map(LatentInfection::getScreeningId).distinct().toList();
        Map<Long, ScreeningCloseContact> closeMap = closeIds.isEmpty() ? Map.of()
                : closeContactService.listByIds(closeIds).stream()
                .collect(Collectors.toMap(ScreeningCloseContact::getId, c -> c, (a, b) -> a));

        for (LatentInfection r : latentList) {
            if (r.getId() == null) {
                continue;
            }
            String method = null;
            if (r.getScreeningId() != null && StrUtil.isNotBlank(r.getPopulationType())) {
                method = switch (r.getPopulationType()) {
                    case "school" -> schoolMethodMap.get(r.getScreeningId());
                    case "keyPopulation", "regular" -> keyMethodMap.get(r.getScreeningId());
                    case "closeContact" -> {
                        ScreeningCloseContact c = closeMap.get(r.getScreeningId());
                        if (c == null) {
                            yield null;
                        }
                        yield switch (r.getActiveRound() == null ? 1 : r.getActiveRound()) {
                            case 2 -> c.getFollowup6ImagingMethod();
                            case 3 -> c.getFollowup12ImagingMethod();
                            default -> c.getInfectionCheckMethod();
                        };
                    }
                    default -> null;
                };
            }
            if (StrUtil.isNotBlank(method)) {
                result.put(r.getId(), ScreeningMethodSupport.normalize(method));
            }
        }
        return result;
    }

    /** 解析潜伏感染者感染筛查方法：优先本表/筛查表，否则从感染筛查结果推断 */
    private String resolveLatentScreenMethod(LatentInfection r, Map<Long, String> screenMethodByLatentId) {
        if (r == null) {
            return "";
        }
        String fromEntity = ScreeningMethodSupport.normalize(r.getScreenMethod());
        if (StrUtil.isNotBlank(fromEntity)) {
            return fromEntity;
        }
        if (r.getId() != null && screenMethodByLatentId != null) {
            String method = ScreeningMethodSupport.normalize(screenMethodByLatentId.get(r.getId()));
            if (StrUtil.isNotBlank(method)) {
                return method;
            }
        }
        return StrUtil.blankToDefault(ScreeningMethodSupport.inferFromInfectionResult(r.getInfectionResult()), "");
    }

    /** 潜伏感染者是否已归档：结案归档、督导表归档均视为已归档 */
    private boolean isLatentArchived(LatentInfection latent, SupervisionForm supervision) {
        if (Integer.valueOf(1).equals(latent.getArchived())) {
            return true;
        }
        if (Integer.valueOf(2).equals(latent.getTreatmentPhase())) {
            return true;
        }
        return supervision != null && Integer.valueOf(2).equals(supervision.getStatus());
    }

    /** 将重点人群多个分类字段拼接为可读字符串 */
    private String buildCrowdCategory(ScreeningKeyPopulation s) {
        List<String> cats = new ArrayList<>();
        if ("是".equals(s.getCrowdCategoryClose()))    cats.add("密接");
        if ("是".equals(s.getCrowdCategoryStudent()))  cats.add("学生");
        if ("是".equals(s.getCrowdCategoryTeacher()))  cats.add("教职工");
        if ("是".equals(s.getCrowdCategoryElder()))    cats.add("老年人");
        if ("是".equals(s.getCrowdCategoryDiabetes())) cats.add("糖尿病");
        if ("是".equals(s.getCrowdCategoryDual()))     cats.add("双感");
        if ("是".equals(s.getCrowdCategoryTbHist()))   cats.add("既往结核史");
        if ("是".equals(s.getCrowdCategoryNormal()))   cats.add("非重点人群");
        return String.join("、", cats);
    }

    private LambdaQueryWrapper<ScreeningSchool> buildScopedSchoolYearWrapper(String year, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = buildYearWrapper(year, ScreeningSchool::getYear);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningSchool::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningKeyPopulation> buildScopedKeyPopulationYearWrapper(String year, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = buildYearWrapper(year, ScreeningKeyPopulation::getYear);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningCloseContact> buildScopedCloseContactYearWrapper(String year, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = buildYearWrapper(year, ScreeningCloseContact::getYear);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningCloseContact::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private <T> com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<T> buildYearWrapper(
            String year, com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> yearField) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        if (year != null && !year.isEmpty()) {
            wrapper.eq(yearField, year);
        }
        return wrapper;
    }

    private Map<String, Object> toMap(Object obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (var field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                map.put(field.getName(), field.get(obj));
            } catch (IllegalAccessException ignored) {}
        }
        return map;
    }

    /** 登记号：主表解析字段或 epidemicData / 导入字段 */
    private static String resolvePatientRegistrationNo(Patient p) {
        if (p == null) {
            return "";
        }
        if (StrUtil.isNotBlank(p.getRegistrationNo())) {
            return p.getRegistrationNo().trim();
        }
        return firstImportField(p, "登记号");
    }

    /** 服药管理单位：通知单优先，其次病案/导入字段 */
    private static String resolvePatientMedicationUnit(Patient p, Notice notice) {
        if (notice != null && StrUtil.isNotBlank(notice.getMedicationManagementUnit())) {
            return notice.getMedicationManagementUnit().trim();
        }
        return firstImportField(p, "服药管理单位");
    }

    /** 与前端 resolvePatientPathogenResult 口径一致 */
    private static String resolvePatientPathogenResultForExport(Patient p) {
        if (p == null) {
            return "";
        }
        String fromImport = firstImportField(p, "病原学结果");
        if (StrUtil.isNotBlank(fromImport)) {
            return fromImport;
        }
        if (StrUtil.isNotBlank(firstImportField(p, "诊断结果")) || isSpecialDiseasePatient(p)) {
            return "";
        }
        return nullToEmpty(p.getDiagnosisResult());
    }

    /** 与前端 resolvePatientDiagnosisResult 口径一致 */
    private static String resolvePatientDiagnosisResultForExport(Patient p) {
        if (p == null) {
            return "";
        }
        String fromImport = firstImportField(p, "诊断结果");
        if (StrUtil.isNotBlank(fromImport)) {
            return fromImport;
        }
        if (isSpecialDiseasePatient(p)) {
            return nullToEmpty(p.getDiagnosisResult());
        }
        return "";
    }

    private static boolean isSpecialDiseasePatient(Patient p) {
        return "specialDisease".equals(p.getPopulationType()) || "specialDisease".equals(p.getSource());
    }

    private static String firstImportField(Patient p, String key) {
        if (p == null || p.getImportFields() == null) {
            return "";
        }
        String value = p.getImportFields().get(key);
        return StrUtil.isNotBlank(value) ? value.trim() : "";
    }

    private void writeExcel(HttpServletResponse response, String fileName, List<Map<String, Object>> rows) throws IOException {
        writeExcel(response, fileName, rows, null);
    }

    private void writeExcel(HttpServletResponse response, String fileName, List<Map<String, Object>> rows,
                            List<String> fallbackHeaders) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);

        List<String> headerKeys = !rows.isEmpty()
                ? new ArrayList<>(rows.get(0).keySet())
                : (fallbackHeaders != null ? fallbackHeaders : List.of());

        if (headerKeys.isEmpty()) {
            EasyExcel.write(response.getOutputStream()).sheet("数据").doWrite(new ArrayList<>());
            return;
        }

        List<List<String>> heads = headerKeys.stream()
                .map(k -> List.of(k)).collect(Collectors.toList());
        List<List<Object>> data = rows.stream()
                .map(row -> headerKeys.stream().map(row::get).collect(Collectors.toList()))
                .collect(Collectors.toList());

        EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(heads)
                .sheet("数据")
                .doWrite(data);
    }
}
