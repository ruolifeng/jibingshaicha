package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.model.FirstVisit;
import cn.luyou.model.FollowUpVisit;
import cn.luyou.model.MedicationManagement;
import cn.luyou.service.FirstVisitService;
import cn.luyou.service.FollowUpVisitService;
import cn.luyou.service.MedicationManagementService;

import java.util.Map;

/**
 * 患者结案（停止治疗）时全程管理统计：实际访视次数、实际服药次数。
 */
public final class FollowUpCaseClosureSupport {

    private FollowUpCaseClosureSupport() {
    }

    /** 统计服药日历中已服药天数（仅 circled / 圈 × 计入；光划 x 不计） */
    public static int countMedicationMarkedDays(String medicationRecordsJson) {
        if (StrUtil.isBlank(medicationRecordsJson)) {
            return 0;
        }
        try {
            Object parsed = JSONUtil.parse(medicationRecordsJson);
            if (parsed instanceof JSONArray arr) {
                // 旧版仅存日期列表，历史语义为已服药
                int count = 0;
                for (Object item : arr) {
                    if (item instanceof String s && s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        count++;
                    }
                }
                return count;
            }
            if (parsed instanceof JSONObject obj) {
                int count = 0;
                for (Map.Entry<String, Object> entry : obj.entrySet()) {
                    String mark = entry.getValue() == null ? "" : entry.getValue().toString();
                    if ("circled".equals(mark)) {
                        count++;
                    }
                }
                return count;
            }
        } catch (Exception ignored) {
            // 兼容异常 JSON
        }
        return 0;
    }

    public static int computeActualVisitCount(FirstVisitService firstVisitService,
                                              FollowUpVisitService followUpVisitService,
                                              Long patientId,
                                              boolean includeCurrentFollowUp) {
        if (patientId == null) {
            return 0;
        }
        long firstCount = firstVisitService.lambdaQuery()
                .eq(FirstVisit::getPatientId, patientId)
                .eq(FirstVisit::getStatus, 1)
                .count();
        long followCount = followUpVisitService.lambdaQuery()
                .eq(FollowUpVisit::getPatientId, patientId)
                .eq(FollowUpVisit::getStatus, 1)
                .count();
        return (int) firstCount + (int) followCount + (includeCurrentFollowUp ? 1 : 0);
    }

    public static int computeActualDoseCount(MedicationManagementService medicationManagementService,
                                             Long patientId) {
        if (patientId == null) {
            return 0;
        }
        MedicationManagement med = medicationManagementService.lambdaQuery()
                .eq(MedicationManagement::getPatientId, patientId)
                .orderByDesc(MedicationManagement::getCreateTime)
                .last("LIMIT 1")
                .one();
        return countMedicationMarkedDays(med != null ? med.getMedicationRecords() : null);
    }

    /** 草稿转完成或新建时，当前随访尚未计入已完成列表 */
    public static boolean shouldIncludeCurrentFollowUp(FollowUpVisit existing) {
        if (existing == null) {
            return true;
        }
        return !Integer.valueOf(1).equals(existing.getStatus());
    }

    public static void applyCaseClosureStats(FollowUpVisit followUpVisit,
                                             FirstVisitService firstVisitService,
                                             FollowUpVisitService followUpVisitService,
                                             MedicationManagementService medicationManagementService,
                                             boolean includeCurrentFollowUp) {
        if (followUpVisit == null || followUpVisit.getPatientId() == null) {
            return;
        }
        if (!"是".equals(followUpVisit.getStopTreatment())) {
            return;
        }
        followUpVisit.setActualVisitCount(computeActualVisitCount(
                firstVisitService, followUpVisitService, followUpVisit.getPatientId(), includeCurrentFollowUp));
        followUpVisit.setActualDoseCount(computeActualDoseCount(
                medicationManagementService, followUpVisit.getPatientId()));
    }
}
