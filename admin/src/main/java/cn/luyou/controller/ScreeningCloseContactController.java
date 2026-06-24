package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.service.ScreeningCloseContactService;
import cn.luyou.utils.CloseContactCaseExcelExportSupport;
import cn.luyou.utils.ScreeningScopeHelper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "密接人群筛查管理")
@RestController
@RequestMapping("/screening/close-contact")
@RequiredArgsConstructor
public class ScreeningCloseContactController {

    private final ScreeningCloseContactService screeningCloseContactService;
    private final ScreeningScopeHelper screeningScopeHelper;

    @Operation(summary = "上传密接人群筛查Excel（71列官方模板）")
    @PostMapping("/upload")
    public ResultResponse<ImportResult> upload(@RequestParam("file") MultipartFile file) {
        return ResultRes.success(screeningCloseContactService.uploadAndParse(file));
    }

    @Operation(summary = "分页查询密接人群筛查数据")
    @GetMapping("/list")
    public ResultResponse<IPage<ScreeningCloseContact>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer ccStatus,
            @RequestParam(required = false) String finalScreeningResult,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResultRes.success(screeningCloseContactService.queryPage(
                page, size, name, idNumber, district, ccStatus, finalScreeningResult, phone, dateFrom, dateTo));
    }

    @Operation(summary = "各最终筛查结果分类统计")
    @GetMapping("/count-by-result")
    public ResultResponse<Map<String, Long>> countByResult() {
        return ResultRes.success(screeningCloseContactService.countByFinalResult());
    }

    @Operation(summary = "新增密接人群筛查记录")
    @PostMapping("/create")
    public ResultResponse<Void> create(@RequestBody ScreeningCloseContact data) {
        screeningCloseContactService.createScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新密接人群筛查记录")
    @PutMapping("/update/{id}")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody ScreeningCloseContact data) {
        data.setId(id);
        screeningCloseContactService.updateScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除密接人群筛查记录（级联删除）")
    @DeleteMapping("/delete/{id}")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        screeningCloseContactService.deleteScreeningCascade(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量删除密接人群筛查记录（级联删除）")
    @DeleteMapping("/batch-delete")
    public ResultResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids != null) ids.forEach(screeningCloseContactService::deleteScreeningCascade);
        return ResultRes.success(null);
    }

    @Operation(summary = "按ID查询密接人群筛查记录详情")
    @GetMapping("/{id}")
    public ResultResponse<ScreeningCloseContact> detail(@PathVariable Long id) {
        return ResultRes.success(screeningCloseContactService.getEnrichedById(id));
    }

    // ==================== 密接专属业务接口 ====================

    @Operation(summary = "设置预计完成治疗时间（潜伏感染者-预防治疗）")
    @PostMapping("/{id}/expected-end-date")
    public ResultResponse<Void> setExpectedEndDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate expectedDate) {
        screeningCloseContactService.setExpectedTreatmentEndDate(id, expectedDate);
        return ResultRes.success(null);
    }

    @Operation(summary = "确认治疗是否完成（到预计完成时间后操作）")
    @PostMapping("/{id}/confirm-treatment")
    public ResultResponse<Void> confirmTreatment(
            @PathVariable Long id,
            @RequestParam boolean done) {
        screeningCloseContactService.confirmTreatmentDone(id, done);
        return ResultRes.success(null);
    }

    @Operation(summary = "提交3月复查结果（未发现异常流程）")
    @PostMapping("/{id}/three-month-check")
    public ResultResponse<Void> threeMonthCheck(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkDate,
            @RequestParam String checkResult,
            @RequestParam String finalResult) {
        screeningCloseContactService.submitThreeMonthCheck(id, checkDate, checkResult, finalResult);
        return ResultRes.success(null);
    }

    // ==================== 导出 ====================

    @Operation(summary = "导出密接人群筛查数据（71列官方模板，可再导入）")
    @GetMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String ids) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode("密接人群筛查数据.xlsx", StandardCharsets.UTF_8));

        var query = Wrappers.<ScreeningCloseContact>lambdaQuery();
        if (ids != null && !ids.isBlank()) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                    .map(Long::valueOf)
                    .toList();
            if (!idList.isEmpty()) query.in(ScreeningCloseContact::getId, idList);
        }
        screeningScopeHelper.applyDepartmentScope(
                query, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
        query.orderByDesc(ScreeningCloseContact::getCreateTime);
        List<ScreeningCloseContact> list = screeningCloseContactService.list(query);

        CloseContactCaseExcelExportSupport.write(
                response.getOutputStream(), CloseContactCaseExcelExportSupport.SHEET_NAME, ScreeningCloseContact.class, list);
    }
}
