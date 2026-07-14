-- V90：腾讯云短信配置（超管后台维护）+ 菜单权限

CREATE TABLE IF NOT EXISTS `sys_sms_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `enabled`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否开启短信：0否 1是',
    `secret_id`   VARCHAR(128) DEFAULT NULL COMMENT '腾讯云 SecretId',
    `secret_key`  VARCHAR(256) DEFAULT NULL COMMENT '腾讯云 SecretKey',
    `sdk_app_id`  VARCHAR(64)  DEFAULT NULL COMMENT '短信 SdkAppId',
    `sign_name`   VARCHAR(64)  DEFAULT NULL COMMENT '短信签名',
    `template_id` VARCHAR(64)  DEFAULT NULL COMMENT '短信模板 ID（单变量）',
    `region`      VARCHAR(32)  NOT NULL DEFAULT 'ap-guangzhou' COMMENT '地域',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统短信配置（单行）';

INSERT INTO `sys_sms_config` (`enabled`, `region`, `deleted`)
SELECT 0, 'ap-guangzhou', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_sms_config` WHERE `deleted` = 0 LIMIT 1);

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'system:sms', '短信配置', 1, p.id, 11
FROM `permission` p
WHERE p.code = 'system'
  AND NOT EXISTS (SELECT 1 FROM `permission` WHERE code = 'system:sms');

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, p.id
FROM `permission` p
WHERE p.code = 'system:sms';
