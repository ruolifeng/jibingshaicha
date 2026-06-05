-- V65：已确认推介保留在推介模块（biz_mode 恢复为 recommend，不再误入追踪列表）

UPDATE `referral_tracking`
SET `biz_mode` = 'recommend'
WHERE `recommend_status` = 2
  AND `biz_mode` = 'track'
  AND `recommend_sent_time` IS NOT NULL
  AND `deleted` = 0;
