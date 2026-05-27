package cn.luyou.service.impl;

import cn.luyou.mapper.PermissionMapper;
import cn.luyou.mapper.RolePermissionMapper;
import cn.luyou.mapper.UserPermissionMapper;
import cn.luyou.model.Permission;
import cn.luyou.model.RolePermission;
import cn.luyou.model.UserPermission;
import cn.luyou.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final RolePermissionMapper rolePermissionMapper;
    private final UserPermissionMapper userPermissionMapper;

    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> allPermissions = list(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
        List<Permission> visiblePermissions = filterDeprecatedPermissions(allPermissions);
        return buildTree(visiblePermissions, 0L);
    }

    /** 过滤已废弃权限及其全部子节点（name 以 [废弃] 开头） */
    private List<Permission> filterDeprecatedPermissions(List<Permission> all) {
        Set<Long> deprecatedIds = all.stream()
                .filter(this::isDeprecatedPermission)
                .map(Permission::getId)
                .collect(Collectors.toCollection(HashSet::new));

        boolean changed;
        do {
            changed = false;
            for (Permission permission : all) {
                Long parentId = permission.getParentId();
                if (parentId != null && parentId > 0
                        && deprecatedIds.contains(parentId)
                        && deprecatedIds.add(permission.getId())) {
                    changed = true;
                }
            }
        } while (changed);

        return all.stream()
                .filter(p -> !deprecatedIds.contains(p.getId()))
                .toList();
    }

    private boolean isDeprecatedPermission(Permission permission) {
        return permission.getName() != null && permission.getName().startsWith("[废弃]");
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

    @Override
    public List<Long> getUserPermissionIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userPermissionMapper.selectList(
                        new LambdaQueryWrapper<UserPermission>().eq(UserPermission::getUserId, userId))
                .stream()
                .map(UserPermission::getPermissionId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserPermissions(Long userId, List<Long> permissionIds) {
        userPermissionMapper.delete(
                new LambdaQueryWrapper<UserPermission>().eq(UserPermission::getUserId, userId)
        );
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long pid : permissionIds) {
                userPermissionMapper.insert(UserPermission.builder()
                        .userId(userId)
                        .permissionId(pid)
                        .build());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAllUserPermissions(Long userId) {
        if (userId == null) return;
        userPermissionMapper.delete(
                new LambdaQueryWrapper<UserPermission>().eq(UserPermission::getUserId, userId)
        );
    }

    @Override
    public List<String> getEffectivePermissionCodes(int role, Long userId) {
        LinkedHashSet<String> codes = new LinkedHashSet<>(getRolePermissionCodes(role));
        if (userId != null) {
            List<Long> extraIds = getUserPermissionIds(userId);
            if (!extraIds.isEmpty()) {
                LambdaQueryWrapper<Permission> w = new LambdaQueryWrapper<>();
                w.in(Permission::getId, extraIds);
                list(w).stream().map(Permission::getCode).forEach(codes::add);
            }
        }
        return new ArrayList<>(codes);
    }

    private List<Permission> buildTree(List<Permission> all, Long parentId) {
        Map<Long, List<Permission>> grouped = all.stream()
                .collect(Collectors.groupingBy(p -> {
                    Long pid = p.getParentId();
                    return (pid == null || pid == 0L) ? 0L : pid;
                }));
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
