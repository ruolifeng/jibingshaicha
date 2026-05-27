package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Permission;
import cn.luyou.service.PermissionService;
import cn.luyou.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "权限管理")
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    private final UserService userService;

    @Operation(summary = "获取权限树")
    @GetMapping("/tree")
    public ResultResponse<List<Permission>> tree() {
        return ResultRes.success(permissionService.getPermissionTree());
    }

    @Operation(summary = "获取可分配权限的同部门用户列表")
    @GetMapping("/users")
    public ResultResponse<List<cn.luyou.model.vo.UserInfoVO>> listAssignableUsers() {
        userService.checkPermissionCode("system:permissions");
        return ResultRes.success(userService.listSameDepartmentUsers());
    }

    @Operation(summary = "获取角色已分配权限ID")
    @GetMapping("/role/{role}")
    public ResultResponse<List<Long>> getRolePermissions(@PathVariable int role) {
        return ResultRes.success(permissionService.getRolePermissionIds(role));
    }

    @Operation(summary = "分配角色权限")
    @PostMapping("/assign")
    public ResultResponse<Void> assign(@RequestBody Map<String, Object> params) {
        userService.checkPermissionCode("permission:assign");
        int role = (int) params.get("role");
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) params.get("permissionIds");
        List<Long> permissionIds = ids.stream().map(Number::longValue).toList();
        permissionService.assignRolePermissions(role, permissionIds);
        return ResultRes.success(null);
    }

    @Operation(summary = "获取用户额外分配的权限ID（不含角色默认权限）")
    @GetMapping("/user/{userId}")
    public ResultResponse<List<Long>> getUserPermissions(@PathVariable Long userId) {
        userService.checkPermissionCode("system:permissions");
        userService.assertSameDepartmentAccess(userId);
        return ResultRes.success(permissionService.getUserPermissionIds(userId));
    }

    @Operation(summary = "分配用户额外权限（全量替换，与角色权限合并生效）")
    @PostMapping("/assign-user")
    public ResultResponse<Void> assignUser(@RequestBody Map<String, Object> params) {
        userService.checkPermissionCode("permission:assign");
        long userId = ((Number) params.get("userId")).longValue();
        userService.assertSameDepartmentAccess(userId);
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) params.get("permissionIds");
        List<Long> permissionIds = ids == null ? List.of() : ids.stream().map(Number::longValue).toList();
        permissionService.assignUserPermissions(userId, permissionIds);
        return ResultRes.success(null);
    }
}
