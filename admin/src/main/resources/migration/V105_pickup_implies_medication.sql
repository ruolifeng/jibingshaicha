-- V105：填写领药隐含服药管理菜单；补齐已有角色/用户；五级仍不授予领药

-- 拥有填写领药但缺少对应服药管理的角色，补上服药管理
SET @v105_rp_id := 105000000;
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v105_rp_id := @v105_rp_id + 1), src.`role`, med.id
FROM (
    SELECT DISTINCT rp.`role`,
           CASE p.`code`
               WHEN 'patientManagement:pickup' THEN 'patientManagement:medication'
               WHEN 'latentManagement:pickup' THEN 'latentManagement:medication'
           END AS med_code
    FROM `role_permission` rp
             INNER JOIN `permission` p ON p.id = rp.permission_id
    WHERE p.`code` IN ('patientManagement:pickup', 'latentManagement:pickup')
) src
         INNER JOIN `permission` med ON med.`code` = src.med_code
WHERE src.med_code IS NOT NULL
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = src.`role` AND x.`permission_id` = med.id
    );

-- 五级明确保留服药管理菜单（患者 + 潜伏）
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v105_rp_id := @v105_rp_id + 1), 6, p.id
FROM `permission` p
WHERE p.`code` IN (
    'patientManagement', 'patientManagement:medication',
    'latentManagement', 'latentManagement:medication'
)
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = 6 AND x.`permission_id` = p.id
    );

-- 五级不允许填写领药
DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.`role` = 6
  AND p.`code` IN ('patientManagement:pickup', 'latentManagement:pickup');

-- 用户额外权限：有领药则补服药管理
SET @v105_up_id := 105100000;
INSERT INTO `user_permission` (`id`, `user_id`, `permission_id`)
SELECT (@v105_up_id := @v105_up_id + 1), src.user_id, med.id
FROM (
    SELECT DISTINCT up.user_id,
           CASE p.`code`
               WHEN 'patientManagement:pickup' THEN 'patientManagement:medication'
               WHEN 'latentManagement:pickup' THEN 'latentManagement:medication'
           END AS med_code
    FROM `user_permission` up
             INNER JOIN `permission` p ON p.id = up.permission_id
    WHERE p.`code` IN ('patientManagement:pickup', 'latentManagement:pickup')
) src
         INNER JOIN `permission` med ON med.`code` = src.med_code
WHERE src.med_code IS NOT NULL
  AND NOT EXISTS (
        SELECT 1 FROM `user_permission` x
        WHERE x.user_id = src.user_id AND x.permission_id = med.id
    );

DELETE up FROM `user_permission` up
         INNER JOIN `permission` p ON p.id = up.permission_id
         INNER JOIN `user` u ON u.id = up.user_id
WHERE u.role = 6
  AND u.deleted = 0
  AND p.`code` IN ('patientManagement:pickup', 'latentManagement:pickup');
