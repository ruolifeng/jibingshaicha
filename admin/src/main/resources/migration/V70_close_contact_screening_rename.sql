-- V70：密接人群管理 —「筛查管理」改名为「密接筛查」（与一级菜单「筛查管理」区分）
UPDATE `permission`
SET `name` = '密接筛查'
WHERE `code` = 'closeContact:screening';
