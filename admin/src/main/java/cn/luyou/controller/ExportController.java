package cn.luyou.controller;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.service.ScreeningKeyPopulationService;
import cn.luyou.service.ScreeningSchoolService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /** 潜伏感染管理列表导出（学校/重点人群） */
    @Operation(summary = "潜伏感染管理列表导出")
    @GetMapping("/latent-list")
    public void exportLatentList(
            @RequestParam String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) Integer archived,
            HttpServletResponse response) throws IOException {

        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .eq(LatentInfection::getPopulationType, populationType)
                .eq(LatentInfection::getReferralResult, "latent")
                .like(StrUtil.isNotBlank(name), LatentInfection::getName, name)
                .like(StrUtil.isNotBlank(idNumber), LatentInfection::getIdNumber, idNumber)
                .eq(archived != null, LatentInfection::getArchived, archived)
                .orderByDesc(LatentInfection::getCreateTime);

        List<Map<String, Object>> rows = new ArrayList<>();
        latentInfectionService.list(wrapper).forEach(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("姓名", r.getName());
            row.put("性别", r.getGender());
            row.put("年龄", r.getAge());
            row.put("证件号", r.getIdNumber());
            row.put("联系电话", r.getPhone());
            row.put("感染筛查结果", r.getInfectionResult());
            row.put("胸片结果", r.getChestXrayResult());
            row.put("诊断结果", r.getDiagnosisFirst());
            row.put("追踪状态", resolveTrackingStatus(r.getTrackingStatus()));
            row.put("转诊结果", resolveReferralResult(r.getReferralResult()));
            row.put("治疗阶段", resolveTreatmentPhase(r.getTreatmentPhase()));
            row.put("服药状态", resolveMedicationStatus(r.getMedicationStatus()));
            row.put("是否归档", Integer.valueOf(1).equals(r.getArchived()) ? "已归档" : "未归档");
            rows.add(row);
        });

        String popLabel = "school".equals(populationType) ? "学校人群" : "重点人群";
        writeExcel(response, popLabel + "_潜伏感染管理", rows);
    }

    /** 患者管理列表导出（学校/重点人群） */
    @Operation(summary = "患者管理列表导出")
    @GetMapping("/patient-list")
    public void exportPatientList(
            @RequestParam String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            HttpServletResponse response) throws IOException {

        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<Patient>()
                .eq(Patient::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .like(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .orderByDesc(Patient::getCreateTime);

        List<Map<String, Object>> rows = new ArrayList<>();
        patientService.list(wrapper).forEach(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("姓名", r.getName());
            row.put("性别", r.getGender());
            row.put("年龄", r.getAge());
            row.put("证件号", r.getIdNumber());
            row.put("联系电话", r.getPhone());
            row.put("诊断结果", r.getDiagnosisResult());
            row.put("来源", "confirmed".equals(r.getSource()) ? "诊断确诊" : "大疫情导入");
            row.put("户籍地址", r.getHouseholdAddress());
            row.put("现住址", r.getCurrentAddress());
            row.put("是否归档", Integer.valueOf(1).equals(r.getArchived()) ? "已归档" : "未归档");
            rows.add(row);
        });

        String popLabel = "school".equals(populationType) ? "学校人群" : "重点人群";
        writeExcel(response, popLabel + "_患者管理", rows);
    }

    private String resolveTrackingStatus(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "待追踪";
            case 1 -> "到位";
            case 2 -> "未到位";
            case 3 -> "其他";
            case 4 -> "强制结束";
            default -> String.valueOf(status);
        };
    }

    private String resolveReferralResult(String result) {
        if (result == null) return "-";
        return switch (result) {
            case "excluded" -> "排除";
            case "other" -> "其他";
            case "confirmed" -> "确诊患者";
            case "suspected" -> "疑似肺结核";
            case "latent" -> "潜伏感染者";
            default -> result;
        };
    }

    private String resolveTreatmentPhase(Integer phase) {
        if (phase == null) return "未开始";
        return switch (phase) {
            case 0 -> "未开始";
            case 1 -> "预防治疗中";
            case 2 -> "已结案";
            default -> String.valueOf(phase);
        };
    }

    private String resolveMedicationStatus(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 1 -> "按要求服药";
            case 2 -> "不服药";
            default -> String.valueOf(status);
        };
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
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);

        if (rows.isEmpty()) {
            EasyExcel.write(response.getOutputStream()).sheet("数据").doWrite(new ArrayList<>());
            return;
        }

        List<List<String>> heads = rows.get(0).keySet().stream()
                .map(k -> List.of(k)).collect(Collectors.toList());
        List<List<Object>> data = rows.stream()
                .map(row -> new ArrayList<Object>(row.values()))
                .collect(Collectors.toList());

        EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(heads)
                .sheet("数据")
                .doWrite(data);
    }
}
