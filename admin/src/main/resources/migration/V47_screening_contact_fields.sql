-- V47：密接筛查 — 联系电话与接触者关系、接触场所-其他

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'phone_contact_relation'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact`
        ADD COLUMN `phone_contact_relation` VARCHAR(64) DEFAULT NULL COMMENT ''联系电话与接触者关系'' AFTER `phone`,
        ADD COLUMN `contact_place_other` VARCHAR(128) DEFAULT NULL COMMENT ''接触场所-其他（手工录入）'' AFTER `contact_place`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
