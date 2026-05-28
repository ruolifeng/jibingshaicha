-- V49：患者/潜伏感染者 — 录入人（五级「谁录入谁可见」）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `patient` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
