-- V75：一至四级用户默认可访问部门管理
-- 问题：部门管理路由曾限制 roles=admin，且 system:department 仅赋给超级管理员，导致一至四级用户无法新增部门

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) r
CROSS JOIN `permission` p
WHERE p.code = 'system:department';
