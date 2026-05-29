-- V50：推介追踪 — 一至五级用户补全大疫情导入/导出权限（幂等）
-- 适用：未执行 V42 的环境，或角色仅有部分推介追踪子权限时

UPDATE `permission`
SET `name` = '导出推介/追踪记录', `parent_id` = 430, `sort` = 3
WHERE `code` = 'referralManagement:export';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN ('referralManagement:epidemicImport', 'referralManagement:export');

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` existing ON existing.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE existing.`code` LIKE 'referralManagement%'
  AND p.`code` IN ('referralManagement:epidemicImport', 'referralManagement:export');
