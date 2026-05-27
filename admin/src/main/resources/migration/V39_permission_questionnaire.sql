-- V39：权限树清理废弃项 + 统计分析问卷权限

INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(131, 'statistics:questionnaire', '筛查问卷', 2, 4, 2);

UPDATE `permission`
SET `parent_id` = 4, `sort` = 2, `name` = '筛查问卷', `type` = 2
WHERE `code` = 'statistics:questionnaire';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'statistics:questionnaire';

DELETE rp FROM `role_permission` rp
    INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE p.`name` LIKE '[废弃]%'
   OR p.`parent_id` IN (SELECT id FROM (SELECT id FROM `permission` WHERE `name` LIKE '[废弃]%') AS deprecated_parents)
   OR p.`parent_id` IN (
       SELECT id FROM (
           SELECT c.id
           FROM `permission` c
                    INNER JOIN `permission` parent ON parent.id = c.parent_id
           WHERE parent.`name` LIKE '[废弃]%'
       ) AS deprecated_children
   );
