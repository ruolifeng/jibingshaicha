-- V34：常规筛查改名为疫情筛查（权限名称）
UPDATE `permission`
SET `name` = '疫情筛查'
WHERE `code` = 'regular:screening';

UPDATE `permission`
SET `name` = '疫情筛查-待诊断'
WHERE `code` = 'regular:suspected';
