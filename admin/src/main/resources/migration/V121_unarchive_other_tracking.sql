-- V121：历史「其他」误归档恢复为可继续追踪（无诊断结果的其他状态）
UPDATE `referral_tracking`
SET `archived` = 0
WHERE `tracking_status` = 3
  AND `archived` = 1
  AND (`diagnosis_result` IS NULL OR `diagnosis_result` = '');
