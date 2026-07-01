-- V83：追踪/转诊增加手动录入的真实到位时间、真实转诊时间（日期精度）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'actual_arrival_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `actual_arrival_date` DATE DEFAULT NULL COMMENT ''真实到位时间（手动录入）'' AFTER `arrival_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'actual_arrival_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `actual_arrival_date` DATE DEFAULT NULL COMMENT ''真实到位时间（手动录入）'' AFTER `tracking_history_json`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'actual_referral_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `actual_referral_date` DATE DEFAULT NULL COMMENT ''真实转诊时间（手动录入）'' AFTER `referral_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral' AND COLUMN_NAME = 'actual_referral_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral` ADD COLUMN `actual_referral_date` DATE DEFAULT NULL COMMENT ''真实转诊时间（手动录入）'' AFTER `confirmed_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
