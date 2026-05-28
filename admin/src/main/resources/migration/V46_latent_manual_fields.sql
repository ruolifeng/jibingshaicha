-- V46：在管总览手动新增/导入潜伏感染者 — screening_id 可空 + 扩展字段

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'household_address'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection`
        MODIFY COLUMN `screening_id` BIGINT DEFAULT NULL COMMENT ''关联筛查数据ID（手动新增可为空）'',
        ADD COLUMN `household_address` VARCHAR(256) DEFAULT NULL COMMENT ''户籍地址'' AFTER `phone`,
        ADD COLUMN `current_address` VARCHAR(256) DEFAULT NULL COMMENT ''现住地址'' AFTER `household_address`,
        ADD COLUMN `phone_contact_relation` VARCHAR(64) DEFAULT NULL COMMENT ''联系电话与联系人关系'' AFTER `current_address`,
        ADD COLUMN `infection_screen_date` DATE DEFAULT NULL COMMENT ''感染筛查日期'' AFTER `phone_contact_relation`,
        ADD COLUMN `remark` TEXT DEFAULT NULL COMMENT ''备注'' AFTER `tracking_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
