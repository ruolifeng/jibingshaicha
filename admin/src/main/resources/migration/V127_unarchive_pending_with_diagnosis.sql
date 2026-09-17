-- V127：回退「创建预填诊断即归档」导致的待追踪/未到位/其他误归档
-- 现象：列表显示待追踪且可点追踪，确认时却提示「该记录已归档，无法继续追踪」

UPDATE `referral_tracking`
SET `archived` = 0
WHERE `archived` = 1
  AND `tracking_status` IN (0, 2, 3)
  AND (`deleted` = 0 OR `deleted` IS NULL);
