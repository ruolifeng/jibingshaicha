-- V123：密接个案表补录入人ID + 回填随访到期日，保证 6/12/24 月提醒能发给真正录入者

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'close_contact_case' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `close_contact_case` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'close_contact_case' AND INDEX_NAME = 'idx_creator_id'
);
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `close_contact_case` ADD KEY `idx_creator_id` (`creator_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 按录入用户名回填 creator_id（优先匹配 username，其次 real_name）
UPDATE `close_contact_case` c
    INNER JOIN `user` u ON u.deleted = 0
        AND (
            u.username = c.creator_username
            OR u.real_name = c.creator_username
        )
SET c.creator_id = u.id
WHERE c.deleted = 0
  AND c.creator_id IS NULL
  AND c.creator_username IS NOT NULL
  AND c.creator_username <> '';

-- 部门内仅一名五级用户时，空录入人代填（与筛查表 V91 策略一致）
UPDATE `close_contact_case` c
    INNER JOIN (
        SELECT u.department_id, MIN(u.id) AS user_id
        FROM `user` u
        WHERE u.role = 6 AND u.deleted = 0 AND u.department_id IS NOT NULL
        GROUP BY u.department_id
        HAVING COUNT(*) = 1
    ) solo ON solo.department_id = c.department_id
SET c.creator_id = solo.user_id
WHERE c.deleted = 0
  AND c.creator_id IS NULL
  AND c.department_id IS NOT NULL;

-- 有 creator_id 仍缺用户名时回填展示名
UPDATE `close_contact_case` c
    INNER JOIN `user` u ON u.id = c.creator_id AND u.deleted = 0
SET c.creator_username = COALESCE(NULLIF(TRIM(u.username), ''), NULLIF(TRIM(u.real_name), ''))
WHERE c.deleted = 0
  AND c.creator_id IS NOT NULL
  AND (c.creator_username IS NULL OR c.creator_username = '');

-- 列表展示用的 6/12/24 月到期日若未落库，按登记日补齐，供定时提醒查询
UPDATE `close_contact_case`
SET `followup_6_due_date` = DATE_ADD(`registration_date`, INTERVAL 6 MONTH)
WHERE `deleted` = 0
  AND `registration_date` IS NOT NULL
  AND `followup_6_due_date` IS NULL;

UPDATE `close_contact_case`
SET `followup_12_due_date` = DATE_ADD(`registration_date`, INTERVAL 12 MONTH)
WHERE `deleted` = 0
  AND `registration_date` IS NOT NULL
  AND `followup_12_due_date` IS NULL;

UPDATE `close_contact_case`
SET `followup_24_due_date` = DATE_ADD(`registration_date`, INTERVAL 24 MONTH)
WHERE `deleted` = 0
  AND `registration_date` IS NOT NULL
  AND `followup_24_due_date` IS NULL;
