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

    /** 权限校验：当前用户角色须 <= requiredMinRole */
    void checkPermission(int requiredMinRole);
}
