-- V101：修复潜伏感染者「追踪 / 服药管理 / 填写领药」权限名称乱码
-- 原因：V97、V100 在未指定 utf8mb4 客户端字符集时执行，中文被二次编码写入

UPDATE `permission`
SET `name` = '追踪'
WHERE `code` = 'latentManagement:track';

UPDATE `permission`
SET `name` = '服药管理'
WHERE `code` = 'latentManagement:medication';

UPDATE `permission`
SET `name` = '填写领药'
WHERE `code` = 'latentManagement:pickup';
