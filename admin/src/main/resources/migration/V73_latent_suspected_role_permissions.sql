-- V73：待诊断操作权限补齐
-- 学生/重点/疫情待诊断页共用 latent:track / latent:xray / latent:diagnosis。
-- 五级用户导入筛查数据后需要继续完成追踪、胸片、诊断；同时幂等补齐一至四级历史库缺失权限。

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.`role`, p.`id`
FROM (
    SELECT 2 AS `role`
    UNION SELECT 3
    UNION SELECT 4
    UNION SELECT 5
    UNION SELECT 6
) r
CROSS JOIN `permission` p
WHERE p.`code` IN ('latent:track', 'latent:xray', 'latent:diagnosis');
