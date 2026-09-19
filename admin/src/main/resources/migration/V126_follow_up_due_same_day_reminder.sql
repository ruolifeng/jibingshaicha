-- 随访/督导提醒：增加当天节点说明（实际发送逻辑由代码 LEAD_DAYS 含 0 实现）
UPDATE `sys_message_reminder_config`
SET `description` = '患者下次随访到期前 7/3/1 天及当天提醒；当天提醒不受此前已读影响'
WHERE `code` = 'follow_up_due';

UPDATE `sys_message_reminder_config`
SET `description` = '潜伏督导到期前 7/3/1 天及当天提醒；当天提醒不受此前已读影响'
WHERE `code` = 'supervision_due';
