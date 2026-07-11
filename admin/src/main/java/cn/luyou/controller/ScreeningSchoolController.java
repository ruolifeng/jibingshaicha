package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.service.ScreeningSchoolService;
import cn.luyou.utils.PageQueryUtil;
import com.alibaba.excel.EasyExcel;
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

@Tag(name = "学校人群筛查管理")
@RestController
@RequestMapping("/screening/school")
@RequiredArgsConstructor
public class ScreeningSchoolController {

    private final ScreeningSchoolService screeningSchoolService;

    @Operation(summary = "上传学校人群筛查Excel")
    @PostMapping("/upload")
    @OperationLog(type = "import", module = "screening", action = "上传学校人群筛查Excel")
    public ResultResponse<ImportResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmSkipInvalid", defaultValue = "false") boolean confirmSkipInvalid,
            @RequestParam(value = "confirmSkipDuplicateInFile", defaultValue = "false") boolean confirmSkipDuplicateInFile) {
        ImportResult result = screeningSchoolService.uploadAndParse(file, confirmSkipInvalid, confirmSkipDuplicateInFile);
        return ResultRes.success(result);
    }

    @Operation(summary = "分页查询学校人群筛查数据")
    @GetMapping("/list")
    public ResultResponse<IPage<ScreeningSchool>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer isLatent,
            @RequestParam(required = false) String diagnosisFirst,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String entryUnit,
            @RequestParam(required = false) String createTimeFrom,
            @RequestParam(required = false) String createTimeTo,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        IPage<ScreeningSchool> result = screeningSchoolService.queryPage(
                page, PageQueryUtil.clampSize(size), name, idNumber, schoolName, district, isLatent, diagnosisFirst, phone, year, entryUnit,
                createTimeFrom, createTimeTo, creatorUsername, columnFilters, sortField, sortOrder);
        return ResultRes.success(result);
    }

    @Operation(summary = "新增学校人群筛查记录")
    @PostMapping("/create")
    @OperationLog(type = "create", module = "screening", action = "新增学校人群筛查记录")
    public ResultResponse<Void> create(@RequestBody ScreeningSchool data) {
        screeningSchoolService.createScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新学校人群筛查记录")
    @PutMapping("/update/{id}")
    @OperationLog(type = "update", module = "screening", action = "编辑学校人群筛查记录")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody ScreeningSchool data) {
        data.setId(id);
        screeningSchoolService.updateScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除学校人群筛查记录（级联删除后续所有关联数据）")
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "delete", module = "screening", action = "删除学校人群筛查记录")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        screeningSchoolService.deleteScreeningCascade(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量删除学校人群筛查记录（级联删除）")
    @DeleteMapping("/batch-delete")
    @OperationLog(type = "delete", module = "screening", action = "批量删除学校人群筛查记录")
    public ResultResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        screeningSchoolService.batchDeleteCascade(ids);
        return ResultRes.success(null);
    }

    @Operation(summary = "按 ID 查询学校人群筛查记录详情")
    @GetMapping("/{id}")
    public ResultResponse<ScreeningSchool> detail(@PathVariable Long id) {
        return ResultRes.success(screeningSchoolService.getById(id));
    }

    @Operation(summary = "导出学校人群筛查数据（支持当前筛选条件或勾选导出）")
    @GetMapping("/export")
    @OperationLog(type = "export", module = "screening", action = "导出学校人群筛查数据")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer isLatent,
            @RequestParam(required = false) String diagnosisFirst,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String entryUnit,
            @RequestParam(required = false) String createTimeFrom,
            @RequestParam(required = false) String createTimeTo,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode("学校人群筛查数据.xlsx", StandardCharsets.UTF_8));

        List<Long> idList = null;
        if (ids != null && !ids.isBlank()) {
            idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                    .map(Long::valueOf)
                    .toList();
        }

        List<ScreeningSchool> list = screeningSchoolService.listForExport(
                name, idNumber, schoolName, district, isLatent, diagnosisFirst, phone, year, entryUnit,
                createTimeFrom, createTimeTo, creatorUsername, columnFilters, sortField, sortOrder, idList);
        EasyExcel.write(response.getOutputStream(), ScreeningSchool.class).sheet("筛查数据").doWrite(list);
    }
}
