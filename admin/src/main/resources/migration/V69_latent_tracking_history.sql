-- 潜伏感染/待诊断追踪：增加追踪历史 JSON，记录每次追踪的备注
ALTER TABLE `latent_infection`
    ADD COLUMN `tracking_history_json` TEXT DEFAULT NULL COMMENT '追踪历史JSON（每次追踪的状态、时间、备注）' AFTER `tracking_remark`;
