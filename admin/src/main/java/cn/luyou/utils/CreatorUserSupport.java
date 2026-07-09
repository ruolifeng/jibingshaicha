package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 录入用户：写入当前用户、按账号/姓名模糊解析 userId。
 */
public final class CreatorUserSupport {

    private CreatorUserSupport() {
    }

    public static String resolveCurrentUsername(UserMapper userMapper) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null || userMapper == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }

    public static Long resolveCurrentUserId() {
        return BaseContext.getCurrentId();
    }

    /** 写入 creatorId + creatorUsername（筛查表） */
    public static void fillCurrentCreator(UserMapper userMapper,
                                          Consumer<Long> setCreatorId,
                                          Consumer<String> setCreatorUsername) {
        Long userId = resolveCurrentUserId();
        if (setCreatorId != null) {
            setCreatorId.accept(userId);
        }
        if (setCreatorUsername != null) {
            setCreatorUsername.accept(resolveCurrentUsername(userMapper));
        }
    }

    /**
     * 按姓名或账号模糊匹配用户 ID 列表；无匹配时返回 [-1]，便于调用方 eq 空结果。
     */
    public static List<Long> resolveUserIdsByKeyword(UserMapper userMapper, String keyword) {
        if (StrUtil.isBlank(keyword) || userMapper == null) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .like(User::getRealName, keyword)
                .or()
                .like(User::getUsername, keyword));
        if (users == null || users.isEmpty()) {
            return List.of(-1L);
        }
        return users.stream().map(User::getId).collect(Collectors.toList());
    }

    /** 按 creator_username 模糊；或按 keyword 解析到的 userId 匹配 creator_id */
    public static void applyCreatorUsernameOrIds(String keyword,
                                                 UserMapper userMapper,
                                                 Consumer<String> likeUsername,
                                                 BiConsumer<Boolean, List<Long>> inCreatorIds) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        if (likeUsername != null) {
            likeUsername.accept(keyword);
        }
        if (inCreatorIds != null && userMapper != null) {
            List<Long> ids = resolveUserIdsByKeyword(userMapper, keyword);
            // 仅 username 模糊时可不传 in；筛查表同时有 username 字段时优先 like username
        }
    }
}
