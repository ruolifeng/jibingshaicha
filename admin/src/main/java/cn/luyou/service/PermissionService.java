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

    /** 用户额外权限 ID 列表 */
    List<Long> getUserPermissionIds(Long userId);

    /** 全量替换某用户的额外权限 */
    void assignUserPermissions(Long userId, List<Long> permissionIds);

    /** 删除某用户全部额外权限（用户删除时调用） */
    void removeAllUserPermissions(Long userId);

    /**
     * 有效权限码：角色权限 ∪ 用户额外权限。超级管理员仍只走角色表（与现有逻辑一致）。
     */
    List<String> getEffectivePermissionCodes(int role, Long userId);
}
