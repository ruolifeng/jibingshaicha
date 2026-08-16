-- V114：随访/督导到期提醒（7/3/1 天）及督导表下次督导时间

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'supervision_form' AND COLUMN_NAME = 'next_supervision_date') = 0,
    'ALTER TABLE `supervision_form` ADD COLUMN `next_supervision_date` DATE DEFAULT NULL COMMENT ''下次督导时间'' AFTER `treatment_end_date`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `visit_supervision_reminder_log` (
    `id`          BIGINT       NOT NULL,
    `biz_type`    VARCHAR(32)  NOT NULL COMMENT 'follow_up / supervision',
    `biz_id`      BIGINT       NOT NULL COMMENT '患者ID或潜伏感染ID',
    `source_id`   BIGINT       DEFAULT NULL COMMENT '首次随访/后续随访/督导表记录ID',
    `due_date`    DATE         NOT NULL COMMENT '计划下次随访或督导日期',
    `lead_days`   INT          NOT NULL COMMENT '提前天数：7/3/1',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_due_lead` (`biz_type`, `biz_id`, `due_date`, `lead_days`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访/督导到期提醒发送记录';
