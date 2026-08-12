-- V109：推介追踪 — 将「操作追踪/录入胸片/录入诊断」挂到「追踪」菜单下
-- 原挂在「推介」下，配置三级等账户的追踪权限时看不到录入诊断，导致追踪页无按钮

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'referralManagement:track'
SET child.`parent_id` = parent.id,
    child.`sort` = 2,
    child.`name` = '操作追踪状态'
WHERE child.`code` = 'referralManagement:trackOperate';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'referralManagement:track'
SET child.`parent_id` = parent.id,
    child.`sort` = 4,
    child.`name` = '录入胸片'
WHERE child.`code` = 'referralManagement:xray';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'referralManagement:track'
SET child.`parent_id` = parent.id,
    child.`sort` = 5,
    child.`name` = '录入诊断'
WHERE child.`code` = 'referralManagement:diagnosis';

-- 追踪页编辑按钮排序保持在胸片之前
UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'referralManagement:track'
SET child.`parent_id` = parent.id,
    child.`sort` = 3,
    child.`name` = '编辑追踪记录'
WHERE child.`code` = 'referralManagement:edit';

-- 一至五级补全（含三级 role=4），避免角色权限表缺项导致有树无按钮
-- 无 AUTO_INCREMENT，必须显式指定 id
SET @v109_rp_id := 109000000;
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v109_rp_id := @v109_rp_id + 1), r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:edit'
)
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = r.role AND x.`permission_id` = p.id
    );

INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v109_rp_id := @v109_rp_id + 1), src.role, p.id
FROM (
    SELECT DISTINCT rp.role
    FROM `role_permission` rp
             INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'referralManagement'
) src
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:edit'
)
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = src.role AND x.`permission_id` = p.id
    );
