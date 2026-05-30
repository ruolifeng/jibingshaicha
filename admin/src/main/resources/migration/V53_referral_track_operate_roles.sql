-- V53：推介追踪 — 一至五级补全编辑/追踪/胸片/诊断/删除操作权限

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:edit',
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:delete'
);

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'referralManagement'
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:edit',
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:delete'
);

-- 补全大疫情导入记录缺失的录入人（department_id 已存在时无法推断具体用户，仅保留 creator_id 为空的占位）
-- 五级用户重新导入同文件时会自动补全 report_card_time 与 creator_id
