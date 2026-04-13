package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.SchoolStatisticsVO;
import cn.luyou.service.StatisticsService;
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

@Tag(name = "统计分析")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "学校人群统计总表")
    @GetMapping("/school")
    public ResultResponse<List<SchoolStatisticsVO>> schoolStatistics(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district) {
        return ResultRes.success(statisticsService.getSchoolStatistics(year, district));
    }

    @Operation(summary = "区县统计表")
    @GetMapping("/district")
    public ResultResponse<List<DistrictStatisticsVO>> districtStatistics(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district) {
        return ResultRes.success(statisticsService.getDistrictStatistics(year, district));
    }

    @Operation(summary = "导出学校人群统计Excel")
    @GetMapping("/school/export")
    public void exportSchool(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String district,
            HttpServletResponse response) throws IOException {
        List<SchoolStatisticsVO> data = statisticsService.getSchoolStatistics(year, district);
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
            HttpServletResponse response) throws IOException {
        List<DistrictStatisticsVO> data = statisticsService.getDistrictStatistics(year, district);
        setExcelResponse(response, "区县统计表");
        EasyExcel.write(response.getOutputStream(), DistrictStatisticsVO.class)
                .sheet("区县统计表")
                .doWrite(data);
    }

    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedName + ".xlsx");
    }
}
