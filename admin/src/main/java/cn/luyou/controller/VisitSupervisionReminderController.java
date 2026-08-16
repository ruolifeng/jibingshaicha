package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.vo.UpcomingVisitSupervisionVO;
import cn.luyou.model.vo.VisitSupervisionDispatchResultVO;
import cn.luyou.service.UserService;
import cn.luyou.service.VisitSupervisionReminderService;
import cn.luyou.utils.DepartmentFilterSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "随访/督导到期提醒")
@RestController
@RequestMapping("/reminder/visit-supervision")
@RequiredArgsConstructor
public class VisitSupervisionReminderController {

    private final VisitSupervisionReminderService visitSupervisionReminderService;
    private final DepartmentFilterSupport departmentFilterSupport;
    private final UserService userService;

    @Operation(summary = "首页：距下次随访/督导 7/3/1 天的提醒列表")
    @GetMapping("/upcoming")
    public ResultResponse<List<UpcomingVisitSupervisionVO>> upcoming(
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        return ResultRes.success(visitSupervisionReminderService.listUpcoming(filterDeptIds));
    }

    @Operation(summary = "扫描并发送站内随访/督导到期提醒（定时任务同逻辑，可手动触发）")
    @PostMapping("/dispatch")
    @OperationLog(type = "update", module = "system", action = "发送随访督导到期提醒")
    public ResultResponse<VisitSupervisionDispatchResultVO> dispatch() {
        userService.checkPermission(1);
        return ResultRes.success(visitSupervisionReminderService.dispatchMessages());
    }

    @Operation(summary = "短信接口：对距下次随访/督导 7/3/1 天的对象发送短信提醒")
    @PostMapping("/sms")
    @OperationLog(type = "update", module = "system", action = "发送随访督导到期短信")
    public ResultResponse<VisitSupervisionDispatchResultVO> sms() {
        userService.checkPermission(1);
        return ResultRes.success(visitSupervisionReminderService.dispatchSms());
    }
}
