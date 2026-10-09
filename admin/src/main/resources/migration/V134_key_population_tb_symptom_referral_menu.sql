-- V134：重点人群结核症状筛查推介 从「统计分析」迁至「重点人群」
-- 并默认授予一至五级（三级/四级看本区县及下属，五级看所在卫生院）

-- 1）权限挂到重点人群下，并统一权限码
UPDATE `permission`
SET `code` = 'keyPopulation:tbSymptomReferral',
    `name` = '重点人群结核症状筛查推介',
    `type` = 1,
    `parent_id` = (SELECT id FROM (SELECT id FROM `permission` WHERE `code` = 'keyPopulation') t),
    `sort` = 6
WHERE `code` IN ('statistics:keyPopulationTbSymptomReferral', 'keyPopulation:tbSymptomReferral');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 132, 'keyPopulation:tbSymptomReferral', '重点人群结核症状筛查推介', 1, parent.id, 6
FROM `permission` parent
WHERE parent.`code` = 'keyPopulation'
  AND NOT EXISTS (
      SELECT 1 FROM `permission`
      WHERE `code` IN ('keyPopulation:tbSymptomReferral', 'statistics:keyPopulationTbSymptomReferral')
  );

-- 2）默认授予超级管理员、一至五级
SET @v134_rp_id := 134000000;
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v134_rp_id := @v134_rp_id + 1), r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'keyPopulation:tbSymptomReferral'
  AND NOT EXISTS (
      SELECT 1
      FROM `role_permission` rp
      WHERE rp.`role` = r.role
        AND rp.`permission_id` = p.id
  );

-- 3）已拥有重点人群相关权限的角色一并补授本报表
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v134_rp_id := @v134_rp_id + 1), src.role, p.id
FROM (
    SELECT DISTINCT rp.role
    FROM `role_permission` rp
             INNER JOIN `permission` parent ON parent.id = rp.permission_id
    WHERE parent.`code` IN (
        'keyPopulation',
        'keyPopulation:screening',
        'keyPopulation:suspected'
    )
) src
         CROSS JOIN `permission` p
WHERE p.`code` = 'keyPopulation:tbSymptomReferral'
  AND NOT EXISTS (
      SELECT 1
      FROM `role_permission` existing
      WHERE existing.`role` = src.role
        AND existing.`permission_id` = p.id
  );
