package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Notice;
import cn.luyou.model.vo.SentNoticeVO;
import cn.luyou.service.NoticeService;
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

    @Operation(summary = "发送通知单")
    @PostMapping("/send")
    public ResultResponse<Void> send(@RequestBody Notice notice) {
        noticeService.send(notice);
        return ResultRes.success(null);
    }

    @Operation(summary = "保存通知单草稿（填写但不发送）")
    @PostMapping("/draft")
    public ResultResponse<Void> saveDraft(@RequestBody Notice notice) {
        noticeService.saveAsDraft(notice);
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

    @Operation(summary = "查询业务关联的通知单列表（含下发人/接收人名称）")
    @GetMapping("/list")
    public ResultResponse<List<Notice>> listByBiz(
            @RequestParam Long bizId,
            @RequestParam String noticeType) {
        return ResultRes.success(noticeService.listByBizWithUsers(bizId, noticeType));
    }

    @Operation(summary = "查询当前用户已发送的通知单列表（含发送者/接收者信息）")
    @GetMapping("/sent")
    public ResultResponse<IPage<SentNoticeVO>> sent(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int size) {
        Long senderId = BaseContext.getCurrentId();
        return ResultRes.success(noticeService.sentPage(senderId, pageNum, size));
    }

    @Operation(summary = "催促接收方接收通知单")
    @PostMapping("/remind/{id}")
    public ResultResponse<Void> remind(@PathVariable Long id) {
        noticeService.remind(id);
        return ResultRes.success(null);
    }
}
