package cn.luyou.service;

import cn.luyou.model.User;
import cn.luyou.model.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User> {

    String login(String username, String password);

    UserInfoVO getCurrentUserInfo();

    IPage<User> queryPage(int page, int size, String username, Integer role);

    void createUser(User user);

    void updateUser(User user);

    void deleteUser(Long id);

    /** 获取所有五级机构用户（用于通知单接收单位选择） */
    List<UserInfoVO> getLevel5Users();

    /** 获取转出/转诊接收方用户（四级、五级，按部门-用户展示） */
    List<UserInfoVO> getReferralReceiverUsers();

    /** 获取三级和四级用户（role=4/5，用于推介追踪接收人选择） */
    List<UserInfoVO> getLevel34Users();

    /** 权限管理-按用户追加：获取同部门用户（超级管理员返回全部） */
    List<UserInfoVO> listSameDepartmentUsers();

    /** 非超级管理员只能操作同部门用户，否则抛出权限不足 */
    void assertSameDepartmentAccess(Long userId);

    /** 权限校验：当前用户角色须 <= requiredMinRole */
    void checkPermission(int requiredMinRole);

    /** 权限校验：当前用户须拥有指定权限码（超级管理员免检） */
    void checkPermissionCode(String code);
}
