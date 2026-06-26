-- V80：督导表增加「治疗完成情况」字段

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'supervision_form' AND COLUMN_NAME = 'treatment_completion_status'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `supervision_form` ADD COLUMN `treatment_completion_status` VARCHAR(32) DEFAULT NULL COMMENT ''治疗完成情况：完成治疗/失败/死亡/失访/不良反应停药/未评估'' AFTER `supervision_records`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
