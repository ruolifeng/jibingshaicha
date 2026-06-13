package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.service.ScreeningKeyPopulationService;
import cn.luyou.utils.ScreeningScopeHelper;
import com.alibaba.excel.EasyExcel;
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
    public ResultResponse<ImportResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", defaultValue = "keyPopulation") String sourceType) {
        ImportResult result = screeningKeyPopulationService.uploadAndParse(file, sourceType);
        return ResultRes.success(result);
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
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String screenMethod,
            @RequestParam(required = false) Integer isLatent,
            @RequestParam(value = "sourceType", defaultValue = "keyPopulation") String sourceType,
            @RequestParam(required = false) String diagnosisFirst,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String entryUnit) {
        return ResultRes.success(screeningKeyPopulationService.queryPage(
                page, size, name, idNumber, phone, district, townshipCommunity, crowdCategory, screenMethod, isLatent, sourceType, diagnosisFirst, dateFrom, dateTo, entryUnit));
    }

    @Operation(summary = "新增重点人群筛查记录")
    @PostMapping("/create")
    public ResultResponse<Void> create(@RequestBody ScreeningKeyPopulation data) {
        screeningKeyPopulationService.createScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新重点人群筛查记录")
    @PutMapping("/update/{id}")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody ScreeningKeyPopulation data) {
        data.setId(id);
        screeningKeyPopulationService.updateScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除重点人群筛查记录（级联删除后续所有关联数据）")
    @DeleteMapping("/delete/{id}")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        screeningKeyPopulationService.deleteScreeningCascade(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量删除重点人群筛查记录（级联删除）")
    @DeleteMapping("/batch-delete")
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
        query.orderByDesc(ScreeningKeyPopulation::getCreateTime);
        List<ScreeningKeyPopulation> list = screeningKeyPopulationService.list(query);
        EasyExcel.write(response.getOutputStream(), ScreeningKeyPopulation.class).sheet("筛查数据").doWrite(list);
    }
}
