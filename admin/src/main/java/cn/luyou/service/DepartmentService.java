package cn.luyou.service;

import cn.luyou.model.Department;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    /** 获取全部未删除部门列表 */
    List<Department> listAll();

    /**
     * 获取指定部门及其所有子孙部门的 ID 集合（含自身）。
     * 用于上级部门查看下级部门数据时的权限过滤。
     */
    List<Long> getDescendantIds(Long deptId);

    /** 创建部门 */
    void createDepartment(Department department);

    /** 更新部门 */
    void updateDepartment(Department department);

    /** 删除部门（逻辑删除） */
    void deleteDepartment(Long id);
}
