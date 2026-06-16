-- V76：一至五级用户（role=2~6）默认可访问部门管理
-- V75 仅覆盖 role 2~5；五级用户及历史库未执行 V75 时仍无 system:department

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
CROSS JOIN `permission` p
WHERE p.code = 'system:department';
