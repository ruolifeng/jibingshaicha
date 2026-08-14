package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.hutool.core.util.StrUtil;
import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.KeyPopulationTbSymptomReferralStatisticsVO;
import cn.luyou.model.vo.PatientDistributionHeatmapVO;
import cn.luyou.model.vo.SchoolStatisticsVO;
import cn.luyou.model.vo.StudentReportStatisticsVO;
import cn.luyou.service.PatientService;
import cn.luyou.service.StatisticsService;
import cn.luyou.service.WorkbenchStatisticsService;
import cn.luyou.utils.DepartmentFilterSupport;
import cn.luyou.utils.KeyPopulationTbSymptomReferralExcelExportSupport;
import cn.luyou.utils.StudentReportExcelExportSupport;
import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "统计分析")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final WorkbenchStatisticsService workbenchStatisticsService;
    private final PatientService patientService;
    private final DepartmentFilterSupport departmentFilterSupport;

    @Operation(summary = "获取区县选项列表")
    @GetMapping("/district-options")
    public ResultResponse<List<String>> districtOptions(
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(statisticsService.getDistrictOptions(filterDeptIds));
    }

    @Operation(summary = "重点人群症状筛查推介报表地区选项（区县+乡镇）")
    @GetMapping("/key-population-tb-symptom-referral/region-options")
    public ResultResponse<List<String>> keyPopulationRegionOptions(
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(statisticsService.getKeyPopulationRegionOptions(filterDeptIds));
    }

    @Operation(summary = "学校人群统计总表")
    @GetMapping("/school")
    public ResultResponse<List<SchoolStatisticsVO>> schoolStatistics(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(statisticsService.getSchoolStatistics(year, district, filterDeptIds));
    }

    @Operation(summary = "新生入学体检结核病检查情况（学生报表）")
    @GetMapping("/student-report")
    public ResultResponse<List<StudentReportStatisticsVO>> studentReportStatistics(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String schoolCategories,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(statisticsService.getStudentReportStatistics(
                year, district, parseSchoolCategories(schoolCategories), filterDeptIds));
    }

    @Operation(summary = "区县统计表")
    @GetMapping("/district")
    public ResultResponse<List<DistrictStatisticsVO>> districtStatistics(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(statisticsService.getDistrictStatistics(year, district, filterDeptIds));
    }

    @Operation(summary = "重点人群肺结核可疑症状筛查和推介情况报表")
    @GetMapping("/key-population-tb-symptom-referral")
    public ResultResponse<List<KeyPopulationTbSymptomReferralStatisticsVO>> keyPopulationTbSymptomReferral(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<Long> selectedDeptIds = departmentFilterSupport.parseSelectedDepartmentIds(departmentIds);
        return ResultRes.success(statisticsService.getKeyPopulationTbSymptomReferralStatistics(
                year, district, filterDeptIds, selectedDeptIds));
    }

    @Operation(summary = "我的工作台年度统计")
    @GetMapping("/workbench")
    public ResultResponse<Map<String, Object>> workbenchStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(workbenchStatisticsService.buildSummary(year, filterDeptIds));
    }

    @Operation(summary = "患者分布热力图（三级及以上用户）")
    @GetMapping("/patient-heatmap")
    public ResultResponse<PatientDistributionHeatmapVO> patientHeatmap(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(patientService.buildPatientDistributionHeatmap(year, district, filterDeptIds));
    }

    @Operation(summary = "导出学校人群统计Excel")
    @GetMapping("/school/export")
    public void exportSchool(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<SchoolStatisticsVO> data = statisticsService.getSchoolStatistics(year, district, filterDeptIds);
        setExcelResponse(response, "学校人群统计总表");
        EasyExcel.write(response.getOutputStream(), SchoolStatisticsVO.class)
                .sheet("辖区教育机构统计总表")
                .doWrite(data);
    }

    @Operation(summary = "导出新生入学体检结核病检查情况（学生报表）")
    @GetMapping("/student-report/export")
    public void exportStudentReport(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String schoolCategories,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<StudentReportStatisticsVO> data = statisticsService.getStudentReportStatistics(
                year, district, parseSchoolCategories(schoolCategories), filterDeptIds);
        setExcelResponse(response, "学生统计报表");
        StudentReportExcelExportSupport.write(response.getOutputStream(), year, data);
    }

    private List<String> parseSchoolCategories(String schoolCategories) {
        if (StrUtil.isBlank(schoolCategories)) {
            return List.of();
        }
        return Arrays.stream(schoolCategories.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Operation(summary = "导出区县统计Excel")
    @GetMapping("/district/export")
    public void exportDistrict(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<DistrictStatisticsVO> data = statisticsService.getDistrictStatistics(year, district, filterDeptIds);
        setExcelResponse(response, "区县统计表");
        EasyExcel.write(response.getOutputStream(), DistrictStatisticsVO.class)
                .sheet("区县统计表")
                .doWrite(data);
    }

    @Operation(summary = "导出重点人群肺结核可疑症状筛查和推介情况报表")
    @GetMapping("/key-population-tb-symptom-referral/export")
    public void exportKeyPopulationTbSymptomReferral(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds,
            HttpServletResponse response) throws IOException {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        List<Long> selectedDeptIds = departmentFilterSupport.parseSelectedDepartmentIds(departmentIds);
        List<KeyPopulationTbSymptomReferralStatisticsVO> data =
                statisticsService.getKeyPopulationTbSymptomReferralStatistics(
                        year, district, filterDeptIds, selectedDeptIds);
        setExcelResponse(response, "重点人群肺结核可疑症状筛查和推介情况报表");
        KeyPopulationTbSymptomReferralExcelExportSupport.write(response.getOutputStream(), year, data);
    }

    @Operation(summary = "按当前表格数据导出重点人群症状筛查推介报表（含手工填写的老年人数）")
    @PostMapping("/key-population-tb-symptom-referral/export")
    public void exportKeyPopulationTbSymptomReferralRows(
            @RequestParam(required = false) String year,
            @RequestBody(required = false) List<KeyPopulationTbSymptomReferralStatisticsVO> rows,
            HttpServletResponse response) throws IOException {
        setExcelResponse(response, "重点人群肺结核可疑症状筛查和推介情况报表");
        KeyPopulationTbSymptomReferralExcelExportSupport.write(
                response.getOutputStream(), year, rows != null ? rows : List.of());
    }

    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedName + ".xlsx");
    }
}
