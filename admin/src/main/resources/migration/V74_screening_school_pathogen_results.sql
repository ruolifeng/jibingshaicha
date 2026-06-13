SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'sputum_smear_result'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `screening_school`
        ADD COLUMN `sputum_smear_result` VARCHAR(64) DEFAULT NULL COMMENT ''痰涂片结果'' AFTER `chest_xray_result`,
        ADD COLUMN `molecular_biology_result` VARCHAR(64) DEFAULT NULL COMMENT ''分子生物学结果'' AFTER `sputum_smear_result`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 历史数据：疑似肺结核应留在待诊断，不应因旧逻辑写入 diagnosis_result 后被列表过滤
UPDATE `latent_infection`
SET `diagnosis_result` = NULL,
    `referral_result` = NULL,
    `archived` = 0,
    `archived_time` = NULL
WHERE `diagnosis_result` = '疑似肺结核';
