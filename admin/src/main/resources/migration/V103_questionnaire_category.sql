-- V103：问卷分类主数据（可增删改，问卷.category 存 code）

CREATE TABLE IF NOT EXISTS `questionnaire_category` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `code` VARCHAR(50) NOT NULL COMMENT '分类编码（写入 questionnaire.category）',
    `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_questionnaire_category_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷分类';

-- 种子：兼容原前端硬编码分类
INSERT INTO `questionnaire_category` (`id`, `code`, `name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 103001, 'satisfaction', '满意度调查', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `questionnaire_category` WHERE `code` = 'satisfaction');

INSERT INTO `questionnaire_category` (`id`, `code`, `name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 103002, 'market', '市场调研', 2, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `questionnaire_category` WHERE `code` = 'market');

INSERT INTO `questionnaire_category` (`id`, `code`, `name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 103003, 'population', '人口调查', 3, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `questionnaire_category` WHERE `code` = 'population');

INSERT INTO `questionnaire_category` (`id`, `code`, `name`, `sort`, `create_time`, `update_time`, `deleted`)
SELECT 103004, 'custom', '自定义', 4, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `questionnaire_category` WHERE `code` = 'custom');

-- 菜单权限
INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 503, 'questionnaire:category', '问卷分类', 1, parent.id, 3
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:category');

INSERT IGNORE INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (500000 + r.role * 100 + (p.id - 500)), r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'questionnaire:category'
  AND NOT EXISTS (
        SELECT 1
        FROM `role_permission` rp
        WHERE rp.`role` = r.role
          AND rp.`permission_id` = p.id
    );
