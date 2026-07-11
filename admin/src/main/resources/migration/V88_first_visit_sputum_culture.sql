-- V88：首次入户随访增加痰培养及补充状态

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'first_visit' AND COLUMN_NAME = 'sputum_culture') = 0,
    'ALTER TABLE `first_visit` ADD COLUMN `sputum_culture` VARCHAR(32) DEFAULT NULL COMMENT ''痰培养情况'' AFTER `sputum_status`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'first_visit' AND COLUMN_NAME = 'sputum_culture_supplement_status') = 0,
    'ALTER TABLE `first_visit` ADD COLUMN `sputum_culture_supplement_status` TINYINT DEFAULT NULL COMMENT ''痰培养补充状态：0未补充 1已补充'' AFTER `sputum_culture`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
