-- V64：推介确认/拒绝后同步接收方系统消息状态（修复历史遗留的「待确认推介」）

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
