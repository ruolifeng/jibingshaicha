-- V85：重点人群/疫情筛查表增加 Excel 导入行号，列表按原 Excel 行顺序展示

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_key_population' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_key_population` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
