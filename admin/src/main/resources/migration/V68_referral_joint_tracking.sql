-- V68：推介追踪共同追踪（接收方开启后，发起方与接收方均可操作，追踪次数合并计算）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'joint_tracking'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `joint_tracking` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否共同追踪：0否 1是'' AFTER `recommend_confirm_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'joint_tracking_time'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `joint_tracking_time` DATETIME DEFAULT NULL COMMENT ''开启共同追踪时间'' AFTER `joint_tracking`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
