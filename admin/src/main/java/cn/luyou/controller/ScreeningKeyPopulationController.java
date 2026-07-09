package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.service.ScreeningKeyPopulationService;
import cn.luyou.utils.ImportRowOrderSupport;
import cn.luyou.utils.KeyPopulationScreeningExcelExportSupport;
import cn.luyou.utils.ScreeningScopeHelper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

@Tag(name = "重点人群筛查管理")
@RestController
@RequestMapping("/screening/key-population")
@RequiredArgsConstructor
public class ScreeningKeyPopulationController {

    private final ScreeningKeyPopulationService screeningKeyPopulationService;
    private final ScreeningScopeHelper screeningScopeHelper;

    @Operation(summary = "上传重点人群/疫情筛查Excel（sourceType=keyPopulation|regular，默认 keyPopulation）")
    @PostMapping("/upload")
    @OperationLog(type = "import", module = "screening", action = "上传重点人群筛查Excel")
    public ResultResponse<ImportResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", defaultValue = "keyPopulation") String sourceType,
            @RequestParam(value = "overwrite", defaultValue = "true") boolean overwrite,
            @RequestParam(value = "confirmSkipInvalid", defaultValue = "false") boolean confirmSkipInvalid) {
        ImportResult result = screeningKeyPopulationService.uploadAndParse(file, sourceType, overwrite, confirmSkipInvalid);
        return ResultRes.success(result);
    }

    @Operation(summary = "上传重点人群/疫情筛查Excel预览（检测与系统重复人员）")
    @PostMapping("/upload/preview")
    public ResultResponse<java.util.Map<String, Object>> previewUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", defaultValue = "keyPopulation") String sourceType) {
        return ResultRes.success(screeningKeyPopulationService.previewUpload(file, sourceType));
    }

    @Operation(summary = "分页查询重点人群/疫情筛查数据（sourceType=keyPopulation|regular，默认 keyPopulation）")
    @GetMapping("/list")
    public ResultResponse<IPage<ScreeningKeyPopulation>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String townshipCommunity,
            @RequestParam(required = false) String crowdCategory, // 逗号分隔多选：单选为单纯分类，多选为 AND
            @RequestParam(required = false) String screenMethod,
            @RequestParam(required = false) Integer isLatent,
            @RequestParam(value = "sourceType", defaultValue = "keyPopulation") String sourceType,
            @RequestParam(required = false) String diagnosisFirst,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String entryUnit,
            @RequestParam(required = false) String createTimeFrom,
            @RequestParam(required = false) String createTimeTo,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters) {
        return ResultRes.success(screeningKeyPopulationService.queryPage(
                page, size, name, idNumber, phone, district, townshipCommunity, crowdCategory, screenMethod, isLatent,
                sourceType, diagnosisFirst, dateFrom, dateTo, entryUnit, createTimeFrom, createTimeTo,
                creatorUsername, columnFilters));
    }

    @Operation(summary = "新增重点人群筛查记录")
    @PostMapping("/create")
    @OperationLog(type = "create", module = "screening", action = "新增重点人群筛查记录")
    public ResultResponse<Void> create(@RequestBody ScreeningKeyPopulation data) {
        screeningKeyPopulationService.createScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新重点人群筛查记录")
    @PutMapping("/update/{id}")
    @OperationLog(type = "update", module = "screening", action = "编辑重点人群筛查记录")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody ScreeningKeyPopulation data) {
        data.setId(id);
        screeningKeyPopulationService.updateScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除重点人群筛查记录（级联删除后续所有关联数据）")
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "delete", module = "screening", action = "删除重点人群筛查记录")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        screeningKeyPopulationService.deleteScreeningCascade(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量删除重点人群筛查记录（级联删除）")
    @DeleteMapping("/batch-delete")
    @OperationLog(type = "delete", module = "screening", action = "批量删除重点人群筛查记录")
    public ResultResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids != null) ids.forEach(screeningKeyPopulationService::deleteScreeningCascade);
        return ResultRes.success(null);
    }

    @Operation(summary = "按 ID 查询重点人群筛查记录详情")
    @GetMapping("/{id}")
    public ResultResponse<ScreeningKeyPopulation> detail(@PathVariable Long id) {
        return ResultRes.success(screeningKeyPopulationService.getById(id));
    }

    @Operation(summary = "导出重点人群筛查数据")
    @GetMapping("/export")
    @OperationLog(type = "export", module = "screening", action = "导出重点人群筛查数据")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String ids,
            @RequestParam(value = "sourceType", defaultValue = "keyPopulation") String sourceType) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = "regular".equals(sourceType) ? "疫情筛查数据.xlsx" : "重点人群筛查数据.xlsx";
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        var query = Wrappers.<ScreeningKeyPopulation>lambdaQuery();
        query.eq(ScreeningKeyPopulation::getSourceType, sourceType);
        if (ids != null && !ids.isBlank()) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                    .map(Long::valueOf)
                    .toList();
            if (!idList.isEmpty()) {
                query.in(ScreeningKeyPopulation::getId, idList);
            }
        }
        screeningScopeHelper.applyDepartmentScope(
                query, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        ImportRowOrderSupport.applyWithBatch(query);
        List<ScreeningKeyPopulation> list = screeningKeyPopulationService.list(query);
        KeyPopulationScreeningExcelExportSupport.write(response.getOutputStream(), list);
    }
}
