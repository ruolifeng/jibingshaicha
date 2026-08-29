-- V119：接收方已确认转出且已生成副本后，源记录必须标记「已转出」并退出转出单位在管/随访列表
-- 仅处理 target_biz_id 已存在的确认记录，避免误伤「未复制」的历史确认数据

UPDATE `patient` p
INNER JOIN `referral` r ON r.biz_id = p.id
  AND r.module_type = 'patient'
  AND r.status = 2
  AND r.deleted = 0
  AND r.target_biz_id IS NOT NULL
SET p.`archive_remark` = '已转出',
    p.`archived` = 1,
    p.`archived_time` = COALESCE(p.`archived_time`, r.`confirmed_time`, NOW())
WHERE p.`deleted` = 0
  AND (
    p.`archive_remark` IS NULL
    OR p.`archive_remark` <> '已转出'
    OR p.`archived` = 0
    OR p.`archived` IS NULL
  );

UPDATE `latent_infection` l
INNER JOIN `referral` r ON r.biz_id = l.id
  AND r.module_type = 'latent'
  AND r.status = 2
  AND r.deleted = 0
  AND r.target_biz_id IS NOT NULL
SET l.`archive_remark` = '已转出',
    l.`archived` = 1,
    l.`archived_time` = COALESCE(l.`archived_time`, r.`confirmed_time`, NOW())
WHERE l.`deleted` = 0
  AND (
    l.`archive_remark` IS NULL
    OR l.`archive_remark` <> '已转出'
    OR l.`archived` = 0
    OR l.`archived` IS NULL
  );
