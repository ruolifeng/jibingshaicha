package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import cn.luyou.model.vo.UserInfoVO;
import cn.luyou.service.PermissionService;
import cn.luyou.service.UserService;
import cn.luyou.utils.BaseContext;
import cn.luyou.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private static final Map<Integer, String> ROLE_NAME_MAP = Map.of(
            1, "超级管理员",
            2, "一级",
            3, "二级",
            4, "三级",
            5, "四级",
            6, "五级"
    );

    @Override
    public String login(String username, String password) {
        User user = lambdaQuery()
                .eq(User::getUsername, username)
                .one();

        if (user == null) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED, "用户名或密码错误");
        }

        if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED, "用户名或密码错误");
        }

        return jwtUtil.generateToken(user.getId());
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = BaseContext.getCurrentId();
        User user = getById(userId);
        if (user == null) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED, "用户不存在");
        }
        return buildUserInfoVO(user);
    }

    @Override
    public IPage<User> queryPage(int page, int size, String username, Integer role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(username), User::getUsername, username)
                .eq(role != null, User::getRole, role)
                .orderByAsc(User::getRole)
                .orderByDesc(User::getCreateTime);
        if (!BaseContext.isSuperAdmin()) {
            wrapper.eq(User::getDepartmentId, BaseContext.getCurrentDepartmentId());
        }
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public void createUser(User user) {
        Long count = lambdaQuery().eq(User::getUsername, user.getUsername()).count();
        if (count > 0) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "用户名已存在");
        }
        if (StrUtil.isBlank(user.getPassword())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "密码不能为空");
        }
        if (user.getRole() == null) {
            user.setRole(6);
        }
        user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        save(user);
    }

    @Override
    public void updateUser(User user) {
        User existing = getById(user.getId());
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "用户不存在");
        }
        if (StrUtil.isNotBlank(user.getUsername()) && !user.getUsername().equals(existing.getUsername())) {
            Long count = lambdaQuery().eq(User::getUsername, user.getUsername()).count();
            if (count > 0) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "用户名已存在");
            }
        }
        // 仅当传入了新密码时才重新加密，否则保持原密码不变
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "用户不存在");
        }
        if (user.getRole() == 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "不能删除超级管理员");
        }
        permissionService.removeAllUserPermissions(id);
        removeById(id);
    }

    @Override
    public List<UserInfoVO> getLevel5Users() {
        // 通知单/转诊接收单位为五级机构（role=6），不限部门，允许跨部门发送
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, 6);
        List<User> users = list(wrapper);
        return users.stream().map(this::buildUserInfoVO).toList();
    }

    @Override
    public List<UserInfoVO> getLevel34Users() {
        // 三级（role=4）、四级（role=5）用户，用于推介追踪接收人选择
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .in(User::getRole, java.util.Arrays.asList(4, 5));
        List<User> users = list(wrapper);
        return users.stream().map(this::buildUserInfoVO).toList();
    }

    @Override
    public List<UserInfoVO> listSameDepartmentUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByAsc(User::getRole)
                .orderByDesc(User::getCreateTime);
        if (!BaseContext.isSuperAdmin()) {
            Long deptId = BaseContext.getCurrentDepartmentId();
            if (deptId == null) {
                return List.of();
            }
            wrapper.eq(User::getDepartmentId, deptId);
        }
        return list(wrapper).stream().map(this::buildUserInfoVO).toList();
    }

    @Override
    public void assertSameDepartmentAccess(Long userId) {
        if (BaseContext.isSuperAdmin() || userId == null) {
            return;
        }
        User target = getById(userId);
        if (target == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "用户不存在");
        }
        Long currentDeptId = BaseContext.getCurrentDepartmentId();
        if (currentDeptId == null || !currentDeptId.equals(target.getDepartmentId())) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "只能操作同部门用户");
        }
    }

    @Override
    public void checkPermission(int requiredMinRole) {
        Long userId = BaseContext.getCurrentId();
        User user = getById(userId);
        if (user == null || user.getRole() > requiredMinRole) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "权限不足");
        }
    }

    @Override
    public void checkPermissionCode(String code) {
        if (StrUtil.isBlank(code)) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "权限不足");
        }
        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 1) {
            return;
        }
        List<String> permissions = permissionService.getEffectivePermissionCodes(role, BaseContext.getCurrentId());
        if (!permissions.contains(code)) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "权限不足");
        }
    }

    private UserInfoVO buildUserInfoVO(User user) {
        String roleName = ROLE_NAME_MAP.getOrDefault(user.getRole(), "未知");
        List<String> roleList = new ArrayList<>();
        roleList.add("level_" + user.getRole());
        if (user.getRole() == 1) {
            roleList.add("admin");
        }
        List<String> permissions = permissionService.getEffectivePermissionCodes(user.getRole(), user.getId());
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .roleName(roleName)
                .orgName(user.getOrgName())
                .departmentId(user.getDepartmentId())
                .roles(roleList)
                .permissions(permissions)
                .build();
    }
}
