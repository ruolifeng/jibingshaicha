package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.SchoolStatisticsVO;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.service.StatisticsService;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import cn.luyou.utils.ScreeningScopeHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final LatentInfectionMapper latentInfectionMapper;
    private final ScreeningScopeHelper screeningScopeHelper;

    @Override
    public List<SchoolStatisticsVO> getSchoolStatistics(String year, String district) {
        List<ScreeningSchool> records = queryRecords(year, district);
        // 胸片/诊断数据已迁移到 latent_infection，按 populationType=school 一次查出
        List<LatentInfection> latentList = queryLatentRecords("school");
        Map<Long, LatentInfection> latentByScreeningId = latentList.stream()
                .filter(l -> l.getScreeningId() != null)
                .collect(Collectors.toMap(LatentInfection::getScreeningId, l -> l, (a, b) -> a));

        Map<String, List<ScreeningSchool>> grouped = new LinkedHashMap<>();
        for (ScreeningSchool r : records) {
            String key = StrUtil.blankToDefault(r.getDistrict(), "未知") + "|" + StrUtil.blankToDefault(r.getSchoolName(), "未知");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<SchoolStatisticsVO> result = new ArrayList<>();
        for (Map.Entry<String, List<ScreeningSchool>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            List<ScreeningSchool> list = entry.getValue();
            result.add(buildSchoolVO(parts[0], parts[1], list, latentByScreeningId));
        }
        return result;
    }

    @Override
    public List<DistrictStatisticsVO> getDistrictStatistics(String year, String district) {
        List<ScreeningSchool> records = queryRecords(year, district);
        List<LatentInfection> latentList = queryLatentRecords("school");
        Map<Long, LatentInfection> latentByScreeningId = latentList.stream()
                .filter(l -> l.getScreeningId() != null)
                .collect(Collectors.toMap(LatentInfection::getScreeningId, l -> l, (a, b) -> a));

        Map<String, List<ScreeningSchool>> grouped = new LinkedHashMap<>();
        for (ScreeningSchool r : records) {
            String key = StrUtil.blankToDefault(r.getDistrict(), "未知");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<DistrictStatisticsVO> result = new ArrayList<>();
        for (Map.Entry<String, List<ScreeningSchool>> entry : grouped.entrySet()) {
            List<ScreeningSchool> list = entry.getValue();
            result.add(buildDistrictVO(entry.getKey(), list, latentByScreeningId));
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
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        return screeningSchoolMapper.selectList(wrapper);
    }

    private List<LatentInfection> queryLatentRecords(String populationType) {
        return latentInfectionMapper.selectList(
                Wrappers.<LatentInfection>lambdaQuery()
                        .eq(LatentInfection::getPopulationType, populationType));
    }

    /**
     * 从 latent_infection 表按 screeningId 集合统计胸片与诊断。
     */
    private long countLatentXray(Set<Long> screeningIds, Map<Long, LatentInfection> latentMap) {
        return screeningIds.stream()
                .filter(latentMap::containsKey)
                .filter(id -> "是".equals(latentMap.get(id).getHasChestXray()))
                .count();
    }

    private long countLatentXrayAbnormal(Set<Long> screeningIds, Map<Long, LatentInfection> latentMap) {
        return screeningIds.stream()
                .filter(latentMap::containsKey)
                .filter(id -> {
                    String val = latentMap.get(id).getChestXrayResult();
                    return StrUtil.isNotBlank(val) && val.contains("异常");
                })
                .count();
    }

    private long countLatentTbPatient(Set<Long> screeningIds, Map<Long, LatentInfection> latentMap) {
        return screeningIds.stream()
                .filter(latentMap::containsKey)
                .filter(id -> {
                    String diag = latentMap.get(id).getDiagnosisFirst();
                    if (StrUtil.isBlank(diag)) {
                        return false;
                    }
                    String trimmed = diag.trim();
                    return "确诊患者".equals(trimmed)
                            || ScreeningDiagnosisSupport.isSuspectedTbDiagnosis(trimmed)
                            || trimmed.contains("肺结核");
                })
                .count();
    }

    private SchoolStatisticsVO buildSchoolVO(String district, String schoolName,
                                             List<ScreeningSchool> list,
                                             Map<Long, LatentInfection> latentByScreeningId) {
        long ppd1 = countScreenResult(list, "PPD+", true);
        long ppd2 = countScreenResult(list, "PPD++", true);
        long ppd3 = countScreenResult(list, "PPD+++", false);
        String remark = list.stream()
                .map(ScreeningSchool::getRemark)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
        Set<Long> ids = list.stream().map(ScreeningSchool::getId).collect(Collectors.toSet());
        return SchoolStatisticsVO.builder()
                .district(district)
                .schoolName(schoolName)
                .shouldScreenCount((long) list.size())
                .actualScreenCount(countEquals(list, ScreeningSchool::getHasInfectionScreen, "是"))
                .closeContactCount(countContains(list, ScreeningSchool::getCloseContactHistory, "有"))
                .suspiciousSymptomCount(countContains(list, ScreeningSchool::getSuspiciousSymptoms, "有"))
                .chestXrayCount(countLatentXray(ids, latentByScreeningId))
                .chestXrayAbnormalCount(countLatentXrayAbnormal(ids, latentByScreeningId))
                .ppdTestCount(countContains(list, ScreeningSchool::getScreenMethod, "PPD"))
                .ppdPositive1(ppd1)
                .ppdPositive2(ppd2)
                .ppdPositive3(ppd3)
                .ppdPositiveTotal(ppd1 + ppd2 + ppd3)
                .ecNegative(countContains(list, ScreeningSchool::getInfectionResult, "EC阴性"))
                .ecPositive(countContains(list, ScreeningSchool::getInfectionResult, "EC阳性"))
                .igraPositive(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阳性"))
                .igraNegative(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阴性"))
                .tbPatientCount(countLatentTbPatient(ids, latentByScreeningId))
                .remark(StrUtil.blankToDefault(remark, null))
                .build();
    }

    private DistrictStatisticsVO buildDistrictVO(String district, List<ScreeningSchool> list,
                                                 Map<Long, LatentInfection> latentByScreeningId) {
        long ppd1 = countScreenResult(list, "PPD+", true);
        long ppd2 = countScreenResult(list, "PPD++", true);
        long ppd3 = countScreenResult(list, "PPD+++", false);
        String remark = list.stream()
                .map(ScreeningSchool::getRemark)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
        Set<Long> ids = list.stream().map(ScreeningSchool::getId).collect(Collectors.toSet());
        return DistrictStatisticsVO.builder()
                .district(district)
                .actualScreenCount(countEquals(list, ScreeningSchool::getHasInfectionScreen, "是"))
                .closeContactCount(countContains(list, ScreeningSchool::getCloseContactHistory, "有"))
                .suspiciousSymptomCount(countContains(list, ScreeningSchool::getSuspiciousSymptoms, "有"))
                .chestXrayCount(countLatentXray(ids, latentByScreeningId))
                .chestXrayAbnormalCount(countLatentXrayAbnormal(ids, latentByScreeningId))
                .ppdTestCount(countContains(list, ScreeningSchool::getScreenMethod, "PPD"))
                .ppdPositive1(ppd1)
                .ppdPositive2(ppd2)
                .ppdPositive3(ppd3)
                .ppdPositiveTotal(ppd1 + ppd2 + ppd3)
                .ecNegative(countContains(list, ScreeningSchool::getInfectionResult, "EC阴性"))
                .ecPositive(countContains(list, ScreeningSchool::getInfectionResult, "EC阳性"))
                .igraPositive(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阳性"))
                .igraNegative(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阴性"))
                .tbPatientCount(countLatentTbPatient(ids, latentByScreeningId))
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
