-- V32：大疫情导入从筛查管理迁移至推介追踪-追踪（权限废弃 + 角色权限同步）
UPDATE `permission`
SET `name` = CONCAT('[废弃] ', `name`)
WHERE `code` IN (
    'epidemic:screening',
    'epidemic:screening:import',
    'epidemic:screening:track',
    'epidemic:screening:xray',
    'epidemic:screening:diagnosis'
)
  AND `name` NOT LIKE '[废弃]%';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` IN (
        'epidemic:screening',
        'epidemic:screening:import',
        'epidemic:screening:track',
        'epidemic:screening:xray',
        'epidemic:screening:diagnosis'
    )
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement', 'referralManagement:track', 'referralManagement:create',
    'referralManagement:trackOperate', 'referralManagement:xray', 'referralManagement:diagnosis',
    'referralManagement:delete', 'referralManagement:epidemicImport', 'referralManagement:export',
    'referralManagement:edit'
);
