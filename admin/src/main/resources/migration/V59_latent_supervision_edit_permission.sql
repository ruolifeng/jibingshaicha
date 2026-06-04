-- V59：潜伏感染者管理 — 督导表记录「修改」独立按钮权限（见 V62 统一修复，此处保持幂等）

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'latentManagement:supervision:edit', '修改督导表', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'latentManagement:supervision'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:supervision:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'latentManagement:supervision'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '修改督导表', child.`type` = 2
WHERE child.`code` = 'latentManagement:supervision:edit';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'latentManagement:supervision'
         CROSS JOIN `permission` p
WHERE p.`code` = 'latentManagement:supervision:edit';
