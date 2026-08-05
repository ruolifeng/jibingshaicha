-- V102：问卷模板 / 问卷列表模块（从抽样系统迁入，剥离抽样耦合，按部门树隔离）

-- ==================== 清理旧筛查问卷 ====================
DELETE up FROM `user_permission` up
         INNER JOIN `permission` p ON p.id = up.permission_id
WHERE p.`code` = 'statistics:questionnaire';

DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE p.`code` = 'statistics:questionnaire';

DELETE FROM `permission` WHERE `code` = 'statistics:questionnaire';

DROP TABLE IF EXISTS `questionnaire_config`;

-- ==================== 问卷表 ====================
CREATE TABLE IF NOT EXISTS `questionnaire` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
    `title` VARCHAR(200) NOT NULL COMMENT '问卷标题',
    `description` TEXT COMMENT '问卷描述',
    `category` VARCHAR(50) DEFAULT 'custom' COMMENT '分类: satisfaction/market/population/custom',
    `template_type` VARCHAR(20) DEFAULT NULL COMMENT '模板类型: public=公用, private=专属, NULL=普通问卷',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-草稿 1-已发布 2-已暂停 3-已关闭',
    `start_time` DATETIME DEFAULT NULL COMMENT '有效期开始',
    `end_time` DATETIME DEFAULT NULL COMMENT '有效期结束',
    `total_visits` INT NOT NULL DEFAULT 0 COMMENT '访问量',
    `total_responses` INT NOT NULL DEFAULT 0 COMMENT '填写量',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_questionnaire_template_type` (`template_type`),
    KEY `idx_questionnaire_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷/模板表';

CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `questionnaire_id` BIGINT NOT NULL COMMENT '所属问卷ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `type` VARCHAR(30) NOT NULL COMMENT '题型',
    `title` VARCHAR(500) NOT NULL COMMENT '题目标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '题目说明',
    `required` TINYINT NOT NULL DEFAULT 1 COMMENT '是否必填: 0-选填 1-必填',
    `options` JSON COMMENT '选项列表',
    `validation_rules` JSON COMMENT '验证规则',
    `logic_rules` JSON COMMENT '逻辑跳转',
    `page_number` INT NOT NULL DEFAULT 1 COMMENT '所在页码',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_questionnaire_id` (`questionnaire_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷题目表';

CREATE TABLE IF NOT EXISTS `questionnaire_response` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `questionnaire_id` BIGINT NOT NULL COMMENT '所属问卷ID',
    `access_token` VARCHAR(100) DEFAULT NULL COMMENT '访问令牌',
    `respondent_ip` VARCHAR(50) DEFAULT NULL COMMENT '填写者IP',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-进行中 1-已提交 2-不良样本',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始填写时间',
    `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
    `duration_seconds` INT DEFAULT NULL COMMENT '填写时长(秒)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_questionnaire_id` (`questionnaire_id`),
    KEY `idx_access_token` (`access_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷回收表';

CREATE TABLE IF NOT EXISTS `questionnaire_answer` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `response_id` BIGINT NOT NULL COMMENT '所属回收ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `answer_value` TEXT COMMENT '答案值',
    PRIMARY KEY (`id`),
    KEY `idx_response_id` (`response_id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷答案表';

-- ==================== 权限 ====================
INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 500, 'questionnaire', '问卷管理', 1, 0, 12
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 501, 'questionnaire:list', '问卷列表', 1, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:list');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 502, 'questionnaire:template:view', '问卷模板', 1, parent.id, 2
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:template:view');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 510, 'questionnaire:create', '创建问卷', 2, parent.id, 10
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:create');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 511, 'questionnaire:update', '编辑问卷', 2, parent.id, 11
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:update');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 512, 'questionnaire:delete', '删除问卷', 2, parent.id, 12
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:delete');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 513, 'questionnaire:publish', '发布问卷', 2, parent.id, 13
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:publish');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 514, 'questionnaire:data', '问卷数据', 2, parent.id, 14
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:data');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 515, 'questionnaire:data:export', '数据导出', 2, parent.id, 15
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:data:export');

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 516, 'questionnaire:template:delete', '删除模板', 2, parent.id, 16
FROM `permission` parent
WHERE parent.`code` = 'questionnaire'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'questionnaire:template:delete');

-- 默认授予超级管理员、一至三级
INSERT IGNORE INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (500000 + r.role * 100 + (p.id - 500)), r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
                   'questionnaire',
                   'questionnaire:list',
                   'questionnaire:template:view',
                   'questionnaire:create',
                   'questionnaire:update',
                   'questionnaire:delete',
                   'questionnaire:publish',
                   'questionnaire:data',
                   'questionnaire:data:export',
                   'questionnaire:template:delete'
    )
  AND NOT EXISTS (
        SELECT 1
        FROM `role_permission` rp
        WHERE rp.`role` = r.role
          AND rp.`permission_id` = p.id
    );
