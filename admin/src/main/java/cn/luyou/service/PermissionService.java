package cn.luyou.service;

import cn.luyou.model.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PermissionService extends IService<Permission> {

    /** 获取全部权限（树形结构） */
    List<Permission> getPermissionTree();

    /** 获取指定角色拥有的权限ID列表 */
    List<Long> getRolePermissionIds(int role);

    /** 获取指定角色拥有的权限编码列表 */
    List<String> getRolePermissionCodes(int role);

    /** 分配角色权限（全量替换） */
    void assignRolePermissions(int role, List<Long> permissionIds);
}
