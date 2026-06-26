-- V81：潜伏感染者表增加人群分类字段（手动新增/导入持久化）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'crowd_category'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `crowd_category` VARCHAR(128) DEFAULT NULL COMMENT ''人群分类（重点人群：老年人/糖尿病/双感；密接：家庭内/家庭外）'' AFTER `population_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
