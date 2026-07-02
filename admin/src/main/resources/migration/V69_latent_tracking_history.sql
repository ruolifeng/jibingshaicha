-- 潜伏感染/待诊断追踪：增加追踪历史 JSON，记录每次追踪的备注

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'tracking_history_json'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `tracking_history_json` TEXT DEFAULT NULL COMMENT ''追踪历史JSON（每次追踪的状态、时间、备注）'' AFTER `tracking_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
