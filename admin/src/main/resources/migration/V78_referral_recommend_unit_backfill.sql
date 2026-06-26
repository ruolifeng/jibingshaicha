-- 历史推介记录补全推介单位名称、填写用户名称
UPDATE referral_tracking rt
    LEFT JOIN user u ON u.id = rt.creator_id AND u.deleted = 0
    LEFT JOIN department d ON d.id = u.department_id
SET rt.recommend_unit_name = COALESCE(NULLIF(d.name, ''), NULLIF(u.org_name, '')),
    rt.fill_user_name = COALESCE(NULLIF(u.real_name, ''), u.username)
WHERE rt.deleted = 0
  AND rt.biz_mode = 'recommend'
  AND rt.creator_id IS NOT NULL
  AND (rt.recommend_unit_name IS NULL OR rt.recommend_unit_name = ''
    OR rt.fill_user_name IS NULL OR rt.fill_user_name = '');
