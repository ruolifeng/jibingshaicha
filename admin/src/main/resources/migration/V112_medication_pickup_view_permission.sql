-- V112：患者/潜伏「查看记录」独立按钮权限（与服药管理、填写领药分离）

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 482, 'patientManagement:pickupView', '查看记录', 2, parent.id, 8
FROM `permission` parent
WHERE parent.`code` = 'patientManagement'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:pickupView');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 483, 'latentManagement:pickupView', '查看记录', 2, parent.id, 6
FROM `permission` parent
WHERE parent.`code` = 'latentManagement'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:pickupView');

-- 已有服药管理或填写领药的角色补上「查看记录」（含五级）
SET @v112_rp_id := 112000000;
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v112_rp_id := @v112_rp_id + 1), src.`role`, view_p.id
FROM (
    SELECT DISTINCT rp.`role`,
           CASE p.`code`
               WHEN 'patientManagement:medication' THEN 'patientManagement:pickupView'
               WHEN 'patientManagement:pickup' THEN 'patientManagement:pickupView'
               WHEN 'latentManagement:medication' THEN 'latentManagement:pickupView'
               WHEN 'latentManagement:pickup' THEN 'latentManagement:pickupView'
           END AS view_code
    FROM `role_permission` rp
             INNER JOIN `permission` p ON p.id = rp.permission_id
    WHERE p.`code` IN (
        'patientManagement:medication', 'patientManagement:pickup',
        'latentManagement:medication', 'latentManagement:pickup'
    )
) src
         INNER JOIN `permission` view_p ON view_p.`code` = src.view_code
WHERE src.view_code IS NOT NULL
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = src.`role` AND x.`permission_id` = view_p.id
    );

-- 用户额外权限：有服药管理/填写领药则补查看记录
SET @v112_up_id := 112100000;
INSERT INTO `user_permission` (`id`, `user_id`, `permission_id`)
SELECT (@v112_up_id := @v112_up_id + 1), src.user_id, view_p.id
FROM (
    SELECT DISTINCT up.user_id,
           CASE p.`code`
               WHEN 'patientManagement:medication' THEN 'patientManagement:pickupView'
               WHEN 'patientManagement:pickup' THEN 'patientManagement:pickupView'
               WHEN 'latentManagement:medication' THEN 'latentManagement:pickupView'
               WHEN 'latentManagement:pickup' THEN 'latentManagement:pickupView'
           END AS view_code
    FROM `user_permission` up
             INNER JOIN `permission` p ON p.id = up.permission_id
    WHERE p.`code` IN (
        'patientManagement:medication', 'patientManagement:pickup',
        'latentManagement:medication', 'latentManagement:pickup'
    )
) src
         INNER JOIN `permission` view_p ON view_p.`code` = src.view_code
WHERE src.view_code IS NOT NULL
  AND NOT EXISTS (
        SELECT 1 FROM `user_permission` x
        WHERE x.user_id = src.user_id AND x.permission_id = view_p.id
    );
