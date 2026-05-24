-- V31：推介追踪过程记录（到位时间、追踪历史）
-- 已部署环境执行；列已存在时自动跳过

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'arrival_time'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `arrival_time` DATETIME DEFAULT NULL COMMENT ''到位时间'' AFTER `tracking_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'tracking_history_json'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `tracking_history_json` TEXT DEFAULT NULL COMMENT ''追踪过程记录JSON'' AFTER `arrival_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
