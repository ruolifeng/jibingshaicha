package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.*;
import cn.luyou.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "首页仪表盘")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LatentInfectionService latentInfectionService;
    private final PatientService patientService;
    private final NoticeService noticeService;
    private final ScreeningCloseContactService closeContactService;
    private final ScreeningSchoolService screeningSchoolService;
    private final ScreeningKeyPopulationService screeningKeyPopulationService;
    private final ReferralService referralService;

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

        // 在管患者：未归档的患者数
        long pendingVisit = patientService.count(
                new LambdaQueryWrapper<Patient>().eq(Patient::getArchived, 0)
        );
        data.put("pendingVisit", pendingVisit);

        // 待确认通知单：状态为已发送（1）的通知单数
        long pendingNotice = noticeService.count(
                new LambdaQueryWrapper<Notice>().eq(Notice::getStatus, 1)
        );
        data.put("pendingNotice", pendingNotice);

        // 近期复查（15天内）：密接人群中登记时间在 165~195 天前，即将需要6月随访复查的人数
        LocalDate today = LocalDate.now();
        long upcomingReview = closeContactService.count(
                new LambdaQueryWrapper<ScreeningCloseContact>()
                        .isNotNull(ScreeningCloseContact::getRegistrationDate)
                        .ge(ScreeningCloseContact::getRegistrationDate, today.minusDays(195))
                        .le(ScreeningCloseContact::getRegistrationDate, today.minusDays(165))
        );
        data.put("upcomingReview", upcomingReview);

        return ResultRes.success(data);
    }

    @Operation(summary = "获取所有上传批次（任务）列表")
    @GetMapping("/batches")
    public ResultResponse<List<String>> batches() {
        Set<String> batchSet = new LinkedHashSet<>();

        screeningSchoolService.listObjs(
                new LambdaQueryWrapper<ScreeningSchool>()
                        .select(ScreeningSchool::getUploadBatch)
                        .isNotNull(ScreeningSchool::getUploadBatch)
                        .groupBy(ScreeningSchool::getUploadBatch)
        ).stream()
                .map(Object::toString)
                .filter(StringUtils::hasText)
                .forEach(batchSet::add);

        screeningKeyPopulationService.listObjs(
                new LambdaQueryWrapper<ScreeningKeyPopulation>()
                        .select(ScreeningKeyPopulation::getUploadBatch)
                        .isNotNull(ScreeningKeyPopulation::getUploadBatch)
                        .groupBy(ScreeningKeyPopulation::getUploadBatch)
        ).stream()
                .map(Object::toString)
                .filter(StringUtils::hasText)
                .forEach(batchSet::add);

        closeContactService.listObjs(
                new LambdaQueryWrapper<ScreeningCloseContact>()
                        .select(ScreeningCloseContact::getUploadBatch)
                        .isNotNull(ScreeningCloseContact::getUploadBatch)
                        .groupBy(ScreeningCloseContact::getUploadBatch)
        ).stream()
                .map(Object::toString)
                .filter(StringUtils::hasText)
                .forEach(batchSet::add);

        return ResultRes.success(new ArrayList<>(batchSet));
    }

    @Operation(summary = "按任务（上传批次）获取三类人群数据统计")
    @GetMapping("/task-stats")
    public ResultResponse<Map<String, Object>> taskStats(
            @RequestParam(required = false) String batch) {
        Map<String, Object> data = new HashMap<>();
        boolean hasBatch = StringUtils.hasText(batch);

        // ===== 学校人群 =====
        long schoolTotal = screeningSchoolService.count(
                hasBatch ? new LambdaQueryWrapper<ScreeningSchool>()
                        .eq(ScreeningSchool::getUploadBatch, batch)
                        : new LambdaQueryWrapper<>()
        );
        List<Object> schoolIds = hasBatch
                ? screeningSchoolService.listObjs(new LambdaQueryWrapper<ScreeningSchool>()
                        .select(ScreeningSchool::getId)
                        .eq(ScreeningSchool::getUploadBatch, batch))
                : null;
        data.put("school", buildPopStats(schoolTotal,
                countLatent("school", hasBatch, schoolIds),
                countPatient("school", hasBatch, schoolIds)));

        // ===== 重点人群 =====
        long keyTotal = screeningKeyPopulationService.count(
                hasBatch ? new LambdaQueryWrapper<ScreeningKeyPopulation>()
                        .eq(ScreeningKeyPopulation::getUploadBatch, batch)
                        : new LambdaQueryWrapper<>()
        );
        List<Object> keyIds = hasBatch
                ? screeningKeyPopulationService.listObjs(new LambdaQueryWrapper<ScreeningKeyPopulation>()
                        .select(ScreeningKeyPopulation::getId)
                        .eq(ScreeningKeyPopulation::getUploadBatch, batch))
                : null;
        data.put("keyPopulation", buildPopStats(keyTotal,
                countLatent("keyPopulation", hasBatch, keyIds),
                countPatient("keyPopulation", hasBatch, keyIds)));

        // ===== 密接人群 =====
        long closeTotal = closeContactService.count(
                hasBatch ? new LambdaQueryWrapper<ScreeningCloseContact>()
                        .eq(ScreeningCloseContact::getUploadBatch, batch)
                        : new LambdaQueryWrapper<>()
        );
        List<Object> closeIds = hasBatch
                ? closeContactService.listObjs(new LambdaQueryWrapper<ScreeningCloseContact>()
                        .select(ScreeningCloseContact::getId)
                        .eq(ScreeningCloseContact::getUploadBatch, batch))
                : null;
        data.put("closeContact", buildPopStats(closeTotal,
                countLatent("closeContact", hasBatch, closeIds),
                countPatient("closeContact", hasBatch, closeIds)));

        return ResultRes.success(data);
    }

    @Operation(summary = "获取消息通知统计（通知单 + 分级诊疗）")
    @GetMapping("/message-stats")
    public ResultResponse<Map<String, Object>> messageStats() {
        Map<String, Object> data = new HashMap<>();

        // 潜伏感染者通知单
        data.put("latentNoticeSent", noticeService.count(
                new LambdaQueryWrapper<Notice>().eq(Notice::getNoticeType, "latent")));
        data.put("latentNoticeConfirmed", noticeService.count(
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getNoticeType, "latent").eq(Notice::getStatus, 2)));

        // 患者通知单
        data.put("patientNoticeSent", noticeService.count(
                new LambdaQueryWrapper<Notice>().eq(Notice::getNoticeType, "patient")));
        data.put("patientNoticeConfirmed", noticeService.count(
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getNoticeType, "patient").eq(Notice::getStatus, 2)));

        // 分级诊疗
        data.put("referralSent", referralService.count(new LambdaQueryWrapper<>()));
        data.put("referralConfirmed", referralService.count(
                new LambdaQueryWrapper<Referral>().eq(Referral::getStatus, 2)));
        data.put("referralRejected", referralService.count(
                new LambdaQueryWrapper<Referral>().eq(Referral::getStatus, 3)));

        return ResultRes.success(data);
    }

    // ===== 内部辅助方法 =====

    private long countLatent(String populationType, boolean hasBatch, List<Object> ids) {
        if (hasBatch && (ids == null || ids.isEmpty())) return 0L;
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .eq(LatentInfection::getPopulationType, populationType);
        if (hasBatch) {
            wrapper.in(LatentInfection::getScreeningId, ids);
        }
        return latentInfectionService.count(wrapper);
    }

    private long countPatient(String populationType, boolean hasBatch, List<Object> ids) {
        if (hasBatch && (ids == null || ids.isEmpty())) return 0L;
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<Patient>()
                .eq(Patient::getPopulationType, populationType);
        if (hasBatch) {
            wrapper.in(Patient::getScreeningId, ids);
        }
        return patientService.count(wrapper);
    }

    private Map<String, Object> buildPopStats(long total, long latent, long patient) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("screeningTotal", total);
        stat.put("latentCount", latent);
        stat.put("latentRatio", total > 0 ? Math.round(latent * 1000.0 / total) / 10.0 : 0.0);
        stat.put("patientCount", patient);
        stat.put("patientRatio", total > 0 ? Math.round(patient * 1000.0 / total) / 10.0 : 0.0);
        return stat;
    }
}
