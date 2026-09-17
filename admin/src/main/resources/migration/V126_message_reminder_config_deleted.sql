-- V126：sys_message_reminder_config 补齐逻辑删除字段，对齐 BaseEntity.@TableLogic
-- 线上曾因缺 deleted 导致 /message/reminders 查询 500：Unknown column 'deleted'

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_message_reminder_config'
      AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE `sys_message_reminder_config` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除'' AFTER `update_time`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
