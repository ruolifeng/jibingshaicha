package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.service.ScreeningSchoolService;
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
import java.util.List;

@Tag(name = "学校人群筛查管理")
@RestController
@RequestMapping("/screening/school")
@RequiredArgsConstructor
public class ScreeningSchoolController {

    private final ScreeningSchoolService screeningSchoolService;

    @Operation(summary = "上传学校人群筛查Excel")
    @PostMapping("/upload")
    public ResultResponse<ImportResult> upload(@RequestParam("file") MultipartFile file) {
        ImportResult result = screeningSchoolService.uploadAndParse(file);
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
            @RequestParam(required = false) Integer isLatent) {
        IPage<ScreeningSchool> result = screeningSchoolService.queryPage(page, size, name, idNumber, schoolName, district, isLatent);
        return ResultRes.success(result);
    }

    @Operation(summary = "导出学校人群筛查数据")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode("学校人群筛查数据.xlsx", StandardCharsets.UTF_8));
        List<ScreeningSchool> list = screeningSchoolService.list(
                Wrappers.<ScreeningSchool>lambdaQuery().orderByDesc(ScreeningSchool::getCreateTime));
        EasyExcel.write(response.getOutputStream(), ScreeningSchool.class).sheet("筛查数据").doWrite(list);
    }
}
