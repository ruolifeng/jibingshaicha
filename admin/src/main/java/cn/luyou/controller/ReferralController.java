package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Referral;
import cn.luyou.model.vo.ReferralDetailVO;
import cn.luyou.model.vo.SentReferralVO;
import cn.luyou.service.ReferralService;
import cn.luyou.utils.FlexibleDateParseUtil;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "分级诊疗管理")
@RestController
@RequestMapping("/referral")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @Operation(summary = "发起分级诊疗推送")
    @PostMapping("/send")
    public ResultResponse<Void> send(@RequestBody Referral referral) {
        referralService.send(referral);
        return ResultRes.success(null);
    }

    @Operation(summary = "接收方确认接收")
    @PostMapping("/confirm/{id}")
    public ResultResponse<Void> confirm(@PathVariable Long id,
                                        @RequestBody(required = false) Map<String, String> body) {
        LocalDate actualReferralDate = parseDate(body != null ? body.get("actualReferralDate") : null);
        referralService.confirm(id, actualReferralDate);
        return ResultRes.success(null);
    }

    private LocalDate parseDate(Object val) {
        return FlexibleDateParseUtil.parse(val);
    }

    @Operation(summary = "接收方拒绝")
    @PostMapping("/reject/{id}")
    public ResultResponse<Void> reject(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("rejectReason") : null;
        referralService.reject(id, reason);
        return ResultRes.success(null);
    }

    @Operation(summary = "发送方重新发起（仅拒绝状态可用）")
    @PostMapping("/resend/{id}")
    public ResultResponse<Void> resend(@PathVariable Long id) {
        referralService.resend(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "查询转诊详情（含发送方/接收方信息）")
    @GetMapping("/{id}")
    public ResultResponse<ReferralDetailVO> detail(@PathVariable Long id) {
        return ResultRes.success(referralService.detail(id));
    }

    @Operation(summary = "查询业务记录关联的分级诊疗列表")
    @GetMapping("/list")
    public ResultResponse<List<Referral>> listByBiz(
            @RequestParam Long bizId,
            @RequestParam String bizType) {
        return ResultRes.success(referralService.listByBiz(bizId, bizType));
    }

    @Operation(summary = "当前用户已发送的分级诊疗分页列表")
    @GetMapping("/sent")
    public ResultResponse<IPage<SentReferralVO>> sent(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int size) {
        Long senderId = BaseContext.getCurrentId();
        return ResultRes.success(referralService.sentPage(senderId, pageNum, size));
    }
}
