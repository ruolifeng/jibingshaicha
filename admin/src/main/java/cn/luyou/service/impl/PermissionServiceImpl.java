package cn.luyou.service.impl;

import cn.luyou.mapper.PermissionMapper;
import cn.luyou.mapper.RolePermissionMapper;
import cn.luyou.model.Permission;
import cn.luyou.model.RolePermission;
import cn.luyou.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> allPermissions = list(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
        return buildTree(allPermissions, 0L);
    }

    @Override
    public List<Long> getRolePermissionIds(int role) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRole, role);
        return rolePermissionMapper.selectList(wrapper)
                .stream()
                .map(RolePermission::getPermissionId)
                .toList();
    }

    @Override
    public List<String> getRolePermissionCodes(int role) {
        List<Long> permIds = getRolePermissionIds(role);
        if (permIds.isEmpty()) return List.of();
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Permission::getId, permIds);
        return list(wrapper).stream().map(Permission::getCode).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolePermissions(int role, List<Long> permissionIds) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRole, role)
        );
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> records = permissionIds.stream()
                    .map(pid -> RolePermission.builder().role(role).permissionId(pid).build())
                    .toList();
            for (RolePermission rp : records) {
                rolePermissionMapper.insert(rp);
            }
        }
    }

    private List<Permission> buildTree(List<Permission> all, Long parentId) {
        Map<Long, List<Permission>> grouped = all.stream()
                .collect(Collectors.groupingBy(Permission::getParentId));
        return buildChildren(grouped, parentId);
    }

    private List<Permission> buildChildren(Map<Long, List<Permission>> grouped, Long parentId) {
        List<Permission> children = grouped.getOrDefault(parentId, new ArrayList<>());
        children.sort(Comparator.comparingInt(Permission::getSort));
        for (Permission child : children) {
            child.setChildren(buildChildren(grouped, child.getId()));
        }
        return children;
    }
}
