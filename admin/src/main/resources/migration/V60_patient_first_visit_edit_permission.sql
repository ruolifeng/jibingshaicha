-- V60：患者管理 — 首次随访「编辑」独立按钮权限

INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(470, 'patientManagement:firstVisit:edit', '编辑首次随访', 2, 422, 1);

UPDATE `permission`
SET `parent_id` = 422, `sort` = 1, `name` = '编辑首次随访', `type` = 2
WHERE `code` = 'patientManagement:firstVisit:edit';

-- 已拥有「首次随访」菜单权限的角色，默认同步授予「编辑首次随访」（保持原有可编辑能力）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement:firstVisit'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:firstVisit:edit';
