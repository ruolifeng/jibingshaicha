-- V44：市/县级用户 — 查看并管理辖区内五级用户工作（聚合菜单权限补全）
-- 说明：V16 仅赋权 role=1/2，此处为二级(3)、三级(4)、四级(5) 补全 patientManagement / latentManagement 权限

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 3 AS role UNION SELECT 4 UNION SELECT 5) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'latentManagement', 'latentManagement:overview', 'latentManagement:edit',
    'latentManagement:notice', 'latentManagement:track', 'latentManagement:xray',
    'latentManagement:diagnosis', 'latentManagement:referral', 'latentManagement:close',
    'latentManagement:supervision', 'latentManagement:history',
    'patientManagement', 'patientManagement:overview', 'patientManagement:edit',
    'patientManagement:notice', 'patientManagement:firstVisit', 'patientManagement:followUp',
    'patientManagement:medication', 'patientManagement:pickup', 'patientManagement:specialDisease',
    'patientManagement:history', 'patientManagement:referral', 'patientManagement:delete'
);
