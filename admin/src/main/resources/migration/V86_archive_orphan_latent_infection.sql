-- V86：归档「关联筛查已删除」的孤儿潜伏感染记录（修复工作台待追踪虚高）

UPDATE `latent_infection` li
SET li.`archived` = 1,
    li.`archived_time` = NOW(),
    li.`archive_remark` = '关联筛查已删除-自动归档'
WHERE li.`deleted` = 0
  AND li.`archived` = 0
  AND li.`screening_id` IS NOT NULL
  AND (
    (li.`population_type` = 'school' AND NOT EXISTS (
        SELECT 1 FROM `screening_school` s
        WHERE s.`id` = li.`screening_id` AND s.`deleted` = 0
    ))
    OR (li.`population_type` IN ('keyPopulation', 'regular') AND NOT EXISTS (
        SELECT 1 FROM `screening_key_population` s
        WHERE s.`id` = li.`screening_id` AND s.`deleted` = 0
    ))
    OR (li.`population_type` = 'closeContact' AND NOT EXISTS (
        SELECT 1 FROM `screening_close_contact` s
        WHERE s.`id` = li.`screening_id` AND s.`deleted` = 0
    ))
  );
