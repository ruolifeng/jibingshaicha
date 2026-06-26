-- V79：统一数据清洗权限码（历史库可能为 dataClean，前端路由使用 dataCleaning）

UPDATE `permission`
SET `code` = 'dataCleaning'
WHERE `code` = 'dataClean';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
CROSS JOIN `permission` p
WHERE p.code = 'dataCleaning';
