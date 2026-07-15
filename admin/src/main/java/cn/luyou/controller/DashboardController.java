package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.*;
import cn.luyou.service.*;
import cn.luyou.utils.DataScopeHelper;
import cn.luyou.utils.DepartmentFilterSupport;
import cn.luyou.utils.LatentScreeningLinkSupport;
import cn.luyou.utils.ScreeningScopeHelper;
import cn.luyou.utils.UploadBatchSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "首页仪表盘")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LatentInfectionService latentInfectionService;
    private final NoticeService noticeService;
    private final ScreeningCloseContactService closeContactService;
    private final ScreeningSchoolService screeningSchoolService;
    private final ScreeningKeyPopulationService screeningKeyPopulationService;
    private final ReferralService referralService;
    private final ReferralTrackingService referralTrackingService;
    private final WorkbenchStatisticsService workbenchStatisticsService;
    private final ScreeningScopeHelper screeningScopeHelper;
    private final DataScopeHelper dataScopeHelper;
    private final DepartmentFilterSupport departmentFilterSupport;

    @Operation(summary = "获取待处理事项汇总")
    @GetMapping("/summary")
    public ResultResponse<Map<String, Object>> summary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        Map<String, Object> data = new HashMap<>();

        data.put("pendingTracking", referralTrackingService.countPendingTrackingForDashboard(filterDeptIds));

        data.putAll(workbenchStatisticsService.buildSummary(year, filterDeptIds));

        LambdaQueryWrapper<Notice> pendingNoticeWrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1);
        dataScopeHelper.applyNoticeScope(pendingNoticeWrapper);
        dataScopeHelper.applyNoticeBizDepartmentFilter(pendingNoticeWrapper, filterDeptIds);
        data.put("pendingNotice", noticeService.count(pendingNoticeWrapper));

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<ScreeningCloseContact> reviewWrapper = scopedCloseContactWrapper(filterDeptIds)
                .isNotNull(ScreeningCloseContact::getRegistrationDate)
                .ge(ScreeningCloseContact::getRegistrationDate, today.minusDays(195))
                .le(ScreeningCloseContact::getRegistrationDate, today.minusDays(165));
        data.put("upcomingReview", closeContactService.count(reviewWrapper));

        return ResultRes.success(data);
    }

    @Operation(summary = "获取所有上传批次（任务）列表")
    @GetMapping("/batches")
    public ResultResponse<List<Map<String, String>>> batches(
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        Map<String, UploadBatchSupport.BatchMeta> metaMap = new LinkedHashMap<>();

        mergeSchoolBatchMeta(metaMap, filterDeptIds);
        mergeKeyPopulationBatchMeta(metaMap, filterDeptIds);
        mergeCloseContactBatchMeta(metaMap, filterDeptIds);

        List<Map<String, String>> result = metaMap.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<String, UploadBatchSupport.BatchMeta> e) ->
                                e.getValue().getUploadTime() != null
                                        ? e.getValue().getUploadTime()
                                        : LocalDateTime.MIN)
                        .reversed())
                .map(entry -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("value", entry.getKey());
                    item.put("label", entry.getValue().toLabel(entry.getKey()));
                    return item;
                })
                .collect(Collectors.toList());

        return ResultRes.success(result);
    }

    private void mergeSchoolBatchMeta(Map<String, UploadBatchSupport.BatchMeta> metaMap, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<ScreeningSchool>()
                .isNotNull(ScreeningSchool::getUploadBatch)
                .ne(ScreeningSchool::getUploadBatch, "")
                .select(ScreeningSchool::getUploadBatch, ScreeningSchool::getCreateTime, ScreeningSchool::getYear);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        departmentFilterSupport.applyDepartmentIdFilter(
                wrapper, ScreeningSchool::getDepartmentId, filterDeptIds);
        mergeBatchRecords(metaMap, "学校筛查", screeningSchoolService.list(wrapper), ScreeningSchool::getUploadBatch,
                ScreeningSchool::getCreateTime, ScreeningSchool::getYear);
    }

    private void mergeKeyPopulationBatchMeta(Map<String, UploadBatchSupport.BatchMeta> metaMap, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<ScreeningKeyPopulation>()
                .isNotNull(ScreeningKeyPopulation::getUploadBatch)
                .ne(ScreeningKeyPopulation::getUploadBatch, "")
                .select(ScreeningKeyPopulation::getUploadBatch, ScreeningKeyPopulation::getCreateTime,
                        ScreeningKeyPopulation::getYear);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        departmentFilterSupport.applyDepartmentIdFilter(
                wrapper, ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
        mergeBatchRecords(metaMap, "重点人群筛查", screeningKeyPopulationService.list(wrapper),
                ScreeningKeyPopulation::getUploadBatch, ScreeningKeyPopulation::getCreateTime,
                ScreeningKeyPopulation::getYear);
    }

    private void mergeCloseContactBatchMeta(Map<String, UploadBatchSupport.BatchMeta> metaMap, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<ScreeningCloseContact>()
                .isNotNull(ScreeningCloseContact::getUploadBatch)
                .ne(ScreeningCloseContact::getUploadBatch, "")
                .select(ScreeningCloseContact::getUploadBatch, ScreeningCloseContact::getCreateTime,
                        ScreeningCloseContact::getYear);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
        departmentFilterSupport.applyDepartmentIdFilter(
                wrapper, ScreeningCloseContact::getDepartmentId, filterDeptIds);
        mergeBatchRecords(metaMap, "密接筛查", closeContactService.list(wrapper),
                ScreeningCloseContact::getUploadBatch, ScreeningCloseContact::getCreateTime,
                ScreeningCloseContact::getYear);
    }

    private <T> void mergeBatchRecords(
            Map<String, UploadBatchSupport.BatchMeta> metaMap,
            String populationLabel,
            List<T> records,
            java.util.function.Function<T, String> batchGetter,
            java.util.function.Function<T, LocalDateTime> timeGetter,
            java.util.function.Function<T, String> yearGetter) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<String, List<T>> grouped = records.stream()
                .filter(r -> StringUtils.hasText(batchGetter.apply(r)))
                .collect(Collectors.groupingBy(r -> batchGetter.apply(r).trim(), LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<T>> entry : grouped.entrySet()) {
            UploadBatchSupport.BatchMeta meta = metaMap.computeIfAbsent(entry.getKey(), key -> new UploadBatchSupport.BatchMeta());
            List<T> batchRecords = entry.getValue();
            LocalDateTime minTime = batchRecords.stream()
                    .map(timeGetter)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            String yearVal = batchRecords.stream()
                    .map(yearGetter)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
            meta.merge(populationLabel, yearVal, minTime, batchRecords.size());
        }
    }

    @Operation(summary = "按年度获取三类人群数据统计")
    @GetMapping("/task-stats")
    public ResultResponse<Map<String, Object>> taskStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        Map<String, Object> data = new HashMap<>();
        boolean hasYear = year != null;
        String yearStr = hasYear ? String.valueOf(year) : null;

        LambdaQueryWrapper<ScreeningSchool> schoolWrapper = buildSchoolTaskWrapper(hasYear, yearStr, filterDeptIds);
        long schoolTotal = screeningSchoolService.count(schoolWrapper);
        List<Object> schoolIds = hasYear
                ? screeningSchoolService.listObjs(
                        buildSchoolTaskWrapper(true, yearStr, filterDeptIds).select(ScreeningSchool::getId))
                : null;
        data.put("school", buildPopStats(schoolTotal,
                countLatent("school", hasYear, schoolIds, filterDeptIds),
                countConfirmedPatient("school", hasYear, yearStr, filterDeptIds)));

        LambdaQueryWrapper<ScreeningKeyPopulation> keyWrapper = buildKeyPopulationTaskWrapper(hasYear, yearStr, filterDeptIds);
        long keyTotal = screeningKeyPopulationService.count(keyWrapper);
        List<Object> keyIds = hasYear
                ? screeningKeyPopulationService.listObjs(
                        buildKeyPopulationTaskWrapper(true, yearStr, filterDeptIds).select(ScreeningKeyPopulation::getId))
                : null;
        data.put("keyPopulation", buildPopStats(keyTotal,
                countLatent("keyPopulation", hasYear, keyIds, filterDeptIds),
                countConfirmedPatient("keyPopulation", hasYear, yearStr, filterDeptIds)));

        LambdaQueryWrapper<ScreeningCloseContact> closeWrapper = buildCloseContactTaskWrapper(hasYear, yearStr, filterDeptIds);
        long closeTotal = closeContactService.count(closeWrapper);

        LambdaQueryWrapper<ScreeningCloseContact> closeLatentWrapper = buildCloseContactTaskWrapper(hasYear, yearStr, filterDeptIds)
                .eq(ScreeningCloseContact::getFinalScreeningResult, "潜伏感染者");
        long closeLatent = closeContactService.count(closeLatentWrapper);
        data.put("closeContact", buildPopStats(closeTotal, closeLatent,
                countConfirmedPatient("closeContact", hasYear, yearStr, filterDeptIds)));

        return ResultRes.success(data);
    }

    @Operation(summary = "获取消息通知统计（通知单 + 分级诊疗）")
    @GetMapping("/message-stats")
    public ResultResponse<Map<String, Object>> messageStats(
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        Map<String, Object> data = new HashMap<>();

        LambdaQueryWrapper<Notice> latentSentWrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getNoticeType, "latent");
        dataScopeHelper.applyNoticeScope(latentSentWrapper);
        dataScopeHelper.applyNoticeBizDepartmentFilter(latentSentWrapper, filterDeptIds);
        data.put("latentNoticeSent", noticeService.count(latentSentWrapper));

        LambdaQueryWrapper<Notice> latentConfirmedWrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getNoticeType, "latent")
                .eq(Notice::getStatus, 2);
        dataScopeHelper.applyNoticeScope(latentConfirmedWrapper);
        dataScopeHelper.applyNoticeBizDepartmentFilter(latentConfirmedWrapper, filterDeptIds);
        data.put("latentNoticeConfirmed", noticeService.count(latentConfirmedWrapper));

        LambdaQueryWrapper<Notice> patientSentWrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getNoticeType, "patient");
        dataScopeHelper.applyNoticeScope(patientSentWrapper);
        dataScopeHelper.applyNoticeBizDepartmentFilter(patientSentWrapper, filterDeptIds);
        data.put("patientNoticeSent", noticeService.count(patientSentWrapper));

        LambdaQueryWrapper<Notice> patientConfirmedWrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getNoticeType, "patient")
                .eq(Notice::getStatus, 2);
        dataScopeHelper.applyNoticeScope(patientConfirmedWrapper);
        dataScopeHelper.applyNoticeBizDepartmentFilter(patientConfirmedWrapper, filterDeptIds);
        data.put("patientNoticeConfirmed", noticeService.count(patientConfirmedWrapper));

        LambdaQueryWrapper<Referral> referralSentWrapper = new LambdaQueryWrapper<>();
        dataScopeHelper.applyReferralScope(referralSentWrapper);
        dataScopeHelper.applyReferralBizDepartmentFilter(referralSentWrapper, filterDeptIds);
        data.put("referralSent", referralService.count(referralSentWrapper));

        LambdaQueryWrapper<Referral> referralConfirmedWrapper = new LambdaQueryWrapper<Referral>()
                .eq(Referral::getStatus, 2);
        dataScopeHelper.applyReferralScope(referralConfirmedWrapper);
        dataScopeHelper.applyReferralBizDepartmentFilter(referralConfirmedWrapper, filterDeptIds);
        data.put("referralConfirmed", referralService.count(referralConfirmedWrapper));

        LambdaQueryWrapper<Referral> referralRejectedWrapper = new LambdaQueryWrapper<Referral>()
                .eq(Referral::getStatus, 3);
        dataScopeHelper.applyReferralScope(referralRejectedWrapper);
        dataScopeHelper.applyReferralBizDepartmentFilter(referralRejectedWrapper, filterDeptIds);
        data.put("referralRejected", referralService.count(referralRejectedWrapper));

        return ResultRes.success(data);
    }

    private LambdaQueryWrapper<ScreeningCloseContact> scopedCloseContactWrapper(List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningCloseContact::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningSchool> buildSchoolTaskWrapper(boolean hasYear, String yearStr, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        if (hasYear) {
            wrapper.eq(ScreeningSchool::getYear, yearStr);
        }
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningSchool::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningKeyPopulation> buildKeyPopulationTaskWrapper(boolean hasYear, String yearStr, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        if (hasYear) {
            wrapper.eq(ScreeningKeyPopulation::getYear, yearStr);
        }
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningCloseContact> buildCloseContactTaskWrapper(boolean hasYear, String yearStr, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        if (hasYear) {
            wrapper.eq(ScreeningCloseContact::getYear, yearStr);
        }
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningCloseContact::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private long countLatent(String populationType, boolean hasYear, List<Object> ids, List<Long> filterDeptIds) {
        if (hasYear && (ids == null || ids.isEmpty())) {
            return 0L;
        }
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .eq(LatentInfection::getPopulationType, populationType)
                .eq(LatentInfection::getReferralResult, "latent")
                .eq(LatentInfection::getArchived, 0)
                .and(w -> w.isNull(LatentInfection::getArchiveRemark)
                        .or()
                        .ne(LatentInfection::getArchiveRemark, LatentInfectionService.ARCHIVE_REMARK_TRANSFERRED_OUT));
        if (hasYear) {
            wrapper.in(LatentInfection::getScreeningId, ids);
        }
        LatentScreeningLinkSupport.applyLinkedScreeningExistsFilter(wrapper);
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, LatentInfection::getDepartmentId, filterDeptIds);
        dataScopeHelper.applyLatentScope(wrapper);
        return latentInfectionService.count(wrapper);
    }

    private long countConfirmedPatient(String populationType, boolean hasYear, String year, List<Long> filterDeptIds) {
        return switch (populationType) {
            case "school" -> {
                LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<ScreeningSchool>()
                        .eq(ScreeningSchool::getDiagnosisFirst, "确诊患者");
                if (hasYear) {
                    wrapper.eq(ScreeningSchool::getYear, year);
                }
                screeningScopeHelper.applyDepartmentScope(
                        wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
                departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningSchool::getDepartmentId, filterDeptIds);
                yield screeningSchoolService.count(wrapper);
            }
            case "keyPopulation" -> {
                LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<ScreeningKeyPopulation>()
                        .eq(ScreeningKeyPopulation::getDiagnosisFirst, "确诊患者");
                if (hasYear) {
                    wrapper.eq(ScreeningKeyPopulation::getYear, year);
                }
                screeningScopeHelper.applyDepartmentScope(
                        wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
                departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
                yield screeningKeyPopulationService.count(wrapper);
            }
            case "closeContact" -> {
                LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<ScreeningCloseContact>()
                        .eq(ScreeningCloseContact::getFinalScreeningResult, "活动性肺结核");
                if (hasYear) {
                    wrapper.eq(ScreeningCloseContact::getYear, year);
                }
                screeningScopeHelper.applyDepartmentScope(
                        wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
                departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningCloseContact::getDepartmentId, filterDeptIds);
                yield closeContactService.count(wrapper);
            }
            default -> 0L;
        };
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
