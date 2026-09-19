-- V128：潜伏感染者总览支持民族、治疗方案（持久化；详情起止时间仍由督导/筛查回填）

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'ethnicity'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection`
        ADD COLUMN `ethnicity` VARCHAR(32) DEFAULT NULL COMMENT ''民族'' AFTER `screen_method`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'treatment_plan'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection`
        ADD COLUMN `treatment_plan` VARCHAR(256) DEFAULT NULL COMMENT ''治疗方案'' AFTER `ethnicity`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 优先从最新已提交/归档督导表回填治疗方案（仅空值）
UPDATE `latent_infection` li
    INNER JOIN (
        SELECT sf.latent_infection_id, sf.treatment_plan
        FROM `supervision_form` sf
        INNER JOIN (
            SELECT latent_infection_id, MAX(id) AS max_id
            FROM `supervision_form`
            WHERE deleted = 0 AND status >= 1 AND treatment_plan IS NOT NULL AND TRIM(treatment_plan) <> ''
            GROUP BY latent_infection_id
        ) latest ON latest.max_id = sf.id
    ) sup ON sup.latent_infection_id = li.id
SET li.treatment_plan = NULLIF(TRIM(sup.treatment_plan), '')
WHERE li.deleted = 0
  AND (li.treatment_plan IS NULL OR TRIM(li.treatment_plan) = '');

-- 其次从通知单回填治疗方案（仅空值）
UPDATE `latent_infection` li
    INNER JOIN (
        SELECT n.biz_id, n.treatment_plan
        FROM `notice` n
        INNER JOIN (
            SELECT biz_id, MAX(id) AS max_id
            FROM `notice`
            WHERE deleted = 0 AND notice_type = 'latent'
              AND treatment_plan IS NOT NULL AND TRIM(treatment_plan) <> ''
            GROUP BY biz_id
        ) latest ON latest.max_id = n.id
    ) nt ON nt.biz_id = li.id
SET li.treatment_plan = NULLIF(TRIM(nt.treatment_plan), '')
WHERE li.deleted = 0
  AND (li.treatment_plan IS NULL OR TRIM(li.treatment_plan) = '');

-- 最后从筛查预防方案回填治疗方案（仅空值）
UPDATE `latent_infection` li
    INNER JOIN `screening_school` s ON s.id = li.screening_id AND s.deleted = 0
SET li.treatment_plan = NULLIF(TRIM(s.preventive_plan), '')
WHERE li.deleted = 0
  AND li.population_type = 'school'
  AND (li.treatment_plan IS NULL OR TRIM(li.treatment_plan) = '')
  AND s.preventive_plan IS NOT NULL AND TRIM(s.preventive_plan) <> '';

UPDATE `latent_infection` li
    INNER JOIN `screening_key_population` k ON k.id = li.screening_id AND k.deleted = 0
SET li.treatment_plan = NULLIF(TRIM(k.preventive_plan), '')
WHERE li.deleted = 0
  AND li.population_type IN ('keyPopulation', 'regular')
  AND (li.treatment_plan IS NULL OR TRIM(li.treatment_plan) = '')
  AND k.preventive_plan IS NOT NULL AND TRIM(k.preventive_plan) <> '';

UPDATE `latent_infection` li
    INNER JOIN `screening_close_contact` c ON c.id = li.screening_id AND c.deleted = 0
SET li.treatment_plan = NULLIF(TRIM(c.preventive_plan), '')
WHERE li.deleted = 0
  AND li.population_type = 'closeContact'
  AND (li.treatment_plan IS NULL OR TRIM(li.treatment_plan) = '')
  AND c.preventive_plan IS NOT NULL AND TRIM(c.preventive_plan) <> '';

-- 从筛查表回填民族（仅空值）
UPDATE `latent_infection` li
    INNER JOIN `screening_school` s ON s.id = li.screening_id AND s.deleted = 0
SET li.ethnicity = NULLIF(TRIM(s.ethnicity), '')
WHERE li.deleted = 0
  AND li.population_type = 'school'
  AND (li.ethnicity IS NULL OR TRIM(li.ethnicity) = '')
  AND s.ethnicity IS NOT NULL AND TRIM(s.ethnicity) <> '';

UPDATE `latent_infection` li
    INNER JOIN `screening_key_population` k ON k.id = li.screening_id AND k.deleted = 0
SET li.ethnicity = NULLIF(TRIM(k.ethnicity), '')
WHERE li.deleted = 0
  AND li.population_type IN ('keyPopulation', 'regular')
  AND (li.ethnicity IS NULL OR TRIM(li.ethnicity) = '')
  AND k.ethnicity IS NOT NULL AND TRIM(k.ethnicity) <> '';

UPDATE `latent_infection` li
    INNER JOIN `screening_close_contact` c ON c.id = li.screening_id AND c.deleted = 0
SET li.ethnicity = NULLIF(TRIM(c.ethnicity), '')
WHERE li.deleted = 0
  AND li.population_type = 'closeContact'
  AND (li.ethnicity IS NULL OR TRIM(li.ethnicity) = '')
  AND c.ethnicity IS NOT NULL AND TRIM(c.ethnicity) <> '';
