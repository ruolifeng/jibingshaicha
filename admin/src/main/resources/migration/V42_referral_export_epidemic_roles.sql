-- V42：推介追踪 — 大疫情导入/导出授予一至五级；导出含追踪过程
UPDATE `permission`
SET `name` = '导出推介/追踪记录', `parent_id` = 430, `sort` = 3
WHERE `code` = 'referralManagement:export';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement',
    'referralManagement:recommend',
    'referralManagement:track',
    'referralManagement:epidemicImport',
    'referralManagement:export',
    'referralManagement:edit',
    'referralManagement:create',
    'referralManagement:trackOperate'
);

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'referralManagement'
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:recommend',
    'referralManagement:track',
    'referralManagement:epidemicImport',
    'referralManagement:export',
    'referralManagement:edit',
    'referralManagement:create',
    'referralManagement:trackOperate'
);
