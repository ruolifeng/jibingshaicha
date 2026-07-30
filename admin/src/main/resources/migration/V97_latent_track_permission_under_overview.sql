-- V97：将「潜伏感染者管理-追踪」权限挂到在管总览下（原先误挂在通知单管理）

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'latentManagement:overview'
SET child.`parent_id` = parent.id,
    child.`sort` = 3,
    child.`name` = '追踪'
WHERE child.`code` = 'latentManagement:track';
