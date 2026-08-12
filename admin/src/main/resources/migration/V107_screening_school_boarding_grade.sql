-- V107：学生筛查对齐《2026年秋季新生入学结核病筛查记录表新》
-- 新增：是否寄宿制、年级

SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'boarding_type') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `boarding_type` VARCHAR(32) DEFAULT NULL COMMENT ''是否寄宿制'' AFTER `school_type`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'grade_name') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `grade_name` VARCHAR(64) DEFAULT NULL COMMENT ''年级'' AFTER `school_name`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
