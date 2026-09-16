-- V124：回退推介/追踪「误归档」——待追踪/未到位/其他/到位未诊断仍被 archived=1 的记录
-- 现象：推介页待追踪无「追踪」按钮、到位无录入按钮（前端把 archived 当结案拦截）

UPDATE `referral_tracking`
SET `archived` = 0
WHERE `archived` = 1
  AND `tracking_status` IN (0, 1, 2, 3)
  AND (`diagnosis_result` IS NULL OR `diagnosis_result` = '');
