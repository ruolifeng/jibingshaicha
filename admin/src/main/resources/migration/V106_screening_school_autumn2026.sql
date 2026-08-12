-- V106：学生筛查对齐 2026 秋季新生入学结核病筛查记录表
-- 新增：填报机构、乡镇/街道、是否参加筛查、可疑症状三列、胸部影像学方法、痰培养结果

SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'reporting_org') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `reporting_org` VARCHAR(128) DEFAULT NULL COMMENT ''填报机构'' AFTER `year`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'township') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `township` VARCHAR(128) DEFAULT NULL COMMENT ''乡镇/街道'' AFTER `district`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'participated_screening') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `participated_screening` VARCHAR(10) DEFAULT NULL COMMENT ''是否参加筛查'' AFTER `ethnicity`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'symptom_cough') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `symptom_cough` VARCHAR(16) DEFAULT NULL COMMENT ''咳嗽咳痰≥两周'' AFTER `suspicious_symptoms`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'symptom_hemoptysis') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `symptom_hemoptysis` VARCHAR(16) DEFAULT NULL COMMENT ''咯血或血痰'' AFTER `symptom_cough`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'symptom_other') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `symptom_other` VARCHAR(16) DEFAULT NULL COMMENT ''可疑症状-其他'' AFTER `symptom_hemoptysis`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'chest_xray_method') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `chest_xray_method` VARCHAR(64) DEFAULT NULL COMMENT ''胸部影像学方法'' AFTER `has_chest_xray`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'sputum_culture_result') = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `sputum_culture_result` VARCHAR(64) DEFAULT NULL COMMENT ''痰培养结果'' AFTER `molecular_biology_result`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
