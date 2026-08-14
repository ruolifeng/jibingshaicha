package cn.luyou.utils;

import cn.luyou.model.Department;
import cn.luyou.model.vo.DepartmentFilterOptionVO;
import cn.luyou.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统计分析部门筛选：按当前用户层级返回可选项，并将多选部门展开为数据过滤 ID（含下级、与辖区取交集）。
 */
@Component
@RequiredArgsConstructor
public class DepartmentFilterSupport {

    /** 无匹配部门时用于返回空结果 */
    public static final long NO_MATCH_DEPARTMENT_ID = -1L;

    private final DepartmentService departmentService;

    /**
     * 解析前端传入的部门 ID（逗号分隔）。未传或非法时返回 null 表示不按部门缩小范围。
     * 选中项会展开为「自身 + 全部下级」，再与当前用户可见辖区取交集。
     */
    public List<Long> resolveFilterDepartmentIds(String departmentIdsParam) {
        if (!StringUtils.hasText(departmentIdsParam)) {
            return null;
        }
        List<Long> selected = parseIdList(departmentIdsParam);
        if (selected.isEmpty()) {
            return null;
        }
        Set<Long> userScope = new HashSet<>(resolveUserScopeDepartmentIds());
        if (userScope.isEmpty() && !BaseContext.isSuperAdmin()) {
            return List.of(NO_MATCH_DEPARTMENT_ID);
        }
        Set<Long> expanded = new LinkedHashSet<>();
        for (Long id : selected) {
            if (!isAllowedSelection(id, userScope)) {
                continue;
            }
            expanded.addAll(departmentService.getDescendantIds(id));
        }
        if (expanded.isEmpty()) {
            return List.of(NO_MATCH_DEPARTMENT_ID);
        }
        if (!userScope.isEmpty()) {
            expanded.retainAll(userScope);
        }
        if (expanded.isEmpty()) {
            return List.of(NO_MATCH_DEPARTMENT_ID);
        }
        return new ArrayList<>(expanded);
    }

    /**
     * 解析用户勾选的部门 ID（不展开下级）。用于报表分行：区县+下属镇同时勾选时，需按镇单独出一行。
     */
    public List<Long> parseSelectedDepartmentIds(String departmentIdsParam) {
        if (!StringUtils.hasText(departmentIdsParam)) {
            return List.of();
        }
        List<Long> selected = parseIdList(departmentIdsParam);
        if (selected.isEmpty()) {
            return List.of();
        }
        Set<Long> userScope = new HashSet<>(resolveUserScopeDepartmentIds());
        List<Long> allowed = new ArrayList<>();
        for (Long id : selected) {
            if (isAllowedSelection(id, userScope)) {
                allowed.add(id);
            }
        }
        return allowed;
    }

    /**
     * 返回当前用户可用于统计筛选的部门树：
     * 市级 → 区县 + 下属社区；区县 → 本区县社区；社区级 → 空（前端隐藏筛选）。
     */
    public List<DepartmentFilterOptionVO> getStatisticsFilterOptions() {
        List<Department> all = departmentService.listAll();
        if (all.isEmpty()) {
            return List.of();
        }
        if (BaseContext.isSuperAdmin()) {
            return buildCityFilterTree(all, findCityRootId(all));
        }
        Long currentDeptId = BaseContext.getCurrentDepartmentId();
        if (currentDeptId == null) {
            return List.of();
        }
        Department current = all.stream()
                .filter(d -> currentDeptId.equals(d.getId()))
                .findFirst()
                .orElse(null);
        if (current == null || current.getLevel() == null) {
            return List.of();
        }
        return switch (current.getLevel()) {
            case 1 -> buildCityFilterTree(all, current.getId());
            case 2 -> buildDistrictFilterOptions(all, current.getId());
            default -> List.of();
        };
    }

    /** 在已有数据权限范围上叠加部门筛选（AND） */
    public <T> void applyDepartmentIdFilter(LambdaQueryWrapper<T> wrapper,
                                            SFunction<T, Long> departmentColumn,
                                            List<Long> filterDeptIds) {
        if (filterDeptIds == null || filterDeptIds.isEmpty()) {
            return;
        }
        wrapper.in(departmentColumn, filterDeptIds);
    }

    public boolean hasActiveFilter(List<Long> filterDeptIds) {
        return filterDeptIds != null && !filterDeptIds.isEmpty();
    }

    private List<Long> resolveUserScopeDepartmentIds() {
        if (BaseContext.isSuperAdmin()) {
            return departmentService.listAll().stream()
                    .map(Department::getId)
                    .filter(Objects::nonNull)
                    .toList();
        }
        Long deptId = BaseContext.getCurrentDepartmentId();
        if (deptId == null) {
            return List.of();
        }
        return departmentService.getDescendantIds(deptId);
    }

    private boolean isAllowedSelection(Long deptId, Set<Long> userScope) {
        if (BaseContext.isSuperAdmin()) {
            Department dept = departmentService.getById(deptId);
            return dept != null && dept.getLevel() != null && dept.getLevel() >= 2;
        }
        return userScope.contains(deptId);
    }

    private Long findCityRootId(List<Department> all) {
        return all.stream()
                .filter(d -> d.getLevel() != null && d.getLevel() == 1)
                .map(Department::getId)
                .findFirst()
                .orElse(null);
    }

    /** 市级视角：区县为根，其下挂社区 */
    private List<DepartmentFilterOptionVO> buildCityFilterTree(List<Department> all, Long cityRootId) {
        if (cityRootId == null) {
            return all.stream()
                    .filter(d -> d.getLevel() != null && d.getLevel() == 2)
                    .map(d -> toOption(d, buildChildren(all, d.getId(), 3)))
                    .collect(Collectors.toList());
        }
        return all.stream()
                .filter(d -> cityRootId.equals(d.getParentId()) && d.getLevel() != null && d.getLevel() == 2)
                .map(d -> toOption(d, buildChildren(all, d.getId(), 3)))
                .collect(Collectors.toList());
    }

    /** 区县视角：仅本区县下属社区（扁平列表，无 children） */
    private List<DepartmentFilterOptionVO> buildDistrictFilterOptions(List<Department> all, Long districtId) {
        return all.stream()
                .filter(d -> districtId.equals(d.getParentId()) && d.getLevel() != null && d.getLevel() == 3)
                .map(d -> toOption(d, List.of()))
                .collect(Collectors.toList());
    }

    private List<DepartmentFilterOptionVO> buildChildren(List<Department> all, Long parentId, int childLevel) {
        return all.stream()
                .filter(d -> parentId.equals(d.getParentId()) && d.getLevel() != null && d.getLevel() == childLevel)
                .map(d -> toOption(d, List.of()))
                .collect(Collectors.toList());
    }

    private DepartmentFilterOptionVO toOption(Department dept, List<DepartmentFilterOptionVO> children) {
        DepartmentFilterOptionVO vo = new DepartmentFilterOptionVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setLevel(dept.getLevel());
        vo.setParentId(dept.getParentId());
        if (children != null && !children.isEmpty()) {
            vo.setChildren(children);
        }
        return vo;
    }

    private List<Long> parseIdList(String raw) {
        String[] parts = raw.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.matches("\\d+")) {
                continue;
            }
            ids.add(Long.valueOf(trimmed));
        }
        return ids;
    }
}
