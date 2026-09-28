-- V133：密接个案 6/12/24 月复查提醒仅针对「潜伏感染者」
-- 未开展：仅潜伏感染且未开展预防性治疗才发送
-- 已开展：默认关闭（政策：其余人员一律不发送复查提醒）

UPDATE `sys_message_reminder_config`
SET `name` = '密接个案-潜伏感染未开展预防性治疗随访提醒',
    `description` = '仅最终筛查结果为潜伏感染者、且未开展预防性治疗时，在 6/12/24 月随访到期窗口提醒录入者；非潜伏感染者不发送'
WHERE `code` = 'close_contact_case_followup_no_preventive';

UPDATE `sys_message_reminder_config`
SET `name` = '密接个案-已开展预防性治疗随访提醒',
    `description` = '仅最终筛查结果为潜伏感染者且已开展预防性治疗时提醒。当前政策默认关闭，非潜伏感染者不会发送',
    `enabled` = 0
WHERE `code` = 'close_contact_case_followup_with_preventive';
