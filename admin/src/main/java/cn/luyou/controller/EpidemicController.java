package cn.luyou.controller;

import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.EpidemicImport;
import cn.luyou.service.EpidemicImportService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "大疫情导入筛查")
@RestController
@RequestMapping("/epidemic")
@RequiredArgsConstructor
public class EpidemicController {

    private final EpidemicImportService epidemicImportService;

    @Operation(summary = "导入大疫情表")
    @PostMapping("/import")
    public ResultResponse<Map<String, Object>> importData(@RequestParam("file") MultipartFile file) {
        return ResultRes.success(epidemicImportService.importData(file));
    }

    @Operation(summary = "分页查询大疫情待诊断列表")
    @GetMapping("/list")
    public ResultResponse<IPage<EpidemicImport>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String diagnosisResult
    ) {
        return ResultRes.success(epidemicImportService.queryPage(page, size, name, idNumber, trackingStatus, archived, diagnosisResult));
    }

    @Operation(summary = "追踪操作")
    @PostMapping("/track")
    public ResultResponse<Void> track(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null || body.get("status") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id 或 status");
        }
        Long id = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        String remark = body.getOrDefault("remark", "").toString();
        epidemicImportService.track(id, status, remark);
        return ResultRes.success(null);
    }

    @Operation(summary = "录入胸片结果")
    @PostMapping("/xray")
    public ResultResponse<Void> xray(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id");
        }
        Long id = Long.valueOf(body.get("id").toString());
        epidemicImportService.saveXray(id, body);
        return ResultRes.success(null);
    }

    @Operation(summary = "录入诊断结果并自动分流")
    @PostMapping("/diagnosis")
    public ResultResponse<Void> diagnosis(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null || body.get("diagnosisResult") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id 或 diagnosisResult");
        }
        Long id = Long.valueOf(body.get("id").toString());
        String diagnosisResult = body.get("diagnosisResult").toString();
        epidemicImportService.saveDiagnosis(id, diagnosisResult);
        return ResultRes.success(null);
    }
}

