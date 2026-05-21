package cn.luyou.controller;

import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.SupervisionForm;
import cn.luyou.service.SupervisionFormService;
import cn.hutool.core.util.StrUtil;
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

    @Operation(summary = "保存督导表（status=1 提交，status=2 归档）")
    @PostMapping("/save")
    public ResultResponse<Void> save(@RequestBody SupervisionForm form) {
        validateRequired(form);
        if (Integer.valueOf(2).equals(form.getStatus())) {
            supervisionFormService.saveAndArchive(form);
        } else {
            supervisionFormService.saveSubmit(form);
        }
        return ResultRes.success(null);
    }

    private void validateRequired(SupervisionForm form) {
        if (form.getLatentInfectionId() == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少关联潜伏感染ID");
        }
        if (StrUtil.isBlank(form.getPopulationType())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "缺少人群类型");
        }
        // 归档时校验必填项；草稿允许部分填写
        if (Integer.valueOf(2).equals(form.getStatus())) {
            if (StrUtil.isBlank(form.getTreatmentPlan())) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择治疗方案");
            }
            if (form.getTreatmentStartDate() == null) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "请选择开始治疗时间");
            }
        }
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
