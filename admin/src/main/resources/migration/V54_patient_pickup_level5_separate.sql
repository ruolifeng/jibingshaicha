-- V54：五级用户保留「服药管理」、移除「填写领药」（与 patientManagement:medication 分离）

DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.`role` = 6
  AND p.`code` = 'patientManagement:pickup';
