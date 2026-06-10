package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.DepartmentMapper;
import cn.luyou.model.Department;
import cn.luyou.model.ImportResult;
import cn.luyou.service.DepartmentService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
        implements DepartmentService {

    @Override
    public List<Department> listAll() {
        return lambdaQuery().orderByAsc(Department::getCreateTime).list();
    }

    @Override
    public void createDepartment(Department department) {
        if (StrUtil.isBlank(department.getName())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "部门名称不能为空");
        }
        boolean exists = lambdaQuery()
                .eq(Department::getName, department.getName().trim())
                .exists();
        if (exists) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "部门名称已存在");
        }
        department.setName(department.getName().trim());
        validateThreeLevelHierarchy(department);
        save(department);
    }

    @Override
    public void updateDepartment(Department department) {
        Department existing = getById(department.getId());
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "部门不存在");
        }
        if (StrUtil.isNotBlank(department.getName())) {
            String newName = department.getName().trim();
            boolean nameConflict = lambdaQuery()
                    .eq(Department::getName, newName)
                    .ne(Department::getId, department.getId())
                    .exists();
            if (nameConflict) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "部门名称已存在");
            }
            department.setName(newName);
        }
        if (department.getLevel() == null) {
            department.setLevel(existing.getLevel() != null ? existing.getLevel() : 1);
        }
        validateThreeLevelHierarchy(department);
        updateById(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "部门不存在");
        }
        boolean hasChildren = lambdaQuery().eq(Department::getParentId, id).exists();
        if (hasChildren) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请先删除或调整下级部门后再删除本部门");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importDepartments(MultipartFile file) {
        List<Map<Integer, String>> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    rows.add(new LinkedHashMap<>(data));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // no-op
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel读取失败");
        }

        ImportResult result = new ImportResult();
        if (rows.size() < 2) {
            return result;
        }

        Map<String, Integer> headerIndex = resolveHeaderIndex(rows.get(0));
        List<DepartmentImportRow> dataRows = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            String name = getCell(row, headerIndex, "部门名称", "名称");
            if (StrUtil.isBlank(name)) {
                continue;
            }
            Integer level = parseLevel(getCell(row, headerIndex, "层级", "部门层级"));
            String parentName = getCell(row, headerIndex, "上级部门", "上级部门名称", "父级部门");
            String description = getCell(row, headerIndex, "描述", "备注");
            if (level == null) {
                result.addError(i + 1, name, "部门层级须为 1/2/3 或 市级/区县/社区");
                continue;
            }
            dataRows.add(new DepartmentImportRow(i + 1, name.trim(), level, StrUtil.trim(parentName), StrUtil.trim(description)));
        }

        for (int level = 1; level <= 3; level++) {
            for (DepartmentImportRow row : dataRows) {
                if (row.level() != level) {
                    continue;
                }
                if (lambdaQuery().eq(Department::getName, row.name()).exists()) {
                    result.addError(row.rowNum(), row.name(), "部门名称已存在，已跳过");
                    continue;
                }
                Long parentId = null;
                if (level > 1) {
                    if (StrUtil.isBlank(row.parentName())) {
                        result.addError(row.rowNum(), row.name(), "区县或社区部门必须填写上级部门");
                        continue;
                    }
                    Department parent = findParentByName(row.parentName(), level - 1);
                    if (parent == null) {
                        result.addError(row.rowNum(), row.name(), "未找到匹配层级的上级部门：" + row.parentName());
                        continue;
                    }
                    parentId = parent.getId();
                }
                Department department = Department.builder()
                        .name(row.name())
                        .description(row.description())
                        .level(level)
                        .parentId(parentId)
                        .build();
                try {
                    createDepartment(department);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (ServiceException e) {
                    result.addError(row.rowNum(), row.name(), e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 三级行政区划：1 市级 → 2 区县 → 3 社区。同级区县数据隔离，仅上级可看全部下级。
     */
    private void validateThreeLevelHierarchy(Department dept) {
        Integer level = dept.getLevel();
        if (level == null) {
            level = 1;
        }
        if (level < 1 || level > 3) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "部门层级须为 1（市级）、2（区县）或 3（社区）");
        }
        dept.setLevel(level);

        if (level == 1) {
            dept.setParentId(null);
            return;
        }

        Long parentId = dept.getParentId();
        if (parentId == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "区县或社区部门必须选择上级部门");
        }
        Department parent = getById(parentId);
        if (parent == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "上级部门不存在");
        }
        Integer pl = parent.getLevel();
        if (pl == null) {
            pl = 1;
        }
        if (level == 2 && pl != 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "区县的上级必须是市级部门");
        }
        if (level == 3 && pl != 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "社区的上级必须是区县部门");
        }
    }

    private Map<String, Integer> resolveHeaderIndex(Map<Integer, String> headerRow) {
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }
        return headerIndex;
    }

    private String getCell(Map<Integer, String> row, Map<String, Integer> headerIndex, String... names) {
        for (String name : names) {
            Integer index = headerIndex.get(name);
            if (index != null && StrUtil.isNotBlank(row.get(index))) {
                return row.get(index).trim();
            }
        }
        return null;
    }

    private Integer parseLevel(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        if ("1".equals(value) || value.contains("市")) {
            return 1;
        }
        if ("2".equals(value) || value.contains("区") || value.contains("县")) {
            return 2;
        }
        if ("3".equals(value) || value.contains("社区") || value.contains("街道") || value.contains("乡镇")) {
            return 3;
        }
        return null;
    }

    private Department findParentByName(String parentName, int parentLevel) {
        return lambdaQuery()
                .eq(Department::getName, parentName)
                .eq(Department::getLevel, parentLevel)
                .last("LIMIT 1")
                .one();
    }

    private record DepartmentImportRow(int rowNum, String name, Integer level, String parentName, String description) {}

    @Override
    public List<Long> getDescendantIds(Long deptId) {
        List<Long> result = new ArrayList<>();
        if (deptId == null) {
            return result;
        }
        // 加载所有部门，在内存中做树遍历，避免多次数据库查询
        List<Department> allDepts = lambdaQuery().list();
        collectDescendants(deptId, allDepts, result);
        return result;
    }

    /**
     * 递归收集指定部门及其所有子孙部门的 ID（含自身）
     */
    private void collectDescendants(Long deptId, List<Department> allDepts, List<Long> result) {
        result.add(deptId);
        for (Department dept : allDepts) {
            if (deptId.equals(dept.getParentId())) {
                collectDescendants(dept.getId(), allDepts, result);
            }
        }
    }

    @Override
    public List<Long> resolveIdsByNameLike(String name) {
        if (StrUtil.isBlank(name)) {
            return List.of();
        }
        return lambdaQuery()
                .like(Department::getName, name.trim())
                .select(Department::getId)
                .list()
                .stream()
                .map(Department::getId)
                .toList();
    }
}
