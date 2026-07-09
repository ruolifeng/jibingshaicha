package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.Referral;
import cn.luyou.model.User;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.ReferralService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 筛查模块数据权限：非超管按部门树隔离，并包含已确认转诊接收的记录。
 * 未绑定部门时避免 {@code IN ()} 导致 SQL 异常。
 */
@Component
@RequiredArgsConstructor
public class ScreeningScopeHelper {

    private final DepartmentService departmentService;
    private final ReferralService referralService;
    private final UserMapper userMapper;

    /**
     * @param populationType 转诊模块人群类型：school / key / close
     */
    public <T> void applyDepartmentScope(LambdaQueryWrapper<T> wrapper,
                                         SFunction<T, Long> departmentIdColumn,
                                         SFunction<T, Long> idColumn,
                                         String populationType) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        Long currentUserId = BaseContext.getCurrentId();
        final List<Long> referredIds;
        if (currentUserId != null) {
            referredIds = referralService.lambdaQuery()
                    .eq(Referral::getModuleType, "screening")
                    .eq(Referral::getPopulationType, populationType)
                    .eq(Referral::getReceiverOrgId, currentUserId)
                    .eq(Referral::getStatus, 2)
                    .list()
                    .stream()
                    .map(Referral::getBizId)
                    .toList();
        } else {
            referredIds = List.of();
        }

        if (deptIds.isEmpty()) {
            if (referredIds.isEmpty()) {
                wrapper.isNull(departmentIdColumn);
            } else {
                wrapper.and(w -> w.isNull(departmentIdColumn)
                        .or()
                        .in(idColumn, referredIds));
            }
            return;
        }
        if (referredIds.isEmpty()) {
            wrapper.in(departmentIdColumn, deptIds);
        } else {
            wrapper.and(w -> w.in(departmentIdColumn, deptIds)
                    .or()
                    .in(idColumn, referredIds));
        }
    }

    /** 无转诊关联的简单部门隔离（如大疫情导入） */
    public <T> void applySimpleDepartmentScope(LambdaQueryWrapper<T> wrapper,
                                               SFunction<T, Long> departmentIdColumn) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds.isEmpty()) {
            wrapper.isNull(departmentIdColumn);
            return;
        }
        wrapper.in(departmentIdColumn, deptIds);
    }

    /** 非超管上传/新增时写入部门；未绑定部门则返回 null（与查询 isNull 逻辑一致） */
    public Long resolveUploadDepartmentId() {
        return BaseContext.getCurrentDepartmentId();
    }

    /**
     * 密接个案表列表/导出范围：按部门树隔离；未绑定部门时仅可见本人录入。
     */
    public <T> void applyCloseContactCaseScope(LambdaQueryWrapper<T> wrapper,
                                               SFunction<T, Long> departmentIdColumn,
                                               SFunction<T, String> creatorUsernameColumn) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds.isEmpty()) {
            String username = resolveCurrentUsername();
            if (StrUtil.isBlank(username)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.eq(creatorUsernameColumn, username);
            }
            return;
        }
        wrapper.in(departmentIdColumn, deptIds);
    }

    /**
     * 导入去重范围：与列表可见范围一致。
     * 非超管仅在辖区内匹配已存在记录；未匹配则插入新行（即使其他辖区已有同证件号）。
     */
    public <T> void applyImportDedupScope(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, Long> departmentIdColumn) {
        applyImportDedupScope(wrapper, departmentIdColumn, null, null);
    }

    public <T> void applyImportDedupScope(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, Long> departmentIdColumn,
                                          SFunction<T, String> creatorUsernameColumn,
                                          SFunction<T, Long> creatorIdColumn) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds.isEmpty()) {
            if (creatorUsernameColumn != null) {
                String username = resolveCurrentUsername();
                if (StrUtil.isNotBlank(username)) {
                    wrapper.eq(creatorUsernameColumn, username);
                    return;
                }
            }
            if (creatorIdColumn != null && BaseContext.getCurrentId() != null) {
                wrapper.eq(creatorIdColumn, BaseContext.getCurrentId());
                return;
            }
            wrapper.isNull(departmentIdColumn);
            return;
        }
        wrapper.in(departmentIdColumn, deptIds);
    }

    private String resolveCurrentUsername() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : null;
    }
}
