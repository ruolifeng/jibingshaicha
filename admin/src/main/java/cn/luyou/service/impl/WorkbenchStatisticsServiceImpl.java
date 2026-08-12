package cn.luyou.service.impl;

import cn.luyou.service.PatientService;
import cn.luyou.service.ReferralTrackingService;
import cn.luyou.service.WorkbenchStatisticsService;
import cn.luyou.utils.StatYearPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkbenchStatisticsServiceImpl implements WorkbenchStatisticsService {

    private final PatientService patientService;
    private final ReferralTrackingService referralTrackingService;

    @Override
    public Map<String, Object> buildSummary(Integer statYear, List<Long> filterDeptIds) {
        int year = statYear != null ? statYear : StatYearPeriod.current().statYear();
        StatYearPeriod period = StatYearPeriod.of(year);

        // 管理患者=分母；发病率分子含经典病原学阳性 + 结核性胸膜炎且0月序影像/分子阳
        long managedPatientCount = patientService.countManagedPatientsForDashboard(year, filterDeptIds);
        long pathogenPositiveCount = patientService.countPathogenPositivePatientsForDashboard(year, filterDeptIds);
        long drugResistanceScreenedCount = patientService.countDrugResistanceScreenedForDashboard(year, filterDeptIds);
        long treatmentSuccessCount = patientService.countTreatmentSuccessForDashboard(year, filterDeptIds);
        long recommendCount = referralTrackingService.countRecommendSentForDashboard(year, filterDeptIds);
        long recommendArrivedCount = referralTrackingService.countRecommendArrivedForDashboard(year, filterDeptIds);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("managementYear", year);
        data.put("statPeriodFrom", period.start().toString());
        data.put("statPeriodTo", period.end().toString());
        // pendingVisit 字段名历史沿用，实际展示为「年度管理患者数」
        data.put("pendingVisit", managedPatientCount);
        data.put("pathogenPositiveCount", pathogenPositiveCount);
        // 发病率 = 分子 / 年度管理患者数（在管+历史）
        data.put("pathogenPositiveRate", managedPatientCount > 0
                ? Math.round(pathogenPositiveCount * 1000.0 / managedPatientCount) / 10.0
                : 0.0);
        data.put("drugResistanceScreenedCount", drugResistanceScreenedCount);
        data.put("drugResistanceScreeningRate", managedPatientCount > 0
                ? Math.round(drugResistanceScreenedCount * 1000.0 / managedPatientCount) / 10.0
                : 0.0);
        data.put("treatmentSuccessCount", treatmentSuccessCount);
        data.put("treatmentSuccessRate", managedPatientCount > 0
                ? Math.round(treatmentSuccessCount * 1000.0 / managedPatientCount) / 10.0
                : 0.0);
        data.put("recommendCount", recommendCount);
        data.put("recommendArrivedCount", recommendArrivedCount);
        data.put("recommendArrivalRate", recommendCount > 0
                ? Math.round(recommendArrivedCount * 1000.0 / recommendCount) / 10.0
                : 0.0);
        data.putAll(referralTrackingService.getTrackDashboardStats(year, filterDeptIds));
        return data;
    }
}
