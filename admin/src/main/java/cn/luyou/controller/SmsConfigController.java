package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.vo.SmsConfigVO;
import cn.luyou.service.SmsService;
import cn.luyou.service.SysSmsConfigService;
import cn.luyou.service.UserService;
import cn.luyou.utils.BaseContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "短信配置")
@RestController
@RequestMapping("/sms-config")
@RequiredArgsConstructor
public class SmsConfigController {

    private final SysSmsConfigService smsConfigService;
    private final SmsService smsService;
    private final UserService userService;

    @Operation(summary = "获取短信配置")
    @GetMapping
    public ResultResponse<SmsConfigVO> get() {
        smsConfigService.assertSuperAdmin();
        return ResultRes.success(smsConfigService.getConfig());
    }

    @Operation(summary = "保存短信配置")
    @PutMapping
    @OperationLog(type = "update", module = "system", action = "保存短信配置")
    public ResultResponse<Void> save(@RequestBody Map<String, Object> body) {
        smsConfigService.assertSuperAdmin();
        smsConfigService.saveConfig(body);
        return ResultRes.success(null);
    }

    @Operation(summary = "发送测试短信")
    @PostMapping("/test")
    @OperationLog(type = "update", module = "system", action = "发送测试短信")
    public ResultResponse<String> test(@RequestBody(required = false) Map<String, Object> body) {
        smsConfigService.assertSuperAdmin();
        if (body == null) {
            body = Map.of();
        }
        String phone = body.get("phone") == null ? null : String.valueOf(body.get("phone")).trim();
        String message = body.get("message") == null ? null : String.valueOf(body.get("message")).trim();
        if (phone == null || phone.isEmpty()) {
            // 默认发给当前超管联系电话
            var me = userService.getById(BaseContext.getCurrentId());
            phone = me != null ? me.getPhone() : null;
        }
        return ResultRes.success(smsService.sendTestSms(phone, message));
    }
}
