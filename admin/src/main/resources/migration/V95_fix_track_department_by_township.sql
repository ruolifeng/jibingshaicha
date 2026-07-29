-- 修复追踪模块 department_id 与乡镇字段不一致（镇级数据串扰）
-- 根因：五级大疫情重新导入时按卡片ID跨镇命中，并把 department_id 抢到本镇

-- 1) 乡镇名精确匹配部门；同区县内纠正错挂
UPDATE referral_tracking rt
INNER JOIN department town ON town.name = rt.township AND town.deleted = 0
LEFT JOIN department cur ON cur.id = rt.department_id
SET rt.department_id = town.id
WHERE rt.deleted = 0
  AND rt.biz_mode = 'track'
  AND rt.township IS NOT NULL AND rt.township <> ''
  AND rt.department_id IS NOT NULL
  AND rt.department_id <> town.id
  AND (
    cur.parent_id IS NULL
    OR town.parent_id = cur.parent_id
    OR town.parent_id = cur.id
    OR town.id = cur.parent_id
    OR EXISTS (
      SELECT 1 FROM department creator_dept
      INNER JOIN user u ON u.id = rt.creator_id
      WHERE u.department_id = creator_dept.id
        AND (
          town.parent_id = creator_dept.parent_id
          OR town.parent_id = creator_dept.id
          OR town.id = creator_dept.id
        )
    )
  );

-- 2) 乡镇字段本身是区县名时，挂到对应区县
UPDATE referral_tracking rt
INNER JOIN department county ON county.name = rt.township AND county.level = 2 AND county.deleted = 0
SET rt.department_id = county.id
WHERE rt.deleted = 0
  AND rt.biz_mode = 'track'
  AND rt.township IS NOT NULL AND rt.township <> ''
  AND (rt.department_id IS NULL OR rt.department_id <> county.id);
