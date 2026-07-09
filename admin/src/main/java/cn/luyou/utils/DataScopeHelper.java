package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.PatientMapper;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.Referral;
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

    /**
     * 通知单统计：关联业务（患者/潜伏感染）在当前部门树辖区内的通知单。
     * 市/区县/社区均按 {@link DepartmentService#getDescendantIds} 隔离。
     */
    public void applyNoticeScope(LambdaQueryWrapper<Notice> wrapper) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds == null || deptIds.isEmpty()) {
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(Notice::getNoticeType, "patient")
                            .inSql(Notice::getBizId,
                                    "SELECT id FROM patient WHERE deleted = 0 AND department_id IS NULL"))
                    .or(w2 -> w2.eq(Notice::getNoticeType, "latent")
                            .inSql(Notice::getBizId,
                                    "SELECT id FROM latent_infection WHERE deleted = 0 AND department_id IS NULL")));
            return;
        }
        String deptCsv = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String userSql = "SELECT id FROM `user` WHERE deleted = 0 AND department_id IN (" + deptCsv + ")";
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Notice::getNoticeType, "patient")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM patient WHERE deleted = 0 AND department_id IN (" + deptCsv + ")"))
                .or(w2 -> w2.eq(Notice::getNoticeType, "latent")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM latent_infection WHERE deleted = 0 AND department_id IN (" + deptCsv + ")"))
                .or().inSql(Notice::getSenderId, userSql)
                .or().inSql(Notice::getReceiverOrgId, userSql));
    }

    /**
     * 分级诊疗统计：发送方或接收方属于当前部门树辖区内的记录。
     */
    public void applyReferralScope(LambdaQueryWrapper<Referral> wrapper) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds == null || deptIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        String deptCsv = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String userSql = "SELECT id FROM `user` WHERE deleted = 0 AND department_id IN (" + deptCsv + ")";
        wrapper.and(w -> w.inSql(Referral::getSenderId, userSql)
                .or()
                .inSql(Referral::getReceiverOrgId, userSql));
    }

    /** 统计分析部门筛选：通知单关联业务须落在指定部门 */
    public void applyNoticeBizDepartmentFilter(LambdaQueryWrapper<Notice> wrapper, List<Long> filterDeptIds) {
        if (filterDeptIds == null || filterDeptIds.isEmpty()) {
            return;
        }
        String csv = filterDeptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Notice::getNoticeType, "patient")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM patient WHERE deleted = 0 AND department_id IN (" + csv + ")"))
                .or(w2 -> w2.eq(Notice::getNoticeType, "latent")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM latent_infection WHERE deleted = 0 AND department_id IN (" + csv + ")")));
    }

    /** 统计分析部门筛选：转诊发送方/接收方用户所属部门在范围内 */
    public void applyReferralBizDepartmentFilter(LambdaQueryWrapper<Referral> wrapper, List<Long> filterDeptIds) {
        if (filterDeptIds == null || filterDeptIds.isEmpty()) {
            return;
        }
        String csv = filterDeptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String userSql = "SELECT id FROM `user` WHERE deleted = 0 AND department_id IN (" + csv + ")";
        wrapper.and(w -> w.inSql(Referral::getSenderId, userSql)
                .or()
                .inSql(Referral::getReceiverOrgId, userSql));
    }

    /**
     * 导入去重范围：与患者/潜伏感染列表可见范围一致。
     * 非超管仅在辖区内匹配已存在记录；未匹配则允许插入新行。
     */
    public <T> void applyImportDedupScope(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, Long> departmentIdColumn,
                                          SFunction<T, Long> creatorIdColumn) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds.isEmpty()) {
            if (creatorIdColumn != null && BaseContext.getCurrentId() != null) {
                wrapper.eq(creatorIdColumn, BaseContext.getCurrentId());
            } else {
                wrapper.isNull(departmentIdColumn);
            }
            return;
        }
        wrapper.in(departmentIdColumn, deptIds);
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
            List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
            String transferBizSql = buildTransferBizSql(noticeType, userId, null);
            wrapper.and(w -> {
                w.inSql(idColumn,
                                "SELECT biz_id FROM notice WHERE receiver_org_id = " + userId
                                        + " AND notice_type = '" + noticeType + "' AND deleted = 0")
                        .or().inSql(StrUtil.isNotBlank(transferBizSql), idColumn, transferBizSql)
                        .or().eq(creatorColumn, userId);
                // 与筛查模块一致：本部门及下级部门数据也可见（兼容导入时未写 creator_id 的历史记录）
                if (deptIds == null || deptIds.isEmpty()) {
                    w.or().isNull(departmentColumn);
                } else {
                    w.or().in(departmentColumn, deptIds);
                }
            });
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds == null || deptIds.isEmpty()) {
            wrapper.isNull(departmentColumn);
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
