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
 * 五级用户（机构账号）：仅本人录入、经通知单接收、或经转出确认接收的记录。
 * 同一乡镇/街道（最小部门）下的不同机构互不可见，转出也按机构隔离，不能按整乡镇共享。
 * <p>
 * 转出副本（source_patient_id / source_latent_id 非空）不通过「旧通知单」扩权可见，
 * 避免跨区转出后原辖区三级/五级仍能在在管列表中看到该患者。
 * <p>
 * 接收方确认转出后，原记录对转出机构不可见（在管总览、随访记录、详情）；
 * 接收机构只看副本。区县等上级仍可按辖区看见接收方副本。
 */
@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    private final DepartmentService departmentService;
    private final PatientMapper patientMapper;
    private final LatentInfectionMapper latentInfectionMapper;

    /** 患者列表/历史：市县级按辖区；五级仅本机构（录入/通知单接收/转出接收） */
    public void applyPatientScope(LambdaQueryWrapper<Patient> wrapper) {
        applyBizScope(wrapper, Patient::getId, Patient::getDepartmentId, Patient::getCreatorId, "patient");
    }

    /** 潜伏感染者列表/历史：同上 */
    public void applyLatentScope(LambdaQueryWrapper<LatentInfection> wrapper) {
        applyBizScope(wrapper, LatentInfection::getId, LatentInfection::getDepartmentId,
                LatentInfection::getCreatorId, "latent");
    }

    /**
     * 通知单统计：市/区县/社区按部门树隔离；五级仅本人发送或接收的通知单。
     * <p>
     * 排除挂在转出副本上的通知单（与已发送列表一致）：复制通知单仍保留原 sender/receiver，
     * 若不排除，原辖区会通过 sender/receiver 路径把跨区转出后的副本通知单重复计入统计。
     */
    public void applyNoticeScope(LambdaQueryWrapper<Notice> wrapper) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        Long userId = BaseContext.getCurrentId();
        if (role != null && role == 6 && userId != null) {
            wrapper.and(w -> w.eq(Notice::getSenderId, userId)
                    .or()
                    .eq(Notice::getReceiverOrgId, userId));
            excludeTransferCopiedNotices(wrapper);
            return;
        }
        List<Long> deptIds = departmentService.getDescendantIds(BaseContext.getCurrentDepartmentId());
        if (deptIds == null || deptIds.isEmpty()) {
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(Notice::getNoticeType, "patient")
                            .inSql(Notice::getBizId,
                                    "SELECT id FROM patient WHERE deleted = 0 AND department_id IS NULL"
                                            + " AND source_patient_id IS NULL"))
                    .or(w2 -> w2.eq(Notice::getNoticeType, "latent")
                            .inSql(Notice::getBizId,
                                    "SELECT id FROM latent_infection WHERE deleted = 0 AND department_id IS NULL"
                                            + " AND source_latent_id IS NULL")));
            return;
        }
        String deptCsv = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String userSql = "SELECT id FROM `user` WHERE deleted = 0 AND department_id IN (" + deptCsv + ")";
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Notice::getNoticeType, "patient")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM patient WHERE deleted = 0 AND department_id IN (" + deptCsv + ")"
                                        + " AND source_patient_id IS NULL"))
                .or(w2 -> w2.eq(Notice::getNoticeType, "latent")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM latent_infection WHERE deleted = 0 AND department_id IN (" + deptCsv + ")"
                                        + " AND source_latent_id IS NULL"))
                .or().inSql(Notice::getSenderId, userSql)
                .or().inSql(Notice::getReceiverOrgId, userSql));
        // sender/receiver 扩权仍可能命中转出副本上的复制通知单，统一再排除一遍
        excludeTransferCopiedNotices(wrapper);
    }

    /** 排除挂在转出副本业务上的通知单（patient.source_patient_id / latent.source_latent_id 非空） */
    private void excludeTransferCopiedNotices(LambdaQueryWrapper<Notice> wrapper) {
        wrapper.and(w -> w
                .nested(n -> n.eq(Notice::getNoticeType, "patient")
                        .notInSql(Notice::getBizId,
                                "SELECT id FROM patient WHERE source_patient_id IS NOT NULL AND deleted = 0"))
                .or()
                .nested(n -> n.eq(Notice::getNoticeType, "latent")
                        .notInSql(Notice::getBizId,
                                "SELECT id FROM latent_infection WHERE source_latent_id IS NOT NULL AND deleted = 0"))
                .or()
                .notIn(Notice::getNoticeType, "patient", "latent"));
    }

    /**
     * 转出统计：市/县/社区按部门树；五级仅本人发送或接收的转出记录。
     */
    public void applyReferralScope(LambdaQueryWrapper<Referral> wrapper) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        Long userId = BaseContext.getCurrentId();
        if (role != null && role == 6 && userId != null) {
            wrapper.and(w -> w.eq(Referral::getSenderId, userId)
                    .or()
                    .eq(Referral::getReceiverOrgId, userId));
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

    /** 统计分析部门筛选：通知单关联业务须落在指定部门（排除转出副本） */
    public void applyNoticeBizDepartmentFilter(LambdaQueryWrapper<Notice> wrapper, List<Long> filterDeptIds) {
        if (filterDeptIds == null || filterDeptIds.isEmpty()) {
            return;
        }
        String csv = filterDeptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Notice::getNoticeType, "patient")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM patient WHERE deleted = 0 AND department_id IN (" + csv + ")"
                                        + " AND source_patient_id IS NULL"))
                .or(w2 -> w2.eq(Notice::getNoticeType, "latent")
                        .inSql(Notice::getBizId,
                                "SELECT id FROM latent_infection WHERE deleted = 0 AND department_id IN (" + csv + ")"
                                        + " AND source_latent_id IS NULL")));
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
     * 五级按机构（录入人）匹配，避免同一乡镇下其他机构的记录挡住本机构导入。
     * 市/县/社区仅在辖区内匹配；未匹配则允许插入新行。
     */
    public <T> void applyImportDedupScope(LambdaQueryWrapper<T> wrapper,
                                          SFunction<T, Long> departmentIdColumn,
                                          SFunction<T, Long> creatorIdColumn) {
        if (BaseContext.isSuperAdmin()) {
            return;
        }
        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 6 && creatorIdColumn != null && BaseContext.getCurrentId() != null) {
            wrapper.eq(creatorIdColumn, BaseContext.getCurrentId());
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
        // 接收确认后：转出机构不可再查阅原记录（在管总览、随访记录、详情等）
        excludeTransferredOutSources(wrapper, idColumn, noticeType);

        Integer role = BaseContext.getCurrentRole();
        if (role != null && role == 6) {
            Long userId = BaseContext.getCurrentId();
            String noticeBizSql = buildNoticeBizSql(noticeType, userId, null);
            String transferBizSql = buildTransferBizSql(noticeType, userId, null);
            // 转出机构即使与接收机构同乡镇，也不可再看到接收方副本
            excludeSenderTransferCopies(wrapper, idColumn, noticeType, userId);
            wrapper.and(w -> {
                w.eq(creatorColumn, userId);
                // 通知单扩权仅针对「非转出副本」：转出确认后副本归属接收机构
                if (StrUtil.isNotBlank(noticeBizSql)) {
                    w.or().inSql(idColumn, noticeBizSql);
                }
                if (StrUtil.isNotBlank(transferBizSql)) {
                    w.or().inSql(idColumn, transferBizSql);
                }
                // 历史导入未写录入人：仅本部门孤儿可见，不把同乡镇其他机构的数据放开
                Long deptId = BaseContext.getCurrentDepartmentId();
                if (deptId != null) {
                    w.or(orphan -> orphan.eq(departmentColumn, deptId).isNull(creatorColumn));
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
        // 排除转出副本：跨区转出后 department 已迁走，但复制通知单仍挂原区五级接收人，
        // 若仍用 notice 扩权，本区三级/五级会继续看到已转出患者
        String noticeBizSql = buildNoticeBizSql(noticeType, null, deptIdCsv);
        String transferBizSql = buildTransferBizSql(noticeType, null, deptIdCsv);
        wrapper.and(w -> {
            w.in(departmentColumn, deptIds);
            if (StrUtil.isNotBlank(noticeBizSql)) {
                w.or().inSql(idColumn, noticeBizSql);
            }
            if (StrUtil.isNotBlank(transferBizSql)) {
                w.or().inSql(idColumn, transferBizSql);
            }
        });
    }

    /**
     * 已确认转出且已生成接收方副本的原业务，对非超管不可见（接收机构只看 target_biz_id）。
     * 同时按 archive_remark=已转出排除。
     * <p>
     * 仅排除 target_biz_id 非空的确认记录，避免「未复制的历史确认」导致接收方也看不到唯一在管记录。
     */
    private <T> void excludeTransferredOutSources(LambdaQueryWrapper<T> wrapper,
                                                  SFunction<T, Long> idColumn,
                                                  String noticeType) {
        String moduleType = "patient".equals(noticeType) ? "patient"
                : "latent".equals(noticeType) ? "latent" : null;
        if (moduleType == null) {
            return;
        }
        wrapper.notInSql(idColumn,
                "SELECT r.biz_id FROM referral r WHERE r.module_type = '" + moduleType
                        + "' AND r.status = 2 AND r.deleted = 0"
                        + " AND r.biz_id IS NOT NULL AND r.target_biz_id IS NOT NULL");
        wrapper.apply("(archive_remark IS NULL OR archive_remark <> {0})", "已转出");
    }

    /**
     * 五级转出机构不可再看到自己已确认转出后的接收方副本（含同乡镇不同机构互转）。
     * 上级（三级等）仍可按辖区看见副本，便于本区内监管。
     */
    private <T> void excludeSenderTransferCopies(LambdaQueryWrapper<T> wrapper,
                                                 SFunction<T, Long> idColumn,
                                                 String noticeType,
                                                 Long senderUserId) {
        String moduleType = "patient".equals(noticeType) ? "patient"
                : "latent".equals(noticeType) ? "latent" : null;
        if (moduleType == null || senderUserId == null) {
            return;
        }
        wrapper.notInSql(idColumn,
                "SELECT r.target_biz_id FROM referral r WHERE r.module_type = '" + moduleType
                        + "' AND r.status = 2 AND r.deleted = 0"
                        + " AND r.sender_id = " + senderUserId
                        + " AND r.target_biz_id IS NOT NULL");
    }

    /**
     * 经通知单接收可见的业务 ID。
     * 故意排除转出复制产生的业务（source_patient_id / source_latent_id 非空）：
     * 转出确认时会复制通知单且保留原 receiver，若不排除会导致原辖区用户继续看见已转至他区的在管记录。
     * 接收方对副本的可见性由 creator / transferBizSql 覆盖（五级不再按整乡镇放开）。
     */
    private String buildNoticeBizSql(String noticeType, Long receiverUserId, String deptIdCsv) {
        String table;
        String sourceColumn;
        if ("patient".equals(noticeType)) {
            table = "patient";
            sourceColumn = "source_patient_id";
        } else if ("latent".equals(noticeType)) {
            table = "latent_infection";
            sourceColumn = "source_latent_id";
        } else {
            return null;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT n.biz_id FROM notice n INNER JOIN `" + table + "` b ON n.biz_id = b.id "
                        + "INNER JOIN `user` u ON n.receiver_org_id = u.id "
                        + "WHERE n.notice_type = '" + noticeType + "' AND n.deleted = 0 "
                        + "AND b.deleted = 0 AND b." + sourceColumn + " IS NULL "
                        + "AND u.deleted = 0");
        if (receiverUserId != null) {
            sql.append(" AND n.receiver_org_id = ").append(receiverUserId);
        } else if (deptIdCsv != null && !deptIdCsv.isBlank()) {
            sql.append(" AND u.role = 6 AND u.department_id IN (").append(deptIdCsv).append(")");
        } else {
            return null;
        }
        return sql.toString();
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
