-- V58：潜伏感染者转出同步（archive_remark、source_latent_id）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'archive_remark'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `archive_remark` VARCHAR(128) DEFAULT NULL COMMENT ''归档备注（如：已转出）'' AFTER `archived_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'source_latent_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `source_latent_id` BIGINT DEFAULT NULL COMMENT ''转出复制来源潜伏感染ID'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'archive_remark'
);
SET @ddl = IF(@col_exists > 0,
    'UPDATE `latent_infection` SET `archived` = 0, `archived_time` = NULL WHERE `archive_remark` = ''已转出'' AND `archived` = 1',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
