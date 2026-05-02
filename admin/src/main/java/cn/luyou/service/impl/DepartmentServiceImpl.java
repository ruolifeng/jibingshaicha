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
        updateById(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department existing = getById(id);
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "部门不存在");
        }
        removeById(id);
    }
}
