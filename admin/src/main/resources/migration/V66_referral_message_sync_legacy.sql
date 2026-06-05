-- V66：修复已确认推介但系统消息仍为「待确认推介」的历史数据（含 biz_mode 误标为 track）

UPDATE `sys_message` sm
    INNER JOIN `referral_tracking` rt ON sm.biz_id = rt.id AND rt.deleted = 0
SET sm.type     = 'referral_tracking_confirmed',
    sm.title    = '推介已接收',
    sm.content  = CONCAT('「', IFNULL(NULLIF(TRIM(rt.name), ''), '（未知姓名）'),
                         '」的推介通知单您已确认接收，已进入追踪环节。'),
    sm.is_read  = 1
WHERE sm.type = 'referral_tracking_receive'
  AND sm.deleted = 0
  AND rt.recommend_status = 2;

UPDATE `sys_message` sm
    INNER JOIN `referral_tracking` rt ON sm.biz_id = rt.id AND rt.deleted = 0
SET sm.type     = 'referral_tracking_rejected',
    sm.title    = '推介已被拒绝',
    sm.content  = CONCAT('「', IFNULL(NULLIF(TRIM(rt.name), ''), '（未知姓名）'),
                         '」的推介通知单您已拒绝，原因：',
                         IFNULL(NULLIF(TRIM(rt.rejected_reason), ''), '（未填写）')),
    sm.is_read  = 1
WHERE sm.type = 'referral_tracking_receive'
  AND sm.deleted = 0
  AND rt.recommend_status = 3;

UPDATE `referral_tracking`
SET `biz_mode` = 'recommend'
WHERE `recommend_sent_time` IS NOT NULL
  AND `biz_mode` = 'track'
  AND `recommend_status` IN (1, 2)
  AND `deleted` = 0;
