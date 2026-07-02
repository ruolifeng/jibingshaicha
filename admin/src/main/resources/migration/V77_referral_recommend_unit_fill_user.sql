-- 推介记录：推介单位名称、填写用户名称（创建时快照）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'recommend_unit_name'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `recommend_unit_name` VARCHAR(200) DEFAULT NULL COMMENT ''推介单位名称'' AFTER `recommend_reason`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'fill_user_name'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `fill_user_name` VARCHAR(100) DEFAULT NULL COMMENT ''填写用户名称'' AFTER `recommend_unit_name`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
