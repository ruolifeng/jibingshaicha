package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.SysMessage;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统消息")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class SysMessageController {

    private final SysMessageService sysMessageService;

    @Operation(summary = "查询当前用户消息列表")
    @GetMapping("/list")
    public ResultResponse<IPage<SysMessage>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer isRead) {
        Long userId = BaseContext.getCurrentId();
        return ResultRes.success(sysMessageService.queryPage(userId, page, size, isRead));
    }

    @Operation(summary = "标记消息已读")
    @PostMapping("/read/{id}")
    public ResultResponse<Void> markRead(@PathVariable Long id) {
        sysMessageService.markRead(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "获取未读消息数")
    @GetMapping("/unread-count")
    public ResultResponse<Long> unreadCount() {
        Long userId = BaseContext.getCurrentId();
        return ResultRes.success(sysMessageService.getUnreadCount(userId));
    }
}
