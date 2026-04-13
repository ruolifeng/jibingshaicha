package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.service.ScreeningSchoolService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "学校人群筛查管理")
@RestController
@RequestMapping("/screening/school")
@RequiredArgsConstructor
public class ScreeningSchoolController {

    private final ScreeningSchoolService screeningSchoolService;

    @Operation(summary = "上传学校人群筛查Excel")
    @PostMapping("/upload")
    public ResultResponse<Integer> upload(@RequestParam("file") MultipartFile file) {
        int count = screeningSchoolService.uploadAndParse(file);
        return ResultRes.success(count);
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
}
