-- V111：潜伏感染者通知单增加登记号，并同步到潜伏感染主表
SET @exist_notice_reg := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notice'
      AND COLUMN_NAME = 'registration_no'
);
SET @sql_notice_reg := IF(
    @exist_notice_reg = 0,
    'ALTER TABLE `notice` ADD COLUMN `registration_no` VARCHAR(64) DEFAULT NULL COMMENT ''登记号（潜伏感染者通知单填写）'' AFTER `household_address`',
    'SELECT 1'
);
PREPARE stmt_notice_reg FROM @sql_notice_reg;
EXECUTE stmt_notice_reg;
DEALLOCATE PREPARE stmt_notice_reg;

SET @exist_latent_reg := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'latent_infection'
      AND COLUMN_NAME = 'registration_no'
);
SET @sql_latent_reg := IF(
    @exist_latent_reg = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `registration_no` VARCHAR(64) DEFAULT NULL COMMENT ''登记号（来自通知单同步）'' AFTER `phone`',
    'SELECT 1'
);
PREPARE stmt_latent_reg FROM @sql_latent_reg;
EXECUTE stmt_latent_reg;
DEALLOCATE PREPARE stmt_latent_reg;

-- 历史数据：从已有通知单回填登记号
UPDATE `latent_infection` li
INNER JOIN `notice` n ON n.`biz_id` = li.`id`
    AND n.`notice_type` = 'latent'
    AND n.`deleted` = 0
SET li.`registration_no` = n.`registration_no`
WHERE n.`registration_no` IS NOT NULL
  AND n.`registration_no` <> ''
  AND (li.`registration_no` IS NULL OR li.`registration_no` = '');
