package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ReferralTracking;
import cn.luyou.service.ReferralTrackingService;
import cn.luyou.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "推介追踪管理")
@RestController
@RequestMapping("/referral-tracking")
@RequiredArgsConstructor
public class ReferralTrackingController {

    private final ReferralTrackingService referralTrackingService;
    private final UserService userService;

    @Operation(summary = "分页查询推介/追踪记录")
    @GetMapping("/list")
    public ResultResponse<IPage<ReferralTracking>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String bizMode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) Integer trackingStatus,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String township,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String creatorOrEntryUnit) {
        return ResultRes.success(referralTrackingService.queryPage(
                page, size, bizMode, name, idNumber, trackingStatus, archived,
                phone, township, dateFrom, dateTo, sourceType, creatorOrEntryUnit));
    }

    @Operation(summary = "查询推介/追踪记录详情")
    @GetMapping("/{id}")
    public ResultResponse<ReferralTracking> detail(@PathVariable Long id) {
        return ResultRes.success(referralTrackingService.getDetail(id));
    }

    @OperationLog(type = "import", module = "referral", action = "大疫情导入追踪记录")
    @Operation(summary = "大疫情表导入（追踪模块）")
    @PostMapping("/import-epidemic")
    public ResultResponse<Map<String, Object>> importEpidemic(@RequestParam("file") MultipartFile file) {
        userService.checkPermissionCode("referralManagement:epidemicImport");
        return ResultRes.success(referralTrackingService.importEpidemic(file));
    }

    @OperationLog(type = "export", module = "referral", action = "导出追踪记录")
    @Operation(summary = "导出追踪记录")
    @GetMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String bizMode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String township,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String creatorOrEntryUnit) {
        userService.checkPermissionCode("referralManagement:export");
        referralTrackingService.exportTrack(response, bizMode, name, idNumber, phone, township,
                dateFrom, dateTo, sourceType, creatorOrEntryUnit);
    }

    @OperationLog(type = "create", module = "referral", action = "新增推介/追踪记录")
    @Operation(summary = "新增推介或追踪记录（bizMode: recommend/track）")
    @PostMapping
    public ResultResponse<ReferralTracking> create(@RequestBody Map<String, Object> body) {
        userService.checkPermissionCode("referralManagement:create");
        return ResultRes.success(referralTrackingService.create(body));
    }

    @OperationLog(type = "update", module = "referral", action = "更新推介/追踪基本信息")
    @Operation(summary = "更新基本信息")
    @PutMapping("/{id}")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        referralTrackingService.update(id, body);
        return ResultRes.success(null);
    }

    @OperationLog(type = "update", module = "referral", action = "发送推介通知")
    @Operation(summary = "发送推介通知（bizMode=recommend）")
    @PostMapping("/{id}/send")
    public ResultResponse<Void> sendRecommend(@PathVariable Long id) {
        referralTrackingService.sendRecommend(id);
        return ResultRes.success(null);
    }

    @OperationLog(type = "update", module = "referral", action = "确认接受推介通知单")
    @Operation(summary = "接收方确认接受推介通知单")
    @PostMapping("/{id}/confirm")
    public ResultResponse<Void> confirmRecommend(@PathVariable Long id) {
        referralTrackingService.confirmRecommend(id);
        return ResultRes.success(null);
    }

    @OperationLog(type = "update", module = "referral", action = "拒绝推介通知单")
    @Operation(summary = "接收方拒绝推介通知单")
    @PostMapping("/{id}/reject")
    public ResultResponse<Void> rejectRecommend(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        String reason = body != null ? (String) body.get("reason") : null;
        referralTrackingService.rejectRecommend(id, reason);
        return ResultRes.success(null);
    }

    @OperationLog(type = "update", module = "referral", action = "开启共同追踪")
    @Operation(summary = "接收方开启共同追踪（发起方与接收方均可追踪）")
    @PostMapping("/{id}/joint-tracking")
    public ResultResponse<Void> enableJointTracking(@PathVariable Long id) {
        referralTrackingService.enableJointTracking(id);
        return ResultRes.success(null);
    }

    @OperationLog(type = "update", module = "referral", action = "操作追踪状态")
    @Operation(summary = "追踪操作（status: 1到位 2未到位 3其他）")
    @PostMapping("/{id}/track")
    public ResultResponse<Void> track(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        if (body.get("status") == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少 status 参数");
        }
        Integer status = Integer.valueOf(body.get("status").toString());
        String remark = body.getOrDefault("remark", "").toString();
        referralTrackingService.track(id, status, remark);
        return ResultRes.success(null);
    }

    @OperationLog(type = "update", module = "referral", action = "保存筛查信息")
    @Operation(summary = "保存到位后的感染筛查+胸片信息")
    @PostMapping("/{id}/screening")
    public ResultResponse<Void> saveScreening(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body) {
        referralTrackingService.saveScreening(id, body);
        return ResultRes.success(null);
    }

    @OperationLog(type = "update", module = "referral", action = "保存诊断结果")
    @Operation(summary = "保存诊断结果并分流（排除/确诊患者/潜伏感染者/其他；确诊患者仅标红结案）")
    @PostMapping("/{id}/diagnosis")
    public ResultResponse<Void> saveDiagnosis(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body) {
        String diagnosisResult = body != null ? (String) body.get("diagnosisResult") : null;
        String diagnosisRemark = body != null ? (String) body.get("diagnosisRemark") : null;
        referralTrackingService.saveDiagnosis(id, diagnosisResult, diagnosisRemark);
        return ResultRes.success(null);
    }

    @OperationLog(type = "delete", module = "referral", action = "删除推介/追踪记录")
    @Operation(summary = "删除推介/追踪记录（软删）")
    @DeleteMapping("/{id}")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        referralTrackingService.deleteRecord(id);
        return ResultRes.success(null);
    }
}
