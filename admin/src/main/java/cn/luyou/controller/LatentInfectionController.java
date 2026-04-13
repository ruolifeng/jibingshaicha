package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.LatentInfection;
import cn.luyou.service.LatentInfectionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "潜伏感染管理")
@RestController
@RequestMapping("/latent")
@RequiredArgsConstructor
public class LatentInfectionController {

    private final LatentInfectionService latentInfectionService;

    @Operation(summary = "分页查询潜伏感染数据")
    @GetMapping("/list")
    public ResultResponse<IPage<LatentInfection>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) Integer archived) {
        return ResultRes.success(latentInfectionService.queryPage(page, size, populationType, name, idNumber, trackingStatus, archived));
    }

    @Operation(summary = "追踪操作")
    @PostMapping("/track")
    public ResultResponse<Void> track(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        String remark = body.getOrDefault("remark", "").toString();
        latentInfectionService.track(id, status, remark);
        return ResultRes.success(null);
    }

    @Operation(summary = "转诊操作")
    @PostMapping("/referral")
    public ResultResponse<Void> referral(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        String result = body.get("result").toString();
        String remark = body.getOrDefault("remark", "").toString();
        latentInfectionService.referral(id, result, remark);
        return ResultRes.success(null);
    }
}
