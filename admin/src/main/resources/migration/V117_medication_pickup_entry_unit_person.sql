-- 领药记录：录入单位、录入人员
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medication_pickup' AND COLUMN_NAME = 'entry_unit'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `medication_pickup` ADD COLUMN `entry_unit` VARCHAR(200) DEFAULT NULL COMMENT ''录入单位'' AFTER `dispensing_unit`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medication_pickup' AND COLUMN_NAME = 'entry_person'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `medication_pickup` ADD COLUMN `entry_person` VARCHAR(100) DEFAULT NULL COMMENT ''录入人员'' AFTER `entry_unit`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
