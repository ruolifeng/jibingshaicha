package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.NoticeService;
import cn.luyou.service.PatientService;
import cn.luyou.service.ScreeningCloseContactService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "首页仪表盘")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LatentInfectionService latentInfectionService;
    private final PatientService patientService;
    private final NoticeService noticeService;
    private final ScreeningCloseContactService closeContactService;

    @Operation(summary = "获取待处理事项汇总")
    @GetMapping("/summary")
    public ResultResponse<Map<String, Object>> summary() {
        Map<String, Object> data = new HashMap<>();

        // 待追踪：潜伏感染、追踪状态为0（待追踪）、未归档
        long pendingTracking = latentInfectionService.count(
                new LambdaQueryWrapper<LatentInfection>()
                        .eq(LatentInfection::getTrackingStatus, 0)
                        .eq(LatentInfection::getArchived, 0)
        );
        data.put("pendingTracking", pendingTracking);

        // 待随访：患者通知单已确认（status=2）但未归档的患者数
        long pendingVisit = patientService.count(
                new LambdaQueryWrapper<Patient>()
                        .eq(Patient::getArchived, 0)
        );
        data.put("pendingVisit", pendingVisit);

        // 待确认通知单：状态为已发送（1）的通知单数
        long pendingNotice = noticeService.count(
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, 1)
        );
        data.put("pendingNotice", pendingNotice);

        // 近期复查（15天内）：密接人群中需要复查的人数
        LocalDate today = LocalDate.now();
        long upcomingReview = closeContactService.count(
                new LambdaQueryWrapper<ScreeningCloseContact>()
                        .eq(ScreeningCloseContact::getIsLatent, 0)
                        .isNotNull(ScreeningCloseContact::getFirstScreenDate)
                        .ge(ScreeningCloseContact::getFirstScreenDate, today.minusDays(195))
                        .le(ScreeningCloseContact::getFirstScreenDate, today.minusDays(165))
        );
        data.put("upcomingReview", upcomingReview);

        // 各类人群筛查总数
        data.put("totalSchool", latentInfectionService.count(
                new LambdaQueryWrapper<LatentInfection>().eq(LatentInfection::getPopulationType, "school")));
        data.put("totalKeyPopulation", latentInfectionService.count(
                new LambdaQueryWrapper<LatentInfection>().eq(LatentInfection::getPopulationType, "keyPopulation")));
        data.put("totalCloseContact", latentInfectionService.count(
                new LambdaQueryWrapper<LatentInfection>().eq(LatentInfection::getPopulationType, "closeContact")));

        return ResultRes.success(data);
    }
}
