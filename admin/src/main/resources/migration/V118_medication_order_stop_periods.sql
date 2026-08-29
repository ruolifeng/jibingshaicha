-- 服药管理：医嘱停药时间/原因（可多段）
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medication_management' AND COLUMN_NAME = 'order_stop_periods'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `medication_management` ADD COLUMN `order_stop_periods` JSON DEFAULT NULL COMMENT ''医嘱停药多段 [{startDate,endDate,reason}]'' AFTER `medication_records`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
