-- V67：患者通知单 — 拆分「填写通知单」按钮权限；学校/重点人群并入筛查管理权限树

-- ---------- 1. 通知单管理下增加「填写通知单」 ----------
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:notice:fill', '填写通知单', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:notice'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:notice:fill');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:notice'
SET child.`parent_id` = parent.id, child.`type` = 2, child.`sort` = 1, child.`name` = '填写通知单'
WHERE child.`code` = 'patientManagement:notice:fill';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:notice'
SET child.`parent_id` = parent.id, child.`type` = 2, child.`sort` = 2, child.`name` = '删除患者'
WHERE child.`code` = 'patientManagement:delete';

-- 拥有「通知单管理」菜单权限的角色，默认补授「填写通知单」（五级用户仅接收，不自动授予）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE parent.`code` = 'patientManagement:notice'
  AND p.`code` = 'patientManagement:notice:fill'
  AND rp.role != 6;

DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.role = 6 AND p.`code` = 'patientManagement:notice:fill';

-- ---------- 2. 学校人群、重点人群并入「筛查管理」权限树 ----------
UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'screening'
SET child.`parent_id` = parent.id, child.`sort` = 1
WHERE child.`code` = 'school';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'screening'
SET child.`parent_id` = parent.id, child.`sort` = 2
WHERE child.`code` = 'keyPopulation';

UPDATE `permission` SET `sort` = 3 WHERE `code` = 'regular:screening';
UPDATE `permission` SET `sort` = 4 WHERE `code` = 'regular:suspected';
UPDATE `permission` SET `sort` = 5 WHERE `code` = 'epidemic:screening';

-- 已分配学校/重点人群相关权限的角色，补授「筛查管理」父菜单（保证侧栏与权限树联动正常）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old ON old.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE p.`code` = 'screening'
  AND old.`code` IN (
      'school', 'keyPopulation',
      'school:screening', 'school:suspected',
      'keyPopulation:screening', 'keyPopulation:suspected'
  );
