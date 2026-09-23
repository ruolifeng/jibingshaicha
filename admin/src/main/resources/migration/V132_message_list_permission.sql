-- V132：系统消息拆出「消息列表」叶子权限，与「消息提醒配置」独立可控
-- 背景：原先 message 既是分组又是消息列表权限，且提醒配置路由 anyPermission 含 message，
-- 导致关闭 message:reminderConfig 后，只要还有消息列表权限仍能看到提醒配置菜单。

-- 1) 新增消息列表叶子权限（挂在系统消息下，与 reminderConfig 同级）
INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
-- 525：远程库 524 已被 referralManagement:recommendEdit 占用
SELECT 525, 'message:list', '消息列表', 1, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'message'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'message:list');

-- 提醒配置排序靠后，保证树中「消息列表」在前
UPDATE `permission`
SET `sort` = 2
WHERE `code` = 'message:reminderConfig'
  AND (`sort` IS NULL OR `sort` <> 2);

-- 2) 角色：凡已有系统消息（message）的，自动补上消息列表（不自动补提醒配置）
SET @v132_rp_id := 132000000;
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v132_rp_id := @v132_rp_id + 1), src.role, list_perm.id
FROM (
    SELECT DISTINCT rp.role
    FROM `role_permission` rp
             INNER JOIN `permission` p ON p.id = rp.permission_id AND p.`code` = 'message'
) src
         CROSS JOIN `permission` list_perm
WHERE list_perm.`code` = 'message:list'
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = src.role AND x.`permission_id` = list_perm.id
    );

-- 3) 用户额外权限：同样补 message:list
SET @v132_up_id := 132100000;
INSERT INTO `user_permission` (`id`, `user_id`, `permission_id`)
SELECT (@v132_up_id := @v132_up_id + 1), src.user_id, list_perm.id
FROM (
    SELECT DISTINCT up.user_id
    FROM `user_permission` up
             INNER JOIN `permission` p ON p.id = up.permission_id AND p.`code` = 'message'
) src
         CROSS JOIN `permission` list_perm
WHERE list_perm.`code` = 'message:list'
  AND NOT EXISTS (
        SELECT 1 FROM `user_permission` x
        WHERE x.`user_id` = src.user_id AND x.`permission_id` = list_perm.id
    );
