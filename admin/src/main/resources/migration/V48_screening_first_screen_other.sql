-- V48：密接筛查初次筛查 — 影像/痰检/最终筛查结果「其他」手工录入

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'imaging_method_other'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact`
        ADD COLUMN `imaging_method_other` VARCHAR(128) DEFAULT NULL COMMENT ''影像方法-其他（手工录入）'' AFTER `imaging_method`,
        ADD COLUMN `imaging_result_other` VARCHAR(128) DEFAULT NULL COMMENT ''影像结果-其他（手工录入）'' AFTER `imaging_result`,
        ADD COLUMN `sputum_check_method_other` VARCHAR(128) DEFAULT NULL COMMENT ''痰检方法-其他（手工录入）'' AFTER `sputum_check_method`,
        ADD COLUMN `sputum_check_result_other` VARCHAR(128) DEFAULT NULL COMMENT ''痰检结果-其他（手工录入）'' AFTER `sputum_check_result`,
        ADD COLUMN `final_screening_result_other` VARCHAR(128) DEFAULT NULL COMMENT ''最终筛查结果-其他（手工录入）'' AFTER `final_screening_result`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
