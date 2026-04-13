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

    private List<ScreeningSchool> queryRecords(String year, String district) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(year), ScreeningSchool::getYear, year)
               .eq(StrUtil.isNotBlank(district), ScreeningSchool::getDistrict, district);
        return screeningSchoolMapper.selectList(wrapper);
    }

    private SchoolStatisticsVO buildSchoolVO(String district, String schoolName, List<ScreeningSchool> list) {
        return SchoolStatisticsVO.builder()
                .district(district)
                .schoolName(schoolName)
                .shouldScreenCount((long) list.size())
                .actualScreenCount((long) list.size())
                .closeContactCount(countContains(list, ScreeningSchool::getCloseContactHistory, "有"))
                .suspiciousSymptomCount(countContains(list, ScreeningSchool::getSuspiciousSymptoms, "有"))
                .chestXrayCount(countEquals(list, ScreeningSchool::getHasChestXray, "是"))
                .chestXrayAbnormalCount(countContains(list, ScreeningSchool::getChestXrayResult, "异常"))
                .ppdTestCount(countContains(list, ScreeningSchool::getScreenMethod, "PPD"))
                .ppdPositive1(countScreenResult(list, "PPD+", true))
                .ppdPositive2(countScreenResult(list, "PPD++", true))
                .ppdPositive3(countScreenResult(list, "PPD+++", false))
                .ppdPositiveTotal(countContains(list, ScreeningSchool::getInfectionResult, "PPD+"))
                .ecNegative(countContains(list, ScreeningSchool::getInfectionResult, "EC阴性"))
                .ecPositive(countContains(list, ScreeningSchool::getInfectionResult, "EC阳性"))
                .igraPositive(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阳性"))
                .igraNegative(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阴性"))
                .tbPatientCount(countContains(list, ScreeningSchool::getDiagnosisResult, "肺结核"))
                .build();
    }

    private DistrictStatisticsVO buildDistrictVO(String district, List<ScreeningSchool> list) {
        return DistrictStatisticsVO.builder()
                .district(district)
                .actualScreenCount((long) list.size())
                .closeContactCount(countContains(list, ScreeningSchool::getCloseContactHistory, "有"))
                .suspiciousSymptomCount(countContains(list, ScreeningSchool::getSuspiciousSymptoms, "有"))
                .chestXrayCount(countEquals(list, ScreeningSchool::getHasChestXray, "是"))
                .chestXrayAbnormalCount(countContains(list, ScreeningSchool::getChestXrayResult, "异常"))
                .ppdTestCount(countContains(list, ScreeningSchool::getScreenMethod, "PPD"))
                .ppdPositive1(countScreenResult(list, "PPD+", true))
                .ppdPositive2(countScreenResult(list, "PPD++", true))
                .ppdPositive3(countScreenResult(list, "PPD+++", false))
                .ppdPositiveTotal(countContains(list, ScreeningSchool::getInfectionResult, "PPD+"))
                .ecNegative(countContains(list, ScreeningSchool::getInfectionResult, "EC阴性"))
                .ecPositive(countContains(list, ScreeningSchool::getInfectionResult, "EC阳性"))
                .igraPositive(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阳性"))
                .igraNegative(countContains(list, ScreeningSchool::getInfectionResult, "IGRA阴性"))
                .tbPatientCount(countContains(list, ScreeningSchool::getDiagnosisResult, "肺结核"))
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
