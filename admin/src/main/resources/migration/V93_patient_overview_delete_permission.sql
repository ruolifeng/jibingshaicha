-- V93：将「删除患者」挂到「在管总览」下，便于权限分配；并为已有在管总览权限的角色补授

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:overview'
SET child.`parent_id` = parent.id,
    child.`type` = 2,
    child.`sort` = 3,
    child.`name` = '删除患者'
WHERE child.`code` = 'patientManagement:delete';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE parent.`code` = 'patientManagement:overview'
  AND p.`code` = 'patientManagement:delete';
