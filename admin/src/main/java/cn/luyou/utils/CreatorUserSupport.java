package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 录入用户：写入当前用户、按账号/姓名模糊解析 userId。
 */
public final class CreatorUserSupport {

    private CreatorUserSupport() {
    }

    /** 当前登录用户的录入人快照（请求内可复用，避免 Excel 回调里重复查库）。 */
    public record CreatorSnapshot(Long creatorId, String creatorUsername) {
        public boolean isPresent() {
            return creatorId != null || StrUtil.isNotBlank(creatorUsername);
        }
    }

    public static CreatorSnapshot resolveCurrentCreator(UserMapper userMapper) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return new CreatorSnapshot(null, null);
        }
        if (userMapper == null) {
            return new CreatorSnapshot(userId, null);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return new CreatorSnapshot(userId, null);
        }
        // 优先账号；账号为空时回退真实姓名，避免列表录入用户列空白
        String username = StrUtil.blankToDefault(user.getUsername(), user.getRealName());
        return new CreatorSnapshot(userId, StrUtil.blankToDefault(username, null));
    }

    public static String resolveCurrentUsername(UserMapper userMapper) {
        return resolveCurrentCreator(userMapper).creatorUsername();
    }

    public static Long resolveCurrentUserId() {
        return BaseContext.getCurrentId();
    }

    /** 写入 creatorId + creatorUsername（筛查表） */
    public static void fillCurrentCreator(UserMapper userMapper,
                                          Consumer<Long> setCreatorId,
                                          Consumer<String> setCreatorUsername) {
        applyCreator(resolveCurrentCreator(userMapper), setCreatorId, setCreatorUsername);
    }

    public static void applyCreator(CreatorSnapshot snapshot,
                                    Consumer<Long> setCreatorId,
                                    Consumer<String> setCreatorUsername) {
        if (snapshot == null) {
            return;
        }
        if (setCreatorId != null) {
            setCreatorId.accept(snapshot.creatorId());
        }
        if (setCreatorUsername != null) {
            setCreatorUsername.accept(snapshot.creatorUsername());
        }
    }

    /**
     * 仅在已有录入人缺失时回填（覆盖导入保留首次录入人；历史空值则补当前用户）。
     */
    public static void fillMissingCreator(Long existingCreatorId,
                                          String existingCreatorUsername,
                                          CreatorSnapshot current,
                                          Consumer<Long> setCreatorId,
                                          Consumer<String> setCreatorUsername) {
        if (current == null || !current.isPresent()) {
            return;
        }
        boolean missingId = existingCreatorId == null;
        boolean missingName = StrUtil.isBlank(existingCreatorUsername);
        if (!missingId && !missingName) {
            return;
        }
        if (missingId && setCreatorId != null) {
            setCreatorId.accept(current.creatorId());
        }
        if (missingName && setCreatorUsername != null) {
            setCreatorUsername.accept(current.creatorUsername());
        }
    }

    /**
     * 列表查询兜底：creator_username 为空但有 creator_id 时，按用户表回填展示名。
     */
    public static <T> void fillMissingUsernames(UserMapper userMapper,
                                                List<T> records,
                                                Function<T, Long> getCreatorId,
                                                Function<T, String> getCreatorUsername,
                                                BiConsumer<T, String> setCreatorUsername) {
        if (userMapper == null || records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream()
                .filter(r -> StrUtil.isBlank(getCreatorUsername.apply(r)))
                .map(getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (User u : userMapper.selectBatchIds(ids)) {
            if (u == null || u.getId() == null) {
                continue;
            }
            String display = StrUtil.blankToDefault(u.getUsername(), u.getRealName());
            if (StrUtil.isNotBlank(display)) {
                nameMap.put(u.getId(), display);
            }
        }
        for (T record : records) {
            if (StrUtil.isNotBlank(getCreatorUsername.apply(record))) {
                continue;
            }
            Long creatorId = getCreatorId.apply(record);
            if (creatorId == null) {
                continue;
            }
            String name = nameMap.get(creatorId);
            if (StrUtil.isNotBlank(name)) {
                setCreatorUsername.accept(record, name);
            }
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
