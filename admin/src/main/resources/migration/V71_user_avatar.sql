-- V71：用户头像

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'avatar'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(512) DEFAULT NULL COMMENT ''头像URL'' AFTER `org_name`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
