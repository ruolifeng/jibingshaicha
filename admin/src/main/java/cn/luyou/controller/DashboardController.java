package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.*;
import cn.luyou.service.*;
import cn.luyou.utils.ScreeningScopeHelper;
import cn.luyou.utils.UploadBatchSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
    private final WorkbenchStatisticsService workbenchStatisticsService;
    private final ScreeningScopeHelper screeningScopeHelper;

    @Operation(summary = "获取待处理事项汇总")
    @GetMapping("/summary")
    public ResultResponse<Map<String, Object>> summary(
            @RequestParam(required = false) Integer year) {
        Map<String, Object> data = new HashMap<>();

        // 待追踪：潜伏感染、追踪状态为0（待追踪）、未归档
        long pendingTracking = latentInfectionService.count(
                new LambdaQueryWrapper<LatentInfection>()
                        .eq(LatentInfection::getTrackingStatus, 0)
                        .eq(LatentInfection::getArchived, 0)
        );
        data.put("pendingTracking", pendingTracking);

        // 年度统计（周期：自然年 1/1—12/31）
        data.putAll(workbenchStatisticsService.buildSummary(year));

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
    public ResultResponse<List<Map<String, String>>> batches() {
        Map<String, UploadBatchSupport.BatchMeta> metaMap = new LinkedHashMap<>();

        mergeSchoolBatchMeta(metaMap);
        mergeKeyPopulationBatchMeta(metaMap);
        mergeCloseContactBatchMeta(metaMap);

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

    private void mergeSchoolBatchMeta(Map<String, UploadBatchSupport.BatchMeta> metaMap) {
        List<Map<String, Object>> rows = screeningSchoolService.listMaps(
                new QueryWrapper<ScreeningSchool>()
                        .select("upload_batch AS uploadBatch",
                                "MIN(create_time) AS minTime",
                                "MIN(year) AS yearVal",
                                "COUNT(*) AS cnt")
                        .isNotNull("upload_batch")
                        .ne("upload_batch", "")
                        .groupBy("upload_batch"));
        mergeBatchRows(metaMap, "学校筛查", rows);
    }

    private void mergeKeyPopulationBatchMeta(Map<String, UploadBatchSupport.BatchMeta> metaMap) {
        List<Map<String, Object>> rows = screeningKeyPopulationService.listMaps(
                new QueryWrapper<ScreeningKeyPopulation>()
                        .select("upload_batch AS uploadBatch",
                                "MIN(create_time) AS minTime",
                                "MIN(year) AS yearVal",
                                "COUNT(*) AS cnt")
                        .isNotNull("upload_batch")
                        .ne("upload_batch", "")
                        .groupBy("upload_batch"));
        mergeBatchRows(metaMap, "重点人群筛查", rows);
    }

    private void mergeCloseContactBatchMeta(Map<String, UploadBatchSupport.BatchMeta> metaMap) {
        List<Map<String, Object>> rows = closeContactService.listMaps(
                new QueryWrapper<ScreeningCloseContact>()
                        .select("upload_batch AS uploadBatch",
                                "MIN(create_time) AS minTime",
                                "MIN(year) AS yearVal",
                                "COUNT(*) AS cnt")
                        .isNotNull("upload_batch")
                        .ne("upload_batch", "")
                        .groupBy("upload_batch"));
        mergeBatchRows(metaMap, "密接筛查", rows);
    }

    private void mergeBatchRows(
            Map<String, UploadBatchSupport.BatchMeta> metaMap,
            String populationLabel,
            List<Map<String, Object>> rows) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Object batchObj = row.get("uploadBatch");
            if (batchObj == null) {
                batchObj = row.get("upload_batch");
            }
            if (batchObj == null) {
                continue;
            }
            String batch = batchObj.toString().trim();
            if (!StringUtils.hasText(batch)) {
                continue;
            }
            UploadBatchSupport.BatchMeta meta = metaMap.computeIfAbsent(batch, key -> new UploadBatchSupport.BatchMeta());
            meta.merge(
                    populationLabel,
                    row.get("yearVal") != null ? row.get("yearVal").toString() : null,
                    parseDateTime(row.get("minTime")),
                    row.get("cnt") != null ? Long.parseLong(row.get("cnt").toString()) : 0L
            );
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime time) {
            return time;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return new java.sql.Timestamp(date.getTime()).toLocalDateTime();
        }
        try {
            return LocalDateTime.parse(value.toString().replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Operation(summary = "按任务（上传批次）获取三类人群数据统计")
    @GetMapping("/task-stats")
    public ResultResponse<Map<String, Object>> taskStats(
            @RequestParam(required = false) String batch) {
        Map<String, Object> data = new HashMap<>();
        BatchScope batchScope = resolveBatchScope(batch);

        // ===== 学校人群 =====
        LambdaQueryWrapper<ScreeningSchool> schoolWrapper = scopedSchoolWrapper();
        if (batchScope.filterSchool()) {
            schoolWrapper.eq(ScreeningSchool::getUploadBatch, batch);
        }
        long schoolTotal = screeningSchoolService.count(schoolWrapper);
        List<Object> schoolIds = batchScope.filterSchool()
                ? screeningSchoolService.listObjs(schoolWrapper.clone().select(ScreeningSchool::getId))
                : null;
        data.put("school", buildPopStats(schoolTotal,
                countLatent("school", batchScope.filterSchool(), schoolIds),
                countSchoolConfirmedPatients(batchScope.filterSchool(), batch, schoolWrapper)));

        // ===== 重点人群 =====
        LambdaQueryWrapper<ScreeningKeyPopulation> keyWrapper = scopedKeyPopulationWrapper();
        if (batchScope.filterKeyPopulation()) {
            keyWrapper.eq(ScreeningKeyPopulation::getUploadBatch, batch);
        }
        long keyTotal = screeningKeyPopulationService.count(keyWrapper);
        List<Object> keyIds = batchScope.filterKeyPopulation()
                ? screeningKeyPopulationService.listObjs(keyWrapper.clone().select(ScreeningKeyPopulation::getId))
                : null;
        data.put("keyPopulation", buildPopStats(keyTotal,
                countLatent("keyPopulation", batchScope.filterKeyPopulation(), keyIds),
                countKeyPopulationConfirmedPatients(batchScope.filterKeyPopulation(), batch, keyWrapper)));

        // ===== 密接人群 =====
        LambdaQueryWrapper<ScreeningCloseContact> closeWrapper = scopedCloseContactWrapper();
        if (batchScope.filterCloseContact()) {
            closeWrapper.eq(ScreeningCloseContact::getUploadBatch, batch);
        }
        long closeTotal = closeContactService.count(closeWrapper);
        LambdaQueryWrapper<ScreeningCloseContact> closeLatentWrapper = scopedCloseContactWrapper()
                .eq(ScreeningCloseContact::getFinalScreeningResult, "潜伏感染者");
        if (batchScope.filterCloseContact()) {
            closeLatentWrapper.eq(ScreeningCloseContact::getUploadBatch, batch);
        }
        long closeLatent = closeContactService.count(closeLatentWrapper);
        data.put("closeContact", buildPopStats(closeTotal,
                closeLatent,
                countCloseContactConfirmedPatients(batchScope.filterCloseContact(), batch, closeWrapper)));

        return ResultRes.success(data);
    }

    /**
     * 批次仅作用于实际包含该人群数据的统计项。
     * 例如选中「重点人群筛查」批次时，学校/密接仍展示各自全量（含部门权限范围）数据。
     */
    private BatchScope resolveBatchScope(String batch) {
        if (!StringUtils.hasText(batch)) {
            return BatchScope.all();
        }
        boolean hasSchool = screeningSchoolService.count(
                new LambdaQueryWrapper<ScreeningSchool>().eq(ScreeningSchool::getUploadBatch, batch)) > 0;
        boolean hasKeyPopulation = screeningKeyPopulationService.count(
                new LambdaQueryWrapper<ScreeningKeyPopulation>().eq(ScreeningKeyPopulation::getUploadBatch, batch)) > 0;
        boolean hasCloseContact = closeContactService.count(
                new LambdaQueryWrapper<ScreeningCloseContact>().eq(ScreeningCloseContact::getUploadBatch, batch)) > 0;
        return new BatchScope(hasSchool, hasKeyPopulation, hasCloseContact);
    }

    private LambdaQueryWrapper<ScreeningSchool> scopedSchoolWrapper() {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningKeyPopulation> scopedKeyPopulationWrapper() {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        return wrapper;
    }

    private LambdaQueryWrapper<ScreeningCloseContact> scopedCloseContactWrapper() {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = new LambdaQueryWrapper<>();
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningCloseContact::getDepartmentId, ScreeningCloseContact::getId, "close");
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

    /**
     * 统计学校/重点人群的潜伏感染者数量。
     * 与潜伏感染菜单保持一致：仅统计转诊结果为"latent"的记录。
     */
    private long countLatent(String populationType, boolean hasBatch, List<Object> ids) {
        if (hasBatch && (ids == null || ids.isEmpty())) return 0L;
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<LatentInfection>()
                .eq(LatentInfection::getPopulationType, populationType)
                .eq(LatentInfection::getReferralResult, "latent");
        if (hasBatch) {
            wrapper.in(LatentInfection::getScreeningId, ids);
        }
        return latentInfectionService.count(wrapper);
    }

    /**
     * 统计筛查确诊患者：以筛查表诊断结果为准（上传 Excel 时 diagnosis_first=确诊患者）。
     * 确诊患者仅标红结案，不进入患者管理表，故不能从 patient 表统计。
     */
    private long countSchoolConfirmedPatients(boolean filterByBatch, String batch,
                                              LambdaQueryWrapper<ScreeningSchool> scopedWrapper) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = scopedWrapper.clone()
                .eq(ScreeningSchool::getDiagnosisFirst, "确诊患者");
        if (filterByBatch) {
            wrapper.eq(ScreeningSchool::getUploadBatch, batch);
        }
        return screeningSchoolService.count(wrapper);
    }

    private long countKeyPopulationConfirmedPatients(boolean filterByBatch, String batch,
                                                     LambdaQueryWrapper<ScreeningKeyPopulation> scopedWrapper) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = scopedWrapper.clone()
                .eq(ScreeningKeyPopulation::getDiagnosisFirst, "确诊患者");
        if (filterByBatch) {
            wrapper.eq(ScreeningKeyPopulation::getUploadBatch, batch);
        }
        return screeningKeyPopulationService.count(wrapper);
    }

    private long countCloseContactConfirmedPatients(boolean filterByBatch, String batch,
                                                    LambdaQueryWrapper<ScreeningCloseContact> scopedWrapper) {
        LambdaQueryWrapper<ScreeningCloseContact> wrapper = scopedWrapper.clone()
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
