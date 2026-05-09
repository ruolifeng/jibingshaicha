package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.DepartmentMapper;
import cn.luyou.model.Department;
import cn.luyou.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
}
