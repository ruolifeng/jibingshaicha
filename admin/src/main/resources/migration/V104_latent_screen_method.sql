-- V104：潜伏感染在管记录持久化感染筛查方法，并从密接个案回填历史数据

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'screen_method'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection`
        ADD COLUMN `screen_method` VARCHAR(64) DEFAULT NULL COMMENT ''感染筛查方法（PPD/EC/IGRA 等）'' AFTER `infection_screen_date`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 密接个案同步的在管记录：按个案感染检测方法回填（完整名称归一为 PPD/EC/IGRA）
UPDATE `latent_infection` li
INNER JOIN `close_contact_case` c
    ON c.deleted = 0
    AND c.final_screening_result LIKE '%潜伏感染者%'
    AND (
        c.id_number = li.id_number
        OR UPPER(TRIM(c.id_number)) = UPPER(TRIM(li.id_number))
    )
SET li.screen_method = CASE
    WHEN c.infection_check_method IS NULL OR TRIM(c.infection_check_method) = '' THEN li.screen_method
    WHEN UPPER(c.infection_check_method) LIKE '%IGRA%'
        OR c.infection_check_method LIKE '%干扰素%' THEN 'IGRA'
    WHEN UPPER(c.infection_check_method) LIKE '%EC%'
        OR c.infection_check_method LIKE '%结核抗原%' THEN 'EC'
    WHEN UPPER(c.infection_check_method) LIKE '%PPD%'
        OR c.infection_check_method LIKE '%结核菌素%' THEN 'PPD'
    WHEN c.infection_check_method IN ('未做', '未查') THEN '未查'
    ELSE TRIM(c.infection_check_method)
END
WHERE li.deleted = 0
  AND li.population_type = 'closeContact'
  AND li.screening_id IS NULL
  AND (li.screen_method IS NULL OR TRIM(li.screen_method) = '');
