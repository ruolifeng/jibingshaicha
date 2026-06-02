package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.PatientMapper;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限：市/县级用户可见本辖区部门数据，以及分配给辖区内五级用户的工作数据。
 * 五级用户：本人录入的记录，或经通知单接收的工作。
 */
@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    private final DepartmentService departmentService;
    private final PatientMapper patientMapper;
    private final LatentInfectionMapper latentInfectionMapper;

    /** 患者列表/历史：市县级 + 辖区内五级用户经通知单接收的工作；五级 + 本人录入 */
    public void applyPatientScope(LambdaQueryWrapper<Patient> wrapper) {
        applyBizScope(wrapper, Patient::getId, Patient::getDepartmentId, Patient::getCreatorId, "patient");
    }

    /** 潜伏感染者列表/历史：同上 */
    public void applyLatentScope(LambdaQueryWrapper<LatentInfection> wrapper) {
        applyBizScope(wrapper, LatentInfection::getId, LatentInfection::getDepartmentId,
                LatentInfection::getCreatorId, "latent");
    }

    public void assertPatientAccessible(Long patientId) {
        if (BaseContext.isSuperAdmin() || patientId == null) {
            return;
        }
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patient::getId, patientId);
        applyPatientScope(wrapper);
        if (patientMapper.selectCount(wrapper) == 0) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权限操作该患者");
        }
    }

    public void assertLatentAccessible(Long latentId) {
        if (BaseContext.isSuperAdmin() || latentId == null) {
            return;
        }
        LambdaQueryWrapper<LatentInfection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LatentInfection::getId, latentId);
        applyLatentScope(wrapper);
        if (latentInfectionMapper.selectCount(wrapper) == 0) {
            throw new ServiceException(StatusEnum.FORBIDDEN, "无权限操作该潜伏感染记录");
        }
    }

    private <T> void applyBizScope(LambdaQueryWrapper<T> wrapper,
                                   SFunction<T, Long> idColumn,
                                   SFunction<T, Long> departmentColumn,
                                   SFunction<T, Long> creatorColumn,
                                   String noticeType) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 6) {
            Long userId = BaseContext.getCurrentId();
            String transferBizSql = buildTransferBizSql(noticeType, userId, null);
            wrapper.and(w -> w.inSql(idColumn,
                            "SELECT biz_id FROM notice WHERE receiver_org_id = " + userId
                                    + " AND notice_type = '" + noticeType + "' AND deleted = 0")
                    .or().inSql(StrUtil.isNotBlank(transferBizSql), idColumn, transferBizSql)
                    .or().eq(creatorColumn, userId));
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds == null || deptIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        String deptIdCsv = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String noticeBizSql = "SELECT n.biz_id FROM notice n INNER JOIN `user` u ON n.receiver_org_id = u.id "
                + "WHERE n.notice_type = '" + noticeType + "' AND n.deleted = 0 AND u.deleted = 0 AND u.role = 6 "
                + "AND u.department_id IN (" + deptIdCsv + ")";
        String transferBizSql = buildTransferBizSql(noticeType, null, deptIdCsv);
        wrapper.and(w -> w.in(departmentColumn, deptIds)
                .or().inSql(idColumn, noticeBizSql)
                .or().inSql(StrUtil.isNotBlank(transferBizSql), idColumn, transferBizSql));
    }

    /** 经转出确认同步至接收方的业务 ID（referral.target_biz_id，兼容未复制前的 biz_id） */
    private String buildTransferBizSql(String noticeType, Long receiverUserId, String deptIdCsv) {
        String moduleType = "patient".equals(noticeType) ? "patient" : "latent".equals(noticeType) ? "latent" : null;
        if (moduleType == null) {
            return null;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT r.target_biz_id FROM referral r INNER JOIN `user` u ON r.receiver_org_id = u.id "
                        + "WHERE r.module_type = '" + moduleType + "' AND r.status = 2 AND r.target_biz_id IS NOT NULL "
                        + "AND r.deleted = 0 AND u.deleted = 0");
        if (receiverUserId != null) {
            sql.append(" AND r.receiver_org_id = ").append(receiverUserId);
        }
        if (deptIdCsv != null && !deptIdCsv.isBlank()) {
            sql.append(" AND u.department_id IN (").append(deptIdCsv).append(")");
        }
        sql.append(" UNION SELECT r.biz_id FROM referral r INNER JOIN `user` u ON r.receiver_org_id = u.id "
                + "WHERE r.module_type = '").append(moduleType).append("' AND r.status = 2 AND r.target_biz_id IS NULL "
                + "AND r.deleted = 0 AND u.deleted = 0");
        if (receiverUserId != null) {
            sql.append(" AND r.receiver_org_id = ").append(receiverUserId);
        }
        if (deptIdCsv != null && !deptIdCsv.isBlank()) {
            sql.append(" AND u.department_id IN (").append(deptIdCsv).append(")");
        }
        return sql.toString();
    }
}
