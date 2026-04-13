package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.User;
import cn.luyou.model.vo.UserInfoVO;
import cn.luyou.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResultResponse<String> login(
            @NotBlank(message = "用户名不能为空") @RequestParam String username,
            @NotBlank(message = "密码不能为空") @RequestParam String password) {
        String token = userService.login(username, password);
        return ResultRes.success(token);
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
        userService.checkPermission(1);
        return ResultRes.success(userService.queryPage(page, size, username, role));
    }

    @Operation(summary = "创建用户")
    @PostMapping("/create")
    public ResultResponse<Void> create(@RequestBody User user) {
        userService.checkPermission(1);
        userService.createUser(user);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    public ResultResponse<Void> update(@RequestBody User user) {
        userService.checkPermission(1);
        userService.updateUser(user);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{id}")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        userService.checkPermission(1);
        userService.deleteUser(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "获取五级机构用户列表（通知单接收单位）")
    @GetMapping("/level5-users")
    public ResultResponse<List<UserInfoVO>> getLevel5Users() {
        return ResultRes.success(userService.getLevel5Users());
    }
}
