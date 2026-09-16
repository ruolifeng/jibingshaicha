package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.SysMessageReminderConfig;
import cn.luyou.service.SysMessageReminderConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "消息提醒配置")
@RestController
@RequestMapping("/message/reminders")
@RequiredArgsConstructor
public class SysMessageReminderConfigController {

    private final SysMessageReminderConfigService reminderConfigService;

    @Operation(summary = "提醒配置列表")
    @GetMapping
    public ResultResponse<List<SysMessageReminderConfig>> list() {
        return ResultRes.success(reminderConfigService.listAll());
    }

    @Operation(summary = "更新提醒开关")
    @PutMapping("/{code}/enabled")
    public ResultResponse<Void> updateEnabled(@PathVariable String code,
                                              @RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("enabled");
        boolean enabled = raw instanceof Boolean b ? b
                : raw != null && ("1".equals(raw.toString()) || "true".equalsIgnoreCase(raw.toString()));
        reminderConfigService.updateEnabled(code, enabled);
        return ResultRes.success(null);
    }
}
