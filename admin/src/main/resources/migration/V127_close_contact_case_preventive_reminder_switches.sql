-- V127：密接个案 6/12/24 月随访提醒改为按「预防性治疗」拆分两个开关
-- （1）未开展预防性治疗（2）已开展预防性治疗；月份到期逻辑仍保留

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 127000001, 'close_contact_case_followup_no_preventive', '密接个案-未开展预防性治疗随访提醒',
       '密接个案表 6/12/24 月随访到期时，对「未开展」预防性治疗的人群提醒录入者', '每天 08:00', 1, 10
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'close_contact_case_followup_no_preventive');

INSERT INTO `sys_message_reminder_config`
(`id`, `code`, `name`, `description`, `schedule_hint`, `enabled`, `sort`)
SELECT 127000002, 'close_contact_case_followup_with_preventive', '密接个案-已开展预防性治疗随访提醒',
       '密接个案表 6/12/24 月随访到期时，对「开展」预防性治疗的人群提醒录入者', '每天 08:00', 1, 15
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_reminder_config` WHERE `code` = 'close_contact_case_followup_with_preventive');

-- 移除旧的按月份拆分开关，避免配置页重复
DELETE FROM `sys_message_reminder_config`
WHERE `code` IN (
    'close_contact_case_followup_6',
    'close_contact_case_followup_12',
    'close_contact_case_followup_24'
);
