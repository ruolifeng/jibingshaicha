package cn.luyou.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import cn.luyou.model.vo.UserInfoVO;
import cn.luyou.service.DepartmentService;
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
    private final DepartmentService departmentService;
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
        applyManageableUserScope(wrapper);
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
        assertCreateUserAllowed(user);
        user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        save(user);
    }

    @Override
    public void updateUser(User user) {
        User existing = getById(user.getId());
        if (existing == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "用户不存在");
        }
        assertUserManageable(existing);
        if (StrUtil.isNotBlank(user.getUsername()) && !user.getUsername().equals(existing.getUsername())) {
            Long count = lambdaQuery().eq(User::getUsername, user.getUsername()).count();
            if (count > 0) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "用户名已存在");
            }
        }
        if (!BaseContext.isSuperAdmin() && existing.getId().equals(BaseContext.getCurrentId())) {
            // 普通用户编辑本人时仅允许修改基础资料/密码，防止通过表单字段调整自身层级和部门。
            user.setRole(existing.getRole());
            user.setDepartmentId(existing.getDepartmentId());
        } else if (!BaseContext.isSuperAdmin()) {
            assertDepartmentInScope(user.getDepartmentId(), "只能将用户分配到本部门或下级部门");
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
    public void updateCurrentUser(User user) {
        Long currentUserId = BaseContext.getCurrentId();
        User existing = getById(currentUserId);
        if (existing == null) {
            throw new ServiceException(StatusEnum.UNAUTHORIZED, "用户不存在");
        }

        var updater = lambdaUpdate()
                .eq(User::getId, currentUserId)
                .set(User::getRealName, user.getRealName())
                .set(User::getOrgName, user.getOrgName())
                .set(User::getAvatar, user.getAvatar());
        if (StrUtil.isNotBlank(user.getPassword())) {
            updater.set(User::getPassword, PASSWORD_ENCODER.encode(user.getPassword()));
        }
        updater.update();
    }

    @Override
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "用户不存在");
        }
        assertUserManageable(user);
        if (id.equals(BaseContext.getCurrentId())) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "不能删除当前登录用户");
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
    public List<UserInfoVO> getReferralReceiverUsers() {
        // 转出接收方：二/三/四/五级用户（role 3–6），按部门-用户树选择
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .in(User::getRole, java.util.Arrays.asList(3, 4, 5, 6))
                .orderByAsc(User::getDepartmentId)
                .orderByAsc(User::getRole)
                .orderByAsc(User::getUsername);
        return list(wrapper).stream().map(this::buildUserInfoVO).toList();
    }

    @Override
    public List<UserInfoVO> getLevel34Users() {
        // 一至五级（role=2-6）用户均可作为推介接收人，支持同级和跨级互推。
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .in(User::getRole, java.util.Arrays.asList(2, 3, 4, 5, 6))
                .orderByAsc(User::getDepartmentId)
                .orderByAsc(User::getRole)
                .orderByAsc(User::getUsername);
        List<User> users = list(wrapper);
        return users.stream().map(this::buildUserInfoVO).toList();
    }

    @Override
    public List<UserInfoVO> listSameDepartmentUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByAsc(User::getRole)
                .orderByDesc(User::getCreateTime);
        if (!BaseContext.isSuperAdmin()) {
            applyManageableUserScope(wrapper);
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
        assertUserManageable(target);
    }

    /** 用户管理范围：本人 + 当前部门及所有下级部门用户。 */
    private void applyManageableUserScope(LambdaQueryWrapper<User> wrapper) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long currentUserId = BaseContext.getCurrentId();
        List<Long> deptIds = resolveScopedDepartmentIds();
        wrapper.and(w -> {
            w.eq(User::getId, currentUserId);
            if (!deptIds.isEmpty()) {
                w.or().in(User::getDepartmentId, deptIds);
            }
        });
    }

    private void assertUserManageable(User target) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null && currentUserId.equals(target.getId())) {
            return;
        }
        assertDepartmentInScope(target.getDepartmentId(), "只能操作本人、本部门或下级部门用户");
    }

    private void assertCreateUserAllowed(User user) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        assertDepartmentInScope(user.getDepartmentId(), "只能在本部门或下级部门创建用户");
    }

    private void assertDepartmentInScope(Long departmentId, String message) {
        if (departmentId == null || !resolveScopedDepartmentIds().contains(departmentId)) {
            throw new ServiceException(StatusEnum.FORBIDDEN, message);
        }
    }

    private List<Long> resolveScopedDepartmentIds() {
        Long deptId = BaseContext.getCurrentDepartmentId();
        if (deptId == null) {
            return List.of();
        }
        List<Long> deptIds = departmentService.getDescendantIds(deptId);
        return deptIds == null ? List.of() : deptIds;
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

    @Override
    public void checkAnyPermissionCode(String... codes) {
        if (codes == null || codes.length == 0) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "权限不足");
        }
        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 1) {
            return;
        }
        List<String> permissions = permissionService.getEffectivePermissionCodes(role, BaseContext.getCurrentId());
        for (String code : codes) {
            if (StrUtil.isNotBlank(code) && permissions.contains(code)) {
                return;
            }
        }
        throw new ServiceException(StatusEnum.FORBIDDEN, "权限不足");
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
                .avatar(user.getAvatar())
                .departmentId(user.getDepartmentId())
                .roles(roleList)
                .permissions(permissions)
                .build();
    }
}
