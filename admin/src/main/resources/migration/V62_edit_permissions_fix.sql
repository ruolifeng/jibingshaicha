-- V62：补全三类「修改」权限（按 code 写入，避免固定 id 冲突）
-- 注意：仅执行本脚本不会创建「填写」权限，请继续执行 V63_fill_edit_button_permissions.sql
-- 适用：权限管理树中看不到「修改」子项，或仅超级管理员能改的情况

-- 1. 潜伏感染者 — 修改督导表
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'latentManagement:supervision:edit', '修改督导表', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'latentManagement:supervision'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:supervision:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'latentManagement:supervision'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '修改督导表', child.`type` = 2
WHERE child.`code` = 'latentManagement:supervision:edit';

-- 2. 患者管理 — 编辑首次随访
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:firstVisit:edit', '编辑首次随访', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:firstVisit'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:firstVisit:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:firstVisit'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '编辑首次随访', child.`type` = 2
WHERE child.`code` = 'patientManagement:firstVisit:edit';

-- 3. 患者管理 — 修改随访记录
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:followUp:edit', '修改随访记录', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:followUp'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:followUp:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:followUp'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '修改随访记录', child.`type` = 2
WHERE child.`code` = 'patientManagement:followUp:edit';

-- 4. 已为对应菜单权限的角色，补授编辑权限（保持升级前「填写后仍可改」的行为）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE (parent.`code` = 'latentManagement:supervision' AND p.`code` = 'latentManagement:supervision:edit')
   OR (parent.`code` = 'patientManagement:firstVisit' AND p.`code` = 'patientManagement:firstVisit:edit')
   OR (parent.`code` = 'patientManagement:followUp' AND p.`code` = 'patientManagement:followUp:edit');
