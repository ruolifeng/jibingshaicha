-- V61：患者管理 — 后续随访记录「修改」独立按钮权限

INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(471, 'patientManagement:followUp:edit', '修改随访记录', 2, 423, 1);

UPDATE `permission`
SET `parent_id` = 423, `sort` = 1, `name` = '修改随访记录', `type` = 2
WHERE `code` = 'patientManagement:followUp:edit';

-- 已拥有「后续随访」菜单权限的角色，默认同步授予「修改随访记录」（保持原有可修改能力）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement:followUp'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:followUp:edit';
