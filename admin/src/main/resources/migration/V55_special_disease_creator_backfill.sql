-- V55：专病网导入补全 creator_id，修复五级用户看不到自己导入的患者

-- 部门内仅一名五级用户时，回填其部门下缺失录入人的专病网患者
UPDATE `patient` p
    INNER JOIN (
        SELECT u.department_id, MIN(u.id) AS user_id, COUNT(*) AS cnt
        FROM `user` u
        WHERE u.role = 6 AND u.deleted = 0 AND u.department_id IS NOT NULL
        GROUP BY u.department_id
        HAVING cnt = 1
    ) solo ON solo.department_id = p.department_id
SET p.creator_id = solo.user_id
WHERE p.population_type = 'specialDisease'
  AND p.creator_id IS NULL
  AND p.deleted = 0;
