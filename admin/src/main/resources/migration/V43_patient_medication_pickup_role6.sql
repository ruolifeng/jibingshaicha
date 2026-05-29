-- V43：五级用户 — 患者管理服药/领药权限
INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`) VALUES
('patientManagement:pickup', '填写领药', 2, 424, 1);

UPDATE `permission`
SET `parent_id` = 424, `sort` = 1, `name` = '填写领药', `type` = 2
WHERE `code` = 'patientManagement:pickup';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 6, p.id
FROM `permission` p
WHERE p.`code` IN (
    'patientManagement',
    'patientManagement:medication',
    'patientManagement:pickup',
    'patientManagement:firstVisit',
    'patientManagement:followUp',
    'patientManagement:notice'
);

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:medication'
         CROSS JOIN `permission` p
WHERE p.`code` IN ('patientManagement', 'patientManagement:medication', 'patientManagement:pickup');

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:firstVisit'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:firstVisit';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:followUp'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:followUp';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:confirmNotice'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:notice';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement'
         CROSS JOIN `permission` p
WHERE p.`code` IN ('patientManagement:pickup', 'patientManagement:medication');

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup';
