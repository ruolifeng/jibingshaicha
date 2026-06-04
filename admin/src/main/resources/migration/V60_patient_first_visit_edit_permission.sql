-- V60：患者管理 — 首次随访「编辑」独立按钮权限（见 V62 统一修复，此处保持幂等）

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:firstVisit:edit', '编辑首次随访', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:firstVisit'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:firstVisit:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:firstVisit'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '编辑首次随访', child.`type` = 2
WHERE child.`code` = 'patientManagement:firstVisit:edit';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement:firstVisit'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:firstVisit:edit';
