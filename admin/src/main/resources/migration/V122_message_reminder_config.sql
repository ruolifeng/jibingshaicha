-- V122：消息提醒开关配置 + 密接随访提醒改为发给录入者所需数据表

CREATE TABLE IF NOT EXISTS `sys_message_reminder_config` (
    `id`            BIGINT       NOT NULL,
    `code`          VARCHAR(64)  NOT NULL COMMENT '提醒编码',
    `name`          VARCHAR(128) NOT NULL COMMENT '展示名称',
    `description`   VARCHAR(512) DEFAULT NULL COMMENT '说明',
    `schedule_hint` VARCHAR(128) DEFAULT NULL COMMENT '触发说明',
    `enabled`       TINYINT      NOT NULL DEFAULT 1 COMMENT '0关 1开',
    `sort`          INT          NOT NULL DEFAULT 0,
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) COMMENT='系统定时消息提醒开关';

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000001, 'close_contact_case_followup_6', '密接个案-6月随访提醒',
       '按个案「6月随访日期」到期窗口提醒对应录入者', '每天 08:00', 1, 10
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'close_contact_case_followup_6');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000002, 'close_contact_case_followup_12', '密接个案-12月随访提醒',
       '按个案「12月随访日期」到期窗口提醒对应录入者', '每天 08:00', 1, 20
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'close_contact_case_followup_12');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000003, 'close_contact_case_followup_24', '密接个案-24月随访提醒',
       '按个案「24月随访日期」到期窗口提醒对应录入者', '每天 08:00', 1, 30
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'close_contact_case_followup_24');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000004, 'close_contact_screening_review_6', '密接筛查-6月复查提醒',
       '按首次筛查日期窗口提醒录入者安排6月复查', '每天 08:00', 1, 40
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'close_contact_screening_review_6');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000005, 'close_contact_screening_review_12', '密接筛查-12月复查提醒',
       '按6月随访日期窗口提醒录入者安排12月复查', '每天 08:00', 1, 50
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'close_contact_screening_review_12');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000006, 'follow_up_due', '患者后续随访提醒',
       '患者下次随访到期前 7/3/1 天及当天提醒；当天提醒不受此前已读影响', '每天 08:00', 1, 60
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'follow_up_due');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000007, 'supervision_due', '督导表到期提醒',
       '潜伏督导到期前 7/3/1 天及当天提醒；当天提醒不受此前已读影响', '每天 08:00', 1, 70
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'supervision_due');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000008, 'notice_timeout', '通知单超时提醒',
       '通知单长时间未确认时提醒', '每小时', 1, 80
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'notice_timeout');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000009, 'supervision_timeout', '督导表超时提醒',
       '督导表长时间未填时提醒', '每小时', 1, 90
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'supervision_timeout');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 122000010, 'visit_timeout', '随访超时提醒',
       '随访长时间未完成时提醒', '每小时', 1, 100
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'visit_timeout');

-- 权限：消息提醒配置挂到系统消息下
INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`)
SELECT 523, 'message:reminderConfig', '消息提醒配置', 1, parent.id, 2
FROM `permission` parent
WHERE parent.`code` = 'message'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'message:reminderConfig');

SET @v122_rp_id := 122000000;
INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v122_rp_id := @v122_rp_id + 1), r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'message:reminderConfig'
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = r.role AND x.`permission_id` = p.id
    );

INSERT INTO `role_permission` (`id`, `role`, `permission_id`)
SELECT (@v122_rp_id := @v122_rp_id + 1), src.role, p.id
FROM (
    SELECT DISTINCT rp.role
    FROM `role_permission` rp
             INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'message'
) src
         CROSS JOIN `permission` p
WHERE p.`code` = 'message:reminderConfig'
  AND NOT EXISTS (
        SELECT 1 FROM `role_permission` x
        WHERE x.`role` = src.role AND x.`permission_id` = p.id
    );
