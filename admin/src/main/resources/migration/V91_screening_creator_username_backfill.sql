-- V91：筛查表历史录入用户名回填（V87 仅加列未灌数）
-- 策略：有 creator_id 补 username → 从关联潜伏借 creator_id → 部门内仅一名五级用户时代填 → 再补 username

-- ========== A. 已有 creator_id、缺 username → 从 user 补 ==========
UPDATE `screening_key_population` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

UPDATE `screening_school` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

UPDATE `screening_close_contact` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

-- ========== B. 两边都空时，从关联潜伏感染借 creator_id（每条筛查取一条，避免一对多不确定） ==========
UPDATE `screening_key_population` sk
    INNER JOIN (
        SELECT li.screening_id, MIN(li.creator_id) AS creator_id
        FROM `latent_infection` li
        WHERE li.deleted = 0
          AND li.population_type IN ('keyPopulation', 'regular')
          AND li.screening_id IS NOT NULL
          AND li.creator_id IS NOT NULL
        GROUP BY li.screening_id
    ) li ON li.screening_id = sk.id
SET sk.creator_id = li.creator_id
WHERE sk.deleted = 0
  AND sk.creator_id IS NULL;

UPDATE `screening_school` ss
    INNER JOIN (
        SELECT li.screening_id, MIN(li.creator_id) AS creator_id
        FROM `latent_infection` li
        WHERE li.deleted = 0
          AND li.population_type = 'school'
          AND li.screening_id IS NOT NULL
          AND li.creator_id IS NOT NULL
        GROUP BY li.screening_id
    ) li ON li.screening_id = ss.id
SET ss.creator_id = li.creator_id
WHERE ss.deleted = 0
  AND ss.creator_id IS NULL;

UPDATE `screening_close_contact` sc
    INNER JOIN (
        SELECT li.screening_id, MIN(li.creator_id) AS creator_id
        FROM `latent_infection` li
        WHERE li.deleted = 0
          AND li.population_type = 'closeContact'
          AND li.screening_id IS NOT NULL
          AND li.creator_id IS NOT NULL
        GROUP BY li.screening_id
    ) li ON li.screening_id = sc.id
SET sc.creator_id = li.creator_id
WHERE sc.deleted = 0
  AND sc.creator_id IS NULL;

-- B 后再补 username（与 A 相同逻辑）
UPDATE `screening_key_population` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

UPDATE `screening_school` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

UPDATE `screening_close_contact` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

-- ========== C. 部门内仅一名五级用户时代填 creator_id（对齐 V55） ==========
UPDATE `screening_key_population` s
    INNER JOIN (
        SELECT u.department_id, MIN(u.id) AS user_id
        FROM `user` u
        WHERE u.role = 6 AND u.deleted = 0 AND u.department_id IS NOT NULL
        GROUP BY u.department_id
        HAVING COUNT(*) = 1
    ) solo ON solo.department_id = s.department_id
SET s.creator_id = solo.user_id
WHERE s.deleted = 0
  AND s.creator_id IS NULL
  AND s.department_id IS NOT NULL;

UPDATE `screening_school` s
    INNER JOIN (
        SELECT u.department_id, MIN(u.id) AS user_id
        FROM `user` u
        WHERE u.role = 6 AND u.deleted = 0 AND u.department_id IS NOT NULL
        GROUP BY u.department_id
        HAVING COUNT(*) = 1
    ) solo ON solo.department_id = s.department_id
SET s.creator_id = solo.user_id
WHERE s.deleted = 0
  AND s.creator_id IS NULL
  AND s.department_id IS NOT NULL;

UPDATE `screening_close_contact` s
    INNER JOIN (
        SELECT u.department_id, MIN(u.id) AS user_id
        FROM `user` u
        WHERE u.role = 6 AND u.deleted = 0 AND u.department_id IS NOT NULL
        GROUP BY u.department_id
        HAVING COUNT(*) = 1
    ) solo ON solo.department_id = s.department_id
SET s.creator_id = solo.user_id
WHERE s.deleted = 0
  AND s.creator_id IS NULL
  AND s.department_id IS NOT NULL;

-- C 后再补 username
UPDATE `screening_key_population` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

UPDATE `screening_school` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;

UPDATE `screening_close_contact` s
    INNER JOIN `user` u ON u.id = s.creator_id AND u.deleted = 0
SET s.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE s.deleted = 0
  AND (s.creator_username IS NULL OR s.creator_username = '')
  AND s.creator_id IS NOT NULL;
