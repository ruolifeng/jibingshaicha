-- V125：修复 referral_tracking 第二接收人列名与 MyBatis-Plus 映射不一致
-- Java 字段 receiverUserId2 默认映射为 receiver_user_id2（不是 receiver_user_id_2）
-- 线上曾因列名错误导致 list/dashboard 查询 500：Unknown column 'receiver_user_id2'

SET @old_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'referral_tracking'
      AND COLUMN_NAME = 'receiver_user_id_2'
);
SET @new_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'referral_tracking'
      AND COLUMN_NAME = 'receiver_user_id2'
);
SET @sql := IF(@old_col > 0 AND @new_col = 0,
    'ALTER TABLE `referral_tracking` CHANGE COLUMN `receiver_user_id_2` `receiver_user_id2` BIGINT NULL COMMENT ''第二接收用户ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @old_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'referral_tracking'
      AND COLUMN_NAME = 'receiver_dept_id_2'
);
SET @new_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'referral_tracking'
      AND COLUMN_NAME = 'receiver_dept_id2'
);
SET @sql := IF(@old_col > 0 AND @new_col = 0,
    'ALTER TABLE `referral_tracking` CHANGE COLUMN `receiver_dept_id_2` `receiver_dept_id2` BIGINT NULL COMMENT ''第二接收部门ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 若仅缺正确列名（无旧列），则补齐，避免旧版 JAR 再因缺列报错
SET @new_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'referral_tracking'
      AND COLUMN_NAME = 'receiver_user_id2'
);
SET @sql := IF(@new_col = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `receiver_user_id2` BIGINT NULL COMMENT ''第二接收用户ID'' AFTER `receiver_dept_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @new_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'referral_tracking'
      AND COLUMN_NAME = 'receiver_dept_id2'
);
SET @sql := IF(@new_col = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `receiver_dept_id2` BIGINT NULL COMMENT ''第二接收部门ID'' AFTER `receiver_user_id2`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
