-- V108：学生人群「待诊断」菜单名改为「学生报表统计」
UPDATE `permission`
SET `name` = '学生报表统计'
WHERE `code` = 'school:suspected';
