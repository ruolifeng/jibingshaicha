package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentCheck;
import cn.luyou.model.LatentFollowUp;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.MedicationManagement;
import cn.luyou.model.MedicationPickup;
import cn.luyou.service.LatentCheckService;
import cn.luyou.service.LatentFollowUpService;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.MedicationManagementService;
import cn.luyou.service.MedicationPickupService;
import cn.luyou.service.UserService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.FlexibleDateParseUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "潜伏感染管理")
@RestController
@RequestMapping("/latent")
@RequiredArgsConstructor
public class LatentInfectionController {

    private static final String[] MEDICATION_PICKUP_WRITE_PERMISSIONS = {
            "latentManagement:pickup"
    };
    private static final String[] MEDICATION_PICKUP_READ_PERMISSIONS = {
            "latentManagement:pickup",
            "latentManagement:medication"
    };

    private final LatentInfectionService latentInfectionService;
    private final LatentFollowUpService latentFollowUpService;
    private final LatentCheckService latentCheckService;
    private final MedicationManagementService medicationManagementService;
    private final MedicationPickupService medicationPickupService;
    private final UserService userService;

    @Operation(summary = "历史患者列表（已归档潜伏感染者）")
    @GetMapping("/history")
    public ResultResponse<IPage<LatentInfection>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String treatmentCompletionStatus,
            @RequestParam(required = false) String columnFilters) {
        return ResultRes.success(latentInfectionService.queryHistoryPage(
                page, size, populationType, name, idNumber, phone, startTime, endTime,
                treatmentCompletionStatus, columnFilters));
    }

    @Operation(summary = "表头筛选：某列实际去重值（Excel 式）")
    @GetMapping("/column-distinct")
    public ResultResponse<List<String>> columnDistinct(
            @RequestParam String field,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false, defaultValue = "0") Integer archived,
            @RequestParam(required = false) String referralResult) {
        return ResultRes.success(latentInfectionService.listDistinctColumnValues(
                field, populationType, archived, referralResult));
    }

    @Operation(summary = "潜伏感染详情")
    @GetMapping("/{id}")
    public ResultResponse<LatentInfection> detail(@PathVariable Long id) {
        return ResultRes.success(latentInfectionService.getDetail(id));
    }

    @Operation(summary = "更新潜伏感染基本信息")
    @PutMapping("/{id}")
    @OperationLog(type = "update", module = "latent", action = "更新潜伏感染基本信息")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        latentInfectionService.updateBasicInfo(id, body);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除潜伏感染记录（级联删除）")
    @DeleteMapping("/{id}")
    @OperationLog(type = "delete", module = "latent", action = "删除潜伏感染记录")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        latentInfectionService.deleteCascade(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "手动新增潜伏感染记录")
    @PostMapping
    @OperationLog(type = "create", module = "latent", action = "新增潜伏感染记录")
    public ResultResponse<Long> create(@RequestBody Map<String, Object> body) {
        return ResultRes.success(latentInfectionService.createManual(body));
    }

    @Operation(summary = "批量导入潜伏感染记录（字段与手动新增一致）")
    @PostMapping("/import")
    @OperationLog(type = "import", module = "latent", action = "批量导入潜伏感染者")
    public ResultResponse<ImportResult> importManual(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmSkipInvalid", defaultValue = "false") boolean confirmSkipInvalid,
            @RequestParam(value = "confirmSkipDuplicateInFile", defaultValue = "false") boolean confirmSkipDuplicateInFile) {
        return ResultRes.success(latentInfectionService.importManualBatch(file, confirmSkipInvalid, confirmSkipDuplicateInFile));
    }

    @Operation(summary = "批量删除潜伏感染记录（级联删除）")
    @DeleteMapping("/batch-delete")
    @OperationLog(type = "delete", module = "latent", action = "批量删除潜伏感染记录")
    public ResultResponse<Void> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("ids");
        if (rawIds == null || rawIds.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择要删除的记录");
        }
        List<Long> ids = rawIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        latentInfectionService.batchDeleteCascade(ids);
        return ResultRes.success(null);
    }

    @Operation(summary = "按筛选条件删除潜伏感染记录（级联删除）")
    @DeleteMapping("/delete-by-filter")
    @OperationLog(type = "delete", module = "latent", action = "按筛选条件删除潜伏感染记录")
    public ResultResponse<Integer> deleteByFilter(
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String referralResult,
            @RequestParam(required = false) String diagnosisFirst,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String formatIssue) {
        return ResultRes.success(latentInfectionService.deleteByFilter(
                populationType, name, idNumber, trackingStatus, archived, referralResult, diagnosisFirst,
                phone, dateFrom, dateTo, dateFilterBy, creatorName, crowdCategory, columnFilters, formatIssue));
    }

    @Operation(summary = "分页查询潜伏感染数据")
    @GetMapping("/list")
    public ResultResponse<IPage<LatentInfection>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String populationType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String referralResult,
            @RequestParam(required = false) String diagnosisFirst,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String dateFilterBy,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String crowdCategory,
            @RequestParam(required = false) String columnFilters,
            @RequestParam(required = false) String formatIssue) {
        return ResultRes.success(latentInfectionService.queryPage(
                page, size, populationType, name, idNumber, trackingStatus, archived, referralResult, diagnosisFirst,
                phone, dateFrom, dateTo, dateFilterBy, creatorName, crowdCategory, null, columnFilters, formatIssue));
    }

    @Operation(summary = "追踪操作")
    @PostMapping("/track")
    @OperationLog(type = "update", module = "latent", action = "潜伏感染追踪操作")
    public ResultResponse<Void> track(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null || body.get("status") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id 或 status");
        }
        Long id = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        String remark = body.getOrDefault("remark", "").toString();
        LocalDate actualArrivalDate = parseDate(body.get("actualArrivalDate"));
        latentInfectionService.track(id, status, remark, actualArrivalDate);
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
    @OperationLog(type = "update", module = "latent", action = "潜伏感染转诊操作")
    public ResultResponse<Void> referral(@RequestBody Map<String, Object> body) {
        if (body.get("id") == null || body.get("result") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少必要参数 id 或 result");
        }
        Long id = Long.valueOf(body.get("id").toString());
        String result = body.get("result").toString();
        String remark = body.getOrDefault("remark", "").toString();
        LocalDate actualReferralDate = parseDate(body.get("actualReferralDate"));
        latentInfectionService.referral(id, result, remark, actualReferralDate);
        return ResultRes.success(null);
    }

    private LocalDate parseDate(Object val) {
        return FlexibleDateParseUtil.parse(val);
    }

    // ==================== 预防治疗管理 ====================

    @Operation(summary = "设置服药状态")
    @PostMapping("/medication-status")
    @OperationLog(type = "update", module = "latent", action = "设置潜伏感染服药状态")
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
    @OperationLog(type = "update", module = "latent", action = "潜伏感染结案归档")
    public ResultResponse<Void> closeCase(@PathVariable Long id) {
        latentInfectionService.closeCase(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "解锁结案归档的潜伏感染者（管理员）")
    @PostMapping("/unarchive/{id}")
    @OperationLog(type = "update", module = "latent", action = "解锁结案归档潜伏感染者")
    public ResultResponse<Void> unarchiveFromCloseCase(@PathVariable Long id) {
        Integer role = BaseContext.getCurrentRole();
        if (role == null || role == 6) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无权限解锁潜伏感染者档案");
        }
        latentInfectionService.unarchiveFromCloseCase(id);
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
    @OperationLog(type = "update", module = "latent", action = "新增潜伏感染电话随访")
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
    @OperationLog(type = "update", module = "latent", action = "新增潜伏感染按期检查")
    public ResultResponse<Void> saveCheck(@RequestBody LatentCheck check) {
        latentCheckService.save(check);
        return ResultRes.success(null);
    }

    // ==================== 服药管理 / 领药 ====================

    @Operation(summary = "保存潜伏感染者服药管理")
    @PostMapping("/medication/save")
    @OperationLog(type = "update", module = "latent", action = "保存潜伏感染服药管理")
    public ResultResponse<Void> saveMedication(@RequestBody MedicationManagement medication) {
        prepareLatentMedication(medication);
        medicationManagementService.saveOrUpdate(medication);
        return ResultRes.success(null);
    }

    @Operation(summary = "查询潜伏感染者服药管理")
    @GetMapping("/medication/{latentInfectionId}")
    public ResultResponse<MedicationManagement> getMedication(@PathVariable Long latentInfectionId) {
        LambdaQueryWrapper<MedicationManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationManagement::getLatentInfectionId, latentInfectionId)
                .orderByDesc(MedicationManagement::getCreateTime)
                .last("LIMIT 1");
        return ResultRes.success(medicationManagementService.getOne(wrapper));
    }

    @Operation(summary = "完成潜伏感染者服药管理（归档）")
    @PostMapping("/medication/complete")
    @OperationLog(type = "update", module = "latent", action = "完成潜伏感染服药管理")
    public ResultResponse<Void> completeMedication(@RequestBody MedicationManagement medication) {
        prepareLatentMedication(medication);
        medicationManagementService.saveOrUpdate(medication);
        if (medication.getStopDate() != null) {
            latentInfectionService.closeCase(medication.getLatentInfectionId());
        }
        return ResultRes.success(null);
    }

    @Operation(summary = "保存潜伏感染者领药记录")
    @PostMapping("/medication-pickup/save")
    @OperationLog(type = "update", module = "latent", action = "保存潜伏感染领药记录")
    public ResultResponse<Void> saveMedicationPickup(@RequestBody MedicationPickup pickup) {
        userService.checkAnyPermissionCode(MEDICATION_PICKUP_WRITE_PERMISSIONS);
        if (pickup.getLatentInfectionId() != null) {
            latentInfectionService.assertLatentOperable(pickup.getLatentInfectionId());
        }
        medicationPickupService.saveLatentPickup(pickup);
        return ResultRes.success(null);
    }

    @Operation(summary = "潜伏感染者领药记录列表")
    @GetMapping("/medication-pickup/list/{latentInfectionId}")
    public ResultResponse<List<MedicationPickup>> listMedicationPickup(@PathVariable Long latentInfectionId) {
        userService.checkAnyPermissionCode(MEDICATION_PICKUP_READ_PERMISSIONS);
        return ResultRes.success(medicationPickupService.listByLatentInfectionId(latentInfectionId));
    }

    @Operation(summary = "删除潜伏感染者领药记录")
    @DeleteMapping("/medication-pickup/{id}")
    @OperationLog(type = "delete", module = "latent", action = "删除潜伏感染领药记录")
    public ResultResponse<Void> deleteMedicationPickup(@PathVariable Long id) {
        userService.checkAnyPermissionCode(MEDICATION_PICKUP_WRITE_PERMISSIONS);
        medicationPickupService.deleteLatentPickup(id);
        return ResultRes.success(null);
    }

    /** 校验归属、补齐人群类型，并确保不会误绑患者记录 */
    private void prepareLatentMedication(MedicationManagement medication) {
        if (medication.getLatentInfectionId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少潜伏感染者ID");
        }
        latentInfectionService.assertLatentOperable(medication.getLatentInfectionId());
        if (medication.getId() != null) {
            MedicationManagement existing = medicationManagementService.getById(medication.getId());
            if (existing == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "服药管理记录不存在");
            }
            if (existing.getLatentInfectionId() == null
                    || !existing.getLatentInfectionId().equals(medication.getLatentInfectionId())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "潜伏感染者与服药管理记录不匹配");
            }
        }
        if (StrUtil.isBlank(medication.getPopulationType())) {
            LatentInfection latent = latentInfectionService.getById(medication.getLatentInfectionId());
            if (latent != null) {
                medication.setPopulationType(latent.getPopulationType());
            }
        }
        if (StrUtil.isBlank(medication.getPopulationType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少人群类型");
        }
        medication.setPatientId(null);
    }
}
