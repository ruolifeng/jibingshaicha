-- V92：转出确认后原记录归档退出在管，保证在管列表同一人仅一条
-- 纠正 V57/V58 将「已转出」强行改回 archived=0 的历史策略

UPDATE `latent_infection`
SET `archived` = 1,
    `archived_time` = COALESCE(`archived_time`, NOW())
WHERE `deleted` = 0
  AND `archive_remark` = '已转出'
  AND (`archived` = 0 OR `archived` IS NULL);

UPDATE `patient`
SET `archived` = 1,
    `archived_time` = COALESCE(`archived_time`, NOW())
WHERE `deleted` = 0
  AND `archive_remark` = '已转出'
  AND (`archived` = 0 OR `archived` IS NULL);
