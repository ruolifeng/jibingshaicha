package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.service.ScreeningKeyPopulationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
