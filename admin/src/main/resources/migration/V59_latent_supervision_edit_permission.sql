-- V59：潜伏感染者管理 — 督导表记录「修改」独立按钮权限

INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(469, 'latentManagement:supervision:edit', '修改督导表', 2, 419, 1);

UPDATE `permission`
SET `parent_id` = 419, `sort` = 1, `name` = '修改督导表', `type` = 2
WHERE `code` = 'latentManagement:supervision:edit';

-- 已拥有「督导表管理」菜单权限的角色，默认同步授予「修改督导表」（保持原有可修改能力）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'latentManagement:supervision'
         CROSS JOIN `permission` p
WHERE p.`code` = 'latentManagement:supervision:edit';
