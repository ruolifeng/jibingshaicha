package cn.luyou.controller;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.FirstVisitMapper;
import cn.luyou.mapper.FollowUpVisitMapper;
import cn.luyou.mapper.MedicationManagementMapper;
import cn.luyou.mapper.NoticeMapper;
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
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.ScreeningKeyPopulationService;
import cn.luyou.service.ScreeningSchoolService;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.QueryDateRangeUtil;
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
    private final FirstVisitMapper firstVisitMapper;
    private final FollowUpVisitMapper followUpVisitMapper;
    private final MedicationManagementMapper medicationManagementMapper;
    private final DataScopeHelper dataScopeHelper;

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
    }

    /** 在管患者总表导出列（无数据时也输出表头） */
    private static final List<String> ALL_PATIENT_EXPORT_HEADERS = List.of(
            "序号", "数据来源", "姓名", "性别", "出生日期", "年龄", "证件类型", "证件号",
            "民族", "联系电话", "户籍地址", "现住址", "最终诊断结果", "通知单状态",
            "首次随访", "后续随访次数", "服药管理", "是否归档", "归档备注", "归档时间", "创建时间"
    );

    /** 潜伏感染者信息总表导出列（无数据时也输出表头） */
    private static final List<String> ALL_LATENT_EXPORT_HEADERS = List.of(
            "序号", "数据来源", "姓名", "性别", "年龄", "证件号", "联系电话", "联系电话与联系人关系",
            "户籍地址", "现住地址", "感染筛查日期", "感染筛查结果", "追踪状态", "未到位次数",
            "首次诊断结果", "最终诊断结果", "是否胸片检查", "胸片检查日期", "胸片检查结果",
            "追踪情况", "备注", "通知单状态", "督导表状态", "预防性治疗方案",
            "预防性治疗开始时间", "预防性治疗完成时间", "预防性治疗结果", "治疗阶段",
            "是否归档", "创建时间"
    );

    /** 大汇总表：三类人群筛查数据合并导出 */
    @Operation(summary = "大汇总表导出")
    @GetMapping("/wide-table")
    public void exportWideTable(
            @RequestParam(defaultValue = "") String year,
            HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();

        // 学校人群
        screeningSchoolService.list(buildYearWrapper(year, ScreeningSchool::getYear)).forEach(s -> {
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
        keyPopulationService.list(buildYearWrapper(year, ScreeningKeyPopulation::getYear)).forEach(s -> {
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
        closeContactService.list(buildYearWrapper(year, ScreeningCloseContact::getYear)).forEach(s -> {
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
    public void exportCategoryTable(
            @RequestParam String populationType,
            @RequestParam(defaultValue = "") String year,
            HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String fileName = "分类汇总表";

        switch (populationType) {
            case "school" -> {
                fileName = "学校人群汇总表";
                screeningSchoolService.list(buildYearWrapper(year, ScreeningSchool::getYear)).forEach(s -> {
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
                keyPopulationService.list(buildYearWrapper(year, ScreeningKeyPopulation::getYear)).forEach(s -> {
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
                closeContactService.list(buildYearWrapper(year, ScreeningCloseContact::getYear)).forEach(s -> {
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
    public void exportCustom(
            @RequestParam String populationType,
            @RequestParam String fields, // 逗号分隔的字段名列表
            @RequestParam(defaultValue = "") String year,
            HttpServletResponse response) throws IOException {
        List<String> fieldList = Arrays.asList(fields.split(","));
        List<Map<String, Object>> allRows = new ArrayList<>();

        // 获取该人群类型的完整数据
        switch (populationType) {
            case "school" -> screeningSchoolService.list(buildYearWrapper(year, ScreeningSchool::getYear))
                    .forEach(s -> allRows.add(toMap(s)));
            case "keyPopulation" -> keyPopulationService.list(buildYearWrapper(year, ScreeningKeyPopulation::getYear))
                    .forEach(s -> allRows.add(toMap(s)));
            case "closeContact" -> closeContactService.list(buildYearWrapper(year, ScreeningCloseContact::getYear))
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

    /** 患者管理列表导出（学校/重点人群），字段与模板表头保持一致 */
    @Operation(summary = "患者管理列表导出")
    @GetMapping("/patient-list")
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
                    populationType, name, idNumber, phone, null, null, 0, dateFrom, dateTo, null, null);

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
    public void exportAllPatients(
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
            HttpServletResponse response) throws IOException {

        List<Patient> patientList = patientService.listForExport(
                populationType, name, idNumber, phone, currentAddress, diagnosisResult,
                archived, dateFrom, dateTo, startTime, endTime);
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

        int seq = 1;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Patient p : patientList) {
            Notice notice = noticeMap.get(p.getId());
            MedicationManagement med = medicationMap.get(p.getId());
            String noticeStatusLabel = notice == null ? "未发送"
                    : (Integer.valueOf(2).equals(notice.getStatus()) ? "已确认"
                    : (Integer.valueOf(1).equals(notice.getStatus()) ? "已发送" : "草稿"));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("序号", seq++);
            row.put("数据来源", POP_TYPE_LABEL.getOrDefault(p.getPopulationType(), p.getPopulationType()));
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
            row.put("最终诊断结果", p.getDiagnosisResult());
            row.put("通知单状态", noticeStatusLabel);
            row.put("首次随访", firstVisitSet.contains(p.getId()) ? "已完成" : "未完成");
            row.put("后续随访次数", followUpCountMap.getOrDefault(p.getId(), 0L));
            row.put("服药管理", med != null ? "已录入" : "未录入");
            row.put("是否归档", Integer.valueOf(1).equals(p.getArchived()) ? "已归档" : "未归档");
            row.put("归档备注", p.getArchiveRemark() != null ? p.getArchiveRemark() : "");
            row.put("归档时间", p.getArchivedTime() != null ? p.getArchivedTime().format(DATETIME_FMT) : "");
            row.put("创建时间", p.getCreateTime() != null ? p.getCreateTime().format(DATETIME_FMT) : "");
            rows.add(row);
        }

        log.info("[导出] 患者信息总表 {} 条", rows.size());
        writeExcel(response, "患者信息总表", rows, ALL_PATIENT_EXPORT_HEADERS);
    }

    /**
     * 潜伏感染者信息总表（全部来源，含来源标签）。
     * populationType 不传时默认排除密接（密接潜伏不纳入聚合统计）；若明确传 closeContact 则包含。
     */
    @Operation(summary = "潜伏感染者信息总表导出（全部来源，含来源标签）")
    @GetMapping("/all-latent")
    public void exportAllLatent(
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer archived,
            HttpServletResponse response) throws IOException {

        LocalDateTime createFrom = QueryDateRangeUtil.parseDateTimeFrom(dateFrom);
        LocalDateTime createTo = QueryDateRangeUtil.parseDateTimeTo(dateTo);
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .like(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .like(StrUtil.isNotBlank(phone), LatentInfection::getPhone, phone)
                .eq(archived != null, LatentInfection::getArchived, archived);
        if (StrUtil.isNotBlank(populationType)) {
            wrapper.eq(LatentInfection::getPopulationType, populationType);
        } else {
            wrapper.and(w -> w.ne(LatentInfection::getPopulationType, "closeContact")
                    .or()
                    .isNull(LatentInfection::getScreeningId));
        }
        if (Integer.valueOf(1).equals(archived) && (StrUtil.isNotBlank(dateFrom) || StrUtil.isNotBlank(dateTo))) {
            wrapper.ge(StrUtil.isNotBlank(dateFrom), LatentInfection::getArchivedTime, dateFrom)
                    .le(StrUtil.isNotBlank(dateTo), LatentInfection::getArchivedTime, dateTo + " 23:59:59");
        } else {
            wrapper.ge(createFrom != null, LatentInfection::getCreateTime, createFrom)
                    .le(createTo != null, LatentInfection::getCreateTime, createTo);
        }
        wrapper.orderByAsc(LatentInfection::getPopulationType);
        if (Integer.valueOf(1).equals(archived)) {
            wrapper.orderByDesc(LatentInfection::getArchivedTime);
        } else {
            wrapper.orderByDesc(LatentInfection::getCreateTime);
        }
        dataScopeHelper.applyLatentScope(wrapper);

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
            row.put("数据来源", POP_TYPE_LABEL.getOrDefault(r.getPopulationType(), r.getPopulationType()));
            row.put("姓名", r.getName());
            row.put("性别", r.getGender());
            row.put("年龄", r.getAge());
            row.put("证件号", r.getIdNumber());
            row.put("联系电话", r.getPhone());
            row.put("联系电话与联系人关系", r.getPhoneContactRelation());
            row.put("户籍地址", r.getHouseholdAddress());
            row.put("现住地址", r.getCurrentAddress());
            row.put("感染筛查日期", formatDate(r.getInfectionScreenDate()));
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
