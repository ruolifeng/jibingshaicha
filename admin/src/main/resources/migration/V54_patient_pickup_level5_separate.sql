-- V54：五级用户保留「服药管理」、移除「填写领药」（与 patientManagement:medication 分离）

DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.`role` = 6
  AND p.`code` = 'patientManagement:pickup';

-- 同步移除五级用户个人额外授予的「填写领药」权限
DELETE up FROM `user_permission` up
         INNER JOIN `permission` p ON p.id = up.permission_id
         INNER JOIN `user` u ON u.id = up.user_id
WHERE u.role = 6
  AND u.deleted = 0
  AND p.`code` = 'patientManagement:pickup';
