-- V131：密接个案表 / 密接筛查 在「备注」后增加「患者传报卡编号」

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'close_contact_case' AND COLUMN_NAME = 'source_patient_report_card_no'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `close_contact_case` ADD COLUMN `source_patient_report_card_no` VARCHAR(64) DEFAULT NULL COMMENT ''患者传报卡编号'' AFTER `remark`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'source_patient_report_card_no'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `source_patient_report_card_no` VARCHAR(64) DEFAULT NULL COMMENT ''患者传报卡编号'' AFTER `remark`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
