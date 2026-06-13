-- V72：推介/追踪诊断其他备注

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'diagnosis_remark'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `diagnosis_remark` TEXT DEFAULT NULL COMMENT ''诊断结果选择其他时的备注'' AFTER `diagnosis_result`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
