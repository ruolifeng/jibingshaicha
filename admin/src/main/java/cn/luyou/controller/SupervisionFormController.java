package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.SupervisionForm;
import cn.luyou.service.SupervisionFormService;
import cn.luyou.service.UserService;
import cn.luyou.utils.BaseContext;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "督导表管理")
@RestController
@RequestMapping("/supervision")
@RequiredArgsConstructor
public class SupervisionFormController {

    private final SupervisionFormService supervisionFormService;
    private final UserService userService;

    @Operation(summary = "查询督导表草稿")
    @GetMapping("/draft/{latentInfectionId}")
    public ResultResponse<SupervisionForm> getDraft(@PathVariable Long latentInfectionId) {
        return ResultRes.success(supervisionFormService.getDraft(latentInfectionId));
    }

    @Operation(summary = "保存督导表草稿（部分填写即可）")
    @PostMapping("/draft")
    @OperationLog(type = "update", module = "latent", action = "保存督导表草稿")
    public ResultResponse<Void> saveDraft(@RequestBody SupervisionForm form) {
        validateLatentId(form);
        userService.checkPermissionCode("latentManagement:supervision:fill");
        supervisionFormService.saveDraft(form);
        return ResultRes.success(null);
    }

    @Operation(summary = "保存督导表（status=1 提交，status=2 归档）")
    @PostMapping("/save")
    @OperationLog(type = "update", module = "latent", action = "保存督导表")
    public ResultResponse<Void> save(@RequestBody SupervisionForm form) {
        validateRequired(form);
        assertSupervisionSavePermission(form);
        if (Integer.valueOf(2).equals(form.getStatus())) {
            supervisionFormService.saveAndArchive(form);
        } else {
            form.setStatus(1);
            supervisionFormService.saveSubmit(form);
        }
        return ResultRes.success(null);
    }

    @Operation(summary = "督导表记录列表（已提交/已归档）")
    @GetMapping("/list/{latentInfectionId}")
    public ResultResponse<List<SupervisionForm>> list(@PathVariable Long latentInfectionId) {
        Integer role = BaseContext.getCurrentRole();
        return ResultRes.success(supervisionFormService.listCompleted(latentInfectionId, role));
    }

    @Operation(summary = "按ID查询督导表详情")
    @GetMapping("/{id}")
    public ResultResponse<SupervisionForm> getById(@PathVariable Long id) {
        return ResultRes.success(supervisionFormService.getById(id));
    }

    @Operation(summary = "删除督导表记录")
    @DeleteMapping("/{id}")
    @OperationLog(type = "delete", module = "latent", action = "删除督导表")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        userService.checkPermissionCode("latentManagement:supervision:edit");
        supervisionFormService.deleteForm(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "查询最新督导表详情（兼容旧接口）")
    @GetMapping("/detail/{latentInfectionId}")
    public ResultResponse<SupervisionForm> detail(@PathVariable Long latentInfectionId) {
        LambdaQueryWrapper<SupervisionForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupervisionForm::getLatentInfectionId, latentInfectionId)
                .ge(SupervisionForm::getStatus, 1)
                .orderByDesc(SupervisionForm::getCreateTime)
                .last("LIMIT 1");
        return ResultRes.success(supervisionFormService.getOne(wrapper));
    }

    private void validateLatentId(SupervisionForm form) {
        if (form.getLatentInfectionId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少关联潜伏感染ID");
        }
    }

    /** 新建/草稿提交用 fill；修改已提交记录用 edit */
    private void assertSupervisionSavePermission(SupervisionForm form) {
        if (form.getId() == null) {
            userService.checkPermissionCode("latentManagement:supervision:fill");
            return;
        }
        SupervisionForm existing = supervisionFormService.getById(form.getId());
        if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            userService.checkPermissionCode("latentManagement:supervision:edit");
        } else {
            userService.checkPermissionCode("latentManagement:supervision:fill");
        }
    }

    private void validateRequired(SupervisionForm form) {
        validateLatentId(form);
        if (StrUtil.isBlank(form.getPopulationType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少人群类型");
        }
        // 归档时校验必填项；草稿/提交允许部分填写
        if (Integer.valueOf(2).equals(form.getStatus())) {
            if (StrUtil.isBlank(form.getTreatmentPlan())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择治疗方案");
            }
            if (form.getTreatmentStartDate() == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择开始治疗时间");
            }
        }
    }
}
