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

    @Operation(summary = "按任务（上传批次）获取三类人群数据统计")
    @GetMapping("/task-stats")
    public ResultResponse<Map<String, Object>> taskStats(
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String departmentIds) {
        List<Long> filterDeptIds = departmentFilterSupport.resolveFilterDepartmentIds(departmentIds);
        Map<String, Object> data = new HashMap<>();
        BatchScope batchScope = resolveBatchScope(batch, filterDeptIds);

        // ===== 学校人群 =====
        LambdaQueryWrapper<ScreeningSchool> schoolWrapper = scopedSchoolWrapper(filterDeptIds);
        if (batchScope.filterSchool()) {
            schoolWrapper.eq(ScreeningSchool::getUploadBatch, batch);
        }
        long schoolTotal = screeningSchoolService.count(schoolWrapper);
        List<Object> schoolIds = batchScope.filterSchool()
                ? screeningSchoolService.listObjs(scopedSchoolWrapper(filterDeptIds)
                        .eq(ScreeningSchool::getUploadBatch, batch)
                        .select(ScreeningSchool::getId))
                : null;
        data.put("school", buildPopStats(schoolTotal,
                countLatent("school", batchScope.filterSchool(), schoolIds, filterDeptIds),
                countSchoolConfirmedPatients(batchScope.filterSchool(), batch, filterDeptIds)));

        // ===== 重点人群 =====
        LambdaQueryWrapper<ScreeningKeyPopulation> keyWrapper = scopedKeyPopulationWrapper(filterDeptIds);
        if (batchScope.filterKeyPopulation()) {
            keyWrapper.eq(ScreeningKeyPopulation::getUploadBatch, batch);
        }
        long keyTotal = screeningKeyPopulationService.count(keyWrapper);
        List<Object> keyIds = batchScope.filterKeyPopulation()
                ? screeningKeyPopulationService.listObjs(scopedKeyPopulationWrapper(filterDeptIds)
                        .eq(ScreeningKeyPopulation::getUploadBatch, batch)
                        .select(ScreeningKeyPopulation::getId))
                : null;
        data.put("keyPopulation", buildPopStats(keyTotal,
                countLatent("keyPopulation", batchScope.filterKeyPopulation(), keyIds, filterDeptIds),
                countKeyPopulationConfirmedPatients(batchScope.filterKeyPopulation(), batch, filterDeptIds)));

        // ===== 密接人群 =====
        LambdaQueryWrapper<ScreeningCloseContact> closeWrapper = scopedCloseContactWrapper(filterDeptIds);
        if (batchScope.filterCloseContact()) {
            closeWrapper.eq(ScreeningCloseContact::getUploadBatch, batch);
        }
        long closeTotal = closeContactService.count(closeWrapper);
        LambdaQueryWrapper<ScreeningCloseContact> closeLatentWrapper = scopedCloseContactWrapper(filterDeptIds)
                .eq(ScreeningCloseContact::getFinalScreeningResult, "潜伏感染者");
        if (batchScope.filterCloseContact()) {
            closeLatentWrapper.eq(ScreeningCloseContact::getUploadBatch, batch);
        }
        long closeLatent = closeContactService.count(closeLatentWrapper);
        data.put("closeContact", buildPopStats(closeTotal,
                closeLatent,
                countCloseContactConfirmedPatients(batchScope.filterCloseContact(), batch, filterDeptIds)));

        return ResultRes.success(data);
    }

    /**
     * 批次仅作用于实际包含该人群数据的统计项。
     * 例如选中「重点人群筛查」批次时，学校/密接仍展示各自全量（含部门权限范围）数据。
     */
    private BatchScope resolveBatchScope(String batch, List<Long> filterDeptIds) {
        if (!StringUtils.hasText(batch)) {
            return BatchScope.all();
        }
        boolean hasSchool = screeningSchoolService.count(
                scopedSchoolWrapper(filterDeptIds).eq(ScreeningSchool::getUploadBatch, batch)) > 0;
        boolean hasKeyPopulation = screeningKeyPopulationService.count(
                scopedKeyPopulationWrapper(filterDeptIds).eq(ScreeningKeyPopulation::getUploadBatch, batch)) > 0;
        boolean hasCloseContact = closeContactService.count(
                scopedCloseContactWrapper(filterDeptIds).eq(ScreeningCloseContact::getUploadBatch, batch)) > 0;
        return new BatchScope(hasSchool, hasKeyPopulation, hasCloseContact);
    }

    private LambdaQueryWrapper<ScreeningSchool> scopedSchoolWrapper(List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningSchool::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningKeyPopulation> scopedKeyPopulationWrapper(List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningCloseContact> scopedCloseContactWrapper(List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, ScreeningCloseContact::getDepartmentId, filterDeptIds);
        return wrapper;
    }

    private record BatchScope(boolean hasSchool, boolean hasKeyPopulation, boolean hasCloseContact) {
        static BatchScope all() {
            return new BatchScope(false, false, false);
        }

        boolean filterSchool() {
            return hasSchool;
        }

        boolean filterKeyPopulation() {
            return hasKeyPopulation;
        }

        boolean filterCloseContact() {
            return hasCloseContact;
        }
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

    /**
     * 统计学校/重点人群的潜伏感染者数量。
     * 与潜伏感染菜单保持一致：仅统计转诊结果为"latent"的记录。
     */
    private long countLatent(String populationType, boolean hasBatch, List<Object> ids, List<Long> filterDeptIds) {
        if (hasBatch && (ids == null || ids.isEmpty())) return 0L;
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .eq(LatentInfection::getPopulationType, populationType)
                .eq(LatentInfection::getReferralResult, "latent")
                .eq(LatentInfection::getArchived, 0)
                .and(w -> w.isNull(LatentInfection::getArchiveRemark)
                        .or()
                        .ne(LatentInfection::getArchiveRemark, LatentInfectionService.ARCHIVE_REMARK_TRANSFERRED_OUT));
        if (hasBatch) {
            wrapper.in(LatentInfection::getScreeningId, ids);
        }
        LatentScreeningLinkSupport.applyLinkedScreeningExistsFilter(wrapper);
        departmentFilterSupport.applyDepartmentIdFilter(wrapper, LatentInfection::getDepartmentId, filterDeptIds);
        dataScopeHelper.applyLatentScope(wrapper);
        return latentInfectionService.count(wrapper);
    }

    private long countSchoolConfirmedPatients(boolean filterByBatch, String batch, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = scopedSchoolWrapper(filterDeptIds)
                .eq(ScreeningSchool::getDiagnosisFirst, "确诊患者");
        if (filterByBatch) {
            wrapper.eq(ScreeningSchool::getUploadBatch, batch);
        }
        return screeningSchoolService.count(wrapper);
    }

    private long countKeyPopulationConfirmedPatients(boolean filterByBatch, String batch, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = scopedKeyPopulationWrapper(filterDeptIds)
                .eq(ScreeningKeyPopulation::getDiagnosisFirst, "确诊患者");
        if (filterByBatch) {
            wrapper.eq(ScreeningKeyPopulation::getUploadBatch, batch);
        }
        return screeningKeyPopulationService.count(wrapper);
    }

    private long countCloseContactConfirmedPatients(boolean filterByBatch, String batch, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = scopedCloseContactWrapper(filterDeptIds)
                .eq(ScreeningCloseContact::getFinalScreeningResult, "活动性肺结核");
        if (filterByBatch) {
            wrapper.eq(ScreeningCloseContact::getUploadBatch, batch);
        }
        return closeContactService.count(wrapper);
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
