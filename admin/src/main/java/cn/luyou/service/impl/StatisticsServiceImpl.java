package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.ReferralTrackingMapper;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.mapper.ScreeningSchoolMapper;
import cn.luyou.model.Department;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.ReferralTracking;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.KeyPopulationTbSymptomReferralStatisticsVO;
import cn.luyou.model.vo.SchoolStatisticsVO;
import cn.luyou.model.vo.StudentReportStatisticsVO;
import cn.luyou.utils.SchoolScreeningStatSupport.ReportCategory;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.StatisticsService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.DepartmentFilterSupport;
import cn.luyou.utils.SchoolScreeningStatSupport;
import cn.luyou.utils.ScreeningCrowdCategoryFilterSupport;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import cn.luyou.utils.ScreeningScopeHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ScreeningSchoolMapper screeningSchoolMapper;
    private final ScreeningKeyPopulationMapper screeningKeyPopulationMapper;
    private final ReferralTrackingMapper referralTrackingMapper;
    private final LatentInfectionMapper latentInfectionMapper;
    private final ScreeningScopeHelper screeningScopeHelper;
    private final DepartmentFilterSupport departmentFilterSupport;
    private final DepartmentService departmentService;

    @Override
    public List<SchoolStatisticsVO> getSchoolStatistics(String year, String district, List<Long> filterDeptIds) {
        List<ScreeningSchool> records = queryRecords(year, district, filterDeptIds);
        Set<Long> screeningIds = records.stream().map(ScreeningSchool::getId).collect(Collectors.toSet());
        Map<Long, LatentInfection> latentByScreeningId = queryLatentRecords("school", screeningIds).stream()
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
    public List<StudentReportStatisticsVO> getStudentReportStatistics(
            String year, String district, List<String> schoolCategories, List<Long> filterDeptIds) {
        List<ScreeningSchool> records = queryRecords(year, district, filterDeptIds);

        Map<ReportCategory, List<ScreeningSchool>> grouped = new LinkedHashMap<>();
        for (ReportCategory category : ReportCategory.ordered()) {
            grouped.put(category, new ArrayList<>());
        }
        for (ScreeningSchool row : records) {
            ReportCategory category = SchoolScreeningStatSupport.resolveReportCategory(row);
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(row);
        }

        Set<String> categoryFilter = null;
        if (schoolCategories != null && !schoolCategories.isEmpty()) {
            categoryFilter = schoolCategories.stream()
                    .filter(StrUtil::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toCollection(HashSet::new));
        }

        List<StudentReportStatisticsVO> result = new ArrayList<>();
        for (ReportCategory category : ReportCategory.ordered()) {
            if (categoryFilter != null && !categoryFilter.contains(category.label())) {
                continue;
            }
            List<ScreeningSchool> list = grouped.getOrDefault(category, List.of());
            result.add(StudentReportStatisticsVO.builder()
                    .schoolCategory(category.label())
                    .enrollmentCount((long) list.size())
                    .acceptedExamCount(list.stream().filter(SchoolScreeningStatSupport::isAcceptedExamined).count())
                    .standardizedExamCount(list.stream()
                            .filter(r -> SchoolScreeningStatSupport.isStandardizedExamined(r, category))
                            .count())
                    .tbPatientCount(list.stream().filter(SchoolScreeningStatSupport::isTbPatientFound).count())
                    .build());
        }
        return result;
    }

    @Override
    public List<DistrictStatisticsVO> getDistrictStatistics(String year, String district, List<Long> filterDeptIds) {
        List<ScreeningSchool> records = queryRecords(year, district, filterDeptIds);
        Set<Long> screeningIds = records.stream().map(ScreeningSchool::getId).collect(Collectors.toSet());
        Map<Long, LatentInfection> latentByScreeningId = queryLatentRecords("school", screeningIds).stream()
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
    public List<String> getDistrictOptions(List<Long> filterDeptIds) {
        LinkedHashMap<String, Boolean> districts = new LinkedHashMap<>();
        LambdaQueryWrapper<ScreeningSchool> schoolWrapper = Wrappers.<ScreeningSchool>lambdaQuery()
                .select(ScreeningSchool::getDistrict)
                .isNotNull(ScreeningSchool::getDistrict)
                .groupBy(ScreeningSchool::getDistrict)
                .orderByAsc(ScreeningSchool::getDistrict);
        screeningScopeHelper.applyDepartmentScope(
                schoolWrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        departmentFilterSupport.applyDepartmentIdFilter(
                schoolWrapper, ScreeningSchool::getDepartmentId, filterDeptIds);
        screeningSchoolMapper.selectList(schoolWrapper).stream()
                .map(ScreeningSchool::getDistrict)
                .filter(StrUtil::isNotBlank)
                .forEach(d -> districts.put(d, Boolean.TRUE));

        LambdaQueryWrapper<ScreeningKeyPopulation> keyWrapper = Wrappers.<ScreeningKeyPopulation>lambdaQuery()
                .select(ScreeningKeyPopulation::getDistrict)
                .eq(ScreeningKeyPopulation::getSourceType, "keyPopulation")
                .isNotNull(ScreeningKeyPopulation::getDistrict)
                .groupBy(ScreeningKeyPopulation::getDistrict)
                .orderByAsc(ScreeningKeyPopulation::getDistrict);
        screeningScopeHelper.applyDepartmentScope(
                keyWrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        departmentFilterSupport.applyDepartmentIdFilter(
                keyWrapper, ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
        screeningKeyPopulationMapper.selectList(keyWrapper).stream()
                .map(ScreeningKeyPopulation::getDistrict)
                .filter(StrUtil::isNotBlank)
                .forEach(d -> districts.put(d, Boolean.TRUE));

        return new ArrayList<>(districts.keySet());
    }

    @Override
    public List<String> getKeyPopulationRegionOptions(List<Long> filterDeptIds) {
        LinkedHashMap<String, Boolean> regions = new LinkedHashMap<>();
        LambdaQueryWrapper<ScreeningKeyPopulation> districtWrapper = Wrappers.<ScreeningKeyPopulation>lambdaQuery()
                .select(ScreeningKeyPopulation::getDistrict)
                .eq(ScreeningKeyPopulation::getSourceType, "keyPopulation")
                .isNotNull(ScreeningKeyPopulation::getDistrict)
                .groupBy(ScreeningKeyPopulation::getDistrict)
                .orderByAsc(ScreeningKeyPopulation::getDistrict);
        screeningScopeHelper.applyDepartmentScope(
                districtWrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        applyKeyPopulationStatsDepartmentFilter(districtWrapper, filterDeptIds);
        screeningKeyPopulationMapper.selectList(districtWrapper).stream()
                .map(ScreeningKeyPopulation::getDistrict)
                .filter(StrUtil::isNotBlank)
                .forEach(d -> regions.put(d.trim(), Boolean.TRUE));

        LambdaQueryWrapper<ScreeningKeyPopulation> townshipWrapper = Wrappers.<ScreeningKeyPopulation>lambdaQuery()
                .select(ScreeningKeyPopulation::getTownshipCommunity)
                .eq(ScreeningKeyPopulation::getSourceType, "keyPopulation")
                .isNotNull(ScreeningKeyPopulation::getTownshipCommunity)
                .groupBy(ScreeningKeyPopulation::getTownshipCommunity)
                .orderByAsc(ScreeningKeyPopulation::getTownshipCommunity);
        screeningScopeHelper.applyDepartmentScope(
                townshipWrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        applyKeyPopulationStatsDepartmentFilter(townshipWrapper, filterDeptIds);
        screeningKeyPopulationMapper.selectList(townshipWrapper).stream()
                .map(ScreeningKeyPopulation::getTownshipCommunity)
                .filter(StrUtil::isNotBlank)
                .forEach(t -> regions.put(t.trim(), Boolean.TRUE));

        List<String> result = new ArrayList<>(regions.keySet());
        result.sort(String::compareTo);
        return result;
    }

    @Override
    public List<KeyPopulationTbSymptomReferralStatisticsVO> getKeyPopulationTbSymptomReferralStatistics(
            String year, String region, List<Long> filterDeptIds) {
        List<ScreeningKeyPopulation> records = queryKeyPopulationRecords(year, region, filterDeptIds);
        Map<String, Long> elderArrivedByDistrict = countReferralArrivedByDistrict(year, filterDeptIds, true);
        Map<String, Long> diabetesArrivedByDistrict = countReferralArrivedByDistrict(year, filterDeptIds, false);

        Map<String, List<ScreeningKeyPopulation>> grouped = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(region)) {
            // 选定具体区县/乡镇时，汇总为一行，地区名用筛选项本身
            grouped.put(region.trim(), new ArrayList<>(records));
        } else {
            for (ScreeningKeyPopulation r : records) {
                String key = StrUtil.blankToDefault(r.getDistrict(), "未知");
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            }
            // 推介到位可能落在筛查无数据的区县，一并纳入
            for (String key : elderArrivedByDistrict.keySet()) {
                grouped.computeIfAbsent(key, k -> new ArrayList<>());
            }
            for (String key : diabetesArrivedByDistrict.keySet()) {
                grouped.computeIfAbsent(key, k -> new ArrayList<>());
            }
        }

        List<KeyPopulationTbSymptomReferralStatisticsVO> result = new ArrayList<>();
        for (Map.Entry<String, List<ScreeningKeyPopulation>> entry : grouped.entrySet()) {
            String districtName = entry.getKey();
            result.add(buildKeyPopulationTbSymptomReferralVO(
                    districtName,
                    entry.getValue(),
                    elderArrivedByDistrict.getOrDefault(districtName, 0L),
                    diabetesArrivedByDistrict.getOrDefault(districtName, 0L)));
        }
        result.sort((a, b) -> StrUtil.blankToDefault(a.getDistrict(), "")
                .compareTo(StrUtil.blankToDefault(b.getDistrict(), "")));
        return result;
    }

    private List<ScreeningSchool> queryRecords(String year, String district, List<Long> filterDeptIds) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(year), ScreeningSchool::getYear, year)
               .eq(StrUtil.isNotBlank(district), ScreeningSchool::getDistrict, district);
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningSchool::getDepartmentId, ScreeningSchool::getId, "school");
        departmentFilterSupport.applyDepartmentIdFilter(
                wrapper, ScreeningSchool::getDepartmentId, filterDeptIds);
        return screeningSchoolMapper.selectList(wrapper);
    }

    private List<ScreeningKeyPopulation> queryKeyPopulationRecords(String year, String region,
                                                                  List<Long> filterDeptIds) {
        // 仅查报表统计所需字段，避免实体字段与库表短暂不一致时全字段查询失败
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        ScreeningKeyPopulation::getId,
                        ScreeningKeyPopulation::getDistrict,
                        ScreeningKeyPopulation::getTownshipCommunity,
                        ScreeningKeyPopulation::getYear,
                        ScreeningKeyPopulation::getSourceType,
                        ScreeningKeyPopulation::getDepartmentId,
                        ScreeningKeyPopulation::getCrowdCategoryClose,
                        ScreeningKeyPopulation::getCrowdCategoryStudent,
                        ScreeningKeyPopulation::getCrowdCategoryTeacher,
                        ScreeningKeyPopulation::getCrowdCategoryElder,
                        ScreeningKeyPopulation::getCrowdCategoryDiabetes,
                        ScreeningKeyPopulation::getCrowdCategoryDual,
                        ScreeningKeyPopulation::getCrowdCategoryTbHist,
                        ScreeningKeyPopulation::getCrowdCategoryNormal,
                        ScreeningKeyPopulation::getHasSuspiciousSymptoms,
                        ScreeningKeyPopulation::getHasChestXray,
                        ScreeningKeyPopulation::getHasInfectionScreen,
                        ScreeningKeyPopulation::getChestXrayResult,
                        ScreeningKeyPopulation::getInfectionResult,
                        ScreeningKeyPopulation::getDiagnosisFirst)
                .eq(ScreeningKeyPopulation::getSourceType, "keyPopulation")
                .eq(StrUtil.isNotBlank(year), ScreeningKeyPopulation::getYear, year);
        if (StrUtil.isNotBlank(region)) {
            String regionValue = region.trim();
            // 区县或乡镇/社区均可筛选（此前仅 district 精确匹配，选乡镇无数据）
            wrapper.and(w -> w.eq(ScreeningKeyPopulation::getDistrict, regionValue)
                    .or()
                    .eq(ScreeningKeyPopulation::getTownshipCommunity, regionValue));
        }
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
        applyKeyPopulationStatsDepartmentFilter(wrapper, filterDeptIds);
        return screeningKeyPopulationMapper.selectList(wrapper);
    }

    /**
     * 本报表部门筛选：除 department_id 外，兼容乡镇部门下筛查数据仍挂在区县部门的情况，
     * 按部门名称匹配 district / township_community。
     */
    private void applyKeyPopulationStatsDepartmentFilter(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper,
                                                         List<Long> filterDeptIds) {
        if (!departmentFilterSupport.hasActiveFilter(filterDeptIds)) {
            return;
        }
        if (filterDeptIds.size() == 1
                && Objects.equals(filterDeptIds.get(0), DepartmentFilterSupport.NO_MATCH_DEPARTMENT_ID)) {
            wrapper.eq(ScreeningKeyPopulation::getDepartmentId, DepartmentFilterSupport.NO_MATCH_DEPARTMENT_ID);
            return;
        }
        Set<Long> idSet = new HashSet<>(filterDeptIds);
        List<String> names = departmentService.listAll().stream()
                .filter(d -> d.getId() != null && idSet.contains(d.getId()))
                .map(Department::getName)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        wrapper.and(w -> {
            w.in(ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
            if (!names.isEmpty()) {
                w.or().in(ScreeningKeyPopulation::getDistrict, names)
                        .or()
                        .in(ScreeningKeyPopulation::getTownshipCommunity, names);
            }
        });
    }

    /**
     * 推介模块追踪到位人数，按部门归属区县汇总。
     *
     * @param elderGroup true=老年人/老年人+糖尿病；false=单一糖尿病
     */
    private Map<String, Long> countReferralArrivedByDistrict(String year, List<Long> filterDeptIds,
                                                            boolean elderGroup) {
        // 仅查统计所需字段，避免实体新增列（如 recommend_unit_name）尚未迁移到库时全字段查询报错
        LambdaQueryWrapper<ReferralTracking> wrapper = Wrappers.<ReferralTracking>lambdaQuery()
                .select(
                        ReferralTracking::getId,
                        ReferralTracking::getCrowdCategory,
                        ReferralTracking::getDepartmentId,
                        ReferralTracking::getReceiverDeptId,
                        ReferralTracking::getTrackingStatus)
                .eq(ReferralTracking::getTrackingStatus, 1)
                .isNotNull(ReferralTracking::getCrowdCategory);
        if (StrUtil.isNotBlank(year)) {
            wrapper.apply("YEAR(IFNULL(actual_arrival_date, IFNULL(arrival_time, create_time))) = {0}", year);
        }
        applyReferralDepartmentScope(wrapper, filterDeptIds);
        List<ReferralTracking> list = referralTrackingMapper.selectList(wrapper);
        Map<Long, String> deptDistrictMap = buildDepartmentDistrictMap();

        Map<String, Long> result = new HashMap<>();
        for (ReferralTracking row : list) {
            if (!matchReferralCrowdCategory(row.getCrowdCategory(), elderGroup)) {
                continue;
            }
            String districtName = resolveReferralDistrict(row, deptDistrictMap);
            if (StrUtil.isBlank(districtName)) {
                districtName = "未知";
            }
            result.merge(districtName, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 推介到位统计部门范围：优先使用页面部门筛选；否则按当前用户辖区隔离（与筛查统计一致）。
     */
    private void applyReferralDepartmentScope(LambdaQueryWrapper<ReferralTracking> wrapper,
                                              List<Long> filterDeptIds) {
        if (departmentFilterSupport.hasActiveFilter(filterDeptIds)) {
            wrapper.and(w -> w.in(ReferralTracking::getDepartmentId, filterDeptIds)
                    .or()
                    .in(ReferralTracking::getReceiverDeptId, filterDeptIds));
            return;
        }
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long currentDeptId = BaseContext.getCurrentDepartmentId();
        if (currentDeptId == null) {
            wrapper.and(w -> w.isNull(ReferralTracking::getDepartmentId)
                    .and(x -> x.isNull(ReferralTracking::getReceiverDeptId)));
            return;
        }
        List<Long> scopeDeptIds = departmentService.getDescendantIds(currentDeptId);
        if (scopeDeptIds == null || scopeDeptIds.isEmpty()) {
            wrapper.and(w -> w.eq(ReferralTracking::getDepartmentId, currentDeptId)
                    .or()
                    .eq(ReferralTracking::getReceiverDeptId, currentDeptId));
            return;
        }
        wrapper.and(w -> w.in(ReferralTracking::getDepartmentId, scopeDeptIds)
                .or()
                .in(ReferralTracking::getReceiverDeptId, scopeDeptIds));
    }

    private boolean matchReferralCrowdCategory(String crowdCategory, boolean elderGroup) {
        if (StrUtil.isBlank(crowdCategory)) {
            return false;
        }
        String trimmed = crowdCategory.trim();
        if (elderGroup) {
            // 模板：老年人、老年人+糖尿病
            return "老年人".equals(trimmed)
                    || "老年人+糖尿病".equals(trimmed)
                    || "老年人及糖尿病".equals(trimmed);
        }
        // 单一糖尿病：精确匹配，排除「老年人+糖尿病」等合并分类
        return "糖尿病".equals(trimmed) || "糖尿病患者".equals(trimmed);
    }

    private String resolveReferralDistrict(ReferralTracking row, Map<Long, String> deptDistrictMap) {
        Long deptId = row.getDepartmentId() != null ? row.getDepartmentId() : row.getReceiverDeptId();
        if (deptId == null) {
            return null;
        }
        return deptDistrictMap.get(deptId);
    }

    /** 部门 ID → 所属区县名称（二级部门名；三级取上级区县名） */
    private Map<Long, String> buildDepartmentDistrictMap() {
        List<Department> all = departmentService.listAll();
        Map<Long, Department> byId = all.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));
        Map<Long, String> result = new HashMap<>();
        for (Department dept : all) {
            if (dept.getId() == null || dept.getLevel() == null) {
                continue;
            }
            if (dept.getLevel() == 2) {
                result.put(dept.getId(), dept.getName());
            } else if (dept.getLevel() == 3 && dept.getParentId() != null) {
                Department parent = byId.get(dept.getParentId());
                if (parent != null) {
                    result.put(dept.getId(), parent.getName());
                }
            }
        }
        return result;
    }

    private KeyPopulationTbSymptomReferralStatisticsVO buildKeyPopulationTbSymptomReferralVO(
            String district,
            List<ScreeningKeyPopulation> list,
            long elderArrived,
            long diabetesArrived) {
        List<ScreeningKeyPopulation> elders = list.stream().filter(this::isElderGroup).toList();
        List<ScreeningKeyPopulation> diabetesOnly = list.stream().filter(this::isDiabetesOnly).toList();

        long elderCount = elders.size();
        long diabetesCount = diabetesOnly.size();

        return KeyPopulationTbSymptomReferralStatisticsVO.builder()
                .district(district)
                // 老年人数：季度报表模板无系统数据来源，留空由页面手工填写
                .elderCount(null)
                // 参加年度体检人数 / 进行症状筛查人数：老年人及老年人+糖尿病总人数
                .elderAnnualExamCount(elderCount)
                .elderSymptomScreenCount(elderCount)
                .elderChestXrayCount(countKeyEquals(elders, ScreeningKeyPopulation::getHasChestXray, "是"))
                .elderInfectionScreenCount(countKeyEquals(elders, ScreeningKeyPopulation::getHasInfectionScreen, "是"))
                .elderSuspiciousSymptomCount(countSuspiciousSymptom(elders))
                .elderChestXrayAbnormalCount(countChestXrayAbnormal(elders))
                .elderInfectionAbnormalCount(countInfectionAbnormal(elders))
                .elderReferralFormCount(countSuspectedTb(elders))
                .elderArrivedCount(elderArrived)
                .elderConfirmedTbCount(countConfirmedTb(elders))
                // 糖尿病：管理数 / 季度随访 / 症状筛查 口径同模板均为单选糖尿病人群总数
                .diabetesManagedCount(diabetesCount)
                .diabetesQuarterFollowCount(diabetesCount)
                .diabetesSymptomScreenCount(diabetesCount)
                .diabetesChestXrayCount(countKeyEquals(diabetesOnly, ScreeningKeyPopulation::getHasChestXray, "是"))
                .diabetesInfectionScreenCount(countKeyEquals(diabetesOnly, ScreeningKeyPopulation::getHasInfectionScreen, "是"))
                .diabetesSuspiciousSymptomCount(countSuspiciousSymptom(diabetesOnly))
                .diabetesChestXrayAbnormalCount(countChestXrayAbnormal(diabetesOnly))
                .diabetesInfectionAbnormalCount(countInfectionAbnormal(diabetesOnly))
                .diabetesReferralFormCount(countSuspectedTb(diabetesOnly))
                .diabetesArrivedCount(diabetesArrived)
                .diabetesConfirmedTbCount(countConfirmedTb(diabetesOnly))
                .build();
    }

    /**
     * 老年人及老年人+糖尿病：
     * 老年人=是，且不含密接/学生/教职工/双感/既往结核史/非重点人群。
     * （允许同时勾选糖尿病，对应模板「老年人+糖尿病」。）
     */
    private boolean isElderGroup(ScreeningKeyPopulation r) {
        if (!ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryElder())) {
            return false;
        }
        return !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryClose())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryStudent())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryTeacher())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryDual())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryTbHist())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryNormal());
    }

    /** 单选糖尿病：糖尿病为「是」且其余人群分类均不为「是」 */
    private boolean isDiabetesOnly(ScreeningKeyPopulation r) {
        if (!ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryDiabetes())) {
            return false;
        }
        return !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryClose())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryStudent())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryTeacher())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryElder())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryDual())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryTbHist())
                && !ScreeningCrowdCategoryFilterSupport.isYes(r.getCrowdCategoryNormal());
    }

    private long countSuspiciousSymptom(List<ScreeningKeyPopulation> list) {
        return list.stream().filter(r -> {
            String val = StrUtil.trim(r.getHasSuspiciousSymptoms());
            return "有".equals(val) || "是".equals(val);
        }).count();
    }

    private long countChestXrayAbnormal(List<ScreeningKeyPopulation> list) {
        return list.stream().filter(r -> {
            String val = r.getChestXrayResult();
            return StrUtil.isNotBlank(val) && val.contains("异常");
        }).count();
    }

    /**
     * 感染筛查异常：官方结果判定阳性档及历史文案（PPD++/+++、EC/IGRA阳性等）。
     */
    private long countInfectionAbnormal(List<ScreeningKeyPopulation> list) {
        return list.stream().filter(r -> isInfectionAbnormal(r.getInfectionResult())).count();
    }

    private boolean isInfectionAbnormal(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return false;
        }
        // 官方结果判定 / 历史文案统一按阳性档识别
        return ScreeningDiagnosisSupport.isPositiveInfection(infectionResult);
    }

    private long countSuspectedTb(List<ScreeningKeyPopulation> list) {
        return list.stream()
                .filter(r -> ScreeningDiagnosisSupport.isSuspectedTbDiagnosis(r.getDiagnosisFirst()))
                .count();
    }

    private long countConfirmedTb(List<ScreeningKeyPopulation> list) {
        return list.stream().filter(r -> {
            String diag = StrUtil.trim(r.getDiagnosisFirst());
            if (StrUtil.isBlank(diag)) {
                return false;
            }
            return "确诊患者".equals(diag)
                    || "确诊结核".equals(diag)
                    || "确诊肺结核".equals(diag)
                    || "在治患者".equals(diag);
        }).count();
    }

    private List<LatentInfection> queryLatentRecords(String populationType, Set<Long> screeningIds) {
        if (screeningIds == null || screeningIds.isEmpty()) {
            return List.of();
        }
        return latentInfectionMapper.selectList(
                Wrappers.<LatentInfection>lambdaQuery()
                        .eq(LatentInfection::getPopulationType, populationType)
                        .in(LatentInfection::getScreeningId, screeningIds));
    }

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
                            || "确诊结核".equals(trimmed)
                            || "在治患者".equals(trimmed)
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
                .actualScreenCount(list.stream().filter(SchoolScreeningStatSupport::isAcceptedExamined).count())
                .standardizedScreenCount(list.stream().filter(SchoolScreeningStatSupport::isStandardizedExamined).count())
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
                .actualScreenCount(list.stream().filter(SchoolScreeningStatSupport::isAcceptedExamined).count())
                .standardizedScreenCount(list.stream().filter(SchoolScreeningStatSupport::isStandardizedExamined).count())
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

    private long countKeyEquals(List<ScreeningKeyPopulation> list,
                                java.util.function.Function<ScreeningKeyPopulation, String> getter,
                                String value) {
        return list.stream()
                .filter(r -> value.equals(getter.apply(r)))
                .count();
    }

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
