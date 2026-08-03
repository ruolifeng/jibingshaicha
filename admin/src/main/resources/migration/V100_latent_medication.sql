-- V100：潜伏感染者服药管理 / 领药（复用 medication_* 表，增加 latent_infection_id）
-- 说明：权限表已无 AUTO_INCREMENT，插入必须显式指定 id（与 init.sql 480/481 对齐）

-- 服药管理表：支持按潜伏感染者关联
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medication_management' AND COLUMN_NAME = 'latent_infection_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `medication_management`
        MODIFY COLUMN `patient_id` BIGINT NULL COMMENT ''关联患者ID（潜伏感染记录为空）'',
        ADD COLUMN `latent_infection_id` BIGINT NULL COMMENT ''关联潜伏感染者ID'' AFTER `patient_id`,
        ADD KEY `idx_latent` (`latent_infection_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 领药记录表：支持按潜伏感染者关联
SET @col_exists2 := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medication_pickup' AND COLUMN_NAME = 'latent_infection_id'
);
SET @sql2 := IF(@col_exists2 = 0,
    'ALTER TABLE `medication_pickup`
        MODIFY COLUMN `patient_id` BIGINT NULL COMMENT ''关联患者ID（潜伏感染记录为空）'',
        ADD COLUMN `latent_infection_id` BIGINT NULL COMMENT ''关联潜伏感染者ID'' AFTER `patient_id`,
        ADD KEY `idx_latent` (`latent_infection_id`)',
    'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 菜单：服药管理（插入到督导表与历史患者之间）
UPDATE `permission` SET `sort` = 4 WHERE `code` = 'latentManagement:history';

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 480, 'latentManagement:medication', '服药管理', 1, parent.id, 3
FROM `permission` parent
WHERE parent.`code` = 'latentManagement'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:medication');

-- 按钮：填写领药（挂在潜伏感染者管理下，与患者侧分离勾选一致）
INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 481, 'latentManagement:pickup', '填写领药', 2, parent.id, 5
FROM `permission` parent
WHERE parent.`code` = 'latentManagement'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:pickup');

-- 拥有潜伏感染者管理（或在管总览）的角色同步获得服药管理菜单
-- 先按 role 去重，避免同一角色因同时有 parent/overview 产生重复 id
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (480000 + roles.`role`), roles.`role`, p.id
FROM (
    SELECT DISTINCT rp.`role`
    FROM `role_permission` rp
             INNER JOIN `permission` existing ON existing.id = rp.permission_id
        AND existing.`code` IN ('latentManagement', 'latentManagement:overview')
) roles
         INNER JOIN `permission` p ON p.`code` = 'latentManagement:medication'
WHERE NOT EXISTS (
    SELECT 1 FROM `role_permission` x WHERE x.`role` = roles.`role` AND x.`permission_id` = p.id
);

-- 五级用户明确授予服药管理（与患者侧 V43 策略一致，不含填写领药）
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (480100 + (CASE p.`code` WHEN 'latentManagement' THEN 0 ELSE 1 END)), 6, p.id
FROM `permission` p
WHERE p.`code` IN ('latentManagement', 'latentManagement:medication')
  AND NOT EXISTS (
    SELECT 1 FROM `role_permission` x WHERE x.`role` = 6 AND x.`permission_id` = p.id
);

-- 非五级角色同步获得填写领药（与患者侧策略一致）
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (481000 + roles.`role`), roles.`role`, p.id
FROM (
    SELECT DISTINCT rp.`role`
    FROM `role_permission` rp
             INNER JOIN `permission` existing ON existing.id = rp.permission_id
        AND existing.`code` IN ('latentManagement', 'latentManagement:overview', 'latentManagement:medication')
    WHERE rp.`role` <> 6
) roles
         INNER JOIN `permission` p ON p.`code` = 'latentManagement:pickup'
WHERE NOT EXISTS (
    SELECT 1 FROM `role_permission` x WHERE x.`role` = roles.`role` AND x.`permission_id` = p.id
);

-- 五级用户明确不授予填写领药
DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.`role` = 6
  AND p.`code` = 'latentManagement:pickup';

DELETE up FROM `user_permission` up
         INNER JOIN `permission` p ON p.id = up.permission_id
         INNER JOIN `user` u ON u.id = up.user_id
WHERE u.role = 6
  AND u.deleted = 0
  AND p.`code` = 'latentManagement:pickup';
