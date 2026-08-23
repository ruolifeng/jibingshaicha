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
import java.util.LinkedHashSet;
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
        applyKeyPopulationReportAccess(districtWrapper, filterDeptIds);
        screeningKeyPopulationMapper.selectList(districtWrapper).stream()
                .map(ScreeningKeyPopulation::getDistrict)
                .filter(StrUtil::isNotBlank)
                .forEach(d -> regions.put(canonicalGeoDisplayName(d), Boolean.TRUE));

        LambdaQueryWrapper<ScreeningKeyPopulation> townshipWrapper = Wrappers.<ScreeningKeyPopulation>lambdaQuery()
                .select(ScreeningKeyPopulation::getTownshipCommunity)
                .eq(ScreeningKeyPopulation::getSourceType, "keyPopulation")
                .isNotNull(ScreeningKeyPopulation::getTownshipCommunity)
                .groupBy(ScreeningKeyPopulation::getTownshipCommunity)
                .orderByAsc(ScreeningKeyPopulation::getTownshipCommunity);
        applyKeyPopulationReportAccess(townshipWrapper, filterDeptIds);
        screeningKeyPopulationMapper.selectList(townshipWrapper).stream()
                .map(ScreeningKeyPopulation::getTownshipCommunity)
                .filter(StrUtil::isNotBlank)
                .forEach(t -> regions.put(canonicalGeoDisplayName(t), Boolean.TRUE));

        List<String> result = new ArrayList<>(regions.keySet());
        result.sort(String::compareTo);
        return result;
    }

    @Override
    public List<KeyPopulationTbSymptomReferralStatisticsVO> getKeyPopulationTbSymptomReferralStatistics(
            String year, String region, List<Long> filterDeptIds, List<Long> selectedDeptIds) {
        List<ScreeningKeyPopulation> records = queryKeyPopulationRecords(year, region, filterDeptIds);
        // 推介到位只查一次，避免老年人/糖尿病各扫一遍全表
        ReferralArrivedByDistrict arrived = countReferralArrivedByDistrict(year, filterDeptIds);
        Map<String, Long> elderArrivedByDistrict = arrived.elder();
        Map<String, Long> diabetesArrivedByDistrict = arrived.diabetes();
        List<Long> groupingDeptIds = (selectedDeptIds != null && !selectedDeptIds.isEmpty())
                ? selectedDeptIds : filterDeptIds;
        KeyPopulationGroupMode groupMode = resolveKeyPopulationGroupMode(filterDeptIds, groupingDeptIds, region);
        Set<String> selectedTownshipNames = resolveSelectedTownshipNames(groupingDeptIds, groupMode);

        // 按地区单次累加指标，避免先按区堆列表再多次 stream 过滤
        Map<String, KeyPopulationStatAccumulator> grouped = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(region)) {
            String regionKey = canonicalGeoDisplayName(region);
            KeyPopulationStatAccumulator acc = grouped.computeIfAbsent(regionKey, k -> new KeyPopulationStatAccumulator());
            for (ScreeningKeyPopulation r : records) {
                acc.accept(r);
            }
        } else {
            for (ScreeningKeyPopulation r : records) {
                String key = resolveKeyPopulationGroupKey(r, groupMode, selectedTownshipNames);
                grouped.computeIfAbsent(key, k -> new KeyPopulationStatAccumulator()).accept(r);
            }
            if (!departmentFilterSupport.hasActiveFilter(filterDeptIds)
                    && groupMode == KeyPopulationGroupMode.DISTRICT) {
                for (String key : elderArrivedByDistrict.keySet()) {
                    grouped.computeIfAbsent(key, k -> new KeyPopulationStatAccumulator());
                }
                for (String key : diabetesArrivedByDistrict.keySet()) {
                    grouped.computeIfAbsent(key, k -> new KeyPopulationStatAccumulator());
                }
            }
        }

        List<KeyPopulationTbSymptomReferralStatisticsVO> result = new ArrayList<>(grouped.size());
        for (Map.Entry<String, KeyPopulationStatAccumulator> entry : grouped.entrySet()) {
            String districtName = entry.getKey();
            result.add(entry.getValue().toVo(
                    districtName,
                    elderArrivedByDistrict.getOrDefault(districtName, 0L),
                    diabetesArrivedByDistrict.getOrDefault(districtName, 0L)));
        }
        result.sort((a, b) -> StrUtil.blankToDefault(a.getDistrict(), "")
                .compareTo(StrUtil.blankToDefault(b.getDistrict(), "")));
        return result;
    }

    private enum KeyPopulationGroupMode {
        /** 按区县一行（合并「富顺/富顺县」） */
        DISTRICT,
        /** 仅选乡镇时全部按乡镇分行 */
        TOWNSHIP,
        /** 同时选区县及其下属镇：匹配到的镇单独一行，其余归入区县 */
        MIXED
    }

    private KeyPopulationGroupMode resolveKeyPopulationGroupMode(
            List<Long> filterDeptIds, List<Long> selectedDeptIds, String region) {
        if (StrUtil.isNotBlank(region) || !departmentFilterSupport.hasActiveFilter(filterDeptIds)) {
            return KeyPopulationGroupMode.DISTRICT;
        }
        if (filterDeptIds.size() == 1
                && Objects.equals(filterDeptIds.get(0), DepartmentFilterSupport.NO_MATCH_DEPARTMENT_ID)) {
            return KeyPopulationGroupMode.DISTRICT;
        }
        List<Department> selectedDepts = listDepartmentsByIds(selectedDeptIds);
        if (selectedDepts.isEmpty()) {
            return KeyPopulationGroupMode.DISTRICT;
        }
        boolean anyTownship = selectedDepts.stream().anyMatch(this::isTownshipDepartment);
        boolean allTownship = selectedDepts.stream().allMatch(this::isTownshipDepartment);
        if (allTownship) {
            return KeyPopulationGroupMode.TOWNSHIP;
        }
        if (anyTownship) {
            return KeyPopulationGroupMode.MIXED;
        }
        return KeyPopulationGroupMode.DISTRICT;
    }

    private boolean isTownshipDepartment(Department dept) {
        return dept != null && dept.getLevel() != null && dept.getLevel() >= 3;
    }

    private List<Department> listDepartmentsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> idSet = new HashSet<>(ids);
        return departmentService.listAll().stream()
                .filter(d -> d.getId() != null && idSet.contains(d.getId()))
                .toList();
    }

    private Set<String> resolveSelectedTownshipNames(List<Long> selectedDeptIds, KeyPopulationGroupMode mode) {
        if (mode == KeyPopulationGroupMode.DISTRICT) {
            return Set.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Department dept : listDepartmentsByIds(selectedDeptIds)) {
            if (!isTownshipDepartment(dept) || StrUtil.isBlank(dept.getName())) {
                continue;
            }
            names.addAll(expandGeoNameAliases(List.of(dept.getName().trim())));
        }
        return names;
    }

    private String resolveKeyPopulationGroupKey(
            ScreeningKeyPopulation row, KeyPopulationGroupMode mode, Set<String> selectedTownshipNames) {
        if (mode == KeyPopulationGroupMode.TOWNSHIP) {
            if (StrUtil.isNotBlank(row.getTownshipCommunity())) {
                return canonicalGeoDisplayName(row.getTownshipCommunity());
            }
            String matched = matchSelectedTownshipName(row, selectedTownshipNames);
            if (matched != null) {
                return matched;
            }
            return canonicalGeoDisplayName(row.getDistrict());
        }
        if (mode == KeyPopulationGroupMode.MIXED) {
            String matched = matchSelectedTownshipName(row, selectedTownshipNames);
            if (matched != null) {
                return matched;
            }
            if (StrUtil.isNotBlank(row.getTownshipCommunity())
                    && nameMatchesAny(row.getTownshipCommunity(), selectedTownshipNames)) {
                return canonicalGeoDisplayName(row.getTownshipCommunity());
            }
        }
        return canonicalGeoDisplayName(row.getDistrict());
    }

    private String matchSelectedTownshipName(ScreeningKeyPopulation row, Set<String> selectedTownshipNames) {
        if (selectedTownshipNames == null || selectedTownshipNames.isEmpty()) {
            return null;
        }
        if (StrUtil.isNotBlank(row.getTownshipCommunity())
                && nameMatchesAny(row.getTownshipCommunity(), selectedTownshipNames)) {
            return canonicalGeoDisplayName(row.getTownshipCommunity());
        }
        if (row.getDepartmentId() != null) {
            Department dept = departmentService.listAll().stream()
                    .filter(d -> row.getDepartmentId().equals(d.getId()))
                    .findFirst()
                    .orElse(null);
            if (isTownshipDepartment(dept) && nameMatchesAny(dept.getName(), selectedTownshipNames)) {
                return canonicalGeoDisplayName(dept.getName());
            }
        }
        return null;
    }

    private boolean nameMatchesAny(String value, Set<String> names) {
        if (StrUtil.isBlank(value) || names == null || names.isEmpty()) {
            return false;
        }
        String trimmed = value.trim();
        if (names.contains(trimmed)) {
            return true;
        }
        for (String name : names) {
            if (geoNamesMatch(trimmed, name)) {
                return true;
            }
        }
        return false;
    }

    /** 将「富顺/富顺县」等别名规范为部门树中的正式名称，避免报表拆成两行 */
    private String canonicalGeoDisplayName(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "未知";
        }
        String trimmed = raw.trim();
        List<Department> all = departmentService.listAll();
        for (Department dept : all) {
            if (StrUtil.isNotBlank(dept.getName()) && trimmed.equals(dept.getName().trim())) {
                return dept.getName().trim();
            }
        }
        boolean looksTownship = endsWithTownshipSuffix(trimmed);
        String best = null;
        Integer bestLevelScore = null;
        for (Department dept : all) {
            if (StrUtil.isBlank(dept.getName()) || !geoNamesMatch(trimmed, dept.getName())) {
                continue;
            }
            int level = dept.getLevel() == null ? 0 : dept.getLevel();
            int score = looksTownship
                    ? (level >= 3 ? 2 : 1)
                    : (level == 2 ? 2 : 1);
            String deptName = dept.getName().trim();
            if (best == null || score > bestLevelScore
                    || (score == bestLevelScore && deptName.length() > best.length())) {
                best = deptName;
                bestLevelScore = score;
            }
        }
        return best != null ? best : trimmed;
    }

    private boolean endsWithTownshipSuffix(String name) {
        return name.endsWith("镇") || name.endsWith("乡") || name.endsWith("街道")
                || name.endsWith("社区") || name.endsWith("办事处");
    }

    private boolean geoNamesMatch(String a, String b) {
        if (StrUtil.isBlank(a) || StrUtil.isBlank(b)) {
            return false;
        }
        String left = a.trim();
        String right = b.trim();
        if (left.equals(right)) {
            return true;
        }
        String strippedLeft = stripGeoSuffix(left);
        String strippedRight = stripGeoSuffix(right);
        return StrUtil.isNotBlank(strippedLeft) && strippedLeft.equals(strippedRight);
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
            List<String> regionAliases = expandGeoNameAliases(List.of(regionValue));
            wrapper.and(w -> w.in(ScreeningKeyPopulation::getDistrict, regionAliases)
                    .or()
                    .in(ScreeningKeyPopulation::getTownshipCommunity, regionAliases));
        }
        // 有部门筛选时：不再叠加「department_id IN 辖区」硬条件。
        // 筛查记录常挂在区县 department_id，选乡镇时硬过滤会把名称本可匹配的数据全部滤空。
        // resolveFilterDepartmentIds 已与用户辖区取交集，这里按部门 ID + 地理名称匹配即可。
        applyKeyPopulationReportAccess(wrapper, filterDeptIds);
        return screeningKeyPopulationMapper.selectList(wrapper);
    }

    /** 重点人群报表统一数据范围：有部门筛选用地理匹配，否则用常规辖区隔离 */
    private void applyKeyPopulationReportAccess(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper,
                                                List<Long> filterDeptIds) {
        if (departmentFilterSupport.hasActiveFilter(filterDeptIds)) {
            applyKeyPopulationStatsDepartmentFilter(wrapper, filterDeptIds);
            return;
        }
        screeningScopeHelper.applyDepartmentScope(
                wrapper, ScreeningKeyPopulation::getDepartmentId, ScreeningKeyPopulation::getId, "key");
    }

    /**
     * 本报表部门筛选：除 department_id 外，兼容乡镇部门下筛查数据仍挂在区县部门 / department_id 为空的情况，
     * 按部门名称（及去后缀别名）匹配 district / township_community。
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
        List<String> names = resolveDepartmentGeoNames(filterDeptIds);
        wrapper.and(w -> {
            w.in(ScreeningKeyPopulation::getDepartmentId, filterDeptIds);
            if (!names.isEmpty()) {
                w.or().in(ScreeningKeyPopulation::getDistrict, names)
                        .or()
                        .in(ScreeningKeyPopulation::getTownshipCommunity, names);
            }
        });
    }

    /** 部门 ID → 可用于匹配 district / township_community 的名称集合（含去后缀别名） */
    private List<String> resolveDepartmentGeoNames(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        Set<Long> idSet = new HashSet<>(deptIds);
        List<String> rawNames = departmentService.listAll().stream()
                .filter(d -> d.getId() != null && idSet.contains(d.getId()))
                .map(Department::getName)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        return expandGeoNameAliases(rawNames);
    }

    /**
     * 扩展地理名称别名，兼容「富顺/富顺县」「邓井关/邓井关街道」等部门名与筛查字段不一致。
     */
    private List<String> expandGeoNameAliases(List<String> names) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        for (String name : names) {
            if (StrUtil.isBlank(name)) {
                continue;
            }
            String trimmed = name.trim();
            aliases.add(trimmed);
            String normalized = stripGeoSuffix(trimmed);
            if (StrUtil.isNotBlank(normalized)) {
                aliases.add(normalized);
                aliases.add(normalized + "县");
                aliases.add(normalized + "区");
                aliases.add(normalized + "市");
                aliases.add(normalized + "镇");
                aliases.add(normalized + "乡");
                aliases.add(normalized + "街道");
                aliases.add(normalized + "社区");
            }
        }
        return new ArrayList<>(aliases);
    }

    private String stripGeoSuffix(String name) {
        if (StrUtil.isBlank(name)) {
            return name;
        }
        String value = name.trim();
        for (String suffix : List.of("办事处", "街道办事处", "街道", "社区", "镇", "乡", "县", "区", "市")) {
            if (value.endsWith(suffix) && value.length() > suffix.length()) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    /**
     * 推介模块追踪到位人数，按部门归属区县汇总。
     * 一次查询同时累计老年人/老年人+糖尿病与单一糖尿病。
     */
    private ReferralArrivedByDistrict countReferralArrivedByDistrict(String year, List<Long> filterDeptIds) {
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

        Map<String, Long> elder = new HashMap<>();
        Map<String, Long> diabetes = new HashMap<>();
        for (ReferralTracking row : list) {
            boolean isElder = matchReferralCrowdCategory(row.getCrowdCategory(), true);
            boolean isDiabetes = !isElder && matchReferralCrowdCategory(row.getCrowdCategory(), false);
            if (!isElder && !isDiabetes) {
                continue;
            }
            String districtName = resolveReferralDistrict(row, deptDistrictMap);
            if (StrUtil.isBlank(districtName)) {
                districtName = "未知";
            } else {
                districtName = canonicalGeoDisplayName(districtName);
            }
            if (isElder) {
                elder.merge(districtName, 1L, Long::sum);
            } else {
                diabetes.merge(districtName, 1L, Long::sum);
            }
        }
        return new ReferralArrivedByDistrict(elder, diabetes);
    }

    private record ReferralArrivedByDistrict(Map<String, Long> elder, Map<String, Long> diabetes) {
    }

    /**
     * 推介到位统计部门范围：优先使用页面部门筛选（含地理名称兼容）；否则按当前用户辖区隔离。
     */
    private void applyReferralDepartmentScope(LambdaQueryWrapper<ReferralTracking> wrapper,
                                              List<Long> filterDeptIds) {
        if (departmentFilterSupport.hasActiveFilter(filterDeptIds)) {
            if (filterDeptIds.size() == 1
                    && Objects.equals(filterDeptIds.get(0), DepartmentFilterSupport.NO_MATCH_DEPARTMENT_ID)) {
                wrapper.and(w -> w.eq(ReferralTracking::getDepartmentId, DepartmentFilterSupport.NO_MATCH_DEPARTMENT_ID)
                        .and(x -> x.eq(ReferralTracking::getReceiverDeptId, DepartmentFilterSupport.NO_MATCH_DEPARTMENT_ID)));
                return;
            }
            // 推介记录也可能挂在区县部门：按选中部门 ID 匹配；乡镇筛选时到位常为 0 属预期
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

    /**
     * 按地区累加重点人群报表指标（单次遍历记录）。
     */
    private final class KeyPopulationStatAccumulator {
        private long elderCount;
        private long elderChestXrayCount;
        private long elderInfectionScreenCount;
        private long elderSuspiciousSymptomCount;
        private long elderChestXrayAbnormalCount;
        private long elderInfectionAbnormalCount;
        private long elderReferralFormCount;
        private long elderConfirmedTbCount;
        private long diabetesCount;
        private long diabetesChestXrayCount;
        private long diabetesInfectionScreenCount;
        private long diabetesSuspiciousSymptomCount;
        private long diabetesChestXrayAbnormalCount;
        private long diabetesInfectionAbnormalCount;
        private long diabetesReferralFormCount;
        private long diabetesConfirmedTbCount;

        void accept(ScreeningKeyPopulation r) {
            if (isElderGroup(r)) {
                elderCount++;
                // 与原 countKeyEquals 一致：精确等于「是」，不 trim
                if ("是".equals(r.getHasChestXray())) {
                    elderChestXrayCount++;
                }
                if ("是".equals(r.getHasInfectionScreen())) {
                    elderInfectionScreenCount++;
                }
                if (hasSuspiciousSymptom(r)) {
                    elderSuspiciousSymptomCount++;
                }
                if (isChestXrayAbnormal(r)) {
                    elderChestXrayAbnormalCount++;
                }
                if (isInfectionAbnormal(r.getInfectionResult())) {
                    elderInfectionAbnormalCount++;
                }
                if (ScreeningDiagnosisSupport.isSuspectedTbDiagnosis(r.getDiagnosisFirst())) {
                    elderReferralFormCount++;
                }
                if (isConfirmedTb(r)) {
                    elderConfirmedTbCount++;
                }
                return;
            }
            if (!isDiabetesOnly(r)) {
                return;
            }
            diabetesCount++;
            if ("是".equals(r.getHasChestXray())) {
                diabetesChestXrayCount++;
            }
            if ("是".equals(r.getHasInfectionScreen())) {
                diabetesInfectionScreenCount++;
            }
            if (hasSuspiciousSymptom(r)) {
                diabetesSuspiciousSymptomCount++;
            }
            if (isChestXrayAbnormal(r)) {
                diabetesChestXrayAbnormalCount++;
            }
            if (isInfectionAbnormal(r.getInfectionResult())) {
                diabetesInfectionAbnormalCount++;
            }
            if (ScreeningDiagnosisSupport.isSuspectedTbDiagnosis(r.getDiagnosisFirst())) {
                diabetesReferralFormCount++;
            }
            if (isConfirmedTb(r)) {
                diabetesConfirmedTbCount++;
            }
        }

        KeyPopulationTbSymptomReferralStatisticsVO toVo(String district, long elderArrived, long diabetesArrived) {
            return KeyPopulationTbSymptomReferralStatisticsVO.builder()
                    .district(district)
                    // 老年人数：季度报表模板无系统数据来源，留空由页面手工填写
                    .elderCount(null)
                    // 参加年度体检人数 / 进行症状筛查人数：老年人及老年人+糖尿病总人数
                    .elderAnnualExamCount(elderCount)
                    .elderSymptomScreenCount(elderCount)
                    .elderChestXrayCount(elderChestXrayCount)
                    .elderInfectionScreenCount(elderInfectionScreenCount)
                    .elderSuspiciousSymptomCount(elderSuspiciousSymptomCount)
                    .elderChestXrayAbnormalCount(elderChestXrayAbnormalCount)
                    .elderInfectionAbnormalCount(elderInfectionAbnormalCount)
                    .elderReferralFormCount(elderReferralFormCount)
                    .elderArrivedCount(elderArrived)
                    .elderConfirmedTbCount(elderConfirmedTbCount)
                    // 糖尿病：管理数 / 季度随访 / 症状筛查 口径同模板均为单选糖尿病人群总数
                    .diabetesManagedCount(diabetesCount)
                    .diabetesQuarterFollowCount(diabetesCount)
                    .diabetesSymptomScreenCount(diabetesCount)
                    .diabetesChestXrayCount(diabetesChestXrayCount)
                    .diabetesInfectionScreenCount(diabetesInfectionScreenCount)
                    .diabetesSuspiciousSymptomCount(diabetesSuspiciousSymptomCount)
                    .diabetesChestXrayAbnormalCount(diabetesChestXrayAbnormalCount)
                    .diabetesInfectionAbnormalCount(diabetesInfectionAbnormalCount)
                    .diabetesReferralFormCount(diabetesReferralFormCount)
                    .diabetesArrivedCount(diabetesArrived)
                    .diabetesConfirmedTbCount(diabetesConfirmedTbCount)
                    .build();
        }
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

    private boolean hasSuspiciousSymptom(ScreeningKeyPopulation r) {
        String val = StrUtil.trim(r.getHasSuspiciousSymptoms());
        return "有".equals(val) || "是".equals(val);
    }

    private boolean isChestXrayAbnormal(ScreeningKeyPopulation r) {
        String val = r.getChestXrayResult();
        return StrUtil.isNotBlank(val) && val.contains("异常");
    }

    /**
     * 感染筛查异常：官方结果判定阳性档及历史文案（PPD++/+++、EC/IGRA阳性等）。
     */
    private boolean isInfectionAbnormal(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) {
            return false;
        }
        // 官方结果判定 / 历史文案统一按阳性档识别
        return ScreeningDiagnosisSupport.isPositiveInfection(infectionResult);
    }

    private boolean isConfirmedTb(ScreeningKeyPopulation r) {
        String diag = StrUtil.trim(r.getDiagnosisFirst());
        if (StrUtil.isBlank(diag)) {
            return false;
        }
        return "确诊患者".equals(diag)
                || "确诊结核".equals(diag)
                || "确诊肺结核".equals(diag)
                || "在治患者".equals(diag);
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
