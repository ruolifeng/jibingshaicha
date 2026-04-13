package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.SchoolStatisticsVO;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ScreeningSchoolMapper screeningSchoolMapper;

    @Override
    public List<SchoolStatisticsVO> getSchoolStatistics(String year, String district) {
        List<ScreeningSchool> records = queryRecords(year, district);
        Map<String, List<ScreeningSchool>> grouped = new LinkedHashMap<>();
        for (ScreeningSchool r : records) {
            String key = StrUtil.blankToDefault(r.getDistrict(), "未知") + "|" + StrUtil.blankToDefault(r.getSchoolName(), "未知");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<SchoolStatisticsVO> result = new ArrayList<>();
        for (Map.Entry<String, List<ScreeningSchool>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            List<ScreeningSchool> list = entry.getValue();
            result.add(buildSchoolVO(parts[0], parts[1], list));
        }
        return result;
    }

    @Override
    public List<DistrictStatisticsVO> getDistrictStatistics(String year, String district) {
        List<ScreeningSchool> records = queryRecords(year, district);
        Map<String, List<ScreeningSchool>> grouped = new LinkedHashMap<>();
        for (ScreeningSchool r : records) {
            String key = StrUtil.blankToDefault(r.getDistrict(), "未知");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<DistrictStatisticsVO> result = new ArrayList<>();
        for (Map.Entry<String, List<ScreeningSchool>> entry : grouped.entrySet()) {
            List<ScreeningSchool> list = entry.getValue();
            result.add(buildDistrictVO(entry.getKey(), list));
        }
        return result;
    }

    @Override
    public List<String> getDistrictOptions() {
        return screeningSchoolMapper.selectList(
                Wrappers.<ScreeningSchool>lambdaQuery()
                        .select(ScreeningSchool::getDistrict)
                        .isNotNull(ScreeningSchool::getDistrict)
                        .groupBy(ScreeningSchool::getDistrict)
                        .orderByAsc(ScreeningSchool::getDistrict)
        ).stream()
                .map(ScreeningSchool::getDistrict)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    private List<ScreeningSchool> queryRecords(String year, String district) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(year), ScreeningSchool::getYear, year)
               .eq(StrUtil.isNotBlank(district), ScreeningSchool::getDistrict, district);
        return screeningSchoolMapper.selectList(wrapper);
    }

    private SchoolStatisticsVO buildSchoolVO(String district, String schoolName, List<ScreeningSchool> list) {
        long ppd1 = countScreenResult(list, "PPD+", true);
        long ppd2 = countScreenResult(list, "PPD++", true);
        long ppd3 = countScreenResult(list, "PPD+++", false);
        // 汇总各记录备注，去重去空后以"；"连接
        String remark = list.stream()
                .map(ScreeningSchool::getRemark)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
        return SchoolStatisticsVO.builder()
                .district(district)
                .schoolName(schoolName)
                // 上传记录总数即应筛查人数；实际进行感染筛查（"是"）的为实际筛查人数
                .shouldScreenCount((long) list.size())
                .actualScreenCount(countEquals(list, ScreeningSchool::getHasInfectionScreen, "是"))
                .closeContactCount(countContains(list, ScreeningSchool::getCloseContactHistory, "有"))
                .suspiciousSymptomCount(countContains(list, ScreeningSchool::getSuspiciousSymptoms, "有"))
                .chestXrayCount(countEquals(list, ScreeningSchool::getHasChestXray, "是"))
                .chestXrayAbnormalCount(countContains(list, ScreeningSchool::getChestXrayResult, "异常"))
                .ppdTestCount(countContains(list, ScreeningSchool::getScreenMethod, "PPD"))
                .ppdPositive1(ppd1)
                .ppdPositive2(ppd2)
                .ppdPositive3(ppd3)
                // 合计 = 三个等级之和，避免字符串模糊匹配偏差
                .ppdPositiveTotal(ppd1 + ppd2 + ppd3)
                .ecNegative(countContains(list, ScreeningSchool::getInfectionResult, "EC阴性"))
                .ecPositive(countContains(list, ScreeningSchool::getInfectionResult, "EC阳性"))
                .igraPositive(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阳性"))
                .igraNegative(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阴性"))
                // "肺结核/疑似肺结核"：诊断结果含"肺结核"（"疑似肺结核"亦包含该子串）
                .tbPatientCount(countContains(list, ScreeningSchool::getDiagnosisResult, "肺结核"))
                .remark(StrUtil.blankToDefault(remark, null))
                .build();
    }

    private DistrictStatisticsVO buildDistrictVO(String district, List<ScreeningSchool> list) {
        long ppd1 = countScreenResult(list, "PPD+", true);
        long ppd2 = countScreenResult(list, "PPD++", true);
        long ppd3 = countScreenResult(list, "PPD+++", false);
        String remark = list.stream()
                .map(ScreeningSchool::getRemark)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
        return DistrictStatisticsVO.builder()
                .district(district)
                .actualScreenCount(countEquals(list, ScreeningSchool::getHasInfectionScreen, "是"))
                .closeContactCount(countContains(list, ScreeningSchool::getCloseContactHistory, "有"))
                .suspiciousSymptomCount(countContains(list, ScreeningSchool::getSuspiciousSymptoms, "有"))
                .chestXrayCount(countEquals(list, ScreeningSchool::getHasChestXray, "是"))
                .chestXrayAbnormalCount(countContains(list, ScreeningSchool::getChestXrayResult, "异常"))
                .ppdTestCount(countContains(list, ScreeningSchool::getScreenMethod, "PPD"))
                .ppdPositive1(ppd1)
                .ppdPositive2(ppd2)
                .ppdPositive3(ppd3)
                .ppdPositiveTotal(ppd1 + ppd2 + ppd3)
                .ecNegative(countContains(list, ScreeningSchool::getInfectionResult, "EC阴性"))
                .ecPositive(countContains(list, ScreeningSchool::getInfectionResult, "EC阳性"))
                .igraPositive(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阳性"))
                .igraNegative(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阴性"))
                .tbPatientCount(countContains(list, ScreeningSchool::getDiagnosisResult, "肺结核"))
                .remark(StrUtil.blankToDefault(remark, null))
                .build();
    }

    private long countContains(List<ScreeningSchool> list,
                               java.util.function.Function<ScreeningSchool, String> getter,
                               String keyword) {
        return list.stream()
                .filter(r -> {
                    String val = getter.apply(r);
                    return StrUtil.isNotBlank(val) && val.contains(keyword);
                })
                .count();
    }

    private long countEquals(List<ScreeningSchool> list,
                             java.util.function.Function<ScreeningSchool, String> getter,
                             String value) {
        return list.stream()
                .filter(r -> value.equals(getter.apply(r)))
                .count();
    }

    /**
     * PPD+ / PPD++ / PPD+++ 分级计数。
     * exact=true 时精确匹配（如 PPD+ 不含 PPD++），exact=false 时直接 contains。
     */
    private long countScreenResult(List<ScreeningSchool> list, String level, boolean exact) {
        return list.stream()
                .filter(r -> {
                    String val = r.getInfectionResult();
                    if (StrUtil.isBlank(val)) return false;
                    if (!exact) return val.contains(level);
                    if ("PPD+".equals(level)) {
                        return val.contains("PPD+") && !val.contains("PPD++");
                    }
                    if ("PPD++".equals(level)) {
                        return val.contains("PPD++") && !val.contains("PPD+++");
                    }
                    return val.contains(level);
                })
                .count();
    }
}
