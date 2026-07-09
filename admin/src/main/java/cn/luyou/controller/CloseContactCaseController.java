package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ImportResult;
import cn.luyou.service.CloseContactCaseService;
import cn.luyou.utils.CloseContactCaseExcelExportSupport;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Tag(name = "密接个案表管理")
@RestController
@RequestMapping("/close-contact/case")
@RequiredArgsConstructor
public class CloseContactCaseController {

    private static final String RESULT_LATENT = "潜伏感染者";
    private static final String RESULT_CONFIRMED = "活动性肺结核";

    private final CloseContactCaseService closeContactCaseService;

    @Operation(summary = "上传密接个案表Excel（72列官方模板）")
    @PostMapping("/upload")
    @OperationLog(type = "import", module = "screening", action = "上传密接个案表Excel")
    public ResultResponse<ImportResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmSkipInvalid", defaultValue = "false") boolean confirmSkipInvalid) {
        return ResultRes.success(closeContactCaseService.uploadAndParse(file, confirmSkipInvalid));
    }

    @Operation(summary = "分页查询密接个案表")
    @GetMapping("/list")
    public ResultResponse<IPage<CloseContactCase>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) String createTimeFrom,
            @RequestParam(required = false) String createTimeTo,
            @RequestParam(required = false) String columnFilters) {
        return ResultRes.success(closeContactCaseService.queryPage(
                page, size, name, idNumber, district, phone, creatorUsername, diagnosisResult,
                createTimeFrom, createTimeTo, columnFilters));
    }

    @Operation(summary = "新增密接个案")
    @PostMapping("/create")
    @OperationLog(type = "create", module = "screening", action = "新增密接个案")
    public ResultResponse<Void> create(@RequestBody CloseContactCase data) {
        closeContactCaseService.createCase(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新密接个案")
    @PutMapping("/update/{id}")
    @OperationLog(type = "update", module = "screening", action = "编辑密接个案")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody CloseContactCase data) {
        data.setId(id);
        closeContactCaseService.updateCase(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除密接个案")
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "delete", module = "screening", action = "删除密接个案")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        closeContactCaseService.deleteCase(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量删除密接个案")
    @DeleteMapping("/batch-delete")
    @OperationLog(type = "delete", module = "screening", action = "批量删除密接个案")
    public ResultResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        closeContactCaseService.batchDelete(ids);
        return ResultRes.success(null);
    }

    @Operation(summary = "导出密接个案表（支持筛选/勾选/按诊断结果导出）")
    @GetMapping("/export")
    @OperationLog(type = "export", module = "screening", action = "导出密接个案表")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String diagnosisResult,
            @RequestParam(required = false) String exportType,
            @RequestParam(required = false) String createTimeFrom,
            @RequestParam(required = false) String createTimeTo) throws Exception {

        String effectiveDiagnosis = diagnosisResult;
        String fileName = "密接个案表.xlsx";
        if ("latent".equals(exportType)) {
            effectiveDiagnosis = RESULT_LATENT;
            fileName = "密接个案表_潜伏感染者.xlsx";
        } else if ("confirmed".equals(exportType)) {
            effectiveDiagnosis = RESULT_CONFIRMED;
            fileName = "密接个案表_确诊患者.xlsx";
        }

        List<Long> idList = null;
        if (ids != null && !ids.isBlank()) {
            idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                    .map(Long::valueOf)
                    .toList();
        }

        List<CloseContactCase> list = closeContactCaseService.listForExport(
                name, idNumber, district, phone, creatorUsername, effectiveDiagnosis, idList,
                createTimeFrom, createTimeTo);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 官方 72 列表头，与导入模板一致
        CloseContactCaseExcelExportSupport.write(response.getOutputStream(), CloseContactCaseExcelExportSupport.SHEET_NAME, CloseContactCase.class, list);
    }

    @Operation(summary = "按ID查询密接个案详情")
    @GetMapping("/{id}")
    public ResultResponse<CloseContactCase> detail(@PathVariable Long id) {
        return ResultRes.success(closeContactCaseService.getAccessibleById(id));
    }
}
