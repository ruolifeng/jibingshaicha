package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.service.ScreeningCloseContactService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "密接人群筛查管理")
@RestController
@RequestMapping("/screening/close-contact")
@RequiredArgsConstructor
public class ScreeningCloseContactController {

    private final ScreeningCloseContactService screeningCloseContactService;

    @Operation(summary = "上传密接人群筛查Excel")
    @PostMapping("/upload")
    public ResultResponse<Integer> upload(@RequestParam("file") MultipartFile file) {
        int count = screeningCloseContactService.uploadAndParse(file);
        return ResultRes.success(count);
    }

    @Operation(summary = "分页查询密接人群筛查数据")
    @GetMapping("/list")
    public ResultResponse<IPage<ScreeningCloseContact>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer isLatent) {
        return ResultRes.success(screeningCloseContactService.queryPage(page, size, name, idNumber, district, isLatent));
    }
}
