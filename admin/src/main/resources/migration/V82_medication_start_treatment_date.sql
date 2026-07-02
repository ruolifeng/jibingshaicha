-- V82：服药管理增加开始治疗日期（治疗记录卡）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medication_management' AND COLUMN_NAME = 'start_treatment_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `medication_management` ADD COLUMN `start_treatment_date` DATE DEFAULT NULL COMMENT ''开始治疗日期'' AFTER `sputum_result`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
