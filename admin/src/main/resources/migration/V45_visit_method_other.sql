-- V45：首次/后续随访 — 随访方式增加「其他」手工录入

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'first_visit' AND COLUMN_NAME = 'visit_method_other'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `first_visit` ADD COLUMN `visit_method_other` VARCHAR(64) DEFAULT NULL COMMENT ''随访方式-其他（手工录入）'' AFTER `visit_method`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'follow_up_visit' AND COLUMN_NAME = 'visit_method_other'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `follow_up_visit` ADD COLUMN `visit_method_other` VARCHAR(64) DEFAULT NULL COMMENT ''随访方式-其他（手工录入）'' AFTER `visit_method`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
