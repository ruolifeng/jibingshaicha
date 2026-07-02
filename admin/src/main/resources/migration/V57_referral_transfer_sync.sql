-- V57：转出确认后同步复制患者至接收方部门，并记录目标业务 ID
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral' AND COLUMN_NAME = 'target_biz_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral` ADD COLUMN `target_biz_id` BIGINT DEFAULT NULL COMMENT ''接收确认后在接收方生成的业务记录ID'' AFTER `biz_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'source_patient_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `patient` ADD COLUMN `source_patient_id` BIGINT DEFAULT NULL COMMENT ''转出复制来源患者ID'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 历史「已转出」归档记录恢复为在管列表展示（patient 表需已有 archive_remark 列）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'archive_remark'
);
SET @ddl = IF(@col_exists > 0,
    'UPDATE `patient` SET `archived` = 0, `archived_time` = NULL WHERE `archive_remark` = ''已转出'' AND `archived` = 1',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
