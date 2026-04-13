package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Notice;
import cn.luyou.service.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Operation(summary = "发送通知单")
    @PostMapping("/send")
    public ResultResponse<Void> send(@RequestBody Notice notice) {
        noticeService.send(notice);
        return ResultRes.success(null);
    }

    @Operation(summary = "确认接收通知单")
    @PostMapping("/confirm/{id}")
    public ResultResponse<Void> confirm(@PathVariable Long id) {
        noticeService.confirm(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "通知单详情")
    @GetMapping("/detail/{id}")
    public ResultResponse<Notice> detail(@PathVariable Long id) {
        return ResultRes.success(noticeService.getById(id));
    }

    @Operation(summary = "查询业务关联的通知单列表")
    @GetMapping("/list")
    public ResultResponse<List<Notice>> listByBiz(
            @RequestParam Long bizId,
            @RequestParam String noticeType) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getBizId, bizId)
                .eq(Notice::getNoticeType, noticeType)
                .orderByDesc(Notice::getCreateTime);
        return ResultRes.success(noticeService.list(wrapper));
    }
}
