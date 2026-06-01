-- V51：修复 patientManagement:pickup 权限未创建（原 id=465 与 epidemic:screening:track 冲突）
-- 并为已有服药/领药相关权限的角色（不含五级）补全「填写领药」

INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`) VALUES
('patientManagement:pickup', '填写领药', 2, 424, 1);

UPDATE `permission`
SET `parent_id` = 424, `sort` = 1, `name` = '填写领药', `type` = 2
WHERE `code` = 'patientManagement:pickup';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` existing ON existing.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE existing.`code` IN (
    'patient:medication',
    'patientManagement:medication',
    'keyPopulation:patient:medication',
    'closeContact:patient:medication'
)
  AND p.`code` = 'patientManagement:pickup'
  AND rp.`role` != 6;

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup'
  AND rp.`role` != 6;
