-- V56：将「填写领药」从「服药管理」子节点提升为患者管理下独立权限，便于单独勾选
-- 同时确保五级用户不拥有填写领药权限（防止此前保存角色权限时被父节点联动写入）

UPDATE `permission`
SET `parent_id` = 420, `sort` = 7, `name` = '填写领药', `type` = 2
WHERE `code` = 'patientManagement:pickup';

DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.`role` = 6
  AND p.`code` = 'patientManagement:pickup';

DELETE up FROM `user_permission` up
         INNER JOIN `permission` p ON p.id = up.permission_id
         INNER JOIN `user` u ON u.id = up.user_id
WHERE u.role = 6
  AND u.deleted = 0
  AND p.`code` = 'patientManagement:pickup';
