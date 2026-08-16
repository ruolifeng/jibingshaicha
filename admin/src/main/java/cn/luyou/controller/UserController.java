package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import cn.luyou.model.vo.UserInfoVO;
import cn.luyou.service.OperationLogService;
import cn.luyou.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OperationLogService operationLogService;
    private final UserMapper userMapper;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResultResponse<String> login(
            @NotBlank(message = "用户名不能为空") @RequestParam String username,
            @NotBlank(message = "密码不能为空") @RequestParam String password,
            HttpServletRequest request) {
        // 登录接口走显式记录（AOP 之前 BaseContext 尚未注入，无法用 @OperationLog 注解）
        cn.luyou.model.OperationLog logEntity = new cn.luyou.model.OperationLog();
        logEntity.setOpType("login");
        logEntity.setOpModule("system");
        logEntity.setOpAction("用户登录");
        logEntity.setRequestMethod(request.getMethod());
        logEntity.setRequestUrl(request.getRequestURI());
        logEntity.setUserName(username);
        String ua = request.getHeader("User-Agent");
        if (ua != null) logEntity.setUserAgent(ua.length() > 256 ? ua.substring(0, 256) : ua);
        logEntity.setIp(getClientIp(request));
        try {
            String token = userService.login(username, password);
            // 登录成功后补全用户信息
            User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (u != null) {
                logEntity.setUserId(u.getId());
                logEntity.setRealName(u.getRealName());
                logEntity.setRole(u.getRole());
                logEntity.setDepartmentId(u.getDepartmentId());
            }
            logEntity.setResultStatus(1);
            operationLogService.saveAsync(logEntity);
            return ResultRes.success(token);
        } catch (RuntimeException e) {
            logEntity.setResultStatus(0);
            String msg = e.getMessage();
            if (msg != null && msg.length() > 2048) msg = msg.substring(0, 2048);
            logEntity.setErrorMessage(msg);
            operationLogService.saveAsync(logEntity);
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null || ip.length() <= 64 ? ip : ip.substring(0, 64);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public ResultResponse<UserInfoVO> getCurrentUser() {
        UserInfoVO userInfo = userService.getCurrentUserInfo();
        return ResultRes.success(userInfo);
    }

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public ResultResponse<IPage<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer role) {
        userService.checkAnyPermissionCode("system:users", "user:create", "user:edit", "user:delete");
        return ResultRes.success(userService.queryPage(page, size, username, role));
    }

    @Operation(summary = "创建用户")
    @PostMapping("/create")
    @OperationLog(type = "create", module = "system", action = "创建用户")
    public ResultResponse<Void> create(@RequestBody User user) {
        userService.checkPermissionCode("user:create");
        userService.createUser(user);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    @OperationLog(type = "update", module = "system", action = "更新用户")
    public ResultResponse<Void> update(@RequestBody User user) {
        userService.checkPermissionCode("user:edit");
        userService.updateUser(user);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新当前登录用户个人信息")
    @PutMapping("/profile")
    @OperationLog(type = "update", module = "system", action = "更新个人信息")
    public ResultResponse<Void> updateProfile(@RequestBody User user) {
        userService.updateCurrentUser(user);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "delete", module = "system", action = "删除用户")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        userService.checkPermissionCode("user:delete");
        userService.deleteUser(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "获取五级机构用户列表（通知单接收单位）")
    @GetMapping("/level5-users")
    public ResultResponse<List<UserInfoVO>> getLevel5Users() {
        return ResultRes.success(userService.getLevel5Users());
    }

    @Operation(summary = "获取转出接收方用户列表（二/三/四/五级，部门-用户树）")
    @GetMapping("/referral-receiver-users")
    public ResultResponse<List<UserInfoVO>> getReferralReceiverUsers() {
        return ResultRes.success(userService.getReferralReceiverUsers());
    }

    @Operation(summary = "获取一至五级用户列表（推介接收人选择）")
    @GetMapping("/level34-users")
    public ResultResponse<List<UserInfoVO>> getLevel34Users() {
        return ResultRes.success(userService.getLevel34Users());
    }

    @Operation(summary = "当前用户所属区县下的三级用户（大疫情跨镇导入选人）")
    @GetMapping("/county-level3-users")
    public ResultResponse<List<UserInfoVO>> getCurrentCountyLevel3Users() {
        return ResultRes.success(userService.getCurrentCountyLevel3Users());
    }
}
