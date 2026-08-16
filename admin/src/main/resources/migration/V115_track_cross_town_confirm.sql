-- 追踪大疫情跨镇导入：三级确认状态
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'cross_town_confirm_status'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `cross_town_confirm_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''跨镇确认：0无需 1待确认 2已确认 3已拒绝'' AFTER `joint_tracking_time`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'cross_town_confirm_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `cross_town_confirm_time` DATETIME DEFAULT NULL COMMENT ''跨镇确认/拒绝时间'' AFTER `cross_town_confirm_status`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
