package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.service.ScreeningKeyPopulationService;
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

@Tag(name = "重点人群筛查管理")
@RestController
@RequestMapping("/screening/key-population")
@RequiredArgsConstructor
public class ScreeningKeyPopulationController {

    private final ScreeningKeyPopulationService screeningKeyPopulationService;

    @Operation(summary = "上传重点人群筛查Excel")
    @PostMapping("/upload")
    public ResultResponse<Integer> upload(@RequestParam("file") MultipartFile file) {
        int count = screeningKeyPopulationService.uploadAndParse(file);
        return ResultRes.success(count);
    }

    @Operation(summary = "分页查询重点人群筛查数据")
    @GetMapping("/list")
    public ResultResponse<IPage<ScreeningKeyPopulation>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer isLatent) {
        return ResultRes.success(screeningKeyPopulationService.queryPage(page, size, name, idNumber, district, isLatent));
    }

    @Operation(summary = "导出重点人群筛查数据")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode("重点人群筛查数据.xlsx", StandardCharsets.UTF_8));
        List<ScreeningKeyPopulation> list = screeningKeyPopulationService.list(
                Wrappers.<ScreeningKeyPopulation>lambdaQuery().orderByDesc(ScreeningKeyPopulation::getCreateTime));
        EasyExcel.write(response.getOutputStream(), ScreeningKeyPopulation.class).sheet("筛查数据").doWrite(list);
    }
}
