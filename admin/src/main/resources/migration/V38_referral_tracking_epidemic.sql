-- V38：推介追踪表补充大疫情导入字段（已部署环境执行；列已存在时自动跳过）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'source_type'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `source_type` VARCHAR(16) NOT NULL DEFAULT ''manual'' COMMENT ''manual=手动 epidemic=大疫情导入'' AFTER `track_reason`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'card_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `card_id` VARCHAR(64) DEFAULT NULL COMMENT ''卡片ID'' AFTER `source_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'parent_name'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `parent_name` VARCHAR(64) DEFAULT NULL COMMENT ''患儿家长姓名'' AFTER `card_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'workplace'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `workplace` VARCHAR(256) DEFAULT NULL COMMENT ''患者工作单位'' AFTER `parent_name`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'township'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `township` VARCHAR(128) DEFAULT NULL COMMENT ''乡镇'' AFTER `workplace`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'case_category'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `case_category` VARCHAR(64) DEFAULT NULL COMMENT ''病例分类'' AFTER `township`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'disease_name'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `disease_name` VARCHAR(128) DEFAULT NULL COMMENT ''疾病名称'' AFTER `case_category`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'report_unit'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `report_unit` VARCHAR(256) DEFAULT NULL COMMENT ''报告单位'' AFTER `disease_name`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'report_card_time'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `report_card_time` DATETIME DEFAULT NULL COMMENT ''报告卡录入时间'' AFTER `report_unit`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'epidemic_remark'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `epidemic_remark` TEXT DEFAULT NULL COMMENT ''大疫情备注'' AFTER `report_card_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'upload_batch'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `upload_batch` VARCHAR(64) DEFAULT NULL COMMENT ''导入批次号'' AFTER `epidemic_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
