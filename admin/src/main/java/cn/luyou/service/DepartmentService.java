package cn.luyou.service;

import cn.luyou.model.Department;
import cn.luyou.model.ImportResult;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    /** 获取全部未删除部门列表 */
    List<Department> listAll();

    /**
     * 获取指定部门及其所有子孙部门的 ID 集合（含自身）。
     * 用于上级部门查看下级部门数据时的权限过滤。
     */
    List<Long> getDescendantIds(Long deptId);

    /**
     * 解析所属区县部门 ID：沿上级走到 level=2；若本身已是区县则返回自身。
     * 市级（level=1）或无法解析时返回 null。
     */
    Long resolveDistrictId(Long deptId);

    /** 按部门名称模糊匹配，返回部门 ID 列表（用于录入单位筛选） */
    List<Long> resolveIdsByNameLike(String name);

    /** 创建部门 */
    void createDepartment(Department department);

    /** 更新部门 */
    void updateDepartment(Department department);

    /** 删除部门（逻辑删除） */
    void deleteDepartment(Long id);

    /** 批量导入部门 */
    ImportResult importDepartments(MultipartFile file);
}
