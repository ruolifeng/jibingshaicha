-- V52：推介追踪分流至潜伏/患者时补全 creator_id，修复五级录入者不可见

UPDATE `latent_infection` li
    INNER JOIN `referral_tracking` rt ON rt.target_latent_id = li.id AND rt.deleted = 0
SET li.creator_id = rt.creator_id
WHERE li.creator_id IS NULL
  AND rt.creator_id IS NOT NULL;

UPDATE `patient` p
    INNER JOIN `referral_tracking` rt ON rt.target_patient_id = p.id AND rt.deleted = 0
SET p.creator_id = rt.creator_id
WHERE p.creator_id IS NULL
  AND rt.creator_id IS NOT NULL;
