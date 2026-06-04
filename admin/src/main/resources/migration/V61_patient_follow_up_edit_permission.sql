-- V61：患者管理 — 后续随访记录「修改」独立按钮权限（见 V62 统一修复，此处保持幂等）

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:followUp:edit', '修改随访记录', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:followUp'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:followUp:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:followUp'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '修改随访记录', child.`type` = 2
WHERE child.`code` = 'patientManagement:followUp:edit';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement:followUp'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:followUp:edit';
