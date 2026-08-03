-- V99：统计分析 — 重点人群肺结核可疑症状筛查和推介情况报表权限

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 132, 'statistics:keyPopulationTbSymptomReferral', '重点人群结核症状筛查推介', 2, parent.id, 3
FROM `permission` parent
WHERE parent.`code` = 'statistics'
  AND NOT EXISTS (
      SELECT 1 FROM `permission` WHERE `code` = 'statistics:keyPopulationTbSymptomReferral'
  );

UPDATE `permission`
SET `parent_id` = (SELECT id FROM (SELECT id FROM `permission` WHERE `code` = 'statistics') t),
    `sort` = 3,
    `name` = '重点人群结核症状筛查推介',
    `type` = 2
WHERE `code` = 'statistics:keyPopulationTbSymptomReferral';

-- 默认授予超级管理员、一至三级（与 statistics:questionnaire / statistics:export 范围一致）
INSERT IGNORE INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (132000 + r.role), r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'statistics:keyPopulationTbSymptomReferral'
  AND NOT EXISTS (
      SELECT 1
      FROM `role_permission` rp
      WHERE rp.`role` = r.role
        AND rp.`permission_id` = p.id
  );

-- 已拥有「统计分析」权限的角色一并补授本报表
INSERT IGNORE INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (132100 + rp.role), rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'statistics'
         CROSS JOIN `permission` p
WHERE p.`code` = 'statistics:keyPopulationTbSymptomReferral'
  AND NOT EXISTS (
      SELECT 1
      FROM `role_permission` existing
      WHERE existing.`role` = rp.role
        AND existing.`permission_id` = p.id
  );
