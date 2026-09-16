-- V120：推介页也要能分配「追踪/胸片/诊断」按钮权限
-- V109 将上述权限挂到「追踪」菜单后，只勾选「推介」的角色在推介页看不到追踪相关按钮

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 520, 'referralManagement:recommendTrack', '推介页-操作追踪', 2, parent.id, 5
FROM `permission` parent
WHERE parent.`code` = 'referralManagement:recommend'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'referralManagement:recommendTrack');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 521, 'referralManagement:recommendXray', '推介页-录入感染检测及胸片', 2, parent.id, 6
FROM `permission` parent
WHERE parent.`code` = 'referralManagement:recommend'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'referralManagement:recommendXray');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 522, 'referralManagement:recommendDiagnosis', '推介页-录入诊断', 2, parent.id, 7
FROM `permission` parent
WHERE parent.`code` = 'referralManagement:recommend'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'referralManagement:recommendDiagnosis');

-- 一至五级 + 已有推介追踪管理权限的角色补全
SET @v120_rp_id := 120000000;
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v120_rp_id := @v120_rp_id + 1), r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:recommendTrack',
    'referralManagement:recommendXray',
    'referralManagement:recommendDiagnosis'
)
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = r.role AND x.`permission_id` = p.id
    );

INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v120_rp_id := @v120_rp_id + 1), src.role, p.id
FROM (
    SELECT DISTINCT rp.role
    FROM `role_permission` rp
             INNER JOIN `permission` parent ON parent.id = rp.permission_id
        AND parent.`code` IN ('referralManagement', 'referralManagement:recommend')
) src
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:recommendTrack',
    'referralManagement:recommendXray',
    'referralManagement:recommendDiagnosis'
)
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = src.role AND x.`permission_id` = p.id
    );
