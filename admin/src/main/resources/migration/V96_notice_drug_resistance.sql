-- V96：患者通知单增加耐药情况

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'drug_resistance') = 0,
    'ALTER TABLE `notice` ADD COLUMN `drug_resistance` VARCHAR(16) DEFAULT NULL COMMENT ''耐药情况：耐药/非耐药/未检测'' AFTER `treatment_plan`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
