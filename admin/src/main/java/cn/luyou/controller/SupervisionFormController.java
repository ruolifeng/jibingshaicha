package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.SupervisionForm;
import cn.luyou.service.SupervisionFormService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "督导表管理")
@RestController
@RequestMapping("/supervision")
@RequiredArgsConstructor
public class SupervisionFormController {

    private final SupervisionFormService supervisionFormService;

    @Operation(summary = "保存并归档督导表")
    @PostMapping("/save")
    public ResultResponse<Void> save(@RequestBody SupervisionForm form) {
        supervisionFormService.saveAndArchive(form);
        return ResultRes.success(null);
    }

    @Operation(summary = "查询督导表详情")
    @GetMapping("/detail/{latentInfectionId}")
    public ResultResponse<SupervisionForm> detail(@PathVariable Long latentInfectionId) {
        LambdaQueryWrapper<SupervisionForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupervisionForm::getLatentInfectionId, latentInfectionId)
                .orderByDesc(SupervisionForm::getCreateTime)
                .last("LIMIT 1");
        return ResultRes.success(supervisionFormService.getOne(wrapper));
    }
}
