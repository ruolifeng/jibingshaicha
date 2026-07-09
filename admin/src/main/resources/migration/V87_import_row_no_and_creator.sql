-- V87：导入行号（按原 Excel 顺序展示）+ 筛查表录入用户

-- ---------- screening_school ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'creator_username'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `creator_username` VARCHAR(64) DEFAULT NULL COMMENT ''录入用户名'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- screening_key_population ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_key_population' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_key_population` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_key_population' AND COLUMN_NAME = 'creator_username'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_key_population` ADD COLUMN `creator_username` VARCHAR(64) DEFAULT NULL COMMENT ''录入用户名'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- screening_close_contact ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'creator_username'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `creator_username` VARCHAR(64) DEFAULT NULL COMMENT ''录入用户名'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- close_contact_case ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'close_contact_case' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `close_contact_case` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- referral_tracking ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- patient ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `patient` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- latent_infection ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
