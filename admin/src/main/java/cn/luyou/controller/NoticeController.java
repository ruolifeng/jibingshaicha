package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Notice;
import cn.luyou.model.vo.SentNoticeVO;
import cn.luyou.model.vo.UpdateNoticeCultureResistanceDTO;
import cn.luyou.model.vo.UpdateNoticeRegistrationNoDTO;
import cn.luyou.model.vo.UserInfoVO;
import cn.luyou.service.NoticeService;
import cn.luyou.service.UserService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "通知单管理")
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final UserService userService;

    private void assertPatientNoticeFill(Notice notice) {
        if (notice != null && "patient".equals(notice.getNoticeType())) {
            userService.checkPermissionCode("patientManagement:notice:fill");
        }
    }

    private void assertPatientNoticeAccess(Notice notice) {
        if (notice != null && "patient".equals(notice.getNoticeType())) {
            userService.checkPermissionCode("patientManagement:notice");
        }
    }

    @Operation(summary = "发送通知单")
    @PostMapping("/send")
    @OperationLog(type = "create", module = "patient", action = "发送通知单")
    public ResultResponse<Void> send(@RequestBody Notice notice) {
        assertPatientNoticeFill(notice);
        noticeService.send(notice);
        return ResultRes.success(null);
    }

    @Operation(summary = "保存通知单草稿（填写但不发送）")
    @PostMapping("/draft")
    @OperationLog(type = "update", module = "patient", action = "保存通知单草稿")
    public ResultResponse<Void> saveDraft(@RequestBody Notice notice) {
        assertPatientNoticeFill(notice);
        noticeService.saveAsDraft(notice);
        return ResultRes.success(null);
    }

    @Operation(summary = "确认接收通知单")
    @PostMapping("/confirm/{id}")
    @OperationLog(type = "update", module = "patient", action = "确认接收通知单")
    public ResultResponse<Void> confirm(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        assertPatientNoticeAccess(notice);
        noticeService.confirm(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "通知单详情")
    @GetMapping("/detail/{id}")
    public ResultResponse<Notice> detail(@PathVariable Long id) {
        return ResultRes.success(noticeService.getDetailWithUsers(id));
    }

    @Operation(summary = "查询业务关联的通知单列表（含下发人/接收人名称）")
    @GetMapping("/list")
    public ResultResponse<List<Notice>> listByBiz(
            @RequestParam Long bizId,
            @RequestParam String noticeType) {
        return ResultRes.success(noticeService.listByBizWithUsers(bizId, noticeType));
    }

    @Operation(summary = "查询已发送的通知单列表（五级仅本人发送，上级按辖区；含发送者/接收者信息）")
    @GetMapping("/sent")
    public ResultResponse<IPage<SentNoticeVO>> sent(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int size) {
        Long senderId = BaseContext.getCurrentId();
        return ResultRes.success(noticeService.sentPage(senderId, pageNum, size));
    }

    @Operation(summary = "催促接收方接收通知单")
    @PostMapping("/remind/{id}")
    @OperationLog(type = "update", module = "patient", action = "催促接收通知单")
    public ResultResponse<Void> remind(@PathVariable Long id) {
        assertPatientNoticeFill(noticeService.getById(id));
        noticeService.remind(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "本区县三级用户（患者通知单培养/耐药变更通知对象）")
    @GetMapping("/{id}/district-level3-users")
    public ResultResponse<List<UserInfoVO>> districtLevel3Users(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        assertPatientNoticeFill(notice);
        return ResultRes.success(noticeService.listDistrictLevel3Users(id));
    }

    @Operation(summary = "修改患者通知单痰培养、耐药情况、治疗方案、分子/病理检查，并同步首次随访")
    @PostMapping("/{id}/culture-resistance")
    @OperationLog(type = "update", module = "patient", action = "修改通知单痰培养耐药治疗方案及分子病理")
    public ResultResponse<Void> updateCultureResistance(
            @PathVariable Long id,
            @RequestBody UpdateNoticeCultureResistanceDTO dto) {
        Notice notice = noticeService.getById(id);
        assertPatientNoticeFill(notice);
        noticeService.updateCultureAndResistance(id, dto);
        return ResultRes.success(null);
    }

    @Operation(summary = "修改潜伏感染者通知单登记号，并同步潜伏感染主表")
    @PostMapping("/{id}/registration-no")
    @OperationLog(type = "update", module = "latent", action = "修改通知单登记号")
    public ResultResponse<Void> updateRegistrationNo(
            @PathVariable Long id,
            @RequestBody UpdateNoticeRegistrationNoDTO dto) {
        // 先校验模块权限，再由 Service 校验通知单类型与角色
        userService.checkPermissionCode("latentManagement:notice");
        noticeService.updateRegistrationNo(id, dto);
        return ResultRes.success(null);
    }
}
