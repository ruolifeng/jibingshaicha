-- V129：共同追踪过程编辑授权（三级以上可向参与管理的四/五级开放编辑）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'joint_tracking_edit_roles'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `joint_tracking_edit_roles` VARCHAR(32) DEFAULT NULL COMMENT ''允许编辑共同追踪过程的角色（如 5,6）'' AFTER `joint_tracking_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'joint_tracking_edit_time'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `joint_tracking_edit_time` DATETIME DEFAULT NULL COMMENT ''共同追踪编辑授权时间'' AFTER `joint_tracking_edit_roles`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'joint_tracking_edit_by'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `joint_tracking_edit_by` BIGINT DEFAULT NULL COMMENT ''共同追踪编辑授权人用户ID'' AFTER `joint_tracking_edit_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
