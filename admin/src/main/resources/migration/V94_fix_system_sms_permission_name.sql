-- V94：修复「短信配置」权限名称乱码（迁移时客户端字符集导致）

UPDATE `permission`
SET `name` = '短信配置'
WHERE `code` = 'system:sms';
