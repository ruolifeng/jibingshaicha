package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.KeyPopulationTbSymptomReferralStatisticsVO;
import cn.luyou.model.vo.PatientDistributionHeatmapVO;
import cn.luyou.model.vo.SchoolStatisticsVO;
import cn.luyou.service.PatientService;
import cn.luyou.service.StatisticsService;
import cn.luyou.service.WorkbenchStatisticsService;
import cn.luyou.utils.DepartmentFilterSupport;
import cn.luyou.utils.KeyPopulationTbSymptomReferralExcelExportSupport;
import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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

    @Operation(summary = "学校人群统计总表")
    @GetMapping("/school")
    public ResultResponse<List<SchoolStatisticsVO>> schoolStatistics(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(statisticsService.getSchoolStatistics(year, district, filterDeptIds));
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
        return ResultRes.success(statisticsService.getKeyPopulationTbSymptomReferralStatistics(
                year, district, filterDeptIds));
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
        List<KeyPopulationTbSymptomReferralStatisticsVO> data =
                statisticsService.getKeyPopulationTbSymptomReferralStatistics(year, district, filterDeptIds);
        setExcelResponse(response, "重点人群肺结核可疑症状筛查和推介情况报表");
        KeyPopulationTbSymptomReferralExcelExportSupport.write(response.getOutputStream(), year, data);
    }

    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedName + ".xlsx");
    }
}
