package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.LatentCheck;
import cn.luyou.model.LatentFollowUp;
import cn.luyou.model.LatentInfection;
import cn.luyou.service.LatentCheckService;
import cn.luyou.service.LatentFollowUpService;
import cn.luyou.service.LatentInfectionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "潜伏感染管理")
@RestController
@RequestMapping("/latent")
@RequiredArgsConstructor
public class LatentInfectionController {

    private final LatentInfectionService latentInfectionService;
    private final LatentFollowUpService latentFollowUpService;
    private final LatentCheckService latentCheckService;

    @Operation(summary = "分页查询潜伏感染数据")
    @GetMapping("/list")
    public ResultResponse<IPage<LatentInfection>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String referralResult) {
        return ResultRes.success(latentInfectionService.queryPage(page, size, populationType, name, idNumber, trackingStatus, archived, referralResult));
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
        latentInfectionService.track(id, status, remark);
        return ResultRes.success(null);
    }

    // ==================== V13 拆分：胸片 / 诊断 两个独立接口 ====================

    @Operation(summary = "录入胸片检查结果（V13 拆分；仅写胸片字段，不写诊断）")
    @PostMapping("/xray-only")
    @OperationLog(type = "update", module = "latent", action = "录入胸片结果")
    public ResultResponse<Void> saveXrayOnly(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id");
        }
        Long id = Long.valueOf(body.get("id").toString());
        latentInfectionService.saveXrayOnly(id, body);
        return ResultRes.success(null);
    }

    @Operation(summary = "录入首次诊断结果（V13 拆分；仅写诊断字段，并按映射触发转诊）")
    @PostMapping("/diagnosis")
    @OperationLog(type = "update", module = "latent", action = "录入诊断结果")
    public ResultResponse<Void> saveDiagnosis(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id");
        }
        Long id = Long.valueOf(body.get("id").toString());
        latentInfectionService.saveDiagnosisOnly(id, body);
        return ResultRes.success(null);
    }

    /**
     * @deprecated V13 起拆分为 {@code /xray-only} + {@code /diagnosis}。
     *             本接口保留用于旧前端和批量导入工具的兼容。
     */
    @Deprecated
    @Operation(summary = "[兼容] 手动录入胸片检查与首次诊断结果（一次性同时写入）")
    @PostMapping("/xray")
    public ResultResponse<Void> saveXray(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id");
        }
        Long id = Long.valueOf(body.get("id").toString());
        latentInfectionService.saveXrayAndDiagnosis(id, body);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量导入胸片+诊断 Excel（按证件号匹配更新）")
    @PostMapping("/xray/import")
    @OperationLog(type = "import", module = "latent", action = "批量导入胸片+诊断")
    public ResultResponse<Integer> importXray(
            @RequestParam("file") MultipartFile file,
            @RequestParam String populationType) {
        int count = latentInfectionService.importXrayBatch(file, populationType);
        return ResultRes.success(count);
    }

    @Operation(summary = "转诊操作")
    @PostMapping("/referral")
    public ResultResponse<Void> referral(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null || body.get("result") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id 或 result");
        }
        Long id = Long.valueOf(body.get("id").toString());
        String result = body.get("result").toString();
        String remark = body.getOrDefault("remark", "").toString();
        latentInfectionService.referral(id, result, remark);
        return ResultRes.success(null);
    }

    // ==================== 预防治疗管理 ====================

    @Operation(summary = "设置服药状态")
    @PostMapping("/medication-status")
    public ResultResponse<Void> setMedicationStatus(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null || body.get("medicationStatus") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id 或 medicationStatus");
        }
        Long id = Long.valueOf(body.get("id").toString());
        Integer medicationStatus = Integer.valueOf(body.get("medicationStatus").toString());
        latentInfectionService.setMedicationStatus(id, medicationStatus);
        return ResultRes.success(null);
    }

    @Operation(summary = "结案归档")
    @PostMapping("/close-case/{id}")
    public ResultResponse<Void> closeCase(@PathVariable Long id) {
        latentInfectionService.closeCase(id);
        return ResultRes.success(null);
    }

    // ==================== 电话随访 ====================

    @Operation(summary = "查询电话随访记录")
    @GetMapping("/follow-up/list/{latentId}")
    public ResultResponse<List<LatentFollowUp>> followUpList(@PathVariable Long latentId) {
        return ResultRes.success(latentFollowUpService.listByLatentId(latentId));
    }

    @Operation(summary = "新增电话随访记录")
    @PostMapping("/follow-up/save")
    public ResultResponse<Void> saveFollowUp(@RequestBody LatentFollowUp followUp) {
        latentFollowUpService.save(followUp);
        return ResultRes.success(null);
    }

    // ==================== 按期检查 ====================

    @Operation(summary = "查询按期检查记录")
    @GetMapping("/check/list/{latentId}")
    public ResultResponse<List<LatentCheck>> checkList(@PathVariable Long latentId) {
        return ResultRes.success(latentCheckService.listByLatentId(latentId));
    }

    @Operation(summary = "新增按期检查记录")
    @PostMapping("/check/save")
    public ResultResponse<Void> saveCheck(@RequestBody LatentCheck check) {
        latentCheckService.save(check);
        return ResultRes.success(null);
    }
}
