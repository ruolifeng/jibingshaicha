-- V63：督导表 / 首次随访 / 后续随访 — 「填写」「修改」拆分为独立按钮权限

-- ---------- 填写 ----------
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'latentManagement:supervision:fill', '填写督导表', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'latentManagement:supervision'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:supervision:fill');

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:firstVisit:fill', '填写首次随访', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:firstVisit'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:firstVisit:fill');

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:followUp:fill', '填写后续随访', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:followUp'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:followUp:fill');

-- ---------- 修改（已存在则校正排序与名称） ----------
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'latentManagement:supervision:edit', '修改督导表', 2, parent.id, 2
FROM `permission` parent
WHERE parent.`code` = 'latentManagement:supervision'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:supervision:edit');

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:firstVisit:edit', '修改首次随访', 2, parent.id, 2
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:firstVisit'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:firstVisit:edit');

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:followUp:edit', '修改随访记录', 2, parent.id, 2
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:followUp'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:followUp:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'latentManagement:supervision'
SET child.`parent_id` = parent.id, child.`type` = 2
WHERE child.`code` IN ('latentManagement:supervision:fill', 'latentManagement:supervision:edit');
UPDATE `permission` SET `sort` = 1, `name` = '填写督导表' WHERE `code` = 'latentManagement:supervision:fill';
UPDATE `permission` SET `sort` = 2, `name` = '修改督导表' WHERE `code` = 'latentManagement:supervision:edit';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:firstVisit'
SET child.`parent_id` = parent.id, child.`type` = 2
WHERE child.`code` IN ('patientManagement:firstVisit:fill', 'patientManagement:firstVisit:edit');
UPDATE `permission` SET `sort` = 1, `name` = '填写首次随访' WHERE `code` = 'patientManagement:firstVisit:fill';
UPDATE `permission` SET `sort` = 2, `name` = '修改首次随访' WHERE `code` = 'patientManagement:firstVisit:edit';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:followUp'
SET child.`parent_id` = parent.id, child.`type` = 2
WHERE child.`code` IN ('patientManagement:followUp:fill', 'patientManagement:followUp:edit');
UPDATE `permission` SET `sort` = 1, `name` = '填写后续随访' WHERE `code` = 'patientManagement:followUp:fill';
UPDATE `permission` SET `sort` = 2, `name` = '修改随访记录' WHERE `code` = 'patientManagement:followUp:edit';

-- 拥有对应「菜单」权限的角色，默认补授填写+修改（与升级前行为一致，可在权限管理中单独取消）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE (parent.`code` = 'latentManagement:supervision'
       AND p.`code` IN ('latentManagement:supervision:fill', 'latentManagement:supervision:edit'))
   OR (parent.`code` = 'patientManagement:firstVisit'
       AND p.`code` IN ('patientManagement:firstVisit:fill', 'patientManagement:firstVisit:edit'))
   OR (parent.`code` = 'patientManagement:followUp'
       AND p.`code` IN ('patientManagement:followUp:fill', 'patientManagement:followUp:edit'));
