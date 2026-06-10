package cn.luyou.utils;

import cn.luyou.model.Referral;
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
        if (BaseContext.isSuperAdmin()) {
            return BaseContext.getCurrentDepartmentId();
        }
        return BaseContext.getCurrentDepartmentId();
    }
}
